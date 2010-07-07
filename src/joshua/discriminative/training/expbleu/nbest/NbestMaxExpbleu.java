package joshua.discriminative.training.expbleu.nbest;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.BLEU;
import joshua.decoder.JoshuaDecoder;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.feature_related.feature_function.FeatureTemplateBasedFF;
import joshua.discriminative.feature_related.feature_template.BaselineFT;
import joshua.discriminative.feature_related.feature_template.FeatureTemplate;
import joshua.discriminative.feature_related.feature_template.IndividualBaselineFT;
import joshua.discriminative.feature_related.feature_template.MicroRuleFT;
import joshua.discriminative.feature_related.feature_template.NgramFT;
import joshua.discriminative.feature_related.feature_template.TMFT;
import joshua.discriminative.feature_related.feature_template.TargetTMFT;
import joshua.discriminative.ranker.HGRanker;
import joshua.discriminative.training.risk_annealer.AbstractMinRiskMERT;
import joshua.discriminative.training.risk_annealer.GradientOptimizer;
import joshua.discriminative.training.risk_annealer.hypergraph.HGAndReferences;
import joshua.discriminative.training.risk_annealer.hypergraph.HGMinRiskDAMert;
import joshua.discriminative.training.risk_annealer.hypergraph.HyperGraphFactory;
import joshua.discriminative.training.risk_annealer.hypergraph.MRConfig;
import joshua.util.FileUtility;

public class NbestMaxExpbleu extends AbstractMinRiskMERT {

	private SymbolTable symbolTbl;
	private boolean haveRefereces = true;
	private String hypFilePrefix;
	private String sourceTrainingFile;
	private Integer oralceFeatureID;
	private double curLossScale;
	private JoshuaDecoder joshuaDecoder;
	private ArrayList<FeatureTemplate> featTemplates;
	private MicroRuleFT microRuleFeatureTemplate;
	private boolean useIntegerString = false; // ToDo
	private String curConfigFile;
	private String curFeatureFile;
	private HashMap<String, Integer> featureStringToIntegerMap;
	private String curHypFilePrefix;
	private String[] devRefs;

	static private Logger logger = Logger.getLogger(HGMinRiskDAMert.class
			.getSimpleName());

	public NbestMaxExpbleu(String configFile, int numTrainingSentence,
			String[] devRefs, String hypFilePrefix, SymbolTable symbolTbl,
			String sourceTrainingFile) {
		super(configFile, numTrainingSentence, devRefs);
		// TODO Auto-generated constructor stub
		this.symbolTbl = symbolTbl;
		this.devRefs = devRefs;
		
		if (devRefs != null) {
			for (String refFile : devRefs) {
				logger.info("add symbols for file " + refFile);
				addAllWordsIntoSymbolTbl(refFile, symbolTbl);
			}
		} else {
			logger
					.info("Must include reference files in Max Expected Bleu Training");
			System.exit(1);
		}

		this.initialize();

		this.hypFilePrefix = hypFilePrefix;
		this.sourceTrainingFile = sourceTrainingFile;

		if (MRConfig.oneTimeHGRerank == false) {
			joshuaDecoder = JoshuaDecoder.getUninitalizedDecoder();
			joshuaDecoder.initialize(configFile);
		}

	}

	private void initialize() {
		// TODO Auto-generated method stub
		// ===== read configurations
		MRConfig.readConfigFile(this.configFile);
		// ===== initialize the featureTemplates
		setupFeatureTemplates();

		// ====== initialize featureStringToIntegerMap and weights
		initFeatureMapAndWeights(MRConfig.featureFile);
	}

