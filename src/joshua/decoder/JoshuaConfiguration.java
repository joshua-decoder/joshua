package joshua.decoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.util.FileUtility;


/* anything that is configurable should go here
 * */

public class JoshuaConfiguration {
	//lm config
	public static boolean use_srilm                  = false;
	public static double  lm_ceiling_cost            = 100;
	public static boolean use_left_euqivalent_state  = false;
	public static boolean use_right_euqivalent_state = true;
	public static int     g_lm_order                 = 3;
	public static boolean use_sent_specific_lm       = false;
	public static String  g_sent_lm_file_name_prefix = "lm.";
	public static String lm_file = null;//TODO	
	
	//tm config
	public static int span_limit = 10;
	//note: owner should be different from each other, it can have same value as a word in LM/TM
	public static String  phrase_owner               = "pt";
	public static String  mono_owner                 = "mono";
	public static String  begin_mono_owner           = "begin_mono";//if such a rule is get applied, then no reordering is possible
	//public static String untranslated_owner          = "<unt>";
	public static String untranslated_owner          = phrase_owner;
	public static String  default_non_terminal       = "PHRASE";
	public static boolean use_sent_specific_tm       = false;
	public static String  g_sent_tm_file_name_prefix = "tm.";
	public static String tm_file = null;//TODO
	
	//pruning config
	public static boolean use_cube_prune          = true;
	public static double  fuzz1                   = 0.1;
	public static double  fuzz2                   = 0.1;
	public static int     max_n_items             = 30;
	public static double  relative_threshold      = 10.0;
	public static int     max_n_rules             = 50;
	public static double  rule_relative_threshold = 10.0;
	
	//nbest config
	public static boolean use_unique_nbest  = false;
	public static boolean use_tree_nbest    = false;
	public static boolean add_combined_cost = true; //in the nbest file, compute the final socre
	public static int topN = 500;
	
	//remote lm server
	public static boolean use_remote_lm_server = false;
	public static String remote_symbol_tbl     = "null"; //this file will first be created by remote_lm_server, and read by remote_suffix_server and the decoder	
	public static int num_remote_lm_servers    = 1;
	public static String f_remote_server_list  = "null";
	
	//parallel decoding
	public static String   parallel_files_prefix = "/tmp/temp.parallel"; // C:\\Users\\zli\\Documents\\temp.parallel; used for parallel decoding
	public static int      num_parallel_decoders = 1; //number of threads should run
	
	//disk hg
	public static boolean save_disk_hg             = false; //if true, save three files: fnbest, fnbest.hg.items, fnbest.hg.rules
	public static boolean forest_pruning           = false;
	public static double  forest_pruning_threshold = 10;
	
	//variational decoding 
	public static boolean use_variational_decoding   = false;
	
	//debug
	public static boolean extract_confusion_grammar = false; //non-parallel version
	public static String f_confusion_grammar = "C:\\Users\\zli\\Documents\\confusion.hg.grammar";
	//debug end
		
	
	private static final Logger logger = Logger.getLogger(JoshuaConfiguration.class.getName());

