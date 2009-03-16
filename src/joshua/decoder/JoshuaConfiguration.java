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
import joshua.util.FileUtility;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Configuration file for Joshua decoder.
 * <p>
 * When adding new features to Joshua,
 * any new configurable parameters should be added to this class.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class JoshuaConfiguration {
	//lm config
	public static boolean use_srilm                  = false;
	public static boolean use_bloomfilter_lm         = false;
	public static double  lm_ceiling_cost            = 100;
	public static boolean use_left_equivalent_state  = false;
	public static boolean use_right_equivalent_state = true;
	public static int     g_lm_order                 = 3;
	public static boolean use_sent_specific_lm       = false;
	public static String  g_sent_lm_file_name_prefix = "lm.";
	public static String  lm_file                    = null;//TODO
	
	//tm config
	public static int span_limit = 10;
	//note: owner should be different from each other, it can have same value as a word in LM/TM
	public static String  phrase_owner               = "pt";
	public static String  mono_owner                 = "mono";
	public static String  begin_mono_owner           = "begin_mono";//if such a rule is get applied, then no reordering is possible
	//public static String untranslated_owner        = "<unt>";
	public static String  untranslated_owner         = phrase_owner;
	public static String  default_non_terminal       = "PHRASE";
	public static boolean use_sent_specific_tm       = false;
	public static String  g_sent_tm_file_name_prefix = "tm.";
	public static String  tm_file                    = null; // TODO
	
	// Parameters for suffix array grammar
	/** File name of source language training corpus. */
	public static String sa_source = null;
	
	/** File name of target language training corpus. */
	public static String sa_target = null;
	
	/** File name of source-target training corpus alignments. */
	public static String sa_alignment = null;
	
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
	
	//pruning config
	public static boolean use_cube_prune          = true;
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
	public static boolean add_combined_cost   = true; //in the nbest file, compute the final socre
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
	public static boolean forest_pruning           = false;
	public static double  forest_pruning_threshold = 10;
	
	//variational decoding
	public static boolean use_variational_decoding = false;
	
	//debug
	public static boolean extract_confusion_grammar = false; //non-parallel version
	public static String f_confusion_grammar = "C:\\Users\\zli\\Documents\\confusion.hg.grammar";
	//debug end
	
	
	//do we use a LM feature?
	public static boolean have_lm_model = false;
	
	private static final Logger logger =
		Logger.getLogger(JoshuaConfiguration.class.getName());
	
	public static void readConfigFile(String configFile) throws IOException {
		BufferedReader reader =
			FileUtility.getReadFileStream(configFile);
		String line;
		while ((line = FileUtility.read_line_lzf(reader)) != null) {
			//line = line.trim().toLowerCase();
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}
			
			if (line.indexOf("=") != -1) { // parameters; (not feature function)
				String[] fds = line.split("\\s*=\\s*");
				if (fds.length != 2) {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
				
				if (0 == fds[0].compareTo("lm_file")) {
					lm_file = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lm file: %s", lm_file));
					
				} else if (0 == fds[0].compareTo("tm_file")) {
					tm_file = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("tm file: %s", tm_file));
					
				} else if (0 == fds[0].compareTo("sa_source")) {
					sa_source = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array source file: %s", sa_source));
					
				} else if (0 == fds[0].compareTo("sa_target")) {
					sa_target = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array target file: %s", sa_target));
					
				} else if (0 == fds[0].compareTo("sa_alignment")) {
					sa_alignment = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array alignment file: %s", sa_alignment));
					
				} else if (0 == fds[0].compareTo("sa_max_phrase_span")) {
					sa_max_phrase_span = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum phrase span: %s", sa_max_phrase_span));
					
				} else if (0 == fds[0].compareTo("sa_max_phrase_length")) {
					sa_max_phrase_length = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum phrase length: %s", sa_max_phrase_length));
					
				} else if (0 == fds[0].compareTo("sa_max_phrase_length")) {
					sa_max_phrase_length = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum phrase length: %s", sa_max_phrase_length));
					
				} else if (0 == fds[0].compareTo("sa_max_nonterminals")) {
					sa_max_nonterminals = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array maximum number of nonterminals: %s", sa_max_nonterminals));
					
				} else if (0 == fds[0].compareTo("sa_min_nonterminal_span")) {
					sa_min_nonterminal_span = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array minimun nonterminal span: %s", sa_min_nonterminal_span));
					
				} else if (0 == fds[0].compareTo("sa_lex_sample_size")) {
					sa_lex_sample_size = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array sample size for lexical probability calculation: %s", sa_lex_sample_size));
					
				} else if (0 == fds[0].compareTo("sa_precalculate_lexprobs")) {
					sa_precalculate_lexprobs = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should lexical probabilities be precalculated: %s", sa_precalculate_lexprobs));
					
				} else if (0 == fds[0].compareTo("sa_rule_sample_size")) {
					sa_rule_sample_size = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array sample size for rules: %s", sa_rule_sample_size));
					
				} else if (0 == fds[0].compareTo("sa_rule_cache_size")) {
					sa_rule_cache_size = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("suffix array cache size for rules: %s", sa_rule_cache_size));
					
				} else if (0 == fds[0].compareTo("sa_sentence_initial_X")) {
					sa_sentence_initial_X = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should suffix array rule extraction allow rules from sentence-initial X: %s", sa_sentence_initial_X));
					
				} else if (0 == fds[0].compareTo("sa_sentence_final_X")) {
					sa_sentence_final_X = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("should suffix array rule extraction allow rules from sentence-final X: %s", sa_sentence_final_X));
					
				} else if (0 == fds[0].compareTo("use_srilm")) {
					use_srilm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_srilm: %s", use_srilm));
					
				} else if (0 == fds[0].compareTo("use_bloomfilter_lm")) {
					use_bloomfilter_lm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_bloomfilter_lm: %s", use_bloomfilter_lm));
					
				} else if (0 == fds[0].compareTo("lm_ceiling_cost")) {
					lm_ceiling_cost = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lm_ceiling_cost: %s", lm_ceiling_cost));
					
				} else if (0 == fds[0].compareTo("use_left_euqivalent_state") || 0 == fds[0].compareTo("use_left_equivalent_state")) {
					use_left_equivalent_state = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_left_equivalent_state: %s", use_left_equivalent_state));
					
				} else if (0 == fds[0].compareTo("use_right_euqivalent_state") || 0 == fds[0].compareTo("use_right_equivalent_state")) {
					use_right_equivalent_state = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_right_equivalent_state: %s", use_right_equivalent_state));
					
				} else if (0 == fds[0].compareTo("order")) {
					g_lm_order = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("g_lm_order: %s", g_lm_order));
					
				} else if (0 == fds[0].compareTo("use_sent_specific_lm")) {
					use_sent_specific_lm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_sent_specific_lm: %s", use_sent_specific_lm));
					
				} else if (0 == fds[0].compareTo("sent_lm_file_name_prefix")) {
					g_sent_lm_file_name_prefix = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("sent_lm_file_name_prefix: %s", g_sent_lm_file_name_prefix));
					
				} else if (0 == fds[0].compareTo("use_sent_specific_tm")) {
					use_sent_specific_tm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("use_sent_specific_tm: %s", use_sent_specific_tm));
					
				} else if (0 == fds[0].compareTo("sent_tm_file_name_prefix")) {
					g_sent_tm_file_name_prefix = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("sent_tm_file_name_prefix: %s", g_sent_tm_file_name_prefix));
					
				} else if (0 == fds[0].compareTo("span_limit")) {
					span_limit = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("span_limit: %s", span_limit));
					
				} else if (0 == fds[0].compareTo("phrase_owner")) {
					phrase_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("phrase_owner: %s", phrase_owner));
					
				} else if (0 == fds[0].compareTo("mono_owner")) {
					mono_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("mono_owner: %s", mono_owner));
					
				} else if (0 == fds[0].compareTo("begin_mono_owner")) {
					begin_mono_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("begin_mono_owner: %s", begin_mono_owner));
					
				} else if (0 == fds[0].compareTo("default_non_terminal")) {
					default_non_terminal = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("default_non_terminal: %s", default_non_terminal));
					
				} else if (0 == fds[0].compareTo("fuzz1")) {
					fuzz1 = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("fuzz1: %s", fuzz1));
					
				} else if (0 == fds[0].compareTo("fuzz2")) {
					fuzz2 = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("fuzz2: %s", fuzz2));
					
				} else if (0 == fds[0].compareTo("max_n_items")) {
					max_n_items = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("max_n_items: %s", max_n_items));
					
				} else if (0 == fds[0].compareTo("relative_threshold")) {
					relative_threshold = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("relative_threshold: %s", relative_threshold));
					
				} else if (0 == fds[0].compareTo("max_n_rules")) {
					max_n_rules = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("max_n_rules: %s", max_n_rules));
					
				} else if (0 == fds[0].compareTo("rule_relative_threshold")) {
					rule_relative_threshold = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("rule_relative_threshold: %s", rule_relative_threshold));
					
				} else if (0 == fds[0].compareTo("use_unique_nbest")) {
					use_unique_nbest = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));
					
				} else if (0 == fds[0].compareTo("add_combined_cost")) {
					add_combined_cost = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("add_combined_cost: %s", add_combined_cost));
					
				} else if (0 == fds[0].compareTo("use_tree_nbest")) {
					use_tree_nbest = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_tree_nbest: %s", use_tree_nbest));
					
				} else if (0 == fds[0].compareTo("include_align_index")) {
					include_align_index = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("include_align_index: %s", include_align_index));
					
				} else if (0 == fds[0].compareTo("top_n")) {
					topN = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("topN: %s", topN));
					
				} else if (0 == fds[0].compareTo("use_remote_lm_server")) {
					use_remote_lm_server = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_remote_lm_server: %s", use_remote_lm_server));
					
				} else if (0 == fds[0].compareTo("f_remote_server_list")) {
					f_remote_server_list = new String(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("f_remote_server_list: %s", f_remote_server_list));
					
				} else if (0 == fds[0].compareTo("num_remote_lm_servers")) {
					num_remote_lm_servers = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("num_remote_lm_servers: %s", num_remote_lm_servers));
					
				} else if (0 == fds[0].compareTo("remote_symbol_tbl")) {
					remote_symbol_tbl = new String(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("remote_symbol_tbl: %s", remote_symbol_tbl));
					
				} else if (0 == fds[0].compareTo("remote_lm_server_port")) {
					//port = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("remote_lm_server_port: not used"));
					
				} else if (0 == fds[0].compareTo("parallel_files_prefix")) {
					parallel_files_prefix = new String(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("parallel_files_prefix: %s", parallel_files_prefix));
					
				} else if (0 == fds[0].compareTo("num_parallel_decoders")) {
					num_parallel_decoders = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("num_parallel_decoders: %s", num_parallel_decoders));
					
				} else if (0 == fds[0].compareTo("save_disk_hg")) {
					save_disk_hg = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("save_disk_hg: %s", save_disk_hg));
					
				} else if (0 == fds[0].compareTo("forest_pruning")) {
					forest_pruning = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("forest_pruning: %s", forest_pruning));
					
				} else if (0 == fds[0].compareTo("forest_pruning_threshold")) {
					forest_pruning_threshold = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("forest_pruning_threshold: %s", forest_pruning_threshold));
					
				} else {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
			} else { // feature function
				String[] fds = line.split("\\s+");
				if (fds[0].compareTo("lm") == 0 && fds.length == 2) { // lm order weight
					have_lm_model = true;
					logger.info("you use a LM feature function, so make sure you have a LM grammar");
				} 
			}
		}
		reader.close();
	}
}
