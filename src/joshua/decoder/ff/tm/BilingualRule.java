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

import java.util.Arrays;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;


/**
 * Normally, the feature score in the rule should be *cost* (i.e., -LogP), 
 * so that the feature weight should be positive
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class BilingualRule extends MonolingualRule {
	
	private int[] english;
	
//===============================================================
// Constructors
//===============================================================

	/**
	 * Constructs a new rule using the provided parameters.
	 * The owner and rule id for this rule are undefined.
	 * 
	 * @param lhs Left-hand side of the rule.
	 * @param source_rhs Source language right-hand side of the rule.
	 * @param target_rhs Target language right-hand side of the rule.
	 * @param feature_scores Feature value scores for the rule.
	 * @param arity Number of nonterminals in the source language right-hand side.
	 */
	public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, float[] featureScores, int arity, int owner, float latticeCost, int ruleID) {
		super(lhs, sourceRhs, featureScores, arity, owner, latticeCost, ruleID);
		this.english = targetRhs;
	}
	
	
	//called by class who does not care about lattice_cost, rule_id, and owner
	public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, float[] featureScores, int arity) {
		super(lhs, sourceRhs, featureScores, arity);
		this.english = targetRhs;
	}
	
	
//===============================================================
// Attributes
//===============================================================
	
	public final void setEnglish(int[] eng) {
		this.english = eng;
	}
	
	public final int[] getEnglish() {
		return this.english;
	}
	

//===============================================================
// Serialization Methods
//===============================================================
	// TODO: remove these methods
	
	// Caching this method significantly improves performance
	// We mark it transient because it is, though cf java.io.Serializable
	private transient String cachedToString = null;
	
	public String toString(Map<Integer,String> ntVocab, SymbolTable sourceVocab, SymbolTable targetVocab) {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(ntVocab.get(this.getLHS()));
			sb.append("] ||| ");
			sb.append(sourceVocab.getWords(this.getFrench(),true));
			sb.append(" ||| ");
			sb.append(targetVocab.getWords(this.english,false));
			//sb.append(java.util.Arrays.toString(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.getFeatureScores().length; i++) {
//				sb.append(String.format(" %.12f", this.getFeatureScores()[i]));
				sb.append(' ');
				sb.append(Float.toString(this.getFeatureScores()[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
	
	//print the rule in terms of Ingeters
	public String toString() {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(this.getLHS());
			sb.append("] ||| ");
			sb.append(Arrays.toString(this.getFrench()));
			sb.append(" ||| ");
			sb.append(Arrays.toString(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.getFeatureScores().length; i++) {
				sb.append(String.format(" %.4f", this.getFeatureScores()[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
	
	public String toString(SymbolTable symbolTable) {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(symbolTable.getWord(this.getLHS()));
			sb.append("] ||| ");
			sb.append(symbolTable.getWords(this.getFrench()));
			sb.append(" ||| ");
			sb.append(symbolTable.getWords(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.getFeatureScores().length; i++) {
				sb.append(String.format(" %.4f", this.getFeatureScores()[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
	
	public String toStringWithoutFeatScores(SymbolTable symbolTable) {
		return new StringBuffer("[")
			.append(symbolTable.getWord(this.getLHS()))
			.append("] ||| ")
			.append(symbolTable.getWords(this.getFrench()))
			.append(" ||| ")
			.append(symbolTable.getWords(english))
			.toString();
	}
}
