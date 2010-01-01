/* This file is part of the Joshua Machine Translation System.
 *
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1n_pruned
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder.chart_parser;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.DPState;
import joshua.decoder.ff.FeatureFunction;


import joshua.decoder.ff.tm.Rule;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * this class implement functions:
 * (1) combine small itesm into larger ones using rules, and create
 *     items and hyper-edges to construct a hyper-graph,
 * (2) evaluate model cost for items,
 * (3) cube-pruning
 * Note: Bin creates Items, but not all Items will be used in the
 * hyper-graph
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
class Cell {
	
//===============================================================
// Private instance fields
//===============================================================	
	private Chart chart = null;
	
	public BeamPruner<HGNode> beamPruner;//TODO: CubePruneCombiner access this
	
	private int goalSymID;
	
		
	// to maintain uniqueness of nodes
	private HashMap<String,HGNode> nodesSigTbl = new HashMap<String,HGNode>();
	
	// signature by lhs
	private Map<Integer,SuperNode> superNodesTbl = new HashMap<Integer,SuperNode>();
	
	/** sort values in nodesSigTbl, 
	 * we need this list when necessary
	 */
	private List<HGNode> sortedNodes = null;
	
	
	
//===============================================================
// Static fields
//===============================================================
	private static final Logger logger = Logger.getLogger(Cell.class.getName());
	
	
//===============================================================
// Constructor
//===============================================================
	
	public Cell(Chart chart, int goalSymID) {
		this.chart     = chart;
		this.goalSymID = goalSymID;
		
		if(JoshuaConfiguration.useBeamAndThresholdPrune){
			PriorityQueue<HGNode> nodesHeap =new PriorityQueue<HGNode>(1, HGNode.negtiveCostComparator);		
			beamPruner = new BeamPruner<HGNode>(nodesHeap, JoshuaConfiguration.relative_threshold, JoshuaConfiguration.max_n_items);
		}
	}
	
	
//===============================================================
// Package-protected methods
//===============================================================
	
	
	/** 
	 * add all the items with GOAL_SYM state into the goal bin
	 * the goal bin has only one Item, which itself has many
	 * hyperedges only "goal bin" should call this function
	 */
	//note that thei nput bin is  bin[0][n], not the goal bin
	void transitToGoal(Cell bin, List<FeatureFunction> featureFunctions, int sentenceLength) { 
		this.sortedNodes = new ArrayList<HGNode>();
		HGNode goalItem = null;
		
		for (HGNode antNode : bin.getSortedNodes()) {
			if (antNode.lhs == this.goalSymID) {
				double cost = antNode.bestHyperedge.bestDerivationCost;
				double finalTransitionCost = 0.0;
				List<HGNode> antNodes = new ArrayList<HGNode>();
				antNodes.add(antNode);
				double[] modelCosts = ComputeNodeResult.computeModelTransitionCost(
						featureFunctions, null, antNodes, 0, sentenceLength, null);
								
				int count=0;
				for (FeatureFunction ff : featureFunctions) {
					finalTransitionCost += ff.getWeight() * modelCosts[count];
					count++;
				}
				
				ArrayList<HGNode> previousItems = new ArrayList<HGNode>();
				previousItems.add(antNode);
				
				HyperEdge dt = new HyperEdge(null, cost + finalTransitionCost, finalTransitionCost, previousItems, null);
				
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(String.format(
						"Goal item, total_cost: %.3f; ant_cost: %.3f; final_tran: %.3f; ",
						cost + finalTransitionCost, cost, finalTransitionCost));
				}
				
				if (null == goalItem) {
					goalItem = new HGNode(0, sentenceLength + 1, this.goalSymID, null, dt, cost + finalTransitionCost);
					this.sortedNodes.add(goalItem);
				} else {
					goalItem.addHyperedgeInItem(dt);
					if (goalItem.bestHyperedge.bestDerivationCost > dt.bestDerivationCost) {
						goalItem.bestHyperedge = dt;
					}
				}
			} // End if item.lhs == this.goalSymID
		} // End foreach Item in bin.get_sorted_items()
		
		
		if (logger.isLoggable(Level.INFO)) {
			if (null == goalItem) {
				logger.severe("goalItem is null (this will cause the RuntimeException below)");
			} else {
				logger.info(String.format("Goal item, best cost is %.3f",
					goalItem.bestHyperedge.bestDerivationCost));
			}
		}
		ensureSorted();
		
		int itemsInGoalBin = getSortedNodes().size();
		if (1 != itemsInGoalBin) {
			throw new RuntimeException("the goal_bin does not have exactly one item");
		}
	}
	
	
	
	/**in order to add a hyperedge into the chart, we need to
	 * (1) do the combination, and compute the cost (if pass the cube-prunning filter)
	 * (2) run through the beam and threshold pruning, which itself has two steps.
	 * */
	
	/**a note about pruning:
	 * when a hyperedge gets created, it first needs to pass through shouldPruneEdge filter.
	 * Then, if it does not trigger a new node (i.e. will be merged to an old node), then does not trigger pruningNodes.
	 * If it does trigger a new node (either because its signature is new or because its cost is better than the old node's cost), 
	 * then it will trigger pruningNodes, which might causes *other* nodes got pruned as well
	 * */
	
	
	/**create a hyperege, and add it into the chart if not got prunned
	 * */
	HGNode addHyperEdgeInCell(
		ComputeNodeResult result, Rule rule, int i, int j,
		List<HGNode> ants, SourcePath srcPath
	) {
		HGNode res = null;
		
		HashMap<Integer,DPState> dpStates = result.getDPStates();
		double expectedTotalCost  = result.getExpectedTotalCost(); // including outside estimation
		double transitionCost    = result.getTransitionTotalCost();
		double finalizedTotalCost = result.getFinalizedTotalCost();
	
		
		if(beamPruner!=null &&  beamPruner.relativeThresholdPrune(expectedTotalCost)){//the hyperedge should be pruned
			this.chart.nPreprunedEdges++;
			res = null;
		}else{
			HyperEdge dt = new HyperEdge(rule, finalizedTotalCost, transitionCost, ants, srcPath);
			res = new HGNode(i, j, rule.getLHS(), dpStates, dt, expectedTotalCost);
			
			/** each node has a list of hyperedges,
			 * need to check whether the node is already exist, 
			 * if yes, just add the hyperedges, this may change the best cost of the node 
			 * */
			HGNode oldNode = this.nodesSigTbl.get( res.getSignature() );
			if (null != oldNode) { // have an item with same states, combine items
				this.chart.nMerged++;
				
				/** the position of oldItem in this.heapItems
				 *  may change, basically, we should remove the
				 *  oldItem, and re-insert it (linear time), this is too expense)
				 **/
				if (res.estTotalCost < oldNode.estTotalCost) {//merget old to new					

					if(beamPruner!=null){
						oldNode.setDead();// this.heapItems.remove(oldItem);
						beamPruner.incrementDeadObjs();
					}
					
					res.addHyperedgesInNode(oldNode.hyperedges);
					addNewNode(res); //this will update the HashMap, so that the oldNode is destroyed
					
				} else {//merge new to old, does not trigger pruningItems
					oldNode.addHyperedgesInNode(res.hyperedges);
				}
				
			} else { // first time item
				this.chart.nAdded++; // however, this item may not be used in the future due to pruning in the hyper-graph
				addNewNode(res);
			}
		}
		return res;
	}
	
	
	List<HGNode> getSortedNodes() {
		ensureSorted();
		return this.sortedNodes;
	}
	
	
	Map<Integer,SuperNode> getSortedSuperItems() {
		ensureSorted();
		return this.superNodesTbl;
	}
	
	 
	
