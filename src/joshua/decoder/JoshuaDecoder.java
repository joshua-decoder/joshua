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
import java.util.List;
import java.util.Map;
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
import joshua.decoder.ff.lm.berkeley_lm.LMGrammarBerkeley;
import joshua.decoder.ff.lm.buildin_lm.LMGrammarJAVA;
import joshua.decoder.ff.lm.kenlm.jni.KenLM;
import joshua.decoder.ff.state_maintenance.NgramStateComputer;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.packed.PackedGrammar;
import joshua.ui.hypergraph_visualizer.HyperGraphViewer;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * Implements decoder initialization,
 * including interaction with <code>JoshuaConfiguration</code>
 * and <code>DecoderThread</code>.
 *
 * @author Matt Post
 * @author Juri Ganitkevitch
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Wren Thornton <wren@users.sourceforge.net>
 * @author Lane Schwartz <dowobeha@users.sourceforge.net>
 */
public class JoshuaDecoder {
	/*
	 * Many of these objects themselves are global objects. We
	 * pass them in when constructing other objects, so that
	 * they all share pointers to the same object. This is good
	 * because it reduces overhead, but it can be problematic
	 * because of unseen dependencies.
	 */
	/** The DecoderFactory is the main thread of decoding */
	private DecoderFactory                decoderFactory;
	private List<GrammarFactory>          grammarFactories;
	private List<NGramLanguageModel>      languageModels;
	
	private List<StateComputer>        stateComputers;
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(JoshuaDecoder.class.getName());
	
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
	
	
	/**
	 * Sets the feature weight values used by the decoder.
	 * <p>
	 * This method assumes that the order of the provided weights is the same as
	 * their order in the decoder's configuration file.
	 * 
	 * @param weights
	 *          Feature weight values
	 */
	public void updateWeights(Map<String, Float> weights) {
		Features.updateWeights(weights);

		for (GrammarFactory grammarFactory : this.grammarFactories)
			grammarFactory.getGrammarForSentence(null).sortGrammar();
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
			if (JoshuaConfiguration.tms.isEmpty())
				throw new RuntimeException("No translation grammars were specified.");

			// Initialize and load grammars.
			this.initializeGrammars();
			
			// Initialize the LMs.
      initializeLanguageModels();
					
			// Initialize the features: requires that LM model has
			// been initialized.
			initializeFeatureFunctions();
					
			// Initialize features that contribute to state (currently only n-grams)
			initializeStateComputers();

			// Sort the TM grammars (needed to do cube pruning)
			for (GrammarFactory grammarFactory : this.grammarFactories) {
				if (grammarFactory instanceof Grammar) {
					Grammar batchGrammar = (Grammar) grammarFactory;
					batchGrammar.sortGrammar();
				}
			}
			
			this.decoderFactory = new DecoderFactory(
				this.grammarFactories,
				JoshuaConfiguration.use_max_lm_cost_for_oov,
				this.stateComputers);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this;
	}
		
	private void initializeLanguageModels() throws IOException {

		this.languageModels = new ArrayList<NGramLanguageModel>();

		// lm = kenlm 5 0 0 100 file
		for (String lm_line: JoshuaConfiguration.lms) {

			String  tokens[]          = lm_line.split("\\s+");
			String  lm_type           = tokens[0];
			int     lm_order          = Integer.parseInt(tokens[1]);
			boolean left_equiv_state  = Boolean.parseBoolean(tokens[2]);
			boolean right_equiv_state = Boolean.parseBoolean(tokens[3]);
			double  lm_ceiling_cost   = Double.parseDouble(tokens[4]);
			String  lm_file           = tokens[5];

			if (lm_type.equals("kenlm")) {
				if (left_equiv_state || right_equiv_state) {
					throw new IllegalArgumentException("KenLM supports state. Joshua should get around to using it.");
				}

				KenLM lm = new KenLM(lm_order, lm_file);
				this.languageModels.add(lm);
				Vocabulary.registerLanguageModel(lm);
				Vocabulary.id(JoshuaConfiguration.default_non_terminal);

			} else if (lm_type.equals("berkeleylm")) {
				LMGrammarBerkeley lm = new LMGrammarBerkeley(lm_order, lm_file);
				this.languageModels.add(lm);
				Vocabulary.registerLanguageModel(lm);
				Vocabulary.id(JoshuaConfiguration.default_non_terminal);

			} else if (lm_type.equals("none")) {
				; // do nothing

			} else {
				logger.warning("WARNING: using built-in language model; you probably didn't intend this");
				logger.warning("  Valid lm types are 'kenlm', 'berkeleylm', 'javalm' and 'none'");

				this.languageModels.add(new LMGrammarJAVA(lm_order, lm_file, left_equiv_state, right_equiv_state));
			}
		}
	}

