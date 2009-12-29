
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

/**
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-12-22 14:00:36 -0500 (星期二, 22 十二月 2009) $
 */

public class ComputeNodeResult {
	
	private double expectedTotalCost;
	private double finalizedTotalCost;
	private double transitionTotalCost;
	
	// the key is feature id; tbl of dpstate for each stateful feature
	private HashMap<Integer,FFDPState> featDPStatesTbl;
	
	
	/** 
	 * Compute costS and the states of thE node
	 */
	public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule, 
			ArrayList<HGNode> antNodes, int i, int j, SourcePath srcPath){
		
		double finalizedTotalCost = 0.0;
		
		if (null != antNodes) {
			for (HGNode item : antNodes) {
				finalizedTotalCost += item.bestHyperedge.bestDerivationCost;
			}
		}
		
		HashMap<Integer,FFDPState> allDPStates = null;
		double transitionCostSum    = 0.0;
		double futureCostEstimation = 0.0;
		
		for (FeatureFunction ff : featureFunctions) {
			////long start2 = Support.current_time();
			FFTransitionResult state = HyperGraph.computeTransition(
					null, rule, antNodes, ff, i, j, srcPath);
			transitionCostSum +=
				ff.getWeight() * state.getTransitionCost();
			
			if (ff.isStateful()) {
				futureCostEstimation +=
					ff.getWeight() * state.getFutureCostEstimation();
				
				FFDPState itemState = state.getStateForNode();
				if (null != itemState) {
					if (null == allDPStates) {
						allDPStates = new HashMap<Integer,FFDPState>();
					}
					allDPStates.put(ff.getFeatureID(), itemState);
				} else {
					throw new RuntimeException("ComputeNodeResult: null getStateForItem()\n");					
				}
			} else {	
				futureCostEstimation += 0.0;
			}
			////ff.time_consumed += Support.current_time() - start2;
		}
		
		/* if we use this one (instead of compute transition
		 * cost on the fly, we will rely on the correctness
		 * of rule.statelesscost. This will cause a nasty
		 * bug for MERT. Specifically, even we change the
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
		this.featDPStatesTbl =  allDPStates;
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
		this.featDPStatesTbl = states;
	}
	
	HashMap<Integer,FFDPState> getFeatDPStates() {
		return this.featDPStatesTbl;
	}
}