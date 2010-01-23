package joshua.discriminative.feature_related;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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



public class FeatureIntersection {
	
	
	public static void main(String[] args) {

		//##read configuration information
		if(args.length<8){
			System.out.println("wrong command, correct command should be: java Perceptron_HG is_crf lf_train_items lf_train_rules lf_orc_items lf_orc_rules f_l_num_sents f_data_sel f_model_out_prefix use_tm_feat use_lm_feat use_edge_bigram_feat_only f_feature_set use_joint_tm_lm_feature");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}
		
		String f_l_train_items=args[0].trim();
		String f_l_train_rules=args[1].trim();
		String f_l_num_sents=args[2].trim();
		String f_data_sel=args[3].trim();
		boolean useTMFeat = new Boolean(args[4].trim());
		boolean useLMFeat = new Boolean(args[5].trim());
		boolean useEdgeNgramOnly = new Boolean(args[6].trim());
		String featureFile =  args[7].trim();
		
		boolean saveModelCosts = false;
		
		//????????????????????????????????????????????????????
		int ngramStateID = 0; 
		//??????????????????????????????????????
		
		SymbolTable p_symbol = new BuildinSymbol(null);
		
		//##setup feature templates list
		ArrayList<FeatureTemplate> featTemplates =  new ArrayList<FeatureTemplate>();
		
		boolean useIntegerString = false;
		boolean useRuleIDName = false;
		
		if(useTMFeat==true){
			FeatureTemplate ft = new TMFT(p_symbol, useIntegerString, useRuleIDName);
			featTemplates.add(ft);
		}
		
		int baseline_lm_order = 3;//TODO
		if(useLMFeat==true){
			FeatureTemplate ft = new NgramFT(p_symbol, false, ngramStateID, baseline_lm_order,1,2);//TODO: unigram and bi gram
			featTemplates.add(ft);
		}else if(useEdgeNgramOnly){//exclusive with use_lm_feat
			FeatureTemplate ft = new EdgeBigramFT(p_symbol, ngramStateID, baseline_lm_order, useIntegerString);
			featTemplates.add(ft);
		}		
		
		System.out.println("feature template are " + featTemplates.toString());
		
		List<String> l_file_train_items = DiscriminativeSupport.readFileList(f_l_train_items);
		List<String> l_file_train_rules = DiscriminativeSupport.readFileList(f_l_train_rules);
		
		List<String> l_num_sents = DiscriminativeSupport.readFileList(f_l_num_sents);		
		HashMap<Integer, Boolean> tbl_sent_selected = DiscriminativeSupport.setupDataSelTbl(f_data_sel);//for data selection
		
		HashSet<String> restrictedFeatureSet = new HashSet<String>();
		HashMap<String,Double> featureIntersectionSet = new HashMap<String,Double>();
		
		
		if(featureFile!=null)
			DiscriminativeSupport.loadFeatureSet(featureFile, restrictedFeatureSet);
		else{
			System.out.println("Must specify feature set"); 
			System.exit(0);
		}
			
		//#####begin to do training
		int sentID=0;		
		for(int fid=0; fid < l_file_train_items.size(); fid++){
			System.out.println("############Process file id " + fid);
			DiskHyperGraph diskHG = new DiskHyperGraph(p_symbol, ngramStateID, saveModelCosts, null); 
			diskHG.initRead(l_file_train_items.get(fid), l_file_train_rules.get(fid),tbl_sent_selected);
				
			int total_num_sent = new Integer((String)l_num_sents.get(fid));
			for(int sent_id=0; sent_id < total_num_sent; sent_id ++){
				System.out.println("#Process sentence " + sentID);
				HyperGraph hg = diskHG.readHyperGraph();
				if(hg!=null)//sent is not skipped
					FeatureExtractionHG.featureExtractionOnHG(hg,featureIntersectionSet, restrictedFeatureSet,  featTemplates);
			
				sentID++;
			}
		}
		
		FileUtilityOld.printHashTblAboveThreshold(featureIntersectionSet, featureFile+".intersection", false, 0, false, false, null);
}
}
