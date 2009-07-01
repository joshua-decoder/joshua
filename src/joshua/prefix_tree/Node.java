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
package joshua.prefix_tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.AbstractGrammar;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;

/**
 * Represents a node in a prefix tree.
 * 
 * @author Lane Schwartz
 */
public class Node extends AbstractGrammar implements Comparable<Node>, Trie {

	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(Node.class.getName());

	/** Unique integer identifier for this node. */
	final int objectID;

	/**
	 * The lower bound in the suffix array
	 * for the source pattern at this node.
	 */
	int lowBoundIndex;

	/**
	 * The upper bound in the suffix array
	 * for the source pattern at this node.
	 */
	int highBoundIndex;

	/** Indicates whether this is an active node. */
	boolean active;
	
	/** Suffix link for this node. */
	Node suffixLink;

	/** 
	 * Maps from integer representations of words to nodes. 
	 * <p>
	 * TODO It may be better to have a single map in PrefixTree that maps (Node,Integer) --> Node
	 */
	Map<Integer,Node> children;
	
	/** Source side hierarchical phrases for this node. */
	MatchedHierarchicalPhrases sourceHierarchicalPhrases;
	
	/** Translation rules for this node. */
	List<Rule> results;
	
	Node() {
		this(true);
	}

	Node(boolean active) {
		this.active = active;
		this.suffixLink = null;
		this.children = new HashMap<Integer,Node>();
		this.objectID = nodeIDCounter++;
		this.sourceHierarchicalPhrases = null;//HierarchicalPhrases.emptyList((SymbolTable) null);
		this.results = Collections.emptyList();
	}
	
	Node calculateSuffixLink(int endOfPattern) {

		Node suffixLink = this.suffixLink.getChild(endOfPattern);

		if (suffixLink==null) {
			throw new NoSuchChildNodeException(this, endOfPattern);
		}
		
		return suffixLink;

	}
	
