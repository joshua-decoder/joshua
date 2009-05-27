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

import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.Trie;
import joshua.corpus.vocab.SymbolTable;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a memory-based bilingual BatchGrammar.
 * <p>
 * The rules are stored in a trie. Each trie node has:
 * (1) RuleBin: a list of rules matching the french sides so far
 * (2) A HashMap  of next-layer trie nodes, the next french word
 *     used as the key in HashMap
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class MemoryBasedBatchGrammar extends BatchGrammar {
	
//===============================================================
// Instance Fields
//===============================================================
	
	static protected double tem_estcost = 0.0;
	
	protected int qtyRulesRead = 0;
	protected int qtyRuleBins  = 0;
	protected MemoryBasedTrie root = null;
	
	//protected ArrayList<FeatureFunction> featureFunctions = null;
	protected int defaultOwner;
	
	/**
	 * the OOV rule should have this lhs, this should be grammar
	 * specific as only the grammar knows what LHS symbol can
	 * be combined with other rules
	 */ 
	protected int defaultLHS; 
	
	protected int goalSymbol;
	
	protected int spanLimit = 10;
	private final SymbolTable symbolTable;

	protected GrammarReader<BilingualRule> modelReader;
	
//===============================================================
// Static Fields
//===============================================================

	public static int OOV_RULE_ID = 0;

	/* Three kinds of rules: 
	 * 		regular rule (id>0)
	 * 		oov rule (id=0)
	 * 		null rule (id=-1)
	 */
	
	static int rule_id_count = 1;
		
	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(MemoryBasedBatchGrammar.class.getName());

//===============================================================
// Constructors
//===============================================================

	public MemoryBasedBatchGrammar() {
		symbolTable = null;
	}
	
	public MemoryBasedBatchGrammar(
			String formatKeyword,
			String grammarFile, 
			SymbolTable symbolTable, 
			String defaultOwner,
			String defaultLHSSymbol,
			String goalSymbol,
			int span_limit) throws IOException 
	{
		
		this.symbolTable  = symbolTable;
		this.defaultOwner = this.symbolTable.addTerminal(defaultOwner);
		this.defaultLHS   = this.symbolTable.addNonterminal(defaultLHSSymbol);
		this.goalSymbol   = this.symbolTable.addNonterminal(goalSymbol);
		this.spanLimit    = span_limit;
		
		this.root = new MemoryBasedTrie();
		
		//==== loading grammar
		this.modelReader = createReader(formatKeyword, grammarFile, symbolTable);
		if (modelReader != null) {
			modelReader.initialize();
			for (BilingualRule rule : modelReader)
				addRule(rule);
		} else {
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Couldn't create a GrammarReader for file " + grammarFile + " with format " + formatKeyword);
		}

		this.print_grammar();
	}
	
	protected GrammarReader<BilingualRule> createReader(String formatKeyword,
			String grammarFile, SymbolTable symbolTable) 
	{
		if ("hiero".equals(formatKeyword)) {
			return new HieroFormatReader(grammarFile, symbolTable);
		} else if ("samt".equals(formatKeyword)) {
			return new SamtFormatReader(grammarFile, symbolTable);
		} else {
			// TODO: throw something?
			// TODO: add special warning if "heiro" mispelling is used
			
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Unknown GrammarReader format " + formatKeyword);
			
			return null;
		}
	}
	
	
//===============================================================
// Methods
//===============================================================

	public int getNumRules() {
		return this.qtyRulesRead;
	}


	public Rule constructOOVRule(int qtyFeatures, int sourceWord,  boolean hasLM) {
		int[] p_french      = new int[1];
		p_french[0]         = sourceWord;
		int[] english       = new int[1];
		english[0]          = sourceWord;
		float[] feat_scores = new float[qtyFeatures];
		
		// TODO: This is a hack to make the decoding without a LM works
		// no LM is used for decoding, so we should set the stateless cost
		if (! hasLM) { 
			//this.feat_scores[0]=100.0/(this.featureFunctions.get(0)).getWeight();
			feat_scores[0] = 100;
		}
		
		return new BilingualRule(this.defaultLHS, p_french, english, feat_scores, 0, this.defaultOwner, 0, getOOVRuleID());
	}

	public int getOOVRuleID() {
		return OOV_RULE_ID;
	}
	
	
	public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores, int arity) {
		return new BilingualRule(lhs, sourceWords, targetWords, scores, arity, this.defaultOwner, 0, getOOVRuleID());
	}
	
	
	
	
	/** 
	 * if the span covered by the chart bin is greater than the
	 * limit, then return false
	 */
	public boolean hasRuleForSpan(int startIndex,	int endIndex,	int pathLength) {
		if (this.spanLimit == -1) { // mono-glue grammar
			return (startIndex == 0);
		} else {
			return (endIndex - startIndex <= this.spanLimit);
		}
	}
	
	public Trie getTrieRoot() {
		return this.root;
	}

	protected void addRule(BilingualRule rule) {
		
		// TODO: Why two increments? 
		this.qtyRulesRead++;
		rule_id_count++;

		rule.setRuleID(rule_id_count);
		rule.setOwner(defaultOwner);
		
		// TODO: make sure costs are calculated here or in reader
		tem_estcost += rule.getEstCost();
		
		// identify the position, and insert the trie nodes as necessary
		MemoryBasedTrie pos = root;
		int[] p_french = rule.getFrench();
		for (int k = 0; k < p_french.length; k++) {
			int cur_sym_id = p_french[k];
			if (this.symbolTable.isNonterminal(p_french[k])) { 
				cur_sym_id = modelReader.cleanNonTerminal(p_french[k]);
			}
			
			MemoryBasedTrie next_layer = pos.matchOne(cur_sym_id);
			if (null == next_layer) {
				next_layer = new MemoryBasedTrie();
				if (pos.hasExtensions() == false) {
					pos.tbl_children = new HashMap<Integer, MemoryBasedTrie>();
				}
				pos.tbl_children.put(cur_sym_id, next_layer);
			}
			pos = next_layer;
		}
		
		this.insertRule(pos, rule);
	}
	
	protected void insertRule(MemoryBasedTrie pos, BilingualRule rule) {
		// add the rule into the trie node
		if (! pos.hasRules()) {
			pos.rule_bin = new MemoryBasedRuleBin(rule.getArity(), rule.getFrench());
			this.qtyRuleBins++;
		}
		
		pos.rule_bin.addRule(rule);
	}

	
	// BUG: This always prints 0 for all fields
	protected void print_grammar() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format(
				"####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",
				this.qtyRulesRead, this.qtyRuleBins, 0, tem_estcost));
		}
		/*if(root!=null)
			root.print_info(Support.DEBUG);*/
	}

	
}
