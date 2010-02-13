package joshua.discriminative.training.risk_annealer.hypergraph;

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
import joshua.discriminative.training.NbestMerger;
import joshua.discriminative.training.risk_annealer.AbstractMinRiskMERT;
import joshua.discriminative.training.risk_annealer.DeterministicAnnealer;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.nbest.NbestMinRiskDAMert;
import joshua.util.FileUtility;

public class HGMinRiskDAMert extends AbstractMinRiskMERT {
	
	JoshuaDecoder joshuaDecoder;
	String sourceTrainingFile;

	SymbolTable symbolTbl;
	
	List<FeatureTemplate> featTemplates;
	HashMap<String, Integer> featureStringToIntegerMap;
	
	MicroRuleFT microRuleFeatureTemplate = null;
	
	String hypFilePrefix;//training hypothesis file prefix
	String curConfigFile;
	String curFeatureFile;
	String curHypFilePrefix;
	
	boolean useIntegerString = false;//TODO
	
	boolean haveRefereces = true;
	
	int oldTotalNumHyp = 0;
	
	
	//== for loss-augmented pruning
	double curLossScale = 0;
	int oralceFeatureID = 0;
	
	static private Logger logger = 
		Logger.getLogger(HGMinRiskDAMert.class.getSimpleName());
	
	
	public HGMinRiskDAMert(String configFile, int numSentInDevSet, String[] devRefs, String hypFilePrefix, SymbolTable symbolTbl, String sourceTrainingFile) {
		
		super(configFile, numSentInDevSet, devRefs);	
		this.symbolTbl = symbolTbl;
		
		if(devRefs!=null){
			haveRefereces = true;
			for(String refFile : devRefs){
				logger.info("add symbols for file " + refFile);
				addAllWordsIntoSymbolTbl(refFile, symbolTbl);
			}
		}else{
			haveRefereces = false;			
		}
		
		this.initialize();
		
		this.hypFilePrefix = hypFilePrefix;		
		this.sourceTrainingFile = sourceTrainingFile;
		
		if(MRConfig.oneTimeHGRerank==false){
			joshuaDecoder = JoshuaDecoder.getUninitalizedDecoder();
			joshuaDecoder.initialize(configFile);
		}
		
		//oralce id-realted
		Integer id = inferOracleFeatureID(this.configFile);
		if(id != null && MRConfig.lossAugmentedPrune==false ){
			logger.severe("lossAugmentedPrune=false, but has a oracle model");
			System.exit(1);
		}
		if(MRConfig.lossAugmentedPrune == true){
			if(id==null){
				logger.severe("no oralce model while doing loss-augmented pruning, must be wrong");
				System.exit(1);
			}else{
				this.oralceFeatureID = id;
			}
			
			this.curLossScale = MRConfig.startLossScale;
			logger.info("startLossScale="+MRConfig.startLossScale+"; oralceFeatureID="+this.oralceFeatureID);
		}
		
		
		if(haveRefereces==false){//minimize conditional entropy
			MRConfig.temperatureAtNoAnnealing = 1;//TODO
		}else{
			if(MRConfig.useModelDivergenceRegula){
				System.out.println("supervised training, we should not do model divergence regular");
				System.exit(0);
			}
		}
			
	}

	
	
