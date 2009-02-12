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
import java.util.Map;

import joshua.decoder.ff.FeatureFunction;

import joshua.decoder.Symbol;
import joshua.util.sentence.Vocabulary;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-08-03 04:12:57 -0400 (Sun, 03 Aug 2008) $
 */


/* Normally, the feature score in the rule should be *cost* (i.e., -LogP), 
 * so that the feature weight should be positive
 * */


public class Rule {
	/* The string format of Rule is:
	 *     [Phrase] ||| french ||| english ||| feature scores
	 */
	public  int     rule_id;
	public  int     lhs;         // tag of this rule, state to upper layer
	public  int[]   french;      // only need to maintain at RuleCollection as all the rules under it share the same Source side
	public  int[]   english;
	
	/* a feature function will be applied  for this rule 
	 * only if the owner of the rule matches the owner of the feature function
	 * */
	public  int     owner;
	
	public  float[] feat_scores; // the feature scores for this rule
	public	float lattice_cost;
	public  int     arity;// = 0;   // TODO: disk-grammar does not have this information, so, arity-penalty feature is not supported in disk-grammar
	
	/* this remember all the stateless cost: sum of cost of all
	 * stateless models (e..g, phrase model, word-penalty,
	 * phrase-penalty). The LM model cost is not included here.
	 * this will be set by Grmmar.estimate_rule
	 */
	protected float statelesscost = 0; //TODO: this is set in estimate_rule(); we should use an abstract class to enforce this behavior

		
//	TODO Ideally, we shouldn't have to have dummy rule IDs and dummy owners. How can this need be eliminated?
	private static final int DUMMY_RULE_ID = 1;
	private static final int DUMMY_OWNER = 1;
	
	public Rule(){
		//do nothing
	}
	
	
	/**
	 * For use in reading rules from disk.
	 * 
	 * @param rule_id
	 * @param lhs Left-hand side of the rule.
	 * @param fr_in Source language right-hand side of the rule.
	 * @param eng_in Target language right-hand side of the rule.
	 * @param owner
	 * @param feat_scores_in Feature value scores for the rule.
	 * @param arity_in Number of nonterminals in the source language right-hand side.
	 */
	public Rule(int rule_id, int lhs, int[] fr_in, int[] eng_in, int owner, float[] feat_scores_in, int arity_in) {
		this.rule_id     = rule_id;
		this.lhs         = lhs;
		this.french      = fr_in;
		this.english     = eng_in;
		this.owner       = owner;
		this.feat_scores = feat_scores_in;
		this.arity       = arity_in;
		this.lattice_cost= 0;
	}
	
	// just used by cloneAndAddLatticeCost:
	private Rule(int rule_id, int lhs, int[] fr_in, int[] eng_in, int owner, float[] feat_scores_in, int arity_in, float statelesscost, float src_cost) {
		this.rule_id     = rule_id;
		this.lhs         = lhs;
		this.french      = fr_in;
		this.english     = eng_in;
		this.owner       = owner;
		this.feat_scores = feat_scores_in;
		this.arity       = arity_in;
		this.statelesscost = statelesscost;
		this.lattice_cost = src_cost;
	}
	
	
	
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
	
	/*TODO: this may not be correct, you need to set the stateless cost*/
	public Rule(int lhs, int[] source_rhs, int[] target_rhs, float[] feature_scores, int arity) {
		this(DUMMY_RULE_ID, lhs, source_rhs, target_rhs, DUMMY_OWNER, feature_scores, arity);
	}

	

