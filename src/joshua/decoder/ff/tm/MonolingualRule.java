package joshua.decoder.ff.tm;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.PrecomputableFF;

/**
 * this class implements MonolingualRule
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
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
  private int arity;

	// The set of dense feature scores for this rule.
  // private float[] featScores;
  private float[] denseFeatures;
	
	// And a string containing the sparse ones
	private String sparseFeatures;

  /*
   * a feature function will be fired for this rule only if the owner of the rule matches the owner
   * of the feature function
   */
  private int owner;

  /**
   * estimate_cost depends on rule itself: statelesscost +
   * transition_cost(non-stateless/non-contexual* models), we need this variable in order to provide
   * sorting for cube-pruning
   */
  private float est_cost = 0;

  // ===============================================================
  // Static Fields
  // ===============================================================

  // TODO: Ideally, we shouldn't have to have dummy rule IDs
  // and dummy owners. How can this need be eliminated?
  public static final int DUMMY_RULE_ID = 1;
  public static final int DUMMY_OWNER = 1;


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
  public MonolingualRule(int lhs, int[] sourceRhs, float[] featureScores, int arity, int owner) {
    this.lhs = lhs;
    this.pFrench = sourceRhs;
    this.denseFeatures = featureScores;
    this.arity = arity;
    this.owner = owner;
  }


  // called by class who does not care about lattice_cost,
  // rule_id, and owner
  public MonolingualRule(int lhs_, int[] source_rhs, float[] feature_scores, int arity_) {
    this.lhs = lhs_;
    this.pFrench = source_rhs;
    this.denseFeatures = feature_scores;
    this.arity = arity_;

    // ==== dummy values
    this.owner = DUMMY_OWNER;
  }

	/**
	 * Sparse feature version.
	 */
  public MonolingualRule(int lhs_, int[] source_rhs, float[] dense_scores, String sparse_features, int arity_) {
    this.lhs = lhs_;
    this.pFrench = source_rhs;
    this.denseFeatures = dense_scores;
		this.sparseFeatures = sparse_features;
    this.arity = arity_;

    // ==== dummy values
    this.owner = DUMMY_OWNER;
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


	/* This function returns the dense (phrasal) features discovered when the rule was loaded.  Dense
	 * features are the list of unlabeled features that preceded labeled ones.  They can also be
	 * specified as labeled features of the form "PhraseModel_OWNER_INDEX", but the former format is
	 * preferred.
	 */ 
  public final float[] getDenseFeatures() {
    return this.denseFeatures;
  }

  public final float getEstCost() {
    if (est_cost <= Double.NEGATIVE_INFINITY) {
      logger.warning("The est cost is neg infinity; must be bad rule; rule is:\n" + toString());
    }
    return est_cost;
  }

  public final void setEstCost(float cost) {
    if (cost <= Double.NEGATIVE_INFINITY) {
      logger.warning("The cost is being set to -infinity in " + "rule:\n" + toString());
    }
    est_cost = cost;
  }

  /**
   * Set a lower-bound estimate inside the rule returns full estimate.
   */
  public final float estimateRuleCost(List<FeatureFunction> featureFunctions) {
    if (null == featureFunctions) {
      return 0.0f;
    } else {
      float estcost = 0.0f;
      for (FeatureFunction ff : featureFunctions) {
				if (ff instanceof PrecomputableFF)
					estcost += ((PrecomputableFF)ff).computeCost(this);
      }

      this.est_cost = estcost;
      return estcost;
    }
  }

  // ===============================================================
  // Methods
  // ===============================================================

  public void setFeatureCost(int column, float score) {
    synchronized (this) {
      denseFeatures[column] = score;
    }
  }


  public float getDenseFeature(int column) {
    synchronized (this) {
      return denseFeatures[column];
    }
  }

  // ===============================================================
  // Serialization Methods
  // ===============================================================
  // BUG: These are all far too redundant. Should be refactored to share.

  // Caching this method significantly improves performance
  // We mark it transient because it is, though cf
  // java.io.Serializable
  private transient String cachedToString = null;

  @Deprecated
  public String toString(Map<Integer, String> ntVocab) {
    if (null == this.cachedToString) {
      StringBuffer sb = new StringBuffer();
      sb.append(ntVocab.get(this.lhs));
      sb.append(" ||| ");
      sb.append(Vocabulary.getWords(this.pFrench));
      sb.append(" |||");
      for (int i = 0; i < this.denseFeatures.length; i++) {
        // sb.append(String.format(" %.4f", this.feat_scores[i]));
        sb.append(' ').append(Float.toString(this.denseFeatures[i]));
      }
      this.cachedToString = sb.toString();
    }
    return this.cachedToString;
  }

  // do not use cachedToString
  @Deprecated
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.lhs));
    sb.append(" ||| ");
    sb.append(Vocabulary.getWords(this.pFrench));
    sb.append(" |||");
    for (int i = 0; i < this.denseFeatures.length; i++) {
      sb.append(String.format(" %.4f", this.denseFeatures[i]));
    }
    return sb.toString();
  }


  @Deprecated
  public String toStringWithoutFeatScores() {
    StringBuffer sb = new StringBuffer();
    sb.append(Vocabulary.word(this.getLHS()));

    return sb.append(" ||| ").append(convertToString(this.getFrench())).toString();
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
