package edu.jhu.joshua.VariationalDecoder;

import java.util.ArrayList;
import java.util.HashMap;

import joshua.decoder.ff.FFTransitionResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

/*This class implements functions to rank HG based on a bunch of feature functions
 *On the other hand, it does not change the topology of the HG
 **/
//### change the best_pointer and best_cost in hypergraph in hg

public class RankHG {
	static private HashMap g_tbl_processed_items =  new HashMap();//help to tranverse a hypergraph	
	static ArrayList<FeatureFunction> l_feat_functions;
	static int num_changed_best_hyperedge = 0;
	
	
	static public void rankHG(HyperGraph hg,  ArrayList<FeatureFunction> feat_functions){
		l_feat_functions = feat_functions;
		g_tbl_processed_items.clear();
		rankHGNode(hg.goal_item);
		System.out.println("number of nodes whose best hyperedge changed is " + num_changed_best_hyperedge + " among total number of nodes " + g_tbl_processed_items.size() );
		g_tbl_processed_items.clear();
		num_changed_best_hyperedge = 0;		
	}
	
	
	//item: recursively call my children deductions, change pointer for best_deduction, and remember changed_cost 
	static private  void rankHGNode(HGNode it ){
		if(g_tbl_processed_items.containsKey(it))	return;
		g_tbl_processed_items.put(it,1);
		
		//### recursively call my children deductions, change pointer for best_deduction
		HyperEdge old_best_dt = it.best_deduction; 
		it.best_deduction=null;
		for(HyperEdge dt : it.l_deductions){					
			rankHyperEdge(it, dt);
			if(it.best_deduction==null || dt.best_cost < it.best_deduction.best_cost)
				it.best_deduction = dt;//prefer smaller cost
		}
		if(old_best_dt!=it.best_deduction)
			num_changed_best_hyperedge++;
	}
	
	
	//adjust best_cost, and recursively call my ant items
	//parent_changed_cost;//deduction-idenpendent parent item cost
	static private void rankHyperEdge(HGNode parent_item, HyperEdge dt){
		dt.best_cost =0 ;
		
		//transition cost
		for(FeatureFunction m : l_feat_functions ){
			double t_res =0;						
			if(dt.get_rule()!=null){//deductions under goal item do not have rules
				FFTransitionResult tem_tbl =  HyperGraph.computeTransition(dt, m, -1, -1);
				t_res = tem_tbl.getTransitionCost();
			}else{//final transtion
				t_res = HyperGraph.computeFinalTransition(dt, m);
			}
			dt.best_cost  += t_res * m.getWeight();
		}			
		
		//ant cost
		if(dt.get_ant_items()!=null){
			for(HGNode ant_it : dt.get_ant_items()){
				rankHGNode(ant_it);
				dt.best_cost += ant_it.best_deduction.best_cost;
			}
		}
	}
	

}
