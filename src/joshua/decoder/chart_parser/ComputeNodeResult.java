
package joshua.decoder.chart_parser;

import java.util.HashMap;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;


/**
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-12-22 14:00:36 -0500 (星期二, 22 十二月 2009) $
 */

public class ComputeNodeResult {
	
	private double expectedTotalCost;
	private double finalizedTotalCost;
	private double transitionTotalCost;
	
	// the key is state id;
	private HashMap<Integer,DPState> dpStates;
	
	
	
	
	/** 
	 * Compute costS and the states of thE node
	 */
	public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule,
			List<HGNode> antNodes, int i, int j, SourcePath srcPath, 
			List<StateComputer> stateComputers, int sentID){
		
		double finalizedTotalCost = 0.0;
		
		if (null != antNodes) {
			for (HGNode item : antNodes) {
				finalizedTotalCost += item.bestHyperedge.bestDerivationCost;
			}
		}
		
		
		HashMap<Integer,DPState> allDPStates = null;
		
		for(StateComputer stateComputer : stateComputers){
			DPState dpState = stateComputer.computeState(rule, antNodes, i, j, srcPath);					
			
			if(allDPStates==null)
				allDPStates = new HashMap<Integer,DPState>();
			allDPStates.put(stateComputer.getStateID(), dpState);
		}
		
		
		//=== compute feature costs
		double transitionCostSum    = 0.0;
		double futureCostEstimation = 0.0;
		
		for (FeatureFunction ff : featureFunctions) {		
			transitionCostSum += 
				ff.getWeight() * ff.transition(rule, antNodes, i, j, srcPath, sentID);
			
			DPState dpState = allDPStates.get(ff.getStateID());
			futureCostEstimation +=
				ff.getWeight() * ff.estimateFutureCost(rule, dpState, sentID);
			
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
		this.dpStates =  allDPStates;
	}
	
	public static double computeCombinedTransitionCost(List<FeatureFunction> featureFunctions, HyperEdge edge, 
			int i, int j, int sentID){
		double res = 0;
		for(FeatureFunction ff : featureFunctions) {				
			if(edge.getRule()!=null)
				res += ff.getWeight() * ff.transition(edge, i,  j, sentID);
			else
				res += ff.getWeight() * ff.finalTransition(edge, i, j, sentID);		
		}
		return res;
	}
	
	public static double[] computeModelTransitionCosts(List<FeatureFunction> featureFunctions, HyperEdge edge, 
			int i, int j,  int sentID){

			double[] res = new double[featureFunctions.size()];
			
			//=== compute feature costs
			int k=0;
			for(FeatureFunction ff : featureFunctions) {				
				if(edge.getRule()!=null)
					res[k] = ff.transition(edge, i, j, sentID);
				else
					res[k] = ff.finalTransition(edge,  i, j, sentID);		
				k++;
			}
			
			return res;		
	}

	public static double[] computeModelTransitionCosts(List<FeatureFunction> featureFunctions, Rule rule, 
					List<HGNode> antNodes, int i, int j, SourcePath srcPath, int sentID){
		
		double[] res = new double[featureFunctions.size()];
		
		//=== compute feature costs
		int k=0;
		for(FeatureFunction ff : featureFunctions) {				
			if(rule!=null)
				res[k] = ff.transition(rule, antNodes, i, j, srcPath, sentID);
			else
				res[k] = ff.finalTransition(antNodes.get(0),  i, j, srcPath, sentID);		
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
	
	void setDPStates(HashMap<Integer,DPState> states) {
		this.dpStates = states;
	}
	
	HashMap<Integer,DPState> getDPStates() {
		return this.dpStates;
	}
}