package joshua.discriminative.training.risk_annealer.hypergraph.deprecated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.BLEU;
import joshua.decoder.ff.state_maintenance.NgramStateComputer;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.training.oracle.DPStateOracle;
import joshua.discriminative.training.oracle.EquivLMState;
import joshua.discriminative.training.oracle.RefineHG;
import joshua.discriminative.training.oracle.RefineHG.RefinedNode;
import joshua.discriminative.training.risk_annealer.hypergraph.FeatureForest;
import joshua.discriminative.training.risk_annealer.hypergraph.FeatureHyperEdge;
import joshua.util.Ngram;
import joshua.util.Regex;



/**The way we extract features (stored in featureTbl) for each edge is as following: 
 * (1) Feature template will return feature-name (i.e. string) and feature value.
 * (2) This class will convert the feature-name to feature-id (i.e. integer) by using featureStringToIntegerMap
 * (3) The featureStringToIntegerMap may be also used for feature filtering
 * */

@Deprecated
public class RiskAndFeatureAnnotation extends  RefineHG<DPStateOracle> {

	SymbolTable symbolTable;
	
	//== variables related to BLEU risk	
	boolean doRiskAnnotation = true;//TODO
	double[] linearCorpusGainThetas; //weights in the Goolge linear corpus gain function
	
	int ngramOrder;
	private static int bleuOrder = 4;
	
	EquivLMState equi;	
	NgramStateComputer ngramStateComputer;
	int ngramStateID = 0;
	
	//== sentence-specific
	protected HashMap<String, Integer> refNgramsTbl = new HashMap<String, Integer>();
	
	EquivLMState equiv;
	
	
	//== variables related to features
	boolean doFeatureAnnotation = true;//TODO
	HashSet<String> restrictedFeatureSet;//this can also be used as feature filter
	private HashMap<String, Integer> featureStringToIntegerMap; 
	private List<FeatureTemplate> featTemplates;
	
	
	/**
	 * @param symbolTable_
	 * @param nGramOrder_
	 * @param linearCorpusGainThetas_
	 * @param featureStringToIntegerMap_
	 * @param doFeatureFiltering_
	 * @param featTemplates_
	 */
	public RiskAndFeatureAnnotation(SymbolTable symbolTable_, int nGramOrder_, double[] linearCorpusGainThetas_,
			HashMap<String, Integer> featureStringToIntegerMap_,  List<FeatureTemplate> featTemplates_) {
		
		
		this.symbolTable = symbolTable_;
		this.ngramOrder = nGramOrder_;
		this.linearCorpusGainThetas = linearCorpusGainThetas_;
		
		this.featureStringToIntegerMap = featureStringToIntegerMap_;
		restrictedFeatureSet = new HashSet<String>(featureStringToIntegerMap.keySet());
		
		this.featTemplates = featTemplates_;
		
		this.equi = new EquivLMState(symbolTable_, bleuOrder);
		this.ngramStateComputer = new NgramStateComputer(symbolTable_, bleuOrder, ngramStateID);
		
		this.equiv = new EquivLMState(this.symbolTable, bleuOrder);
		System.out.println("use RiskAndFeatureAnnotation====");
	}

	
	public FeatureForest riskAnnotationOnHG(HyperGraph hg, String refSentStr){

		setupRefAndPrefixAndSurfixTbl(refSentStr);//TODO: should enforce this in parent class		
		return new FeatureForest(  splitHG(hg) );
	}
	
