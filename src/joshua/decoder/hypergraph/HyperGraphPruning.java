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
package joshua.decoder.hypergraph;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.hypergraph.HyperGraph;

import java.util.HashMap;


/**
 * during the pruning process, many Item/Deductions may not be
 * explored at all due to the early-stop in pruning_deduction
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class HyperGraphPruning extends TrivialInsideOutside {
	
	HashMap<HGNode,Boolean> processedNodesTbl = new HashMap<HGNode,Boolean>();
	double bestLogProb;//viterbi unnormalized log prob in the hypergraph
	
	boolean ViterbiPruning = false;//Viterbi or Posterior pruning
	
	boolean fixThresholdPruning = true;
	double THRESHOLD_GENERAL = 10;//if the merit is worse than the best_log_prob by this number, then prune
	double THRESHOLD_GLUE = 10;//if the merit is worse than the best_log_prob by this number, then prune
	
	
	int numSurvivedEdges = 0;
	int numSurvivedNodes = 0;
	
	int glueGrammarOwner=0;//TODO
	
	
	public HyperGraphPruning(SymbolTable symbolTable, boolean fixThreshold, double thresholdGeneral, double thresholdGlue){
		fixThresholdPruning = fixThreshold;
		THRESHOLD_GENERAL = thresholdGeneral;
		THRESHOLD_GLUE = thresholdGlue;
		glueGrammarOwner = symbolTable.addTerminal(JoshuaConfiguration.glue_owner);//TODO
	}
	
	
	
	
	public void clearState(){
		processedNodesTbl.clear();
		super.clearState();
	}
	

//	######################### pruning here ##############
	public void pruningHG(HyperGraph hg) {
		
		runInsideOutside(hg, 2, 1, 1.0);//viterbi-max, log-semiring
		
		if (fixThresholdPruning) {
			pruningHGHelper(hg);
			super.clearState();
		} else {
			throw new RuntimeException("wrong call");
		}
	}
	
	private void  pruningHGHelper(HyperGraph hg) {
		
		this.bestLogProb = getLogNormalizationConstant();//set the best_log_prob
		
		numSurvivedEdges = 0;
		numSurvivedNodes = 0;
		processedNodesTbl.clear(); 
		pruningNode(hg.goalNode);
		
		//clear up
		processedNodesTbl.clear();
		
		System.out.println("Item suvived ratio: "+ numSurvivedNodes*1.0/hg.numNodes + " =  " + numSurvivedNodes + "/" + hg.numNodes);
		System.out.println("Deduct suvived ratio: "+ numSurvivedEdges*1.0/hg.numEdges + " =  " + numSurvivedEdges + "/" + hg.numEdges);
	}
		
	
	private void pruningNode(HGNode it) {
		
		if (processedNodesTbl.containsKey(it)) 
			return;
		
		processedNodesTbl.put(it,true);
		boolean shouldSurvive = false;
		
		//### recursive call on each deduction
		for (int i = 0; i < it.hyperedges.size(); i++) {
			HyperEdge dt = it.hyperedges.get(i);
			boolean survived = pruningEdge(dt, it);//deduction-specifc operation
			if (survived) {
				shouldSurvive = true; // at least one deduction survive
			} else {
				it.hyperedges.remove(i);
				i--;
			}
		}
		//TODO: now we simply remove the pruned deductions, but in general, we may want to update the variables mainted in the item (e.g., best_deduction); this depends on the pruning method used
		
		/*by defintion: "should_surive==false" should be impossible, since if I got called, then my upper-deduction must survive, then i will survive
		* because there must be one way to reach me from lower part in order for my upper-deduction survive*/
		if (! shouldSurvive) {
			throw new RuntimeException("item explored but does not survive");
			//TODO: since we always keep the best_deduction, this should never be true
		} else {
			numSurvivedNodes++;
		}
	}
	
		
	//if survive, return true
	//best-deduction is always kept
	private boolean pruningEdge(HyperEdge dt, HGNode parent) {
		
		/**TODO: theoretically, if an item is get called, then its best deduction should always be kept even just by the threshold-checling. 
		 * In reality, due to precision of Double, the threshold-checking may not be perfect*/
		if (dt != parent.bestHyperedge) { // best deduction should always survive if the Item is get called
			//### prune?
			if (shouldPruneHyperedge(dt, parent)) {
				return false; // early stop
			}
		}
		
		//### still survive, recursive call all my ant-items
		if (null != dt.getAntNodes()) {
			for (HGNode ant_it : dt.getAntNodes()) {
				pruningNode(ant_it); // recursive call on each ant item, note: the ant_it will not be pruned as I need it
			}
		}
		
		//### if get to here, then survive; remember: if I survive, then my upper-item must survive
		numSurvivedEdges++;
		return true; // survive
	}
	
	private boolean shouldPruneHyperedge(HyperEdge dt, HGNode parent) {
		
		//### get merit
		double postLogProb = getEdgeUnormalizedPosteriorLogProb(dt, parent);
		
		
		if (dt.getRule() != null
		&& dt.getRule().getOwner() == glueGrammarOwner
		&& dt.getRule().getArity() == 2) { // specicial rule: S->S X
			//TODO
			return (postLogProb - this.bestLogProb < THRESHOLD_GLUE);
		} else {
			return (postLogProb - this.bestLogProb < THRESHOLD_GENERAL);
		}
	}
	
}
