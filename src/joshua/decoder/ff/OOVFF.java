package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
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
  private int ownerID = -1;

  private static String OOV_FF_NAME =  "oovpenalty";
  
  public OOVFF(FeatureVector weights) {
    super(weights, OOV_FF_NAME);

    ownerID = Vocabulary.id("oov");
  }

  public static String getFeatureName(){
    return OOV_FF_NAME;
  }
  
  /**
   * OOV rules cover exactly one word, and such rules belong to a grammar whose owner is "oov". Each
   * OOV fires the OOVPenalty feature with a value of 1, so the cost is simply the weight, which was
   * cached when the feature was created.
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {
    
    if (rule != null && this.ownerID == rule.getOwner())
      acc.add(name, 1.0f);

    return null;
  }
  
  /**
   * It's important for the OOV feature to contribute to the rule's estimated cost, so that OOV
   * rules (which are added for all words, not just ones without translation options) get sorted
   * to the bottom during cube pruning.
   * 
   * Important! estimateCost returns the *weighted* feature value.
   */
  @Override
  public float estimateCost(Rule rule, int sentID) {
    if (rule != null && this.ownerID == rule.getOwner())
      return weights.get(name);
    return 0.0f;
  }
}
