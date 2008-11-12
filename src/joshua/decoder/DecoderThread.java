package joshua.decoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;



import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KbestExtraction;
import joshua.lattice.Lattice;
import joshua.util.FileUtility;
import joshua.util.sentence.Phrase;

/**
 * this class implements: 
 * (1) interact with the chart-parsing functions 
 * (2) parallel decoding
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 (星期一, 20 十月 2008) $
 */

public class DecoderThread {
	private JoshuaDecoder p_main_controller = null;//point back to the main controller
	private Thread[]          parallel_threads;
	private String[]          parallel_testFiles;
	private String[]          parallel_nbestFiles;
	private DiskHyperGraph[]  parallel_DHGs;
	private KbestExtraction[] parallel_kbestExtractors;
	
	private static final Logger logger = Logger.getLogger(DecoderThread.class.getName());

	public  DecoderThread(JoshuaDecoder main_controller){
		p_main_controller = main_controller;		
	}
	
	public void decodingTestSet(String test_file, String nbest_file){
//		###### decode the sentences, maybe in parallel
		if (JoshuaConfiguration.num_parallel_decoders == 1) {
			DiskHyperGraph  d_hg = null;
			KbestExtraction kbest_extrator = new KbestExtraction(p_main_controller.p_symbol);
			if (JoshuaConfiguration.save_disk_hg) {
				//d_hg = new DiskHyperGraph(p_main_controller.p_symbol, p_main_controller.p_l_feat_functions);
				d_hg = new DiskHyperGraph(p_main_controller.p_symbol, p_main_controller.haveLMFeature(p_main_controller.p_l_feat_functions).getFeatureID());
				d_hg.init_write(nbest_file + ".hg.items", JoshuaConfiguration.forest_pruning, JoshuaConfiguration.forest_pruning_threshold);
			}
			decode_a_file(test_file, nbest_file, 0, d_hg, kbest_extrator);
			if (JoshuaConfiguration.save_disk_hg) {
				d_hg.write_rules_non_parallel(nbest_file + ".hg.rules");
			}
			
		} else {
			if (JoshuaConfiguration.use_remote_lm_server) { // TODO
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("You cannot run parallel decoder and remote lm server together");
				System.exit(1);
			}
			run_parallel_decoder(test_file, nbest_file);
		}
	}
	
	
	private void run_parallel_decoder(String test_file, String nbest_file) {
		int    n_lines           = FileUtility.number_lines_in_file(test_file);
		double num_per_thread_double = n_lines * 1.0 / JoshuaConfiguration.num_parallel_decoders;
		int    num_per_thread_int    = (int)num_per_thread_double;
		parallel_threads         = new Thread[JoshuaConfiguration.num_parallel_decoders];
		parallel_testFiles       = new String[JoshuaConfiguration.num_parallel_decoders];
		parallel_nbestFiles      = new String[JoshuaConfiguration.num_parallel_decoders];
		parallel_DHGs            = new DiskHyperGraph[JoshuaConfiguration.num_parallel_decoders];
		parallel_kbestExtractors = new KbestExtraction[JoshuaConfiguration.num_parallel_decoders];
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info("num_per_file_double: " + num_per_thread_double	+ "num_per_file_int: " + num_per_thread_int);
		BufferedReader t_reader_test = FileUtility.getReadFileStream(test_file);
		
		
		// Initialize all threads and their input files
		int decoder_i = 1;
		String cur_test_file  = JoshuaConfiguration.parallel_files_prefix + ".test."  + decoder_i;
		String cur_nbest_file = JoshuaConfiguration.parallel_files_prefix + ".nbest." + decoder_i;
		BufferedWriter t_writer_test =
			FileUtility.getWriteFileStream(cur_test_file);
		int sent_id       = 0;
		int start_sent_id = sent_id;
		
		String cn_sent;
		while ((cn_sent = FileUtility.read_line_lzf(t_reader_test)) != null) {
			sent_id++;
			FileUtility.write_lzf(t_writer_test, cn_sent + "\n");
			
			//make the Symbol table finalized before running multiple threads, this is to avoid synchronization among threads
			{
				String words[] = cn_sent.split("\\s+");				
				p_main_controller.p_symbol.addTerminalSymbols(words); // TODO				
			}			
			
			if (0 != sent_id
			// we will include all additional lines into last file
			&& decoder_i < JoshuaConfiguration.num_parallel_decoders
			&& sent_id % num_per_thread_int == 0) {			
				// submit current job
				FileUtility.flush_lzf(t_writer_test);
				FileUtility.close_write_file(t_writer_test);
				DiskHyperGraph dhg = null;
				if (JoshuaConfiguration.save_disk_hg) {
					//dhg = new DiskHyperGraph(p_main_controller.p_symbol, p_main_controller.p_l_feat_functions);
					dhg = new DiskHyperGraph(p_main_controller.p_symbol, p_main_controller.haveLMFeature(p_main_controller.p_l_feat_functions).getFeatureID());
					dhg.init_write(	cur_nbest_file + ".hg.items",  JoshuaConfiguration.forest_pruning,  JoshuaConfiguration.forest_pruning_threshold);
				}
				KbestExtraction kbest_extrator = new KbestExtraction(p_main_controller.p_symbol);
				ParallelDecoderThread pdecoder = new ParallelDecoderThread(cur_test_file, cur_nbest_file,	start_sent_id, dhg, kbest_extrator);
				parallel_threads[        decoder_i-1] = pdecoder;
				parallel_testFiles[      decoder_i-1] = cur_test_file;
				parallel_nbestFiles[     decoder_i-1] = cur_nbest_file;
				parallel_DHGs[           decoder_i-1] = dhg;
				parallel_kbestExtractors[decoder_i-1] = kbest_extrator;
				
				// prepare next job
				start_sent_id  = sent_id;
				decoder_i++;
				cur_test_file  = JoshuaConfiguration.parallel_files_prefix + ".test."  + decoder_i;
				cur_nbest_file = JoshuaConfiguration.parallel_files_prefix + ".nbest." + decoder_i;
				t_writer_test  = FileUtility.getWriteFileStream(cur_test_file);
			}
		}
		
		// prepare the the last job
		FileUtility.flush_lzf(t_writer_test);
		FileUtility.close_write_file(t_writer_test);
		DiskHyperGraph dhg = null;
		if (JoshuaConfiguration.save_disk_hg) {
			//dhg = new DiskHyperGraph(p_main_controller.p_symbol, p_main_controller.p_l_feat_functions);
			dhg = new DiskHyperGraph(p_main_controller.p_symbol, p_main_controller.haveLMFeature(p_main_controller.p_l_feat_functions).getFeatureID());
			dhg.init_write(	cur_nbest_file + ".hg.items", JoshuaConfiguration.forest_pruning,  JoshuaConfiguration.forest_pruning_threshold);
		}
		KbestExtraction kbest_extrator = new KbestExtraction(p_main_controller.p_symbol);
		ParallelDecoderThread pdecoder = new ParallelDecoderThread(cur_test_file, cur_nbest_file,	start_sent_id,	dhg, kbest_extrator);
		parallel_threads[        decoder_i-1] = pdecoder;
		parallel_testFiles[      decoder_i-1] = cur_test_file;
		parallel_nbestFiles[     decoder_i-1] = cur_nbest_file;
		parallel_DHGs[           decoder_i-1] = dhg;		
		parallel_kbestExtractors[decoder_i-1] = kbest_extrator;
		FileUtility.close_read_file(t_reader_test);
		// End initializing threads and their files
			
		
		// run all the jobs
		for (int i = 0; i < parallel_threads.length; i++) {
			if (logger.isLoggable(Level.INFO)) logger.info(
				"##############start thread " + i);
			parallel_threads[i].start();
		}
		
		// wait for the threads finish
		for (int i = 0; i < parallel_threads.length; i++) {
			try {
				parallel_threads[i].join();
			} catch (InterruptedException e) {
				if (logger.isLoggable(Level.WARNING)) 
					logger.warning("Warning: thread is interupted for server " + i);
			}
		}
		
		//#### merge the nbest files, and remove tmp files
		BufferedWriter t_writer_nbest =
			FileUtility.getWriteFileStream(nbest_file);
		BufferedWriter t_writer_dhg_items = null;
		if (JoshuaConfiguration.save_disk_hg) {
			t_writer_dhg_items = FileUtility.getWriteFileStream(nbest_file + ".hg.items");
		}
		for (int i = 0; i < parallel_threads.length; i++) {
			String sent;
			//merge nbest
			BufferedReader t_reader =
				FileUtility.getReadFileStream(parallel_nbestFiles[i]);
			while ((sent = FileUtility.read_line_lzf(t_reader)) != null) {
				FileUtility.write_lzf(t_writer_nbest,sent+"\n");
			}
			FileUtility.close_read_file(t_reader);	
			//TODO: remove the tem nbest file
			
			//merge hypergrpah items
			if (JoshuaConfiguration.save_disk_hg) {
				BufferedReader t_reader_dhg_items = 
					FileUtility.getReadFileStream(
						parallel_nbestFiles[i] + ".hg.items");
				while ((sent = FileUtility.read_line_lzf(t_reader_dhg_items)) != null) {
					FileUtility.write_lzf(t_writer_dhg_items, sent + "\n");
				}
				FileUtility.close_read_file(t_reader_dhg_items);
				//TODO: remove the tem nbest file
			}
		}
		FileUtility.flush_lzf(t_writer_nbest);	
		FileUtility.close_write_file(t_writer_nbest);
		if (JoshuaConfiguration.save_disk_hg) {
			FileUtility.flush_lzf(t_writer_dhg_items);
			FileUtility.close_write_file(t_writer_dhg_items);
		}
		
		//merge the grammar rules for disk hyper-graphs
		if (JoshuaConfiguration.save_disk_hg) {
			HashMap tbl_done = new HashMap();
			BufferedWriter t_writer_dhg_rules = 
				FileUtility.getWriteFileStream(nbest_file + ".hg.rules");
			for (DiskHyperGraph dhg2 : parallel_DHGs) {
				dhg2.write_rules_parallel(t_writer_dhg_rules, tbl_done);
			}
			FileUtility.flush_lzf(t_writer_dhg_rules);
			FileUtility.close_write_file(t_writer_dhg_rules);
		}
	}
	
	
	
