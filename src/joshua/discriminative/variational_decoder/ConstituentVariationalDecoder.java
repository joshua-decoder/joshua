package joshua.discriminative.variational_decoder;


import java.util.HashMap;


import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.TrivialInsideOutside;




public class ConstituentVariationalDecoder extends TrivialInsideOutside {
    HashMap g_tbl_processed_items = new HashMap();

    
    //return changed hg
    public HyperGraph decoding(HyperGraph hg){
            runInsideOutside(hg, 0, 1, 1.0);//ADD_MODE=0=sum; LOG_SEMIRING=1;
            rerankHG(hg);
            clearState();                      
            return hg;
    }

    //  #################### rerank HG #######
	private void rerankHG(HyperGraph hg){
		//### change the best_pointer and best_cost in hypergraph in hg
		g_tbl_processed_items.clear();
		rerankHGNode(hg.goalNode);
		g_tbl_processed_items.clear();
	}

	
	//item: recursively call my children deductions, change pointer for best_deduction, and remember changed_cost 
	private  void rerankHGNode(HGNode it ){
		if(g_tbl_processed_items.containsKey(it))	return;
		g_tbl_processed_items.put(it,1);
		
		//### recursively call my children deductions, change pointer for best_deduction
		it.bestHyperedge=null;
		for(HyperEdge dt : it.hyperedges){					
			rerankHyperEdge(it, dt);
			it.semiringPlus(dt);
		}
	}
	
	
	//adjust best_cost, and recursively call my ant items
	//parent_changed_cost;//deduction-idenpendent parent item cost
	private void rerankHyperEdge(HGNode parent_item, HyperEdge dt){
		//dt.best_cost = get_deduction_merit(dt, parent_item);
		dt.bestDerivationLogP = Math.log(getEdgePosteriorProb(dt, parent_item));
		
		if(dt.getAntNodes()!=null){
			for(HGNode ant_it : dt.getAntNodes()){
				rerankHGNode(ant_it);
				dt.bestDerivationLogP += ant_it.bestHyperedge.bestDerivationLogP;
			}
		}
		dt.getTransitionLogP(true);
	}
	
}
