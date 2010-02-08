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

import joshua.util.Cache;
import joshua.util.Regex;
import joshua.util.io.LineReader;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

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
	public static boolean use_srilm                  = false;
	public static boolean use_bloomfilter_lm         = false;
	public static boolean use_trie_lm                = false;
	public static double  lm_ceiling_cost            = 100;
	public static boolean use_left_equivalent_state  = false;
	public static boolean use_right_equivalent_state = true;
	public static int     lmOrder                 = 3;
	public static boolean use_sent_specific_lm       = false;
	public static String  g_sent_lm_file_name_prefix = "lm.";
	public static String  lm_file                    = null;//TODO
	public static int ngramStateID = 0;//TODO?????????????
	
	//tm config
	public static int span_limit = 10;
	//note: owner should be different from each other, it can have same value as a word in LM/TM
	public static String  phrase_owner               = "pt";
	public static String  glue_owner           = "glue_owner";//if such a rule is get applied, then no reordering is possible
	public static String  default_non_terminal       = "PHRASE";
	public static String  goal_symbol                = "S";
	public static boolean use_sent_specific_tm       = false;
	public static String  g_sent_tm_file_name_prefix = "tm.";
	public static float   oovFeatureCost = 100;
	
	public static String  tm_file                    = null;
	// TODO: default to glue grammar provided with Joshua
	// TODO: support multiple glue grammars
	public static String  glue_file                  = null;
	
	public static String tm_format                   = null;
	public static String glue_format                 = null;
	
	// Parameters for suffix array grammar
//	/** File name prefix for source language binary training files. */
//	public static String sa_source = null;
//	
//	/** File name prefix for source language binary training files. */
//	public static String sa_target = null;
//	
//	/** File name of source-target training corpus alignments. */
//	public static String sa_alignment = null;
	
	public static int     sa_max_phrase_span       = 10;
	public static int     sa_max_phrase_length     = 10;
	public static int     sa_max_nonterminals      = 2;
	public static int     sa_min_nonterminal_span  = 2;
	public static int     sa_lex_sample_size       = 1000;
	public static int     sa_lex_cache_size        = Cache.DEFAULT_CAPACITY;
	public static boolean sa_precalculate_lexprobs = false;
	public static int     sa_rule_sample_size      = 300;
	public static int     sa_rule_cache_size       = 1000;
	public static boolean sa_sentence_initial_X    = true;
	public static boolean sa_sentence_final_X      = true;
	public static boolean sa_edgeXMayViolatePhraseSpan = true;
	public static float   sa_lex_floor_prob        = Float.MIN_VALUE;
	
	// TODO: introduce the various corpus/tm file package formats
//	public static String sa_vocab_suffix = "vocab";
//	public static String sa_corpus_suffix = "corpus";
//	public static String sa_suffixes_suffix = "suffixes";
	
	
	//pruning config
	//note we can use both cube pruning and "beamAndThreshold" pruning
	public static boolean useCubePrune          = true;
	public static boolean useBeamAndThresholdPrune = true;
	public static double  fuzz1                   = 0.1;
	public static double  fuzz2                   = 0.1;
	public static int     max_n_items             = 30;
	public static double  relative_threshold      = 10.0;
	public static int     max_n_rules             = 50;
	public static double  rule_relative_threshold = 10.0;
	
	//nbest config
	public static boolean use_unique_nbest    = false;
	public static boolean use_tree_nbest      = false;
	public static boolean include_align_index = false;
	public static boolean add_combined_cost   = true; //in the nbest file, compute the final score
	public static int topN = 500;
	
	//remote lm server
	public static boolean use_remote_lm_server = false;
	public static String  remote_symbol_tbl    = "null"; //this file will first be created by remote_lm_server, and read by remote_suffix_server and the decoder
	public static int    num_remote_lm_servers = 1;
	public static String f_remote_server_list  = "null";
	
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
	public static String f_confusion_grammar = "C:\\Users\\zli\\Documents\\confusion.hg.grammar";
	//debug end
	
	//do we use a LM feature?
	public static boolean have_lm_model = false;
	
	public static String segmentFileParserClass = null;//PlainSegmentParser, HackishSegmentParser, SAXSegmentParser
	
	
	//discriminative model options
	public static boolean useTMFeat = true;
	public static boolean useRuleIDName = false;
	public static boolean useLMFeat = true;
	public static boolean useTMTargetFeat = true;
	public static boolean useEdgeNgramOnly = false;
	public static int startNgramOrder = 1;
	public static int endNgramOrder = 2;
	
	public static boolean useMicroTMFeat = true;
	public static String wordMapFile;/*tbl for mapping rule words*/

	
	
	//=== use goolge linear corpus gain?
	public static boolean useGoogleLinearCorpusGain = false;
	public static double[] linearCorpusGainThetas = null;
	
	
	private static final Logger logger =
		Logger.getLogger(JoshuaConfiguration.class.getName());
	
	