//===============================================================
// Private Methods
//===============================================================

	/**two cases this function gets called
	 * (1) a new hyperedge leads to a non-existing node signature
	 * (2) a new hyperedge's signature matches an old node's signature, but the best-cost of old node is worse than the new hyperedge's cost
	 * */
	private void addNewNode(HGNode node) {
		this.nodesSigTbl.put(node.getSignature(), node); // add/replace the item
		this.sortedNodes = null; // reset the list
			
		//update bestItemCost and cutoffCost
		if(beamPruner!=null){
			List<HGNode> prunedNodes = beamPruner.addOneObjWithPrune(node);
			this.chart.nPrunedItems += prunedNodes.size();
			for(HGNode prunedNode : prunedNodes)
				nodesSigTbl.remove(prunedNode.getSignature());
		}	
		
		//since this.sortedItems == null, this is not necessary because we will always call ensure_sorted to reconstruct the this.tableSuperItems
		//add a super-items if necessary
		SuperNode si = this.superNodesTbl.get(node.lhs);
		if (null == si) {
			si = new SuperNode(node.lhs);
			this.superNodesTbl.put(node.lhs, si);
		}
		si.nodes.add(node);//TODO what about the dead items?
		
	
	}

	
	
	/** get a sorted list of Nodes in the cell, and also make
	 * sure the list of node in any SuperItem is sorted, this
	 * will be called only necessary, which means that the list
	 * is not always sorted, mainly needed for goal_bin and
	 * cube-pruning
	 */
	private void ensureSorted() {
		
		if (null == this.sortedNodes) {
			//== get sortedNodes
			Object[] tCollection = this.nodesSigTbl.values().toArray();
			
			Arrays.sort(tCollection);
			
			this.sortedNodes = new ArrayList<HGNode>();
			for (int c = 0; c < tCollection.length;c++) {
				this.sortedNodes.add((HGNode)tCollection[c]);
			}
			//TODO: we cannot create new SuperItem here because the DotItem link to them
			
			//== update superNodesTbl
			ArrayList<SuperNode> tem_list =
				new ArrayList<SuperNode>(this.superNodesTbl.values());
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
			
			//== remove SuperNodes who may not contain any node any more due to pruning
			ArrayList<Integer> to_remove = new ArrayList<Integer>();
			for (Integer k : this.superNodesTbl.keySet()) {
				if (this.superNodesTbl.get(k).nodes.size() <= 0) {
					 // note that: we cannot directly do the remove, because it will throw ConcurrentModificationException
					to_remove.add(k);
					//System.out.println("have zero items in superitem " + k);
					//this.tableSuperItems.remove(k);
				}
			}
			for (Integer t : to_remove) {
				this.superNodesTbl.remove(t);
			}
		}
	}
}
