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
import joshua.decoder.ff.tm.HieroGrammar.MemoryBasedBatchGrammar;

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
	public  int     rule_id;//TODO
	public  int     lhs; // tag of this rule
	public  int[]   p_french; //pointer to the RuleCollection, as all the rules under it share the same Source side
	public  int[]   english;
	public  int     arity;
	
	/* a feature function will be applied  for this rule 
	 * only if the owner of the rule matches the owner of the feature function
	 * */
	public  int     owner;
	
	public  float[] feat_scores; // the feature scores for this rule
	public	float lattice_cost; //TODO: consider remove this from the general class, and create a new specific Rule class
	
		
	/** 
	 * estimate_cost depends on rule itself: statelesscost + transition_cost(non-stateless/non-contexual* models), 
	 * it is only used in TMGrammar pruning and chart.prepare_rulebin, shownup in
	 * chart.expand_unary but not really used
	 */
	protected float est_cost = 0;
		
	
	//TODO Ideally, we shouldn't have to have dummy rule IDs and dummy owners. How can this need be eliminated?
	private static final int DUMMY_RULE_ID = 1;
	private static final int DUMMY_OWNER = 1;
	
	
	public Rule(){
		//do nothing
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
	public Rule(int lhs_, int[] source_rhs, int[] target_rhs, float[] feature_scores, int arity_) {
		this.lhs         = lhs_;
		this.p_french      = source_rhs;
		this.english     = target_rhs;
		this.feat_scores = feature_scores;
		this.arity       = arity_;
		
		//==== dummy values
		this.lattice_cost= 0;
		this.rule_id     = DUMMY_RULE_ID;
		this.owner       = DUMMY_OWNER;
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
		r.p_french     = new int[1];
	   	r.p_french[0]  = fr_in;
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
	   	
		return r;
	}
	
	
		
	
//	 create a copy of the rule and set the lattice cost field
	public Rule cloneAndAddLatticeCostIfNonZero(float cost) {
		if (cost == 0.0f) 
			return this;
		else{
			Rule r = new Rule();
			r.rule_id     = this.rule_id;
			r.lhs         = this.lhs;
			r.p_french      = this.p_french;
			r.english     = this.english;
			r.owner       = this.owner;
			r.feat_scores = this.feat_scores;
			r.arity       = this.arity;
			
			r.lattice_cost = cost;//the only thing that is from the caller of this function
			return r;
		}
	}
	
	/* ~~~~~ Attributes (only used in DiskHyperGraph) */
	public final boolean isOutOfVocabularyRule() {
		return (this.rule_id == MemoryBasedBatchGrammar.OOV_RULE_ID);
	}
	
	
	public final int getRuleID() {
		return this.rule_id;
	}
	
	public double getEstRuleCost(){
		if(est_cost <= Double.NEGATIVE_INFINITY){
			System.out.println("The est cost is neg infinity; must be bad rule; rule is:\n" +toString());
		}
		return est_cost;
	}

	/** 
	 * set a lower-bound estimate inside the rule returns full estimate.
	 */
	public float estimateRuleCost(ArrayList<FeatureFunction> p_l_models) {
		if (null == p_l_models) {
			return 0;
		}else{		
			float estcost      = 0.0f;
			for (FeatureFunction ff : p_l_models) {
				double mdcost = ff.estimate(this) * ff.getWeight();
				estcost += mdcost;
			}
			this.est_cost = estcost;
			return estcost;
		}
	}
	
	public static Comparator<Rule> NegtiveCostComparator	= new Comparator<Rule>() {
		public int compare(Rule rule1, Rule rule2) {
			float cost1 = rule1.est_cost;
			float cost2 = rule2.est_cost;
			if (cost1 > cost2) {
				return -1;
			} else if (cost1 == cost2) {
				return 0;
			} else {
				return 1;
			}
		}
	};
	
	
	//===================================================== serialization method=====================================
	// Caching this method significantly improves performance
	// We mark it transient because it is, though cf java.io.Serializable
	private transient String cachedToString = null;
	
	public String toString(Map<Integer,String> ntVocab, Vocabulary sourceVocab, Vocabulary targetVocab) {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(ntVocab.get(this.lhs));
			sb.append("] ||| ");
			sb.append(sourceVocab.getWords(this.p_french,true));
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
	
	
	//print the rule in terms of Ingeters
	public String toString() {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(this.lhs);
			sb.append("] ||| ");
			sb.append(this.p_french);
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
	
	public String toString(SymbolTable p_symbolTable) {
		if (null == this.cachedToString) {
			StringBuffer sb = new StringBuffer("[");
			sb.append(p_symbolTable.getWord(this.lhs));
			sb.append("] ||| ");
			sb.append(p_symbolTable.getWords(this.p_french));
			sb.append(" ||| ");
			sb.append(p_symbolTable.getWords(this.english));
			sb.append(" |||");
			for (int i = 0; i < this.feat_scores.length; i++) {
				sb.append(String.format(" %.4f", this.feat_scores[i]));
			}
			this.cachedToString = sb.toString();
		}
		return this.cachedToString;
	}
	
    public String toStringWithoutFeatScores(SymbolTable p_symbolTable){
            StringBuffer res = new StringBuffer();
            res.append("["); res.append(p_symbolTable.getWord(lhs)); res.append("] ||| ");
            res.append(p_symbolTable.getWords(p_french)); res.append(" ||| ");
            res.append(p_symbolTable.getWords(english));
            return res.toString();
    }

	
}
