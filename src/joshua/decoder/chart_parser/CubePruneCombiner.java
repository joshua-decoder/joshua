package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class CubePruneCombiner implements Combiner{

	
	/** Add complete Items in Chart pruning inside this function */
	// TODO: our implementation do the prunining for each DotItem
	//       under each grammar, not aggregated as in the python
	//       version
	// TODO: the implementation is little bit different from
	//       the description in Liang'2007 ACL paper
	public void combine(Chart chart, Cell cell, int i, int j, ArrayList<SuperItem> superItems, List<Rule> rules, int arity, SourcePath srcPath) {
//		 combinations: rules, antecent items
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
		ComputeNodeResult result =	new ComputeNodeResult(chart.featureFunctions, currentRule, currentAntecedents, i, j, srcPath);
		
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
			cell.addHyperEdgeInCell(cur_state.tbl_item_states, cur_state.rule, i, j,cur_state.l_ants, srcPath); // pre-pruning inside this function
			
			//if the best state is pruned, then all the remaining states should be pruned away
			if (cur_state.tbl_item_states.getExpectedTotalCost() > cell.getCutCost() + JoshuaConfiguration.fuzz1) {
				//n_prepruned += heap_cands.size();
				chart.n_prepruned_fuzz1 += combinationHeap.size();
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
						new ComputeNodeResult(chart.featureFunctions, currentRule, currentAntecedents, i, j, srcPath),
					new_ranks, currentRule, currentAntecedents);
				
				// add state into heap
				cube_state_tbl.put(new_sig,1);				
				if (result.getExpectedTotalCost() < cell.getCutCost() + JoshuaConfiguration.fuzz2) {
					combinationHeap.add(t_state);
				} else {
					//n_prepruned += 1;
					chart.n_prepruned_fuzz2 += 1;
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
	
	

	
//	===============================================================
//	 CubePruneState class
//	===============================================================
		private static class CubePruneState implements Comparable<CubePruneState> {
			int[]             ranks;
			ComputeNodeResult tbl_item_states;
			Rule              rule;
			ArrayList<HGNode> l_ants;
			
			public CubePruneState(ComputeNodeResult state, int[] ranks, Rule rule, 
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
			 * @param another State to which this state will be compared
			 * @return -1 if this state's expected cost is less
			 *            than that stat's expected cost,
			 *         0  if this state's expected cost is equal
			 *            to that stat's expected cost,
			 *         +1 if this state's expected cost is
			 *            greater than that stat's expected cost
			 */
			public int compareTo(CubePruneState another) {
				if (this.tbl_item_states.getExpectedTotalCost() < another.tbl_item_states.getExpectedTotalCost()) {
					return -1;
				} else if (this.tbl_item_states.getExpectedTotalCost() == another.tbl_item_states.getExpectedTotalCost()) {
					return 0;
				} else {
					return 1;
				}
			}
		}
		

}
