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
package joshua.discriminative.monolingual_parser;


import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.MonolingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hiero.MemoryBasedRuleBin;
import joshua.decoder.ff.tm.hiero.MemoryBasedTrie;

/**
 * this class implements MemoryBasedBatchGrammar
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-03-09 12:52:29 -0400 (  2009) $
 */

public class MonolingualGrammar  extends BatchGrammar {
	/*TMGrammar is composed by Trie nodes
	Each trie node has: 
	(1) RuleBin: a list of rules matching the french sides so far
	(2) a HashMap  of next-layer trie nodes, the next french word used as the key in HashMap  
	*/
	
//	===============================================================
//	 Instance Fields
//	===============================================================
		
	protected int qtyRulesRead    = 0;
	protected int qtyRuleBins     = 0;
	protected MemoryBasedTrie root = null;	

	boolean addFakeFeatScoreForEM = false;//if this grammar is for EM, we will add a fake feature score for each rule
	

	protected int defaultOwner;
	protected int defaultLHS;
	protected int goalSymbol;
		
	protected int spanLimit = 10;
	SymbolTable symbolTable = null;

	
	protected GrammarReader<MonolingualRule> modelReader;
	
//	===============================================================
//	 Static Fields
//	===============================================================

	public    static int OOV_RULE_ID          = 0;
	
	private static final Logger logger = Logger.getLogger(MonolingualGrammar.class.getName());
	
	static int ruleIDCount =1; //three kinds of rule: regular rule (id>0); oov rule (id=0), and null rule (id=-1)
	
	static protected double tem_estcost = 0.0;//debug

	
	
	public MonolingualGrammar(){
		//do nothing
	}
	

	public MonolingualGrammar(
		String formatKeyword,
		SymbolTable psymbolTable,
		String grammarFile,
		String                     default_owner,
		String defaultLHSSymbol,
		String goalSymbol,
		int                        span_limit,
		boolean addFakeFeatScoreForEM_
	) throws IOException {		
		this.symbolTable = psymbolTable;
		this.defaultOwner             = symbolTable.addTerminal(default_owner);
		this.defaultLHS = this.symbolTable.addNonterminal(defaultLHSSymbol);
		this.goalSymbol = this.symbolTable.addNonterminal(goalSymbol);		
		this.spanLimit = span_limit;
		this.addFakeFeatScoreForEM = addFakeFeatScoreForEM_;
		
		this.root = new MemoryBasedTrie();
		
		////==== loading grammar
		this.modelReader = createReader(formatKeyword, grammarFile, symbolTable);
		if (modelReader != null) {
			modelReader.initialize();
			for (MonolingualRule rule : modelReader)
				addRule(rule);
		}

		this.printGrammar();
		
	}
	
	
	protected GrammarReader<MonolingualRule> createReader(String formatKeyword,
			String grammarFile, SymbolTable symbolTable) 
	{
		if ("monolingual".equals(formatKeyword)) {
			return new MonolingualGrammarReader(grammarFile, symbolTable, addFakeFeatScoreForEM);
		} else {
			logger.severe("wrong grammar formatKeyword: " + formatKeyword);
			return null;
		}
	}
	
	
		
//	===============================================================
//	 Methods
//	===============================================================
	
	
	public int getNumRules() {
		return qtyRulesRead;
	}
	
	public Rule constructOOVRule(int num_feats, int sourceWord, int targetWord, boolean have_lm_model) {
		int[] p_french     = new int[1];
	   	p_french[0]  = sourceWord;
	   
	   	float[] feat_scores;
		if(addFakeFeatScoreForEM)
			feat_scores = new float[num_feats+1];
		else
			feat_scores = new float[num_feats];
		
	   	/**TODO
	   	 * This is a hack to make the decoding without a LM works
	   	 * */
	   	if(have_lm_model==false){//no LM is used for decoding, so we should set the stateless cost
	   		//this.feat_scores[0]=100.0/((FeatureFunction)p_l_models.get(0)).getWeight();//TODO
	   		feat_scores[0]=100;//TODO
	   	}
	   	
		return new MonolingualRule(this.defaultLHS, p_french, feat_scores,  0, this.defaultOwner, 0, getOOVRuleID());
	}
	
	public int getOOVRuleID() {
		return OOV_RULE_ID;
	}
	
	
	
	
	/** 
	 * if the span covered by the chart bin is greater than the limit, 
	 * then return false 
	 **/
	// TODO: catch glue grammar case in glue grammar class?
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

	protected void addRule(MonolingualRule rule) {
		
		// TODO: Why two increments? 
		this.qtyRulesRead++;
		ruleIDCount++;

		rule.setRuleID(ruleIDCount);
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
					pos.setExtensions( new HashMap<Integer, MemoryBasedTrie>() );
				}
				pos.getExtensionsTable().put(cur_sym_id, next_layer);
			}
			pos = next_layer;
		}
		
		this.insertRule(pos, rule);
	}
	
	protected void insertRule(MemoryBasedTrie pos, MonolingualRule rule) {
		// add the rule into the trie node
		if (! pos.hasRules()) {
			pos.setRuleBin( new MemoryBasedRuleBin(rule.getArity(), rule.getFrench()) );
			this.qtyRuleBins++;
		}
		
		((MemoryBasedRuleBin)pos.getRules()).addRule(rule);
	}
		
	protected void printGrammar() {
		if (logger.isLoggable(Level.INFO)) {
			logger.info("###########Grammar###########");
			logger.info(String.format("####num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f", this.qtyRulesRead, this.qtyRuleBins, 0, tem_estcost));
		}
		/*if(root!=null)
			root.print_info(Support.DEBUG);*/
	}
	

	
	//====================== functions for EM training ==========================
	/**We use 
	 * the last field of featScores to store the posteriorProb collected during E step
	 * the first field of featScores to store the normalized cost in the M step 
	 */
	public static float incrementRulePosteriorProb(Rule rl, double posteriorProb){
		return rl.incrementFeatureScore(rl.getFeatureScores().length-1, posteriorProb);
	} 
	public static float getRulePosteriorProb(Rule rl){
		return rl.getFeatureCost(rl.getFeatureScores().length-1);
	} 
	public static void resetRulePosteriorProb(Rule rl){
		rl.setFeatureCost(rl.getFeatureScores().length-1, 0);
	}
	public static float getRuleNormalizedCost(Rule rl){
		return rl.getFeatureCost(0);
	}
	static float CEILING_COST = 100;
	public static void setRuleNormalizedCost(Rule rl, float prob){
		float cost = (float) -Math.log(prob);
		if(cost>CEILING_COST)
			cost = CEILING_COST;
		rl.setFeatureCost(0, cost);		
	}
	
	
	


	public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores, int aritity) {
		// TODO Auto-generated method stub
		return null;
	}



}