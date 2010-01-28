/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.discriminative.training.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

/**
 * This class implements general ways of spliting the hypergraph based on coarse-to-fine idea
 * 
 * input is a hypergraph
 * output is another hypergraph that has changed state structures.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com> (Johns Hopkins University)
 * @version $LastChangedDate: 2009-03-05 12:37:57 -0500$
 */
public abstract class RefineHG<DPS extends DPState> {

	//Key: item; Value: a list of split virtual items
	HashMap<HGNode, List<RefinedNode>> splitRefinedNodesTbl =  new HashMap<HGNode, List<RefinedNode>> ();
	
	//number of items or deductions after splitting the hypergraph
	public int numRefinedNodes = 0;
	public int numRefinedEdges = 0;
	
	
	
	//=========================== abstract methods and class that must be implemented
	protected abstract DPS computeState(HGNode originalParentItem, HyperEdge originalEdge, List<HGNode> antVirtualItems);
	
	/**children will at list change the ant-nodes of the edge, it may also add more fields in the edge
	 **/
	protected abstract HyperEdge createNewHyperEdge(HyperEdge originalEdge, List<HGNode> antVirtualItems, DPS dps);
	
	
	
	
//	=== all the functions should be called after running split_hg(), before clearing g_tbl_split_virtual_items
	public double getBestGoalCost(HyperGraph hg, HashMap<HGNode, List<RefinedNode>> splitVirtualItemsTbl){
		double res = getVirtualGoalItem(hg, splitVirtualItemsTbl).bestHyperedge.bestDerivationLogP;
		//System.out.println("best bleu is " +res);
		return res;
	}

	
//	============= split hg =============	
	protected  HyperGraph splitHG(HyperGraph hg){		
		
		splitRefinedNodesTbl.clear(); 
		numRefinedNodes = 0;
		numRefinedEdges = 0;		
		
		splitNode(hg.goalNode);
		HGNode newGoal = getVirtualGoalItem(hg, splitRefinedNodesTbl);
		printInfo();
		
		HyperGraph newHG = new HyperGraph(newGoal, numRefinedNodes, numRefinedEdges, hg.sentID, hg.sentLen);
		return newHG;
	}	
	

	private RefinedNode getVirtualGoalItem(HyperGraph original_hg, HashMap<HGNode, List<RefinedNode>> splitVirtualItemsTbl){
		List<RefinedNode> virtualItems =  splitVirtualItemsTbl.get(original_hg.goalNode);
		if(virtualItems.size()!=1){
			System.out.println("number of virtual goal items is not equal to one"); 
			System.exit(0);
		}
		return virtualItems.get(0);
	}
	
	//for each original Item, get a list of VirtualItem
	private void splitNode(HGNode it){
		if(splitRefinedNodesTbl.containsKey(it))
			return;//already processed
		
		HashMap<String, RefinedNode> refinedNodeSigs = new HashMap<String, RefinedNode>();
		
		//### recursive call on each hyperedge
		if( speedUpNode(it) ){
			for(HyperEdge dt : it.hyperedges){					
				splitHyperedge(dt, refinedNodeSigs, it);
			}
		}
		//### item-specific operation
		ArrayList<RefinedNode> refinedNodes = new ArrayList<RefinedNode>();//a list of items result by splitting me
		for(Iterator iter = refinedNodeSigs.keySet().iterator(); iter.hasNext();)
			refinedNodes.add(refinedNodeSigs.get(iter.next()));
		
		splitRefinedNodesTbl.put(it,refinedNodes);
		numRefinedNodes += refinedNodes.size();
		
		/*
		if(refinedNodes.size()>1){
			System.out.println("num of split items is " + refinedNodes.size());
			//System.out.println("refinedNodeSigs= " + refinedNodeSigs.keySet());
			//System.exit(1);
			//get_best_virtual_score(it);//debug
		}*/
	}	
	
	private void splitHyperedge(HyperEdge curEdge, HashMap<String, RefinedNode> virtualItemSigs, HGNode parentNode){
		if(speedUpHyperedge(curEdge)==false) 
			return;//no need to continue
		
		//### recursively split all my ant items, get a l_split_items for each original item
		if(curEdge.getAntNodes()!=null)
			for(HGNode ant_it : curEdge.getAntNodes())
				splitNode(ant_it);
		
		//### recombine the hyperedge
		redoCombine(curEdge, virtualItemSigs, parentNode);
	}	
	
	public void printInfo(){
		System.out.println("numRefinedNodes="+numRefinedNodes);
		System.out.println("numRefinedEdges="+numRefinedEdges);
	}
	
	
	/*This procedure does
	 * (1) create a new hyperedge (based on curEdge and ant_virtual_item)
	 * (2) find whether an Item can contain this hyperedge (based on virtualItemSigs which is a hashmap specific to a parent_item)
	 * 	(2.1) if yes, add the hyperedge, 
	 *  (2.2) otherwise
	 *  	(2.2.1) create a new item
	 *		(2.2.2) and add the item into virtualItemSigs
	 **/
	
