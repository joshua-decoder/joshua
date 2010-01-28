package joshua.discriminative.variational_decoder;

import java.util.List;

import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.semiring_parsing.AtomicSemiring;
import joshua.discriminative.semiring_parsing.DefaultSemiringParser;
import joshua.discriminative.semiring_parsing.ExpectationSemiring;
import joshua.discriminative.semiring_parsing.SignedValue;

/*compute D(p||q)*/

/*Note that Entropy itself is a special case*/

public class CrossEntropyOnHG extends DefaultSemiringParser {
	
	List<FeatureFunction> pFeatFunctions;
	List<FeatureFunction> qFeatFunctions;

	public CrossEntropyOnHG(int semiring, int add_mode, double scale,  List<FeatureFunction> pFeatFunctions,  List<FeatureFunction> qFeatFunctions ){
		super(semiring, add_mode, scale);//TODO: use different scale for p and q?
		this.pFeatFunctions = pFeatFunctions;
		this.qFeatFunctions = qFeatFunctions;
	}

	protected ExpectationSemiring createNewSemiringMember() {
		return new ExpectationSemiring();
	}
	
	protected ExpectationSemiring getHyperedgeSemiringWeight(HyperEdge dt, HGNode parent_item, double scale, AtomicSemiring p_atomic_semiring){
		ExpectationSemiring res = null;
		if(p_atomic_semiring.ATOMIC_SEMIRING==AtomicSemiring.LOG_SEMIRING){
			double logProbP = - scale * computeTransitionCost(parent_item, dt, pFeatFunctions);//from p
			double valQ = -  scale * computeTransitionCost(parent_item, dt, qFeatFunctions);//from q;//s(x,y); to compute E(s(x,y)); real semiring
			
			//double factor1 = Math.exp(logProbP)*valQ; //real semiring
			SignedValue factor1 =  SignedValue.multi(
					logProbP,
					SignedValue.createSignedValue(valQ)
			);
			res = new ExpectationSemiring(logProbP, factor1);
		}else{
			System.out.println("un-implemented atomic-semiring");
			System.exit(1);
		}
		return res;
	}

	static private double computeTransitionCost(HGNode parentNode, HyperEdge dt, List<FeatureFunction> featFunctions){
		
		double[] transitionCosts = ComputeNodeResult.computeModelTransitionLogPs(
				featFunctions, dt, parentNode.i, parentNode.j, -1);
		
		//transition cost
		double transCost =0 ;
		int i=0;
		for(FeatureFunction m : featFunctions ){
			transCost  += transitionCosts[i++] * m.getWeight();
		}			
		return transCost;		
	}
	

}