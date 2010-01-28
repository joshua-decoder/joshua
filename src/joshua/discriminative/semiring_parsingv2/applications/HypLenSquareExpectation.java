package joshua.discriminative.semiring_parsingv2.applications;

import java.util.ArrayList;
import java.util.logging.Logger;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.semiring_parsingv2.DefaultIOParserWithXLinearCombinator;
import joshua.discriminative.semiring_parsingv2.SignedValue;
import joshua.discriminative.semiring_parsingv2.bilinear_operator.ScalarBO;
import joshua.discriminative.semiring_parsingv2.pmodule.ExpectationSemiringPM;
import joshua.discriminative.semiring_parsingv2.pmodule.ScalarPM;
import joshua.discriminative.semiring_parsingv2.semiring.ExpectationSemiring;
import joshua.discriminative.semiring_parsingv2.semiring.LogSemiring;




public class HypLenSquareExpectation  extends DefaultIOParserWithXLinearCombinator<
ExpectationSemiring<LogSemiring,ScalarPM>,
ExpectationSemiringPM<LogSemiring,ScalarPM,ScalarPM,ScalarPM,ScalarBO>
> {
	
	private static final Logger logger = 
		Logger.getLogger(HypLenSquareExpectation.class.getName());
	
	double scale;
	static  ScalarBO pBilinearOperator = new ScalarBO();
	
	public HypLenSquareExpectation(double scale_){
		super();
		this.scale = scale_;
	}

	@Override
	protected ExpectationSemiringPM<LogSemiring, ScalarPM, ScalarPM, ScalarPM, ScalarBO> 
	createNewXWeight() {
		ScalarPM s = new ScalarPM();
		ScalarPM t = new ScalarPM();
		return new ExpectationSemiringPM<LogSemiring, ScalarPM, ScalarPM, ScalarPM, ScalarBO>(s, t, pBilinearOperator);
	}

	@Override
	protected ExpectationSemiring<LogSemiring, ScalarPM> createNewKWeight() {
		LogSemiring p = new LogSemiring();
		ScalarPM s = new ScalarPM();
		return new ExpectationSemiring<LogSemiring, ScalarPM>(p,s);
	}

	@Override
	protected ExpectationSemiringPM<LogSemiring, ScalarPM, ScalarPM, ScalarPM,ScalarBO> 
	getEdgeXWeight(HyperEdge dt, HGNode parent_item) {
		//== p
		double logProb = scale * dt.getTransitionLogP(false);
		LogSemiring p = new LogSemiring(logProb);
		
		//== r
		double val = 0;//real 
		if(dt.getRule()!=null){
			val = dt.getRule().getEnglish().length-dt.getRule().getArity();//length; real semiring
		}		
		ScalarPM r =  new ScalarPM( SignedValue.createSignedValueFromRealNumber(val) );
		
		//== s
		ScalarPM s = r;
		
		//== t
		ScalarPM t = pBilinearOperator.bilinearMulti(r, s);
		
		//s = p s
		s.multiSemiring(p);
		
		//t= p t
		t.multiSemiring(p);
		
		return new ExpectationSemiringPM<LogSemiring, ScalarPM, ScalarPM, ScalarPM, ScalarBO>(s, t, pBilinearOperator);
	}
	
	

	@Override
	protected ExpectationSemiring<LogSemiring, ScalarPM> 
	getEdgeKWeight(HyperEdge dt, HGNode parent_item) {
		//== p
		double logProb = scale * dt.getTransitionLogP(false);
		LogSemiring p = new LogSemiring(logProb);
		
		//== r
		double val = 0;//real 
		if(dt.getRule()!=null){
			val = dt.getRule().getEnglish().length-dt.getRule().getArity();//length; real semiring
		}		
		ScalarPM r =  new ScalarPM( SignedValue.createSignedValueFromRealNumber(val) );
		
		// r= p r
		r.multiSemiring(p);
		
		return new ExpectationSemiring<LogSemiring, ScalarPM>(p,r);
	}
	

	@Override
	public void normalizeGoal() {
		ExpectationSemiring<LogSemiring,ScalarPM> goalKVal = getGoalK();
		ExpectationSemiringPM<LogSemiring, ScalarPM, ScalarPM, ScalarPM, ScalarBO> goalXVal = getGoalX();
		
		//goalKVal.printInfor();
		//goalXVal.printInfor();
		
		double normConstant = goalKVal.getP().getLogValue();//p
		goalKVal.getR().multiSemiring(-normConstant);//r
		goalXVal.getS().multiSemiring(-normConstant);//s
		goalXVal.getT().multiSemiring(-normConstant);//t
		
	}
	
	
	public double getSecondOrderExpectation(){
		return getGoalX().getT().getValue().convertToRealValue();
	}


//	#######################################################################	
	public static void main(String[] args) {
		if(args.length<1){
			System.out.println("Wrong command: java HypLenSquareExpectation  f_nbest_prefix scale numSent ....");
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
		
	
		
		SymbolTable p_symbol = new BuildinSymbol(null);
		int baseline_lm_feat_id =0;
		
		ArrayList<HyperGraph> hyperGraphs = new  ArrayList<HyperGraph>();;
		
		HypLenSquareExpectation ds = new HypLenSquareExpectation(scale);
		DiskHyperGraph diskHG = new DiskHyperGraph(p_symbol, baseline_lm_feat_id, true, null); //have model costs stored
		diskHG.initRead(f_dev_items, f_dev_rules,null);
		for(int k=0;k<136; k++){
			for(int sent_id=0; sent_id < num_sents; sent_id ++){
				System.out.println("#Process sentence " + sent_id);
				HyperGraph hg_test; 
				if(k==0){	
					hg_test = diskHG.readHyperGraph();
					hyperGraphs.add(hg_test);
				}else
					hg_test = hyperGraphs.get(sent_id);
				ds.setHyperGraph(hg_test);
				ds.runInsideOutside();
				//ds.printTotalX();
				ds.normalizeGoal();
				double lenSecondOrderExpectation = ds.getSecondOrderExpectation();
					
				
				System.out.println("hyplensquireexpectation is " + lenSecondOrderExpectation);
				ds.clearState();
			}
			HypLenSquareExpectation.logger.info("numTimesCalled=" + k);
		}
		diskHG.closeReaders();
		
		
		
	}



	

	
}
