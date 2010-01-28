package joshua.discriminative.variational_decoder;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.WordPenaltyFF;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.feature_function.BaselineComboFF;
import joshua.discriminative.feature_related.feature_function.FeatureTemplateBasedFF;
import joshua.discriminative.feature_related.feature_function.EdgeTblBasedBaselineFF;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.feature_related.feature_template.NgramFT;



/* anything that is configurable should go here
 * */

public class VariationalDecoderConfiguration {	
	//options for variational decoder
	static int stateID = 0;
	static int baselineLMOrder = 3;
	static double insideoutsideScalingFactor=1.0;
	static int ngramStateID =0;
	//end
	
	private static final Logger logger = Logger.getLogger(VariationalDecoderConfiguration.class.getName());

	public static void readConfigFile(String configFile) {
		
		BufferedReader configReader = FileUtilityOld.getReadFileStream(configFile);
		String line;
		while ((line = FileUtilityOld.readLineLzf(configReader)) != null) {
			//line = line.trim().toLowerCase();
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}
			
			if (line.indexOf("=") != -1) { // parameters
				String[] fds = line.split("\\s*=\\s*");
				if (fds.length != 2) {
					if (logger.isLoggable(Level.SEVERE)) logger.severe(
						"Wrong config line: " + line);
					System.exit(1);
				}
				
				if (0 == fds[0].compareTo("baseline_lm_order")) {
					baselineLMOrder = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("baseline_lm_order: %s", baselineLMOrder));
				} else if (0 == fds[0].compareTo("insideoutside_scaling_factor")) {
					insideoutsideScalingFactor = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("lm insideoutside_scaling_factor: %s", insideoutsideScalingFactor));
				}else if (0 == fds[0].compareTo("baseline_lm_feat_id")) {
					ngramStateID = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST)) logger.finest(String.format("baseline_lm_feat_id: %s", ngramStateID));
				}else {
					if (logger.isLoggable(Level.SEVERE)) logger.severe(
						"Wrong config line: " + line);
					System.exit(1);
				}
			}else{//models
				//do nothing
			}
		}
		FileUtilityOld.closeReadFile(configReader);
	}
	
	public static void initializeModels(String configFile, SymbolTable symbolTbl, List<FeatureFunction> featFunctions,  
				HashMap<VariationalNgramApproximator, FeatureTemplateBasedFF> approximatorMap) {
		
		BufferedReader configReader = FileUtilityOld.getReadFileStream(configFile);
		String line;
		while ((line = FileUtilityOld.readLineLzf(configReader)) != null) {
			//line = line.trim().toLowerCase();
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}
			
			if (line.indexOf("=") != -1) { // parameters
				//do nothing
			}else{//models
				String[] fds = line.split("\\s+");
				if (fds[0].compareTo("vbaseline") == 0 && fds.length == 2) { 
					//baseline feature
					double baselineWeight = new Double(fds[1].trim());
					
					FeatureFunction ff =  new EdgeTblBasedBaselineFF(ngramStateID+1+featFunctions.size(), baselineWeight);					
					featFunctions.add(ff);
					logger.info(String.format("Baseline feature wiht weight: " + baselineWeight));				
					
				}else if (fds[0].compareTo("vbaselinecombo") == 0 && fds.length > 2) { 
					//baseline combo features: vbaselinecombo list-of-baseline-features (each one should be " pos_id||inter-weight ") weight
					double weight = new Double(fds[fds.length-1].trim());
					List<Integer> positions = new ArrayList<Integer>();
					List<Double> interWeights = new ArrayList<Double>();
					for(int i=1; i<fds.length-1; i++){
						String[] tems = fds[i].split("\\|{2}");
						int pos = new Integer(tems[0]);
						double interWeight = new Double(tems[1]);
						positions.add(pos);
						interWeights.add(interWeight);
					}
					System.out.println("baseline combo model");
					FeatureFunction ff =  new BaselineComboFF(ngramStateID+1+featFunctions.size(), weight, positions, interWeights);
					featFunctions.add(ff);
					logger.info( String.format("Baseline combo model with weight: " + weight));		
					
				}else if(fds[0].compareTo("vconstituent") == 0 && fds.length == 2) {
					/*
					double constituentWeight = new Double(fds[1].trim());					
					HashMap<HyperEdge,Double> tbl = new HashMap<HyperEdge,Double>(); 
					FeatureFunction ff =  new BaselineFF(baselineLMFeatID+1+featFunctions.size(), constituentWeight, tbl);
					FeatureTemplate ft  = new FeatureTemplateConstituent();		
					VariationalLMApproximator rmodel = new VariationalLMApproximator(ff, ft, tbl);
					featFunctions.add(ff);
					approximatorMap.add(rmodel);
					 
					logger.info( String.format("constituent model with weight: " + constituentWeight));	
					*/
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				} else if(fds[0].compareTo("vlm")==0  && fds.length == 3){
					int vlmOrder = new Integer(fds[1].trim());
					if(vlmOrder> baselineLMOrder){
						System.out.println("varatioanl_ngram_order is greater than baseline_lm_order; must be wrong");
						System.exit(1);
					}
					double weight = new Double(fds[2].trim());					
					FeatureTemplate ft = new NgramFT(symbolTbl, true , ngramStateID, baselineLMOrder, vlmOrder, vlmOrder);
					FeatureTemplateBasedFF ff =  new FeatureTemplateBasedFF(ngramStateID+1+featFunctions.size(), weight, ft);
					
					VariationalNgramApproximator rmodel = new VariationalNgramApproximator(symbolTbl, ft, 1.0, vlmOrder);
					featFunctions.add(ff);
					approximatorMap.put(rmodel, ff);					
					logger.info( String.format("vlm feature with weight: " + weight));	
					
				}else if(fds[0].compareTo("word_penalty_weight") == 0 && fds.length == 2) {
					double weight = new Double(fds[1].trim());
					System.out.println("word penalty feature");
					FeatureFunction ff =  new WordPenaltyFF(ngramStateID+1+featFunctions.size(), weight);
					featFunctions.add(ff);					
					logger.info( String.format("word penalty feature with weight: " + weight));
					
				}else{
					if (logger.isLoggable(Level.SEVERE)) 
						logger.severe(	"Wrong config line: " + line);
					System.exit(1);
				}
			}
		}
		FileUtilityOld.closeReadFile(configReader);
	}
	
}
