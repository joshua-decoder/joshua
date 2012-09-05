package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**
 * This class defines is the entry point for Joshua's dense+sparse feature implementation.  It
 * defines some basic variables and interfaces that are common to all features, and is immediately
 * inherited by StatelessFF and StatefulFF, which provide functionality common to stateless and
 * stateful features, respectively.  Any feature implementation should extend those classes, and not
 * this one.
 *
 * Features in Joshua work like templates.  Each feature function defines any number of actual
 * features, which are associated with weights.  The task of the feature function is to compute the
 * features that are fired in different circumstances and then return the inner product of those
 * features with the weight vector.  Feature functions can also produce estimates of their future
 * cost; these values are not used in computing the score, but are only used for pruning.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */
public abstract class FeatureFunction {

  // ===============================================================
  // Attributes
  // ===============================================================

  // The name of the feature function (also the prefix on weights)
  private String name = null;

  // The list of arguments passed to the feature.
  private String argString;

  // The weight vector used by the decoder, passed it when the feature is instantiated.
  private WeightVector weights;

  // Accessor functions
  public String getName() { 
    return name;
  }

  // Whether the feature has state.
  public boolean isStateful();

  // ===============================================================
  // Methods
  // ===============================================================

  public FeatureFunction(WeightVector weights) {
    this.weights = weights;
  }

  public FeatureFunction(WeightVector weights, String args) {
    this.weights = weights;
    this.argString = args;

    processArgs(this.argString);
  }

  /**
   * This function could be implemented to process the feature-line arguments in a generic way, if
   * so desired.
   */
  private void processArgs(String argString) {
    return;
  }

  /**************************************************************
   * OLD INTERFACE DON'T USE WILL SOON DELETE *******************
   **************************************************************/

  /**
   * It is used when initializing translation grammars (for pruning purpose, and to get stateless
   * logP for each rule). This is also required to sort the rules (required by Cube-pruning).
   */
  double estimateLogP(Rule rule, int sentID);


  /**
   * estimate future logP, e.g., the logPs of partial n-grams asscociated with the left-edge ngram
   * state
   * */
  double estimateFutureLogP(Rule rule, DPState curDPState, int sentID);

  double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
      SourcePath srcPath, int sentID);

  double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID);

  double reEstimateTransitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
      SourcePath srcPath, int sentID);

  /**
   * Edges calling finalTransition do not have concret rules associated with them.
   * */
  double finalTransitionLogP(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath,
      int sentID);

  double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID);

}
