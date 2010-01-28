package joshua.discriminative.variational_decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.TrivialInsideOutside;
import joshua.discriminative.feature_related.feature_function.EdgeTblBasedBaselineFF;
import joshua.discriminative.feature_related.feature_function.FeatureTemplateBasedFF;
import joshua.discriminative.semiring_parsing.DefaultSemiringParser;
import joshua.discriminative.semiring_parsing.ExpectationSemiring;



public class InformationMeasures {	
	
	
//	#######################################################################	
	public static void main(String[] args) {
		if(args.length!=4){
			System.out.println("Wrong number of parameters, it must have at least four parameters: java NbestMinRiskAnnealer use_shortest_ref f_config gain_factor f_dev_src f_nbest_prefix f_dev_ref1 f_dev_ref2....");
			System.exit(1);
		}
		
		//long start_time = System.currentTimeMillis();		
		String testItemsFile=args[0].trim();
		String testRulesFile=args[1].trim();
		int num_sents=new Integer(args[2].trim());
		String f_config=args[3].trim();//be careful with the weights
		
		//set up models
		VariationalDecoderConfiguration.readConfigFile(f_config);
		SymbolTable symbolTbl = new BuildinSymbol(null);	
		List<FeatureFunction> featFunctions = new ArrayList<FeatureFunction>();
		HashMap<VariationalNgramApproximator, FeatureTemplateBasedFF> approximatorMap = new HashMap<VariationalNgramApproximator, FeatureTemplateBasedFF> ();
		VariationalDecoderConfiguration.initializeModels(f_config, symbolTbl, featFunctions, approximatorMap);		
		double insideOutsideScalingFactor =  VariationalDecoderConfiguration.insideoutsideScalingFactor;				
		
		List<FeatureFunction> pFeatFunctions = new ArrayList<FeatureFunction>();
		List<FeatureFunction> qFeatFunctions = new ArrayList<FeatureFunction>();
		for(FeatureFunction ff : featFunctions){
			if(ff instanceof EdgeTblBasedBaselineFF){
				pFeatFunctions.add(ff);
				System.out.println("############### add one feature in P");
			}else{
				qFeatFunctions.add(ff);//TODO assume all other features go to q
				System.out.println("############## add one feature in q");
			}
		}
		
		double scale = 1.0;
		int ngramStateID = 0;
		DefaultSemiringParser parserEntropyP = new CrossEntropyOnHG(1, 0, scale, pFeatFunctions, pFeatFunctions);
		DefaultSemiringParser parserEntropyQ = new CrossEntropyOnHG(1, 0, scale, qFeatFunctions, qFeatFunctions);
		DefaultSemiringParser parserCrossentropyPQ = new CrossEntropyOnHG(1, 0, scale, pFeatFunctions, qFeatFunctions);
		
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, true, null); //have model costs stored
		diskHG.initRead(testItemsFile, testRulesFile, null);		
		for(int sent_id=0; sent_id < num_sents; sent_id ++){
			System.out.println("#Process sentence " + sent_id);
			HyperGraph testHG = diskHG.readHyperGraph();
			
			//################setup the model: including estimation of variational model
			//### step-1: run inside-outside
			//note, inside and outside will use the transition_cost of each hyperedge, this cost is already linearly interpolated
			TrivialInsideOutside insideOutsider = new TrivialInsideOutside();
			insideOutsider.runInsideOutside(testHG, 0, 1, insideOutsideScalingFactor);//ADD_MODE=0=sum; LOG_SEMIRING=1;
			
			//### step-2: model extraction based on the definition of Q
			for(Map.Entry<VariationalNgramApproximator, FeatureTemplateBasedFF> entry : approximatorMap.entrySet()){
				VariationalNgramApproximator approximator = entry.getKey();
				FeatureTemplateBasedFF featureFunction = entry.getValue();
				HashMap<String, Double> model = approximator.estimateModel(testHG, insideOutsider);
				featureFunction.setModel(model);			
			}
			
	
			//###############semiring parsing
			parserEntropyP.insideEstimationOverHG(testHG);
			parserEntropyQ.insideEstimationOverHG(testHG);
			parserCrossentropyPQ.insideEstimationOverHG(testHG);
			
			ExpectationSemiring pGoalSemiring = (ExpectationSemiring) parserEntropyP.getGoalSemiringMember(testHG);
			ExpectationSemiring qGoalSemiring = (ExpectationSemiring) parserEntropyQ.getGoalSemiringMember(testHG);
			ExpectationSemiring pqGoalSemiring = (ExpectationSemiring) parserCrossentropyPQ.getGoalSemiringMember(testHG);
			
			pGoalSemiring.normalizeFactors();
			pGoalSemiring.printInfor();
			qGoalSemiring.normalizeFactors();
			qGoalSemiring.printInfor();
			pqGoalSemiring.normalizeFactors();
			pqGoalSemiring.printInfor();
			
			
			double entropyP = pGoalSemiring.getLogProb() - pGoalSemiring.getFactor1().convertRealValue();//logZ-E(s)??????????
			double entropyQ = qGoalSemiring.getLogProb() - qGoalSemiring.getFactor1().convertRealValue();//logZ-E(s)?????
			double crossEntropyPQ = qGoalSemiring.getLogProb()- pqGoalSemiring.getFactor1().convertRealValue();//logZ(q)-E(s)?????????
			double klPQ = -entropyP + crossEntropyPQ;
			if(klPQ<0){System.out.println("kl divergence is negative, must be wrong"); System.exit(1);}
			System.out.println("p_entropy=" + entropyP +"; "+"q_entropy=" + entropyQ +"; "+"pq_entropy=" + crossEntropyPQ +"; "+"pq_kl=" + klPQ);
		}		
	}
}
