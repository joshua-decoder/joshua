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
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.decoder.ff.lm.bloomfilter_lm.BloomFilterLanguageModel;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;
import joshua.decoder.ff.lm.distributed_lm.LMGrammarRemote;
import joshua.decoder.ff.lm.srilm.LMGrammarSRILM;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.HieroGrammar.MemoryBasedBatchGrammar;
import joshua.sarray.Corpus;
import joshua.sarray.MemoryMappedCorpusArray;
import joshua.sarray.MemoryMappedSuffixArray;
import joshua.sarray.SAGrammarFactory;
import joshua.sarray.SampledLexProbs;
import joshua.sarray.Suffixes;
import joshua.corpus.SymbolTable;
import joshua.util.io.BinaryIn;
import joshua.util.io.LineReader;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.lexprob.LexicalProbabilities;
import joshua.util.sentence.Vocabulary;
import joshua.util.sentence.alignment.Alignments;
import joshua.util.sentence.alignment.MemoryMappedAlignmentGrids;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * this class implements:
 * (1) mainly initialize, and control the interaction with
 * JoshuaConfiguration and DecoderThread
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */

public class JoshuaDecoder {
	/*
	 * Many of these objects themselves are global objects. We
	 * pass them in when constructing other objects, so that
	 * they all share pointers to the same object. This is good
	 * because it reduces overhead, but it can be problematic
	 * because of unseen dependencies (for example, in the
	 * SymbolTable shared by language model, translation grammar,
	 * etc).
	 */
	// The DecoderFactory is the main thread of decoding
	private DecoderFactory             decoderFactory;
	private GrammarFactory[]           grammarFactories;
	private ArrayList<FeatureFunction> featureFunctions;
	private ArrayList<Integer>         defaultNonterminals;
	private NGramLanguageModel         languageModel;
	
	/**
	 * Shared symbol table for source language terminals,
	 * target language terminals, and shared nonterminals.
	 * <p>
	 * It may be that separate tables should be maintained
	 * for the source and target languages.
	 */
	private SymbolTable                symbolTable;
	
	private static final Logger logger =
		Logger.getLogger(JoshuaDecoder.class.getName());
	
	
//===============================================================
// Public Methods
//===============================================================
	
	/* this assumes that the weights are ordered according to the decoder's config file */
	public void changeFeatureWeightVector(double[] weights) {
		if (this.featureFunctions.size() != weights.length) {
			logger.severe("JoshuaDecoder.changeFeatureWeightVector: number of weights does not match number of feature functions");
			System.exit(0);
		}
		{ int i = 0; for (FeatureFunction ff : this.featureFunctions) {
			double oldWeight = ff.getWeight();
			ff.setWeight(weights[i]);
			System.out.println("Feature function : " +
				ff.getClass().getSimpleName() +
				"; weight changed from " + oldWeight + " to " + ff.getWeight());
		i++; }}
		
		// BUG: this works for Batch grammar only; not for sentence-specific grammars
		for (GrammarFactory grammarFactory : this.grammarFactories) {
			grammarFactory.getGrammarForSentence(null)
				.sortGrammar(this.featureFunctions);
		}
	}
	
	
	/** Decode a whole test set. This may be parallel. */
	public void decodeTestSet(String testFile, String nbestFile, String oracleFile) throws IOException {
		this.decoderFactory.decodeTestSet(testFile, nbestFile, oracleFile);
	}
	
