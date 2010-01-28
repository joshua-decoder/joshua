package joshua.training.risk_annealer.hypergraph;

import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;


import joshua.discriminative.FileUtilityOld;
import joshua.util.Regex;



public class MRConfig {

	//=== general
	public static boolean oneTimeHGRerank = false;
	public static int maxNumIter = 5;
	public static boolean useSemiringV2 = true;	
	public static int maxNumHGInQueue = 100;
	public static int numThreads = 4;
	public static boolean saveHGInMemory;

	//==disk hg related
	public static int baselineLMOrder;	
	public static int ngramStateID;

	
	//== first feature options
	public static boolean fixFirstFeature = true;
	public static boolean normalizeByFirstFeature = false;
	
    
	//=== option for not using annealing at all
	public static int annealingMode = 0;//0:no annealing; 1: quenching; 2: DA+Quenching	 
	public static double temperatureAtNoAnnealing = 0;
	public static double startScaleAtNoAnnealing = 1;
	public static double gainFactor = 1.0;//argmax gainfactor*gain + T*Enropy
	public static boolean isMinimizer = false;
	public static boolean useL2Regula = false;
	public static double varianceForL2 = 1;
	
	/*when we do not anneal, is the scaling factor a parameter in the tuning?*/
	public static boolean isScalingFactorTunable = false; 
	
	
	//=== use goolge linear corpus gain?
	public static boolean useGoogleLinearCorpusGain = false;
	public static double unigramPrecision = 0.85;
	public static double precisionDecayRatio = 0.7;
	public static int numUnigramTokens = 10;
	
	
	//======= feature realtes
	//public static boolean doFeatureFiltering;
	
	//== dense features
	public static boolean useBaseline;
	public static String baselineFeatureName;
	public static double baselineFeatureWeight = 1.0;
	
	public static boolean useIndividualBaselines;
	public static String individualBSFeatNamePrefix="bs";
	public static int numIndividualBaselines = 5;
	
	//== sparse features
	public static String featureFile;
	
	public static boolean useSparseFeature;
	public static boolean useTMFeat;
	public static boolean useRuleIDName= true;
	
	public static boolean useTMTargetFeat;
	public static boolean useLMFeat;
	public static int startNgramOrder = 1;
	public static int endNgramOrder = 2;
	
	
		
	private static final Logger logger =
		Logger.getLogger(MRConfig.class.getName());
	
