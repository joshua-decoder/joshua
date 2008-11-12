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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

import java.util.logging.Level;

import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FeatureFunction;

/**
 * this class implement Hypergraph node (i.e., HGNode); also known as Item in parsing 
 * 
 *
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-11-03 16:59:05 -0500 (星期一, 03 十一月 2008) $
 */

public class HGNode implements Comparable<HGNode> {
	public int i, j;
	public ArrayList<HyperEdge> l_deductions=null;//each deduction is a "and" node
	public HyperEdge best_deduction=null;//used in pruning, compute_item, and transit_to_goal
	public int lhs; //this is the symbol like: NP, VP, and so on
	HashMap<Integer, FFDPState> tbl_ff_dpstates; //the key is the feature id; remember the state required by each model, for example, edge-ngrams for LM model
	
	//######### auxiluary variables, no need to store on disk
	private String signature=null;//signature of this item: lhs, states
	static String FF_SIG_SEP = " -f- "; //seperator for the signature for each feature function
	
	//######## for pruning purpose
	public boolean is_dead=false;
	public double est_total_cost=0.0; //it includes the bonus cost
			
	public HGNode(int i_in, int j_in, int lhs_in, HashMap<Integer, FFDPState>   states_in, HyperEdge init_deduction, double est_total_cost_in){
		i = i_in;
		j= j_in;					
		lhs = lhs_in;
		tbl_ff_dpstates = states_in;
		est_total_cost=est_total_cost_in;
		add_deduction_in_item(init_deduction);
	}

	
	//used by disk hg
	public HGNode(int i_in, int j_in, int lhs_in,  ArrayList<HyperEdge> l_deductions_in, HyperEdge best_deduction_in, HashMap<Integer, FFDPState> states_in){
		i = i_in;
		j= j_in;
		lhs = lhs_in;
		l_deductions = l_deductions_in;
		best_deduction = best_deduction_in;			
		tbl_ff_dpstates = states_in;
	}
			
	public void add_deduction_in_item(HyperEdge dt){
		if(l_deductions==null)l_deductions = new ArrayList<HyperEdge>();			
		l_deductions.add(dt);
		if(best_deduction==null || best_deduction.best_cost>dt.best_cost) best_deduction=dt;//no change when tied			
	}
	
	public void add_deductions_in_item(ArrayList<HyperEdge> l_dt){
		for(HyperEdge dt : l_dt) add_deduction_in_item(dt);			
	}	
		
	
	public HashMap<Integer, FFDPState>  getTblFeatDPStates(){
		return tbl_ff_dpstates;
	}
	
	
	public FFDPState getFeatDPState(FeatureFunction ff){			
		return getFeatDPState(ff.getFeatureID());
	}
	
	public FFDPState getFeatDPState(int feat_id){
		if(tbl_ff_dpstates==null)
			return null;
		else
			return (FFDPState) tbl_ff_dpstates.get(feat_id);
	}
	
	public void print_info(Level level){
		if (HyperGraph.logger.isLoggable(level)) HyperGraph.logger.log(level, String.format("lhs: %s; cost: %.3f",lhs, best_deduction.best_cost));
	}		
	
	
	//signature of this item: lhs, states (we do not need i, j)
	public String get_signature() {
		if (null == this.signature) {				
			StringBuffer signature_ = new StringBuffer();
			signature_.append(lhs);
			
			if(tbl_ff_dpstates!=null && tbl_ff_dpstates.size()>0){
				for (Iterator iter = tbl_ff_dpstates.entrySet().iterator(); iter.hasNext();){//for each model
	                Map.Entry entry = (Map.Entry)iter.next();
	                FFDPState dpstate = (FFDPState)entry.getValue();
	                signature_.append(dpstate.getSignature(false));
	                if(iter.hasNext()) signature_.append(FF_SIG_SEP);
				}
			}
			
			this.signature = signature_.toString();
		}
		//System.out.println("sig is: " +signature);
		//Support.write_log_line(String.format("Signature is %s", res), Support.INFO);
		return this.signature;
	}
	
	//sort by est_total_cost: for prunning purpose
	public int compareTo(HGNode anotherItem) throws ClassCastException {
	    if (!(anotherItem instanceof HGNode))
	      throw new ClassCastException("An Item object expected.");
	    if(this.est_total_cost < ((HGNode)anotherItem).est_total_cost)
	    	return -1;
	    else if(this.est_total_cost == ((HGNode)anotherItem).est_total_cost)
	    	return 0;
	    else
	    	return 1;    
	}
	
	public static Comparator<HGNode> NegtiveCostComparator
		= new Comparator<HGNode>() {
			
			public int compare(HGNode item1, HGNode item2) {
				double cost1 = item1.est_total_cost;
				double cost2 = item2.est_total_cost;
				if (cost1 > cost2) {
					return -1;
				} else if (cost1 == cost2) {
					return 0;
				} else {
					return 1;
				}
			}
	};
	
	

}