	public FeatureForest riskAnnotationOnHG(HyperGraph hg, String[] refSentStrs){

		setupRefAndPrefixAndSurfixTbl(refSentStrs);//TODO: should enforce this in parent class		
		return new FeatureForest(  splitHG(hg) );
	}


//	TODO: should enforce this in parent class
	private void setupRefAndPrefixAndSurfixTbl(String refSentStr){	
		
		//== ref tbL and effective ref len
		int[] refWords = this.symbolTable.addTerminals(refSentStr.split("\\s+"));
					
		refNgramsTbl.clear();
		Ngram.getNgrams(refNgramsTbl, 1, bleuOrder, refWords);
		
		//== prefix and suffix tbl
		equi.setupPrefixAndSurfixTbl(refNgramsTbl);
		
	}
	
	
	private void setupRefAndPrefixAndSurfixTbl(String[] refSentStrs){		
		
		//== ref tbL and effective ref len
		int[] refLens = new int[refSentStrs.length];
		ArrayList<HashMap<String, Integer>> listRefNgramTbl = new ArrayList<HashMap<String, Integer>>();		
		for(int i =0; i<refSentStrs.length; i++){
			int[] refWords = this.symbolTable.addTerminals(refSentStrs[i].split("\\s+"));
			refLens[i] = refWords.length;		
			HashMap<String, Integer> tRefNgramsTbl = new HashMap<String, Integer>();
			Ngram.getNgrams(tRefNgramsTbl, 1, bleuOrder, refWords);	
			listRefNgramTbl.add(tRefNgramsTbl);			
		}
		
		refNgramsTbl =  BLEU.computeMaxRefCountTbl(listRefNgramTbl);
		
		//== prefix and suffix tbl
		equi.setupPrefixAndSurfixTbl(refNgramsTbl);
	}
		


	@Override
	protected HyperEdge createNewHyperEdge(HyperEdge originalEdge, List<HGNode> antVirtualItems, DPStateOracle dps) {
	
		//== risk annotation
		double riskTransitionCost = 0;
		if(doRiskAnnotation)
			riskTransitionCost = getRiskTransitionCost(originalEdge, antVirtualItems, dps);//TODO
		
		//System.out.println("tran2=" + riskTransitionCost);
		
		//== feature annotation
		HashMap<Integer, Double> featureTbl= null;
		if(doFeatureAnnotation)
			featureTbl = featureExtraction(originalEdge, null);//TODO: originalEdge? null parentNode
		
		/**compared wit the original edge, three changes:
		 * (1) change the list of ant nodes
		 * (2) add risk cost at edge (but does not change the orignal model cost)
		 * (3) add feature tbl
		 * */
		return new FeatureHyperEdge(originalEdge.getRule(), originalEdge.bestDerivationLogP, originalEdge.getTransitionLogP(false), antVirtualItems, 
				originalEdge.getSourcePath(), featureTbl, riskTransitionCost);
	}
	
	
	private double getRiskTransitionCost(HyperEdge originalEdge, List<HGNode> antVirtualItems, DPStateOracle dps){//note: transition_cost is already linearly interpolated
		
		double riskTransitionCost = dps.bestDerivationLogP;
		if(antVirtualItems!=null)	
			for(HGNode ant_it :antVirtualItems ){
				RefinedNode it2 = (RefinedNode) ant_it;//TODO
				riskTransitionCost -= ((DPStateOracle)it2.dpState).bestDerivationLogP;
			}			
		return -riskTransitionCost;
	}
	
	protected  DPStateOracle computeState(HGNode originalParentItem, HyperEdge originalEdge, List<HGNode> antVirtualItems){
		/*
		double refLen = computeAvgLen(originalParentItem.j-originalParentItem.i, srcSentLen, refSentLen);
		
		//=== hypereges under "goal item" does not have rule
		if(originalEdge.getRule()==null){
			if(antVirtualItems.size()!=1){
				System.out.println("error deduction under goal item have more than one item"); 
				System.exit(0);
			}
			double bleuCost = antVirtualItems.get(0).bestHyperedge.bestDerivationCost;
			return  new DPStateOracle(0, null, null, null, bleuCost);//no DPState at all
			 
		}
		
		
		ComputeOracleStateResult lmState = computeLMState(originalEdge, antVirtualItems);	
			
		int hypLen = lmState.numNewWordsAtEdge;
		
		int[] numNgramMatches = new int[bleuOrder];
		
		Iterator iter = lmState.newNgramsTbl.keySet().iterator();
    	while(iter.hasNext()){
    		String ngram = (String)iter.next();
    		
			int finalCount =  lmState.newNgramsTbl.get(ngram);
			if(doLocalNgramClip)
				numNgramMatches[ngram.split("\\s+").length-1] += Support.findMin(finalCount, refNgramsTbl.get(ngram)) ;
			else 
				numNgramMatches[ngram.split("\\s+").length-1] += finalCount; //do not do any cliping    			
    	}
		
    	if(antVirtualItems!=null){
			for(int i=0; i<antVirtualItems.size(); i++){
				DPStateOracle antDPState = (DPStateOracle)((RefinedNode)antVirtualItems.get(i)).dpState;    			
				hypLen += antDPState.bestLen;
				for(int t=0; t<bleuOrder; t++)
					numNgramMatches[t] += antDPState.ngramMatches[t];
			}
    	}
		double bleuCost = - computeBleu(hypLen, refLen, numNgramMatches, bleuOrder);

		return  new DPStateOracle(hypLen, numNgramMatches, lmState.leftEdgeWords, lmState.rightEdgeWords, bleuCost);*/
		return null;
	}
	

	
 
	
	
	
	//========================================== BLEU realted =====================================
	//TODO: merge with joshua.decoder.BLEU
	
