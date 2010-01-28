package joshua.discriminative.training.risk_annealer.hypergraph;

import java.util.HashMap;
import java.util.Map;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.SparseMap;

/**
 * 
 * ideally, we should first process the hypergraph to store risk and feature information,
 * and then using a feature filter to filter unwanted features.
 * Each feature should have a unique feature ID.
 **/

public class FeatureForest  extends HyperGraph {

	//latest model
	double[] featureWeights;
	double scale;
	
	
	public FeatureForest(HyperGraph hg) {
		super(hg.goalNode, hg.numNodes, hg.numEdges, hg.sentID, hg.sentLen);
	}

	public void setFeatureWeights(double[] featureWeights){
		this.featureWeights = featureWeights;
	}
	
	public void setScale(double scale){
		this.scale = scale;
	}
	
	
	public final  HashMap<Integer, Double> featureExtraction(HyperEdge dt, HGNode parentItem){
		return ((FeatureHyperEdge)dt).featureTbl;//TODO
	}	

	
	public final  double getEdgeRisk( HyperEdge dt){
		
		if(dt.getRule() == null){//hyperedges under goal item does not contribute BLEU
			return 0;
		}else{
			return ((FeatureHyperEdge)dt).transitionRisk;//TODO
		}
	}
	
	
	//edge transition log-probability
	public final double getEdgeLogTransitionProb(HyperEdge edge, HGNode parentItem){
		
		double transitionLogP =0;
		
		/**assume all feature are fired
		 **/
		HashMap<Integer, Double> features = featureExtraction(edge, parentItem);
		
		for(Map.Entry<Integer, Double> feature : features.entrySet()){
			int featID = feature.getKey();
			transitionLogP += this.featureWeights[featID] * feature.getValue();
		}
		
		return  scale * transitionLogP;
	}

	
	public final SparseMap getGradientSparseMap(HGNode parentItem, HyperEdge dt, double logTransitionProb){

		HashMap<Integer, Double> features = featureExtraction(dt, parentItem);
		
		HashMap<Integer, SignedValue> gradientsMap = new HashMap<Integer, SignedValue>(); 		
		for(Map.Entry<Integer, Double> feature : features.entrySet()){
			int featID = feature.getKey();
			
			//P_e * \gamma * \Phi(e)
			SignedValue logGradient = SignedValue.createSignedValueFromRealNumber( scale*feature.getValue() );
			logGradient.multiLogNumber(logTransitionProb);
			
			gradientsMap.put(featID, logGradient);
		}
		
		return new SparseMap(gradientsMap);
	}
		

}
