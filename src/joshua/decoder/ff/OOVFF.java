package joshua.decoder.ff;

import joshua.decoder.ff.tm.AbstractGrammar;
import joshua.decoder.ff.tm.Rule;

import joshua.decoder.chart_parser.SourcePath;

/**
 * This feature is fired when an out-of-vocabulary word (with respect to the translation model) is
 * entered into the chart.
 *
 * @author Matt Post <post@cs.jhu.edu>
 */
public class OOVFF extends PrecomputableFF {

  public OOVFF(FeatureVector weights) {
    super(weights, "OOVpenalty");
  }

  /**
   * Each additional word gets a penalty. The more number of words, the more negative. So, to
   * encourage longer sentence, we should have a negative weight on the feature
   */
  public float computeCost(final Rule rule) {
    if (rule.getRuleID() == AbstractGrammar.OOV_RULE_ID)
      return 1.0f;
    else
      return 0.0f;
  }

	public FeatureVector computeFeatures(final Rule rule) {
		float value = (rule.getRuleID() == AbstractGrammar.OOV_RULE_ID) ? 1.0f : 0.0f;
		return new FeatureVector(name, value);
	}
}
