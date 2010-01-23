package joshua.discriminative.ranker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.ViterbiExtractor;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.feature_template.EdgeBigramFT;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.feature_related.feature_template.NgramFT;
import joshua.discriminative.feature_related.feature_template.TMFT;
import joshua.discriminative.feature_related.feature_template.TableBasedBaselineFT;




/*This class rescore the hypergraph, and directly change the one-best pointer and logPs
 * (Note: we should not change the order of dependency, that is, all features are local)
 * */

@Deprecated
 public class RescorerHGSimple {
	 
	private HashMap processedNodesTtbl =  new HashMap();//help to tranverse a hypergraph
	
	private HashMap correctiveModel = null; //corrective model: this should not have the baseline feature

	private HashSet<String> restrictedFeatSet =null; //feature set
	private List<FeatureTemplate> featTemplates=null;
	
	private HashMap<HyperEdge, Double> hyperEdgeBaselineLogPTbl = new  HashMap<HyperEdge, Double>();
	

	private int numChanges=0;

	
	
	public RescorerHGSimple(){
		//do nothing
	}
	
//	###########  public interfaces
//	the baseline scale must be explicitly indicated in baseline_scale, instead of implicitly indicated in corrective_model
	

	//return the reranked onebest hg
	//	corrective model: this should not have the baseline feature
	public HyperGraph rerankHGAndGet1best(HyperGraph hg, HashMap correctiveModel, HashSet<String> restrictedFeatSet,
			List<FeatureTemplate> featTemplates, boolean isValueAVector){
		numChanges=0;
		adjustHGLogP( hg, correctiveModel, restrictedFeatSet, featTemplates);
		System.out.println("numChanges="+numChanges);		
		return   ViterbiExtractor.getViterbiTreeHG(hg);			
	}
	
	
	//==========================================
	public HashMap<HyperEdge, Double>  collectTransitionLogPs(HyperGraph hg){
		hyperEdgeBaselineLogPTbl.clear();
		processedNodesTtbl.clear();	
		numChanges=0;
		collectTransitionLogPs(hg.goalNode);
		processedNodesTtbl.clear();
		
		return hyperEdgeBaselineLogPTbl;
	}

	
	private  void collectTransitionLogPs(HGNode it ){
		if(processedNodesTtbl.containsKey(it))	return;
		processedNodesTtbl.put(it,1);		
		for(HyperEdge dt : it.hyperedges){					
			collectTransitionLogPs(it, dt);
		}		
	}

	private void collectTransitionLogPs(HGNode parentNode, HyperEdge dt){
		hyperEdgeBaselineLogPTbl.put(dt, dt.getTransitionLogP(false));//get baseline score	
		
		if(dt.getAntNodes()!=null){
			for(HGNode antNode : dt.getAntNodes()){
				collectTransitionLogPs(antNode);
			}
		}
	}	
	//==========================================
	
	
	
	private void adjustHGLogP(HyperGraph hg, HashMap correctiveModel_, HashSet<String> restrictedFeatSet_, List<FeatureTemplate> featTemplates_){
		processedNodesTtbl.clear();
		correctiveModel=correctiveModel_;
		restrictedFeatSet = restrictedFeatSet_;
		featTemplates = featTemplates_;
		adjustNodeLogP(hg.goalNode);
		processedNodesTtbl.clear();
	}
//########### end public interfaces	
	
	//item: recursively call my children edges, change pointer for bestHyperedge
	private  void adjustNodeLogP(HGNode it ){
		if(processedNodesTtbl.containsKey(it))	
			return;
		processedNodesTtbl.put(it,1);
		
		//==== recursively call my children edges, change pointer for bestHyperedge
		HyperEdge oldEdge =it.bestHyperedge; 
		it.bestHyperedge=null;
		for(HyperEdge dt : it.hyperedges){					
			adjustHyperedgeLogP(it, dt );//deduction-specifc feature
			it.semiringPlus(dt);
		}
		if(it.bestHyperedge!=oldEdge)
			numChanges++;
	}
	
	
	//adjust best_cost, and recursively call my ant items
	private void adjustHyperedgeLogP(HGNode parentNode, HyperEdge dt){
		dt.bestDerivationLogP =0;
		if(dt.getAntNodes()!=null){
			for(HGNode antNode : dt.getAntNodes()){
				adjustNodeLogP(antNode);
				dt.bestDerivationLogP += antNode.bestHyperedge.bestDerivationLogP;
			}
		}
		double res = getTransitionLogP(parentNode, dt);
		dt.setTransitionLogP(res) ;
		dt.bestDerivationLogP += res;
		
	}	
	
	
	//give a dt and pointers to ant items, find all features that apply, non-recursive
	private double getTransitionLogP(HGNode parentNode, HyperEdge dt ){
		double res =0;
		
		HashMap featTbl = new HashMap();
		for(FeatureTemplate template : featTemplates){			
			template.getFeatureCounts(dt,  featTbl,  restrictedFeatSet, 1);//scale is one: hard count			
		}	
		
		return DiscriminativeSupport.computeLinearCombinationLogP(featTbl, correctiveModel);
		
	}
	
	
	
	
	
	
	
	
//================== example main funciton======================
	
	public static void main(String[] args) 	throws IOException{
		
		if(args.length<11){
			System.out.println("wrong command, correct command should be: java Perceptron_TEST is_avg_model is_nbest f_test_items f_test_rules num_sent f_perceptron_model baseline_scale use_tm_feat use_lm_feat use_edge_bigram_feat_only freranked_1best");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)
				System.out.println("arg is: " + args[i]);	
			System.exit(0);		
		}
		
		long start_time = System.currentTimeMillis();

		boolean isAvgModel = new Boolean(args[0].trim());
		boolean isNbest = new Boolean(args[1].trim());
		String testNodesFile=args[2].trim();
		String testRulesFile=args[3].trim();
		int numSent = new Integer(args[4].trim());
		String modelFile = args[5].trim();
		double baselineScale = new Double(args[6].trim()); 
		boolean useTMFeat = new Boolean(args[7].trim());
		boolean useLMFeat = new Boolean(args[8].trim());
		boolean useEdgeNgramOnly = new Boolean(args[9].trim());
		String reranked1bestFile = args[10].trim();

		boolean saveModelCosts = true;
		
		String featureFile = null;
		if(args.length>11) 
			featureFile = args[11].trim();		
		
//		????????????????????????????????????????????????????
		int ngramStateID = 0; 
		//??????????????????????????????????????
		
		SymbolTable symbolTbl = new BuildinSymbol(null);
		
		boolean useIntegerString = false;
		boolean useRuleIDName = false;
		
		
		//####### nbest decoding
		/*if(is_nbest){
			//TODO
		}else{//#####hg decoding*/
		
			
			//======== setup feature templates list
			List<FeatureTemplate> featTemplates =  new ArrayList<FeatureTemplate>();	
			
			String baselineName = "baseline_lzf";//TODO
			FeatureTemplate baselineFeature = new TableBasedBaselineFT(baselineName, baselineScale);
			featTemplates.add(baselineFeature);	
			
			if(useTMFeat==true){
				FeatureTemplate ft = new TMFT(symbolTbl, useIntegerString, useRuleIDName);
				featTemplates.add(ft);
			}
				
			int baselineLMOrder = 5;//TODO??????????????????
			if(useLMFeat==true){	
				FeatureTemplate ft = new NgramFT(symbolTbl, false, ngramStateID, baselineLMOrder, 1, 2);//TODO: unigram and bi gram
				featTemplates.add(ft);
			}else if(useEdgeNgramOnly){//exclusive with use_lm_feat
				FeatureTemplate ft = new EdgeBigramFT(symbolTbl, ngramStateID, baselineLMOrder, useIntegerString);
				featTemplates.add(ft);
			}		
			System.out.println("templates are: " + featTemplates);
			
			//============= restricted feature set : normally this is not used as the model itself is a restriction
			HashSet<String> restrictedFeatureSet = null;
			if(featureFile!=null){
				restrictedFeatureSet = new HashSet<String>();
				DiscriminativeSupport.loadFeatureSet(featureFile, restrictedFeatureSet);
				//restricted_feature_set.put(HGDiscriminativeLearner.g_baseline_feat_name, 1.0); //should not add the baseline feature
				System.out.println("============use  restricted feature set========================");
			}
			
			//================ model
			HashMap<String, Double> modelTbl =  new HashMap<String, Double>();
						
			DiscriminativeSupport.loadModel(modelFile, modelTbl, null);
			
			BufferedWriter out1best = FileUtilityOld.getWriteFileStream(reranked1bestFile);
			
			RescorerHGSimple reranker = new RescorerHGSimple();
			
			DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null); 
			diskHG.initRead(testNodesFile, testRulesFile, null);
			for(int sent_id=0; sent_id < numSent; sent_id ++){
				System.out.println("#Process sentence " + sent_id);
				HyperGraph testHG = diskHG.readHyperGraph();
				((TableBasedBaselineFT) baselineFeature).setBaselineScoreTbl( reranker.collectTransitionLogPs(testHG) );
				HyperGraph rerankedOnebestHG = reranker.rerankHGAndGet1best(testHG, modelTbl, restrictedFeatureSet, featTemplates, isAvgModel);
				System.out.println("bestScore=" + rerankedOnebestHG.goalNode.bestHyperedge.bestDerivationLogP );
				String reranked_1best = ViterbiExtractor.extractViterbiString(symbolTbl, rerankedOnebestHG.goalNode);
				
				FileUtilityOld.writeLzf(out1best, reranked_1best + "\n");				
			}
			FileUtilityOld.closeWriteFile(out1best);
		//}
			
		System.out.println("Time cost: " + ((System.currentTimeMillis()-start_time)/1000));
	}

	
}