	/**
	 * Gets the representation of the source side tokens corresponding
	 * to the hierarchical phrases for this node.
	 * 
	 * @return the source language pattern for this node
	 */
	public Pattern getSourcePattern() {
		return sourceHierarchicalPhrases.getPattern();
	}
	
	
	/**
	 * Gets rules for this node and the children of this node.
	 *
	 * @return rules for this node and the children of this node.
	 */
	public List<Rule> getAllRules() {
		
		List<Rule> result = new ArrayList<Rule>(
				(results==null) ? Collections.<Rule>emptyList() : results);
			
		for (Node child : children.values()) {
			result.addAll(child.getAllRules());
		}
		
		return result;
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#getRules */
	public RuleCollection getRules() {
		
		Pattern sourcePattern = sourceHierarchicalPhrases.getPattern();
		
		int[] empty = {};
		
		final int[] sourceSide = (sourcePattern==null) ? empty : sourcePattern.getWordIDs();
		final int arity = (sourcePattern==null) ? 0 : sourcePattern.arity();
		
		//XXX Is results sorted at this point? It needs to be, but I'm not sure it is.
		logger.severe("Node sorted == " + this.isSorted());
		
		return new BasicRuleCollection(arity, sourceSide, results);
		
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#hasExtensions */
	public boolean hasExtensions() {
		return ! children.isEmpty();
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#hasRules */
	public boolean hasRules() {
		return ! sourceHierarchicalPhrases.isEmpty();
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#matchOne */
	public Trie matchOne(int symbol) {
		if (children.containsKey(symbol)) {
			return children.get(symbol);
		} else {
			return null;
		}
	}

	/* See Javadoc for joshua.decoder.ff.tm.Trie#getExtensions */
	public Collection<Node> getExtensions() {
		return this.children.values();
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Grammar#getTrieRoot */
	public Trie getTrieRoot() {
		return this;
	}
	
	/**
	 * Determines whether this node has a specified child.
	 * 
	 * @param child
	 * @return <code>true</code> if this node has a specified child,
	 *         <code>false</code> otherwise
	 */
	public boolean hasChild(int child) {
		return children.containsKey(child);
	}

	public Node getChild(int child) {
		return children.get(child);
	}

	public Node addChild(int child) {
		if (children.containsKey(child)) {
			throw new ChildNodeAlreadyExistsException(this, child);
		} else {
			Node node = new Node();
			children.put(child, node);
			return node;
		}
	}

	/**
	 * Sets the suffix link for this node.
	 * 
	 * @param suffix Suffix link for this node
	 */
	public void linkToSuffix(Node suffix) {
		this.suffixLink = suffix;
	}

	/**
	 * Sets the lower and upper bounds in the suffix array
	 * where the source pattern associated with this node
	 * are located.
	 * 
	 * @param lowBound the lower bound in the suffix array
	 *                 for the source pattern at this node
	 * @param highBound the upper bound in the suffix array
	 *                 for the source pattern at this node
	 */
	public void setBounds(int lowBound, int highBound) {
		lowBoundIndex = lowBound;
		highBoundIndex = highBound;
	}


	/**
	 * Stores in this node a list of source language hierarchical
	 * phrases, the associated source language pattern, and the
	 * list of associated translation rules.
	 * <p>
	 * This method is responsible for creating and storing
	 * translation rules from the provided list of source
	 * language hierarchical phrases.
	 * 
	 * @param hierarchicalPhrases Source language hierarchical phrases.
	 */
	public void storeResults(MatchedHierarchicalPhrases hierarchicalPhrases, List<Rule> rules) {
		
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Storing " + hierarchicalPhrases.size() + " source phrases at node " + objectID + ":");
		}

		this.sourceHierarchicalPhrases = hierarchicalPhrases;
		this.results = rules;
		
	}



	/**
	 * Gets the number of rules stored in the grammar.
	 * 
	 * @return the number of rules stored in the grammar
	 */
	public int getNumRules() {
		
		int numRules = 
			(results==null) ? 0 : results.size();

		if (children != null) {
			for (Node child : children.values()) {
				numRules += child.getNumRules();
			}
		}
		
		return numRules;
	}
	
	/**
	 * Gets the number of nodes in the sub-tree rooted at this node.
	 * <p>
	 * This method recursively traverses through all nodes
	 * in the sub-tree every time this method is called.
	 * 
	 * @return the number of nodes in the sub-tree rooted at this node
	 */
	public int size() {

		int size = 1;

		for (Node child : children.values()) {
			size += child.size();
		}

		return size;
	}



	/* See Javadoc for java.lang.Object#hashCode */
	public int hashCode() {
		return objectID*31;
	}
	
	/**
	 * Compares this node to another node
	 * based solely on their respective objectIDs.
	 * 
	 * @param o Another node
	 * @return <code>true</code> if this node's objectID 
	 *         is equal to the other objectID,
	 *         false otherwise
	 */
	public boolean equals(Object o) {
		if (this==o) {
			return true;
		} else if (o instanceof Node) {
			Node other = (Node) o;
			return (objectID == other.objectID);
		} else {
			return false;
		}
	}
	
	/**
	 * Compares this node to another node
	 * based solely on their respective objectIDs.
	 * 
	 * @param o Another node
	 * @return -1 if this node's objectID is less than the other objectID,
	 *          0 if this node's objectID is equal to the other objectID,
	 *          1 if this node's objectID is greater than the other objectID
	 */
	public int compareTo(Node o) {
		Integer i = objectID;
		Integer j = o.objectID;

		return i.compareTo(j);
	}
	
	/**
	 * Gets a String representation of the sub-tree rooted at this node.
	 * 
	 * @return a String representation of the sub-tree rooted at this node
	 */
	public String toString(SymbolTable vocab, int incomingArcValue) {
		
		StringBuilder s = new StringBuilder();

		s.append("[id");
		s.append(objectID);
		s.append(' ');
		
		if (incomingArcValue==PrefixTree.X) {
			s.append('X');
		} else if (incomingArcValue==PrefixTree.ROOT_NODE_ID) {
			s.append("ROOT");
		} else if (vocab!=null) {
			s.append(vocab.getWord(incomingArcValue));
		} else {
			s.append('v');
			s.append(incomingArcValue);
		} 

		s.append(" (");
		if (null != suffixLink) {
			s.append(suffixLink.objectID);
		} else {
			s.append("null");
		}
		s.append(')');
		s.append(' ');

		ArrayList<Map.Entry<Integer, Node>> k = new ArrayList<Map.Entry<Integer, Node>>(children.entrySet());
		Collections.sort(k, NodeEntryComparator.get());
		
		for (Map.Entry<Integer, Node> kidEntry : k) {
			Integer arcValue = kidEntry.getKey();
			Node kid = kidEntry.getValue();
			
			s.append(kid.toString(vocab, arcValue));
			s.append(' ');
		}

		if (!active) s.append('*');
		s.append(']');

		return s.toString();

	}

	String toShortString(SymbolTable vocab) {
		
		StringBuilder s = new StringBuilder();

		s.append("[id");
		s.append(objectID);
		s.append(' ');
		
		s.append(" (");
		if (null != suffixLink) {
			s.append(suffixLink.objectID);
		} else {
			s.append("null");
		}
		s.append(')');
		s.append(' ');

		s.append('{');
		s.append(children.size());
		s.append(" children}");

		if (!active) s.append('*');
		s.append(']');

		return s.toString();
	}
	
	protected String toTreeString(String tabs, SymbolTable vocab, int incomingArcValue) {

		StringBuilder s = new StringBuilder();

		s.append(tabs); 
		s.append("[id");
		s.append(objectID);
		s.append(' ');

		if (incomingArcValue==PrefixTree.X) {
			s.append('X');
		} else if (incomingArcValue==PrefixTree.ROOT_NODE_ID) {
			s.append("ROOT");
		} else if (vocab!=null) {
			s.append(vocab.getWord(incomingArcValue));
		} else {
			s.append('v');
			s.append(incomingArcValue);
		} 

		s.append(" (");
		if (null != suffixLink) {
			s.append(suffixLink.objectID);
		} else {
			s.append("null");
		}
		s.append(')');

		if (children.size() > 0) {
			s.append(" \n\n");

			ArrayList<Map.Entry<Integer, Node>> k = new ArrayList<Map.Entry<Integer, Node>>(children.entrySet());
			Collections.sort(k, NodeEntryComparator.get());

			for (Map.Entry<Integer, Node> kidEntry : k) {
				Integer arcValue = kidEntry.getKey();
				Node kid = kidEntry.getValue();

				s.append(kid.toTreeString(tabs+"\t", vocab, arcValue));
				s.append(' ');
			}

			s.append(tabs);
		} else {
			s.append(' ');
		}

		if (!active) s.append('*');
		s.append(']');

		return s.toString();

	}
	

	static int nodeIDCounter = 0;
	
	static void resetNodeCounter() {
		nodeIDCounter = 0;
	}
}
