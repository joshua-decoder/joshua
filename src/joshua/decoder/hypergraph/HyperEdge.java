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

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.tm.Rule;

/**
 * this class implement Hyperedge
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

public class HyperEdge {
	
	/** the 1-best cost of all possible derivations: 
	 * best costs of ant hgnodes + transitionCost
	 **/
	public double bestDerivationCost = Double.POSITIVE_INFINITY;

	/**this remembers the stateless + non_stateless cost 
	 * assocated with the rule (excluding the best-cost from ant nodes)
	 * */
	private Double transitionCost=null;
	
	private Rule rule;
	
	private SourcePath srcPath = null;

	/**If antNodes is null, then this edge corresponds to a rule with zero arity.
	 * Aslo, the nodes appear in the list as per the index of the Foreign side non-terminal
	 * */
	private ArrayList<HGNode> antNodes = null; 
	
	public HyperEdge(Rule rl, double bestDerivationCost_, Double transitionCost_, ArrayList<HGNode> antNodes_, SourcePath sp){
		bestDerivationCost = bestDerivationCost_;
		transitionCost=transitionCost_;
		rule=rl;
		antNodes= antNodes_;
		srcPath = sp;
	}
	
	public Rule getRule(){
		return rule;
	}

	public SourcePath getSourcePath() {
		return srcPath;
	}
	
	public ArrayList<HGNode> getAntNodes(){
		return antNodes;
	}
	
	
	public double getTransitionCost(boolean forceCompute){//note: transition_cost is already linearly interpolated
		if(forceCompute || transitionCost==null){
			double res = bestDerivationCost;
			if(antNodes!=null)	
				for(HGNode ant_it : antNodes)
					res -= ant_it.bestHyperedge.bestDerivationCost;
			transitionCost = res;				
		}
		return transitionCost;
	}
	
	public void setTransitionCost(double transitionCost_){
		transitionCost = transitionCost_;
	}
	
}
