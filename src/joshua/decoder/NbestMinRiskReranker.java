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

import joshua.util.FileUtility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;


/**
 * this class implements: 
 * (1) nbest min risk (MBR) reranking using BLEU as a gain funtion
 * 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 (星期一, 20 十月 2008) $
 */

/* This assume that the string is unique in the nbest list
 * In Hiero, due to spurious ambiguity, a string may correspond to many possible derivations, and ideally the probability of a string should be the sum of all the derivataions leading to
 * that string. But, in practice, one normally uses a Viterbi approximation: the probability of a string is its best derivation probability
 * So, if one want to deal with spurious ambiguity, he/she should do that before calling this class
 * */


public class NbestMinRiskReranker  {
	boolean produce_reranked_nbest=false;//TODO: this functionality is not implemented yet; default is to produce 1best without any feature scores; 
	double scaling_factor = 1.0;
	
	static int bleu_order =4;
	static boolean do_ngram_clip = true;
	
	static boolean use_google_linear_corpus_gain =false;
	
	public NbestMinRiskReranker(boolean produce_reranked_nbest_, double scaling_factor_){
		this.produce_reranked_nbest = produce_reranked_nbest_;
		this.scaling_factor = scaling_factor_;
	}
	
	
	public String process_one_sent( ArrayList<String> nbest, int sent_id){
		System.out.println("Now process sentence " + sent_id);
		//step-0: preprocess
		//assumption: each hyp has a formate: "sent_id ||| hyp_itself ||| feature scores ||| linear-combination-of-feature-scores(this should be logP)"
		ArrayList<String> l_hyp_itself = new ArrayList<String>();
		//ArrayList<String> l_feat_scores = new ArrayList<String>();
		ArrayList<Double> l_baseline_scores = new ArrayList<Double>();//linear combination of all baseline features
		ArrayList<HashMap<String, Integer>> l_ngram_tbls = new ArrayList<HashMap<String, Integer>>();
		ArrayList<Integer> l_sent_lens = new ArrayList<Integer>();
		for(String hyp : nbest){
			String[] fds = hyp.split("\\s+\\|{3}\\s+");
			int t_sent_id = new Integer(fds[0]); if(sent_id != t_sent_id){System.out.println("sentence_id does not match"); System.exit(1);}
			l_hyp_itself.add(fds[1]);
			
			String[] t_wrds = fds[1].split("\\s+");
			l_sent_lens.add(t_wrds.length);
			
			HashMap<String, Integer> tbl_ngram = new HashMap<String, Integer>();
			BLEU.get_ngrams(tbl_ngram, 4, t_wrds);
			l_ngram_tbls.add(tbl_ngram);
			
			//l_feat_scores.add(fds[2]);
			l_baseline_scores.add(new Double(fds[3]));
			
		}
		
		//step-1: get normalized distribution
		computeNormalizedProbs(l_baseline_scores, scaling_factor);//value in l_baseline_scores will be changed to normalized probability
		ArrayList<Double> l_normalized_probs = l_baseline_scores;
		
		//#### required by google linear corpus gain
		HashMap<String, Double> tbl_posterior_counts = null;
		if(use_google_linear_corpus_gain){
			tbl_posterior_counts = new HashMap<String, Double>();
			getGooglePosteriorCounts(l_ngram_tbls, l_normalized_probs, tbl_posterior_counts);
		}
		//####
		
		//step-2: rerank the nbest
		double best_gain = -1000000000;//set as worst gain
		String best_hyp=null;
		ArrayList<Double> l_gains = new ArrayList<Double>();
		for(int i=0; i<l_hyp_itself.size(); i++){
			String cur_hyp = (String) l_hyp_itself.get(i);
			int cur_hyp_len = (Integer) l_sent_lens.get(i);
			HashMap<String, Integer> tbl_ngram_cur_hyp = l_ngram_tbls.get(i);
			//double cur_gain = computeGain(cur_hyp, l_hyp_itself, l_normalized_probs);
			double cur_gain=0;
			if(use_google_linear_corpus_gain)
				cur_gain = computeLinearCorpusGain(cur_hyp_len, tbl_ngram_cur_hyp, tbl_posterior_counts);
			else
				cur_gain = computeGain(cur_hyp_len, tbl_ngram_cur_hyp, l_ngram_tbls, l_sent_lens,l_normalized_probs);
			
			l_gains.add( cur_gain);
			if(i==0 || cur_gain > best_gain){//maximize
				best_gain = cur_gain;
				best_hyp = cur_hyp;
			}
		}
		
		//step-3: output the 1best or nbest
		if (this.produce_reranked_nbest) {
			//TOTO: sort the list and write the reranked nbest; Use Collections.sort(List list, Comparator c)
		} else {
			/*
			this.out.write(best_hyp);
			this.out.write("\n");
			out.flush();
			*/
		}
		System.out.println("best gain: " + best_gain);
		if (null == best_hyp) {
			System.out.println("mbr reranked one best is null, must be wrong");
			System.exit(1);
		}
		return best_hyp;
	}