	public void mainLoop(){
		
		/**Here, we need multiple iterations as we do pruning when generate the hypergraph
		 * Note that DeterministicAnnealer itself many need to solve an optimization problem at each temperature,
		 * and each optimization is solved by LBFGS which itself involves many iterations (of computing gradients) 
		 * */
        for(int iter=1; iter<=MRConfig.maxNumIter; iter++){
        	
        	//==== re-normalize weights, and save config files
        	this.curConfigFile =  configFile+"." + iter;
        	this.curFeatureFile = MRConfig.featureFile +"." + iter;
        	if(MRConfig.normalizeByFirstFeature)
        		normalizeWeightsByFirstFeature(lastWeightVector, 0);        	     	
        	saveLastModel(configFile, curConfigFile, MRConfig.featureFile, curFeatureFile);
        	//writeConfigFile(lastWeightVector, configFile, configFile+"." + iter);
        	
        	//==== re-decode based on the new weights
        	if(MRConfig.oneTimeHGRerank){
	        	this.curHypFilePrefix = hypFilePrefix; 
        	}else{
        		this.curHypFilePrefix = hypFilePrefix +"." + iter;
	        	decodingTestSet(null, curHypFilePrefix);
        	}

        	
        	//==== merge hypergrphs and check convergency
        	if(MRConfig.hyp_merge_mode>0){
        		try {
		        	String oldMergedFile = hypFilePrefix +".merged." + (iter-1);
		        	String newMergedFile = hypFilePrefix +".merged." + (iter);
		        	int newTotalNumHyp =0;
		        	
		        	if(MRConfig.use_kbest_hg==false && MRConfig.hyp_merge_mode==2){
		        		System.out.println("use_kbest_hg==false && MRConfig.hyp_merge_mode; we will look at the nbest");
		            	if(iter ==1){
		            		FileUtility.copyFile(curHypFilePrefix, newMergedFile);
		            		newTotalNumHyp = FileUtilityOld.numberLinesInFile(newMergedFile);
		            	}else{
		            		newTotalNumHyp = NbestMerger.mergeNbest(oldMergedFile, curHypFilePrefix, newMergedFile);
		                }		            	
	        		}else{		        	
			        	if(iter ==1){		        		
							FileUtility.copyFile(curHypFilePrefix+".hg.items", newMergedFile+".hg.items");						
			        		FileUtility.copyFile(curHypFilePrefix+".hg.rules", newMergedFile+".hg.rules");
			        	}else{
			        		boolean saveModelCosts = true;
			        		
				            /**TODO: this assumes that the feature values for the same hypothesis does not change,
				             * though the weights for these features can change. In particular, this means
				             * we cannot tune the weight for the aggregate discriminative model while we are tunining the individual 
				             * discriminative feature. This is also true for the bestHyperEdge pointer.*/
				            newTotalNumHyp = DiskHyperGraph.mergeDiskHyperGraphs(MRConfig.ngramStateID, saveModelCosts, this.numTrainingSentence, 
				            		MRConfig.use_unique_nbest, MRConfig.use_tree_nbest,
				            		oldMergedFile, curHypFilePrefix, newMergedFile, (MRConfig.hyp_merge_mode==2));
				            
			            }
			        	
			        	this.curHypFilePrefix = newMergedFile;
		        	}
		        	
		        	//check convergence
		        	double newRatio = (newTotalNumHyp-oldTotalNumHyp)*1.0/oldTotalNumHyp;
		        	if(iter <=2 || newRatio > MRConfig.stop_hyp_ratio) {
		        		System.out.println("oldTotalNumHyp=" + oldTotalNumHyp + "; newTotalNumHyp=" + newTotalNumHyp + "; newRatio="+ newRatio +";  at iteration " + iter);
		        		oldTotalNumHyp = newTotalNumHyp; 
		            }else{
		            	System.out.println("No new hypotheses generated at iteration " + iter + " for stop_hyp_ratio=" + MRConfig.stop_hyp_ratio); 
		            	break;
		            }
		        	
        		} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        	
        	Map<String, Integer>  ruleStringToIDTable = DiskHyperGraph.obtainRuleStringToIDTable(curHypFilePrefix+".hg.rules");
        	
        	//try to abbrevate the featuers if possible
        	addAbbreviatedNames(ruleStringToIDTable);
        	
        	
        	//micro rule features
        	if(MRConfig.useSparseFeature && MRConfig.useMicroTMFeat){	        	
	        	this.microRuleFeatureTemplate.setupTbl(ruleStringToIDTable, featureStringToIntegerMap.keySet());
        	}

        	//=====compute onebest BLEU
        	computeOneBestBLEU(curHypFilePrefix);
        	
        	//==== run DA annealer to obtain optimal weight vector using the hypergraphs as training data 	
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
        	}
        	
        	
        	//=====re-compute onebest BLEU
        	if(MRConfig.normalizeByFirstFeature)
            	normalizeWeightsByFirstFeature(lastWeightVector, 0);
        	
        	
        	computeOneBestBLEU(curHypFilePrefix);
        	
        	//@todo: check convergency
        	
        	//@todo: delete files
        	if(false){
	        	FileUtility.deleteFile(this.curHypFilePrefix+".hg.items");
	        	FileUtility.deleteFile(this.curHypFilePrefix+".hg.rules");
        	}
        	
        	if(MRConfig.lossAugmentedPrune){
    			this.curLossScale -= MRConfig.lossDecreaseConstant;
    			if(this.curLossScale<=0)
    				this.curLossScale = 0;
    		}
        }
        
        //final output
        if(MRConfig.normalizeByFirstFeature)
        	normalizeWeightsByFirstFeature(lastWeightVector, 0);        	     	
    	saveLastModel(configFile, configFile + ".final", MRConfig.featureFile, MRConfig.featureFile + ".final");
        //writeConfigFile(lastWeightVector, configFile, configFile+".final");
    	
        //System.out.println("#### Final weights are: ");
        //annealer.getLBFGSRunner().printStatistics(-1, -1, null, lastWeightVector);
	}
	
	

