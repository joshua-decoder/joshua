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
package joshua.lattice;


/**
 * An arc in a directed graph.
 * 
 * @author Lane Schwartz
 * @since 2008-07-08
 * @version $LastChangedDate$
 *
 * @param <Label> Type of label associated with an arc.
 */
public class Arc<Label> {

	/**
	 * Weight of this arc. Package-private scope so that Node
	 * and Lattice can quickly access this variable.
         * @todo should be a vector of costs
	 */
	final double cost;
	
	/**
	 * Node where this arc begins. Package-private scope so
	 * that Node and Lattice can quickly access this variable.
	 */
	final Node<Label> head;
	
	/** 
	 * Node where this arc ends. Package-private scope so that
	 * Node and Lattice can quickly access this variable.
	 */
	final Node<Label> tail;
	
	/** 
	 * Label associated with this arc. Package-private scope
	 * so that Node and Lattice can quickly access this variable.
	 */
	final Label label;
	
	/**
	 * Creates an arc with the specified head, tail, cost, and label.
	 * 
	 * @param head The node where this arc begins.
	 * @param tail The node where this arc ends.
	 * @param cost The cost of this arc.
	 * @param label The label associated with this arc.
	 */
	public Arc(Node<Label> head, Node<Label> tail, double cost, Label label) {
		this.head = head;
		this.tail = tail;
		this.cost = cost;
		this.label = label;
	}

	/**
	 * Gets the cost of this arc.
	 * 
	 * @return The cost of this arc.
         * @todo should support indexing for multiple costs associated
         * with each arc
	 */
	public double getCost() {
		return cost;
	}

	/**
	 * Gets the head of this arc (the node where this arc begins).
	 * 
	 * @return The head of this arc.
	 */
	public Node<Label> getHead() {
		return head;
	}

	/**
	 * Gets the tail of this arc (the node where this arc ends).
	 * 
	 * @return The tail of this arc.
	 */
	public Node<Label> getTail() {
		return tail;
	}
	
	/** 
	 * Gets the label associated with this arc.
	 * 
	 * @return The label associated with this arc.
	 */
	public Label getLabel() {
		return label;
	}
	
}
