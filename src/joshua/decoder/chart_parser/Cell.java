package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/**
 * this class implement functions: (1) combine small itesm into larger ones using rules, and create
 * items and hyper-edges to construct a hyper-graph, (2) evaluate model score for items, (3)
 * cube-pruning Note: Bin creates Items, but not all Items will be used in the hyper-graph
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
class Cell {

  // ===============================================================
  // Private instance fields
  // ===============================================================
  private Chart chart = null;

  private int goalSymID;

  // to maintain uniqueness of nodes
  private HashMap<HGNode.Signature, HGNode> nodesSigTbl = new HashMap<HGNode.Signature, HGNode>();

  // signature by lhs
  private Map<Integer, SuperNode> superNodesTbl = new HashMap<Integer, SuperNode>();

  /**
   * sort values in nodesSigTbl, we need this list when necessary
   */
  private List<HGNode> sortedNodes = null;

  // ===============================================================
  // Static fields
  // ===============================================================
  private static final Logger logger = Logger.getLogger(Cell.class.getName());

  // ===============================================================
  // Constructor
  // ===============================================================

  public Cell(Chart chart, int goalSymID) {
    this.chart = chart;
    this.goalSymID = goalSymID;
  }

  public Cell(Chart chart, int goal_sym_id, int constraint_symbol_id) {
    this(chart, goal_sym_id);
  }

  // ===============================================================
  // Package-protected methods
  // ===============================================================

  /**
   * add all the items with GOAL_SYM state into the goal bin the goal bin has only one Item, which
   * itself has many hyperedges only "goal bin" should call this function
   */
  // note that the input bin is bin[0][n], not the goal bin
  boolean transitToGoal(Cell bin, List<FeatureFunction> featureFunctions, int sentenceLength) {
    this.sortedNodes = new ArrayList<HGNode>();
    HGNode goalItem = null;

    for (HGNode antNode : bin.getSortedNodes()) {
      if (antNode.lhs == this.goalSymID) {
        double logP = antNode.bestHyperedge.bestDerivationLogP;
        List<HGNode> antNodes = new ArrayList<HGNode>();
        antNodes.add(antNode);

        double finalTransitionLogP = ComputeNodeResult.computeFinalCost(featureFunctions, antNodes,
            0, sentenceLength, null, this.chart.segmentID);

        List<HGNode> previousItems = new ArrayList<HGNode>();
        previousItems.add(antNode);

        HyperEdge dt = new HyperEdge(null, logP + finalTransitionLogP, finalTransitionLogP,
            previousItems, null);

        if (null == goalItem) {
          goalItem = new HGNode(0, sentenceLength + 1, this.goalSymID, null, dt, logP
              + finalTransitionLogP);
          this.sortedNodes.add(goalItem);
        } else {
          goalItem.addHyperedgeInNode(dt);
        }
      } // End if item.lhs == this.goalSymID
    } // End foreach Item in bin.get_sorted_items()

    if (logger.isLoggable(Level.INFO)) {
      if (null == goalItem) {
        logger.severe("goalItem is null!");
        return false;
      } else {
        logger.info(String.format("Sentence id=" + this.chart.segmentID + "; BestlogP=%.3f",
            goalItem.bestHyperedge.bestDerivationLogP));
      }
    }
    ensureSorted();

    int itemsInGoalBin = getSortedNodes().size();
    if (1 != itemsInGoalBin) {
      logger.severe("the goal_bin does not have exactly one item");
      return false;
    }

    return true;
  }

  /**
   * a note about pruning: when a hyperedge gets created, it first needs to pass through
   * shouldPruneEdge filter. Then, if it does not trigger a new node (i.e. will be merged to an old
   * node), then does not trigger pruningNodes. If it does trigger a new node (either because its
   * signature is new or because its logP is better than the old node's logP), then it will trigger
   * pruningNodes, which might causes *other* nodes got pruned as well
   * */

  /**
   * Creates a new hyperedge and adds it to the chart, subject to pruning. The logic of this
   * function is as follows: if the pruner permits the edge to be added, we build the new edge,
   * which ends in an HGNode. If this is the first time we've built an HGNode for this point in the
   * graph, it gets added automatically. Otherwise, we add the hyperedge to the existing HGNode,
   * possibly updating the HGNode's cache of the best incoming hyperedge.
   * 
   * @return the new hypernode, or null if the cell was pruned.
   */
  HGNode addHyperEdgeInCell(ComputeNodeResult result, Rule rule, int i, int j, List<HGNode> ants,
      SourcePath srcPath, boolean noPrune) {

    HGNode newNode = null;

    // System.err.println(String.format("ADD_EDGE(%s,%d,%d", rule, i, j));

    List<DPState> dpStates = result.getDPStates();
    double pruningEstimate = result.getPruningEstimate();
    double transitionLogP = result.getTransitionCost();
    double finalizedTotalLogP = result.getViterbiCost();

    /**
     * Here, the edge has passed pre-pruning. The edge will be added to the chart in one of three
     * ways:
     * 
     * 1. If there is no existing node, a new one gets created and the edge is its only incoming
     * hyperedge.
     * 
     * 2. If there is an existing node, the edge will be added to its list of incoming hyperedges,
     * possibly taking place as the best incoming hyperedge for that node.
     */

    HyperEdge hyperEdge = new HyperEdge(rule, finalizedTotalLogP, transitionLogP, ants, srcPath);
    newNode = new HGNode(i, j, rule.getLHS(), dpStates, hyperEdge, pruningEstimate);

    /**
     * each node has a list of hyperedges, need to check whether the node is already exist, if
     * yes, just add the hyperedges, this may change the best logP of the node
     * */
    HGNode oldNode = this.nodesSigTbl.get(newNode.signature());
    if (null != oldNode) { // have an item with same states, combine items
      this.chart.nMerged++;

      /**
       * the position of oldItem in this.heapItems may change, basically, we should remove the
       * oldItem, and re-insert it (linear time), this is too expense)
       **/
      if (newNode.getPruneLogP() > oldNode.getPruneLogP()) { // merge old to new: semiring plus

        newNode.addHyperedgesInNode(oldNode.hyperedges);
        // This will update the HashMap, so that the oldNode is destroyed.
        addNewNode(newNode, noPrune);
      } else {// merge new to old, does not trigger pruningItems
        oldNode.addHyperedgesInNode(newNode.hyperedges);
      }

    } else { // first time item
      this.chart.nAdded++; // however, this item may not be used in the future due to pruning in
      // the hyper-graph
      addNewNode(newNode, noPrune);
    }

    return newNode;
  }

  List<HGNode> getSortedNodes() {
    ensureSorted();
    return this.sortedNodes;
  }

  Map<Integer, SuperNode> getSortedSuperItems() {
    ensureSorted();
    return this.superNodesTbl;
  }

  // ===============================================================
  // Private Methods
  // ===============================================================

  /**
   * two cases this function gets called (1) a new hyperedge leads to a non-existing node signature
   * (2) a new hyperedge's signature matches an old node's signature, but the best-logp of old node
   * is worse than the new hyperedge's logP
   * */
  private void addNewNode(HGNode node, boolean noPrune) {
    this.nodesSigTbl.put(node.signature(), node); // add/replace the item
    this.sortedNodes = null; // reset the list

    // since this.sortedItems == null, this is not necessary because we will always call
    // ensure_sorted to reconstruct the this.tableSuperItems
    // add a super-items if necessary
    SuperNode si = this.superNodesTbl.get(node.lhs);
    if (null == si) {
      si = new SuperNode(node.lhs);
      this.superNodesTbl.put(node.lhs, si);
    }
    si.nodes.add(node);// TODO what about the dead items?
  }

  /**
   * get a sorted list of Nodes in the cell, and also make sure the list of node in any SuperItem is
   * sorted, this will be called only necessary, which means that the list is not always sorted,
   * mainly needed for goal_bin and cube-pruning
   */
  private void ensureSorted() {
    if (null == this.sortedNodes) {
      // Get sortedNodes.
      HGNode[] nodesArray = new HGNode[this.nodesSigTbl.size()];
      int i = 0;
      for (HGNode node : this.nodesSigTbl.values())
        nodesArray[i++] = node;

      /**
       * sort the node in an decreasing-LogP order
       * */
      Arrays.sort(nodesArray, HGNode.inverseLogPComparator);

      this.sortedNodes = new ArrayList<HGNode>();
      for (HGNode node : nodesArray) {
        this.sortedNodes.add(node);
      }

      // TODO: we cannot create new SuperItem here because the DotItem link to them

      // Update superNodesTbl
      List<SuperNode> tem_list = new ArrayList<SuperNode>(this.superNodesTbl.values());
      for (SuperNode t_si : tem_list) {
        t_si.nodes.clear();
      }

      for (HGNode it : this.sortedNodes) {
        SuperNode si = this.superNodesTbl.get(it.lhs);
        if (null == si) { // sanity check
          throw new RuntimeException("Does not have super Item, have to exist");
        }
        si.nodes.add(it);
      }

      // Remove SuperNodes who may not contain any node any more due to pruning
      List<Integer> toRemove = new ArrayList<Integer>();
      for (Integer k : this.superNodesTbl.keySet()) {
        if (this.superNodesTbl.get(k).nodes.size() <= 0) {
          // note that: we cannot directly do the remove, because it will throw
          // ConcurrentModificationException
          toRemove.add(k);
          // System.out.println("have zero items in superitem " + k);
          // this.tableSuperItems.remove(k);
        }
      }
      for (Integer t : toRemove) {
        this.superNodesTbl.remove(t);
      }
    }
  }
}
