/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder;


import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.ArityPhrasePenaltyFF;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.decoder.ff.SourceLatticeArcCostFF;
import joshua.decoder.ff.lm.LMGrammar;
import joshua.decoder.ff.lm.LMModel;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;
import joshua.decoder.ff.lm.distributed_lm.LMGrammarRemote;
import joshua.decoder.ff.lm.srilm.LMGrammarSRILM;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.MemoryBasedTMGrammar;
import joshua.decoder.ff.tm.TMGrammar;
import joshua.util.FileUtility;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class implements:
 * (1) mainly initialize, and control the interaction with JoshuaConfiguration and DecoderThread
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class JoshuaDecoder {	
	DecoderThread p_decoder_thread = null;//pointer to the main thread of decoding
	GrammarFactory[] p_tm_grammars = null;
	
	//TODO: deal with cases of multiple LMs or no LM at all
	LMGrammar       p_lm_grammar  = null;//the lm grammar itself (not lm model)
	
	ArrayList<FeatureFunction> p_l_models     = null;
	ArrayList<Integer> l_default_nonterminals = null;
		
	private static final Logger logger = Logger.getLogger(JoshuaDecoder.class.getName());
	
//===============================================================
//===============================================================
	public static void main(String[] args) {
	
		if (logger.isLoggable(Level.FINEST)) 
			logger.finest("Starting decoder");
		
		long start = System.currentTimeMillis();
		if (args.length != 3) {
			System.out.println("Usage: java joshua.decoder.Decoder config_file test_file outfile");
			System.out.println("num of args is "+ args.length);
			for (int i = 0; i < args.length; i++) {
				System.out.println("arg is: " + args[i]);
			}
			System.exit(1);
		}		
		String config_file = args[0].trim();
		String test_file   = args[1].trim();
		String nbest_file  = args[2].trim();
		
		JoshuaDecoder p_decoder = new JoshuaDecoder();
		
		//############ initialize the decoder ########		
		p_decoder.initializeDecoder(config_file);
				
		//###### statistics
		double t_sec = (System.currentTimeMillis() - start) / 1000;
		if (logger.isLoggable(Level.INFO)) 
			logger.info("before translation, loaddingtime is " + t_sec);

		//############ Decoding ########
		p_decoder.decodingTestSet(test_file, nbest_file);
		
		//############ clean up ########
		p_decoder.cleanUp();
		
		t_sec = (System.currentTimeMillis() - start) / 1000;
		if (logger.isLoggable(Level.INFO)) 
			logger.info("Total running time is " + t_sec);
		
	} // end main()
//===============================================================
//===============================================================
	
//	##### procedures: read config, init lm, init sym tbl, init models, read lm, read tm
	public void initializeDecoder(String config_file){
//		##### read config file
		JoshuaConfiguration.read_config_file(config_file);	
		
		p_decoder_thread = new DecoderThread(this);
		
		//TODO ##### add default non-terminals
		setDefaultNonTerminals(JoshuaConfiguration.default_non_terminal);
		
		//##### inside, it will init sym tbl (and add global symbols)
		initializeLanguageModel();
				
		//##### initialize the models(need to read config file again)
		p_l_models = init_models(config_file, p_lm_grammar);
		
		//##### read LM grammar
		if (! JoshuaConfiguration.use_sent_specific_lm) {
			load_lm_grammar_file(JoshuaConfiguration.lm_file);
		}
				
		//##### load TM grammar
		if (! JoshuaConfiguration.use_sent_specific_tm) {
			initializeTranslationGrammars(JoshuaConfiguration.tm_file);
		}				
	}
	
	/*Decoding a whole test set
	 * This may be parallel
	 * */
	public void decodingTestSet(String test_file, String nbest_file){		
		p_decoder_thread.decodingTestSet(test_file, nbest_file);
	}
	
	/*Decoding a sentence
	 * This must be non-parallel
	 * */
	public void decodingSentence(String test_sentence, String[] nbests){
		//TODO
	}
	
	public void cleanUp(){
		p_lm_grammar.end_lm_grammar(); //end the threads
	}
	
	
	public static ArrayList<FeatureFunction>  init_models(String config_file, LMGrammar lm_grammar){
		BufferedReader t_reader_config = FileUtility.getReadFileStream(config_file);
		ArrayList<FeatureFunction> l_models = new ArrayList<FeatureFunction>();
		
		String line;
		while ((line = FileUtility.read_line_lzf(t_reader_config)) != null){
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {//ignore empty lines or lines commented out
				continue;
			}
			
			if (line.indexOf("=") == -1) { //ignore lines with "="
				String[] fds = line.split("\\s+");
				if (fds[0].compareTo("lm") == 0 && fds.length == 2) { // lm order weight
					double weight = (new Double(fds[1].trim())).doubleValue();
					l_models.add(new LMModel(JoshuaConfiguration.g_lm_order, lm_grammar, weight));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest( String.format("Line: %s\nAdd LM, order: %d; weight: %.3f;", line, JoshuaConfiguration.g_lm_order, weight));				
				} else if (0 == fds[0].compareTo("latticecost")	&& fds.length == 2) {
					double weight = Double.parseDouble(fds[1].trim());
					l_models.add(new SourceLatticeArcCostFF(weight));
					if (logger.isLoggable(Level.FINEST)) logger.finest(
						String.format("Line: %s\nAdd Source lattice cost, weight: %.3f", weight));
				} else if (0 == fds[0].compareTo("phrasemodel")	&& fds.length == 4) { // phrasemodel owner column(0-indexed) weight
					int owner = Symbol.add_terminal_symbol(fds[1]);
					int column = (new Integer(fds[2].trim())).intValue();
					double weight = (new Double(fds[3].trim())).doubleValue();
					l_models.add(new PhraseModelFF(weight, owner, column));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd PhraseModel, owner: %s; column: %d; weight: %.3f", line, owner, column, weight));				
				} else if (0 == fds[0].compareTo("arityphrasepenalty") && fds.length == 5){//arityphrasepenalty owner start_arity end_arity weight
					int owner = Symbol.add_terminal_symbol(fds[1]);
					int start_arity = (new Integer(fds[2].trim())).intValue();
					int end_arity = (new Integer(fds[3].trim())).intValue();
					double weight = (new Double(fds[4].trim())).doubleValue();
					l_models.add(new ArityPhrasePenaltyFF(weight, owner, start_arity, end_arity));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd ArityPhrasePenalty, owner: %s; start_arity: %d; end_arity: %d; weight: %.3f",line, owner, start_arity, end_arity, weight));			
				} else if (0 == fds[0].compareTo("wordpenalty")	&& fds.length == 2) { // wordpenalty weight
					double weight = (new Double(fds[1].trim())).doubleValue();
					l_models.add(new WordPenaltyFF(weight));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd WordPenalty, weight: %.3f", line, weight));
				} else {
					if (logger.isLoggable(Level.SEVERE)) logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
			}
		}
		FileUtility.close_read_file(t_reader_config);
		return l_models;
	}
	
	private void setDefaultNonTerminals(String default_non_terminal){
		//TODO ##### add default non-terminals
		l_default_nonterminals = new ArrayList<Integer>();
		l_default_nonterminals.add(Symbol.add_non_terminal_symbol(default_non_terminal));
	}
	
	
	private void initializeLanguageModel() {
		if (JoshuaConfiguration.use_remote_lm_server) {
			if (JoshuaConfiguration.use_left_euqivalent_state || JoshuaConfiguration.use_right_euqivalent_state){
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("use local srilm, we cannot use suffix/prefix stuff");
				System.exit(1);
			}
			p_lm_grammar = new LMGrammarRemote(JoshuaConfiguration.g_lm_order, JoshuaConfiguration.remote_symbol_tbl, JoshuaConfiguration.f_remote_server_list, JoshuaConfiguration.num_remote_lm_servers);			
		} else if (JoshuaConfiguration.use_srilm) {
			System.loadLibrary("srilm"); //load once
			if (JoshuaConfiguration.use_left_euqivalent_state || JoshuaConfiguration.use_right_euqivalent_state) {
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("use SRILM, we cannot use suffix/prefix stuff");
				System.exit(1);
			}			
			p_lm_grammar = new LMGrammarSRILM(JoshuaConfiguration.g_lm_order);			
		} else {//using the built-in JAVA implementatoin of LM, may not be as scalable as SRILM
			p_lm_grammar = new LMGrammarJAVA(JoshuaConfiguration.g_lm_order, JoshuaConfiguration.use_left_euqivalent_state, JoshuaConfiguration.use_right_euqivalent_state);
		}
	}	
	
	private void load_lm_grammar_file(String lm_file){
		if (logger.isLoggable(Level.FINER)) logger.finer(
			"############## load lm from file" + lm_file);
		p_lm_grammar.read_lm_grammar_from_file(lm_file);
	}
	
	// This depends (invisibly) on the language model in order to do pruning of the TM at load time.
	private  void initializeTranslationGrammars(String tm_file) {
		if (logger.isLoggable(Level.FINER)) 
			logger.finer("############## load tm from file" + tm_file);
		
		p_tm_grammars = new GrammarFactory[2];
		
		// Glue Grammar
		GrammarFactory glueGrammar = new MemoryBasedTMGrammar(null, true, p_l_models, JoshuaConfiguration.phrase_owner, -1, "^\\[[A-Z]+\\,[0-9]*\\]$", "[\\[\\]\\,0-9]+");	
		p_tm_grammars[0] = glueGrammar;		
		if(((TMGrammar)glueGrammar).getTrieRoot()==null){
			System.out.println("grammar  getTrieRoot is null; i is 0"); System.exit(0);
		}
		
		// Regular TM Grammar
		GrammarFactory regularGrammar = new MemoryBasedTMGrammar(tm_file, false, p_l_models, JoshuaConfiguration.phrase_owner, JoshuaConfiguration.span_limit, "^\\[[A-Z]+\\,[0-9]*\\]$", "[\\[\\]\\,0-9]+");				
		p_tm_grammars[1] = regularGrammar;
	}
	
}