	//get a normalized distributeion and put it back to nbest_logps
	static public void computeNormalizedProbs(List<Double> nbest_logps, double scaling_factor){
		//### get noralization constant, remember features, remember the combined linear score
		double normalization_constant = Double.NEGATIVE_INFINITY;//log-semiring
		
		for(double logp : nbest_logps){
			normalization_constant = add_in_log_semiring(normalization_constant, logp*scaling_factor, 0);
		}
		//System.out.println("normalization_constant (logP) is " + normalization_constant);
		
		//### get normalized prob for each hyp
		double t_sum = 0;
		for(int i=0; i< nbest_logps.size(); i++){
			double normalized_prob = Math.exp(nbest_logps.get(i)*scaling_factor-normalization_constant);
			t_sum+= normalized_prob;
			nbest_logps.set(i, normalized_prob);
			if(Double.isNaN(normalized_prob)){
				System.out.println("prob is NaN, must be wrong");
				System.out.println("nbest_logps.get(i): "+ nbest_logps.get(i) + "; scaling_factor: " + scaling_factor +"; normalization_constant:" + normalization_constant);
				System.exit(1);
			}
			//System.out.println("probability: " + normalized_prob);
		}
		
		//sanity check
		if(Math.abs(t_sum-1.0)>1e-4){
			System.out.println("probabilities not sum to one, must be wrong");
			System.exit(1);
		}
		
	} 
	
	
	//Gain(e) = \sum_{e'} G(e, e')P(e')
	//cur_hyp: e
	//true_hyp: e'
	public double computeGain(int cur_hyp_len, HashMap<String, Integer> tbl_ngram_cur_hyp, ArrayList<HashMap<String, Integer>> l_ngram_tbls, 
			ArrayList<Integer> l_sent_lens, ArrayList<Double> nbest_probs){
		//### get noralization constant, remember features, remember the combined linear score
		double gain = 0;
		
		for(int i=0; i<nbest_probs.size(); i++){
			HashMap<String, Integer> tbl_ngram_true_hyp = (HashMap<String, Integer>)l_ngram_tbls.get(i);
			double true_prob = (Double) nbest_probs.get(i);
			int true_len = (Integer) l_sent_lens.get(i);
			gain += true_prob*BLEU.compute_sentence_bleu(true_len, tbl_ngram_true_hyp, cur_hyp_len, tbl_ngram_cur_hyp, do_ngram_clip, bleu_order);
		}
		//System.out.println("Gain is " + gain);
		return gain;
	} 
	
	//Gain(e) = \sum_{e'} G(e, e')P(e')
	//cur_hyp: e
	//true_hyp: e'
	static public double computeGain(String cur_hyp, List<String> nbest_hyps, List<Double> nbest_probs){
		//### get noralization constant, remember features, remember the combined linear score
		double gain = 0;
		
		for(int i=0; i<nbest_hyps.size(); i++){
			String true_hyp = (String)nbest_hyps.get(i);
			double true_prob = (Double) nbest_probs.get(i);
			gain += true_prob*BLEU.compute_sentence_bleu(true_hyp, cur_hyp, do_ngram_clip, bleu_order);
		}
		//System.out.println("Gain is " + gain);
		return gain;
	} 
	
