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
 * @version $LastChangedDate: 2008-11-03 16:59:05 -0500 (星期一, 03 十一月 2008) $
 */

public class HGNode implements Comparable<HGNode> {
	public int i, j;
	
	// each hyperedge is a "and" node
	public ArrayList<HyperEdge> l_hyperedges = null;
	
	// used in pruning, compute_item, and transit_to_goal
	public HyperEdge best_hyperedge = null;
	
	// this is the symbol like: NP, VP, and so on
	public int lhs;
	
	// the key is the feature id; remember the state required by each model, for example, edge-ngrams for LM model
	HashMap<Integer,FFDPState> tbl_ff_dpstates;
	
	//######### auxiluary variables, no need to store on disk
	// signature of this item: lhs, states
	private String signature = null;
	// seperator for the signature for each feature function
	private static final String FF_SIG_SEP = " -f- ";
	
	//######## for pruning purpose
	public boolean is_dead        = false;
	public double  est_total_cost = 0.0; //it includes the bonus cost
	
	
//===============================================================
// Constructors
//===============================================================

	public HGNode(int i, int j, int lhs, HashMap<Integer,FFDPState> states, HyperEdge init_hyperedge, double est_total_cost) {
		this.i   = i;
		this.j   = j;
		this.lhs = lhs;
		this.tbl_ff_dpstates = states;
		this.est_total_cost  = est_total_cost;
		add_hyperedge_in_item(init_hyperedge);
	}
	
	
	//used by disk hg
	public HGNode(int i, int j, int lhs, ArrayList<HyperEdge> l_hyperedges, HyperEdge best_hyperedge, HashMap<Integer,FFDPState> states) {
		this.i   = i;
		this.j   = j;
		this.lhs = lhs;
		this.l_hyperedges    = l_hyperedges;
		this.best_hyperedge  = best_hyperedge;
		this.tbl_ff_dpstates = states;
	}
	
	
//===============================================================
// Methods
//===============================================================
	
	public void add_hyperedge_in_item(HyperEdge dt) {
		if (null == l_hyperedges) {
			l_hyperedges = new ArrayList<HyperEdge>();
		}
		l_hyperedges.add(dt);
		if (null == best_hyperedge
		|| best_hyperedge.best_cost > dt.best_cost) {
			best_hyperedge = dt; //no change when tied
		}
	}
	
	
	public void add_hyperedges_in_item(ArrayList<HyperEdge> hyperedges) {
		for(HyperEdge hyperEdge : hyperedges) 
			add_hyperedge_in_item(hyperEdge);
	}
	
	
	public HashMap<Integer,FFDPState> getTblFeatDPStates() {
		return tbl_ff_dpstates;
	}
	
	
	public FFDPState getFeatDPState(FeatureFunction ff) {
		return getFeatDPState(ff.getFeatureID());
	}
	
	
	public FFDPState getFeatDPState(int featureID) {
		if (null == this.tbl_ff_dpstates) {
			return null;
		} else {
			return this.tbl_ff_dpstates.get(featureID);
		}
	}
	
	
	public void print_info(Level level) {
		if (HyperGraph.logger.isLoggable(level))
			HyperGraph.logger.log(level,
				String.format("lhs: %s; cost: %.3f",
					lhs, best_hyperedge.best_cost));
	}
	
	
	//signature of this item: lhs, states (we do not need i, j)
	public String get_signature() {
		if (null == this.signature) {
			StringBuffer s = new StringBuffer();
			s.append(lhs);
			
			if (null != this.tbl_ff_dpstates
			&& this.tbl_ff_dpstates.size() > 0) {
				Iterator<Map.Entry<Integer,FFDPState>> it
					= this.tbl_ff_dpstates.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<Integer,FFDPState> entry = it.next();
					
					s.append(entry.getValue().getSignature(false));
					if (it.hasNext()) s.append(FF_SIG_SEP);
				}
			}
			
			this.signature = s.toString();
		}
		//System.out.println("sig is: " +signature);
		//Support.write_log_line(String.format("Signature is %s", res), Support.INFO);
		return this.signature;
	}
	
	
	//sort by est_total_cost: for prunning purpose
	public int compareTo(HGNode anotherItem) throws ClassCastException {
		if (!(anotherItem instanceof HGNode)) {
			throw new ClassCastException("An HGNode object expected.");
		} else if (this.est_total_cost < anotherItem.est_total_cost) {
			return -1;
		} else if (this.est_total_cost == anotherItem.est_total_cost) {
			return 0;
		} else {
			return 1;
		}
	}
	
	
	public static Comparator<HGNode> NegtiveCostComparator	= new Comparator<HGNode>() {			
			public int compare(HGNode item1, HGNode item2) {
				double cost1 = item1.est_total_cost;
				double cost2 = item2.est_total_cost;
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
