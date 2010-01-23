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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;


/**
 * This class define the interface for Rule. Normally, the feature
 * score in the rule should be *cost* (i.e., -LogP), so that the
 * feature weight should be positive.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface Rule {
	
//===============================================================
// Attributes
//===============================================================
	
	void setRuleID(int id);
	int  getRuleID();
	
	void setArity(int arity);
	int  getArity();
	
	void setOwner(int ow);
	int  getOwner();
	
	void setLHS(int lhs);
	int  getLHS();
	
	void  setEnglish(int[] eng);
	int[] getEnglish();
	
	void  setFrench(int[] french);
	int[] getFrench();
	
	void    setFeatureScores(float[] scores);
	float[] getFeatureScores();
	
	
	/**
	 * @param column start from zero
	 */
	void  setFeatureCost(int column, float cost);
	float getFeatureCost(int column);	
	float incrementFeatureScore(int column, double score);
	
	void  setLatticeCost(float cost);
	float getLatticeCost();
	
	// How does this differ from estimateRuleCost ?
	float getEstCost();
	
	
//===============================================================
// Methods
//===============================================================
	
	/**
	 * Set a lower-bound estimate inside the rule returns full
	 * estimate.
	 */
	float estimateRuleCost(List<FeatureFunction> featureFunctions);
	
	
	/**
	 * In order to provide sorting for cube-pruning, we need
	 * to provide this Comparator.
	 */
	Comparator<Rule> NegtiveCostComparator = new Comparator<Rule>() {
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
	String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab);
	
	/** Print the rule in terms of Ingeters. */
	@Deprecated
	String toString(); 
	
	@Deprecated
	String toString(SymbolTable symbolTable);
	
	@Deprecated
	String toStringWithoutFeatScores(SymbolTable symbolTable);
}
