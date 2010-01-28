package joshua.discriminative.training.risk_annealer.nbest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import joshua.decoder.BLEU;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.risk_annealer.AbstractMinRiskMERT;
import joshua.discriminative.training.risk_annealer.DeterministicAnnealer;
import joshua.discriminative.training.risk_annealer.GradientComputer;
import joshua.discriminative.training.risk_annealer.hypergraph.MRConfig;


/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400  $
*/
public abstract class NbestMinRiskDAMert extends AbstractMinRiskMERT {		
	
	String nbestPrefix;
	boolean useShortestRef;
	
	private static Logger logger = Logger.getLogger(NbestMinRiskDAMert.class.getSimpleName());
	
	//TODO
	boolean useL2Regula = false;
	double varianceForL2 = 1;
	
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
        	normalizeWeightsByFirstFeature(lastWeightVector);
        
        	//############decoding        
        	writeConfigFile(lastWeightVector, configFile, configFile+"." + iter);
        	String f_nbest = nbestPrefix +"." + iter;
        	decodingTestSet(lastWeightVector, f_nbest); //call decoder to produce an nbest using the new weight vector
           
        	//##############merge nbest and check convergency
        	String f_nbest_merged_old = nbestPrefix +".merged." + (iter-1);
        	String f_nbest_merged_new = nbestPrefix +".merged." + (iter);
        	if(iter ==1){
        		copyNbest(f_nbest, f_nbest_merged_new);
        	}else{
	            boolean have_new_hyp = mergeNbest(f_nbest_merged_old, f_nbest, f_nbest_merged_new);
	            if(have_new_hyp==false) {System.out.println("No new hypotheses generated at iteration " + iter); break;}
            }
        	
