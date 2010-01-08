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

import joshua.util.io.LineReader;
import joshua.util.FileUtility;
import joshua.util.Ngram;
import joshua.util.Regex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * this class implements: 
 * (1) nbest min risk (MBR) reranking using BLEU as a gain funtion.
 * <p>
 * This assume that the string is unique in the nbest list In Hiero,
 * due to spurious ambiguity, a string may correspond to many
 * possible derivations, and ideally the probability of a string
 * should be the sum of all the derivataions leading to that string.
 * But, in practice, one normally uses a Viterbi approximation: the
 * probability of a string is its best derivation probability So,
 * if one want to deal with spurious ambiguity, he/she should do
 * that before calling this class
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class NbestMinRiskReranker {
	
	boolean produce_reranked_nbest = false;//TODO: this functionality is not implemented yet; default is to produce 1best without any feature scores; 
	double scaling_factor = 1.0;
	
	static int bleu_order = 4;
	static boolean do_ngram_clip = true;
	
	static boolean use_google_linear_corpus_gain = false;
	
	final PriorityBlockingQueue<RankerResult> resultsQueue =
		new PriorityBlockingQueue<RankerResult>();
	
	public NbestMinRiskReranker(boolean produce_reranked_nbest_, double scaling_factor_) {
		this.produce_reranked_nbest = produce_reranked_nbest_;
		this.scaling_factor = scaling_factor_;
	}
	
	
	public String process_one_sent( ArrayList<String> nbest, int sent_id) {
		System.out.println("Now process sentence " + sent_id);
		//step-0: preprocess
		//assumption: each hyp has a formate: "sent_id ||| hyp_itself ||| feature scores ||| linear-combination-of-feature-scores(this should be logP)"
		ArrayList<String> l_hyp_itself = new ArrayList<String>();
		//ArrayList<String> l_feat_scores = new ArrayList<String>();
		ArrayList<Double> l_baseline_scores = new ArrayList<Double>(); // linear combination of all baseline features
		ArrayList<HashMap<String,Integer>> l_ngram_tbls = new ArrayList<HashMap<String,Integer>>();
		ArrayList<Integer> l_sent_lens = new ArrayList<Integer>();
		for (String hyp : nbest) {
			String[] fds = Regex.threeBarsWithSpace.split(hyp);
			int t_sent_id = Integer.parseInt(fds[0]);
			if (sent_id != t_sent_id) { 
				throw new RuntimeException("sentence_id does not match");
			}
			String hypothesis = (fds.length==4) ? fds[1] : "";
			l_hyp_itself.add(hypothesis);
			
			String[] t_wrds = Regex.spaces.split(hypothesis);
			l_sent_lens.add(t_wrds.length);
			
			HashMap<String,Integer> ngramTbl = new HashMap<String,Integer>();
			Ngram.getNgrams(ngramTbl, 1, 4, t_wrds);
			l_ngram_tbls.add(ngramTbl);
			
			//l_feat_scores.add(fds[2]);
			
			// The value of finalIndex is expected to be 3,
			//     unless the hyp_itself is empty,
			//     in which case finalIndex will be 2.
			int finalIndex = fds.length - 1;
			l_baseline_scores.add(Double.parseDouble(fds[finalIndex]));
			
		}
		
		//step-1: get normalized distribution
		computeNormalizedProbs(l_baseline_scores, scaling_factor);//value in l_baseline_scores will be changed to normalized probability
		ArrayList<Double> l_normalized_probs = l_baseline_scores;
		
		//#### required by google linear corpus gain
		HashMap<String, Double> tbl_posterior_counts = null;
		if (use_google_linear_corpus_gain) {
			tbl_posterior_counts = new HashMap<String,Double>();
			getGooglePosteriorCounts(l_ngram_tbls, l_normalized_probs, tbl_posterior_counts);
		}
		//####
		
		//step-2: rerank the nbest
		double best_gain = -1000000000;//set as worst gain
		String best_hyp = null;
		ArrayList<Double> l_gains = new ArrayList<Double>();
		for (int i = 0; i < l_hyp_itself.size(); i++) {
			String cur_hyp = (String) l_hyp_itself.get(i);
			int cur_hyp_len = (Integer) l_sent_lens.get(i);
			HashMap<String, Integer> tbl_ngram_cur_hyp = l_ngram_tbls.get(i);
			//double cur_gain = computeGain(cur_hyp, l_hyp_itself, l_normalized_probs);
			double cur_gain = 0;
			if (use_google_linear_corpus_gain) {
				cur_gain = computeLinearCorpusGain(cur_hyp_len, tbl_ngram_cur_hyp, tbl_posterior_counts);
			} else {
				cur_gain = computeGain(cur_hyp_len, tbl_ngram_cur_hyp, l_ngram_tbls, l_sent_lens,l_normalized_probs);
			}
			
			l_gains.add( cur_gain);
			if (i == 0 || cur_gain > best_gain) { // maximize
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
			throw new RuntimeException("mbr reranked one best is null, must be wrong");
		}
		return best_hyp;
	}

	
	/**based on a list of log-probabilities in nbestLogProbs, obtain a 
	 * normalized distribution, and put the normalized probability (real value in [0,1]) into nbestLogProbs
	 * */
	//get a normalized distributeion and put it back to nbest_logps
	static public void computeNormalizedProbs(List<Double> nbestLogProbs, double scalingFactor){
		
		//=== get noralization constant, remember features, remember the combined linear score
		double normalizationConstant = Double.NEGATIVE_INFINITY;//log-semiring
		
		for (double logp : nbestLogProbs) {
			normalizationConstant = addInLogSemiring(normalizationConstant, logp * scalingFactor, 0);
		}
		//System.out.println("normalization_constant (logP) is " + normalization_constant);
		
		//=== get normalized prob for each hyp
		double tSum = 0;
		for (int i = 0; i < nbestLogProbs.size(); i++) {
			
			double normalizedProb = Math.exp(nbestLogProbs.get(i) * scalingFactor-normalizationConstant);
			tSum += normalizedProb;
			nbestLogProbs.set(i, normalizedProb);
			
			if (Double.isNaN(normalizedProb)) {
				throw new RuntimeException(
					"prob is NaN, must be wrong\nnbest_logps.get(i): "
					+ nbestLogProbs.get(i)
					+ "; scaling_factor: " + scalingFactor
					+ "; normalization_constant:" + normalizationConstant );
			}
			//logger.info("probability: " + normalized_prob);
		}
		
		//sanity check
		if (Math.abs(tSum - 1.0) > 1e-4) {
			throw new RuntimeException("probabilities not sum to one, must be wrong");
		}
		
	} 
	
	
	//Gain(e) = \sum_{e'} G(e, e')P(e')
	//cur_hyp: e
	//true_hyp: e'
	public double computeGain(int cur_hyp_len, HashMap<String, Integer> tbl_ngram_cur_hyp, ArrayList<HashMap<String,Integer>> l_ngram_tbls, 
			ArrayList<Integer> l_sent_lens, ArrayList<Double> nbest_probs) {
		//### get noralization constant, remember features, remember the combined linear score
		double gain = 0;
		
		for (int i = 0; i < nbest_probs.size(); i++) {
			HashMap<String,Integer> tbl_ngram_true_hyp = l_ngram_tbls.get(i);
			double true_prob = nbest_probs.get(i);
			int true_len = l_sent_lens.get(i);
			gain += true_prob * BLEU.computeSentenceBleu(true_len, tbl_ngram_true_hyp, cur_hyp_len, tbl_ngram_cur_hyp, do_ngram_clip, bleu_order);
		}
		//System.out.println("Gain is " + gain);
		return gain;
	} 
	
	//Gain(e) = \sum_{e'} G(e, e')P(e')
	//cur_hyp: e
	//true_hyp: e'
	static public double computeGain(String cur_hyp, List<String> nbest_hyps, List<Double> nbest_probs) {
		//### get noralization constant, remember features, remember the combined linear score
		double gain = 0;
		
		for (int i = 0; i < nbest_hyps.size(); i++) {
			String true_hyp  = nbest_hyps.get(i);
			double true_prob = nbest_probs.get(i);
			gain += true_prob * BLEU.computeSentenceBleu(true_hyp, cur_hyp, do_ngram_clip, bleu_order);
		}
		//System.out.println("Gain is " + gain);
		return gain;
	} 
	
	void getGooglePosteriorCounts(ArrayList<HashMap<String,Integer>>  l_ngram_tbls, ArrayList<Double> l_normalized_probs, HashMap<String,Double> tbl_posterior_counts) {
		//TODO
	}
	
	double computeLinearCorpusGain(int cur_hyp_len, HashMap<String,Integer> tbl_ngram_cur_hyp, HashMap<String,Double> tbl_posterior_counts) {
		//TODO
		double[] thetas = { -1, 1, 1, 1, 1 };
		
		double res = 0;
		res += thetas[0] * cur_hyp_len;
		for (Entry<String,Integer> entry : tbl_ngram_cur_hyp.entrySet()) {
			String   key = entry.getKey();
			String[] tem = Regex.spaces.split(key);
			
			double post_prob = tbl_posterior_counts.get(key);
			res += entry.getValue() * post_prob * thetas[tem.length];
		}
		return res;
	}
	
//	OR: return Math.log(Math.exp(x) + Math.exp(y));
	static private double addInLogSemiring(double x, double y, int addMode){//prevent over-flow 
		if (addMode == 0) { // sum
			if (x == Double.NEGATIVE_INFINITY) {//if y is also n-infinity, then return n-infinity
				return y;
			}
			if (y == Double.NEGATIVE_INFINITY) {
				return x;
			}
			
			if (y <= x) {
				return x + Math.log(1+Math.exp(y-x));
			} else {
				return y + Math.log(1+Math.exp(x-y));
			}
		} else if (addMode == 1) { // viter-min
			return (x <= y) ? x : y;
		} else if (addMode == 2) { // viter-max
			return (x >= y) ? x : y;
		} else {
			throw new RuntimeException("invalid add mode");
		}
	}
	

	
	public static void main(String[] args) throws IOException {
		
		// If you don't know what to use for scaling factor, try using 1
		
		if (args.length<4 || args.length>5) {
			System.out.println("wrong command, correct command should be: java NbestMinRiskReranker f_nbest_in f_out produce_reranked_nbest scaling_factor [numThreads]");
			System.out.println("num of args is "+ args.length);
			for(int i = 0; i < args.length; i++) {
				System.out.println("arg is: " + args[i]);
			}
			System.exit(-1);
		}
		long start_time = System.currentTimeMillis();
		String f_nbest_in = args[0].trim();
		String f_out = args[1].trim();
		boolean produce_reranked_nbest = Boolean.valueOf(args[2].trim());
		double scaling_factor = Double.parseDouble(args[3].trim());
		int numThreads = (args.length==5) ? Integer.parseInt(args[4].trim()) : 1;
	
		
		BufferedWriter t_writer_out =	FileUtility.getWriteFileStream(f_out);
		NbestMinRiskReranker mbr_reranker =
			new NbestMinRiskReranker(produce_reranked_nbest, scaling_factor);
		
		System.out.println("##############running mbr reranking");
		
		int old_sent_id = -1;
		LineReader nbestReader = new LineReader(f_nbest_in);
		ArrayList<String> nbest = new ArrayList<String>();

		if (numThreads==1) {
			
			try { for (String line : nbestReader) {
				String[] fds = Regex.threeBarsWithSpace.split(line);
				int new_sent_id = Integer.parseInt(fds[0]);
				if (old_sent_id != -1 && old_sent_id != new_sent_id) {
					String best_hyp = mbr_reranker.process_one_sent(nbest, old_sent_id);//nbest: list of unique strings
					t_writer_out.write(best_hyp);
					t_writer_out.newLine();
					t_writer_out.flush();
					nbest.clear();
				}
				old_sent_id = new_sent_id;
				nbest.add(line);
			} } finally { nbestReader.close(); }

			//last nbest
			String best_hyp = mbr_reranker.process_one_sent(nbest, old_sent_id);
			t_writer_out.write(best_hyp);
			t_writer_out.newLine();
			t_writer_out.flush();
			nbest.clear();
			t_writer_out.close();
			
		} else {
			
			ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
			
			for (String line : nbestReader) {			
				String[] fds = Regex.threeBarsWithSpace.split(line);
				int new_sent_id = Integer.parseInt(fds[0]);
				if (old_sent_id != -1 && old_sent_id != new_sent_id) {
					
					threadPool.execute(mbr_reranker.new RankerTask(nbest, old_sent_id));
					
					nbest.clear();
				}
				old_sent_id = new_sent_id;
				nbest.add(line);
			}
			
			//last nbest
			threadPool.execute(mbr_reranker.new RankerTask(nbest, old_sent_id));
			nbest.clear();
			
			threadPool.shutdown();
			
			try {
				threadPool.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
				
				while (! mbr_reranker.resultsQueue.isEmpty()) {
					RankerResult result = mbr_reranker.resultsQueue.remove();
					String best_hyp = result.toString();
					t_writer_out.write(best_hyp);
					t_writer_out.newLine();
				}
				
				t_writer_out.flush();
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				t_writer_out.close();
			}
			
		}
		
		System.out.println("Total running time (seconds) is "
			+ (System.currentTimeMillis() - start_time) / 1000.0);
	}
	
	private class RankerTask implements Runnable {

		final ArrayList<String> nbest;
		final int sent_id;
		
		RankerTask(final ArrayList<String> nbest, final int sent_id) {
			this.nbest = new ArrayList<String>(nbest);
			this.sent_id = sent_id;
		}
		
		public void run() {
			String result = process_one_sent(nbest, sent_id);
			resultsQueue.add(new RankerResult(result,sent_id));
		}
		
	}
	
	private static class RankerResult implements Comparable<RankerResult> {
		final String result;
		final Integer sentenceNumber;
		
		RankerResult(String result, int sentenceNumber) {
			this.result = result;
			this.sentenceNumber = sentenceNumber;
		}

		public int compareTo(RankerResult o) {
			return sentenceNumber.compareTo(o.sentenceNumber);
		}
		
		public String toString() {
			return result;
		}
	}
}
