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

import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FFTransitionResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * this class implement 
 * (1) HyperGraph-related data structures (Item and Hyper-edges) 
 *
 * Note:to seed the kbest extraction, each deduction should have the best_cost properly set. We do not require any list being sorted
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class HyperGraph {

	// pointer to goal HGNode
	public HGNode goal_item = null;
	
	public int num_items = -1;
	public int num_hyperedges = -1;
	public int sent_id = -1;
	public int sent_len = -1;
	
	static final Logger logger = Logger.getLogger(HyperGraph.class.getName());
	
	public HyperGraph(HGNode g_item, int n_items, int n_deducts, int s_id, int s_len){
		goal_item = g_item;
		num_items = n_items;
		num_hyperedges = n_deducts;
		sent_id = s_id;
		sent_len = s_len;
	}
	
	
	
	static public FFTransitionResult computeTransition(HyperEdge dt, FeatureFunction m, int start_span, int end_span){
		return computeTransition(dt, dt.get_rule(), dt.get_ant_items(), m,  start_span,  end_span);
	}
	
	static public FFTransitionResult computeTransition(HyperEdge dt, Rule rl,  ArrayList<HGNode> l_ant_hgnodes, FeatureFunction m, int start_span, int end_span){
		FFTransitionResult res = null;
		if(m.isStateful() == false){//stateless feature
			res = m.transition(dt, rl, null, start_span, end_span);
		}else{
			ArrayList<FFDPState> previous_states = null;
			if (l_ant_hgnodes != null) {
				previous_states = new ArrayList<FFDPState>();
				for (HGNode it : l_ant_hgnodes) {
					previous_states.add( it.getFeatDPState(m) );
				}
			}
			res = m.transition(dt, rl, previous_states, start_span, end_span);
		}
		if (null == res) {
			logger.severe("compute_item: transition returned null state");
			System.exit(0);
		}
		return res;
	} 
	
	static public double computeFinalTransition(HyperEdge dt, FeatureFunction m){
		double res = 0;
		if(m.isStateful() == false){//stateless feature
			res = m.finalTransition(dt, null);
		}else{//stateful feature
			res = m.finalTransition(dt, dt.get_ant_items().get(0).getFeatDPState(m));
		}
		return res;
	} 
	
	
	
	//####### template to explore hypergraph #########################
	/*
	private void operation_hg(HyperGraph hg){		
		tbl_processed_items.clear(); 
		operation_item(hg.goal_item);
	}
	
	private void operation_item(Item it){
		if(tbl_processed_items.containsKey(it))return;
		tbl_processed_items.put(it,1);
		
		//### recursive call on each deduction
		for(Deduction dt : it.l_deductions){
			operation_deduction(dt);//deduction-specifc operation
		}
		
		//### item-specific operation
	}
	
	private void operation_deduction(Deduction dt){
		//### recursive call on each ant item
		if(dt.get_ant_items()!=null)
			for(Item ant_it : dt.get_ant_items())
				operation_item(ant_it);
		
		//### deduction-specific operation				
		Rule rl = dt.get_rule();
		if(rl!=null){
		
		}
	}
	*/
	//############ end ##############################
}
