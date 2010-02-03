package joshua.discriminative.feature_related.feature_template;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import joshua.util.FileUtility;
import joshua.util.io.LineReader;

public class ExtractMicroRuleFeatsFromGrammar {

	public static void main(String[] args) throws IOException {
		
		String wordMapFile = args[0].trim();
		String grammarFile = args[1].trim();
		String featureFile = args[2].trim();
		
		//TODO
		int startNgramOrder = 2;
		int endNgramOrder = 2;
		
		//======== read word map		
		Map<String, String> wordMap = MicroRuleFT.readWordMap(wordMapFile);
		
		//======== read grmmar and extract features
		LineReader   grammarReader = new LineReader(grammarFile);
		Map<String, Integer> featMap = new HashMap<String, Integer>();		
		for (String line : grammarReader) {
			Map<String,Integer> tbl = MicroRuleFT.computeTargetNgramFeature(line.trim(), wordMap, startNgramOrder, endNgramOrder);
			featMap.putAll(tbl);
		}		
		grammarReader.close();
		
		//======== write the table out
		BufferedWriter writer = FileUtility.getWriteFileStream(featureFile);
		double initWeight = 0;
		for(String name : featMap.keySet()){
			writer.write(name +" ||| " +  initWeight +"\n");
		}
		writer.close();
		
	}
	
}
