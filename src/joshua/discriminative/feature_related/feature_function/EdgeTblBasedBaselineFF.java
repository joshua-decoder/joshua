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
	
	private HashMap<HyperEdge, Double> hyperEdgeBaselineLogPTbl = new HashMap<HyperEdge, Double>() ;
	private HashSet<HGNode> processedNodesTtbl = new HashSet<HGNode>();
	
	
	/*hyperEdgeBaselineLogPTbl should contain **logP**; not cost
	 * */
	public EdgeTblBasedBaselineFF(final int featID, final double weight) {
		super(weight, -1, featID);//TODO: owner
		
	}
	
	public double estimateLogP(Rule rule, int sentID) {
		logger.severe("unimplement function");
		System.exit(1);
		return 0;
	}
	
	@Override
	public double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return hyperEdgeBaselineLogPTbl.get(edge);
	}

	@Override
	public double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return hyperEdgeBaselineLogPTbl.get(edge);
	}
	
	
//	==========================================
	public HashMap<HyperEdge, Double>  collectTransitionLogPs(HyperGraph hg){
		hyperEdgeBaselineLogPTbl.clear();
		processedNodesTtbl.clear();		
		collectTransitionCosts(hg.goalNode);
	
		processedNodesTtbl.clear();
		return hyperEdgeBaselineLogPTbl;
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
		hyperEdgeBaselineLogPTbl.put(dt, dt.getTransitionLogP(false));//get baseline score	
	
		if(dt.getAntNodes()!=null){
			for(HGNode antNode : dt.getAntNodes()){
				collectTransitionCosts(antNode);
			}
		}
	}	
	
	
}