	/**
	 * Initialize grammars from configuration. Line format is as follows:
	 * tm = format sent_specific span_limit grammar_file
	 * @throws IOException
	 */
	private void initializeGrammars() throws IOException {
		logger.info("Constructing glue grammar...");
		
		// First round, find packed grammar and load it.
		boolean has_packed = false;
		for (String tm_line : JoshuaConfiguration.tms) {
			String tokens[] = tm_line.split("\\s+");
			String tm_type            = tokens[0];
			int span_limit            = Integer.parseInt(tokens[2]);
			String tm_file            = tokens[3];

			if ("packed".equals(tm_type)) {
				if (!has_packed) {
					logger.info("Loading grammar: " + tm_file);
					this.grammarFactories.add(new PackedGrammar(tm_file, span_limit));
					has_packed = true;
				} else {
					logger.severe("Can only handle one packed grammar at a time. " +
							"Ignoring grammar: " + tm_file);
				}
			}
		}
		// Second round, load unpacked grammars.
		for (String tm_line : JoshuaConfiguration.tms) {
			String tokens[] = tm_line.split("\\s+");
			String tm_type            = tokens[0];
			boolean sentence_specific = Boolean.parseBoolean(tokens[1]);
			int span_limit            = Integer.parseInt(tokens[2]);
			String tm_file            = tokens[3];

			if (!"packed".equals(tm_type)) {
				logger.info("Loading grammar: " + tm_file);
				this.grammarFactories.add(new MemoryBasedBatchGrammar(
						tm_type, tm_file, JoshuaConfiguration.default_non_terminal,
						span_limit, JoshuaConfiguration.oov_feature_cost));
			}
		}
		logger.info("Basing sentence-specific grammars on file " +
				JoshuaConfiguration.tm_file);
	}
	
	private void initializeStateComputers() {
		stateComputers = new ArrayList<StateComputer>();
		StateComputer ngramStateComputer = new NgramStateComputer(
				JoshuaConfiguration.lm_order, JoshuaConfiguration.ngramStateID);
		stateComputers.add(ngramStateComputer);
	}
	
	// Iterate over the features that were discovered when the config file was
	// read.
	private void initializeFeatureFunctions() {
		Features.initialize();

		int num_phrasal_features = 0;
		
		for (String featureLine: JoshuaConfiguration.features) {

			String fields[] = featureLine.split("\\s+");
			String feature = fields[0];

			// initialize the language model
			if (feature.equals("lm") && ! JoshuaConfiguration.lm_type.equals("none")) {
				int index;
				float weight;

				// new format
				if (fields.length == 3) {
					index = Integer.parseInt(fields[1]);
					weight = Float.parseFloat(fields[2]);
				} else {
					index = 0;
					weight = Float.parseFloat(fields[1]);
				}

				if (index >= this.languageModels.size()) {
					logger.severe(String.format("* FATAL: there is no LM " +
							"corresponding to LM feature index %d", index));
					System.exit(1);
				}

				Features.add(
						new LanguageModelFF(JoshuaConfiguration.ngramStateID,	
								this.languageModels.get(index).getOrder(),
								this.languageModels.get(index)),
						"", weight);

				logger.info(String.format("FEATURE: language model #%d, order %d (weight %.3f)", (index+1), languageModels.get(index).getOrder(), weight));
			}

			else if (feature.equals("latticecost")) {
				float weight = Float.parseFloat(fields[1]);
				Features.add(new SourcePathFF(), "latticecost", weight);
				logger.info(String.format("FEATURE: lattice cost (weight %.3f)", weight));
			}

			else if (feature.equals("phrasemodel")) {
				int    owner  = Vocabulary.id(fields[1]);
				int    column = Integer.parseInt(fields[2].trim());
				double weight = Double.parseDouble(fields[3].trim());

				Features.add( 
						new PhraseModelFF(
								weight, column));
				num_phrasal_features += 1;

				logger.info(String.format("FEATURE: phrase model %d (weight %.3f)", column, weight));
			}

			else if (feature.equals("arityphrasepenalty")) {
				int owner      = Vocabulary.id(fields[1]);
				int startArity = Integer.parseInt(fields[2].trim());
				int endArity   = Integer.parseInt(fields[3].trim());
				double weight  = Double.parseDouble(fields[4].trim());
				Features.add(new ArityPhrasePenaltyFF(startArity, endArity));
				logger.info(String.format("FEATURE: arity phrase penalty: start %d, end %d (weight %.3f)", startArity, endArity, weight));
			}

			else if (feature.equals("wordpenalty")) {
				double weight = Double.parseDouble(fields[1].trim());

				Features.add(new WordPenaltyFF());

				logger.info(String.format("FEATURE: word penalty (weight %.3f)", weight));
			}

			else if (feature.equals("oovpenalty")) {
				double weight = Double.parseDouble(fields[1].trim());
				Features.add(new OOVFF(), "oovpenalty", weight);
				num_phrasal_features += 1;
				logger.info(String.format("FEATURE: OOV penalty (weight %.3f)", weight));
			} else {
				System.err.println("* WARNING: invalid feature '" + featureLine + "'");
			}
		}
	}

	public static void main(String[] args) throws IOException {

		String logFile = System.getenv().get("JOSHUA") + "/logging.properties";
		try {
			java.util.logging.LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
		} catch (IOException e) {
			logger.warning("Couldn't initialize logging properties from '" + logFile + "'");
		}

		long startTime = System.currentTimeMillis();
		
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
		// cases. (1) For backwards compatibility, Joshua can be called
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

		logger.info(String.format("Model loading took %d seconds",
				(System.currentTimeMillis() - startTime) / 1000));

		/* Step-2: Decoding */
		decoder.decodeTestSet(testFile, nbestFile, oracleFile);

		/* Step-3: clean up */
		decoder.cleanUp();
		logger.info(String.format("Total running time: %d seconds", 
				(System.currentTimeMillis() - startTime) / 1000));
	}
}