//===============================================================
// Methods
//===============================================================
	
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
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lm file: %s", lm_file));
					
				} else if ("tm_file".equals(fds[0])) {
					tm_file = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("tm file: %s", tm_file));
				
				} else if ("glue_file".equals(fds[0])) {
					glue_file = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("glue file: %s", glue_file));
				
				} else if ("tm_format".equals(fds[0])) {
					tm_format = fds[1].trim();
						
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("tm format: %s", tm_format));

				} else if ("glue_format".equals(fds[0])) {
					glue_format = fds[1].trim();
						
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("glue format: %s", glue_format));
					
//				} else if ("sa_source".equals(fds[0])) {
//					sa_source = fds[1].trim();
//					if (logger.isLoggable(Level.FINEST))
//						logger.finest(String.format("suffix array source file: %s", sa_source));
//					
//				} else if ("sa_target".equals(fds[0])) {
//					sa_target = fds[1].trim();
//					if (logger.isLoggable(Level.FINEST))
//						logger.finest(String.format("suffix array target file: %s", sa_target));
//					
//				} else if ("sa_alignment".equals(fds[0])) {
//					sa_alignment = fds[1].trim();
//					if (logger.isLoggable(Level.FINEST))
//						logger.finest(String.format("suffix array alignment file: %s", sa_alignment));
//					
				} else if ("sa_max_phrase_span".equals(fds[0])) {
					sa_max_phrase_span = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum phrase span: %s", sa_max_phrase_span));
					
				} else if ("sa_max_phrase_length".equals(fds[0])) {
					sa_max_phrase_length = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum phrase length: %s", sa_max_phrase_length));
					
				} else if ("sa_max_phrase_length".equals(fds[0])) {
					sa_max_phrase_length = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum phrase length: %s", sa_max_phrase_length));
					
				} else if ("sa_max_nonterminals".equals(fds[0])) {
					sa_max_nonterminals = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum number of nonterminals: %s", sa_max_nonterminals));
					
				} else if ("sa_min_nonterminal_span".equals(fds[0])) {
					sa_min_nonterminal_span = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array minimun nonterminal span: %s", sa_min_nonterminal_span));
					
				} else if ("sa_lex_sample_size".equals(fds[0])) {
					sa_lex_sample_size = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array sample size for lexical probability calculation: %s", sa_lex_sample_size));
					
				} else if ("sa_precalculate_lexprobs".equals(fds[0])) {
					sa_precalculate_lexprobs = Boolean.valueOf(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should lexical probabilities be precalculated: %s", sa_precalculate_lexprobs));
					
				} else if ("sa_rule_sample_size".equals(fds[0])) {
					sa_rule_sample_size = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array sample size for rules: %s", sa_rule_sample_size));
					
				} else if ("sa_rule_cache_size".equals(fds[0])) {
					sa_rule_cache_size = Integer.parseInt(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array cache size for rules: %s", sa_rule_cache_size));
					
				} else if ("sa_sentence_initial_X".equals(fds[0])) {
					sa_sentence_initial_X = Boolean.valueOf(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should suffix array rule extraction allow rules from sentence-initial X: %s", sa_sentence_initial_X));
					
				} else if ("sa_sentence_final_X".equals(fds[0])) {
					sa_sentence_final_X = Boolean.valueOf(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should suffix array rule extraction allow rules from sentence-final X: %s", sa_sentence_final_X));
					
				} else if ("sa_edgeXMayViolatePhraseSpan".equals(fds[0])) {
					sa_edgeXMayViolatePhraseSpan = Boolean.valueOf(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should suffix array rule extraction allow rules where sa_edgeXMayViolatePhraseSpan: %s", sa_edgeXMayViolatePhraseSpan));
				} else if ("sa_lex_floor_prob".equals(fds[0])) {
					sa_lex_floor_prob = Float.valueOf(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("floor value for probabilities returned as lexical transaltion probabilities: %s", sa_lex_floor_prob));
				} else if ("use_srilm".equals(fds[0])) {
					use_srilm = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_srilm: %s", use_srilm));
					
				} else if ("use_bloomfilter_lm".equals(fds[0])) {
					use_bloomfilter_lm = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_bloomfilter_lm: %s", use_bloomfilter_lm));
					
				} else if ("use_trie_lm".equals(fds[0])) {
					use_trie_lm = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_trie_lm: %s", use_trie_lm));
					
				} else if ("lm_ceiling_cost".equals(fds[0])) {
					lm_ceiling_cost = Double.parseDouble(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lm_ceiling_cost: %s", lm_ceiling_cost));
					
				// BUG: accepting typos in config file is not acceptable
				} else if ("use_left_euqivalent_state".equals(fds[0])) {
					use_left_equivalent_state = Boolean.parseBoolean(fds[1]);
					
					logger.warning("Misspelling in configuration file: 'use_right_euqivalent_state'");
					
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_left_equivalent_state: %s", use_left_equivalent_state));
				
				// BUG: accepting typos in config file is not acceptable
				} else if ("use_right_euqivalent_state".equals(fds[0])) {
					use_right_equivalent_state = Boolean.parseBoolean(fds[1]);
					
					logger.warning("Misspelling in configuration file: 'use_right_euqivalent_state'");
					
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_right_equivalent_state: %s", use_right_equivalent_state));
					
				} else if ("use_left_equivalent_state".equals(fds[0])) {
					use_left_equivalent_state = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_left_equivalent_state: %s", use_left_equivalent_state));
				
				} else if ("use_right_equivalent_state".equals(fds[0])) {
					use_right_equivalent_state = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_right_equivalent_state: %s", use_right_equivalent_state));
					
				} else if ("order".equals(fds[0])) {
					lmOrder = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("g_lm_order: %s", lmOrder));
					
				} else if ("use_sent_specific_lm".equals(fds[0])) {
					use_sent_specific_lm = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_sent_specific_lm: %s", use_sent_specific_lm));
					
				} else if ("sent_lm_file_name_prefix".equals(fds[0])) {
					g_sent_lm_file_name_prefix = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("sent_lm_file_name_prefix: %s", g_sent_lm_file_name_prefix));
					
				} else if ("use_sent_specific_tm".equals(fds[0])) {
					use_sent_specific_tm = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_sent_specific_tm: %s", use_sent_specific_tm));
					
				} else if ("sent_tm_file_name_prefix".equals(fds[0])) {
					g_sent_tm_file_name_prefix = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("sent_tm_file_name_prefix: %s", g_sent_tm_file_name_prefix));
					
				} else if ("span_limit".equals(fds[0])) {
					span_limit = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("span_limit: %s", span_limit));
					
				} else if ("phrase_owner".equals(fds[0])) {
					phrase_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("phrase_owner: %s", phrase_owner));
					
				} else if ("glue_owner".equals(fds[0])) {
					glue_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("glue_owner: %s", glue_owner));
					
				}  else if ("default_non_terminal".equals(fds[0])) {
					default_non_terminal = "[" + fds[1].trim() + "]";
//					default_non_terminal = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("default_non_terminal: %s", default_non_terminal));
					
				} else if ("goalSymbol".equals(fds[0])) {
					goal_symbol = "[" + fds[1].trim() + "]";
//					goal_symbol = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("goalSymbol: %s", goal_symbol));
					
				} else if ("fuzz1".equals(fds[0])) {
					fuzz1 = Double.parseDouble(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("fuzz1: %s", fuzz1));
					
				} else if ("fuzz2".equals(fds[0])) {
					fuzz2 = Double.parseDouble(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("fuzz2: %s", fuzz2));
					
				} else if ("max_n_items".equals(fds[0])) {
					max_n_items = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("max_n_items: %s", max_n_items));
					
				} else if ("relative_threshold".equals(fds[0])) {
					relative_threshold = Double.parseDouble(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("relative_threshold: %s", relative_threshold));
					
				} else if ("max_n_rules".equals(fds[0])) {
					max_n_rules = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("max_n_rules: %s", max_n_rules));
					
				} else if ("rule_relative_threshold".equals(fds[0])) {
					rule_relative_threshold = Double.parseDouble(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("rule_relative_threshold: %s", rule_relative_threshold));
					
				} else if ("use_unique_nbest".equals(fds[0])) {
					use_unique_nbest = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));
					
				} else if ("add_combined_cost".equals(fds[0])) {
					add_combined_cost = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("add_combined_cost: %s", add_combined_cost));
					
				} else if ("use_tree_nbest".equals(fds[0])) {
					use_tree_nbest = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_tree_nbest: %s", use_tree_nbest));
					
				} else if ("include_align_index".equals(fds[0])) {
					include_align_index = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("include_align_index: %s", include_align_index));
					
				} else if ("top_n".equals(fds[0])) {
					topN = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("topN: %s", topN));
					
				} else if ("use_remote_lm_server".equals(fds[0])) {
					use_remote_lm_server = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_remote_lm_server: %s", use_remote_lm_server));
					
				} else if ("f_remote_server_list".equals(fds[0])) {
					f_remote_server_list = fds[1];
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("f_remote_server_list: %s", f_remote_server_list));
					
				} else if ("num_remote_lm_servers".equals(fds[0])) {
					num_remote_lm_servers = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("num_remote_lm_servers: %s", num_remote_lm_servers));
					
				} else if ("remote_symbol_tbl".equals(fds[0])) {
					remote_symbol_tbl = fds[1]; 
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("remote_symbol_tbl: %s", remote_symbol_tbl));
					
				} else if ("remote_lm_server_port".equals(fds[0])) {
					//port = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("remote_lm_server_port: not used"));
					
				} else if ("parallel_files_prefix".equals(fds[0])) {
					Random random = new Random();
		            int v = random.nextInt(10000000);//make it random
					parallel_files_prefix = fds[1] + v;
					logger.info(String.format("parallel_files_prefix: %s", parallel_files_prefix));
				} else if ("num_parallel_decoders".equals(fds[0])) {
					num_parallel_decoders = Integer.parseInt(fds[1]);
					if (num_parallel_decoders <= 0) {
						throw new IllegalArgumentException("Must specify a positive number for num_parallel_decoders");
					}
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("num_parallel_decoders: %s", num_parallel_decoders));
					
				} else if ("save_disk_hg".equals(fds[0])) {
					save_disk_hg = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("save_disk_hg: %s", save_disk_hg));
					
				} else if ("use_kbest_hg".equals(fds[0])) {
					use_kbest_hg = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_kbest_hg: %s", use_kbest_hg));
					
				} else if ("forest_pruning".equals(fds[0])) {
					forest_pruning = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("forest_pruning: %s", forest_pruning));
					
				} else if ("forest_pruning_threshold".equals(fds[0])) {
					forest_pruning_threshold = Double.parseDouble(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("forest_pruning_threshold: %s", forest_pruning_threshold));
				
				} else if ("visualize_hypergraph".equals(fds[0])) {
					visualize_hypergraph = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("visualize_hypergraph: %s", visualize_hypergraph));
				} else if ("segment_file_parser_class".equals(fds[0])) {
					segmentFileParserClass = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest("segmentFileParserClass: " + segmentFileParserClass);
					
				} else if ("useCubePrune".equals(fds[0])) {
					useCubePrune = Boolean.valueOf(fds[1]);
					if(useCubePrune==false)
						logger.warning("useCubePrune=false");
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useCubePrune: %s", useCubePrune));				
				}else if ("useBeamAndThresholdPrune".equals(fds[0])) {
					useBeamAndThresholdPrune = Boolean.valueOf(fds[1]);
					if(useBeamAndThresholdPrune==false)
						logger.warning("useBeamAndThresholdPrune=false");
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useBeamAndThresholdPrune: %s", useBeamAndThresholdPrune));				
				} else if ("oovFeatureCost".equals(fds[0])) {
					oovFeatureCost = Float.parseFloat(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("oovFeatureCost: %s", oovFeatureCost));
				} else if ("useTMFeat".equals(fds[0])) {
					useTMFeat = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useTMFeat: %s", useTMFeat));
				} else if ("useLMFeat".equals(fds[0])) {
					useLMFeat = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useLMFeat: %s", useLMFeat));
				} else if ("useMicroTMFeat".equals(fds[0])) {
					useMicroTMFeat = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useMicroTMFeat: %s", useMicroTMFeat));					
				} else if ("wordMapFile".equals(fds[0])) {
					wordMapFile = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("wordMapFile: %s", wordMapFile));					
				} else if ("useRuleIDName".equals(fds[0])) {
					useRuleIDName = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useRuleIDName: %s", useRuleIDName));					
				}else if ("startNgramOrder".equals(fds[0])) {
					startNgramOrder = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("startNgramOrder: %s", startNgramOrder));
				} else if ("endNgramOrder".equals(fds[0])) {
					endNgramOrder = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("endNgramOrder: %s", endNgramOrder));
				}else if ("useEdgeNgramOnly".equals(fds[0])) {
					useEdgeNgramOnly = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useEdgeNgramOnly: %s", useEdgeNgramOnly));
				}else if ("useTMTargetFeat".equals(fds[0])) {
					useTMTargetFeat = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useTMTargetFeat: %s", useTMTargetFeat));
				} else if ("useGoogleLinearCorpusGain".equals(fds[0])) {
					useGoogleLinearCorpusGain = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useGoogleLinearCorpusGain: %s", useGoogleLinearCorpusGain));					
				} else if ("googleBLEUWeights".equals(fds[0])) {
					String[] googleWeights = fds[1].trim().split(";");
					if(googleWeights.length!=5){
						logger.severe("wrong line=" + line);
						System.exit(1);
					}
					linearCorpusGainThetas = new double[5];
					for(int i=0; i<5; i++)
						linearCorpusGainThetas[i] = new Double(googleWeights[i]);
					
					
					logger.finest(String.format("googleBLEUWeights: %s", linearCorpusGainThetas));		
					
				} else {
					logger.warning("Maybe Wrong config line: " + line);
				}
				
				
			} else { // feature function
				String[] fds = Regex.spaces.split(line);
				if ("lm".equals(fds[0]) && fds.length == 2) { // lm order weight
					have_lm_model = true;
					logger.info("you use a LM feature function, so make sure you have a LM grammar");
				} 
			}
			
			
			
		} } finally { configReader.close(); }
		
		if( useGoogleLinearCorpusGain  ){
			if( linearCorpusGainThetas==null){
				logger.info("linearCorpusGainThetas is null, did you set googleBLEUWeights properly?");
				System.exit(1);
			}else if( linearCorpusGainThetas.length!=5){
				logger.info("linearCorpusGainThetas does not have five values, did you set googleBLEUWeights properly?");
				System.exit(1);
			}
		}
	}
}
