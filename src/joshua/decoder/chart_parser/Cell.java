/* This file is part of the Joshua Machine Translation System.
 *
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
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
import joshua.decoder.Support;
import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FFTransitionResult;
import joshua.decoder.ff.FeatureFunction;

import joshua.decoder.ff.tm.Rule;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
// Private instance fields (maybe could be protected instead)
//===============================================================
	
	/** The cost of the best item in the bin */
	private double bestItemCost = IMPOSSIBLE_COST;
	
	/** cutoff = bestItemCost + relative_threshold */
	private double cutoffCost = IMPOSSIBLE_COST;
	
	// num of corrupted items in this.heapItems, note that the
	// item in this.tableItems is always good
	private int qtyDeadItems = 0;
	
	private Chart chart = null;
	
	private int goalSymID;
	
	/* we need always maintain the priority queue (worst first),
	 * so that we can do prunning efficiently. On the other
	 * hand, we need the this.sortedItems only when necessary
	 */
	
	/* NOTE: MIN-HEAP, we put the worst-cost item at the top
	 * of the heap by manipulating the compare function
	 * this.heapItems: the only purpose is to help deecide which
	 * items should be removed from this.tableItems during
	 * pruning
	 */
	// TODO: initial capacity?
	private PriorityQueue<HGNode> heapItems =
		new PriorityQueue<HGNode>(1, HGNode.negtiveCostComparator);
	
	// to maintain uniqueness of items
	private HashMap<String,HGNode> tableItems =
		new HashMap<String,HGNode>();
	
	// signature by lhs
	private Map<Integer,SuperItem> tableSuperItems =
		new HashMap<Integer,SuperItem>();
	
	// sort values in tbl_item_signature, we need this list
	// whenever necessary
	private ArrayList<HGNode> sortedItems = null;
	
	
//===============================================================
// Static fields
//===============================================================
	
	private static final double EPSILON = 0.000001;
	private static final int IMPOSSIBLE_COST = 99999;
	
	private static final Logger logger = Logger.getLogger(Cell.class.getName());
	
	
//===============================================================
// Constructor
//===============================================================
	
	// TODO: This should be a non-static inner class of Chart. That would give us implicit access to all the arguments of this constructor (which are the same at all call sites)
	
	public Cell(Chart chart, int goalSymID) {
		this.chart     = chart;
		this.goalSymID = goalSymID;
	}
	
	