	//TODO: known synchronization problem: LM cache; srilm call;
	
	public class ParallelDecoderThread extends Thread {
		String          test_file;
		String          nbest_file;
		int             start_sent_id; //start sent id
		DiskHyperGraph  dpg;
		KbestExtraction kbest_extractor;
		
		
		public ParallelDecoderThread(String test_file_in, String nbest_file_in,	int start_sent_id_in, DiskHyperGraph dpg_in, KbestExtraction extractor) {
			this.test_file       = test_file_in;
			this.nbest_file      = nbest_file_in;
			this.start_sent_id   = start_sent_id_in;
			this.dpg             = dpg_in;
			this.kbest_extractor = extractor;
		}
		
		
		public void run() {
			decode_a_file(test_file, nbest_file, start_sent_id, dpg, kbest_extractor);
		}
	}
	
//	TODO: log file is not properly handled for parallel decoding
	private void decode_a_file( String test_file, String nbest_file, int start_sent_id, DiskHyperGraph dhg, KbestExtraction kbest_extractor) {
		BufferedReader t_reader_test = FileUtility.getReadFileStream(test_file);
		BufferedWriter t_writer_nbest =	FileUtility.getWriteFileStream(nbest_file);
		
		String cn_sent;
		int sent_id = start_sent_id; // if no sent tag, then this will be used
		while ((cn_sent = FileUtility.read_line_lzf(t_reader_test)) != null) {
			if (logger.isLoggable(Level.FINE)) 
				logger.fine("now translate\n" + cn_sent);
			int[] tem_id = new int[1];
			cn_sent = get_sent_id(cn_sent, tem_id);
			if (tem_id[0] > 0) {
				sent_id = tem_id[0];
			}
			/*if (JoshuaConfiguration.use_sent_specific_lm) {
				load_lm_grammar_file(JoshuaConfiguration.g_sent_lm_file_name_prefix + sent_id + ".gz");
			}
			if (JoshuaConfiguration.use_sent_specific_tm) {
				initializeTranslationGrammars(JoshuaConfiguration.g_sent_tm_file_name_prefix + sent_id + ".gz");
			}*/
			
			translate(p_main_controller.p_tm_grammars, p_main_controller.p_l_feat_functions, cn_sent, p_main_controller.l_default_nonterminals, t_writer_nbest, sent_id, JoshuaConfiguration.topN, dhg, kbest_extractor);
			sent_id++;
			//if (sent_id > 10) break;//debug
		}
		FileUtility.close_read_file(t_reader_test);
		FileUtility.flush_lzf(t_writer_nbest);		
		FileUtility.close_write_file(t_writer_nbest);
		
		//debug
		//g_con.print_confusion_tbl(f_confusion_grammar);
	}
	
	
	
