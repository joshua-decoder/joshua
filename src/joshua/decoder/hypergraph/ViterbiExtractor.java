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

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;


/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/

public class ViterbiExtractor  {

//	get one-best string under item
	public static String extractViterbiString(SymbolTable p_symbolTable, HGNode item) {
		StringBuffer res = new StringBuffer();
		
		HyperEdge p_edge = item.bestHyperedge;
		Rule rl = p_edge.getRule();
		
		if (null == rl) { // deductions under "goal item" does not have rule
			if (p_edge.getAntNodes().size() != 1) {
				throw new RuntimeException("deduction under goal item have not equal one item");
			}
			return extractViterbiString(p_symbolTable, (HGNode)p_edge.getAntNodes().get(0));
		}
		int[] english = rl.getEnglish();
		for (int c = 0; c < english.length; c++) {
			if (p_symbolTable.isNonterminal(english[c])) {
				int id = p_symbolTable.getTargetNonterminalIndex(english[c]);
				HGNode child = (HGNode)p_edge.getAntNodes().get(id);
				res.append(extractViterbiString(p_symbolTable, child));
			} else {
				res.append(p_symbolTable.getWord(english[c]));
			}
			if (c < english.length-1) res.append(' ');
		}
		return res.toString();
	}
	
//	######## find 1best hypergraph#############	
	public static HyperGraph getViterbiTreeHG(HyperGraph hg_in) {
		HyperGraph res = new HyperGraph(clone_item_with_best_deduction(hg_in.goalNode), -1, -1, hg_in.sentID, hg_in.sentLen); // TODO: number of items/deductions
		get_1best_tree_item(res.goalNode);
		return res;
	}
	
	private static void get_1best_tree_item(HGNode it) {
		HyperEdge dt = it.bestHyperedge;
		if (null != dt.getAntNodes()) {
			for (int i = 0; i < dt.getAntNodes().size(); i++) {
				HGNode ant_it = (HGNode) dt.getAntNodes().get(i);
				HGNode new_it = clone_item_with_best_deduction(ant_it);
				dt.getAntNodes().set(i, new_it);
				get_1best_tree_item(new_it);
			}
		}
	}
	
	// TODO: tbl_states
	private static HGNode clone_item_with_best_deduction(HGNode it_in) {
		ArrayList<HyperEdge> l_deductions = new ArrayList<HyperEdge>(1);
		HyperEdge clone_dt = clone_deduction(it_in.bestHyperedge);
		l_deductions.add(clone_dt);
		return new HGNode(it_in.i, it_in.j, it_in.lhs,  l_deductions, clone_dt, it_in.dpStates);
	}
	
	
	private static HyperEdge clone_deduction(HyperEdge dt_in) {
		ArrayList<HGNode> l_ant_items = null;
		if (null != dt_in.getAntNodes()) {
			l_ant_items = new ArrayList<HGNode>(dt_in.getAntNodes());//l_ant_items will be changed in get_1best_tree_item
		}
		HyperEdge res = new HyperEdge(dt_in.getRule(), dt_in.bestDerivationCost, dt_in.getTransitionCost(false), l_ant_items, dt_in.getSourcePath());
		return res;
	}
	//###end
}
