package joshua.decoder.ff.tm.HieroGrammar;

import java.util.ArrayList;
import java.util.PriorityQueue;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;



//TODO: bug in getSortedRules: even the l_models is new, we do not update the est cost for the rule, which is wrong

/** contain all rules with the same french side (and thus same arity) */
	public class MemoryBasedRuleBinWithPrune	extends MemoryBasedRuleBin {
		static double EPSILON = 0.000001;	
		static int IMPOSSIBLE_COST=99999;//max cost
			
		
		protected double  cutoff =  IMPOSSIBLE_COST;//initial cost
		protected PriorityQueue<Rule> heapRules   = null;//sort the rules based on the stateless cost
		
		
		/**
		 * TODO: now, we assume this function will be called
		 * only after all the rules have been read; this
		 * method need to be synchronized as we will call
		 * this function only after the decoding begins;
		 * to avoid the synchronized method, we should call
		 * this once the grammar is finished
		 * what about the weights changed as in MERT??
		 */
		//public synchronized ArrayList<Rule> get_sorted_rules() {
		public ArrayList<Rule> getSortedRules(ArrayList<FeatureFunction> l_models) {
			if (l_models!=null  || !this.sorted) {
				//TODO: even the l_models is new, we do not update the est cost for the rule, which is wrong
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
		
		
		protected void add_rule(Rule rule) {
			if (null == this.heapRules) {//firt time
				this.heapRules = new PriorityQueue<Rule>(1, Rule.NegtiveCostComparator);//TODO: initial capacity?
				this.arity = rule.getArity();
				this.french = rule.getFrench(); 
			}
			if (rule.getArity() != this.arity) {
				Support.write_log_line(String.format("RuleBin: arity is not matching, old: %d; new: %d", this.arity, rule.getArity()),	Support.ERROR);
				return;
			}
			this.heapRules.add(rule);
			if (rule.getEstRuleCost() + JoshuaConfiguration.rule_relative_threshold < this.cutoff) {
				this.cutoff = rule.getEstRuleCost() + JoshuaConfiguration.rule_relative_threshold;
			}
			rule.setFrench(this.french); //TODO: this will release the memory in each rule, but still have a pointer
		}
		
		
		protected int run_pruning() {
			int n_pruned = 0;			
			while (this.heapRules.size() > JoshuaConfiguration.max_n_rules || this.heapRules.peek().getEstRuleCost() >= this.cutoff) {
				n_pruned++;
				this.heapRules.poll();
				if(this.heapRules.peek()==null){System.out.println("the stack is empty, which might be wrong; cutoff:" + this.cutoff);}
			}
			if (this.heapRules.size() == JoshuaConfiguration.max_n_rules) {
				this.cutoff =
					(this.cutoff < this.heapRules.peek().getEstRuleCost())
					? this.cutoff
					: this.heapRules.peek().getEstRuleCost() + EPSILON;//TODO
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