	/**
	 * Translate a sentence.
	 * 
	 * @param grammars Translation grammars to be used during translation.
	 * @param models Models to be used when scoring rules.
	 * @param sentence The sentence to be translated.
	 * @param defaultNonterminals
	 * @param out
	 * @param sentenceID
	 * @param topN
	 * @param diskHyperGraph
	 * @param kbestExtractor
	 */
	private void translate(GrammarFactory[]  grammarFactories, ArrayList<FeatureFunction> models, String sentence, ArrayList<Integer>   defaultNonterminals,
		BufferedWriter out, int  sentenceID, int   topN, DiskHyperGraph diskHyperGraph, KbestExtraction kbestExtractor) {
		long  start = System.currentTimeMillis();
		int[] sentence_numeric = p_main_controller.p_symbol.addTerminalSymbols(sentence);
		
		Integer[] input = new Integer[sentence_numeric.length];
		for (int i = 0; i < sentence_numeric.length; i++) {
			input[i] = sentence_numeric[i];
		}
		Lattice<Integer> inputLattice = new Lattice<Integer>(input);
		
		Grammar[] grammars = new Grammar[grammarFactories.length ];
		for (int i = 0; i < grammarFactories.length; i++) {
			grammars[i] = grammarFactories[i].getGrammarForSentence(null);//TODO: if using suffix-array, then we need provide a non-null Phrase object (i.e., the input sentence)
			if(grammars[i].getTrieRoot()==null){
				System.out.println("grammars  getTrieRoot is null; i is "+i); System.exit(0);
			}
		}
		
		//seeding: the chart only sees the grammars, not the grammarFactories
		Chart chart  = new Chart(inputLattice, models,	this.p_main_controller.p_symbol, sentenceID,	grammars, defaultNonterminals, JoshuaConfiguration.untranslated_owner, this.p_main_controller.have_lm_model);//TODO: owner		
		if (logger.isLoggable(Level.FINER)) 
			logger.finer("after seed, time: " + (System.currentTimeMillis() - start) / 1000);
		
		//parsing
		HyperGraph p_hyper_graph = chart.expand();
		if (logger.isLoggable(Level.FINER)) 
			logger.finer("after expand, time: " + (System.currentTimeMillis() - start)/1000);
		
		//kbest extraction
		kbestExtractor.lazy_k_best_extract_hg(p_hyper_graph, models, topN, JoshuaConfiguration.use_unique_nbest, sentenceID, out, JoshuaConfiguration.use_tree_nbest, JoshuaConfiguration.add_combined_cost);
		if (logger.isLoggable(Level.FINER)) logger.finer(
			"after kbest, time: " + (System.currentTimeMillis() - start)/1000);
		
		if (null != diskHyperGraph) {
			diskHyperGraph.save_hyper_graph(p_hyper_graph);
		}
		
		//debug
		if(JoshuaConfiguration.use_variational_decoding){/*
			ConstituentVariationalDecoder vd = new ConstituentVariationalDecoder();
			vd.decoding(p_hyper_graph);
			System.out.println("#### new 1best is #####\n" + HyperGraph.extract_best_string(p_main_controller.p_symbol,p_hyper_graph.goal_item));
			*/
		}
		//end
		
		
		//debug
		//g_con.get_confusion_in_hyper_graph_cell_specific(p_hyper_graph,p_hyper_graph.sent_len);
	}
	
	
	
	//return sent without the tag
	//if no sent id, then return -1 in sent_id[]
	private static String get_sent_id(String sent, int[] sent_id) {
		if (sent.matches("^<seg\\s+id=.*$")) {//havd sent id
			String res_sent = sent.replaceAll("^<seg\\s+id=\"", "");
			String str_id   = "";
			for (int i = 0; i < res_sent.length(); i++) {
				char cur = res_sent.charAt(i);
				if (cur != '"') {
					str_id += cur;
				} else {
					break;
				}
			}
			int res_id = (new Integer(str_id)).intValue();
			res_sent   = res_sent.replaceFirst(str_id+"\">", "");
			res_sent   = res_sent.replaceAll("</seg>", "");
			sent_id[0] = res_id;
			return res_sent;
		} else {
			sent_id[0] = -1;
			return sent;
		}
    }
	
}
