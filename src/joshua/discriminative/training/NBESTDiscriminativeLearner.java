package joshua.discriminative.training;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joshua.discriminative.DiscriminativeSupport;
import joshua.discriminative.FileUtilityOld;
import joshua.discriminative.training.learning_algorithm.DefaultCRF;
import joshua.discriminative.training.learning_algorithm.DefaultPerceptron;
import joshua.discriminative.training.learning_algorithm.GradientBasedOptimizer;




public class NBESTDiscriminativeLearner  {
	
	GradientBasedOptimizer optimizer =  null;
	static boolean usingCRF = true;//default is crf

	
	//##feature realted
	HashMap<String,Double> empiricalFeatsTbl = new HashMap<String,Double>();//experical feature counts
	HashMap<String,Double> modelFeatsTbl = new HashMap<String,Double>();	//feature counts assigned by model
	HashSet<String> restrictedFeatureSet = null;// only consider feature in this set, if null, then ignore this
	
	//## batch update related
	int numProcessedExamples=0;
				
	//## reranking: baseline
	static String baselineFeatName ="baseline_lzf";
	static boolean fixBaseline= true;//TODO: the current version assume we always fix the baseline during training
	static double baselineScale = 1.0; //the scale for baseline relative to the corrective score
	
	
	static int startNgramOrder =1;
	static int endNgramOrder =2;
	static int hypStrID = 1;//hyp format: sent_id ||| hyp_str ||| features ||| final score
	
	public NBESTDiscriminativeLearner(GradientBasedOptimizer optimizer, HashSet<String> restrictedFeatureSet){
		this.optimizer = optimizer;		
		this.restrictedFeatureSet = restrictedFeatureSet;
	}
	

//	all hyp are represented as a hyper-graph
	public void processOneSent( ArrayList nbest, String hyp_oracle, String ref_sent){		
		//####feature extraction using the current model, get modelFeatsTbl
		if(usingCRF){
			getFeatureExpection(modelFeatsTbl, optimizer.getSumModel(),  restrictedFeatureSet,  nbest);
		}else{//perceptron
			String  rerankedOnebest = rerankNbest(optimizer.getSumModel(), restrictedFeatureSet, nbest);
			featureExtraction(rerankedOnebest, modelFeatsTbl, restrictedFeatureSet, hypStrID, false);
		}
		
		featureExtraction(hyp_oracle, empiricalFeatsTbl, restrictedFeatureSet, 0, false);		
				
		//####common: update the sum and avg model
		/*System.out.println("g_tbl_feats_model size: " + g_tbl_feats_model.size());
		System.out.println("g_tbl_feats_emperical size: " + g_tbl_feats_empirical.size());
		System.out.println("g_restricted_feature_set size: " + g_restricted_feature_set.size());
		*/
		//####common: update the models
		numProcessedExamples++;
		updateModel(false);
	}
	
	public void updateModel(boolean force_update){
		
		if(force_update || numProcessedExamples>=optimizer.getBatchSize()){
			/*//debug
			System.out.println("baseline feature emprical " + g_tbl_feats_empirical.get(g_baseline_feat_name));
			System.out.println("baseline feature model "    + g_tbl_feats_model.get(g_baseline_feat_name));
			//edn*/
			
			optimizer.updateModel(empiricalFeatsTbl, modelFeatsTbl);
			//System.out.println("baseline feature weight " + p_optimizer.get_sum_model().get(g_baseline_feat_name));
			resetBaselineFeat();
			//System.out.println("baseline feature weight " + p_optimizer.get_sum_model().get(g_baseline_feat_name));
			
			empiricalFeatsTbl.clear();
			modelFeatsTbl.clear();
			numProcessedExamples=0;						
		}	
	}
	
	public void resetBaselineFeat(){
		if(fixBaseline)
			optimizer.setFeatureWeight(baselineFeatName, baselineScale);
		else{
			System.out.println("not implemented"); System.exit(0);
		}	
	}
	
	public String rerankNbest(HashMap<String,Double> corrective_model,  HashSet<String> restrictedFeatSet, ArrayList nbest){
		double best_score = -1000000000;
		String best_hyp=null;
		HashMap<String,Double> tbl_feat_set= new HashMap<String,Double>();
		for(int i=0; i<nbest.size(); i++){
			tbl_feat_set.clear();
			String cur_hyp = (String)nbest.get(i);
			featureExtraction(cur_hyp, tbl_feat_set, restrictedFeatSet, hypStrID, true);
			double cur_score = DiscriminativeSupport.computeLinearCombinationLogP(tbl_feat_set, corrective_model);
			if(i==0 || cur_score > best_score){//maximize
				best_score = cur_score;
				best_hyp = cur_hyp;
			}
		}
		return best_hyp;
	}
	

