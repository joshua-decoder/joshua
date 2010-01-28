package joshua.discriminative.training.risk_annealer.hypergraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.BLEU;
import joshua.decoder.ff.lm.NgramExtractor;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;


/**The way we extract features (stored in featureTbl) for each edge is as following: 
 * (1) Feature template will return feature-name (i.e. string) and feature value.
 * (2) This class will convert the feature-name to feature-id (i.e. integer) by using featureStringToIntegerMap
 * (3) The featureStringToIntegerMap may be also used for feature filtering
 * */

public class RiskAndFeatureAnnotationOnLMHG {
	
	//private SymbolTable symbolTbl;
	
	private HashSet<HGNode> processedHGNodesTbl =  new HashSet<HGNode>();
	
//	== variables related to BLEU risk
	private boolean doRiskAnnotation = true;//TODO
	private double[] linearCorpusGainThetas; //weights in the Goolge linear corpus gain function
	
	private NgramExtractor ngramExtractor;
	
	private int baselineLMOrder;
	private static int startNgramOrder=1;
	private static int endNgramOrder=4;
	
	
//	== variables related to feature annotation
	private boolean doFeatureAnnotation = true;
	private HashMap<String, Integer> featureStringToIntegerMap; //this can also be used as feature filter
	private HashSet<String> restrictedFeatureSet;
	private List<FeatureTemplate> featTemplates;
	double scale = 1.0; //TODO
	
	Logger logger = Logger.getLogger(RiskAndFeatureAnnotationOnLMHG.class.getSimpleName());
	
	public RiskAndFeatureAnnotationOnLMHG(int baselineLMOrder,	int ngramStateID,	double[] linearCorpusGainThetas,  SymbolTable symbolTbl,
			HashMap<String, Integer> featureStringToIntegerMap,  List<FeatureTemplate> featTemplates, boolean doRiskAnnotation){
		
		this.baselineLMOrder = baselineLMOrder;
		this.doRiskAnnotation = doRiskAnnotation;
		
		if(this.baselineLMOrder<endNgramOrder){
			System.out.println("Baseline LM n-gram order is too small");
			System.exit(1);
		}
		//this.symbolTbl = symbolTbl;
		

		this.linearCorpusGainThetas = linearCorpusGainThetas;
		this.ngramExtractor = new NgramExtractor(symbolTbl, ngramStateID, false, baselineLMOrder); 
	
		//=== feature related
		this.featureStringToIntegerMap = featureStringToIntegerMap;
		this.restrictedFeatureSet = new HashSet<String>( featureStringToIntegerMap.keySet() );
		this.featTemplates = featTemplates;
			
		//System.out.println("use riskAnnotatorNoEquiv====");
	}
	
	/**Input a hypergraph, return
	 * a FeatureForest
	 * Note that the input hypergraph has been changed*/	
	
	public FeatureForest riskAnnotationOnHG(HyperGraph hg, String[] referenceSentences){
		
		processedHGNodesTbl.clear(); 
				
		if(doRiskAnnotation){
			HashMap<String, Integer> refereceNgramTable = BLEU.constructMaxRefCountTable(referenceSentences, endNgramOrder);
			annotateNode(hg.goalNode, refereceNgramTable);
		}else{
			annotateNode(hg.goalNode, null);
		}
		releaseNodesStateMemroy();
		return new FeatureForest(hg);		
	}
	

