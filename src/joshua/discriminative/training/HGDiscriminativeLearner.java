package joshua.discriminative.training;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DefaultInsideOutside;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.FeatureBasedInsideOutside;
import joshua.discriminative.feature_related.FeatureExtractionHG;
import joshua.discriminative.feature_related.feature_template.BaselineFT;
import joshua.discriminative.feature_related.feature_template.EdgeBigramFT;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.feature_related.feature_template.NgramFT;
import joshua.discriminative.feature_related.feature_template.TMFT;
import joshua.discriminative.ranker.RescorerHGSimple;
import joshua.discriminative.training.learning_algorithm.DefaultCRF;
import joshua.discriminative.training.learning_algorithm.DefaultPerceptron;
import joshua.discriminative.training.learning_algorithm.GradientBasedOptimizer;



/* This class inplement hypergraph-based discriminative reranking
 * (1) hypergraph-based FeatureTemplate
 * (2) reranking related: baseline feature
 * (3) batch updates
 * */

public class HGDiscriminativeLearner  {
	
	GradientBasedOptimizer optimizer =  null;
	static boolean usingCRF = true;//default is crf
	static boolean usingStringOracle = false;//TODO
	
	//##feature realted
	HashMap empiricalFeatsTbl = new HashMap();//experical feature counts
	HashMap modelFeatsTbl = new HashMap();	//feature counts assigned by model
	HashSet<String> restrictedFeatureSet = null;// only consider feature in this set, if null, then ignore this
	
	//## batch update related
	int numProcessedExamples=0;
	
	//## reranking: baseline
	public static String baselineFeatName ="baseline_lzf";
	static boolean fixBaseline= true;//TODO: the current version assume we always fix the baseline during training
	static double baselineScale = 1.0; //the scale for baseline relative to the corrective score
	
	RescorerHGSimple reranker = new RescorerHGSimple();
	
	public HGDiscriminativeLearner(GradientBasedOptimizer optimizer, HashSet<String> restrictedFeatureSet){
		this.optimizer = optimizer;		
		this.restrictedFeatureSet = restrictedFeatureSet;
	}
	