	/** 
	 * only called when creating oov rule in Chart or DiskHypergraph, all
	 * others should call the other contructors; the
	 * transition cost for phrase model, arity penalty,
	 * word penalty are all zero, except the LM cost or the first feature if no LM feature is used
	 */
	public static Rule constructOOVRule(ArrayList<FeatureFunction> p_l_models, int num_feats, int oov_rule_id, int lhs_in, int fr_in, int owner_in, boolean have_lm_model) {
		Rule r = new Rule();
		r.rule_id    = oov_rule_id;
		r.lhs        = lhs_in;
		r.owner      = owner_in;
		r.arity      = 0;
		r.french     = new int[1];
	   	r.french[0]  = fr_in;
	   	r.english    = new int[1];
	   	r.english[0] = fr_in;
	   	r.feat_scores     = new float[num_feats];
	   	r.lattice_cost	= 0;
		

	   	/**TODO
	   	 * This is a hack to make the decoding without a LM works
	   	 * */
	   	if(have_lm_model==false){//no LM is used for decoding, so we should set the stateless cost
	   		//this.feat_scores[0]=100.0/((FeatureFunction)p_l_models.get(0)).getWeight();//TODO
	   		r.feat_scores[0]=100;//TODO
	   	}
	   	
	   	r.computeStatelessCost(p_l_models);//set statelesscost
		return r;
	}
	
	
	
	public float getStatelessCost(){
		return this.statelesscost;
	}
	
	
	/*compute and set stateless cost*/
	public float computeStatelessCost(ArrayList<FeatureFunction> p_l_models) {
		if (null == p_l_models) {
			return 0;
		}		
		this.statelesscost = 0.0f;
		
		for (FeatureFunction ff : p_l_models) {
			double mdcost = ff.estimate(this) * ff.getWeight();//TODO: should use transition()?
			if (! ff.isStateful()) {
				this.statelesscost += mdcost;
			}
		}
		return this.statelesscost;
	}
	

	
	
//	 create a copy of the rule and set the lattice cost field
	public Rule cloneAndAddLatticeCostIfNonZero(float cost) {
		if (cost == 0.0f) return this;
		Rule r = new Rule(rule_id, lhs, french, english, owner, feat_scores, arity, statelesscost, cost);
		return r;
	}
	
	/* ~~~~~ Attributes (only used in DiskHyperGraph) */
	public final boolean isOutOfVocabularyRule() {
		return (this.rule_id == BatchGrammar.OOV_RULE_ID);
	}
	
	
	public final int getRuleID() {
		return this.rule_id;
	}
	
	
	/* ~~~~~ Serialization methods */
	// Caching this method significantly improves performance
	// We mark it transient because it is, though cf java.io.Serializable
	private transient String cachedToString = null;
	
	public String toString(Map<Integer,String> ntVocab, Vocabulary sourceVocab, Vocabulary targetVocab) {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(ntVocab.get(this.lhs));
			sb.append("] ||| ");
			sb.append(sourceVocab.getWords(this.french,true));
			sb.append(" ||| ");
			sb.append(targetVocab.getWords(this.english,false));
			//sb.append(java.util.Arrays.toString(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.feat_scores.length; i++) {
				//sb.append(String.format(" %.4f", this.feat_scores[i]));
				sb.append(' ');
				sb.append(Float.toString(this.feat_scores[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
	public String toString(Symbol p_symbol) {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(p_symbol.getWord(this.lhs));
			sb.append("] ||| ");
			sb.append(p_symbol.getWords(this.french));
			sb.append(" ||| ");
			sb.append(p_symbol.getWords(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.feat_scores.length; i++) {
				sb.append(String.format(" %.4f", this.feat_scores[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
	//print the rule in terms of Ingeters
	public String toString() {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(this.lhs);
			sb.append("] ||| ");
			sb.append(this.french);
			sb.append(" ||| ");
			sb.append(this.english);
			sb.append(" |||");
			for (int i = 0; i < this.feat_scores.length; i++) {
				sb.append(String.format(" %.4f", this.feat_scores[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
	
    public String toStringWithoutFeatScores(Symbol p_symbol){
            StringBuffer res = new StringBuffer();
            res.append("["); res.append(p_symbol.getWord(lhs)); res.append("] ||| ");
            res.append(p_symbol.getWords(french)); res.append(" ||| ");
            res.append(p_symbol.getWords(english));
            return res.toString();
    }
}
