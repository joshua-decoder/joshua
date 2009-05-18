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
import java.util.Comparator;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;


/**
 * This class define the interface for Rule. Normally, the feature
 * score in the rule should be *cost* (i.e., -LogP), so that the
 * feature weight should be positive
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface Rule {
	
//===============================================================
// Attributes
//===============================================================
	
	public void setRuleID(int id);
	public int  getRuleID();
	
	public void setArity(int arity);
	public int  getArity();
	
	public void setOwner(int ow);
	public int  getOwner();
	
	public void setLHS(int lhs);
	public int  getLHS();
	
	public void  setEnglish(int[] eng);
	public int[] getEnglish();
	
	public void  setFrench(int[] french);
	public int[] getFrench();
	
	public void    setFeatureScores(float[] scores);
	public float[] getFeatureScores();
	
	
	/**
	 * the following methods will be useful when we store a
	 * non-standard score in the last field of a rule's
	 * feat_scores, for example, during EM training, we can
	 * store the soft-count in it.
	 *
	 * column: start from zero
	 */
	public void  setFeatureScore(int column, float score);
	public float getFeatureScore(int column);	
	public float incrementFeatureScore(int column, double score);
	
	public void  setLatticeCost(float cost);
	public float getLatticeCost();
	
	// How does this differ from estimateRuleCost ?
	public float getEstCost();
	
	
//===============================================================
// Methods
//===============================================================
	
	/**
	 * Set a lower-bound estimate inside the rule returns full
	 * estimate.
	 */
	public float estimateRuleCost(ArrayList<FeatureFunction> featureFunctions);
	
	
	/**
	 * In order to provide sorting for cube-pruning, we need
	 * to provide this Comparator.
	 */
	public static Comparator<Rule> NegtiveCostComparator = new Comparator<Rule>() {
		public int compare(Rule rule1, Rule rule2) {
			float cost1 = rule1.getEstCost();
			float cost2 = rule2.getEstCost();
			if (cost1 > cost2) {
				return -1;
			} else if (cost1 == cost2) {
				return 0;
			} else {
				return 1;
			}
		}
	};
	
	@Deprecated
	public String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab);
	
	/** Print the rule in terms of Ingeters. */
	@Deprecated
	public String toString(); 
	
	@Deprecated
	public String toString(SymbolTable symbolTable);
	
	@Deprecated
	public String toStringWithoutFeatScores(SymbolTable symbolTable);
}
