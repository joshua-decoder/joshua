package joshua.decoder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;

import joshua.util.FileUtility;


/**
 * this class implements: 
 * (1) nbest min risk reranking using BLEU as a gain funtion
 * 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-20 00:12:30 -0400 (星期一, 20 十月 2008) $
 */


public class NbestMinRiskReranker  {
	BufferedWriter out;
	boolean produce_reranked_nbest=false;//default is to produce 1best without any feature scores
	double scaling_factor = 1.0;
	
	int bleu_order =4;
	boolean do_ngram_clip =true;
	
	public NbestMinRiskReranker(BufferedWriter out_, boolean produce_reranked_nbest_, double scaling_factor_){
		this.out= FileUtility.handle_null_writer(out_);
		this.produce_reranked_nbest = produce_reranked_nbest_;
		this.scaling_factor = scaling_factor_;
	}
	
	
//	all hyp are represented as a hyper-graph
	public void process_one_sent( ArrayList<String> nbest, int sent_id){
		System.out.println("Now process sentence " + sent_id);
		//step-0: preprocess
		//assumption: each hyp has a formate: "sent_id ||| hyp_itself ||| feature scores ||| linear-combination-of-feature-scores(this should be logP)"
		ArrayList<String> l_hyp_itself = new ArrayList<String>();
		//ArrayList<String> l_feat_scores = new ArrayList<String>();
		ArrayList<Double> l_baseline_scores = new ArrayList<Double>();//linear combination of all baseline features
		for(String hyp : nbest){
			String[] fds = hyp.split("\\s+\\|{3}\\s+");
			int t_sent_id = new Integer(fds[0]); if(sent_id != t_sent_id){System.out.println("sentence_id does not match"); System.exit(1);}
			l_hyp_itself.add(fds[1]);
			//l_feat_scores.add(fds[2]);
			l_baseline_scores.add(new Double(fds[3]));
			
		}
		
		//step-1: get normalized distribution
		computeNormalizedProbs(l_baseline_scores);//value in l_baseline_scores will be changed to normalized probability
		
		//step-2: rerank the nbest
		double best_gain = -1000000000;//set as worst gain
		String best_hyp=null;
		ArrayList<Double> l_gains = new ArrayList<Double>();
		ArrayList<Double> l_normalized_probs = l_baseline_scores;
		for(int i=0; i<l_hyp_itself.size(); i++){
			String cur_hyp = (String)l_hyp_itself.get(i);
			double cur_gain = computeGain(cur_hyp, l_hyp_itself, l_normalized_probs); 
			l_gains.add( cur_gain);
			if(i==0 || cur_gain > best_gain){//maximize
				best_gain = cur_gain;
				best_hyp = cur_hyp;
			}
		}
		
		//step-3: output the 1best or nbest
		if(this.produce_reranked_nbest){
			//TOTO: sort the list and write the reranked nbest; Use Collections.sort(List list, Comparator c)
		}else{
			FileUtility.write_lzf(this.out,best_hyp);
			FileUtility.write_lzf(this.out,"\n");
			FileUtility.flush_lzf(out);
		}
		System.out.println("best gain: " + best_gain);
		//System.exit(1);
	}
	
	
	
	
	//get a normalized distributeion and put it back to nbest_logps
	public void computeNormalizedProbs(ArrayList<Double> nbest_logps){
		//### get noralization constant, remember features, remember the combined linear score
		double normalization_constant = Double.NEGATIVE_INFINITY;//log-semiring
		
		for(double logp : nbest_logps){
			normalization_constant = add_in_log_semiring(normalization_constant, logp*scaling_factor, 0);
		}
		System.out.println("normalization_constant (logP) is " + normalization_constant);
		
		//### get normalized prob for each hyp
		double t_sum = 0;
		for(int i=0; i< nbest_logps.size(); i++){
			double normalized_prob = Math.exp(nbest_logps.get(i)*scaling_factor-normalization_constant);
			t_sum+= normalized_prob;
			nbest_logps.set(i, normalized_prob);
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
	public double computeGain(String cur_hyp, ArrayList<String> nbest_hyps, ArrayList<Double> nbest_probs){
		//### get noralization constant, remember features, remember the combined linear score
		double gain = 0;
		
		for(int i=0; i<nbest_hyps.size(); i++){
			String true_hyp = (String)nbest_hyps.get(i);
			double true_prob = (Double) nbest_probs.get(i);
			gain += true_prob*computeGain(cur_hyp, true_hyp);
		}
		//System.out.println("Gain is " + gain);
		return gain;
	} 
	
	//loss should be negative, the more negative, the worse
	private double computeGain(String cur_hyp, String true_hyp){
		return BLEU.compute_sentence_bleu(true_hyp, cur_hyp, do_ngram_clip, bleu_order);
	}
	
	
//	OR: return Math.log(Math.exp(x) + Math.exp(y));
	private double add_in_log_semiring(double x, double y, int add_mode){//prevent over-flow 
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
	
	
	public static void main(String[] args) {		
		if(args.length!=4){
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
	
		
		BufferedReader t_reader_nbest = FileUtility.getReadFileStream(f_nbest_in,"UTF-8");
		BufferedWriter t_writer_out =	FileUtility.getWriteFileStream(f_out);
		NbestMinRiskReranker reranker = new NbestMinRiskReranker(t_writer_out, produce_reranked_nbest, scaling_factor);
		
		System.out.println("##############running mbr reranking");
		String line=null;
		int old_sent_id=-1;
		ArrayList<String> nbest = new ArrayList<String>();
		while((line=FileUtility.read_line_lzf(t_reader_nbest))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			int new_sent_id = new Integer(fds[0]);
			if(old_sent_id!=-1 && old_sent_id!=new_sent_id){						
				reranker.process_one_sent(nbest, old_sent_id);			
				nbest.clear();
			}
			old_sent_id = new_sent_id;
			nbest.add(line);
		}
		//last nbest
		reranker.process_one_sent( nbest, old_sent_id);	
		nbest.clear();
		
		FileUtility.close_read_file(t_reader_nbest);
		FileUtility.close_write_file(t_writer_out);
	}
	
}
