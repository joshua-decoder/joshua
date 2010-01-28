package joshua.discriminative.semiring_parsingv2;

import java.util.HashMap;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.semiring_parsingv2.semiring.Semiring;

/**This implements the algorithm of Figure-2 in the emnlp paper (Li and Eisner, 2009).
 * To compute a new first-order statistics, we just need to specifiy a weight for each hyperedge.
 * Specifically, we need to implement the following two functions
 * 1. createNewSemiringMember
 * 2. getHyperedgeSemiringWeight
 * */


/** semiring parsing on a Hypergraph, 
 *  The semiring weight at each hyperedge is specificed by getHyperedgeSemiringWeight()
 */

public abstract class DefaultInsideSemiringParser<K extends Semiring<K>> {

	protected HashMap<HGNode, K> insideSemiringWeightsTable ;

	protected HyperGraph hg;
	

	public DefaultInsideSemiringParser(){
		insideSemiringWeightsTable =  new HashMap<HGNode, K>();
	}
	

	public void setHyperGraph(HyperGraph hg_){
		this.clearState();
		this.hg = hg_;
	}
	
	

	/**for correctness and saving memory, 
	 * external class should call this method*/
	public  void clearState(){
		insideSemiringWeightsTable.clear();
	}
	
	
	
	
	/** This is where the semiring and hyperedge weights get specified
	 * */
	protected abstract K getEdgeKWeight(HyperEdge dt, HGNode parent_item);	
	protected abstract K createNewKWeight();

	
		
//================== bottomn-up insdide estimation ===============	

	public  K getGoalK(){
		return insideSemiringWeightsTable.get(hg.goalNode);	
	}
	
	public void insideEstimationOverHG(){
		insideSemiringWeightsTable.clear(); 		
		insideEstimationOverNode(hg.goalNode);
	}
	
	protected K insideEstimationOverNode(HGNode it){
		
		if(insideSemiringWeightsTable.containsKey(it))
			return  insideSemiringWeightsTable.get(it);
		
		K res = createNewKWeight();
		res.setToZero();
		
		//=== recursive call on each hyperedge
		for(HyperEdge edge : it.hyperedges){
			K edgeWeight = insideEstimationOverHyperedge(edge, it);
			res.add(edgeWeight);
		}		

		insideSemiringWeightsTable.put(it,res);
		return res;
	}

	private K insideEstimationOverHyperedge(HyperEdge dt, HGNode parentNode){
		K res = createNewKWeight();
		res.setToOne();
		 
		//=== recursive call on each ant item
		if(dt.getAntNodes()!=null)
			for(HGNode antNode : dt.getAntNodes()){
				K nodeWeight = insideEstimationOverNode(antNode);
				res.multi(nodeWeight);				
			}
				
		//=== hyperedge operation
		K edgeWeight = getEdgeKWeight(dt, parentNode);
		res.multi(edgeWeight);	
		return res;
	}
//=================== end inside estimation	

		
}