	public static void read_config_file(String config_file) {
		BufferedReader t_reader_config = FileUtility.getReadFileStream(config_file);
		String line;
		while ((line = FileUtility.read_line_lzf(t_reader_config)) != null) {
			//line = line.trim().toLowerCase();
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}
			
			if (line.indexOf("=") != -1) { // parameters
				String[] fds = line.split("\\s*=\\s*");
				if (fds.length != 2) {
					if (logger.isLoggable(Level.SEVERE)) logger.severe(
						"Wrong config line: " + line);
					System.exit(1);
				}
				
				if (0 == fds[0].compareTo("lm_file")) {
					lm_file = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("lm file: %s", lm_file));
					
				} else if (0 == fds[0].compareTo("tm_file")) {
					tm_file = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("tm file: %s", tm_file));
					
				} else if (0 == fds[0].compareTo("use_srilm")) {
					use_srilm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_srilm: %s", use_srilm));
					
				} else if (0 == fds[0].compareTo("lm_ceiling_cost")) {
					lm_ceiling_cost = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("lm_ceiling_cost: %s", lm_ceiling_cost));
					
				} else if (0 == fds[0].compareTo("use_left_euqivalent_state")) {
					use_left_euqivalent_state = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_left_euqivalent_state: %s", use_left_euqivalent_state));
					
				} else if (0 == fds[0].compareTo("use_right_euqivalent_state")) {
					use_right_euqivalent_state = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_right_euqivalent_state: %s", use_right_euqivalent_state));
					
				} else if (0 == fds[0].compareTo("order")) {
					g_lm_order = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("g_lm_order: %s", g_lm_order));
					
				} else if (0 == fds[0].compareTo("use_sent_specific_lm")) {
					use_sent_specific_lm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_sent_specific_lm: %s", use_sent_specific_lm));
					
				} else if (0 == fds[0].compareTo("sent_lm_file_name_prefix")) {
					g_sent_lm_file_name_prefix = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("sent_lm_file_name_prefix: %s", g_sent_lm_file_name_prefix));
					
				} else if (0 == fds[0].compareTo("use_sent_specific_tm")) {
					use_sent_specific_tm = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_sent_specific_tm: %s", use_sent_specific_tm));
					
				} else if (0 == fds[0].compareTo("sent_tm_file_name_prefix")) {
					g_sent_tm_file_name_prefix = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("sent_tm_file_name_prefix: %s", g_sent_tm_file_name_prefix));
					
				} else if (0 == fds[0].compareTo("span_limit")) {
					span_limit = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("span_limit: %s", span_limit));
					
				} else if (0 == fds[0].compareTo("phrase_owner")) {
					phrase_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("phrase_owner: %s", phrase_owner));
					
				} else if (0 == fds[0].compareTo("mono_owner")) {
					mono_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("mono_owner: %s", mono_owner));
					
				} else if (0 == fds[0].compareTo("begin_mono_owner")) {
					begin_mono_owner = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("begin_mono_owner: %s", begin_mono_owner));
					
				} else if (0 == fds[0].compareTo("default_non_terminal")) {
					default_non_terminal = fds[1].trim();
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("default_non_terminal: %s", default_non_terminal));
					
				} else if (0 == fds[0].compareTo("fuzz1")) {
					fuzz1 = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("fuzz1: %s", fuzz1));
					
				} else if (0 == fds[0].compareTo("fuzz2")) {
					fuzz2 = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("fuzz2: %s", fuzz2));
					
				} else if (0 == fds[0].compareTo("max_n_items")) {
					max_n_items = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("max_n_items: %s", max_n_items));
					
				} else if (0 == fds[0].compareTo("relative_threshold")) {
					relative_threshold = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("relative_threshold: %s", relative_threshold));
					
				} else if (0 == fds[0].compareTo("max_n_rules")) {
					max_n_rules = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("max_n_rules: %s", max_n_rules));
					
				} else if (0 == fds[0].compareTo("rule_relative_threshold")) {
					rule_relative_threshold = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("rule_relative_threshold: %s", rule_relative_threshold));
					
				} else if (0 == fds[0].compareTo("use_unique_nbest")) {
					use_unique_nbest = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));
					
				} else if (0 == fds[0].compareTo("add_combined_cost")) {
					add_combined_cost = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("add_combined_cost: %s", add_combined_cost));
					
				} else if (0 == fds[0].compareTo("use_tree_nbest")) {
					use_tree_nbest = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_tree_nbest: %s", use_tree_nbest));
					
				} else if (0 == fds[0].compareTo("top_n")) {
					topN = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("topN: %s", topN));
					
				} else if (0 == fds[0].compareTo("use_remote_lm_server")) {
					use_remote_lm_server = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("use_remote_lm_server: %s", use_remote_lm_server));
					
				} else if (0 == fds[0].compareTo("f_remote_server_list")) {
					f_remote_server_list = new String(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("f_remote_server_list: %s", f_remote_server_list));
					
				} else if (0 == fds[0].compareTo("num_remote_lm_servers")) {
					num_remote_lm_servers = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("num_remote_lm_servers: %s", num_remote_lm_servers));
					
				} else if (0 == fds[0].compareTo("remote_symbol_tbl")) {
					remote_symbol_tbl = new String(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("remote_symbol_tbl: %s", remote_symbol_tbl));
					
				} else if (0 == fds[0].compareTo("remote_lm_server_port")) {
					//port = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("remote_lm_server_port: not used"));
					
				} else if (0 == fds[0].compareTo("parallel_files_prefix")) {
					parallel_files_prefix = new String(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("parallel_files_prefix: %s", parallel_files_prefix));
					
				} else if (0 == fds[0].compareTo("num_parallel_decoders")) {
					num_parallel_decoders = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("num_parallel_decoders: %s", num_parallel_decoders));
					
				} else if (0 == fds[0].compareTo("save_disk_hg")) {
					save_disk_hg = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("save_disk_hg: %s", save_disk_hg));
					
				} else if (0 == fds[0].compareTo("forest_pruning")) {
					forest_pruning = new Boolean(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("forest_pruning: %s", forest_pruning));
					
				} else if (0 == fds[0].compareTo("forest_pruning_threshold")) {
					forest_pruning_threshold = new Double(fds[1]);
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("forest_pruning_threshold: %s", forest_pruning_threshold));
					
				} else {
					if (logger.isLoggable(Level.SEVERE)) logger.severe(
						"Wrong config line: " + line);
					System.exit(1);
				}
			}
		}
		FileUtility.close_read_file(t_reader_config);
	}
	
	
	
}
