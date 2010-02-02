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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joshua.corpus.vocab.SymbolTable;
import joshua.util.Ngram;
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
		Ngram.getNgrams(hypNgramTbl, 1, bleuOrder, hypWrds);
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
		return constructMaxRefCountTable(null, refSents, bleuOrder);
	}
	
	/**words in the ngrams are using integer symbol ID
	 * */
	public  static HashMap<String, Integer> constructMaxRefCountTable(SymbolTable symbolTbl, String[] refSents, int bleuOrder){
		
		List<HashMap<String, Integer>> listRefNgramTbl = new ArrayList<HashMap<String, Integer>>();
		for(int i=0; i<refSents.length; i++){
			//if(refSents[i]==null){System.out.println("null ref sent"); System.exit(1);}
			//String[] refWords = refSents[i].split("\\s+");
			String[] refWords = Regex.spaces.split(refSents[i]);
			
			HashMap<String, Integer> refNgramTbl = new HashMap<String, Integer>();
			if(symbolTbl!=null)
				Ngram.getNgrams(symbolTbl, refNgramTbl, 1, bleuOrder, refWords);
			else
				Ngram.getNgrams(refNgramTbl, 1, bleuOrder, refWords);
			listRefNgramTbl.add(refNgramTbl);			
		}
		
		return computeMaxRefCountTbl(listRefNgramTbl);
	}
	
	
	/**compute max_ref_count for each ngram in the reference sentences
	 * */
	public static HashMap<String, Integer> computeMaxRefCountTbl(List<HashMap<String, Integer>> listRefNgramTbl){
		
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
	
	
	
	
	
	
	public  static double computeSentenceBleu(String refSent, String hypSent, boolean doNgramClip, int bleuOrder){
		String[] refWrds = Regex.spaces.split(refSent);
		String[] hypWrds = Regex.spaces.split(hypSent);
		HashMap<String, Integer> refNgramTbl = new HashMap<String, Integer>();
		Ngram.getNgrams(refNgramTbl, 1, bleuOrder, refWrds);
		HashMap<String, Integer> hypNgramTbl = new HashMap<String, Integer>();
		Ngram.getNgrams(hypNgramTbl, 1, bleuOrder, hypWrds);
		return computeSentenceBleu(refWrds.length, refNgramTbl, hypWrds.length, hypNgramTbl, doNgramClip, bleuOrder);
	}
	
	public  static double computeSentenceBleu(int refLen, HashMap<String, Integer> refNgramTbl, int hypLen, HashMap<String, Integer> hypNgramTbl, boolean doNgramClip, int bleuOrder){
		double resBleu = 0;
		
		int[] numNgramMatch = new int[bleuOrder];
		for(Iterator<String> it = hypNgramTbl.keySet().iterator(); it.hasNext();){
			String ngram = it.next();
			if (refNgramTbl.containsKey(ngram)) {
				if (doNgramClip) {
					numNgramMatch[Regex.spaces.split(ngram).length-1] += Support.findMin(refNgramTbl.get(ngram), hypNgramTbl.get(ngram)); //ngram clip
				} else {
					numNgramMatch[Regex.spaces.split(ngram).length-1] += hypNgramTbl.get(ngram);//without ngram count clipping
				}
    		}
		}
		resBleu = computeBleu(hypLen, refLen, numNgramMatch, bleuOrder);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);
		//System.out.println("Blue is " + res_bleu);
		return resBleu;
	}
	
	//sentence-bleu: BLEU= bp * prec; where prec = exp (sum 1/4 * log(prec[order]))
	public static double computeBleu(int hypLen, double refLen, int[] numNgramMatch, int bleuOrder){
		if (hypLen <= 0 || refLen <= 0) {
			System.out.println("error: ref or hyp is zero len");
			System.exit(1);
		}
		double res = 0;
		double wt = 1.0/bleuOrder;
		double prec = 0;
		double smooth_factor=1.0;
		for (int t = 0; t < bleuOrder && t < hypLen; t++) {
			if (numNgramMatch[t] > 0) {
				prec += wt*Math.log(numNgramMatch[t]*1.0/(hypLen-t));
			} else {
				smooth_factor *= 0.5;//TODO
				prec += wt*Math.log(smooth_factor/(hypLen-t));
			}
		}
		double bp = (hypLen >= refLen) ? 1.0 : Math.exp(1-refLen/hypLen);
		res = bp*Math.exp(prec);
		//System.out.println("hyp_len: " + hyp_len + "; ref_len:" + ref_len + "prec: " + Math.exp(prec) + "; bp: " + bp + "; bleu: " + res);
		return res;
	}
	
	
	
	
	public  static HashMap<String, Integer> constructNgramTable(String sentence, int bleuOrder){		
		HashMap<String, Integer> ngramTable = new HashMap<String, Integer>();	
		String[] refWrds = Regex.spaces.split(sentence);						
		Ngram.getNgrams(ngramTable, 1, bleuOrder, refWrds);
		return ngramTable;
	}


	
	//================================ Google linear corpus gain ============================================
	public  static double computeLinearCorpusGain(double[] linearCorpusGainThetas, String[] refSents, String hypSent){
		int bleuOrder = 4;
		int hypLength =Regex.spaces.split(hypSent).length;
		HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(refSents, bleuOrder);
		HashMap<String, Integer> hypNgramTable = BLEU.constructNgramTable(hypSent, bleuOrder); 
		return computeLinearCorpusGain(linearCorpusGainThetas, hypLength, hypNgramTable,  refereceNgramTable);
	}
	/** 
	 * speed consideration: assume hypNgramTable has a smaller
	 * size than referenceNgramTable does
	 */
	public static double computeLinearCorpusGain(double[] linearCorpusGainThetas, int hypLength, Map<String,Integer> hypNgramTable,  Map<String,Integer> referenceNgramTable) {
		double res = 0;
		res += linearCorpusGainThetas[0] * hypLength;
		for (Entry<String,Integer> entry : hypNgramTable.entrySet()) {
			String   ngram = entry.getKey();
			if(referenceNgramTable.containsKey(ngram)){//delta function
				int ngramOrder = Regex.spaces.split(ngram).length;
				res += entry.getValue() * linearCorpusGainThetas[ngramOrder];
			}
		}
		return res;
	}
	
	public static int[] computeNgramMatches(String[] refSents, String hypSent){
		int bleuOrder = 4;
		int hypLength =Regex.spaces.split(hypSent).length;
		HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(refSents, bleuOrder);
		HashMap<String, Integer> hypNgramTable = BLEU.constructNgramTable(hypSent, bleuOrder); 
		return computeNgramMatches(hypLength, hypNgramTable,  refereceNgramTable, bleuOrder);
	}
	
	public static int[] computeNgramMatches(int hypLength, Map<String,Integer> hypNgramTable,  Map<String,Integer> referenceNgramTable, int highestOrder) {
		int[] res = new int[highestOrder+1];
		res[0] = hypLength;
		for (Entry<String,Integer> entry : hypNgramTable.entrySet()) {
			String   ngram = entry.getKey();
			if(referenceNgramTable.containsKey(ngram)){//delta function
				int ngramOrder = Regex.spaces.split(ngram).length;
				res[ngramOrder] += entry.getValue();
			}
		}
		return res;
	}
	
	static public  double[] computeLinearCorpusThetas(int numUnigramTokens, double unigramPrecision, double decayRatio){
		double[] res = new double[5];
		res[0] = -1.0/numUnigramTokens;
		for(int i=1; i<5; i++)
			res[i] = 1.0/(4.0*numUnigramTokens*unigramPrecision*Math.pow(decayRatio, i-1));
		
		double firstWeight = res[0];
		for(int i=0; i<5; i++)
			res[i] /= Math.abs(firstWeight);//normalize by first one
		
		
		System.out.print("Normalized Thetas are: ");
		for(int i=0; i<5; i++)
			System.out.print(res[i] + " ");
		System.out.print("\n");
				
		return res;
	}		
	
	
}
