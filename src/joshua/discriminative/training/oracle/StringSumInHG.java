package joshua.discriminative.training.oracle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.semiring_parsing.AtomicSemiring;



/* Given a general hypergraph and a string (known to be contained in the hypergraph), this 
 * class returns a filtered hypergraph that contains only the derivations yielding that string
 * The method is not exact, in other words, it may actually contains derivations that do not yield the given string
 * Or, the recall (of the derivations we are seeking) is perfect, but the precision is not
 * */


/**This programm assumes the transition_cost in the input hypergraph is properly set!!!!!!!!!!!!!!!!!
 * */

public class StringSumInHG {
	
	SymbolTable symbolTbl;
	int lmFeatID = 0;//TODO
	int baselineLMOrder = 5;//TODO
	AtomicSemiring atomicSemirng = new AtomicSemiring(1,0);
	
	//kbest
	KBestExtractor kbestExtractor;
	int topN= 1000000000;//to make sure the filtered hypergraph is exhausitively enumerated
	boolean useUniqueNbest =false;
	boolean useTreeNbest = false;
	boolean addCombinedCost = true;	
	
	ApproximateFilterHGByOneString p_filter;
	
	public StringSumInHG(SymbolTable symbol_, KBestExtractor kbestextractor, ApproximateFilterHGByOneString filter_){
		symbolTbl = symbol_;
		kbestExtractor = kbestextractor;
		p_filter = filter_;
	}
	
	//for a given string, find the approximate and true sum 
	public double computeSumForString(HyperGraph hg, int sentenceID, String ref_string){
		System.out.println("Now process string: " + ref_string);
		int[] ref_sent_wrds_in = symbolTbl.addTerminals(ref_string);
		
		//### filter hypergraph
		HyperGraph filtedHG = p_filter.approximate_filter_hg(hg,ref_sent_wrds_in);
		
		//debug
		/*
		double inside_outside_scaling_factor =1.0;
		TrivialInsideOutside p_inside_outside = new TrivialInsideOutside();
		p_inside_outside.run_inside_outside(filted_hg, 0, 1, inside_outside_scaling_factor);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		double io_norm_constant =  p_inside_outside.get_normalization_constant();		
		p_inside_outside.clear_state();
		
		p_inside_outside.run_inside_outside(hg, 0, 1, inside_outside_scaling_factor);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		System.out.println("global norm is " + p_inside_outside.get_normalization_constant());
		p_inside_outside.clear_state();*/
		
		//### extract all possible derivations in the filted hypergraph
		ArrayList<String> nonUniqueNbestStrings = new ArrayList<String>();
		kbestExtractor.lazyKBestExtractOnHG(filtedHG, null, this.topN,  sentenceID, nonUniqueNbestStrings);//???????????????????????????
		if(nonUniqueNbestStrings.size()>=topN){
			System.out.println("number of possible derivations reaches topN, should increase its value");
			System.exit(1);
		}
		
		//### check if each derivation yields the ref string, and compute the true_sum
		int num_good_derivations = 0;
		double good_log_sum_prob = Double.NEGATIVE_INFINITY;
		int num_bad_derivations = 0;		
		double bad_log_sum_prob = Double.NEGATIVE_INFINITY;
		for(String derivation_string : nonUniqueNbestStrings){
			//System.out.println(derivation_string);
			String[] fds = derivation_string.split("\\s+\\|{3}\\s+");
			String hyp_string = fds[1];
			double log_prob = new Double(fds[fds.length-1]);//TODO: use inside_outside_scaling_factor here 
			
			if(hyp_string.compareTo(ref_string)==0){//the same
				good_log_sum_prob = atomicSemirng.add_in_atomic_semiring(good_log_sum_prob, log_prob);
				num_good_derivations++;
			}else{
				bad_log_sum_prob = atomicSemirng.add_in_atomic_semiring(bad_log_sum_prob, log_prob);
				num_bad_derivations++;
			}
			//System.out.println("log_prob: " + log_prob + "; sum: " + good_log_sum_prob);
		}
		System.out.println("good_sum: " + good_log_sum_prob + "; good_num: " + num_good_derivations + "; bad_sum: " + bad_log_sum_prob + "; bad_num: " + num_bad_derivations);
		//if(Math.abs(io_norm_constant-good_log_sum_prob)>1e-3){System.out.println("Norm is not equal!!!!!!!!!!!!!");System.exit(1);}
		p_filter.clear_state();
		return good_log_sum_prob;
	}
	

