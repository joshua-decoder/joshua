package joshua.decoder.ff;

import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

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

public class PhraseModel extends StatelessFF {

  /* The owner of the grammar. */
  private int ownerID;
  
  private float[] phrase_weights = null;

  public PhraseModel(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "tm_", args, config);

    String owner = parsedArgs.get("owner");
    this.name = String.format("tm_%s", owner);

    /*
     * This is an efficiency hack; we cache the full dot product of the weights with the dense
     * features, storing them as a value under the name "tm_OWNER". There won't be a weight for
     * that, so we add a weight to the weights vector. This weight will never be output because when
     * the k-best list is retrieved and the actual feature values asked for, the accumulator will
     * fetch the fine-grained dense features.
     */
    if (weights.containsKey(name)) {
      System.err.println(String.format(
          "* FATAL: Your weights file contains an entry for '%s', shouldn't", name));
      System.exit(1);
    }
    weights.put(name, 1.0f);
    
    int num_features = 0;
    while (weights.containsKey(String.format("tm_%s_%d", owner, num_features)))
      num_features++;

    phrase_weights = new float[num_features];
    for (int i = 0; i < num_features; i++)
      phrase_weights[i] = weights.get(String.format("tm_%s_%d", owner, i));
        
    // Store the owner.
    this.ownerID = Vocabulary.id(owner);
  }

  /**
   * Estimates the cost of applying this rule, which is just the score of the precomputable
   * feature functions.
   */
  @Override
  public float estimateCost(final Rule rule, Sentence sentence) {
    
    if (rule != null && rule.getOwner() == ownerID) {
      if (rule.getPrecomputableCost() <= Float.NEGATIVE_INFINITY)
        rule.setPrecomputableCost(phrase_weights, weights);
        
      return rule.getPrecomputableCost();
    }
    
    return 0.0f;
  }

  /**
   * Just chain to computeFeatures(rule), since this feature doesn't use the sourcePath or sentID. *
   */
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule != null && rule.getOwner() == ownerID) {
      /*
       * Here, we peak at the Accumulator object. If it's asking for scores, then we don't bother to
       * add each feature, but rather compute the inner product and add *that*. This is totally
       * cheating; the Accumulator is supposed to be a generic object. But without this cheat
       */
      if (acc instanceof ScoreAccumulator) {
        if (rule.getPrecomputableCost() <= Float.NEGATIVE_INFINITY) {
//          float score = rule.getFeatureVector().innerProduct(weights);
          rule.setPrecomputableCost(phrase_weights, weights);
        }
        acc.add(name, rule.getPrecomputableCost());
      } else {
        FeatureVector features = rule.getFeatureVector();
        for (String key : features.keySet())
          acc.add(key, features.get(key));
      }
    }

    return null;
  }

  public String toString() {
    return name + " " + Vocabulary.word(ownerID);
  }
}
