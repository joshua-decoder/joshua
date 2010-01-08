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
	
	static private double temEstcost = 0.0;
	
	private int qtyRulesRead = 0;
	private int qtyRuleBins  = 0;
	private MemoryBasedTrie root = null;
	
	//protected ArrayList<FeatureFunction> featureFunctions = null;
	private int defaultOwner;
	
	private float oovFeatureCost = 100;
	
	/**
	 * the OOV rule should have this lhs, this should be grammar
	 * specific as only the grammar knows what LHS symbol can
	 * be combined with other rules
	 */ 
	private int defaultLHS; 
	
	
	private int spanLimit = 10;
	private final SymbolTable symbolTable;

	private GrammarReader<BilingualRule> modelReader;
	
//===============================================================
// Static Fields
//===============================================================

	public static final int OOV_RULE_ID = 0;

	/* Three kinds of rules: 
	 * 		regular rule (id>0)
	 * 		oov rule (id=0)
	 * 		null rule (id=-1)
	 */
	
	static int ruleIDCount = 1;
		
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
			int spanLimit,
			float oovFeatureCost_) throws IOException 
	{
		
		this.symbolTable  = symbolTable;
		this.defaultOwner = this.symbolTable.addTerminal(defaultOwner);
		this.defaultLHS   = this.symbolTable.addNonterminal(defaultLHSSymbol);
		this.spanLimit    = spanLimit;
		this.oovFeatureCost = oovFeatureCost_;
		this.root = new MemoryBasedTrie();
		
		//==== loading grammar
		this.modelReader = createReader(formatKeyword, grammarFile, symbolTable);
		if (modelReader != null) {
			modelReader.initialize();
			for (BilingualRule rule : modelReader)
				if (rule != null) 
					addRule(rule);
		} else {
			if (logger.isLoggable(Level.WARNING))
				logger.warning("Couldn't create a GrammarReader for file " + grammarFile + " with format " + formatKeyword);
		}

		this.printGrammar();
	}
	
	protected GrammarReader<BilingualRule> createReader(String formatKeyword,
			String grammarFile, SymbolTable symbolTable){
		
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


	public Rule constructOOVRule(int qtyFeatures, int sourceWord, int targetWord, boolean hasLM) {
		int[] french      = new int[1];
		french[0]         = sourceWord;
		int[] english       = new int[1];
		english[0]          = targetWord;
		float[] feat_scores = new float[qtyFeatures];
		
		// TODO: This is a hack to make the decoding without a LM works
		/**when a ngram LM is used, the OOV word will have a cost 100.
		 * if no LM is used for decoding, so we should set the cost of some
		 * TM feature to be maximum
		 * */
		if ( (!hasLM) && qtyFeatures > 0) { 
			feat_scores[0] = oovFeatureCost;
		}
		
		return new BilingualRule(this.defaultLHS, french, english, feat_scores, 0, this.defaultOwner, 0, getOOVRuleID());
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
		ruleIDCount++;

		rule.setRuleID(ruleIDCount);
		rule.setOwner(defaultOwner);
		
		// TODO: make sure costs are calculated here or in reader
		temEstcost += rule.getEstCost();
		
		//=== identify the position, and insert the trie nodes as necessary
		MemoryBasedTrie pos = root;
		int[] french = rule.getFrench();
		for (int k = 0; k < french.length; k++) {
			int curSymID = french[k];
			
			/**Note that the nonTerminal symbol in the french is not cleaned (i.e., will be sth 
			 * like [X,1]), but the symbol in the Trie has to be cleaned, so that the match does
			 * not care about the markup (i.e., [X,1] or [X,2] means the same thing, that is X)*/
			if (this.symbolTable.isNonterminal(french[k])) { 
				curSymID = modelReader.cleanNonTerminal(french[k]);
			}
			
			MemoryBasedTrie nextLayer = pos.matchOne(curSymID);
			if (null == nextLayer) {
				nextLayer = new MemoryBasedTrie();
				if (pos.hasExtensions() == false) {
					pos.childrenTbl = new HashMap<Integer, MemoryBasedTrie>();
				}
				pos.childrenTbl.put(curSymID, nextLayer);
			}
			pos = nextLayer;
		}
		
		
		//=== add the rule into the trie node
		if (! pos.hasRules()) {
			pos.ruleBin = new MemoryBasedRuleBin(rule.getArity(), rule.getFrench());
			this.qtyRuleBins++;
		}
		pos.ruleBin.addRule(rule);
	}
	

	
	// BUG: This always prints 0 for all fields
	protected void printGrammar() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format(
				"####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",
				this.qtyRulesRead, this.qtyRuleBins, 0, temEstcost));
		}
		/*if(root!=null)
			root.print_info(Support.DEBUG);*/
	}

	
}
