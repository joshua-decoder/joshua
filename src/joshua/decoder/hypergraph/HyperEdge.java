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
  public double bestDerivationLogP = Double.NEGATIVE_INFINITY;

  /**
   * this remembers the stateless + non_stateless logP assocated with the rule (excluding the
   * best-logP from ant nodes)
   * */
  private Double transitionLogP = null;

  private Rule rule;

  private SourcePath srcPath = null;

  /**
   * If antNodes is null, then this edge corresponds to a rule with zero arity. Aslo, the nodes
   * appear in the list as per the index of the Foreign side non-terminal
   * */
  private List<HGNode> antNodes = null;

  public HyperEdge(Rule rule, double bestDerivationLogP, Double transitionLogP,
      List<HGNode> antNodes, SourcePath srcPath) {
    this.bestDerivationLogP = bestDerivationLogP;
    this.transitionLogP = transitionLogP;
    this.rule = rule;
    this.antNodes = antNodes;
    this.srcPath = srcPath;
   }

  public Rule getRule() {
    return rule;
  }

  public SourcePath getSourcePath() {
    return srcPath;
  }

  public List<HGNode> getTailNodes() {
    return antNodes;
  }
  public List<HGNode> getAntNodes() {
    return antNodes;
  }


  public double getTransitionLogP(boolean forceCompute) {
    StringBuilder sb = new StringBuilder();
    if (forceCompute || transitionLogP == null) {
      double res = bestDerivationLogP;
      sb.append(String.format("Best derivation = %.5f", res));
      if (antNodes != null) 
        for (HGNode tailNode : antNodes) {
          res -= tailNode.bestHyperedge.bestDerivationLogP;
          sb.append(String.format(", tail = %.5f", tailNode.bestHyperedge.bestDerivationLogP));
        }
      transitionLogP = res;
    }
//    System.err.println("HYPEREDGE SCORE = " + sb.toString());
    return transitionLogP;
  }

  public void setTransitionLogP(double transitionLogP) {
    this.transitionLogP = transitionLogP;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("HYPEREDGE[rule=" + this.rule);
    if (getTailNodes() != null)
      for (HGNode tailNode: getTailNodes()) {
        sb.append(" tail=" + tailNode);
      }
    sb.append("]");
    return sb.toString();
  }
}
