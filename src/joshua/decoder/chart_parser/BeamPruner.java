package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


import joshua.decoder.Support;


/**(1) relative threshold pruning 
 * when the cost of a new edge (or an existing node) is worse than the best by a threshold, prune it
 * (2) when the number of node is greater than a threshold, prune some nodes
 * (3) maintain bestCost and nodesHeap
 * */

public class BeamPruner<Obj extends Prunable> {

	/**Ideally, if the cost of an object changes, it should be 
	 * removed from the heap (linear time), and re-insert it (logN time). But, this is 
	 * too expensive. So, instead, we will mark an object as dead, and simply add
	 * a new object with the updated cost.
	 * */
	
	//num of corrupted items in this.heapItems
	private int qtyDeadItems = 0;
		
	
	/** cutoff = bestItemCost + relative_threshold */
	private double cutoffCost = Integer.MAX_VALUE;
	
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
	public boolean relativeThresholdPrune(double totalCost) {		
		return (totalCost >= this.cutoffCost);
	}
	
	
	public int incrementDeadObjs(){
		return ++ this.qtyDeadItems;
	}

	/**This will add the object, update the cutOff cost,
	 * and trigger pruningObjs*/
	public List<Obj> addOneObjInHeapWithPrune(Obj obj){		
		this.nodesHeap.add(obj);
		updateCutoffCost(obj.getPruneCost());
		List<Obj> prunedNodes = pruningObjs();
		return prunedNodes;
	}
	
	public double getCutCost(){
		return this.cutoffCost;
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
				
				/**This pruning is necessary as the bestCost may have been changed after the object is
				 * inserted into the heap*/
				|| relativeThresholdPrune( this.nodesHeap.peek().getPruneCost() ) ) { // relative threshold pruning
			
			Obj worstNode = this.nodesHeap.poll();
			if ( worstNode.isDead() ) { //dead object
				this.qtyDeadItems--;
			} else {
				prunedObjs.add(worstNode);
			}
			
		}
		
		/**if the heap reaches its capacity, we will do more
		 * aggressive threshold pruning, by reduce the cutoffCost
		 * */
		if (this.nodesHeap.size() - this.qtyDeadItems >= maxNumObjs) {
			greedyUpdateCutoffCost(this.nodesHeap.peek().getPruneCost());
		}
		return prunedObjs;
	}
	
	
	
	private void updateCutoffCost(double newCost){		
		this.cutoffCost = 
			Support.findMin(this.cutoffCost, newCost + relativeThreshold);		
	}
	
	/**if the heap is already full, then we do more
	 * aggressive threshold pruning
	 * */
	private void greedyUpdateCutoffCost(double worstHeapCost){
		this.cutoffCost = Support.findMin(
				this.cutoffCost,
				worstHeapCost + EPSILON);
	}
	
}
