package joshua.decoder.ff;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

/**
 * Non-local feature functions are computed over k-best derivations, rather than over the hypergraph
 * directly. For more information, see (especially §3.2--3.3):
 * 
 * @formatter:off
 * @inproceedings{huang2008forest,
 *   Address = {Columbus, Ohio},
 *   Author = {Huang, Liang},
 *   Booktitle = ACL2008,
 *   Month = {June},
 *   Title = {Forest Reranking: Discriminative Parsing with Non-Local Features},
 *   Year = {2008}}
 * @formatter: on
 * 
 * When a non-local feature is requested, the decoder will trigger a few operations that are a bit
 * more expensive.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */

public abstract class NonLocalFF extends FeatureFunction {

  public NonLocalFF(FeatureVector weights, String name) {
    super(weights, name);
  }

  public NonLocalFF(FeatureVector weights, String name, String args) {
    super(weights, name, args);
  }

  public final boolean isStateful() {
    return false;
  }

  /**
   * The estimated cost of applying this feature, given only the rule. This is used in sorting the
   * rules for cube pruning. For most features, this will be 0.0.
   */
  public float estimateCost(Rule rule, int sentID) {
    return 0.0f;
  }

  /**
   * Implementations of this should return null, since no state is contributed.
   */
  @Override
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, int sentID, Accumulator acc);

  /**
   * Implementations of this should return null, since no state is contributed.
   */
  @Override
  public DPState computeFinal(HGNode tailNode, int i, int j, SourcePath sourcePath, int sentID,
      Accumulator acc) {
    return null;
  }

  /**
   * Stateless functions do not have an estimate of the future cost because they do not have access
   * to the state.
   */
  public final float estimateFutureCost(Rule rule, DPState state, int sentID) {
    return 0.0f;
  }
}
