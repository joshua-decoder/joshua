package joshua.discriminative.semiring_parsingv2.applications;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.semiring_parsingv2.DefaultInsideSemiringParser;
import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.pmodule.ScalarPM;
import joshua.discriminative.semiring_parsingv2.semiring.ExpectationSemiring;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;

public class EntropyOnHG extends DefaultInsideSemiringParser<ExpectationSemiring<LogSemiring,ScalarPM>> {
	double scale;
	
	public EntropyOnHG(double scale_){
		super();
		this.scale = scale_;
	}
	
	@Override
	protected ExpectationSemiring<LogSemiring, ScalarPM> createNewKWeight() {
		LogSemiring p = new LogSemiring();
		ScalarPM r = new ScalarPM();
		return new ExpectationSemiring<LogSemiring,ScalarPM>(p, r);
	}

	@Override
	protected ExpectationSemiring<LogSemiring, ScalarPM> getEdgeKWeight(HyperEdge dt, HGNode parent_item) {
		ExpectationSemiring<LogSemiring,ScalarPM> res = null;
		
		double logProb = scale * dt.getTransitionLogP(false);
		LogSemiring p = new LogSemiring(logProb);
		
		double val = logProb;
		ScalarPM r =  new ScalarPM( SignedValue.createSignedValueFromRealNumber(val) );
		r.multiSemiring(p);
		
		res = new ExpectationSemiring<LogSemiring,ScalarPM>(p, r);
		
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
		
		
		SymbolTable p_symbol = new BuildinSymbol(null);
		int ngramStateID =0;
		
		double sumEntropy = 0;
		
		DefaultInsideSemiringParser ds = new EntropyOnHG(1.0);
		DiskHyperGraph dhg_test = new DiskHyperGraph(p_symbol, ngramStateID, true, null); //
		dhg_test.initRead(f_dev_items, f_dev_rules,null);
		for(int sent_id=0; sent_id < num_sents; sent_id ++){
			System.out.println("#Process sentence " + sent_id);
			HyperGraph hg_test = dhg_test.readHyperGraph();		
			ds.setHyperGraph(hg_test);
			ds.insideEstimationOverHG();
			ExpectationSemiring<LogSemiring,ScalarPM> goalSemiring = (ExpectationSemiring<LogSemiring,ScalarPM>) ds.getGoalK();
			//goal_semiring.printInfor();
			goalSemiring.getR().getValue().multiLogNumber(-goalSemiring.getP().getLogValue());//normalize
			goalSemiring.printInfor();
			double entropy = goalSemiring.getP().getLogValue() - goalSemiring.getR().getValue().convertToRealValue();
			System.out.println("entropy is " + entropy);
			sumEntropy += entropy;
		}		
		System.out.println("scale=" + scale + "; num_sents=" + num_sents +"; numSrcWords="+numSrcWords);
		
		//a nats has 1.44 bits
		System.out.println("sum_entropy: " + scale + " " + 1.44*sumEntropy/numSrcWords);
	}

	

}
