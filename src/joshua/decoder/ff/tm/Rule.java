package joshua.decoder.ff.tm;

import java.util.Comparator;
import java.util.List;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;


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
  
  String getEnglishWords();

  void setFrench(int[] french);

  int[] getFrench();
  
  String getFrenchWords();

  /**
   * This function returns the dense (phrasal) features discovered when the rule was loaded. Dense
   * features are the list of unlabeled features that preceded labeled ones. They can also be
   * specified as labeled features of the form "tm_OWNER_INDEX", but the former format is preferred.
   */
  public FeatureVector getFeatureVector();

  /**
   * This allows the estimated cost of a rule to be applied from the outside.
   * 
   * @param cost
   */
  void setEstimatedCost(float cost);

  /**
   * This function is called by the rule comparator when sorting the grammar. As such it may be
   * called many times and any implementation of it should be a cached implementation.
   * 
   * @return the estimated cost of the rule (a lower bound on the true cost)
   */
  float getEstimatedCost();

  /**
   * Precomputable costs is the inner product of the weights found on each grammar rule and the
   * weight vector. This is slightly different from the estimated rule cost, which can include other
   * features (such as a language model estimate). This getter and setter should also be cached, and
   * is basically provided to allow the PhraseModel feature to cache its (expensive) computation for
   * each rule.
   * 
   * @return the precomputable cost of each rule
   */
  float getPrecomputableCost();

  void setPrecomputableCost(float cost);

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * Set a lower-bound estimate inside the rule returns full estimate.
   */
  float estimateRuleCost(List<FeatureFunction> models);


  /**
   * This comparator is used for sorting during cube pruning. It sorts items in reverse.
   */
  Comparator<Rule> NegativeCostComparator = new Comparator<Rule>() {
    public int compare(Rule rule1, Rule rule2) {
      float cost1 = rule1.getEstimatedCost();
      float cost2 = rule2.getEstimatedCost();
      if (cost1 > cost2) {
        return -1;
      } else if (cost1 == cost2) {
        return 0;
      } else {
        return 1;
      }
    }
  };

  String toString();
}
