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
import joshua.decoder.hypergraph.HyperGraph.Deduction;
import joshua.decoder.hypergraph.HyperGraph.Item;

import java.util.HashMap;


/**
 * to use the functions here, one need to extend the class  to provide a way to calculate the deduction cost based on feature set
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

//TODO: currently assume log semiring, need to generalize to other semiring
//already implement both max-product and sum-product algortithms for log-semiring
//Note: this class does not require the correctness of the best_cost?

public abstract class InsideOutside {
	/*Two operations: add and multi
	 * add: different hyperedges lead to a specific item
	 * multi: prob of a derivation is a multi of all constituents
	 **/
	int ADD_MODE=0; //0: sum; 1: viterbi-min, 2: viterbi-max
	int LOG_SEMIRING=1;
	int SEMIRING=LOG_SEMIRING; //default is in log; or real, or logic
	double ZERO_IN_SEMIRING = Double.NEGATIVE_INFINITY;//log-domain
	double ONE_IN_SEMIRING = 0;//log-domain
	
	HashMap tbl_inside_cost =  new HashMap();//remember inside cost of each item: 
	HashMap tbl_outside_cost =  new HashMap();//remember outside cost of each item
	double normalization_constant = ONE_IN_SEMIRING;
	
	/* for each item, remember how many deductions pointering to me, this is needed for outside estimation
	 * during outside estimation, an item will recursive call its deductions to do outside-estimation only after it itself is done with outside estimation, this is necessary because
	 * the outside estimation of the items under its deductions require the item's outside value
	 */
	private HashMap tbl_num_parent_deductions = new HashMap();
	
	private HashMap tbl_for_sanity_check = null;
	double g_sanity_post_prob =0;
	
	//get feature-set specific score for deduction
	protected abstract double get_deduction_score(Deduction dt, Item parent_it);
	
	
	//the results are stored in tbl_inside_cost and tbl_outside_cost
	public void run_inside_outside(HyperGraph hg, int add_mode, int semiring){//add_mode||| 0: sum; 1: viterbi-min, 2: viterbi-max
		setup_semiring(semiring, add_mode);
		
		//System.out.println("outside estimation");
		inside_estimation_hg(hg);
		//System.out.println("inside estimation");
		outside_estimation_hg(hg);
		normalization_constant = (Double)tbl_inside_cost.get(hg.goal_item);
		tbl_num_parent_deductions.clear();
	}
	
	//to save memory, external class should call this method
	public  void clear_state(){
		tbl_num_parent_deductions.clear();
		tbl_inside_cost.clear();
		tbl_outside_cost.clear();
	}

	//######### use of inside-outside costs ##########################
	//this is the logZ
    public double get_normalization_constant(){
    	return normalization_constant;
    }
	
	//this is the expected log cost of this deduction, without normalization
	public double get_deduction_merit(Deduction dt, Item parent){
		//### outside of parent
		double outside = (Double)tbl_outside_cost.get(parent);
		
		//### get inside cost of all my ant-items
		double inside = ONE_IN_SEMIRING;
		if(dt.get_ant_items()!=null){
			for(Item ant_it : dt.get_ant_items())
				inside = multi_in_semiring(inside,(Double)tbl_inside_cost.get(ant_it));
		}
		
		//### add deduction/rule specific cost
		double merit = multi_in_semiring(inside, outside);
		merit = multi_in_semiring(merit, get_deduction_score(dt, parent));
		
		return merit;
	}	
	
	public double get_deduction_post_prob(Deduction dt, Item parent ){		
		if(SEMIRING==LOG_SEMIRING){
			return Math.exp((get_deduction_merit(dt, parent)-normalization_constant));
		}else{
			System.out.println("not implemented"); System.exit(0);
			return 1;
		}
	}
	
	
	public void sanity_check_hg(HyperGraph hg){		
		tbl_for_sanity_check = new HashMap();
		sanity_check_item(hg.goal_item);
		System.out.println("g_sanity_post_prob is " + g_sanity_post_prob);
		tbl_for_sanity_check.clear();
	}
	
	private void sanity_check_item(Item it){
		if(tbl_for_sanity_check.containsKey(it))return;
		tbl_for_sanity_check.put(it,1);
		
		//### recursive call on each deduction
		for(Deduction dt : it.l_deductions){
			g_sanity_post_prob += get_deduction_post_prob(dt,it);
			sanity_check_deduction(dt);//deduction-specifc operation
		}
		
		//### item-specific operation
	}
	
	private void sanity_check_deduction(Deduction dt){
		//### recursive call on each ant item
		if(dt.get_ant_items()!=null)
			for(Item ant_it : dt.get_ant_items())
				sanity_check_item(ant_it);
		
		//### deduction-specific operation				
		
	}
	//################## end use of inside-outside costs 
	
	
	
