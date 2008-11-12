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

import joshua.decoder.Symbol;
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
	public HGNode goal_item=null;
	public int num_items = -1;
	public int num_deduction = -1;
	public int sent_id = -1;
	public int sent_len = -1;
	
	Symbol p_symbol = null;
	
	static final Logger logger = Logger.getLogger(HyperGraph.class.getName());
	
	public HyperGraph(HGNode g_item, int n_items, int n_deducts, int s_id, int s_len){
		goal_item = g_item;
		num_items = n_items;
		num_deduction = n_deducts;
		sent_id = s_id;
		sent_len = s_len;
	}
	
	//get one-best string under item
	public static String extract_best_string(Symbol p_symbol, HGNode item){
		StringBuffer res = new StringBuffer();

		HyperEdge p_edge = item.best_deduction;
		Rule rl = p_edge.get_rule();
		
		if (null == rl) { // deductions under "goal item" does not have rule
			if (p_edge.get_ant_items().size() != 1) {
				System.out.println("error deduction under goal item have not equal one item");
				System.exit(1);
			}
			return extract_best_string(p_symbol, (HGNode)p_edge.get_ant_items().get(0));
		}	
		
		for(int c=0; c<rl.english.length; c++){
    		if(p_symbol.isNonterminal(rl.english[c])==true){
    			int id=p_symbol.getEngNonTerminalIndex(rl.english[c]);
    			HGNode child = (HGNode)p_edge.get_ant_items().get(id);
    			res.append(extract_best_string(p_symbol, child));
    		}else{
    			res.append(p_symbol.getWord(rl.english[c]));
    		}
    		if(c<rl.english.length-1) res.append(" ");
		}
		return res.toString();
	}
	
//	######## find 1best hypergraph#############	
	public HyperGraph get_1best_tree_hg(){		
		HyperGraph res = new HyperGraph(clone_item_with_best_deduction(goal_item), -1, -1, sent_id, sent_len);//TODO: number of items/deductions
		get_1best_tree_item(res.goal_item);
		return res;
	}
	
	private void get_1best_tree_item(HGNode it){	
		HyperEdge dt = it.best_deduction;
		if(dt.get_ant_items()!=null)
			for(int i=0; i< dt.get_ant_items().size(); i++){
				HGNode ant_it = (HGNode) dt.get_ant_items().get(i);
				HGNode new_it = clone_item_with_best_deduction(ant_it);
				dt.get_ant_items().set(i, new_it);
				get_1best_tree_item(new_it);	
			}		
	}	
	
	//TODO: tbl_states
	public static HGNode clone_item_with_best_deduction(HGNode it_in){
		ArrayList<HyperEdge> l_deductions = new ArrayList<HyperEdge>(1);
		HyperEdge clone_dt = clone_deduction(it_in.best_deduction);
		l_deductions.add(clone_dt);
		return new HGNode(it_in.i, it_in.j, it_in.lhs,  l_deductions, clone_dt, it_in.tbl_ff_dpstates);	
	}
	
	//TODO: tbl_states
	public static HGNode clone_item(HGNode it_in){
		ArrayList<HyperEdge> l_deductions = new ArrayList<HyperEdge>();
		HyperEdge best_dt=null;
		for(HyperEdge dt : it_in.l_deductions){	
			if(dt==it_in.best_deduction) best_dt = dt; 				
			HyperEdge clone_dt = clone_deduction(dt); 
			l_deductions.add(clone_dt);
		}
		return new HGNode(it_in.i, it_in.j, it_in.lhs,  l_deductions, best_dt, it_in.tbl_ff_dpstates);	
	}
	
	public static HyperEdge clone_deduction(HyperEdge dt_in){
		ArrayList<HGNode> l_ant_items = null;
		if (null != dt_in.get_ant_items()) {
			l_ant_items = new ArrayList<HGNode>(dt_in.get_ant_items());//l_ant_items will be changed in get_1best_tree_item
		}
		HyperEdge res = new HyperEdge(dt_in.get_rule(), dt_in.best_cost, dt_in.get_transition_cost(false), l_ant_items);
		return res;
	}
	//###end
	
	
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