	//all hyp are represented as a hyper-graph
	public void processOneSent(HyperGraph fullHG, Object oracle, String refSent, List<FeatureTemplate> featTemplates, List<FeatureTemplate> featTemplatesNobaseline){
		 //String sent_original_1best_debug =  HyperGraph.extract_best_string(hg_full.goal_item);
		 //String sent_rerank_1best_debug = null;
		 //String orc_string_debug= null;
		  
		//HyperGraph  hg_original_1best = hg_full.get_1best_tree_hg();//debug #### find 1-best based on original model (no corrective)
		
		//####feature extraction using the current model, get g_tbl_feats_model
		if(usingCRF){
			DefaultInsideOutside insideOutsider = new FeatureBasedInsideOutside(optimizer.getSumModel(), featTemplates, restrictedFeatureSet);//do inference using current model
			insideOutsider.runInsideOutside(fullHG, 0, 1,1);
			//inside_outside.sanity_check_hg(hg_full);
			FeatureExtractionHG.featureExtractionOnHG(fullHG, insideOutsider, modelFeatsTbl, restrictedFeatureSet, featTemplates);	
			insideOutsider.clearState();
			//sent_rerank_1best_debug = RescorerHG.rerank_hg_and_get_1best_string(hg_full, p_optimizer.get_sum_model(), g_baseline_scale, g_restricted_feature_set, l_feat_templates_nobaseline, false);
		}else{//perceptron	
			HyperGraph  rerankedOnebest = reranker.rerankHGAndGet1best(fullHG, optimizer.getSumModel(), restrictedFeatureSet, featTemplatesNobaseline, false);
			FeatureExtractionHG.featureExtractionOnHG(rerankedOnebest, modelFeatsTbl, restrictedFeatureSet, featTemplates);
			//sent_rerank_1best_debug = HyperGraph.extract_best_string(hg_reranked_1best.goal_item);
		}
		
		//####feature extraction on emperical, get  g_tbl_feats_emperical
		//TODO in the case of hidden variable, we should run inside-outsude to get the expectation under current model
	
		if(usingStringOracle==false){
			FeatureExtractionHG.featureExtractionOnHG((HyperGraph) oracle, null,  empiricalFeatsTbl, restrictedFeatureSet, featTemplates);
			//orc_string_debug = HyperGraph.extract_best_string(((HyperGraph)oracle).goal_item);
		}else{			
			NBESTDiscriminativeLearner.featureExtraction((String)oracle, empiricalFeatsTbl, restrictedFeatureSet, 0, false);
			//orc_string_debug = (String)oracle;
		}
		

		//double original_bleu = OracleExtractionHG.compute_sentence_bleu(orc_string_debug, sent_original_1best_debug, true, 4);
		//double reranked_bleu = OracleExtractionHG.compute_sentence_bleu(orc_string_debug, sent_rerank_1best_debug, true, 4);
		//System.out.println("reranked bleu: " + reranked_bleu + "; original bleu: " + original_bleu + "; difference: " + (reranked_bleu-original_bleu));
		/*System.out.println("g_tbl_feats_model size: " + g_tbl_feats_model.size());
		System.out.println("g_tbl_feats_emperical size: " + g_tbl_feats_empirical.size());
		System.out.println("g_restricted_feature_set size: " + g_restricted_feature_set.size());
		*/
		//####common: update the models
		numProcessedExamples++;
		update_model(false);
		
		
//		debug begin
		/*
        //###########DEBUG
        //System.out.println("sum table size is " + g_tbl_sum_model.size());
        //System.out.println("avg table size is " + g_tbl_avg_model.size());
        //Support.print_hash_tbl(g_tbl_sum_model);
        //Support.print_hash_tbl(g_tbl_avg_model);
        String sent_orc =  HyperGraph.extract_best_string(hg_oracle2.goal_item);
        String sent_original_1best =  HyperGraph.extract_best_string(hg_original_1best.goal_item);
        String sent_reranked_1best =  HyperGraph.extract_best_string(hg_reranked_1best.goal_item);
        //System.out.println("ref: " + ref_sent);
        //System.out.println("orc: " + sent_orc);
        //System.out.println("1or: " + sent_original_1best);
        //System.out.println("1re: " + sent_reranked_1best);
        //OracleExtractor.compute_sentence_bleu(ref_sent, sent_orc, true);
        //OracleExtractor.compute_sentence_bleu(ref_sent,sent_original_1best, true );
        //OracleExtractor.compute_sentence_bleu(ref_sent,sent_reranked_1best, true );
        //OracleExtractor.compute_sentence_bleu(sent_orc,sent_1best );
        //##################END*/
		//end
		
	}
	

	public void update_model(boolean force_update){
		if(force_update || numProcessedExamples>=optimizer.getBatchSize()){
			/*//debug
			System.out.println("baseline feature emprical " + g_tbl_feats_empirical.get(g_baseline_feat_name));
			System.out.println("baseline feature model "    + g_tbl_feats_model.get(g_baseline_feat_name));
			//edn*/
			
			optimizer.updateModel(empiricalFeatsTbl, modelFeatsTbl);
			//System.out.println("baseline feature weight " + p_optimizer.get_sum_model().get(g_baseline_feat_name));
			reset_baseline_feat();
			//System.out.println("baseline feature weight " + p_optimizer.get_sum_model().get(g_baseline_feat_name));
			
			empiricalFeatsTbl.clear();
			modelFeatsTbl.clear();
			numProcessedExamples=0;						
		}	
	}
	
	public void reset_baseline_feat(){
		if(fixBaseline)
			optimizer.setFeatureWeight(baselineFeatName, baselineScale);
		else{
			System.out.println("not implemented"); System.exit(0);
		}	
	}
	

