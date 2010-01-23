package joshua.discriminative.feature_related;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.feature_template.EdgeBigramFT;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.feature_related.feature_template.NgramFT;
import joshua.discriminative.feature_related.feature_template.TMFT;



public class FeatureSelectionHG {
	
	
	/*for 919 sent, time_on_reading: 148797
	time_on_orc_extract: 580286*/
	public static void main(String[] args) {	
		//long start_time = System.currentTimeMillis();
			
		if(args.length<8){
			System.out.println("wrong command, correct command should be: java FeatureSelection f_l_test_items f_l_test_rules f_l_num_sents use_tm_feat use_lm_feat use_edge_ngram_only use_joint_tm_lm_feature f_feature_set_out [threshold]");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}
		
		String f_l_test_items= args[0].trim();
		String f_l_test_rules= args[1].trim();
		String f_l_num_sents= args[2].trim();		
		boolean use_tm_feat = new Boolean(args[3].trim());
		boolean use_lm_feat = new Boolean(args[4].trim());
		boolean use_edge_ngram_only = new Boolean(args[5].trim());
		boolean use_joint_tm_lm_feature = new Boolean(args[6].trim());
		String f_feature_set_out= args[7].trim();//output file
		int baseline_lm_order = new Integer(args[8].trim());
		double threshold = 0.0;
		if(args.length>9) threshold = new Double(args[9].trim());
		
		boolean saveModelScore = true;//diskHG have costs stored
		
//		????????????????????????????????????????????????????
		int ngramStateID = 0; 
		//??????????????????????????????????????
		
		boolean addBaselineFeature = true;//TODO
		String baselineFeatureName = "baseline_lzf";//TODO
		
		
		SymbolTable p_symbol = new BuildinSymbol(null);
		
//		##setup feature templates list
		ArrayList<FeatureTemplate> featureTemplates =  new ArrayList<FeatureTemplate>();
		
		boolean useIntegerString = false;
		boolean useRuleIDName = false;
		
		if(use_tm_feat==true){
			FeatureTemplate ft = new TMFT(p_symbol, useIntegerString, useRuleIDName);
			featureTemplates.add(ft);
		}
		
		if(use_lm_feat==true){
			FeatureTemplate ft = new NgramFT(p_symbol, false, ngramStateID, baseline_lm_order, 1 ,2);//TODO: unigram and bi gram
			featureTemplates.add(ft);
		}else if(use_edge_ngram_only){//exclusive with use_lm_feat
			FeatureTemplate ft = new EdgeBigramFT(p_symbol, ngramStateID, baseline_lm_order, useIntegerString);
			featureTemplates.add(ft);
		}
		
		if(use_joint_tm_lm_feature){
			//TODO: not implement
			System.out.println("not implemented"); System.exit(0);
		}
		
		
		List<String> testItemsFiles = DiscriminativeSupport.readFileList(f_l_test_items);
		List<String> testRulesFiles = DiscriminativeSupport.readFileList(f_l_test_rules);
		List<String> l_num_sents = DiscriminativeSupport.readFileList(f_l_num_sents);				
	
		//#### extract feat tbl
		HashMap<String, Double> tbl_feats = new HashMap<String, Double>();
				
		for(int fid=0; fid < testItemsFiles.size(); fid++){
			System.out.println("############Process file id " + fid);
			DiskHyperGraph dhg_train = new DiskHyperGraph(p_symbol, ngramStateID, saveModelScore, null); 
			dhg_train.initRead((String)testItemsFiles.get(fid), (String)testRulesFiles.get(fid),null);		
			int total_num_sent = new Integer((String)l_num_sents.get(fid));
			for(int sent_id=0; sent_id < total_num_sent; sent_id ++){
				System.out.println("############Process sentence " + sent_id);
				HyperGraph hg_train = dhg_train.readHyperGraph();				
				FeatureExtractionHG.featureExtractionOnHG(hg_train, tbl_feats, null, featureTemplates);
			}
		}
		System.out.println("===feature table size is " + tbl_feats.size());
		//#### write hashtable
		boolean useZeroValue = true;
		boolean keyOnly = false;		
		FileUtilityOld.printHashTblAboveThreshold(tbl_feats, f_feature_set_out, keyOnly, threshold, useZeroValue, addBaselineFeature, baselineFeatureName);
		
	}	
	
	
	
	
}
