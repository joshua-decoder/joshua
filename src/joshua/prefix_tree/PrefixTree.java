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

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.RuleExtractor;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.lexprob.LexicalProbabilities;
import joshua.corpus.suffix_array.HierarchicalPhrases;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a prefix tree with suffix links, for use in extracting
 * hierarchical phrase-based statistical translation rules.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-11-13 13:13:31 -0600 (Thu, 13 Nov 2008) $
 */
public class PrefixTree {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(PrefixTree.class.getName());

	/**
	 * Integer representation of the nonterminal X. All
	 * nonterminals are guaranteed to be represented by negative
	 integers.
	 */
	public static final int X = -1;
	
	/** Operating system-specific end of line character(s). */
	static final byte[] newline = System.getProperty("line.separator").getBytes();
	
	/** Root node of this tree. */
	final Node root;

	/**
	 * Responsible for performing sampling and creating translation
	 * rules.
	 */
	final RuleExtractor ruleExtractor;
	
	/**
	 * Max span in the source corpus of any extracted hierarchical
	 * phrase.
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
	
	
	/**
	 * Represents a very high cost, corresponding to a very
	 * unlikely probability.
	 */
	static final float VERY_UNLIKELY = -1.0f * (float) Math.log(1.0e-9);
	
	/** 
	 * Indicates whether rules with an initial source-side
	 * nonterminal should be extracted from phrases at the start
	 * of a sentence, even though such rules do not have
	 * supporting corporal evidence.
	 * <p>
	 * This is included for compatibility with Adam Lopez's
	 * Hiero rule extractor, in which this setting is set to
	 * <code>true</code>.
	 * <p>
	 * The default value is <code>false</code>.
	 */
	static boolean SENTENCE_INITIAL_X = false;
	
	/** 
	 * Indicates whether rules with a final source-side nonterminal 
	 * should be extracted from phrases at the end of a sentence,
	 * even though such rules do not have supporting corporal
	 * evidence.
	 * <p>
	 * This is included for compatibility with Adam Lopez's
	 * Hiero rule extractor, in which this setting is set to
	 * <code>true</code>.
	 * <p>
	 * The default value is <code>false</code>.
	 */
	static boolean SENTENCE_FINAL_X = false;
	
	
	static boolean EDGE_X_MAY_VIOLATE_PHRASE_SPAN = false;
	
	
	/** Unique integer identifier for the root node. */
	static final int ROOT_NODE_ID = -999;
	
	/** 
	 * Unique integer identifier for the special ⊥ node
	 * that represents the suffix of the root node.
	 * @see Lopez (2008), footnote 9 on p73
	 */
	static final int BOT_NODE_ID = -2000;

	/**
	 * Gets a special map that maps any integer key to the root
	 * node.
	 *
	 * @param root Root node, which this map will always return
	 *             as a value.
	 * @return Special map that maps any integer key to the
	 *         root node.
	 * @see "Lopez (2008), footnote 9 on p73"
	 */
	static Map<Integer,Node> botMap(final Node root) {
		return new Map<Integer,Node>() {
			public void clear() { throw new UnsupportedOperationException(); }
			public boolean containsKey(Object key) { return true; }
			public boolean containsValue(Object value) { return value==root; }
			public Set<java.util.Map.Entry<Integer, Node>> entrySet() { throw new UnsupportedOperationException(); }
			public Node get(Object key) { return root; }
			public boolean isEmpty() { return false; }
			public Set<Integer> keySet() { throw new UnsupportedOperationException(); }
			public Node put(Integer key, Node value) { throw new UnsupportedOperationException(); }
			public void putAll(Map<? extends Integer, ? extends Node> t) { throw new UnsupportedOperationException(); }
			public Node remove(Object key) { throw new UnsupportedOperationException(); }
			public int size() { throw new UnsupportedOperationException(); }
			public Collection<Node> values() { return Collections.singleton(root); }		
		};
	}

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
	
	/** Symbol table */
	final SymbolTable vocab;
	
	/** Empty pattern */
	final Pattern epsilon;
	
	/** 
	 * Node representing phrases that start with the nonterminal
	 * X. This node's parent is the root node of the tree.
	 */
	private final Node xnode;

