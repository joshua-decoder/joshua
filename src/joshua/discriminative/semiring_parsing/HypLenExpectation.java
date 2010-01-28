package joshua.discriminative.semiring_parsing;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

public class HypLenExpectation extends DefaultSemiringParser {

	public HypLenExpectation(int semiring, int add_mode, double scale) {
		super(semiring, add_mode, scale);
		// TODO Auto-generated constructor stub
	}
	
	protected ExpectationSemiring createNewSemiringMember() {
		return new ExpectationSemiring();
	}
	
	//example for length of strings
	protected ExpectationSemiring getHyperedgeSemiringWeight(HyperEdge dt, HGNode parent_item,  double scale, AtomicSemiring p_atomic_semiring){
		ExpectationSemiring res = null;
		if(p_atomic_semiring.ATOMIC_SEMIRING==AtomicSemiring.LOG_SEMIRING){
			double logProb = scale * dt.getTransitionLogP(false);
			double val = 0;//TODO real semiring
			if(dt.getRule()!=null){
				val = dt.getRule().getEnglish().length-dt.getRule().getArity(); //length				
			}
			//double factor1 = Math.exp(logProb)*val; //TODO real semiring
			SignedValue factor1 =  SignedValue.multi(
												logProb,
												SignedValue.createSignedValue(val)
											);
			
			//System.out.println("factor 1: " +factor1);
			res = new ExpectationSemiring(logProb, factor1);
		}else{
			System.out.println("un-implemented atomic-semiring");
			System.exit(1);
		}
		return res;
	}

	
//	#######################################################################	
	public static void main(String[] args) {
		if(args.length!=1){
			System.out.println("Wrong number of parameters, it must have at least four parameters: java NbestMinRiskAnnealer use_shortest_ref f_config gain_factor f_dev_src f_nbest_prefix f_dev_ref1 f_dev_ref2....");
			System.exit(1);
		}
		
		
		
		String f_dev_hg_prefix=args[0].trim();
		String f_dev_items = f_dev_hg_prefix +".items";
		String f_dev_rules = f_dev_hg_prefix +".rules";
		
		SymbolTable p_symbol = new BuildinSymbol(null);
		int baseline_lm_feat_id =0;
		int num_sents =5;
		double scale=1.0;
		DefaultSemiringParser ds = new HypLenExpectation(1,0,scale);
		DiskHyperGraph dhg_test = new DiskHyperGraph(p_symbol, baseline_lm_feat_id, true, null); //have model costs stored
		dhg_test.initRead(f_dev_items, f_dev_rules,null);
		for(int sent_id=0; sent_id < num_sents; sent_id ++){
			System.out.println("#Process sentence " + sent_id);
			HyperGraph hg_test = dhg_test.readHyperGraph();			
			ds.insideEstimationOverHG(hg_test);
			CompositeSemiring goalSemiring = ds.getGoalSemiringMember(hg_test);
			goalSemiring.normalizeFactors();
			goalSemiring.printInfor();
		}		
	}

}
