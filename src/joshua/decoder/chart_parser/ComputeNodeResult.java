
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
	
	private double expectedTotalLogP;
	private double finalizedTotalLogP;
	private double transitionTotalLogP;
	
	// the key is state id;
	private HashMap<Integer,DPState> dpStates;
	
	
	
	
	/** 
	 * Compute logPs and the states of thE node
	 */
	public ComputeNodeResult(List<FeatureFunction> featureFunctions, Rule rule,
			List<HGNode> antNodes, int i, int j, SourcePath srcPath, 
			List<StateComputer> stateComputers, int sentID){
		
		double finalizedTotalLogP = 0.0;
		
		if (null != antNodes) {
			for (HGNode item : antNodes) {
				finalizedTotalLogP += item.bestHyperedge.bestDerivationLogP; //semiring times
			}
		}
		
		
		HashMap<Integer,DPState> allDPStates = null;
		
		if(stateComputers!=null){
			for(StateComputer stateComputer : stateComputers){
				DPState dpState = stateComputer.computeState(rule, antNodes, i, j, srcPath);					
				
				if(allDPStates==null)
					allDPStates = new HashMap<Integer,DPState>();
				allDPStates.put(stateComputer.getStateID(), dpState);
			}
		}
		
		//=== compute feature logPs
		double transitionLogPSum    = 0.0;
		double futureLogPEstimation = 0.0;
		
		for (FeatureFunction ff : featureFunctions) {		
			transitionLogPSum += 
				ff.getWeight() * ff.transitionLogP(rule, antNodes, i, j, srcPath, sentID);
			
			DPState dpState = null;
			if(allDPStates!=null)
				dpState = allDPStates.get(ff.getStateID());
			futureLogPEstimation +=
				ff.getWeight() * ff.estimateFutureLogP(rule, dpState, sentID);
			
		}
		
		/* if we use this one (instead of compute transition
		 * logP on the fly, we will rely on the correctness
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
		//System.out.println(futureLogPEstimation);
		
		finalizedTotalLogP += transitionLogPSum;
		double expectedTotalLogP = finalizedTotalLogP + futureLogPEstimation;
		
		
		//== set the final results
		this.expectedTotalLogP = expectedTotalLogP;
		this.finalizedTotalLogP = finalizedTotalLogP;
		this.transitionTotalLogP = transitionLogPSum;
		this.dpStates =  allDPStates;
		
		//System.out.println(rule.toString());
		//printInfo();
	}
	
	public static double computeCombinedTransitionLogP(List<FeatureFunction> featureFunctions, HyperEdge edge, 
			int i, int j, int sentID){
		double res = 0;
		for(FeatureFunction ff : featureFunctions) {				
			if(edge.getRule()!=null)
				res += ff.getWeight() * ff.transitionLogP(edge, i,  j, sentID);
			else
				res += ff.getWeight() * ff.finalTransitionLogP(edge, i, j, sentID);		
		}
		return res;
	}
	
	public static double computeCombinedTransitionLogP(List<FeatureFunction> featureFunctions, Rule rule, 
			List<HGNode> antNodes,  int i, int j,  SourcePath srcPath, int sentID){
		double res = 0;
		for(FeatureFunction ff : featureFunctions) {				
			if(rule!=null)
				res += ff.getWeight() * ff.transitionLogP(rule, antNodes, i,  j, srcPath, sentID);
			else
				res += ff.getWeight() * ff.finalTransitionLogP(antNodes.get(0), i,  j, srcPath, sentID);		
		}
		return res;
	}
	
	public static double[] computeModelTransitionLogPs(List<FeatureFunction> featureFunctions, HyperEdge edge, 
			int i, int j,  int sentID){

			double[] res = new double[featureFunctions.size()];
			
			//=== compute feature logPs
			int k=0;
			for(FeatureFunction ff : featureFunctions) {				
				if(edge.getRule()!=null)
					res[k] = ff.transitionLogP(edge, i, j, sentID);
				else
					res[k] = ff.finalTransitionLogP(edge,  i, j, sentID);		
				k++;
			}
			
			return res;		
	}

	public static double[] computeModelTransitionLogPs(List<FeatureFunction> featureFunctions, Rule rule, 
					List<HGNode> antNodes, int i, int j, SourcePath srcPath, int sentID){
		
		double[] res = new double[featureFunctions.size()];
		
		//=== compute feature logPs
		int k=0;
		for(FeatureFunction ff : featureFunctions) {				
			if(rule!=null)
				res[k] = ff.transitionLogP(rule, antNodes, i, j, srcPath, sentID);
			else
				res[k] = ff.finalTransitionLogP(antNodes.get(0),  i, j, srcPath, sentID);		
			k++;
		}
		
		return res;		
	}
		
	
	
	void setExpectedTotalLogP(double logP) {
		this.expectedTotalLogP = logP;
	}
	
	public double getExpectedTotalLogP() {
		return this.expectedTotalLogP;
	}
	
	void setFinalizedTotalLogP(double logP) {
		this.finalizedTotalLogP = logP;
	}
	
	double getFinalizedTotalLogP() {
		return this.finalizedTotalLogP;
	}
	
	void setTransitionTotalLogP(double logP) {
		this.transitionTotalLogP = logP;
	}
	
	double getTransitionTotalLogP() {
		return this.transitionTotalLogP;
	}
	
	void setDPStates(HashMap<Integer,DPState> states) {
		this.dpStates = states;
	}
	
	HashMap<Integer,DPState> getDPStates() {
		return this.dpStates;
	}
	
	public void printInfo(){
		System.out.println("scores: "+ transitionTotalLogP + "; " + finalizedTotalLogP + "; " +  expectedTotalLogP);
	}
}