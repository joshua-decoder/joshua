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

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.ArityPhrasePenaltyFF;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.OOVFF;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.SourcePathFF;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.decoder.ff.lm.NGramLanguageModel;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.decoder.ff.state_maintenance.NgramStateComputer;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.ui.hypergraph_visualizer.HyperGraphViewer;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * Implements decoder initialization,
 * including interaction with <code>JoshuaConfiguration</code>
 * and <code>DecoderThread</code>.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Lane Schwartz <dowobeha@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class JoshuaDecoder {
	/*
	 * Many of these objects themselves are global objects. We
	 * pass them in when constructing other objects, so that
	 * they all share pointers to the same object. This is good
	 * because it reduces overhead, but it can be problematic
	 * because of unseen dependencies (for example, in the
	 * Vocabulary shared by language model, translation grammar,
	 * etc).
	 */
	/** The DecoderFactory is the main thread of decoding */
	private DecoderFactory             decoderFactory;
	private List<GrammarFactory>       grammarFactories;
	private ArrayList<FeatureFunction> featureFunctions;
	private NGramLanguageModel         languageModel;
	
	private List<StateComputer>        stateComputers;
	
	private Map<String,Integer>        ruleStringToIDTable;
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(JoshuaDecoder.class.getName());
	
//===============================================================
// Constructors
//===============================================================

	/**
	 * Constructs a new decoder using the specified configuration
	 * file.
	 *
	 * @param configFile Name of configuration file.
	 */
	public JoshuaDecoder(String configFile) {
		this();
		this.initialize(configFile);
	}
	
	/** 
	 * Constructs an uninitialized decoder for use in testing.
	 * <p>
	 * This method is private because it should only ever be
	 * called by the {@link #getUninitalizedDecoder()} method
	 * to provide an uninitialized decoder for use in testing.
	 */
	private JoshuaDecoder() {
		this.grammarFactories = new ArrayList<GrammarFactory>();
	}
	
	/** 
	 * Gets an uninitialized decoder for use in testing.
	 * <p>
	 * This method is called by unit tests or any outside packages (e.g., MERT) 
	 * relying on the decoder.
	 */
	static public JoshuaDecoder getUninitalizedDecoder() {
		return new JoshuaDecoder();
	}
	
//===============================================================
// Public Methods
//===============================================================
	
	public void changeBaselineFeatureWeights(double[] weights){
		changeFeatureWeightVector(weights, null);
	} 
	
	public void changeDiscrminativeModelOnly(String discrminativeModelFile) {
		changeFeatureWeightVector(null, discrminativeModelFile);
	}
	
	/** 
	 * Sets the feature weight values used by the decoder.
	 * <p>
	 * This method assumes that the order of the provided weights
	 * is the same as their order in the decoder's configuration
	 * file.
	 * 
	 * @param weights Feature weight values
	 */
	public void changeFeatureWeightVector(double[] weights, String discrminativeModelFile) {
		if(weights!=null){
			if (this.featureFunctions.size() != weights.length) {
				throw new IllegalArgumentException("number of weights does not match number of feature functions");
			}
			
			int i = 0; 
			for (FeatureFunction ff : this.featureFunctions) {
				double oldWeight = ff.getWeight();
				ff.setWeight(weights[i]);
				logger.info("Feature function : " +	ff.getClass().getSimpleName() 
						+	"; weight changed from " + oldWeight + " to " + ff.getWeight());
				i++; 
			}
		}
		
		//FIXME: this works for Batch grammar only; not for sentence-specific grammars
		for (GrammarFactory grammarFactory : this.grammarFactories) {
//			if (grammarFactory instanceof Grammar) {
			grammarFactory.getGrammarForSentence(null)
				.sortGrammar(this.featureFunctions);
//			}
		}
	}
	
	
	/** 
	 * Decode a whole test set. This may be parallel.
	 *
	 * @param testFile
	 * @param nbestFile
	 * @param oracleFile
	 */
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
	
	public void visualizeHyperGraphForSentence(String sentence)
	{
		HyperGraphViewer.visualizeHypergraphInFrame(this.decoderFactory.getHyperGraphForSentence(sentence));
	}
	
	
	public static void writeConfigFile(double[] newWeights, String template, String outputFile, String newDiscriminativeModel) {
		try {
			int columnID = 0;
			
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
					StringBuffer newSent = new StringBuffer();
					if (! Regex.floatingNumber.matches(fds[fds.length-1])) {
						throw new IllegalArgumentException("last field is not a number; the field is: " + fds[fds.length-1]);
					}
					
					if(newDiscriminativeModel!=null && "discriminative".equals(fds[0])){
						newSent.append(fds[0]).append(' ');
						newSent.append(newDiscriminativeModel).append(' ');//change the file name
						for (int i = 2; i < fds.length-1; i++) {
							newSent.append(fds[i]).append(' ');
						}
					}else{//regular
						for (int i = 0; i < fds.length-1; i++) {
							newSent.append(fds[i]).append(' ');
						}
					}
					if(newWeights!=null)
						newSent.append(newWeights[columnID++]);//change the weight
					else
						newSent.append(fds[fds.length-1]);//do not change
					
					writer.write(newSent.toString());
					writer.newLine();
				}
			} } finally {
				reader.close();
				writer.close();
			}
			
			if (newWeights!=null && columnID != newWeights.length) {
				throw new IllegalArgumentException("number of models does not match number of weights");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
//===============================================================
// Initialization Methods
//===============================================================
	
	/** 
	 * Initialize all parts of the JoshuaDecoder.
	 *
	 * @param configFile File containing configuration options
	 * @return An initialized decoder
	 */
	public JoshuaDecoder initialize(String configFile) {
		try {

			if (JoshuaConfiguration.tm_file == null)
				throw new RuntimeException("No translation grammar or suffix array grammar was specified.");

			// Sets: languageModel
			if (JoshuaConfiguration.have_lm_model)
				initializeLanguageModel();


			// initialize and load grammar
			this.initializeGlueGrammar();					
			this.initializeMainTranslationGrammar();
					
			//					 Initialize the features: requires that
			// LM model has been initialized. If an LM
			// feature is used, need to read config file
			// again
			this.initializeFeatureFunctions(configFile);
					
			this.initializeStateComputers(JoshuaConfiguration.lm_order, JoshuaConfiguration.ngramStateID);

			// Sort the TM grammars (needed to do cube pruning)
			for (GrammarFactory grammarFactory : this.grammarFactories) {
				if (grammarFactory instanceof Grammar) {
					Grammar batchGrammar = (Grammar) grammarFactory;
					batchGrammar.sortGrammar(this.featureFunctions);
				}
			}
			
			this.decoderFactory = new DecoderFactory(
				this.grammarFactories,
				JoshuaConfiguration.use_max_lm_cost_for_oov,
				this.featureFunctions,
				this.stateComputers);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this;
	}
		
	// TODO: maybe move to JoshuaConfiguration to enable moving the featureFunction parsing there (Needs: Vocabulary; Sets: languageModel)
	// TODO: check we actually have a feature that requires a language model
	private void initializeLanguageModel() throws IOException {
		// BUG: All these different boolean LM fields should just be an enum.
		// FIXME: And we should check only once for the default (which supports left/right equivalent state) vs everything else (which doesn't)
		// TODO: maybe have a special exception type for BadConfigfileException instead of using IllegalArgumentException?
		
	if (JoshuaConfiguration.use_srilm || JoshuaConfiguration.use_kenlm) {
			if (JoshuaConfiguration.use_left_equivalent_state
			|| JoshuaConfiguration.use_right_equivalent_state) {
				throw new IllegalArgumentException("KenLM supports state.  Joshua should get around to using it.");
			}
			KenLM lm = new KenLM(JoshuaConfiguration.lm_file);
			this.languageModel = lm;
			Vocabulary.registerLanguageModel(lm);
			Vocabulary.id(JoshuaConfiguration.default_non_terminal);
	} else {

		logger.warning("WARNING: using built-in language model; you probably didn't intend this");
			
		this.languageModel = new LMGrammarJAVA(
				JoshuaConfiguration.lm_order,
				JoshuaConfiguration.lm_file,
				JoshuaConfiguration.use_left_equivalent_state,
				JoshuaConfiguration.use_right_equivalent_state);
	}
	}
	
	
	private void initializeGlueGrammar() throws IOException {
		logger.info("Constructing glue grammar...");
		
		MemoryBasedBatchGrammar gr = new MemoryBasedBatchGrammar(
				JoshuaConfiguration.glue_format,
				JoshuaConfiguration.glue_file,
				JoshuaConfiguration.glue_owner,
				JoshuaConfiguration.default_non_terminal,
				-1,
				JoshuaConfiguration.oov_feature_cost);
		
		this.grammarFactories.add(gr);
		
		if(JoshuaConfiguration.useRuleIDName){
			if(this.ruleStringToIDTable==null)
				this.ruleStringToIDTable = new HashMap<String,Integer>();
			gr.obtainRulesIDTable(this.ruleStringToIDTable);			
		}
		
	}
	
	
	private void initializeMainTranslationGrammar() throws IOException {

		if (! JoshuaConfiguration.use_sent_specific_tm) {
			if (logger.isLoggable(Level.INFO))
				logger.info("Using grammar read from file " + JoshuaConfiguration.tm_file);

			MemoryBasedBatchGrammar gr = new MemoryBasedBatchGrammar(
					JoshuaConfiguration.tm_format,
					JoshuaConfiguration.tm_file,
					JoshuaConfiguration.phrase_owner,
					JoshuaConfiguration.default_non_terminal,
					JoshuaConfiguration.span_limit,
					JoshuaConfiguration.oov_feature_cost);

			this.grammarFactories.add(gr);

			if(JoshuaConfiguration.useRuleIDName){
				if(this.ruleStringToIDTable==null)
					this.ruleStringToIDTable = new HashMap<String,Integer>();
				gr.obtainRulesIDTable(this.ruleStringToIDTable);			
			}
		} else {
			if (logger.isLoggable(Level.INFO))
				logger.info("Basing sentence-specific grammars on file " + JoshuaConfiguration.tm_file);
		}
	}
	
	private void initializeStateComputers(int nGramOrder, int ngramStateID){
		stateComputers = new ArrayList<StateComputer>();
		StateComputer ngramStateComputer = new NgramStateComputer(nGramOrder, ngramStateID);
		stateComputers.add(ngramStateComputer);
	}
	
	// BUG: why are we re-reading the configFile? JoshuaConfiguration should do this. (Needs: languageModel, Vocabulary, (logger?); Sets: featureFunctions)
	private void initializeFeatureFunctions(String configFile)
	throws IOException {
		this.featureFunctions = new ArrayList<FeatureFunction>();
		JoshuaConfiguration.num_phrasal_features = 0;	
		
		LineReader reader = new LineReader(configFile);
		try { for (String line : reader) {
			line = line.trim();
			if (Regex.commentOrEmptyLine.matches(line)) 
				continue;
			
			if (line.indexOf("=") == -1) { // ignore lines with "="
				String[] fds = Regex.spaces.split(line);
				
				if ("lm".equals(fds[0]) && fds.length == 2) { // lm weight
					if (null == this.languageModel) {
						throw new IllegalArgumentException("LM model has not been properly initialized before setting order and weight");
					}
					double weight = Double.parseDouble(fds[1].trim());
					this.featureFunctions.add(
						new LanguageModelFF(
							JoshuaConfiguration.ngramStateID,	
							this.featureFunctions.size(),
							JoshuaConfiguration.lm_order,
							this.languageModel, weight));
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format(
							"Line: %s\nAdd LM, order: %d; weight: %.3f;",
							line, JoshuaConfiguration.lm_order, weight));
                    
				} else if ("latticecost".equals(fds[0]) && fds.length == 2) {
					double weight = Double.parseDouble(fds[1].trim());
					this.featureFunctions.add(
						new SourcePathFF(
							this.featureFunctions.size(), weight));
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format(
							"Line: %s\nAdd Source lattice cost, weight: %.3f",
							line, weight));
					
				} else if ("phrasemodel".equals(fds[0]) && fds.length == 4) { // phrasemodel owner column(0-indexed) weight
					int    owner  = Vocabulary.id(fds[1]);
					int    column = Integer.parseInt(fds[2].trim());
					double weight = Double.parseDouble(fds[3].trim());
					this.featureFunctions.add(
						new PhraseModelFF(
							this.featureFunctions.size(),
							weight, owner, column));
					JoshuaConfiguration.num_phrasal_features += 1;
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format(
							"Process Line: %s\nAdd PhraseModel, owner: %s; column: %d; weight: %.3f",
							line, owner, column, weight));
					
				} else if ("arityphrasepenalty".equals(fds[0]) && fds.length == 5) { // arityphrasepenalty owner start_arity end_arity weight
					int owner      = Vocabulary.id(fds[1]);
					int startArity = Integer.parseInt(fds[2].trim());
					int endArity   = Integer.parseInt(fds[3].trim());
					double weight  = Double.parseDouble(fds[4].trim());
					this.featureFunctions.add(
						new ArityPhrasePenaltyFF(
							this.featureFunctions.size(),
							weight, owner, startArity, endArity));
					
					if (logger.isLoggable(Level.INFO))
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

				} else if ("oovpenalty".equals(fds[0]) && fds.length == 2) { // wordpenalty weight
					double weight = Double.parseDouble(fds[1].trim());
					int owner  = Vocabulary.id("pt");
					int column = JoshuaConfiguration.num_phrasal_features;

					this.featureFunctions.add(
						new OOVFF(
							this.featureFunctions.size(), weight, owner));

					JoshuaConfiguration.oov_feature_index = column;
					JoshuaConfiguration.num_phrasal_features += 1;

					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format(
							"Process Line: %s\nAdd OOVPenalty, weight: %.3f",
							line, weight));
					
					
				} else {
					throw new IllegalArgumentException("Wrong config line: " + line);
				}
			}
		} } finally {
			reader.close();
		}
	}
	
	
