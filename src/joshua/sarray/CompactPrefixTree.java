package joshua.sarray;

import java.util.BitSet;
import java.util.Map;

public class CompactPrefixTree {

	static final boolean   ACTIVE = true;
	static final boolean INACTIVE = false;
	
	private static final int DEFAULT_CAPACITY = 512;
	private static final int DEFAULT_CAPACITY_INCREMENT = 512;
	
	/** Maximum number of nodes that can be stored in this tree. */
	private int capacity;
	
	/** 
	 * Value by which the capacity should be incremented 
	 * if additional space is required to store more nodes. 
	 */
	private int capacityIncrement;
	
	
	/** Indicates which nodes are active. */
	private final BitSet active;

	/** Indicates which nodes have children. */
	private final BitSet hasChildren;
	
	/**
	 * Stores several pieces of information compactly.
	 * 
	 * For each node in the tree, the following integers are stored:
	 * 
	 * <ul>
	 *   <li>Incoming arc value</li>
	 *   <li>Lower bound index</li>
	 *   <li>Upper bound index</li>
	 *   <li>Node ID of suffix link</li>
	 * </ul>
	 * 
	 * Each node is identified by a unique integer.
	 * This identifier is not explicitly stored.
	 * Rather, the identifier is implicitly stored
	 * as the index into the data structure.
	 * <p>
	 * In other words, the first values stored in data are for
	 * the node with identifier 0; the next values are for the 
	 * node with identifier 1, and so on.
	 */
	private int[] data;
	
	/** Number of integers stored in data for each node. */
	private static int INTS_PER_NODE = 4;

	private static int INCOMING_ARC_OFFSET=0;
	private static int LOWER_BOUND_OFFSET=1;
	private static int UPPER_BOUND_OFFSET=2;
	private static int SUFFIX_LINK_OFFSET=3;
	
	private static int BITS_PER_INT = 32;
	

	private static final int ROOT_NODE_INCOMING_ARC = Integer.MIN_VALUE;

	/** Unique integer identifier for the root node. */
	private static final int ROOT_NODE_ID = 0;
	
	/** 
	 * Maps from (Node ID, outgoing arc) --> Node.
	 * <p>
	 * This uses a long to encode (int,int). 
	 */
	Map<Long,Integer> children;
	
	private int size;
	
	public CompactPrefixTree() {
		this(DEFAULT_CAPACITY, DEFAULT_CAPACITY_INCREMENT);
	}
	
	public CompactPrefixTree(int capacity, int capacityIncrement) {
		this.capacity = capacity;
		this.capacityIncrement = capacityIncrement;
		this.active = new BitSet(capacity);
		this.hasChildren = new BitSet(capacity);
		this.data = new int[capacity * INTS_PER_NODE];
		
		// Insert root node
		this.data[ROOT_NODE_ID + INCOMING_ARC_OFFSET] = ROOT_NODE_INCOMING_ARC;
		this.size = 1;
	}
	
	public int size() {
		return size;
	}
	
	private boolean isActive(int node) {
		return active.get(node);
	}
	
	/**
	 * Adds a new child node to a parent node.
	 * 
	 * @param parentNode Node to which a child will be added.
	 * @param connectingArc Integer representation of the word that connects the parent to the child.
	 */
	private void addChild(int parentNode, int connectingArc) {
		
		// Ensure capacity
		if (size >= capacity) {
			int newCapacity = capacity + capacityIncrement;
			int[] newData = new int[newCapacity];
			System.arraycopy(data, 0, newData, 0, capacity);
			this.data = null;
			this.data = newData;
		}
		
		// Add the child node to the data array
		int childNode = size++;
		this.data[childNode*INTS_PER_NODE + INCOMING_ARC_OFFSET] = connectingArc;
		
		// Store the connection in the children map
		long key = getKey(parentNode, connectingArc);
		children.put(key, childNode);
	}
	
	private int getChild(int parentNode, int outgoingArc) {
		long key = getKey(parentNode, outgoingArc);
		return children.get(key);
	}
	
	private boolean hasChild(int parentNode, int outgoingArc) {
		long key = getKey(parentNode, outgoingArc);
		return children.containsKey(key);
	}
	
	private void linkToSuffix(int node, int suffixNode) {
		data[node*INTS_PER_NODE + SUFFIX_LINK_OFFSET] = suffixNode;
	}
	
	private int getIncomingArcValue(int node) {
		return data[node*INTS_PER_NODE + INCOMING_ARC_OFFSET];
	}
	
	private int getSuffixLink(int node) {
		return data[node*INTS_PER_NODE + SUFFIX_LINK_OFFSET];
	}
	
	
	private void setBounds(int node, int[] bounds) {
		data[node*INTS_PER_NODE + LOWER_BOUND_OFFSET] = bounds[0];
		data[node*INTS_PER_NODE + UPPER_BOUND_OFFSET] = bounds[1];
	}
	
	private int getLowerBound(int node) {
		return data[node*INTS_PER_NODE + LOWER_BOUND_OFFSET];
	}
	
	private int getUpperBound(int node) {
		return data[node*INTS_PER_NODE + UPPER_BOUND_OFFSET];
	}
	
	private int calculateSuffixLink(int node, int endOfPattern) {

		int suffixOfNode = getSuffixLink(node);
		int childOfSuffix = getChild(suffixOfNode, endOfPattern);

		return childOfSuffix;

	}
	
	private boolean hasExtensions(int node) {
		return hasChildren.get(node);
	}
	
	private static long getKey(int parentNode, int outgoingArc) {
		
		long key = (parentNode << BITS_PER_INT);
		
		key |= outgoingArc;
		
		return key;
		
	}
	
	public int getCapacity() {
		return capacity;
	}
}
