/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.discriminative.training.oracle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;





import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.Support;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.util.FileUtility;

/**
 * approximated BLEU
 * (1) do not consider clipping effect
 * (2) in the dynamic programming, do not maintain different states for different hyp length
 * (3) brief penalty is calculated based on the avg ref length
 * (4) using sentence-level BLEU, instead of doc-level BLEU
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com> (Johns Hopkins University)
 * @version $LastChangedDate: 2009-04-02 15:34:43 -0400 $
 */
public class OracleExtractionOnHGV2 extends RefineHG<DPStateOracle> {
	
	static String BACKOFF_LEFT_LM_STATE_SYM="<lzfbo>";
	public int BACKOFF_LEFT_LM_STATE_SYM_ID;//used for equivelant state
	
	static String NULL_LEFT_LM_STATE_SYM="<lzflnull>";
	public int NULL_LEFT_LM_STATE_SYM_ID;//used for equivelant state
	
	static String NULL_RIGHT_LM_STATE_SYM="<lzfrnull>";
	public int NULL_RIGHT_LM_STATE_SYM_ID;//used for equivelant state
	
	
//	int[] ref_sentence;//reference string (not tree)
	protected  int srcSentLen =0;
	protected  int refSentLen =0;
	protected  int lmOrder=4; //only used for decide whether to get the LM state by this class or not in compute_state
	static protected boolean doLocalNgramClip =false;
	static protected boolean maitainLengthState = false;
	static protected  int bleuOrder=4;
	
	static boolean useLeftEquivState = true;
	static boolean useRightEquivState = true;
	
	HashMap<String, Boolean> suffixTbl = new HashMap<String, Boolean>();
	HashMap<String, Boolean> prefixTbl = new HashMap<String, Boolean>();
	
	static PrefixGrammar prefixGrammar = new PrefixGrammar();//TODO
	static PrefixGrammar suffixGrammar = new PrefixGrammar();//TODO
	
	
	
	protected HashMap<String, Integer> refNgramsTbl = new HashMap<String, Integer>();
	

	static boolean alwaysMaintainSeperateLMState = true; //if true: the virtual item maintain its own lm state regardless whether lm_order>=g_bleu_order
	
	/**
	 * 
	 */
	SymbolTable symbolTable;
	
	int ngramStateID=0; //the baseline LM feature id
	
	/**
	 * Constructs a new object capable of extracting
	 * a tree from a hypergraph that most closely matches
	 * a provided oracle sentence. 
	 * @param symbolTable_ 
	 * @param lmFeatID_
	 */
	public OracleExtractionOnHGV2(SymbolTable symbolTable_, int lmFeatID_){
		this.symbolTable = symbolTable_;
		this.ngramStateID = lmFeatID_;
		this.BACKOFF_LEFT_LM_STATE_SYM_ID = symbolTable.addTerminal(BACKOFF_LEFT_LM_STATE_SYM);
		this.NULL_LEFT_LM_STATE_SYM_ID = symbolTable.addTerminal(NULL_RIGHT_LM_STATE_SYM);
		this.NULL_RIGHT_LM_STATE_SYM_ID = symbolTable.addTerminal(NULL_RIGHT_LM_STATE_SYM);
	}
	
	
	
	//find the oracle hypothesis in the nbest list
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

	
	public HyperGraph oracleExtractOnHG(HyperGraph hg, int srcSentLenIn, int lmOrder_,  String refSentStr){
		
		int[] refSent = this.symbolTable.addTerminals(refSentStr.split("\\s+"));
		lmOrder= lmOrder_;		
		srcSentLen = srcSentLenIn;
		refSentLen = refSent.length;		
		
		refNgramsTbl.clear();
		getNgrams(refNgramsTbl,bleuOrder,refSent, false);	
		if(useLeftEquivState || useRightEquivState){
			prefixTbl.clear();	suffixTbl.clear();
			setupPrefixSuffixTbl(refSent,  bleuOrder, prefixTbl, suffixTbl);
			setupPrefixSuffixGrammar(refSent,  bleuOrder, prefixGrammar, suffixGrammar);//TODO
		}
		
		return splitHG(hg);
	}
		
	private double computeAvgLen(int spanLen, int srcSentLen, int refSentLen){
		return (spanLen>=srcSentLen) ? refSentLen :  spanLen*refSentLen*1.0/srcSentLen;//avg len?
	}

	

