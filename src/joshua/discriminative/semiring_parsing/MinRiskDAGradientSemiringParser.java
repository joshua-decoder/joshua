package joshua.discriminative.semiring_parsing;

import java.util.HashMap;
import java.util.Map;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

/** In order to compute the expectation statistics on a hypergraph based on a particular model,
 * we just need to know: 
 * (1) the topology of the hypergraph
 * (2) what features fire at each hyperedge
 * */

public class MinRiskDAGradientSemiringParser  extends MinRiskDAAbstractSemiringParser {
	
	public MinRiskDAGradientSemiringParser(int semiring, int addMode, double scale,	double temperature_) {
		
		super(semiring, addMode, scale,  temperature_);
	}

	

	@Override
	protected VarianceSemiringHashMap createNewSemiringMember() {
		return new VarianceSemiringHashMap();
	}

	
	@Override
	/**This gives us: \sum_y p(y) \phi_i(y) ( l(y,y^*) + T ( s(x,y) ) ); where s(x,y) is the linear combination of feature scores (include the scaling factor) 
	 * */
	protected VarianceSemiringHashMap getHyperedgeSemiringWeight(HyperEdge dt, HGNode parentItem, double scale,  AtomicSemiring p_atomic_semiring) {
		
		VarianceSemiringHashMap res = null;
		if(p_atomic_semiring.ATOMIC_SEMIRING==AtomicSemiring.LOG_SEMIRING){
			
			//=== extract active feature on the hyperege			
			//HashMap<Integer, Double> activeFeatures = featureExtraction(dt, parentItem);
			HashMap<Integer, Double> activeFeatures  = getFeatureForest().featureExtraction(dt, parentItem);
			
			//=== compute transition probility
			//double logProb = getlogProb(dt, parentItem, activeFeatures);
			double logProb = getFeatureForest().getEdgeLogTransitionProb(dt, parentItem);
			
			//factor1: this factor depends on the risk and the aggregate transition cost at the hyperedge
			double factor1Raw = getFactor1RawValue(dt, parentItem, logProb);// \sum_y p(y) ( l(y,y^*) +  T ( s(x,y) ) ) 
			
			SignedValue factor1 =  SignedValue.multi(
					logProb,
					SignedValue.createSignedValue(factor1Raw)
				);
			
			//factor2 and combined
			HashMap<Integer, Double> factor2Raw = activeFeatures;// \sum_y p(y) \phi_i(y)  
			
			HashMap<Integer, SignedValue> factor2 = new HashMap<Integer, SignedValue>();
			HashMap<Integer, SignedValue> combined = new HashMap<Integer, SignedValue>();			
			
			//for each feature get fired at the hyperedge
			for(Map.Entry<Integer, Double> feature : factor2Raw.entrySet()){
				Integer key = feature.getKey();
				

				double  val =  feature.getValue();				
				//factor2Raw[i] =  -getFeatureCost(dt, parentItem, i);
				
				factor2.put(key, SignedValue.multi(
						logProb,
						SignedValue.createSignedValue(val))
						);			
				
				combined.put(key, SignedValue.multi(
						factor1,
						SignedValue.createSignedValue(val))
						);
			}			
			
			res = new VarianceSemiringHashMap(logProb, factor1, factor2, combined);
			
		}else{
			System.out.println("un-implemented atomic-semiring");
			System.exit(1);
		}
		return res;
	}
	
	
	
//	 l(y,y^*) + T ( s(x,y) ); where s(x,y) is the linear combination of feature scores (times the scaling factor) 
	protected double getFactor1RawValue(HyperEdge dt, HGNode parentItem, double logTransitionProb){
	
		double factor1Raw = 0;		
		//l(y,y^*)
		if(dt.getRule() != null){//note: hyperedges under goal item does not contribute BLEU
			//TODO
			//factor1Raw = WithRiskAnnotationDiskHyperGraph.getRiskAnnotation((WithModelCostsHyperEdge)dt);		
			factor1Raw = getFeatureForest().getEdgeRisk(dt);

		}
		/**logP has two parts: s(x,y) - logZ(x)   
		 * where s(x,y) is the linear combination of feature scores (times the scaling factor)
		 * We need to consider logZ(x) at the root;
		 * Also, we cannot incorporate the constant *one* here, as it is not linearly decomposible 
		 **/
		factor1Raw += temperature * logTransitionProb;//s(x,y) = getlogProb(.)
		
		return factor1Raw;		
	} 
	


	
	
	public HashMap<Integer, Double> computeGradientForTheta(){
		insideEstimationOverHG(hg);
		CompositeSemiring goalSemiring = getGoalSemiringMember(hg);
		goalSemiring.normalizeFactors();
		
		//goal_semiring.printInfor();
		
		//get final gradient
		SignedValue  factor1 = ((VarianceSemiringHashMap)goalSemiring).getFactor1();
		HashMap<Integer, SignedValue>  factor2 = ((VarianceSemiringHashMap)goalSemiring).getFactor2();
		HashMap<Integer, SignedValue> combinedFactor = ((VarianceSemiringHashMap)goalSemiring).getCombinedfactor();
		
		//System.out.print("Gradients are: ");
		HashMap<Integer, Double> gradient = new HashMap<Integer, Double>();;
		for(Map.Entry<Integer, SignedValue> feature : combinedFactor.entrySet()){
			double factor2Val = factor2.get( feature.getKey() ).convertRealValue();
			double combinedVal = feature.getValue().convertRealValue();
			double val =computeGradientForTheta(factor1.convertRealValue(), factor2Val, combinedVal);
			gradient.put(feature.getKey(),  val);
			//System.out.print( feature.getKey()+"=" + val +" " );
		}
		//System.out.print("\n");
		return gradient;
	}
	

	protected double computeGradientForTheta(double factor1, double factor2, double combined){
		double res = combined;
		
		res -= factor1 * factor2;
		/*
		gradient[i] += factor2[i] * temperature * (1.0-logProb);//consider logZ(x)=logProb;
		gradient[i] -= (factor1 + temperature * (1.0 - logProb)) * factor2[i];//consider logZ(x);
		 //**It seems the net effect of the above two statements is simply euqivalent to
		 //* gradient[i] -= factor1*factor2[i]
		*/
		
		res *= scale;
		
	
		
		if(Double.isNaN(res)){
			System.out.println("gradient value is NaN"); 
			System.exit(1);
		} 
		return res;
		
	}
}
