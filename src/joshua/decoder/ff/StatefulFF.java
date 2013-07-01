package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.ff.state_maintenance.StateComputer;

/**
 * Stateful features contribute dynamic programming state, so they cannot in general be precomputed.
 * State is maintained and computed separately in Joshua, so each stateful feature function needs to
 * be passed in its state. This allows sharing state objects across features (e.g., for separate
 * language models).
 * 
 * The state objects should be initialized in JoshuaDecoder and passed in when the feature is
 * computed.
 * 
 * TODO: Features should really know about their own state. Sharing would not be needed if a single
 * LMFeatureFunction were responsible for loading all LMs.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */
public abstract class StatefulFF extends FeatureFunction {

  /*
   * Stateful features have a state computer, which is recorded with the feature function.
   */
  protected StateComputer stateComputer = null;

  public StatefulFF(FeatureVector weights, String name, StateComputer stateComputer) {
    super(weights, name, "");

    if (stateComputer == null) {
      System.err.println("* WARNING: state computer is null");
    }
    this.stateComputer = stateComputer;
  }

  public StatefulFF(FeatureVector weights, String name, String args, StateComputer stateComputer) {
    super(weights, name, args);

    this.stateComputer = stateComputer;
  }

  public final boolean isStateful() {
    return true;
  }

  public StateComputer getStateComputer() {
    return stateComputer;
  }

  /**
   * Computes the features and their values induced by applying this rule. This is used for the
   * k-best extraction code, and should also be called from ComputeCost(). Makes use of the
   * FeatureVector class, but note this contains feature values and not weights.
   */
  public abstract FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID);

  /**
   * Convenience function for the above.
   */
  public FeatureVector computeFeatures(HyperEdge edge, int i, int j, int sentID) {
    return computeFeatures(edge.getRule(), edge.getTailNodes(), i, j, edge.getSourcePath(), sentID);
  }

  /**
   * Return the cost of applying a rule for a particular sentence. The cost is the inner product of
   * (1) the feature vector of features that fire on this rule and (2) the associated weights from
   * the weight vector.
   * 
   * For stateless features, the features can only come from the rule itself, the input sentence,
   * neither, or both. This function should be overridden to be made more efficient than the hash
   * lookup defined here; this default implementation assumes the feature value is 1 and multiplies
   * it times the weight obtained inefficiently from the hash.
   * 
   * These functions should make use of computeFeatures().
   */
  public abstract float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID);

  /**
   * This is a convenience function that unpacks the rule and tail nodes from the hyperedge and
   * chains the call.
   */
  public float computeCost(HyperEdge edge, int i, int j, int sentID) {
    return computeCost(edge.getRule(), edge.getTailNodes(), i, j, edge.getSourcePath(), sentID);
  }

  /**
   * Often the final transition cost differs in some key way from regular transition costs. This
   * function can compute that difference. For the final transition, no rule is applied, so no
   * hyperedge is created, so we pass in the final node instead.
   */
  public float computeFinalCost(HGNode node, int i, int j, SourcePath sourcePath, int sentID) {
    return 0.0f;
  }

  /**
   * Computes an estimated future cost of this rule. Note that this is not compute as part of the
   * score but is used for pruning.
   */
  public abstract float estimateFutureCost(Rule rule, DPState state, int sentID);
}
