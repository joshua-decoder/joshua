package joshua.decoder.hypergraph;

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;

/**
 * this class implement Hyperedge
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */

public class HyperEdge {

  /**
   * the 1-best logP of all possible derivations: best logP of ant hgnodes + transitionlogP
   **/
  private float bestDerivationScore = Float.NEGATIVE_INFINITY;

  /**
   * this remembers the stateless + non_stateless logP assocated with the rule (excluding the
   * best-logP from ant nodes)
   * */
  private Float transitionScore = null;

  private Rule rule;

  private SourcePath srcPath = null;

  /**
   * If antNodes is null, then this edge corresponds to a rule with zero arity. Aslo, the nodes
   * appear in the list as per the index of the Foreign side non-terminal
   * */
  private List<HGNode> tailNodes = null;

  public HyperEdge(Rule rule, float bestDerivationScore, float transitionScore,
      List<HGNode> tailNodes, SourcePath srcPath) {
    this.bestDerivationScore = bestDerivationScore;
    this.transitionScore = transitionScore;
    this.rule = rule;
    this.tailNodes = tailNodes;
    this.srcPath = srcPath;
  }

  public Rule getRule() {
    return rule;
  }
  
  public float getBestDerivationScore() {
    return bestDerivationScore;
  }

  public SourcePath getSourcePath() {
    return srcPath;
  }

  public List<HGNode> getTailNodes() {
    return tailNodes;
  }

  public float getTransitionLogP(boolean forceCompute) {
    StringBuilder sb = new StringBuilder();
    if (forceCompute || transitionScore == null) {
      float res = bestDerivationScore;
      sb.append(String.format("Best derivation = %.5f", res));
      if (tailNodes != null) for (HGNode tailNode : tailNodes) {
        res += tailNode.bestHyperedge.bestDerivationScore;
        sb.append(String.format(", tail = %.5f", tailNode.bestHyperedge.bestDerivationScore));
      }
      transitionScore = res;
    }
    // System.err.println("HYPEREDGE SCORE = " + sb.toString());
    return transitionScore;
  }

  public void setTransitionLogP(float transitionLogP) {
    this.transitionScore = transitionLogP;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.rule);
//    if (getTailNodes() != null) for (HGNode tailNode : getTailNodes()) {
//      sb.append(" tail=" + tailNode);
//    }
    return sb.toString();
  }
}
