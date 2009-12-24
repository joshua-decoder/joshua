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

import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FeatureFunction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
public class HGNode implements Comparable<HGNode> {
	
	public int i, j;
	
	// this is the symbol like: NP, VP, and so on
	public int lhs;
	
	// each hyperedge is an "and" node
	public ArrayList<HyperEdge> hyperedges = null;
	
	// used in pruning, compute_item, and transit_to_goal
	public HyperEdge bestHyperedge = null;
	

	// the key is the feature id; remember the state required by each model, for example, edge-ngrams for LM model
	HashMap<Integer,FFDPState> ffDpstatesTbl;
	
	
	//============== auxiluary variables, no need to store on disk
	// signature of this item: lhs, states
	private String signature = null;
	// seperator for the signature for each feature function
	private static final String FF_SIG_SEP = " -f- ";
	
	//============== for pruning purpose
	public boolean isDead        = false;
	public double  estTotalCost = 0.0; //it includes the bonus cost
	
	
//===============================================================
// Constructors
//===============================================================

	public HGNode(int i, int j, int lhs, HashMap<Integer,FFDPState> states, HyperEdge init_hyperedge, double est_total_cost) {
		this.i   = i;
		this.j   = j;
		this.lhs = lhs;
		this.ffDpstatesTbl = states;
		this.estTotalCost  = est_total_cost;
		addHyperedgeInItem(init_hyperedge);
	}
	
	
	//used by disk hg
	public HGNode(int i, int j, int lhs, ArrayList<HyperEdge> l_hyperedges, HyperEdge best_hyperedge, HashMap<Integer,FFDPState> states) {
		this.i   = i;
		this.j   = j;
		this.lhs = lhs;
		this.hyperedges    = l_hyperedges;
		this.bestHyperedge  = best_hyperedge;
		this.ffDpstatesTbl = states;
	}
	
	
//===============================================================
// Methods
//===============================================================
	
	public void addHyperedgeInItem(HyperEdge dt) {
		if (null == hyperedges) {
			hyperedges = new ArrayList<HyperEdge>();
		}
		hyperedges.add(dt);
		if (null == bestHyperedge
		|| bestHyperedge.bestDerivationCost > dt.bestDerivationCost) {
			bestHyperedge = dt; //no change when tied
		}
	}
	
	
	public void addHyperedgesInItem(ArrayList<HyperEdge> hyperedges) {
		for(HyperEdge hyperEdge : hyperedges) 
			addHyperedgeInItem(hyperEdge);
	}
	
	
	public HashMap<Integer,FFDPState> getTblFeatDPStates() {
		return ffDpstatesTbl;
	}
	
	
	public FFDPState getFeatDPState(FeatureFunction ff) {
		return getFeatDPState(ff.getFeatureID());
	}
	
	
	public FFDPState getFeatDPState(int featureID) {
		if (null == this.ffDpstatesTbl) {
			return null;
		} else {
			return this.ffDpstatesTbl.get(featureID);
		}
	}
	
	
	public void print_info(Level level) {
		if (HyperGraph.logger.isLoggable(level))
			HyperGraph.logger.log(level,
				String.format("lhs: %s; cost: %.3f",
					lhs, bestHyperedge.bestDerivationCost));
	}
	
	
	//signature of this item: lhs, states (we do not need i, j)
	public String getSignature() {
		if (null == this.signature) {
			StringBuffer s = new StringBuffer();
			s.append(lhs);
			s.append(" ");
			
			if (null != this.ffDpstatesTbl
			&& this.ffDpstatesTbl.size() > 0) {
				Iterator<Map.Entry<Integer,FFDPState>> it
					= this.ffDpstatesTbl.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer,FFDPState> entry = it.next();
					
					s.append(entry.getValue().getSignature(false));
					if (it.hasNext()) s.append(FF_SIG_SEP);
				}
			}
			
			this.signature = s.toString();
		}
		// TODO: class needs logger
//		if (logger.isLoggable(Level.INFO)) logger.finest(String.format("Signature is %s", res));
		return this.signature;
	}
	
	public void releaseStateMemory(){
		ffDpstatesTbl = null;
	}
	
	/*this will called by the sorting
	 * in Cell.ensureSorted()*/
	//sort by est_total_cost: for pruning purpose
	public int compareTo(HGNode anotherItem) {
		if (this.estTotalCost < anotherItem.estTotalCost) {
			return -1;
		} else if (this.estTotalCost == anotherItem.estTotalCost) {
			return 0;
		} else {
			return 1;
		}
		
	}
	
	
	public static Comparator<HGNode> negtiveCostComparator	= new Comparator<HGNode>() {			
			public int compare(HGNode item1, HGNode item2) {
				double cost1 = item1.estTotalCost;
				double cost2 = item2.estTotalCost;
				if (cost1 > cost2) {
					return -1;
				} else if (cost1 == cost2) {
					return 0;
				} else {
					return 1;
				}
			}
	};
}
