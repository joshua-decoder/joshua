/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.decoder.hypergraph;

import joshua.decoder.hypergraph.HyperGraph;

import java.util.HashMap;


/**
 * to use the functions here, one need to extend the class  to provide a way to calculate the deduction cost based on feature set
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-11-05 11:40:13 -0500 (星期三, 05 十一月 2008) $
 */

//TODO: currently assume log semiring, need to generalize to other semiring
//already implement both max-product and sum-product algortithms for log-semiring
//Note: this class requires the correctness of transition prob of each hyperedge, which itself may require the correctness of best_cost at each item

public abstract class DefaultInsideOutside {
	/*Two operations: add and multi
	 * add: different hyperedges lead to a specific item
	 * multi: prob of a derivation is a multi of all constituents
	 **/
	int ADD_MODE=0; //0: sum; 1: viterbi-min, 2: viterbi-max
	int LOG_SEMIRING=1;
	int SEMIRING=LOG_SEMIRING; //default is in log; or real, or logic
	double ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;//log-domain
	double ONE_IN_SEMIRING = 0;//log-domain
	double scaling_factor ; //try to scale the original distribution: smooth or winner-take-all
	
	private HashMap tbl_inside_prob =  new HashMap();//remember inside prob of each item: 
	private HashMap tbl_outside_prob =  new HashMap();//remember outside prob of each item
	double normalization_constant = ONE_IN_SEMIRING;
	
	/* for each item, remember how many deductions pointering to me, this is needed for outside estimation
	 * during outside estimation, an item will recursive call its deductions to do outside-estimation only after it itself is done with outside estimation, this is necessary because
	 * the outside estimation of the items under its deductions require the item's outside value
	 */
	private HashMap tbl_num_parent_deductions = new HashMap();
	
	private HashMap tbl_for_sanity_check = null;
	
	//get feature-set specific **log probability** for each hyperedge
	protected abstract double get_deduction_prob(HyperEdge dt, HGNode parent_it);
	
	protected  double get_deduction_prob(HyperEdge dt, HGNode parent_it, double scaling_factor){
		return get_deduction_prob(dt, parent_it)*scaling_factor;
	}
	
	//the results are stored in tbl_inside_prob and tbl_outside_prob
	public void run_inside_outside(HyperGraph hg, int add_mode, int semiring, double scaling_factor_){//add_mode||| 0: sum; 1: viterbi-min, 2: viterbi-max
		setup_semiring(semiring, add_mode);
		scaling_factor = scaling_factor_;
		
		//System.out.println("outside estimation");
		inside_estimation_hg(hg);
		//System.out.println("inside estimation");
		outside_estimation_hg(hg);
		normalization_constant = (Double)tbl_inside_prob.get(hg.goal_item);
		System.out.println("normalization constant is " + normalization_constant);
		tbl_num_parent_deductions.clear();
		//sanity_check_hg(hg);
	}
	
	//to save memory, external class should call this method
	public  void clear_state(){
		tbl_num_parent_deductions.clear();
		tbl_inside_prob.clear();
		tbl_outside_prob.clear();
	}

	//######### use of inside-outside probs ##########################
	//this is the logZ where Z is the sum[ exp( log prob ) ]
    public double get_normalization_constant(){
    	return normalization_constant;
    }
	
	//this is the log of expected/posterior prob (i.e., LogP, where P is the posterior probability), without normalization
	public double get_deduction_unnormalized_posterior_log_prob(HyperEdge dt, HGNode parent){
		//### outside of parent
		double outside = (Double)tbl_outside_prob.get(parent);
		
		//### get inside prob of all my ant-items
		double inside = ONE_IN_SEMIRING;
		if(dt.get_ant_items()!=null){
			for(HGNode ant_it : dt.get_ant_items())
				inside = multi_in_semiring(inside,(Double)tbl_inside_prob.get(ant_it));
		}
		
		//### add deduction/rule specific prob
		double merit = multi_in_semiring(inside, outside);
		merit = multi_in_semiring(merit, get_deduction_prob(dt, parent, this.scaling_factor));
		
		return merit;
	}	
	
