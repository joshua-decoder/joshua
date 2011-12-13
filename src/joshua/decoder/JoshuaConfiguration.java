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
	public static String  glue_owner                 = "glue_owner";//if such a rule is get applied, then no reordering is possible
	public static String  default_non_terminal       = "PHRASE";
	public static String  goal_symbol                = "S";
	public static boolean use_sent_specific_tm       = false;
	public static boolean keep_sent_specific_tm      = false;
	
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

	// Cube pruning is always on, with a span-level pop limit of 1000.
	// Beam and threshold pruning can be enabled, which also changes
	// the nature of cube pruning so that the pop limit is no longer
	// used.  If both are turned off, exhaustive pruning takes effect.
    public static int     pop_limit                = 1000;
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
	
	//variational decoding
	public static boolean use_variational_decoding = false;
	
	//debug
	public static boolean extract_confusion_grammar = false; //non-parallel version
	public static String  f_confusion_grammar       = "C:\\Users\\zli\\Documents\\confusion.hg.grammar";
	//debug end
	
	// do we use a LM feature?
	public static boolean have_lm_model = false;

	
	public static String segmentFileParserClass = null;//PlainSegmentParser, HackishSegmentParser, SAXSegmentParser
	
	
	// discriminative model options
	public static boolean useTMFeat = true;
	public static boolean useRuleIDName = false;
	public static boolean useLMFeat = true;
	public static boolean useTMTargetFeat = true;
	public static boolean useEdgeNgramOnly = false;
	public static int startNgramOrder = 1;
	public static int endNgramOrder = 2;
	
	public static boolean useMicroTMFeat = true;
	public static String wordMapFile;/*tbl for mapping rule words*/
	
	// use google linear corpus gain?
	public static boolean useGoogleLinearCorpusGain = false;
	public static double[] linearCorpusGainThetas = null;
	public static boolean mark_oovs = true;
	
	// used to extract oracle hypotheses from the forest
	public static String oracleFile = "";
	
	private static final Logger logger =
		Logger.getLogger(JoshuaConfiguration.class.getName());
	
	
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

	// This is static instead of a constructor because all the fields are static. Yuck.
	public static void readConfigFile(String configFile) throws IOException {
		
		LineReader configReader = new LineReader(configFile);
		try { for (String line : configReader) {
			line = line.trim(); // .toLowerCase();
			if (Regex.commentOrEmptyLine.matches(line)) continue;
			
			
			if (line.indexOf("=") != -1) { // parameters; (not feature function)
				String[] fds = Regex.equalsWithSpaces.split(line);
				if (fds.length != 2) {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
			
				if ("lm_file".equals(fds[0])) {
					lm_file = fds[1].trim();
					logger.finest(String.format("lm file: %s", lm_file));
				} else if ("tm_file".equals(fds[0])) {
					tm_file = fds[1].trim();
					logger.finest(String.format("tm file: %s", tm_file));
				
				} else if ("glue_file".equals(fds[0])) {
					glue_file = fds[1].trim();
					logger.finest(String.format("glue file: %s", glue_file));
				
				} else if ("tm_format".equals(fds[0])) {
					tm_format = fds[1].trim();
						
					logger.finest(String.format("tm format: %s", tm_format));

				} else if ("glue_format".equals(fds[0])) {
					glue_format = fds[1].trim();
						
					logger.finest(String.format("glue format: %s", glue_format));
				} else if ("lm_type".equals(fds[0])) {
					lm_type = String.valueOf(fds[1]);
					if (! lm_type.equals("kenlm") && ! lm_type.equals("berkeleylm") && ! lm_type.equals("javalm")) {
						System.err.println("* FATAL: lm_type '" + lm_type + "' not supported");
						System.err.println("* supported types are 'kenlm' (default), 'berkeleylm', and 'javalm' (not recommended)");
						System.exit(1);
					}
					
				} else if ("lm_ceiling_cost".equals(fds[0])) {
					lm_ceiling_cost = Double.parseDouble(fds[1]);
					logger.finest(String.format("lm_ceiling_cost: %s", lm_ceiling_cost));
					
				} else if ("use_left_equivalent_state".equals(fds[0])) {
					use_left_equivalent_state = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_left_equivalent_state: %s", use_left_equivalent_state));
				
				} else if ("use_right_equivalent_state".equals(fds[0])) {
					use_right_equivalent_state = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_right_equivalent_state: %s", use_right_equivalent_state));
					
				} else if ("order".equals(fds[0])) {
					lm_order = Integer.parseInt(fds[1]);
					logger.finest(String.format("g_lm_order: %s", lm_order));
					
				} else if ("use_sent_specific_lm".equals(fds[0])) {
					use_sent_specific_lm = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_sent_specific_lm: %s", use_sent_specific_lm));
					
				} else if ("use_sent_specific_tm".equals(fds[0])) {
					use_sent_specific_tm = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_sent_specific_tm: %s", use_sent_specific_tm));

				} else if ("keep_sent_specific_tm".equals(fds[0])) {
					keep_sent_specific_tm = Boolean.valueOf(fds[1]);
					logger.finest(String.format("keep_sent_specific_tm: %s", use_sent_specific_tm));
					
				} else if ("span_limit".equals(fds[0])) {
					span_limit = Integer.parseInt(fds[1]);
					logger.finest(String.format("span_limit: %s", span_limit));
					
				} else if ("phrase_owner".equals(fds[0])) {
					phrase_owner = fds[1].trim();
					logger.finest(String.format("phrase_owner: %s", phrase_owner));
					
				} else if ("glue_owner".equals(fds[0])) {
					glue_owner = fds[1].trim();
					logger.finest(String.format("glue_owner: %s", glue_owner));
					
				}  else if ("default_non_terminal".equals(fds[0])) {
					default_non_terminal = "[" + fds[1].trim() + "]";
//					default_non_terminal = fds[1].trim();
					logger.finest(String.format("default_non_terminal: %s", default_non_terminal));
					
				} else if ("goalSymbol".equals(fds[0]) || "goal_symbol".equals(fds[0]) ) {
					goal_symbol = "[" + fds[1].trim() + "]";
//					goal_symbol = fds[1].trim();
					logger.finest("goalSymbol: " + goal_symbol);
					
				} else if ("constrain_parse".equals(fds[0])) {
					constrain_parse = Boolean.parseBoolean(fds[1]);

				} else if ("oov_feature_index".equals(fds[0])) {
					oov_feature_index = Integer.parseInt(fds[1]);

				} else if ("use_pos_labels".equals(fds[0])) {
					use_pos_labels = Boolean.parseBoolean(fds[1]);
					
				} else if ("fuzz1".equals(fds[0])) {
					fuzz1 = Double.parseDouble(fds[1]);
					logger.finest(String.format("fuzz1: %s", fuzz1));
					
				} else if ("fuzz2".equals(fds[0])) {
					fuzz2 = Double.parseDouble(fds[1]);
					logger.finest(String.format("fuzz2: %s", fuzz2));
					
				} else if ("max_n_items".equals(fds[0])) {
					max_n_items = Integer.parseInt(fds[1]);
					logger.finest(String.format("max_n_items: %s", max_n_items));
					
				} else if ("relative_threshold".equals(fds[0])) {
					relative_threshold = Double.parseDouble(fds[1]);
					logger.finest(String.format("relative_threshold: %s", relative_threshold));
					
				} else if ("max_n_rules".equals(fds[0])) {
					max_n_rules = Integer.parseInt(fds[1]);
					logger.finest(String.format("max_n_rules: %s", max_n_rules));
					
				} else if ("use_unique_nbest".equals(fds[0])) {
					use_unique_nbest = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));
					
				} else if ("add_combined_cost".equals(fds[0])) {
					add_combined_cost = Boolean.valueOf(fds[1]);
					logger.finest(String.format("add_combined_cost: %s", add_combined_cost));
					
				} else if ("use_tree_nbest".equals(fds[0])) {
					use_tree_nbest = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_tree_nbest: %s", use_tree_nbest));
					
				} else if ("escape_trees".equals(fds[0])) {
					escape_trees = Boolean.valueOf(fds[1]);
					logger.finest(String.format("escape_trees: %s", escape_trees));
					
				} else if ("include_align_index".equals(fds[0])) {
					include_align_index = Boolean.valueOf(fds[1]);
					logger.finest(String.format("include_align_index: %s", include_align_index));
					
				} else if ("top_n".equals(fds[0])) {
					topN = Integer.parseInt(fds[1]);
					logger.finest(String.format("topN: %s", topN));
					
				} else if ("parallel_files_prefix".equals(fds[0])) {
					Random random = new Random();
		            int v = random.nextInt(10000000);//make it random
					parallel_files_prefix = fds[1] + v;
					logger.info(String.format("parallel_files_prefix: %s", parallel_files_prefix));

				} else if ("num_parallel_decoders".equals(fds[0]) || "threads".equals(fds[0]) ) {
					num_parallel_decoders = Integer.parseInt(fds[1]);
					if (num_parallel_decoders <= 0) {
						throw new IllegalArgumentException("Must specify a positive number for num_parallel_decoders");
					}
					logger.finest(String.format("num_parallel_decoders: %s", num_parallel_decoders));
					
				} else if ("save_disk_hg".equals(fds[0])) {
					save_disk_hg = Boolean.valueOf(fds[1]);
					logger.finest(String.format("save_disk_hg: %s", save_disk_hg));
					
				} else if ("use_kbest_hg".equals(fds[0])) {
					use_kbest_hg = Boolean.valueOf(fds[1]);
					logger.finest(String.format("use_kbest_hg: %s", use_kbest_hg));
					
				} else if ("forest_pruning".equals(fds[0])) {
					forest_pruning = Boolean.valueOf(fds[1]);
					logger.finest(String.format("forest_pruning: %s", forest_pruning));
					
				} else if ("forest_pruning_threshold".equals(fds[0])) {
					forest_pruning_threshold = Double.parseDouble(fds[1]);
					logger.finest(String.format("forest_pruning_threshold: %s", forest_pruning_threshold));
				
				} else if ("visualize_hypergraph".equals(fds[0])) {
					visualize_hypergraph = Boolean.valueOf(fds[1]);
					logger.finest(String.format("visualize_hypergraph: %s", visualize_hypergraph));
					
				} else if ("mark_oovs".equals(fds[0])) {
					mark_oovs = Boolean.valueOf(fds[1]);
					logger.finest(String.format("mark_oovs: %s", mark_oovs));
					
				} else if ("segment_file_parser_class".equals(fds[0])) {
					segmentFileParserClass = fds[1].trim();
					logger.finest("segmentFileParserClass: " + segmentFileParserClass);
					
                } else if ("pop-limit".equals(fds[0])) {
                    pop_limit = Integer.valueOf(fds[1]);
                    logger.finest(String.format("pop-limit: %s", pop_limit)); 

				} else if ("useCubePrune".equals(fds[0])) {
					useCubePrune = Boolean.valueOf(fds[1]);
					if(useCubePrune==false)
						logger.warning("useCubePrune=false");
					logger.finest(String.format("useCubePrune: %s", useCubePrune));				
				}else if ("useBeamAndThresholdPrune".equals(fds[0])) {
					useBeamAndThresholdPrune = Boolean.valueOf(fds[1]);
					if(useBeamAndThresholdPrune==false)
						logger.warning("useBeamAndThresholdPrune=false");
					logger.finest(String.format("useBeamAndThresholdPrune: %s", useBeamAndThresholdPrune));				
				} else if ("oovFeatureCost".equals(fds[0])) {
					oov_feature_cost = Float.parseFloat(fds[1]);
					logger.finest(String.format("oovFeatureCost: %s", oov_feature_cost));
				} else if ("useTMFeat".equals(fds[0])) {
					useTMFeat = Boolean.valueOf(fds[1]);
					logger.finest(String.format("useTMFeat: %s", useTMFeat));

				} else if ("useLMFeat".equals(fds[0])) {
					useLMFeat = Boolean.valueOf(fds[1]);
					logger.finest(String.format("useLMFeat: %s", useLMFeat));

				} else if ("useMicroTMFeat".equals(fds[0])) {
					useMicroTMFeat = new Boolean(fds[1].trim());
					logger.finest(String.format("useMicroTMFeat: %s", useMicroTMFeat));					
				} else if ("wordMapFile".equals(fds[0])) {
					wordMapFile = fds[1].trim();
					logger.finest(String.format("wordMapFile: %s", wordMapFile));					
				} else if ("useRuleIDName".equals(fds[0])) {
					useRuleIDName = new Boolean(fds[1].trim());
					logger.finest(String.format("useRuleIDName: %s", useRuleIDName));					
				} else if ("startNgramOrder".equals(fds[0])) {
					startNgramOrder = Integer.parseInt(fds[1]);
					logger.finest(String.format("startNgramOrder: %s", startNgramOrder));
				} else if ("endNgramOrder".equals(fds[0])) {
					endNgramOrder = Integer.parseInt(fds[1]);
					logger.finest(String.format("endNgramOrder: %s", endNgramOrder));
				}else if ("useEdgeNgramOnly".equals(fds[0])) {
					useEdgeNgramOnly = Boolean.valueOf(fds[1]);
					logger.finest(String.format("useEdgeNgramOnly: %s", useEdgeNgramOnly));
				}else if ("useTMTargetFeat".equals(fds[0])) {
					useTMTargetFeat = Boolean.valueOf(fds[1]);
					logger.finest(String.format("useTMTargetFeat: %s", useTMTargetFeat));
				} else if ("useGoogleLinearCorpusGain".equals(fds[0])) {
					useGoogleLinearCorpusGain = new Boolean(fds[1].trim());
					logger.finest(String.format("useGoogleLinearCorpusGain: %s", useGoogleLinearCorpusGain));					
				} else if ("googleBLEUWeights".equals(fds[0])) {
					String[] googleWeights = fds[1].trim().split(";");
					if (googleWeights.length!=5){
						logger.severe("wrong line=" + line);
						System.exit(1);
					}
					linearCorpusGainThetas = new double[5];
					for(int i=0; i<5; i++)
						linearCorpusGainThetas[i] = new Double(googleWeights[i]);
					
					logger.finest(String.format("googleBLEUWeights: %s", linearCorpusGainThetas));		
					
				} else if ("oracleFile".equals(fds[0])) {
					oracleFile = fds[1].trim();
					if (! new File(oracleFile).exists()) {
						logger.warning("FATAL: can't find oracle file '" + oracleFile + "'");
						System.exit(1);
					}
				} else {
					logger.warning("WARNING: unknown configuration parameter '" + fds[0] + "'");
					// System.exit(1);
				}
				
				
			} else { // feature function
				String[] fds = Regex.spaces.split(line);
				if ("lm".equals(fds[0]) && fds.length == 2) { // lm  weight
					have_lm_model = true;
					if (new Double(fds[1].trim())!=0){
						use_max_lm_cost_for_oov = true;
					}
					logger.info("useMaxLMCostForOOV=" + use_max_lm_cost_for_oov);
				} 
			}
			
			} 
		} finally { configReader.close(); }
		
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
}
