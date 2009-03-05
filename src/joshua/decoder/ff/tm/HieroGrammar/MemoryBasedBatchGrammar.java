package joshua.decoder.ff.tm.HieroGrammar;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.TrieGrammar;
import joshua.util.FileUtility;

public class MemoryBasedBatchGrammar  extends BatchGrammar {
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	
	protected int num_rule_read    = 0;
	protected int num_rule_bin     = 0;
	protected MemoryBasedTrieGrammar root = null;
	
	static protected double tem_estcost = 0.0;//debug
	
	static int rule_id_count =1; //three kinds of rule: regular rule (id>0); oov rule (id=0), and null rule (id=-1)

	
	private static final Logger logger = Logger.getLogger(MemoryBasedBatchGrammarWithPrune.class.getName());
	

	//moved from batch grammar
	public    static int OOV_RULE_ID          = 0;
	protected  ArrayList<FeatureFunction> p_l_models = null;
	protected int defaultOwner  ;
	protected  String nonterminalRegexp = "^\\[[A-Z]+\\,[0-9]*\\]$";//e.g., [X,1]
	protected String nonterminalReplaceRegexp = "[\\[\\]\\,0-9]+";
	
	protected int spanLimit = 10;
	SymbolTable p_symbolTable = null;

		
	public MemoryBasedBatchGrammar(
		SymbolTable psymbolTable,
		String grammar_file,
		boolean is_glue_grammar,
		ArrayList<FeatureFunction> l_models,
		String                     default_owner,
		int                        span_limit,
		String                     nonterminal_regexp,
		String                     nonterminal_replace_regexp
	) throws IOException {
		
		this.p_symbolTable = psymbolTable;
		this.p_l_models               = l_models;
		this.defaultOwner             = p_symbolTable.addTerminal(default_owner);
		this.nonterminalRegexp        = nonterminal_regexp;
		this.nonterminalReplaceRegexp = nonterminal_replace_regexp;		
		this.spanLimit = span_limit;
		
		//read the grammar from file
		if (is_glue_grammar) {
			if (null != grammar_file) {
				logger.severe("You provide a grammar file, but you also indicate it is a glue grammar, there must be sth wrong");
				System.exit(1);
			}
			read_tm_grammar_glue_rules();
		} else {
			if (null == grammar_file) {
				logger.severe("You must provide a grammar file for MemoryBasedTMGrammar");
				System.exit(1);
			}
			read_tm_grammar_from_file(grammar_file);
		}
	}
	
	
	protected void read_tm_grammar_from_file(String grammar_file)
	throws IOException {
		this.root = new MemoryBasedTrieGrammar(); //root should not have valid ruleBin entries
		BufferedReader t_reader_tree = FileUtility.getReadFileStream(grammar_file);
		if (logger.isLoggable(Level.INFO)) logger.info(
			"Reading grammar from file " + grammar_file);
		
		String line;
		while ((line = FileUtility.read_line_lzf(t_reader_tree)) != null) {
			this.add_rule(line,  defaultOwner);
		}
		this.print_grammar();
		this.sortGrammar(null);//the rule cost has been estimated using the latest feature function
	}
	

