/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.prefix_tree;

import java.util.BitSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.RuleExtractor;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;

/**
 * Space-compact implementation of a prefix tree with suffix links.
 * <p>
 * <em>Note</em>: This class is under development 
 * and is <em>not</em> ready for prime-time.</em>
 * 
 * @author Lane Schwartz
 */
public class CompactPrefixTree {

	private static final Logger logger = Logger.getLogger(CompactPrefixTree.class.getName());
	
	static final boolean   ACTIVE = true;
	static final boolean INACTIVE = false;
	
	private static final int DEFAULT_CAPACITY = 512;
	private static final int DEFAULT_CAPACITY_INCREMENT = 512;
	
	/** Maximum number of nodes that can be stored in this tree. */
	private int capacity;
	
	/** 
	 * Value by which the capacity should be incremented if
	 * additional space is required to store more nodes.
	 */
	private int capacityIncrement;
	
	
	/** Indicates which nodes are active. */
	private final BitSet active;

	/** Indicates which nodes have children. */
	private final BitSet hasChildren;
	
	/**
	 * Stores several pieces of information compactly.
	 *
	 * For each node in the tree, the following integers are
	 * stored:
	 *
	 * <ul>
	 *   <li>Incoming arc value</li>
	 *   <li>Lower bound index</li>
	 *   <li>Upper bound index</li>
	 *   <li>Node ID of suffix link</li>
	 * </ul>
	 * 
	 * Each node is identified by a unique integer. This
	 * identifier is not explicitly stored. Rather, the identifier
	 * is implicitly stored as the index into the data structure.
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
	
	/** Suffix array representing the source language corpus. */
	final Suffixes suffixArray;
	
	/** Corpus array representing the target language corpus. */
	final Corpus targetCorpus;
	
	/**
	 * Represents alignments between words in the source corpus
	 * and the target corpus.
	 */
	final Alignments alignments;
	
	/** Lexical translation probabilities. */
	final LexicalProbabilities lexProbs;
	
	/** Source side symbol table */
	final SymbolTable vocab;
	
	/**
	 * Responsible for performing sampling and creating translation
	 * rules.
	 */
	final RuleExtractor ruleExtractor;
	
	/**
	 * Max span in the source corpus of any extracted hierarchical
	 * phrase
	 */
	final int maxPhraseSpan;   
	
	/**
	 * Maximum number of terminals plus nonterminals allowed
	 * in any extracted hierarchical phrase.
	 */
	final int maxPhraseLength;
	
	/**
	 * Maximum number of nonterminals allowed in any extracted
	 * hierarchical phrase.
	 */
	final int maxNonterminals;

	/**
	 * Minimum span in the source corpus of any nonterminal in
	 * an extracted hierarchical phrase.
	 */
	final int minNonterminalSpan;
	
	public CompactPrefixTree(Suffixes suffixArray, Corpus targetCorpus, Alignments alignments, SymbolTable vocab, LexicalProbabilities lexProbs, RuleExtractor ruleExtractor, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan) {
		this(DEFAULT_CAPACITY, DEFAULT_CAPACITY_INCREMENT, suffixArray, targetCorpus, alignments, vocab, lexProbs, ruleExtractor, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
	}
	
	public CompactPrefixTree(int capacity, int capacityIncrement, Suffixes suffixArray, Corpus targetCorpus, Alignments alignments, SymbolTable vocab, LexicalProbabilities lexProbs, RuleExtractor ruleExtractor, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan) {
		
		if (logger.isLoggable(Level.FINE)) logger.fine("\n\n\nConstructing new CompactPrefixTree\n\n");

		this.suffixArray = suffixArray;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.vocab = vocab;
		this.lexProbs = lexProbs;
		this.ruleExtractor = ruleExtractor;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
		
		this.capacity = capacity;
		this.capacityIncrement = capacityIncrement;
		this.active = new BitSet(capacity);
		this.hasChildren = new BitSet(capacity);
		this.data = new int[capacity * INTS_PER_NODE];
		
		// Insert root node
		this.data[ROOT_NODE_ID + INCOMING_ARC_OFFSET] = ROOT_NODE_INCOMING_ARC;
		this.size = 1;
		
		//TODO Deal with bot and botMap
		
		//TODO Deal with HierarchicalPhrases
		
		if (suffixArray != null) {
			int[] bounds = {0, suffixArray.size()-1};
			setBounds(ROOT_NODE_ID, bounds);
		}
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
	 * @param parentNode    Node to which a child will be added.
	 * @param connectingArc Integer representation of the word
	 *                      that connects the parent to the child.
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
		
		// Store the parentNode id in the highest 32 bits of the long
		long key = (parentNode << BITS_PER_INT);
		
		// Store the outgoingArc value in the lowest 32 bits of the long
		key |= outgoingArc;
		
		return key;
		
	}
	
	public int getCapacity() {
		return capacity;
	}
}
