package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * Implements stateless feature functions. Stateless feature functions do not need any access to the
 * state, so they are computable as soon as they are seen. The typical use case for stateless
 * feature function is for feature that are found in the grammar file. If the weight vector is
 * known, the cost of the applying the rule can be computed once at load time, rather than at each
 * time the rule is applied.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */

public abstract class StatelessFF extends FeatureFunction {

  public StatelessFF(FeatureVector weights, String name) {
    super(weights, name);
  }

  public StatelessFF(FeatureVector weights, String name, String args) {
    super(weights, name, args);
  }

  public final boolean isStateful() {
    return false;
  }

  /**
   * By default, just call computeCost.
   */
  public float estimateCost(Rule rule, int sentID) {
    return computeCost(rule, null, sentID);
  }

  public final FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {
    return computeFeatures(rule, sourcePath, sentID);
  }


  /**
   * Computes the features and their values induced by applying this rule. This is used for the
   * k-best extraction code, and should also be called from ComputeCost(). Makes use of the
   * FeatureVector class, but note this contains feature values and not weights.
   */
  public abstract FeatureVector computeFeatures(Rule rule, SourcePath sourcePath, int sentID);


  /**
   * Stateless features do not have access to the state, so we force a chained call to a reduced
   * version.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return
   */
  public final float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID) {
    return computeCost(rule, sourcePath, sentID);
  }

  /**
   * Costs accumulated along the final edge (where no rule is applied).
   */
  public float computeFinalCost(HGNode tailNode, int i, int j, SourcePath sourcePath, int sentID) {
    return 0.0f;
  }

  /**
   * Features across the final, rule-less edge.
   */
  public FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j, SourcePath sourcePath, int sentID) {
    return new FeatureVector(name, 0.0f);
  }


  public final StateComputer getStateComputer() {
    return null;
  }

  /**
   * Stateless functions do not have an estimate of the future cost because they do not have access
   * to the state.
   */
  public final float estimateFutureCost(Rule rule, DPState state, int sentID) {
    return 0.0f;
  }

  /**
   * Return the cost of applying a rule for a particular sentence. The cost is the inner product of
   * (1) the feature vector of features that fire on this rule and (2) the associated weights from
   * the weight vector.
   * 
   * This function should be overridden to be made more efficient than the hash * lookup defined
   * here; this default implementation assumes the feature value is 1 and multiplies * it times the
   * weight obtained inefficiently from the hash.
   */
  public abstract float computeCost(Rule rule, SourcePath sourcePath, int sentID);

}
