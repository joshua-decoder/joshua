package joshua.discriminative.training.structured_lm_em;

import joshua.decoder.ff.tm.Grammar;
import joshua.discriminative.monolingual_parser.MonolingualJoshuaDecoder;
import joshua.discriminative.training.learning_algorithm.DefaultEM;

/** 
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2008-10-20 00:12:30 -0400   $
*/

public class StructuredLMEM extends DefaultEM {
	MonolingualJoshuaDecoder p_decoder;
	String trainingFile;
	String outGrammarPrefix;
	String decoderConfigFile;
	Grammar trainingGrammar;
	
	
	public StructuredLMEM(int maxNumIter_, double relativeLikelihoodThreshold_, int maxConvergeNum_, 
			String decoderConfigFile_, String trainingFile_, String outGrammarPrefix_) {
		super( maxNumIter_, relativeLikelihoodThreshold_, maxConvergeNum_);
		
		//initiallize
		trainingFile = trainingFile_;
		decoderConfigFile = decoderConfigFile_;
		outGrammarPrefix = outGrammarPrefix_;
		
		p_decoder = new MonolingualJoshuaDecoder();
		p_decoder.initialize(decoderConfigFile);
	}
	
	@Override
	public void runOneEMStep(int iterNum) {
		p_decoder.decodingTestSet(trainingFile, outGrammarPrefix+"."+iterNum); //call decoder to run EM		
	}

	@Override
	public double getLastLikelihood() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEMConverged() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void printStatistics(int iter_num) {
		// TODO Auto-generated method stub		
	}

	
	//=============================== main method ===============================
	public static void main(String[] args) {
		/*String f_joshua_config="C:/data_disk/java_work_space/discriminative_at_clsp/edu/jhu/joshua/discriminative_training/lbfgs/example.config.javalm";
		String f_dev_src="C:/data_disk/java_work_space/sf_trunk/example/example.test.in";
		String f_nbest_prefix="C:/data_disk/java_work_space/discriminative_at_clsp/edu/jhu/joshua/discriminative_training/lbfgs/example.nbest.javalm.out";
		String f_dev_ref="C:/data_disk/java_work_space/sf_trunk/example/example.test.ref.0";
		*/
		if(args.length<4){
			System.out.println("Wrong number of parameters, it must have at least two parameters: java StructuredLMEM f_joshua_config f_train");
			System.exit(1);
		}
		
		
		String f_joshua_config=args[0].trim();
		String f_dev_src=args[1].trim();
		String outGrammarPrefix=args[2].trim();		
		int maxNumIter = new Integer(args[3].trim());
		
		double relativeLikelihoodThreshold = 1e-5;
		int maxConvergeNum = 10000;
		DefaultEM p_trainer= new StructuredLMEM(maxNumIter, relativeLikelihoodThreshold, maxConvergeNum, f_joshua_config, f_dev_src, outGrammarPrefix);
		p_trainer.runEM();
	}	
}
