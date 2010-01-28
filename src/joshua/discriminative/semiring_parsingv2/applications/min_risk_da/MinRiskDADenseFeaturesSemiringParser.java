package joshua.discriminative.semiring_parsingv2.applications.min_risk_da;

import java.util.HashMap;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.semiring_parsingv2.DefaultIOParserWithXLinearCombinator;
import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.ExpectationSemiringPM;
import joshua.discriminative.semiring_parsingv2.pmodule.ListPM;
import joshua.discriminative.semiring_parsingv2.pmodule.SparseMap;
import joshua.discriminative.semiring_parsingv2.semiring.ExpectationSemiring;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;
import joshua.discriminative.training.risk_annealer.hypergraph.FeatureForest;

/** P is in a SemiringLog
 *  R is a RiskAndEntropyPModule, containing entropy and risk
 *  S is a map
 *  T is a map
 * */

/** This class implements the method described in Sec 6.2.
 * It requires a hyperpgraph, who provides the topology 
 * and the four quantities including P_e, L_e, log P_e, and (P_e)'.
 * This is provided by the feature forest, through three functions:
 * getEdgeLogTransitionProb, getEdgeRisk, and getGradientSparseMap
 * 
 * */

/**compute func and graident for 
 *  risk - temperature*entropy*/

public class MinRiskDADenseFeaturesSemiringParser 
extends  DefaultIOParserWithXLinearCombinator<
	ExpectationSemiring<LogSemiring, RiskAndEntropyPM>,
	ExpectationSemiringPM<LogSemiring, RiskAndEntropyPM, ListPM, ListPM, MinRiskDABO>
	> {
	
	MinRiskDABO pBilinearOperator = new MinRiskDABO();
	
	//annealing parameters
	private double temperature; 

	//latest value
	private double entropy;
	private double risk;
	private double functionValue;
	
	//TODO
	boolean computeEntropy = true;
	boolean computeRisk = true;
	 
	
	public MinRiskDADenseFeaturesSemiringParser(double temperature) {
		
		super();						 
		this.temperature = temperature;
	}

	@Override
	protected ExpectationSemiring<LogSemiring, RiskAndEntropyPM> 
	
	createNewKWeight() {		
		LogSemiring p = new LogSemiring();
		RiskAndEntropyPM r = new RiskAndEntropyPM();
		return new ExpectationSemiring<LogSemiring, RiskAndEntropyPM>( p, r );
	}


	@Override
	protected ExpectationSemiringPM<LogSemiring, RiskAndEntropyPM, ListPM, ListPM, MinRiskDABO> 
	
	createNewXWeight() {		
		ListPM s = new ListPM( new SparseMap() );		
		ListPM t = new ListPM( new SparseMap() );		
		return new ExpectationSemiringPM<LogSemiring, RiskAndEntropyPM, ListPM, ListPM,  MinRiskDABO>( s, t,  pBilinearOperator);
	}

	
	@Override
	protected ExpectationSemiring<LogSemiring, RiskAndEntropyPM> 
	
	getEdgeKWeight(HyperEdge dt, HGNode parentItem) {		
		
		//== p
		double logProb = getFeatureForest().getEdgeLogTransitionProb(dt, parentItem);
		LogSemiring p = new LogSemiring(logProb);
		
		//== r
		double rRisk = getFeatureForest().getEdgeRisk(dt);
		double rEntropy =  logProb;//log(p_e)
		double rMixed = rRisk + this.getTemperature() * rEntropy; //the objective is risk - T * entropy
		
		RiskAndEntropyPM r =  new RiskAndEntropyPM(
				SignedValue.createSignedValueFromRealNumber(rMixed),
				SignedValue.createSignedValueFromRealNumber(rEntropy),
				SignedValue.createSignedValueFromRealNumber(rRisk)
		);
		
		//r= p r
		r.multiSemiring(p);
		return new ExpectationSemiring<LogSemiring, RiskAndEntropyPM>(p,r);
	}
	
	
	
	
	@Override
	protected ExpectationSemiringPM<LogSemiring, RiskAndEntropyPM, ListPM, ListPM, MinRiskDABO> 
	
	getEdgeXWeight(HyperEdge dt, HGNode parentItem) {
		//TODO: p and r has been computed twice, consider speed up		
		//== p
		double logProb = getFeatureForest().getEdgeLogTransitionProb(dt, parentItem);
		
		//== r
		double rRisk = getFeatureForest().getEdgeRisk(dt);
		double rEntropy = logProb;//log(p_e); 
		
		double rMixed = rRisk + this.getTemperature()*(rEntropy + 1); //( L_e + temperature * (logP_e + 1) )
		
		/*
		RiskAndEntropyPM r =  new RiskAndEntropyPM(
				SignedValue.createSignedValueFromRealNumber(rMixed),
				SignedValue.createSignedValueFromRealNumber(rEntropy),
				SignedValue.createSignedValueFromRealNumber(rRisk)
		);*/
				
			
		SparseMap gradientsMap = getFeatureForest().getGradientSparseMap(parentItem, dt, logProb);
		ListPM s = new ListPM( gradientsMap );
		
	
		//== t = L_e * (P_e)' - temperature * (1+logP_e) (P_e)' = (P_e)' * ( L_e - temperature * (1+logP_e) )
		//ListPM t = pBilinearOperator.bilinearMulti(r, s);
		ListPM t = pBilinearOperator.bilinearMulti(SignedValue.createSignedValueFromRealNumber(rMixed), s);
		
		
		return new ExpectationSemiringPM<LogSemiring, RiskAndEntropyPM, ListPM, ListPM, MinRiskDABO>(s, t, pBilinearOperator);
	}

	
	@Override
	public void normalizeGoal() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	//============== additional functions ========
	
	public final void setTemperature(double temperature_){
		this.temperature = temperature_;
	}
	
	protected final double getTemperature(){
		return this.temperature;
	}
	
	

	//return the entropy 
	public double getEntropy(){
		return this.entropy;
	}
	
	
	//return the entropy 
	public double getRisk(){
		return this.risk;
		
	}
	
	public double getFuncVal(){
		return functionValue;//this.risk - this.temperature*this.entropy;		
	}
	
	
	
	public HashMap<Integer, Double> computeGradientForTheta(){
		this.clearState();
		this.insideEstimationOverHG(); 
		this.outsideEstimationOverHG();
		
		ExpectationSemiring<LogSemiring, RiskAndEntropyPM> goalK = this.getGoalK();
		ExpectationSemiringPM<LogSemiring, RiskAndEntropyPM, ListPM, ListPM, MinRiskDABO> goalX = this.getGoalX();
		double logZ = goalK.getP().getLogValue();
		//goalK.printInfor();
		//goalX.printInfor();
	
		
		//=== entropy
		//--normalize
		SignedValue entropyFactor = goalK.getR().getEntropy().duplicate();
		entropyFactor.multiLogNumber(-logZ);
		
		this.entropy = logZ - entropyFactor.convertToRealValue();//logZ - \bar{r}/Z
		//System.out.print("Entropy is" + entropy);
		
		//=== risk
		//--normalize
		SignedValue riskFactor = goalK.getR().getRisk().duplicate();
		riskFactor.multiLogNumber(-logZ);
		this.risk =  riskFactor.convertToRealValue();
		//System.out.print("Risk is" + risk);
		
		this.functionValue = this.risk - this.temperature*this.entropy;
		
		
		//=== gradients
		//System.out.print("Gradients are: ");
		HashMap<Integer, Double>  gradient = new HashMap<Integer, Double>();
		for(Integer featID : goalX.getT().getValue().getIds()){
			//delta(r)*Z/Z^2=delta(r)/Z
			//--normalize
			SignedValue resT = goalX.getT().getValue().getValueAt(featID).duplicate(); 
			resT.multiLogNumber(-logZ);
				
			//-delta(Z)*r/Z^2
			SignedValue resRS = SignedValue.multi(
					goalX.getS().getValue().getValueAt(featID),
					goalK.getR().getValue()
				);
			resRS.negate();
			resRS.multiLogNumber(-2*logZ);
			
			//-T*delta(Z)/Z
			SignedValue resS = goalX.getS().getValue().getValueAt(featID).duplicate();
			resS.multiLogNumber(Math.log(this.getTemperature()));
			resS.negate();			
			resS.multiLogNumber(-logZ);
			
			//add them together
			resT.add(resRS);
			resT.add(resS);
			double finalVal = resT.convertToRealValue();
			
			gradient.put(featID, finalVal);
			if(Double.isNaN(finalVal)){
				System.out.println("gradient value for theta is NaN"); 
				System.exit(1);
			} 
			//System.out.print( gradient[i]+" " );
		}
		/*
		System.out.println("Risk is : " + risk);
		System.out.println("Entropy is : " + entropy);
		System.out.println("Function value is : " + functionValue);
		*/
		return gradient;
	}
	
	
	//@todo: parameterize the HG 
	private final FeatureForest getFeatureForest(){
		return (FeatureForest) hg;
	}
	
}
