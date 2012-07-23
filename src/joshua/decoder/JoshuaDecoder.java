/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
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
import joshua.decoder.ff.similarity.EdgePhraseSimilarityFF;
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
 * Implements decoder initialization, including interaction with <code>JoshuaConfiguration</code>
 * and <code>DecoderThread</code>.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Lane Schwartz <dowobeha@users.sourceforge.net>
 * @version $LastChangedDate$
 */
public class JoshuaDecoder {
  /*
   * Many of these objects themselves are global objects. We pass them in when constructing other
   * objects, so that they all share pointers to the same object. This is good because it reduces
   * overhead, but it can be problematic because of unseen dependencies (for example, in the
   * Vocabulary shared by language model, translation grammar, etc).
   */
  /** The DecoderFactory is the main thread of decoding */
  private DecoderFactory decoderFactory;
  private List<GrammarFactory> grammarFactories;
  private ArrayList<FeatureFunction> featureFunctions;
  private ArrayList<NGramLanguageModel> languageModels;

  private List<StateComputer> stateComputers;

  private Map<String, Integer> ruleStringToIDTable;

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(JoshuaDecoder.class.getName());

  // ===============================================================
  // Constructors
  // ===============================================================

  /**
   * Constructs a new decoder using the specified configuration file.
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
   * This method is private because it should only ever be called by the
   * {@link #getUninitalizedDecoder()} method to provide an uninitialized decoder for use in
   * testing.
   */
  private JoshuaDecoder() {
    this.grammarFactories = new ArrayList<GrammarFactory>();
  }

  /**
   * Gets an uninitialized decoder for use in testing.
   * <p>
   * This method is called by unit tests or any outside packages (e.g., MERT) relying on the
   * decoder.
   */
  static public JoshuaDecoder getUninitalizedDecoder() {
    return new JoshuaDecoder();
  }

  // ===============================================================
  // Public Methods
  // ===============================================================

  public void changeBaselineFeatureWeights(double[] weights) {
    changeFeatureWeightVector(weights, null);
  }

  public void changeDiscrminativeModelOnly(String discrminativeModelFile) {
    changeFeatureWeightVector(null, discrminativeModelFile);
  }

  /**
   * Sets the feature weight values used by the decoder.
   * <p>
   * This method assumes that the order of the provided weights is the same as their order in the
   * decoder's configuration file.
   * 
   * @param weights Feature weight values
   */
  public void changeFeatureWeightVector(double[] weights, String discrminativeModelFile) {
    if (weights != null) {
      if (this.featureFunctions.size() != weights.length) {
        throw new IllegalArgumentException(
            "number of weights does not match number of feature functions");
      }

      int i = 0;
      for (FeatureFunction ff : this.featureFunctions) {
        double oldWeight = ff.getWeight();
        ff.setWeight(weights[i]);
        logger.info("Feature function : " + ff.getClass().getSimpleName()
            + "; weight changed from " + oldWeight + " to " + ff.getWeight());
        i++;
      }
    }
    // FIXME: this works for Batch grammar only; not for sentence-specific grammars
    for (GrammarFactory grammarFactory : this.grammarFactories) {
      // if (grammarFactory instanceof Grammar) {
      grammarFactory.getGrammarForSentence(null).sortGrammar(this.featureFunctions);
      // }
    }
  }


  /**
   * Decode a whole test set. This may be parallel.
   * 
   * @param testFile
   * @param nbestFile
   * @param oracleFile
   */
  public void decodeTestSet(String testFile, String nbestFile, String oracleFile)
      throws IOException {
    this.decoderFactory.decodeTestSet(testFile, nbestFile, oracleFile);
  }

  public void decodeTestSet(String testFile, String nbestFile) {
    this.decoderFactory.decodeTestSet(testFile, nbestFile, null);
  }


  /** Decode a sentence. This must be non-parallel. */
  public void decodeSentence(String testSentence, String[] nbests) {
    // TODO
  }


  public void cleanUp() {
    // TODO
    // this.languageModel.end_lm_grammar(); //end the threads
  }

  public void visualizeHyperGraphForSentence(String sentence) {
    HyperGraphViewer.visualizeHypergraphInFrame(this.decoderFactory
        .getHyperGraphForSentence(sentence));
  }


