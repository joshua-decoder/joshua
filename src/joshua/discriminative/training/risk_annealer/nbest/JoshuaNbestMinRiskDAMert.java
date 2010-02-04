package joshua.discriminative.training.risk_annealer.nbest;

import joshua.decoder.JoshuaDecoder;
import joshua.discriminative.FileUtilityOld;

/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400  $
*/

public class JoshuaNbestMinRiskDAMert extends NbestMinRiskDAMert{
	
	JoshuaDecoder joshuaDecoder;
	String sourceTrainingFile;
	
	public JoshuaNbestMinRiskDAMert(boolean useShortestRef, String joshuaConfig, String devSrcFile, int numSentInDevSet, String[] refFiles, String nbestPrefix) {
		super(useShortestRef, joshuaConfig, numSentInDevSet, refFiles, nbestPrefix);
		
		//initiallize
		//?????????????
		sourceTrainingFile = devSrcFile;
		joshuaDecoder = JoshuaDecoder.getUninitalizedDecoder();
		joshuaDecoder.initialize(configFile);
	}
	

	public void decodingTestSet(double[] weights, String nbestFile) {
    	joshuaDecoder.changeBaselineFeatureWeights(weights);
    	joshuaDecoder.decodeTestSet(sourceTrainingFile, nbestFile); //call Joshua decoder to produce an nbest using the new weight vector		
	}


	public void writeConfigFile(double[] weights, String configTemplate, String outConfig){
		JoshuaDecoder.writeConfigFile(weights, configTemplate, outConfig, null);
	}

	
	
	public static void main(String[] args) {
		/*String f_joshua_config="C:/data_disk/java_work_space/discriminative_at_clsp/edu/jhu/joshua/discriminative_training/lbfgs/example.config.javalm";
		String f_dev_src="C:/data_disk/java_work_space/sf_trunk/example/example.test.in";
		String f_nbest_prefix="C:/data_disk/java_work_space/discriminative_at_clsp/edu/jhu/joshua/discriminative_training/lbfgs/example.nbest.javalm.out";
		String f_dev_ref="C:/data_disk/java_work_space/sf_trunk/example/example.test.ref.0";
		*/
		if(args.length<5){
			System.out.println("Wrong number of parameters, it must have at least four parameters: java NbestMinRiskAnnealer use_shortest_ref f_joshua_config gain_factor f_dev_src f_nbest_prefix f_dev_ref1 f_dev_ref2....");
			System.exit(1);
		}
		
		//long start_time = System.currentTimeMillis();
		boolean useShortestRef = new Boolean(args[0].trim());
		String joshuaConfig=args[1].trim();
		String devSrcFile=args[2].trim();
		String nbestPrefix=args[3].trim();
		String[] refFiles = new String[args.length-4];
		for(int i=4; i< args.length; i++)
			refFiles[i-4]= args[i].trim();
		
		
		int numSentInDevSet = FileUtilityOld.numberLinesInFile(devSrcFile);
		NbestMinRiskDAMert trainer= new JoshuaNbestMinRiskDAMert(useShortestRef, joshuaConfig, devSrcFile, numSentInDevSet, refFiles, nbestPrefix);
		trainer.mainLoop();
	}

}