	/**
	 * Constructs a new prefix tree with suffix links using the
	 * GENERATE_PREFIX_TREE algorithm from Lopez (2008) PhD
	 * Thesis, Algorithm 2, p 76.
	 * 
	 * @param suffixArray
	 * @param targetCorpus
	 * @param alignments
	 * @param vocab
	 * @param lexProbs
	 * @param ruleExtractor
	 * @param maxPhraseSpan
	 * @param maxPhraseLength
	 * @param maxNonterminals
	 * @param minNonterminalSpan Minimum number of source
	 *            language tokens a nonterminal is allowed to
	 *            encompass.
	 */
	public PrefixTree(Suffixes suffixArray, 
			Corpus targetCorpus, Alignments alignments, SymbolTable vocab, 
			LexicalProbabilities lexProbs, RuleExtractor ruleExtractor, 
			int maxPhraseSpan, int maxPhraseLength, 
			int maxNonterminals, int minNonterminalSpan) {

		
		if (logger.isLoggable(Level.FINE)) logger.fine("\n\n\nConstructing new PrefixTree\n\n");

		this.suffixArray = suffixArray;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.lexProbs = lexProbs;
		this.ruleExtractor = ruleExtractor;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
		this.vocab = vocab;

		Node bot = new Node(this,BOT_NODE_ID);
		bot.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(vocab);
		
		this.root = new Node(this,ROOT_NODE_ID);
		bot.children = botMap(root);
		this.root.linkToSuffix(bot);


		
//		if (suffixArray==null) {
////			vocab = null;
//		} else {
		if (suffixArray != null) {
//			vocab = suffixArray.getVocabulary();
			int[] bounds = {0, suffixArray.size()-1};
			root.setBounds(bounds);
		}
		root.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(vocab);

		// Define epsilon to be an empty pattern
		epsilon = new Pattern(vocab);

		
		// 1: children(p_eps) <-- children(p_eps) U p_x

		if (maxNonterminals > 0) {	// Create and set up the X node that comes off of ROOT
			
			// Add a link from root node to X
			xnode = root.addChild(X);
			
			{ 	// Set the list of hierarchical phrases be for the X node that comes off of ROOT to an empty list.
				// Alternatively, one could consider every phrase in the corpus to match here.
				xnode.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(vocab);
				if (suffixArray != null)
					xnode.sourcePattern = new Pattern(suffixArray.getVocabulary(), X);
				
				// Set the bounds of the X node to be the entire suffix array.
				if (suffixArray!=null) {
					int[] bounds = {0, suffixArray.size()-1};
					xnode.setBounds(bounds);
				}
			}

			// Add a suffix link from X back to root
			Node suffixLink = root.calculateSuffixLink(X);

			if (logger.isLoggable(Level.FINEST)) {
				String oldSuffixLink = (xnode.suffixLink==null) ? "null" : "id"+xnode.suffixLink.objectID;
				String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
				logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xnode.toShortString() + " with token " + X);
			}

			xnode.linkToSuffix(suffixLink);
		} else {
			this.xnode = null;
		}