	private void initFeatureMapAndWeights(String featureFile) {
		// TODO Auto-generated method stub
		featureStringToIntegerMap = new HashMap<String, Integer>();
		List<Double> temInitWeights = new ArrayList<Double>();
		int featID = 0;

		// ==== baseline feature
		if (MRConfig.useBaseline) {
			featureStringToIntegerMap.put(MRConfig.baselineFeatureName,
					featID++);
			temInitWeights.add(MRConfig.baselineFeatureWeight);
		}

		// ==== individual bs feature
		if (MRConfig.useIndividualBaselines) {
			List<Double> weights = readBaselineFeatureWeights(this.configFile);
			for (int id : MRConfig.baselineFeatIDsToTune) {
				String featName = MRConfig.individualBSFeatNamePrefix + id;
				featureStringToIntegerMap.put(featName, featID++);
				double weight = weights.get(id);
				temInitWeights.add(weight);
			}
		}

		// ==== features in file
		if (MRConfig.useSparseFeature) {
			BufferedReader reader = FileUtilityOld.getReadFileStream(
					featureFile, "UTF-8");
			String line;
			while ((line = FileUtilityOld.readLineLzf(reader)) != null) {
				String[] fds = line.split("\\s+\\|{3}\\s+");// feature_key |||
															// feature vale; the
															// feature_key
															// itself may
															// contain "|||"
				StringBuffer featKey = new StringBuffer();
				for (int i = 0; i < fds.length - 1; i++) {
					featKey.append(fds[i]);
					if (i < fds.length - 2)
						featKey.append(" ||| ");
				}
				double initWeight = new Double(fds[fds.length - 1]);// initial
																	// weight
				temInitWeights.add(initWeight);
				featureStringToIntegerMap.put(featKey.toString(), featID++);
			}
			FileUtilityOld.closeReadFile(reader);
		}

		// ==== initialize lastWeightVector
		numPara = temInitWeights.size();
		lastWeightVector = new double[numPara];
		for (int i = 0; i < numPara; i++)
			lastWeightVector[i] = temInitWeights.get(i);
	}

	private void setupFeatureTemplates() {
		// TODO Auto-generated method stub
		this.featTemplates = new ArrayList<FeatureTemplate>();

		if (MRConfig.useBaseline) {
			FeatureTemplate ft = new BaselineFT(MRConfig.baselineFeatureName,
					true);
			featTemplates.add(ft);
		}

		if (MRConfig.useIndividualBaselines) {
			for (int id : MRConfig.baselineFeatIDsToTune) {
				String featName = MRConfig.individualBSFeatNamePrefix + id;
				FeatureTemplate ft = new IndividualBaselineFT(featName, id,
						true);
				featTemplates.add(ft);
			}
		}

		if (MRConfig.useSparseFeature) {

			if (MRConfig.useMicroTMFeat) {
				// FeatureTemplate ft = new TMFT(symbolTbl, useIntegerString,
				// MRConfig.useRuleIDName);
				this.microRuleFeatureTemplate = new MicroRuleFT(
						MRConfig.useRuleIDName, MRConfig.startTargetNgramOrder,
						MRConfig.endTargetNgramOrder, MRConfig.wordMapFile);
				featTemplates.add(microRuleFeatureTemplate);
			}

			if (MRConfig.useTMFeat) {
				FeatureTemplate ft = new TMFT(symbolTbl, useIntegerString,
						MRConfig.useRuleIDName);
				featTemplates.add(ft);
			}

			if (MRConfig.useTMTargetFeat) {
				FeatureTemplate ft = new TargetTMFT(symbolTbl, useIntegerString);
				featTemplates.add(ft);
			}

			if (MRConfig.useLMFeat) {
				FeatureTemplate ft = new NgramFT(symbolTbl, useIntegerString,
						MRConfig.ngramStateID, MRConfig.baselineLMOrder,
						MRConfig.startNgramOrder, MRConfig.endNgramOrder);
				featTemplates.add(ft);
			}
		}

		System.out.println("feature template are " + featTemplates.toString());
	}

