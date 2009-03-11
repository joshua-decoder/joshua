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

import java.util.ArrayList;
import java.util.PriorityQueue;
import joshua.decoder.Support;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2009-03-09 14:05:28 -0400 (星期一, 09 三月 2009) $
*/

/** contain all rules with the same french side (and thus same arity) */
	public class MemoryBasedRuleBin	implements RuleCollection {
		protected int arity = 0;//number of non-terminals
		protected int[] french;
		
		protected boolean                    sorted      = false;
		protected ArrayList<Rule>            sortedRules = new ArrayList<Rule>();
	
		/**
		 * TODO: now, we assume this function will be called
		 * only after all the rules have been read; this
		 * method need to be synchronized as we will call
		 * this function only after the decoding begins;
		 * to avoid the synchronized method, we should call
		 * this once the grammar is finished
		 */
		//public synchronized ArrayList<Rule> get_sorted_rules() {
		public ArrayList<Rule> getSortedRules(ArrayList<FeatureFunction> l_models) {
			if (l_models!=null || !this.sorted) {
				//==use a priority queue to help sort
				PriorityQueue<Rule> t_heapRules = new PriorityQueue<Rule>(1, Rule.NegtiveCostComparator);
				for(Rule rule : sortedRules ){	
					if(l_models!=null)
						rule.estimateRuleCost(l_models);
					t_heapRules.add(rule);
				}
				
				//==rearange the sortedRules based on t_heapRules
				this.sortedRules.clear();
				while (t_heapRules.size() > 0) {
					Rule t_r = (Rule) t_heapRules.poll();
					this.sortedRules.add(0, t_r);
				}
				this.sorted    = true;
			}
			return this.sortedRules;
		}
		
		public int[] getSourceSide() {
			return this.french;
		}
		
		public void setSourceSide(int[] french_) {
			this.french = french_;
		}
		
		public int getArity() {
			return this.arity;
		}
		
		public void setArity(int arity_) {
			this.arity = arity_;
		}
		
		
		public void addRule(Rule rule) {
			if (sortedRules.size()<=0) {//first time
				this.arity = rule.getArity();
				this.french = rule.getFrench(); 
			}
			if (rule.getArity() != this.arity) {
				Support.write_log_line(	String.format("RuleBin: arity is not matching, old: %d; new: %d", this.arity, rule.getArity()),	Support.ERROR);
				return;
			}
			sortedRules.add(rule);
			sorted      = false;
			rule.setFrench(this.french); //TODO: this will release the memory in each rule, but each rule still have a pointer to it
		}
	}