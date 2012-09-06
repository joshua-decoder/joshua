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
public final class OOVFF extends StatelessFF {

  public OOVFF(FeatureVector weights) {
    super(weights, "OOVpenalty");
  }

  /**
   * Each additional word gets a penalty. The more number of words, the more negative. So, to
   * encourage longer sentence, we should have a negative weight on the feature
   */
  public float computeCost(final Rule rule, final SourcePath sourcePath, int sentID) {
    if (rule.getRuleID() == AbstractGrammar.OOV_RULE_ID)
      return 1.0f;
    else
      return 0.0f;
  }
}
