package edu.jhu.joshua.VariationalDecoder;

import java.util.HashMap;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.TrivialInsideOutside;



public class ConstituentVariationalDecoder extends TrivialInsideOutside {
    HashMap g_tbl_processed_items = new HashMap();

    //return changed hg
    public HyperGraph decoding(HyperGraph hg){
            run_inside_outside(hg, 0, 1);//ADD_MODE=0=sum; LOG_SEMIRING=1;
            rerankHG(hg);
            clear_state();
          
            
            return hg;
    }

    //  #################### rerank HG #######
	private void rerankHG(HyperGraph hg){
		//### change the best_pointer and best_cost in hypergraph in hg
		g_tbl_processed_items.clear();
		rerankHGNode(hg.goal_item);
		g_tbl_processed_items.clear();
	}

	
	//item: recursively call my children deductions, change pointer for best_deduction, and remember changed_cost 
	private  void rerankHGNode(HGNode it ){
		if(g_tbl_processed_items.containsKey(it))	return;
		g_tbl_processed_items.put(it,1);
		
		//### recursively call my children deductions, change pointer for best_deduction
		it.best_deduction=null;
		for(HyperEdge dt : it.l_deductions){					
			rerankHyperEdge(it, dt);
			if(it.best_deduction==null || dt.best_cost < it.best_deduction.best_cost)
				it.best_deduction = dt;//prefer smaller cost
		}
	}
	
	
	//adjust best_cost, and recursively call my ant items
	//parent_changed_cost;//deduction-idenpendent parent item cost
	private void rerankHyperEdge(HGNode parent_item, HyperEdge dt){
		dt.best_cost = get_deduction_merit(dt, parent_item);
		
		if(dt.get_ant_items()!=null){
			for(HGNode ant_it : dt.get_ant_items()){
				rerankHGNode(ant_it);
				dt.best_cost += ant_it.best_deduction.best_cost;
			}
		}
	}
	
}
