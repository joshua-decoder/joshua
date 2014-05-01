package joshua.decoder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.fragmentlm.Tree;
import joshua.util.FormatUtils;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * Configuration file for Joshua decoder.
 * 
 * When adding new features to Joshua, any new configurable parameters should be added to this
 * class.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class JoshuaConfiguration {
  // List of language models to load
  public ArrayList<String> lms = new ArrayList<String>();

  // List of grammar files to read
  public ArrayList<String> tms = new ArrayList<String>();

  /*
   * The file to read the weights from (part of the sparse features implementation). Weights can
   * also just be listed in the main config file.
   */
  public String weights_file = "";

  // Default symbols. The symbol here should be enclosed in square brackets.
  public String default_non_terminal = FormatUtils.markup("X");
  public String goal_symbol = FormatUtils.markup("GOAL");

  /*
   * A list of OOV symbols in the form
   * 
   * [X1] weight [X2] weight [X3] weight ...
   * 
   * where the [X] symbols are nonterminals and the weights are weights. For each OOV word w in the
   * input sentence, Joshua will create rules of the form
   * 
   * X1 -> w (weight)
   * 
   * If this is empty, an unweighted default_non_terminal is used.
   */
  public String[] oov_list = null;
  public float[] oov_weights = null;

  /*
   * Whether to segment OOVs into a lattice
   */
  public boolean segment_oovs = false;

  /*
   * If false, sorting of the complete grammar is done at load time. If true, grammar tries are not
   * sorted till they are first accessed. Amortized sorting means you get your first translation
   * much, much quicker (good for debugging), but that per-sentence decoding is a bit slower.
   */
  public boolean amortized_sorting = true;

  // syntax-constrained decoding
  public boolean constrain_parse = false;
  public boolean use_pos_labels = false;

  // oov-specific
  public boolean true_oovs_only = false;

  /* Dynamic sentence-level filtering. */
  public boolean filter_grammar = false;

  /* The cube pruning pop limit. Set to 0 for exhaustive pruning. */
  public int pop_limit = 100;

  /* Maximum sentence length. Sentences longer than this are truncated. */
  public int maxlen = 200;

  /*
   * N-best configuration.
   */
  // Make sure output strings in the n-best list are unique.
  public boolean use_unique_nbest = false;

  /* Include the phrasal alignments in the output (not word-level alignmetns at the moment). */
  public boolean include_align_index = false;

  /* The number of hypotheses to output by default. */
  public int topN = 1;

  /**
   * This string describes the format of each line of output from the decoder (i.e., the
   * translations). The string can include arbitrary text and also variables. The following
   * variables are available:
   * 
   * <pre>
   * - %i the 0-indexed sentence number 
   * - %e the source string %s the translated sentence 
   * - %S the translated sentence with some basic capitalization and denormalization 
   * - %t the synchronous derivation 
   * - %f the list of feature values (as name=value pairs) 
   * - %c the model cost
   * - %w the weight vector 
   * - %a the alignments between source and target words (currently unimplemented) 
   * - %d a verbose, many-line version of the derivation
   * </pre>
   */
  public String outputFormat = "%i ||| %s ||| %f ||| %c";

  /* The number of decoding threads to use (-threads). */
  public int num_parallel_decoders = 1;

  // disk hg
  public String hypergraphFilePattern = "";

  // hypergraph visualization
  public boolean visualize_hypergraph = false;

  // use google linear corpus gain?
  public boolean useGoogleLinearCorpusGain = false;
  public double[] linearCorpusGainThetas = null;

  /*
   * When true, _OOV is appended to all words that are passed through (useful for something like
   * transliteration on the target side
   */
  public boolean mark_oovs = true;

  /* Enables synchronous parsing. */
  public boolean parse = false; // perform synchronous parsing

  private final Logger logger = Logger.getLogger(JoshuaConfiguration.class.getName());

  /* A list of the feature functions. */
  public ArrayList<String> features = new ArrayList<String>();

  /* A list of weights found in the main config file (instead of in a separate weights file) */
  public ArrayList<String> weights = new ArrayList<String>();

  /* If set, Joshua will start a (multi-threaded, per "threads") TCP/IP server on this port. */
  public int server_port = 0;

  /*
   * Whether to do forest rescoring. If set to true, the references are expected on STDIN along with
   * the input sentences in the following format:
   * 
   * input sentence ||| ||| reference1 ||| reference2 ...
   * 
   * (The second field is reserved for the output sentence for alignment and forced decoding).
   */

  public boolean rescoreForest = false;
  public float rescoreForestWeight = 10.0f;

  /*
   * Location of fragment mapping file, which maps flattened SCFG rules to their internal
   * representation.
   */
  public String fragmentMapFile = null;

  /*
   * Whether to use soft syntactic constraint decoding /fuzzy matching, which allows that any
   * nonterminal may be substituted for any other nonterminal (except for OOV and GOAL)
   */
  public boolean fuzzy_matching = false;
  public static final String SOFT_SYNTACTIC_CONSTRAINT_DECODING_PROPERTY_NAME = "fuzzy_matching";

  /**
   * This method resets the state of JoshuaConfiguration back to the state after initialization.
   * This is useful when for example making different calls to the decoder within the same java
   * program, which otherwise leads to potential errors due to inconsistent state as a result of
   * loading the configuration multiple times without resetting etc.
   * 
   * This leads to the insight that in fact it may be an even better idea to refactor the code and
   * make JoshuaConfiguration an object that is is created and passed as an argument, rather than a
   * shared static object. This is just a suggestion for the next step.
   * 
   */
  public void reset() {
    logger.info("Resetting the JoshuaConfiguration to its defaults ...");
    logger.info("\n\tResetting the StatefullFF global state index ...");
    logger.info("\n\t...done");
    StatefulFF.resetGlobalStateIndex();
    lms = new ArrayList<String>();
    tms = new ArrayList<String>();
    weights_file = "";
    default_non_terminal = "[X]";
    oov_list = null;
    oov_weights = null;
    goal_symbol = "[GOAL]";
    amortized_sorting = true;
    constrain_parse = false;
    use_pos_labels = false;
    true_oovs_only = false;
    filter_grammar = false;
    pop_limit = 100;
    maxlen = 200;
    use_unique_nbest = false;
    include_align_index = false;
    topN = 1;
    outputFormat = "%i ||| %s ||| %f ||| %c";
    num_parallel_decoders = 1;
    hypergraphFilePattern = "";
    visualize_hypergraph = false;
    useGoogleLinearCorpusGain = false;
    linearCorpusGainThetas = null;
    mark_oovs = true;
    // oracleFile = null;
    parse = false; // perform synchronous parsing
    features = new ArrayList<String>();
    weights = new ArrayList<String>();
    server_port = 0;
    logger.info("...done");
  }

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * To process command-line options, we write them to a file that looks like the config file, and
   * then call readConfigFile() on it. It would be more general to define a class that sits on a
   * stream and knows how to chop it up, but this was quicker to implement.
   */
  public void processCommandLineOptions(String[] options) {
    try {
      File tmpFile = File.createTempFile("options", null, null);
      PrintWriter out = new PrintWriter(new FileWriter(tmpFile));

      for (int i = 0; i < options.length; i++) {
        String key = options[i].substring(1);
        if (i + 1 == options.length || options[i + 1].startsWith("-")) {
          // if this is the last item, or if the next item
          // is another flag, then this is an argument-less
          // flag
          out.println(key + "=true");

        } else {
          out.println(key + "=" + options[i + 1]);
          // skip the next item
          i++;
        }
      }
      out.close();
      this.readConfigFile(tmpFile.getCanonicalPath());

      tmpFile.delete();

    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void readConfigFile(String configFile) throws IOException {

    LineReader configReader = new LineReader(configFile);
    try {
      for (String line : configReader) {
        line = line.trim(); // .toLowerCase();

        if (Regex.commentOrEmptyLine.matches(line))
          continue;

        /*
         * There are two kinds of substantive (non-comment, non-blank) lines: parameters and feature
         * values. Parameters match the pattern "key = value"; all other substantive lines are
         * interpreted as features.
         */

        if (line.indexOf("=") != -1) { // parameters; (not feature function)
          String[] fds = Regex.equalsWithSpaces.split(line);
          if (fds.length != 2) {
            logger.severe("* FATAL: bad config file line '" + line + "'");
            System.exit(1);
          }

          String parameter = normalize_key(fds[0]);

          // store the line for later processing
          if (parameter.equals(normalize_key("lm"))) {
            lms.add(fds[1]);

          } else if (parameter.equals(normalize_key("tm"))) {
            tms.add(fds[1]);

          } else if (parameter.equals(normalize_key("parse"))) {
            parse = Boolean.parseBoolean(fds[1]);
            logger.finest(String.format("parse: %s", parse));

          } else if (parameter.equals(normalize_key("dump-hypergraph"))) {
            hypergraphFilePattern = fds[1].trim();
            logger
                .finest(String.format("  hypergraph dump file format: %s", hypergraphFilePattern));

          } else if (parameter.equals(normalize_key("oov-list"))) {
            String[] oovs = fds[1].trim().split("\\s+");
            if (oovs.length % 2 != 0) {
              System.err.println(String.format("* FATAL: invalid format for '%s'", fds[0]));
              System.exit(1);
            }

            oov_list = new String[oovs.length / 2];
            oov_weights = new float[oovs.length / 2];

            for (int i = 0; i < oovs.length; i += 2) {
              oov_list[i / 2] = FormatUtils.markup(oovs[i]);
              oov_weights[i / 2] = Float.parseFloat(oovs[i + 1]);
            }

          } else if (parameter.equals(normalize_key("segment-oovs"))) {
            segment_oovs = true;

          } else if (parameter.equals(normalize_key("default-non-terminal"))) {
            default_non_terminal = "[" + fds[1].trim() + "]";
            // default_non_terminal = fds[1].trim();
            logger.finest(String.format("default_non_terminal: %s", default_non_terminal));

          } else if (parameter.equals(normalize_key("goal-symbol"))) {
            goal_symbol = fds[1].trim();

            // If the goal symbol was not enclosed in square brackets, then add them
            if (!goal_symbol.matches("\\[.*\\]"))
              goal_symbol = "[" + goal_symbol + "]";

            logger.finest("goalSymbol: " + goal_symbol);

          } else if (parameter.equals(normalize_key("weights-file"))) {
            weights_file = fds[1];

          } else if (parameter.equals(normalize_key("constrain_parse"))) {
            constrain_parse = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("true_oovs_only"))) {
            true_oovs_only = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("filter-grammar"))) {
            filter_grammar = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("amortize"))) {
            amortized_sorting = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("use_pos_labels"))) {
            use_pos_labels = Boolean.parseBoolean(fds[1]);

          } else if (parameter.equals(normalize_key("use_unique_nbest"))) {
            use_unique_nbest = Boolean.valueOf(fds[1]);
            logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));

          } else if (parameter.equals(normalize_key("output-format"))) {
            outputFormat = fds[1];
            logger.finest(String.format("output-format: %s", outputFormat));

          } else if (parameter.equals(normalize_key("include_align_index"))) {
            include_align_index = Boolean.valueOf(fds[1]);
            logger.finest(String.format("include_align_index: %s", include_align_index));

          } else if (parameter.equals(normalize_key("top_n"))) {
            topN = Integer.parseInt(fds[1]);
            logger.finest(String.format("topN: %s", topN));

          } else if (parameter.equals(normalize_key("num_parallel_decoders"))
              || parameter.equals(normalize_key("threads"))) {
            num_parallel_decoders = Integer.parseInt(fds[1]);
            if (num_parallel_decoders <= 0) {
              throw new IllegalArgumentException(
                  "Must specify a positive number for num_parallel_decoders");
            }
            logger.finest(String.format("num_parallel_decoders: %s", num_parallel_decoders));

          } else if (parameter.equals(normalize_key("visualize_hypergraph"))) {
            visualize_hypergraph = Boolean.valueOf(fds[1]);
            logger.finest(String.format("visualize_hypergraph: %s", visualize_hypergraph));

          } else if (parameter.equals(normalize_key("mark_oovs"))) {
            mark_oovs = Boolean.valueOf(fds[1]);
            logger.finest(String.format("mark_oovs: %s", mark_oovs));

          } else if (parameter.equals(normalize_key("pop-limit"))) {
            pop_limit = Integer.valueOf(fds[1]);
            logger.finest(String.format("pop-limit: %s", pop_limit));

          } else if (parameter.equals(normalize_key("useGoogleLinearCorpusGain"))) {
            useGoogleLinearCorpusGain = new Boolean(fds[1].trim());
            logger
                .finest(String.format("useGoogleLinearCorpusGain: %s", useGoogleLinearCorpusGain));

          } else if (parameter.equals(normalize_key("googleBLEUWeights"))) {
            String[] googleWeights = fds[1].trim().split(";");
            if (googleWeights.length != 5) {
              logger.severe("wrong line=" + line);
              System.exit(1);
            }
            linearCorpusGainThetas = new double[5];
            for (int i = 0; i < 5; i++)
              linearCorpusGainThetas[i] = new Double(googleWeights[i]);

            logger.finest(String.format("googleBLEUWeights: %s", linearCorpusGainThetas));

          } else if (parameter.equals(normalize_key("server-port"))) {
            server_port = Integer.parseInt(fds[1]);
            logger.info(String.format("    server-port: %d", server_port));

          } else if (parameter.equals(normalize_key("rescore-forest"))) {
            rescoreForest = true;
            logger.info(String.format("    rescore-forest: %s", rescoreForest));

          } else if (parameter.equals(normalize_key("rescore-forest-weight"))) {
            rescoreForestWeight = Float.parseFloat(fds[1]);
            logger.info(String.format("    rescore-forest-weight: %f", rescoreForestWeight));

          } else if (parameter.equals(normalize_key("maxlen"))) {
            // reset the maximum length
            maxlen = Integer.parseInt(fds[1]);

          } else if (parameter.equals("c") || parameter.equals("config")) {
            // this was used to send in the config file, just ignore it
            ;

          } else if (parameter.equals(normalize_key("feature-function"))) {
            // add the feature to the list of features for later processing
            features.add("feature_function = " + fds[1]);

          } else if (parameter.equals(normalize_key("maxlen"))) {
            // add the feature to the list of features for later processing
            maxlen = Integer.parseInt(fds[1]);

          } else if (parameter
              .equals(normalize_key(SOFT_SYNTACTIC_CONSTRAINT_DECODING_PROPERTY_NAME))) {
            fuzzy_matching = Boolean.parseBoolean(fds[1]);
            logger.finest(String.format(fuzzy_matching + ": %s", fuzzy_matching));
          } else if (parameter.equals(normalize_key("fragment-map"))) {
            fragmentMapFile = fds[1];
            Tree.readMapping(fragmentMapFile);
          }

          else {

            if (parameter.equals(normalize_key("use-sent-specific-tm"))
                || parameter.equals(normalize_key("add-combined-cost"))
                || parameter.equals(normalize_key("use-tree-nbest"))
                || parameter.equals(normalize_key("use-kenlm"))
                || parameter.equals(normalize_key("useCubePrune"))
                || parameter.equals(normalize_key("useBeamAndThresholdPrune"))
                || parameter.equals(normalize_key("regexp-grammar"))) {
              logger.warning(String.format("WARNING: ignoring deprecated parameter '%s'", fds[0]));

            } else {
              logger.warning("FATAL: unknown configuration parameter '" + fds[0] + "'");
              System.exit(1);
            }
          }

          logger.info(String.format("    %s = '%s'", normalize_key(fds[0]), fds[1]));

        } else {
          /*
           * Lines that don't have an equals sign and are not blank lines, empty lines, or comments,
           * are feature values, which can be present in this file
           */

          weights.add(line);
        }
      }
    } finally {
      configReader.close();
    }

    if (useGoogleLinearCorpusGain) {
      if (linearCorpusGainThetas == null) {
        logger.info("linearCorpusGainThetas is null, did you set googleBLEUWeights properly?");
        System.exit(1);
      } else if (linearCorpusGainThetas.length != 5) {
        logger
            .info("linearCorpusGainThetas does not have five values, did you set googleBLEUWeights properly?");
        System.exit(1);
      }
    }
  }

  /**
   * Checks for invalid variable configurations
   */
  public void sanityCheck() {
  }

  /**
   * Normalizes parameter names by removing underscores and hyphens and lowercasing. This defines
   * equivalence classes on external use of parameter names, permitting arbitrary_under_scores and
   * camelCasing in paramter names without forcing the user to memorize them all. Here are some
   * examples of equivalent ways to refer to parameter names:
   * 
   * {pop-limit, poplimit, PopLimit, popLimit, pop_lim_it} {lmfile, lm-file, LM-FILE, lm_file}
   */
  public static String normalize_key(String text) {
    return text.replaceAll("[-_]", "").toLowerCase();
  }
}