	private void redoCombine(HyperEdge originalEdge, HashMap<String, RefinedNode> refinedNodeSigs, HGNode originalParentHGNode){
		
		List<HGNode> originalAntNodes = originalEdge.getAntNodes();
		
		if(originalAntNodes!=null){					
			if(originalAntNodes.size()==1){//arity: one
				HGNode it = originalAntNodes.get(0);
				List<RefinedNode> virtualItems = splitRefinedNodesTbl.get(it);				
				for(RefinedNode antVirtualItem: virtualItems){
					ArrayList<HGNode> antRefinedNodes = new ArrayList<HGNode>();//used in combination
					antRefinedNodes.add(antVirtualItem);
					
					handleOneCombination(originalParentHGNode, originalEdge, antRefinedNodes, refinedNodeSigs);
				}
				
			}else if(originalAntNodes.size()==2){//arity: two
				HGNode it1 = originalAntNodes.get(0);
				HGNode it2 = originalAntNodes.get(1);
				List<RefinedNode> virtualItems1 = splitRefinedNodesTbl.get(it1);
				List<RefinedNode> virtualItems2 = splitRefinedNodesTbl.get(it2);
				
				/*if(virtualItems1.size()>1 && virtualItems2.size()>1){
					System.out.println("virtualItems1.size " + virtualItems1.size());
					System.out.println("virtualItems2.size " + virtualItems2.size());
					System.out.println("combination.size " + virtualItems1.size()*virtualItems2.size());
					
				}*/
				
				for(RefinedNode virtualIt1: virtualItems1){
					for(RefinedNode virtualIt2: virtualItems2){
						ArrayList<HGNode> antRefinedNodes = new ArrayList<HGNode>();//used in combination
						antRefinedNodes.add(virtualIt1);
						antRefinedNodes.add(virtualIt2);
						
						handleOneCombination(originalParentHGNode, originalEdge, antRefinedNodes, refinedNodeSigs);
					}					
				}
			}else{
				System.out.println("Sorry, we can only deal with rules with at most TWO non-terminals");
				System.exit(0);
			}	
			
		}else{//arity: zero; axiom case: no nonterminal

			handleOneCombination(originalParentHGNode, originalEdge, null, refinedNodeSigs);
		}

	}
	
	private void handleOneCombination(HGNode originalParentHGNode, HyperEdge originalEdge, ArrayList<HGNode> antRefinedNodes, HashMap<String, RefinedNode> refinedNodeSigs){
		
		DPS dps = computeState(originalParentHGNode, originalEdge,  antRefinedNodes);
		HyperEdge tEdge = createNewHyperEdge(originalEdge, antRefinedNodes, dps);
		addHyperedge(originalParentHGNode, refinedNodeSigs, tEdge, dps);
	}
	
	
	
	//refinedNodeSigs is specific to parentNode
	private  void addHyperedge(HGNode parentNode, HashMap<String, RefinedNode> refinedNodeSigs, HyperEdge edge, DPS dpstate){
		
		if(edge==null) {
			System.out.println("hyperege is null"); 
			System.exit(0);
		}
		
		//String sig = RefinedNode.getSignature(dpstate);
		String sig = dpstate.getSignature(true);
		RefinedNode tRefinedNode = refinedNodeSigs.get(sig);
		
		if(tRefinedNode!=null){
			tRefinedNode.addHyperedgeInNode(edge, dpstate);
		}else{
			tRefinedNode = new RefinedNode(parentNode.i, parentNode.j, parentNode.lhs, edge, dpstate);
			refinedNodeSigs.put(sig, tRefinedNode );
		}		
		
		numRefinedEdges++;
	}
	
	
	//return false if we can skip the item;
	protected  boolean speedUpNode(HGNode it){
		return true;//e.g., if the lm state is not valid, then no need to continue
	}
	
	//return false if we can skip the deduction;
	protected  boolean speedUpHyperedge(HyperEdge dt){
		return true;// if the rule state is not valid, then no need to continue	
	}
	

	
	
	
	/*In general, variables of items
	 * (1) list of hyperedges
	 * (2) best hyperedge
	 * (3) DP state
	 * (4) signature (operated on part/full of DP state)
	 * */
	
	public static class RefinedNode extends HGNode {
		
		//dynamic programming state: not all the variables in dpState are in the signature
		public DPState dpState;
		
		public RefinedNode(int i, int j, int lhs, HyperEdge init_hyperedge, DPState dstate){
			super(i, j, lhs, null, init_hyperedge,  0);
			this.dpState = dstate;				
		}
		
		public void addHyperedgeInNode(HyperEdge dt, DPState dstate) {
			if (null == hyperedges) {
				hyperedges = new ArrayList<HyperEdge>();
			}
			hyperedges.add(dt);
			if (null == bestHyperedge || bestHyperedge.bestDerivationLogP > dt.bestDerivationLogP) {
				bestHyperedge = dt; //no change when tied
				
				/** since not all variables in dp_state will go into the signature, it is possible that the signature
				 * of two hypereges is the same, but the dp_state is different, so we should update the dp_state whenever 
				 * best_cost is changed; otherwise the oracle blue score found will be very bad
				 * */
				dpState = dstate;
			}
		}
		
		
		/*
		// not all the variable in dp_state are in the signature
		public String getSignature(){
			return getSignature(dpState);
		}
		
		public static String getSignature(DPState dstate){
			return dstate.getSignature();
		}*/
	}
	
		
		
}
