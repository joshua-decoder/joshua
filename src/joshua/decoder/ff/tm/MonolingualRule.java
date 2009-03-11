package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Map;

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.util.sentence.Vocabulary;

public class MonolingualRule implements Rule {
	/* The string format of Rule is:
	 *     [Phrase] ||| french ||| english ||| feature scores
	 */
	private  int     rule_id;
	private  int     lhs; // tag of this rule
	private  int[]   p_french; //pointer to the RuleCollection, as all the rules under it share the same Source side
	private  int     arity;
	private  float[] feat_scores; // the feature scores for this rule
	
	//==================
	/* a feature function will be fired  for this rule 
	 * only if the owner of the rule matches the owner of the feature function
	 * */
	private  int     owner;
	

	//TODO: consider remove this from the general class, and create a new specific Rule class
	private	float lattice_cost; 
	
		
	/** 
	 * estimate_cost depends on rule itself: statelesscost + transition_cost(non-stateless/non-contexual* models), 
	 * we need this variable in order to provide sorting for cube-pruning
	 */
	private float est_cost = 0;
		
	
	//TODO Ideally, we shouldn't have to have dummy rule IDs and dummy owners. How can this need be eliminated?
	private static final int DUMMY_RULE_ID = 1;
	private static final int DUMMY_OWNER = 1;
	
	
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
	public MonolingualRule(int lhs_, int[] source_rhs, float[] feature_scores,  int arity_, int owner_, float lattice_cost_, int rule_id_) {
		this.lhs         = lhs_;
		this.p_french      = source_rhs;
		this.feat_scores = feature_scores;
		this.arity       = arity_;
		this.lattice_cost= lattice_cost_;
		this.rule_id     = rule_id_;
		this.owner       = owner_;
	}

	
	//called by class who does not care about lattice_cost, rule_id, and owner
	public MonolingualRule(int lhs_, int[] source_rhs, float[] feature_scores, int arity_) {
		this.lhs         = lhs_;
		this.p_french      = source_rhs;
		this.feat_scores = feature_scores;
		this.arity       = arity_;
		
		//==== dummy values
		this.lattice_cost= 0;
		this.rule_id     = DUMMY_RULE_ID;
		this.owner       = DUMMY_OWNER;
	}
		
	
	//========================== set and get methods for the field
	public final void setRuleID(int id) {
		this.rule_id = id;;
	}
	
	public final int getRuleID() {
		return this.rule_id;
	}
	
	public final void setArity(int arity_) {
		this.arity = arity_;;
	}
	
	public final int getArity() {
		return this.arity;
	}
	
	public final void setOwner(int ow) {
		this.owner = ow;;
	}
	
	public final int getOwner() {
		return this.owner;
	}
	
	public final void setLHS(int lhs_) {
		this.lhs = lhs_;
	}
	
	public final int getLHS() {
		return this.lhs;
	}
		
	public void setEnglish(int[] eng_) {
		//TODO: do nothing
	}
	
	public int[] getEnglish() {
		//TODO
		return null;
	}
	
	public final void setFrench(int[] french_) {
		this.p_french = french_;
	}
	
	public final int[] getFrench() {
		return this.p_french;
	}
	
	public final void setFeatureScores(float[] scores) {
		this.feat_scores = scores;
	}
	
	public final float[] getFeatureScores() {
		return this.feat_scores;
	}
	
	public final void setLatticeCost(float cost) {
		this.lattice_cost = cost;
	}
	
	public final float getLatticeCost() {
		return this.lattice_cost;
	}
	
	public final float getEstCost(){
		if(est_cost <= Double.NEGATIVE_INFINITY){
			System.out.println("The est cost is neg infinity; must be bad rule; rule is:\n" +toString());
		}
		return est_cost;
	}

	/** 
	 * set a lower-bound estimate inside the rule returns full estimate.
	 */
	public final float estimateRuleCost(ArrayList<FeatureFunction> p_l_models) {
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
            res.append(p_symbolTable.getWords(p_french));
            return res.toString();
    }
}
