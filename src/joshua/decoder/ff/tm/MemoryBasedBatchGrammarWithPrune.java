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
package joshua.decoder.ff.tm;


import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.Symbol;
import joshua.decoder.ff.FeatureFunction;
import joshua.util.FileUtility;

import java.io.BufferedReader;
import java.util.HashMap ;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 * this class implement 
 * (1) load the translation grammar
 * (2) provide a DOT interfact
 * (3) Rule information
 * 
 *public interfaces
 * TMGrammar: init and load the grammar
 * TrieGrammar: match symbol for next layer
 * RuleBin: get sorted rules
 * Rule: rule information
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class MemoryBasedBatchGrammarWithPrune extends BatchGrammar {
	private static double EPSILON = 0.000001;	
	private static int IMPOSSIBLE_COST=99999;//max cost
	
	private int num_rule_read    = 0;
	private int num_rule_pruned  = 0;
	private int num_rule_bin     = 0;
	private MemoryBasedTrieGrammar root = null;
	
	static int rule_id_count =1; //three kinds of rule: regular rule (id>0); oov rule (id=0), and null rule (id=-1)
	static private double tem_estcost = 0.0;//debug
	
	private static final Logger logger = Logger.getLogger(MemoryBasedBatchGrammarWithPrune.class.getName());
	
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	
	public MemoryBasedBatchGrammarWithPrune(
		Symbol psymbol,
		String grammar_file,
		boolean is_glue_grammar,
		ArrayList<FeatureFunction> l_models,
		String                     default_owner,
		int                        span_limit,
		String                     nonterminal_regexp,
		String                     nonterminal_replace_regexp
	) {		
		super(psymbol, grammar_file, l_models, default_owner, span_limit, nonterminal_regexp, nonterminal_replace_regexp);
		
		//read the grammar from file
		if(is_glue_grammar==true){
			if(grammar_file!=null){
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("You provide a grammar file, but you also indicate it is a glue grammar, there must be sth wrong");
				System.exit(1);
			}
			read_tm_grammar_glue_rules();
		}else{
			if(grammar_file==null){
				if (logger.isLoggable(Level.SEVERE)) 
					logger.severe("You must provide a grammar file for MemoryBasedTMGrammar");
				System.exit(1);
			}
			read_tm_grammar_from_file(grammar_file);
		}
	}
	
	
	protected void read_tm_grammar_from_file(String grammar_file) {
		this.root = new MemoryBasedTrieGrammar(); //root should not have valid ruleBin entries
		BufferedReader t_reader_tree = 
			FileUtility.getReadFileStream(grammar_file,"utf8");  // BUG? shouldn't this be the implicit "UTF-8" instead?
		if (logger.isLoggable(Level.INFO)) logger.info(
			"Reading grammar from file " + grammar_file);
		
		String line;
		while ((line = FileUtility.read_line_lzf(t_reader_tree)) != null) {
			this.add_rule(line,  defaultOwner);
		}
		this.print_grammar();
		this.ensure_grammar_sorted();
	}
	

	//	TODO: this should read from file
	protected void read_tm_grammar_glue_rules() {
		final double alpha = Math.log10(Math.E); //Cost
		this.root = new MemoryBasedTrieGrammar(); //root should not have valid ruleBin entries
		
		this.add_rule("S ||| ["	+ JoshuaConfiguration.default_non_terminal + ",1] ||| [" + JoshuaConfiguration.default_non_terminal	+ ",1] ||| 0",	this.p_symbol.addTerminalSymbol(JoshuaConfiguration.begin_mono_owner));//this does not have any cost	
		//TODO: search consider_start_sym (Decoder.java, LMModel.java, and Chart.java)
		//glue_gr.add_rule("S ||| [PHRASE,1] ||| "+Symbol.START_SYM+" [PHRASE,1] ||| 0", begin_mono_owner);//this does not have any cost
		this.add_rule("S ||| [S,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| [S,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| " + alpha,this.p_symbol.addTerminalSymbol(JoshuaConfiguration.begin_mono_owner));
		//glue_gr.add_rule("S ||| [S,1] [PHRASE,2] [PHRASE,3] ||| [S,1] [PHRASE,2] [PHRASE,3] ||| "+alpha, MONO_OWNER);
		print_grammar();
		ensure_grammar_sorted();
	}
	
	
	public TrieGrammar getTrieRoot() {
		return this.root;
	}
	
	
	private Rule add_rule(String line, int owner) {
		this.num_rule_read++;
		num_rule_read++;
		rule_id_count++;
		//######1: parse the line
		//######2: create a rule
		//MemoryBasedRule p_rule = new MemoryBasedRule(this,rule_id_count, line, owner);	
		MemoryBasedRule p_rule = new  MemoryBasedRule(p_symbol, p_l_models, nonterminalRegexp, nonterminalReplaceRegexp,rule_id_count, line, owner);
		tem_estcost += p_rule.getEstCost();
		
		//######### identify the position, and insert the trinodes if necessary
		MemoryBasedTrieGrammar pos = root;
		for (int k = 0; k < p_rule.french.length; k++) {
			int cur_sym_id = p_rule.french[k];
			if (this.p_symbol.isNonterminal(p_rule.french[k])) { //TODO: p_rule.french store the original format like "[X,1]"
				cur_sym_id = this.p_symbol.addNonTerminalSymbol(replace_french_non_terminal(nonterminalReplaceRegexp, this.p_symbol.getWord(p_rule.french[k])));
			}
			
			MemoryBasedTrieGrammar next_layer = pos.matchOne(cur_sym_id);
			if (null != next_layer) {
				pos = next_layer;
			} else {
				MemoryBasedTrieGrammar tem = new MemoryBasedTrieGrammar();//next layer node
				if (null == pos.tbl_children) {
					pos.tbl_children = new HashMap ();
				}
				pos.tbl_children.put(cur_sym_id, tem);
				pos = tem;
			}
		}
		
		//#########3: now add the rule into the trinode
		if (null == pos.rule_bin) {
			pos.rule_bin        = new MemoryBasedRuleBin();
			pos.rule_bin.french = p_rule.french;
			pos.rule_bin.arity  = p_rule.arity;
			num_rule_bin++;
		}
		if (p_rule.getEstCost() > pos.rule_bin.cutoff) {
			num_rule_pruned++;
		} else {
			pos.rule_bin.add_rule(p_rule);
			num_rule_pruned += pos.rule_bin.run_pruning();
		}
		return p_rule;
	}
	
	
	//this method should be called such that all the rules in rulebin are sorted, this will avoid synchronization for get_sorted_rules function
	private void ensure_grammar_sorted() {
		if (null != this.root) {
			this.root.ensure_sorted();
		}
	}
	
	
	protected void print_grammar() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format("####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",num_rule_read, num_rule_bin, num_rule_pruned, tem_estcost));
		}
		/*if(root!=null)
			root.print_info(Support.DEBUG);*/
	}
	
	
	public class MemoryBasedTrieGrammar implements TrieGrammar {
		private MemoryBasedRuleBin rule_bin     = null;
		private HashMap        tbl_children = null;
		
		
		public MemoryBasedTrieGrammar matchOne(int sym_id) {
			//looking for the next layer trinode corresponding to this symbol
			/*if(sym_id==null)
				Support.write_log_line("Match_symbol: sym is null", Support.ERROR);*/
			if (null == tbl_children) {
				return null;
			} else {
				return (MemoryBasedTrieGrammar) tbl_children.get(sym_id);
			}
		}
		
		
		public boolean hasExtensions() {
			return (null != this.tbl_children);
		}
		
		
		public boolean hasRules() {
			return (null != this.rule_bin);
		}
		
		
		public RuleCollection getRules() {
			return this.rule_bin;
		}
		
		
		//recursive call, to make sure all rules are sorted
		private void ensure_sorted() {
			if (null != this.rule_bin) {
				this.rule_bin.getSortedRules();
			}
			if (null != this.tbl_children) {
				Object[] tem = this.tbl_children.values().toArray();
				for (int i = 0; i < tem.length; i++) {
					((MemoryBasedTrieGrammar)tem[i]).ensure_sorted();
				}
			}
		}
		
/* TODO Possibly remove - this method is never called.		
		private void print_info(int level) {
			Support.write_log_line("###########TrieGrammar###########",level);
			if (null != rule_bin) {
				Support.write_log_line("##### RuleBin(in TrieGrammar) is",level);
				rule_bin.print_info(level);
			}
			if (null != tbl_children) {
				Object[] tem = tbl_children.values().toArray();
				for (int i = 0; i < tem.length; i++) {
					Support.write_log_line("##### ChildTrieGrammar(in TrieGrammar) is",level);
					((TrieNode_Memory)tem[i]).print_info(level);
				}
			}
		}
*/
	}
	
	
	/** contain all rules with the same french side (and thus same arity) */
	public class MemoryBasedRuleBin	extends RuleBin {
		private PriorityQueue<MemoryBasedRule> heapRules   = null;
		private double                     cutoff      = IMPOSSIBLE_COST;
		private boolean                    sorted      = false;
		private ArrayList<Rule>            sortedRules = new ArrayList<Rule>();
		
		
		/**
		 * TODO: now, we assume this function will be called
		 * only after all the rules have been read this
		 * method need to be synchronized as we will call
		 * this function only after the decoding begins to
		 * avoid the synchronized method, we should call
		 * this once the grammar is finished
		 */
		//public synchronized ArrayList<Rule> get_sorted_rules() {
		public ArrayList<Rule> getSortedRules() {
			if (! this.sorted) {
				this.sortedRules.clear();
				while (this.heapRules.size() > 0) {
					Rule t_r = (Rule) this.heapRules.poll();
					this.sortedRules.add(0, t_r);
				}
				this.sorted    = true;
				this.heapRules = null;
			}
			return this.sortedRules;
		}
		
		
		public int[] getSourceSide() {
			return this.french;
		}
		
		
		public int getArity() {
			return this.arity;
		}
		
		
		private void add_rule(MemoryBasedRule rule) {
			if (null == this.heapRules) {
				this.heapRules = new PriorityQueue<MemoryBasedRule>(1, MemoryBasedRule.NegtiveCostComparator);//TODO: initial capacity?
				this.arity = rule.arity;
			}
			if (rule.arity != this.arity) {
				Support.write_log_line(
					String.format("RuleBin: arity is not matching, old: %d; new: %d", this.arity, rule.arity),	Support.ERROR);
				return;
			}
			this.heapRules.add(rule);	//TODO: what is offer()
			if (rule.getEstCost() + JoshuaConfiguration.rule_relative_threshold < this.cutoff) {
				this.cutoff = rule.getEstCost() + JoshuaConfiguration.rule_relative_threshold;
			}
			rule.french = this.french; //TODO: this will release the memory in each rule, but still have a pointer
		}
		
		
		private int run_pruning() {
			int n_pruned = 0;
			
			while (this.heapRules.size() > JoshuaConfiguration.max_n_rules || this.heapRules.peek().getEstCost() >= this.cutoff) {
				n_pruned++;
				this.heapRules.poll();
				if(this.heapRules.peek()==null){System.out.println("the stack is empty, which is strange; cutoff:" + this.cutoff);}
			}
			if (this.heapRules.size() == JoshuaConfiguration.max_n_rules) {
				this.cutoff =
					(this.cutoff < this.heapRules.peek().getEstCost())
					? this.cutoff
					: this.heapRules.peek().getEstCost() + EPSILON;//TODO
			}
			return n_pruned++;
		}
		
/* TODO Possibly remove - this method is never called.		
		private void print_info(int level) {
			Support.write_log_line(
				String.format("RuleBin, arity is %d", this.arity),
				level);
			ArrayList<Rule> t_l = getSortedRules();
			for (int i = 0; i < t_l.size(); i++) {
				((Rule_Memory)t_l.get(i)).print_info(level);
			}
		}
*/		
	}
	
}
