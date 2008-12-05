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
import joshua.decoder.ff.lm.LMFeatureFunction;
import joshua.decoder.ff.lm.LMGrammar;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;
import joshua.decoder.ff.lm.distributed_lm.LMGrammarRemote;
import joshua.decoder.ff.lm.srilm.LMGrammarSRILM;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.MemoryBasedBatchGrammarWithPrune;
import joshua.util.FileUtility;


import java.io.BufferedReader;
import java.io.BufferedWriter;
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
	private DecoderFactory p_decoder_factory = null;//pointer to the main thread of decoding
	private GrammarFactory[] p_tm_grammars = null;
	private boolean have_lm_model = false;
	private ArrayList<FeatureFunction> p_l_feat_functions  = null;
	private ArrayList<Integer> l_default_nonterminals = null;
	private Symbol p_symbol = null;
	
//	TODO: deal with cases of multiple LMs or no LM at all
	//LMGrammar       p_lm_grammar  = null;//the lm grammar itself (not lm model)
	
	
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
		
		//############ Step-1: initialize the decoder ########		
		p_decoder.initializeDecoder(config_file);
		
		/* debug
		double[] weights = new double[5];
		weights[0]=1;weights[1]=1;weights[2]=1;weights[3]=1;weights[4]=1;
		p_decoder.changeFeatureWeightVector(weights);
		*/
		
		//###### statistics
		double t_sec = (System.currentTimeMillis() - start) / 1000;
		if (logger.isLoggable(Level.INFO)) 
			logger.info("before translation, loaddingtime is " + t_sec);

		//############ Step-2: Decoding ########
		p_decoder.decodingTestSet(test_file, nbest_file);
		
		//############ Step-3: clean up ########
		p_decoder.cleanUp();
		
		t_sec = (System.currentTimeMillis() - start) / 1000;
		if (logger.isLoggable(Level.INFO)) 
			logger.info("Total running time is " + t_sec);
		
	} // end main()
//===============================================================
//===============================================================
	
	/*this assumes that the weight_vector is ordered according to the decoder config file
	 * */
	public void changeFeatureWeightVector(double[] weight_vector){
		if(p_l_feat_functions.size()!=weight_vector.length){
			System.out.println("In updateFeatureWeightVector: number of weights does not match number of feature functions");
			System.exit(0);
		}		
		for(int i=0; i<p_l_feat_functions.size(); i++){
			FeatureFunction ff = p_l_feat_functions.get(i);
			double old_weight = ff.getWeight();
			ff.putWeight(weight_vector[i]);
			System.out.println("Feature function : " + ff.getClass().getSimpleName() + "; weight changed from " + old_weight + " to " + ff.getWeight());
		}
	}
	
