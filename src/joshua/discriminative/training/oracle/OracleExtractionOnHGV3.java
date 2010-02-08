package joshua.discriminative.training.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.BLEU;
import joshua.decoder.Support;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;


public class OracleExtractionOnHGV3 extends RefineHG<DPStateOracle> {
	
	SymbolTable symbolTable;
	
	protected  int srcSentLen =0;
	EquivLMState equiv;
	
	static protected boolean doLocalNgramClip =false;
	static protected boolean maitainLengthState = false;
	static protected  int bleuOrder = 4;
	
	
	//== the way to compute effective reference length
	boolean useShortestRef = false; //TODO
	
	//derived from reference sentence
	protected HashMap<String, Integer> refNgramsTbl = new HashMap<String, Integer>();
	protected double refSentLen =0;
	

	
	public OracleExtractionOnHGV3(SymbolTable symbolTable ) {
		this.symbolTable = symbolTable;
		
		this.equiv = new EquivLMState(this.symbolTable, bleuOrder);
	}

	
//	find the oracle hypothesis in the nbest list
	public Object[] oracleExtractOnNbest(KBestExtractor kbest_extractor, HyperGraph hg, int n, boolean do_ngram_clip, String ref_sent){
		
		if(hg.goalNode==null) 
			return null;
		kbest_extractor.resetState();				
		int next_n=0;
		double orc_bleu=-1;
		String orc_sent=null;
		while(true){
			String hyp_sent = kbest_extractor.getKthHyp(hg.goalNode, ++next_n, -1, null, null);//?????????
			//System.out.println(hyp_sent);
			if(hyp_sent==null || next_n > n) break;
			double t_bleu = computeSentenceBleu(this.symbolTable, ref_sent, hyp_sent, do_ngram_clip, 4);
			if(t_bleu>orc_bleu){
				orc_bleu = t_bleu;
				orc_sent = hyp_sent;
			}			
		}
		System.out.println("Oracle sent in nbest: " + orc_sent);
		System.out.println("Oracle bleu in nbest: " + orc_bleu);
		Object[] res = new Object[2];
		res[0]=orc_sent;
		res[1]=orc_bleu;
		return res;
	}
	
	
	public HyperGraph oracleExtractOnHG(HyperGraph hg, int srcSentLenIn,  int baselineLMOrder, String refSentStr){
//TODO: baselineLMOrder
		
		srcSentLen = srcSentLenIn;
		
		//== ref tbL and effective ref len
		int[] refWords = this.symbolTable.addTerminals(refSentStr.split("\\s+"));
		
		refSentLen = refWords.length;				
		refNgramsTbl.clear();
		getNgrams(refNgramsTbl, bleuOrder, refWords, false);
		
		equiv.setupPrefixAndSurfixTbl(refNgramsTbl);
		
		HyperGraph res= splitHG(hg);
		
		return res;
	}
	
	public HyperGraph oracleExtractOnHG(HyperGraph hg, int srcSentLenIn,  int baselineLMOrder, String[] refSentStrs){
//		TODO: baselineLMOrder
				
		srcSentLen = srcSentLenIn;
		//== ref tbL and effective ref len
		int[] refLens = new int[refSentStrs.length];
		ArrayList<HashMap<String, Integer>> listRefNgramTbl = new ArrayList<HashMap<String, Integer>>();
		
		for(int i =0; i<refSentStrs.length; i++){
			int[] refWords = this.symbolTable.addTerminals(refSentStrs[i].split("\\s+"));
			refLens[i] = refWords.length;		
			HashMap<String, Integer> tRefNgramsTbl = new HashMap<String, Integer>();
			getNgrams(tRefNgramsTbl, bleuOrder, refWords, false);	
			listRefNgramTbl.add(tRefNgramsTbl);			
		}
		refSentLen = BLEU.computeEffectiveLen(refLens, useShortestRef);
		refNgramsTbl =  BLEU.computeMaxRefCountTbl(listRefNgramTbl);
		
		equiv.setupPrefixAndSurfixTbl(refNgramsTbl);
		
		HyperGraph res= splitHG(hg);
		
		return res;
	}
			
	
		
	
	private double computeAvgLen(int spanLen, int srcSentLen, double refSentLen){
		return (spanLen>=srcSentLen) ? refSentLen :  spanLen*refSentLen*1.0/srcSentLen;//avg len?
	}


	

