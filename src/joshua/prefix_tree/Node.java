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
import joshua.corpus.RuleExtractor;
import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.util.Cache;

/**
 * Represents a node in a prefix tree.
 * 
 * @author Lane Schwartz
 */
public class Node implements Comparable<Node>, Trie {

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
	
	
//	List<Rule> results;
	
	protected final ParallelCorpusGrammarFactory parallelCorpus;
	
//	private final Suffixes suffixArray;
//	private final Cache<Pattern, List<Rule>> ruleCache;
//	private final Cache<Pattern, MatchedHierarchicalPhrases> matchedPhrasesCache;
	
	Pattern sourcePattern;
	
	
	
	
////================================	
//	//add by zhifei??????????????????????????????????????????? these parameters are not intialized by the constructor
//	public static final int OOV_RULE_ID = 0;
//	private int defaultOwner;
//	private float oovFeatureCost = 100;
//	
//	/**
//	 * the OOV rule should have this lhs, this should be grammar
//	 * specific as only the grammar knows what LHS symbol can
//	 * be combined with other rules
//	 */ 
//	private int defaultLHS;
//	private int spanLimit = 10;
////==============================	
//	
	
	/** 
	 * Gets translation rules for this node. 
	 * <p>
	 * The results of this method are guaranteed to be 
	 * sorted according to whatever feature functions are in use.
	 * 
	 * Calling this method will return results equivalent to those 
	 * that would be returned by calling 
	 * <code>HierarchicalRuleExtractor#extractRules(getMatchedPhrases())</code>.
	 * 
	 * @see RuleExtractor#extractRules(MatchedHierarchicalPhrases)
	 * @return translation rules for this node
	 */
	protected List<Rule> getResults() {
		
		Cache<Pattern,List<Rule>> ruleCache = parallelCorpus.getSuffixArray().getCachedRules();
		
		List<Rule> results;
		
		if (ruleCache.containsKey(sourcePattern)) {
			results = ruleCache.get(sourcePattern);
			// The rules from the cache are guaranteed to be sorted.
		} else {
			results = parallelCorpus.getRuleExtractor().extractRules(getMatchedPhrases());
			// The above list of rules extracted is guaranteed to be sorted.
			ruleCache.put(sourcePattern, results);
		}
		
		// These rules are sorted.
		return results;
	}
	
	protected MatchedHierarchicalPhrases getMatchedPhrases() {
		
		//TODO Implement this method
		return this.sourceHierarchicalPhrases;
		
//		MatchedHierarchicalPhrases results;
//		
//		if (matchedPhrasesCache.containsKey(sourcePattern)) {
//			results = matchedPhrasesCache.get(sourcePattern);
//		} else {
//			
//			// Do some extra lookup
//			
//			
//			throw new RuntimeException("This code not yet implemented");
//			
//		}
//		
//		return results;
	}
	
	Node(Node parent) {
//		this(parent.ruleCache, parent.matchedPhrasesCache, true);
		this(parent.parallelCorpus, true, nodeIDCounter++);
	}
	
	Node(ParallelCorpusGrammarFactory parallelCorpus, int objectID) {
		this(parallelCorpus, true, objectID);
//		this(
//			(suffixArray==null ? null : suffixArray.getCachedRules()), 
//			(suffixArray==null ? null : suffixArray.getCachedHierarchicalPhrases()), 
//			true, objectID);
	}
	
