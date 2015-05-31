package joshua.decoder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
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
  
  public class OOVItem implements Comparable<OOVItem> {
    public String label;
    public float weight;

    OOVItem(String l, float w) {
      label = l;
      weight = w;
    }
    
    @Override
    public int compareTo(OOVItem other) {
      if (weight > other.weight) 
        return -1;
      else if (weight < other.weight)
        return 1;
      return 0;
    }
  }
  public ArrayList<OOVItem> oovList = null;

  /*
   * Whether to segment OOVs into a lattice
   */
  public boolean segment_oovs = false;
  
  /*
   * Enable lattice decoding.
   */
  public boolean lattice_decoding = false;
  
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
  public boolean use_unique_nbest = true;

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

  /*
   * When true, _OOV is appended to all words that are passed through (useful for something like
   * transliteration on the target side
   */
  public boolean mark_oovs = false;

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

  /***
   * Phrase-based decoding parameters.
   */
  
  /* The search algorithm: currently either "cky" or "stack" */
  public String search_algorithm = "cky";
  
  /* The distortion limit */
  public int reordering_limit = 8;
  
  /* The number of target sides considered for each source side (after sorting by model weight) */
  public int num_translation_options = 20;

  /* If true, decode using a dot chart (standard CKY+); if false, use the much more efficient
   * version of Sennrich (SSST 2014)
   */
  public boolean use_dot_chart = true;
  
  /* Moses compatibility */
  public boolean moses = false;
  
  /* If true, just print out the weights found in the config file, and exit. */
  public boolean show_weights_and_quit = false;
  
  /* Read input from a file (Moses compatible flag) */
  public String input_file = null;
  
  /* Write n-best output to this file */
  public String n_best_file = null;

  /* Whether to look at source side for special annotations */
  public boolean source_annotations = false;

  /* Weights overridden from the command line */
  public String weight_overwrite = "";
  
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
    tms = new ArrayList<String>();
    weights_file = "";
    default_non_terminal = "[X]";
    oovList = new ArrayList<OOVItem>(); 
    oovList.add(new OOVItem(default_non_terminal, 1.0f));
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
    mark_oovs = false;
    // oracleFile = null;
    parse = false; // perform synchronous parsing
    features = new ArrayList<String>();
    weights = new ArrayList<String>();
    server_port = 0;
    
    reordering_limit = 8;
    num_translation_options = 20;
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
          // is another flag, then this is a boolean flag
          out.println(key + " = true");

        } else {
          out.print(key + " =");
          while (i + 1 < options.length && ! options[i + 1].startsWith("-")) {
            out.print(String.format(" %s", options[i + 1]));
            i++;
          }
          out.println();
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

    LineReader configReader = new LineReader(configFile, false);
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
          String[] fds = Regex.equalsWithSpaces.split(line, 2);
          if (fds.length < 2) {
            Decoder.LOG(1, String.format("* WARNING: skipping config file line '%s'", line));
            continue;
          }

          String parameter = normalize_key(fds[0]);

          if (parameter.equals(normalize_key("lm"))) {
            /* This is deprecated. This support old LM lines of the form
             * 
             *   lm = berkeleylm 5 false false 100 lm.gz
             * 
             * LMs are now loaded as general feature functions, so we transform that to either
             * 
             *   feature-function = LanguageModel -lm_order 5 -lm_type berkeleylm -lm_file lm.gz
             * 
             * If the line were state minimizing:
             * 
             *   lm = kenlm 5 true false 100 lm.gz
             *              
             * feature-function = StateMinimizingLanguageModel -lm_order 5 -lm_file lm.gz
             */
            
            String[] tokens = fds[1].split("\\s+");
            if (tokens[2].equals("true"))
              features.add(String.format("feature_function = StateMinimizingLanguageModel -lm_type kenlm -lm_order %s -lm_file %s",
                  tokens[1], tokens[5]));
            else
              features.add(String.format("feature_function = LanguageModel -lm_type %s -lm_order %s -lm_file %s",
                  tokens[0], tokens[1], tokens[5]));

          } else if (parameter.equals(normalize_key("tm"))) {
            /* If found, convert old format:
             *   tm = TYPE OWNER MAXSPAN PATH
             * to new format
             *   tm = TYPE -owner OWNER -maxspan MAXSPAN -path PATH    
             */
            String tmLine = fds[1];
            
            String[] tokens = fds[1].split("\\s+");
            if (! tokens[1].startsWith("-")) { // old format
              tmLine = String.format("%s -owner %s -maxspan %s -path %s", tokens[0], tokens[1], tokens[2], tokens[3]);
              Decoder.LOG(1, String.format("WARNING: Converting deprecated TM line from '%s' -> '%s'", fds[1], tmLine));
            }
            tms.add(tmLine);
            
          } else if (parameter.equals("v")) {
            Decoder.VERBOSE = Integer.parseInt(fds[1]);

          } else if (parameter.equals(normalize_key("parse"))) {
            parse = Boolean.parseBoolean(fds[1]);
            logger.finest(String.format("parse: %s", parse));

          } else if (parameter.equals(normalize_key("dump-hypergraph"))) {
            hypergraphFilePattern = fds[1].trim();
            logger
                .finest(String.format("  hypergraph dump file format: %s", hypergraphFilePattern));

          } else if (parameter.equals(normalize_key("oov-list"))) {
            if (new File(fds[1]).exists()) {
              oovList = new ArrayList<OOVItem>();
              try {
                File file = new File(fds[1]);
                BufferedReader br = new BufferedReader(new FileReader(file));
                try {
                  String str = br.readLine();
                  while (str != null) {
                    String[] tokens = str.trim().split("\\s+");

                    oovList.add(new OOVItem(FormatUtils.markup(tokens[0]),
                            (float) Math.log(Float.parseFloat(tokens[1]))));

                    str = br.readLine();
                  }
                  br.close();
                } catch(IOException e){
                  System.out.println(e);
                }
              } catch(IOException e){
                System.out.println(e);
              }
              Collections.sort(oovList);

            } else {
              String[] tokens = fds[1].trim().split("\\s+");
              if (tokens.length % 2 != 0) {
                  System.err.println(String.format("* FATAL: invalid format for '%s'", fds[0]));
                  System.exit(1);
                }

              oovList = new ArrayList<OOVItem>();

              for (int i = 0; i < tokens.length; i += 2)
                oovList.add(new OOVItem(FormatUtils.markup(tokens[i]),
                    (float) Math.log(Float.parseFloat(tokens[i + 1]))));

              Collections.sort(oovList);
            }

          } else if (parameter.equals(normalize_key("lattice-decoding"))) {
            lattice_decoding = true;
            
          } else if (parameter.equals(normalize_key("segment-oovs"))) {
            segment_oovs = true;
            lattice_decoding = true;

          } else if (parameter.equals(normalize_key("default-non-terminal"))) {
            default_non_terminal = String.format("[%s]", FormatUtils.cleanNonterminal(fds[1].trim()));
            logger.finest(String.format("default_non_terminal: %s", default_non_terminal));

          } else if (parameter.equals(normalize_key("goal-symbol"))) {
            goal_symbol = String.format("[%s]", FormatUtils.cleanNonterminal(fds[1].trim()));
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

          } else if (parameter.equals(normalize_key("mark_oovs"))) {
            mark_oovs = Boolean.valueOf(fds[1]);
            logger.finest(String.format("mark_oovs: %s", mark_oovs));

          } else if (parameter.equals(normalize_key("pop-limit"))) {
            pop_limit = Integer.valueOf(fds[1]);
            logger.finest(String.format("pop-limit: %s", pop_limit));

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

          /** PHRASE-BASED PARAMETERS **/
          } else if (parameter.equals(normalize_key("search"))) {
            search_algorithm = fds[1];
            
            if (!search_algorithm.equals("cky") && !search_algorithm.equals("stack")) {
              System.err.println("* FATAL: -search must be one of 'stack' (for phrase-based decoding)");
              System.err.println("*   or 'cky' (for hierarchical / syntactic decoding)");
              System.exit(1);
            }

          } else if (parameter.equals(normalize_key("reordering-limit"))) {
            reordering_limit = Integer.parseInt(fds[1]);

          } else if (parameter.equals(normalize_key("num-translation-options"))) {
            num_translation_options = Integer.parseInt(fds[1]);
            
          } else if (parameter.equals(normalize_key("no-dot-chart"))) {
            use_dot_chart = false;
            
          } else if (parameter.equals(normalize_key("moses"))) {
            moses = true; // triggers some Moses-specific compatibility options
            
          } else if (parameter.equals(normalize_key("show-weights"))) {
            show_weights_and_quit = true;

          } else if (parameter.equals(normalize_key("input-type"))) {
            ; // for Moses compatibility; ignore this 

          } else if (parameter.equals(normalize_key("n-best-list"))) {
            // for Moses compatibility
            String[] tokens = fds[1].split("\\s+");
            n_best_file = tokens[0];
            if (tokens.length > 1)
              topN = Integer.parseInt(tokens[1]);

          } else if (parameter.equals(normalize_key("input-file"))) {
            // for Moses compatibility
            input_file = fds[1];
            
          } else if (parameter.equals(normalize_key("weight-file"))) {
            // for Moses, ignore

          } else if (parameter.equals(normalize_key("weight-overwrite"))) {
            weight_overwrite = fds[1];
            
          } else if (parameter.equals(normalize_key("source-annotations"))) {
            // Check source sentence
            source_annotations = true;

          } else {

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

          Decoder.LOG(1, String.format("    %s = '%s'", normalize_key(fds[0]), fds[1]));

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
