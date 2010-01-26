package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.lm.NgramExtractor;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.DiscriminativeSupport;

public class NgramFT extends AbstractFeatureTemplate {
	
	private int startNgramOrder = 1;
	private int endNgramOrder = 3;
		
	private NgramExtractor ngramExtractor;
	
	
	public NgramFT(SymbolTable symbolTbl, boolean useIntegerNgram, int ngramStateID, int baselineLMOrder, int startOrder, int endOrder){
	
		this.startNgramOrder = startOrder;
		this.endNgramOrder = endOrder;
		
		if(baselineLMOrder<endNgramOrder){
			System.out.println("baseline lm order is smaller than end_lm_order");
			System.exit(0);
		}
		
		this.ngramExtractor = new NgramExtractor(symbolTbl, ngramStateID, useIntegerNgram, baselineLMOrder);
		
		System.out.println("startOrder=" + startOrder);
		System.out.println("endOrder=" + endOrder);
		
	}
	
	
	public void getFeatureCounts(HyperEdge dt,  HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		HashMap<String,Integer> ngramsTbl;
		if(dt.getRule()==null)
			ngramsTbl = ngramExtractor.getFinalTransitionNgrams(dt, startNgramOrder, endNgramOrder);
		else
			ngramsTbl = ngramExtractor.getTransitionNgrams(dt, startNgramOrder, endNgramOrder);
		
		if(ngramsTbl!=null){						
			for(Map.Entry<String,Integer> entry : ngramsTbl.entrySet()){
				String ngramFeatKey= entry.getKey();					
				if(restrictedFeatureSet ==null || restrictedFeatureSet.contains(ngramFeatKey)==true){				
						DiscriminativeSupport.increaseCount(featureTbl, ngramFeatKey,entry.getValue()*scale);	
				
				}
			}
		}	
	}


	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		HashMap<String,Integer> ngramsTbl;
		if(rule==null)
			ngramsTbl = ngramExtractor.getFinalTransitionNgrams(antNodes.get(0), startNgramOrder, endNgramOrder);
		else
			ngramsTbl = ngramExtractor.getTransitionNgrams(rule, antNodes, startNgramOrder, endNgramOrder);
		
		if(ngramsTbl!=null){						
			for(Map.Entry<String,Integer> entry : ngramsTbl.entrySet()){
				String ngramFeatKey= entry.getKey();					
				if(restrictedFeatureSet ==null || restrictedFeatureSet.contains(ngramFeatKey)==true){				
						DiscriminativeSupport.increaseCount(featureTbl, ngramFeatKey,entry.getValue()*scale);	
				
				}
			}
		}
		
	}


	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		HashMap<String,Integer> ngramsTbl = ngramExtractor.getRuleNgrams(rule, startNgramOrder, endNgramOrder);		
		if(ngramsTbl!=null){						
			for(Map.Entry<String,Integer> entry : ngramsTbl.entrySet()){
				String ngramFeatKey= entry.getKey();					
				if(restrictedFeatureSet ==null || restrictedFeatureSet.contains(ngramFeatKey)==true){				
						DiscriminativeSupport.increaseCount(featureTbl, ngramFeatKey,entry.getValue()*scale);				
				}
			}
		}		
	}

	
}
