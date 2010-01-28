package joshua.discriminative.semiring_parsing;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;

public class MinRiskDAFuncValSemiringParser extends MinRiskDAAbstractSemiringParser {
	
	int numValues = 2;//we want to compute two values: risk and entropy
	
	double finalRisk;
	double finalEntropy;
	
	public MinRiskDAFuncValSemiringParser(int semiring, int add_mode, double scale,
			double temperature_) {
		super(semiring, add_mode, scale, temperature_);
	}

	@Override
	protected ExpectationSemiringVector createNewSemiringMember() {
		return new ExpectationSemiringVector(numValues);
	}

	@Override
	protected ExpectationSemiringVector getHyperedgeSemiringWeight(HyperEdge dt, HGNode parentItem, double scale,  AtomicSemiring p_atomic_semiring) {
		
		ExpectationSemiringVector res = null;
		if(p_atomic_semiring.ATOMIC_SEMIRING==AtomicSemiring.LOG_SEMIRING){
		
			//double logProb = getlogProb(dt, parentItem, activeFeatures);			
			double logProb = getFeatureForest().getEdgeLogTransitionProb(dt, parentItem);
			
			//double realProb = Math.exp(logProb);
			
			//factor1
			double[] factor1Raw = new double[numValues];
			SignedValue[] factor1 = new SignedValue[numValues];
			
			//==risk
			factor1Raw[0] =  computeRiskRawFactor(dt, parentItem);
			//factor1[0] = realProb*factor1Raw[0];
			factor1[0] =  SignedValue.multi(
					logProb,
					SignedValue.createSignedValue(factor1Raw[0])
				);
			
			//==entropy
			factor1Raw[1] =  computeEntropyRawFactor(dt, parentItem, logProb);
			//factor1[1] = realProb*factor1Raw[1];
			factor1[1] =  SignedValue.multi(
					logProb,
					SignedValue.createSignedValue(factor1Raw[1])
				);
			
			res = new ExpectationSemiringVector(logProb, factor1);
		}else{
			System.out.println("un-implemented atomic-semiring");
			System.exit(1);
		}
		return res;
	}
		
	private double computeRiskRawFactor(HyperEdge dt, HGNode parentItem){
		
		double riskFactor1Raw = 0;
		
		if(dt.getRule() != null){//note: hyperedges under goal item does not contribute BLEU			
			//TODO
			riskFactor1Raw = getFeatureForest().getEdgeRisk( dt);
		}
		
		return riskFactor1Raw;
	}
	
	
	/**-logP has two parts: logZ(x) -  s(x,y) 
	 * where s(x,y) is the linear combination of feature scores
	 * We need to consider logZ(x) at the root
	 **/
	private double computeEntropyRawFactor(HyperEdge dt, HGNode parentItem, double logTransitionProb){
		return - logTransitionProb;
	}
	

	public double computeFunctionVal(){
		insideEstimationOverHG(hg);
		CompositeSemiring goal_semiring = getGoalSemiringMember(hg);
		goal_semiring.normalizeFactors();
		
		//goal_semiring.printInfor();
		
		//get final gradient
		double  logProb = ((ExpectationSemiringVector)goal_semiring).getLogProb();
		SignedValue[]  factor1 = ((ExpectationSemiringVector)goal_semiring).getFactor1();
		finalRisk = factor1[0].convertRealValue();
		finalEntropy = factor1[1].convertRealValue() + logProb;//logProb is the normalization constant
		double functionValue = finalRisk - temperature*finalEntropy;
		if(finalEntropy<0){
			System.out.println("Entropy is negative, must be wrong; " + finalEntropy);
			//System.exit(1);
		}
		
		
		if(Double.isNaN(finalRisk)){System.out.println("risk is NaN"); System.exit(1);}
		if(Double.isNaN(finalEntropy)){System.out.println("entropy is NaN"); System.exit(1);}
		if(Double.isNaN(functionValue)){System.out.println("functionValue is NaN"); System.exit(1);}
		
		/*
		System.out.println("Risk is : " + finalRisk);
		System.out.println("Entropy is : " + finalEntropy);
		System.out.println("Function value is : " + functionValue);
		*/
		return functionValue;		
	}
	
	public double getRisk(){
		return finalRisk;
	}
	
	public double getEntropy(){
		return finalEntropy;
	}
}
