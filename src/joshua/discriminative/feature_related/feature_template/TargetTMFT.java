package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.discriminative.DiscriminativeSupport;

public class TargetTMFT  extends AbstractFeatureTemplate {

	SymbolTable symbolTbl;
	
	double globalScale;
	
	public TargetTMFT(SymbolTable symbolTbl, double globalScale){
		this.symbolTbl = symbolTbl;	
		this.globalScale = globalScale;
		System.out.println("TM template, globalScale is " + this.globalScale);
	}
	
	public TargetTMFT(SymbolTable symbol){
		this(symbol, 1);
	}
	

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		
		if(rule!=null){
			String featName= ruleEnglishString( (BilingualRule) rule, symbolTbl);//TODO
			if(  restrictedFeatureSet==null ||
			   ( restrictedFeatureSet!=null && restrictedFeatureSet.contains(featName) ) ){
				DiscriminativeSupport.increaseCount(featureTbl, featName, scale*globalScale);									
			}	
		}	
	}
	
	
	private String ruleEnglishString(BilingualRule rule, SymbolTable symbolTable) {
		return symbolTable.getWords(rule.getEnglish());
	}

	
}
