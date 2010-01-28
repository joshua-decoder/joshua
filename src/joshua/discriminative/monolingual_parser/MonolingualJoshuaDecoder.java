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
package joshua.discriminative.monolingual_parser;



import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.ArityPhrasePenaltyFF;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.decoder.ff.SourceLatticeArcLogPFF;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.util.FileUtility;


import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class implements:
 * (1) mainly initialize, and control the interaction with JoshuaConfiguration and DecoderThread
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-03-09 23:49:09 -0400  $
 */
public class MonolingualJoshuaDecoder {
	private MonolingualDecoderFactory p_decoder_factory; // pointer to the main thread of decoding
	private GrammarFactory[] grammarFactories;
	private ArrayList<FeatureFunction> featureFunctions;
	private ArrayList<Integer> l_default_nonterminals;
	private SymbolTable p_symbolTable;
		
	boolean runEM =true;
	
	private static final Logger logger =
		Logger.getLogger(MonolingualJoshuaDecoder.class.getName());
	
	
//===============================================================
// Main
//===============================================================
	public static void main(String[] args) throws IOException {
		logger.finest("Starting decoder");
		
		long start = System.currentTimeMillis();
		if (args.length != 3 && args.length != 4) {
			System.out.println("Usage: java joshua.decoder.Decoder config_file test_file outfile (oracle_file)");
			System.out.println("num of args is "+ args.length);
			for (int i = 0; i < args.length; i++) {
				System.out.println("arg is: " + args[i]);
			}
			System.exit(1);
		}
		String config_file = args[0].trim();
		String test_file   = args[1].trim();
		String nbest_file  = args[2].trim();
	
		
		MonolingualJoshuaDecoder p_decoder = new MonolingualJoshuaDecoder();
		
		//============== Step-1: initialize the decoder ==============
		p_decoder.initialize(config_file);
		
		
		//============== statistics
		double t_sec = (System.currentTimeMillis() - start) / 1000;
		if (logger.isLoggable(Level.INFO)) 
			logger.info("before translation, loaddingtime is " + t_sec);
		
		//#============== Step-2: Decoding ==============
		p_decoder.decodingTestSet(test_file, nbest_file);
		
		//============== Step-3: clean up ==============
		p_decoder.cleanUp();
		
		t_sec = (System.currentTimeMillis() - start) / 1000;
		if (logger.isLoggable(Level.INFO)) 
			logger.info("Total running time is " + t_sec);
		
	}
// end main()
	
//===============================================================
	
	
	//##### procedures: read config, init lm, init sym tbl, init models, read lm, read tm
	public void initialize(String config_file) {
		try {
			//=== read config file
			JoshuaConfiguration.readConfigFile(config_file);
			
			//=== initialize symbol table
			initSymbolTbl();
			
			//TODO ##### add default non-terminals
			setDefaultNonTerminals(JoshuaConfiguration.default_non_terminal);
								
			//===  load TM grammar		
			if (JoshuaConfiguration.tm_file != null) {
				initializeTranslationGrammars(JoshuaConfiguration.tm_file);
			}  else {
				throw new RuntimeException("No translation grammargrammar was specified.");
			}
	
			//=== initialize the models(need to read config file again)
			featureFunctions = initializeFeatureFunctions(p_symbolTable, config_file);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void decodingTestSet(String test_file, String nbest_file) {
//		create factory
		if(runEM){//run EM training
			p_decoder_factory = new EMDecoderFactory(
					this.grammarFactories,
					false,//no LM model
					this.featureFunctions,
					this.l_default_nonterminals,
					this.p_symbolTable,
					nbest_file);//nbest_file *is* outGrammarFile
		}else{//regular decoding
			p_decoder_factory = new NbestDecoderFactory(
					this.grammarFactories,
					false,//no LM model
					this.featureFunctions,
					this.l_default_nonterminals,
					this.p_symbolTable, 
					nbest_file);
		}
		
//		 BUG: this works for Batch grammar only; not for sentence-specific grammars
		for (GrammarFactory grammarFactory : this.grammarFactories) {
			grammarFactory.getGrammarForSentence(null)
				.sortGrammar(this.featureFunctions);
		}
		p_decoder_factory.decodingTestSet(test_file);
	}
	
	
	public void cleanUp() {
		//TODO
		//p_lm_grammar.end_lm_grammar(); //end the threads
	}
	
	
	public static ArrayList<FeatureFunction> initializeFeatureFunctions(SymbolTable psymbolTable, String config_file)
	throws IOException {
		BufferedReader t_reader_config =
			FileUtility.getReadFileStream(config_file);
		ArrayList<FeatureFunction> l_models = new ArrayList<FeatureFunction>();
		
		String line;
		while ((line = FileUtility.read_line_lzf(t_reader_config)) != null) {
			line = line.trim();
			if (line.matches("^\\s*(?:\\#.*)?$")) {
				// ignore empty lines or lines commented out
				continue;
			}
			
			if (line.indexOf("=") == -1) { //ignore lines with "="
				String[] fds = line.split("\\s+");
				if (0 == fds[0].compareTo("latticecost") && fds.length == 2) {
					double weight = Double.parseDouble(fds[1].trim());
					l_models.add(new SourceLatticeArcLogPFF(l_models.size(), weight));
					if (logger.isLoggable(Level.FINEST)) logger.finest(
						String.format("Line: %s\nAdd Source lattice cost, weight: %.3f", weight));
				} else if (0 == fds[0].compareTo("phrasemodel") && fds.length == 4) { // phrasemodel owner column(0-indexed) weight
					int owner = psymbolTable.addTerminal(fds[1]);
					int column = (new Integer(fds[2].trim())).intValue();
					double weight = (new Double(fds[3].trim())).doubleValue();
					l_models.add(new PhraseModelFF(l_models.size(), weight, owner, column));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd PhraseModel, owner: %s; column: %d; weight: %.3f", line, owner, column, weight));
				} else if (0 == fds[0].compareTo("arityphrasepenalty") && fds.length == 5){//arityphrasepenalty owner start_arity end_arity weight
					int owner = psymbolTable.addTerminal(fds[1]);
					int start_arity = (new Integer(fds[2].trim())).intValue();
					int end_arity = (new Integer(fds[3].trim())).intValue();
					double weight = (new Double(fds[4].trim())).doubleValue();
					l_models.add(new ArityPhrasePenaltyFF(l_models.size(), weight, owner, start_arity, end_arity));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("Process Line: %s\nAdd ArityPhrasePenalty, owner: %s; start_arity: %d; end_arity: %d; weight: %.3f",line, owner, start_arity, end_arity, weight));
				} else if (0 == fds[0].compareTo("wordpenalty") && fds.length == 2) { // wordpenalty weight
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
		t_reader_config.close();
		return l_models;
	}
	
	
	private void setDefaultNonTerminals(String default_non_terminal) {
		//TODO ##### add default non-terminals
		l_default_nonterminals = new ArrayList<Integer>();
		l_default_nonterminals.add(this.p_symbolTable.addNonterminal(default_non_terminal));
	}
	
	
	private void initSymbolTbl() throws IOException {
		this.p_symbolTable = new BuildinSymbol(null);		
	}
	
	
	
	
	// This depends (invisibly) on the language model in order to do pruning of the TM at load time.
	private void initializeTranslationGrammars(String tm_file)
	throws IOException {
		grammarFactories = new GrammarFactory[2];
		
		// Glue Grammar
		GrammarFactory glueGrammar =
			//new MemoryBasedBatchGrammarWithPrune(
			new MonolingualGrammar(
				"monolingual",
				p_symbolTable, 
				JoshuaConfiguration.glue_file,
				JoshuaConfiguration.phrase_owner,
				JoshuaConfiguration.default_non_terminal,
				JoshuaConfiguration.goal_symbol,
				-1,
				runEM);
		
		grammarFactories[0] = glueGrammar;
		
		// Regular TM Grammar
		GrammarFactory regularGrammar =
			//new MemoryBasedBatchGrammarWithPrune(
			new MonolingualGrammar(
				"monolingual",
				p_symbolTable, tm_file, 
				JoshuaConfiguration.phrase_owner,
				JoshuaConfiguration.default_non_terminal,
				JoshuaConfiguration.goal_symbol,
				JoshuaConfiguration.span_limit,
				runEM);
		
		grammarFactories[1] = regularGrammar;
		
		//TODO if suffix-array: call SAGrammarFactory(SuffixArray sourceSuffixArray, CorpusArray targetCorpus, AlignmentArray alignments, LexicalProbabilities lexProbs, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int spanLimit) {
	}
	
	
	
	
	
	public void writeConfigFile(double[] new_weights, String template, String file_to_write) {
		try {
			BufferedReader t_reader_config = 
				FileUtility.getReadFileStream(template);
			BufferedWriter t_writer_config = 
				FileUtility.getWriteFileStream(file_to_write);
			String line;
			int feat_id = 0;
			while ((line = FileUtility.read_line_lzf(t_reader_config)) != null) {
				line = line.trim();
				if (line.matches("^\\s*(?:\\#.*)?$")
				|| line.indexOf("=") != -1) {
					//comment, empty line, or parameter lines: just copy
					t_writer_config.write(line + "\n");
					
				} else { //models: replace the weight
					String[] fds = line.split("\\s+");
					StringBuffer new_line = new StringBuffer();
					if (! fds[fds.length-1].matches("^[\\d\\.\\-\\+]+")) {
						System.out.println("last field is not a number, must be wrong; the field is: " + fds[fds.length-1]); System.exit(1);
					}
					for (int i = 0; i < fds.length-1; i++) {
						new_line.append(fds[i]);
						new_line.append(" ");
					}
					new_line.append(new_weights[feat_id++]);
					t_writer_config.write(new_line.toString() + "\n");
				}
			}
			if (feat_id != new_weights.length) {
				System.out.println("number of models does not match number of weights, must be wrong");
				System.exit(1);
			}
			t_reader_config.close();
			t_writer_config.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
