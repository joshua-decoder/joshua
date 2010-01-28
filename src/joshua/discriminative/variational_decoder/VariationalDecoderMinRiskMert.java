package joshua.discriminative.variational_decoder;


import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.risk_annealer.nbest.NbestMinRiskDAMert;



public class VariationalDecoderMinRiskMert  extends NbestMinRiskDAMert {

	VariationalDecoder p_decoder;
	String f_dev_items;
	String f_dev_rules;
	int num_dev_sents;
	
	
	public static void main(String[] args) {
		if(args.length<5){
			System.out.println("Wrong number of parameters, it must have at least four parameters: java NbestMinRiskAnnealer use_shortest_ref f_config gain_factor f_dev_src f_nbest_prefix f_dev_ref1 f_dev_ref2....");
			System.exit(1);
		}
		
		//long start_time = System.currentTimeMillis();
		boolean use_shortest_ref = new Boolean(args[0].trim());
		String f_config=args[1].trim();
		String f_dev_hg_prefix=args[2].trim();
		String f_nbest_prefix=args[3].trim();
		String[] f_dev_refs = new String[args.length-4];
		for(int i=4; i< args.length; i++)
			f_dev_refs[i-4]= args[i].trim();
		
		
		int num_sent_in_dev_set = FileUtilityOld.numberLinesInFile(f_dev_refs[0]);
		String f_dev_items = f_dev_hg_prefix +".items";
		String f_dev_rules = f_dev_hg_prefix +".rules";
		
		
		NbestMinRiskDAMert p_trainer= new VariationalDecoderMinRiskMert(use_shortest_ref, f_config, f_dev_items, f_dev_rules, num_sent_in_dev_set, f_dev_refs, f_nbest_prefix);
		p_trainer.mainLoop();
	}

	
	public VariationalDecoderMinRiskMert(boolean use_shortest_ref_, String f_config_, String f_dev_items_, String f_dev_rules_, int number_sent_, String[] f_dev_refs_, String f_nbest_prefix_){
		super(use_shortest_ref_, f_config_, number_sent_, f_dev_refs_, f_nbest_prefix_);
		
		//initiallize
		//?????????????
		num_dev_sents = number_sent_;
		f_dev_items = f_dev_items_;
		f_dev_rules = f_dev_rules_;
		p_decoder = new VariationalDecoder();
		p_decoder.initializeDecoder(configFile);
	}
	
	public void decodingTestSet(double[] weights, String f_nbest) {
    	p_decoder.changeFeatureWeightVector(weights);
    	p_decoder.decodingTestSet(f_dev_items, f_dev_rules, num_dev_sents, f_nbest); //call decoder to produce an nbest using the new weight vector
	}

	public void writeConfigFile(double[] weights, String f_config_template, String f_config_out) {
		p_decoder.writeConfigFile(weights, f_config_template, f_config_out);
	}
	
}
