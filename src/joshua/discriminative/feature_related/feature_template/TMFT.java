package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.discriminative.DiscriminativeSupport;



public class TMFT extends AbstractFeatureTemplate {

	SymbolTable symbolTbl;	
	double globalScale;
	
	public TMFT(SymbolTable symbolTbl, double globalScale){
		this.symbolTbl = symbolTbl;		
		this.globalScale = globalScale;
		System.out.println("TM template, globalScale is " + this.globalScale);
	}
	
	public TMFT(SymbolTable symbolTbl){
		this(symbolTbl, 1);
	}
	
	
	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashMap<String, Integer> restrictedFeatureSet, double scale) {
		
		if(rule != null){			
			String key =  rule.toStringWithoutFeatScores(symbolTbl);//TODO
			if(restrictedFeatureSet == null || restrictedFeatureSet.containsKey(key)==true){
				DiscriminativeSupport.increaseCount(featureTbl, key, scale*globalScale);
				//System.out.println("key is " + key +"; lhs " + symbolTbl.getWord(rl.getLHS()));	//System.exit(0);
			}
			
		}
		
	}


}
