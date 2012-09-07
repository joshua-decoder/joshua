package joshua.decoder.ff.tm;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import joshua.decoder.ff.FeatureFunction;


/**
 * This class define the interface for Rule. Normally, the feature score in the rule should be
 * *cost* (i.e., -LogP), so that the feature weight should be positive.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public interface Rule {

  // ===============================================================
  // Attributes
  // ===============================================================

  void setArity(int arity);

  int getArity();

  void setOwner(int ow);

  int getOwner();

  void setLHS(int lhs);

  int getLHS();

  void setEnglish(int[] eng);

  int[] getEnglish();

  void setFrench(int[] french);

  int[] getFrench();

	/* This function returns the dense (phrasal) features discovered when the rule was loaded.  Dense
	 * features are the list of unlabeled features that preceded labeled ones.  They can also be
	 * specified as labeled features of the form "PhraseModel_OWNER_INDEX", but the former format is
	 * preferred.
	 */ 
	float[] getDenseFeatures();

  void setEstCost(float cost);

  float getEstCost();


  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * Set a lower-bound estimate inside the rule returns full estimate.
   */
  float estimateRuleCost(List<FeatureFunction> featureFunctions);


  /**
   * In order to provide sorting for cube-pruning, we need to provide this Comparator.
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
  String toString(Map<Integer, String> ntVocab);

  @Deprecated
  String toString();

  @Deprecated
  String toStringWithoutFeatScores();
}
