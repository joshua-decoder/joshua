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
package joshua.decoder.ff.tm.hiero;

import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;

/**
 * Stores a collection of all rules with the same french side (and
 * thus same arity).
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class MemoryBasedRuleBin extends BasicRuleCollection {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(MemoryBasedRuleBin.class.getName());

	/**
	 * Constructs an initially empty rule collection.
	 *
	 * @param arity Number of nonterminals in the source pattern
	 * @param sourceTokens Sequence of terminals and nonterminals
	 *                     in the source pattern
	 */
	public MemoryBasedRuleBin(int arity, int[] sourceTokens) {
		super(arity, sourceTokens);
	}
	
	/**
	 * Adds a rule to this collection.
	 *
	 * @param rule Rule to add to this collection.
	 */
	public void addRule(Rule rule) {
		//XXX This if clause seems bogus.
		if (rules.size() <= 0) { // first time
			this.arity = rule.getArity();
			this.sourceTokens = rule.getFrench();
		}
		if (rule.getArity() != this.arity) {
			if (logger.isLoggable(Level.SEVERE)) logger.finest(String.format("RuleBin: arity is not matching, old: %d; new: %d", this.arity, rule.getArity()));
			return;
		}
		rules.add(rule);
		sorted = false;
		rule.setFrench(this.sourceTokens); //TODO: this will release the memory in each rule, but each rule still have a pointer to it
	}
}
