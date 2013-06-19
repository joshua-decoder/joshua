package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**
 * This class defines is the entry point for Joshua's dense+sparse feature implementation. It
 * defines some basic variables and interfaces that are common to all features, and is immediately
 * inherited by StatelessFF and StatefulFF, which provide functionality common to stateless and
 * stateful features, respectively. Any feature implementation should extend those classes, and not
 * this one.
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

  /**
   * This function computes a *weighted* cost of this feature. Stateless features have access to
   * only stateless variables.
   * 
   * @param rule
   * @param tailNodes
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return the *weighted* cost of the feature.
   */
  public abstract float computeCost(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID);

  /**
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
  public abstract FeatureVector computeFeatures(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID);

  /**
   * This function is called for the final transition. For example, the LanguageModel feature
   * function treats the last rule specially. It needs to return the *weighted* cost of applying the
   * feature.
   * 
   * @param tailNode
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return a *weighted* feature cost
   */
  public abstract float computeFinalCost(HGNode tailNode, int i, int j, SourcePath sourcePath,
      int sentID);

  /**
   * Returns the *unweighted* feature delta for the final transition (e.g., for the language model
   * feature function).
   * 
   * @param tailNode
   * @param i
   * @param j
   * @param sourcePath
   * @param sentID
   * @return
   */
  public abstract FeatureVector computeFinalFeatures(HGNode tailNode, int i, int j,
      SourcePath sourcePath, int sentID);

  public abstract StateComputer getStateComputer();

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
   */
  private void processArgs(String argString) {
    return;
  }

  /**************************************************************
   * OLD INTERFACE DON'T USE WILL SOON DELETE *******************
   **************************************************************/

  /**
   * estimate future logP, e.g., the logPs of partial n-grams asscociated with the left-edge ngram
   * state
   * */
  // double estimateFutureLogP(Rule rule, DPState curDPState, int sentID);

  // double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
  // SourcePath srcPath, int sentID);

  // double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID);

  // double reEstimateTransitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd,
  // SourcePath srcPath, int sentID);

  /**
   * Edges calling finalTransition do not have concret rules associated with them.
   * */
  // double finalTransitionLogP(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath,
  // int sentID);

  // double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID);

}
