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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.chart_parser.Prunable;
import joshua.decoder.ff.state_maintenance.DPState;

/**
 * This class implements a hypergraph node, also known as an item in parsing.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Juri Ganitkevitch
 */

// TODO: Handle the case that the hypergraph only maintains the one-best tree.
public class HGNode implements Prunable<HGNode> {

	private static final Logger logger = Logger.getLogger(HGNode.class.getName());

	public int i, j;

	// The constituent label governing the span.
	public int lhs;

	// Each hyperedge is an "and" node.
	public List<HyperEdge> hyperedges = null;

	// used in pruning, compute_item, and transit_to_goal
	public HyperEdge bestHyperedge = null;

	// The key is the state id; remember the state required by each model,
	// for example, edge n-grams for LM model.
	TreeMap<Integer, DPState> dpStates;

	// Signature hash code for the node.
	private int signature = 0;

	public boolean isDead = false;
	private double estTotalLogP = 0.0;

	public HGNode(int i, int j, int lhs,
			TreeMap<Integer, DPState> dpStates,
			HyperEdge initHyperedge,
			double estTotalLogP) {
		this.i = i;
		this.j = j;
		this.lhs = lhs;
		this.dpStates = dpStates;
		this.estTotalLogP = estTotalLogP;
		addHyperedgeInNode(initHyperedge);
	}

	// used by disk hg
	public HGNode(int i,
			int j,
			int lhs,
			List<HyperEdge> hyperedges,
			HyperEdge bestHyperedge,
			TreeMap<Integer, DPState> states) {
		this.i = i;
		this.j = j;
		this.lhs = lhs;
		this.hyperedges = hyperedges;
		this.bestHyperedge = bestHyperedge;
		this.dpStates = states;
	}

	public void addHyperedgeInNode(HyperEdge dt) {
		if (dt != null) {
			if (null == hyperedges) {
				hyperedges = new ArrayList<HyperEdge>();
			}
			hyperedges.add(dt);
			semiringPlus(dt);
		}
	}

	public void semiringPlus(HyperEdge dt) {
		if (null == bestHyperedge || bestHyperedge.bestDerivationLogP < dt.bestDerivationLogP) {
			bestHyperedge = dt; // no change when tied
		}
	}

	public void addHyperedgesInNode(List<HyperEdge> hyperedges) {
		for (HyperEdge hyperEdge : hyperedges)
			addHyperedgeInNode(hyperEdge);
	}

	public TreeMap<Integer, DPState> getDPStates() {
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
			HyperGraph.logger.log(level, String.format("lhs: %s; logP: %.3f", lhs,
					bestHyperedge.bestDerivationLogP));
	}

	// Hash signature of this item: includes lhs, states.
	public int getSignature() {
		if (this.signature == 0) {
			this.signature = Math.abs(lhs);
			if (this.dpStates != null)
				for (DPState dps : dpStates.values())
					this.signature = this.signature * 31 + dps.getSignature(false);
		}
		return this.signature;
	}

	public void releaseDPStatesMemory() {
		dpStates = null;
	}

	public double getEstTotalLogP() {
		return this.estTotalLogP;
	}

	public int compareTo(HGNode anotherItem) {
		logger.severe("This compare function should never be called.");
		System.exit(1);
		return 0;
	}

	// Inverse order.
	public static Comparator<HGNode> inverseLogPComparator = new Comparator<HGNode>() {
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

	// Natural order.
	public static Comparator<HGNode> logPComparator = new Comparator<HGNode>() {
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