	public static void readConfigFile(String configFile){
		BufferedReader reader = FileUtilityOld.getReadFileStream(configFile);
		String line;
		while ((line = FileUtilityOld.readLineLzf(reader)) != null) {
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}			
			if (line.indexOf("=") != -1) { // parameters
				String[] fds = Regex.equalsWithSpaces.split(line);
				if (fds.length != 2) {
					logger.severe("Wrong config line: " + line);
					System.exit(1);
				}
				
				if ("useGoogleLinearCorpusGain".equals(fds[0])) {
					useGoogleLinearCorpusGain = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useGoogleLinearCorpusGain: %s", useGoogleLinearCorpusGain));					
				}else if ("unigramPrecision".equals(fds[0])) {
					unigramPrecision = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("unigramPrecision: %s", unigramPrecision));					
				}else if ("precisionDecayRatio".equals(fds[0])) {
					precisionDecayRatio = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("precisionDecayRatio: %s", precisionDecayRatio));					
				}else if ("numUnigramTokens".equals(fds[0])) {
					numUnigramTokens = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("numUnigramTokens: %s", numUnigramTokens));					
				}else if ("oneTimeHGRerank".equals(fds[0])) {
					oneTimeHGRerank = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("oneTimeHGRerank: %s", oneTimeHGRerank));					
				} else if ("annealingMode".equals(fds[0])) {
					annealingMode = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("annealingMode: %s", annealingMode));					
				} else if ("useL2Regula".equals(fds[0])) {
					useL2Regula = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useL2Regula: %s", useL2Regula));					
				} else if ("varianceForL2".equals(fds[0])) {
					varianceForL2 = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("varianceForL2: %s", varianceForL2));					
				} else if ("isScalingFactorTunable".equals(fds[0])) {
					isScalingFactorTunable = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("isScalingFactorTunable: %s", isScalingFactorTunable));					
				} else if ("maxNumIter".equals(fds[0])) {
					maxNumIter = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("maxNumIter: %s", maxNumIter));										
				} else if ("baselineLMOrder".equals(fds[0]) || "order".equals(fds[0])) {
					baselineLMOrder = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("baselineLMOrder: %s", baselineLMOrder));					
				} else if ("ngramStateID".equals(fds[0])) {
					ngramStateID = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("ngramStateID: %s", ngramStateID));					
				}  /*else if ("doFeatureFiltering".equals(fds[0])) {
					doFeatureFiltering = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("doFeatureFiltering: %s", doFeatureFiltering));					
				}*/ else if ("useBaseline".equals(fds[0])) {
					useBaseline = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useBaseline: %s", useBaseline));					
				} else if ("baselineFeatureName".equals(fds[0])) {
					baselineFeatureName = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("baselineFeatureName: %s", baselineFeatureName));					
				} else if ("baselineFeatureWeight".equals(fds[0])) {
					baselineFeatureWeight = new Double( fds[1].trim() );
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("baselineFeatureWeight: %s", baselineFeatureWeight));					
				} else if ("useIndividualBaselines".equals(fds[0])) {
					useIndividualBaselines = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useIndividualBaselines: %s", useIndividualBaselines));					
				}else if ("numIndividualBaselines".equals(fds[0])) {
					numIndividualBaselines = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("numIndividualBaselines: %s", numIndividualBaselines));					
				} else if ("useSparseFeature".equals(fds[0])) {
					useSparseFeature = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useSparseFeature: %s", useSparseFeature));					
				} else if ("useTMFeat".equals(fds[0])) {
					useTMFeat = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useTMFeat: %s", useTMFeat));					
				} else if ("useRuleIDName".equals(fds[0])) {
					useRuleIDName = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useRuleIDName: %s", useRuleIDName));					
				} else if ("useTMTargetFeat".equals(fds[0])) {
					useTMTargetFeat = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useTMTargetFeat: %s", useTMTargetFeat));					
				} else if ("useLMFeat".equals(fds[0])) {
					useLMFeat = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useLMFeat: %s", useLMFeat));					
				} else if ("startNgramOrder".equals(fds[0])) {
					startNgramOrder = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("startNgramOrder: %s", startNgramOrder));					
				} else if ("endNgramOrder".equals(fds[0])) {
					endNgramOrder = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("endNgramOrder: %s", endNgramOrder));					
				} else if ("saveHGInMemory".equals(fds[0])) {
					saveHGInMemory = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("saveHGInMemory: %s", saveHGInMemory));					
				} else if ("fixFirstFeature".equals(fds[0])) {
					fixFirstFeature = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("fixFirstFeature: %s", fixFirstFeature));					
				} else if ("useSemiringV2".equals(fds[0])) {
					useSemiringV2 = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useSemiringV2: %s", useSemiringV2));					
				} else if ("maxNumHGInQueue".equals(fds[0])) {
					maxNumHGInQueue = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("maxNumHGInQueue: %s", maxNumHGInQueue));					
				} else if ("numThreads".equals(fds[0])) {
					numThreads = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("numThreads: %s", numThreads));					
				} else if ("normalizeByFirstFeature".equals(fds[0])) {
					normalizeByFirstFeature = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("normalizeByFirstFeature: %s", normalizeByFirstFeature));					
				} 
				     
							
				
			}else{//models
				
				String[] fds = Regex.spaces.split(line);
				if ("discriminative".equals(fds[0]) && fds.length == 3) { //discriminative weight modelFile
										
					featureFile = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("featureFile: %s", featureFile));	
					
				}
			}
		}
		FileUtilityOld.closeReadFile(reader);
		
		/**three scenarios:
		 * (1) individual baseline features
		 * (2) baselineCombo + sparse feature
		 * (3) individual baseline features + sparse features
		*/
		
		if(useIndividualBaselines==true && useBaseline == false  && useSparseFeature == false){
			logger.info("========== regular MERT scenario: tune only baseline features");
		}else if(useIndividualBaselines==false && useBaseline == true  && useSparseFeature == true){
			logger.info("========== scenario: baselineCombo + sparseFeature");
		}else if(useIndividualBaselines==true && useBaseline == false  && useSparseFeature == true){
			logger.info("========== scenario: IndividualBaselines + sparseFeature");
		}else{
			logger.info("==== wrong training scenario ====");
			System.exit(1);
		}
		
		if(oneTimeHGRerank && maxNumIter!=1){
			logger.info("oneTimeHGRerank=true, but maxNumIter!=1");
			System.exit(1);
		}
	}

}
