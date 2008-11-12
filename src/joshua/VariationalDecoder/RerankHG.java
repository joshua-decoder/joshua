package edu.jhu.joshua.VariationalDecoder;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;

import edu.jhu.joshua.discriminative_training.DiscriminativeSupport;
import edu.jhu.joshua.discriminative_training.feature_related.FeatureTemplate;
import edu.jhu.joshua.discriminative_training.feature_related.FeatureTemplateEdgeBigram;
import edu.jhu.joshua.discriminative_training.feature_related.DiscriminativeNgramFeatureTemplate;
import edu.jhu.joshua.discriminative_training.feature_related.FeatureTemplateTM;

import joshua.decoder.BuildinSymbol;
import joshua.decoder.Symbol;
import joshua.decoder.ff.FFTransitionResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.util.FileUtility;

/*This class rescore the hypergraph, and directly change the one-best pointer and costs
 * (Note: we should not change the order of dependency, that is, all features are local)
 * */

/*to compute reranked score, we need to maintain corrective score, instead of just computing the final scores bottom-up,
 * because we do not know the transition cost by baseline model of each deduction (or the transition cost is changed)*/

 public class RerankHG {
	static private HashMap g_tbl_processed_items =  new HashMap();//help to tranverse a hypergraph	
	static private double g_scale_baseline = 1.0;//the scale for the current cost in the HG
	static ArrayList<FeatureFunction> l_reank_feat_functions;

//	###########  public interfaces
//	the baseline scale must be explicitly indicated in baseline_scale, instead of implicitly indicated in corrective_model
	
	//return the reranked onebest hg
	//	corrective model: this should not have the baseline feature
	static public HyperGraph rerank_hg_and_get_1best_hg(HyperGraph hg,  ArrayList<FeatureFunction> reank_feat_functions, double baseline_scale){
		adjust_cost_hg( hg, reank_feat_functions, baseline_scale);
		return   hg.get_1best_tree_hg();			
	}
	
	static public void adjust_cost_hg(HyperGraph hg,  ArrayList<FeatureFunction> reank_feat_functions, double baseline_scale){
		//### change the best_pointer and best_cost in hypergraph in hg
		g_tbl_processed_items.clear();
		g_scale_baseline =  baseline_scale;
		l_reank_feat_functions = reank_feat_functions;
		adjust_item_cost(hg.goal_item);
		g_tbl_processed_items.clear();
	}
//########### end public interfaces	
	
	//item: recursively call my children deductions, change pointer for best_deduction, and remember changed_cost 
	static private  void adjust_item_cost(HGNode it ){
		if(g_tbl_processed_items.containsKey(it))	
			return;
		g_tbl_processed_items.put(it,1);
		double old_best_deduction_cost = it.best_deduction.best_cost;
		
		//##add item-specific feature score, deduction-independent
		double item_corrective_score = adjust_item_cost_helper(it);//we will factor this cost into all the children deductions
		
		//### recursively call my children deductions, change pointer for best_deduction
		it.best_deduction=null;
		for(HyperEdge dt : it.l_deductions){					
			adjust_deduction_cost(it, dt, item_corrective_score );//deduction-specifc feature
			if(it.best_deduction==null || dt.best_cost < it.best_deduction.best_cost) it.best_deduction = dt;//prefer smaller cost
		}		
		
		//## remember the change in the best_deduction cost
		double cost_changed = it.best_deduction.best_cost - old_best_deduction_cost;
		g_tbl_processed_items.put(it, cost_changed);
	}
	
	
	//adjust best_cost, and recursively call my ant items
	//parent_changed_cost;//deduction-idenpendent parent item cost
	static private void adjust_deduction_cost(HGNode parent_item, HyperEdge dt,  double parent_item_corrective_score){
		//###first adjust my ant items, and add their corrective scores
		double cost_changed = 0;
		if(dt.get_ant_items()!=null){
			for(HGNode ant_it : dt.get_ant_items()){
				adjust_item_cost(ant_it);
				cost_changed += (Double) g_tbl_processed_items.get(ant_it);//we must do this way as each item will be processed only once
			}
		}
		//###then myself
		double deduct_corrective_score =  adjust_deduction_cost_helper(parent_item, dt);
		
		//###do the real change: change the deduction's best_cost
		if(g_scale_baseline!=0){		
			double scale_corrective;
			scale_corrective= 1.0/g_scale_baseline;		
			cost_changed += scale_corrective * (parent_item_corrective_score + deduct_corrective_score);//note: factor parent_item_corrective_score in
			dt.best_cost += cost_changed;
		}else
			dt.best_cost = parent_item_corrective_score + deduct_corrective_score;//do not use baseline score at all
	}	
	
	//get deduction-independent item corrective score
	static private double adjust_item_cost_helper(HGNode it){
		double res =0;
		//HashMap tbl_feats = new HashMap();
		//feature_extraction_item_helper(it , tbl_feats);
		//res= compute_corrective_score(corrective_model, tbl_feats, use_avg_model);
		return res;
	}
	
	//give a dt and pointers to ant items, find all features that apply, non-recursive
	static private double adjust_deduction_cost_helper(HGNode parent_item, HyperEdge dt ){
		double transition_cost_sum    = 0.0;		
		for (FeatureFunction ff : l_reank_feat_functions) {
			FFTransitionResult state =  HyperGraph.computeTransition(dt, dt.get_rule(), dt.get_ant_items(), ff, parent_item.i, parent_item.j);
			transition_cost_sum	+= ff.getWeight() * state.getTransitionCost();
		}		
		return transition_cost_sum;
	}

	
	
//################## example main funciton #############################
	public static void main(String[] args) {
		if(args.length<11){
			System.out.println("wrong command, correct command should be: java Perceptron_TEST is_avg_model is_nbest f_test_items f_test_rules num_sent f_perceptron_model baseline_scale use_tm_feat use_lm_feat use_edge_bigram_feat_only freranked_1best");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);	System.exit(0);		
		}
		long start_time = System.currentTimeMillis();

		boolean is_avg_model = new Boolean(args[0].trim());
		boolean is_nbest = new Boolean(args[1].trim());
		String f_test_items=args[2].trim();
		String f_test_rules=args[3].trim();
		int num_sent = new Integer(args[4].trim());
		String f_perceptron_model = args[5].trim();
		double baseline_scale = new Double(args[6].trim()); 
		boolean use_tm_feat = new Boolean(args[7].trim());
		boolean use_lm_feat = new Boolean(args[8].trim());
		boolean use_edge_ngram_only = new Boolean(args[9].trim());
		String freranked_1best = args[10].trim();

		String f_feature_set = null;
		if(args.length>11) f_feature_set = args[11].trim();		
		
//		????????????????????????????????????????????????????
		int baseline_lm_feat_id = 0; 
		//??????????????????????????????????????
		
		Symbol p_symbol = new BuildinSymbol(null);
		
		//####### nbest decoding
		/*if(is_nbest){
			//TODO
		}else{//#####hg decoding*/
		
			
			//##setup feature templates list
			ArrayList<FeatureTemplate> l_feat_templates =  new ArrayList<FeatureTemplate>();	
			
			//we should not add the baseline template
			//FeatureTemplate ft_bs = new FeatureTemplateBaseline(HGDiscriminativeLearner.g_baseline_feat_name, false);//baseline feature
			//l_feat_templates.add(ft_bs);	
			
			if(use_tm_feat==true){
				FeatureTemplate ft = new FeatureTemplateTM(p_symbol, null);
				l_feat_templates.add(ft);
			}
				
			int baseline_lm_order = 3;//TODO
			if(use_lm_feat==true){
				FeatureTemplate ft = new DiscriminativeNgramFeatureTemplate(p_symbol, false, baseline_lm_feat_id, baseline_lm_order,1,2);//TODO: unigram and bi gram
				l_feat_templates.add(ft);
			}else if(use_edge_ngram_only){//exclusive with use_lm_feat
				FeatureTemplate ft = new FeatureTemplateEdgeBigram(p_symbol, baseline_lm_feat_id, baseline_lm_order);
				l_feat_templates.add(ft);
			}		
							
			//####### restricted feature set : normally this is not used as the model itself is a restriction
			HashMap restricted_feature_set = null;
			if(f_feature_set!=null){
				restricted_feature_set = new HashMap();
				DiscriminativeSupport.load_feature_set(f_feature_set,restricted_feature_set);
				//restricted_feature_set.put(HGDiscriminativeLearner.g_baseline_feat_name, 1.0); //should not add the baseline feature
			}
					
			HashMap tbl_model =  new HashMap();
			if(is_avg_model){
				DiscriminativeSupport.load_avg_percetron_model(f_perceptron_model, tbl_model, is_nbest);
			}else{
				DiscriminativeSupport.load_regular_model(f_perceptron_model, tbl_model, is_nbest);
			}
			
			BufferedWriter out_1best = FileUtility.getWriteFileStream(freranked_1best);
			
			DiskHyperGraph dhg_test = new DiskHyperGraph(p_symbol, baseline_lm_feat_id); 
			dhg_test.init_read(f_test_items, f_test_rules, null);
			for(int sent_id=0; sent_id < num_sent; sent_id ++){
				System.out.println("#Process sentence " + sent_id);
				HyperGraph hg_test = dhg_test.read_hyper_graph();
				HyperGraph hg_reranked_1best = RerankHG.rerank_hg_and_get_1best_hg(hg_test,  null,  baseline_scale);
				String reranked_1best = HyperGraph.extract_best_string(p_symbol, hg_reranked_1best.goal_item);
				
				FileUtility.write_lzf(out_1best, reranked_1best + "\n");				
			}
			FileUtility.close_write_file(out_1best);
		//}
		System.out.println("Time cost: " + ((System.currentTimeMillis()-start_time)/1000));
	}
//	################## end of example main funciton #############################
}
