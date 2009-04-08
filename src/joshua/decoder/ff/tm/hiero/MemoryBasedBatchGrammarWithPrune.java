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

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureFunctionList;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/** 
 * this class implement 
 * (1) load the translation grammar
 * (2) provide a DOT interface
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

/* We should keep this code alive, even though currently nobody uses it; 
 * as we may want to do offline pruning of grammar
 */


//TODO: bug in sortGrammar: even the l_models is new, we do not update the est cost for the rule, which is wrong
public class MemoryBasedBatchGrammarWithPrune extends MemoryBasedBatchGrammar {
	private int num_rule_pruned = 0;
	
	private static final Logger logger =
		Logger.getLogger(MemoryBasedBatchGrammarWithPrune.class.getName());
	
	
	public MemoryBasedBatchGrammarWithPrune(
			String formatKeyword,
			String grammarFile,
			SymbolTable symbolTable, 
			FeatureFunctionList features,
			String defaultOwner, String defaultLHSSymbol, String goalSymbol, 
			int spanLimit) throws IOException 
	{
		super(formatKeyword, grammarFile, symbolTable, features, 
				defaultOwner, defaultLHSSymbol, goalSymbol, spanLimit);
	}
	
	@Override
	protected void insertRule(MemoryBasedTrie pos, BilingualRule rule) {
		if (null == pos.rule_bin) {
			pos.rule_bin        = new MemoryBasedRuleBinWithPrune(rule.getArity(), rule.getFrench());
			this.qtyRuleBins++;
		}
		
		// TODO: the downcasting part isn't too pretty.
		// pruning
		if (rule.getEstCost() > ((MemoryBasedRuleBinWithPrune) pos.rule_bin).cutoff) {
			num_rule_pruned++;
		} else {
			pos.rule_bin.addRule(rule);
			num_rule_pruned += ((MemoryBasedRuleBinWithPrune) pos.rule_bin).run_pruning();
		}
	}
	
	
	protected void print_grammar() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format(
				"####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",
				this.qtyRulesRead, this.qtyRuleBins, num_rule_pruned, tem_estcost));
		}
	}
}
