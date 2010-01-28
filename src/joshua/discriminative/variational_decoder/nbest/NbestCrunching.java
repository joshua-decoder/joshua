package joshua.discriminative.variational_decoder.nbest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.semiring_parsing.AtomicSemiring;



public class NbestCrunching {
	
	int topN=300;
	boolean useUniqueNbest =false;
	boolean useTreeNbest = false;//still produce string, though the p(y)=p(y(d))
	boolean addCombinedCost = true;	
	
	
	SymbolTable symbolTbl;
	KBestExtractor kbestExtractor;
	
	double scalingFactor= 1.0;

	AtomicSemiring atomicSemirng = new AtomicSemiring(1,0);
	
	public NbestCrunching(SymbolTable symbolTbl, double insideOutsideScalingFactor, int topN){
		this.scalingFactor = insideOutsideScalingFactor;
		this.topN = topN;
		
		this.symbolTbl = symbolTbl;
		this.kbestExtractor = new KBestExtractor(this.symbolTbl, this.useUniqueNbest, this.useTreeNbest, false, this.addCombinedCost,  false, true);
	}
		

	//if return_disorder_nbest=true; then a disorder nbest with sum log_prob (sent_id ||| hyp ||| empty_feature_scores ||| sum_log_prob)
	//else return 1best (just the hypotheses itself)
	public List<String> processOneSent(HyperGraph hg, int sentenceID, boolean returnDisorderNbest){
		
		List<String> result = new ArrayList<String> ();
		
		/*
		//### step-1: run inside-outside, rank the hg, and get normalization constant
		//note, inside and outside will use the transition_cost of each hyperedge, this cost is already linearly interpolated
		TrivialInsideOutside p_inside_outside = new TrivialInsideOutside();
		p_inside_outside.run_inside_outside(hg, 0, 1, inside_outside_scaling_factor);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		double norm_constant = p_inside_outside.get_normalization_constant();	
		p_inside_outside.clear_state();
		*/
			
		//### step-2: get a nbest derivations
		List<String> nonUniqueNbestStrings = new ArrayList<String>();
		kbestExtractor.lazyKBestExtractOnHG(hg, null, this.topN, sentenceID, nonUniqueNbestStrings);
		
		//### step-3: get the sum for each of the unique strings
		HashMap<String, Double> uniqueStringsSumProbTbl = new HashMap<String, Double>();
		HashMap<String, Double> uniqueStringsViterbiProbTbl = new HashMap<String, Double>();//debug
		HashMap<String, Integer> uniqueStringsNumDuplicatesTbl = new HashMap<String, Integer>();//debug
		
		for(String derivationString : nonUniqueNbestStrings){
			//System.out.println(derivation_string);
			String[] fds = derivationString.split("\\s+\\|{3}\\s+");
			String hypString = fds[1];
			double logProb = new Double(fds[fds.length-1])*scalingFactor;//normalized log prob //TODO: use inside_outside_scaling_factor here 
			Double oldSum =  (Double)uniqueStringsSumProbTbl.get(hypString);
			if(oldSum==null){ 
				oldSum= Double.NEGATIVE_INFINITY;//zero prob
				uniqueStringsNumDuplicatesTbl.put(hypString, 1);
				uniqueStringsViterbiProbTbl.put(hypString, logProb);
			}else{
				uniqueStringsNumDuplicatesTbl.put(hypString, uniqueStringsNumDuplicatesTbl.get(hypString)+1);
			}
			uniqueStringsSumProbTbl.put(hypString, atomicSemirng.add_in_atomic_semiring(oldSum, logProb));
		}
		
		//### step-4: find the nbest or find the translation string having the best sum-probablity
		if(returnDisorderNbest){			
			for(String hyp : uniqueStringsSumProbTbl.keySet()){
				StringBuffer fullHyp = new StringBuffer();
				fullHyp.append(sentenceID); fullHyp.append(" ||| ");
				fullHyp.append(hyp); fullHyp.append(" ||| ");
				fullHyp.append("empty_feature_scores"); fullHyp.append(" ||| ");
				fullHyp.append(uniqueStringsSumProbTbl.get(hyp));
				result.add(fullHyp.toString());
				//System.out.println(full_hyp.toString());
			}
			System.out.println("n_derivations=" + nonUniqueNbestStrings.size() + "; n_strings=" + uniqueStringsSumProbTbl.size());
		}else{
			double bestSumProb = Double.NEGATIVE_INFINITY;
			String bestString = null;			
			double sumProb = Double.NEGATIVE_INFINITY;;
					
			for(String hyp : uniqueStringsSumProbTbl.keySet()){
				sumProb   = uniqueStringsSumProbTbl.get(hyp);
			
				if(sumProb > bestSumProb){
					bestSumProb = sumProb;			
					bestString = hyp;
				}
				//System.out.println(sentenceID + " ||| " +  hyp +" ||| " + n_duplicates + " ||| " + viter_prob + " ||| "  + sum_prob);//+ " ||| " + Math.exp(max_log_prob-viter_prob)
			}
			System.out.println(sentenceID + " ||| " +  bestString +" ||| " + bestSumProb);//un-normalized logProb
			result.add(bestString);
		}
		return result;
	}
	
	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		
		if(args.length!=6){
			System.out.println("Wrong number of parameters, it must be  5");
			System.exit(1);
		}		
				
		String testItemsFile=args[0].trim();
		String testRulesFile=args[1].trim();
		int numSents=new Integer(args[2].trim());
		String onebestFile=args[3].trim();//output
		int topN = new Integer(args[4].trim());
		double insideOutsideScalingFactor = new Double(args[5].trim()); 
	
		int ngramStateID = 0;
		SymbolTable symbolTbl = new BuildinSymbol(null);
		
		NbestCrunching cruncher = new NbestCrunching(symbolTbl, insideOutsideScalingFactor, topN);		
	
		BufferedWriter onebestWriter =	FileUtilityOld.getWriteFileStream(onebestFile);	
		
		System.out.println("############Process file  " + testItemsFile);
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, true, null); //have model costs stored
		diskHG.initRead(testItemsFile, testRulesFile,null);			
		for(int sentID=0; sentID < numSents; sentID ++){
			System.out.println("#Process sentence " + sentID);
			HyperGraph testHG = diskHG.readHyperGraph();			
			List<String> oneBest = cruncher.processOneSent(testHG, sentID, false);//produce the reranked onebest
			FileUtilityOld.writeLzf(onebestWriter, oneBest.get(0) + "\n");
		}
		FileUtilityOld.closeWriteFile(onebestWriter);				
	}
}
