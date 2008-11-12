package edu.jhu.joshua.VariationalDecoder;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.jhu.joshua.discriminative_training.feature_related.FeatureExtractionHG;
import edu.jhu.joshua.discriminative_training.feature_related.FeatureTemplate;
import edu.jhu.joshua.discriminative_training.feature_related.FeatureTemplateBaseline2;


import joshua.decoder.BuildinSymbol;

import joshua.decoder.Symbol;
import joshua.decoder.ff.FeatureFunction;

import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KbestExtraction;
import joshua.decoder.hypergraph.TrivialInsideOutside;
import joshua.util.FileUtility;


public class VariationalDecoder {	
	Symbol p_symbol;
	double baseline_weight =1.0;
	double ngam_weight =1.0;	
	int baseline_lm_order = 3;
	
	int topN=300;
	boolean use_unique_nbest =true;
	boolean use_tree_nbest = false;
	boolean add_combined_cost = true;	
	
	int baseline_lm_feat_id =0;
	int baseline_feat_id = 2000;	
	
	FeatureTemplate baseline_ft_template; 
	HashMap<HyperEdge,Double> baseline_feat_tbl = new HashMap<HyperEdge,Double>(); 
	
	FeatureTemplate ngram_ft_template; //for ngram feature extraction
	HashMap<String,Double> ngram_feat_tbl = new HashMap<String,Double>();	

	ArrayList<FeatureFunction> l_feat_functions = new ArrayList<FeatureFunction>() ; //for HG reranking and kbest extraction
	
	public VariationalDecoder(Symbol symbol_, int baseline_lm_feat_id_, double baseline_weight_, double ngam_weight_, int baseline_lm_order_ ){
		this.p_symbol = symbol_;
		this.baseline_weight = baseline_weight_;
		this.ngam_weight = ngam_weight_;
		this.baseline_lm_order = baseline_lm_order_;
		this.baseline_lm_feat_id = baseline_lm_feat_id_;
		this.baseline_ft_template = new FeatureTemplateBaseline2();	
		this.ngram_ft_template = new GenerativeNgramFeatureTemplate(this.p_symbol, true , this.baseline_lm_feat_id, this.baseline_lm_order, 1, this.baseline_lm_order); 
	}
	
	//return changed hg
	public HyperGraph decoding(HyperGraph hg, int sentenceID, BufferedWriter out){		
		//### step-1: run inside-outside
		TrivialInsideOutside p_inside_outside = new TrivialInsideOutside();
		p_inside_outside.run_inside_outside(hg, 0, 1);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		
		
		//### step-2: model extraction based on the definition of Q
		 
		//step-2.1: baseline feature: do not use insideoutside
		this.baseline_feat_tbl.clear();
		FeatureExtractionHG.feature_extraction_hg(hg, this.baseline_feat_tbl, null, this.baseline_ft_template);	
		 
		//step-2.2: ngram feature
		this.ngram_feat_tbl.clear();
		FeatureExtractionHG.feature_extraction_hg(hg, p_inside_outside, this.ngram_feat_tbl, null, this.ngram_ft_template);	
		p_inside_outside.clear_state();
		VariationalLMFeature.getNormalizedLM(this.ngram_feat_tbl, this.baseline_lm_order);	
		
		//### step-3: rank the HG using the baseline and variational feature
		l_feat_functions.clear();		
		
		FeatureFunction base_ff =  new BaselineFF(this.baseline_feat_id, this.baseline_weight, this.baseline_feat_tbl);
		l_feat_functions.add(base_ff);
			
		FeatureFunction variational_ff =  new VariationalLMFeature(this.baseline_lm_feat_id, this.baseline_lm_order, this.p_symbol, this.ngram_feat_tbl,  this.ngam_weight); 
		l_feat_functions.add(variational_ff);
		
		RankHG.rankHG(hg, l_feat_functions);
		
				
		//### step-4: kbest extraction from the reranked HG: remember to add the new feature function into the model list
		//two features: baseline feature, and reranking feature
		KbestExtraction kbestExtractor = new KbestExtraction(p_symbol);
		kbestExtractor.lazy_k_best_extract_hg(hg, this.l_feat_functions, this.topN, this.use_unique_nbest, sentenceID, out, this.use_tree_nbest, this.add_combined_cost);
		
		return hg;
	}
	
	
	
	public static void main(String[] args) throws InterruptedException, IOException {

		/*//##read configuration information
		if(args.length<8){
			System.out.println("wrong command, correct command should be: java Perceptron_HG is_crf lf_train_items lf_train_rules lf_orc_items lf_orc_rules f_l_num_sents f_data_sel f_model_out_prefix use_tm_feat use_lm_feat use_edge_bigram_feat_only f_feature_set use_joint_tm_lm_feature");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}*/
		
		long start_time = System.currentTimeMillis();		
		String f_test_items=args[0].trim();
		String f_test_rules=args[1].trim();
		int num_sents=new Integer(args[2].trim());
		String f_nbest=args[3].trim();
		//num_sents=2;
		//????????????????????????????????????????????????????
		int baseline_lm_feat_id = 0; 
		int baseline_lm_order = 3;//TODO
		double baseline_weight = 0.0;
		double ngram_weight = 1.0;
		//??????????????????????????????????????
		
		Symbol p_symbol = new BuildinSymbol(null);
		BufferedWriter t_writer_nbest =	FileUtility.getWriteFileStream(f_nbest);	
		VariationalDecoder vdecoder = new VariationalDecoder(p_symbol, baseline_lm_feat_id, baseline_weight, ngram_weight, baseline_lm_order);
		

		System.out.println("############Process file  " + f_test_items);
		DiskHyperGraph dhg_test = new DiskHyperGraph(p_symbol, baseline_lm_feat_id); 
		dhg_test.init_read(f_test_items, f_test_rules,null);
			
		for(int sent_id=0; sent_id < num_sents; sent_id ++){
			System.out.println("#Process sentence " + sent_id);
			HyperGraph hg_test = dhg_test.read_hyper_graph();
			vdecoder.decoding(hg_test, sent_id, t_writer_nbest);
			t_writer_nbest.flush();
			//Thread.sleep(3000);
		}
	
	}
	
}
