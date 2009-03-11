package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import joshua.corpus.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.util.sentence.Vocabulary;

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
	
	public String toString(Map<Integer,String> ntVocab, Vocabulary sourceVocab, Vocabulary targetVocab);
	
	//print the rule in terms of Ingeters
	public String toString(); 
	
	public String toString(SymbolTable p_symbolTable);
	
	 public String toStringWithoutFeatScores(SymbolTable p_symbolTable);
}
