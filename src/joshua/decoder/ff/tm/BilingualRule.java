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
 * Normally, the feature score in the rule should be *cost* (i.e.,
 * -LogP), so that the feature weight should be positive
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
	 * Constructs a new rule using the provided parameters. The
	 * owner and rule id for this rule are undefined.
	 * 
	 * @param lhs Left-hand side of the rule.
	 * @param sourceRhs Source language right-hand side of the rule.
	 * @param targetRhs Target language right-hand side of the rule.
	 * @param featureScores Feature value scores for the rule.
	 * @param arity Number of nonterminals in the source language
	 *              right-hand side.
	 * @param owner
	 * @param latticeCost
	 * @param ruleID
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
	
	
	//print the rule in terms of Integers
	public String toString() {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer();
			sb.append(this.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this)));
			sb.append("~~~");
			sb.append(this.getLHS());
			sb.append(" ||| ");
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
			StringBuffer sb = new StringBuffer();
			sb.append(symbolTable.getWord(this.getLHS()));
			sb.append(" ||| ");
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
		StringBuffer sb = new StringBuffer();
		if(symbolTable==null)
			sb.append(this.getLHS());
		else
			sb.append(symbolTable.getWord(this.getLHS()));
		
		return sb.append(" ||| ")
		  		 .append(convertToString(this.getFrench(), symbolTable))
		  		 .append(" ||| ")
		  		 .append(convertToString(this.getEnglish(), symbolTable))
		  		 .toString();
	}
	
	
	/**
	 * Two BilingualRules are equal of they have the same LHS, the same
	 * source RHS and the same target RHS.
	 *
	 * @param o the object to check for equality
	 * @return true if o is the same BilingualRule as this rule, false
	 * otherwise
	 */
	public boolean equals(Object o)
	{
		if (!(o instanceof BilingualRule)) {
			return false;
		}
		BilingualRule other = (BilingualRule) o;
		if (getLHS() != other.getLHS()) {
			return false;
		}
		if (!Arrays.equals(getFrench(), other.getFrench())) {
			return false;
		}
		if (!Arrays.equals(english, other.getEnglish())) {
			return false;
		}
		return true;
	}

	public int hashCode()
	{
		// I just made this up. If two rules are equal they'll have the
		// same hashcode. Maybe someone else can do a better job though?
		int frHash = Arrays.hashCode(getFrench());
		int enHash = Arrays.hashCode(english);
		return frHash ^ enHash ^ getLHS();
	}
	
	

}
