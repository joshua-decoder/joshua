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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A lattice representation of a directed graph.
 * 
 * @author Lane Schwartz
 * @since 2008-07-08
 * @version $LastChangedDate$
 *
 * @param <Label> Type of label associated with an arc.
 */
public class Lattice<Value> implements Iterable<Node<Value>> {

	/** 
	 * Costs of the best path between each pair of nodes in the
	 * lattice.
	 */
	private final double[][] costs;

	/**
	 * List of all nodes in the lattice. Nodes are assumed to
	 * be in topological order.
	 */
	private final List<Node<Value>> nodes;
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(Lattice.class.getName());
	
	/**
	 * Constructs a new lattice from an existing list of
	 * (connected) nodes.
	 * <p>
	 * The list of nodes must already be in topological order.
	 * If the list is not in topological order, the behavior
	 * of the lattice is not defined.
	 * 
	 * @param nodes A list of nodes which must be in topological order.
	 */
	public Lattice(List<Node<Value>> nodes) {
		this.nodes = nodes;
		this.costs = calculateAllPairsShortestPath(nodes);
	}
	
	
	public Lattice(Value[] linearChain) {
		this.nodes = new ArrayList<Node<Value>>();
		
		Node<Value> previous = new Node<Value>(0);
		nodes.add(previous);
		
		int i=1;
		
		for (Value value : linearChain) {
			
			Node<Value> current = new Node<Value>(i);
			float cost = 0.0f;
			// if (i > 4) cost = (float)i/1.53432f;
			previous.addArc(current, cost, value);
			
			nodes.add(current);
			
			previous = current;
			i++;
		}
		
		this.costs = calculateAllPairsShortestPath(nodes);
	}
	
	
	/**
	 * Constructs a lattice from a given string representation.
	 * 
	 * @param data String representation of a lattice.
	 * @return A lattice that corresponds to the given string.
	 */
	public static Lattice<String> createFromString(String data) {
		
		Map<Integer,Node<String>> nodes = new HashMap<Integer,Node<String>>();
		
		Pattern nodePattern = Pattern.compile("(.+?)\\((\\(.+?\\),)\\)(.*)");
		Pattern arcPattern = Pattern.compile("\\('(.+?)',(\\d+.\\d+),(\\d+)\\),(.*)");
		
		Matcher nodeMatcher = nodePattern.matcher(data);
		
		int nodeID = -1;
		
		while (nodeMatcher.matches()) {
			
			String nodeData = nodeMatcher.group(2);
			String remainingData = nodeMatcher.group(3);
			
			nodeID++;
			
			Node<String> currentNode;
			if (nodes.containsKey(nodeID)) {
				currentNode = nodes.get(nodeID);
			} else {
				currentNode = new Node<String>(nodeID);
				nodes.put(nodeID, currentNode);
			}
			
			if (logger.isLoggable(Level.FINE)) logger.fine("Node " + nodeID + ":");
			
			Matcher arcMatcher = arcPattern.matcher(nodeData);
			
			while (arcMatcher.matches()) {
				String arcLabel = arcMatcher.group(1);
				double arcWeight = Double.valueOf(arcMatcher.group(2));
				int destinationNodeID = nodeID + Integer.valueOf(arcMatcher.group(3));
				
				Node<String> destinationNode;
				if (nodes.containsKey(destinationNodeID)) {
					destinationNode = nodes.get(destinationNodeID);
				} else {
					destinationNode = new Node<String>(destinationNodeID);
					nodes.put(destinationNodeID, destinationNode);
				}
				
				String remainingArcs = arcMatcher.group(4);
				
				if (logger.isLoggable(Level.FINE)) logger.fine("\t" + arcLabel + " " + arcWeight + " " + destinationNodeID);
				
				currentNode.addArc(destinationNode, arcWeight, arcLabel);
				
				arcMatcher = arcPattern.matcher(remainingArcs);
			}
			
			nodeMatcher = nodePattern.matcher(remainingData);
		}
		
		List<Node<String>> nodeList = new ArrayList<Node<String>>(nodes.values());
		Collections.sort(nodeList);
		
		if (logger.isLoggable(Level.FINE)) logger.fine(nodeList.toString());
		
		return new Lattice<String>(nodeList);
	}
	
	
	/**
	 * Gets the cost of the shortest path between two nodes.
	 * 
	 * @param from ID of the starting node.
	 * @param to ID of the ending node.
	 * @return The cost of the shortest path between the two nodes.
	 */
	public double getShortestPath(int from, int to) {
		return costs[from][to];
	}
	
	
	/**
	 * Gets the node with a specified integer identifier.
	 * 
	 * @param index Integer identifier for a node.
	 * @return The node with the specified integer identifier
	 */
	public Node<Value> getNode(int index) {
		return nodes.get(index);
	}
	
	
	/**
	 * Returns an iterator over the nodes in this lattice.
	 * 
	 * @return An iterator over the nodes in this lattice.
	 */
	public Iterator<Node<Value>> iterator() {
		return nodes.iterator();
	}
	
	
	/**
	 * Returns the number of nodes in this lattice.
	 * 
	 * @return The number of nodes in this lattice.
	 */
	public int size() {
		return nodes.size();
	}
	
	
	/**
	 * Calculate the all-pairs shortest path for all pairs of nodes.
	 * <p>
	 * Note: This method assumes no backward arcs. 
	 * If there are backward arcs, the returned shortest path
	 * costs for that node may not be accurate.
	 * 
	 * @param nodes A list of nodes which must be in topological order.
	 * @return The all-pairs shortest path for all pairs of nodes.
	 */
	private double[][] calculateAllPairsShortestPath(List<Node<Value>> nodes) {
		
		int size = nodes.size();
		double[][] costs = new double[size][size];
		
		// Initialize pairwise costs to be infinite for
		// each pair of nodes
		for (int from = 0; from < size; from++) {
			for (int to = 0; to < size; to++) {
				costs[from][to] = Double.POSITIVE_INFINITY;
			}
		}
		
		// Loop over all pairs of immediate neighbors and
		// record the actual costs.
		for (Node<Value> head : nodes) {
			for (Arc<Value> arc : head.outgoingArcs) {
				Node<Value> tail = arc.tail;
				
				int from = head.id;
				int to = tail.id;
				// this is slightly different
				// than it was defined in Dyer et al 2008
				double cost = arc.cost;
				// minimally, cost should be weighted by
				// the feature weight assigned, so we just
				// set this to 1.0 for now
				cost = 1.0;
				
				if (cost < costs[from][to]) {
					costs[from][to] = cost;
				}
			}
		}
		
		
		// Loop over every possible starting node (the last
		// node is assumed to not be a starting node)
		for (int i=0; i < size-2; i++) {
			
			// Loop over every possible ending node,
			// starting two nodes past the starting
			// node (this assumes no backward arcs)
			for (int j=i+2; j < size; j++) {
				
				// Loop over every possible middle
				// node, starting one node past the
				// starting node (this assumes no
				// backward arcs)
				for (int k=i+1; k < j; k++) {
					
					// The best cost is the
					// minimum of the previously
					// recorded cost and the sum
					// of costs in the currently
					// considered path
					costs[i][j] = Math.min(costs[i][j], costs[i][k] + costs[k][j]);
					
				}
			}
		}
		
		return costs;
	}
	
	
	public static void main(String[] args) {
		
		List<Node<String>> nodes = new ArrayList<Node<String>>();
		for (int i=0; i < 4; i++) {
			nodes.add(new Node<String>(i));
		}
		
		nodes.get(0).addArc(nodes.get(1), 1.0, "x");
		nodes.get(1).addArc(nodes.get(2), 1.0, "y");
		nodes.get(0).addArc(nodes.get(2), 1.5, "a");
		nodes.get(2).addArc(nodes.get(3), 3.0, "b");
		nodes.get(2).addArc(nodes.get(3), 5.0, "c");
		
		Lattice<String> graph = new Lattice<String>(nodes);
		
		System.out.println("Shortest path from 0 to 3: " + graph.getShortestPath(0,3));
	}

}
