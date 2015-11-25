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

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.segment_file.Sentence;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */

public class ViterbiExtractor {

  /**
   * This function recursively visits the nodes of the Viterbi derivation in a depth-first
   * traversal, applying the walker to each of the nodes. It provides a more general framework for
   * implementing operations on a tree.
   * 
   * @param node the node to start viterbi traversal from
   * @param walker an implementation of the WalkerFunction interface, to be applied to each node in
   *        the tree
   * @param nodeIndex the tail node index of the given node. This allows implementations of the
   *        WalkerFunction to associate nonTerminals with the index of node in the outgoing edges
   *        list of tail nodes.
   */
  public static void viterbiWalk(
      final HGNode node,
      final WalkerFunction walker,
      final int nodeIndex) {
    // apply the walking function to the node
    walker.apply(node, nodeIndex);
    // recurse on the anterior nodes of the best hyperedge in source order
    final HyperEdge bestEdge = node.bestHyperedge;
    final List<HGNode> tailNodes = bestEdge.getTailNodes();
    if (tailNodes != null) {
      for (int tailNodeIndex = 0; tailNodeIndex < tailNodes.size(); tailNodeIndex++) {
        viterbiWalk(tailNodes.get(tailNodeIndex), walker, tailNodeIndex);
      }
    }
  }
  
  public static void viterbiWalk(final HGNode node, final WalkerFunction walker) {
    viterbiWalk(node, walker, 0);
  }
  
  /**
   * Returns the Viterbi translation of the Hypergraph (includes sentence markers)
   */
  public static String getViterbiString(final HyperGraph hg) {
    if (hg == null)
      return "";
    
    final WalkerFunction viterbiOutputStringWalker = new OutputStringExtractor(false);
    viterbiWalk(hg.goalNode, viterbiOutputStringWalker);
    return viterbiOutputStringWalker.toString();
  }
  
  /**
   * Returns the Viterbi feature vector
   */
  public static FeatureVector getViterbiFeatures(
      final HyperGraph hg,
      final List<FeatureFunction> featureFunctions,
      final Sentence sentence) {
    if (hg == null)
      return new FeatureVector();
    
    final FeatureVectorExtractor extractor = new FeatureVectorExtractor(
        featureFunctions, sentence);
      viterbiWalk(hg.goalNode, extractor);
      return extractor.getFeatures();
  }
  
  /**
   * Returns the Viterbi Word Alignments as String.
   */
  public static String getViterbiWordAlignments(final HyperGraph hg) {
    if (hg == null)
      return "";
    
    final WordAlignmentExtractor wordAlignmentWalker = new WordAlignmentExtractor();
    viterbiWalk(hg.goalNode, wordAlignmentWalker);
    return wordAlignmentWalker.toString();
  }
  
  /**
   * Returns the Viterbi Word Alignments as list of lists (target-side).
   */
  public static List<List<Integer>> getViterbiWordAlignmentList(final HyperGraph hg) {
    if (hg == null)
      return emptyList();
    
    final WordAlignmentExtractor wordAlignmentWalker = new WordAlignmentExtractor();
    viterbiWalk(hg.goalNode, wordAlignmentWalker);
    return wordAlignmentWalker.getFinalWordAlignments();
  }
  
  /** find 1best hypergraph */
  public static HyperGraph getViterbiTreeHG(HyperGraph hg_in) {
    HyperGraph res =
        new HyperGraph(cloneNodeWithBestHyperedge(hg_in.goalNode), -1, -1, null); 
    // TODO: number of items/deductions
    get1bestTreeNode(res.goalNode);
    return res;
  }

  private static void get1bestTreeNode(HGNode it) {
    HyperEdge dt = it.bestHyperedge;
    if (null != dt.getTailNodes()) {
      for (int i = 0; i < dt.getTailNodes().size(); i++) {
        HGNode antNode = dt.getTailNodes().get(i);
        HGNode newNode = cloneNodeWithBestHyperedge(antNode);
        dt.getTailNodes().set(i, newNode);
        get1bestTreeNode(newNode);
      }
    }
  }

  // TODO: tbl_states
  private static HGNode cloneNodeWithBestHyperedge(HGNode inNode) {
    List<HyperEdge> hyperedges = new ArrayList<HyperEdge>(1);
    HyperEdge cloneEdge = cloneHyperedge(inNode.bestHyperedge);
    hyperedges.add(cloneEdge);
    return new HGNode(inNode.i, inNode.j, inNode.lhs, hyperedges, cloneEdge, inNode.getDPStates());
  }


  private static HyperEdge cloneHyperedge(HyperEdge inEdge) {
    List<HGNode> antNodes = null;
    if (null != inEdge.getTailNodes()) {
      antNodes = new ArrayList<HGNode>(inEdge.getTailNodes());// l_ant_items will be changed in
                                                             // get_1best_tree_item
    }
    HyperEdge res =
        new HyperEdge(inEdge.getRule(), inEdge.getBestDerivationScore(), inEdge.getTransitionLogP(false),
            antNodes, inEdge.getSourcePath());
    return res;
  }
}
