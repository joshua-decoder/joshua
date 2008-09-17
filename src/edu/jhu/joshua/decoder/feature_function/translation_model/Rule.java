/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.joshua.decoder.feature_function.translation_model;
import  edu.jhu.joshua.decoder.feature_function.translation_model.TMGrammar;

import edu.jhu.joshua.decoder.Symbol;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-08-03 04:12:57 -0400 (Sun, 03 Aug 2008) $
 */
public abstract class Rule {
	/* The string format of Rule is:
	 *     [Phrase] ||| french ||| english ||| feature scores
	 */
	private int    rule_id = TMGrammar.OOV_RULE_ID;
	public int     lhs;         // tag of this rule, state to upper layer
	public int[]   french;      // only need to maintain at rulebine
	public int[]   english;
	public int     owner;
	public float[] feat_scores; // the feature scores for this rule
	public int     arity = 0;   // TODO: disk-grammar does not have this information, so, arity-penalty feature is not supported in disk-grammar
	
	/* this remember all the stateless cost: sum of cost of all
	 * stateless models (e..g, phrase model, word-penalty,
	 * phrase-penalty). The LM model cost is not included here.
	 * this will be set by Grmmar.estimate_rule
	 */
	public float statelesscost = (float)0.0; //this is set in estimate_rule()
	
	
	/* ~~~~~ Constructors */
	public Rule(String line) {
		//TODO
	}
	
	public Rule() {
		//TODO
	}
	
	
	/* ~~~~~ Attributes (only used in DiskHyperGraph) */
	public final boolean isOutOfVocabularyRule() {
		return (this.rule_id == TMGrammar.OOV_RULE_ID);
	}
	
	public final int getRuleID() {
		return this.rule_id;
	}
	
	
	/* ~~~~~ Serialization methods */
	// Caching this method significantly improves performance
	// We mark it transient because it is, though cf java.io.Serializable
	private transient String cachedToString = null;
	public String toString() {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(Symbol.get_string(this.lhs));
			sb.append("] ||| ");
			sb.append(Symbol.get_string(this.french));
			sb.append(" ||| ");
			sb.append(Symbol.get_string(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.feat_scores.length; i++) {
				sb.append(String.format(" %.4f", this.feat_scores[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
}