	Node(ParallelCorpusGrammarFactory parallelCorpus, boolean active) {
		this(parallelCorpus, active, nodeIDCounter++);
	}
	
	
//	Node(Cache<Pattern, List<Rule>> ruleCache, Cache<Pattern, MatchedHierarchicalPhrases> matchedPhrasesCache, boolean active) {
//		this(ruleCache, matchedPhrasesCache, active, nodeIDCounter++);
//	}
	
//	Node(Cache<Pattern, List<Rule>> ruleCache, Cache<Pattern, MatchedHierarchicalPhrases> matchedPhrasesCache, boolean active, int objectID) {
	Node(ParallelCorpusGrammarFactory parallelCorpus, boolean active, int objectID) {
//		this.ruleCache = ruleCache;
//		this.matchedPhrasesCache = matchedPhrasesCache;
		this.parallelCorpus = parallelCorpus;
//		this.suffixArray = suffixArray;
		this.active = active;
		this.suffixLink = null;
		this.children = new HashMap<Integer,Node>();
		this.objectID = objectID;
		this.sourceHierarchicalPhrases = null;//HierarchicalPhrases.emptyList((SymbolTable) null);
//		this.results = Collections.emptyList();
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
//		return sourceHierarchicalPhrases.getPattern();
		return sourcePattern;
	}
	
	
	/**
	 * Gets rules for this node and the children of this node.
	 *
	 * @return rules for this node and the children of this node.
	 */
	public List<Rule> getAllRules() {
		
		List<Rule> results = this.getResults();
		
		List<Rule> result = new ArrayList<Rule>(
				(results==null) ? Collections.<Rule>emptyList() : results);
			
		for (Node child : children.values()) {
			result.addAll(child.getAllRules());
		}
		
		return result;
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#getRules */
	public RuleCollection getRules() {
				
		final int[] sourceSide = 
			(sourcePattern==null) 
			? new int[]{}  
			: sourcePattern.getWordIDs();
			
		final int arity = 
			(sourcePattern==null) 
			? 0 
			: sourcePattern.arity();
		
		List<Rule> results = this.getResults();
		
		return new BasicRuleCollection(arity, sourceSide, results);
		
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#hasExtensions */
	public boolean hasExtensions() {
		return ! children.isEmpty();
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#hasRules */
	public boolean hasRules() {
		
		if (active) {
			MatchedHierarchicalPhrases sourceHierarchicalPhrases = this.getMatchedPhrases();

			return ! sourceHierarchicalPhrases.isEmpty();
		} else {
			return false;
		}
	}
	
	/* See Javadoc for joshua.decoder.ff.tm.Trie#matchOne */
	public Trie matchOne(int symbol) {
		if (children.containsKey(symbol)) {
			Node child = children.get(symbol);
			if (child.active) {
				return child;
			} else {
				return null;
			}
//			return children.get(symbol);
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
			Node node = new Node(this);
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

		this.sourcePattern = hierarchicalPhrases.getPattern();
//		this.matchedPhrasesCache.put(sourcePattern, hierarchicalPhrases);
		
		//This is not needed, because this is put into the cache by HierarchicalRuleExtractor
//		this.parallelCorpus.getSuffixArray().getCachedRules().put(sourcePattern, rules);
		
		this.sourceHierarchicalPhrases = hierarchicalPhrases;
		
//		int numPhrases = hierarchicalPhrases.size();
//		if (numPhrases > 0) {
//			int lowerBound = hierarchicalPhrases.getFirstTerminalIndex(0);
//			int upperBound = hierarchicalPhrases.getFirstTerminalIndex(numPhrases-1);
//			this.setBounds(lowerBound, upperBound);
//		}
//		this.results = rules;
		
	}



	/**
	 * Gets the number of rules stored in the grammar.
	 * 
	 * @return the number of rules stored in the grammar
	 */
	public int getNumRules() {
		
		List<Rule> results = this.getResults();
		
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
		
		if (incomingArcValue==SymbolTable.X) {
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

		if (incomingArcValue==SymbolTable.X) {
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
	

	static int nodeIDCounter = 2;
	
	static void resetNodeCounter() {
		nodeIDCounter = 2;
	}

//	public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores, int arity) {
//		return new BilingualRule(lhs, sourceWords, targetWords, scores, arity, this.defaultOwner, 0, getOOVRuleID());
//	}
//	
//
//
//	public int getOOVRuleID() {
//		return OOV_RULE_ID;
//	}
//	/** 
//	 * if the span covered by the chart bin is greater than the
//	 * limit, then return false
//	 */
//	public boolean hasRuleForSpan(int startIndex,	int endIndex,	int pathLength) {
//		if (this.spanLimit == -1) { // mono-glue grammar
//			return (startIndex == 0);
//		} else {
//			return (endIndex - startIndex <= this.spanLimit);
//		}
//	}
//
//	public Rule constructOOVRule(int qtyFeatures, int sourceWord, int targetWord, boolean hasLM) {
//		int[] french      = new int[1];
//		french[0]         = sourceWord;
//		int[] english       = new int[1];
//		english[0]          = targetWord;
//		float[] feat_scores = new float[qtyFeatures];
//		
//		// TODO: This is a hack to make the decoding without a LM works
//		/**when a ngram LM is used, the OOV word will have a cost 100.
//		 * if no LM is used for decoding, so we should set the cost of some
//		 * TM feature to be maximum
//		 * */
//		if ( (!hasLM) && qtyFeatures > 0) { 
//			feat_scores[0] = oovFeatureCost;
//		}
//		
//		return new BilingualRule(this.defaultLHS, french, english, feat_scores, 0, this.defaultOwner, 0, getOOVRuleID());
//	}

}
