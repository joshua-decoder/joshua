package joshua.decoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import joshua.decoder.Support;


/**
 * this class implements: 
 * (1) sentence-level bleu, with smoothing
 * 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 (星期一, 20 十月 2008) $
 */

public class BLEU {
	//do_ngram_clip: consider global n-gram clip
	
	//########################multiple references
	public  static double compute_sentence_bleu(String[] ref_sents, String hyp_sent, boolean do_ngram_clip, int bleu_order, boolean use_shortest_ref){
		int[] ref_lens = new int[ref_sents.length];
		ArrayList<HashMap<String, Integer>> list_ref_ngram_tbl = new ArrayList<HashMap<String, Integer>>();
		for(int i =0; i<ref_sents.length; i++){
			String[] ref_wrds = ref_sents[i].split("\\s+");
			ref_lens[i] = ref_wrds.length;
			HashMap<String, Integer> ref_ngram_tbl = new HashMap<String, Integer>();
			get_ngrams(ref_ngram_tbl, bleu_order, ref_wrds);	
			list_ref_ngram_tbl.add(ref_ngram_tbl);			
		}
		double effective_ref_len=computeEffectiveLen(ref_lens, use_shortest_ref);
		
				
		String[] hyp_wrds = hyp_sent.split("\\s+");
		HashMap<String, Integer> hyp_ngram_tbl = new HashMap<String, Integer>();
		get_ngrams(hyp_ngram_tbl, bleu_order, hyp_wrds);
		
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
		for(Iterator it = hyp_ngram_tbl.keySet().iterator(); it.hasNext();){
			String ngram = (String) it.next();
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
			num_ngram_match[ngram.split("\\s+").length-1] += effective_num_match;
		}
		res_bleu = compute_bleu(hyp_len, effective_ref_len, num_ngram_match, bleu_order);
		//System.out.println("hyp_len: " + hyp_sent.length + "; ref_len:" + ref_sent.length + "; bleu: " + res_bleu +" num_ngram_matches: " + num_ngram_match[0] + " " +num_ngram_match[1]+
		//		" " + num_ngram_match[2] + " " +num_ngram_match[3]);
		//System.out.println("Blue is " + res_bleu);
		return res_bleu;
	}
	//########################multiple end
	
	
	
	public  static double compute_sentence_bleu(String ref_sent, String hyp_sent, boolean do_ngram_clip, int bleu_order){
		String[] ref_wrds = ref_sent.split("\\s+");
		String[] hyp_wrds = hyp_sent.split("\\s+");
		HashMap<String, Integer> ref_ngram_tbl = new HashMap<String, Integer>();
		get_ngrams(ref_ngram_tbl, bleu_order, ref_wrds);
		HashMap<String, Integer> hyp_ngram_tbl = new HashMap<String, Integer>();
		get_ngrams(hyp_ngram_tbl, bleu_order, hyp_wrds);
		
		return compute_sentence_bleu(ref_wrds.length, ref_ngram_tbl, hyp_wrds.length, hyp_ngram_tbl, do_ngram_clip, bleu_order);
	}
	
	public  static double compute_sentence_bleu(int ref_len, HashMap<String, Integer> ref_ngram_tbl, int hyp_len, HashMap<String, Integer> hyp_ngram_tbl, boolean do_ngram_clip, int bleu_order){
		double res_bleu = 0;
		
		int[] num_ngram_match = new int[bleu_order];
		for(Iterator it = hyp_ngram_tbl.keySet().iterator(); it.hasNext();){
			String ngram = (String) it.next();
			if(ref_ngram_tbl.containsKey(ngram)){
				if(do_ngram_clip)
					num_ngram_match[ngram.split("\\s+").length-1] += Support.find_min(ref_ngram_tbl.get(ngram), hyp_ngram_tbl.get(ngram)); //ngram clip
				else
					num_ngram_match[ngram.split("\\s+").length-1] += hyp_ngram_tbl.get(ngram);//without ngram count clipping    			
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
		if(hyp_len<=0 || ref_len<=0){System.out.println("error: ref or hyp is zero len"); System.exit(0);}
		double res=0;		
		double wt = 1.0/bleu_order;
		double prec = 0;
		double smooth_factor=1.0;
		for(int t=0; t<bleu_order && t<hyp_len; t++){
			if(num_ngram_match[t]>0)
				prec += wt*Math.log(num_ngram_match[t]*1.0/(hyp_len-t));
			else{
				smooth_factor *= 0.5;//TODO
				prec += wt*Math.log(smooth_factor/(hyp_len-t));
			}
		}
		double bp = (hyp_len>=ref_len) ? 1.0 : Math.exp(1-ref_len/hyp_len);	
		res = bp*Math.exp(prec);
		//System.out.println("hyp_len: " + hyp_len + "; ref_len:" + ref_len + "prec: " + Math.exp(prec) + "; bp: " + bp + "; bleu: " + res);
		return res;
	}
	
	//accumulate ngram counts into tbl

	
//	accumulate ngram counts into tbl
	public static void get_ngrams(HashMap<String, Integer> tbl, int order, String[] wrds){
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){	
					ngram.append(wrds[k]);
					if(k<i+j) ngram.append(" ");
				}
				String ngram_str = ngram.toString();
				if(tbl.containsKey(ngram_str))
					tbl.put(ngram_str, (Integer)tbl.get(ngram_str)+1);
				else
					tbl.put(ngram_str, 1);
			}
	}
	
	
	
}
