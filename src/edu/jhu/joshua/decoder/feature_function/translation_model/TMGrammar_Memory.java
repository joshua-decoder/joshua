/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.joshua.decoder.feature_function.translation_model;

import java.io.BufferedReader;
import java.util.Comparator;
import java.util.HashMap ;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.jhu.joshua.decoder.Decoder;
import edu.jhu.joshua.decoder.Support;
import edu.jhu.joshua.decoder.Symbol;
import edu.jhu.joshua.decoder.feature_function.FeatureFunction;
import edu.jhu.lzfUtility.FileUtility;

/** 
 * this class implement 
 * (1) load the translation grammar
 * (2) provide a DOT interfact
 * (3) Rule information
 * 
 *public interfaces
 * TMGrammar: init and load the grammar
 * TrieNode: match symbol for next layer
 * RuleBin: get sorted rules
 * Rule: rule information
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class TMGrammar_Memory extends TMGrammar {
	private  int num_rule_read=0;
	private int num_rule_pruned=0;
	private int num_rule_bin=0;
	private TrieNode_Memory root = null;
	
	static int rule_id =1; //three kinds of rule: regular rule (id>0); oov rule (id=0), and null rule (id=-1)
	static private double tem_estcost =0.0;//debug
	
	private static final Logger logger = Logger.getLogger(TMGrammar_Memory.class.getName());
	
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	
	public TMGrammar_Memory(
		ArrayList<FeatureFunction> l_models,
		String default_owner,
		int    span_limit,
		String nonterminal_regexp,
		String nonterminal_replace_regexp
	) {
		super(l_models, default_owner, span_limit, nonterminal_regexp, nonterminal_replace_regexp);	
	}
	
	public void read_tm_grammar_from_file(String grammar_file){
		root = new TrieNode_Memory(); //root should not have valid ruleBin entries
		BufferedReader t_reader_tree = FileUtility.getReadFileStream(grammar_file,"utf8");		
		if (logger.isLoggable(Level.INFO)) logger.info("Reading grammar from file " + grammar_file);
		String line;
		while((line=FileUtility.read_line_lzf(t_reader_tree))!=null){
			add_rule(line, TMGrammar_Memory.defaultOwner);
		}
		print_grammar();
		ensure_grammar_sorted();
	}
	
	public void read_tm_grammar_glue_rules(){
		root = new TrieNode_Memory(); //root should not have valid ruleBin entries
		double alpha =Math.log10(Math.E);//cost

		//TODO: this should read from file
		add_rule("S ||| ["+Decoder.default_non_terminal+",1] ||| ["+Decoder.default_non_terminal+",1] ||| 0", Symbol.add_terminal_symbol(Decoder.begin_mono_owner));//this does not have any cost	
		//TODO: search consider_start_sym (Decoder.java, LMModel.java, and Chart.java)
		//glue_gr.add_rule("S ||| [PHRASE,1] ||| "+Symbol.START_SYM+" [PHRASE,1] ||| 0", begin_mono_owner);//this does not have any cost
		add_rule("S ||| [S,1] ["+Decoder.default_non_terminal+",2] ||| [S,1] ["+Decoder.default_non_terminal+",2] ||| "+alpha, Symbol.add_terminal_symbol(Decoder.begin_mono_owner));		
		//glue_gr.add_rule("S ||| [S,1] [PHRASE,2] [PHRASE,3] ||| [S,1] [PHRASE,2] [PHRASE,3] ||| "+alpha, MONO_OWNER);		
		print_grammar();
		ensure_grammar_sorted();		
	}
	
	public TrieNode get_root(){
		return root;
	}
	
	
	private Rule add_rule(String line, int owner){
		num_rule_read++;
		rule_id++;
		//######1: parse the line
		//######2: create a rule
		Rule_Memory p_rule = new Rule_Memory(rule_id, line, owner);		
		
		
		//######### identify the position, and insert the trinodes if necessary
		TrieNode_Memory pos = root;
		for(int k=0; k < p_rule.french.length; k++){
			int cur_sym_id=p_rule.french[k];
			if(Symbol.is_nonterminal(p_rule.french[k])){//TODO: p_rule.french store the original format like "[X,1]"
				cur_sym_id = Symbol.add_non_terminal_symbol(TMGrammar_Memory.replace_french_non_terminal(Symbol.get_string(p_rule.french[k])));
			}
				
			TrieNode_Memory next_layer=pos.match_symbol(cur_sym_id);
			if(next_layer!=null){
				pos=next_layer;
			}else{		
				TrieNode_Memory tem = new TrieNode_Memory();//next layer node
				if(pos.tbl_children==null){
					pos.tbl_children = new HashMap ();
				}
				pos.tbl_children.put(cur_sym_id, tem); 
				pos = tem;
			}
		}		
		
		//#########3: now add the rule into the trinode
		if(pos.rule_bin==null){
			pos.rule_bin = new RuleBin_Memory();
			pos.rule_bin.french = p_rule.french;
			pos.rule_bin.arity=p_rule.arity;
			num_rule_bin++;
		}
		if(p_rule.est_cost>pos.rule_bin.cutoff){
			num_rule_pruned++;
		}else{
			pos.rule_bin.add_rule(p_rule);
			num_rule_pruned += pos.rule_bin.run_pruning();
		}
		return p_rule;
	}
		
	private static String replace_french_non_terminal(String symbol){
		return symbol.replaceAll(TMGrammar_Memory.nonterminalReplaceRegexp, "");//remove [, ], and numbers
	}
		
	
	//this method should be called such that all the rules in rulebin are sorted, this will avoid synchronization for get_sorted_rules function
	private void ensure_grammar_sorted(){		
		if(root!=null)
			root.ensure_sorted();
	}
	
	protected void print_grammar(){
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format("####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",num_rule_read, num_rule_bin, num_rule_pruned, tem_estcost));
		}
		/*if(root!=null)
			root.print_info(Support.DEBUG);*/
	}
	
	public class TrieNode_Memory extends TMGrammar.TrieNode
	{
		private	RuleBin_Memory rule_bin=null;
		private HashMap  tbl_children=null;
		
		public TrieNode_Memory match_symbol(int sym_id){//looking for the next layer trinode corresponding to this symbol
			/*if(sym_id==null)
				Support.write_log_line("Match_symbol: sym is null", Support.ERROR);*/
			if(tbl_children==null)
				return null;
			return (TrieNode_Memory) tbl_children.get(sym_id);
		}
		
		public RuleBin get_rule_bin(){
			return rule_bin;
		}
		
		public boolean is_no_child_trienodes(){
			return (tbl_children==null);
		}
		
		//recursive call, to make sure all rules are sorted
		private void ensure_sorted(){
			if(rule_bin!=null)
				rule_bin.get_sorted_rules();
			if(tbl_children!=null){
				Object[] tem = tbl_children.values().toArray();
				for(int i=0; i< tem.length; i++){					
					((TrieNode_Memory)tem[i]).ensure_sorted();
				}
			}
		}
		
		private void print_info(int level){
			Support.write_log_line("###########TrieNode###########",level);
			if(rule_bin!=null){
				Support.write_log_line("##### RuleBin(in TrieNode) is",level);
				rule_bin.print_info(level);
			}
			if(tbl_children!=null){
				Object[] tem = tbl_children.values().toArray();
				for(int i=0; i< tem.length; i++){
					Support.write_log_line("##### ChildTrieNode(in TrieNode) is",level);
					((TrieNode_Memory)tem[i]).print_info(level);
				}
			}
		}
	}

	//contain all rules with the same french side (and thus same arity)
	public class RuleBin_Memory extends TMGrammar.RuleBin {
		private PriorityQueue<Rule_Memory> heap_rules = null;
		private double cutoff=Symbol.IMPOSSIBLE_COST;
        private boolean sorted=false;
		private ArrayList<Rule> l_sorted_rules = new ArrayList<Rule>();
		
		
		//TODO: now, we assume this function will be called only after all the rules have been read
		//this method need to be synchronized as we will call this function only after the decoding begins
		//to avoid the synchronized method, we should call this once the grammar is finished
		//public synchronized ArrayList<Rule> get_sorted_rules(){		
		public ArrayList<Rule> get_sorted_rules(){
			if(sorted==false){//sort once				
				l_sorted_rules.clear();
				while(this.heap_rules.size()>0){
					Rule t_r = (Rule) this.heap_rules.poll();
					l_sorted_rules.add(0,t_r);
				}
				sorted=true;
				this.heap_rules = null;
			}
			return l_sorted_rules;
		}
		
		public  int[] get_french(){
			return french;
		}
		
		public int get_arity(){
			return arity;
		}
		
		private void add_rule(Rule_Memory rl) {
			if (null == this.heap_rules) {
				this.heap_rules = new PriorityQueue<Rule_Memory>(1, Rule_Memory.NegtiveCostComparator);//TODO: initial capacity?
				arity=rl.arity;
			}
			if(rl.arity!=arity){
				Support.write_log_line(String.format("RuleBin: arity is not matching, old: %d; new: %d", arity,rl.arity), Support.ERROR);
				return;
			}
			this.heap_rules.add(rl);	//TODO: what is offer()
			if(rl.est_cost+Decoder.rule_relative_threshold<cutoff)
				cutoff=rl.est_cost+Decoder.rule_relative_threshold;
			rl.french = french; //TODO: this will release the memory in each rule, but still have a pointer
		}

		private int run_pruning() {
			int n_pruned = 0;
			while (this.heap_rules.size() > Decoder.max_n_rules
				|| heap_rules.peek().est_cost >= cutoff
			) {
				n_pruned++;
				heap_rules.poll();
			}
			if (this.heap_rules.size() == Decoder.max_n_rules) {
				cutoff = (cutoff < heap_rules.peek().est_cost)
					? cutoff
					: heap_rules.peek().est_cost + Symbol.EPSILON;//TODO
			}
			return n_pruned++;
		}
		
		private void print_info(int level){
			Support.write_log_line(String.format("RuleBin, arity is %d",arity),level);
			ArrayList t_l = get_sorted_rules();
			for(int i=0; i< t_l.size(); i++)
				((Rule_Memory)t_l.get(i)).print_info(level);
		}
	}

	public static class Rule_Memory extends Rule {
		private float est_cost=0;/*estimate_cost depends on rule itself, nothing else: statelesscost + transition_cost(non-stateless/non-contexual models),
		it is only used in TMGrammar pruning and chart.prepare_rulebin, shownup in chart.expand_unary but not really used*/

		
//		only called when creating rule in Chart, all others should call the other contructor
		/*the transition cost for phrase model, arity penalty, word penalty are all zero, except the LM cost*/
		public Rule_Memory(int lhs_in, int fr_in, int owner_in){
			super();
			lhs = lhs_in;
		   	french = new int[1];
		   	french[0]= fr_in;
		   	english = new int[1];
		   	english[0]= fr_in;
		   	feat_scores = new float[1];
		   	feat_scores[0]=0;
		   	arity=0;
		   	owner = owner_in;		   	
		   	tem_estcost += estimate_rule();//estimate lower-bound for pruning purse, and set statelesscost
		}
					
		public Rule_Memory(int r_id, String line, int owner_in){
			super(line);
			rule_id = r_id;
			owner  = owner_in;
//			######1: parse the line
			String[] fds = line.split("\\s+\\|{3}\\s+");		
			if(fds.length != 4){
				Support.write_log_line("rule line does not have four fds; " + line, Support.ERROR);
			}			
			lhs = Symbol.add_non_terminal_symbol(TMGrammar_Memory.replace_french_non_terminal(fds[0]));
				
			arity=0;
			String[] french_tem= fds[1].split("\\s+");
			french = new int[french_tem.length];			
			for(int i=0; i< french_tem.length; i++){				
				if(is_non_terminal(french_tem[i])==true){
					arity++;
					//french[i]= Symbol.add_non_terminal_symbol(TMGrammar_Memory.replace_french_non_terminal(french_tem[i]));
					french[i]= Symbol.add_non_terminal_symbol(french_tem[i]);//when storing hyper-graph, we need this
				}else
					french[i]= Symbol.add_terminal_symbol(french_tem[i]);
			}
			
			//english side
			String[] english_tem= fds[2].split("\\s+");
			english = new int[english_tem.length];			
			for(int i=0; i< english_tem.length; i++){				
				if(is_non_terminal(english_tem[i])==true){
					english[i]= Symbol.add_non_terminal_symbol(english_tem[i]);
				}else
					english[i]=Symbol.add_terminal_symbol(english_tem[i]);
			}
			
			String[] t_scores = fds[3].split("\\s+");
			feat_scores = new float[t_scores.length];
			int i=0;
			for(String score : t_scores)
				feat_scores[i++] = (new Float(score)).floatValue();
			
			tem_estcost += estimate_rule();//estimate lower-bound, and set statelesscost, this must be called
		}

		
		/* set the stateless cost, and set a lower-bound
		 * estimate inside the rule returns full estimate.
		 */
		protected float estimate_rule() {
			if (null == TMGrammar.p_l_models) {
				return 0;
			}
			
			// Need to cast because java complains about loosing precision
			float estcost      = (float)0.0;
			this.statelesscost = (float)0.0;
			
			for (FeatureFunction ff : TMGrammar.p_l_models) {
				double mdcost = ff.estimate(this) * ff.getWeight();
				estcost += mdcost;
				if (! ff.isStateful()) {
					this.statelesscost += mdcost;
				}
			}
			this.est_cost = estcost;
			return estcost;
		}
		

		protected void print_info(int level){
			//Support.write_log("Rule is: "+ lhs + " ||| " + Support.arrayToString(french, " ") + " ||| " + Support.arrayToString(english, " ") + " |||", level);
			Support.write_log("Rule is: "+ Symbol.get_string(lhs) +  " ||| ", level);
			 for(int i=0; i<english.length;i++)
				 Support.write_log(Symbol.get_string(english[i]) +" ", level);
			Support.write_log("||| ", level);
			for(int i=0; i< feat_scores.length; i++){
				Support.write_log(" " + feat_scores[i],level);
			}			
			Support.write_log_line(" ||| owner: " + owner + "; statelesscost: " + String.format("%.3f",statelesscost) + String.format("; estcost: %.3f",est_cost)+"; arity:" + arity,level);
		}
		
		
		protected static Comparator<Rule_Memory> NegtiveCostComparator
			= new Comparator<Rule_Memory>() {
				public int compare(Rule_Memory rule1, Rule_Memory rule2) {
					float cost1 = rule1.est_cost;
					float cost2 = rule2.est_cost;
					if (cost1 > cost2) {
						return -1;
					} else if (cost1 == cost2) {
						return 0;
					} else {
						return 1;
					}
				}
			};
	}
	

}