//===============================================================
// Package-protected methods
//===============================================================
	
	/** 
	 * Compute cost and the states of this item returned
	 * ArrayList: expectedTotalCost, finalizedTotalCost,
	 * transition_cost, bonus, list of states
	 */
	ComputeItemResult computeItem(
		Rule rule, ArrayList<HGNode> previousItems, int i, int j, SourcePath srcPath
	) {
		long startTime = Support.current_time(); // It's a lie, always == 0
		this.chart.n_called_compute_item++;
		
		double finalizedTotalCost = 0.0;
		
		//// See bug note in FeatureFunction about List vs ArrayList
		
		if (null != previousItems) {
			for (HGNode item : previousItems) {
				finalizedTotalCost += item.best_hyperedge.best_cost;
			}
		}
		
		HashMap<Integer,FFDPState> allItemStates = null;
		double transitionCostSum    = 0.0;
		double futureCostEstimation = 0.0;
		
		for (FeatureFunction ff : this.chart.featureFunctions) {
			////long start2 = Support.current_time();
			if (ff.isStateful()) {
				//System.out.println("class name is " + ff.getClass().getName());
				FFTransitionResult state = HyperGraph.computeTransition(
					null, rule, previousItems, ff, i, j, srcPath);
				
				transitionCostSum +=
					ff.getWeight() * state.getTransitionCost();
				
				futureCostEstimation +=
					ff.getWeight() * state.getFutureCostEstimation();
				
				FFDPState itemState = state.getStateForItem();
				if (null != itemState) {
					if (null == allItemStates) {
						allItemStates = new HashMap<Integer,FFDPState>();
					}
					allItemStates.put(ff.getFeatureID(), itemState);
				} else {
					throw new RuntimeException("compute_item: null getStateForItem()"
						+ "\n*"
						+ "\n* This will lead insidiously to a crash in"
						+ "\n* HyperGraph$Item.get_signature() since noone"
						+ "\n* checks invariant conditions before then."
						+ "\n*"
						+ "\n* Good luck tracking it down\n");
				}
			} else {
				FFTransitionResult state = HyperGraph.computeTransition(
					null, rule, previousItems, ff, i, j, srcPath);
				
				transitionCostSum +=
					ff.getWeight() * state.getTransitionCost();
				
				futureCostEstimation += 0.0;
			}
			////ff.time_consumed += Support.current_time() - start2;
		}
		
		/* if we use this one (instead of compute transition
		 * cost on the fly, we will rely on the correctness
		 * of rule.statelesscost. This will cause a nasty
		 * bug for MERT. specifically, even we change the
		 * weight vector for features along the iteration,
		 * the HG cost does not reflect that as the Grammar
		 * is not reestimated!!! Of course, compute it on
		 * the fly will slow down the decoding (e.g., from
		 * 5 seconds to 6 seconds, for the example test
		 * set)
		 */
		//transitionCostSum += rule.getStatelessCost();
		
		finalizedTotalCost += transitionCostSum;
		double expectedTotalCost = finalizedTotalCost + futureCostEstimation;
		
		ComputeItemResult result = new ComputeItemResult();
		result.setExpectedTotalCost(expectedTotalCost);
		result.setFinalizedTotalCost(finalizedTotalCost);
		result.setTransitionTotalCost(transitionCostSum);
		result.setFeatDPStates(allItemStates);
		
		this.chart.g_time_compute_item += Support.current_time() - startTime;
		
		return result;
	}
	
	
	/** 
	 * add all the items with GOAL_SYM state into the goal bin
	 * the goal bin has only one Item, which itself has many
	 * deductions only "goal bin" should call this function
	 */
	void transitToGoal(Cell bin) { // the bin[0][n], this is not goal bin
		this.sortedItems = new ArrayList<HGNode>();
		HGNode goalItem = null;
		
		for (HGNode item : bin.getSortedItems()) {
			if (item.lhs == this.goalSymID) {
				double cost = item.best_hyperedge.best_cost;
				double finalTransitionCost = 0.0;
				
				for (FeatureFunction ff : this.chart.featureFunctions) {
					finalTransitionCost +=
						ff.getWeight()
						* ff.finalTransition(item.getFeatDPState(ff));
				}
				
				ArrayList<HGNode> previousItems = new ArrayList<HGNode>();
				previousItems.add(item);
				
				HyperEdge dt = new HyperEdge(
					null, cost + finalTransitionCost, finalTransitionCost, previousItems, null);
				
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(String.format(
						"Goal item, total_cost: %.3f; ant_cost: %.3f; final_tran: %.3f; ",
						cost + finalTransitionCost, cost, finalTransitionCost));
				}
				
				if (null == goalItem) {
					// FIXME: this is the only place Chart.sentenceLength is accessed outside of Chart. Maybe it should be an argument to this method? This is also the only method where we use goalSymID
					goalItem = new HGNode(
						0, this.chart.sentenceLength + 1, this.goalSymID, null, dt, cost + finalTransitionCost);
					this.sortedItems.add(goalItem);
				} else {
					goalItem.addHyperedgeInItem(dt);
					if (goalItem.best_hyperedge.best_cost > dt.best_cost) {
						goalItem.best_hyperedge = dt;
					}
				}
			} // End if item.lhs == this.goalSymID
		} // End foreach Item in bin.get_sorted_items()
		
		
		if (logger.isLoggable(Level.INFO)) {
			// BUG: what happened to make this necessary? This happens for the ./example2 decoder run (but not for ./example). Whatever it was, it happened in r878
			if (null == goalItem) {
				logger.severe("goalItem is null (this will cause the RuntimeException below)");
			} else {
				logger.info(String.format("Goal item, best cost is %.3f",
					goalItem.best_hyperedge.best_cost));
			}
		}
		ensureSorted();
		
		int itemsInGoalBin = getSortedItems().size();
		if (1 != itemsInGoalBin) {
			throw new RuntimeException("the goal_bin does not have exactly one item");
		}
	}
	
	
	/** axiom is for the zero-arity rules */
	void addAxiom(int i, int j, Rule rule, SourcePath srcPath) {
		addHyperEdgeInCell(
			computeItem(rule, null, i, j, srcPath),
			rule, i, j, null, srcPath);
	}
	
	
	/**
	 * Add complete Items in Chart, 
	 * pruning inside this function.
	 * 
	 * @param i
	 * @param j
	 * @param superItems List of language model items
	 * @param rules
	 * @param arity Number of nonterminals
	 * @param srcPath
	 */
	void completeCell(
		int i, int j, ArrayList<SuperItem> superItems,
		List<Rule> rules, int arity, SourcePath srcPath
	) {
		//System.out.println(String.format("Complet_cell is called, n_rules: %d ", rules.size()));
		// consider all the possbile combinations (while
		// in Cube-pruning, we do not consider all the
		// possible combinations)
		for (Rule rule : rules) {
			if (1 == arity) {
				SuperItem super_ant1 = superItems.get(0);
				for (HGNode antecedent: super_ant1.l_items) {
					ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
					antecedents.add(antecedent);
					addHyperEdgeInCell(
						computeItem(rule, antecedents, i, j, srcPath),
						rule, i, j, antecedents, srcPath);
				}
				
			} else if (arity == 2) {
				SuperItem super_ant1 = superItems.get(0);
				SuperItem super_ant2 = superItems.get(1);
				for (HGNode it_ant1: super_ant1.l_items) {
					for (HGNode it_ant2: super_ant2.l_items) {
						ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
						antecedents.add(it_ant1);
						antecedents.add(it_ant2);
						addHyperEdgeInCell(
							computeItem(rule, antecedents, i, j, srcPath),
							rule, i, j, antecedents, srcPath);
					}
				}
			} else {
				// BUG: We should fix this, as per the suggested implementation over email.
				throw new RuntimeException("Sorry, we can only deal with rules with at most TWO non-terminals");
			}
		}
	}
	
	
	/** Add complete Items in Chart pruning inside this function */
	// TODO: our implementation do the prunining for each DotItem
	//       under each grammar, not aggregated as in the python
	//       version
	// TODO: the implementation is little bit different from
	//       the description in Liang'2007 ACL paper
	void completeCellWithCubePrune(
		int i, int j, ArrayList<SuperItem> superItems,
		List<Rule> rules, SourcePath srcPath
	) { // combinations: rules, antecent items
		// in the paper, heap_cands is called cand[v]
		PriorityQueue<CubePruneState> combinationHeap =	new PriorityQueue<CubePruneState>();
		
		// rememeber which state has been explored
		HashMap<String,Integer> cube_state_tbl = new HashMap<String,Integer>();
		
		if (null == rules || rules.size() <= 0) {
			return;
		}
		
		//== seed the heap with best item
		Rule currentRule = rules.get(0);
		ArrayList<HGNode> currentAntecedents = new ArrayList<HGNode>();
		for (SuperItem si : superItems) {
			// TODO: si.l_items must be sorted
			currentAntecedents.add(si.l_items.get(0));
		}
		ComputeItemResult result =	computeItem(currentRule, currentAntecedents, i, j, srcPath);
		
		int[] ranks = new int[1+superItems.size()]; // rule, ant items
		for (int d = 0; d < ranks.length; d++) {
			ranks[d] = 1;
		}
		
		CubePruneState best_state =	new CubePruneState(result, ranks, currentRule, currentAntecedents);
		combinationHeap.add(best_state);
		cube_state_tbl.put(best_state.get_signature(),1);
		// cube_state_tbl.put(best_state,1);
		
		// extend the heap
		Rule   oldRule = null;
		HGNode oldItem = null;
		int    tem_c   = 0;
		while (combinationHeap.size() > 0) {
			
			//========== decide if the top in the heap should be pruned
			tem_c++;
			CubePruneState cur_state = combinationHeap.poll();
			currentRule = cur_state.rule;
			currentAntecedents = new ArrayList<HGNode>(cur_state.l_ants); // critical to create a new list
			//cube_state_tbl.remove(cur_state.get_signature()); // TODO, repeat
			addHyperEdgeInCell(cur_state.tbl_item_states, cur_state.rule, i, j,cur_state.l_ants, srcPath); // pre-pruning inside this function
			
			//if the best state is pruned, then all the remaining states should be pruned away
			if (cur_state.tbl_item_states.getExpectedTotalCost() > this.cutoffCost + JoshuaConfiguration.fuzz1) {
				//n_prepruned += heap_cands.size();
				this.chart.n_prepruned_fuzz1 += combinationHeap.size();
				break;
			}
			
			//========== extend the cur_state, and add the candidates into the heap
			for (int k = 0; k < cur_state.ranks.length; k++) {
				
				//GET new_ranks
				int[] new_ranks = new int[cur_state.ranks.length];
				for (int d = 0; d < cur_state.ranks.length; d++) {
					new_ranks[d] = cur_state.ranks[d];
				}
				new_ranks[k] = cur_state.ranks[k] + 1;
				
				String new_sig = CubePruneState.get_signature(new_ranks);
				
				if (cube_state_tbl.containsKey(new_sig) // explored before
				|| (k == 0 && new_ranks[k] > rules.size())
				|| (k != 0 && new_ranks[k] > superItems.get(k-1).l_items.size())
				) {
					continue;
				}
				
				if (k == 0) { // slide rule
					oldRule = currentRule;
					currentRule = rules.get(new_ranks[k]-1);
				} else { // slide ant
					oldItem = currentAntecedents.get(k-1); // conside k == 0 is rule
					currentAntecedents.set(k-1,
						superItems.get(k-1).l_items.get(new_ranks[k]-1));
				}
				
				CubePruneState t_state = new CubePruneState(
					computeItem(currentRule, currentAntecedents, i, j, srcPath),
					new_ranks, currentRule, currentAntecedents);
				
				// add state into heap
				cube_state_tbl.put(new_sig,1);				
				if (result.getExpectedTotalCost() < this.cutoffCost + JoshuaConfiguration.fuzz2) {
					combinationHeap.add(t_state);
				} else {
					//n_prepruned += 1;
					this.chart.n_prepruned_fuzz2 += 1;
				}
				
				// recover
				if (k == 0) { // rule
					currentRule = oldRule;
				} else { // ant
					currentAntecedents.set(k-1, oldItem);
				}
			}
		}
	}
	
	/**in order to add a hyperedge into the chart, we need to
	 * (1) do the combination, and compute the cost (if pass the cube-prunning)
	 * (2) run through the beam and threshold pruning, which itself has two steps
	 * */
	
	/**create a hyperege, and add it into the chart if not got prunned
	 * */
	HGNode addHyperEdgeInCell(
		ComputeItemResult result, Rule rule, int i, int j,
		ArrayList<HGNode> ants, SourcePath srcPath
	) {
		long start = Support.current_time();
		HGNode res = null;
		HashMap<Integer,FFDPState> item_state_tbl = result.getFeatDPStates();
		double expectedTotalCost  = result.getExpectedTotalCost(); // including outside estimation
		double transition_cost    = result.getTransitionTotalCost();
		double finalizedTotalCost = result.getFinalizedTotalCost();
		
		//double bonus = tbl_states.get(BONUS); // not used
		if ( ! shouldPruneEdge(expectedTotalCost) ) {
			HyperEdge dt = new HyperEdge(rule, finalizedTotalCost, transition_cost, ants, srcPath);
			HGNode item = new HGNode(i, j, rule.getLHS(), item_state_tbl, dt, expectedTotalCost);
			
			addHyperedge(item);
			
			if (logger.isLoggable(Level.FINEST)) 
				logger.finest(String.format("add an deduction with arity %d", rule.getArity()));
			
			res = item;
		} else {
			this.chart.n_prepruned++;
//			if (logger.isLoggable(Level.INFO)) logger.finest(String.format("Prepruned an deduction with arity %d", rule.getArity()));
			res = null;
		}
		this.chart.g_time_add_deduction += Support.current_time() - start;
		return res;
	}
	
	
	ArrayList<HGNode> getSortedItems() {
		ensureSorted();
		return this.sortedItems;
	}
	
	
	Map<Integer,SuperItem> getSortedSuperItems() {
		ensureSorted();
		return this.tableSuperItems;
	}
	
	
