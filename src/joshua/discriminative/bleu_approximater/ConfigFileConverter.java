package joshua.discriminative.bleu_approximater;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.JoshuaDecoder;
import joshua.decoder.ff.ArityPhrasePenaltyFF;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.SourcePathFF;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.decoder.ff.lm.LanguageModelFF;
import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.feature_related.feature_function.BLEUOracleModel;
import joshua.util.FileUtility;
import joshua.util.Regex;
import joshua.util.io.LineReader;

public class ConfigFileConverter {
	
	
	static Logger logger = Logger.getLogger(ConfigFileConverter.class.getSimpleName());
	
	public static List<Double> readGoogleWeightsFromJoshuaConfig(String joshuaConfig) throws IOException{
		List<Double> res = new ArrayList<Double>();
		LineReader     reader = new LineReader(joshuaConfig);
		for (String line : reader) {
			line = line.trim();
			
			//comment, empty line, or parameter lines
			if (!Regex.commentOrEmptyLine.matches(line) && line.indexOf("googleBLEUWeights") != -1) {
				
				String[] fds = Regex.equalsWithSpaces.split(line);
				if (fds.length != 2) {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
				
				//== add new lines for models
				//googleBLEUWeights=1.0;-1.0;1;9;2
																	
				String[] weights = fds[1].trim().split(";");//1;0.1;
				if(weights.length!=5){
					logger.severe("Wrong number of weights in line: " + line);
					System.exit(1);
				}
				for(int i=0; i<5; i ++){//0gram 1.0
					double weight = new Double(weights[i]);
					res.add(weight);
				}							
				
			} 
		}
		reader.close();
		if(res.size()!=5){
			logger.severe("Wrong number of google weights, " + res.size());
			System.exit(1);
		}
		return res;		
	}
	
	static public void convertJoshuaToMertFormat(String joshuaConfig, String mertConfigTemplate,  String outputFile) throws IOException{
		List<Double> weights = readGoogleWeightsFromJoshuaConfig(joshuaConfig);
		double[] newWeights = new double[weights.size()];
		for(int i=0; i<weights.size(); i++)
			newWeights[i] = weights.get(i);
		
		JoshuaDecoder.writeConfigFile(newWeights, mertConfigTemplate, outputFile, null);
	}
	
	static public List<Double> readGoogleWeightsFromMERTConfig(String mertConfig) throws IOException{
		List<Double> res = new ArrayList<Double>();
		LineReader     reader = new LineReader(mertConfig);
		for (String line : reader) {
			line = line.trim();
			
			//comment, empty line, or parameter lines
			if (!Regex.commentOrEmptyLine.matches(line) && line.indexOf("gramMatch") != -1) {
				
				String[] fds = Regex.spaces.split(line);
				if (fds.length != 2) {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
				
				res.add(new Double(fds[1]));
			} 
		}
		if(res.size()!=5){
			logger.severe("Wrong number of google weights, " + res.size());
			System.exit(1);
		}
		reader.close();
		return res;		
	}
	
	

	static public void convertMertToJoshuaFormat(String mertConfig, String joshuaConfigTemplate,  String outputFile) throws IOException{
		List<Double> weights = readGoogleWeightsFromMERTConfig(mertConfig);
		

		BufferedWriter writer = FileUtility.getWriteFileStream(outputFile);
		LineReader     reader = new LineReader(joshuaConfigTemplate);
		for (String line : reader) {
			line = line.trim();
			
			
			if (!Regex.commentOrEmptyLine.matches(line) && line.indexOf("googleBLEUWeights") != -1) {
				
				String[] fds = Regex.equalsWithSpaces.split(line);
				if (fds.length != 2) {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
				
				//== add new lines for models
				//googleBLEUWeights=1.0;-1.0;1;9;2
				StringBuffer newLine = new StringBuffer();
				newLine.append("googleBLEUWeights=");
				
				for(int i=0; i<5; i ++){//0gram 1.0
					newLine.append(weights.get(i));
					if(i<4)
						newLine.append(";");	
				}							
				newLine.append("\n");
				writer.write(newLine.toString());
			} else{
				writer.write(line+"\n");
			}
		}
		writer.close();
		reader.close();
		
	}
	
	
	public static String[] getReferenceFileNames(String configFile)	throws IOException {
		
		String[] referenceFiles= null;
		
		LineReader reader = new LineReader(configFile);
		try { 
			for (String line : reader) {
				line = line.trim();
				if (Regex.commentOrEmptyLine.matches(line)) 
					continue;
				
				if (line.indexOf("=") == -1) { // ignore lines with "="
					String[] fds = Regex.spaces.split(line);
					
					if ("oracle".equals(fds[0]) && fds.length >= 3) { //oracle files weight										
						referenceFiles = new String[fds.length-2];
						for(int i=0; i< referenceFiles.length; i++)
							referenceFiles[i] =  fds[i+1].trim();			
					}
				}
			} 
		} finally {
			reader.close();
		}
		
		return referenceFiles;
	}
	
	public static void main(String[] args) throws IOException {
		
		String joshuaConfig = args[0].trim();
		String mertConfig = args[1].trim();
		String outputTemplateFile = args[2].trim();
		boolean fromJoshuaToMert = new Boolean(args[3].trim());
		
		if(fromJoshuaToMert){			
			ConfigFileConverter.convertJoshuaToMertFormat(joshuaConfig, outputTemplateFile, mertConfig);
		}else{
			ConfigFileConverter.convertMertToJoshuaFormat(mertConfig, outputTemplateFile, joshuaConfig);			
		}
	}
	
	
}
