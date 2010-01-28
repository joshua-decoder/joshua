package joshua.discriminative.semiring_parsingv2;


import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.discriminative.semiring_parsingv2.pmodule.PModule;
import joshua.discriminative.semiring_parsingv2.semiring.Semiring;


/**This implements the algorithm of Figure-4 in the emnlp paper (Li and Eisner, 2009).
 * To compute a new second-order statistics, we just need to specifiy a weight for each hyperedge.
 * Specifically, we need to implement the following four functions
 * 1. createNewKWeight
 * 2. createNewXWeight
 * 3. getEdgeKWeight
 * 4. getEdgeXWeight
 * */

/** In addition to run the regular inside-outside algorithm,
 * this also collects the posterior count along with the outside parsing pass
 * */

/**should be an ExpectationSemiring<K,X>*/
public abstract class DefaultIOParserWithXLinearCombinator<K extends Semiring<K>, X extends PModule<K,X>> 
extends DefaultInsideOutsideSemiringParser<K> {
	
	protected X goalX; //the x weight at the root, this is the total sum of posterior
		
	public DefaultIOParserWithXLinearCombinator() {
		super();
		
		goalX = createNewXWeight();
		goalX.setToZero();
	}
	

	protected abstract X getEdgeXWeight(HyperEdge dt, HGNode parent_item);
	
	protected abstract X createNewXWeight();

	/**since K and X are not concrete yet, only
	 * the extended class knows how to normalize*/
	public abstract void normalizeGoal();
	
	
	/** hyperedgeX = hyperedgeX* exclusiveWeight
	 * */
	final protected void moduleMultiSemiring(X hyperedgeX, K exclusiveWeight){
		hyperedgeX.multiSemiring(exclusiveWeight);
	}


	/**for correctness and saving memory, 
	 * external class should call this method*/
	public  void clearState(){
		super.clearState();
		goalX.setToZero();
	}
	
	public void runInsideOutside(){
		insideEstimationOverHG(); 
		outsideEstimationOverHG();
	}
	
	

	/**this will run outside, 
	 * and collect posterior counts*/
	@Override
	final protected void outsideEstimationOverHyperedge(HyperEdge dt, HGNode parentNode, K parentNodeOutsideWeight){
	
		//==== compute the exclusive weight in the P-semiring
		K exclusiveKWeight =  createNewKWeight();//\overline{k_e}
		exclusiveKWeight.setToOne();
		exclusiveKWeight.multi(parentNodeOutsideWeight);
		
		//we do not need to compute outside prob if no ant nodes
		if(dt.getAntNodes()!=null){
			//=== deduction specific prob
			K edgeWeight = getEdgeKWeight(dt, parentNode);
			
			//=== recursive call on each ant node
			for(HGNode antNode : dt.getAntNodes()){
				exclusiveKWeight.multi( insideSemiringWeightsTable.get(antNode) );
				outsideEstimationOverNode(antNode, parentNodeOutsideWeight, dt, edgeWeight);
			}
		}
		
		//== collect posterior count: x += \overline{k_e} x_e
		X edgeX = getEdgeXWeight(dt, parentNode);
		moduleMultiSemiring(edgeX, exclusiveKWeight);
		goalX.add( edgeX );
	
	}
	
	public void printGoalX(){
		goalX.printInfor();
	}
	
	public X getGoalX(){
		return goalX;
	}
	
}