	@Override
	public void decodingTestSet(double[] weights, String nbestFile) {
		// TODO Auto-generated method stub
		/**
		 * three scenarios: (1) individual baseline features (2) baselineCombo +
		 * sparse feature (3) individual baseline features + sparse features
		 */

		if (MRConfig.useSparseFeature)
			joshuaDecoder.changeFeatureWeightVector(
					getIndividualBaselineWeights(), this.curFeatureFile);
		else
			joshuaDecoder.changeFeatureWeightVector(
					getIndividualBaselineWeights(), null);

		// call Joshua decoder to produce an hypergraph using the new weight
		// vector
		joshuaDecoder.decodeTestSet(sourceTrainingFile, nbestFile);

	}
	public static boolean mergeNbest(String oldMergedNbestFile, String newNbestFile, String newMergedNbestFile){
		boolean haveNewHyp =false;
		BufferedReader newNbestReader = FileUtilityOld.getReadFileStream(newNbestFile);
		BufferedReader oldMergedNbestReader = FileUtilityOld.getReadFileStream(oldMergedNbestFile);
		BufferedWriter newMergedNbestReader =	FileUtilityOld.getWriteFileStream(newMergedNbestFile);		
		
		int oldSentID=-1;
		String line;
		String previousLineInNewNbest = FileUtilityOld.readLineLzf(newNbestReader);;
		HashMap<String, String> oldNbests = new HashMap<String, String>();//key: hyp itself, value: remaining fds exlcuding sent_id
		while((line=FileUtilityOld.readLineLzf(oldMergedNbestReader))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			int newSentID = new Integer(fds[0]);
			if(oldSentID!=-1 && oldSentID!=newSentID){
				boolean[] t_have_new_hyp = new boolean[1];
				previousLineInNewNbest = processNbest(newNbestReader, newMergedNbestReader, oldSentID, oldNbests, previousLineInNewNbest, t_have_new_hyp);
				if(t_have_new_hyp[0]==true) 
					haveNewHyp = true;
			}
			oldSentID = newSentID;
			oldNbests.put(fds[1], fds[2]);//last field is not needed
		}
		//last nbest
		boolean[] t_have_new_hyp = new boolean[1];
		previousLineInNewNbest= processNbest(newNbestReader, newMergedNbestReader, oldSentID, oldNbests, previousLineInNewNbest, t_have_new_hyp);
		if(previousLineInNewNbest!=null){
			System.out.println("last line is not null, must be wrong"); 
			System.exit(0);
		}
		if(t_have_new_hyp[0]==true) 
			haveNewHyp = true;
		
		FileUtilityOld.closeReadFile(oldMergedNbestReader);
		FileUtilityOld.closeReadFile(newNbestReader);
		FileUtilityOld.closeWriteFile(newMergedNbestReader);
		return haveNewHyp;
	}
	private static String processNbest(BufferedReader newNbestReader, BufferedWriter newMergedNbestReader, int oldSentID, HashMap<String, String> oldNbests,
			String previousLine, boolean[] have_new_hyp){
		have_new_hyp[0] = false;
		String previousLineInNewNbest = previousLine;
//		#### read new nbest and merge into nbests
		while(true){
			String[] t_fds = previousLineInNewNbest.split("\\s+\\|{3}\\s+");
			int t_new_id = new Integer(t_fds[0]); 
			if( t_new_id == oldSentID){
				if(oldNbests.containsKey(t_fds[1])==false){//new hyp
					have_new_hyp[0] = true;
					oldNbests.put(t_fds[1], t_fds[2]);//merge into nbests
				}
			}else{
				break;
			}
			previousLineInNewNbest = FileUtilityOld.readLineLzf(newNbestReader);
			if(previousLineInNewNbest==null) 
				break;
		}
		//#### print the nbest: order is not important; and the last field is ignored
		for (Map.Entry<String, String> entry : oldNbests.entrySet()){ 
		    FileUtilityOld.writeLzf(newMergedNbestReader, oldSentID + " ||| " + entry.getKey() + " ||| " + entry.getValue() + "\n");
		}				
		oldNbests.clear();
		return previousLineInNewNbest;
	}
	@Override
	public void mainLoop() {
		// TODO Auto-generated method stub
		/**
		 * Here, we need multiple iterations as we do pruning when generate the
		 * hypergraph Note that DeterministicAnnealer itself many need to solve
		 * an optimization problem at each temperature, and each optimization is
		 * solved by LBFGS which itself involves many iterations (of computing
		 * gradients)
		 * */
		
		String oldmergedNbest = hypFilePrefix + ".merged";
		String newmergedNbest = "";
		for (int iter = 1; iter <= 20; iter++) {

			// ==== re-normalize weights, and save config files
			this.curConfigFile = configFile + "." + iter;
			this.curFeatureFile = MRConfig.featureFile + "." + iter;
			if (MRConfig.normalizeByFirstFeature)
				normalizeWeightsByFirstFeature(lastWeightVector, 0);
			saveLastModel(configFile, curConfigFile, MRConfig.featureFile,
					curFeatureFile);
			// writeConfigFile(lastWeightVector, configFile, configFile+"." +
			// iter);

			// ==== re-decode based on the new weights
			if (MRConfig.oneTimeHGRerank) {
				this.curHypFilePrefix = hypFilePrefix;
			} else {
				this.curHypFilePrefix = hypFilePrefix + "." + iter;
				decodingTestSet(null, curHypFilePrefix);
				System.out.println("Decoded: " + curHypFilePrefix);
				newmergedNbest = hypFilePrefix + ".merged" + "." + iter;
				if(iter == 1){
					try {
						FileUtility.copyFile(curHypFilePrefix, newmergedNbest);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(!mergeNbest(oldmergedNbest,curHypFilePrefix,newmergedNbest)){
					break;
				}
				oldmergedNbest = newmergedNbest;
				this.curHypFilePrefix = newmergedNbest;
			}
			

//      	Map<String, Integer>  ruleStringToIDTable = DiskHyperGraph.obtainRuleStringToIDTable(curHypFilePrefix+".hg.rules");
        	
        	//try to abbrevate the featuers if possible
//        	addAbbreviatedNames(ruleStringToIDTable);
        	
        	
        	//micro rule features
//        	if(MRConfig.useSparseFeature && MRConfig.useMicroTMFeat){	        	
//	        	this.microRuleFeatureTemplate.setupTbl(ruleStringToIDTable, featureStringToIntegerMap.keySet());
//        	}

//        	//=====compute onebest BLEU
//        	computeOneBestBLEU(curHypFilePrefix);
        	
   /*     	//==== run DA annealer to obtain optimal weight vector using the hypergraphs as training data 	
        	HyperGraphFactory hgFactory = new HyperGraphFactory(curHypFilePrefix, referenceFiles, MRConfig.ngramStateID,  symbolTbl, this.haveRefereces);   
         	GradientComputer gradientComputer = new HGRiskGradientComputer(MRConfig.useSemiringV2,
    				numTrainingSentence, numPara, MRConfig.gainFactor, 1.0, 0.0, true,
        			MRConfig.fixFirstFeature, hgFactory,
        			MRConfig.maxNumHGInQueue, MRConfig.numThreads,
        			
        			MRConfig.ngramStateID,  MRConfig.baselineLMOrder, symbolTbl,
        			featureStringToIntegerMap, featTemplates,
        			MRConfig.linearCorpusGainThetas,
        			this.haveRefereces
    		);
 	        
        	annealer = new DeterministicAnnealer(numPara,  lastWeightVector, MRConfig.isMinimizer, gradientComputer, 
        			MRConfig.useL2Regula, MRConfig.varianceForL2, MRConfig.useModelDivergenceRegula, MRConfig.lambda, MRConfig.printFirstN);
        	if(MRConfig.annealingMode==0)//do not anneal
        		lastWeightVector = annealer.runWithoutAnnealing(MRConfig.isScalingFactorTunable, MRConfig.startScaleAtNoAnnealing, MRConfig.temperatureAtNoAnnealing);
        	else if(MRConfig.annealingMode==1)
        		lastWeightVector = annealer.runQuenching(1.0);
        	else if(MRConfig.annealingMode==2)
        		lastWeightVector = annealer.runDAAndQuenching();
        	else{
        		logger.severe("unsorported anneal mode, " + MRConfig.annealingMode);
        		System.exit(0);
        	}*/        	
        	
        	
//        	//=====re-compute onebest BLEU
//        	if(MRConfig.normalizeByFirstFeature)
//        		normalizeWeightsByFirstFeature(lastWeightVector, 0);
//
//
//        	computeOneBestBLEU(curHypFilePrefix);

        	//@todo: check convergency
        	NbestExpbleuGradientComputer comp = new NbestExpbleuGradientComputer(
        			this.curHypFilePrefix,
        			devRefs,
        			this.numPara,
        			MRConfig.gainFactor,
        			1.0, 0.0,
        			false, 
        			this.numTrainingSentence);
        	//        	comp.reComputeFunctionValueAndGradient(lastWeightVector);
        	GradientOptimizer lbfgsRunner = new GradientOptimizer(this.numPara, lastWeightVector, false, comp, 
        			MRConfig.useL2Regula, MRConfig.varianceForL2, MRConfig.useModelDivergenceRegula, MRConfig.lambda, MRConfig.printFirstN);
        	lastWeightVector = lbfgsRunner.runLBFGS();
		}

		//final output
		if(MRConfig.normalizeByFirstFeature)
			normalizeWeightsByFirstFeature(lastWeightVector, 0);        	     	
		saveLastModel(configFile, configFile + ".final", MRConfig.featureFile, MRConfig.featureFile + ".final");
//		writeConfigFile(lastWeightVector, configFile, configFile+".final");

		//System.out.println("#### Final weights are: ");
		//annealer.getLBFGSRunner().printStatistics(-1, -1, null, lastWeightVector);
	}	


	private void addAbbreviatedNames(Map<String, Integer> rulesIDTable){
//		try to abbrevate the featuers if possible
    	if(MRConfig.useRuleIDName){
    		//add the abbreviated feature name into featureStringToIntegerMap
    		
    		//System.out.println("size1=" + featureStringToIntegerMap.size());
    		
    		for(Entry<String, Integer> entry : rulesIDTable.entrySet()){
    			Integer featureID = featureStringToIntegerMap.get(entry.getKey());
    			if(featureID!=null){
    				String abbrFeatName = "r" + entry.getValue();//TODO??????
    				featureStringToIntegerMap.put(abbrFeatName, featureID);
    				//System.out.println("full="+entry.getKey() + "; abbrFeatName="+abbrFeatName + "; id="+featureID);
    			}
    		}
    		//System.out.println("size2=" + featureStringToIntegerMap);
    		//System.exit(0);
    	}
    	
	}

	private void computeOneBestBLEU(String curHypFilePrefix){
		if(this.haveRefereces==false)
			return;
		
		double bleuSum = 0;
		double googleGainSum = 0;
		double modelSum = 0;
		
		//==== feature-based feature
		int featID = 999;
		double weight = 1.0;
		HashSet<String> restrictedFeatureSet = null;
		HashMap<String, Double> modelTbl = (HashMap<String, Double>) obtainModelTable(this.featureStringToIntegerMap, this.lastWeightVector);
		//System.out.println("modelTable: " + modelTbl);
		FeatureFunction ff = new FeatureTemplateBasedFF(featID, weight, modelTbl, this.featTemplates, restrictedFeatureSet); 

		//==== reranker
		List<FeatureFunction> features =  new ArrayList<FeatureFunction>();
		features.add(ff);
		HGRanker reranker = new HGRanker(features);	
		
		
		//==== kbest 
		boolean addCombinedCost = false;	
		KBestExtractor kbestExtractor = new KBestExtractor(symbolTbl, MRConfig.use_unique_nbest, MRConfig.use_tree_nbest, false, addCombinedCost, false, true);
		
		//==== loop
		HyperGraphFactory hgFactory = new HyperGraphFactory(curHypFilePrefix, referenceFiles, MRConfig.ngramStateID,  symbolTbl, true);
		hgFactory.startLoop();
		for(int sentID=0; sentID< this.numTrainingSentence; sentID ++){			
			HGAndReferences res = hgFactory.nextHG();
			reranker.rankHG(res.hg);//reset best pointer and transition prob
		
			String hypSent = kbestExtractor.getKthHyp(res.hg.goalNode, 1, -1, null, null);
			double bleu = BLEU.computeSentenceBleu(res.referenceSentences, hypSent);
			bleuSum  += bleu;
			
			double googleGain = BLEU.computeLinearCorpusGain(MRConfig.linearCorpusGainThetas, res.referenceSentences, hypSent);
			googleGainSum += googleGain;
			
			modelSum +=  res.hg.bestLogP();
			//System.out.println("logP=" + res.hg.bestLogP() + "; Bleu=" + bleu +"; googleGain="+googleGain);
							
		}
		hgFactory.endLoop();
		
		System.out.println("AvgLogP=" + modelSum/this.numTrainingSentence + "; AvgBleu=" + bleuSum/this.numTrainingSentence
				+ "; AvgGoogleGain=" + googleGainSum/this.numTrainingSentence + "; SumGoogleGain=" + googleGainSum);
	}

	private Map<String, Double> obtainModelTable(
			HashMap<String, Integer> featureStringToIntegerMap, double[] weightVector){
				HashMap<String,Double> modelTbl = new HashMap<String,Double>();
				for(Map.Entry<String,Integer> entry : featureStringToIntegerMap.entrySet()){
					int featID = entry.getValue();
					double weight =  lastWeightVector[featID];//last model
					modelTbl.put(entry.getKey(), weight);
				}
				return modelTbl;
			}

	private void saveLastModel(String configTemplate, String configOutput,
			String sparseFeaturesTemplate, String sparseFeaturesOutput) {
		// TODO Auto-generated method stub
		if (MRConfig.useSparseFeature) {
			JoshuaDecoder.writeConfigFile(getIndividualBaselineWeights(),
					configTemplate, configOutput, sparseFeaturesOutput);
			saveSparseFeatureFile(sparseFeaturesTemplate, sparseFeaturesOutput);
		} else {
			JoshuaDecoder.writeConfigFile(getIndividualBaselineWeights(),
					configTemplate, configOutput, null);
		}
	}

	private void saveSparseFeatureFile(String fileTemplate, String outputFile) {
		// TODO Auto-generated method stub
		BufferedReader template = FileUtilityOld.getReadFileStream(
				fileTemplate, "UTF-8");
		BufferedWriter writer = FileUtilityOld.getWriteFileStream(outputFile);
		String line;

		while ((line = FileUtilityOld.readLineLzf(template)) != null) {
			// == construct feature name
			String[] fds = line.split("\\s+\\|{3}\\s+");// feature_key |||
														// feature vale; the
														// feature_key itself
														// may contain "|||"
			StringBuffer featKey = new StringBuffer();
			for (int i = 0; i < fds.length - 1; i++) {
				featKey.append(fds[i]);
				if (i < fds.length - 2)
					featKey.append(" ||| ");
			}

			// == write the learnt weight
			// double oldWeight = new Double(fds[fds.length-1]);//initial weight
			int featID = this.featureStringToIntegerMap.get(featKey.toString());
			double newWeight = lastWeightVector[featID];// last model
			// System.out.println(featKey +"; old=" + oldWeight + "; new=" +
			// newWeight);
			FileUtilityOld.writeLzf(writer, featKey.toString() + " ||| "
					+ newWeight + "\n");

			featID++;
		}
		FileUtilityOld.closeReadFile(template);
		FileUtilityOld.closeWriteFile(writer);
	}
private double[] getIndividualBaselineWeights(){
		
		double baselineWeight = 1.0;
		if(MRConfig.useBaseline)
			baselineWeight = getBaselineWeight();
		
		List<Double> weights  = readBaselineFeatureWeights(this.configFile);
		
		//change the weights we are tunning
		if(MRConfig.useIndividualBaselines){		
			for(int id : MRConfig.baselineFeatIDsToTune){				
				String featName = MRConfig.individualBSFeatNamePrefix +id;
				int featID = featureStringToIntegerMap.get(featName);
				weights.set(id, baselineWeight*lastWeightVector[featID]);				
			}
		}
		
		if(MRConfig.lossAugmentedPrune){
			String featName = MRConfig.individualBSFeatNamePrefix +this.oralceFeatureID;
			if(featureStringToIntegerMap.containsKey(featName)){
				logger.severe("we are tuning the oracle model, must be wrong in specifying baselineFeatIDsToTune");
				System.exit(1);
			}
			
			weights.set(this.oralceFeatureID, this.curLossScale);
			System.out.println("curLossScale=" + this.curLossScale + "; oralceFeatureID="+this.oralceFeatureID);
		}
		
		double[] res = new double[weights.size()];
		for(int i=0; i<res.length; i++)
			res[i] = weights.get(i);
		return res;
	}
	

	private double getBaselineWeight(){
		String featName = MRConfig.baselineFeatureName;
		int featID = featureStringToIntegerMap.get(featName);
		double weight = lastWeightVector[featID];
		System.out.println("baseline weight is " + weight);
		return weight;
	}
	static public void addAllWordsIntoSymbolTbl(String file,
			SymbolTable symbolTbl) {
		BufferedReader reader = FileUtilityOld.getReadFileStream(file, "UTF-8");
		String line;
		while ((line = FileUtilityOld.readLineLzf(reader)) != null) {
			symbolTbl.addTerminals(line);
		}
		FileUtilityOld.closeReadFile(reader);
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("Wrong number of parameters!");
			System.exit(1);
		}

		// long start_time = System.currentTimeMillis();
		String joshuaConfigFile = args[0].trim();
		String sourceTrainingFile = args[1].trim();
		String hypFilePrefix = args[2].trim();

		String[] devRefs = null;
		if (args.length > 3) {
			devRefs = new String[args.length - 3];
			for (int i = 3; i < args.length; i++) {
				devRefs[i - 3] = args[i].trim();
				System.out.println("Use ref file " + devRefs[i - 3]);
			}
		}
		SymbolTable symbolTbl = new BuildinSymbol(null);

		int numSentInDevSet = FileUtilityOld
				.numberLinesInFile(sourceTrainingFile);

		NbestMaxExpbleu trainer = new NbestMaxExpbleu(joshuaConfigFile,
				numSentInDevSet, devRefs, hypFilePrefix, symbolTbl,
				sourceTrainingFile);
		
		trainer.mainLoop();
	}

}