	@Override
	protected HyperEdge createNewHyperEdge(HyperEdge originalEdge, List<HGNode> antVirtualItems, DPStateOracle dps) {
		return new HyperEdge(originalEdge.getRule(), dps.bestDerivationLogP, null, antVirtualItems, originalEdge.getSourcePath());
	}


		
//	=================== commmon funcions ==========================
	//based on tbl_oracle_states, tbl_ref_ngrams, and dt, get the state
	//get the new state: STATE_BEST_DEDUCT STATE_BEST_BLEU STATE_BEST_LEN NGRAM_MATCH_COUNTS
	protected  DPStateOracle computeState(HGNode parentNode, HyperEdge dt, List<HGNode> antVirtualItems){	
		double refLen = computeAvgLen(parentNode.j-parentNode.i, srcSentLen, refSentLen);
		
		//=== hypereges under "goal item" does not have rule
		if(dt.getRule()==null){
			if(antVirtualItems.size()!=1){
				System.out.println("error deduction under goal item have more than one item"); 
				System.exit(0);
			}
			double bleu = antVirtualItems.get(0).bestHyperedge.bestDerivationLogP;
			return  new DPStateOracle(0, null, null,null, bleu);//no DPState at all
		}
		
		//======== hypereges *not* under "goal item"		
		HashMap<String, Integer> newNgramCounts = new HashMap<String, Integer>();//new ngrams created due to the combination
		HashMap<String, Integer> oldNgramCounts = new HashMap<String, Integer>();//the ngram that has already been computed
		int hypLen =0;
		int[] numNgramMatches = new int[bleuOrder];
		int[] enWords = dt.getRule().getEnglish();
		
		//=== calulate new and old ngram counts, and len
    	ArrayList<Integer> words= new ArrayList<Integer>();
    	ArrayList<Integer> leftStateSequence = null; //used for compute left-lm state
    	ArrayList<Integer> rightStateSequence = null; //used for compute right-lm state
    	int correctLMOrder = lmOrder;
    	if(alwaysMaintainSeperateLMState==true || lmOrder<bleuOrder) {
    		leftStateSequence = new ArrayList<Integer>();
    		rightStateSequence = new ArrayList<Integer>();
    		correctLMOrder = bleuOrder;//if lm_order is smaller than g_bleu_order, we will get the lm state by ourself
    	}
    	
    	//==== get leftStateSequence, rightStateSequence, hypLen, num_ngram_match
    	for(int c=0; c<enWords.length; c++){
    		
    		int c_id = enWords[c];
    		if(symbolTable.isNonterminal(c_id)==true){
    			
    			int index=this.symbolTable.getTargetNonterminalIndex(c_id);
    			DPStateOracle antDPState = (DPStateOracle)((RefinedNode)antVirtualItems.get(index)).dpState;    			
    			hypLen += antDPState.bestLen;
    			for(int t=0; t<bleuOrder; t++)
    				numNgramMatches[t] += antDPState.ngramMatches[t];
    	  			
    			List<Integer> l_context = antDPState.leftLMState;
    			List<Integer> r_context = antDPState.rightLMState;
    			
    			for(int t : l_context){//always have l_context
    				words.add(t);
    				if(leftStateSequence!=null && leftStateSequence.size()<bleuOrder-1) 
    					leftStateSequence.add(t);
    			}
    			getNgrams(oldNgramCounts, bleuOrder, l_context, true);    			
    			if(r_context.size()>=correctLMOrder-1){//the right and left are NOT overlapping	    	
    				getNgrams(newNgramCounts, bleuOrder, words, true);
    				getNgrams(oldNgramCounts, bleuOrder, r_context, true);
	    			words.clear();//start a new chunk    
	    			if(rightStateSequence!=null)rightStateSequence.clear();
	    			for(int t : r_context)
	    				words.add(t);	    			
	    		}
    			if(rightStateSequence!=null)
    				for(int t : r_context)
    					rightStateSequence.add(t);
    		}else{
    			words.add(c_id);
    			hypLen += 1;
    			
    			if(leftStateSequence!=null && leftStateSequence.size()<bleuOrder-1)
    				leftStateSequence.add(c_id);
    			
    			if(rightStateSequence!=null) 
    				rightStateSequence.add(c_id);
    		}
    	}
    	getNgrams(newNgramCounts, bleuOrder, words, true);
    
    	//=== now deduct ngram counts
    	Iterator iter = newNgramCounts.keySet().iterator();
    	while(iter.hasNext()){
    		String ngram = (String)iter.next();
    		if(refNgramsTbl.containsKey(ngram)){
	    		int finalCount = newNgramCounts.get(ngram);
	    		if(oldNgramCounts.containsKey(ngram)){
	    			finalCount -= oldNgramCounts.get(ngram);
	    			if(finalCount<0){
	    				System.out.println("error: negative count for ngram: "+ this.symbolTable.getWord(11844) + "; new: " + newNgramCounts.get(ngram) +"; old: " +oldNgramCounts.get(ngram) ); 
	    				System.exit(0);
	    			}
	    		}
	    		if(finalCount>0){//TODO: not correct/global ngram clip
	    			if(doLocalNgramClip)
	    				numNgramMatches[ngram.split("\\s+").length-1] += Support.findMin(finalCount, refNgramsTbl.get(ngram)) ;
	    			else 
	    				numNgramMatches[ngram.split("\\s+").length-1] += finalCount; //do not do any cliping    			
	    		}
    		}
    	}
    	
    	//=== now calculate the BLEU score and state
    	List<Integer> leftLMState = null;
    	List<Integer>  rightLMState= null;
		if(alwaysMaintainSeperateLMState==false && lmOrder>=bleuOrder){	//do not need to change lm state, just use orignal lm state
			NgramDPState state     = (NgramDPState) parentNode.getDPState(this.ngramStateID);
			leftLMState = state.getLeftLMStateWords();
			rightLMState = state.getRightLMStateWords();
		}else{
			leftLMState = getLeftEquivState(leftStateSequence, suffixTbl);
			rightLMState = getRightEquivState(rightStateSequence, prefixTbl); 
			
			//debug
			//System.out.println("lm_order is " + lm_order);
			//compare_two_int_arrays(left_lm_state, (int[])parent_item.tbl_states.get(Symbol.LM_L_STATE_SYM_ID));
			//compare_two_int_arrays(right_lm_state, (int[])parent_item.tbl_states.get(Symbol.LM_R_STATE_SYM_ID));
			//end						
		}
		
		double bleu = computeBleu(hypLen, refLen, numNgramMatches, bleuOrder);
		
		return  new DPStateOracle(hypLen, numNgramMatches, leftLMState, rightLMState, bleu);
	}
	
	
	private List<Integer> getLeftEquivState(List<Integer> leftStateSequence, HashMap<String, Boolean> suffixTbl){
		
		int l_size = (leftStateSequence.size()<bleuOrder-1)? leftStateSequence.size() : (bleuOrder-1);
		
		if(useLeftEquivState==false || l_size<bleuOrder-1){//regular
			return leftStateSequence;
		}else{
			List<Integer> leftLMState = new ArrayList<Integer>(l_size);
			for(int i=l_size-1; i>=0; i--){//right to left
				if(isASuffixInTbl(leftStateSequence, 0, i, suffixTbl)){
				//if(is_a_suffix_in_grammar(left_state_sequence, 0, i, grammar_suffix)){
					for(int j=i; j>=0; j--)
						leftLMState.set(j, leftStateSequence.get(j));
					break;
				}else{
					leftLMState.set(i, this.NULL_LEFT_LM_STATE_SYM_ID);
				}
			}
			return leftLMState;
		}
		
	}
	
	
	private  List<Integer> getRightEquivState(List<Integer> rightStateSequence, HashMap<String, Boolean> prefixTbl){
		
		int r_size = (rightStateSequence.size()<bleuOrder-1)? rightStateSequence.size() : (bleuOrder-1);
		
		if(useRightEquivState==false || r_size<bleuOrder-1){//regular
			return rightStateSequence;
		}else{
			 List<Integer> rightLMState = new ArrayList<Integer>(r_size);
			for(int i=0; i<r_size; i++){//left to right
				if(isAPrefixInTbl(rightStateSequence, rightStateSequence.size()-r_size+i, rightStateSequence.size()-1, prefixTbl)){
				//if(is_a_prefix_in_grammar(right_state_sequence, right_state_sequence.size()-r_size+i, right_state_sequence.size()-1, grammar_prefix)){
					for(int j=i; j<r_size; j++)
						rightLMState.set(j, rightStateSequence.get(rightStateSequence.size()-r_size+j) );
					break;
				}else{
					rightLMState.set(i, this.NULL_RIGHT_LM_STATE_SYM_ID );
				}
			}
			//System.out.println("origi right:" + Symbol.get_string(right_state_sequence)+ "; equiv right:" + Symbol.get_string(right_lm_state));
			return rightLMState;	
		}
		
	}
	