//===============================================================
// CubePruneState class
//===============================================================
	private static class CubePruneState implements Comparable<CubePruneState> {
		int[]             ranks;
		ComputeItemResult tbl_item_states;
		Rule              rule;
		ArrayList<HGNode> l_ants;
		
		public CubePruneState(ComputeItemResult state, int[] ranks, Rule rule, 
				ArrayList<HGNode> antecedents)
		{
			this.tbl_item_states = state;
			this.ranks           = ranks;
			this.rule            = rule;
			// create a new vector is critical, because
			// currentAntecedents will change later
			this.l_ants = new ArrayList<HGNode>(antecedents);
		}
		
		
		private static String get_signature(int[] ranks2) {
			StringBuffer sb = new StringBuffer();
			if (null != ranks2) {
				for (int i = 0; i < ranks2.length; i++) {
					sb.append(' ').append(ranks2[i]);
				}
			}
			return sb.toString();
		}
		
		
		private String get_signature() {
			return get_signature(ranks);
		}
		
		
		/**
		 * Compares states by expected cost, allowing states
		 * to be sorted according to their natural order.
		 * 
		 * @param that State to which this state will be compared
		 * @return -1 if this state's expected cost is less
		 *            than that stat's expected cost,
		 *         0  if this state's expected cost is equal
		 *            to that stat's expected cost,
		 *         +1 if this state's expected cost is
		 *            greater than that stat's expected cost
		 */
		public int compareTo(CubePruneState that) {
			if (this.tbl_item_states.getExpectedTotalCost() < that.tbl_item_states.getExpectedTotalCost()) {
				return -1;
			} else if (this.tbl_item_states.getExpectedTotalCost() == that.tbl_item_states.getExpectedTotalCost()) {
				return 0;
			} else {
				return 1;
			}
		}
	}
	
	