		if (logger.isLoggable(Level.FINEST)) logger.finest("CURRENT TREE:  " + root);

	}

	/**
	 * Constructs a new prefix tree with suffix links using the
	 * GENERATE_PREFIX_TREE algorithm from Lopez (2008) PhD
	 * Thesis, Algorithm 2, p 76.
	 * <p>
	 * This constructor does not take a suffix array parameter.
	 * Instead any prefix tree constructed by this constructor
	 * will assume that all possible phrases of this sentence
	 * are valid phrases.
	 * <p>
	 * This constructor is meant to be used primarily for testing
	 * purposes.
	 *
	 * @param sentence
	 * @param maxPhraseSpan
	 * @param maxPhraseLength
	 * @param maxNonterminals
	 */
	PrefixTree(SymbolTable vocab, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals) {
		this(null, null, null, vocab, null, null, maxPhraseSpan, maxPhraseLength, maxNonterminals, 2);
	}


	/**
	 * Modify this prefix tree by adding phrases for this
	 * sentence.
	 *
	 * @param sentence
	 */
	public void add(int[] sentence) {
		
		int START_OF_SENTENCE = 0;
		int END_OF_SENTENCE = sentence.length - 1;
		
		Queue<Tuple> queue = new LinkedList<Tuple>();

		if (logger.isLoggable(Level.FINE)) logger.fine("Last sentence index == I == " + END_OF_SENTENCE);

		// 2: for i from 1 to I
		for (int i=START_OF_SENTENCE; i<=END_OF_SENTENCE; i++) {
			//if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + i + ","+ i +","+root+",{"+intToString(sentence[i])+"})");
			if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (\u03b5," + i + ","+ i +","+root.toShortString() +")");
			
			// 3: Add <f_i, i, i+1, p_eps> to queue
			queue.add(new Tuple(epsilon, i, i, root));
		}

		if (this.maxNonterminals > 0) {	Pattern xpattern = new Pattern(vocab,X);
			
			int start = START_OF_SENTENCE;
			if (!SENTENCE_INITIAL_X) start += 1;
		
			// 4: for i from 1 to I
			for (int i=start; i<=END_OF_SENTENCE; i++) {
				//if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + (i-1) + ","+(i)+","+root+",{"+X+","+intToString(sentence[i])+"})");
				if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (X," + (i-1) + ","+ i +","+xnode.toShortString() +")");
				
				// 5: Add <X f_i, i-1, i+1, p_x> to queue
				if (EDGE_X_MAY_VIOLATE_PHRASE_SPAN) {
					queue.add(new Tuple(xpattern, i, i, xnode));	
				} else {
					queue.add(new Tuple(xpattern, i-1, i, xnode));
				}
			}
		}


		// 6: While queue is not empty do
		while (! queue.isEmpty()) {

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("\n");
				if (logger.isLoggable(Level.FINEST)) logger.finest("CURRENT TREE:      " + root);
			}
			
			// 7: Pop <alpha, i, j, p_alphaBeta> from queue
			Tuple tuple = queue.remove();

			int i = tuple.spanStart;
			int j = tuple.spanEnd;
			Node prefixNode = tuple.prefixNode;
			Pattern prefixPattern = tuple.pattern;

//			if (prefixNode.objectID==329 //) {
//					|| (prefixNode.objectID==28 && i==13 && j==17)) {
//				int x = -1;
//				x++;
//			}
			
			if (logger.isLoggable(Level.FINE)) logger.fine("Have tuple (" +prefixPattern+","+ i + ","+j+","+prefixNode.toShortString()+")");

			if (j <= END_OF_SENTENCE) {

				// 8: If p_alphaBetaF_i elementOf children(p_alphaBeta) then
				if (prefixNode.hasChild(sentence[j])) {

					if (logger.isLoggable(Level.FINER)) logger.finer("EXISTING node for \"" + sentence[j] + "\" from " + prefixNode.toShortString() + " to node " + prefixNode.getChild(sentence[j]).toShortString() + " with pattern " + prefixPattern);

					// child is p_alphaBetaF_j
					Node child = prefixNode.getChild(sentence[j]);
					
					// 9: If p_alphaBetaF_j is inactive then
					if (child.active == Node.INACTIVE) {
						
						// 10: Continue to next item in queue
						continue;
						
						// 11: Else
					} else { 
						
						// 12: EXTEND_QUEUE(alpha beta f_j, i, j, f_1^I)
						if (logger.isLoggable(Level.FINER)) {
							logger.finer("Calling EXTEND_QUEUE("+i+","+j+","+prefixPattern+","+prefixNode.toShortString());
							if (logger.isLoggable(Level.FINEST)) logger.finest("TREE BEFOR EXTEND: " + root);
						}
						extendQueue(queue, i, j, sentence, new Pattern(prefixPattern,sentence[j]), child);
						if (logger.isLoggable(Level.FINEST)) logger.finest("TREE AFTER EXTEND: " + root);
						
					}

				} else { // 13: Else

					// 14: children(alphaBeta) <-- children(alphaBeta) U p_alphaBetaF_j
					//     (Add new child node)
					if (logger.isLoggable(Level.FINER)) logger.finer("Adding new node to node " + prefixNode.toShortString());
					Node newNode = prefixNode.addChild(sentence[j]);
					if (logger.isLoggable(Level.FINER)) {
						String word = (suffixArray==null) ? ""+sentence[j] : suffixArray.getVocabulary().getWord(sentence[j]);
						logger.finer("Created new node " + newNode.toShortString() +" for \"" + word + "\" and \n  added it to " + prefixNode.toShortString());
					}


					// 15: p_beta <-- suffix_link(p_alpha_beta)
					//     suffixNode in this code is p_beta_f_j, not p_beta
					Node suffixNode = prefixNode.calculateSuffixLink(sentence[j]);

					if (logger.isLoggable(Level.FINEST)) {
						String oldSuffixLink = (newNode.suffixLink==null) ? "null" : "id"+newNode.suffixLink.objectID;
						String newSuffixLink = (suffixNode==null) ? "null" : "id"+suffixNode.objectID;
						logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + newNode.toShortString() + " (prefix node " + prefixNode.toShortString() + " ) with token " + sentence[j]);
					}
					
					newNode.linkToSuffix( suffixNode );


					// 16: if p_beta_f_j is inactive then
					if (suffixNode.active == Node.INACTIVE) {
						
						// 17: Mark p_alpha_beta_f_j inactive
						newNode.active = Node.INACTIVE;
						
						// 18: else
					} else { 

						Pattern extendedPattern = new Pattern(prefixPattern,sentence[j]);

						MatchedHierarchicalPhrases result = null;
						
						if (suffixArray != null) {
							
							// 19: Q_alpha-beta-f_j <-- query(alpha-beta-f_j, Q_alpha-beta, Q_beta-f_j)
							result = query(extendedPattern, newNode, prefixNode, suffixNode);
							
						}

						// 20: if Q_alpha_beta_f_j = ∅ (meaning that no results were found for this query)
						//if (result != null && result.isEmpty()) {// && prefixNode != xnode) {
						if (result != null && result.isEmpty()) {
							
							// 21: Mark p_alpha_beta_f_j inactive
							newNode.active = Node.INACTIVE;
							
							// 22: else
						} else {
							
							// 23: Mark p_alpha_beta_f_j active
							newNode.active = Node.ACTIVE;
							
							// 24: EXTEND_QUEUE(alpha beta f_j, i, j, f_1^I)
							extendQueue(queue, i, j, sentence, extendedPattern, newNode);
							
						}
					}
				}
			}

		}

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("\n");
			if (logger.isLoggable(Level.FINEST)) logger.finest("FINAL TREE:  " + root);
		}
	}
	

	/**
	 * Implements the root QUERY algorithm (Algorithm 4) of
	 * Adam Lopez's (2008) doctoral thesis.
	 *
	 * @param pattern Pattern to search for
	 * @param node Node in the prefix tree
	 * @param prefixNode Prefix node
	 * @param suffixNode Suffix node
	 * @return List of matched hierarchical phrases for the specified pattern.
	 * 
	 * @see "Lopez (2008)"
	 */
	public MatchedHierarchicalPhrases query(Pattern pattern, Node node, Node prefixNode, Node suffixNode) {

		if (logger.isLoggable(Level.FINE)) logger.fine("PrefixTree.query( " + pattern + ",\n\t   new node " + node + ",\n\tprefix node " + prefixNode + ",\n\tsuffix node " + suffixNode + ")");
		
		
		MatchedHierarchicalPhrases result;

		int arity = pattern.arity();
		
		// 1: if alpha=u then
		//    If the pattern is contiguous, look up the pattern in the suffix array
		if (arity == 0) {

			// 2: SUFFIX-ARRAY-LOOKUP(SA_f, a alpha b, l_a_alpha, h_a_alpha
			// Get the first and last index in the suffix array for the specified pattern
			int[] bounds = suffixArray.findPhrase(pattern, 0, pattern.size(), prefixNode.lowBoundIndex, prefixNode.highBoundIndex);
			if (bounds==null) {
				result = HierarchicalPhrases.emptyList(vocab);
				//TOOD Should node.setBounds(bounds) be called here?
			} else {
				node.setBounds(bounds);
				int[] startingPositions = suffixArray.getAllPositions(bounds);
				result = suffixArray.createHierarchicalPhrases(startingPositions, pattern, vocab);
			}
			
		} else { // 3: else --- alpha is a discontiguous pattern

			// 8: If M_a_alpha_b has been precomputed (then result will be non-null)
			// 9: Retrieve M_a_alpha_b from cache of precomputations
			result = suffixArray.getMatchingPhrases(pattern);
			
			// 10: else
			if (result == null) {
				
				// 16: M_a_alpha_b <-- QUERY_INTERSECT(M_a_alpha, M_alpha_b)
				
				int[] sourceWords = prefixNode.sourcePattern.getWordIDs();
				
				// Special handling of case when prefixNode is the X off of root (hierarchicalPhrases for that node is empty)
				//if (arity==1 && prefixNode.sourcePattern.startsWithNonterminal() && prefixNode.sourcePattern.endsWithNonterminal())
				if (arity==1 && sourceWords[0] < 0 && sourceWords[sourceWords.length-1] < 0){
				//prefixNode.sourcePattern.words[prefixNode.sourcePattern.words.length-1] < 0
					
					
//					SymbolTable vocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
					
//					int[] xwords = new int[suffixNode.sourcePattern.words.length+1];
//					xwords[0] = X;
//					for (int i=0; i<suffixNode.sourcePattern.words.length; i++) {
//						xwords[i+1] = suffixNode.sourcePattern.words[i];
//					}
//					Pattern xpattern = new Pattern(vocab, xwords);
//					result = suffixNode.sourceHierarchicalPhrases.copyWith(xpattern);
					result = suffixNode.sourceHierarchicalPhrases.copyWithInitialX();
//					result = new HierarchicalPhrases(xpattern, suffixNode.sourceHierarchicalPhrases);

				} else { 
					
					// Normal query intersection case (when prefixNode != X off of root)
					
					if (logger.isLoggable(Level.FINEST)) logger.finest("Calling queryIntersect("+pattern+" M_a_alpha.pattern=="+prefixNode.sourcePattern + ", M_alpha_b.pattern=="+suffixNode.sourcePattern+")");
					
					result = HierarchicalPhrases.queryIntersect(pattern, prefixNode.sourceHierarchicalPhrases, suffixNode.sourceHierarchicalPhrases, minNonterminalSpan, maxPhraseSpan);
				}
				
				suffixArray.setMatchingPhrases(pattern, result);
			}
		}

		// 17: Return M_a_alpha_b
		node.storeResults(result, pattern);
//		node.storeResults(result, pattern.words);
		return result;

	}
	
	/**
	 * Implements Function EXTEND_QUEUE from Lopez (2008) PhD
	 * Thesis, Algorithm 2, p 76
	 *
	 * @param queue Queue of tuples
	 * @param i Start index of the pattern in the source input
	 *          sentence (inclusive, 1-based).
	 * @param j End index of the pattern in the source input
	 *          sentence (inclusive, 1-based).
	 * @param sentence
	 * @param pattern Pattern corresponding to the prefix node.
	 *                In Lopez's terminology, this pattern is
	 *                alpha f_j.
	 * @param node Node in the prefix tree to which a new node
	 *             (corresponding to the pattern) will eventually
	 *             be attached.
	 */
	private void extendQueue(Queue<Tuple> queue, int i, int j, int[] sentence, Pattern pattern, Node node) {

		int J = j;
		if (!SENTENCE_FINAL_X) J += 1;

		int endOfPhraseSpan = (j+1)-i+1;

		
		// 1: if |alpha| < MaxPhraseLength  and  j-i+1<=MaxPhraseSpan then 		
		if (pattern.size() < maxPhraseLength  && J<sentence.length) {

			if (endOfPhraseSpan <= maxPhraseSpan) {
				// 2: Add <alpha f_j, i, j+1, p_alpha> to queue
				//    (add new tuple to the queue)
				if (logger.isLoggable(Level.FINEST)) logger.finest("\nextendQueue: Adding tuple (" +pattern+","+ i + ","+ (j+1) +","+node+")");//(new Pattern(alphaPattern,sentence[j+1]))+"})");
				queue.add(new Tuple(pattern, i, j+1, node));//, sentence[j+1]));
			}

			if (EDGE_X_MAY_VIOLATE_PHRASE_SPAN) endOfPhraseSpan -= 1;
			
			// 3: if arity(alpha) < MaxNonterminals then
			if (pattern.arity() < maxNonterminals && endOfPhraseSpan <= maxPhraseSpan) {
				Node xNode;

				if (! node.children.containsKey(X)) {

					// 4: children(p_alpha) <-- children(p_alpha) U p_alphaX
					//    (add new child node in tree and mark in as active)
					xNode = node.addChild(X);
					if (logger.isLoggable(Level.FINEST)) logger.finest("Adding node for \"" + X + "\" from " + node + " to new node " + xNode + " with alphaPattern " + pattern + "  (in extendQueue)");

					Node suffixLink = node.calculateSuffixLink(X);

					if (logger.isLoggable(Level.FINEST)) {
						String oldSuffixLink = (xNode.suffixLink==null) ? "null" : "id"+xNode.suffixLink.objectID;
						String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
						logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xNode + " (prefix node " + node + " ) with token " + X);
					}

					xNode.linkToSuffix( suffixLink );

				} else {
					xNode = node.children.get(X);
					if (logger.isLoggable(Level.FINEST)) logger.finest("X Node is already " + xNode + " for prefixNode " + node);
				}

				// 5: Mark p_alphaX active
				xNode.active = Node.ACTIVE;
				
				int[] patternWords = pattern.getWordIDs();
				
				// 6: Q_alphaX <-- Q_alpha
				{
					SymbolTable vocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
					Pattern xpattern = new Pattern(vocab, patternWords, X);
					
//					HierarchicalPhrases phrasesWithFinalX = new HierarchicalPhrases(xpattern, node.sourceHierarchicalPhrases); 
					MatchedHierarchicalPhrases phrasesWithFinalX = 
//						node.sourceHierarchicalPhrases.copyWith(xpattern);
						node.sourceHierarchicalPhrases.copyWithFinalX();
//						new HierarchicalPhrases(xpattern, node.sourceHierarchicalPhrases); 
					
					xNode.storeResults(phrasesWithFinalX, xpattern);
//					xNode.storeResults(phrasesWithFinalX, xpattern.words);
				}
			
				if (logger.isLoggable(Level.FINEST)) logger.finest("Alpha pattern is " + pattern);

				// For efficiency, don't add any tuples to the queue whose patterns would exceed the max allowed number of tokens
				if (patternWords.length+2 <= maxPhraseLength) {
					
					int I = sentence.length;
					if (!SENTENCE_FINAL_X) I -= 1;
					
					int min = (I<i+maxPhraseSpan) ? I : i+maxPhraseSpan-1;
					Pattern patternX = new Pattern(pattern, X);

					// 7: for k from j+1 to min(I, i+MaxPhraseLength) do
					for (int k=j+2; k<=min; k++) {

						// 8: Add <alpha f_j X, i, k, p_alphaX> to queue
						if (logger.isLoggable(Level.FINEST)) logger.finest("extendQueue: Adding tuple ("+patternX+","+i+","+k+","+xNode+ " ) in EXTEND_QUEUE ****************************************" );
						queue.add(new Tuple(patternX, i, k, xNode));

					}
				} else if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Not extending " + pattern + "+X ");
				}
			}
		}
		

	}


	/**
	 * Gets the root node of this tree.
	 * 
	 * @return the root node of this tree
	 */
	public Grammar getRoot() {
		return root;
	}
	
	public List<Rule> getAllRules() {
		
		return root.getAllRules();
		
	}



	public String toString() {
		return root.toTreeString("", vocab);
	}

	public int size() {
		return root.size();
	}

	
	/**
	 * Default constructor - for testing purposes only.
	 * <p>
	 * The unit tests for Node require a dummy PrefixTree.
	 */
	private PrefixTree() {
		this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	/**
	 * Gets an invalid, dummy prefix tree.
	 * <p>
	 * For testing purposes only.
	 * 
	 */
	private PrefixTree(int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan) {
		root = null;
		suffixArray = null;
		targetCorpus = null;
		alignments = null;
		lexProbs = null;
		xnode = null;
		ruleExtractor = null;
		this.epsilon = null;
		this.vocab = null;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
	}

	/**
	 * Gets an invalid, dummy prefix tree.
	 * <p>
	 * For testing purposes only.
	 * 
	 * @return an invalid, dummy prefix tree
	 */
	static PrefixTree getDummyPrefixTree() {
		return new PrefixTree();
	}
	
	/**
	 * Gets an invalid, dummy prefix tree.
	 * <p>
	 * For testing purposes only.
	 * 
	 * @return an invalid, dummy prefix tree
	 */
	static PrefixTree getDummyPrefixTree(int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan) {
		return new PrefixTree(maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
	}
	

}
