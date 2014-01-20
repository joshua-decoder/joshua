package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * This class defines is the entry point for Joshua's sparse feature implementation. It defines some
 * basic variables and interfaces that are common to all features, and is immediately inherited by
 * StatelessFF and StatefulFF, which provide functionality common to stateless and stateful
 * features, respectively. Any feature implementation should extend those classes, and not this one.
 * 
 * Features in Joshua work like templates. Each feature function defines any number of actual
 * features, which are associated with weights. The task of the feature function is to compute the
 * features that are fired in different circumstances and then return the inner product of those
 * features with the weight vector. Feature functions can also produce estimates of their future
 * cost; these values are not used in computing the score, but are only used for pruning. The
 * individual features produced by each template should have globally unique names; a good
 * convention is to prefix each feature with the name of the template that produced it.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */
public abstract class FeatureFunction {

  // ===============================================================
  // Attributes
  // ===============================================================

  // The name of the feature function (also the prefix on weights)
  protected String name = null;

  // The list of arguments passed to the feature.
  private String argString;

  // The weight vector used by the decoder, passed it when the feature is instantiated.
  protected FeatureVector weights;

  // Accessor functions
  public String getName() {
    return name;
  }

  // Whether the feature has state.
  public abstract boolean isStateful();

  // ===============================================================
  // Methods
  // ===============================================================

  public FeatureFunction(FeatureVector weights, String name) {
    this.weights = weights;
    this.name = name;
  }

  public FeatureFunction(FeatureVector weights, String name, String args) {
    this.weights = weights;
    this.name = name;
    this.argString = args;

    processArgs(this.argString);
  } 

  public String logString() {
    try {
      return String.format("%s (weight %.3f)", name, weights.get(name));
    } catch (RuntimeException e) {
      return name;
    }
  }

  /**
   * The result of applying a rule is a new state. The Accumulator object is responsible for
   * accumulating the features contributed by the feature template when applying the current rule,
   * and is available for querying after this call.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @param acc
   * @return
   */
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID, Accumulator acc);

  /**
   * This is a convenience function for retrieving the cost of applying a rule, provided for
   * backwards compatibility.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return the *weighted* cost of the feature.
   */
  public final float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {

    ScoreAccumulator score = new ScoreAccumulator();
    compute(rule, tailNodes, i, j, sourcePath, sentID, score);
    return score.getScore();
  }

  /**
   * This is a convenience function for retrieving the features fired when applying a rule, provided
   * for backward compatibility.
   * 
   * Returns the *unweighted* cost of the features delta computed at this position. Note that this
   * is a feature delta, so existing feature costs of the tail nodes should not be incorporated, and
   * it is very important not to incorporate the feature weights. This function is used in the kbest
   * extraction code but could also be used in computing the cost.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return an *unweighted* feature delta
   */
  public final FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {

    FeatureAccumulator features = new FeatureAccumulator();
    compute(rule, tailNodes, i, j, sourcePath, sentID, features);
    return features.getFeatures();
  }

  /**
   * This function is called for the final transition. For example, the LanguageModel feature
   * function treats the last rule specially. It needs to return the *weighted* cost of applying the
   * feature. Provided for backward compatibility.
   * 
   * @param tailNode
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return a *weighted* feature cost
   */
  public final float computeFinalCost(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID) {

    ScoreAccumulator score = new ScoreAccumulator();
    computeFinal(tailNode, i, j, sourcePath, sentID, score);
    return score.getScore();
  }

  /**
   * Returns the *unweighted* feature delta for the final transition (e.g., for the language model
   * feature function). Provided for backward compatibility.
   * 
   * @param tailNode
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return
   */
  public final FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j,
      SourcePath sourcePath, int sentID) {

    FeatureAccumulator features = new FeatureAccumulator();
    computeFinal(tailNode, i, j, sourcePath, sentID, features);
    return features.getFeatures();
  }

  /**
   * Feature functions must overrided this. StatefulFF and StatelessFF provide reasonable defaults
   * since most features do not fire on the goal node.
   * 
   * @param tailNode
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @param acc
   * @return the DPState (null if none)
   */
  public abstract DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc);

  /**
   * This function is called when initializing translation grammars (for pruning purpose, and to get
   * stateless cost for each rule). This is also needed to sort the rules for cube pruning. It must
   * return the *weighted* estimated cost of applying a feature. This need not be the actual cost of
   * applying the rule in context. Basically, it's the inner product of the weight vector and all
   * features found in the grammar rule, though some features (like LanguageModelFF) can also
   * compute some of their values.
   * 
   * @return the *weighted* cost of applying the feature.
   */
  public abstract float estimateCost(Rule rule, int sentID);

  /**
   * This feature is called to produce a *weighted estimate* of the future cost of applying this
   * feature. This value is not incorporated into the model score but is used in pruning decisions.
   * Stateless features return 0.0f by default, but Stateful features might want to override this.
   * 
   * @param rule
   * @param state
   * @param sentID
   * @return the *weighted* future cost estimate of applying this rule in context.
   */
  public abstract float estimateFutureCost(Rule rule, DPState state, int sentID);

  /**
   * This function could be implemented to process the feature-line arguments in a generic way, if
   * so desired.
   * 
   * TODO: implement this.
   */
  private void processArgs(String argString) {
    return;
  }

  /**
   * Accumulator objects allow us to generalize feature computation. ScoreAccumulator takes
   * (feature,value) pairs and simple stores the weighted sum (for decoding). FeatureAccumulator
   * records the named feature values (for k-best extraction).
   * 
   * @author Matt Post <post@cs.jhu.edu>
   */

  public interface Accumulator {
    public void add(String name, float value);
  }

  public class ScoreAccumulator implements Accumulator {
    private float score;

    public ScoreAccumulator() {
      this.score = 0.0f;
    }

    public void add(String name, float value) {
      if (weights.containsKey(name)) {
        score += value * weights.get(name);
      }
    }

    public float getScore() {
      return score;
    }
  }

  public class FeatureAccumulator implements Accumulator {
    private FeatureVector features;

    public FeatureAccumulator() {
      this.features = new FeatureVector();
    }

    public void add(String name, float value) {
      if (features.containsKey(name)) {
        features.put(name, features.get(name) + value);
      } else {
        features.put(name, value);
      }
    }

    public FeatureVector getFeatures() {
      return features;
    }
  }
}