	public void decodingTestSet(double[] weights, String hypFilePrefix){
		/**three scenarios:
		 * (1) individual baseline features
		 * (2) baselineCombo + sparse feature
		 * (3) individual baseline features + sparse features
		*/
		
		if(MRConfig.useSparseFeature)
			joshuaDecoder.changeFeatureWeightVector( getIndividualBaselineWeights(), this.curFeatureFile );
		else
			joshuaDecoder.changeFeatureWeightVector( getIndividualBaselineWeights(), null);
		
    	//call Joshua decoder to produce an hypergraph using the new weight vector
    	joshuaDecoder.decodeTestSet(sourceTrainingFile, hypFilePrefix);
    	 		
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
		HashMap<String, Double> modelTbl = obtainModelTable(this.featureStringToIntegerMap, this.lastWeightVector);
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
	
	
	public void saveLastModel(String configTemplate, String configOutput, String sparseFeaturesTemplate, String sparseFeaturesOutput){
		if(MRConfig.useSparseFeature){
			JoshuaDecoder.writeConfigFile( getIndividualBaselineWeights(), configTemplate, configOutput, sparseFeaturesOutput);
			saveSparseFeatureFile(sparseFeaturesTemplate, sparseFeaturesOutput);			
		}else{
			JoshuaDecoder.writeConfigFile( getIndividualBaselineWeights(), configTemplate, configOutput, null);
		}
	}
	
	private void initialize(){
		//===== read configurations
		MRConfig.readConfigFile(this.configFile);
		
		//===== initialize googleCorpusBLEU
		if(MRConfig.useGoogleLinearCorpusGain==true){
			//do nothing		
		}else{
			logger.severe("On hypergraph, we must use the linear corpus gain.");
			System.exit(1);
		}
		
		//===== initialize the featureTemplates
		setupFeatureTemplates();
		
		//====== initialize featureStringToIntegerMap and weights
		initFeatureMapAndWeights(MRConfig.featureFile);	
	}
	
	
	//TODO: should merge with setupFeatureTemplates in HGMinRiskDAMert
	private void setupFeatureTemplates(){
		
		this.featTemplates = new ArrayList<FeatureTemplate>();
		
		if(MRConfig.useBaseline){
			FeatureTemplate ft = new BaselineFT(MRConfig.baselineFeatureName, true);
			featTemplates.add(ft);
		}
		
		if(MRConfig.useIndividualBaselines){
			for(int id : MRConfig.baselineFeatIDsToTune){				
				String featName = MRConfig.individualBSFeatNamePrefix +id;				
				FeatureTemplate ft = new IndividualBaselineFT(featName, id, true);
				featTemplates.add(ft);	
			}
		}
		
		if(MRConfig.useSparseFeature){
			
			if(MRConfig.useMicroTMFeat){	
				//FeatureTemplate ft = new TMFT(symbolTbl, useIntegerString, MRConfig.useRuleIDName);
				this.microRuleFeatureTemplate = new MicroRuleFT(MRConfig.useRuleIDName, MRConfig.startTargetNgramOrder, MRConfig.endTargetNgramOrder, MRConfig.wordMapFile);
				featTemplates.add(microRuleFeatureTemplate);
			}
			
			if(MRConfig.useTMFeat){
				FeatureTemplate ft = new TMFT(symbolTbl, useIntegerString, MRConfig.useRuleIDName);				
				featTemplates.add(ft);
			}
			
			if(MRConfig.useTMTargetFeat){			
				FeatureTemplate ft = new TargetTMFT(symbolTbl, useIntegerString);
				featTemplates.add(ft);		
			}	
				
			if(MRConfig.useLMFeat){
				FeatureTemplate ft = new NgramFT(symbolTbl, useIntegerString, MRConfig.ngramStateID, 
												MRConfig.baselineLMOrder, MRConfig.startNgramOrder, MRConfig.endNgramOrder);
				featTemplates.add(ft);
			}
		}	
		
		System.out.println("feature template are " + featTemplates.toString());

	}

	//read feature map into featureStringToIntegerMap
	//TODO we assume the featureId is the line ID (starting from zero)
	private void initFeatureMapAndWeights(String featureFile){
		
		featureStringToIntegerMap = new HashMap<String, Integer>();
		List<Double> temInitWeights = new ArrayList<Double>();
		int featID = 0;
		

		//==== baseline feature
		if(MRConfig.useBaseline){
			featureStringToIntegerMap.put(MRConfig.baselineFeatureName, featID++);
			temInitWeights.add(MRConfig.baselineFeatureWeight);
		}
		
		//==== individual bs feature
		if(MRConfig.useIndividualBaselines){
			List<Double> weights = readBaselineFeatureWeights(this.configFile);
			for(int id : MRConfig.baselineFeatIDsToTune){				
				String featName = MRConfig.individualBSFeatNamePrefix + id;	
				featureStringToIntegerMap.put(featName,  featID++);
				double  weight = weights.get(id);
				temInitWeights.add(weight);
			}
		}

		//==== features in file
		if(MRConfig.useSparseFeature){
			BufferedReader reader = FileUtilityOld.getReadFileStream(featureFile ,"UTF-8");	
			String line;
			while((line=FileUtilityOld.readLineLzf(reader))!=null){
				String[] fds = line.split("\\s+\\|{3}\\s+");// feature_key ||| feature vale; the feature_key itself may contain "|||"
				StringBuffer featKey = new StringBuffer();
				for(int i=0; i<fds.length-1; i++){
					featKey.append(fds[i]);
					if(i<fds.length-2) 
						featKey.append(" ||| ");
				}
				double initWeight = new Double(fds[fds.length-1]);//initial weight
				temInitWeights.add(initWeight);
				featureStringToIntegerMap.put(featKey.toString(), featID++);
			}
			FileUtilityOld.closeReadFile(reader);
		}
		
		//==== initialize lastWeightVector
		numPara = temInitWeights.size();
		lastWeightVector = new double[numPara];
		for(int i=0; i<numPara; i++)
			lastWeightVector[i] = temInitWeights.get(i);
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
	
	private void saveSparseFeatureFile(String fileTemplate, String outputFile){
	
		BufferedReader template = FileUtilityOld.getReadFileStream(fileTemplate,"UTF-8");	
		BufferedWriter writer = FileUtilityOld.getWriteFileStream(outputFile);
		String line;
		
		while((line=FileUtilityOld.readLineLzf(template))!=null){
			//== construct feature name
			String[] fds = line.split("\\s+\\|{3}\\s+");// feature_key ||| feature vale; the feature_key itself may contain "|||"
			StringBuffer featKey = new StringBuffer();
			for(int i=0; i<fds.length-1; i++){
				featKey.append(fds[i]);
				if(i<fds.length-2) 
					featKey.append(" ||| ");
			}
			
			//== write the learnt weight
			//double oldWeight = new Double(fds[fds.length-1]);//initial weight
			int featID = featureStringToIntegerMap.get(featKey.toString());
			double newWeight =  lastWeightVector[featID];//last model
			//System.out.println(featKey +"; old=" + oldWeight + "; new=" + newWeight);
			FileUtilityOld.writeLzf(writer, featKey.toString() + " ||| " + newWeight +"\n");
			
			featID++;
		}
		FileUtilityOld.closeReadFile(template);
		FileUtilityOld.closeWriteFile(writer);
	}
	
	
	private HashMap<String,Double> obtainModelTable(HashMap<String, Integer> featureStringToIntegerMap, double[] weightVector){
		HashMap<String,Double> modelTbl = new HashMap<String,Double>();
		for(Map.Entry<String,Integer> entry : featureStringToIntegerMap.entrySet()){
			int featID = entry.getValue();
			double weight =  lastWeightVector[featID];//last model
			modelTbl.put(entry.getKey(), weight);
		}
		return modelTbl;
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
	
	
	static public void addAllWordsIntoSymbolTbl(String file, SymbolTable symbolTbl){
		BufferedReader reader = FileUtilityOld.getReadFileStream(file,"UTF-8");	
		String line;
		while((line=FileUtilityOld.readLineLzf(reader))!=null){
			symbolTbl.addTerminals(line);
		}
		FileUtilityOld.closeReadFile(reader);
	}
	
	
	
	
	
	public static void main(String[] args) {
		/*String f_joshua_config="C:/data_disk/java_work_space/discriminative_at_clsp/edu/jhu/joshua/discriminative_training/lbfgs/example.config.javalm";
		String f_dev_src="C:/data_disk/java_work_space/sf_trunk/example/example.test.in";
		String f_nbest_prefix="C:/data_disk/java_work_space/discriminative_at_clsp/edu/jhu/joshua/discriminative_training/lbfgs/example.nbest.javalm.out";
		String f_dev_ref="C:/data_disk/java_work_space/sf_trunk/example/example.test.ref.0";
		*/
		if(args.length<3){
			System.out.println("Wrong number of parameters!");
			System.exit(1);
		}
		
	
		
		//long start_time = System.currentTimeMillis();
		String joshuaConfigFile=args[0].trim();
		String sourceTrainingFile=args[1].trim();
		String hypFilePrefix=args[2].trim();
		
		String[] devRefs = null;
		if(args.length>3){
			devRefs = new String[args.length-3];
			for(int i=3; i< args.length; i++){
				devRefs[i-3]= args[i].trim();
				System.out.println("Use ref file " + devRefs[i-3]);
			}
		}
		
		SymbolTable symbolTbl = new BuildinSymbol(null);
		
		int numSentInDevSet = FileUtilityOld.numberLinesInFile(sourceTrainingFile);
		
		
		HGMinRiskDAMert trainer =  new HGMinRiskDAMert(joshuaConfigFile,numSentInDevSet, devRefs, hypFilePrefix, symbolTbl, sourceTrainingFile);

		trainer.mainLoop();
	}
	
}