	public static void main(String[] args) {
		//##read configuration information
		if(args.length<11){
			System.out.println("wrong command, correct command should be: java Perceptron_HG is_crf lf_train_items lf_train_rules lf_orc_items lf_orc_rules f_l_num_sents f_data_sel f_model_out_prefix use_tm_feat use_lm_feat use_edge_bigram_feat_only f_feature_set use_joint_tm_lm_feature");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}
		long start_time = System.currentTimeMillis();
		SymbolTable symbolTbl = new BuildinSymbol(null);	
		boolean is_using_crf =  new Boolean(args[0].trim());
		HGDiscriminativeLearner.usingCRF=is_using_crf;
		String f_l_train_items=args[1].trim();
		String f_l_train_rules=args[2].trim();
		String f_l_orc_items=args[3].trim();
		String f_l_orc_rules=args[4].trim();
		String f_l_num_sents=args[5].trim();
		String f_data_sel=args[6].trim();
		String f_model_out_prefix=args[7].trim();
		boolean use_tm_feat = new Boolean(args[8].trim());
		boolean use_lm_feat = new Boolean(args[9].trim());
		boolean use_edge_ngram_only = new Boolean(args[10].trim());
		String f_feature_set = null;
		if(args.length>11) f_feature_set = args[11].trim();
	
		boolean use_joint_tm_lm_feature = false;
		if(args.length>12) use_joint_tm_lm_feature = new Boolean(args[12].trim());		
		
		
		boolean saveModelCosts = true;
		
//		????????????????????????????????????????????????????
		int ngramStateID = 0; 
		//??????????????????????????????????????
		
		//##setup feature templates list
		ArrayList<FeatureTemplate> l_feat_templates =  new ArrayList<FeatureTemplate>();
		ArrayList<FeatureTemplate> l_feat_templates_nobaseline =  new ArrayList<FeatureTemplate>();
		
		FeatureTemplate ft_bs = new BaselineFT(baselineFeatName, true);//baseline feature
		l_feat_templates.add(ft_bs);		
		
		
		boolean useIntegerString = false;
		boolean useRuleIDName = false;
		
		if(use_tm_feat==true){
			FeatureTemplate ft = new TMFT(symbolTbl, useIntegerString, useRuleIDName);
			l_feat_templates.add(ft);
			l_feat_templates_nobaseline.add(ft);
		}
		
	 
		
		int baseline_lm_order = 3;//TODO
		if(use_lm_feat==true){
			FeatureTemplate ft = new NgramFT(symbolTbl, false, ngramStateID, baseline_lm_order,1,2);//TODO: unigram and bi gram
			l_feat_templates.add(ft);
			l_feat_templates_nobaseline.add(ft);
		}else if(use_edge_ngram_only){//exclusive with use_lm_feat
			FeatureTemplate ft = new EdgeBigramFT(symbolTbl, ngramStateID, baseline_lm_order, useIntegerString);
			l_feat_templates.add(ft);
			l_feat_templates_nobaseline.add(ft);
		}
		
		if(use_joint_tm_lm_feature){
			//TODO: not implement
		}
		
		System.out.println("feature template are " + l_feat_templates.toString());
		System.out.println("feature template(no baseline) are " + l_feat_templates_nobaseline.toString());

		int max_loop = 3;//TODO
		
		List<String> l_file_train_items = DiscriminativeSupport.readFileList(f_l_train_items);
		List<String> l_file_train_rules = DiscriminativeSupport.readFileList(f_l_train_rules);
		List<String> l_file_orc_items = DiscriminativeSupport.readFileList(f_l_orc_items);
		List<String> l_file_orc_rules =null;
		if(f_l_orc_rules.compareTo("flat")!=0){//TODO: oracle is a hg, not a flat string
			System.out.println("oracles are in a hypergraph by " + f_l_orc_rules);
			HGDiscriminativeLearner.usingStringOracle=false;
			l_file_orc_rules = DiscriminativeSupport.readFileList(f_l_orc_rules);
		}else{
			System.out.println("flat oracles");
			HGDiscriminativeLearner.usingStringOracle=true;
		}
		
		List<String> l_num_sents = DiscriminativeSupport.readFileList(f_l_num_sents);		
		HashMap tbl_sent_selected = DiscriminativeSupport.setupDataSelTbl(f_data_sel);//for data selection
		
		//###### INIT Model ###################
		HGDiscriminativeLearner hgdl = null;	
		//TODO optimal parameters
		int train_size = 610000;
		int batch_update_size = 30;
		int converge_pass =  1;
		double init_gain = 0.1;
		double sigma = 0.5;
		boolean is_minimize_score = true;
		
		//setup optimizer
		GradientBasedOptimizer optimizer = null;
		if(usingCRF){
			HashMap crfModel = new HashMap();
			if(f_feature_set!=null){
				DiscriminativeSupport.loadModel(f_feature_set, crfModel, null);
			}else{
				System.out.println("In crf, must specify feature set"); System.exit(0);
			}
			optimizer = new DefaultCRF(crfModel, train_size, batch_update_size, converge_pass, init_gain, sigma, is_minimize_score);
			optimizer.initModel(-1, 1);//TODO optimal initial parameters
			hgdl = new HGDiscriminativeLearner(optimizer, new HashSet<String>(crfModel.keySet()));
			hgdl.reset_baseline_feat();//add and init baseline feature
			System.out.println("size3: " + optimizer.getSumModel().size());
		}else{//perceptron
			HashMap perceptron_sum_model = new HashMap();
			HashMap perceptron_avg_model = new HashMap();
			HashMap perceptronModel = new HashMap();
			if(f_feature_set!=null){
				DiscriminativeSupport.loadModel(f_feature_set, perceptronModel, null);
				perceptronModel.put(baselineFeatName, 1.0);
				System.out.println("feature set size is " + perceptronModel.size());
			}else{
				System.out.println("In perceptron, should specify feature set");				
			}
			optimizer = new DefaultPerceptron(perceptron_sum_model, perceptron_avg_model,train_size, batch_update_size, converge_pass, init_gain, sigma, is_minimize_score);
			hgdl = new HGDiscriminativeLearner(optimizer,  new HashSet<String>(perceptronModel.keySet()));
			hgdl.reset_baseline_feat();
		}		
				
		//#####begin to do training
		int g_sent_id=0;
		for(int loop_id=0; loop_id<max_loop; loop_id++){
			System.out.println("###################################Loop " + loop_id);
			for(int fid=0; fid < l_file_train_items.size(); fid++){
				System.out.println("############Process file id " + fid);
				DiskHyperGraph dhg_train = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null); 
				dhg_train.initRead((String)l_file_train_items.get(fid), (String)l_file_train_rules.get(fid),tbl_sent_selected);
				DiskHyperGraph dhg_orc =null;
				BufferedReader t_reader_orc =null;
				if(l_file_orc_rules!=null){
					dhg_orc = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null);
					dhg_orc.initRead((String)l_file_orc_items.get(fid), (String)l_file_orc_rules.get(fid), tbl_sent_selected);
				}else{
					t_reader_orc = FileUtilityOld.getReadFileStream((String)l_file_orc_items.get(fid),"UTF-8");
				}
					
				int total_num_sent = new Integer((String)l_num_sents.get(fid));
				for(int sent_id=0; sent_id < total_num_sent; sent_id ++){
					System.out.println("#Process sentence " + g_sent_id);
					HyperGraph hg_train = dhg_train.readHyperGraph();
					HyperGraph hg_orc =null;
					String hyp_oracle =null;
					if(l_file_orc_rules!=null)
						hg_orc = dhg_orc.readHyperGraph();
					else
						hyp_oracle = FileUtilityOld.readLineLzf(t_reader_orc);
					if(hg_train!=null){//sent is not skipped
						if(l_file_orc_rules!=null)
							hgdl.processOneSent( hg_train, hg_orc, null, l_feat_templates, l_feat_templates_nobaseline);
						else
							hgdl.processOneSent( hg_train, hyp_oracle, null, l_feat_templates, l_feat_templates_nobaseline);
					}
					g_sent_id++;
				}
			}
		
			if(usingCRF){
				hgdl.update_model(true);
				FileUtilityOld.printHashTbl(optimizer.getSumModel(), f_model_out_prefix+".crf." + loop_id, false, false);
			}else{//perceptron
				hgdl.update_model(true);
				((DefaultPerceptron)optimizer).force_update_avg_model();
				FileUtilityOld.printHashTbl(optimizer.getSumModel(), f_model_out_prefix+".sum." + loop_id, false, false);
				FileUtilityOld.printHashTbl(optimizer.getAvgModel(), f_model_out_prefix+".avg." + loop_id, false, true);
			}
			System.out.println("Time cost: " + ((System.currentTimeMillis()-start_time)/1000));
		}
	}
	

	
}