	public void decodeTestSet(String testFile, String nbestFile) {
		this.decoderFactory.decodeTestSet(testFile, nbestFile, null);
	}
	
	
	/** Decode a sentence. This must be non-parallel. */
	public void decodeSentence(String testSentence, String[] nbests) {
		//TODO
	}
	
	
	public void cleanUp() {
		//TODO
		//this.languageModel.end_lm_grammar(); //end the threads
	}
	
	
	public void writeConfigFile(double[] newWeights, String template, String outputFile) {
		try {
			int featureID = 0;
			
			BufferedWriter writer = FileUtility.getWriteFileStream(outputFile);
			LineReader     reader = new LineReader(template);
			try { for (String line : reader) {
				line = line.trim();
				if (Regex.commentOrEmptyLine.matches(line)
				|| line.indexOf("=") != -1) {
					//comment, empty line, or parameter lines: just copy
					writer.write(line);
					writer.newLine();
					
				} else { //models: replace the weight
					String[] fds = Regex.spaces.split(line);
					StringBuffer newLine = new StringBuffer();
					if (! Regex.floatingNumber.matches(fds[fds.length-1])) {
						logger.severe("last field is not a number; the field is: " + fds[fds.length-1]);
						System.exit(1);
					}
					for (int i = 0; i < fds.length-1; i++) {
						newLine.append(fds[i]).append(" ");
					}
					newLine.append(newWeights[featureID++]);
					writer.write(newLine.toString());
					writer.newLine();
				}
			} } finally {
				reader.close();
				writer.close();
			}
			
			if (featureID != newWeights.length) {
				logger.severe("number of models does not match number of weights");
				System.exit(1);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
//===============================================================
// Initialization Methods
//===============================================================
	
	/** Initialize all parts of the JoshuaDecoder. */
	public JoshuaDecoder initialize(String configFile) {
		try {
			JoshuaConfiguration.readConfigFile(configFile);
			
			// Sets: symbolTable, defaultNonterminals
			this.initializeSymbolTable();
			
			// Needs: symbolTable; Sets: languageModel
			if (JoshuaConfiguration.have_lm_model) initializeLanguageModel();
			
			// initialize and load grammar
			if (! JoshuaConfiguration.use_sent_specific_tm) {
				if (null != JoshuaConfiguration.tm_file) {
					
					initializeTranslationGrammars(JoshuaConfiguration.tm_file);
					
				} else if (null != JoshuaConfiguration.sa_source
					&& null != JoshuaConfiguration.sa_target
					&& null != JoshuaConfiguration.sa_alignment) {
					
					try {
						initializeSuffixArrayGrammar();
					} catch (Exception e) {
						logger.severe("Error reading suffix array grammar - exiting decoder.");
						e.printStackTrace();
						System.exit(-1);
					}
					
				} else {
					throw new RuntimeException("No translation grammar or suffix array grammar was specified.");
				}
			}
			
			
			// Initialize the features: requires that
			// LM model has been initialied. If an LM
			// feature is used, need to read config file
			// again
			this.initializeFeatureFunctions(configFile);
			
			
			// Sort the TM grammars (needed to do cube pruning)
			// BUG: this works for Batch grammar only; not for sentence-specific grammars
			for (GrammarFactory grammarFactory : this.grammarFactories) {
				grammarFactory.getGrammarForSentence(null)
					.sortGrammar(this.featureFunctions);
			}
			
			
			this.decoderFactory = new DecoderFactory(
				this.grammarFactories,
				JoshuaConfiguration.have_lm_model,
				this.featureFunctions,
				this.defaultNonterminals,
				this.symbolTable);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this;
	}
	
	// TODO: maybe move to JoshuaConfiguration to enable moving the featureFunction parsing there (Sets: symbolTable, defaultNonterminals)
	private void initializeSymbolTable() throws IOException {
		if (JoshuaConfiguration.use_remote_lm_server) {
			// Within the decoder, we assume BuildinSymbol when using the remote LM
			this.symbolTable =
				new BuildinSymbol(JoshuaConfiguration.remote_symbol_tbl);
			
		} else if (JoshuaConfiguration.use_srilm) {
			this.symbolTable =
				new SrilmSymbol(null, JoshuaConfiguration.g_lm_order);
			logger.finest("Using SRILM symbol table");
			
		} else {
			this.symbolTable = new BuildinSymbol(null);
		}
		
		// Add the default nonterminal
		this.defaultNonterminals = new ArrayList<Integer>();
		this.defaultNonterminals.add(
			this.symbolTable.addNonterminal(
				JoshuaConfiguration.default_non_terminal));
	}
	
	
	// TODO: maybe move to JoshuaConfiguration to enable moving the featureFunction parsing there (Needs: symbolTable; Sets: languageModel)
	//TODO: check we actually have a feature that requires a langage model
	private void initializeLanguageModel() throws IOException {
		if (JoshuaConfiguration.use_remote_lm_server) {
			if (JoshuaConfiguration.use_left_equivalent_state
			|| JoshuaConfiguration.use_right_equivalent_state) {
				logger.severe("use remote LM, we cannot use suffix/prefix stuff");
				System.exit(1);
			}
			this.languageModel = new LMGrammarRemote(
				this.symbolTable,
				JoshuaConfiguration.g_lm_order,
				JoshuaConfiguration.f_remote_server_list,
				JoshuaConfiguration.num_remote_lm_servers);
			
		} else if (JoshuaConfiguration.use_srilm) {
			if (JoshuaConfiguration.use_left_equivalent_state
			|| JoshuaConfiguration.use_right_equivalent_state) {
				logger.severe("use SRILM, we cannot use suffix/prefix stuff");
				System.exit(1);
			}
			this.languageModel = new LMGrammarSRILM(
				(SrilmSymbol)this.symbolTable,
				JoshuaConfiguration.g_lm_order,
				JoshuaConfiguration.lm_file);
			
		} else if (JoshuaConfiguration.use_bloomfilter_lm) {
			if (JoshuaConfiguration.use_left_equivalent_state
			|| JoshuaConfiguration.use_right_equivalent_state) {
				logger.severe("use Bloomfilter LM, we cannot use suffix/prefix stuff");
				System.exit(1);
			}
			this.languageModel = new BloomFilterLanguageModel(
					this.symbolTable,
					JoshuaConfiguration.g_lm_order,
					JoshuaConfiguration.lm_file);
		} else {
			//using the built-in JAVA implementatoin of LM, may not be as scalable as SRILM
			this.languageModel = new LMGrammarJAVA(
				(BuildinSymbol)this.symbolTable,
				JoshuaConfiguration.g_lm_order,
				JoshuaConfiguration.lm_file,
				JoshuaConfiguration.use_left_equivalent_state,
				JoshuaConfiguration.use_right_equivalent_state);
		}
	}
	
	
	// TODO: these Patterns should probably be extracted out and compiled only once (either by us or by MemoryBasedBatchGrammar)
	private void initializeGlueGrammar() throws IOException {
		this.grammarFactories = new GrammarFactory[2];
		
		logger.info("Constructing glue grammar...");
		this.grammarFactories[0] =
			// if this is used, then it depends on the LMModel to do pruning
			//new MemoryBasedBatchGrammarWithPrune(
			new MemoryBasedBatchGrammar(
				this.symbolTable, null, true,
				JoshuaConfiguration.phrase_owner,
				-1,
				"^\\[[A-Z]+\\,[0-9]*\\]$",
				"[\\[\\]\\,0-9]+");
	}
	
	
	// TODO: these Patterns should probably be extracted out and compiled only once (either by us or by MemoryBasedBatchGrammar)
	private void initializeTranslationGrammars(String tmFile)
	throws IOException {
		initializeGlueGrammar();
		
		if (logger.isLoggable(Level.INFO))
			logger.info("Using grammar read from file " + tmFile);
		this.grammarFactories[1] =
			//new MemoryBasedBatchGrammarWithPrune(
			new MemoryBasedBatchGrammar(
				this.symbolTable, tmFile, false,
				JoshuaConfiguration.phrase_owner,
				JoshuaConfiguration.span_limit,
				"^\\[[A-Z]+\\,[0-9]*\\]$",
				"[\\[\\]\\,0-9]+");
		
		//TODO if suffix-array: call SAGrammarFactory(SuffixArray sourceSuffixArray, CorpusArray targetCorpus, AlignmentArray alignments, LexicalProbabilities lexProbs, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int spanLimit) {
	}
	
	
	private void initializeSuffixArrayGrammar() throws IOException, ClassNotFoundException {
		initializeGlueGrammar();
		
		
		int maxCacheSize = JoshuaConfiguration.sa_rule_cache_size;
		
		String binarySourceVocabFileName = 
			JoshuaConfiguration.sa_source + "." + JoshuaConfiguration.sa_vocab_suffix;
		
		String binarySourceCorpusFileName =
			JoshuaConfiguration.sa_source + "." + JoshuaConfiguration.sa_corpus_suffix;
		
		String binarySourceSuffixesFileName =
			JoshuaConfiguration.sa_target + "." + JoshuaConfiguration.sa_suffixes_suffix;
		
		
		String binaryTargetVocabFileName = 
			JoshuaConfiguration.sa_target + "." + JoshuaConfiguration.sa_vocab_suffix;
		
		String binaryTargetCorpusFileName =
			JoshuaConfiguration.sa_target + "." + JoshuaConfiguration.sa_corpus_suffix;
		
		String binaryTargetSuffixesFileName =
			JoshuaConfiguration.sa_target + "." + JoshuaConfiguration.sa_suffixes_suffix;
		
		
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading source language vocabulary from " + 
					binarySourceVocabFileName);
		Vocabulary sourceVocab = new Vocabulary(); {
			ObjectInput in = BinaryIn.vocabulary(binarySourceVocabFileName);
			sourceVocab.readExternal(in);
		}
		
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading source language corpus from " + 
					binarySourceCorpusFileName);
		Corpus sourceCorpusArray = 
			new MemoryMappedCorpusArray(sourceVocab, binarySourceCorpusFileName);
		
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading source language suffix array from " + 
					binarySourceSuffixesFileName);
		Suffixes sourceSuffixArray = 
			new MemoryMappedSuffixArray(
					binarySourceSuffixesFileName, 
					sourceCorpusArray, 
					maxCacheSize);
		
		
		
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading target language vocabulary from " + 
					binarySourceVocabFileName);
		Vocabulary targetVocab = new Vocabulary(); {
			ObjectInput in = BinaryIn.vocabulary(binaryTargetVocabFileName);
			sourceVocab.readExternal(in);
		}
		
		
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading target language corpus from " + 
					binaryTargetCorpusFileName);
		Corpus targetCorpusArray = 
			new MemoryMappedCorpusArray(targetVocab, binaryTargetCorpusFileName);

		
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading target language suffix array from " + 
					binaryTargetSuffixesFileName);
		Suffixes targetSuffixArray = 
			new MemoryMappedSuffixArray(
					binaryTargetSuffixesFileName, 
					targetCorpusArray, 
					maxCacheSize);
		
		
		String binaryAlignmentFileName = JoshuaConfiguration.sa_alignment;
		if (logger.isLoggable(Level.INFO)) 
			logger.info(
					"Reading alignment grid data from " + 
					binaryAlignmentFileName);
		Alignments alignments = 
			new MemoryMappedAlignmentGrids(
					binaryAlignmentFileName, 
					sourceCorpusArray, 
					targetCorpusArray);

		
		LexicalProbabilities lexProbs = new SampledLexProbs(
			JoshuaConfiguration.sa_lex_sample_size,
			sourceSuffixArray,
			targetSuffixArray,
			alignments,
			JoshuaConfiguration.sa_lex_cache_size,
			JoshuaConfiguration.sa_precalculate_lexprobs);
		
		// Finally, add the Suffix Array Grammar
		this.grammarFactories[1] = new SAGrammarFactory(
			sourceSuffixArray,
			targetCorpusArray,
			alignments,
			lexProbs,
			JoshuaConfiguration.sa_rule_sample_size,
			JoshuaConfiguration.sa_max_phrase_span,
			JoshuaConfiguration.sa_max_phrase_length,
			JoshuaConfiguration.sa_max_nonterminals,
			JoshuaConfiguration.sa_min_nonterminal_span);
	}
	
	
	// BUG: why are we re-reading the configFile? JoshuaConfiguration should do this. (Needs: languageModel, symbolTable, (logger?); Sets: featureFunctions)
	private void initializeFeatureFunctions(String configFile)
	throws IOException {
		this.featureFunctions = new ArrayList<FeatureFunction>();
		
		LineReader reader = new LineReader(configFile);
		try { for (String line : reader) {
			line = line.trim();
			if (Regex.commentOrEmptyLine.matches(line)) continue;
			
			if (line.indexOf("=") == -1) { //ignore lines with "="
				String[] fds = Regex.spaces.split(line);
				
				if ("lm".equals(fds[0]) && fds.length == 2) { // lm order weight
					if (null == this.languageModel) {
						logger.severe("LM model has not been properly initialized before setting order and weight");
						System.exit(1);
					}
					double weight = Double.parseDouble(fds[1].trim());
					this.featureFunctions.add(
						new LanguageModelFF(
							this.featureFunctions.size(),
							JoshuaConfiguration.g_lm_order,
							this.symbolTable, this.languageModel, weight));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format(
							"Line: %s\nAdd LM, order: %d; weight: %.3f;",
							line, JoshuaConfiguration.g_lm_order, weight));
					
				} else if ("latticecost".equals(fds[0]) && fds.length == 2) {
					double weight = Double.parseDouble(fds[1].trim());
					this.featureFunctions.add(
						new SourceLatticeArcCostFF(
							this.featureFunctions.size(), weight));
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format(
							"Line: %s\nAdd Source lattice cost, weight: %.3f",
							weight));
					
				} else if ("phrasemodel".equals(fds[0]) && fds.length == 4) { // phrasemodel owner column(0-indexed) weight
					int    owner  = this.symbolTable.addTerminal(fds[1]);
					int    column = Integer.parseInt(fds[2].trim());
					double weight = Double.parseDouble(fds[3].trim());
					this.featureFunctions.add(
						new PhraseModelFF(
							this.featureFunctions.size(),
							weight, owner, column));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format(
							"Process Line: %s\nAdd PhraseModel, owner: %s; column: %d; weight: %.3f",
							line, owner, column, weight));
					
				} else if ("arityphrasepenalty".equals(fds[0]) && fds.length == 5){//arityphrasepenalty owner start_arity end_arity weight
					int owner      = this.symbolTable.addTerminal(fds[1]);
					int startArity = Integer.parseInt(fds[2].trim());
					int endArity   = Integer.parseInt(fds[3].trim());
					double weight  = Double.parseDouble(fds[4].trim());
					this.featureFunctions.add(
						new ArityPhrasePenaltyFF(
							this.featureFunctions.size(),
							weight, owner, startArity, endArity));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format(
							"Process Line: %s\nAdd ArityPhrasePenalty, owner: %s; startArity: %d; endArity: %d; weight: %.3f",
							line, owner, startArity, endArity, weight));
					
				} else if ("wordpenalty".equals(fds[0]) && fds.length == 2) { // wordpenalty weight
					double weight = Double.parseDouble(fds[1].trim());
					this.featureFunctions.add(
						new WordPenaltyFF(
							this.featureFunctions.size(), weight));
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format(
							"Process Line: %s\nAdd WordPenalty, weight: %.3f",
							line, weight));
					
				} else {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
			}
		} } finally { reader.close(); }
	}
	
	
