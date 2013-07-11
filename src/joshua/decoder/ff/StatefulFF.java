package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

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

  /* Every stateful FF takes a unique index value and increments this. */
  static int GLOBAL_STATE_INDEX = 0;

  protected int stateIndex = 0;

  public StatefulFF(FeatureVector weights, String name) {
    super(weights, name, "");

    System.err.println("Stateful object with state index " + GLOBAL_STATE_INDEX);
    stateIndex = GLOBAL_STATE_INDEX++;
  }

  public StatefulFF(FeatureVector weights, String name, String args) {
    super(weights, name, args);
    
    System.err.println("Stateful object with state index " + GLOBAL_STATE_INDEX);
    stateIndex = GLOBAL_STATE_INDEX++;
    
  }

  public final boolean isStateful() {
    return true;
  }

  public final int getStateIndex() {
    return stateIndex;
  }

  /**
   * Computes the features and their values induced by applying this rule. This is used for the
   * k-best extraction code, and should also be called from ComputeCost(). Makes use of the
   * FeatureVector class, but note this contains feature values and not weights.
   */
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID, Accumulator acc);

  public abstract DPState computeFinal(HGNode tailNodes, int i, int j, SourcePath sourcePath,
      int sentID, Accumulator acc);

  /**
   * Computes an estimated future cost of this rule. Note that this is not compute as part of the
   * score but is used for pruning.
   */
  public abstract float estimateFutureCost(Rule rule, DPState state, int sentID);
}