	//=================================================================================================
	//==================== ngram extraction functions ==========================================
	//=================================================================================================
	public void getNgrams(HashMap<String, Integer> tbl, int order, int[] wrds, boolean ignoreNullEquivSymbol){
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				boolean contain_null=false;
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					if(wrds[k]==this.NULL_LEFT_LM_STATE_SYM_ID || wrds[k]==this.NULL_RIGHT_LM_STATE_SYM_ID ){
						contain_null=true;
						if(ignoreNullEquivSymbol) 
							break;
					}
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
	public void getNgrams(HashMap<String, Integer>  tbl, int order, List<Integer> wrds, boolean ignoreNullEquivSymbol){
		for(int i=0; i<wrds.size(); i++)
			for(int j=0; j<order && j+i<wrds.size(); j++){//ngram: [i,i+j]
				boolean contain_null=false;
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds.get(k);
					if(t_wrd==this.NULL_LEFT_LM_STATE_SYM_ID || t_wrd==this.NULL_RIGHT_LM_STATE_SYM_ID ){
						contain_null=true;
						if(ignoreNullEquivSymbol) 
							break;
					}
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
	
	
	//=================================================================================================
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
	//==================== table-based suffix/prefix lookup==========================================
	//=================================================================================================
	
	
	public static void setupPrefixSuffixTbl(int[] wrds, int order, HashMap<String, Boolean>  prefixTbl, HashMap<String, Boolean> suffixTbl){
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();	

				//=== prefix
				for(int k=i; k<i+j; k++){//all ngrams [i,i+j-1]
					ngram.append(wrds[k]);
					prefixTbl.put(ngram.toString(),true);
					ngram.append(" ");
				}				
				
				//=== suffix: right-most wrd first
				ngram = new StringBuffer();
				for(int k=i+j; k>i; k--){//all ngrams [i+1,i+j]: reverse order
					ngram.append(wrds[k]);
					suffixTbl.put(ngram.toString(),true);//stored in reverse order
					ngram.append(" ");
				}				
			}
	}
	

	private boolean isAPrefixInTbl(List<Integer> rightStateSequence, int startPos, int endPos, HashMap<String, Boolean> prefixTbl){
		
		if( rightStateSequence.get(startPos)==this.NULL_RIGHT_LM_STATE_SYM_ID)
			return false;
		
		StringBuffer prefix = new StringBuffer();
		for(int i=startPos; i<=endPos; i++){
			prefix.append(rightStateSequence.get(i));
			if(i<endPos) 
				prefix.append(" ");
		}		
		
		return  prefixTbl.containsKey(prefix.toString());
	}
	
	
	private boolean isASuffixInTbl(List<Integer> leftStateSequence, int startPos, int endPos, HashMap<String, Boolean> suffixTbl){
		
		if( leftStateSequence.get(endPos)==this.NULL_LEFT_LM_STATE_SYM_ID)
			return false;
		
		StringBuffer suffix = new StringBuffer();
		for(int i=endPos; i>=startPos; i--){//right-most first
			suffix.append(leftStateSequence.get(i));
			if(i>startPos) suffix.append(" ");
		}		
		
		return suffixTbl.containsKey(suffix.toString());
	}
	
	
	
	
	//=================================================================================================
	//==================== grammar-based suffix/prefix lookup==========================================
	//=================================================================================================
	
	public static void setupPrefixSuffixGrammar(int[] wrds,  int order, PrefixGrammar prefix_gr, PrefixGrammar suffix_gr){
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				//### prefix
				prefix_gr.add_ngram(wrds, i, i+j-1);//ngram: [i,i+j-1]
				 			
				//### suffix: right-most wrd first
				int[] reverse_wrds = new int[j];
				for(int k=i+j, t=0; k>i; k--){//all ngrams [i+1,i+j]: reverse order
					reverse_wrds[t++] = wrds[k];
				}
				suffix_gr.add_ngram(reverse_wrds, 0, j-1);
			}
	}
	
	
	
	private boolean isAPrefixInGrammar(ArrayList<Integer> rightStateSequence, int start_pos, int end_pos, PrefixGrammar gr_prefix){
		if( rightStateSequence.get(start_pos)==this.NULL_RIGHT_LM_STATE_SYM_ID)
			return false;
		return gr_prefix.containNgram(rightStateSequence,  start_pos,  end_pos);
	}
	
	private boolean isASuffixInGrammar(ArrayList<Integer> leftStateSequence, int start_pos, int end_pos, PrefixGrammar grammar_suffix){
		if( leftStateSequence.get(end_pos)== this.NULL_LEFT_LM_STATE_SYM_ID)
			return false;
		ArrayList<Integer> suffix = new ArrayList<Integer>();
		for(int i=end_pos; i>=start_pos; i--){//right-most first
			suffix.add(leftStateSequence.get(i));
		}		
		return grammar_suffix.containNgram(suffix,  0,  suffix.size()-1);
	}
	
	
	/*a backoff node is a hashtable, it may include:
	 * (1) probabilititis for next words
	 * (2) pointers to a next-layer backoff node (hashtable)
	 * (3) backoff weight for this node
	 * (4) suffix/prefix flag to indicate that there is ngrams start from this suffix
     */
	private static class PrefixGrammar {
		HashMap<Integer, HashMap> root = new HashMap<Integer, HashMap>();
		
		//add prefix information
		public void add_ngram(int[] wrds, int start_pos, int end_pos){			
			//######### identify the position, and insert the trinodes if necessary
			HashMap<Integer, HashMap> pos = root;
			for(int k=start_pos; k <=end_pos; k++){
				int cur_sym_id=wrds[k];
				HashMap<Integer, HashMap> next_layer = pos.get(cur_sym_id);
				if(next_layer!=null){
					pos=next_layer;
				}else{		
					HashMap<Integer, HashMap> tem = new HashMap<Integer, HashMap>();//next layer node
					pos.put(cur_sym_id, tem); 
					pos = tem;
				}
			}
		}
		
		public boolean containNgram(ArrayList<Integer> wrds, int start_pos, int end_pos){
			if(end_pos<start_pos)return false;
			HashMap pos = root;
			for(int k=start_pos; k <=end_pos; k++){
				int cur_sym_id=  wrds.get(k);
				HashMap next_layer = (HashMap) pos.get(cur_sym_id);
				if(next_layer!=null){
					pos=next_layer;
				}else{
					return false;
				}
			}
			return true;
		}			
	}
	
	
	


	//=================================================================================================
	//====================  example main function ==========================================
	//=================================================================================================
	
	
	/*for 919 sent, time_on_reading: 148797
	time_on_orc_extract: 580286*/
	public static void main(String[] args) throws IOException {
	
		/*String f_hypergraphs="C:\\Users\\zli\\Documents\\mt03.src.txt.ss.nbest.hg.items";
		String f_rule_tbl="C:\\Users\\zli\\Documents\\mt03.src.txt.ss.nbest.hg.rules";
		String f_ref_files="C:\\Users\\zli\\Documents\\mt03.ref.txt.1";
		String f_orc_out ="C:\\Users\\zli\\Documents\\mt03.orc.txt";*/
		if(args.length!=6){
			System.out.println("wrong command, correct command should be: java Decoder f_hypergraphs f_rule_tbl f_ref_files f_orc_out lm_order orc_extract_nbest");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}		
		String f_hypergraphs = args[0].trim();
		String f_rule_tbl = args[1].trim();
		String f_ref_files = args[2].trim();
		String f_orc_out =  args[3].trim();
		int lm_order = Integer.parseInt(args[4].trim());
		boolean orc_extract_nbest = new Boolean(args[5].trim()); //oracle extraction from nbest or hg
		
		boolean saveModelScores = true;
		
		//????????????????????????????????????????????????????
		int baseline_lm_feat_id = 0; 
		//??????????????????????????????????????
		
		SymbolTable p_symbolTable = new BuildinSymbol(null);
		
		KBestExtractor kbestExtractor =null;
		int topN=300;//TODO
		boolean extract_unique_nbest = true;//TODO
		boolean do_ngram_clip_nbest = true; //TODO
		if(orc_extract_nbest==true){
			System.out.println("oracle extraction from nbest list");
			kbestExtractor = new KBestExtractor(p_symbolTable, extract_unique_nbest, false, false, false,  false, true);
		}
		
		BufferedWriter orc_out = FileUtility.getWriteFileStream(f_orc_out); 
		boolean rerankKbestOracles = true;
		BufferedWriter rerankOrcOut=null;
		if(rerankKbestOracles==true){
			rerankOrcOut = FileUtility.getWriteFileStream(f_orc_out+".rerank");
		}
		
		long start_time0 = System.currentTimeMillis();
		long time_on_reading = 0;
		long time_on_orc_extract = 0;
		BufferedReader t_reader_ref = FileUtility.getReadFileStream(f_ref_files);
		
		DiskHyperGraph dhg_read  = new DiskHyperGraph(p_symbolTable, baseline_lm_feat_id, saveModelScores, null);
		
	
		dhg_read.initRead(f_hypergraphs, f_rule_tbl, null);
		
		KBestExtractor oracleKbestExtractor = new KBestExtractor(p_symbolTable, extract_unique_nbest, false, false, true,  false, true);//extract kbest oracles
		KBestExtractor rerankOracleKbestExtractor = new KBestExtractor(p_symbolTable, extract_unique_nbest, false, false, false,  false, true);//extract kbest oracles
		int topKOracles= 500;//TODO
		//OracleExtractionOnHGV2 orc_extractor = new OracleExtractionOnHGV2(p_symbolTable, baseline_lm_feat_id);
		OracleExtractionOnHGV3 orc_extractor = new OracleExtractionOnHGV3(p_symbolTable);
		String ref_sent= null;
		int sent_id=0;
		long start_time = System.currentTimeMillis();
		while( (ref_sent=FileUtility.read_line_lzf(t_reader_ref))!= null ){
			System.out.println("############Process sentence " + sent_id);
			start_time = System.currentTimeMillis();
			sent_id++;
			//if(sent_id>10)break;
			
			HyperGraph hg = dhg_read.readHyperGraph();
			if(hg==null)continue;
			
			double orc_bleu=0;
			
			//System.out.println("read disk hyp: " + (System.currentTimeMillis()-start_time));
			time_on_reading += System.currentTimeMillis()-start_time;
			start_time = System.currentTimeMillis();
			
			if(orc_extract_nbest){
				Object[] res = orc_extractor.oracleExtractOnNbest(kbestExtractor, hg, topN, do_ngram_clip_nbest, ref_sent);
				String orc_sent = (String) res[0];
				orc_bleu = (Double) res[1];
				orc_out.write(orc_sent+"\n");
			}else{				
				HyperGraph hg_oracle = orc_extractor.oracleExtractOnHG(hg, hg.sentLen, lm_order, ref_sent);
				oracleKbestExtractor.lazyKBestExtractOnHG(hg_oracle, null, topKOracles, hg.sentID, orc_out);
				orc_bleu = hg_oracle.goalNode.bestHyperedge.bestDerivationLogP;
				time_on_orc_extract += System.currentTimeMillis()-start_time;
				//System.out.println("num_virtual_items: " + orc_extractor.numRefinedNodes + " num_virtual_dts: " + orc_extractor.numRefinedEdges);
				//System.out.println("oracle extract: " + (System.currentTimeMillis()-start_time));
				
				
				//==== rerank the kbest-oracles to verify the approximation for DP is ok
				if(rerankKbestOracles){
					Object[] res = orc_extractor.oracleExtractOnNbest(rerankOracleKbestExtractor, hg_oracle, topKOracles, do_ngram_clip_nbest, ref_sent);
					String orc_sent = (String) res[0];
					//double rerankedOrcBleu = (Double) res[1];
					rerankOrcOut.write(orc_sent+"\n");
				}				
			}			
			
			System.out.println("orc bleu is " + orc_bleu);
		}
		t_reader_ref.close();
		orc_out.close();
		if(rerankOrcOut!=null) 
			rerankOrcOut.close();
		
		System.out.println("time_on_reading: " + time_on_reading);
		System.out.println("time_on_orc_extract: " + time_on_orc_extract);
		System.out.println("total running time: "
			+ (System.currentTimeMillis() - start_time0));
	}



	
	
}
