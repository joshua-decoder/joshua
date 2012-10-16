package joshua.decoder.ff;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.packed.PackedGrammar.PackedRule;
import joshua.decoder.chart_parser.SourcePath;
import joshua.corpus.Vocabulary;


/**
 * This feature handles the list of features that are found with grammar rules in the grammar
 * file. dense features that may be associated with the rules in a grammar file.  The feature names
 * of these dense rules are a function of the phrase model owner.  When the feature is loaded, it
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
    return computeCost(rule, null, sentID);
  }
  
  /**
   * Computes the cost of applying the feature.  
   */
  @Override
  public float computeCost(final Rule rule, SourcePath sourcePath, int sentID) {
    float cost = 0.0f;

    if (rule != null && this.ownerID == rule.getOwner()) {
      if (rule instanceof PackedRule) {
        cost = computeFeatures(rule, sourcePath, sentID).innerProduct(weights);
      } else {
        if (rule.getPrecomputableCost() <= Float.NEGATIVE_INFINITY) {
          float t = computeFeatures(rule, sourcePath, sentID).innerProduct(weights);
          rule.setPrecomputableCost(t);
        }
        cost = rule.getPrecomputableCost();
      }
    }
    
    return cost; 
  }

  /**
   * Just chain to computeFeatures(rule), since this feature doesn't use the sourcePath or sentID.   * 
   */
  @Override
  public FeatureVector computeFeatures(Rule rule, SourcePath sourcePath, int sentID) {
    if (rule != null && rule.getOwner() == ownerID) {
      return rule.getFeatureVector();
    } else {
      return new FeatureVector();
    }
  }
  
  public String toString() {
    return name + " " + Vocabulary.word(ownerID); 
  }
}
