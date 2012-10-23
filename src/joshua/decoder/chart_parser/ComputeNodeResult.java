package joshua.decoder.chart_parser;

import java.util.List;
import java.util.TreeMap;

import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;


/**
 * This class computes the cost of applying a rule.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */

public class ComputeNodeResult {

  // The cost of applying a rule at a particular spot in the hypergraph.
  private float transitionCost;

  // transitionCost + the Viterbi costs of the tail nodes.
  private float viterbiCost;

  // viterbiCost + a future estimate (outside cost estimate).
  private float pruningCostEstimate;

  // The StateComputer objects themselves serve as keys.
  private TreeMap<StateComputer, DPState> dpStates;

  /**
   * Computes the new state(s) that are produced when applying the given rule to the list of tail
   * nodes. Also computes a range of costs of doing so (the transition cost, the total (Viterbi)
   * cost, and a score that includes a future cost estimate).
   */
  public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule,
      List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      List<StateComputer> stateComputers, int sentID) {

    // The total Viterbi cost of this edge. This is the Viterbi cost of the tail nodes, plus
    // whatever costs we incur applying this rule to create a new hyperedge.
    float viterbiCost = 0.0f;

    /*
     * Here we sum the accumulated cost of each of the tail nodes. The total cost of the new
     * hyperedge (the inside or Viterbi cost) is the sum of these nodes plus the cost of the
     * transition. Note that this could and should all be generalized to whatever semiring is being
     * used.
     */
    if (null != tailNodes) {
      for (HGNode item : tailNodes) {
        viterbiCost += item.bestHyperedge.bestDerivationLogP;
      }
    }

    /*
     * For each type of state (usually just the ngram state), we need to compute the new state that
     * is created when applying the rule to the tail nodes at the current span. This new state is
     * stored in a hash indexed by the state computer, so that the stateful feature functions can
     * find them.
     */
    TreeMap<StateComputer, DPState> allDPStates = new TreeMap<StateComputer, DPState>();
    if (stateComputers != null) {
      for (StateComputer stateComputer : stateComputers) {
        DPState dpState = stateComputer.computeState(rule, tailNodes, i, j, sourcePath);
        allDPStates.put(stateComputer, dpState);
      }
    }

    // The transition cost is the new cost incurred by applying this rule
    float transitionCost = 0.0f;

    // The future cost estimate is a heuristic estimate of the outside cost of this edge.
    float futureCostEstimate = 0.0f;

//    StringBuffer sb = new StringBuffer("ComputeNodeResult:");
//    FeatureVector features = new FeatureVector();
    
    /*
     * We now iterate over all the feature functions, computing their cost and their expected future
     * cost.
     */
    for (FeatureFunction feature : featureFunctions) {
      transitionCost += feature.computeCost(rule, tailNodes, i, j, sourcePath, sentID);
//      features.add(feature.computeFeatures(rule,tailNodes,i,j,sourcePath,sentID));
//      sb.append(String.format(" %s: %.3f", feature.getClass().getSimpleName(), feature.computeCost(rule, tailNodes, i, j, sourcePath, sentID)));
      if (feature instanceof StatefulFF) {
        futureCostEstimate += feature.estimateFutureCost(rule, allDPStates.get(feature.getStateComputer()), sentID);
      }
    }
    //transitionCost -= rule.getEstimatedCost();
    
    viterbiCost += transitionCost;

//    System.err.println(sb.toString() + " ||| " + viterbiCost + " ||| " + features);
    
    float pruningEstimate = viterbiCost + futureCostEstimate;

    // Set the final results.
    this.pruningCostEstimate = pruningEstimate; // old expectedTotalLogP
    this.viterbiCost = viterbiCost; // old finalizedTotalLogP
    this.transitionCost = transitionCost; // old transitionTotalLogP
    this.dpStates = allDPStates;
  }

  /**
   * This is called from Cell.java when making the final transition to the goal state.
   */
  public static float computeFinalCost(List<FeatureFunction> featureFunctions,
      List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, int sentID) {

    float cost = 0;
    for (FeatureFunction ff : featureFunctions) {
      if (ff instanceof StatefulFF)
        cost += ((StatefulFF) ff).computeFinalCost(tailNodes.get(0), i, j, sourcePath, sentID);
    }
    return cost;
  }


  /**
   * This function is called in the hypergraph code for doing k-best extraction. It computes and
   * returns the features that are fired when the rule is applied at this edge. Note that this
   * feature vector is the delta computed by the transition, not the total inside/Viterbi cost of
   * the edge. The transition might increment previous features or introduce new ones entirely.
   */
  public static FeatureVector computeTransitionFeatures(List<FeatureFunction> featureFunctions,
      HyperEdge edge, int i, int j, int sentID) {

    // Initialize the set of features with those that were present with the rule in the grammar.
    FeatureVector featureDelta = new FeatureVector();
    
    // === compute feature logPs
    for (FeatureFunction ff : featureFunctions) {
      // A null rule signifies the final transition.
      if (edge.getRule() == null)
        featureDelta.add(ff.computeFinalFeatures(edge.getTailNodes().get(0), i, j, edge.getSourcePath(), sentID));
      else {
        featureDelta.add(ff.computeFeatures(edge.getRule(), edge.getTailNodes(), i, j, edge.getSourcePath(), sentID));
      }
    }
    
    return featureDelta;
  }

  // For backward compatibility.
  public float getExpectedTotalLogP() {
    return getPruningEstimate();
  }

  public float getPruningEstimate() {
    return this.pruningCostEstimate;
  }

  float getFinalizedTotalLogP() {
    return getViterbiCost();
  }

  float getViterbiCost() {
    return this.viterbiCost;
  }

  float getTransitionTotalLogP() {
    return getTransitionCost();
  }

  float getTransitionCost() {
    return this.transitionCost;
  }

  TreeMap<StateComputer, DPState> getDPStates() {
    return this.dpStates;
  }

  public void printInfo() {
    System.out.println("scores: " + transitionCost + "; " + viterbiCost + "; "
        + pruningCostEstimate);
  }
}