	//normalized probabily in [0,1]
	public double get_deduction_posterior_prob(HyperEdge dt, HGNode parent ){
		if(SEMIRING==LOG_SEMIRING){
			double res = Math.exp((get_deduction_unnormalized_posterior_log_prob(dt, parent)-get_normalization_constant()));
			//System.out.println("dt cost: " + dt.get_transition_cost(false)+" ;merit: " + get_deduction_unnormalized_posterior_log_prob(dt, parent) + "; prob: " + res);
			if(res<0.0-1e-2 || res >1.0+1e-2){
				System.out.println("res is not within [0,1], must be wrong value: " + res);
				System.exit(0);
			}
			return res;
		}else{
			System.out.println("not implemented"); System.exit(0);
			return 1;
		}
	}
	
//	this is the log of expected/posterior prob (i.e., LogP, where P is the posterior probability), without normalization
	public double get_hgnode_unnormalized_posterior_log_prob(HGNode node){
		//### outside of parent
		double inside =  (Double)tbl_inside_prob.get(node);
		double outside = (Double)tbl_outside_prob.get(node);
		return multi_in_semiring(inside, outside);
	}	
	
	
//	normalized probabily in [0,1]
	public double get_hgnode_posterior_prob(HGNode node ){
		if(SEMIRING==LOG_SEMIRING){
			double res = Math.exp((get_hgnode_unnormalized_posterior_log_prob(node)-get_normalization_constant()));
			//System.out.println("dt cost: " + dt.get_transition_cost(false)+" ;merit: " + get_deduction_unnormalized_posterior_log_prob(dt, parent) + "; prob: " + res);
			if(res<0.0-1e-2 || res >1.0+1e-2){
				System.out.println("res is not within [0,1], must be wrong value: " + res);
				System.exit(0);
			}
			return res;
		}else{
			System.out.println("not implemented"); System.exit(0);
			return 1;
		}
	}
	
	/*Originally, to see if the sum of the posterior probabilities of all the hyperedges sum to one
	 * However, this won't work! The sum should be greater than 1.
	 * */
	public void sanity_check_hg(HyperGraph hg){	
		tbl_for_sanity_check = new HashMap();
		//System.out.println("num_dts: " + hg.goal_item.l_deductions.size());
		sanity_check_item(hg.goal_item);
		System.out.println("survied sanity check!!!!");
	}
	
	private void sanity_check_item(HGNode it){		
		if(tbl_for_sanity_check.containsKey(it))return;
		tbl_for_sanity_check.put(it,1);
		double prob_sum=0;
		//### recursive call on each deduction
		for(HyperEdge dt : it.l_deductions){
			prob_sum += get_deduction_posterior_prob(dt,it);
			//System.out.println("tran_cost: " + dt.get_transition_cost(true) + "; prob: " +  get_deduction_posterior_prob(dt,it));
			sanity_check_deduction(dt);//deduction-specifc operation
		}
		double supposed_sum = get_hgnode_posterior_prob(it);
		if(Math.abs(prob_sum-supposed_sum)>1e-3){
			System.out.println("prob_sum=" + prob_sum + "; supposed_sum=" + supposed_sum + "; sanity check fail!!!!");
			System.exit(0);
		}
		//### item-specific operation
	}
	
	private void sanity_check_deduction(HyperEdge dt){
		//### recursive call on each ant item
		if(dt.get_ant_items()!=null)
			for(HGNode ant_it : dt.get_ant_items())
				sanity_check_item(ant_it);
		
		//### deduction-specific operation				
		
	}
	//################## end use of inside-outside probs 
	
	
	
//############ bottomn-up insdide estimation ##########################	
	private void inside_estimation_hg(HyperGraph hg){
		tbl_inside_prob.clear(); 
		tbl_num_parent_deductions.clear();
		inside_estimation_item(hg.goal_item);
	}
	
	private double inside_estimation_item(HGNode it){
		//### get number of deductions that point to me
		Integer num_called = (Integer)tbl_num_parent_deductions.get(it);
		if(num_called!=null)
			tbl_num_parent_deductions.put(it, num_called+1);
		else
			tbl_num_parent_deductions.put(it, 1);
		
		if(tbl_inside_prob.containsKey(it))
			return (Double) tbl_inside_prob.get(it);
		double inside_prob = ZERO_IN_SEMIRING;
		
		//### recursive call on each deduction
		for(HyperEdge dt : it.l_deductions){
			double v_dt = inside_estimation_deduction(dt, it);//deduction-specifc operation
			inside_prob = add_in_semiring(inside_prob, v_dt);
		}		
		//### item-specific operation, but all the prob should be factored into each deduction
		
		tbl_inside_prob.put(it,inside_prob);
		return inside_prob;
	}
	
	private double inside_estimation_deduction(HyperEdge dt, HGNode parent_item){
		double inside_prob = ONE_IN_SEMIRING; 
		//### recursive call on each ant item
		if(dt.get_ant_items()!=null)
			for(HGNode ant_it : dt.get_ant_items()){
				double v_item = inside_estimation_item(ant_it);
				inside_prob =  multi_in_semiring(inside_prob, v_item);				
			}
				
		//### deduction operation
		double deduct_prob = get_deduction_prob(dt, parent_item, this.scaling_factor);//feature-set specific	
		inside_prob =  multi_in_semiring(inside_prob, deduct_prob);	
		return inside_prob;
	}
//########### end inside estimation	

//############ top-downn outside estimation ##########################
	
	private void outside_estimation_hg(HyperGraph hg){	
		tbl_outside_prob.clear(); 
		tbl_outside_prob.put(hg.goal_item, ONE_IN_SEMIRING);//initialize
		for(HyperEdge dt : hg.goal_item.l_deductions)
			outside_estimation_deduction(dt, hg.goal_item);	
	}
	
