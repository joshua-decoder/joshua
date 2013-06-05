package joshua.decoder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * Configuration file for Joshua decoder.
 * <p>
 * When adding new features to Joshua, any new configurable parameters should be added to this
 * class.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class JoshuaConfiguration {
  // lm config
  // new format enabling multiple language models
  public static ArrayList<String> lms = new ArrayList<String>();

  // new format enabling any number of grammar files
  public static ArrayList<String> tms = new ArrayList<String>();

  // old format specifying attributes of a single language model separately
  public static String lm_type = "kenlm";
  public static float lm_ceiling_cost = 100;
  public static boolean use_left_equivalent_state = false;
  public static boolean use_right_equivalent_state = false;
  public static int lm_order = 3;

  public static String lm_file = null;

  /*
   * The file to read the weights from (part of the sparse features implementation).
   */
  public static String weights_file = "";

  /*
   * The span limit is the maximum span of the input to which rules from the main translation
   * grammar can be applied. It does not apply to the glue grammar.
   */
  public static int span_limit = 20;

  /*
   * This word is in an index into a grammars feature sets. The name here ties together the features
   * present on each grammar line in a grammar file, and the features present in the Joshua
   * configuration file. This allows you to have different sets of features (or shared) across
   * grammar files.
   */
  public static String phrase_owner = "pt";
  public static String glue_owner = "glue";

  // Default symbols. The symbol here should be enclosed in square brackets.
  public static String default_non_terminal = "[X]";
  public static String goal_symbol = "[GOAL]";

  public static boolean dense_features = true;

  /* If false, sorting of the complete grammar is done at load time. If true, grammar tries are not
   * sorted till they are first accessed. */
  public static boolean amortized_sorting = true;

  public static String tm_file = null;
  public static String tm_format = "thrax";

  // TODO: support multiple glue grammars
  public static String glue_file = null;
  public static String glue_format = "thrax";

  // syntax-constrained decoding
  public static boolean constrain_parse = false;
  public static boolean use_pos_labels = false;

  // oov-specific
  public static boolean true_oovs_only = false;
  
  /* Sentence-level filtering. */
  public static boolean filter_grammar = false;

  // pruning config

  // Cube pruning is always on, with a span-level pop limit of 100.
  // Beam and threshold pruning can be enabled, which also changes
  // the nature of cube pruning so that the pop limit is no longer
  // used. If both are turned off, exhaustive pruning takes effect.
  public static int pop_limit = 100;
  public static boolean useCubePrune = true;
  public static boolean useBeamAndThresholdPrune = false;
  public static double fuzz1 = 0.1;
  public static double fuzz2 = 0.1;
  public static int max_n_items = 30;
  public static double relative_threshold = 10.0;
  public static int max_n_rules = 50;

  /* Maximum sentence length */
  public static int maxlen = 200;

  /*
   * N-best configuration.
   */
  // make sure output strings are unique
  public static boolean use_unique_nbest = false;
  // include the phrasal alignments in the output
  public static boolean include_align_index = false;
  // The number of hypotheses to output by default
  public static int topN = 1;

  /*
   * This string describes the format of each line of output from the decoder (i.e., the
   * translations). The string can include arbitrary text and also variables.  The following variables
   * are available:
   * 
   *   %i the 0-index sentence number 
   *   %s the translated sentence 
   *   %S the translated sentence, denormalized
   *   %t the synchronous derivation
   *   %f the list of feature values (as name=value pairs) 
   *   %c the model cost 
   *   %w the weight vector 
   *   %a the alignments between source and target words (currently unimplemented) 
   */
  public static String outputFormat = "%i ||| %s ||| %f ||| %c";

  public static boolean escape_trees = false;

  public static int num_parallel_decoders = 1; // number of threads should run

  // disk hg
  public static String hypergraphFilePattern = "";

  // hypergraph visualization
  public static boolean visualize_hypergraph = false;

  // use google linear corpus gain?
  public static boolean useGoogleLinearCorpusGain = false;
  public static double[] linearCorpusGainThetas = null;
  public static boolean mark_oovs = true;

  // used to extract oracle hypotheses from the forest
  public static String oracleFile = null;

  public static boolean parse = false; // perform synchronous parsing

  private static final Logger logger = Logger.getLogger(JoshuaConfiguration.class.getName());

  public static ArrayList<String> features = new ArrayList<String>();

  /* If set, Joshua will start a (multi-threaded, per "threads") TCP/IP server on this port. */
  public static int server_port = 0;

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * To process command-line options, we write them to a file that looks like the config file, and
   * then call readConfigFile() on it. It would be more general to define a class that sits on a
   * stream and knows how to chop it up, but this was quicker to implement.
   */
  public static void processCommandLineOptions(String[] options) {
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
      JoshuaConfiguration.readConfigFile(tmpFile.getCanonicalPath());

      tmpFile.delete();

    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  // This is static instead of a constructor because all the fields
  // are static.
  public static void readConfigFile(String configFile) throws IOException {

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

          } else if (parameter.equals(normalize_key("dump-hypergraph"))) {
            hypergraphFilePattern = fds[1].trim();
            logger
                .finest(String.format("  hypergraph dump file format: %s", hypergraphFilePattern));

          } else if (parameter.equals(normalize_key("lm_file"))) {
            lm_file = fds[1].trim();
            logger.finest(String.format("lm file: %s", lm_file));
          } else if (parameter.equals(normalize_key("parse"))) {
            parse = Boolean.parseBoolean(fds[1]);
            logger.finest(String.format("parse: %s", parse));

          } else if (parameter.equals(normalize_key("tm_file"))) {
            tm_file = fds[1].trim();
            logger.finest(String.format("tm file: %s", tm_file));

          } else if (parameter.equals(normalize_key("glue_file"))) {
            glue_file = fds[1].trim();
            logger.finest(String.format("glue file: %s", glue_file));

          } else if (parameter.equals(normalize_key("tm_format"))) {
            tm_format = fds[1].trim();
            logger.finest(String.format("tm format: %s", tm_format));

          } else if (parameter.equals(normalize_key("glue_format"))) {
            glue_format = fds[1].trim();
            logger.finest(String.format("glue format: %s", glue_format));

          } else if (parameter.equals(normalize_key("dump-hypergraph"))) {
            hypergraphFilePattern = fds[1].trim();
            logger
                .finest(String.format("  hypergraph dump file format: %s", hypergraphFilePattern));

          } else if (parameter.equals(normalize_key("lm_type"))) {
            lm_type = String.valueOf(fds[1]);
            if (!lm_type.equals("kenlm") && !lm_type.equals("berkeleylm")
                && !lm_type.equals("none") && !lm_type.equals("javalm")) {
              System.err.println("* FATAL: lm_type '" + lm_type + "' not supported");
              System.err
                  .println("* supported types are 'kenlm' (default), 'berkeleylm', and 'javalm' (not recommended), and 'none'");
              System.exit(1);
            }

          } else if (parameter.equals(normalize_key("lm_ceiling_cost"))) {
            lm_ceiling_cost = Float.parseFloat(fds[1]);
            logger.finest(String.format("lm_ceiling_cost: %s", lm_ceiling_cost));

          } else if (parameter.equals(normalize_key("use_left_equivalent_state"))) {
            use_left_equivalent_state = Boolean.valueOf(fds[1]);
            logger
                .finest(String.format("use_left_equivalent_state: %s", use_left_equivalent_state));

          } else if (parameter.equals(normalize_key("use_right_equivalent_state"))) {
            use_right_equivalent_state = Boolean.valueOf(fds[1]);
            logger.finest(String.format("use_right_equivalent_state: %s",
                use_right_equivalent_state));

          } else if (parameter.equals(normalize_key("order"))) {
            lm_order = Integer.parseInt(fds[1]);
            logger.finest(String.format("g_lm_order: %s", lm_order));

          } else if (parameter.equals(normalize_key("span_limit"))) {
            span_limit = Integer.parseInt(fds[1]);
            logger.finest(String.format("span_limit: %s", span_limit));

          } else if (parameter.equals(normalize_key("phrase_owner"))) {
            phrase_owner = fds[1].trim();
            logger.finest(String.format("phrase_owner: %s", phrase_owner));

          } else if (parameter.equals(normalize_key("glue_owner"))) {
            glue_owner = fds[1].trim();
            logger.finest(String.format("glue_owner: %s", glue_owner));

          } else if (parameter.equals(normalize_key("default_non_terminal"))) {
            default_non_terminal = "[" + fds[1].trim() + "]";
            // default_non_terminal = fds[1].trim();
            logger.finest(String.format("default_non_terminal: %s", default_non_terminal));

          } else if (parameter.equals(normalize_key("goalSymbol"))) {
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

          } else if (parameter.equals(normalize_key("fuzz1"))) {
            fuzz1 = Double.parseDouble(fds[1]);
            logger.finest(String.format("fuzz1: %s", fuzz1));

          } else if (parameter.equals(normalize_key("fuzz2"))) {
            fuzz2 = Double.parseDouble(fds[1]);
            logger.finest(String.format("fuzz2: %s", fuzz2));

          } else if (parameter.equals(normalize_key("max_n_items"))) {
            max_n_items = Integer.parseInt(fds[1]);
            logger.finest(String.format("max_n_items: %s", max_n_items));

          } else if (parameter.equals(normalize_key("relative_threshold"))) {
            relative_threshold = Double.parseDouble(fds[1]);
            logger.finest(String.format("relative_threshold: %s", relative_threshold));

          } else if (parameter.equals(normalize_key("max_n_rules"))) {
            max_n_rules = Integer.parseInt(fds[1]);
            logger.finest(String.format("max_n_rules: %s", max_n_rules));

          } else if (parameter.equals(normalize_key("use_unique_nbest"))) {
            use_unique_nbest = Boolean.valueOf(fds[1]);
            logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));

          } else if (parameter.equals(normalize_key("output-format"))) {
            outputFormat = fds[1];
            logger.finest(String.format("output-format: %s", outputFormat));

          } else if (parameter.equals(normalize_key("escape_trees"))) {
            escape_trees = Boolean.valueOf(fds[1]);
            logger.finest(String.format("escape_trees: %s", escape_trees));

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

          } else if (parameter.equals(normalize_key("useCubePrune"))) {
            useCubePrune = Boolean.valueOf(fds[1]);
            if (useCubePrune == false)
              logger.warning("useCubePrune=false");
            logger.finest(String.format("useCubePrune: %s", useCubePrune));

          } else if (parameter.equals(normalize_key("useBeamAndThresholdPrune"))) {
            useBeamAndThresholdPrune = Boolean.valueOf(fds[1]);
            if (useBeamAndThresholdPrune == false)
              logger.warning("useBeamAndThresholdPrune=false");
            logger.finest(String.format("useBeamAndThresholdPrune: %s", useBeamAndThresholdPrune));

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

          } else if (parameter.equals(normalize_key("oracleFile"))) {
            oracleFile = fds[1].trim();
            logger.info(String.format("    oracle file: %s", oracleFile));
            if (!new File(oracleFile).exists()) {
              logger.warning("FATAL: can't find oracle file '" + oracleFile + "'");
              System.exit(1);
            }
            
          } else if (parameter.equals(normalize_key("server-port"))) {
            server_port = Integer.parseInt(fds[1]);
            logger.info(String.format("    server-port: %d", server_port));

          } else if (parameter.equals("c") || parameter.equals("config")) {
            // this was used to send in the config file, just ignore it
            ;

          } else if (parameter.equals(normalize_key("feature-function"))) {
            // add the feature to the list of features for later processing
            features.add("feature_function = " + fds[1]);

          } else if (parameter.equals(normalize_key("maxlen"))) {
            // add the feature to the list of features for later processing
            maxlen = Integer.parseInt(fds[1]);
            
          } else {
            
            if (parameter.equals(normalize_key("use-sent-specific-tm"))
                || parameter.equals(normalize_key("add-combined-cost"))
                || parameter.equals(normalize_key("use-tree-nbest"))
                || parameter.equals(normalize_key("use-kenlm"))
                || parameter.equals(normalize_key("regexp-grammar"))) {  
              logger.warning(String.format("WARNING: ignoring deprecated parameter '%s'", fds[0]));

            } else {
              logger.warning("FATAL: unknown configuration parameter '" + fds[0] + "'");
              System.exit(1);
            }
          }

          logger.info(String.format("    %s = '%s'", normalize_key(fds[0]), fds[1]));

        } else {
          // Feature function. These are processed a bit later
          // in JoshuaDecoder initialization, so we just set
          // them aside for now.

          features.add(line);
        }
      }
    } finally {
      configReader.close();
    }

    // This is for backwards compatibility of LM format. If the
    // config file did not contain lines of the form "lm = ...",
    // then we create one from the handful of separately-specified
    // parameters. These combined lines are later processed in
    // JoshuaDecoder as part of the multiple LM support
    if (lms.size() == 0 && lm_file != null) {
      String line = String.format("%s %d %b %b %.2f %s", lm_type, lm_order,
          use_left_equivalent_state, use_right_equivalent_state, lm_ceiling_cost, lm_file);
      lms.add(line);
    }

    // Language model orders are particular to each LM, but for
    // purposes of state maintenance, we set the global value to
    // the maximum of any of the individual models
    for (String lmLine : lms) {
      String tokens[] = lmLine.split("\\s+");
      int order = Integer.parseInt(tokens[1]);
      if (order > JoshuaConfiguration.lm_order)
        JoshuaConfiguration.lm_order = order;
    }

    /*
     * Now we do a similar thing for the TMs, enabling backward compatibility with the old format
     * that allowed for just two grammars. The new format is
     * 
     * tm = FORMAT OWNER SPAN_LIMIT FILE
     */
    if (tms.size() == 0 && tm_file != null) {
      tms.add(String.format("%s %s %d %s", tm_format, phrase_owner, span_limit, tm_file));
      tms.add(String.format("%s %s %d %s", glue_format, glue_owner, -1, glue_file));
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
  public static void sanityCheck() {
    if (pop_limit > 0 && useBeamAndThresholdPrune) {
      System.err
          .println("* FATAL: 'pop-limit' >= 0 is incompatible with 'useBeamAndThresholdPrune'");
      System.exit(0);
    }
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
