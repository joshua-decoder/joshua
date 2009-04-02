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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import joshua.decoder.ff.FeatureFunction;

/**
 * Basic collection of translation rules.
 * 
 * @author Lane Schwartz
 * @author Zhifei Li
 * @version $LastChangedDate$
 */
public class BasicRuleCollection implements RuleCollection {

	private static final Logger logger = 
		Logger.getLogger(BasicRuleCollection.class.getName());
	
	protected boolean sorted = false;
	protected final List<Rule> rules;
	
	/** Number of nonterminals */
	protected int arity;
	protected int[] sourceTokens;
	
	
	public BasicRuleCollection(int arity, int[] sourceTokens) {
		this.rules = new ArrayList<Rule>();
		this.sourceTokens = sourceTokens;
		this.arity = arity;
	}
	
	/**
	 * Constructs a rule collection with the given data.
	 * <p>
	 * NOTE: if rules==null, the rule member variable
	 * will be initialized to an <em>immutable</em> empty list.
	 * 
	 * @param arity
	 * @param sourceTokens
	 * @param rules
	 */
	public BasicRuleCollection(int arity, int[] sourceTokens, List<Rule> rules) {
		if (rules==null) {
			this.rules = Collections.<Rule>emptyList();
		} else {
			this.rules = rules;
		}
		this.sourceTokens = sourceTokens;
		this.arity = arity;
	}
	
	public int getArity() {
		return this.arity;
	}
	
	public void sortRules(ArrayList<FeatureFunction> l_models) {
		if (!sorted && l_models!=null) {

			// use a priority queue to help sort
			PriorityQueue<Rule> t_heapRules = new PriorityQueue<Rule>(1, Rule.NegtiveCostComparator);
			for (Rule rule : rules) {
				if (null != l_models) {
					rule.estimateRuleCost(l_models);
				}
				t_heapRules.add(rule);
			}
			
			// rearange the sortedRules based on t_heapRules
			rules.clear();
			while (t_heapRules.size() > 0) {
				Rule t_r = (Rule) t_heapRules.poll();
				rules.add(0, t_r);
			}
		}
		
		this.sorted = true;
	}
	
	public List<Rule> getSortedRules() {
		
		if (!this.sorted) {
			logger.warning("Sorting rules without using feature function values.");
			sortRules(null);
		}
		
		return this.rules;
	}

	public int[] getSourceSide() {
		return this.sourceTokens;
	}
}