//===============================================================
// Private Methods
//===============================================================

	/** each item has a list of hyperedges,
	 * need to check whether the item is already exist, 
	 * if yes, just add the hyperedges, this will change the best cost of the item 
	 * */
	private boolean addHyperedge(HGNode newItem) {
		boolean res = false;
		HGNode oldItem = this.tableItems.get(newItem.getSignature());
		if (null != oldItem) { // have an item with same states, combine items
			this.chart.n_merged++;
			if (newItem.est_total_cost < oldItem.est_total_cost) {
				// the position of oldItem in the this.heapItems
				// may change, basically, we should remove the
				// oldItem, and re-insert it (linear time,
				// this is too expense)
				oldItem.is_dead = true; // this.heapItems.remove(oldItem);
				this.qtyDeadItems++;
				newItem.addHyperedgesInItem(oldItem.l_hyperedges);
				addNewItem(newItem); // this will update the HashMap, so that the oldItem is destroyed
				res = true;
			} else {
				oldItem.addHyperedgesInItem(newItem.l_hyperedges);
			}
		} else { // first time item
			this.chart.n_added++; // however, this item may not be used in the future due to pruning in the hyper-graph
			addNewItem(newItem);
			res = true;
		}
		this.cutoffCost = Support.find_min(
			this.bestItemCost + JoshuaConfiguration.relative_threshold,
			IMPOSSIBLE_COST);
		pruningItems();
		return res;
	}
	
	
	/**this function is called only 
	 * there is no such item in the tbl*/
	private void addNewItem(HGNode item) {
		this.tableItems.put(item.getSignature(), item); // add/replace the item
		this.sortedItems = null; // reset the list
		this.heapItems.add(item);
		
		//since this.sortedItems == null, this is not necessary because we will always call ensure_sorted to reconstruct the this.tableSuperItems
		//add a super-items if necessary
		SuperItem si = this.tableSuperItems.get(item.lhs);
		if (null == si) {
			si = new SuperItem(item.lhs);
			this.tableSuperItems.put(item.lhs, si);
		}
		si.l_items.add(item);
		
		if (item.est_total_cost < this.bestItemCost) {
			this.bestItemCost = item.est_total_cost;
		}
	}
	
	private boolean shouldPruneEdge(double total_cost) {
		return (total_cost >= this.cutoffCost);
	}
	
	
	private void pruningItems() {
		if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("Pruning: heap size: %d; n_dead_items: %d", this.heapItems.size(),this.qtyDeadItems));
		if (this.heapItems.size() == this.qtyDeadItems) { // TODO:clear the heap, and reset this.qtyDeadItems??
			this.heapItems.clear();
			this.qtyDeadItems = 0;
			return;
		}
		while (this.heapItems.size() - this.qtyDeadItems > JoshuaConfiguration.max_n_items //bin limit pruning
		|| this.heapItems.peek().est_total_cost >= this.cutoffCost) { // relative threshold pruning
			HGNode worstItem = this.heapItems.poll();
			if (worstItem.is_dead) { // clear the corrupted item
				this.qtyDeadItems--;
			} else {
				this.tableItems.remove(worstItem.getSignature()); // always make this.tableItems current
				this.chart.n_pruned++;
//				if (logger.isLoggable(Level.INFO)) logger.info(String.format("Run_pruning: %d; cutoff=%.3f, realcost: %.3f",this.chart.n_pruned,this.cutoffCost,worstItem.est_total_cost));
			}
		}
		if (this.heapItems.size() - this.qtyDeadItems == JoshuaConfiguration.max_n_items) { // TODO:??
			this.cutoffCost = Support.find_min(
				this.cutoffCost,
				this.heapItems.peek().est_total_cost + EPSILON);
		}
	}
	
	
	/** get a sorted list of Items in the bin, and also make
	 * sure the list of items in any SuperItem is sorted, this
	 * will be called only necessary, which means that the list
	 * is not always sorted mainly needed for goal_bin and
	 * cube-pruning
	 */
	private void ensureSorted() {
		
		if (null == this.sortedItems) {
			//get a sorted items ArrayList
			Object[] t_col = this.tableItems.values().toArray();
			Arrays.sort(t_col);
			this.sortedItems = new ArrayList<HGNode>();
			for (int c = 0; c < t_col.length;c++) {
				this.sortedItems.add((HGNode)t_col[c]);
			}
			//TODO: we cannot create new SuperItem here because the DotItem link to them
			
			//update this.tableSuperItems
			ArrayList<SuperItem> tem_list =
				new ArrayList<SuperItem>(this.tableSuperItems.values());
			for (SuperItem t_si : tem_list) {
				t_si.l_items.clear();
			}
			
			for (HGNode it : this.sortedItems) {
				SuperItem si = this.tableSuperItems.get(it.lhs);
				if (null == si) { // sanity check
					throw new RuntimeException("Does not have super Item, have to exist");
				}
				si.l_items.add(it);
			}
			
			ArrayList<Integer> to_remove = new ArrayList<Integer>();
			//note: some SuperItem may not contain any items any more due to pruning
			for (Integer k : this.tableSuperItems.keySet()) {
				if (this.tableSuperItems.get(k).l_items.size() <= 0) {
					to_remove.add(k); // note that: we cannot directly do the remove, because it will throw ConcurrentModificationException
					//System.out.println("have zero items in superitem " + k);
					//this.tableSuperItems.remove(k);
				}
			}
			for (Integer t : to_remove) {
				this.tableSuperItems.remove(t);
			}
		}
	}
	
	
	
//===============================================================
// Package-protected ComputeItemResult class
//===============================================================
	static class ComputeItemResult {
		private double expectedTotalCost;
		private double finalizedTotalCost;
		private double transitionTotalCost;
		// the key is feature id; tbl of dpstate for each stateful feature
		private HashMap<Integer,FFDPState> tbl_feat_dpstates;
		
		private void setExpectedTotalCost(double cost) {
			this.expectedTotalCost = cost;
		}
		
		private double getExpectedTotalCost() {
			return this.expectedTotalCost;
		}
		
		private void setFinalizedTotalCost(double cost) {
			this.finalizedTotalCost = cost;
		}
		
		private double getFinalizedTotalCost() {
			return this.finalizedTotalCost;
		}
		
		private void setTransitionTotalCost(double cost) {
			this.transitionTotalCost = cost;
		}
		
		private double getTransitionTotalCost() {
			return this.transitionTotalCost;
		}
		
		private void setFeatDPStates(HashMap<Integer,FFDPState> states) {
			this.tbl_feat_dpstates = states;
		}
		
		private HashMap<Integer,FFDPState> getFeatDPStates() {
			return this.tbl_feat_dpstates;
		}
	}
}
