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

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunction;


/**
 * this class define the interface for Rule
 * Normally, the feature score in the rule should be *cost* (i.e., -LogP), 
 * so that the feature weight should be positive
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-03-09 12:52:29 -0400 (星期一, 09 三月 2009) $
 */


public interface Rule {
	
	public void setRuleID(int id);
	
	public int getRuleID();
	
	public void setArity(int arity);
	
	public int getArity();
	
	public void setOwner(int ow);
	
	public int getOwner();
	
	public void setLHS(int lhs);
	
	public int getLHS();
		
	public void setEnglish(int[] eng_);
	
	public int[] getEnglish();
	
	public void setFrench(int[] french_);
	
	public int[] getFrench();
	
	public void setFeatureScores(float[] scores);
	
	public float[] getFeatureScores();
	
	public void setLatticeCost(float cost);
	
	public float getLatticeCost();
	
	public float getEstCost();
	
	/** 
	 * set a lower-bound estimate inside the rule returns full estimate.
	 */
	public float estimateRuleCost(ArrayList<FeatureFunction> p_l_models);
	

	/** in order to provide sorting for cube-pruning, we need to provide this Comparator
	 * */
	public static Comparator<Rule> NegtiveCostComparator	= new Comparator<Rule>() {
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
	
	/**@todo: should the Vocabulary change to SymbolTable?
	 * */
	public String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab);
	
	/**print the rule in terms of Ingeters
	 * */
	public String toString(); 
	
	public String toString(SymbolTable p_symbolTable);
	
	 public String toStringWithoutFeatScores(SymbolTable p_symbolTable);
}