  public static void writeConfigFile(double[] newWeights, String template, String outputFile,
      String newDiscriminativeModel) {
    try {
      int columnID = 0;

      BufferedWriter writer = FileUtility.getWriteFileStream(outputFile);
      LineReader reader = new LineReader(template);
      try {
        for (String line : reader) {
          line = line.trim();
          if (Regex.commentOrEmptyLine.matches(line) || line.indexOf("=") != -1) {
            // comment, empty line, or parameter lines: just copy
            writer.write(line);
            writer.newLine();

          } else { // models: replace the weight
            String[] fds = Regex.spaces.split(line);
            StringBuffer newSent = new StringBuffer();
            if (!Regex.floatingNumber.matches(fds[fds.length - 1])) {
              throw new IllegalArgumentException("last field is not a number; the field is: "
                  + fds[fds.length - 1]);
            }

            if (newDiscriminativeModel != null && "discriminative".equals(fds[0])) {
              newSent.append(fds[0]).append(' ');
              newSent.append(newDiscriminativeModel).append(' ');// change the file name
              for (int i = 2; i < fds.length - 1; i++) {
                newSent.append(fds[i]).append(' ');
              }
            } else {// regular
              for (int i = 0; i < fds.length - 1; i++) {
                newSent.append(fds[i]).append(' ');
              }
            }
            if (newWeights != null)
              newSent.append(newWeights[columnID++]);// change the weight
            else
              newSent.append(fds[fds.length - 1]);// do not change

            writer.write(newSent.toString());
            writer.newLine();
          }
        }
      } finally {
        reader.close();
        writer.close();
      }

      if (newWeights != null && columnID != newWeights.length) {
        throw new IllegalArgumentException("number of models does not match number of weights");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }



  // ===============================================================
  // Initialization Methods
  // ===============================================================

  /**
   * Initialize all parts of the JoshuaDecoder.
   * 
   * @param configFile File containing configuration options
   * @return An initialized decoder
   */
  public JoshuaDecoder initialize(String configFile) {
    try {

      long pre_load_time = System.currentTimeMillis();
      // Initialize and load grammars.
      this.initializeTranslationGrammars();
      logger.info(String.format("Grammar loading took: %d seconds.",
          (System.currentTimeMillis() - pre_load_time) / 1000));

      // Initialize the LM.
      initializeLanguageModel();

      // Initialize the features: requires that LM model has been initialized.
      this.initializeFeatureFunctions();

      // Initialize features that contribute to state (currently only n-grams).
      this.initializeStateComputers(JoshuaConfiguration.lm_order, JoshuaConfiguration.ngramStateID);

      long pre_sort_time = System.currentTimeMillis();
      // Sort the TM grammars (needed to do cube pruning)
      for (GrammarFactory grammarFactory : this.grammarFactories) {
        if (grammarFactory instanceof Grammar) {
          Grammar batchGrammar = (Grammar) grammarFactory;
          batchGrammar.sortGrammar(this.featureFunctions);
        }
      }
      logger.info(String.format("Grammar sorting took: %d seconds.",
          (System.currentTimeMillis() - pre_sort_time) / 1000));

      this.decoderFactory =
          new DecoderFactory(this.grammarFactories, JoshuaConfiguration.use_max_lm_cost_for_oov,
              this.featureFunctions, this.stateComputers);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return this;
  }

  private void initializeLanguageModel() throws IOException {

    this.languageModels = new ArrayList<NGramLanguageModel>();

    // lm = kenlm 5 0 0 100 file
    for (String lmLine : JoshuaConfiguration.lms) {

      String tokens[] = lmLine.split("\\s+");
      String lm_type = tokens[0];
      int lm_order = Integer.parseInt(tokens[1]);
      boolean left_equiv_state = Boolean.parseBoolean(tokens[2]);
      boolean right_equiv_state = Boolean.parseBoolean(tokens[3]);
      double lm_ceiling_cost = Double.parseDouble(tokens[4]);
      String lm_file = tokens[5];

      if (lm_type.equals("kenlm")) {
        if (left_equiv_state || right_equiv_state) {
          throw new IllegalArgumentException(
              "KenLM supports state.  Joshua should get around to using it.");
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

        this.languageModels.add(new LMGrammarJAVA(lm_order, lm_file, left_equiv_state,
            right_equiv_state));
      }
    }
  }

  private void initializeGlueGrammar() throws IOException {
    logger.info("Constructing glue grammar...");

    MemoryBasedBatchGrammar gr = (JoshuaConfiguration.glue_file == null) 
      ? new MemoryBasedBatchGrammar(JoshuaConfiguration.glue_format, 
            System.getenv().get("JOSHUA") + "/data/" + "glue-grammar",
            JoshuaConfiguration.glue_owner, JoshuaConfiguration.default_non_terminal, -1,
            JoshuaConfiguration.oov_feature_cost)
      : new MemoryBasedBatchGrammar(JoshuaConfiguration.glue_format, JoshuaConfiguration.glue_file,
            JoshuaConfiguration.glue_owner, JoshuaConfiguration.default_non_terminal, -1,
            JoshuaConfiguration.oov_feature_cost);

    this.grammarFactories.add(gr);

  }

  private void initializeTranslationGrammars() throws IOException {

		if (JoshuaConfiguration.tms.size() > 0) {

      // tm = {thrax/hiero,packed,samt} OWNER LIMIT FILE
      for (String tmLine: JoshuaConfiguration.tms) {
        String tokens[] = tmLine.split("\\s+");
        String format = tokens[0];
        String owner = tokens[1];
        int span_limit = Integer.parseInt(tokens[2]);
        String file = tokens[3];

        logger.info("Using grammar read from file " + file);

        if (format.equals("packed")) {
          this.grammarFactories.add(new PackedGrammar(file, span_limit));
        } else {
          this.grammarFactories.add(new MemoryBasedBatchGrammar(format, file, owner, 
              JoshuaConfiguration.default_non_terminal, span_limit,
              JoshuaConfiguration.oov_feature_cost));
        }
      }
    } else {
      logger.warning("* WARNING: no grammars supplied!  Supplying dummy glue grammar.");
      // TODO: this should initialize the grammar dynamically so that the goal symbol and default
      // non terminal match
      MemoryBasedBatchGrammar glueGrammar = new MemoryBasedBatchGrammar(JoshuaConfiguration.glue_format, 
        System.getenv().get("JOSHUA") + "/data/" + "glue-grammar",
        JoshuaConfiguration.glue_owner, JoshuaConfiguration.default_non_terminal, -1,
        JoshuaConfiguration.oov_feature_cost);
      this.grammarFactories.add(glueGrammar);
		}

		logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));
  }
	

  private void initializeMainTranslationGrammar() throws IOException {
		if (JoshuaConfiguration.tm_file == null) {
      logger.warning("* WARNING: no TM specified");
			return;
		}

    if (JoshuaConfiguration.use_sent_specific_tm) {
      logger.info("Basing sentence-specific grammars on file " + JoshuaConfiguration.tm_file);
      return;
    } else if ("packed".equals(JoshuaConfiguration.tm_format)) {
      this.grammarFactories.add(new PackedGrammar(JoshuaConfiguration.tm_file,
          JoshuaConfiguration.span_limit));
    } else {
      logger.info("Using grammar read from file " + JoshuaConfiguration.tm_file);
      this.grammarFactories.add(new MemoryBasedBatchGrammar(JoshuaConfiguration.tm_format,
          JoshuaConfiguration.tm_file, JoshuaConfiguration.phrase_owner,
          JoshuaConfiguration.default_non_terminal, JoshuaConfiguration.span_limit,
          JoshuaConfiguration.oov_feature_cost));
    }

    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));
  }

