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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A node in a directed graph.
 * 
 * @author Lane Schwartz
 * @since 2008-07-08
 * @version $LastChangedDate$
 *
 * @param <Label> Type of label associated with an arc.
 */
public class Node<Label> implements Comparable<Node<Label>> {

	//===============================================================
	//Member variables
	//===============================================================
	
	/** 
	 * Numeric integer identifier of this node. Package-private
	 * scope so that Lattice can quickly access this variable.
	 */
	final Integer id;

	/** 
	 * Arcs which begin at this node. Package-private scope so
	 * that Lattice can quickly access this variable.
	 */
	final List<Arc<Label>> outgoingArcs;
	
	
	//===============================================================
	//Constructor(s)
	//===============================================================
	
	/**
	 * Constructs a new node with the specified numeric identifier.
	 */
	public Node(int id) {
		this.id = id;
		this.outgoingArcs = new ArrayList<Arc<Label>>();
	}
	
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/**
	 * Gets the numeric integer identifier of this node.
	 * 
	 * @return Numeric integer identifier of this node.
	 */
	public int getNumber() {
		return id;
	}
	
	
	/** 
	 * Gets the arcs that begin at this node. 
	 * 
	 * @return The arcs that begin at this node.
	 */
	public Iterable<Arc<Label>> getOutgoingArcs() {
		return outgoingArcs;
	}
	
	
	/**
	 * Gets an iterable object capable of iterating over all
	 * nodes directly reachable from this node. This will be
	 * all nodes which are the target of an outgoing arc from
	 * this node.
	 * 
	 * @return An iterable object capable of iterating over all
	 *         nodes directly reachable from this node.
	 */
	public Iterable<Node<Label>> reachableNodes() {
		final Iterator<Arc<Label>> arcIterator = outgoingArcs.iterator();
		
		return new Iterable<Node<Label>>() {
			public Iterator<Node<Label>> iterator() {
				return new Iterator<Node<Label>>() {

					public boolean hasNext() {
						return arcIterator.hasNext();
					}

					public Node<Label> next() {
						return arcIterator.next().getTail();
					}

					public void remove() {
						throw new UnsupportedOperationException();	
					}
				};
			}
		};
	}
	
	
	/**
	 * Adds a new outgoing arc to this node that points to the
	 * specified destination. The new arc will have the specified
	 * weight and specified label.
	 *   
	 * @param destination Destination node of the new outgoing arc.
	 * @param weight Weight of the new outgoing arc.
	 * @param label Label of the new outgoing arc.
	 */
	public void addArc(Node<Label> destination, double weight, Label label) {
		outgoingArcs.add(new Arc<Label>(this, destination, weight, label));
	}
	
	
	/**
	 * Gets the number of outgoing arcs that begin at this node.
	 * 
	 * @return The number of outgoing arcs that begin at this node.
	 */
	public int size() {
		return outgoingArcs.size();
	}
	
	
	public String toString() {
		return "Node-"+id;
	}
	
	
	public int compareTo(Node<Label> o) {
		return id.compareTo(o.id);
	}
	
}
