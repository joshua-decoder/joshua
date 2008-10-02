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
package edu.jhu.sa.util.suffix_array;

import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.TrieGrammar;
import joshua.util.sentence.LabelledSpan;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Span;
import joshua.util.sentence.Vocabulary;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.jhu.sa.util.suffix_array.Pattern.PrefixCase;
import edu.jhu.sa.util.suffix_array.Pattern.SuffixCase;



public class PrefixTree {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(PrefixTree.class.getName());

	public static int MAX_NT_SPAN = 5; //XXX Should this be stored elsewhere?
	public static int MAX_PHRASE_LENGTH = 9; //XXX Should this be stored elsewhere?
	public static int SAMPLE_SIZE = 100; //XXX Should this be stored elsewhere?
	
	private final int spanLimit; //XXX Should this be stored elsewhere?
	
	/** Integer representation of the nonterminal X. All nonterminals are guaranteed to be represented by negative integers. */
	static final int X = -1;

	final Node root;
	final Phrase sentence;

	private final int maxPhraseSpan;
	private final int maxPhraseLength;
	private final int maxNonterminals;


	static final int ROOT_NODE_ID = -999;
	static final int BOT_NODE_ID = -2000;

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

	final SuffixArray suffixArray;
	final CorpusArray targetCorpus;
	final AlignmentArray alignments;
	
	/**
	 * Default constructor - for testing purposes only.
	 * <p>
	 * The unit tests for Node require a dummy PrefixTree.
	 */
	private PrefixTree() {
		root = null;
		sentence = null;
		suffixArray = null;
		targetCorpus = null;
		alignments = null;
		maxPhraseSpan = Integer.MIN_VALUE;
		maxPhraseLength = Integer.MIN_VALUE;
		maxNonterminals = Integer.MIN_VALUE;
		spanLimit = Integer.MAX_VALUE;
	}
	
	static PrefixTree getDummyPrefixTree() {
		return new PrefixTree();
	}
	
	/**
	 * Constructs a new prefix tree with suffix links
	 * using the GENERATE_PREFIX_TREE algorithm 
	 * from Lopez (2008) PhD Thesis, Algorithm 2, p 76. 
	 * 
	 * @param suffixArray
	 * @param sentence
	 * @param maxPhraseSpan
	 * @param maxPhraseLength
	 * @param maxNonterminals
	 */
	public PrefixTree(SuffixArray suffixArray, CorpusArray targetCorpus, AlignmentArray alignments, int[] sentence, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int spanLimit) {

		if (logger.isLoggable(Level.FINE)) logger.fine("\n\n\nConstructing new PrefixTree\n\n");

		this.suffixArray = suffixArray;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
		this.spanLimit = spanLimit;

		int START_OF_SENTENCE = 0;
		int END_OF_SENTENCE = sentence.length - 1;

		Node bot = new Node(BOT_NODE_ID);
		bot.hierarchicalPhrases = Collections.emptyList();
		
		this.root = new Node(ROOT_NODE_ID);
		bot.children = botMap(root);
		this.root.linkToSuffix(bot);

		Vocabulary vocab;

		if (suffixArray==null) {
			this.sentence = null;
			vocab = null;
		} else {
			this.sentence = new BasicPhrase(sentence, suffixArray.getVocabulary());
			vocab = suffixArray.getVocabulary();
			int[] bounds = {0, suffixArray.size()-1};
			root.setBounds(bounds);
		}
		root.hierarchicalPhrases = Collections.emptyList();

		Pattern epsilon = new Pattern(vocab);




		{	// 1: children(p_eps) <-- children(p_eps) U p_x

			// Add a link from root node to X
			Node xnode = root.addChild(X);
			xnode.hierarchicalPhrases = Collections.emptyList();

			// Add a suffix link from X back to root
			Node suffixLink = calculateSuffixLink(root, X);

			if (logger.isLoggable(Level.FINEST)) {
				String oldSuffixLink = (xnode.suffixLink==null) ? "null" : "id"+xnode.suffixLink.objectID;
				String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
				logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xnode + " with token " + X);
			}

			xnode.linkToSuffix(suffixLink);
		}

		if (logger.isLoggable(Level.FINE)) logger.fine("CURRENT TREE:  " + root);

		//int I = END_OF_SENTENCE; //sentence.length-1;

		Queue<Tuple> queue = new LinkedList<Tuple>();

		if (logger.isLoggable(Level.FINE)) logger.fine("Last sentence index == I == " + END_OF_SENTENCE);

		// 2: for i from 1 to I
		for (int i=START_OF_SENTENCE; i<=END_OF_SENTENCE; i++) {
			if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + i + ","+ i +","+root+",{"+sentence[i]+"})");