	//	TODO: this should read from file
	protected void read_tm_grammar_glue_rules() {
		final double alpha = Math.log10(Math.E); //Cost
		this.root = new MemoryBasedTrieGrammar(); //root should not have valid ruleBin entries
		
		this.add_rule("S ||| ["	+ JoshuaConfiguration.default_non_terminal + ",1] ||| [" + JoshuaConfiguration.default_non_terminal	+ ",1] ||| 0",	this.p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner));//this does not have any cost	
		//TODO: search consider_start_sym (Decoder.java, LMModel.java, and Chart.java)
		//glue_gr.add_rule("S ||| [PHRASE,1] ||| "+Symbol.START_SYM+" [PHRASE,1] ||| 0", begin_mono_owner);//this does not have any cost
		this.add_rule("S ||| [S,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| [S,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| " + alpha,this.p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner));
		//glue_gr.add_rule("S ||| [S,1] [PHRASE,2] [PHRASE,3] ||| [S,1] [PHRASE,2] [PHRASE,3] ||| "+alpha, MONO_OWNER);
		print_grammar();
		sortGrammar(null);//the rule cost has been estimated using the latest feature function
	}
	
	

	
	
	/** if the span covered by the chart bin is greater than the limit, then return false */
	public boolean hasRuleForSpan(int startIndex,	int endIndex,	int pathLength) {
		if (this.spanLimit == -1) { // mono-glue grammar
			return (startIndex == 0);
		} else {
			return (endIndex - startIndex <= this.spanLimit);
		}
	}
	
	
	protected static final String replace_french_non_terminal(String nonterminalReplaceRegexp_, String symbol) {
		return symbol.replaceAll(nonterminalReplaceRegexp_, "");//remove [, ], and numbers
	}
	
	
	//TODO: we assume all the Chinese training text is lowercased, and all the non-terminal symbols are in [A-Z]+
	protected  static final boolean is_non_terminal(String nonterminalRegexp_, String symbol) {
		return symbol.matches(nonterminalRegexp_);
	}
	
	
	public TrieGrammar getTrieRoot() {
		return this.root;
	}
	
	
	public static Rule createRule(SymbolTable p_symbolTable, ArrayList<FeatureFunction> p_l_models, String nonterminalRegexp_, String nonterminalReplaceRegexp_, int r_id, String line, int owner_in) {
		Rule res = new Rule(); 
		res.rule_id = r_id;
		res.owner   = owner_in;
		
		//rule format: X ||| Foreign side ||| English side ||| feature scores
		String[] fds = line.split("\\s+\\|{3}\\s+");
		if (fds.length != 4) {
			Support.write_log_line("rule line does not have four fds; " + line, Support.ERROR);
		}
		
		//=== lhs
		res.lhs = p_symbolTable.addNonterminal(replace_french_non_terminal(nonterminalReplaceRegexp_, fds[0]));
		
		int arity = 0;
		String[] french_tem = fds[1].split("\\s+");
		res.p_french = new int[french_tem.length];
		for (int i = 0; i < french_tem.length; i++) {
			if (is_non_terminal(nonterminalRegexp_, french_tem[i])) {
				arity++;
				res.p_french[i] = p_symbolTable.addNonterminal(french_tem[i]);//when storing hyper-graph, we need this
			} else {
				res.p_french[i] = p_symbolTable.addTerminal(french_tem[i]);
			}
		}
		res.arity = arity;
		
		//english side
		String[] english_tem = fds[2].split("\\s+");
		res.english = new int[english_tem.length];
		for (int i = 0; i < english_tem.length; i++) {
			if (is_non_terminal(nonterminalRegexp_, english_tem[i])) {
				res.english[i] = p_symbolTable.addNonterminal(english_tem[i]);
			} else {
				res.english[i] = p_symbolTable.addTerminal(english_tem[i]);
			}
		}
		
		String[] t_scores = fds[3].split("\\s+");
		res.feat_scores = new float[t_scores.length];
		int i = 0;
		for (String score : t_scores) {
			res.feat_scores[i++] = Float.parseFloat(score);
		}
		res.lattice_cost = 0;
		//tem_estcost += estimate_rule();//estimate lower-bound, and set statelesscost, this must be called
		
		res.estimateRuleCost(p_l_models);//estimate lower-bound, and set statelesscost, this must be called
		return res;

	}

	
	protected Rule add_rule(String line, int owner) {
		this.num_rule_read++;
		num_rule_read++;
		rule_id_count++;
		//######1: parse the line
		//######2: create a rule
		//Rule p_rule = new Rule(this,rule_id_count, line, owner);	
		Rule p_rule = createRule(p_symbolTable, p_l_models, nonterminalRegexp, nonterminalReplaceRegexp,rule_id_count, line, owner);
		tem_estcost += p_rule.getEstRuleCost();
		
		//######### identify the position, and insert the trinodes if necessary
		MemoryBasedTrieGrammar pos = root;
		for (int k = 0; k < p_rule.p_french.length; k++) {
			int cur_sym_id = p_rule.p_french[k];
			if (this.p_symbolTable.isNonterminal(p_rule.p_french[k])) { //TODO: p_rule.french store the original format like "[X,1]"
				cur_sym_id = this.p_symbolTable.addNonterminal(replace_french_non_terminal(nonterminalReplaceRegexp, this.p_symbolTable.getWord(p_rule.p_french[k])));
			}
			
			MemoryBasedTrieGrammar next_layer = pos.matchOne(cur_sym_id);
			if (null != next_layer) {
				pos = next_layer;
			} else {
				MemoryBasedTrieGrammar tem = new MemoryBasedTrieGrammar();//next layer node
				if (null == pos.tbl_children) {
					pos.tbl_children = new HashMap<Integer,MemoryBasedTrieGrammar> ();
				}
				pos.tbl_children.put(cur_sym_id, tem);
				pos = tem;
			}
		}
		
		//#########3: now add the rule into the trinode
		if (null == pos.rule_bin) {
			pos.rule_bin        = new MemoryBasedRuleBin();
			pos.rule_bin.french = p_rule.p_french;
			pos.rule_bin.arity  = p_rule.arity;
			num_rule_bin++;
		}
		
		pos.rule_bin.add_rule(p_rule);
		
		return p_rule;
	}
	
	
	//this method should be called such that all the rules in rulebin are sorted, this will avoid synchronization for get_sorted_rules function
	public void sortGrammar(ArrayList<FeatureFunction> l_models) {
		if (null != this.root) {
			this.root.ensure_sorted(l_models);
		}
	}
	
	
	protected void print_grammar() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format("####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",num_rule_read, num_rule_bin, 0, tem_estcost));
		}
		/*if(root!=null)
			root.print_info(Support.DEBUG);*/
	}
	
	
	
}