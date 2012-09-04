package joshua.decoder.chart_parser;

import java.util.List;
import java.util.TreeMap;

import joshua.decoder.ff.DefaultStatefulFF;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-12-22 14:00:36 -0500 $
 */

public class ComputeNodeResult {

  private double expectedTotalLogP;
  private double finalizedTotalLogP;
  private double transitionTotalLogP;

  // the key is state id;
  private TreeMap<Integer, DPState> dpStates;

  /**
   * Compute logPs and the states of the node
   */
  public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule,
      List<HGNode> antNodes, int i, int j, SourcePath srcPath, List<StateComputer> stateComputers,
      int sentID) {

    double finalizedTotalLogP = 0.0;

    if (null != antNodes) {
      for (HGNode item : antNodes) {
        finalizedTotalLogP += item.bestHyperedge.bestDerivationLogP; // semiring times
      }
    }

    TreeMap<Integer, DPState> allDPStates = null;

    /* For each type of state (usually just the ngram state), we need to compute the new state that
     * is created when applying the rule to the tail nodes at the current span.
     */
    if (stateComputers != null) {
      for (StateComputer stateComputer : stateComputers) {
        // Compute the new state and store it in a treemap of states indexed by the state ID.  This
        // treemap can contain subtypes of many different kinds.  These DP states are used to
        // identify the resultant hypergraph node in the hyperforest, and are also passed to their
        // associated stateful feature functions (note that the function passing DP states to
        // feature functions below does not make this clear).
        DPState dpState = stateComputer.computeState(rule, antNodes, i, j, srcPath);

        if (allDPStates == null) allDPStates = new TreeMap<Integer, DPState>();
        allDPStates.put(stateComputer.getStateID(), dpState);
      }
    }

    // Compute feature logPs.
    double transitionLogPSum = 0.0;
    double futureLogPEstimation = 0.0;

    /* We now iterate over all the feature functions, computing their cost and their expected future
     * cost.  This code does not effectively distinguish between stateful and stateless feature
     * functions, which is somewhat of a waste, since the dpState that is passed in to the feature
     * function's call to estimateFutureLogP() will be null for all stateless features.
     */
    for (FeatureFunction ff : featureFunctions) {
      transitionLogPSum +=
          ff.getWeight() * ff.reEstimateTransitionLogP(rule, antNodes, i, j, srcPath, sentID);
      DPState dpState = null;
      if (allDPStates != null) dpState = allDPStates.get(ff.getStateID());
      futureLogPEstimation += ff.getWeight() * ff.estimateFutureLogP(rule, dpState, sentID);
    }
    transitionLogPSum -= rule.getEstCost();

    finalizedTotalLogP += transitionLogPSum;
    double expectedTotalLogP = finalizedTotalLogP + futureLogPEstimation;

    // Set the final results.
    this.expectedTotalLogP = expectedTotalLogP;
    this.finalizedTotalLogP = finalizedTotalLogP;
    this.transitionTotalLogP = transitionLogPSum;
    this.dpStates = allDPStates;
  }


  /**
   * This is called when making the final transition to the goal state.
   */ 
  public static double computeCombinedTransitionLogP(List<FeatureFunction> featureFunctions,
      HyperEdge edge, int i, int j, int sentID) {
    double res = 0;
    for (FeatureFunction ff : featureFunctions) {
      if (edge.getRule() != null)
        res += ff.getWeight() * ff.transitionLogP(edge, i, j, sentID);
      else
        res += ff.getWeight() * ff.finalTransitionLogP(edge, i, j, sentID);
    }
    return res;
  }


  /**
   * This is called when making the final transition to the goal state.
   */ 
  public static double computeCombinedTransitionLogP(List<FeatureFunction> featureFunctions,
      Rule rule, List<HGNode> antNodes, int i, int j, SourcePath srcPath, int sentID) {
    double res = 0;
    for (FeatureFunction ff : featureFunctions) {
      if (rule != null)
        res += ff.getWeight() * ff.transitionLogP(rule, antNodes, i, j, srcPath, sentID);
      else
        res += ff.getWeight() * ff.finalTransitionLogP(antNodes.get(0), i, j, srcPath, sentID);
    }
    return res;
  }


  /**
   *  This function is called in the hypergraph code for doing k-best extraction.
   */
  public static double[] computeModelTransitionLogPs(List<FeatureFunction> featureFunctions,
      HyperEdge edge, int i, int j, int sentID) {

    double[] res = new double[featureFunctions.size()];

    // === compute feature logPs
    int k = 0;
    for (FeatureFunction ff : featureFunctions) {
      if (edge.getRule() != null)
        res[k] = ff.transitionLogP(edge, i, j, sentID);
      else
        res[k] = ff.finalTransitionLogP(edge, i, j, sentID);
      k++;
    }

    return res;
  }

  /**
   *  This function is called in the hypergraph code for doing k-best extraction.
   */
  public static double[] computeModelTransitionLogPs(List<FeatureFunction> featureFunctions,
      Rule rule, List<HGNode> antNodes, int i, int j, SourcePath srcPath, int sentID) {

    double[] res = new double[featureFunctions.size()];

    // === compute feature logPs
    int k = 0;
    for (FeatureFunction ff : featureFunctions) {
      if (rule != null)
        res[k] = ff.transitionLogP(rule, antNodes, i, j, srcPath, sentID);
      else
        res[k] = ff.finalTransitionLogP(antNodes.get(0), i, j, srcPath, sentID);
      k++;
    }

    return res;
  }

  // this function is never called
  void setExpectedTotalLogP(double logP) {
    this.expectedTotalLogP = logP;
  }

  public double getExpectedTotalLogP() {
    return this.expectedTotalLogP;
  }

  void setFinalizedTotalLogP(double logP) {
    this.finalizedTotalLogP = logP;
  }

  double getFinalizedTotalLogP() {
    return this.finalizedTotalLogP;
  }

  void setTransitionTotalLogP(double logP) {
    this.transitionTotalLogP = logP;
  }

  double getTransitionTotalLogP() {
    return this.transitionTotalLogP;
  }

  void setDPStates(TreeMap<Integer, DPState> states) {
    this.dpStates = states;
  }

  TreeMap<Integer, DPState> getDPStates() {
    return this.dpStates;
  }

  public void printInfo() {
    System.out.println("scores: " + transitionTotalLogP + "; " + finalizedTotalLogP + "; "
        + expectedTotalLogP);
  }
}
