package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.discriminative.DiscriminativeSupport;



public class TMFT extends AbstractFeatureTemplate {

	SymbolTable symbolTbl;
	
	boolean useIntegerString = true;
	
	public TMFT(SymbolTable symbolTbl, boolean useIntegerString){
		this.symbolTbl = symbolTbl;	
		this.useIntegerString = useIntegerString;
		
		System.out.println("TM template");
	}
	
	
	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		
		if(rule != null){			
			String key = null;
			if(this.useIntegerString)//TODO
				key =  rule.toStringWithoutFeatScores(null);
			else
				key =  rule.toStringWithoutFeatScores(symbolTbl);
			
			if(restrictedFeatureSet == null || restrictedFeatureSet.contains(key)==true){
				DiscriminativeSupport.increaseCount(featureTbl, key, scale);
				//System.out.println("key is " + key +"; lhs " + symbolTbl.getWord(rule.getLHS()));	//System.exit(0);
			}
			
		}
		
	}
	
	

}
