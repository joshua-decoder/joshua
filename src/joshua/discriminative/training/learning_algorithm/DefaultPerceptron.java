package joshua.discriminative.training.learning_algorithm;

import java.util.HashMap;
import java.util.Iterator;




/*Zhifei Li, <zhifei.work@gmail.com>
* Johns Hopkins University
*/

/*Cleasses extend this should include
 * (1) process_one_sent: get the reranked 1-best; get the feature counts
 * (2) rerank the hypothesis
 * (3) feature extraction from 1-best and oracle
 * */

public class DefaultPerceptron extends GradientBasedOptimizer {
	
	HashMap g_tbl_sum_model = null; //key: feat str; val: model paramemter
	HashMap g_tbl_avg_model = null;//key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter 
	
	
	public DefaultPerceptron(HashMap sum_model, HashMap avg_model,int train_size, int batch_update_size, int converge_pass, double init_gain, double sigma, boolean is_minimize_score){
		super(train_size, batch_update_size, converge_pass, init_gain,  sigma, is_minimize_score);
		g_tbl_sum_model = sum_model;
		g_tbl_avg_model = avg_model;
		if(g_tbl_sum_model==null || g_tbl_avg_model==null){System.out.println("model table is null"); System.exit(0);}
	}		
	

	public  void initModel(double min_value, double max_value){
		//TODO do nothing
	}
	
	
//	update tbl_sum_model and  tbl_avg_model inside
	public  void updateModel(HashMap tbl_feats_empirical, HashMap tbl_feats_model){
		numModelChanges++;
		System.out.println("######## update the perceptron model ############### " + numModelChanges);
		HashMap gradient = getGradient(tbl_feats_empirical, tbl_feats_model);
		//Support.print_hash_tbl(gradient);
		double update_gain = computeGain(numModelChanges);
		System.out.println("update gain is " + update_gain + "; gradident table size " + gradient.size());
		update_sum_model(g_tbl_sum_model, gradient, update_gain);
		update_avg_model(g_tbl_sum_model, g_tbl_avg_model, gradient, numModelChanges);
	}
	
	 
	//update tbl_sum_model inside
	protected void update_sum_model(HashMap tbl_sum_model, HashMap gradient, double update_gain){
		for(Iterator it =gradient.keySet().iterator(); it.hasNext();){
			String key = (String)it.next();
			Double old_v = (Double)tbl_sum_model.get(key);
			if(old_v!=null)
				tbl_sum_model.put(key, old_v + update_gain*(Double)gradient.get(key));
			else
				tbl_sum_model.put(key, update_gain*(Double)gradient.get(key)); //incrementally add feature
		}
	}

	
//	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
	//update tbl_avg_model inside
	protected void update_avg_model(HashMap tbl_sum_model, HashMap tbl_avg_model, HashMap feature_set, int cur_iter_id){//feature_set: the features need to be updated
		for(Iterator it =feature_set.keySet().iterator(); it.hasNext();){
			String key = (String)it.next();
			update_avg_model_one_feature(tbl_sum_model, tbl_avg_model, key, cur_iter_id);
		}
	}
	
//tbl_sum_model has already been updated	
//	key: feat str; val: (1) last avg-model paramemter, (2) last iter-id; (3) the last sum-model paramemter
//	update tbl_avg_model inside
	protected void update_avg_model_one_feature(HashMap tbl_sum_model, HashMap tbl_avg_model, String feat_key, int cur_iter_id){		
		Double[] old_v = (Double[])tbl_avg_model.get(feat_key);
		Double[] new_v = new Double[3];
		new_v[1] = new Double(cur_iter_id);//iter id 
		new_v[2] = (Double)tbl_sum_model.get(feat_key);//sum model para
		if(old_v!=null)
			new_v[0] = ( old_v[0]*old_v[1] + old_v[2]*(cur_iter_id-old_v[1]-1) + new_v[2] )/cur_iter_id;//avg
		else//incrementally add feature
			new_v[0] = new_v[2]/cur_iter_id;//avg			
		tbl_avg_model.put(feat_key, new_v);
	}
	
		
	//force update the whole avg model (for each feature, it will automatically handle case where feature already updated)
	public void force_update_avg_model(){
	    System.out.println("force avg update is called");
	    update_avg_model(g_tbl_sum_model, g_tbl_avg_model, g_tbl_sum_model, numModelChanges);	//update all features
	   
	}


	public HashMap getAvgModel() {
		return g_tbl_avg_model;
	}


	public HashMap getSumModel() {
		return g_tbl_sum_model;
	}


	public void setFeatureWeight(String feat, double weight) {
		g_tbl_sum_model.put(feat, weight);
		Double[] vals = new Double[3];
		vals[0]=weight;
		vals[1]=1.0;//TODO
		vals[2]=0.0;//TODO
		g_tbl_avg_model.put(feat, vals);
		
	}

}
