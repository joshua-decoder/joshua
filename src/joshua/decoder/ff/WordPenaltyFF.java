package joshua.decoder.ff;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.chart_parser.SourcePath;

/**
 * 
 * @author Zhifei Li <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */
public final class WordPenaltyFF extends StatelessFF {

  private static final float OMEGA = -(float) Math.log10(Math.E); // -0.435

  /*
   * This is a single-value feature template, so we cache the weight here.
   */
  private float weight;

  public WordPenaltyFF(final FeatureVector weights) {
    super(weights, "WordPenalty", "");

    // Find the weight for this feature in the weights hash and cache it.
    if (weights.containsKey(name)) {
      weight = weights.get(name);
    } else {
      System.err.println("* WARNING: no weight for feature '" + name + "'");
      weight = 0.0f;
    }
  }

  /*
   * Each word in each rule incurs a penalty of OMEGA. So we compute the number of words and
   * multiply it by OMEGA.
   * 
   * I'm not sure why it doesn't just incur a penalty of one.
   */
  public FeatureVector computeFeatures(Rule rule) {
    if (rule != null)
      return new FeatureVector(name, OMEGA * (rule.getEnglish().length - rule.getArity()));

    return new FeatureVector();
  }

  @Override
  public FeatureVector computeFeatures(Rule rule, SourcePath sourcePath, int sentID) {
    return computeFeatures(rule);
  }

  /**
   * Compute the cost directly instead of chaining to computeFeatures, since we have just one
   * weight.
   */
  @Override
  public float computeCost(Rule rule, SourcePath sourcePath, int sentID) {
    return weight * OMEGA * (rule.getEnglish().length - rule.getArity());
  }
}
