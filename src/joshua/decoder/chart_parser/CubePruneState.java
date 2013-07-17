package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;

// ===============================================================
// CubePruneState class
// ===============================================================
public class CubePruneState implements Comparable<CubePruneState> {
  int[] ranks;
  ComputeNodeResult computeNodeResult;
  Rule rule;
  List<HGNode> antNodes;
  private DotNode dotNode;

  public CubePruneState(ComputeNodeResult state, int[] ranks, Rule rule, List<HGNode> antecedents) {
    this.computeNodeResult = state;
    this.ranks = ranks;
    this.rule = rule;
    // create a new vector is critical, because currentAntecedents will change later
    this.antNodes = new ArrayList<HGNode>(antecedents);
    this.dotNode = null;
  }

  /**
   * This returns the list of DP states associated with the result.
   * 
   * @return
   */
  List<DPState> getDPStates() {
    return this.computeNodeResult.getDPStates();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("STATE ||| rule=" + rule + " inside cost = " + computeNodeResult.getViterbiCost()
        + " estimate = " + computeNodeResult.getPruningEstimate());
    return sb.toString();
  }

  public void setDotNode(DotNode node) {
    this.dotNode = node;
  }

  public DotNode getDotNode() {
    return this.dotNode;
  }

  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (!this.getClass().equals(obj.getClass()))
      return false;
    CubePruneState state = (CubePruneState) obj;
    if (state.ranks.length != ranks.length)
      return false;
    for (int i = 0; i < ranks.length; i++)
      if (state.ranks[i] != ranks[i])
        return false;
    if (getDotNode() != state.getDotNode())
      return false;

    return true;
  }

  public int hashCode() {
    int hash = (dotNode != null) ? dotNode.hashCode() : 0;
    hash += Arrays.hashCode(ranks);

    return hash;
  }

  /**
   * Compares states by ExpectedTotalLogP, allowing states to be sorted according to their inverse
   * order (high-prob first).
   */
  public int compareTo(CubePruneState another) {
    if (this.computeNodeResult.getPruningEstimate() < another.computeNodeResult
        .getPruningEstimate()) {
      return 1;
    } else if (this.computeNodeResult.getPruningEstimate() == another.computeNodeResult
        .getPruningEstimate()) {
      return 0;
    } else {
      return -1;
    }
  }
}