//############ bottomn-up insdide estimation ##########################	
	private void inside_estimation_hg(HyperGraph hg){
		tbl_inside_cost.clear(); 
		tbl_num_parent_deductions.clear();
		inside_estimation_item(hg.goal_item);
	}
	
	private double inside_estimation_item(Item it){
		//### get number of deductions that point to me
		Integer num_called = (Integer)tbl_num_parent_deductions.get(it);
		if(num_called!=null)
			tbl_num_parent_deductions.put(it, num_called+1);
		else
			tbl_num_parent_deductions.put(it, 1);
		
		if(tbl_inside_cost.containsKey(it))
			return (Double) tbl_inside_cost.get(it);
		double inside_cost = ZERO_IN_SEMIRING;
		
		//### recursive call on each deduction
		for(Deduction dt : it.l_deductions){
			double v_dt = inside_estimation_deduction(dt, it);//deduction-specifc operation
			inside_cost = add_in_semiring(inside_cost, v_dt);
		}		
		//### item-specific operation, but all the cost should be factored into each deduction
		
		tbl_inside_cost.put(it,inside_cost);
		return inside_cost;
	}
	
	private double inside_estimation_deduction(Deduction dt, Item parent_item){
		double inside_cost = ONE_IN_SEMIRING; 
		//### recursive call on each ant item
		if(dt.get_ant_items()!=null)
			for(Item ant_it : dt.get_ant_items()){
				double v_item = inside_estimation_item(ant_it);
				inside_cost =  multi_in_semiring(inside_cost, v_item);				
			}
				
		//### deduction operation
		double deduct_cost = get_deduction_score(dt, parent_item);//feature-set specific	
		inside_cost =  multi_in_semiring(inside_cost, deduct_cost);	
		return inside_cost;
	}
//########### end inside estimation	

//############ top-downn outside estimation ##########################
	
	private void outside_estimation_hg(HyperGraph hg){	
		tbl_outside_cost.clear(); 
		tbl_outside_cost.put(hg.goal_item, ONE_IN_SEMIRING);//initialize
		for(Deduction dt : hg.goal_item.l_deductions)
			outside_estimation_deduction(dt, hg.goal_item);	
	}
	
	private void outside_estimation_item(Item cur_it, Item upper_item, Deduction parent_dt, double parent_deduct_cost){
		Integer num_called = (Integer)tbl_num_parent_deductions.get(cur_it);
		if(num_called==null || num_called==0){System.out.println("un-expected call, must be wrong"); System.exit(0);}
		tbl_num_parent_deductions.put(cur_it, num_called-1);		
		
		double old_outside_cost = ZERO_IN_SEMIRING;
		if(tbl_outside_cost.containsKey(cur_it))
			old_outside_cost = (Double) tbl_outside_cost.get(cur_it);
		
		double additional_outside_cost = ONE_IN_SEMIRING;
		
		//### add parent deduction cost
		additional_outside_cost =  multi_in_semiring(additional_outside_cost, parent_deduct_cost);
		
		//### sibing specifc
		if(parent_dt.get_ant_items()!=null && parent_dt.get_ant_items().size()>1)
			for(Item ant_it : parent_dt.get_ant_items()){
				if(ant_it != cur_it){
					double inside_cost_item =(Double)tbl_inside_cost.get(ant_it);//inside cost
					additional_outside_cost =  multi_in_semiring(additional_outside_cost, inside_cost_item);
				}				
			}
				
		//### upper item
		double outside_cost_item = (Double)tbl_outside_cost.get(upper_item);//outside cost
		additional_outside_cost =  multi_in_semiring(additional_outside_cost, outside_cost_item);
		
		//#### add to old cost 
		additional_outside_cost = add_in_semiring(additional_outside_cost, old_outside_cost);		
		
		tbl_outside_cost.put(cur_it, additional_outside_cost);
		
		//### recursive call on each deduction
		if( num_called-1<=0){//i am done
			for(Deduction dt : cur_it.l_deductions){
				//TODO: potentially, we can collect the feature expection in each hyperedge here, to avoid another pass of the hypergraph to get the counts
				outside_estimation_deduction(dt, cur_it);
			}
		}
	}
	
	
	private void outside_estimation_deduction(Deduction dt, Item parent_item){
		//we do not need to outside cost if no ant items
		if(dt.get_ant_items()!=null){
			//### deduction specific cost
			double deduction_cost = get_deduction_score(dt, parent_item);//feature-set specific
			
			//### recursive call on each ant item
			for(Item ant_it : dt.get_ant_items()){
				outside_estimation_item(ant_it, parent_item, dt, deduction_cost);
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
	private double multi_in_log_semiring(double x, double y){//value is cost
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
