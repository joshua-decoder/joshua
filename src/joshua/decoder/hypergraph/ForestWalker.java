/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
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

  private Set<HGNode> visitedNodes;

  // private int numVisited;

  public ForestWalker() {
    visitedNodes = new HashSet<HGNode>();
    // numVisited = 0;
  }

  public void walk(HGNode node, WalkerFunction walker) {
    // short circuit
    if (visitedNodes.contains(node)) return;
    visitedNodes.add(node);
    // numVisited++;
    // if (numVisited % 1000 == 0)
    // System.err.printf(" * Visited %d nodes\n", numVisited);
    // apply the function
    walker.apply(node);

    if (node.getHyperEdges() != null) {
      for (HyperEdge edge : node.getHyperEdges()) {
        if (edge.getAntNodes() != null) {
          for (HGNode tailNode : edge.getAntNodes()) {
            walk(tailNode, walker);
          }
        }
      }
    }
  }
}
