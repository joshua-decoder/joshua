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
import java.util.List;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;


/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/

public class ViterbiExtractor  {

//	get one-best string under item
	public static String extractViterbiString(SymbolTable symbolTable, HGNode node) {
		StringBuffer res = new StringBuffer();
		
		HyperEdge edge = node.bestHyperedge;
		Rule rl = edge.getRule();
		
		if (null == rl) { // deductions under "goal item" does not have rule
			if (edge.getAntNodes().size() != 1) {
				throw new RuntimeException("deduction under goal item have not equal one item");
			}
			return extractViterbiString(symbolTable, edge.getAntNodes().get(0));
		}
		int[] english = rl.getEnglish();
		for (int c = 0; c < english.length; c++) {
			if (symbolTable.isNonterminal(english[c])) {
				int id = symbolTable.getTargetNonterminalIndex(english[c]);
				HGNode child = (HGNode)edge.getAntNodes().get(id);
				res.append(extractViterbiString(symbolTable, child));
			} else {
				res.append(symbolTable.getWord(english[c]));
			}
			if (c < english.length-1) res.append(' ');
		}
		return res.toString();
	}
	
//	######## find 1best hypergraph#############	
	public static HyperGraph getViterbiTreeHG(HyperGraph hg_in) {
		HyperGraph res = new HyperGraph(cloneNodeWithBestHyperedge(hg_in.goalNode), -1, -1, hg_in.sentID, hg_in.sentLen); // TODO: number of items/deductions
		get1bestTreeNode(res.goalNode);
		return res;
	}
	
	private static void get1bestTreeNode(HGNode it) {
		HyperEdge dt = it.bestHyperedge;
		if (null != dt.getAntNodes()) {
			for (int i = 0; i < dt.getAntNodes().size(); i++) {
				HGNode antNode = dt.getAntNodes().get(i);
				HGNode newNode = cloneNodeWithBestHyperedge(antNode);
				dt.getAntNodes().set(i, newNode);
				get1bestTreeNode(newNode);
			}
		}
	}
	
	// TODO: tbl_states
	private static HGNode cloneNodeWithBestHyperedge(HGNode inNode) {
		List<HyperEdge> hyperedges = new ArrayList<HyperEdge>(1);
		HyperEdge cloneEdge = cloneHyperedge(inNode.bestHyperedge);
		hyperedges.add(cloneEdge);
		return new HGNode(inNode.i, inNode.j, inNode.lhs,  hyperedges, cloneEdge, inNode.dpStates);
	}
	
	
	private static HyperEdge cloneHyperedge(HyperEdge inEdge) {
		List<HGNode> antNodes = null;
		if (null != inEdge.getAntNodes()) {
			antNodes = new ArrayList<HGNode>(inEdge.getAntNodes());//l_ant_items will be changed in get_1best_tree_item
		}
		HyperEdge res = new HyperEdge(inEdge.getRule(), inEdge.bestDerivationLogP, inEdge.getTransitionLogP(false), antNodes, inEdge.getSourcePath());
		return res;
	}
	//###end
}
