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
package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;

import joshua.decoder.hypergraph.HGNode;

/** 
 * Represents a list of items in the hypergraph that have the same
 * left-hand side but may have different LM states.
 * 
 * @author Zhifei Li
 */
class SuperNode {
	
	/** Common left-hand side state. */
	final int lhs;
	
	/** 
	 * List of hypergraph nodes, each of which has its own
	 * language model state.
	 */
	final List<HGNode> nodes;
	
	
	/**
	 * Constructs a super item defined by a common left-hand
	 * side.
	 * 
	 * @param lhs Left-hand side token
	 */
	public SuperNode(int lhs) {
		this.lhs = lhs;
		this.nodes = new ArrayList<HGNode>();
	}
}