        	//String f_nbest_merged_new = "C:/Users/zli/Documents/minriskannealer.nbest.merged.17";//????????????
        	//String f_nbest_merged_new = "C:/Users/zli/Documents/minriskannealer.nbest.merged.1";//????????????
        	GradientComputer gradientComputer = new NbestRiskGradientComputer(f_nbest_merged_new, referenceFiles, useShortestRef, numTrainingSentence, numPara, MRConfig.gainFactor, 1.0, 0.0, true, linearCorpusGainThetas);
        	annealer = new DeterministicAnnealer( numPara,  lastWeightVector, MRConfig.isMinimizer, gradientComputer, this.useL2Regula, this.varianceForL2);
        	
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
        normalizeWeightsByFirstFeature(lastWeightVector);
        writeConfigFile(lastWeightVector, configFile, configFile+".final");
        System.out.println("#### Final weights are: ");
        annealer.getLBFGSRunner().printStatistics(-1, -1, null, lastWeightVector);
	}


	//return false: if the nbest does not add any new hyp
	//TODO: decide converged if the number of new hyp generate is very small
	//TODO: terminate decoding when the weights does not change much; this one makes more sense, as if the weights do not change much; then new hypotheses will be rare
	private boolean mergeNbest(String f_nbest_merged_old, String f_nbest_new, String f_nbest_merged_new){
		boolean have_new_hyp =false;
		BufferedReader t_reader_merged_old = FileUtilityOld.getReadFileStream(f_nbest_merged_old);
		BufferedReader t_reader_nbest_new = FileUtilityOld.getReadFileStream(f_nbest_new);
		BufferedWriter t_writer_new_merged_nbest =	FileUtilityOld.getWriteFileStream(f_nbest_merged_new);		
		
		int old_sent_id=-1;
		String line;
		String previous_line_in_new_nbest = FileUtilityOld.readLineLzf(t_reader_nbest_new);;
		HashMap<String, String> nbests = new HashMap<String, String>();//key: hyp itself, value: remaining fds exlcuding sent_id
		while((line=FileUtilityOld.readLineLzf(t_reader_merged_old))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
			int new_sent_id = new Integer(fds[0]);
			if(old_sent_id!=-1 && old_sent_id!=new_sent_id){
				boolean[] t_have_new_hyp = new boolean[1];
				previous_line_in_new_nbest = processNbest(t_reader_nbest_new, t_writer_new_merged_nbest, old_sent_id, nbests, previous_line_in_new_nbest, t_have_new_hyp);
				if(t_have_new_hyp[0]==true) have_new_hyp = true;
			}
			old_sent_id = new_sent_id;
			nbests.put(fds[1], fds[2]);//last field is not needed
		}
		//last nbest
		boolean[] t_have_new_hyp = new boolean[1];
		previous_line_in_new_nbest= processNbest(t_reader_nbest_new, t_writer_new_merged_nbest, old_sent_id, nbests, previous_line_in_new_nbest, t_have_new_hyp);
		if(previous_line_in_new_nbest!=null){System.out.println("last line is not null, must be wrong"); System.exit(0);}
		if(t_have_new_hyp[0]==true) have_new_hyp = true;
		
		FileUtilityOld.closeReadFile(t_reader_merged_old);
		FileUtilityOld.closeReadFile(t_reader_nbest_new);
		FileUtilityOld.closeWriteFile(t_writer_new_merged_nbest);
		return have_new_hyp;
	}
	
	private String processNbest(BufferedReader t_reader_nbest_new, BufferedWriter t_writer_new_merged_nbest, int old_sent_id, HashMap<String, String> nbests,
			String previous_line, boolean[] have_new_hyp){
		have_new_hyp[0] = false;
		String previous_line_in_new_nbest = previous_line;
//		#### read new nbest and merge into nbests
		while(true){
			String[] t_fds = previous_line_in_new_nbest.split("\\s+\\|{3}\\s+");
			int t_new_id = new Integer(t_fds[0]); 
			if( t_new_id == old_sent_id){
				if(nbests.containsKey(t_fds[1])==false){//new hyp
					have_new_hyp[0] = true;
					nbests.put(t_fds[1], t_fds[2]);//merge into nbests
				}
			}else{
				break;
			}
			previous_line_in_new_nbest = FileUtilityOld.readLineLzf(t_reader_nbest_new);
			if(previous_line_in_new_nbest==null) break;
		}
		//#### print the nbest: order is not important; and the last field is ignored
		for (Iterator iter = nbests.entrySet().iterator(); iter.hasNext();){ 
		    Map.Entry entry = (Map.Entry)iter.next();
		    FileUtilityOld.writeLzf(t_writer_new_merged_nbest, old_sent_id + " ||| " + entry.getKey() + " ||| " + entry.getValue() + "\n");
		}				
		nbests.clear();
		return previous_line_in_new_nbest;
	}
	
	//return false: if the nbest does not add any new hyp
	private void copyNbest(String f_nbest_new, String f_nbest_merged_new){
		BufferedReader t_reader_nbest_new = FileUtilityOld.getReadFileStream(f_nbest_new);
		BufferedWriter t_writer_new_merged_nbest =	FileUtilityOld.getWriteFileStream(f_nbest_merged_new);		
		
		String line;
		while((line=FileUtilityOld.readLineLzf(t_reader_nbest_new))!=null){
			String[] fds = line.split("\\s+\\|{3}\\s+");
		    FileUtilityOld.writeLzf(t_writer_new_merged_nbest, fds[0] + " ||| " + fds[1] + " ||| " + fds[2] + "\n");		
		}
		FileUtilityOld.closeReadFile(t_reader_nbest_new);
		FileUtilityOld.closeWriteFile(t_writer_new_merged_nbest);
	}
	
	
//	set lastWeightVector and google linear corpus
	private void initialize() {
		//===== read configurations
		MRConfig.readConfigFile(this.configFile);
		
		
		/**initialize the linear corpus gain*/
		if(MRConfig.useGoogleLinearCorpusGain==true){
			this.linearCorpusGainThetas = 
				BLEU.computeLinearCorpusThetas(MRConfig.numUnigramTokens, MRConfig.unigramPrecision, MRConfig.precisionDecayRatio);
		}
		
		
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