	@Override
	protected HyperEdge createNewHyperEdge(HyperEdge originalEdge, List<HGNode> antVirtualItems, DPStateOracle dps) {
		/**compared wit the original edge, two changes:
		 * (1) change the list of ant nodes
		 * (2) change the transition logProb to BLEU from Model logProb
		 * */
		return new HyperEdge(originalEdge.getRule(), dps.bestDerivationLogP, null, antVirtualItems, originalEdge.getSourcePath());
	}

	


	/*This procedure does
	 * (1) create a new hyperedge (based on curEdge and ant_virtual_item)
	 * (2) find whether an Item can contain this hyperedge (based on virtualItemSigs which is a hashmap specific to a parent_item)
	 * 	(2.1) if yes, add the hyperedge, 
	 *  (2.2) otherwise
	 *  	(2.2.1) create a new item
	 *		(2.2.2) and add the item into virtualItemSigs
	 **/

	protected  DPStateOracle computeState(HGNode originalParentItem, HyperEdge originalEdge, List<HGNode> antVirtualItems){
		
		double refLen = computeAvgLen(originalParentItem.j-originalParentItem.i, srcSentLen, refSentLen);
		
		//=== hypereges under "goal item" does not have rule
		if(originalEdge.getRule()==null){
			if(antVirtualItems.size()!=1){
				System.out.println("error deduction under goal item have more than one item"); 
				System.exit(0);
			}
			double bleu = antVirtualItems.get(0).bestHyperedge.bestDerivationLogP;
			return  new DPStateOracle(0, null, null, null, bleu);//no DPState at all
			 
		}
		
		
		ComputeOracleStateResult lmState = computeLMState(originalEdge, antVirtualItems);	
			
		int hypLen = lmState.numNewWordsAtEdge;
		
		int[] numNgramMatches = new int[bleuOrder];
		
		Iterator iter = lmState.newNgramsTbl.keySet().iterator();
    	while(iter.hasNext()){
    		String ngram = (String)iter.next();
    		
			int finalCount =  lmState.newNgramsTbl.get(ngram);
			if(doLocalNgramClip)
				numNgramMatches[ngram.split("\\s+").length-1] += Support.findMin(finalCount, refNgramsTbl.get(ngram)) ;
			else 
				numNgramMatches[ngram.split("\\s+").length-1] += finalCount; //do not do any cliping    			
    	}
		
    	if(antVirtualItems!=null){
			for(int i=0; i<antVirtualItems.size(); i++){
				DPStateOracle antDPState = (DPStateOracle)((RefinedNode)antVirtualItems.get(i)).dpState;    			
				hypLen += antDPState.bestLen;
				for(int t=0; t<bleuOrder; t++)
					numNgramMatches[t] += antDPState.ngramMatches[t];
			}
    	}
		double bleu = computeBleu(hypLen, refLen, numNgramMatches, bleuOrder);

		return  new DPStateOracle(hypLen, numNgramMatches, lmState.leftEdgeWords, lmState.rightEdgeWords, bleu);
	}
	