//===============================================================
// Main
//===============================================================
	public static void main(String[] args) throws IOException {
		logger.finest("Starting decoder");
		
		long startTime = 0;
		if (logger.isLoggable(Level.INFO)) {
			startTime = System.currentTimeMillis();
		}
		
		if (args.length != 3 && args.length != 4) {
			System.out.println("Usage: java " +
				JoshuaDecoder.class.getName() +
				" configFile testFile outputFile (oracleFile)");
			
			System.out.println("num of args is " + args.length);
			for (int i = 0; i < args.length; i++) {
				System.out.println("arg is: " + args[i]);
			}
			System.exit(1);
		}
		String configFile = args[0].trim();
		String testFile   = args[1].trim();
		String nbestFile  = args[2].trim();
		String oracleFile = (4 == args.length ? args[3].trim() : null);
		
		
		/* Step-1: initialize the decoder */
		JoshuaDecoder decoder = new JoshuaDecoder().initialize(configFile);
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Before translation, loading time is "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		}
		
		
		/* Step-2: Decoding */
		decoder.decodeTestSet(testFile, nbestFile, oracleFile);
		
		
		/* Step-3: clean up */
		decoder.cleanUp();
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Total running time is "
				+ ((double)(System.currentTimeMillis() - startTime) / 1000.0)
				+ " seconds");
		}
	}
}
