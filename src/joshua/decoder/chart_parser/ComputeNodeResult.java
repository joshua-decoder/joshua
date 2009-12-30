
package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.decoder.ff.DPState;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.StateComputeResult;
import joshua.decoder.ff.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;


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
	private HashMap<Integer,DPState> featDPStatesTbl;
	
	
	
	
	/** 
	 * Compute costS and the states of thE node
	 */
	public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule, 
			ArrayList<HGNode> antNodes, int i, int j, SourcePath srcPath,
			List<StateComputer> stateComputers){
		
		double finalizedTotalCost = 0.0;
		
		if (null != antNodes) {
			for (HGNode item : antNodes) {
				finalizedTotalCost += item.bestHyperedge.bestDerivationCost;
			}
		}
		
		HashMap<Integer, StateComputeResult>  allStateResults = null;
		HashMap<Integer,DPState> allDPStates = null;
		
		for(StateComputer stateComputer : stateComputers){
			StateComputeResult stateResult = stateComputer.computeState(rule, antNodes, i, j, srcPath);			
			if(allStateResults==null)
				allStateResults = new HashMap<Integer, StateComputeResult>();
			allStateResults.put(stateComputer.getStateID(), stateResult);
			
			DPState dpState = stateResult.generateDPState();
			if(allDPStates==null)
				allDPStates = new HashMap<Integer,DPState>();
			allDPStates.put(stateComputer.getStateID(), dpState);
		}
		
		
		//=== compute feature costs
		double transitionCostSum    = 0.0;
		double futureCostEstimation = 0.0;
		
		for (FeatureFunction ff : featureFunctions) {
			StateComputeResult stateResult = allStateResults.get(ff.getStateID());
			
			transitionCostSum += 
				ff.getWeight() * ff.transition(rule, stateResult);
			
			futureCostEstimation +=
				ff.getWeight() * ff.estimateFutureCost(rule, stateResult);
			
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
	
	
	
	public static double[] computeModelTransitionCost(List<FeatureFunction> featureFunctions, Rule rule, 
					List<HGNode> antNodes, int i, int j, SourcePath srcPath,
					List<StateComputer> stateComputers){
		
		double[] res = new double[featureFunctions.size()];
		
		//=== compute state
		HashMap<Integer, StateComputeResult>  allStateResults = null;
		for(StateComputer stateComputer : stateComputers){
			StateComputeResult stateResult;
			if(rule!=null)
				stateResult = stateComputer.computeState(rule, antNodes, i, j, srcPath);
			else
				stateResult = stateComputer.computeFinalState(antNodes.get(0), i, j, srcPath);
			
			if(allStateResults==null)
				allStateResults = new HashMap<Integer, StateComputeResult>();
			allStateResults.put(stateComputer.getStateID(), stateResult);			
		}
		
		
		//=== compute feature costs
		int k=0;
		for(FeatureFunction ff : featureFunctions) {			
			StateComputeResult stateResult = allStateResults.get(ff.getStateID());	
			if(rule!=null)
				res[k] = ff.transition(rule, stateResult);
			else
				res[k] = ff.finalTransition(stateResult);		
			k++;
		}
		
		return res;		
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
	
	void setFeatDPStates(HashMap<Integer,DPState> states) {
		this.featDPStatesTbl = states;
	}
	
	HashMap<Integer,DPState> getFeatDPStates() {
		return this.featDPStatesTbl;
	}
}