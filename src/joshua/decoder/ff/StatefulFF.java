package joshua.decoder.ff;

import joshua.decoder.hypergraph.HyperEdge;

/**
 * Implements stateFUL feature functions.  These are functions that require access to the dynamic
 * programming state, so they cannot in general be precomputed.  However, there are times when
 * portions of the feature value can be precomputed; an example is in scoring rules by the language
 * model, where words found on the righthand side sometimes have enough context to be computed ahead
 * of time.  For this reason, the 
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */
public abstract class StatefulFF implements FeatureFunction {

  public StatefulFF(WeightVector weights, String name) {
    super(weights, name);
  }

  public StatefulFF(WeightVector weights, String name, String args) {
    super(weights, name, args);
  }

  public final boolean isStateful() {
    return true;
  }

  /**
   * This is a convenience function that unpacks the rule and tail nodes from the hyperedge and
   * chains the call.
   */
  @Override
  public double computeCost(HyperEdge edge, int i, int j, int sentID) {
    return computeCost(edge.getRule(), edge.getAntNodes(), i, j, edge.getSourcePath(), sentID);
  }

  /**
   * Computes the features and their values induced by applying this rule.  This is used for the
   * k-best extraction code, and should also be called from ComputeCost().  Makes use of the
   * WeightVector class, but note this contains feature values and not weights.
   */
  @Override
  public WeightVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, int sentID);


  /**
   * Return the cost of applying a rule for a particular sentence.  The cost is the inner product of
   * (1) the feature vector of features that fire on this rule and (2) the associated weights from
   * the weight vector.
   *
   * For stateless features, the features can only come from the rule itself, the input sentence,
   * neither, or both.  This function should be overridden to be made more efficient than the hash
   * lookup defined here; this default implementation assumes the feature value is 1 and multiplies
   * it times the weight obtained inefficiently from the hash.
   *
   * These functions should make use of computeFeatures().
   */
  @Override
  public double computeCost(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, int sentID);


  /**
   * Often the final transition cost differs in some key way from regular transition costs.  This
   * function can compute that difference.  By default it just chains to computeCost().
   */
  @Override
  public double computeFinalCost(HyperEdge edge, int i, int j, int sentID) {
    return computeCost(edge, i, j, sentID);
  }
}
