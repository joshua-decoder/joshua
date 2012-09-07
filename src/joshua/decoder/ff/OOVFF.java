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
    super(weights, "OOVPenalty");
  }

  /**
   * Each additional word gets a penalty. The more number of words, the more negative. So, to
   * encourage longer sentence, we should have a negative weight on the feature
   */
  public float computeCost(final Rule rule) {
		return 1.0f;
  }

	public FeatureVector computeFeatures(final Rule rule) {
		return new FeatureVector(name, 1.0f);
	}
}
