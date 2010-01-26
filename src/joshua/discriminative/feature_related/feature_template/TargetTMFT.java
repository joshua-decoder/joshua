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
	
	boolean useIntegerString = true;
	
	public TargetTMFT(SymbolTable symbolTbl, boolean useIntegerString){
		this.symbolTbl = symbolTbl;	
		this.useIntegerString = useIntegerString;
		System.out.println("TargetTMFT template");
	}
	

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		computeCounts(rule, featureTbl, restrictedFeatureSet, scale);
		
	}
	
	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		computeCounts(rule, featureTbl, restrictedFeatureSet, scale);				
	}
	
	private String ruleEnglishString(BilingualRule rule, SymbolTable symbolTable) {
		if(useIntegerString){			
			return rule.convertToString(rule.getEnglish(), null);			
		}else{
			return rule.convertToString(rule.getEnglish(), symbolTable);
		}
	}



	private void computeCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale){
		if(rule!=null){
			String featName= ruleEnglishString( (BilingualRule) rule, this.symbolTbl);//TODO
			if(  restrictedFeatureSet==null ||
			   ( restrictedFeatureSet!=null && restrictedFeatureSet.contains(featName) ) ){
				DiscriminativeSupport.increaseCount(featureTbl, featName, scale);									
			}	
		}	
	}
	
}
