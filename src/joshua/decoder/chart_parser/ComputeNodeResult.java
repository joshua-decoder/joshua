package joshua.decoder.chart_parser;

import java.util.ArrayList;

import java.util.List;

import joshua.decoder.Decoder;
import joshua.decoder.ff.StatefulFF;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.segment_file.Sentence;

/**
 * This class computes the cost of applying a rule.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */

public class ComputeNodeResult {

  // The cost incurred by the rule itself (and all associated feature functions)
  private float transitionCost;

  // transitionCost + the Viterbi costs of the tail nodes.
  private float viterbiCost;

  // viterbiCost + a future estimate (outside cost estimate).
  private float pruningCostEstimate;

  // The StateComputer objects themselves serve as keys.
  private List<DPState> dpStates;
  
  /**
   * Computes the new state(s) that are produced when applying the given rule to the list of tail
   * nodes. Also computes a range of costs of doing so (the transition cost, the total (Viterbi)
   * cost, and a score that includes a future cost estimate).
   * 
   * Old version that doesn't use the derivation state.
   */
  public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule, List<HGNode> tailNodes,
      int i, int j, SourcePath sourcePath, Sentence sentence) {

    // The total Viterbi cost of this edge. This is the Viterbi cost of the tail nodes, plus
    // whatever costs we incur applying this rule to create a new hyperedge.
    float viterbiCost = 0.0f;
    
    if (Decoder.VERBOSE >= 4) {
      System.err.println("ComputeNodeResult():");
      System.err.println("-> RULE " + rule);
    }
      
    /*
     * Here we sum the accumulated cost of each of the tail nodes. The total cost of the new
     * hyperedge (the inside or Viterbi cost) is the sum of these nodes plus the cost of the
     * transition. Note that this could and should all be generalized to whatever semiring is being
     * used.
     */
    if (null != tailNodes) {
      for (HGNode item : tailNodes) {
        if (Decoder.VERBOSE >= 4) {
          System.err.println("  -> item.bestedge: " + item);
          System.err.println("-> TAIL NODE " + item);
        }        
        viterbiCost += item.bestHyperedge.getBestDerivationScore();
      }
    }

    List<DPState> allDPStates = new ArrayList<DPState>();

    // The transition cost is the new cost incurred by applying this rule
    float transitionCost = 0.0f;

    // The future cost estimate is a heuristic estimate of the outside cost of this edge.
    float futureCostEstimate = 0.0f;
    
    /*
     * We now iterate over all the feature functions, computing their cost and their expected future
     * cost.
     */
    for (FeatureFunction feature : featureFunctions) {
      FeatureFunction.ScoreAccumulator acc = feature.new ScoreAccumulator(); 

      DPState newState = feature.compute(rule, tailNodes, i, j, sourcePath, sentence, acc);
      transitionCost += acc.getScore();
      
      if (Decoder.VERBOSE >= 4)
        System.err.println(String.format("-> FEATURE %s = %.3f * %.3f = %.3f", 
            feature.getName(), acc.getScore() / Decoder.weights.getSparse(feature.getName()),
            Decoder.weights.getSparse(feature.getName()), acc.getScore()));

      if (feature.isStateful()) {
        futureCostEstimate += feature.estimateFutureCost(rule, newState, sentence);
        allDPStates.add(((StatefulFF)feature).getStateIndex(), newState);
      }
    }
  
    viterbiCost += transitionCost;

    if (Decoder.VERBOSE >= 4)
      System.err.println(String.format("-> COST = %.3f", transitionCost));
    
    // Set the final results.
    this.pruningCostEstimate = viterbiCost + futureCostEstimate;
    this.viterbiCost = viterbiCost;
    this.transitionCost = transitionCost;
    this.dpStates = allDPStates;
  }
  
  /**
   * This is called from Cell.java when making the final transition to the goal state.
   * This is done to allow feature functions to correct for partial estimates, since
   * they now have the knowledge that the whole sentence is complete. Basically, this
   * is only used by LanguageModelFF, which does not score partial n-grams, and therefore
   * needs to correct for this when a short sentence ends. KenLMFF corrects for this by
   * always scoring partial hypotheses, and subtracting off the partial score when longer
   * context is available. This would be good to do for the LanguageModelFF feature function,
   * too: it makes search better (more accurate at the beginning, for example), and would
   * also do away with the need for the computeFinal* class of functions (and hooks in
   * the feature function interface).
   */
  public static float computeFinalCost(List<FeatureFunction> featureFunctions,
      List<HGNode> tailNodes, int i, int j, SourcePath sourcePath, Sentence sentence) {

    float cost = 0;
    for (FeatureFunction ff : featureFunctions) {
      cost += ff.computeFinalCost(tailNodes.get(0), i, j, sourcePath, sentence);
    }
    return cost;
  }
  
  public static FeatureVector computeTransitionFeatures(List<FeatureFunction> featureFunctions,
      HyperEdge edge, int i, int j, Sentence sentence) {

    // Initialize the set of features with those that were present with the rule in the grammar.
    FeatureVector featureDelta = new FeatureVector();
    
    // === compute feature logPs
    for (FeatureFunction ff : featureFunctions) {
      // A null rule signifies the final transition.
      if (edge.getRule() == null)
        featureDelta.add(ff.computeFinalFeatures(edge.getTailNodes().get(0), i, j, edge.getSourcePath(), sentence));
      else {
        featureDelta.add(ff.computeFeatures(edge.getRule(), edge.getTailNodes(), i, j, edge.getSourcePath(), sentence));
      }
    }
    
    return featureDelta;
  }

  public float getPruningEstimate() {
    return this.pruningCostEstimate;
  }

  /**
   *  The complete cost of the Viterbi derivation at this point
   */
  public float getViterbiCost() {
    return this.viterbiCost;
  }
  
  public float getBaseCost() {
    return getViterbiCost() - getTransitionCost();
  }

  /**
   * The cost incurred by this edge alone
   * 
   * @return
   */
  public float getTransitionCost() {
    return this.transitionCost;
  }

  public List<DPState> getDPStates() {
    return this.dpStates;
  }

  public void printInfo() {
    System.out.println("scores: " + transitionCost + "; " + viterbiCost + "; "
        + pruningCostEstimate);
  }
}
