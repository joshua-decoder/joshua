package joshua.decoder.ff.tm;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;

/**
 * Normally, the feature score in the rule should be *cost* (i.e., -LogP), so that the feature
 * weight should be positive
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public class BilingualRule extends Rule {

  private static final Logger logger = Logger.getLogger(BilingualRule.class.getName());

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
  private float estimatedCost = Float.NEGATIVE_INFINITY;

  private float precomputableCost = Float.NEGATIVE_INFINITY;

  private int[] english;

	private byte [] alignment;

  // ===============================================================
  // Constructors
  // ===============================================================

  /**
   * Constructs a new rule using the provided parameters. The owner and rule id for this rule are
   * undefined. Note that some of the sparse features may be unlabeled, but they cannot be mapped to
   * their default names ("tm_OWNER_INDEX") until later, when we know the owner of the rule. This is
   * not known until the rule is actually added to a grammar in Grammar::addRule().
   * 
   * @param lhs Left-hand side of the rule.
   * @param sourceRhs Source language right-hand side of the rule.
   * @param targetRhs Target language right-hand side of the rule.
   * @param sparseFeatures Feature value scores for the rule.
   * @param arity Number of nonterminals in the source language right-hand side.
   * @param owner
   */
  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity,
      int owner) {
    this.lhs = lhs;
    this.pFrench = sourceRhs;
    this.sparseFeatures = sparseFeatures;
    this.arity = arity;
    this.owner = owner;
    this.english = targetRhs;
  }

  // Sparse feature version
  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity) {
    this.lhs = lhs;
    this.pFrench = sourceRhs;
    this.sparseFeatures = sparseFeatures;
    this.arity = arity;
    this.owner = -1;
    this.english = targetRhs;
  }

  public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, String sparseFeatures, int arity, byte [] alignment) {
		this(lhs, sourceRhs, targetRhs, sparseFeatures, arity);
		this.alignment = alignment;
	}

  // ===============================================================
  // Attributes
  // ===============================================================

  public final void setEnglish(int[] eng) {
    this.english = eng;
  }

  public final int[] getEnglish() {
    return this.english;
  }

  /**
   * Two BilingualRules are equal of they have the same LHS, the same source RHS and the same target
   * RHS.
   * 
   * @param o the object to check for equality
   * @return true if o is the same BilingualRule as this rule, false otherwise
   */
  public boolean equals(Object o) {
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

  public int hashCode() {
    // I just made this up. If two rules are equal they'll have the
    // same hashcode. Maybe someone else can do a better job though?
    int frHash = Arrays.hashCode(getFrench());
    int enHash = Arrays.hashCode(english);
    return frHash ^ enHash ^ getLHS();
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

  @Override
  public final void setFrench(int[] french) {
    this.pFrench = french;
  }

  @Override
  public final int[] getFrench() {
    return this.pFrench;
  }

  /*
   * This function returns the feature vector found in the rule's grammar file.
   */
  @Override
  public final FeatureVector getFeatureVector() {
    return computeFeatures();
  }

  @Override
  /**
   * Sets the estimated cost. Calling estimateRuleCost(models) will also set the cost, but this
   * function can be used if the cost is computed elsewhere.
   */
  public final void setEstimatedCost(float cost) {
    if (cost <= Float.NEGATIVE_INFINITY) {
      logger.warning("The cost is being set to -infinity in " + "rule:\n" + toString());
    }
    estimatedCost = cost;
  }

  /**
   * This function returns the estimated cost of a rule, which should have been computed when the
   * grammar was first sorted via a call to Rule::estimateRuleCost(). This function is a getter
   * only; it will not compute the value if it has not already been set. It is necessary in addition
   * to estimateRuleCost(models) because sometimes the value needs to be retrieved from contexts
   * that do not have access to the feature functions.
   */
  @Override
  public final float getEstimatedCost() {
    return estimatedCost;
  }

  @Override
  public final float getPrecomputableCost() {
    return precomputableCost;
  }

  @Override
  public final void setPrecomputableCost(float cost) {
    this.precomputableCost = cost;
  }

  /**
   * This function estimates the cost of a rule, which is used for sorting the rules for cube
   * pruning. The estimated cost is basically the set of precomputable features (features listed
   * along with the rule in the grammar file) along with any other estimates that other features
   * would like to contribute (e.g., a language model estimate). This cost will be a lower bound on
   * the rule's actual cost.
   * 
   * The value of this function is used only for sorting the rules. When the rule is later applied
   * in context to particular hypernodes, the rule's actual cost is computed.
   * 
   * @param models the list of models available to the decoder
   * @return estimated cost of the rule
   */
  @Override
  public final float estimateRuleCost(List<FeatureFunction> models) {
    if (null == models)
      return 0.0f;

    if (this.estimatedCost <= Float.NEGATIVE_INFINITY) {
      this.estimatedCost = 0.0f; // weights.innerProduct(computeFeatures());
      // StringBuilder sb = new StringBuilder("estimateRuleCost(" + toString() + ")");

      for (FeatureFunction ff : models) {
        this.estimatedCost -= ff.estimateCost(this, -1);
        // sb.append(String.format(" %s: %.3f", ff.getClass().getSimpleName(),
        // -ff.estimateCost(this, -1)));
      }
      // sb.append(String.format(" ||| total=%.5f",this.estimatedCost));
      // System.err.println(sb.toString());
    }

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

    FeatureVector features = new FeatureVector(sparseFeatures, "tm_" + Vocabulary.word(owner) + "_");

    return features;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.getLHS()));
    sb.append(" ||| ");
    sb.append(getFrenchWords());
    sb.append(" ||| ");
    sb.append(getEnglishWords());
    sb.append(" |||");
    sb.append(" " + getFeatureVector());
    sb.append(String.format(" ||| %.3f", getEstimatedCost()));
    return sb.toString();
  }

  public final String getFeatureString() {
    return sparseFeatures;
  }
  
  public byte[] getAlignment() {
    return alignment;
  }
}
