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

import joshua.decoder.ff.tm.Rule;

/**
 * this class implement Hyperedge
 * 
 *
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-11-03 16:59:05 -0500 (星期一, 03 十一月 2008) $
 */

public class HyperEdge {
	public double best_cost= Double.POSITIVE_INFINITY;//the 1-best cost of all possible derivation: best costs of ant items + non_stateless_transition_cost + r.statelesscost
	private Double transition_cost=null;//this remember the stateless + non_stateless cost assocated with the rule (excluding the best-cost from ant items)
	private Rule rule;
	//if(l_ant_items==null), then this shoud be the terminal rule
	private ArrayList<HGNode> l_ant_items=null; //ant items. In comparison, in a derivation, the parent should be the sub-derivation of the tail of the hyper-arc
	
	public HyperEdge(Rule rl, double total_cost, Double trans_cost, ArrayList<HGNode> ant_items){
		best_cost=total_cost;
		transition_cost=trans_cost;
		rule=rl;
		l_ant_items=ant_items;
	}
	
	public Rule get_rule(){return rule;}
	public ArrayList<HGNode> get_ant_items(){return l_ant_items;}
	//public double get_best_cost(){return best_cost;}
	
	public double get_transition_cost(boolean force_compute){//note: transition_cost is already linearly interpolated
		if(force_compute || transition_cost==null){
			double res = best_cost;
			if(l_ant_items!=null)	
				for(HGNode ant_it : l_ant_items)
					res -= ant_it.best_deduction.best_cost;
			transition_cost = res;				
		}
		return transition_cost;
	}
}