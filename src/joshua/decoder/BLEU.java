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
package joshua.decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import joshua.decoder.Support;
import joshua.util.Regex;


/**
 * this class implements: 
 * (1) sentence-level bleu, with smoothing
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class BLEU {
	//do_ngram_clip: consider global n-gram clip
	
	public  static double computeSentenceBleu(String[] refSents, String hypSent) {
		return computeSentenceBleu(refSents, hypSent, true, 4, false);
	}
	
	//====================multiple references
	/**
	 * 
	 * @param refSents 
	 * @param hypSent
	 * @param doNgramClip Should usually be true
	 * @param bleuOrder Should usually be 4
	 * @param useShortestRef Probably use false
	 */
	public  static double computeSentenceBleu(String[] refSents, String hypSent, boolean doNgramClip, int bleuOrder, boolean useShortestRef){
		//=== ref tbl
		HashMap<String, Integer> maxRefCountTbl = constructMaxRefCountTable(refSents, bleuOrder);
		
		//== ref len
		int[] refLens = new int[refSents.length];
		for(int i =0; i<refSents.length; i++){
			String[] refWords = Regex.spaces.split(refSents[i]);
			refLens[i] = refWords.length;					
		}
		
		double effectiveRefLen=computeEffectiveLen(refLens, useShortestRef);
		
		//=== hyp tbl
		String[] hypWrds = Regex.spaces.split(hypSent);
		HashMap<String, Integer> hypNgramTbl = new HashMap<String, Integer>();
		accumulateNgramCounts(hypNgramTbl, bleuOrder, hypWrds);
		
		return computeSentenceBleu(effectiveRefLen, maxRefCountTbl, hypWrds.length, hypNgramTbl, doNgramClip, bleuOrder);
	}
	
	public static double computeEffectiveLen(int[] refLens, boolean useShortestRef ){
		if(useShortestRef){
			int res=Integer.MAX_VALUE;
			for(int i=0; i<refLens.length;i++)
				if(refLens[i]<res)
					res = refLens[i];
			return res;
		}else{//default is average length
			double res=0;
			for(int i=0; i<refLens.length;i++)
				res += refLens[i];
			return res*1.0/refLens.length;
		}
	}
	
	
	/**
	 * construct maxRefCount tbl for multiple references
	 */
	public  static HashMap<String, Integer> constructMaxRefCountTable(String[] refSents, int bleuOrder){
		
		ArrayList<HashMap<String, Integer>> listRefNgramTbl = new ArrayList<HashMap<String, Integer>>();
		for(int i =0; i<refSents.length; i++){
			String[] refWords = Regex.spaces.split(refSents[i]);			
			HashMap<String, Integer> refNgramTbl = new HashMap<String, Integer>();
			accumulateNgramCounts(refNgramTbl, bleuOrder, refWords);	
			listRefNgramTbl.add(refNgramTbl);			
		}
		
		return computeMaxRefCountTbl(listRefNgramTbl);
	}
	
	/**compute max_ref_count for each ngram in the reference sentences
	 * */
	public static HashMap<String, Integer> computeMaxRefCountTbl(ArrayList<HashMap<String, Integer>> listRefNgramTbl){
		
		HashMap<String, Integer> merged = new HashMap<String, Integer>();
		
		//== get merged key set
		for(HashMap<String, Integer> tbl : listRefNgramTbl){
			for(String ngram : tbl.keySet()){
				merged.put(ngram, 0);
			}
		}
		
		//== get max ref count
		for(String ngram : merged.keySet()){
			int max=0;
			for(HashMap<String, Integer> tbl : listRefNgramTbl){
				Integer val = tbl.get(ngram);
				if(val!=null && val>max)
					max = val;
			}			
			
			merged.put(ngram, max);
		}
		return merged;
	}
	
	public  static double computeSentenceBleu(double effectiveRefLen, HashMap<String, Integer> maxRefCountTbl, int hypLen, 
			HashMap<String, Integer> hypNgramTbl, boolean doNgramClip, int bleuOrder){
		
		double resBleu = 0;
		
		int[] numNgramMatch = new int[bleuOrder];
		for(String ngram : hypNgramTbl.keySet()){//each ngram in hyp
			if(maxRefCountTbl.containsKey(ngram)){				
				int hypNgramCount =  hypNgramTbl.get(ngram);
				
				int effectiveNumMatch = hypNgramCount;				
				
				if(doNgramClip){//min{hypNgramCount, maxRefCount}
					int maxRefCount =  maxRefCountTbl.get(ngram);				
					effectiveNumMatch = (int)Support.findMin(hypNgramCount, maxRefCount); //ngram clip;
				}    			
		    		
				
				numNgramMatch[Regex.spaces.split(ngram).length-1] += effectiveNumMatch;
			}
		}
		
		resBleu = computeBleu(hypLen, effectiveRefLen, numNgramMatch, bleuOrder);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);
		//System.out.println("Blue is " + res_bleu);
		return resBleu;
	}
	
	
	//==============================multiple references end
	
	
	
	
	
	
	public  static double computeSentenceBleu(String ref_sent, String hyp_sent, boolean do_ngram_clip, int bleu_order){
		String[] ref_wrds = Regex.spaces.split(ref_sent);
		String[] hyp_wrds = Regex.spaces.split(hyp_sent);
		HashMap<String, Integer> ref_ngram_tbl = new HashMap<String, Integer>();
		accumulateNgramCounts(ref_ngram_tbl, bleu_order, ref_wrds);
		HashMap<String, Integer> hyp_ngram_tbl = new HashMap<String, Integer>();
		accumulateNgramCounts(hyp_ngram_tbl, bleu_order, hyp_wrds);
		
		return computeSentenceBleu(ref_wrds.length, ref_ngram_tbl, hyp_wrds.length, hyp_ngram_tbl, do_ngram_clip, bleu_order);
	}
	
	public  static double computeSentenceBleu(int ref_len, HashMap<String, Integer> ref_ngram_tbl, int hyp_len, HashMap<String, Integer> hyp_ngram_tbl, boolean do_ngram_clip, int bleu_order){
		double res_bleu = 0;
		
		int[] num_ngram_match = new int[bleu_order];
		for(Iterator<String> it = hyp_ngram_tbl.keySet().iterator(); it.hasNext();){
			String ngram = it.next();
			if (ref_ngram_tbl.containsKey(ngram)) {
				if (do_ngram_clip) {
					num_ngram_match[Regex.spaces.split(ngram).length-1] += Support.findMin(ref_ngram_tbl.get(ngram), hyp_ngram_tbl.get(ngram)); //ngram clip
				} else {
					num_ngram_match[Regex.spaces.split(ngram).length-1] += hyp_ngram_tbl.get(ngram);//without ngram count clipping
				}
    		}
		}
		res_bleu = computeBleu(hyp_len, ref_len, num_ngram_match, bleu_order);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);
		//System.out.println("Blue is " + res_bleu);
		return res_bleu;
	}
	
	//sentence-bleu: BLEU= bp * prec; where prec = exp (sum 1/4 * log(prec[order]))
	public static double computeBleu(int hyp_len, double ref_len, int[] num_ngram_match, int bleu_order){
		if (hyp_len <= 0 || ref_len <= 0) {
			System.out.println("error: ref or hyp is zero len");
			System.exit(1);
		}
		double res = 0;
		double wt = 1.0/bleu_order;
		double prec = 0;
		double smooth_factor=1.0;
		for (int t = 0; t < bleu_order && t < hyp_len; t++) {
			if (num_ngram_match[t] > 0) {
				prec += wt*Math.log(num_ngram_match[t]*1.0/(hyp_len-t));
			} else {
				smooth_factor *= 0.5;//TODO
				prec += wt*Math.log(smooth_factor/(hyp_len-t));
			}
		}
		double bp = (hyp_len >= ref_len) ? 1.0 : Math.exp(1-ref_len/hyp_len);
		res = bp*Math.exp(prec);
		//System.out.println("hyp_len: " + hyp_len + "; ref_len:" + ref_len + "prec: " + Math.exp(prec) + "; bp: " + bp + "; bleu: " + res);
		return res;
	}
	
	
	
	
	public  static HashMap<String, Integer> constructReferenceTable(String refSentence, int bleu_order){		
		HashMap<String, Integer> referenceNgramTable = new HashMap<String, Integer>();	
		String[] ref_wrds = Regex.spaces.split(refSentence);			
		accumulateNgramCounts(referenceNgramTable, bleu_order, ref_wrds);				
	
		return referenceNgramTable;
	}


	public static void accumulateNgramCounts(HashMap<String,Integer> tbl, int order, String[] wrds) {
		for (int i = 0; i < wrds.length; i++) {
			for (int j = 0; j < order && j + i < wrds.length; j++) { // ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for (int k = i; k <= i+j; k++) {
					ngram.append(wrds[k]);
					if (k < i+j) ngram.append(' ');
				}
				String ngram_str = ngram.toString();
				if (tbl.containsKey(ngram_str)) {
					tbl.put(ngram_str, (Integer)tbl.get(ngram_str)+1);
				} else {
					tbl.put(ngram_str, 1);
				}
			}
		}
	}
	

	/** 
	 * speed consideration: assume hypNgramTable has a smaller
	 * size than referenceNgramTable does
	 */
	public static double computeLinearCorpusGain(double[] linearCorpusGainThetas, int hypLength, HashMap<String,Double> hypNgramTable,  HashMap<String,Integer> referenceNgramTable) {
		double res = 0;
		int[] numMatches = new int[5];
		res += linearCorpusGainThetas[0] * hypLength;
		numMatches[0] = hypLength;
		for (Entry<String,Double> entry : hypNgramTable.entrySet()) {
			String   key = entry.getKey();
			Integer refNgramCount = referenceNgramTable.get(key);
			//System.out.println("key is " + key); System.exit(1);
			if(refNgramCount!=null){//delta function
				int ngramOrder = Regex.spaces.split(key).length;
				res += entry.getValue() * linearCorpusGainThetas[ngramOrder];
				numMatches[ngramOrder] += entry.getValue();
			}
		}
		/*
		System.out.print("Google BLEU stats are: ");
		for(int i=0; i<5; i++)
			System.out.print(numMatches[i]+ " ");
		System.out.print(" ; BLUE is " + res);
		System.out.println();
		*/
		return res;
	}
	
	
	
	static public  double[] computeLinearCorpusThetas(int numUnigramTokens, double unigramPrecision, double decayRatio){
		double[] res = new double[5];
		res[0] = -1.0/numUnigramTokens;
		for(int i=1; i<5; i++)
			res[i] = 1.0/(4.0*numUnigramTokens*unigramPrecision*Math.pow(decayRatio, i-1));
		System.out.print("Thetas are: ");
		for(int i=0; i<5; i++)
			System.out.print(res[i] + " ");
		System.out.print("\n");
		return res;
	}		
	
	
}
