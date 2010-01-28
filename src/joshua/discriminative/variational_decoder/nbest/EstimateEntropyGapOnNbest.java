package joshua.discriminative.variational_decoder.nbest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.discriminative.semiring_parsing.AtomicSemiring;
import joshua.discriminative.training.risk_annealer.nbest.NbestRiskGradientComputer;



public class EstimateEntropyGapOnNbest {
	
	int topN=300;

	boolean useUniqueNbest =false;
	boolean useTreeNbest = false;
	boolean addCombinedCost = true;	
	
	
	SymbolTable symbolTbl;
	KBestExtractor kbestExtractor;
	
	double scalingFactor= 1.0;

	AtomicSemiring atomicSemirng = new AtomicSemiring(1,0);
	
	public EstimateEntropyGapOnNbest(SymbolTable symbol_, double scalingFactor, int topn){
		this.scalingFactor = scalingFactor;
		this.topN = topn;
		
		this.symbolTbl = symbol_;
		this.kbestExtractor = new KBestExtractor(this.symbolTbl, this.useUniqueNbest, this.useTreeNbest, false, this.addCombinedCost,  false, true);
	}
		

	//if return_disorder_nbest=true; then a disorder nbest with sum log_prob (sent_id ||| hyp ||| empty_feature_scores ||| sum_log_prob)
	//else return 1best (just the hypotheses itself)
	public double processOneSent(HyperGraph hg, int sentenceID, boolean returnDisorderNbest){
		//### step-2: get a nbest derivations
		List<String> nbestNonUniqueStrings = new ArrayList<String>();
		kbestExtractor.lazyKBestExtractOnHG(hg, null, this.topN, sentenceID, nbestNonUniqueStrings);
		
		//### step-3: get the sum for each of the unique strings
		HashMap<String, Double> uniqueStringsSumProbTbl = new HashMap<String, Double>();
		HashMap<String, List<Double>> uniqueStringsListProbTbl = new HashMap<String, List<Double>>();
		double globalLogNorm = Double.NEGATIVE_INFINITY;
		for(String derivation_string : nbestNonUniqueStrings){
			String[] fds = derivation_string.split("\\s+\\|{3}\\s+");
			String hyp_string = fds[1];
			double log_prob = new Double(fds[fds.length-1])*scalingFactor;//normalized log prob //TODO: use inside_outside_scaling_factor here 
			
			
			//sum probablity
			Double old_sum =  (Double)uniqueStringsSumProbTbl.get(hyp_string);
			if(old_sum==null){ 
				old_sum= Double.NEGATIVE_INFINITY;//zero prob
			}
			uniqueStringsSumProbTbl.put(hyp_string, atomicSemirng.add_in_atomic_semiring(old_sum, log_prob));
			globalLogNorm = atomicSemirng.add_in_atomic_semiring(globalLogNorm, log_prob);
			 
			//list of probabilities
			List<Double> oldList  =  uniqueStringsListProbTbl.get(hyp_string);
			if(oldList==null){ 
				oldList= new ArrayList<Double>();
				uniqueStringsListProbTbl.put(hyp_string, oldList);
			}
			oldList.add(log_prob);
		}
		
		//### step-4: find the nbest or find the translation string having the best sum-probablity
		
		ArrayList<Double> listSumProbs = new ArrayList<Double>(); 
		double tGlobalSum = 0;
		double gap = 0;
		for(String hyp : uniqueStringsSumProbTbl.keySet()){//each unique string
			double sumProb   = Math.exp(uniqueStringsSumProbTbl.get(hyp) - globalLogNorm);
			tGlobalSum += sumProb;
			listSumProbs.add(sumProb);
			
			//compute H(d|y)
			double tLocalSum = 0;
			double sumLogProb = uniqueStringsSumProbTbl.get(hyp);
			List<Double> derivationProbs = uniqueStringsListProbTbl.get(hyp);
			for(int i=0; i<derivationProbs.size(); i++){
				double tProb = Math.exp( derivationProbs.get(i)-sumLogProb );
				tLocalSum += tProb;
				derivationProbs.set(i, tProb);
			}
			if(Math.abs(tLocalSum-1.0)>1e-4){System.out.println("local P is not sum to one, must be wrong; " +tLocalSum);	System.exit(1);}
			double entropyDGivenY = NbestRiskGradientComputer.computeEntropy(derivationProbs);
			gap += entropyDGivenY*sumProb;
		}
		double stringEntropy = NbestRiskGradientComputer.computeEntropy(listSumProbs);
		if(Math.abs(tGlobalSum-1.0)>1e-4){System.out.println("global P is not sum to one, must be wrong");	System.exit(1);}
	
		System.out.println("stringEntropy " + stringEntropy + "gap " + gap);
		return gap;
	}
	
	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		if(args.length!=6){
			System.out.println("Wrong number of parameters, it must be  5");
			System.exit(1);
		}		
		//long start_time = System.currentTimeMillis();		
		String f_test_items=args[0].trim();
		String f_test_rules=args[1].trim();
		int num_sents=new Integer(args[2].trim());
		//String f_1best=args[3].trim();//output
		int topN = new Integer(args[4].trim());
		double inside_outside_scaling_factor = new Double(args[5].trim()); 
	
		int baseline_lm_feat_id = 0;
		SymbolTable p_symbol = new BuildinSymbol(null);
		
		EstimateEntropyGapOnNbest cruncher = new EstimateEntropyGapOnNbest(p_symbol, inside_outside_scaling_factor, topN);		
	
		//BufferedWriter t_writer_1best =	FileUtilityOld.getWriteFileStream(f_1best);	
		
		System.out.println("############Process file  " + f_test_items);
		DiskHyperGraph dhg_test = new DiskHyperGraph(p_symbol, baseline_lm_feat_id, true, null); //have model costs stored
		dhg_test.initRead(f_test_items, f_test_rules,null);		
		double sumGap = 0;
		for(int sent_id=0; sent_id < num_sents; sent_id ++){
			System.out.println("#Process sentence " + sent_id);
			HyperGraph hg_test = dhg_test.readHyperGraph();			
			double gap = cruncher.processOneSent(hg_test, sent_id, false);//produce the reranked onebest
			sumGap += gap;
		}
		sumGap *= 1.44;
		System.out.println("sum of bits in gap is: " +  sumGap);
		//FileUtilityOld.close_write_file(t_writer_1best);				
	}
}