	void getGooglePosteriorCounts(ArrayList<HashMap<String, Integer>>  l_ngram_tbls, ArrayList<Double> l_normalized_probs, HashMap<String, Double> tbl_posterior_counts){
		//TODO
	}
	
	double computeLinearCorpusGain(int cur_hyp_len, HashMap<String, Integer> tbl_ngram_cur_hyp, HashMap<String, Double> tbl_posterior_counts){
		//TODO
		double[] thetas = new double[5];
		thetas[0]=-1;thetas[1]=1;thetas[2]=1;thetas[3]=1;thetas[4]=1;//TODO
		
		double res=0;
		res += thetas[0]*cur_hyp_len;
		for(Entry<String, Integer> entry : tbl_ngram_cur_hyp.entrySet()){
			String key = entry.getKey();
			String[] tem = key.split("\\s+");
			
			double post_prob = tbl_posterior_counts.get(key);
			res += entry.getValue()*post_prob*thetas[tem.length];
		}
		return res;
	}
	
//	OR: return Math.log(Math.exp(x) + Math.exp(y));
	static private double add_in_log_semiring(double x, double y, int add_mode){//prevent over-flow 
		if(add_mode==0){//sum
			if(x==Double.NEGATIVE_INFINITY)//if y is also n-infinity, then return n-infinity
				return y;
			if(y==Double.NEGATIVE_INFINITY)
				return x;
			
			if(y<=x)
				return x + Math.log(1+Math.exp(y-x));
			else//x<y
				return y + Math.log(1+Math.exp(x-y));
		}else if (add_mode==1){//viter-min
			return (x<=y)?x:y;
		}else if (add_mode==2){//viter-max
			return (x>=y)?x:y;
		}else{
			System.out.println("invalid add mode"); System.exit(0); return 0;
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		if (4 != args.length) {
			System.out.println("wrong command, correct command should be: java NbestMinRiskReranker f_nbest_in f_out produce_reranked_nbest scaling_factor");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++) System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}
		long start_time = System.currentTimeMillis();			
		String f_nbest_in=args[0].trim();
		String f_out=args[1].trim();
		boolean produce_reranked_nbest =  new Boolean(args[2].trim());		
		double scaling_factor  =  new Double(args[3].trim());	
	
		
		BufferedReader t_reader_nbest =
			FileUtility.getReadFileStream(f_nbest_in);
		BufferedWriter t_writer_out =	FileUtility.getWriteFileStream(f_out);
		NbestMinRiskReranker mbr_reranker = new NbestMinRiskReranker(produce_reranked_nbest, scaling_factor);
		
		System.out.println("##############running mbr reranking");
		String line=null;
		int old_sent_id=-1;
		ArrayList<String> nbest = new ArrayList<String>();
		while((line=FileUtility.read_line_lzf(t_reader_nbest))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			int new_sent_id = new Integer(fds[0]);
			if(old_sent_id!=-1 && old_sent_id!=new_sent_id){						
				String best_hyp = mbr_reranker.process_one_sent(nbest, old_sent_id);//nbest: list of unique strings
				t_writer_out.write(best_hyp+"\n");
				t_writer_out.flush();
				nbest.clear();
			}
			old_sent_id = new_sent_id;
			nbest.add(line);
		}
		//last nbest
		String best_hyp = mbr_reranker.process_one_sent(nbest, old_sent_id);
		t_writer_out.write(best_hyp + "\n");
		t_writer_out.flush();
		nbest.clear();
		
		t_reader_nbest.close();
		t_writer_out.close();
		System.out.println("Total running time (seconds) is "
			+ (System.currentTimeMillis() - start_time) / 1000.0);
	}
	
}