	protected  ComputeOracleStateResult computeLMState(HyperEdge dt, List<HGNode> antVirtualItems){	
		
		//=== hypereges under "goal item" does not have rule
		if(dt.getRule()==null){
			/**TODO: we did not consider <s> and </s> here
			 * */
			HashMap<String, Integer> finalNewGramCounts = null;
			int hypLen = 0;
			return  new ComputeOracleStateResult(finalNewGramCounts, null, null, hypLen);
		}
		
		//======== hypereges *not* under "goal item"		
		HashMap<String, Integer> newGramCounts = new HashMap<String, Integer>();//new ngrams created due to the combination
		HashMap<String, Integer> oldNgramCounts = new HashMap<String, Integer>();//the ngram that has already been computed
		int hypLen =0;
		int[] enWords = dt.getRule().getEnglish();
		
		
    	List<Integer> words= new ArrayList<Integer>();
    	List<Integer> leftStateSequence =  new ArrayList<Integer>();
    	List<Integer> rightStateSequence = new ArrayList<Integer>();
  
    	
    	//==== get left_state_sequence, right_state_sequence, total_hyp_len, num_ngram_match
    	for(int c=0; c<enWords.length; c++){
    		
    		int word = enWords[c];
    		if(symbolTable.isNonterminal(word)==true){
    			
    			int index = this.symbolTable.getTargetNonterminalIndex(word);
    			
    			DPStateOracle antState = (DPStateOracle)((RefinedNode)antVirtualItems.get(index)).dpState;//TODO    			
    		     	  			
    			List<Integer> leftContext = antState.leftLMState;
    			List<Integer> rightContext = antState.rightLMState;
    			
    			for(int t : leftContext){//always have l_context
    				words.add(t);
    				if(leftStateSequence!=null && leftStateSequence.size()<bleuOrder-1) 
    					leftStateSequence.add(t);
    			}
    			getNgrams(oldNgramCounts, bleuOrder, leftContext, true);   
    			
    			if(rightContext.size()>=bleuOrder-1){//the right and left are NOT overlapping	    	
    				getNgrams(newGramCounts, bleuOrder, words, true);
    				getNgrams(oldNgramCounts, bleuOrder, rightContext, true);
	    			words.clear();//start a new chunk    
	    			if(rightStateSequence!=null)
	    				rightStateSequence.clear();
	    			for(int t : rightContext)
	    				words.add(t);	    			
	    		}
    			if(rightStateSequence!=null)
    				for(int t : rightContext)
    					rightStateSequence.add(t);
    		}else{
    			words.add(word);
    			hypLen++;
    			if(leftStateSequence!=null && leftStateSequence.size()<bleuOrder-1)
    				leftStateSequence.add(word);
    			if(rightStateSequence!=null) 
    				rightStateSequence.add(word);
    		}
    	}
    	getNgrams(newGramCounts, bleuOrder, words, true);
    
    	//=== now deduct ngram counts
    	HashMap<String, Integer> finalNewGramCounts = new HashMap<String, Integer>();
    	Iterator iter = newGramCounts.keySet().iterator();
    	//System.out.println("new size= " + newGramCounts.size());
    	//System.out.println("old size= " + oldNgramCounts.size());
    	while(iter.hasNext()){
    	
    		String ngram = (String)iter.next();
    		if(refNgramsTbl.containsKey(ngram)){//TODO
	    		int finalCount = newGramCounts.get(ngram);
	    		if(oldNgramCounts.containsKey(ngram)){
	    			finalCount -= oldNgramCounts.get(ngram);
	    			if(finalCount<0){
	    				System.out.println("error: negative count for ngram: "+ this.symbolTable.getWord(11844) + "; new: " + newGramCounts.get(ngram) +"; old: " +oldNgramCounts.get(ngram) ); 
	    				System.exit(0);
	    			}
	    		}
	    		if(finalCount>0){
    				finalNewGramCounts.put(ngram, finalCount);	    				
    			}
    		}
    	}
    	
    	List<Integer> leftLMState = equiv.getLeftEquivState(leftStateSequence);
    	List<Integer> rightLMState =  equiv.getRightEquivState(rightStateSequence); 		
		
    	ComputeOracleStateResult res = new  ComputeOracleStateResult(finalNewGramCounts, leftLMState, rightLMState, hypLen);
    	//res.printInfo();
    	
		return  res;
	}
	
 
	
	
	
	
	
//	=================================================================================================
	//==================== BLEU-related functions ==========================================
	//=================================================================================================
	//TODO: consider merge with joshua.decoder.BLEU
	
