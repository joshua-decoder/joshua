package joshua.discriminative.feature_related.feature_function;



import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

import joshua.decoder.ff.DefaultStatelessFF;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

public class EdgeTblBasedBaselineFF extends DefaultStatelessFF {
	
	private static Logger logger = Logger.getLogger(EdgeTblBasedBaselineFF.class.getName());
	
	private HashMap<HyperEdge, Double> hyperEdgeBaselineCostTbl = new HashMap<HyperEdge, Double>() ;
	private HashSet<HGNode> processedNodesTtbl = new HashSet<HGNode>();
	private double sumBaselineCost = 0;
	
	/*baseline_feat_tbl should contain **cost**; not prob
	 * */
	public EdgeTblBasedBaselineFF(final int featID, final double weight) {
		super(weight, -1, featID);//TODO: owner
		
	}
	
	public double estimate(Rule rule, int sentID) {
		logger.severe("unimplement function");
		System.exit(1);
		return 0;
	}
	
	@Override
	public double transition(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		//return this.baselineFeatTbl.get(edge);
		//System.out.println("cost:" + edge.getTransitionCost(true));
		//return edge.getTransitionCost(true);
		return hyperEdgeBaselineCostTbl.get(edge);
	}

	@Override
	public double finalTransition(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		//return this.baselineFeatTbl.get(edge);
		return hyperEdgeBaselineCostTbl.get(edge);
	}
	
	
//	==========================================
	public HashMap<HyperEdge, Double>  collectTransitionCosts(HyperGraph hg){
		hyperEdgeBaselineCostTbl.clear();
		processedNodesTtbl.clear();		
		sumBaselineCost = 0;
		collectTransitionCosts(hg.goalNode);
		logger.info("sumBaselineCost="+sumBaselineCost);
		processedNodesTtbl.clear();
		return hyperEdgeBaselineCostTbl;
	}
	
//	item: recursively call my children deductions, change pointer for best_deduction, and remember changed_cost 
	private  void collectTransitionCosts(HGNode it ){
		if(processedNodesTtbl.contains(it))	
			return;
		processedNodesTtbl.add(it);		
		for(HyperEdge dt : it.hyperedges){					
			collectTransitionCosts(it, dt);
		}		
	}
//	adjust best_cost, and recursively call my ant items
	//parent_changed_cost;//deduction-idenpendent parent item cost
	private void collectTransitionCosts(HGNode parentNode, HyperEdge dt){
		hyperEdgeBaselineCostTbl.put(dt, dt.getTransitionCost(false));//get baseline score	
		sumBaselineCost +=  dt.getTransitionCost(false);
		if(dt.getAntNodes()!=null){
			for(HGNode antNode : dt.getAntNodes()){
				collectTransitionCosts(antNode);
			}
		}
	}	
	
	
}