	//accumulate feature expectations in res_feat_tbl
	public void getFeatureExpection(HashMap<String,Double> res_feat_tbl, HashMap<String,Double> corrective_model,  HashSet<String> restricted_feat_set, ArrayList nbest){
		//### get noralization constant, remember features, remember the combined linear score
		double normalization_constant = Double.NEGATIVE_INFINITY;//log-semiring
		ArrayList<HashMap> l_feats = new ArrayList<HashMap>();
		ArrayList<Double> l_score = new ArrayList<Double>();		
		for(int i=0; i<nbest.size(); i++){
			HashMap<String,Double> tbl_feat_set= new HashMap<String,Double>();
			String cur_hyp = (String)nbest.get(i);
			featureExtraction(cur_hyp, tbl_feat_set, restricted_feat_set, hypStrID, true);
			double curScore = DiscriminativeSupport.computeLinearCombinationLogP(tbl_feat_set, corrective_model);
			normalization_constant = addInLogSemiring(normalization_constant, curScore,0);
			l_feats.add(tbl_feat_set);
			l_score.add(curScore);
		}
		
		////### get expected feature count
		double sum=0;
		for(int i=0; i<nbest.size(); i++){
			double cur_score = l_score.get(i);
			HashMap<String, Double> feats = l_feats.get(i);
			double post_prob = Math.exp(cur_score-normalization_constant);
			sum += post_prob;
			//accumulate feature counts
			for (Map.Entry<String, Double> entry : feats.entrySet() ){ 
			    DiscriminativeSupport.increaseCount(res_feat_tbl, entry.getKey(), entry.getValue()*post_prob);
			}
		}
		System.out.println("Sum is " + sum);
	}
	
	
//	OR: return Math.log(Math.exp(x) + Math.exp(y));
	private double addInLogSemiring(double x, double y, int add_mode){//prevent over-flow 
		if(add_mode==0){//sum
			if(x==Double.NEGATIVE_INFINITY)//if y is also n-infinity, then return n-infinity
				return y;
			if(y==Double.NEGATIVE_INFINITY)
				return x;
			
			if(y<=x)
				return x + Math.log(1+Math.exp(y-x));
			else//x<y
				return y + Math.log(1+Math.exp(x-y));
		}else if (add_mode==1){//viter-min
			return (x<=y)?x:y;
		}else if (add_mode==2){//viter-max
			return (x>=y)?x:y;
		}else{
			System.out.println("invalid add mode"); System.exit(0); return 0;
		}
	}
	
