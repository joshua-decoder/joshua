package joshua.decoder.hypergraph;

import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Logger;

import joshua.decoder.BLEU;

/**
 * this class implement (1) HyperGraph-related data structures (Item and Hyper-edges)
 * 
 * Note: to seed the kbest extraction, each deduction should have the best_cost properly set. We do
 * not require any list being sorted
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class HyperGraph {

  // pointer to goal HGNode
  public HGNode goalNode = null;

  public int numNodes = -1;
  public int numEdges = -1;
  public int sentID = -1;
  public int sentLen = -1;

  static final Logger logger = Logger.getLogger(HyperGraph.class.getName());

  public HyperGraph(HGNode goalNode, int numNodes, int numEdges, int sentID, int sentLen) {
    this.goalNode = goalNode;
    this.numNodes = numNodes;
    this.numEdges = numEdges;
    this.sentID = sentID;
    this.sentLen = sentLen;
  }

  /**
   * Dump the hypergraph to the specified file.
   * 
   * @param fileName
   */
  public void dump(String fileName) {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter(new FileWriter(fileName));
    } catch (IOException e) {
      System.err.println("* Can't dump hypergraph to file '" + fileName + "'");
      e.printStackTrace();
    }

    HashSet<HGNode> allNodes = new HashSet<HGNode>();
    Stack<HGNode> nodesToVisit = new Stack<HGNode>();
    nodesToVisit.push(this.goalNode);
    while (!nodesToVisit.empty()) {
      HGNode node = nodesToVisit.pop();
      allNodes.add(node);
      if (node.getHyperEdges() != null)
        for (HyperEdge edge : node.getHyperEdges())
          if (edge.getTailNodes() != null)
            for (HGNode tailNode : edge.getTailNodes()) {
              if (!allNodes.contains(tailNode))
                nodesToVisit.push(tailNode);
            }
    }

    ArrayList<HGNode> list = new ArrayList<HGNode>();
    for (HGNode node : allNodes)
      list.add(node);

    Collections.sort(list, HGNode.spanComparator);
    try {
      for (HGNode node : list) {

        out.write(String.format("%s %s\n", Integer.toHexString(node.hashCode()), node));
        if (node.getHyperEdges() != null)
          for (HyperEdge edge : node.getHyperEdges()) {
            out.write(String.format("  %s", edge));
            if (edge.getTailNodes() != null)
              for (HGNode tailNode : edge.getTailNodes())
                out.write(String.format(" ||| %s", Integer.toHexString(tailNode.hashCode())));
            out.write("\n");
          }
      }
      out.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public float bestScore() {
    return this.goalNode.bestHyperedge.getBestDerivationScore();
  }

  /**
   * Rescore a forest using BLEU score.
   * 
   * This is a somewhat complicated endeavor. What we want is to have every edge in the forest
   * reflect a weighted score between (a) the original model score and (b) a BLEU approximation.
   * 
   * This function walks the hyperforest in a depth-first, post-order traversal. This provides us
   * with the necessary CKY guarantee that the antecedents of each node will have been updated
   * before the node itself.
   * 
   * The update does a few things:
   * 
   * <ol>
   * <li> For each hyperedge, we compute the BLEU statistics incurred by applying that edge. Together 
   * with the BLEU statistics from the tail nodes, we can compute a BLEU score, which is recorded
   * for that hyperedge.
   * <li> It is
   * 
   * @param corpusStats
   */
  public void rescore(BLEU.Stats corpusStats, float bleuWeight) {
    final HashMap<HGNode, BLEU.Stats> bleuStatsHash = new HashMap<HGNode, BLEU.Stats>();

    ForestWalker walker = new ForestWalker(ForestWalker.TRAVERSAL.POSTORDER);
    walker.walk(this.goalNode, new WalkerFunction() {
      public void apply(HGNode node) {
        for (HyperEdge edge : node.getHyperEdges()) {
          BLEU.Stats stats = BLEU.compute(edge.getRule());
          if (edge.getTailNodes() != null) {
            for (HGNode tailNode : edge.getTailNodes()) {
              stats.add(bleuStatsHash.get(tailNode));
            }
          }
        }
      }
    });
  }
}
