package joshua.decoder.ff.tm;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;

/**
 * This class implements MonolingualRule.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class MonolingualRule implements Rule {

  private static final Logger logger = Logger.getLogger(MonolingualRule.class.getName());

  // ===============================================================
  // Instance Fields
  // ===============================================================

  /*
   * The string format of Rule is: [Phrase] ||| french ||| english ||| feature scores
   */
  private int lhs; // tag of this rule
  private int[] pFrench; // pointer to the RuleCollection, as all the rules under it share the same
                         // Source side
  protected int arity;

  // And a string containing the sparse ones
  protected String sparseFeatures;

  /*
   * a feature function will be fired for this rule only if the owner of the rule matches the owner
   * of the feature function
   */
  private int owner = -1;

  /**
   * This is the cost computed only from the features present with the grammar rule. This cost is
   * needed to sort the rules in the grammar for cube pruning, but isn't the full cost of applying
   * the rule (which will include contextual features that can't be computed until the rule is
   * applied).
   */
  private float estimatedCost = 0.0f;

  // ===============================================================
  // Constructors
  // ===============================================================

  /**
   * Constructs a new rule using the provided parameters. The owner and rule id for this rule are
   * undefined.
   * 
   * @param lhs Left-hand side of the rule.
   * @param sourceRhs Source language right-hand side of the rule.
   * @param featureScores Feature value scores for the rule.
   * @param arity Number of nonterminals in the source language right-hand side.
   * @param owner
   */
  public MonolingualRule(int lhs, int[] sourceRhs, String sparseFeatures, int arity, int owner) {
    this.lhs = lhs;
    this.pFrench = sourceRhs;
    this.sparseFeatures = sparseFeatures;
    this.arity = arity;
    this.owner = owner;
  }


  // called by class who does not care about lattice_cost,
  // rule_id, and owner
  public MonolingualRule(int lhs_, int[] source_rhs, int arity_) {
    this.lhs = lhs_;
    this.pFrench = source_rhs;
    this.arity = arity_;

    this.owner = -1;
  }

  /**
   * Sparse feature version.
   */
  public MonolingualRule(int lhs_, int[] source_rhs, String sparse_features, int arity_) {
    this.lhs = lhs_;
    this.pFrench = source_rhs;
    this.sparseFeatures = sparse_features;
    this.arity = arity_;

    this.owner = -1;
  }


  // ===============================================================
  // Attributes
  // ===============================================================

  public final void setArity(int arity) {
    this.arity = arity;
  }

  public final int getArity() {
    return this.arity;
  }

  public final void setOwner(int owner) {
    this.owner = owner;
  }

  public final int getOwner() {
    return this.owner;
  }

  public final void setLHS(int lhs) {
    this.lhs = lhs;
  }

  public final int getLHS() {
    return this.lhs;
  }

  public void setEnglish(int[] eng) {
    // TODO: do nothing
  }

  public int[] getEnglish() {
    // TODO
    return null;
  }

  public final void setFrench(int[] french) {
    this.pFrench = french;
  }

  public final int[] getFrench() {
    return this.pFrench;
  }


  /*
   * This function returns the feature vector found in the rule's grammar file.
   */
  public final FeatureVector getFeatureVector() {
    return computeFeatures();
  }

  public final String getFeatureString() {
    return sparseFeatures;
  }

  public final void setEstimatedCost(float cost) {
    if (cost <= Double.NEGATIVE_INFINITY) {
      logger.warning("The cost is being set to -infinity in " + "rule:\n" + toString());
    }
    estimatedCost = cost;
  }

  /**
   * This function returns the cost of a rule, which should have been computed when the grammar was
   * first sorted via a call to Rule::estimateRuleCost().
   */
  public final float getEstimatedCost() {
    if (estimatedCost <= Double.NEGATIVE_INFINITY) {
      logger
          .warning("The estimatedCost is neg infinity; must be bad rule; rule is:\n" + toString());
    }
    return estimatedCost;
  }

  /**
   * Set a lower-bound estimate inside the rule returns full estimate. By lower bound, we mean the
   * set of precomputable features. This includes all features listed with the rule in the grammar
   * file, as well as certain stateful features like n-gram probabilities of any complete n-grams
   * found with the rule. The value of this function is used only for sorting the rules. When the
   * rule is later applied in context to particular hypernodes, the rule's actual cost is computed.
   * 
   * @param models the list of models available to the decoder
   * @return estimated cost of the rule
   */
  public final float estimateRuleCost(List<FeatureFunction> models) {
    if (null == models) return 0.0f;

    // TODO: this should be cached
    this.estimatedCost = 0.0f; // weights.innerProduct(computeFeatures());
//    StringBuilder sb = new StringBuilder("estimateRuleCost(" + toString() + ")");

    for (FeatureFunction ff : models) {
      this.estimatedCost -= ff.estimateCost(this, -1);
//      sb.append(String.format(" %s: %.3f", ff.getClass().getSimpleName(),
//          -ff.estimateCost(this, -1)));
    }
//    sb.append(String.format(" ||| total=%.5f",this.estimatedCost));
//    System.err.println(sb.toString());

    return estimatedCost;
  }


  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * This function does the work of turning the string version of the sparse features (passed in
   * when the rule was created) into an actual set of features. This is a bit complicated because we
   * support intermingled labeled and unlabeled features, where the unlabeled features are mapped to
   * a default name template of the form "tm_OWNER_INDEX".
   */
  public FeatureVector computeFeatures() {

    /*
     * Now read the feature scores, which can be any number of dense features and sparse features.
     * Any unlabeled feature becomes a dense feature. By convention, dense features should precede
     * sparse (labeled) ones, but it's not required.
     */

    if (owner == -1) {
      System.err
          .println("* FATAL: You asked me to compute the features for a rule, but haven't told me the rule's owner.");
      System.err.println("* RULE: " + this.toString());
      System.exit(1);
    }

    FeatureVector features =
        new FeatureVector(sparseFeatures, "tm_" + Vocabulary.word(owner) + "_");
    features.times(-1);
    return features;
  }


  // ===============================================================
  // Serialization Methods
  // ===============================================================
  // BUG: These are all far too redundant. Should be refactored to share.

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.lhs));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(this.pFrench));
    sb.append(" ||| " + sparseFeatures);
    // FeatureVector features = this.getFeatureVector();
    // for (String feature: features.keySet()) {
    // sb.append(String.format(" %s=%.5f", feature, features.get(feature)));
    // }
    return sb.toString();
  }

  public String convertToString(int[] words) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < words.length; i++) {
      sb.append(Vocabulary.word(words[i]));

      if (i < words.length - 1) sb.append(" ");
    }
    return sb.toString();
  }
}
