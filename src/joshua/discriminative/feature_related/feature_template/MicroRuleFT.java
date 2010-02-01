package joshua.discriminative.feature_related.feature_template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.discriminative.DiscriminativeSupport;

/**each rule may corresponds a list of small features
 * */
public class MicroRuleFT extends AbstractFeatureTemplate {
	
	/**we can use the ruleID 
	 * as feature name*/
	boolean useRuleIDName = true;
	String prefix="r";
	
	//key: ruleName; value: list of MicroFeatures
	Map<String, List<MicroRuleFeature>> ruleFeatureTbl;

	public MicroRuleFT(boolean useRuleIDName, Map<String, List<MicroRuleFeature>>  ruleFeatureTbl){
		this.useRuleIDName = useRuleIDName;
		this.ruleFeatureTbl = ruleFeatureTbl;
	}
	
	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		computeCounts(rule, featureTbl, restrictedFeatureSet, scale);
	}

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		computeCounts(rule, featureTbl, restrictedFeatureSet, scale);
	}

	private void computeCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale){
		if(rule != null){			
			if(this.useRuleIDName){
				String key = this.prefix + rule.getRuleID();
				List<MicroRuleFeature> features = ruleFeatureTbl.get(key);
				for(MicroRuleFeature feature : features ){
					if(restrictedFeatureSet == null || restrictedFeatureSet.contains(feature.featName)==true){
						DiscriminativeSupport.increaseCount(featureTbl, feature.featName, scale*feature.featValue);
						//System.out.println("key is " + key +"; lhs " + symbolTbl.getWord(rule.getLHS()));	
					}
				}
				//System.out.println("key is " + key + "; And: " +rule.toStringWithoutFeatScores(symbolTbl));System.exit(0);
			}else{
				System.out.println("unimplemented function");
				System.exit(1);
			}			
		}
	}
}