	public static void featureExtraction(String hyp, HashMap<String,Double> feat_tbl, HashSet<String> restricted_feat_set, int hyp_str_id, boolean extract_baseline_feat){
		String[] fds = hyp.split("\\s+\\|{3}\\s+");
		
		//### baseline feature
		if(extract_baseline_feat){
			String score = replaceBadSymbol(fds[fds.length-1]);
			double baseline_score = new Double(score);
			feat_tbl.put(baselineFeatName, baseline_score);
		}
		
		//### ngram feature
		String[] wrds = fds[hyp_str_id].split("\\s+");
		for(int i=0; i<wrds.length; i++)
			for(int j=startNgramOrder-1; j<endNgramOrder  && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					String t_wrd = wrds[k];
					ngram.append(t_wrd);
					if(k<i+j) ngram.append(" ");
				}
				String ngram_str = ngram.toString();
				if(restricted_feat_set==null || restricted_feat_set.contains(ngram_str)){//filter
					DiscriminativeSupport.increaseCount(feat_tbl, ngram_str, 1.0);
				}
		}
	}	
	
	private static String replaceBadSymbol(String in){
		if(in.startsWith("--"))
			return in.substring(1);
		else
			return in;
	}
	
	
	public static void main(String[] args) {		
		if(args.length<5){
			System.out.println("wrong command, correct command should be: java is_using_crf f_l_train_nbest f_l_orc f_data_sel f_model_out_prefix [f_feature_set]");
			System.out.println("num of args is "+ args.length);
			for(int i=0; i <args.length; i++) System.out.println("arg is: " + args[i]);
			System.exit(0);		
		}
		long start_time = System.currentTimeMillis();			
		boolean is_using_crf =  new Boolean(args[0].trim());
		NBESTDiscriminativeLearner.usingCRF=is_using_crf;
		String f_l_train_nbest=args[1].trim();
		String f_l_orc=args[2].trim();
		String f_data_sel=args[3].trim();
		String f_model_out_prefix =args[4].trim();		
		String initModelFile=null;
		if(args.length>5) 
			initModelFile = args[5].trim();
		
		int max_loop = 3;//TODO
		
		List<String> l_file_train_nbest = DiscriminativeSupport.readFileList(f_l_train_nbest);
		List<String> l_file_orc = DiscriminativeSupport.readFileList(f_l_orc);	
		HashMap tbl_sent_selected = DiscriminativeSupport.setupDataSelTbl(f_data_sel);//for data selection
		
		//####### training
		
			
//		###### INIT Model ###################
		NBESTDiscriminativeLearner ndl = null;	
		//TODO optimal parameters
		int trainSize = 610000;
		int batchUpdateSize = 1;
		int convergePass =  1;
		double initGain = 0.1;
		double sigma = 0.5;
		boolean isMinimizeScore = false;
		
		//setup optimizer
		GradientBasedOptimizer optimizer = null;
		if(usingCRF){
			HashMap<String,Double> crfModel = new HashMap<String,Double>();
			if(initModelFile!=null)
				DiscriminativeSupport.loadModel(initModelFile, crfModel, null);
			else{
				System.out.println("In crf, must specify feature set"); 
				System.exit(0);
			}
			optimizer = new DefaultCRF(crfModel, trainSize, batchUpdateSize, convergePass, initGain, sigma, isMinimizeScore);
			optimizer.initModel(0, 0);//TODO optimal initial parameters
			ndl = new NBESTDiscriminativeLearner(optimizer, new HashSet<String>(crfModel.keySet()));
			ndl.resetBaselineFeat();//add and init baseline feature
		}else{//perceptron
			HashMap<String,Double> perceptronSumModel = new HashMap<String,Double>();
			HashMap<String,Double> perceptronAvgModel = new HashMap<String,Double>();
			HashMap<String,Double> perceptronModel = new HashMap<String,Double>();
			if(initModelFile!=null){
				DiscriminativeSupport.loadModel(initModelFile, perceptronModel, null);
				perceptronModel.put(baselineFeatName, 1.0);
			}else{
				System.out.println("In perceptron, should specify feature set");				
			}
			optimizer = new DefaultPerceptron(perceptronSumModel, perceptronAvgModel,trainSize, batchUpdateSize, convergePass, initGain, sigma, isMinimizeScore);
			ndl = new NBESTDiscriminativeLearner(optimizer,  new HashSet<String>(perceptronModel.keySet()));
			ndl.resetBaselineFeat();
		}	
		
		//TODO
		ndl.optimizer.set_no_cooling();
		//ndl.p_optimizer.set_no_regularization();
			
		for(int loop_id=0; loop_id<max_loop; loop_id++){
			System.out.println("###################################Loop " + loop_id);
			for(int fid=0; fid < l_file_train_nbest.size(); fid++){
				System.out.println("#######Process file id " + fid);
				BufferedReader t_reader_nbest = FileUtilityOld.getReadFileStream((String)l_file_train_nbest.get(fid),"UTF-8");
				BufferedReader t_reader_orc = FileUtilityOld.getReadFileStream((String)l_file_orc.get(fid),"UTF-8");
				String line=null;
				int old_sent_id=-1;
				ArrayList<String> nbest = new ArrayList<String>();
				while((line=FileUtilityOld.readLineLzf(t_reader_nbest))!=null){
					String[] fds = line.split("\\s+\\|{3}\\s+");
					int new_sent_id = new Integer(fds[0]);
					if(old_sent_id!=-1 && old_sent_id!=new_sent_id){						
						String hyp_oracle = FileUtilityOld.readLineLzf(t_reader_orc);
						if(tbl_sent_selected.containsKey(old_sent_id)){
							System.out.println("#Process sentence " + old_sent_id);
							ndl.processOneSent( nbest, hyp_oracle, null);
						}else
							System.out.println("#Skip sentence " + old_sent_id);
						nbest.clear();
						//Support.print_hash_tbl(perceptron.g_tbl_sum_model);//debug
					}
					old_sent_id = new_sent_id;
					nbest.add(line);
				}
				//last nbest
				String hyp_oracle = FileUtilityOld.readLineLzf(t_reader_orc);
				if(tbl_sent_selected.containsKey(old_sent_id)){
					System.out.println("#Process sentence " + old_sent_id);
					ndl.processOneSent( nbest, hyp_oracle, null);
				}else
					System.out.println("#Skip sentence " + old_sent_id);
				nbest.clear();
				
				FileUtilityOld.closeReadFile(t_reader_nbest);
				FileUtilityOld.closeReadFile(t_reader_orc);
			}
			
			if(usingCRF){
				ndl.updateModel(true);
				FileUtilityOld.printHashTbl(optimizer.getSumModel(), f_model_out_prefix+".crf." + loop_id, false, false);
			}else{//perceptron
				ndl.updateModel(true);
				((DefaultPerceptron)optimizer).force_update_avg_model();
				FileUtilityOld.printHashTbl(optimizer.getSumModel(), f_model_out_prefix+".sum." + loop_id, false, false);
				FileUtilityOld.printHashTbl(optimizer.getAvgModel(), f_model_out_prefix+".avg." + loop_id, false, true);
			}
			System.out.println("Time cost: " + ((System.currentTimeMillis()-start_time)/1000));
			//clean up			
		}
	
	}
	

	
}
