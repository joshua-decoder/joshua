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

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(BasicRuleCollection.class.getName());
	
	/** 
	 * Indicates whether the rules in this collection have been
	 * sorted based on the latest feature function values.
	 */
	protected boolean sorted;
	
	/** List of rules stored in this collection. */
	protected final List<Rule> rules;
	
	/** Number of nonterminals in the source pattern. */
	protected int arity;
	
	/**
	 * Sequence of terminals and nonterminals in the source
	 * pattern.
	 */
	protected int[] sourceTokens;
	
	/**
	 * Constructs an initially empty rule collection.
	 * 
	 * @param arity Number of nonterminals in the source pattern
	 * @param sourceTokens Sequence of terminals and nonterminals
	 *                     in the source pattern
	 */
	public BasicRuleCollection(int arity, int[] sourceTokens) {
		this.rules = new ArrayList<Rule>();
		this.sourceTokens = sourceTokens;
		this.arity = arity;
		this.sorted = false;
	}
	
	/**
	 * Constructs a rule collection with the given data.
	 * <p>
	 * The list of rules must already be sorted
	 * <p>
	 * NOTE: if rules==null, the rule member variable will be
	 * initialized to an <em>immutable</em> empty list.
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
		this.sorted = true;
	}
	
	/* See Javadoc comments for RuleCollection interface. */
	public int getArity() {
		return this.arity;
	}
	
	public List<Rule> getRules(){
		return this.rules;
	}
	
	public static void sortRules(List<Rule> rules, List<FeatureFunction> l_models) {
		
		// use a priority queue to help sort
		PriorityQueue<Rule> t_heapRules = new PriorityQueue<Rule>(1, Rule.NegtiveCostComparator);
		for (Rule rule : rules) {
//			if (null != l_models) {
				rule.estimateRuleCost(l_models);
//			}
			t_heapRules.add(rule);
		}
		
		// rearange the sortedRules based on t_heapRules
		rules.clear();
		while (t_heapRules.size() > 0) {
			Rule t_r = t_heapRules.poll();
			rules.add(0, t_r);
		}
	}
	
	/* See Javadoc comments for RuleCollection interface. */
	public void sortRules(List<FeatureFunction> l_models) {	
		sortRules(this.rules, l_models);
		this.sorted = true;
	}
	
	/* See Javadoc comments for RuleCollection interface. */
	public List<Rule> getSortedRules() {	
		if (!this.sorted) {
			String message = "Grammar has not been sorted which is reqired by cube pruning; " +
				"sortGrammar should have been called after loading the grammar, but was not.";
			logger.severe(message);			
			throw new UnsortedRuleCollectionException(message);
		}
		
		return this.rules;
	}

	/* See Javadoc comments for RuleCollection interface. */
	public int[] getSourceSide() {
		return this.sourceTokens;
	}
}
