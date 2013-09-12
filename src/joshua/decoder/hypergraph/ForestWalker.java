package joshua.decoder.hypergraph;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Matt Post <post@cs.jhu.edu>
 */

/**
 * This class visits every node in a forest using a depth-first, preorder traversal, applying the
 * WalkerFunction to each node. It would be easy to add other traversals if the demand arose.
 */
public class ForestWalker {

  public static enum TRAVERSAL {
    PREORDER, POSTORDER
  };

  private Set<HGNode> visitedNodes;
  private TRAVERSAL traversalType = TRAVERSAL.PREORDER;

  public ForestWalker() {
    visitedNodes = new HashSet<HGNode>();
  }

  public ForestWalker(TRAVERSAL traversal) {
    this.traversalType = traversal;
    visitedNodes = new HashSet<HGNode>();
  }

  public void walk(HGNode node, WalkerFunction walker) {
    // short circuit
    if (visitedNodes.contains(node))
      return;

    visitedNodes.add(node);
    
    if (this.traversalType == TRAVERSAL.PREORDER)
      walker.apply(node);

    if (node.getHyperEdges() != null) {
      for (HyperEdge edge : node.getHyperEdges()) {
        if (edge.getTailNodes() != null) {
          for (HGNode tailNode : edge.getTailNodes()) {
            walk(tailNode, walker);
          }
        }
      }
    }
    
    if (this.traversalType == TRAVERSAL.POSTORDER)
      walker.apply(node);
  }
}
