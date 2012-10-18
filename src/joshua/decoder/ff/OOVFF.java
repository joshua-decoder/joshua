package joshua.decoder.ff;

import joshua.decoder.ff.tm.Rule;
import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;

/**
 * This feature is fired when an out-of-vocabulary word (with respect to the translation model) is
 * entered into the chart. OOVs work in the following manner: for each word in the input that is OOV
 * with respect to the translation model, we create a rule that pushes that word through
 * untranslated (the suffix "_OOV" can optionally be appended according to the runtime parameter
 * "mark-oovs") . These rules are all stored in a grammar whose owner is "oov". The OOV feature
 * function template then fires the "OOVPenalty" feature whenever it is asked to score an OOV rule.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class OOVFF extends StatelessFF {
  private float weight = 0.0f;
  private int ownerID = -1;

  public OOVFF(FeatureVector weights) {
    super(weights, "OOVPenalty");

    if (!weights.containsKey(name))
      System.err.println("* WARNING: No weight for OOVPenalty found.");
    else
      weight = weights.get(name);

    ownerID = Vocabulary.id("oov");
  }

  /**
   * OOV rules cover exactly one word, and such rules belong to a grammar whose owner is "oov". Each
   * OOV fires the OOVPenalty feature with a value of 1, so the cost is simply the weight, which was
   * cached when the feature was created.
   */
  @Override
  public float computeCost(final Rule rule, SourcePath sourcePath, int sentID) {
    if (rule != null && this.ownerID == rule.getOwner()) 
      return weight;

    return 0.0f;
  }

  /**
   * If the supplied rule is an OOV rule, we fire the OOVPenalty feature with a value of 1.
   */
  @Override
  public FeatureVector computeFeatures(final Rule rule, SourcePath sourcePath, int sentID) {
    if (rule != null && this.ownerID == rule.getOwner()) return new FeatureVector(name, 1.0f);

    return new FeatureVector();
  }
}
