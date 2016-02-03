/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
      walk(node, walker, 0);
  }

  private void walk(HGNode node, WalkerFunction walker, int nodeIndex) {
    // short circuit
    if (visitedNodes.contains(node))
      return;

    visitedNodes.add(node);
    
    if (this.traversalType == TRAVERSAL.PREORDER)
      walker.apply(node, 0);

    if (node.getHyperEdges() != null) {
      for (HyperEdge edge : node.getHyperEdges()) {
        if (edge.getTailNodes() != null) {
          int tailNodeIndex = 0;
          for (HGNode tailNode : edge.getTailNodes()) {
            walk(tailNode, walker, tailNodeIndex);
            tailNodeIndex++;
          }
        }
      }
    }
    
    if (this.traversalType == TRAVERSAL.POSTORDER)
      walker.apply(node, nodeIndex);
  }
}
