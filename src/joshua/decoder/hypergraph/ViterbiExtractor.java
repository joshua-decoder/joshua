package joshua.decoder.hypergraph;

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */

public class ViterbiExtractor {

  // get one-best string under item
  public static String extractViterbiString(HGNode node) {
    StringBuffer res = new StringBuffer();

    HyperEdge edge = node.bestHyperedge;
    Rule rl = edge.getRule();

    if (null == rl) { // deductions under "goal item" does not have rule
      if (edge.getTailNodes().size() != 1) {
        throw new RuntimeException("deduction under goal item have not equal one item");
      }
      return extractViterbiString(edge.getTailNodes().get(0));
    }
    int[] english = rl.getEnglish();
    for (int c = 0; c < english.length; c++) {
      if (Vocabulary.nt(english[c])) {
        int id = -(english[c] + 1);
        HGNode child = (HGNode) edge.getTailNodes().get(id);
        res.append(extractViterbiString(child));
      } else {
        res.append(Vocabulary.word(english[c]));
      }
      if (c < english.length - 1) res.append(' ');
    }
    return res.toString();
  }

  // ######## find 1best hypergraph#############
  public static HyperGraph getViterbiTreeHG(HyperGraph hg_in) {
    HyperGraph res =
        new HyperGraph(cloneNodeWithBestHyperedge(hg_in.goalNode), -1, -1, null); 
    // TODO: number of items/deductions
    get1bestTreeNode(res.goalNode);
    return res;
  }

  /**
   * This function recursively visits the nodes of the Viterbi derivation in a depth-first
   * traversal, applying the walker to each of the nodes. It provides a more general framework for
   * implementing operations on a tree.
   * 
   * @param node the node to start traversal from
   * @param walker an implementation of the ViterbieWalker interface, to be applied to each node in
   *        the tree
   */
  public static void walk(HGNode node, WalkerFunction walker) {
    // apply the walking function to the node
    walker.apply(node);

    // recurse on the anterior nodes of the best hyperedge
    HyperEdge bestEdge = node.bestHyperedge;
    if (null != bestEdge.getTailNodes()) {
      for (HGNode antNode : bestEdge.getTailNodes()) {
        walk(antNode, walker);
      }
    }
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
  // ###end
}