//	##### procedures: read config, init lm, init sym tbl, init models, read lm, read tm
	public void initializeDecoder(String config_file){
//		##### read config file
		JoshuaConfiguration.read_config_file(config_file);	
		
		//#### initialize symbol table
		initSymbolTbl();
		
		
		//TODO ##### add default non-terminals
		setDefaultNonTerminals(JoshuaConfiguration.default_non_terminal);
	
				
		//##### initialize the models(need to read config file again)
		p_l_feat_functions = initializeFeatureFunctions(p_symbol, config_file);
		
		have_lm_model = (haveLMFeature(p_l_feat_functions) !=null) ? true : false;//check to see if there is a LM feature
		System.out.println("have lm model: " + have_lm_model);
		//##### load TM grammar
		if (! JoshuaConfiguration.use_sent_specific_tm) {
			initializeTranslationGrammars(JoshuaConfiguration.tm_file);
		}
		
		//create factory
		p_decoder_factory = new DecoderFactory(this.p_tm_grammars, this.have_lm_model, this.p_l_feat_functions, this.l_default_nonterminals, this.p_symbol);
		
	}
	
	/*Decoding a whole test set
	 * This may be parallel
	 * */
	public void decodingTestSet(String test_file, String nbest_file){		
		p_decoder_factory.decodingTestSet(test_file, nbest_file);
	}
	
	/*Decoding a sentence
	 * This must be non-parallel
	 * */
	public void decodingSentence(String test_sentence, String[] nbests){
		//TODO
	}
	
	public void cleanUp(){
		//TODO
		//p_lm_grammar.end_lm_grammar(); //end the threads
	}
	
	
	public  static ArrayList<FeatureFunction>  initializeFeatureFunctions(Symbol psymbol, String config_file){
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
					LMGrammar lm_grammar = initializeLanguageModel(psymbol);					
					l_models.add(new LMFeatureFunction(l_models.size(), JoshuaConfiguration.g_lm_order, psymbol, lm_grammar, weight));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest( String.format("Line: %s\nAdd LM, order: %d; weight: %.3f;", line, JoshuaConfiguration.g_lm_order, weight));				
				} else if (0 == fds[0].compareTo("latticecost")	&& fds.length == 2) {
					double weight = Double.parseDouble(fds[1].trim());
					l_models.add(new SourceLatticeArcCostFF(l_models.size(), weight));
					if (logger.isLoggable(Level.FINEST)) logger.finest(
						String.format("Line: %s\nAdd Source lattice cost, weight: %.3f", weight));
				} else if (0 == fds[0].compareTo("phrasemodel")	&& fds.length == 4) { // phrasemodel owner column(0-indexed) weight
					int owner = psymbol.addTerminalSymbol(fds[1]);
					int column = (new Integer(fds[2].trim())).intValue();
					double weight = (new Double(fds[3].trim())).doubleValue();
					l_models.add(new PhraseModelFF(l_models.size(), weight, owner, column));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd PhraseModel, owner: %s; column: %d; weight: %.3f", line, owner, column, weight));				
				} else if (0 == fds[0].compareTo("arityphrasepenalty") && fds.length == 5){//arityphrasepenalty owner start_arity end_arity weight
					int owner = psymbol.addTerminalSymbol(fds[1]);
					int start_arity = (new Integer(fds[2].trim())).intValue();
					int end_arity = (new Integer(fds[3].trim())).intValue();
					double weight = (new Double(fds[4].trim())).doubleValue();
					l_models.add(new ArityPhrasePenaltyFF(l_models.size(), weight, owner, start_arity, end_arity));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd ArityPhrasePenalty, owner: %s; start_arity: %d; end_arity: %d; weight: %.3f",line, owner, start_arity, end_arity, weight));			
				} else if (0 == fds[0].compareTo("wordpenalty")	&& fds.length == 2) { // wordpenalty weight
					double weight = (new Double(fds[1].trim())).doubleValue();
					l_models.add(new WordPenaltyFF(l_models.size(), weight));
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
		l_default_nonterminals.add(this.p_symbol.addNonTerminalSymbol(default_non_terminal));
	}
	
	
	private void initSymbolTbl(){
		if (JoshuaConfiguration.use_remote_lm_server) {
			this.p_symbol = new BuildinSymbol(JoshuaConfiguration.remote_symbol_tbl);//within decoder, we assume to use buildin table when remote lm is used
		}else  if (JoshuaConfiguration.use_srilm) {
			this.p_symbol = new SrilmSymbol(null, JoshuaConfiguration.g_lm_order);
			if (logger.isLoggable(Level.FINEST)) 
				logger.finest("Using SRILM symbol table");
			
		}else{//using the built-in JAVA implementatoin of LM
			this.p_symbol = new BuildinSymbol(null);
		}
	}
	
	
	private static LMGrammar initializeLanguageModel(Symbol psymbol) {
		LMGrammar lm_grammar;
		if (JoshuaConfiguration.use_remote_lm_server) {
			if (JoshuaConfiguration.use_left_euqivalent_state || JoshuaConfiguration.use_right_euqivalent_state){
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("use remote LM, we cannot use suffix/prefix stuff");
				System.exit(1);
			}
			lm_grammar = new LMGrammarRemote(psymbol, JoshuaConfiguration.g_lm_order, JoshuaConfiguration.f_remote_server_list, JoshuaConfiguration.num_remote_lm_servers);			
		} else if (JoshuaConfiguration.use_srilm) {
			if (JoshuaConfiguration.use_left_euqivalent_state || JoshuaConfiguration.use_right_euqivalent_state){
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("use SRILM, we cannot use suffix/prefix stuff");
				System.exit(1);
			}
			
			lm_grammar = new LMGrammarSRILM( (SrilmSymbol)psymbol, JoshuaConfiguration.g_lm_order, JoshuaConfiguration.lm_file);
			//lm_grammar = new LMGrammarSRILM((SrilmSymbol)psymbol, JoshuaConfiguration.g_lm_order, JoshuaConfiguration.lm_file);			
		} else {//using the built-in JAVA implementatoin of LM, may not be as scalable as SRILM
			lm_grammar = new LMGrammarJAVA( (BuildinSymbol)psymbol, JoshuaConfiguration.g_lm_order, JoshuaConfiguration.lm_file, JoshuaConfiguration.use_left_euqivalent_state, JoshuaConfiguration.use_right_euqivalent_state);
		}
		
		return lm_grammar;
	}	
	
	
	// This depends (invisibly) on the language model in order to do pruning of the TM at load time.
	private  void initializeTranslationGrammars(String tm_file) {	
		p_tm_grammars = new GrammarFactory[2];
		
		// Glue Grammar
		GrammarFactory glueGrammar = new MemoryBasedBatchGrammarWithPrune(p_symbol, null, true, p_l_feat_functions, JoshuaConfiguration.phrase_owner, -1, "^\\[[A-Z]+\\,[0-9]*\\]$", "[\\[\\]\\,0-9]+");	
		p_tm_grammars[0] = glueGrammar;		
		
		// Regular TM Grammar
		GrammarFactory regularGrammar = new MemoryBasedBatchGrammarWithPrune(p_symbol, tm_file, false, p_l_feat_functions, JoshuaConfiguration.phrase_owner, JoshuaConfiguration.span_limit, "^\\[[A-Z]+\\,[0-9]*\\]$", "[\\[\\]\\,0-9]+");				
		p_tm_grammars[1] = regularGrammar;
		
		//TODO if suffix-array: call SAGrammarFactory(SuffixArray sourceSuffixArray, CorpusArray targetCorpus, AlignmentArray alignments, LexicalProbabilities lexProbs, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int spanLimit) {
	}
	
	static public FeatureFunction haveLMFeature(ArrayList<FeatureFunction> l_models){
		for(FeatureFunction ff : l_models){
			if(ff instanceof LMFeatureFunction){
				return ff;
			}
		}
		return null;
	}
	
	public void writeConfigFile(double[] new_weights, String template, String file_to_write){
		BufferedReader t_reader_config = FileUtility.getReadFileStream(template);
		BufferedWriter t_writer_config =	FileUtility.getWriteFileStream(file_to_write);		
		String line;
		int feat_id = 0;
		while ((line = FileUtility.read_line_lzf(t_reader_config)) != null) {
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$") || line.indexOf("=") != -1) {//comment, empty line, or parameter lines: just copy
				 FileUtility.write_lzf(t_writer_config, line + "\n");
			}else{//models: replace the weight
				String[] fds = line.split("\\s+");
				StringBuffer new_line = new StringBuffer();
				if(fds[fds.length-1].matches("^[\\d\\.\\-\\+]+")==false){System.out.println("last field is not a number, must be wrong; the field is: " + fds[fds.length-1]); System.exit(1);};
				for(int i=0; i<fds.length-1; i++){
					new_line.append(fds[i]);
					new_line.append(" ");
				}
				new_line.append(new_weights[feat_id++]);	
				FileUtility.write_lzf(t_writer_config, new_line.toString() + "\n");
			}				
		}
		if(feat_id!=new_weights.length){System.out.println("number of models does not match number of weights, must be wrong"); System.exit(1);};
		FileUtility.close_read_file(t_reader_config);
		FileUtility.close_write_file(t_writer_config);		
	}
}
