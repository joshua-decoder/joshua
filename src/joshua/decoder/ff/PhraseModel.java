package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Grammar;
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
  private String owner;

  private float[] phrase_weights = null;

  public PhraseModel(FeatureVector weights, String[] args, JoshuaConfiguration config, Grammar g) {
    super(weights, "tm_", args, config);

    String owner = parsedArgs.get("owner");
    this.name = String.format("tm_%s", owner);

    /*
     * Determine the number of features by querying the example grammar that was passed in.
     */
    phrase_weights = new float[g.getNumDenseFeatures()];
//    System.err.println(String.format("GOT %d FEATURES FOR %s", g.getNumDenseFeatures(), owner));
    for (int i = 0; i < phrase_weights.length; i++)
      phrase_weights[i] = weights.getSparse(String.format("tm_%s_%d", owner, i));

    // Store the owner.
    this.owner = owner;
    this.ownerID = Vocabulary.id(owner);
  }

  /**
   * Just register a single weight, tm_OWNER, and use that to set its precomputed cost
   */
  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;

    ArrayList<String> names = new ArrayList<String>();
    for (int i = 0; i < phrase_weights.length; i++)
      names.add(String.format("tm_%s_%d", owner, i));
    return names;
  }

  /**
   * Estimates the cost of applying this rule, which is just the score of the precomputable feature
   * functions.
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
      if (rule.getPrecomputableCost() <= Float.NEGATIVE_INFINITY) {
        // float score = rule.getFeatureVector().innerProduct(weights);
        rule.setPrecomputableCost(phrase_weights, weights);
      }
      
//      System.err.println(String.format("RULE = %s / %f", rule.getEnglishWords(), rule.getPrecomputableCost()));
      for (int k = 0; k < phrase_weights.length; k++) {
//        System.err.println(String.format("k = %d, denseFeatureIndex = %d, owner = %s, ownerID = %d", k, denseFeatureIndex, owner, ownerID));
        acc.add(k + denseFeatureIndex, rule.getDenseFeature(k));
      }
      
      for (String key: rule.getFeatureVector().keySet())
        acc.add(key, rule.getFeatureVector().getSparse(key));
    }

    return null;
  }

  public String toString() {
    return name + " " + Vocabulary.word(ownerID);
  }
}
