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

import joshua.decoder.chart_parser.Prunable;
import joshua.decoder.ff.state_maintenance.DPState;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * this class implement Hypergraph node (i.e., HGNode); also known
 * as Item in parsing.
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

//@todo: handle the case that the Hypergraph only maintains the one-best tree 
public class HGNode implements Prunable<HGNode> {
	
	public int i, j;
	
	// this is the symbol like: NP, VP, and so on
	public int lhs;
	
	// each hyperedge is an "and" node
	public List<HyperEdge> hyperedges = null;
	
	// used in pruning, compute_item, and transit_to_goal
	public HyperEdge bestHyperedge = null;
	

	// the key is the state id; remember the state required by each model, for example, edge-ngrams for LM model
	HashMap<Integer,DPState> dpStates;
	
	
	//============== auxiluary variables, no need to store on disk
	// signature of this item: lhs, states
	private String signature = null;
	// seperator for the signature for each state
	private static final String STATE_SIG_SEP = " -f- ";
	
	//============== for pruning purpose
	public boolean isDead        = false;
	private double  estTotalLogP = 0.0; //it includes the estimated LogP
	
	
//===============================================================
// Constructors
//===============================================================

	public HGNode(int i, int j, int lhs, HashMap<Integer,DPState> dpStates, HyperEdge initHyperedge, double estTotalLogP) {
		this.i   = i;
		this.j   = j;
		this.lhs = lhs;
		this.dpStates = dpStates;
		this.estTotalLogP  = estTotalLogP;
		addHyperedgeInNode(initHyperedge);
	}
	
	
	//used by disk hg
	public HGNode(int i, int j, int lhs, List<HyperEdge> hyperedges, HyperEdge bestHyperedge, HashMap<Integer,DPState> states) {
		this.i   = i;
		this.j   = j;
		this.lhs = lhs;
		this.hyperedges    = hyperedges;
		this.bestHyperedge  = bestHyperedge;
		this.dpStates = states;
	}
	
	
//===============================================================
// Methods
//===============================================================
	
	public void addHyperedgeInNode(HyperEdge dt) {
		if(dt!=null){
			if (null == hyperedges) {
				hyperedges = new ArrayList<HyperEdge>();
			}
			hyperedges.add(dt);
			semiringPlus(dt);
		}
	}
	
	public void semiringPlus(HyperEdge dt){		
		if (null == bestHyperedge || bestHyperedge.bestDerivationLogP < dt.bestDerivationLogP){//semiring + operation
			bestHyperedge = dt; //no change when tied
		}
	}
	
	public void addHyperedgesInNode(List<HyperEdge> hyperedges) {
		for(HyperEdge hyperEdge : hyperedges) 
			addHyperedgeInNode(hyperEdge);
	}
	
	
	public HashMap<Integer,DPState> getDPStates() {
		return dpStates;
	}
	
	
	public DPState getDPState(int stateID) {
		if (null == this.dpStates) {
			return null;
		} else {
			return this.dpStates.get(stateID);
		}
	}
	
	
	public void printInfo(Level level) {
		if (HyperGraph.logger.isLoggable(level))
			HyperGraph.logger.log(level,
				String.format("lhs: %s; logP: %.3f",
					lhs, bestHyperedge.bestDerivationLogP));
	}
	
	
	//signature of this item: lhs, states (we do not need i, j)
	public String getSignature() {
		if (null == this.signature) {
			StringBuffer s = new StringBuffer();
			s.append(lhs);
			s.append(" ");
			
			if (null != this.dpStates && this.dpStates.size() > 0) {
				Iterator<Map.Entry<Integer,DPState>> it = this.dpStates.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer,DPState> entry = it.next();					
					s.append(entry.getValue().getSignature(false));
					if (it.hasNext()) 
						s.append(STATE_SIG_SEP);
				}
			}
			
			this.signature = s.toString();
		}
	
		return this.signature;
	}
	
	public void releaseDPStatesMemory(){
		dpStates = null;
	}
	
	public double getEstTotalLogP(){
		return this.estTotalLogP;
	}
	
	
	/*this will called by the sorting
	 * in Cell.ensureSorted()*/
	//sort by estTotalLogP: for pruning purpose
	public int compareTo(HGNode anotherItem) {
		System.out.println("HGNode, compare functiuon should never be called");
		System.exit(1);
		return 0;
		/*
		if (this.estTotalLogP > anotherItem.estTotalLogP) {
			return -1;
		} else if (this.estTotalLogP == anotherItem.estTotalLogP) {
			return 0;
		} else {
			return 1;
		}*/
		
	}
	
	
	public static Comparator<HGNode> inverseLogPComparator	= new Comparator<HGNode>() {			
		public int compare(HGNode item1, HGNode item2) {
			double logp1 = item1.estTotalLogP;
			double logp2 = item2.estTotalLogP;
			if (logp1 > logp2) {
				return -1;
			} else if (logp1 == logp2) {
				return 0;
			} else {
				return 1;
			}
		}
	};

	/**natural order
	 * */
	public static Comparator<HGNode> logPComparator	= new Comparator<HGNode>() {			
		public int compare(HGNode item1, HGNode item2) {
			double logp1 = item1.estTotalLogP;
			double logp2 = item2.estTotalLogP;
			if (logp1 > logp2) {
				return 1;
			} else if (logp1 == logp2) {
				return 0;
			} else {
				return -1;
			}
		}
	};

	public boolean isDead() {
		return this.isDead;
	}


	public double getPruneLogP() {
		return this.estTotalLogP;
	}


	public void setDead() {
		this.isDead = true;		
	}


	public void setPruneLogP(double estTotalLogP) {
		this.estTotalLogP = estTotalLogP;
	}
}