//===============================================================
// Main
//===============================================================
	public static void main(String[] args) throws IOException {

		String logFile = System.getenv().get("JOSHUA") + "/logging.properties";
		try {
			java.util.logging.LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
		} catch (IOException e) {
			logger.warning("Couldn't initialize logging properties from '" + logFile + "'");
		}

		long startTime = 0;
		if (logger.isLoggable(Level.INFO)) {
			startTime = System.currentTimeMillis();
		}
		
		if (args.length < 1) {
			System.out.println("Usage: java " +
							   JoshuaDecoder.class.getName() +
							   " -c configFile [other args]");
			System.exit(1);
		}

		String configFile  = null;
		String testFile    = "-";
		String nbestFile   = "-";
		String oracleFile  = null;

        // Step-0: Process the configuration file.  We accept two use
        // cases. (1) For backwards compatility, Joshua can be called
        // with as "Joshua configFile [testFile [outputFile
        // [oracleFile]]]".  (2) Command-line options can be used, in
        // which case we look for an argument to the "-config" flag.
        // We can distinguish these two cases by looking at the first
        // argument; if it starts with a hyphen, the new format has
        // been invoked.

        if (args[0].startsWith("-")) {

            // Search for the configuration file
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-c") || args[i].equals("-config")) {

                    configFile = args[i+1].trim();
                    JoshuaConfiguration.readConfigFile(configFile);
                    JoshuaConfiguration.processCommandLineOptions(args);

                    break;
                }
            }
        } else {

            configFile  = args[0].trim();

            JoshuaConfiguration.readConfigFile(configFile);

            if (args.length >= 2)
                testFile   = args[1].trim();
            if (args.length >= 3)
                nbestFile  = args[2].trim();
            if (args.length == 4)
                oracleFile = args[3].trim();
        }
		

		/* Step-0: some sanity checking */
		JoshuaConfiguration.sanityCheck();
		
		/* Step-1: initialize the decoder, test-set independent */
		JoshuaDecoder decoder = new JoshuaDecoder(configFile);

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
