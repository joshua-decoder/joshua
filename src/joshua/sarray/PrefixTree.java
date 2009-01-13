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
package joshua.sarray;

import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.TrieGrammar;
import joshua.util.lexprob.LexicalProbabilities;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;
import joshua.util.sentence.alignment.Alignments;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



//TODO Ask Adam if he has an efficient way of calculating lexical translation probabilities

/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-11-13 13:13:31 -0600 (Thu, 13 Nov 2008) $
 */
public class PrefixTree {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(PrefixTree.class.getName());

	/** Integer representation of the nonterminal X. All nonterminals are guaranteed to be represented by negative integers. */
	public static final int X = -1;
	
	/** 
	 * Vocabulary of nonterminal symbols. 
	 * Maps from the integer representation of a nonterminal 
	 * to the string representation of the nonterminal.
	 */
	static final Map<Integer,String> ntVocab = new HashMap<Integer,String>();
	static { ntVocab.put(X, "X"); }
	
	/** Operating system-specific end of line character(s). */
	private static final byte[] newline = System.getProperty("line.separator").getBytes();
	
	/** Root node of this tree. */
	final Node root;
	
	/** Source language sentence which this tree represents. */
	final Phrase sentence;

	private final RuleExtractor ruleExtractor;
	
	
	/** Max span in the source corpus of any extracted hierarchical phrase */
	final int maxPhraseSpan;   
	
	/** Maximum number of terminals plus nonterminals allowed in any extracted hierarchical phrase. */
	final int maxPhraseLength;
	
	/** Maximum number of nonterminals allowed in any extracted hierarchical phrase. */
	final int maxNonterminals;
	
	/** Maximum span in the source corpus of any nonterminal in an extracted hierarchical phrase. */
//	private final int maxNonterminalSpan;

	/** Minimum span in the source corpus of any nonterminal in an extracted hierarchical phrase. */
	final int minNonterminalSpan;
	
	/** 
	 * Maximum number of instances of a source phrase 
	 * from the source corpus to use when translating a source phrase. 
	 * <p>
	 * Note: This is <em>not</em> the maximum number of hierarchical phrases
	 * to store at each node in the prefix tree.
	 */
//	private final int sampleSize;
	
	/** Represents a very high cost, corresponding to a very unlikely probability. */
	static final float VERY_UNLIKELY = -1.0f * (float) Math.log(1.0e-9);
	
	/** */
	static boolean SENTENCE_INITIAL_X = false;
	
	/** */
	static boolean SENTENCE_FINAL_X = false;
	
	/** Unique integer identifier for the root node. */
	static final int ROOT_NODE_ID = -999;
	
	/** 
	 * Unique integer identifier for the special ⊥ node that represents the suffix of the root node.
	 * @see Lopez (2008), footnote 9 on p73
	 */
	static final int BOT_NODE_ID = -2000;

	/**
	 * Gets a special map that maps any integer key to the root node.
	 *  
	 * @param root Root node, which this map will always return as a value. 
	 * @return Special map that maps any integer key to the root node.
	 * @see Lopez (2008), footnote 9 on p73
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
	final SuffixArray suffixArray;
	
	/** Corpus array representing the target language corpus. */
	final CorpusArray targetCorpus;
	
	/** Represents alignments between words in the source corpus and the target corpus. */
	final Alignments alignments;
	
	/** Lexical translation probabilities. */
	final LexicalProbabilities lexProbs;
		
	/** 
	 * Node representing phrases that start with the nonterminal X. 
	 * This node's parent is the root node of the tree. 
	 */
	private final Node xnode;
	
