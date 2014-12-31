package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * This class defines Joshua's feature function interface, for both sparse and
 * dense features. It is immediately inherited by StatelessFF and StatefulFF,
 * which provide functionality common to stateless and stateful features,
 * respectively. Any feature implementation should extend those classes, and not
 * this one. The distinction between stateless and stateful features is somewhat
 * narrow: all features have the opportunity to return an instance of a
 * {@link DPState} object, and stateless ones just return null.
 * 
 * Features in Joshua work like templates. Each feature function defines any
 * number of actual features, which are associated with weights. The task of the
 * feature function is to compute the features that are fired in different
 * circumstances and then return the inner product of those features with the
 * weight vector. Feature functions can also produce estimates of their future
 * cost (via {@link estimateCost()}); these values are not used in computing the
 * score, but are only used for sorting rules during cube pruning. The
 * individual features produced by each template should have globally unique
 * names; a good convention is to prefix each feature with the name of the
 * template that produced it.
 * 
 * Joshua does not retain individual feature values while decoding, since this
 * requires keeping a sparse feature vector along every hyperedge, which can be
 * expensive. Instead, it computes only the weighted cost of each edge. If the
 * individual feature values are requested, the feature functions are replayed
 * in post-processing, say during k-best list extraction. This is implemented in
 * a generic way by passing an {@link Accumulator} object to the compute()
 * function. During decoding, the accumulator simply sums weighted features in a
 * scalar. During k-best extraction, when individual feature values are needed,
 * a {@link FeatureAccumulator} is used to retain the individual values.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */
public abstract class FeatureFunction {

  /*
   * The name of the feature function; this generally matches the weight name on
   * the config file. This can also be used as a prefix for feature / weight
   * names, for templates that define multiple features.
   */
  protected String name = null;

  // The list of arguments passed to the feature.
  private String argString;

  /*
   * The global weight vector used by the decoder, passed it when the feature is
   * instantiated
   */
  protected FeatureVector weights;

  public String getName() {
    return name;
  }

  // Whether the feature has state.
  public abstract boolean isStateful();

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
   * This is the main function for defining feature values. The implementor
   * should compute all the features along the hyperedge, calling acc.put(name,
   * value) for each feature. It then returns the newly-computed dynamic
   * programming state for this feature (for example, for the
   * {@link LanguageModelFF} feature, this returns the new language model
   * context). For stateless features, this value is null.
   * 
   * Note that the accumulator accumulates *unweighted* feature values. The
   * feature vector is multiplied times the weight vector later on.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @param acc
   * @return the new dynamic programming state (null for stateless features)
   */
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID, Accumulator acc);

  /**
   * This is a convenience function for retrieving the features fired when
   * applying a rule, provided for backward compatibility.
   * 
   * Returns the *unweighted* cost of the features delta computed at this
   * position. Note that this is a feature delta, so existing feature costs of
   * the tail nodes should not be incorporated, and it is very important not to
   * incorporate the feature weights. This function is used in the kbest
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
   * This function is called for the final transition. For example, the
   * LanguageModel feature function treats the last rule specially. It needs to
   * return the *weighted* cost of applying the feature. Provided for backward
   * compatibility.
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
   * Returns the *unweighted* feature delta for the final transition (e.g., for
   * the language model feature function). Provided for backward compatibility.
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
   * Feature functions must overrided this. StatefulFF and StatelessFF provide
   * reasonable defaults since most features do not fire on the goal node.
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
   * This function is called when sorting rules for cube pruning. It must return
   * the *weighted* estimated cost of applying a feature. This need not be the
   * actual cost of applying the rule in context. Basically, it's the inner
   * product of the weight vector and all features found in the grammar rule,
   * though some features (like LanguageModelFF) can also compute some of their
   * values. This is just an estimate of the cost, which helps do better
   * sorting. Later, the real cost of this feature function is called via
   * compute();
   * 
   * @return the *weighted* cost of applying the feature.
   */
  public abstract float estimateCost(Rule rule, int sentID);

  /**
   * This feature is called to produce a *weighted estimate* of the future cost
   * of applying this feature. This value is not incorporated into the model
   * score but is used in pruning decisions. Stateless features return 0.0f by
   * default, but Stateful features might want to override this.
   * 
   * @param rule
   * @param state
   * @param sentID
   * @return the *weighted* future cost estimate of applying this rule in
   *         context.
   */
  public abstract float estimateFutureCost(Rule rule, DPState state, int sentID);

  /**
   * This function could be implemented to process the feature-line arguments in
   * a generic way, if so desired.
   * 
   * TODO: implement this.
   */
  private void processArgs(String argString) {
    return;
  }

  /**
   * Accumulator objects allow us to generalize feature computation.
   * ScoreAccumulator takes (feature,value) pairs and simple stores the weighted
   * sum (for decoding). FeatureAccumulator records the named feature values
   * (for k-best extraction).
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
