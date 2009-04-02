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

import java.util.logging.Logger;

import joshua.decoder.Support;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;

/**
 * Contain all rules with the same french side (and thus same arity).
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class MemoryBasedRuleBin extends BasicRuleCollection implements RuleCollection {
	
	@SuppressWarnings("unused")
	private static final Logger logger = 
		Logger.getLogger(MemoryBasedRuleBin.class.getName());
//	
//	protected int arity = 0; // number of non-terminals
//	protected int[] french;
	
	public MemoryBasedRuleBin(int arity, int[] sourceTokens) {
		super(arity, sourceTokens);
	}
	
	
//	protected ArrayList<Rule> sortedRules = new ArrayList<Rule>();
	
	
	/**
	 * TODO: now, we assume this function will be called
	 * only after all the rules have been read; this
	 * method need to be synchronized as we will call
	 * this function only after the decoding begins;
	 * to avoid the synchronized method, we should call
	 * this once the grammar is finished
	 */
//	public void sortRules(ArrayList<FeatureFunction> l_models) {
//		
////		if (!this.sorted) {
////			sortRules(sortedRules, l_models);
////		}
//		
//		this.sorted = true;
//	}
//	
//	public ArrayList<Rule> getSortedRules() {
//		
//		if (!this.sorted) {
//			logger.warning("Sorting rules without using feature function values.");
//			sortRules(null);
//		}
//		
//		return this.sortedRules;
//	}
	
	
//	public int[] getSourceSide() {
//		return this.french;
//	}
	
//	public void setSourceSide(int[] french) {
//		this.french = french;
//	}
	
//	public int getArity() {
//		return this.arity;
//	}
	
//	public void setArity(int arity) {
//		this.arity = arity;
//	}
	
	
	public void addRule(Rule rule) {
		if (rules.size() <= 0) { // first time
			this.arity = rule.getArity();
			this.sourceTokens = rule.getFrench();
		}
		if (rule.getArity() != this.arity) {
			Support.write_log_line(String.format("RuleBin: arity is not matching, old: %d; new: %d", this.arity, rule.getArity()),	Support.ERROR);
			return;
		}
		rules.add(rule);
		sorted = false;
		rule.setFrench(this.sourceTokens); //TODO: this will release the memory in each rule, but each rule still have a pointer to it
	}
}