			// 3: Add <f_i, i, i+1, p_eps> to queue
			queue.add(new Tuple(i, i, root, epsilon));
		}


		// 4: for i from 1 to I
		for (int i=START_OF_SENTENCE+1; i<=END_OF_SENTENCE; i++) {
			if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + (i-1) + ","+(i)+","+root+",{"+X+","+sentence[i]+"})");

			// 5: Add <X f_i, i-1, i+1, p_x> to queue
			queue.add(new Tuple(i-1, i, root.getChild(X), new Pattern(vocab,X)));
		}


		// 6: While queue is not empty do
		while (! queue.isEmpty()) {

			if (logger.isLoggable(Level.FINER)) logger.finer("\n");
			if (logger.isLoggable(Level.FINE)) logger.fine("CURRENT TREE:      " + root);

			// 7: Pop <alpha, i, j, p_alphaBeta> from queue
			Tuple tuple = queue.remove();

			int i = tuple.spanStart;
			int j = tuple.spanEnd;
			Node prefixNode = tuple.prefixNode;
			Pattern pattern = tuple.pattern;

			if (logger.isLoggable(Level.FINER)) logger.finer("Have tuple (" + i + ","+j+","+prefixNode+","+pattern+")");

			if (j <= END_OF_SENTENCE) {

				// 8: If p_alphaBetaF_i elementOf children(p_alphaBeta) then
				if (prefixNode.hasChild(sentence[j])) {

					if (logger.isLoggable(Level.FINER)) logger.finer("EXISTING node for \"" + sentence[j] + "\" from " + prefixNode + " to node " + prefixNode.getChild(sentence[j])+ " with pattern " + pattern);

					// 9: If p_alphaBetaF_i is inactive then
					if (prefixNode.getChild(sentence[j]).active == Node.INACTIVE) {
						// 10: Continue to next item in queue
						continue;
					} else { // 11: Else
						// 12: EXTEND_QUEUE(alpha beta f_j, i, j, f_1^I
						if (logger.isLoggable(Level.FINER)) logger.finer("TREE BEFOR EXTEND: " + root);
						if (logger.isLoggable(Level.FINER)) logger.finer("Calling EXTEND_QUEUE("+i+","+j+","+pattern+","+prefixNode);
						extendQueue(queue, i, j, sentence, new Pattern(pattern,sentence[j]), prefixNode.getChild(sentence[j]));
						if (logger.isLoggable(Level.FINER)) logger.finer("TREE AFTER EXTEND: " + root);
					}

				} else { // 13: Else

					// Add new child node
					// 14: children(alphaBeta) <-- children(alphaBeta) U p_alphaBetaF_j
					if (logger.isLoggable(Level.FINER)) logger.finer("Adding new node to " + prefixNode);
					Node newNode = prefixNode.addChild(sentence[j]);
					if (logger.isLoggable(Level.FINER)) logger.finer("Created new node " + newNode +" for \"" + sentence[j] + "\" and \n  added new node " + newNode + " to " + prefixNode);



					Node suffixNode = calculateSuffixLink(prefixNode, sentence[j]);

					if (logger.isLoggable(Level.FINEST)) {
						String oldSuffixLink = (newNode.suffixLink==null) ? "null" : "id"+newNode.suffixLink.objectID;
						String newSuffixLink = (suffixNode==null) ? "null" : "id"+suffixNode.objectID;
						logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + newNode + " (prefix node " + prefixNode + " ) with token " + sentence[j]);
					}

					newNode.linkToSuffix( suffixNode );

					//Node suffixNode = prefixNode.suffixLink;
					//Node suffixChild = suffixNode.getChild(sentence[j]);

					//if (suffixChild.active == Node.INACTIVE) {
					if (suffixNode.active == Node.INACTIVE) {
						newNode.active = Node.INACTIVE;
					} else {

						Pattern extendedPattern = new Pattern(pattern,sentence[j]);
						//Pattern p = new Pattern(extendedPattern,suffixArray.getVocabulary());

						// Q_alpha-beta-f_j <-- query(alpha-beta-f_j, Q_alpha-beta, Q_beta-f_j

						List<HierarchicalPhrase> result = null;
						
						if (suffixArray != null) {
							result = query(extendedPattern, newNode, prefixNode, suffixNode);
						}


						if (result != null && result.isEmpty()) {
							newNode.active = Node.INACTIVE;
							
						} else {
							
							newNode.active = Node.ACTIVE;
							extendQueue(queue, i, j, sentence, extendedPattern, newNode);
							
						}
					}
				}
			}

		}

		if (logger.isLoggable(Level.FINER)) logger.finer("\n");
		if (logger.isLoggable(Level.FINE)) logger.fine("FINAL TREE:  " + root);


	}

	/**
	 * Constructs a new prefix tree with suffix links
	 * using the GENERATE_PREFIX_TREE algorithm 
	 * from Lopez (2008) PhD Thesis, Algorithm 2, p 76. 
	 * <p>
	 * This constructor does not take a suffix array parameter.
	 * Instead any prefix tree constructed by this constructor
	 * will assume that all possible phrases of this sentence
	 * are valid phrases.
	 * <p>
	 * This constructor is meant to be used primarily for testing purposes.
	 * 
	 * @param sentence
	 * @param maxPhraseSpan
	 * @param maxPhraseLength
	 * @param maxNonterminals
	 */
	PrefixTree(int[] sentence, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int spanLimit) {
		this(null, null, null, sentence, maxPhraseSpan, maxPhraseLength, maxNonterminals, spanLimit);
	}

	public Grammar getRoot() {
		return root;
	}

	final Comparator<HierarchicalPhrase> nonOverlapping = new Comparator<HierarchicalPhrase>() {
		public int compare(HierarchicalPhrase m1, HierarchicalPhrase m2) {
			if (m1.sentenceNumber < m2.sentenceNumber)
				return -1;
			else if (m1.sentenceNumber > m2.sentenceNumber)
				return 1;
			else {
				if (m1.terminalSequenceStartIndices[0] >= m2.terminalSequenceStartIndices[0]-1)
					return 1;
				else if (m1.terminalSequenceStartIndices[0] <= m2.terminalSequenceStartIndices[0]-maxPhraseSpan)
					return -1;
				else
					return 0;
			}
		}
	};

	final Comparator<HierarchicalPhrase> overlapping = new Comparator<HierarchicalPhrase>() {
		
		//TODO This method may or may not be correct!!!!!
		public int compare(HierarchicalPhrase m1, HierarchicalPhrase m2) {
			
			//int m1Start;
			int m2Start, m1End;
			//int m2End;
			
			SuffixCase m1Suffix = m1.pattern.suffixCase;
			PrefixCase m2Prefix = m2.pattern.prefixCase;
			
			if (m1Suffix == SuffixCase.EMPTY_SUFFIX) {
				if (m2Prefix == PrefixCase.EMPTY_PREFIX) {
					return 0;
				} else {
					throw new RuntimeException("Overlapping phrases should not have empty suffix but nonempty prefix");
				}	
			} else if (m2Prefix == PrefixCase.EMPTY_PREFIX) {
				throw new RuntimeException("Overlapping phrases should not have empty prefix but nonempty suffix");
			}
			
			
			m1End   = m1.terminalSequenceStartIndices.length - 1;
			m2Start = 0;
			/*
			switch(m1Suffix) {
				
				case STARTS_WITH_NONTERMINAL:
				case STARTS_WITH_TWO_TERMINALS:
					m1Start = 0; 
					break;
					
				case STARTS_WITH_TERMINAL_NONTERMINAL:
					m1Start = 1; 
					break;
				
				default:
					if (m2Prefix==PrefixCase.EMPTY_PREFIX)
						return 0;
					else
						throw new RuntimeException("Overlapping phrases by definition should not have an empty suffix");
			}
			
			
			switch(m2Prefix) {
				
				case ENDS_WITH_NONTERMINAL:
				case ENDS_WITH_TWO_TERMINALS: 
					m2End   = m2.corpusIndicesOfTerminalSequences.length - 1; 
					break;
					
				case ENDS_WITH_NONTERMINAL_TERMINAL: 
					m2End   = m2.corpusIndicesOfTerminalSequences.length - 2; 
					break;
					
				case EMPTY_PREFIX:
				default:
					throw new RuntimeException("Overlapping phrases by definition should not have an empty prefix");
			}
			*/
			//int m2Length = m2.corpusIndicesOfTerminalSequences.length;
			
			int result = m1.terminalSequenceStartIndices[m1End] - m2.terminalSequenceStartIndices[m2Start];
			
			if (result == 0) {
				
				int endPosition = m2.terminalSequenceStartIndices[0] + m2.terminalSequenceStartIndices.length;
				int combinedLength = endPosition - m1.terminalSequenceStartIndices[0];
				
				if (combinedLength <= maxPhraseSpan) 
					return 0;
				else
					return 1;
				
			}
			
			return result;
			
			
		}
	};

	private int compare(HierarchicalPhrase m_a_alpha, HierarchicalPhrase m_alpha_b) {
		
		//int suffixStart = m_alpha_b.getCorpusStartPosition();
		boolean matchesOverlap;
		if (m_a_alpha.pattern.endsWithNonTerminal() && m_alpha_b.pattern.startsWithNonTerminal()) 
			matchesOverlap = false;
		else
			matchesOverlap = true;
		
		if (matchesOverlap)
			return overlapping.compare(m_a_alpha, m_alpha_b);
		else
			return nonOverlapping.compare(m_a_alpha, m_alpha_b);
	}

	public List<HierarchicalPhrase> query(Pattern pattern, Node node, Node prefixNode, Node suffixNode) {

		List<HierarchicalPhrase> result;

		int prefixLowerBound = prefixNode.lowBoundIndex;
		int prefixUpperBound = prefixNode.highBoundIndex;
		
		// If the pattern is contiguous, look up the pattern in the suffix array
		if (pattern.arity() == 0) {

			// Get the first and last index in the suffix array for the specified pattern
			int[] bounds = suffixArray.findPhrase(pattern, 0, pattern.size(), prefixLowerBound, prefixUpperBound);
			if (bounds==null) {
				result = Collections.emptyList();
			} else {
				node.setBounds(bounds);
				int[] startingPositions = suffixArray.getAllPositions(bounds);
				result = suffixArray.invertedIndex.getHierarchicalPhrases(startingPositions, pattern);
			}
			
		} else {

			result = suffixArray.invertedIndex.getMatchingPhrases(pattern);
			
			//if (result != null)
			//	return result;
			//else {
			if (result == null) {
				result = queryIntersect(pattern, prefixNode.hierarchicalPhrases, suffixNode.hierarchicalPhrases);
				suffixArray.invertedIndex.setMatchingPhrases(pattern, result);
			}
		}

		node.setHierarchicalPhrases(result, pattern.words);
		return result;

	}

	/**
	 * Implements the QUERY_INTERSECT algorithm from Adam Lopez's thesis (Lopez 2008).
	 * This implementation follows a corrected algorithm (Lopez, personal communication).
	 * 
	 * @param M_a_alpha
	 * @param M_alpha_b
	 * @param overlapping
	 * @return
	 */
	List<HierarchicalPhrase> queryIntersect(Pattern pattern, List<HierarchicalPhrase> M_a_alpha, List<HierarchicalPhrase> M_alpha_b) {

		// results is M_{a_alpha_b} in the paper
		List<HierarchicalPhrase> results = new ArrayList<HierarchicalPhrase>();

		int I = M_a_alpha.size();
		int J = M_alpha_b.size();

		int i = 0;
		int j = 0;

		while (i<I && j<J) {

			HierarchicalPhrase m_a_alpha, m_alpha_b;
			m_a_alpha = M_a_alpha.get(i);
			m_alpha_b = M_alpha_b.get(j);
			
			while (j<J && compare(m_a_alpha, m_alpha_b) > 0) {
				j++; // advance j past no longer needed item in M_alpha_b
				m_alpha_b = M_alpha_b.get(j);
			}

			int k = i;
			int l = j;
			
			// Process all matchings in M_alpha_b with same first element
			while (M_alpha_b.get(i).terminalSequenceStartIndices[0] == M_alpha_b.get(k).terminalSequenceStartIndices[0]) {
				
				int compare_i_l = compare(M_a_alpha.get(i), M_alpha_b.get(l));
				while (compare_i_l >= 0) {
					
					if (compare_i_l == 0) {
						
						// append M_a_alpha[i] |><| M_alpha_b[l] to M_a_alpha_b
						results.add(new HierarchicalPhrase(pattern, M_a_alpha.get(i)));
						//results.add(new HierarchicalPhrase(pattern, M_a_alpha.get(i), M_alpha_b.get(l)));
						
					} // end if
					
					l++; // we can visit m_alpha_b[l] again, but only next time through outermost loop
					
					compare_i_l = compare(M_a_alpha.get(i), M_alpha_b.get(l));
					
				} // end while
				
				i++; // advance i past no longer needed item in M_a_alpha
				
			} // end while
			
		} // end while
		
		return results;
		
	}


	/**
	 * Implements Function EXTEND_QUEUE from Lopez (2008) PhD Thesis, Algorithm 2, p 76
	 * 
	 * @param queue
	 * @param i
	 * @param j
	 * @param sentence
	 * @param alphaPattern
	 * @param prefixNode
	 */
	private void extendQueue(Queue<Tuple> queue, int i, int j, int[] sentence, Pattern alphaPattern, Node prefixNode) {

		if (alphaPattern.size() < maxPhraseLength  &&  (j+1)-i+1 <= maxPhraseSpan  && (j+1)<sentence.length) {

			// Add new tuple to the queue
			if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + i + ","+ (j+1) +","+prefixNode+",{"+(new Pattern(alphaPattern,sentence[j+1]))+"})");
			queue.add(new Tuple(i, j+1, prefixNode, alphaPattern));//, sentence[j+1]));


			if (alphaPattern.arity() < maxNonterminals) {
				Node xNode;

				if (! prefixNode.children.containsKey(X)) {

					// Add new child node in tree and mark in as active
					xNode = prefixNode.addChild(X);
					if (logger.isLoggable(Level.FINEST)) logger.finest("Adding node for \"" + X + "\" from " + prefixNode + " to new node " + xNode + " with alphaPattern " + alphaPattern + "  (in extendQueue)");

					Node suffixLink = calculateSuffixLink(prefixNode, X);

					if (logger.isLoggable(Level.FINEST)) {
						String oldSuffixLink = (xNode.suffixLink==null) ? "null" : "id"+xNode.suffixLink.objectID;
						String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
						logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xNode + " (prefix node " + prefixNode + " ) with token " + X);
					}

					xNode.linkToSuffix( suffixLink );

				} else {
					xNode = prefixNode.children.get(X);
					xNode.active = Node.ACTIVE;
					if (logger.isLoggable(Level.FINEST)) logger.finest("X Node is already " + xNode + " for prefixNode " + prefixNode);
				}

				// Q_alphaX <-- Q_alpha
				
				xNode.setHierarchicalPhrases(prefixNode.hierarchicalPhrases, pattern(alphaPattern.words, X));
				
				if (logger.isLoggable(Level.FINEST)) logger.finest("Alpha pattern is " + alphaPattern);

				int I = sentence.length-1;
				int min = (I<i+maxPhraseLength) ? I : i+maxPhraseLength-1;
				for (int k=j+2; k<=min; k++) {
					if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple ("+i+","+k+","+xNode+","+alphaPattern+"+X+"+sentence[k] + " ) in EXTEND_QUEUE ****************************************" );
					queue.add(new Tuple(i, k, xNode, alphaPattern, X));
				}
			}
		}


	}

	static Node calculateSuffixLink(Node parent, int endOfPattern) {

		Node suffixLink = parent.suffixLink.getChild(endOfPattern);

		if (suffixLink==null)
			throw new RuntimeException("No child " + endOfPattern + " for node " + parent.suffixLink + " (Parent was " + parent + ")");

		return suffixLink;

	}


	/** Maps from integer word ids to strings. Used only for debugging. */
	static Map<Integer,String> idsToStrings;


	public String toString() {
		if (suffixArray==null)
			return root.toTreeString("", null);
		else
			return root.toTreeString("", suffixArray.getVocabulary());
	}
	
	public String toString(Vocabulary vocab) {
		return root.toTreeString("", vocab);
	}

	public int size() {
		return root.size();
	}

	public void print(OutputStream out) throws UnsupportedEncodingException, IOException {
		
		Vocabulary sourceVocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
		Vocabulary targetVocab = (targetCorpus==null) ? null : targetCorpus.vocab;
		
		for (Node node : root.children.values()) {
			//String sourcePhrase = (vocab==null) ? ""+node.incomingArcValue : ""+node.incomingArcValue;
			node.print("", out, sourceVocab, targetVocab);
			//out.write(node.toRuleString(vocab).getBytes("UTF-8"));
		}
	}
	
	/**
	 * Builds a hierarchical phrase in the target language substituting the terminal sequences
	 *  in the target side with nonterminal symbols corresponding to the source nonterminals.
	 * <p>
	 * This assumes that the source and target spans are consistent.
	 * 
	 * @param sourceSpan Span in the corpus of the source phrase; this is needed because the accurate span will not be in the sourcePhrase if it starts or ends with a nonterminal
	 * 
	 * @return null if no translation can be constructed
	 */
	Pattern constructTranslation(HierarchicalPhrase sourcePhrase, Span sourceSpan, Span targetSpan, boolean sourceStartsWithNT, boolean sourceEndsWithNT) {
		
		// Construct a pattern for the trivial case where there are no nonterminals
		if (sourcePhrase.pattern.arity == 0) {
			//return new HierarchicalPhrase(targetSpan, targetCorpus);
			
			int[] words = new int[targetSpan.size()];
			
			for (int i=targetSpan.start; i<targetSpan.end; i++) {
				words[i-targetSpan.start] = targetCorpus.corpus[i];
			}
			
			return new Pattern(targetCorpus.vocab, words);
			
		}

		
		// Handle the more complex cases...
		List<LabelledSpan> targetNTSpans = new ArrayList<LabelledSpan>();
		int patternSize = targetSpan.size();
		
		int nonterminalID = -1;
		
		// For each non terminal in the source, find their corresponding positions in the target span... 
		
		// If the source phrase starts with a nonterminal, we have to handle that NT as a special case
		if (sourceStartsWithNT) {
			
			// If the source phrase starts with NT, then we need to calculate the span of the first NT
			Span nonterminalSourceSpan = new Span(sourceSpan.start, sourcePhrase.terminalSequenceStartIndices[0]);
			
			Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);
			
			if (nonterminalTargetSpan==null) return null;
			
			targetNTSpans.add(new LabelledSpan(nonterminalID,nonterminalTargetSpan));
			nonterminalID--;
			// the pattern length will be reduced by the length of the non-terminal, and increased by 1 for the NT itself.
			patternSize = patternSize - nonterminalTargetSpan.size() +1;
		}
		
		// Process all internal nonterminals
		for (int i=0; i<sourcePhrase.terminalSequenceStartIndices.length-1; i++) {
			
			Span nonterminalSourceSpan = new Span(sourcePhrase.terminalSequenceEndIndices[i], sourcePhrase.terminalSequenceStartIndices[i+1]);
			
			Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);
			
			if (nonterminalTargetSpan==null) return null;
			
			targetNTSpans.add(new LabelledSpan(nonterminalID,nonterminalTargetSpan));
			nonterminalID--;
			patternSize = patternSize - nonterminalTargetSpan.size() + 1;
		}
			
		// If the source phrase starts with a nonterminal, we have to handle that NT as a special case
		if (sourceEndsWithNT) {
			
			// If the source phrase starts with NT, then we need to calculate the span of the first NT
			Span nonterminalSourceSpan = new Span(sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1],sourceSpan.end);
			
			Span nonterminalTargetSpan = alignments.getConsistentTargetSpan(nonterminalSourceSpan);
			
			if (nonterminalTargetSpan==null) return null;
			
			targetNTSpans.add(new LabelledSpan(nonterminalID,nonterminalTargetSpan));
			nonterminalID--;
			patternSize = patternSize - nonterminalTargetSpan.size() + 1;
		}
		
		// Create the pattern...
		int[] words = new int[patternSize];
		int patterCounter = 0;
		
		Collections.sort(targetNTSpans);
		
		// if we don't start with a non-terminal, then write out all the words
		// until we get to the first non-terminal
		if(targetNTSpans.get(0).getSpan().start != targetSpan.start) {
			// the target pattern starts with a non-terminal
			for(int i = targetSpan.start; i < targetNTSpans.get(0).getSpan().start; i++) {
				words[patterCounter] = targetCorpus.getWordID(i);
				patterCounter++;
			}
		}

		// add the first non-terminal
		words[patterCounter] = targetNTSpans.get(0).getLabel();
		patterCounter++;
		
		// add everything until the final non-terminal
		for(int i = 1; i < targetNTSpans.size(); i++) {
			LabelledSpan NT1 = targetNTSpans.get(i-1);
			LabelledSpan NT2 = targetNTSpans.get(i);
			
			for(int j = NT1.getSpan().end; j < NT2.getSpan().start; j++) {
				words[patterCounter] = targetCorpus.getWordID(j);
				patterCounter++;
			}
			words[patterCounter] = NT2.getLabel();
			patterCounter++;
		}
		
		// if we don't end with a non-terminal, then write out all remaining words
		if(targetNTSpans.get(targetNTSpans.size()-1).getSpan().end != targetSpan.end) {
			// the target pattern starts with a non-terminal
			for(int i = targetNTSpans.get(targetNTSpans.size()-1).getSpan().end; i < targetSpan.end; i++) {
				words[patterCounter] = targetCorpus.getWordID(i);
				patterCounter++;
			}
		}
		
		return new Pattern(targetCorpus.vocab, words);
	}
	
	static int nodeIDCounter = 0;
	
	static void resetNodeCounter() {
		nodeIDCounter = 0;
	}
	
	/**
	 * Represents a node in a prefix tree with suffix links.
	 * 
	 * @author Lane Schwartz
	 * @see Lopez (2008) PhD Thesis, Sec 4.3.1,2, p 71-74.
	 */
	class Node implements Comparable<Node>, Grammar, TrieGrammar {

		static final boolean   ACTIVE = true;
		static final boolean INACTIVE = false;

		final int incomingArcValue;
		final int objectID;

		private int lowBoundIndex;
		private int highBoundIndex;

		boolean active;
		Node suffixLink;

		List<Matching> matchings;

		Map<Integer,Node> children;
		
		List<HierarchicalPhrase> hierarchicalPhrases;
		List<Rule> results;
		
		int[] sourceWords;
		
		Node(int incomingArcValue) {
			this(true,incomingArcValue);
		}

		Node(boolean active, int incomingArcValue) {
			this.active = active;
			this.suffixLink = null;
			this.children = new HashMap<Integer,Node>();
			this.incomingArcValue = incomingArcValue;
			this.objectID = nodeIDCounter++;
			this.hierarchicalPhrases = Collections.emptyList();
			this.results = Collections.emptyList();
		}
		
		
		public RuleCollection getRules() {
			// return RuleCollection.newFromList( this.results );
			throw new RuntimeException(PrefixTree.class.getName() + ".getRules(): not implemented");
		}
		
		
		public boolean hasExtensions() {
			if (children.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}
		
		
		public boolean hasRules() {
			if (hierarchicalPhrases.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}
		
		
		public TrieGrammar matchOne(int symbol) {
			if (children.containsKey(symbol)) {
				return children.get(symbol);
			} else {
				//TODO Implement this
				throw new RuntimeException("Not yet implemented");
			}
		}

		public TrieGrammar matchPrefix(List<Integer> symbols) {
			
			Node node = this;
			
			for (Integer symbol : symbols) {
				if (node.children.containsKey(symbol)) {
					node = node.children.get(symbol);
				} else {
					//TODO Implement this
					throw new RuntimeException("Not yet implemented");
				}
			}
			
			return node;
		}
		
		
		public void setHierarchicalPhrases(List<HierarchicalPhrase> hierarchicalPhrases, int[] sourceWords) {
			this.hierarchicalPhrases = hierarchicalPhrases;
			this.sourceWords = sourceWords;
			
			//TODO Implement this so that we store rules instead of just hierarchical phrases
			
//			this.results = new ArrayList<Rule>(hierarchicalPhrases.size());
//			
//			int dummyRuleID = 1;
//			int dummyOwner = 1;
//			
//			for (HierarchicalPhrase targetPhrase : hierarchicalPhrases) {
//				
//				
//				float[] featureScores = null;
//				if (true) throw new RuntimeException("Assigning features to rules is not yet implemented");
//
//				results.add(new Rule(dummyRuleID, X, sourceWords, targetPhrase.pattern.words, dummyOwner, featureScores, targetPhrase.pattern.arity));
//			}
			
		}
		
		public boolean hasChild(int child) {
			return children.containsKey(child);
		}

		public Node getChild(int child) {
			return children.get(child);
		}

		public Node addChild(int child) {
			if (children.containsKey(child)) throw new RuntimeException("Child " + child + " already exists in node " + this);
			Node node = new Node(child);
			children.put(child, node);
			return node;
		}

		public void linkToSuffix(Node suffix) {
			this.suffixLink = suffix;
		}

		public void setBounds(int[] bounds) {
			lowBoundIndex = bounds[0];
			highBoundIndex = bounds[1];
		}

		/**
		 * Given a node in a prefix tree, find a list of translations for the source phrase represented by that node.
		 * 
		 * @return a list of translations for the source phrase represented by this node.
		 */
		public List<Pattern> translate() {
			List<Pattern> translations = new ArrayList<Pattern>();
			
			int totalPossibleTranslations = hierarchicalPhrases.size();
			int step = totalPossibleTranslations / SAMPLE_SIZE;
			
			// Sample from cached hierarchicalPhrases
			List<HierarchicalPhrase> samples = new ArrayList<HierarchicalPhrase>(SAMPLE_SIZE);
			for (int i=0; i<totalPossibleTranslations; i+=step) {
				samples.add(hierarchicalPhrases.get(i));
			}
			
			
			// For each sample HierarchicalPhrase
			for (HierarchicalPhrase sourcePhrase : samples) {
				
				// Case 1:  If sample !startsWithNT && !endsWithNT
				if (!sourcePhrase.startsWithNonterminal() && !sourcePhrase.endsWithNonterminal()) {
					
					// Get target span
					Span sourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0], sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]);//+sample.length); 
					
					Span targetSpan = alignments.getConsistentTargetSpan(sourceSpan);
					
					// If target span and source span are consistent
					if (targetSpan!=null) {
						
						// Construct a translation
						Pattern translation = constructTranslation(sourcePhrase, sourceSpan, targetSpan, false, false);
						
						if (translation != null) translations.add(translation);
						
					}
					
				}
				
				// Case 2: If sourcePhrase startsWithNT && !endsWithNT
				else if (sourcePhrase.startsWithNonterminal() && !sourcePhrase.endsWithNonterminal()) {
					
					int startOfSentence = suffixArray.corpus.getSentencePosition(sourcePhrase.sentenceNumber);
					int startOfTerminalSequence = sourcePhrase.terminalSequenceStartIndices[0];
					int endOfTerminalSequence = sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1];
					
					// Start by assuming the initial source nonterminal starts one word before the first source terminal 
					Span possibleSourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0]-1, sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]);//+sample.length); 
					
					// Loop over all legal source spans 
					//      (this is variable because we don't know the length of the NT span)
					//      looking for a source span with a consistent translation
					while (possibleSourceSpan.start >= startOfSentence && 
							startOfTerminalSequence-possibleSourceSpan.start<=MAX_NT_SPAN && 
							endOfTerminalSequence-possibleSourceSpan.start<=MAX_PHRASE_LENGTH) {
						
						// Get target span
						Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

						// If target span and source span are consistent
						if (targetSpan!=null) {

							// Construct a translation
							Pattern translation = constructTranslation(sourcePhrase, possibleSourceSpan, targetSpan, true, false);

							if (translation != null) {
								translations.add(translation);
								break;
							}

						} 
						
						possibleSourceSpan.start--;
						
					}
					
				}
				
				// Case 3: If sourcePhrase !startsWithNT && endsWithNT
				else if (!sourcePhrase.startsWithNonterminal() && sourcePhrase.endsWithNonterminal()) {
					
					int endOfSentence = suffixArray.corpus.getSentencePosition(sourcePhrase.sentenceNumber+1);
					int startOfTerminalSequence = sourcePhrase.terminalSequenceStartIndices[0];
					int endOfTerminalSequence = sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1];
					
					// Start by assuming the initial source nonterminal starts one word after the last source terminal 
					Span possibleSourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0], sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]+1); 
					
					// Loop over all legal source spans 
					//      (this is variable because we don't know the length of the NT span)
					//      looking for a source span with a consistent translation
					while (possibleSourceSpan.end < endOfSentence && 
							startOfTerminalSequence-possibleSourceSpan.start<=MAX_NT_SPAN && 
							endOfTerminalSequence-possibleSourceSpan.start<=MAX_PHRASE_LENGTH) {
						
						// Get target span
						Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

						// If target span and source span are consistent
						if (targetSpan!=null) {

							// Construct a translation
							Pattern translation = constructTranslation(sourcePhrase, possibleSourceSpan, targetSpan, false, true);

							if (translation != null) {
								translations.add(translation);
								break;
							}

						} 
						
						possibleSourceSpan.end++;
						
					}
					
				}
				
				// Case 4: If sourcePhrase startsWithNT && endsWithNT
				else if (sourcePhrase.startsWithNonterminal() && sourcePhrase.endsWithNonterminal()) {
					
					int startOfSentence = suffixArray.corpus.getSentencePosition(sourcePhrase.sentenceNumber);
					int endOfSentence = suffixArray.corpus.getSentencePosition(sourcePhrase.sentenceNumber+1);
					int startOfTerminalSequence = sourcePhrase.terminalSequenceStartIndices[0];
					int endOfTerminalSequence = sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1];
					
					// Start by assuming the initial source nonterminal 
					//   starts one word before the first source terminal and
					//   ends one word after the last source terminal 
					Span possibleSourceSpan = new Span(sourcePhrase.terminalSequenceStartIndices[0]-1, sourcePhrase.terminalSequenceEndIndices[sourcePhrase.terminalSequenceEndIndices.length-1]+1); 
					
					// Loop over all legal source spans 
					//      (this is variable because we don't know the length of the NT span)
					//      looking for a source span with a consistent translation
					while (possibleSourceSpan.start >= startOfSentence && 
							possibleSourceSpan.end < endOfSentence && 
							startOfTerminalSequence-possibleSourceSpan.start<=MAX_NT_SPAN && 
							endOfTerminalSequence-possibleSourceSpan.start<=MAX_PHRASE_LENGTH) {
						
						// Get target span
						Span targetSpan = alignments.getConsistentTargetSpan(possibleSourceSpan);

						// If target span and source span are consistent
						if (targetSpan!=null) {

							// Construct a translation
							Pattern translation = constructTranslation(sourcePhrase, possibleSourceSpan, targetSpan, true, true);

							if (translation != null) {
								translations.add(translation);
								break;
							}

						} 
						
						if (possibleSourceSpan.end+1 < endOfSentence) {
							possibleSourceSpan.end++;
						} else {
							possibleSourceSpan.end = 1;
							possibleSourceSpan.start--;
						}
												
					}
					
				}
				
			}
			
			
			return translations;
		}
		
		
		
		
		public int size() {

			int size = 1;

			for (Node child : children.values()) {
				size += child.size();
			}

			return size;
		}

		public String toString(Vocabulary vocab) {

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
			} else if (idsToStrings==null || !idsToStrings.containsKey(incomingArcValue)) {
				s.append('v');
				s.append(incomingArcValue);
			} else {
				s.append(idsToStrings.get(incomingArcValue));
			}
			s.append(" (");
			if (suffixLink!=null) s.append(suffixLink.objectID); else s.append("null");
			s.append(')');
			s.append(' ');

			for (Map.Entry<Integer, Node> entry : children.entrySet()) {
				s.append(entry.getValue().toString(vocab));
				s.append(' ');
			}

			s.append(']');

			return s.toString();

			//return ""+id;
		}
		
		public String toString() {
			return toString(null);
		}

		private void print(String partialSourcePhrase, OutputStream out, Vocabulary sourceVocab, Vocabulary targetVocab) throws UnsupportedEncodingException, IOException {
			
			String leftHandSide = "[X]";
			
			String sourceSide = partialSourcePhrase;
			if (incomingArcValue==PrefixTree.X) {
				sourceSide += "[X] ";
			} else if (sourceVocab==null) {
				sourceSide += incomingArcValue + " ";
			} else {
				sourceSide += sourceVocab.getWord(incomingArcValue) + " ";
			}
			
			String targetSide = "";
			for (HierarchicalPhrase phrase : hierarchicalPhrases) {
				
				int[] targetWords = phrase.pattern.words;
				for (int targetWord : targetWords) {
					if (targetWord < 0) {
						targetSide += "[X," + (-1 * targetWord) + "] ";
					} else if (targetVocab==null) {
						targetSide += targetWord + " ";
					} else {
						targetSide += targetVocab.getWord(targetWord) + " ";
					}
				}
				
			}
			
			out.write(leftHandSide.getBytes("UTF-8"));
			out.write("||| ".getBytes("UTF-8"));
			out.write(sourceSide.getBytes("UTF-8"));
			out.write("||| ".getBytes("UTF-8"));
			out.write(targetSide.getBytes("UTF-8"));
			out.write("||| ".getBytes("UTF-8"));

			
			for (Node node : children.values()) {
				node.print(sourceSide, out, sourceVocab, targetVocab);
			}
			
		}
		
		String toTreeString(String tabs, Vocabulary vocab) {

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
			} else if (idsToStrings==null || !idsToStrings.containsKey(incomingArcValue)) {
				s.append('v');
				s.append(incomingArcValue);
			} else {
				s.append(idsToStrings.get(incomingArcValue));
			}
			s.append(" (");
			if (suffixLink!=null) s.append(suffixLink.objectID); else s.append("null");
			s.append(')');

			if (children.size() > 0) {
				s.append(" \n\n");

				List<Node> kids = new ArrayList<Node>(children.values());
				Collections.sort(kids);

				for (Node entry : kids) {
					s.append(entry.toTreeString(tabs+"\t", vocab));
					s.append("\n\n");
				}

				s.append(tabs);
			} else {
				s.append(' ');
			}

			s.append(']');

			return s.toString();

			//return ""+id;
		}

		public int compareTo(Node o) {
			Integer i = objectID;
			Integer j = o.objectID;

			return i.compareTo(j);
		}

		public TrieGrammar getTrieRoot() {
			return this;
		}

		public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
			if (PrefixTree.this.spanLimit == -1) { // mono-glue grammar
				return (startIndex == 0);
			} else {
				return (endIndex - startIndex <= PrefixTree.this.spanLimit);
			}
		}

	}

	/**
	 * Represents a tuple used during prefix tree construction.
	 * 
	 * @author Lane Schwartz
	 * @see Lopez (2008) PhD Thesis, Algorithm 2, p 76
	 */
	private static class Tuple {

		final Pattern pattern;
		final int spanStart;
		final int spanEnd;
		final Node prefixNode;

		/**
		 * Constructs a new tuple.
		 * 
		 * @param spanStart
		 * @param spanEnd
		 * @param prefixNode
		 * @param pattern
		 */
		Tuple(int spanStart, int spanEnd, Node prefixNode, Pattern pattern) {
			this.pattern = pattern;
			this.spanStart = spanStart;
			this.spanEnd = spanEnd;
			this.prefixNode = prefixNode;
		}

		/**
		 * Constructs a new tuple.
		 * 
		 * @param spanStart
		 * @param spanEnd
		 * @param prefixNode
		 * @param oldPattern
		 * @param newPattern
		 */
		Tuple(int spanStart, int spanEnd, Node prefixNode, Pattern oldPattern, int... newPattern) {
			this.pattern = new Pattern(oldPattern, newPattern);/*new int[oldPattern.length + newPattern.length];

			for (int index=0; index<oldPattern.length; index++) {
				pattern[index] = oldPattern[index];
			}
			for (int index=oldPattern.length; index<oldPattern.length+newPattern.length; index++) {
				pattern[index] = newPattern[index - oldPattern.length];
			}
			 */
			this.spanStart = spanStart;
			this.spanEnd = spanEnd;
			this.prefixNode = prefixNode;
		}
	}

	protected static int[] pattern(int[] oldPattern, int... newPattern) {
		int[] pattern = new int[oldPattern.length + newPattern.length];

		for (int index=0; index<oldPattern.length; index++) {
			pattern[index] = oldPattern[index];
		}
		for (int index=oldPattern.length; index<oldPattern.length+newPattern.length; index++) {
			pattern[index] = newPattern[index - oldPattern.length];
		}

		return pattern;
	}


	/** 
	 * For testing purposes...
	 */
	public static void main(String[] args) {
		// Adam Lopez's example...
		String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";
		String queryString = "it persuades him and it disheartens him";
		
		Vocabulary vocab = new Vocabulary();
		BasicPhrase corpusSentence = new BasicPhrase(corpusString, vocab);
		BasicPhrase querySentence = new BasicPhrase(queryString, vocab);
		vocab.alphabetize();
		vocab.fixVocabulary();
		
		// create the suffix array...
		int[] sentences = new int[1];
		sentences[0] = 0;
		int[] corpus = new int[corpusSentence.size()];
		for(int i = 0; i < corpusSentence.size(); i++) {
			corpus[i] = corpusSentence.getWordID(i);
		}
		CorpusArray corpusArray = new CorpusArray(corpus, sentences, vocab);
		SuffixArray suffixArray = new SuffixArray(corpusArray);
		
		CorpusArray targetCorpus = null;
		AlignmentArray alignments = null;
		
		int maxPhraseSpan = 10;
		int maxPhraseLength = 10;
		int maxNonterminals = 2;
		int spanLimit = 8;
		
		PrefixTree prefixTree = new PrefixTree(suffixArray, targetCorpus, alignments, querySentence.getWordIDs(), maxPhraseSpan, maxPhraseLength, maxNonterminals, spanLimit);
		
		System.out.println(prefixTree.toString());
		System.out.println();
		System.out.println(prefixTree.size());
	}
}
