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
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

public class BLEU {
	//do_ngram_clip: consider global n-gram clip
	
	public  static double compute_sentence_bleu(String[] ref_sents, String hyp_sent) {
		return compute_sentence_bleu(ref_sents, hyp_sent, true, 4, false);
	}
	
	//########################multiple references
	/**
	 * 
	 * @param ref_sents 
	 * @param hyp_sent
	 * @param do_ngram_clip Should usually be true
	 * @param bleu_order Should usually be 4
	 * @param use_shortest_ref Probably use false
	 */
	public  static double compute_sentence_bleu(String[] ref_sents, String hyp_sent, boolean do_ngram_clip, int bleu_order, boolean use_shortest_ref){
		int[] ref_lens = new int[ref_sents.length];
		ArrayList<HashMap<String, Integer>> list_ref_ngram_tbl = new ArrayList<HashMap<String, Integer>>();
		for(int i =0; i<ref_sents.length; i++){
			String[] ref_wrds = Regex.spaces.split(ref_sents[i]);
			ref_lens[i] = ref_wrds.length;
			HashMap<String, Integer> ref_ngram_tbl = new HashMap<String, Integer>();
			accumulateNgramCounts(ref_ngram_tbl, bleu_order, ref_wrds);	
			list_ref_ngram_tbl.add(ref_ngram_tbl);			
		}
		double effective_ref_len=computeEffectiveLen(ref_lens, use_shortest_ref);
		
				
		String[] hyp_wrds = Regex.spaces.split(hyp_sent);
		HashMap<String, Integer> hyp_ngram_tbl = new HashMap<String, Integer>();
		accumulateNgramCounts(hyp_ngram_tbl, bleu_order, hyp_wrds);
		
		return compute_sentence_bleu(effective_ref_len, list_ref_ngram_tbl, hyp_wrds.length, hyp_ngram_tbl, do_ngram_clip, bleu_order);
	}
	
	private static double computeEffectiveLen(int[] ref_lens, boolean use_shortest_ref ){
		if(use_shortest_ref){
			int res=1000000000;
			for(int i=0; i<ref_lens.length;i++)
				if(ref_lens[i]<res)
					res = ref_lens[i];
			return res;
		}else{//default is average length
			double res=0;
			for(int i=0; i<ref_lens.length;i++)
				res += ref_lens[i];
			return res*1.0/ref_lens.length;
		}
	}
	
	public  static double compute_sentence_bleu(double effective_ref_len, ArrayList<HashMap<String, Integer>> list_ref_ngram_tbl, int hyp_len, HashMap<String, Integer> hyp_ngram_tbl, boolean do_ngram_clip, int bleu_order){
		double res_bleu = 0;
		
		int[] num_ngram_match = new int[bleu_order];
		for(Iterator<String> it = hyp_ngram_tbl.keySet().iterator(); it.hasNext();){
			String ngram = it.next();
			int effective_num_match = 0;
			for(HashMap<String, Integer> ref_ngram_tbl : list_ref_ngram_tbl){
				if(ref_ngram_tbl.containsKey(ngram)){
					if(do_ngram_clip){
						int t_num = (int)Support.find_min(ref_ngram_tbl.get(ngram), hyp_ngram_tbl.get(ngram)); //ngram clip;
						if(t_num>effective_num_match)
							effective_num_match=t_num;
					}else{
						effective_num_match += hyp_ngram_tbl.get(ngram);//without ngram count clipping
						break;
					}    			
	    		}
			}
			num_ngram_match[Regex.spaces.split(ngram).length-1] += effective_num_match;
		}
		res_bleu = compute_bleu(hyp_len, effective_ref_len, num_ngram_match, bleu_order);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);
		//System.out.println("Blue is " + res_bleu);
		return res_bleu;
	}
	//########################multiple end
	
	
	
	public  static double compute_sentence_bleu(String ref_sent, String hyp_sent, boolean do_ngram_clip, int bleu_order){
		String[] ref_wrds = Regex.spaces.split(ref_sent);
		String[] hyp_wrds = Regex.spaces.split(hyp_sent);
		HashMap<String, Integer> ref_ngram_tbl = new HashMap<String, Integer>();
		accumulateNgramCounts(ref_ngram_tbl, bleu_order, ref_wrds);
		HashMap<String, Integer> hyp_ngram_tbl = new HashMap<String, Integer>();
		accumulateNgramCounts(hyp_ngram_tbl, bleu_order, hyp_wrds);
		
		return compute_sentence_bleu(ref_wrds.length, ref_ngram_tbl, hyp_wrds.length, hyp_ngram_tbl, do_ngram_clip, bleu_order);
	}
	
	public  static double compute_sentence_bleu(int ref_len, HashMap<String, Integer> ref_ngram_tbl, int hyp_len, HashMap<String, Integer> hyp_ngram_tbl, boolean do_ngram_clip, int bleu_order){
		double res_bleu = 0;
		
		int[] num_ngram_match = new int[bleu_order];
		for(Iterator<String> it = hyp_ngram_tbl.keySet().iterator(); it.hasNext();){
			String ngram = it.next();
			if (ref_ngram_tbl.containsKey(ngram)) {
				if (do_ngram_clip) {
					num_ngram_match[Regex.spaces.split(ngram).length-1] += Support.find_min(ref_ngram_tbl.get(ngram), hyp_ngram_tbl.get(ngram)); //ngram clip
				} else {
					num_ngram_match[Regex.spaces.split(ngram).length-1] += hyp_ngram_tbl.get(ngram);//without ngram count clipping
				}
    		}
		}
		res_bleu = compute_bleu(hyp_len, ref_len, num_ngram_match, bleu_order);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);
		//System.out.println("Blue is " + res_bleu);
		return res_bleu;
	}
	
	//sentence-bleu: BLEU= bp * prec; where prec = exp (sum 1/4 * log(prec[order]))
	public static double compute_bleu(int hyp_len, double ref_len, int[] num_ngram_match, int bleu_order){
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
	
	
	/** construct a ngram table containing ngrams that appear in the ref_sents for a given source sentence
	 */
	public  static HashMap<String, Integer> constructReferenceTable(String[] ref_sents, int bleu_order){		
		HashMap<String, Integer> referenceNgramTable = new HashMap<String, Integer>();
		for(String refSentence: ref_sents){
			String[] ref_wrds = Regex.spaces.split(refSentence);			
			accumulateNgramCounts(referenceNgramTable, bleu_order, ref_wrds);				
		}
		return referenceNgramTable;
	}
	public  static HashMap<String, Integer> constructReferenceTable(String refSentence, int bleu_order){		
		HashMap<String, Integer> referenceNgramTable = new HashMap<String, Integer>();	
		String[] ref_wrds = Regex.spaces.split(refSentence);			
		accumulateNgramCounts(referenceNgramTable, bleu_order, ref_wrds);				
	
		return referenceNgramTable;
	}
	
//	accumulate ngram counts into tbl; ngrams with an order in [1,order]
	public static void accumulateNgramCounts(HashMap<String,Integer> tbl, int order, String[] wrds) {
		for (int i = 0; i < wrds.length; i++) {
			for (int j = 0; j < order && j + i < wrds.length; j++) { // ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for (int k = i; k <= i+j; k++) {
					ngram.append(wrds[k]);
					if (k < i+j) ngram.append(" ");
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
	
	

	/** speed consideration: assume hypNgramTable has a smaller size than referenceNgramTable does
	 * */
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
		double[] res =new double[5];
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