	//do_ngram_clip: consider global n-gram clip
	public  double computeSentenceBleu(SymbolTable p_symbol, String ref_sent, String hyp_sent, boolean do_ngram_clip, int bleu_order){
		int[] numeric_ref_sent = p_symbol.addTerminals(ref_sent.split("\\s+"));
		int[] numeric_hyp_sent = p_symbol.addTerminals(hyp_sent.split("\\s+"));
		return computeSentenceBleu(numeric_ref_sent, numeric_hyp_sent, do_ngram_clip, bleu_order);		
	}
	
	
	public  double computeSentenceBleu( int[] ref_sent, int[] hyp_sent, boolean do_ngram_clip, int bleu_order){
		double res_bleu = 0;
		int order =4;
		HashMap<String, Integer>  ref_ngram_tbl = new HashMap<String, Integer> ();
		getNgrams(ref_ngram_tbl, order, ref_sent,false);
		HashMap<String, Integer>  hyp_ngram_tbl = new HashMap<String, Integer> ();
		getNgrams(hyp_ngram_tbl, order, hyp_sent,false);
		
		int[] num_ngram_match = new int[order];
		for(Iterator it = hyp_ngram_tbl.keySet().iterator(); it.hasNext();){
			String ngram = (String) it.next();
			if(ref_ngram_tbl.containsKey(ngram)){
				if(do_ngram_clip)
					num_ngram_match[ngram.split("\\s+").length-1] += Support.findMin(ref_ngram_tbl.get(ngram),hyp_ngram_tbl.get(ngram)); //ngram clip
				else
					num_ngram_match[ngram.split("\\s+").length-1] += hyp_ngram_tbl.get(ngram);//without ngram count clipping    			
    		}
		}
		res_bleu = computeBleu(hyp_sent.length, ref_sent.length, num_ngram_match, bleu_order);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);

		return res_bleu;
	}
		
	
//	sentence-bleu: BLEU= bp * prec; where prec = exp (sum 1/4 * log(prec[order]))
	public static double computeBleu(int hypLen, double refLen, int[] numNgramMatches, int bleuOrder){
		if(hypLen<=0 || refLen<=0){
			System.out.println("error: ref or hyp is zero len"); 
			System.exit(0);
		}
		
		double res=0;		
		double wt = 1.0/bleuOrder;
		double prec = 0;
		double smoothFactor=1.0;
		for(int t=0; t<bleuOrder && t<hypLen; t++){
			if(numNgramMatches[t]>0)
				prec += wt*Math.log(numNgramMatches[t]*1.0/(hypLen-t));
			else{
				smoothFactor *= 0.5;//TODO
				prec += wt*Math.log(smoothFactor/(hypLen-t));
			}
		}
		
		double bp = (hypLen>=refLen) ? 1.0 : Math.exp(1-refLen/hypLen);	
		
		res = bp*Math.exp(prec);
		//System.out.println("hyp_len: " + hyp_len + "; ref_len:" + ref_len + "prec: " + Math.exp(prec) + "; bp: " + bp + "; bleu: " + res);
		return res;
	}




	//=================================================================================================
	//==================== ngram extraction functions ==========================================
	//=================================================================================================
	protected void getNgrams(HashMap<String, Integer> tbl, int order, int[] wrds, boolean ignoreNullEquivSymbol){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				boolean contain_null=false;
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					
					ngram.append(wrds[k]);
					if(k<i+j) ngram.append(" ");
				}
				if(ignoreNullEquivSymbol && contain_null) 
					continue;//skip this ngram
				String ngram_str = ngram.toString();
				if(tbl.containsKey(ngram_str))
					tbl.put(ngram_str,  tbl.get(ngram_str)+1);
				else
					tbl.put(ngram_str, 1);
			}
	}
	
//	accumulate ngram counts into tbl
	protected void getNgrams(HashMap<String, Integer>  tbl, int order, List<Integer> wrds, boolean ignoreNullEquivSymbol){
		
		for(int i=0; i<wrds.size(); i++)
			for(int j=0; j<order && j+i<wrds.size(); j++){//ngram: [i,i+j]
				boolean contain_null=false;
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds.get(k);
					
					ngram.append(t_wrd);
					if(k<i+j) ngram.append(" ");
				}
				if(ignoreNullEquivSymbol && contain_null) 
					continue;//skip this ngram
				String ngram_str = ngram.toString();
				if(tbl.containsKey(ngram_str))
					tbl.put(ngram_str, tbl.get(ngram_str)+1);
				else
					tbl.put(ngram_str, 1);
			}
	}
	
	

}
