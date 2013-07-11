package joshua.decoder.ff;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * This feature handles the list of features that are found with grammar rules in the grammar file.
 * dense features that may be associated with the rules in a grammar file. The feature names of
 * these dense rules are a function of the phrase model owner. When the feature is loaded, it
 * queries the weights for the set of features that are active for this grammar, storing them in an
 * array.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li <zhifei.work@gmail.com>
 */

public class PhraseModelFF extends StatelessFF {

  /* The owner of the grammar. */
  private int ownerID;

  public PhraseModelFF(FeatureVector weights, String owner) {
    super(weights, "tm_" + owner, "");

    // Store the owner.
    this.ownerID = Vocabulary.id(owner);
  }

  @Override
  public float estimateCost(final Rule rule, int sentID) {
    return computeCost(rule, null, -1, -1, null, sentID);
  }

  /**
   * Just chain to computeFeatures(rule), since this feature doesn't use the sourcePath or sentID. *
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc) {

    if (rule != null && rule.getOwner() == ownerID) {
      FeatureVector features = rule.getFeatureVector();
      for (String key: features.keySet())
        acc.add(key, features.get(key));
    }
    
    return null;
  }

  public String toString() {
    return name + " " + Vocabulary.word(ownerID);
  }
}