	public static void main(String[] args) throws InterruptedException, IOException {
		/*//##read configuration information
		if(args.length<8){
			System.out.println("wrong command, correct command should be: java Perceptron_HG is_crf lf_train_items lf_train_rules lf_orc_items lf_orc_rules f_l_num_sents f_data_sel f_model_out_prefix use_tm_feat use_lm_feat use_edge_bigram_feat_only f_feature_set use_joint_tm_lm_feature");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}*/
				
		if(args.length!=8){
			System.out.println("Wrong number of parameters, it must be  7");
			System.exit(1);
		}
		
		
		String f_test_items=args[0].trim();
		String f_test_rules=args[1].trim();
		int num_sents=new Integer(args[2].trim());
		String f_nbest=args[3].trim();//output
		String f_1best=args[4].trim();//output
		int topN = new Integer(args[5].trim());//nbest size of the unique strings
		
		int baseline_lm_order = new Integer(args[7].trim());//nbest size of the unique strings
	
		int baseline_lm_feat_id = 0;//???????
		
		int max_num_words =25;
		
		SymbolTable p_symbol = new BuildinSymbol(null);
		KBestExtractor kbest_extractor = new KBestExtractor(p_symbol, true, false, false, false,  false, true);//????????????
		ApproximateFilterHGByOneString filter = new ApproximateFilterHGByOneString(p_symbol,baseline_lm_feat_id,baseline_lm_order);
		StringSumInHG p_sumer = new StringSumInHG(p_symbol, kbest_extractor, filter);
		
		//#### process test set
		BufferedWriter t_writer_nbest =	FileUtilityOld.getWriteFileStream(f_nbest);	
		BufferedWriter t_writer_1best =	FileUtilityOld.getWriteFileStream(f_1best);
		System.out.println("############Process file  " + f_test_items);
		DiskHyperGraph dhg_test = new DiskHyperGraph(p_symbol, baseline_lm_feat_id, true, null); //have model costs stored
		dhg_test.initRead(f_test_items, f_test_rules,null);
			
		for(int sent_id=0; sent_id < num_sents; sent_id ++){
			System.out.println("#Process sentence " + sent_id);
			HyperGraph hg_test = dhg_test.readHyperGraph();			
			//if(sent_id==1)System.exit(1);
			//generate a unique nbest of strings based on viterbi cost
			ArrayList<String> nonUniqueNbestStrings = new ArrayList<String>();
			kbest_extractor.lazyKBestExtractOnHG(hg_test, null, topN, sent_id, nonUniqueNbestStrings);
			
			double max_prob = Double.NEGATIVE_INFINITY;
			String max_string = "";
			
			//chech if the sentence is too long
			boolean skip=false;
			for(String unique_string : nonUniqueNbestStrings){
				//System.out.println(unique_string);
				String[] fds = unique_string.split("\\s+\\|{3}\\s+");
				String hyp_string = fds[1];
				String[] wrds = hyp_string.split("\\s+");
				if(wrds.length>max_num_words){
					skip=true;
					break;
				}
			}
			if(skip==false){
				for(String unique_string : nonUniqueNbestStrings){
					//System.out.println(unique_string);
					String[] fds = unique_string.split("\\s+\\|{3}\\s+");
					String hyp_string = fds[1];
					String[] wrds = hyp_string.split("\\s+");
					if(wrds.length>max_num_words)
						break;
					double true_sum_prob = p_sumer.computeSumForString(hg_test, sent_id, hyp_string);
					System.out.println( unique_string + " ||| " + true_sum_prob);
					FileUtilityOld.writeLzf(t_writer_nbest, unique_string + " ||| " + true_sum_prob + "\n");
					if(true_sum_prob>max_prob){
						max_string = hyp_string;
						max_prob = true_sum_prob;
					}
				}
			}else{
				System.out.println("lzf; skip sentence " + sent_id);
			}
			FileUtilityOld.writeLzf(t_writer_1best, max_string + "\n");
		}
		FileUtilityOld.closeWriteFile(t_writer_nbest);
		FileUtilityOld.closeWriteFile(t_writer_1best);
				
	}
		
}
