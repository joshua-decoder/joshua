package joshua.discriminative.variational_decoder;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.hypergraph.TrivialInsideOutside;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.feature_function.FeatureTemplateBasedFF;
import joshua.discriminative.feature_related.feature_function.EdgeTblBasedBaselineFF;
import joshua.discriminative.ranker.HGRanker;




public class VariationalDecoder {	
	
	private int topN=300;
	private boolean useUniqueNbest =true;
	private boolean useTreeNbest = false;
	private boolean addCombinedCost = true;	
	
	private KBestExtractor kbestExtractor;
	
	SymbolTable symbolTbl;
	List<FeatureFunction> featFunctions ; //for HG reranking and kbest extraction
	HashMap<VariationalNgramApproximator, FeatureTemplateBasedFF> approximatorMap;
	
	
	double insideOutsideScalingFactor= 0.5;

	HGRanker ranker;
	
	public VariationalDecoder(){
		//do nothing;
	}
	
	
	
	
//	return changed hg
	public HyperGraph decoding(HyperGraph hg, int sentenceID, BufferedWriter out) {		
	
		//=== step-1: run inside-outside
		//note, inside and outside will use the transition_cost of each hyperedge, this cost is already linearly interpolated
		TrivialInsideOutside pInsideOutside = new TrivialInsideOutside();
		pInsideOutside.runInsideOutside(hg, 0, 1, insideOutsideScalingFactor);//ADD_MODE=0=sum; LOG_SEMIRING=1;
		
		//=== initialize baseline table
		//TODO:???????????????????? 
		((EdgeTblBasedBaselineFF)featFunctions.get(0)).collectTransitionLogPs(hg);
		
		//=== step-2: model extraction based on the definition of Q
		for(Map.Entry<VariationalNgramApproximator, FeatureTemplateBasedFF> entry : approximatorMap.entrySet()){
			VariationalNgramApproximator approximator = entry.getKey();
			FeatureTemplateBasedFF featureFunction = entry.getValue();
			HashMap<String, Double> model = approximator.estimateModel(hg, pInsideOutside);
			featureFunction.setModel(model);			
		}
		
		//clean up
		pInsideOutside.clearState();
		
		//=== step-3: rank the HG using the baseline and variational feature
		this.ranker.rankHG(hg);

		//=== step-4: kbest extraction from the reranked HG: remember to add the new feature function into the model list
		try{
			kbestExtractor.lazyKBestExtractOnHG(hg, this.featFunctions, this.topN, sentenceID, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return hg;
	}
	
	

	public void initializeDecoder(String configFile){
		VariationalDecoderConfiguration.readConfigFile(configFile);
		this.symbolTbl = new BuildinSymbol(null);
		this.featFunctions = new ArrayList<FeatureFunction>();
		this.ranker = new HGRanker(featFunctions);
		this.approximatorMap = new HashMap<VariationalNgramApproximator, FeatureTemplateBasedFF>();		
		VariationalDecoderConfiguration.initializeModels(configFile, this.symbolTbl, this.featFunctions, this.approximatorMap);		
		this.insideOutsideScalingFactor =  VariationalDecoderConfiguration.insideoutsideScalingFactor;

		//this.kbestExtractor = new KBestExtractor(p_symbol);	
		this.kbestExtractor = new KBestExtractor(this.symbolTbl, this.useUniqueNbest, this.useTreeNbest, false, this.addCombinedCost, false, true);
	}
	
	public void  decodingTestSet(String testItemsFile, String testRulesFile, int numSents, String nbestFile) {

		BufferedWriter nbestWriter =	FileUtilityOld.getWriteFileStream(nbestFile);	
		System.out.println("############Process file  " + testItemsFile);
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, VariationalDecoderConfiguration.ngramStateID, true, null); //have model costs stored
		diskHG.initRead(testItemsFile, testRulesFile,null);
			
		for(int sentID=0; sentID < numSents; sentID ++){
			System.out.println("#Process sentence " + sentID);
			HyperGraph testhg = diskHG.readHyperGraph();
			/*if(use_constituent_decoding)
				vdecoder.constitudent_decoding(hg_test, sent_id, t_writer_nbest);
			else*/
				decoding(testhg, sentID, nbestWriter);
			
			//Thread.sleep(3000);
		}
		FileUtilityOld.closeWriteFile(nbestWriter);
	} 
	
	public void writeConfigFile(double[] newWeights, String template, String fileToWrite){
		BufferedReader configReader = FileUtilityOld.getReadFileStream(template);
		BufferedWriter configWriter =	FileUtilityOld.getWriteFileStream(fileToWrite);		
		String line;
		int featID = 0;
		while ((line = FileUtilityOld.readLineLzf(configReader)) != null) {
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$") || line.indexOf("=") != -1) {//comment, empty line, or parameter lines: just copy
				 FileUtilityOld.writeLzf(configWriter, line + "\n");
			}else{//models: replace the weight
				String[] fds = line.split("\\s+");
				StringBuffer new_line = new StringBuffer();
				if(fds[fds.length-1].matches("^[\\d\\.\\-\\+]+")==false){System.out.println("last field is not a number, must be wrong; the field is: " + fds[fds.length-1]); System.exit(1);};
				for(int i=0; i<fds.length-1; i++){
					new_line.append(fds[i]);
					new_line.append(" ");
				}
				new_line.append(newWeights[featID++]);	
				FileUtilityOld.writeLzf(configWriter, new_line.toString() + "\n");
			}				
		}
		if(featID!=newWeights.length){System.out.println("number of models does not match number of weights, must be wrong"); System.exit(1);};
		FileUtilityOld.closeReadFile(configReader);
		FileUtilityOld.closeWriteFile(configWriter);		
	}
	