	private void outside_estimation_item(HGNode cur_it, HGNode upper_item, HyperEdge parent_dt, double parent_deduct_prob){
		Integer num_called = (Integer)tbl_num_parent_deductions.get(cur_it);
		if(num_called==null || num_called==0){System.out.println("un-expected call, must be wrong"); System.exit(0);}
		tbl_num_parent_deductions.put(cur_it, num_called-1);		
		
		double old_outside_prob = ZERO_IN_SEMIRING;
		if(tbl_outside_prob.containsKey(cur_it))
			old_outside_prob = (Double) tbl_outside_prob.get(cur_it);
		
		double additional_outside_prob = ONE_IN_SEMIRING;
		
		//### add parent deduction prob
		additional_outside_prob =  multi_in_semiring(additional_outside_prob, parent_deduct_prob);
		
		//### sibing specifc
		if(parent_dt.get_ant_items()!=null && parent_dt.get_ant_items().size()>1)
			for(HGNode ant_it : parent_dt.get_ant_items()){
				if(ant_it != cur_it){
					double inside_prob_item =(Double)tbl_inside_prob.get(ant_it);//inside prob
					additional_outside_prob =  multi_in_semiring(additional_outside_prob, inside_prob_item);
				}				
			}
				
		//### upper item
		double outside_prob_item = (Double)tbl_outside_prob.get(upper_item);//outside prob
		additional_outside_prob =  multi_in_semiring(additional_outside_prob, outside_prob_item);
		
		//#### add to old prob 
		additional_outside_prob = add_in_semiring(additional_outside_prob, old_outside_prob);		
		
		tbl_outside_prob.put(cur_it, additional_outside_prob);
		
		//### recursive call on each deduction
		if( num_called-1<=0){//i am done
			for(HyperEdge dt : cur_it.l_deductions){
				//TODO: potentially, we can collect the feature expection in each hyperedge here, to avoid another pass of the hypergraph to get the counts
				outside_estimation_deduction(dt, cur_it);
			}
		}
	}
	
	
	private void outside_estimation_deduction(HyperEdge dt, HGNode parent_item){
		//we do not need to outside prob if no ant items
		if(dt.get_ant_items()!=null){
			//### deduction specific prob
			double deduction_prob = get_deduction_prob(dt, parent_item, this.scaling_factor);//feature-set specific
			
			//### recursive call on each ant item
			for(HGNode ant_it : dt.get_ant_items()){
				outside_estimation_item(ant_it, parent_item, dt, deduction_prob);
			}
		}
	}
//########### end outside estimation		
	
	

//############ common ##########################
	private void setup_semiring(int semiring, int add_mode){
		ADD_MODE=add_mode;		
		SEMIRING = semiring;
		if(SEMIRING==LOG_SEMIRING){
			if(ADD_MODE==0){//sum
				ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;
				ONE_IN_SEMIRING = 0;
			}else if (ADD_MODE==1){//viter-min
				ZERO_IN_SEMIRING = Double.POSITIVE_INFINITY;
				ONE_IN_SEMIRING = 0;
			}else if (ADD_MODE==2){//viter-max
				ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;
				ONE_IN_SEMIRING = 0;
			}else{
				System.out.println("invalid add mode"); System.exit(0);
			}			
		}else{
			System.out.println("un-supported semiring"); System.exit(0);
		}
	}
	
	private double multi_in_semiring(double x, double y){
		if(SEMIRING==LOG_SEMIRING){
			return multi_in_log_semiring(x,y);
		}else{
			System.out.println("un-supported semiring"); System.exit(0); return -1;
		}
	} 	
	
	
	private double divide_in_semiring(double x, double y){// x/y
		if(SEMIRING==LOG_SEMIRING){
			return x-y;
		}else{
			System.out.println("un-supported semiring"); System.exit(0); return -1;
		}
	} 	
	
	private double add_in_semiring(double x, double y){
		if(SEMIRING==LOG_SEMIRING){
			return add_in_log_semiring(x,y);
		}else{
			System.out.println("un-supported semiring"); System.exit(0); return -1;
		}
	} 	
	
	//AND
	private double multi_in_log_semiring(double x, double y){//value is Log prob
		return x + y;
	}
	
	//OR: return Math.log(Math.exp(x) + Math.exp(y));
	private double add_in_log_semiring(double x, double y){//prevent over-flow 
		if(ADD_MODE==0){//sum
			if(x==Double.NEGATIVE_INFINITY)//if y is also n-infinity, then return n-infinity
				return y;
			if(y==Double.NEGATIVE_INFINITY)
				return x;
			
			if(y<=x)
				return x + Math.log(1+Math.exp(y-x));
			else//x<y
				return y + Math.log(1+Math.exp(x-y));
		}else if (ADD_MODE==1){//viter-min
			return (x<=y)?x:y;
		}else if (ADD_MODE==2){//viter-max
			return (x>=y)?x:y;
		}else{
			System.out.println("invalid add mode"); System.exit(0); return 0;
		}
	}
//############ end common #####################	
	
}
