/**
 * 
 */
package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FFTransitionResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperGraph;

public class ComputeItemResult {
	private double expectedTotalCost;
	private double finalizedTotalCost;
	private double transitionTotalCost;
	
	// the key is feature id; tbl of dpstate for each stateful feature
	private HashMap<Integer,FFDPState> tbl_feat_dpstates;
	
	
	/** 
	 * Compute cost and the states of this item returned
	 * ArrayList: expectedTotalCost, finalizedTotalCost,
	 * transition_cost, bonus, list of states
	 */
	public ComputeItemResult(List<FeatureFunction> featureFunctions, Rule rule, 
			ArrayList<HGNode> previousItems, int i, int j, SourcePath srcPath){
		
		double finalizedTotalCost = 0.0;
		
		
		if (null != previousItems) {
			for (HGNode item : previousItems) {
				finalizedTotalCost += item.bestHyperedge.bestDerivationCost;
			}
		}
		
		HashMap<Integer,FFDPState> allItemStates = null;
		double transitionCostSum    = 0.0;
		double futureCostEstimation = 0.0;
		
		for (FeatureFunction ff : featureFunctions) {
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
		
		
		//== set the final results
		this.expectedTotalCost = expectedTotalCost;
		this.finalizedTotalCost = finalizedTotalCost;
		this.transitionTotalCost = transitionCostSum;
		this.tbl_feat_dpstates =  allItemStates;
	}
	
	
	
	void setExpectedTotalCost(double cost) {
		this.expectedTotalCost = cost;
	}
	
	public double getExpectedTotalCost() {
		return this.expectedTotalCost;
	}
	
	void setFinalizedTotalCost(double cost) {
		this.finalizedTotalCost = cost;
	}
	
	double getFinalizedTotalCost() {
		return this.finalizedTotalCost;
	}
	
	void setTransitionTotalCost(double cost) {
		this.transitionTotalCost = cost;
	}
	
	double getTransitionTotalCost() {
		return this.transitionTotalCost;
	}
	
	void setFeatDPStates(HashMap<Integer,FFDPState> states) {
		this.tbl_feat_dpstates = states;
	}
	
	HashMap<Integer,FFDPState> getFeatDPStates() {
		return this.tbl_feat_dpstates;
	}
}