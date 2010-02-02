package joshua.discriminative.feature_related.feature_template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SetupMicroRuleFeatures {
	boolean useWholeRuleFeature = true;
	boolean useRuleIDName = true; //do we use an abbreviated name for the wholeRuleFeature
	
	boolean useTargetSideRuleFeature = false;
	
	boolean useTargetRuleNgramFeatuure = false;
	
	public SetupMicroRuleFeatures(){
		
	}
	
	public Map<String, List<MicroRuleFeature>> setupTbl(Map<String, Integer> ruleStringToIDTable, Map<String,Integer> featureStringToIntegerMap){
		
		/**key: rule abbreviated name
		 * value: list of microRuleFeature
		 ***/
		Map<String, List<MicroRuleFeature>> resTbl = new HashMap<String, List<MicroRuleFeature>>();
	
		for(Entry<String, Integer> entry : ruleStringToIDTable.entrySet()){
			String ruleFullName = entry.getKey();
			String ruleAbbrName = "r" + entry.getValue();//TODO

			List<MicroRuleFeature> microRuleFeatures = new ArrayList<MicroRuleFeature>();
			resTbl.put(ruleAbbrName, microRuleFeatures);
			
			if(useWholeRuleFeature){//e.g., [x] ||| a b c ||| c d e
				extractWholeRuleFeature(ruleFullName, ruleAbbrName, featureStringToIntegerMap, microRuleFeatures);
			}
			
			if(useTargetSideRuleFeature){
				extractTargetRuleFeature(ruleFullName, featureStringToIntegerMap, microRuleFeatures);
			}
			
			
		
			
		}
		return resTbl;
	}
	
	private void extractWholeRuleFeature(String ruleFullName, String ruleAbbrName, Map<String,Integer> featureStringToIntegerMap, List<MicroRuleFeature> microRuleFeatures){
		String featureName = null;
		if(this.useRuleIDName)
			featureName = ruleAbbrName;//abbreviated name
		else
			featureName = ruleFullName;//the whole rule name
		
		if(featureStringToIntegerMap.containsKey(featureName)){
			MicroRuleFeature feature = new MicroRuleFeature(featureName, 1.0);
			microRuleFeatures.add(feature);
		}
		
	}
	
	private void extractTargetRuleFeature(String ruleFullName, Map<String,Integer> featureStringToIntegerMap, List<MicroRuleFeature> microRuleFeatures){
		String targetRule = null;//TODO
		
		if(featureStringToIntegerMap.containsKey(targetRule)){
			MicroRuleFeature feature = new MicroRuleFeature(targetRule, 1.0);
			microRuleFeatures.add(feature);
		}
		
	}
	
	private void extractTargetNgramFeature(String ruleFullName, Map<String,Integer> featureStringToIntegerMap, List<MicroRuleFeature> microRuleFeatures){
		String targetRule = null;//TODO
		//?????????????
		if(featureStringToIntegerMap.containsKey(targetRule)){
			MicroRuleFeature feature = new MicroRuleFeature(targetRule, 1.0);
			microRuleFeatures.add(feature);
		}
		
	}
	
}
