package joshua.discriminative.semiring_parsingv2.applications;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.semiring_parsingv2.DefaultIOParserWithXLinearCombinator;
import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.ScalarPM;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;


public class EntropyOnHGUsingIO extends DefaultIOParserWithXLinearCombinator<LogSemiring,ScalarPM>{
	double scale;
	
	public EntropyOnHGUsingIO(double scale){
		super();
		this.scale = scale;
	}
	
	@Override
	protected ScalarPM createNewXWeight() {
		return new ScalarPM();
	}	 

	@Override
	protected LogSemiring createNewKWeight() {
		return new LogSemiring();
	}

	@Override
	protected LogSemiring getEdgeKWeight(HyperEdge dt, HGNode parent_item) {
		double logProb = scale * dt.getTransitionLogP(false);
		return  new LogSemiring(logProb);		
	}

	@Override
	protected ScalarPM getEdgeXWeight(HyperEdge dt, HGNode parent_item) {
		double logProb = scale * dt.getTransitionLogP(false);
		LogSemiring p = new LogSemiring(logProb);
		
		double val = logProb;
		
		ScalarPM r =  new ScalarPM( SignedValue.createSignedValueFromRealNumber(val) );
		moduleMultiSemiring(r, p);
		
		return r;
	}


	
	
	@Override
	public void normalizeGoal() {
		LogSemiring goalKVal = getGoalK();
		ScalarPM goalX = getGoalX();
		
		//goalKVal.printInfor();
		//goalXVal.printInfor();
		
		double normConstant = goalKVal.getLogValue();//p
		goalX.getValue().multiLogNumber(-normConstant);//r
		
	}
	
	public double getEntropy(HyperGraph hg){
		
		return  getGoalK().getLogValue() - getGoalX().getValue().convertToRealValue();
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
		
		int numSents =5;
		if(args.length>=3)
			numSents = new Integer(args[2].trim());
		
		int numSrcWords =1;
		if(args.length>=4)
			numSrcWords = new Integer(args[3].trim());
		
		
		SymbolTable symbolTbl = new BuildinSymbol(null);
		int ngramStateID =0;
		
		double sumEntropy = 0;
		
		EntropyOnHGUsingIO ds = new EntropyOnHGUsingIO(1.0);
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, true, null); //have model costs stored
		diskHG.initRead(f_dev_items, f_dev_rules,null);
		for(int sentID=0; sentID < numSents; sentID ++){
			System.out.println("#Process sentence " + sentID);
			HyperGraph testHG = diskHG.readHyperGraph();		
			
			ds.setHyperGraph(testHG);
			
			ds.runInsideOutside();
			ds.printGoalX();
		
			ds.normalizeGoal();
			double entropy = ds.getEntropy(testHG);
			System.out.println("entropy is " + entropy);
			sumEntropy += entropy;
			ds.clearState();
		}		
		System.out.println("scale=" + scale + "; num_sents=" + numSents +"; numSrcWords="+numSrcWords);
		
		//a nats has 1.44 bits
		System.out.println("sum_entropy: " + scale + " " + 1.44*sumEntropy/numSrcWords);
	}

	
	
	
}