	private void annotateNode(HGNode node, HashMap<String, Integer> refereceNgramTable){
		
		if(processedHGNodesTbl.contains(node))
			return;
		processedHGNodesTbl.add(node);
		
		//=== recursive call on each edge
		for(int i=0; i<node.hyperedges.size(); i++){
			HyperEdge oldEdge = node.hyperedges.get(i);
			HyperEdge newEdge = annotateHyperEdge(oldEdge, refereceNgramTable);
			node.hyperedges.set(i, newEdge);
		}
		
		//===@todo: release the memory consumed by the state of node, but we have to make sure all parent hyperedges have been processed
	}
	
 
	private HyperEdge annotateHyperEdge(HyperEdge oldEdge, HashMap<String, Integer> refereceNgramTable){
		
		//=== recursive call on each ant item
		if(oldEdge.getAntNodes()!=null)
			for(HGNode antNode : oldEdge.getAntNodes())
				annotateNode(antNode, refereceNgramTable);
		
		//=== HyperEdge-specific operation				
		return createNewHyperEdge(oldEdge, refereceNgramTable);
	}
	
	
	protected HyperEdge createNewHyperEdge(HyperEdge oldEdge, HashMap<String, Integer> refereceNgramTable) {
	
		//======== risk annotation
		double transitionRisk = 0;
		if(doRiskAnnotation)
			transitionRisk = getTransitionRisk(oldEdge, refereceNgramTable);
		
		//System.out.println("tran2=" + riskTransitionCost);
		
		//======== feature annotation
		HashMap<Integer, Double> featureTbl= null;
		if(doFeatureAnnotation)
			featureTbl = featureExtraction(oldEdge, null, scale);
		
	
		/**compared wit the original edge, two changes:
		 * (1) add risk at edge (but does not change the orignal model score)
		 * (2) add feature tbl
		 * */
		return new FeatureHyperEdge(oldEdge, featureTbl, transitionRisk);
	}
	
	
	private void releaseNodesStateMemroy(){
		for(HGNode node : processedHGNodesTbl){
			//System.out.println("releaseNodesStateMemroy");
			node.releaseDPStatesMemory();
		}
	}
	
	
	private double getTransitionRisk(HyperEdge dt, HashMap<String, Integer> refereceNgramTable){ 
		
		double transitionRisk = 0;
		if(dt.getRule() != null){//note: hyperedges under goal item does not contribute BLEU
			int hypLength = dt.getRule().getEnglish().length-dt.getRule().getArity();
			HashMap<String, Integer> hyperedgeNgramTable = ngramExtractor.getTransitionNgrams(dt, startNgramOrder, endNgramOrder);			
			transitionRisk = - BLEU.computeLinearCorpusGain(linearCorpusGainThetas, hypLength, hyperedgeNgramTable, refereceNgramTable);
			
			/*
			System.out.println("hyp tbl: " + hyperedgeNgramTable);
			System.out.println("ref tbl: " + refereceNgramTable);
			System.out.println("hypLength: " + hypLength);
			System.out.println("risk is " + transitionRisk);	
			System.exit(1);*/
		}
		
		return transitionRisk;
	}
	
	
	
	
	//TODO: copied from RiskAndFeatureAnnotation, consider merge
	
	//============================================================================================
	//==================================== feature extraction function ======================================
	//============================================================================================
	

	/**The way we extract features (stored in featureTbl) for each edge is as following: 
	 * (1) Feature template will return feature-name (i.e. string) and feature value.
	 * (2) This class will convert the feature-name to feature-id (i.e. integer) by using featureStringToIntegerMap
	 * (3) The featureStringToIntegerMap (derive restrictedFeatureSet) will be also used for feature filtering
	 * */
	
	private final  HashMap<Integer, Double> featureExtraction(HyperEdge dt, HGNode parentItem, double scale){		
	
		//=== extract feature counts
		HashMap<String, Double> activeFeaturesHelper = new HashMap<String, Double>();

		for(FeatureTemplate template : featTemplates){			
			template.getFeatureCounts(dt,  activeFeaturesHelper,  restrictedFeatureSet, scale);
			
		}
		
		//=== convert the featureString to featureInteger
		HashMap<Integer, Double> res = new HashMap<Integer, Double>();
		for(Map.Entry<String, Double> feature : activeFeaturesHelper.entrySet()){
			Integer featureID = featureStringToIntegerMap.get(feature.getKey());
			if(featureID==null){
				logger.severe("Null feature ID, featureID="+feature.getKey());
				System.exit(1);
			}
			res.put(featureID, feature.getValue());
		}
		//System.out.println("Feature extraction res: " + res);
		return res;			
	}
	
}
