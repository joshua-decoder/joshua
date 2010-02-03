package joshua.discriminative.ranker;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.hypergraph.ViterbiExtractor;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.feature_function.EdgeTblBasedBaselineFF;


/**This class implements functions to rank HG based on a bunch of feature functions
 *It does not change the topology of the HG, but it changes the 
 *bestHyperedge and bestLogP in hypergraph.
 **/


public class HGRanker {
	
	private HashSet<HGNode> processedNodesTbl =  new HashSet<HGNode>();	
	private List<FeatureFunction> featFunctions;
	
	private int numChangedBestHyperedge = 0;

	
	private static Logger logger = Logger.getLogger(HGRanker.class.getName());
	
	public HGRanker(List<FeatureFunction> featFunctions){
		this.featFunctions = featFunctions;
	}
	
	/**Change the input hypergraph based on featFunctions
	 */
	public void rankHG(HyperGraph hg){
		resetState();
		rankHGNode(hg.goalNode);
		//logger.info("number of nodes whose best hyperedge changes is " + numChangedBestHyperedge 
		//		+ " among total number of nodes " + processedNodesTbl.size() );
		resetState();		
	}
	
	//get 1best HG
	public HyperGraph rerankHGAndGet1best(HyperGraph hg){
		rankHG(hg);
		return  ViterbiExtractor.getViterbiTreeHG(hg);			
	}
	
	
	public void resetState(){
		processedNodesTbl.clear();
		numChangedBestHyperedge = 0;
	}
	
 
	
	
	private  void rankHGNode(HGNode it ){
		if(processedNodesTbl.contains(it))	
			return;
		processedNodesTbl.add(it);
		
		//==== recursively call my children deductions, change pointer for bestHyperedge
		HyperEdge oldBestHyperedge = it.bestHyperedge;
		
		it.bestHyperedge=null;
		for(HyperEdge dt : it.hyperedges){					
			rankHyperEdge(it, dt );
			it.semiringPlus(dt);
		}	
		
		/**Due to diskHG precision, the behavior may not be precise
		 **/
		if(it.bestHyperedge!=oldBestHyperedge){
			numChangedBestHyperedge++;
		}
	}

	private void rankHyperEdge(HGNode parentNode, HyperEdge dt){
		
		dt.bestDerivationLogP = 0;
		if(dt.getAntNodes()!=null){
			for(HGNode antNode : dt.getAntNodes()){
				rankHGNode(antNode);
				dt.bestDerivationLogP += antNode.bestHyperedge.bestDerivationLogP;//semiring times
			}
		}
		double transLogP = getTransitionLogP(parentNode, dt);		
		dt.setTransitionLogP(transLogP);		
		dt.bestDerivationLogP += transLogP;
		
	}	
	
	
	private double getTransitionLogP(HGNode parentNode, HyperEdge dt ){
		return ComputeNodeResult.computeCombinedTransitionLogP(
				this.featFunctions, dt, parentNode.i, parentNode.j, -1);
	}
	
	
	
	
//	================== example main funciton======================
	
	public static void main(String[] args) 	throws IOException{
		
		if(args.length<10){
			System.out.println("wrong command, correct command");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++)
				System.out.println("arg is: " + args[i]);	
			System.exit(0);		
		}
		
		long startTime = System.currentTimeMillis();

		String testNodesFile=args[0].trim();
		String testRulesFile=args[1].trim();
		int numSent = new Integer(args[2].trim());
		String modelFile = args[3].trim();
		double baselineWeight = new Double(args[4].trim()); 
		boolean useTMFeat = new Boolean(args[5].trim());
		boolean useLMFeat = new Boolean(args[6].trim());
		boolean useEdgeNgramOnly = new Boolean(args[7].trim());
		boolean useTMTargetFeat = new Boolean(args[8].trim());
		String reranked1bestFile = args[9].trim();

		boolean saveModelCosts = true;
		
		String featureFile = null;
		if(args.length>9) 
			featureFile = args[9].trim();		
		
		
		SymbolTable symbolTbl = new BuildinSymbol(null);			
		List<FeatureFunction> features =  new ArrayList<FeatureFunction>();
		
		
		
		//=== baseline feature ====
		//TODO: ????????????????????????????????????????????????????			
		int baselineFeatID = 99;
		//??????????????????????????????????????
		
		EdgeTblBasedBaselineFF baselineFeature = new EdgeTblBasedBaselineFF(baselineFeatID, baselineWeight);
		features.add(baselineFeature);
		

		//=== reranking feature === 
		//TODO: ??????????????
		int ngramStateID = 0; 
		int baselineLMOrder = 5;
		int startNgramOrder = 1;
		int endNgramOrder = 2;
		int featID = 100;
		double weight = 1.0;
		//????????
		
		Map<String,Integer> rulesIDTable = null; //TODO??
	
		//TODO
		FeatureFunction rerankFF = DiscriminativeSupport.setupRerankingFeature(featID, weight, symbolTbl, useTMFeat, useLMFeat, useEdgeNgramOnly, useTMTargetFeat, 
				JoshuaConfiguration.useMicroTMFeat, JoshuaConfiguration.wordMapFile,
				ngramStateID, 
				baselineLMOrder, startNgramOrder, endNgramOrder, featureFile, modelFile, rulesIDTable);
		
		features.add(rerankFF);
		
		//=== reranker using the feature functions
		HGRanker reranker = new HGRanker(features);
		
		BufferedWriter out1best = FileUtilityOld.getWriteFileStream(reranked1bestFile);
		
					
		int topN=3;
		boolean useUniqueNbest =true;
		boolean useTreeNbest = false;
		boolean addCombinedCost = true;	
		KBestExtractor kbestExtractor = new KBestExtractor(symbolTbl, useUniqueNbest, useTreeNbest, false, addCombinedCost, false, true);
		
		
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null); 
		diskHG.initRead(testNodesFile, testRulesFile, null);
		for(int sentID=0; sentID < numSent; sentID ++){
			System.out.println("#Process sentence " + sentID);
			HyperGraph testHG = diskHG.readHyperGraph();
			baselineFeature.collectTransitionLogPs(testHG);
			reranker.rankHG(testHG);
		
			try{
				kbestExtractor.lazyKBestExtractOnHG(testHG, features, topN, sentID, out1best);
			} catch (IOException e) {
				e.printStackTrace();
			}
							
		}
		FileUtilityOld.closeWriteFile(out1best);
		
			
		System.out.println("Time cost: " + ((System.currentTimeMillis()-startTime)/1000));
	}

	

	
}
