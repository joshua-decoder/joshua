package joshua.discriminative.training.risk_annealer;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import joshua.discriminative.FileUtilityOld;

public abstract class AbstractMinRiskMERT {
		
	protected String configFile;	
	protected double[] lastWeightVector;
	
	//== annealer specific
	protected DeterministicAnnealer annealer;	
	protected String[] referenceFiles;
	protected int numPara;
	protected int numTrainingSentence;
	
	//protected double[] linearCorpusGainThetas;
	
	
//=======================================
	
	
	private static final Logger logger =
		Logger.getLogger(AbstractMinRiskMERT.class.getSimpleName());
	
	
	public AbstractMinRiskMERT(String configFile, int numTrainingSentence, String[] referenceFiles) {	
		
		this.configFile = configFile;					
		this.referenceFiles = referenceFiles;		
		this.numTrainingSentence = numTrainingSentence;
	
	}

	//this function should have an option for not annealing
	public abstract void mainLoop();	
	public abstract void decodingTestSet(double[] weights, String nbestFile);
	

	
	
	protected List<Double> readBaselineFeatureWeights(String configFile){
		
		//== get the weights
		List<Double> weights = new ArrayList<Double>();
		BufferedReader configReader = FileUtilityOld.getReadFileStream(configFile);
		String line;
		while ((line = FileUtilityOld.readLineLzf(configReader)) != null) {
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}else if (line.indexOf("=") != -1) { // parameters
				continue;				
			}else{//models
				String[] fds = line.split("\\s+");
				double weight = new Double(fds[fds.length-1].trim());
				weights.add(weight);
			}
		}
		FileUtilityOld.closeReadFile(configReader);
		return weights;
	}
	
	
	protected Integer inferOracleFeatureID(String configFile){
		
		//== get the weights
		
		BufferedReader configReader = FileUtilityOld.getReadFileStream(configFile);
		String line;
		int id = 0;
		Integer oracleFeatureID = null;
		while ((line = FileUtilityOld.readLineLzf(configReader)) != null) {
			line = line.trim();
			if (line.matches("^\\s*\\#.*$") || line.matches("^\\s*$")) {
				continue;
			}else if (line.indexOf("=") != -1) { // parameters
				continue;				
			}else{//models
				String[] fds = line.split("\\s+");
				if("oracle".equals(fds[0])){
					if(oracleFeatureID==null)
						oracleFeatureID = id;
					else{
						logger.severe("more than one oralce model, must be wrong");
						System.exit(1);
					}
				}
				id++;
			}
		}
		FileUtilityOld.closeReadFile(configReader);
	
		return oracleFeatureID;
	}
	

	protected void normalizeWeightsByFirstFeature(double[] weightVector, int featID){
		double weight = weightVector[featID];
		
		if(weight<=0){
			logger.warning("first weight is negative"); 
			//System.exit(0);
		}
		
		for(int i=0; i<weightVector.length; i++)
			weightVector[i] /=  Math.abs( weight );
		
	}
	

}
