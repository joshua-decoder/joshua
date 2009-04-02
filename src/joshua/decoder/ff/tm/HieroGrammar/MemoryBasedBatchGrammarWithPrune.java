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

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;

import java.io.IOException;
import java.util.HashMap ;
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
 * We should keep this code alive, even though currently nobody uses it; as we may want to do offline pruning of grammar
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class MemoryBasedBatchGrammarWithPrune extends MemoryBasedBatchGrammar {
	private int num_rule_pruned = 0;
	
	private static final Logger logger =
		Logger.getLogger(MemoryBasedBatchGrammarWithPrune.class.getName());
	
	
	public MemoryBasedBatchGrammarWithPrune(
		SymbolTable symbolTable, String grammarFile, boolean isGlueGrammar,
		ArrayList<FeatureFunction> featureFunctions, String defaultOwner,
		int spanLimit,
		String nonterminalRegexp, String nonterminalReplaceRegexp
	) throws IOException {
		super(symbolTable, grammarFile, isGlueGrammar, defaultOwner, spanLimit, nonterminalRegexp, nonterminalReplaceRegexp);
	}
	
	
	protected Rule add_rule(String line, int owner) {
		this.qtyRulesRead++;
		this.qtyRulesRead++;
		rule_id_count++;
		//######1: parse the line
		//######2: create a rule
		//Rule p_rule = new Rule(this,rule_id_count, line, owner);
		Rule p_rule = createRule(this.symbolTable, nonterminalRegexp, nonterminalReplaceRegexp,rule_id_count, line, owner);
		tem_estcost += p_rule.getEstCost();
		
		//######### identify the position, and insert the trinodes if necessary
		MemoryBasedTrie pos = root;
		int[] p_french = p_rule.getFrench();
		for (int k = 0; k < p_french.length; k++) {
			int cur_sym_id = p_french[k];
			if (this.symbolTable.isNonterminal(p_french[k])) { //TODO: p_rule.french store the original format like "[X,1]"
				cur_sym_id = this.symbolTable.addNonterminal(
					replace_french_non_terminal(
						this.nonterminalReplaceRegexp,
						this.symbolTable.getWord(p_french[k])));
			}
			
			MemoryBasedTrie next_layer = pos.matchOne(cur_sym_id);
			if (null != next_layer) {
				pos = next_layer;
			} else {
				MemoryBasedTrie tem = new MemoryBasedTrie();//next layer node
				if (null == pos.tbl_children) {
					pos.tbl_children = new HashMap<Integer,MemoryBasedTrie>();
				}
				pos.tbl_children.put(cur_sym_id, tem);
				pos = tem;
			}
		}
		
		//#########3: now add the rule into the trinode
		if (null == pos.rule_bin) {
			pos.rule_bin        = new MemoryBasedRuleBinWithPrune(p_rule.getArity(), p_french);
//			pos.rule_bin.french = p_french;
//			pos.rule_bin.arity  = p_rule.getArity();
			this.qtyRuleBins++;
		}
		
		
		//==== prune related part===================
		if (p_rule.getEstCost() > ((MemoryBasedRuleBinWithPrune)pos.rule_bin).cutoff) {
			num_rule_pruned++;
		} else {
			pos.rule_bin.addRule(p_rule);
			num_rule_pruned += ((MemoryBasedRuleBinWithPrune)pos.rule_bin).run_pruning();
		}
		
		return p_rule;
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
