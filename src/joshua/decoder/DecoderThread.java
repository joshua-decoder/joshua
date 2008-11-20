package joshua.decoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
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


/**
 * this class implements: 
 * (1) interact with the chart-parsing functions 
 * 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 (星期一, 20 十月 2008) $
 */

//TODO: known synchronization problem: LM cache; srilm call;

public class DecoderThread  extends Thread {
	//these variables may be the same across all threads (e.g., just copy from DecoderFactory), or differ from thread to thread 
	private GrammarFactory[] p_grammar_factories = null;
	private boolean have_lm_model = false;
	private ArrayList<FeatureFunction> p_l_feat_functions  = null;
	private ArrayList<Integer> l_default_nonterminals = null;
	private Symbol p_symbol = null;
	
	//more test set specific
	String          test_file;
	String          nbest_file;
	int             start_sent_id; //start sent id
	KbestExtraction kbest_extractor;
	DiskHyperGraph  p_disk_hg;
	
	
	private static final Logger logger = Logger.getLogger(DecoderThread.class.getName());
	
	public DecoderThread(GrammarFactory[] grammar_facories,  boolean have_lm_model_, ArrayList<FeatureFunction> l_feat_functions , ArrayList<Integer> l_default_nonterminals_ , Symbol symbol,
			String test_file_in, String nbest_file_in,	int start_sent_id_in) {
		this.p_grammar_factories = 	grammar_facories;
		this.have_lm_model =  have_lm_model_;
		this.p_l_feat_functions = l_feat_functions;
		this.l_default_nonterminals = l_default_nonterminals_;
		this.p_symbol = symbol;
		
		this.test_file       = test_file_in;
		this.nbest_file      = nbest_file_in;
		this.start_sent_id   = start_sent_id_in;
		
		this.kbest_extractor = new KbestExtraction(this.p_symbol);
		
		if (JoshuaConfiguration.save_disk_hg) {
			//this.p_disk_hg = new DiskHyperGraph(this.p_symbol, JoshuaDecoder.haveLMFeature(this.p_l_feat_functions).getFeatureID());
			this.p_disk_hg = new DiskHyperGraph(this.p_symbol, JoshuaDecoder.haveLMFeature(this.p_l_feat_functions).getFeatureID(), true, p_l_feat_functions);//always store model cost
			this.p_disk_hg.init_write(	this.nbest_file + ".hg.items",  JoshuaConfiguration.forest_pruning,  JoshuaConfiguration.forest_pruning_threshold);
		}
	}
	
	
	public void run() {
		decode_a_file();
	}
	

//	TODO: log file is not properly handled for parallel decoding
	public void decode_a_file() {
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
			
			translate(this.p_grammar_factories, this.p_l_feat_functions, cn_sent, this.l_default_nonterminals, t_writer_nbest, sent_id, JoshuaConfiguration.topN, p_disk_hg, kbest_extractor);
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
		int[] sentence_numeric = this.p_symbol.addTerminalSymbols(sentence);
		
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
		Chart chart  = new Chart(inputLattice, models,	this.p_symbol, sentenceID,	grammars, defaultNonterminals, JoshuaConfiguration.untranslated_owner, this.have_lm_model);//TODO: owner		
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
