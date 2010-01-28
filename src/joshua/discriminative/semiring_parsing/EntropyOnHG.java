package joshua.discriminative.semiring_parsing;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

public class EntropyOnHG extends DefaultSemiringParser {

	public EntropyOnHG(int semiring, int add_mode, double scale) {
		super(semiring, add_mode, scale);
		// TODO Auto-generated constructor stub
	}

	protected ExpectationSemiring createNewSemiringMember() {
		return new ExpectationSemiring();
	}
	
	protected ExpectationSemiring getHyperedgeSemiringWeight(HyperEdge dt, HGNode parent_item, double scale, AtomicSemiring p_atomic_semiring){
		ExpectationSemiring res = null;
		if(p_atomic_semiring.ATOMIC_SEMIRING==AtomicSemiring.LOG_SEMIRING){
			double logProb = scale * dt.getTransitionLogP(false);
			double val = scale * dt.getTransitionLogP(false);//s(x,y); to compute E(s(x,y)); s(x,y) is the linear combintation (considered scaling factor)
			
			//double factor1 = Math.exp(prob)*val; //real semiring
			SignedValue factor1 =  SignedValue.multi(
					logProb,
					SignedValue.createSignedValue(val)
			);
			res = new ExpectationSemiring(logProb, factor1);
		}else{
			System.out.println("un-implemented atomic-semiring");
			System.exit(1);
		}
		return res;
	}

	
//	#######################################################################	
	public static void main(String[] args) {
		if(args.length>4){
			System.out.println("Wrong number of parameters, it must have at least four parameters: java NbestMinRiskAnnealer use_shortest_ref f_config gain_factor f_dev_src f_nbest_prefix f_dev_ref1 f_dev_ref2....");
			System.exit(1);
		}
		
		
		
		String f_dev_hg_prefix=args[0].trim();
		String f_dev_items = f_dev_hg_prefix +".items";
		String f_dev_rules = f_dev_hg_prefix +".rules";
		
		double scale = 1;
		if(args.length>=2)
			scale = new Double(args[1].trim());
		
		int num_sents =5;
		if(args.length>=3)
			num_sents = new Integer(args[2].trim());
		
		int numSrcWords =1;
		if(args.length>=4)
			numSrcWords = new Integer(args[3].trim());
		
		
		SymbolTable symbolTbl = new BuildinSymbol(null);
		int ngramStateID =0;
		
		double sumEntropy = 0;
		
		DefaultSemiringParser ds = new EntropyOnHG(1,0,scale);
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, true, null); //have model costs stored
		diskHG.initRead(f_dev_items, f_dev_rules,null);
		for(int sentID=0; sentID < num_sents; sentID ++){
			System.out.println("#Process sentence " + sentID);
			HyperGraph testHG = diskHG.readHyperGraph();			
			ds.insideEstimationOverHG(testHG);
			ExpectationSemiring goalSemiring = (ExpectationSemiring) ds.getGoalSemiringMember(testHG);
			//goal_semiring.printInfor();
			goalSemiring.normalizeFactors();
			goalSemiring.printInfor();
			double entropy = goalSemiring.getLogProb() - goalSemiring.getFactor1().convertRealValue();//logZ-E(s)/Z
			
			System.out.println("entropy is " + entropy);
			sumEntropy += entropy;
		}		
		System.out.println("scale=" + scale + "; num_sents=" + num_sents +"; numSrcWords="+numSrcWords);
		
		//a nats has 1.44 bits
		System.out.println("sum_entropy: " + scale + " " + 1.44*sumEntropy/numSrcWords);
	}
}