	/** 
	 * speed consideration: assume hypNgramTable has a smaller
	 * size than referenceNgramTable does
	 */
	public static double computeLinearCorpusGain(double[] linearCorpusGainThetas, int hypLength, HashMap<String,Integer> hypNgramTable,  HashMap<String,Integer> referenceNgramTable) {
		double res = 0;
		int[] numMatches = new int[5];
		res += linearCorpusGainThetas[0] * hypLength;
		numMatches[0] = hypLength;
		for (Entry<String,Integer> entry : hypNgramTable.entrySet()) {
			String   key = entry.getKey();
			Integer refNgramCount = referenceNgramTable.get(key);
			//System.out.println("key is " + key); System.exit(1);
			if(refNgramCount!=null){//delta function
				int ngramOrder = Regex.spaces.split(key).length;
				res += entry.getValue() * linearCorpusGainThetas[ngramOrder];
				numMatches[ngramOrder] += entry.getValue();
			}
		}
		/*
		System.out.print("Google BLEU stats are: ");
		for(int i=0; i<5; i++)
			System.out.print(numMatches[i]+ " ");
		System.out.print(" ; BLUE is " + res);
		System.out.println();
		*/
		return res;
	}
	
	
	
	public static double[] computeLinearCorpusThetas(int numUnigramTokens, double unigramPrecision, double decayRatio){
		double[] res = new double[5];
		res[0] = -1.0/numUnigramTokens;
		for(int i=1; i<5; i++)
			res[i] = 1.0/(4.0*numUnigramTokens*unigramPrecision*Math.pow(decayRatio, i-1));
		System.out.print("Thetas are: ");
		for(int i=0; i<5; i++)
			System.out.print(res[i] + " ");
		System.out.print("\n");
		return res;
	}



	
	//============================================================================================
	//==================================== feature extraction function ======================================
	//============================================================================================
	
	/**The way we extract features (stored in featureTbl) for each edge is as following: 
	 * (1) Feature template will return feature-name (i.e. string) and feature value.
	 * (2) This method will convert the feature-name to feature-id (i.e. integer) by using featureStringToIntegerMap
	 * (3) The featureStringToIntegerMap may be also used for feature filtering
	 * */
	
	private final  HashMap<Integer, Double> featureExtraction(HyperEdge dt, HGNode parentItem){		
	
		//=== extract feature counts
		HashMap<String, Double> activeFeaturesHelper = new HashMap<String, Double>();

		double tScale = 1.0;//TODO
		for(FeatureTemplate template : featTemplates){		
			template.getFeatureCounts(dt,  activeFeaturesHelper,  restrictedFeatureSet, tScale);		
		}
		
		//=== convert the featureString to featureInteger
		HashMap<Integer, Double> res = new HashMap<Integer, Double>();
		for(Map.Entry<String, Double> feature : activeFeaturesHelper.entrySet()){
			Integer featureID = featureStringToIntegerMap.get(feature.getKey());
			if(featureID==null){
				System.out.println("Null feature ID");
				System.exit(1);
			}
			res.put(featureID, feature.getValue());			
			//System.out.println("Str1=" + feature.getKey() + "; ID=" + featureID + "; val=" +feature.getValue());
		}
		//System.out.println("Feature extraction res: " + res);
		return res;
			
	}


	
	
}
