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
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Trie;
import joshua.util.io.LineReader;
import joshua.util.Regex;


/**
 * this class implements MemoryBasedBatchGrammar
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

public class MemoryBasedBatchGrammar  extends BatchGrammar {
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	
	protected int num_rule_read    = 0;
	protected int num_rule_bin     = 0;
	protected MemoryBasedTrie root = null;
	
	static protected double tem_estcost = 0.0;//debug
	
	static int rule_id_count = 1; //three kinds of rule: regular rule (id>0); oov rule (id=0), and null rule (id=-1)

	
	private static final Logger logger = Logger.getLogger(MemoryBasedBatchGrammar.class.getName());
	

	//moved from batch grammar
	public    static int OOV_RULE_ID          = 0;
	//protected  ArrayList<FeatureFunction> p_l_models = null;
	protected int defaultOwner;
	
	// TODO: replace with joshua.util.Regex so we only compile once
	protected  String nonterminalRegexp = "^\\[[A-Z]+\\,[0-9]*\\]$";//e.g., [X,1]
	protected String nonterminalReplaceRegexp = "[\\[\\]\\,0-9]+";
	
	protected int spanLimit = 10;
	SymbolTable p_symbolTable = null;

	public MemoryBasedBatchGrammar(){
		//do nothing
	}
	
	public MemoryBasedBatchGrammar(
		SymbolTable psymbolTable,
		String grammar_file,
		boolean is_glue_grammar,
		//ArrayList<FeatureFunction> l_models,
		String                     default_owner,
		int                        span_limit,
		String                     nonterminal_regexp,
		String                     nonterminal_replace_regexp
	) throws IOException {
		
		this.p_symbolTable = psymbolTable;
		//this.p_l_models               = l_models;
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
		this.root = new MemoryBasedTrie(); //root should not have valid ruleBin entries
		if (logger.isLoggable(Level.INFO))
			logger.info("Reading grammar from file " + grammar_file);
		
		LineReader treeReader = new LineReader(grammar_file);
		try { for (String line : treeReader) {
			this.add_rule(line, defaultOwner);
		} } finally { treeReader.close(); }
		
		this.print_grammar();
		this.sortGrammar(null);//the rule cost has been estimated using the latest feature function
	}
	

	//	TODO: this should read from file
	protected void read_tm_grammar_glue_rules() {
		final double alpha = Math.log10(Math.E); //Cost
		this.root = new MemoryBasedTrie(); //root should not have valid ruleBin entries
		
		this.add_rule("S ||| [" + JoshuaConfiguration.default_non_terminal + ",1] ||| [" + JoshuaConfiguration.default_non_terminal + ",1] ||| 0",	this.p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner));//this does not have any cost	
		//TODO: search consider_start_sym (Decoder.java, LMModel.java, and Chart.java)
		//glue_gr.add_rule("S ||| [PHRASE,1] ||| "+Symbol.START_SYM+" [PHRASE,1] ||| 0", begin_mono_owner);//this does not have any cost
		this.add_rule("S ||| [S,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| [S,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| " + alpha,this.p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner));
		//glue_gr.add_rule("S ||| [S,1] [PHRASE,2] [PHRASE,3] ||| [S,1] [PHRASE,2] [PHRASE,3] ||| "+alpha, MONO_OWNER);
		
		//ITG rules
		//this.add_rule("X ||| [X,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| [X,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| " + alpha,this.p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner));		
		//this.add_rule("X ||| [X,1] [" + JoshuaConfiguration.default_non_terminal + ",2] ||| [X,2] [" + JoshuaConfiguration.default_non_terminal + ",1] ||| " + alpha,this.p_symbolTable.addTerminal(JoshuaConfiguration.begin_mono_owner));
	
		
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
	
	
	// TODO: reorganize to pass in a joshua.util.Regex instead of String
	protected static final String replace_french_non_terminal(String nonterminalReplaceRegexp_, String symbol) {
		return symbol.replaceAll(nonterminalReplaceRegexp_, "");//remove [, ], and numbers
	}
	
	
	// TODO: reorganize to pass in a joshua.util.Regex instead of String
	//TODO: we assume all the Chinese training text is lowercased, and all the non-terminal symbols are in [A-Z]+
	protected  static final boolean is_non_terminal(String nonterminalRegexp_, String symbol) {
		return symbol.matches(nonterminalRegexp_);
	}
	
	
	public Trie getTrieRoot() {
		return this.root;
	}
	
	
	
	public static Rule createRule(SymbolTable p_symbolTable, String nonterminalRegexp_, String nonterminalReplaceRegexp_, int r_id, String line, int owner_in) {
		
		//rule format: X ||| Foreign side ||| English side ||| feature scores
		String[] fds = Regex.threeBarsWithSpace.split(line);
		if (fds.length != 4) {
			Support.write_log_line("rule line does not have four fds; " + line, Support.ERROR);
		}
		
		//=== lhs
		int lhs = p_symbolTable.addNonterminal(replace_french_non_terminal(nonterminalReplaceRegexp_, fds[0]));
		
		//=== arity and french
		int arity = 0;
		String[] french_tem = Regex.spaces.split(fds[1]);
		int[] french_ints = new int[french_tem.length];
		for (int i = 0; i < french_tem.length; i++) {
			if (is_non_terminal(nonterminalRegexp_, french_tem[i])) {
				arity++;
				french_ints[i] = p_symbolTable.addNonterminal(french_tem[i]);//when storing hyper-graph, we need this
			} else {
				french_ints[i] = p_symbolTable.addTerminal(french_tem[i]);
			}
		}
		
		//=== english side
		String[] english_tem = Regex.spaces.split(fds[2]);
		int[] english = new int[english_tem.length];
		for (int i = 0; i < english_tem.length; i++) {
			if (is_non_terminal(nonterminalRegexp_, english_tem[i])) {
				english[i] = p_symbolTable.addNonterminal(english_tem[i]);
			} else {
				english[i] = p_symbolTable.addTerminal(english_tem[i]);
			}
		}
		
		//=== feature costs
		String[] t_scores = Regex.spaces.split(fds[3]);
		float[] scores = new float[t_scores.length];
		int i = 0;
		for (String score : t_scores) {
			scores[i++] = Float.parseFloat(score);
		}
		
		
		Rule res = new BilingualRule(lhs, french_ints, english, scores,  arity, owner_in, 0, r_id);
		
		//tem_estcost += estimate_rule();//estimate lower-bound, and set statelesscost, this must be called
		//res.estimateRuleCost(p_l_models);//estimate lower-bound, and set statelesscost, this must be called
		return res;

	}

	
	protected Rule add_rule(String line, int owner) {
		this.num_rule_read++;
		num_rule_read++;
		rule_id_count++;
		//######1: parse the line
		//######2: create a rule
		//Rule p_rule = new Rule(this,rule_id_count, line, owner);	
		Rule p_rule = createRule(p_symbolTable, nonterminalRegexp, nonterminalReplaceRegexp,rule_id_count, line, owner);
		tem_estcost += p_rule.getEstCost();
		
		//######### identify the position, and insert the trinodes if necessary
		MemoryBasedTrie pos = root;
		int[] p_french = p_rule.getFrench();
		for (int k = 0; k < p_french.length; k++) {
			int cur_sym_id = p_french[k];
			if (this.p_symbolTable.isNonterminal(p_french[k])) { //TODO: p_rule.french store the original format like "[X,1]"
				cur_sym_id = this.p_symbolTable.addNonterminal(replace_french_non_terminal(nonterminalReplaceRegexp, this.p_symbolTable.getWord(p_french[k])));
			}
			
			MemoryBasedTrie next_layer = pos.matchOne(cur_sym_id);
			if (null != next_layer) {
				pos = next_layer;
			} else {
				MemoryBasedTrie tem = new MemoryBasedTrie();//next layer node
				if (! pos.hasExtensions()) {
					pos.tbl_children = new HashMap<Integer,MemoryBasedTrie>();
				}
				pos.tbl_children.put(cur_sym_id, tem);
				pos = tem;
			}
		}
		
		//#########3: now add the rule into the trinode
		if (! pos.hasRules()) {
			pos.rule_bin        = new MemoryBasedRuleBin();
			pos.rule_bin.french = p_french;
			pos.rule_bin.arity  = p_rule.getArity();
			num_rule_bin++;
		}
		
		pos.rule_bin.addRule(p_rule);
		
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


	public Rule constructOOVRule(int num_feats, int lhs, int sourceWord, int owner, boolean have_lm_model) {
		int[] p_french      = new int[1];
	   	p_french[0]         = sourceWord;
	   	int[] english       = new int[1];
	   	english[0]          = sourceWord;
	   	float[] feat_scores = new float[num_feats];
	   	
	   	/**TODO
	   	 * This is a hack to make the decoding without a LM works
	   	 * */
	   	if (! have_lm_model) {//no LM is used for decoding, so we should set the stateless cost
	   		//this.feat_scores[0]=100.0/((FeatureFunction)p_l_models.get(0)).getWeight();//TODO
	   		feat_scores[0]=100;//TODO
	   	}
	   	
		return new BilingualRule(lhs, p_french, english, feat_scores,  0, owner, 0, getOOVRuleID());
	}

	public int getOOVRuleID() {
		return OOV_RULE_ID;
	}
	
	
	
}