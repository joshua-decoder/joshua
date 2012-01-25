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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.util.Cache;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * Configuration file for Joshua decoder.
 * <p>
 * When adding new features to Joshua, any new configurable parameters
 * should be added to this class.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class JoshuaConfiguration {
	//lm config
    // new format enabling multiple language models
    public static ArrayList<String> lms              = new ArrayList<String>();

    // old format specifying attributes of a single language model separately
	public static String lm_type                     = "kenlm";
	public static double  lm_ceiling_cost            = 100;
	public static boolean use_left_equivalent_state  = false;
	public static boolean use_right_equivalent_state = true;
	public static int     lm_order                   = 3;
	public static boolean use_sent_specific_lm       = false;
	public static String  lm_file                    = null;
	public static int     ngramStateID               = 0;    // TODO ?????????????
	
	//tm config
	public static int     span_limit 								 = 10;
	//note: owner should be different from each other, it can have same value as a word in LM/TM
	public static String  phrase_owner               = "pt";
	public static String  glue_owner                 = "pt";
	public static String  default_non_terminal       = "PHRASE";
	public static String  goal_symbol                = "S";
	public static boolean use_sent_specific_tm       = false;
	
	public static String  tm_file                    = null;
	public static String  tm_format                  = null;
	
	// TODO: default to glue grammar provided with Joshua
	// TODO: support multiple glue grammars
	public static String  glue_file                  = null;
	public static String  glue_format                = null;
	
	// syntax-constrained decoding
	public static boolean constrain_parse            = false;
	public static boolean use_pos_labels             = false;
	
	// oov-specific
	public static float   oov_feature_cost           = 100;
	public static boolean use_max_lm_cost_for_oov    = false;
	public static int     oov_feature_index          = -1;

	// number of phrasal features, for correct oov rule creation
	public static int     num_phrasal_features       = 0;
	
	//pruning config

	// Cube pruning is always on, with a span-level pop limit of 100.
	// Beam and threshold pruning can be enabled, which also changes
	// the nature of cube pruning so that the pop limit is no longer
	// used.  If both are turned off, exhaustive pruning takes effect.
    public static int     pop_limit                = 100;
    public static boolean useCubePrune             = true;
    public static boolean useBeamAndThresholdPrune = false;
	public static double  fuzz1                    = 0.1;
	public static double  fuzz2                    = 0.1;
	public static int     max_n_items              = 30;
	public static double  relative_threshold       = 10.0;
	public static int     max_n_rules              = 50;
	
	//nbest config
	public static boolean use_unique_nbest    = false;
	public static boolean use_tree_nbest      = false;
	public static boolean include_align_index = false;
	public static boolean add_combined_cost   = true; //in the nbest file, compute the final score
	public static int     topN                = 500;
	public static boolean escape_trees        = false;
	
	//parallel decoding
	public static String parallel_files_prefix = "/tmp/temp.parallel"; // C:\\Users\\zli\\Documents\\temp.parallel; used for parallel decoding
	public static int    num_parallel_decoders = 1; //number of threads should run
	
	//disk hg
	public static boolean save_disk_hg             = false; //if true, save three files: fnbest, fnbest.hg.items, fnbest.hg.rules
	public static boolean use_kbest_hg = false;
	public static boolean forest_pruning           = false;
	public static double  forest_pruning_threshold = 10;
	
	// hypergraph visualization
	public static boolean visualize_hypergraph = false;
	
	// use google linear corpus gain?
	public static boolean useGoogleLinearCorpusGain = false;
	public static double[] linearCorpusGainThetas = null;
	public static boolean mark_oovs = true;
	
	// used to extract oracle hypotheses from the forest
	public static String oracleFile = "";
	
	private static final Logger logger =
		Logger.getLogger(JoshuaConfiguration.class.getName());

    public static ArrayList<String> features = new ArrayList<String>();
	
//===============================================================
// Methods
//===============================================================
	
    /**
     * To process command-line options, we write them to a file that
     * looks like the config file, and then call readConfigFile() on
     * it.  It would be more general to define a class that sits on a
     * stream and knows how to chop it up, but this was quicker to implement.
     */
    public static void processCommandLineOptions(String[] options) {
        try { 
            File tmpFile = File.createTempFile("options", null, null);
            PrintWriter out = new PrintWriter(new FileWriter(tmpFile));

            for (int i = 0; i < options.length; i++) {
                String key = options[i].substring(1);
                if (i + 1 == options.length || options[i+1].startsWith("-")) {
                    // if this is the last item, or if the next item
                    // is another flag, then this is an argument-less
                    // flag
                    out.println(key + "=true");

                } else {
                    out.println(key + "=" + options[i+1]);
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
		try { for (String line : configReader) {
			line = line.trim(); // .toLowerCase();

			if (Regex.commentOrEmptyLine.matches(line)) continue;
			
            /* There are two kinds of substantive (non-comment,
             * non-blank) lines: parameters and feature values.
             * Parameters match the pattern "key = value"; all other
             * substantive lines are interpreted as features.
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

                } else if (parameter.equals(normalize_key("lm_file"))) {
					lm_file = fds[1].trim();
					logger.finest(String.format("lm file: %s", lm_file));
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
				} else if (parameter.equals(normalize_key("lm_type"))) {
					lm_type = String.valueOf(fds[1]);
					if (! lm_type.equals("kenlm")
                        && ! lm_type.equals("berkeleylm")
                        && ! lm_type.equals("none")
                        && ! lm_type.equals("javalm")) {
						System.err.println("* FATAL: lm_type '" + lm_type + "' not supported");
						System.err.println("* supported types are 'kenlm' (default), 'berkeleylm', and 'javalm' (not recommended), and 'none'");
						System.exit(1);
					}
					
				} else if (parameter.equals(normalize_key("lm_ceiling_cost"))) {
					lm_ceiling_cost = Double.parseDouble(fds[1]);
					logger.finest(String.format("lm_ceiling_cost: %s", lm_ceiling_cost));
					
				} else if (parameter.equals(normalize_key("use_left_equivalent_state"))) {
					use_left_equivalent_state = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_left_equivalent_state: %s", use_left_equivalent_state));
				
				} else if (parameter.equals(normalize_key("use_right_equivalent_state"))) {
					use_right_equivalent_state = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_right_equivalent_state: %s", use_right_equivalent_state));
					
				} else if (parameter.equals(normalize_key("order"))) {
					lm_order = Integer.parseInt(fds[1]);
					logger.finest(String.format("g_lm_order: %s", lm_order));
					
				} else if (parameter.equals(normalize_key("use_sent_specific_lm"))) {
					use_sent_specific_lm = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_sent_specific_lm: %s", use_sent_specific_lm));
					
				} else if (parameter.equals(normalize_key("use_sent_specific_tm"))) {
					use_sent_specific_tm = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_sent_specific_tm: %s", use_sent_specific_tm));

				} else if (parameter.equals(normalize_key("span_limit"))) {
					span_limit = Integer.parseInt(fds[1]);
					logger.finest(String.format("span_limit: %s", span_limit));
					
				} else if (parameter.equals(normalize_key("phrase_owner"))) {
					phrase_owner = fds[1].trim();
					logger.finest(String.format("phrase_owner: %s", phrase_owner));
					
				} else if (parameter.equals(normalize_key("glue_owner"))) {
					glue_owner = fds[1].trim();
					logger.finest(String.format("glue_owner: %s", glue_owner));
					
				}  else if (parameter.equals(normalize_key("default_non_terminal"))) {
					default_non_terminal = "[" + fds[1].trim() + "]";
//					default_non_terminal = fds[1].trim();
					logger.finest(String.format("default_non_terminal: %s", default_non_terminal));
					
				} else if (parameter.equals(normalize_key("goalSymbol"))) {
					goal_symbol = "[" + fds[1].trim() + "]";
//					goal_symbol = fds[1].trim();
					logger.finest("goalSymbol: " + goal_symbol);
					
				} else if (parameter.equals(normalize_key("constrain_parse"))) {
					constrain_parse = Boolean.parseBoolean(fds[1]);

				} else if (parameter.equals(normalize_key("oov_feature_index"))) {
					oov_feature_index = Integer.parseInt(fds[1]);

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
					
				} else if (parameter.equals(normalize_key("add_combined_cost"))) {
					add_combined_cost = Boolean.valueOf(fds[1]);
					logger.finest(String.format("add_combined_cost: %s", add_combined_cost));
					
				} else if (parameter.equals(normalize_key("use_tree_nbest"))) {
					use_tree_nbest = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_tree_nbest: %s", use_tree_nbest));
					
				} else if (parameter.equals(normalize_key("escape_trees"))) {
					escape_trees = Boolean.valueOf(fds[1]);
					logger.finest(String.format("escape_trees: %s", escape_trees));
					
				} else if (parameter.equals(normalize_key("include_align_index"))) {
					include_align_index = Boolean.valueOf(fds[1]);
					logger.finest(String.format("include_align_index: %s", include_align_index));
					
				} else if (parameter.equals(normalize_key("top_n"))) {
					topN = Integer.parseInt(fds[1]);
					logger.finest(String.format("topN: %s", topN));
					
				} else if (parameter.equals(normalize_key("parallel_files_prefix"))) {
					Random random = new Random();
		            int v = random.nextInt(10000000);//make it random
					parallel_files_prefix = fds[1] + v;
					logger.info(String.format("parallel_files_prefix: %s", parallel_files_prefix));

				} else if (parameter.equals(normalize_key("num_parallel_decoders"))
                    || parameter.equals(normalize_key("threads"))) {
					num_parallel_decoders = Integer.parseInt(fds[1]);
					if (num_parallel_decoders <= 0) {
						throw new IllegalArgumentException("Must specify a positive number for num_parallel_decoders");
					}
					logger.finest(String.format("num_parallel_decoders: %s", num_parallel_decoders));
					
				} else if (parameter.equals(normalize_key("save_disk_hg"))) {
					save_disk_hg = Boolean.valueOf(fds[1]);
					logger.finest(String.format("save_disk_hg: %s", save_disk_hg));
					
				} else if (parameter.equals(normalize_key("use_kbest_hg"))) {
					use_kbest_hg = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_kbest_hg: %s", use_kbest_hg));
					
				} else if (parameter.equals(normalize_key("forest_pruning"))) {
					forest_pruning = Boolean.valueOf(fds[1]);
					logger.finest(String.format("forest_pruning: %s", forest_pruning));
					
				} else if (parameter.equals(normalize_key("forest_pruning_threshold"))) {
					forest_pruning_threshold = Double.parseDouble(fds[1]);
					logger.finest(String.format("forest_pruning_threshold: %s", forest_pruning_threshold));
				
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
					if(useCubePrune==false)
						logger.warning("useCubePrune=false");
					logger.finest(String.format("useCubePrune: %s", useCubePrune));				
				} else if (parameter.equals(normalize_key("useBeamAndThresholdPrune"))) {
					useBeamAndThresholdPrune = Boolean.valueOf(fds[1]);
					if(useBeamAndThresholdPrune==false)
						logger.warning("useBeamAndThresholdPrune=false");
					logger.finest(String.format("useBeamAndThresholdPrune: %s", useBeamAndThresholdPrune));				

				} else if (parameter.equals(normalize_key("oovFeatureCost"))) {
					oov_feature_cost = Float.parseFloat(fds[1]);
					logger.finest(String.format("oovFeatureCost: %s", oov_feature_cost));

				} else if (parameter.equals(normalize_key("useGoogleLinearCorpusGain"))) {
					useGoogleLinearCorpusGain = new Boolean(fds[1].trim());
					logger.finest(String.format("useGoogleLinearCorpusGain: %s", useGoogleLinearCorpusGain));					

				} else if (parameter.equals(normalize_key("googleBLEUWeights"))) {
					String[] googleWeights = fds[1].trim().split(";");
					if (googleWeights.length!=5){
						logger.severe("wrong line=" + line);
						System.exit(1);
					}
					linearCorpusGainThetas = new double[5];
					for(int i=0; i<5; i++)
						linearCorpusGainThetas[i] = new Double(googleWeights[i]);
					
					logger.finest(String.format("googleBLEUWeights: %s", linearCorpusGainThetas));		
					
				} else if (parameter.equals(normalize_key("oracleFile"))) {
					oracleFile = fds[1].trim();
					if (! new File(oracleFile).exists()) {
						logger.warning("FATAL: can't find oracle file '" + oracleFile + "'");
						System.exit(1);
					}

                } else if (parameter.equals("c") || parameter.equals("config")) {
                    // this was used to send in the config file, just ignore it
                    ;

				} else {
					logger.warning("FATAL: unknown configuration parameter '" + fds[0] + "'");
					System.exit(1);
				}
				
			} else {
                // Feature function.  These are processed a bit later
                // in JoshuaDecoder initialization, so we just set
                // them aside for now.

                features.add(line);
            }

				// if ("lm".equals(fds[0]) && fds.length == 2) { // lm  weight
				// 	if (new Double(fds[1].trim())!=0){
				// 		use_max_lm_cost_for_oov = true;
				// 	}
				// 	logger.info("useMaxLMCostForOOV=" + use_max_lm_cost_for_oov);
				// }
			}
			
		} finally { configReader.close(); }
		
        // This is for backwards compatibility of LM format.  If the
        // config file did not contain lines of the form "lm = ...",
        // then we create one from the handful of separately-specified
        // parameters.  These combined lines are later processed in
        // JoshuaDecoder as part of the multiple LM support
        if (lms.size() == 0) {
            String line = String.format("%s %d %b %b %.2f %s",
                lm_type, lm_order, use_left_equivalent_state, use_right_equivalent_state, lm_ceiling_cost, lm_file);
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

		if (useGoogleLinearCorpusGain) {
			if (linearCorpusGainThetas==null) {
				logger.info("linearCorpusGainThetas is null, did you set googleBLEUWeights properly?");
				System.exit(1);
			} else if (linearCorpusGainThetas.length!=5) {
				logger.info("linearCorpusGainThetas does not have five values, did you set googleBLEUWeights properly?");
				System.exit(1);
			}
		}
	}

	/**
	 * Checks for invalid variable configurations
	 */
	public static void sanityCheck() {
		if (pop_limit > 0 && useBeamAndThresholdPrune) {
			System.err.println("* FATAL: 'pop-limit' >= 0 is incompatible with 'useBeamAndThresholdPrune'");
			System.exit(0);
		}
	}

    /**
     * Normalizes parameter names by removing underscores and hyphens
     * and lowercasing.  This defines equivalence classes on external
     * use of parameter names, permitting arbitrary_under_scores and
     * camelCasing in paramter names without forcing the user to
     * memorize them all.  Here are some examples of equivalent ways
     * to refer to parameter names:
     *
     * {pop-limit, poplimit, PopLimit, popLimit, pop_lim_it}
     * {lmfile, lm-file, LM-FILE, lm_file}
     */
    public static String normalize_key(String text) {
        return text.replaceAll("[-_]", "").toLowerCase();
    }
}
