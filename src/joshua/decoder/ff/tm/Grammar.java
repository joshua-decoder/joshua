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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;

/**
 * Grammar is a class for wrapping a trie of TrieGrammar in order
 * to store holistic metadata.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface Grammar  {
	
	/**
	 * Gets the root of the <code>Trie</code> backing this
	 * grammar.
	 * <p>
	 * <em>Note</em>: This method should run as a small
	 * constant-time function.
	 * 
	 * @return the root of the <code>Trie</code> backing this
	 *         grammar
	 */
	Trie getTrieRoot();
	
	
	
	/**
	 * After calling this method, the rules in this grammar are
	 * guaranteed to be sorted based on the latest feature
	 * function values.
	 * <p>
	 * Cube-pruning requires that the grammar be sorted based
	 * on the latest feature functions.
	 * 
	 * @param models List of feature functions
	 */
	void sortGrammar(List<FeatureFunction> models);
	

	
	/** 
	 * Determines whether the rules in this grammar have been
	 * sorted based on the latest feature function values.
	 * <p>
	 * This method is needed for the cube-pruning algorithm.
	 * 
	 * @return <code>true</code> if the rules in this grammar
	 *         have been sorted based on the latest feature
	 *         function values, <code>false</code> otherwise
	 */
	boolean isSorted();
	
	/**
	 * Returns whether this grammar has any valid rules for
	 * covering a particular span of a sentence. Heiro's "glue"
	 * grammar will only say True if the span is longer than
	 * our span limit, and is anchored at startIndex==0. Heiro's
	 * "regular" grammar will only say True if the span is less
	 * than the span limit. Other grammars, e.g. for rule-based
	 * systems, may have different behaviors.
	 * 
	 * @param startIndex Indicates the starting index 
	 * 		of a phrase in a source input phrase,
	 * 		or a starting node identifier 
	 * 		in a source input lattice
	 * @param endIndex Indicates the ending index 
	 * 		of a phrase in a source input phrase,
	 * 		or an ending node identifier 
	 * 		in a source input lattice
	 * @param pathLength Length of the input path in a source input lattice.
	 * 		If a source input phrase is used instead of a lattice,
	 * 		this value will likely be ignored by the underlying implementation,
	 * 		but would normally be defined as <code>endIndex-startIndex</code>
	 */
	boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength);

		
	/**
	 * Gets the number of rules stored in the grammar.
	 * 
	 * @return the number of rules stored in the grammar
	 */
	int getNumRules();
	
	/**
	 * Construct an out-of-vocabulary (OOV) rule for the word
	 * source. Only called when creating oov rule in Chart or
	 * DiskHypergraph, all the transition cost for phrase model,
	 * arity penalty, word penalty are all zero, except the LM
	 * cost or the first feature if useMaxLMCost==false.
	 *
	 * TODO: will try to get rid of owner, have_lm_model, and num_feats
	 */
	Rule constructOOVRule(int num_feats, int sourceWord, int targetWord, boolean useMaxLMCost);
	
	
	/**
	 * Gets the integer identifier of this grammar's out-of-vocabulary
	 * (OOV) rule.
	 * 
	 * @return the integer identifier of this grammar's
	 *         out-of-vocabulary (OOV) rule
	 */
	int getOOVRuleID();
	
	
	/**
	 * This is used to construct a manual rule supported from
	 * outside the grammar, but the owner should be the same
	 * as the grammar. Rule ID will the same as OOVRuleId, and
	 * no lattice cost
	 */
	Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores, int aritity);
	
	
	void writeGrammarOnDisk(String file, SymbolTable symbolTable);
	
	void changeGrammarCosts(Map<String, Double> weightTbl, HashMap<String, Integer> featureMap, double[] scores, String prefix, int column, boolean negate);
	
	void obtainRulesIDTable(Map<String, Integer> rulesIDTable,  SymbolTable symbolTable); 
}
