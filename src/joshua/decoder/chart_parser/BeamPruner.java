package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.Support;


/**(1) relative threshold pruning 
 * when the logP of a new edge (or an existing node) is worse than the best by a threshold, prune it
 * (2) when the number of node is greater than a threshold, prune some nodes
 * (3) maintain bestLogP and nodesHeap
 * */

public class BeamPruner<Obj extends Prunable> {

	/**Ideally, if the goodness of an object changes, it should be 
	 * removed from the heap (linear time), and re-insert it (logN time). But, this is 
	 * too expensive. So, instead, we will mark an object as dead, and simply add
	 * a new object with the updated goodness.
	 * */
	
	//num of corrupted items in this.heapItems
	private int qtyDeadItems = 0;
		
	
	/** cutoff = bestItemLogP - relative_threshold */
	private double cutoffLogP = Double.NEGATIVE_INFINITY;
	
	private double relativeThreshold;
	
	private int maxNumObjs;
	

	/**MIN-HEAP, we put the worst-cost item at the top
	 * of the heap by manipulating the compare function
	 * The only purpose of nodesHeap tis to help decide which
	 * nodes should be removed during pruning.
	 */
	private PriorityQueue<Obj> nodesHeap; 
		//=new PriorityQueue<HGNode>(1, HGNode.negtiveCostComparator);
	
	
	
//	===============================================================
//	 Static fields
//	===============================================================
		
	private static final double EPSILON = 0.000001;
	
	private static final Logger logger = 
		Logger.getLogger(BeamPruner.class.getName());
	
	
	public BeamPruner(PriorityQueue<Obj> nodesHeap, double relativeThreshold, int maxNumObjs){
		this.nodesHeap = nodesHeap;		
		this.relativeThreshold = relativeThreshold;
		this.maxNumObjs = maxNumObjs;
	}
	
		
	/**threshold cutoff pruning
	 * */
	public boolean relativeThresholdPrune(double logP) {		
		return (logP <= this.cutoffLogP);
	}
	
	
	public int incrementDeadObjs(){
		return ++ this.qtyDeadItems;
	}

	/**This will add the object, update the cutOff logP,
	 * and trigger pruningObjs*/
	public List<Obj> addOneObjInHeapWithPrune(Obj obj){		
		this.nodesHeap.add(obj);
		//System.out.println("Add: " + obj.getPruneLogP()+ "; " +((HGNode)obj).i + "; " + ((HGNode)obj).j + "; best= " + ((HGNode)obj).bestHyperedge.bestDerivationLogP);
		updateCutoffLogP(obj.getPruneLogP());
		List<Obj> prunedNodes = pruningObjs();
		return prunedNodes;
	}
	

	/**This will add the object, update the cutOff logP,*/
	public List<Obj> addOneObjInHeapWithoutPrune(Obj obj){		
		this.nodesHeap.add(obj);
		//System.out.println("Add: " + obj.getPruneLogP()+ "; " +((HGNode)obj).i + "; " + ((HGNode)obj).j + "; best= " + ((HGNode)obj).bestHyperedge.bestDerivationLogP);
		updateCutoffLogP(obj.getPruneLogP());

		return null;
	}
	
	public double getCutoffLogP(){
		return this.cutoffLogP;
	}	

	
	
	/**pruning at the object level
	 **/
	private List<Obj> pruningObjs() {
		if (logger.isLoggable(Level.FINEST)) 
			logger.finest(String.format("Pruning: heap size: %d; n_dead_items: %d", this.nodesHeap.size(),this.qtyDeadItems));
		
		if (this.nodesHeap.size() == this.qtyDeadItems) {
			this.nodesHeap.clear();
			this.qtyDeadItems = 0;
			
			/**since all these objects are already dead, 
			 * we do not consider them prunned objectives, so return null*/
			
			return null;
		}
		
		
		List<Obj> prunedObjs = new ArrayList<Obj>();		
		
		while (this.nodesHeap.size() - this.qtyDeadItems > maxNumObjs //bin limit pruning				
				
				/**This pruning is necessary as the bestLogP may have been changed after the object is
				 * inserted into the heap*/
				|| relativeThresholdPrune( this.nodesHeap.peek().getPruneLogP() ) ) { // relative threshold pruning
			
			Obj worstNode = this.nodesHeap.poll();
			if ( worstNode.isDead() ) { //dead object
				this.qtyDeadItems--;
			} else {
				prunedObjs.add(worstNode);
			}
			
		}
		
		/**if the heap reaches its capacity, we will do more
		 * aggressive threshold pruning, by increase the cutoffLogP
		 * */
		if (this.nodesHeap.size() - this.qtyDeadItems >= maxNumObjs) {
			greedyUpdateCutoffLogP(this.nodesHeap.peek().getPruneLogP());
		}
		return prunedObjs;
	}
	
	
	
	private void updateCutoffLogP(double newLogP){		
		this.cutoffLogP = 
			Support.findMax(this.cutoffLogP, newLogP - relativeThreshold);		
	}
	
	/**if the heap is already full, then we do more
	 * aggressive threshold pruning
	 * */
	private void greedyUpdateCutoffLogP(double worstHeapLogP){
		this.cutoffLogP = Support.findMax(
				this.cutoffLogP,
				worstHeapLogP - EPSILON);
	}
	
}
