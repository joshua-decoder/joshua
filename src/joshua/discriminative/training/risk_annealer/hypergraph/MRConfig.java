package joshua.discriminative.training.risk_annealer.hypergraph;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
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
	
	public static boolean useModelDivergenceRegula = false;
	public static double lambda = -1;
	
	/*when we do not anneal, is the scaling factor a parameter in the tuning?*/
	public static boolean isScalingFactorTunable = false; 
	
	
	//=== use goolge linear corpus gain?
	public static boolean useGoogleLinearCorpusGain = false;
	public static double[] linearCorpusGainThetas = null;
	
	
	//======= feature realtes
	//public static boolean doFeatureFiltering;
	
	//== dense features
	public static boolean useBaseline;
	public static String baselineFeatureName;
	public static double baselineFeatureWeight = 1.0;
	
	public static boolean useIndividualBaselines;
	public static String individualBSFeatNamePrefix="bs";
	public static List<Integer> baselineFeatIDsToTune;
	
	
	//== sparse features
	public static String featureFile;
	
	public static boolean useSparseFeature = false;
	public static boolean useTMFeat = false;
	public static boolean useRuleIDName= true;
	
	public static boolean useMicroTMFeat = true;
	public static String wordMapFile = null; /*tbl for mapping rule words*/
	public static int startTargetNgramOrder = 2;//TODO
	public static int endTargetNgramOrder = 2;//TODO
	
	
	public static boolean useTMTargetFeat = false;
	
	public static boolean useLMFeat;
	public static int startNgramOrder = 1;
	public static int endNgramOrder = 2;
	
	public static int printFirstN=2;
	
	//==loss augmented inferene
	public static boolean lossAugmentedPrune = false;
	public static double startLossScale = 10;
	public static double lossDecreaseConstant = 1;
	
	//nbest based training
	public static boolean use_unique_nbest    = false;
	public static boolean use_tree_nbest      = false;
		
	public static int topN = 500;
	public static boolean use_kbest_hg = false;
	public static double stop_hyp_ratio = 1e-2; //how many new hypotheses should be generated before converge
	
	public static int hyp_merge_mode  = 2; //0: no merge; 1: merge without de-duplicate; 2: merge with de-duplicate
	
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
				} else if ("googleBLEUWeights".equals(fds[0])) {
					String[] googleWeights = fds[1].trim().split(";");
					if(googleWeights.length!=5){
						logger.severe("wrong line=" + line);
						System.exit(1);
					}
					linearCorpusGainThetas = new double[5];
					for(int i=0; i<5; i++)
						linearCorpusGainThetas[i] = new Double(googleWeights[i]);
					
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("googleBLEUWeights: %s", linearCorpusGainThetas));		
					
				} else if ("lossAugmentedPrune".equals(fds[0])) {
					lossAugmentedPrune = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lossAugmentedPrune: %s", lossAugmentedPrune));					
				} else if ("startLossScale".equals(fds[0])) {
					startLossScale = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("startLossScale: %s", startLossScale));					
				} else if ("lossDecreaseConstant".equals(fds[0])) {
					lossDecreaseConstant = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lossDecreaseConstant: %s", lossDecreaseConstant));					
				} else if ("oneTimeHGRerank".equals(fds[0])) {
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
				} else if ("useModelDivergenceRegula".equals(fds[0])) {
					useModelDivergenceRegula = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useModelDivergenceRegula: %s", useModelDivergenceRegula));					
				} else if ("lambda".equals(fds[0])) {
					lambda = new Double(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("lambda: %s", lambda));					
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
				}else if ("baselineFeatIDsToTune".equals(fds[0])) {
					String[] ids = fds[1].trim().split(";");
					baselineFeatIDsToTune = new ArrayList<Integer>();
					for(String id : ids){
						baselineFeatIDsToTune.add(new Integer(id.trim()));
					}
					System.out.println(String.format("baselineFeatIDsToTune: %s", baselineFeatIDsToTune));					
				} else if ("useSparseFeature".equals(fds[0])) {
					useSparseFeature = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useSparseFeature: %s", useSparseFeature));					
				} else if ("wordMapFile".equals(fds[0])) {
					wordMapFile = fds[1].trim();
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("wordMapFile: %s", wordMapFile));					
				} else if ("useTMFeat".equals(fds[0])) {
					useTMFeat = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useTMFeat: %s", useTMFeat));					
				} else if ("useMicroTMFeat".equals(fds[0])) {
					useMicroTMFeat = new Boolean(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("useMicroTMFeat: %s", useMicroTMFeat));					
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
				} else if ("printFirstN".equals(fds[0])) {
					printFirstN = new Integer(fds[1].trim());
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("printFirstN: %s", printFirstN));					
				} else if ("use_unique_nbest".equals(fds[0])) {
					use_unique_nbest = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_unique_nbest: %s", use_unique_nbest));					
				} else if ("use_tree_nbest".equals(fds[0])) {
					use_tree_nbest = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_tree_nbest: %s", use_tree_nbest));					
				} else if ("top_n".equals(fds[0])) {
					topN = Integer.parseInt(fds[1]);
					if (logger.isLoggable(Level.FINEST))
						logger.finest(String.format("topN: %s", topN));					
				} else if ("use_kbest_hg".equals(fds[0])) {
					use_kbest_hg = Boolean.valueOf(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("use_kbest_hg: %s", use_kbest_hg));					
				} else if ("hyp_merge_mode".equals(fds[0])) {
					hyp_merge_mode = new Integer(fds[1]);
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("hyp_merge_mode: %s", hyp_merge_mode));					
				} else if ("stop_hyp_ratio".equals(fds[0])) {
					stop_hyp_ratio = new Double( fds[1].trim() );
					if (logger.isLoggable(Level.FINEST)) 
						logger.finest(String.format("stop_hyp_ratio: %s", stop_hyp_ratio));					
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
		
		if( useGoogleLinearCorpusGain && linearCorpusGainThetas==null ){
			logger.info("linearCorpusGainThetas is null, did you set googleBLEUWeights properly?");
			System.exit(1);
		}else if(linearCorpusGainThetas.length!=5){
			logger.info("linearCorpusGainThetas does not have five values, did you set googleBLEUWeights properly?");
			System.exit(1);
		}
		
		if(oneTimeHGRerank && maxNumIter!=1){
			logger.info("oneTimeHGRerank=true, but maxNumIter!=1");
			System.exit(1);
		}
		
		if(use_kbest_hg==false && hyp_merge_mode==2){
			logger.warning("use_kbest_hg==false && hyp_merge_mode==2, cannot do dedup-merge for real hypergraph-based training, back to nbest merge, but trained on hg");
			//System.exit(1);
		}
	}

}
