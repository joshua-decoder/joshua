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
package joshua.decoder.chart_parser;


import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Symbol;
import joshua.decoder.chart_parser.Bin.ComputeItemResult;
import joshua.decoder.chart_parser.DotChart.DotItem;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.MemoryBasedRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.TrieGrammar;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.lattice.Lattice;
import joshua.lattice.Arc;
import joshua.lattice.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Chart class
 * this class implements chart-parsing: 
 * (1) seeding the chart 
 * (2) cky main loop over bins, 
 * (3) identify applicable rules in each bin
 * Note: the combination operation will be done in Bin
 * 
 * Signatures of class:
 * Bin: i, j
 * SuperItem (used for CKY check): i,j, lhs
 * Item (or node): i,j, lhs, edge ngrams
 * Deduction (and node)
 * 
 * index of sentences: start from zero
 * index of cell: cell (i,j) represent span of words indexed [i,j-1] where i is in [0,n-1] and j is in [1,n]
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class Chart {

    public int UNTRANS_OWNER_SYM_ID = 0;//untranslated word id
	
	public  Grammar[]        grammars;
	public  DotChart[]       dotcharts;//each grammar should have a dotchart associated with it
	public  Bin[][]          bins;//note that in some cell, it might be null
	private Bin              goal_bin;
	
	private Lattice<Integer> sentence;//a list of foreign words
	//private int[] sentence;
	public  int              sent_len;//foreign sent len
	private int              sent_id;
	
	//decoder-wide variables
	ArrayList<FeatureFunction> p_l_models;
	
	Symbol p_symbol;
	
	//statistics
	int gtem                  = 0;
	int n_prepruned           = 0;//how many items have been pruned away because its cost is greater than the cutoff in calling chart.add_deduction_in_chart()
	int n_prepruned_fuzz1     = 0;
	int n_prepruned_fuzz2     = 0;
	int n_pruned              = 0;
	int n_merged              = 0;
	int n_added               = 0;
	int n_dotitem_added       = 0;//note: there is no pruning in dot-item
	int n_called_compute_item = 0;
	
	//time-profile variables, debug purpose
	long        g_time_compute_item      = 0;
	long        g_time_add_deduction     = 0;
	static long g_time_lm                = 0;
	static long g_time_score_sent        = 0;
	static long g_time_check_nonterminal = 0;
	static long g_time_kbest_extract     = 0;
	
	private static final Logger logger = Logger.getLogger(Chart.class.getName());
	
	
	
	
	//public Chart(int[] sentence_in, ArrayList<Model> models, int sent_id1) {
//	public Chart(Lattice<Integer> sentence_in, ArrayList<Model> models, int sent_id1) {
	public Chart(
		Lattice<Integer>           sentence_,
		//int[] sentence,
		ArrayList<FeatureFunction> models_,
		Symbol symbol_,
		int                        sent_id_,
		Grammar[]                  grammars_,
		ArrayList<Integer>         default_nonterminals,
		String untranslated_owner_,
		boolean have_lm_model
	) {	
//		public Chart(int[] sentence_in, ArrayList<FeatureFunction> models, int sent_id1) {   
		this.sentence = sentence_;
		//this.sent_len = sentence.length;
		this.sent_len = sentence.size() - 1;
		this.p_l_models   = models_;
		this.p_symbol = symbol_;
		this.bins     = new Bin[sent_len][sent_len+1];		
		this.sent_id  = sent_id_;
		this.goal_bin = new Bin(this);
		this.UNTRANS_OWNER_SYM_ID = this.p_symbol.addTerminalSymbol(untranslated_owner_);
		
		/** add un-translated words into the chart as item (with large cost) */
		//TODO: grammar specific?
		this.grammars  = grammars_;
		this.dotcharts = new DotChart[this.grammars.length];//each grammar will have a dot chart
		for (int i = 0; i < this.grammars.length; i++) {
			this.dotcharts[i] = new DotChart(this.sentence,	this.grammars[i], this);
			this.dotcharts[i].seed(); // TODO: should fold into the constructor
		}
		//add OOV rules
		//TODO: the transition cost for phrase model, arity penalty, word penalty are all zero, except the LM cost
//		if (sentence_str.length+1 != this.sent_len) {
//			if (logger.isLoggable(Level.SEVERE)) logger.severe(
//				"In Chart constructor, length of (?)integerized string(?) does not match length of integerized lattice");
//			System.exit(1);
//		}
		for (Node<Integer> node : sentence) {
			for (Arc<Integer> arc : node.getOutgoingArcs()) {
				for (int lhs : default_nonterminals) {//create a rule, but do not add into the grammar trie     
					Rule rule = Rule.constructOOVRule(p_l_models, p_l_models.size(), BatchGrammar.OOV_RULE_ID, lhs, arc.getLabel(), this.UNTRANS_OWNER_SYM_ID, have_lm_model);
					// Tail and head are switched - FIX names:
					add_axiom(node.getNumber(), arc.getTail().getNumber(), rule, (float)arc.getCost());
				}
			}
		}
		if (logger.isLoggable(Level.FINE)) logger.fine("####finished seeding");
	}
	
	
	/** construct the hypergraph with the help from DotChart */
	public HyperGraph expand() {
		//long start = System.currentTimeMillis();
		long time_step1 = 0;
		long time_step2 = 0;
		long time_step3 = 0;
		long time_step4 = 0;
		
		for (int width = 1; width <= sent_len; width++) {
			for (int i = 0; i <= sent_len-width; i++) {
				int j = i + width;
				//Support.write_log_line(String.format("Process span (%d, %d)",i,j), Support.DEBUG);
				
				//(1)### expand the cell in dotchart
				//long start_step1= Support.current_time();
				//Support.write_log_line("Step 1: expance cell", Support.DEBUG);
				for (int k = 0; k < this.grammars.length; k++) {
					this.dotcharts[k].expand_cell(i,j);
				}			
				//Support.write_log_line(String.format("n_dotitem= %d",n_dotitem_added), Support.INFO);
				//time_step1 += Support.current_time()-start_step1;
				
				//(2)### populate COMPLETE rules into Chart: the regular CKY part
				//long start_step2= Support.current_time();
				//Support.write_log_line("Step 2: add complte items into Chart", Support.DEBUG);
				for (int k = 0; k < this.grammars.length; k++) {
					if (this.grammars[k].hasRuleForSpan(i, j, sent_len)
					&& null != this.dotcharts[k].l_dot_bins[i][j]) {
						for (DotItem dt: this.dotcharts[k].l_dot_bins[i][j].l_dot_items) {
							float lattice_cost = dt.lattice_cost;
							RuleCollection rules = dt.tnode.getRules();
							if (null != rules) {//have rules under this trienode
								if (rules.getArity() == 0) {//rules without any non-terminal
									List<Rule> l_rules = rules.getSortedRules();
									for (Rule rule : l_rules) {
										add_axiom(i, j, rule, lattice_cost);
									}
								} else {//rules with non-terminal
									if (JoshuaConfiguration.use_cube_prune) {
										complete_cell_cube_prune(i, j, dt, rules, lattice_cost);
									} else {
										complete_cell(i, j, dt, rules, lattice_cost);//populate chart.bin[i][j] with rules from dotchart[i][j]
									}
								}
							}
						}
					}
				}
				//time_step2 += Support.current_time()-start_step2;
				
				//(3)### process unary rules (e.g., S->X, NP->NN), just add these items in chart, assume acyclic
				//long start_step3= Support.current_time();
				//Support.write_log_line("Step 3: add unary items into Chart", Support.DEBUG);
				for (int k = 0; k < this.grammars.length; k++) {
					if(this.grammars[k].hasRuleForSpan(i, j, sent_len)) {
						add_unary_items(this.grammars[k],i,j);//single-branch path
					}
				}
				//time_step3 += Support.current_time()-start_step3;
				
				//(4)### in dot_cell(i,j), add dot-items that start from the /complete/ superIterms in chart_cell(i,j)
				//long start_step4= Support.current_time();
				//Support.write_log_line("Step 4: init new dot-items that starts from complete items in this cell", Support.DEBUG);
				for (int k = 0; k < this.grammars.length; k++) {
					if (this.grammars[k].hasRuleForSpan(i, j, sent_len)) {
						this.dotcharts[k].start_dotitems(i,j);
					}
				}
				//time_step4 += Support.current_time()-start_step4;
				
				//(5)### sort the items in the cell: for pruning purpose
				//Support.write_log_line(String.format("After Process span (%d, %d), called:= %d",i,j,n_called_compute_item), Support.INFO);
				if (null != this.bins[i][j]) {
					//this.bins[i][j].print_info(Support.INFO);
					ArrayList<HGNode> l_s_its = this.bins[i][j].get_sorted_items();//this is required
					
					/*sanity check with this cell
					int sum_d=0; double sum_c =0.0;	double sum_total=0.0;
					for(Item t_item : l_s_its){
						if(t_item.l_deductions!=null)
							sum_d += t_item.l_deductions.size();
						sum_c += t_item.best_deduction.best_cost;
						sum_total += t_item.est_total_cost;
					}
					//System.out.println(String.format("n_items =%d; n_deductions: %d; s_cost: %.3f; c_total: %.3f", this.bins[i][j].tbl_items.size(),sum_d,sum_c,sum_total));*/
				}
				//print_info(Support.INFO);
			}
		}
		print_info(Level.FINE);
		System.err.println("sent_len = " + sent_len);
		//transition_final: setup a goal item, which may have many deductions
		if (null != this.bins[0][sent_len]) {
			goal_bin.transit_to_goal(this.bins[0][sent_len]);//update goal_bin				
		} else {
			if (logger.isLoggable(Level.SEVERE)) logger.severe(
				"No complete item in the cell(0,n)");
			System.exit(1);
		}
		
		//debug purpose
		//long sec_consumed = (System.currentTimeMillis() -start)/1000;
		//Support.write_log_line("######Expand time consumption: "+ sec_consumed, Support.INFO);
		//Support.write_log_line(String.format("Step1: %d; step2: %d; step3: %d; step4: %d", time_step1, time_step2, time_step3, time_step4), Support.INFO);
		
		/*Support.write_log_line(String.format("t_compute_item: %d; t_add_deduction: %d;", g_time_compute_item/1000,g_time_add_deduction/1000), Support.INFO);
		for(FeatureFunction m: this.models){
			Support.write_log_line("FeatureFunction cost: " + m.time_consumed/1000, Support.INFO);
		}*/

		//Support.write_log_line(String.format("t_lm: %d; t_score_lm: %d; t_check_nonterminal: %d", g_time_lm, g_time_score_sent, g_time_check_nonterminal), Support.INFO);
		//LMModel tm_lm = (LMModel)this.models.get(0);
		//Support.write_log_line(String.format("LM lookupwords1, step1: %d; step2: %d; step3: %d",tm_lm.time_step1,tm_lm.time_step2,tm_lm.time_step3),Support.INFO);
		//debug end
		
		return new HyperGraph((HGNode)goal_bin.get_sorted_items().get(0),	-1,	-1,	sent_id, sent_len);//num_items/deductions : -1
	}
	
	public void print_info(Level level) {
		if (logger.isLoggable(level)) 
			logger.log(level,
				String.format("ADD: %d; MERGED: %d; pruned: %d; pre-pruned: %d ,fuzz1: %d, fuzz2: %d; n_dotitem_added: %d",
					this.n_added,
					this.n_merged,
					this.n_pruned,
					this.n_prepruned,
					this.n_prepruned_fuzz1,
					this.n_prepruned_fuzz2,
					this.n_dotitem_added));
	}
	
	/**
	 * agenda based extension: this is necessary in case more than two unary rules can be applied in topological order s->x; ss->s
	 * for unary rules like s->x, once x is complete, then s is also complete
	 */
	private int add_unary_items(Grammar gr, int i, int j) {
		Bin chart_bin = this.bins[i][j];
		if (null == chart_bin) {
			return 0;
		}
		int count_of_additions_to_t_queue = 0;
		ArrayList<HGNode> t_queue
			= new ArrayList<HGNode>(chart_bin.get_sorted_items());
		
		
		while (t_queue.size() > 0) {
			HGNode item = (HGNode)t_queue.remove(0);
			TrieGrammar child_tnode = gr.getTrieRoot().matchOne(item.lhs);//match rule and complete part
			if (child_tnode != null
			&& child_tnode.getRules() != null
			&& child_tnode.getRules().getArity() == 1) {//have unary rules under this trienode					
				ArrayList<HGNode> l_ants = new ArrayList<HGNode>();
				l_ants.add(item);
				List<Rule> l_rules = child_tnode.getRules().getSortedRules();
				
				for (Rule rule : l_rules){//for each unary rules								
					ComputeItemResult tbl_states = chart_bin.compute_item(rule, l_ants, i, j);				
					HGNode res_item = chart_bin.add_deduction_in_bin(tbl_states, rule, i, j, l_ants, 0.0f);
					if (null != res_item) {
						t_queue.add(res_item);
						count_of_additions_to_t_queue++;
					}
				}
			}
		}
		return count_of_additions_to_t_queue;
	}
	
	
	/** axiom is for rules with zero-arity */
	private void add_axiom(int i, int j, Rule rule, float lattice_cost) {
		if (null == this.bins[i][j]) {
			this.bins[i][j] = new Bin(this);
		}
		this.bins[i][j].add_axiom(i, j, rule, lattice_cost);
	}
	
	
	private void complete_cell(int i, int j, DotItem dt, RuleCollection rb, float lattice_cost) {
		if (null == this.bins[i][j]) {
			this.bins[i][j] = new Bin(this);
		}
		this.bins[i][j].complete_cell(i, j, dt.l_ant_super_items, rb, lattice_cost);//combinations: rules, antecent items
	}
	
	
	private void complete_cell_cube_prune(
		int i,
		int j,
		DotItem dt,
		RuleCollection rb,
		float lattice_cost
	) {
		if (null == this.bins[i][j]) {
			this.bins[i][j] = new Bin(this);
		}
		this.bins[i][j].complete_cell_cube_prune(i, j, dt.l_ant_super_items, rb, lattice_cost);//combinations: rules, antecent items
	}
}
