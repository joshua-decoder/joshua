package joshua.discriminative.training.risk_annealer.nbest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.NbestMerger;
import joshua.discriminative.training.risk_annealer.AbstractMinRiskMERT;
import joshua.discriminative.training.risk_annealer.DeterministicAnnealer;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.MRConfig;
import joshua.util.FileUtility;


/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400  $
*/
public abstract class NbestMinRiskDAMert extends AbstractMinRiskMERT {		
	int totalNumHyp = 0;
	
	String nbestPrefix;
	boolean useShortestRef;
	
	private static Logger logger = Logger.getLogger(NbestMinRiskDAMert.class.getSimpleName());
	
	
	public NbestMinRiskDAMert(boolean useShortestRef, String decoderConfigFile, int numSentInTrainSet, String[] refFiles, String nbestPrefix) {
		super(decoderConfigFile, numSentInTrainSet, refFiles);	
		this.nbestPrefix = nbestPrefix;
		this.useShortestRef = useShortestRef;
		
		initialize();
	}

	public abstract void writeConfigFile(double[] weights, String configFileTemplate, String configOutFile);
	
	public void mainLoop(){
        for(int iter=1; iter<=MRConfig.maxNumIter; iter++){
        	//#re-normalize weights
        	normalizeWeightsByFirstFeature(lastWeightVector,0);
        
        	//############decoding        
        	writeConfigFile(lastWeightVector, configFile, configFile+"." + iter);
        	String curNbestFile = nbestPrefix +"." + iter;
        	decodingTestSet(lastWeightVector, curNbestFile); //call decoder to produce an nbest using the new weight vector
           
        	//##############merge nbest and check convergency
        	String oldNbestMergedFile = nbestPrefix +".merged." + (iter-1);
        	String newNbestMergedFile = nbestPrefix +".merged." + (iter);
        	if(iter ==1){
        		copyNbest(curNbestFile, newNbestMergedFile);
        	}else{
        		boolean haveNewHyp = true;
        		if(true){
        			int newTotalNumHyp = NbestMerger.mergeNbest(oldNbestMergedFile, curNbestFile, newNbestMergedFile);
        			if(newTotalNumHyp!=totalNumHyp)
        				haveNewHyp = true;
        			totalNumHyp = newTotalNumHyp;
        		}else{
        			haveNewHyp = mergeNbest(oldNbestMergedFile, curNbestFile, newNbestMergedFile);
        		}
	            
	            if(haveNewHyp==false) {
	            	System.out.println("No new hypotheses generated at iteration " + iter); 
	            	break;
	            }
            }
        	
        	//String f_nbest_merged_new = "C:/Users/zli/Documents/minriskannealer.nbest.merged.17";//????????????
        	//String f_nbest_merged_new = "C:/Users/zli/Documents/minriskannealer.nbest.merged.1";//????????????
        	GradientComputer gradientComputer = new NbestRiskGradientComputer(newNbestMergedFile, referenceFiles, useShortestRef, numTrainingSentence, 
        			numPara, MRConfig.gainFactor, 1.0, 0.0, true, MRConfig.linearCorpusGainThetas);
        	annealer = new DeterministicAnnealer( numPara,  lastWeightVector, MRConfig.isMinimizer, gradientComputer, 
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
        	//last_weight_vector will be used intial weights in the next iteration
        }
        
        //final output
        normalizeWeightsByFirstFeature(lastWeightVector,0);
        writeConfigFile(lastWeightVector, configFile, configFile+".final");
        System.out.println("#### Final weights are: ");
        annealer.getLBFGSRunner().printStatistics(-1, -1, null, lastWeightVector);
	}


	//return false: if the nbest does not add any new hyp
	//TODO: decide converged if the number of new hyp generate is very small
	//TODO: terminate decoding when the weights does not change much; this one makes more sense, as if the weights do not change much; then new hypotheses will be rare
	public static boolean mergeNbest(String oldMergedNbestFile, String newNbestFile, String newMergedNbestFile){
		boolean haveNewHyp =false;
		BufferedReader oldMergedNbestReader = FileUtilityOld.getReadFileStream(oldMergedNbestFile);
		BufferedReader newNbestReader = FileUtilityOld.getReadFileStream(newNbestFile);
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
	
	//return false: if the nbest does not add any new hyp
	public static void copyNbest(String newNbestFile, String newMergedNbestFile){
		/*
		BufferedReader newNbestReader = FileUtilityOld.getReadFileStream(newNbestFile);
		BufferedWriter newMergedNbestReader =	FileUtilityOld.getWriteFileStream(newMergedNbestFile);		
		
		String line;
		while((line=FileUtilityOld.readLineLzf(newNbestReader))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
		    FileUtilityOld.writeLzf(newMergedNbestReader, fds[0] + " ||| " + fds[1] + " ||| " + fds[2] + "\n");		
		}
		FileUtilityOld.closeReadFile(newNbestReader);
		FileUtilityOld.closeWriteFile(newMergedNbestReader);*/
		try {
			FileUtility.copyFile(newNbestFile, newMergedNbestFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
//	set lastWeightVector and google linear corpus
	private void initialize() {
		//===== read configurations
		MRConfig.readConfigFile(this.configFile);
		
		
		logger.info("intilize features and weights");
		//== get the weights
		List<Double> weights = readBaselineFeatureWeights(this.configFile);
		
		/**initialize the weights*/
		int numPara=weights.size();
		this.lastWeightVector = new double[numPara];
		for(int i=0; i<lastWeightVector.length; i++){
			lastWeightVector[i] = weights.get(i);
			logger.info("weight: " + lastWeightVector[i]);
		}
		this.numPara = this.lastWeightVector.length;
	}
	
}
