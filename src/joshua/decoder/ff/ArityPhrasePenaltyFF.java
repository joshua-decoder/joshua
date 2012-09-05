package joshua.decoder.ff;

import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;

/**
 * This feature function counts rules from a particular grammar (identified by the owner) having an
 * arity within a specific range.  It expects three parameters upon initialization: the owner, the
 * minimum arity, and the maximum arity.
 *
 * @author Matt Post <post@cs.jhu.edu
 * @author Zhifei Li <zhifei.work@gmail.com>
 */
public final class ArityPhrasePenaltyFF extends StatelessFF {

  // when the rule.arity is in the range, then this feature is activated
  private final int owner;
  private final int minArity;
  private final int maxArity;

  public ArityPhrasePenaltyFF(final WeightVector weights, String argString) {
    super(weight, "ArityPenalty", argString);

    // Process the args for the owner, minimum, and maximum.  
    
    // TODO: This should be done in a general way by FeatureFunction::processArgs, in a way that
    // allows any feature to have arguments.
    String args[] = argString.split();
    this.owner = Vocabulary.id(args[0]);
    this.minArity = Integer.parseInt(args[1]);
    this.maxArity = Integer.parseInt(args[2]);

    logger.fine(String.format("ArityPhrasePenaltyFF feature with owner=%s, minArity=%d, maxArity=%d", owner, minArity, maxArity));
  }

  /**
   * Returns 1 if the arity penalty feature applies to the current rule
   */
  private int isEligible(final Rule rule) {
    if (this.owner == rule.getOwner() && rule.getArity() >= this.minArity && rule.getArity() <= this.maxArity)
      return 1;

    return 0;
  }

  public FeatureVector computeFeatures(Rule rule, SourcePath sourcePath, int sentID) {
    return FeatureVector(name, isEligible(rule));
  }


  public float computeCost(Rule rule, int SentID) {
    return weight * isEligible(rule);
  }
}
