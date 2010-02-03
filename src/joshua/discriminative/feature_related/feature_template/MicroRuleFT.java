package joshua.discriminative.feature_related.feature_template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.discriminative.DiscriminativeSupport;
import joshua.util.Ngram;
import joshua.util.io.LineReader;

/**each rule may corresponds a list of small features
 * */
public class MicroRuleFT extends AbstractFeatureTemplate {
	
	/**we can use the ruleID 
	 * as feature name*/
	boolean useRuleIDName = true;
	String prefix="r";
		
	boolean useTargetRuleNgramFeature = true;
	int startNgramOrder = 2;//TODO
	int endNgramOrder = 2;//TODO
	
	Map<String, String> wordMap;
	
	//key: ruleName; value: list of MicroFeatures
	Map<String, List<MicroRuleFeature>> ruleFeatureTbl;

	
	static Logger logger = Logger.getLogger(MicroRuleFT.class.getSimpleName());
	
	public MicroRuleFT( boolean useRuleIDName, int startNgramOrder, int endNgramOrder, String wordMapFile){

		this.useRuleIDName = useRuleIDName;
		this.startNgramOrder = startNgramOrder;
		this.endNgramOrder = endNgramOrder;
		
		try {
			this.wordMap = readWordMap(wordMapFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void estimateFeatureCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		computeCounts(rule, featureTbl, restrictedFeatureSet, scale);
	}

	public void getFeatureCounts(Rule rule, List<HGNode> antNodes, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale) {
		computeCounts(rule, featureTbl, restrictedFeatureSet, scale);
	}

	public void setModelTbl(Map<String, List<MicroRuleFeature>>  ruleFeatureTbl){
		this.ruleFeatureTbl = ruleFeatureTbl;;
	}
	
	private void computeCounts(Rule rule, HashMap<String, Double> featureTbl, HashSet<String> restrictedFeatureSet, double scale){
		if(rule != null){			
			if(this.useRuleIDName){
				String key = this.prefix + rule.getRuleID();
				List<MicroRuleFeature> features = ruleFeatureTbl.get(key);
				if(features!=null){
					for(MicroRuleFeature feature : features ){
						if(restrictedFeatureSet == null || restrictedFeatureSet.contains(feature.featName)==true){ 	
							DiscriminativeSupport.increaseCount(featureTbl, feature.featName, scale*feature.featValue);
							//System.out.println("key=" + feature.featName + "; value="+feature.featValue);	
						}
					}
				}
				//System.out.println("key is " + key + "; And: " +rule.toStringWithoutFeatScores(symbolTbl));System.exit(0);
			}else{
				System.out.println("unimplemented function");
				System.exit(1);
			}			
		}
	}
	
	//======================== extract all the features and put them in a table, this should be set
	public void setupTbl(Map<String, Integer> ruleStringToIDTable, Set<String> restrictedFeatureSet){
		
		/**key: rule abbreviated name
		 * value: list of microRuleFeature
		 ***/
		this.ruleFeatureTbl = new HashMap<String, List<MicroRuleFeature>>();
	
		for(Entry<String, Integer> entry : ruleStringToIDTable.entrySet()){
			String ruleFullName = entry.getKey();
			String ruleAbbrName = "r" + entry.getValue();//TODO

			List<MicroRuleFeature> microRuleFeatures = new ArrayList<MicroRuleFeature>();
			this.ruleFeatureTbl.put(ruleAbbrName, microRuleFeatures);
			
			/*if(useWholeRuleFeature){//e.g., [x] ||| a b c ||| c d e
				extractWholeRuleFeature(ruleFullName, ruleAbbrName, restrictedFeatureSet, microRuleFeatures);
			}
			
			if(useTargetSideRuleFeature){
				extractTargetRuleFeature(ruleFullName, restrictedFeatureSet, microRuleFeatures);
			}*/
			
			if(useTargetRuleNgramFeature){
				this.extractTargetNgramFeature(ruleFullName, restrictedFeatureSet, microRuleFeatures, this.wordMap);
			}
			
		}
		
	}
	
	/*
	private void extractWholeRuleFeature(String ruleFullName, String ruleAbbrName, Set<String> restrictedFeatureSet, List<MicroRuleFeature> microRuleFeatures){
		String featureName = null;
		if(this.useRuleIDName)
			featureName = ruleAbbrName;//abbreviated name
		else
			featureName = ruleFullName;//the whole rule name
		
		if(restrictedFeatureSet.contains(featureName)){
			MicroRuleFeature feature = new MicroRuleFeature(featureName, 1.0);
			microRuleFeatures.add(feature);
		}
		
	}
	
	private void extractTargetRuleFeature(String ruleFullName, Set<String> restrictedFeatureSet, List<MicroRuleFeature> microRuleFeatures){
		String targetRule = getTargetRule(ruleFullName);
		
		if(restrictedFeatureSet.contains(targetRule)){
			MicroRuleFeature feature = new MicroRuleFeature(targetRule, 1.0);
			microRuleFeatures.add(feature);
		}
		
	}
	*/
	
	private void extractTargetNgramFeature(String ruleFullName, Set<String> restrictedFeatureSet, List<MicroRuleFeature> microRuleFeatures, Map<String,String> convertMap){
		
		Map<String,Integer> ngramFeatures = computeTargetNgramFeature(ruleFullName, convertMap, this.startNgramOrder, this.endNgramOrder);
		
		for(Map.Entry<String,Integer> entry : ngramFeatures.entrySet()){			
			if(restrictedFeatureSet == null || restrictedFeatureSet.contains(entry.getKey())){
				MicroRuleFeature feature = new MicroRuleFeature(entry.getKey(), entry.getValue());
				microRuleFeatures.add(feature);
			}
		}
	}
	
	
	static public Map<String,Integer> computeTargetNgramFeature(String ruleFullName, Map<String,String> convertMap, int startNgramOrder, int endNgramOrder){
		
		String convertedTargetSide = replaceStringWithClass( getTargetRule(ruleFullName), convertMap);
		
		Map<String,Integer> resTbl = new HashMap<String,Integer>();		
		Ngram.getNgrams(resTbl, startNgramOrder, endNgramOrder, convertedTargetSide.split("\\s+"));		
		return resTbl;
	}
	
	
	/**convertMap convert a word to
	 * (1) specific function word
	 * (2) or function word class
	 * (3) or "oov"
	 * */
	static private String replaceStringWithClass(String inStr, Map<String,String> convertMap){
		StringBuffer outStr = new StringBuffer();
		String[] words = inStr.split("\\s+");
		for(int i=0; i<words.length; i++){
			String newWord = convertWord(words[i], convertMap);
			outStr.append(newWord);
			if(i<words.length-1){
				outStr.append(" ");
			}
		}
		//System.out.println(inStr + " === " + outStr.toString() );
		return outStr.toString();
	}
	
	/**if the map does not know how to convert, then convert to 
	 * oov*/
	 static  private String convertWord(String inWord, Map<String,String> convertMap){
		String res = convertMap.get(inWord);
		if(res==null)
			res = "oov";
		return res;
	}
	
	 
	static private String getTargetRule(String ruleFullName){
		String[] fds = ruleFullName.split("\\s+\\|{3}\\s+");//TODO
		if(fds.length<3){
			logger.severe("ruleFuleName does not have at least three fields, " + ruleFullName);
			System.exit(1);
		}
		return fds[2];
	}
	

//	======== read word map
	static public Map<String, String> readWordMap(String mapFile) throws IOException{
		LineReader     mapReader = new LineReader(mapFile);
		Map<String, String> wordMap = new HashMap<String, String>();
		for (String line : mapReader) {
			String[] fds = line.trim().split("\\s+\\|{3}\\s+");
			wordMap.put(fds[0], fds[1]);
		}
		mapReader.close();
		return wordMap;
	}
}