  private void initializeStateComputers(int nGramOrder, int ngramStateID) {
    stateComputers = new ArrayList<StateComputer>();
    if (nGramOrder > 0) stateComputers.add(new NgramStateComputer(nGramOrder, ngramStateID));
  }

  // iterate over the features that were discovered when the config file was read
  private void initializeFeatureFunctions() {
    this.featureFunctions = new ArrayList<FeatureFunction>();
    JoshuaConfiguration.num_phrasal_features = 0;

    for (String featureLine : JoshuaConfiguration.features) {

      String fields[] = featureLine.split("\\s+");
      String feature = fields[0];

      // initialize the language model
      if (feature.equals("lm") && !JoshuaConfiguration.lm_type.equals("none")) {
        int index;
        double weight;

        // new format
        if (fields.length == 3) {
          index = Integer.parseInt(fields[1]);
          weight = Double.parseDouble(fields[2]);
        } else {
          index = 0;
          weight = Double.parseDouble(fields[1]);
        }

        if (index >= this.languageModels.size()) {
          System.err.println(String.format(
              "* FATAL: there is no LM corresponding to LM feature index %d", index));
          System.exit(1);
        }

        this.featureFunctions.add(new LanguageModelFF(JoshuaConfiguration.ngramStateID,
            this.featureFunctions.size(), this.languageModels.get(index).getOrder(),
            this.languageModels.get(index), weight));

        // TODO: lms should have a name or something
        logger.info(String.format("FEATURE: language model #%d, order %d (weight %.3f)",
            (index + 1), languageModels.get(index).getOrder(), weight));
      }

      else if (feature.equals("latticecost")) {
        double weight = Double.parseDouble(fields[1]);
        this.featureFunctions.add(new SourcePathFF(this.featureFunctions.size(), weight));
        logger.info(String.format("FEATURE: lattice cost (weight %.3f)", weight));
      }

      else if (feature.equals("phrasemodel")) {
        // TODO: error-checking

        int owner = Vocabulary.id(fields[1]);
        int column = Integer.parseInt(fields[2].trim());
        double weight = Double.parseDouble(fields[3].trim());

        this.featureFunctions.add(new PhraseModelFF(this.featureFunctions.size(), weight, owner,
            column));
        JoshuaConfiguration.num_phrasal_features += 1;

        logger.info(String.format("FEATURE: phrase model %d, owner %s (weight %.3f)", column,
            owner, weight));
      }

      else if (feature.equals("arityphrasepenalty")) {
        int owner = Vocabulary.id(fields[1]);
        int startArity = Integer.parseInt(fields[2].trim());
        int endArity = Integer.parseInt(fields[3].trim());
        double weight = Double.parseDouble(fields[4].trim());
        this.featureFunctions.add(new ArityPhrasePenaltyFF(this.featureFunctions.size(), weight,
            owner, startArity, endArity));

        logger.info(String.format(
            "FEATURE: arity phrase penalty: owner %s, start %d, end %d (weight %.3f)", owner,
            startArity, endArity, weight));
      }

      else if (feature.equals("wordpenalty")) {
        double weight = Double.parseDouble(fields[1].trim());

        this.featureFunctions.add(new WordPenaltyFF(this.featureFunctions.size(), weight));

        logger.info(String.format("FEATURE: word penalty (weight %.3f)", weight));
      }

      else if (feature.equals("oovpenalty")) {
        double weight = Double.parseDouble(fields[1].trim());
        int owner = Vocabulary.id("pt");
        int column = JoshuaConfiguration.num_phrasal_features;

        this.featureFunctions.add(new OOVFF(this.featureFunctions.size(), weight, owner));

        JoshuaConfiguration.oov_feature_index = column;
        JoshuaConfiguration.num_phrasal_features += 1;

        logger.info(String.format("FEATURE: OOV penalty (weight %.3f)", weight));
      } else if (feature.equals("edge-sim")) {
        String host = fields[1].trim();
        int port = Integer.parseInt(fields[2].trim());
        double weight = Double.parseDouble(fields[3].trim());
        try {
          this.featureFunctions.add(new EdgePhraseSimilarityFF(JoshuaConfiguration.ngramStateID,
              weight, this.featureFunctions.size(), host, port));
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
        logger.info(String.format("FEATURE: edge similarity (weight %.3f)", weight));
      } else {
        System.err.println("* WARNING: invalid feature '" + featureLine + "'");
      }
    }
  }


  // ===============================================================
  // Main
  // ===============================================================
  public static void main(String[] args) throws IOException {

    String logFile = System.getenv().get("JOSHUA") + "/logging.properties";
    try {
      java.util.logging.LogManager.getLogManager().readConfiguration(new FileInputStream(logFile));
    } catch (IOException e) {
      logger.warning("Couldn't initialize logging properties from '" + logFile + "'");
    }

    long startTime = System.currentTimeMillis();

    // if (args.length < 1) {
    //   System.out.println("Usage: java " + JoshuaDecoder.class.getName()
    //       + " -c configFile [other args]");
    //   System.exit(1);
    // }

    String configFile = null;
    String testFile = "-";
    String nbestFile = "-";
    String oracleFile = null;

    // Step-0: Process the configuration file. We accept two use
    // cases. (1) For backwards compatility, Joshua can be called
    // with as "Joshua configFile [testFile [outputFile
    // [oracleFile]]]". (2) Command-line options can be used, in
    // which case we look for an argument to the "-config" flag.
    // We can distinguish these two cases by looking at the first
    // argument; if it starts with a hyphen, the new format has
    // been invoked.

		if (args.length >= 1) {
			if (args[0].startsWith("-")) {

				// Search for the configuration file
				for (int i = 0; i < args.length; i++) {
					if (args[i].equals("-c") || args[i].equals("-config")) {

						configFile = args[i + 1].trim();
						JoshuaConfiguration.readConfigFile(configFile);

						break;
					}
				}

				// now process all the command-line args
				JoshuaConfiguration.processCommandLineOptions(args);

				oracleFile = JoshuaConfiguration.oracleFile;

			} else {

				configFile = args[0].trim();

				JoshuaConfiguration.readConfigFile(configFile);

				if (args.length >= 2) testFile = args[1].trim();
				if (args.length >= 3) nbestFile = args[2].trim();
				if (args.length == 4) oracleFile = args[3].trim();
			}
		}

    /* Step-0: some sanity checking */
    JoshuaConfiguration.sanityCheck();

    /* Step-1: initialize the decoder, test-set independent */
    JoshuaDecoder decoder = new JoshuaDecoder(configFile);

    logger.info(String.format("Model loading took %d seconds",
        (System.currentTimeMillis() - startTime) / 1000));
    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));

    /* Step-2: Decoding */
    decoder.decodeTestSet(testFile, nbestFile, oracleFile);

    logger.info("Decoding completed.");
    logger.info(String.format("Memory used %.1f MB", ((Runtime.getRuntime().totalMemory() - Runtime
        .getRuntime().freeMemory()) / 1000000.0)));

    /* Step-3: clean up */
    decoder.cleanUp();
    logger.info(String.format("Total running time: %d seconds",
        (System.currentTimeMillis() - startTime) / 1000));
  }
}