	/*this assumes that the weight_vector is ordered according to the decoder config file
	 * */
	public void changeFeatureWeightVector(double[] weight_vector){
		if(featFunctions.size()!=weight_vector.length){
			System.out.println("In updateFeatureWeightVector: number of weights does not match number of feature functions");
			System.exit(0);
		}		
		for(int i=0; i<featFunctions.size(); i++){
			FeatureFunction ff = featFunctions.get(i);
			double old_weight = ff.getWeight();
			ff.setWeight(weight_vector[i]);
			System.out.println("Feature function : " + ff.getClass().getSimpleName() + "; weight changed from " + old_weight + " to " + ff.getWeight());
		}
	}
	
	
	
	
	//============================ main function ==============================
	public static void main(String[] args) throws InterruptedException, IOException {

		/*//##read configuration information
		if(args.length<8){
			System.out.println("wrong command, correct command should be: java Perceptron_HG is_crf lf_train_items lf_train_rules lf_orc_items lf_orc_rules f_l_num_sents f_data_sel f_model_out_prefix use_tm_feat use_lm_feat use_edge_bigram_feat_only f_feature_set use_joint_tm_lm_feature");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}*/
		if(args.length!=5){
			System.out.println("Wrong number of parameters, it must be  5");
			System.exit(1);
		}
		
		//long start_time = System.currentTimeMillis();		
		String testItemsFile=args[0].trim();
		String testRulesFile=args[1].trim();
		int numSents=new Integer(args[2].trim());
		String nbestFile=args[3].trim();
		String configFile=args[4].trim();
		
		VariationalDecoder vdecoder = new VariationalDecoder();
		vdecoder.initializeDecoder(configFile);
		vdecoder.decodingTestSet(testItemsFile, testRulesFile, numSents, nbestFile);
	}
	
	
	
	/*
	//return changed hg
	public HyperGraph decoding_old(HyperGraph hg, int sentenceID, BufferedWriter out){		
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
		VariationalLMFeature.getNormalizedLM(this.ngram_feat_tbl, this.baseline_lm_order, false, false, false);	
		
		//step-2.2: constituent feature
		this.constituent_feat_tbl.clear();
		FeatureExtractionHG.feature_extraction_hg(hg, p_inside_outside, this.constituent_feat_tbl, null, this.constituent_template);	
		
		//clean
		p_inside_outside.clear_state();
		
		//### step-3: rank the HG using the baseline and variational feature
		l_feat_functions.clear();		
		
		FeatureFunction base_ff =  new BaselineFF(this.baseline_feat_id, this.baseline_weight, this.baseline_feat_tbl);
		l_feat_functions.add(base_ff);
			
		FeatureFunction variational_ff =  new VariationalLMFeature(this.baseline_lm_feat_id, this.baseline_lm_order, this.p_symbol, this.ngram_feat_tbl,  this.ngam_weight, false, false, false); 
		l_feat_functions.add(variational_ff);
		
		FeatureFunction constituent_ff =  new BaselineFF(this.constituent_feat_id, this.constituent_weight, this.constituent_feat_tbl);
		l_feat_functions.add(constituent_ff);
		
		RankHG.rankHG(hg, l_feat_functions);
		
				
		//### step-4: kbest extraction from the reranked HG: remember to add the new feature function into the model list
		//two features: baseline feature, and reranking feature
		KbestExtraction kbestExtractor = new KbestExtraction(p_symbol);
		kbestExtractor.lazy_k_best_extract_hg(hg, this.l_feat_functions, this.topN, this.use_unique_nbest, sentenceID, out, this.use_tree_nbest, this.add_combined_cost);
		
		return hg;
	}
	
	
	
	
//	return changed hg
	public HyperGraph constitudent_decoding(HyperGraph hg, int sentenceID, BufferedWriter out){		
		
		ConstituentVariationalDecoder decoder = new ConstituentVariationalDecoder();
		decoder.decoding(hg);
			
		//### step-2: model extraction based on the definition of Q
		 
		//step-2.1: baseline feature: do not use insideoutside
		this.baseline_feat_tbl.clear();
		FeatureExtractionHG.feature_extraction_hg(hg, this.baseline_feat_tbl, null, this.baseline_ft_template);	
		
		
		//### step-3: rank the HG using the baseline and variational feature
		l_feat_functions.clear();		
		
		FeatureFunction base_ff =  new BaselineFF(this.baseline_feat_id, this.baseline_weight, this.baseline_feat_tbl);
		l_feat_functions.add(base_ff);
			
		//RankHG.rankHG(hg, l_feat_functions);
		
		
		//### step-4: kbest extraction from the reranked HG: remember to add the new feature function into the model list
		//two features: baseline feature, and reranking feature
		this.kbestExtractor.lazy_k_best_extract_hg(hg, this.l_feat_functions, this.topN, this.use_unique_nbest, sentenceID, out, this.use_tree_nbest, this.add_combined_cost);
		
		return hg;
	}
*/	
	
}
