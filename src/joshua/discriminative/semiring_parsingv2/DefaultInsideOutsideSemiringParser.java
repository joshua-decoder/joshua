package joshua.discriminative.semiring_parsingv2;

import java.util.HashMap;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.semiring_parsingv2.semiring.Semiring;




/**This implements the algorithm of Figure-3 in the emnlp paper (Li and Eisner, 2009),
 * in addition to the algorithm of Figure-2 that is implemented by the parent class DefaultInsideSemiringParser.
 * We may not want to extend this class directly. Instead, we will extend its child class DefaultIOParserWithXLinearCombinator,
 * which runs inside-outside and also collects the posterior counts
 * */

public abstract class DefaultInsideOutsideSemiringParser<K extends Semiring<K>> 
extends DefaultInsideSemiringParser<K> {
	
	private HashMap<HGNode, K> outsideSemiringWeightsTable ;	
	
	/**
	 * for each node, remember how many hyperedges pointering
	 * to me, this is needed for outside estimation. 
	 * An node will recursive call its incoming
	 * hyperedges to do outside-estimation only after it itself
	 * is done with outside estimation, this is necessary
	 * because the outside estimation of the node's incoding edges 
	 * require the node's outside value
	 */
	private HashMap<HGNode,Integer> numParentHyperedgesTable;
	

	public DefaultInsideOutsideSemiringParser() {
		super();
		outsideSemiringWeightsTable =  new HashMap<HGNode, K>();
		numParentHyperedgesTable = new HashMap<HGNode,Integer>();
	}

	
	/**for correctness and saving memory, 
	 * external class should call this method*/
	@Override
	public  void clearState(){
		super.clearState();
		outsideSemiringWeightsTable.clear();
		numParentHyperedgesTable.clear();
	}

	@Override
	protected K insideEstimationOverNode(HGNode it){	
		rememberNumParentHyperedges(it);
		return super.insideEstimationOverNode(it);
	}
	
//	=============================== top-downn outside estimation ====================== 
	public void outsideEstimationOverHG(){	
		outsideSemiringWeightsTable.clear(); 
		
		K initWeight = createNewKWeight();
		initWeight.setToOne();
		outsideSemiringWeightsTable.put(hg.goalNode, initWeight);//initialize
		
		for(HyperEdge dt : hg.goalNode.hyperedges)
			outsideEstimationOverHyperedge(dt, hg.goalNode, initWeight);	
	}
	
	final protected void outsideEstimationOverNode(HGNode node, K parentNodeOutsideWeight, HyperEdge parentEdge, K parentEdgeWeight){
		
		Integer numCalled = numParentHyperedgesTable.get(node);
		if (null == numCalled || 0 == numCalled) {
			System.out.println("num_called="+numCalled);
			throw new RuntimeException("un-expected call (the number of calls is greater than the number of parent hyperedges), must be wrong");
		}
		numParentHyperedgesTable.put(node, numCalled-1);
		
		//====== compute: outside(v) * k_e * product of inside prob of sibling nodes
		K additionalOutsideProb = createNewKWeight();
		additionalOutsideProb.setToOne();	
		
		//=== upper item's outside weight
		//K outsideProbNode = outsideSemiringWeightsTable.get(parentNode);//outside prob
		additionalOutsideProb.multi(parentNodeOutsideWeight);//outside(v)
		
		//=== parent hyperedge weight
		additionalOutsideProb.multi(parentEdgeWeight);//k_e
		
		//=== sibing specifc inside weights
		if(parentEdge.getAntNodes()!=null && parentEdge.getAntNodes().size()>1)
			for(HGNode antNode : parentEdge.getAntNodes()){
				if(antNode != node){
					K nodeInsideProb = insideSemiringWeightsTable.get(antNode);//inside prob
					additionalOutsideProb.multi(nodeInsideProb);
				}				
			}
				
		//=== add to old prob 
		K oldOutsideProb  = outsideSemiringWeightsTable.get(node);
		if (oldOutsideProb == null) {
			oldOutsideProb =  createNewKWeight();
			oldOutsideProb.setToZero();
		}		
		
		oldOutsideProb.add(additionalOutsideProb);		
		
		outsideSemiringWeightsTable.put(node, oldOutsideProb);
		
		//=== recursive call on each deduction
		if( numCalled-1<=0){//i am done
			for(HyperEdge dt : node.hyperedges){
				outsideEstimationOverHyperedge(dt, node, oldOutsideProb);
			}
		}
	}
	
	
	protected void outsideEstimationOverHyperedge(HyperEdge dt, HGNode parentNode, K parentNodeOutsideWeight){
	
		//we do not need to compute outside prob if no ant items
		if(dt.getAntNodes()!=null){
			//=== deduction specific prob
			K edgeWeight = getEdgeKWeight(dt, parentNode);
			
			//=== recursive call on each ant item
			for(HGNode antNode : dt.getAntNodes()){
				outsideEstimationOverNode(antNode, parentNodeOutsideWeight, dt, edgeWeight);
			}
		}
	}
	//=============================== end outside estimation	
	
	
	
	//================ get number of hyperedges that point to me
	/**This function will be used to get the number of parent hyperedges, 
	 * which in turn will be used for outside estimation*/
	final private void rememberNumParentHyperedges(HGNode node){
		//System.out.println("called");
		Integer numCalled = (Integer)numParentHyperedgesTable.get(node);
		if (null == numCalled) {
			numParentHyperedgesTable.put(node, 1);
		} else {
			numParentHyperedgesTable.put(node, numCalled+1);
		}
	}
}
