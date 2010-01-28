package joshua.discriminative.training.contrastive_estimation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;

import joshua.discriminative.FileUtilityOld;

public class ConvertGrammarToFeatureFile {
	
	public static void main(String[] args) {
		
		//BLEU.computeEffectiveLen(null, false);
		
		String grammarFile = args[0];
		String featureFile = args[1];
		boolean includeLHS = new Boolean(args[2]);
		boolean includeForeign = new Boolean(args[3]);
		boolean includeEnglish = new Boolean(args[4]);
		
		
		double initWeight = 0;
		BufferedReader reader = FileUtilityOld.getReadFileStream(grammarFile ,"UTF-8");	
		
		//== get the hash table
		HashMap<String, Double> featureTbl = new HashMap<String, Double>(); 
		
		String line;
		int numLinesInGrammar = 0;
		while((line=FileUtilityOld.readLineLzf(reader))!=null){
			
			String[] fds = line.split("\\s+\\|{3}\\s+");// lhs ||| foreign ||| english ||| feature values
			
			StringBuffer featKey = new StringBuffer();
			
			if(includeLHS){
				featKey.append(fds[0]);
				featKey.append(" ||| ");
			}
			
			if(includeForeign){
				featKey.append(fds[1]);
				featKey.append(" ||| ");
			}
			
			if(includeEnglish){
				featKey.append(fds[2]);
				featKey.append(" ||| ");
			}
			
			featureTbl.put(featKey.toString(), initWeight);		
			numLinesInGrammar++;
		}
		FileUtilityOld.closeReadFile(reader);
		
		//== write the feature file
		BufferedWriter writer = FileUtilityOld.getWriteFileStream(featureFile ,"UTF-8");
		for(String featKey : featureTbl.keySet()){
			double featWeight = featureTbl.get(featKey);
			FileUtilityOld.writeLzf(writer, featKey + featWeight +"\n");
		}
		FileUtilityOld.closeWriteFile(writer);
		
		System.out.println("numLinesInGrammar= " + numLinesInGrammar);
		System.out.println("numFeature= " + featureTbl.size());
		
		
	} 
}
