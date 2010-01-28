package joshua.discriminative.training.learning_algorithm;

import java.util.HashMap;
import java.util.Map;

/*This class implements common functions:
 * (1) gradient computation
 * (2) batch update
 * (3) cooling schedule
 * (4) regularization
 * */

public abstract class GradientBasedOptimizer {	
	
	private int BATCH_UPDATE_SIZE=30; //update the perceptron after processing BATCH_SIZE
	private int TRAIN_SIZE = 610000;//number of training examples
	private int CONVERGE_PASS = 1;//assume the model will converge after pass CONVERGE_PASS
	private double COOLING_SCHEDULE_T = TRAIN_SIZE*CONVERGE_PASS*1.0/BATCH_UPDATE_SIZE; //where parameter t was adjusted such that the gain is halved after one pass through the data (610k*2/30)
	private double INITIAL_GAIN = 0.1;
	
	private double SIGMA=1.0;//the smaller SIGMA, the sharp the prior is; the more regularized of the model (meaning more feature weights goes close to zero)
	private double REG_CONSTANT_RATIO = 0; 
	
	private boolean IS_MINIMIZE_SCORE = false;
	
	protected int numModelChanges = 0 ; //how many times the model is changed
	
	private boolean noRegularization=false;//default is with regularization; (perceptron does not use this)
	private boolean noCooling=false;//default is with cooling
	
	public void set_no_regularization(){
		noRegularization =true;
	}
	
	public void set_no_cooling(){
		noCooling =true;
	}
	
	public GradientBasedOptimizer(int train_size, int batch_update_size, int converge_pass, double init_gain, double sigma, boolean is_minimize_score){
		TRAIN_SIZE = train_size;
		BATCH_UPDATE_SIZE = batch_update_size;
		CONVERGE_PASS = converge_pass;
		INITIAL_GAIN = init_gain;
		COOLING_SCHEDULE_T = TRAIN_SIZE*CONVERGE_PASS*1.0/BATCH_UPDATE_SIZE;
		
		SIGMA = sigma;
		REG_CONSTANT_RATIO = BATCH_UPDATE_SIZE*1.0/(TRAIN_SIZE*SIGMA*SIGMA);
		
		IS_MINIMIZE_SCORE = is_minimize_score;
		System.out.println("TRAIN_SIZE: " + TRAIN_SIZE);
		System.out.println("BATCH_UPDATE_SIZE: " + BATCH_UPDATE_SIZE);
		System.out.println("CONVERGE_PASS: " + CONVERGE_PASS);
		System.out.println("INITIAL_GAIN: " + INITIAL_GAIN);
		System.out.println("COOLING_SCHEDULE_T: " + COOLING_SCHEDULE_T);
		System.out.println("SIGMA: " + SIGMA);
		System.out.println("REG_CONSTANT_RATIO: " + REG_CONSTANT_RATIO);
		System.out.println("IS_MINIMIZE_SCORE: " + IS_MINIMIZE_SCORE);
	}		
	
	public  abstract void initModel(double minValue, double maxValue);// random start
	
	public  abstract void updateModel(HashMap tbl_feats_empirical, HashMap tbl_feats_model);
	
	public  abstract HashMap getAvgModel();
	
	public  abstract HashMap getSumModel();
	
	public  abstract void setFeatureWeight(String feat, double weight);
	
	public int getBatchSize(){
		return BATCH_UPDATE_SIZE;
	}
	
//	########################### common between CRF and Perceptron ######################
	protected  HashMap<String, Double>  getGradient(HashMap<String, Double>  empiricalFeatsTbl, HashMap<String, Double> modelFeatsTbl){
		HashMap<String, Double> res = new HashMap<String, Double>();
		//##process tbl_feats_oracle
		for( Map.Entry<String, Double>  entry : empiricalFeatsTbl.entrySet() ){
			String key = entry.getKey();
			double gradient = entry.getValue();
			Double v_1best = modelFeatsTbl.get(key);
			if(v_1best!=null) 
				gradient -= v_1best;//v_oracle - v_1best
			if(gradient != 0)//TODO
				if(IS_MINIMIZE_SCORE)
					res.put(key,-gradient);//note: we are minizing the cost
				else
					res.put(key,gradient);//note: we are max the prob
		}
		
		//##process tbl_feats_1best
		for(Map.Entry<String, Double>  entry : modelFeatsTbl.entrySet() ){
			String key = entry.getKey();
			Double v_oracle = empiricalFeatsTbl.get(key);
			if(v_oracle==null)//this feat only activate in the 1best, not in oracle
				if(IS_MINIMIZE_SCORE)
					res.put(key,  entry.getValue());//note: we are minizing the cost
				else
					res.put(key,  -entry.getValue());//note: we are maximize the prob
		}
		//System.out.println("gradient size is: " + res.size());
		return res;
	}

	protected  double computeGain(int iterNumber){//the numbers of updating the model
		if(noCooling)
			return 1.0;
		else 
			return INITIAL_GAIN*COOLING_SCHEDULE_T/(COOLING_SCHEDULE_T+iterNumber);
	}
	
	protected  double computeRegularizationScale(double updateGain){
		if(noRegularization)
			return 1.0;
		else 
			return 1.0-REG_CONSTANT_RATIO*updateGain;
	}
//	end ########################### common between CRF and Perceptron ######################	
}