	public static PrefixTree getPrefixTree(String sourceFileName, String targetFileName, String alignmentFileName, String testFileName, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int ruleSampleSize, int lexSampleSize, int cacheSize, int minNonterminalSpan) throws IOException {
		
		SuffixArray.CACHE_CAPACITY = cacheSize;
		if (logger.isLoggable(Level.FINE)) logger.fine("Suffix array will cache hierarchical phrases for at most " + SuffixArray.CACHE_CAPACITY + " patterns.");
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing source language vocabulary.");
		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing source language corpus array.");
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing source language suffix array.");
		SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray);

		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing target language vocabulary.");		
		Vocabulary targetVocab = new Vocabulary();
		int[] targetWordsSentences = SuffixArrayFactory.createVocabulary(targetFileName, targetVocab);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing target language corpus array.");
		CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, targetWordsSentences[0], targetWordsSentences[1]);
		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing target language suffix array.");
		SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray);

		if (logger.isLoggable(Level.FINE)) logger.fine("Reading alignment data.");
		Alignments alignments = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);

		if (logger.isLoggable(Level.FINE)) logger.fine("Constructing lexical probabilities table");

		SampledLexProbs lexProbs = 
			new SampledLexProbs(lexSampleSize, sourceSuffixArray, targetSuffixArray, alignments, false);
		//new LexProbs(source_given_target, target_given_source, sourceVocab, targetVocab);

		if (logger.isLoggable(Level.FINE)) logger.fine("Done constructing lexical probabilities table");

		if (logger.isLoggable(Level.FINE)) logger.fine("Should store a max of " + ruleSampleSize + " rules at each node in a prefix tree.");

		Scanner testFileScanner = new Scanner(new File(testFileName));
		String line = testFileScanner.nextLine();
		
		int[] sentence = sourceVocab.getIDs(line);
		
		RuleExtractor ruleExtractor = new HierarchicalRuleExtractor(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, ruleSampleSize, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
		
		return new PrefixTree(sourceSuffixArray, targetCorpusArray, alignments, lexProbs, ruleExtractor, sentence, maxPhraseSpan, maxPhraseLength, maxNonterminals, minNonterminalSpan);
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
	 * @param minNonterminalSpan Minimum number of source language tokens 
	 *                           a nonterminal is allowed to encompass.
	 */
	public PrefixTree(SuffixArray suffixArray, CorpusArray targetCorpus, Alignments alignments, LexicalProbabilities lexProbs, RuleExtractor ruleExtractor, int[] sentence, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan) {

		if (logger.isLoggable(Level.FINE)) logger.fine("\n\n\nConstructing new PrefixTree\n\n");

		this.suffixArray = suffixArray;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.lexProbs = lexProbs;
		this.ruleExtractor = ruleExtractor;
		this.maxPhraseSpan = maxPhraseSpan;
//		this.maxNonterminalSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
//		this.sampleSize = sampleSize;

		int START_OF_SENTENCE = 0;
		int END_OF_SENTENCE = sentence.length - 1;

		Node bot = new Node(BOT_NODE_ID);
		bot.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(this);
		
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
		root.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(this);

		Pattern epsilon = new Pattern(vocab);



		// 1: children(p_eps) <-- children(p_eps) U p_x

		{	// Create and set up the X node that comes off of ROOT
			
			// Add a link from root node to X
			xnode = root.addChild(X);
			
			{ 	// Set the list of hierarchical phrases be for the X node that comes off of ROOT to an empty list.
				// Alternatively, one could consider every phrase in the corpus to match here.
				xnode.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(this);
				if (suffixArray != null)
					xnode.sourcePattern = new Pattern(suffixArray.getVocabulary(), X);
				
				// Set the bounds of the X node to be the entire suffix array.
				if (suffixArray!=null) {
					int[] bounds = {0, suffixArray.size()-1};
					xnode.setBounds(bounds);
				}
			}

			// Add a suffix link from X back to root
			Node suffixLink = calculateSuffixLink(root, X);

			if (logger.isLoggable(Level.FINEST)) {
				String oldSuffixLink = (xnode.suffixLink==null) ? "null" : "id"+xnode.suffixLink.objectID;
				String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
				logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xnode.toShortString() + " with token " + X);
			}

			xnode.linkToSuffix(suffixLink);
		}

		if (logger.isLoggable(Level.FINEST)) logger.finest("CURRENT TREE:  " + root);

		//int I = END_OF_SENTENCE; //sentence.length-1;

		Queue<Tuple> queue = new LinkedList<Tuple>();

		if (logger.isLoggable(Level.FINE)) logger.fine("Last sentence index == I == " + END_OF_SENTENCE);

		// 2: for i from 1 to I
		for (int i=START_OF_SENTENCE; i<=END_OF_SENTENCE; i++) {
			//if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + i + ","+ i +","+root+",{"+intToString(sentence[i])+"})");
			if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (\u03b5," + i + ","+ i +","+root.toShortString() +")");
			
			// 3: Add <f_i, i, i+1, p_eps> to queue
			queue.add(new Tuple(epsilon, i, i, root));
		}

		{	Pattern xpattern = new Pattern(vocab,X);
			
			int start = START_OF_SENTENCE;
			if (!SENTENCE_INITIAL_X) start += 1;
		
			// 4: for i from 1 to I
			for (int i=start; i<=END_OF_SENTENCE; i++) {
				//if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + (i-1) + ","+(i)+","+root+",{"+X+","+intToString(sentence[i])+"})");
				if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (X," + (i-1) + ","+ i +","+xnode.toShortString() +")");
				
				// 5: Add <X f_i, i-1, i+1, p_x> to queue
				queue.add(new Tuple(xpattern, i-1, i, xnode));
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
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Have tuple (" +prefixPattern+","+ i + ","+j+","+prefixNode.toShortString()+")");

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
					Node suffixNode = calculateSuffixLink(prefixNode, sentence[j]);

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

						HierarchicalPhrases result = null;
						
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
	PrefixTree(int[] sentence, int maxPhraseSpan, int maxPhraseLength, int maxNonterminals) {
		this(null, null, null, null, null, sentence, maxPhraseSpan, maxPhraseLength, maxNonterminals, 2);
	}

	public Grammar getRoot() {
		return root;
	}



	/**
	 * Implements the root QUERY algorithm (Algorithm 4) of Adam Lopez's (2008) doctoral thesis.
	 * 
	 * @param pattern
	 * @param node
	 * @param prefixNode
	 * @param suffixNode
	 * @return
	 */
	public HierarchicalPhrases query(Pattern pattern, Node node, Node prefixNode, Node suffixNode) {

		if (logger.isLoggable(Level.FINE)) logger.fine("PrefixTree.query( " + pattern + ",\n\t   new node " + node + ",\n\tprefix node " + prefixNode + ",\n\tsuffix node " + suffixNode + ")");
			
		HierarchicalPhrases result;

		int arity = pattern.arity();
		
		// 1: if alpha=u then
		//    If the pattern is contiguous, look up the pattern in the suffix array
		if (arity == 0) {

			// 2: SUFFIX-ARRAY-LOOKUP(SA_f, a alpha b, l_a_alpha, h_a_alpha
			// Get the first and last index in the suffix array for the specified pattern
			int[] bounds = suffixArray.findPhrase(pattern, 0, pattern.size(), prefixNode.lowBoundIndex, prefixNode.highBoundIndex);
			if (bounds==null) {
				result = HierarchicalPhrases.emptyList(this);
				//TOOD Should node.setBounds(bounds) be called here?
			} else {
				node.setBounds(bounds);
				int[] startingPositions = suffixArray.getAllPositions(bounds);
				result = suffixArray.createHierarchicalPhrases(startingPositions, pattern, this);
			}
			
		} else {

			// 8: If M_a_alpha_b has been precomputed (then result will be non-null)
			// 9: Retrieve M_a_alpha_b from cache of precomputations
			result = suffixArray.getMatchingPhrases(pattern);
			
			// 10: else
			if (result == null) {
				
				// 16: M_a_alpha_b <-- QUERY_INTERSECT(M_a_alpha, M_alpha_b)
				
				// Special handling of case when prefixNode is the X off of root (hierarchicalPhrases for that node is empty)
				if (arity==1 && prefixNode.sourcePattern.startsWithNonterminal() && prefixNode.sourcePattern.endsWithNonterminal()) {
			
//					result = new ArrayList<HierarchicalPhrase>(suffixNode.sourceHierarchicalPhrases.size());
					
					Vocabulary vocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
					
					int[] xwords = new int[suffixNode.sourcePattern.words.length+1];
					xwords[0] = X;
					for (int i=0; i<suffixNode.sourcePattern.words.length; i++) {
						xwords[i+1] = suffixNode.sourcePattern.words[i];
					}
					Pattern xpattern = new Pattern(vocab, xwords);
					result = new HierarchicalPhrases(xpattern, suffixNode.sourceHierarchicalPhrases);
					
//					
//					
//					for (HierarchicalPhrase phrase : suffixNode.sourceHierarchicalPhrases) {
//						result.add(new HierarchicalPhrase(xpattern, phrase.terminalSequenceStartIndices, phrase.terminalSequenceEndIndices, phrase.corpusArray, phrase.length));
//					}
					
				} else { 
					
					// Normal query intersection case (when prefixNode != X off of root)
					
					if (logger.isLoggable(Level.FINEST)) logger.finest("Calling queryIntersect("+pattern+" M_a_alpha.pattern=="+prefixNode.sourcePattern + ", M_alpha_b.pattern=="+suffixNode.sourcePattern+")");
					
					result = HierarchicalPhrases.queryIntersect(pattern, prefixNode.sourceHierarchicalPhrases, suffixNode.sourceHierarchicalPhrases);
				}
				
				suffixArray.setMatchingPhrases(pattern, result);
			}
		}

		// 17: Return M_a_alpha_b
		node.storeResults(result, pattern.words);
		return result;

	}

	
	/**
	 * Implements Function EXTEND_QUEUE from Lopez (2008) PhD Thesis, Algorithm 2, p 76
	 * 
	 * @param queue Queue of tuples
	 * @param i Start index of the pattern in the source input sentence (inclusive, 1-based).
	 * @param j End index of the pattern in the source input sentence (inclusive, 1-based).
	 * @param sentence
	 * @param pattern Pattern corresponding to the prefix node. In Lopez's terminology, this pattern is alpha f_j.
	 * @param node Node in the prefix tree to which a new node (corresponding to the pattern) will eventually be attached.
	 */
	private void extendQueue(Queue<Tuple> queue, int i, int j, int[] sentence, Pattern pattern, Node node) {

		int J = j;
		if (!SENTENCE_FINAL_X) J += 1;
		
		// 1: if |alpha| < MaxPhraseLength  and  j-i+1<=MaxPhraseSpan then 		
		if (pattern.size() < maxPhraseLength  &&  (j+1)-i+1 <= maxPhraseSpan  && J<sentence.length) {

			// 2: Add <alpha f_j, i, j+1, p_alpha> to queue
			//    (add new tuple to the queue)
			if (logger.isLoggable(Level.FINEST)) logger.finest("\nextendQueue: Adding tuple (" +pattern+","+ i + ","+ (j+1) +","+node+")");//(new Pattern(alphaPattern,sentence[j+1]))+"})");
			queue.add(new Tuple(pattern, i, j+1, node));//, sentence[j+1]));

			// 3: if arity(alpha) < MaxNonterminals then
			if (pattern.arity() < maxNonterminals) {
				Node xNode;

				if (! node.children.containsKey(X)) {

					// 4: children(p_alpha) <-- children(p_alpha) U p_alphaX
					//    (add new child node in tree and mark in as active)
					xNode = node.addChild(X);
					if (logger.isLoggable(Level.FINEST)) logger.finest("Adding node for \"" + X + "\" from " + node + " to new node " + xNode + " with alphaPattern " + pattern + "  (in extendQueue)");

					Node suffixLink = calculateSuffixLink(node, X);

					if (logger.isLoggable(Level.FINEST)) {
						String oldSuffixLink = (xNode.suffixLink==null) ? "null" : "id"+xNode.suffixLink.objectID;
						String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
						logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xNode + " (prefix node " + node + " ) with token " + X);
					}

					xNode.linkToSuffix( suffixLink );

				} else {
					// TODO Should this method simply return (or throw an exception) in this case?
					xNode = node.children.get(X);
					if (logger.isLoggable(Level.FINEST)) logger.finest("X Node is already " + xNode + " for prefixNode " + node);
				}

				// 5: Mark p_alphaX active
				xNode.active = Node.ACTIVE;
				
				// 6: Q_alphaX <-- Q_alpha
				{
//					List<HierarchicalPhrase> phrasesWithFinalX = new ArrayList<HierarchicalPhrase>(node.sourceHierarchicalPhrases.size());

					Vocabulary vocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
					Pattern xpattern = new Pattern(vocab, pattern(pattern.words, X));
					
					HierarchicalPhrases phrasesWithFinalX = new HierarchicalPhrases(xpattern, node.sourceHierarchicalPhrases); 
					
//					for (HierarchicalPhrase phrase : node.sourceHierarchicalPhrases) {
//						phrasesWithFinalX.add(new HierarchicalPhrase(phrase, X));
//					}
					//xNode.storeResults(prefixNode.sourceHierarchicalPhrases, pattern(alphaPattern.words, X));
					xNode.storeResults(phrasesWithFinalX, xpattern.words);
				}
			
				if (logger.isLoggable(Level.FINEST)) logger.finest("Alpha pattern is " + pattern);

				// For efficiency, don't add any tuples to the queue whose patterns would exceed the max allowed number of tokens
				if (pattern.words.length+2 <= maxPhraseLength) {
					
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

	static Node calculateSuffixLink(Node parent, int endOfPattern) {

		Node suffixLink = parent.suffixLink.getChild(endOfPattern);

		if (suffixLink==null)
			throw new RuntimeException("No child " + endOfPattern + " for node " + parent.suffixLink + " (Parent was " + parent + ")");

		return suffixLink;

	}

	public List<Rule> getAllRules() {
		
		return root.getAllRules();
		
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
			node.print(out, sourceVocab, targetVocab);
			//out.write(node.toRuleString(vocab).getBytes("UTF-8"));
		}
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
		
		/** Source side hierarchical phrases for this node. */
		HierarchicalPhrases sourceHierarchicalPhrases;
//		List<HierarchicalPhrase> sourceHierarchicalPhrases;

		/** Representation of the source side tokens corresponding to the hierarchical phrases for this node. */
		Pattern sourcePattern;
		
		/** Translation rules for this node. */
		List<Rule> results;
		
		
		Node(int incomingArcValue) {
			this(true,incomingArcValue);
		}

		Node(boolean active, int incomingArcValue) {
			this.active = active;
			this.suffixLink = null;
			this.children = new HashMap<Integer,Node>();
			this.incomingArcValue = incomingArcValue;
			this.objectID = nodeIDCounter++;
			this.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(PrefixTree.this);
			this.results = Collections.emptyList();
		}
		
		
		public RuleCollection getRules() {
			
			int[] empty = {};
			
			final int[] sourceSide = (sourcePattern==null) ? empty : sourcePattern.words;
			final int arity = (sourcePattern==null) ? 0 : sourcePattern.arity;
			final List<Rule> sortedResults = (results==null) ? Collections.<Rule>emptyList() : results;
			
			return new RuleCollection() {

				public int getArity() {
					return arity;
				}

				public List<Rule> getSortedRules() {
					return sortedResults;
				}

				public int[] getSourceSide() {
					return sourceSide;
				}
				
			};
			
		}
		
		/**
		 * Gets rules for this node and the children of this node.
		 * 
		 * @return rules for this node and the children of this node.
		 */
		public List<Rule> getAllRules() {
			
			List<Rule> result = new ArrayList<Rule>(getRules().getSortedRules());
			
			for (Node child : children.values()) {
				result.addAll(child.getAllRules());
			}
			
			return result;
		}
		
		
		public boolean hasExtensions() {
			if (children.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}
		
		
		public boolean hasRules() {
			if (sourceHierarchicalPhrases.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}
		
		
		public TrieGrammar matchOne(int symbol) {
			if (children.containsKey(symbol)) {
				return children.get(symbol);
			} else {
				//TOOD Is this the right thing to do here?
				return null;
			}
		}

		public TrieGrammar matchPrefix(List<Integer> symbols) {
			
			Node node = this;
			
			for (Integer symbol : symbols) {
				if (node.children.containsKey(symbol)) {
					node = node.children.get(symbol);
				} else {
					//TOOD Is this the right thing to do here?
					return null;
				}
			}
			
			return node;
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
		 * Stores in this node
		 * a list of source language hierarchical phrases, 
		 * the associated source language pattern,
		 * and the list of associated translation rules.
		 * <p>
		 * This method is responsible for creating and storing
		 * translation rules from the provided list 
		 * of source language hierarchical phrases.
		 * 
		 * @param hierarchicalPhrases Source language hierarchical phrases.
		 * @param sourceTokens Source language pattern that should correspond to the hierarchical phrases.
		 */
		public void storeResults(HierarchicalPhrases hierarchicalPhrases, int[] sourceTokens) {
			
			if (logger.isLoggable(Level.FINER)) {
				logger.finer("Storing " + hierarchicalPhrases.size() + " source phrases at node " + objectID + ":");
//				if (logger.isLoggable(Level.FINEST)) {
//					for (HierarchicalPhrase phrase : hierarchicalPhrases) {
//						logger.finest("\t" + phrase);
//					}
//				}
			}

			Vocabulary vocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
			this.sourcePattern = new Pattern(vocab, sourceTokens);
			this.results = new ArrayList<Rule>(hierarchicalPhrases.size());
			
			this.sourceHierarchicalPhrases = hierarchicalPhrases;

			if (ruleExtractor!=null) {
//				SampledList<HierarchicalPhrase> sampledHierarchicalPhrases = new SampledList<HierarchicalPhrase>(hierarchicalPhrases, sampleSize);
//				this.results = ruleExtractor.extractRules(sourcePattern, sampledHierarchicalPhrases);
				this.results = ruleExtractor.extractRules(sourcePattern, hierarchicalPhrases);
			}
			
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

			List<Node> kids = new ArrayList<Node>(children.values());
			Collections.sort(kids);

			for (Node kid : kids) {
				s.append(kid.toString(vocab));
				s.append(' ');
			}

			if (!active) s.append('*');
			s.append(']');

			return s.toString();

			//return ""+id;
		}
		
		public String toString() {
			if (suffixArray==null || suffixArray.getVocabulary()==null)
				return toString(null);
			else
				return toString(suffixArray.getVocabulary());
		}

		public String toShortString() {
			if (suffixArray==null || suffixArray.getVocabulary()==null)
				return toShortString(null);
			else
				return toShortString(suffixArray.getVocabulary());
		}
		
		public String toShortString(Vocabulary vocab) {

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

			s.append('{');
			s.append(children.size());
			s.append(" children}");

			if (!active) s.append('*');
			s.append(']');

			return s.toString();
		}
		
		private void print(OutputStream out, Vocabulary sourceVocab, Vocabulary targetVocab) throws UnsupportedEncodingException, IOException {
			
			out.write(("// Node " + objectID + ":\n").getBytes());
			
			if (results.isEmpty()) {
				out.write(("\t EMPTY\n").getBytes());
			}
			
			for (Rule rule : results) {
				out.write(rule.toString(ntVocab, sourceVocab, targetVocab).getBytes("UTF-8"));
				out.write(newline);
			}
			
			for (Node node : children.values()) {
				node.print(out, sourceVocab, targetVocab);
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

			if (!active) s.append('*');
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
			if (PrefixTree.this.maxPhraseSpan == -1) { // mono-glue grammar
				return (startIndex == 0);
			} else {
				return (endIndex - startIndex <= PrefixTree.this.maxPhraseSpan);
			}
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
	 * Default constructor - for testing purposes only.
	 * <p>
	 * The unit tests for Node require a dummy PrefixTree.
	 */
	private PrefixTree() {
		this(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
	
	private PrefixTree(int maxPhraseSpan, int maxPhraseLength, int maxNonterminals, int minNonterminalSpan) {
		root = null;
		sentence = null;
		suffixArray = null;
		targetCorpus = null;
		alignments = null;
		lexProbs = null;
		xnode = null;
		ruleExtractor = null;
		this.maxPhraseSpan = maxPhraseSpan;
		this.maxPhraseLength = maxPhraseLength;
		this.maxNonterminals = maxNonterminals;
		this.minNonterminalSpan = minNonterminalSpan;
//		this.sampleSize = sampleSize;
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
