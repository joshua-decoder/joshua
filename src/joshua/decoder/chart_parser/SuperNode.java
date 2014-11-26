package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.hypergraph.HGNode;

/**
 * Represents a list of items in the hypergraph that have the same left-hand side but may have
 * different LM states.
 * 
 * @author Zhifei Li
 */
class SuperNode {

  /** Common left-hand side state. */
  final int lhs;

  /**
   * List of hypergraph nodes, each of which has its own language model state.
   */
  final List<HGNode> nodes;

  /**
   * All nodes in a SuperNode have the same start and end points, so we pick the first one and
   * return it.
   * 
   * @return
   */
  public int end() {
    return nodes.get(0).j;
  }
  
  
  /**
   * Constructs a super item defined by a common left-hand side.
   * 
   * @param lhs Left-hand side token
   */
  public SuperNode(int lhs) {
    this.lhs = lhs;
    this.nodes = new ArrayList<HGNode>();
  }
}
