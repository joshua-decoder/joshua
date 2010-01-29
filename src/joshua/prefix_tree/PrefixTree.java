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
import joshua.corpus.suffix_array.ParallelCorpusGrammarFactory;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.AbstractGrammar;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hiero.MemoryBasedBatchGrammar;
import joshua.util.Cache;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
public class PrefixTree extends AbstractGrammar {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(PrefixTree.class.getName());

	/**
	 * Integer representation of the nonterminal X. 
	 * All nonterminals are guaranteed to be represented by negative integers.
	 */
	public static final int X = SymbolTable.X;//-1;
	
	/** Operating system-specific end of line character(s). */
	static final byte[] newline = System.getProperty("line.separator").getBytes();
	
	/** Root node of this tree. */
	final RootNode root;

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
	boolean sentenceInitialX = false;
	
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
	boolean sentenceFinalX = false;
	
	
	boolean edgeXMayViolatePhraseSpan = false;
	
	
	/** Unique integer identifier for the root node. */
	static final int ROOT_NODE_ID = -999;
	
	/** 
	 * Unique integer identifier for the special ⊥ node
	 * that represents the suffix of the root node.
	 * @see Lopez (2008), footnote 9 on p73
	 */
	static final int BOT_NODE_ID = 0;//-2000;

	/** Suffix array representing the source language corpus. */
	final Suffixes suffixArray;
	
	/** Corpus array representing the target language corpus. */
	final Corpus targetCorpus;
	
	/** */
	final ParallelCorpusGrammarFactory parallelCorpus;
	
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

	private Set<Integer> printedNodes = null;
	
	private Map<Integer,String> ntVocab;
	
	private PrintStream out = null;
	
	private final int ruleOwner;
	
	private final int defaultLHS;
	
	private final float oovFeatureCost;
	
	/**
	 * Constructs a new prefix tree with suffix links using the
	 * GENERATE_PREFIX_TREE algorithm from Lopez (2008) PhD
	 * Thesis, Algorithm 2, p 76.
	 * 
	 * @param parallelCorpus
	 */
	public PrefixTree(ParallelCorpusGrammarFactory parallelCorpus) {

		
		if (logger.isLoggable(Level.FINER)) logger.finer("\n\n\nConstructing new PrefixTree\n\n");

		this.parallelCorpus = parallelCorpus;
		this.suffixArray = parallelCorpus.getSuffixArray();
		this.targetCorpus = parallelCorpus.getTargetCorpus();
		this.alignments = parallelCorpus.getAlignments();
		this.lexProbs = parallelCorpus.getLexProbs();
		this.ruleExtractor = parallelCorpus.getRuleExtractor();
		this.maxPhraseSpan = parallelCorpus.getMaxPhraseSpan();
		this.maxPhraseLength = parallelCorpus.getMaxPhraseLength();
		this.maxNonterminals = parallelCorpus.getMaxNonterminals();
		this.minNonterminalSpan = parallelCorpus.getMinNonterminalSpan();
		this.vocab = parallelCorpus.getSourceCorpus().getVocabulary();
		this.ruleOwner = vocab.getID(parallelCorpus.getRuleOwner());
		this.defaultLHS = vocab.getID(parallelCorpus.getDefaultLHSSymbol());
		this.oovFeatureCost = parallelCorpus.getOovFeatureCost();
		
		this.root = new RootNode(this,ROOT_NODE_ID);
		Node bot = new BotNode(parallelCorpus, root);
		this.root.linkToSuffix(bot);

		this.ntVocab = new HashMap<Integer,String>();
		ntVocab.put(PrefixTree.X, "X");
		
////		if (suffixArray==null) {
//////			vocab = null;
////		} else {
//		if (suffixArray != null) {
////			vocab = suffixArray.getVocabulary();
//			//int[] bounds = {0, suffixArray.size()-1};
//			root.setBounds(0, suffixArray.size()-1);
//		}
//		root.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList(vocab);

		// Define epsilon to be an empty pattern
		epsilon = new Pattern(vocab);

		
		// 1: children(p_eps) <-- children(p_eps) U p_x

		if (maxNonterminals > 0) {	// Create and set up the X node that comes off of ROOT
			
			// Add a link from root node to X
			xnode = root.addChild(X);

			// Add a suffix link from X back to root
			Node suffixLink = root.calculateSuffixLink(X);

			if (logger.isLoggable(Level.FINEST)) {
				String oldSuffixLink = (xnode.suffixLink==null) ? "null" : "id"+xnode.suffixLink.objectID;
				String newSuffixLink = (suffixLink==null) ? "null" : "id"+suffixLink.objectID;
				logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + xnode.toShortString(vocab) + " with token " + X);
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
		this(new ParallelCorpusGrammarFactory((Suffixes) null, (Suffixes) null, (Alignments) null, null, Integer.MAX_VALUE, maxPhraseSpan, maxPhraseLength, maxNonterminals, 2, Float.MIN_VALUE, JoshuaConfiguration.phrase_owner, JoshuaConfiguration.default_non_terminal, JoshuaConfiguration.oovFeatureCost));
	}


	/**
	 * Sets a print stream to which newly extracted rules will be written.
	 *
	 * @param out a print stream
	 *            to which newly extracted rules will be written
	 */
	public void setPrintStream(PrintStream out) {
		logger.info("Setting output stream");
		this.out = out;
		this.printedNodes = new HashSet<Integer>();
	}
	
	/**
	 * Modify this prefix tree by adding phrases for this
	 * sentence.
	 *
	 * @param sentence
	 */
	public void add(int[] sentence) {
		
		long startTime = System.nanoTime();
		
		int START_OF_SENTENCE = 0;
		int END_OF_SENTENCE = sentence.length - 1;
		
		Queue<Tuple> queue = new LinkedList<Tuple>();

		if (logger.isLoggable(Level.FINER)) logger.finer("Last sentence index == I == " + END_OF_SENTENCE);

		// 2: for i from 1 to I
		for (int i=START_OF_SENTENCE; i<=END_OF_SENTENCE; i++) {
			//if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + i + ","+ i +","+root+",{"+intToString(sentence[i])+"})");
			if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (\u03b5," + i + ","+ i +","+root.toShortString(vocab) +")");
			
			// 3: Add <f_i, i, i+1, p_eps> to queue
			queue.add(new Tuple(epsilon, i, i, root));
		}

		if (this.maxNonterminals > 0) {	Pattern xpattern = new Pattern(vocab,X);
			
			int start = START_OF_SENTENCE;
			if (!sentenceInitialX) start += 1;
		
			// 4: for i from 1 to I
			for (int i=start; i<=END_OF_SENTENCE; i++) {
				//if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (" + (i-1) + ","+(i)+","+root+",{"+X+","+intToString(sentence[i])+"})");
				if (logger.isLoggable(Level.FINEST)) logger.finest("Adding tuple (X," + (i-1) + ","+ i +","+xnode.toShortString(vocab) +")");
				
				// 5: Add <X f_i, i-1, i+1, p_x> to queue
				if (edgeXMayViolatePhraseSpan) {
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
			
			if (logger.isLoggable(Level.FINER)) logger.finer("Have tuple (" +prefixPattern+","+ i + ","+j+","+prefixNode.toShortString(vocab)+")");

			if (j <= END_OF_SENTENCE) {

				// 8: If p_alphaBetaF_i elementOf children(p_alphaBeta) then
				if (prefixNode.hasChild(sentence[j])) {

					if (logger.isLoggable(Level.FINER)) logger.finer("EXISTING node for \"" + sentence[j] + "\" from " + prefixNode.toShortString(vocab) + " to node " + prefixNode.getChild(sentence[j]).toShortString(vocab) + " with pattern " + prefixPattern);

					// child is p_alphaBetaF_j
					Node child = prefixNode.getChild(sentence[j]);
					
					// 9: If p_alphaBetaF_j is inactive then
					if (! child.active) {
						
						// 10: Continue to next item in queue
						continue;
						
						// 11: Else
					} else { 
						
						// 12: EXTEND_QUEUE(alpha beta f_j, i, j, f_1^I)
						if (logger.isLoggable(Level.FINER)) {
							logger.finer("Calling EXTEND_QUEUE("+i+","+j+","+prefixPattern+","+prefixNode.toShortString(vocab));
							if (logger.isLoggable(Level.FINEST)) logger.finest("TREE BEFOR EXTEND: " + root);
						}
						extendQueue(queue, i, j, sentence, new Pattern(prefixPattern,sentence[j]), child);
						if (logger.isLoggable(Level.FINEST)) logger.finest("TREE AFTER EXTEND: " + root);
						
					}

				} else { // 13: Else

					// 14: children(alphaBeta) <-- children(alphaBeta) U p_alphaBetaF_j
					//     (Add new child node)
					if (logger.isLoggable(Level.FINER)) logger.finer("Adding new node to node " + prefixNode.toShortString(vocab));
					Node newNode = prefixNode.addChild(sentence[j]);
					if (logger.isLoggable(Level.FINER)) {
						String word = (suffixArray==null) ? ""+sentence[j] : suffixArray.getVocabulary().getWord(sentence[j]);
						logger.finer("Created new node " + newNode.toShortString(vocab) +" for \"" + word + "\" and \n  added it to " + prefixNode.toShortString(vocab));
					}


					// 15: p_beta <-- suffix_link(p_alpha_beta)
					//     suffixNode in this code is p_beta_f_j, not p_beta
					Node suffixNode = prefixNode.calculateSuffixLink(sentence[j]);

					if (logger.isLoggable(Level.FINEST)) {
						String oldSuffixLink = (newNode.suffixLink==null) ? "null" : "id"+newNode.suffixLink.objectID;
						String newSuffixLink = (suffixNode==null) ? "null" : "id"+suffixNode.objectID;
						logger.finest("Changing suffix link from " + oldSuffixLink + " to " + newSuffixLink + " for node " + newNode.toShortString(vocab) + " (prefix node " + prefixNode.toShortString(vocab) + " ) with token " + sentence[j]);
					}
					
					newNode.linkToSuffix( suffixNode );


					// 16: if p_beta_f_j is inactive then
					if (! suffixNode.active) {
						
						// 17: Mark p_alpha_beta_f_j inactive
						newNode.active = false; //Node.INACTIVE;
						
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
							newNode.active = false; //Node.INACTIVE;
							
							// 22: else
						} else {
							
							// 23: Mark p_alpha_beta_f_j active
							newNode.active = true; //Node.ACTIVE;
							
							// 24: EXTEND_QUEUE(alpha beta f_j, i, j, f_1^I)
							extendQueue(queue, i, j, sentence, extendedPattern, newNode);
							
						}
					}
				}
			}

		}

		long endTime = System.nanoTime();
		long microseconds = (endTime - startTime) / 1000;
		float milliseconds = microseconds / 1000.0f;
		logger.info("Sentence total extraction time:\t"+ milliseconds + " milliseconds");
		
		
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

		if (logger.isLoggable(Level.FINER)) logger.finer("PrefixTree.query( " + pattern + ",\n\t   new node " + node + ",\n\tprefix node " + prefixNode + ",\n\tsuffix node " + suffixNode + ")");
		long startTime = System.nanoTime();
		
		MatchedHierarchicalPhrases result;

//		boolean stop = false;
//		if (pattern.toString().startsWith("[de ")) {
//			logger.warning("Found it! " + pattern.toString() + " yahoo");
//			int x;
//			x=5;
//			x+=1;
//			stop = true;
//		}
//		
//		if (stop) {
//			if (stop) {
//				logger.info("Stopping");
//				logger.info("Did you stop?");
//			}
//		}
//		
		
		if (suffixArray.getCachedHierarchicalPhrases().containsKey(pattern)) {
			result = suffixArray.getCachedHierarchicalPhrases().get(pattern);
			int[] bounds = suffixArray.findPhrase(pattern, 0, pattern.size(), prefixNode.lowBoundIndex, prefixNode.highBoundIndex);
			if (bounds!=null) {
				node.setBounds(bounds[0],bounds[1]);
			}
		} else {
			if (pattern.toString().startsWith("[de ")) {
				int x = 5;
				x++;
			}

			int arity = pattern.arity();

			// 1: if alpha=u then
			//    If the pattern is contiguous, look up the pattern in the suffix array
			if (arity == 0) {

				// 2: SUFFIX-ARRAY-LOOKUP(SA_f, a alpha b, l_a_alpha, h_a_alpha
				// Get the first and last index in the suffix array for the specified pattern
				int[] bounds = suffixArray.findPhrase(pattern, 0, pattern.size(), prefixNode.lowBoundIndex, prefixNode.highBoundIndex);
				if (bounds==null) {
					result = HierarchicalPhrases.emptyList(pattern);
					suffixArray.cacheMatchingPhrases(result);
					//TODO Should node.setBounds(bounds) be called here?
				} else {
					node.setBounds(bounds[0],bounds[1]);
					int[] startingPositions = suffixArray.getAllPositions(bounds);
					result = suffixArray.createTriviallyHierarchicalPhrases(startingPositions, pattern, vocab);
				}


			} else { // 3: else --- alpha is a discontiguous pattern

				// 8: If M_a_alpha_b has been precomputed (then result will be non-null)
				// 9: Retrieve M_a_alpha_b from cache of precomputations


				// 10: else
				if (suffixArray.getCachedHierarchicalPhrases().containsKey(pattern)) {	
					result = suffixArray.getMatchingPhrases(pattern);
				} else {

					// 16: M_a_alpha_b <-- QUERY_INTERSECT(M_a_alpha, M_alpha_b)

					int[] sourceWords = prefixNode.getSourcePattern().getWordIDs();

					// Special handling of case when prefixNode is the X off of root (hierarchicalPhrases for that node is empty)
					if (arity==1 && sourceWords[0] < 0 && sourceWords[sourceWords.length-1] < 0){

						result = suffixNode.getMatchedPhrases().copyWithInitialX();

					} else { 

						// Normal query intersection case (when prefixNode != X off of root)

						if (logger.isLoggable(Level.FINEST)) logger.finest("Calling queryIntersect("+pattern+" M_a_alpha.pattern=="+prefixNode.getSourcePattern() + ", M_alpha_b.pattern=="+suffixNode.getSourcePattern()+")");

						result = HierarchicalPhrases.queryIntersect(pattern, prefixNode.getMatchedPhrases(), suffixNode.getMatchedPhrases(), minNonterminalSpan, maxPhraseSpan, suffixArray);

					}

					suffixArray.cacheMatchingPhrases(result);
				}
			}
		}
		
		long finalQueryTime = System.nanoTime();
		if (logger.isLoggable(Level.FINE)) {
			long elapsedQueryTime = finalQueryTime - startTime;
			long microseconds = elapsedQueryTime / 1000;
			float milliseconds = microseconds / 1000.0f;
			logger.fine("Time to query pattern:\t" + pattern.toString() + "\t" + milliseconds + " milliseconds\t" + result.size() + " instances");
		}
		
		// 17: Return M_a_alpha_b
		List<Rule> rules = ruleExtractor.extractRules(result);
//		node.storeResults(result, rules);
		storeResults(node, result, rules);
		
		if (logger.isLoggable(Level.FINE)) {
			long elapsedTime = System.nanoTime() - finalQueryTime;
			long microseconds = elapsedTime / 1000;
			float milliseconds = microseconds / 1000.0f;
			logger.fine("Time to extract rules for pattern:\t" + pattern.toString() + "\t" + milliseconds + " milliseconds\t" + result.size() + " instances");
		}

		return result;

	}
	
	@SuppressWarnings("deprecation")
	private void storeResults(Node node, MatchedHierarchicalPhrases result, List<Rule> rules) {
		if (printedNodes==null || !printedNodes.contains(node.objectID)) {
			node.storeResults(result, rules);

			if (out==null) {
				logger.finer("Not printing rules");
			} else {

				for (Rule rule : rules) {
					String ruleString = rule.toString(ntVocab, suffixArray.getVocabulary(), targetCorpus.getVocabulary());
					if (logger.isLoggable(Level.FINEST)) logger.finest("Rule: " + ruleString);
					out.println(ruleString);
				}
				printedNodes.add(node.objectID);

			}
		}
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
		if (!sentenceFinalX) J += 1;

		int endOfPhraseSpan = (j+1)-i+1;

		
		// 1: if |alpha| < MaxPhraseLength  and  j-i+1<=MaxPhraseSpan then 		
		if (pattern.size() < maxPhraseLength  && J<sentence.length) {

			if (endOfPhraseSpan <= maxPhraseSpan) {
				// 2: Add <alpha f_j, i, j+1, p_alpha> to queue
				//    (add new tuple to the queue)
				if (logger.isLoggable(Level.FINEST)) logger.finest("\nextendQueue: Adding tuple (" +pattern+","+ i + ","+ (j+1) +","+node+")");//(new Pattern(alphaPattern,sentence[j+1]))+"})");
				queue.add(new Tuple(pattern, i, j+1, node));//, sentence[j+1]));
			}

			if (edgeXMayViolatePhraseSpan) endOfPhraseSpan -= 1;
			
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
				xNode.active = true; //Node.ACTIVE;
				
				int[] patternWords = pattern.getWordIDs();
				
				// 6: Q_alphaX <-- Q_alpha
				{
					SymbolTable vocab = (suffixArray==null) ? null : suffixArray.getVocabulary();
					Pattern xpattern = new Pattern(vocab, patternWords, X);
					
//					HierarchicalPhrases phrasesWithFinalX = new HierarchicalPhrases(xpattern, node.sourceHierarchicalPhrases); 
					MatchedHierarchicalPhrases phrasesWithFinalX;
					if (suffixArray==null) {
						// This should only happen in certain unit tests
						logger.severe("This should only be encountered during unit testing!");
						if (node.sourceHierarchicalPhrases==null) {
							node.sourceHierarchicalPhrases = HierarchicalPhrases.emptyList((SymbolTable) null);
							node.sourcePattern = node.sourceHierarchicalPhrases.getPattern();
						}
						phrasesWithFinalX = node.getMatchedPhrases().copyWithFinalX();
					} else {
						Cache<Pattern,MatchedHierarchicalPhrases> cache = suffixArray.getCachedHierarchicalPhrases();
						if (cache.containsKey(xpattern)) {
							phrasesWithFinalX = cache.get(xpattern);
						} else {
							phrasesWithFinalX = node.getMatchedPhrases().copyWithFinalX();
							suffixArray.cacheMatchingPhrases(phrasesWithFinalX);
						}
					}	
					
					List<Rule> rules = (ruleExtractor==null) ? 
								Collections.<Rule>emptyList() : 
								ruleExtractor.extractRules(phrasesWithFinalX);
					//xNode.storeResults(phrasesWithFinalX, rules);
					storeResults(xNode, phrasesWithFinalX, rules);
				}
			
				if (logger.isLoggable(Level.FINEST)) logger.finest("Alpha pattern is " + pattern);

				// For efficiency, don't add any tuples to the queue whose patterns would exceed the max allowed number of tokens
				if (patternWords.length+2 <= maxPhraseLength) {
					
					int I = sentence.length;
					if (!sentenceFinalX) I -= 1;
					
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


//	/**
//	 * Gets the root node of this tree.
//	 * 
//	 * @return the root node of this tree
//	 */
//	public Grammar getRoot() {
//		return root;
//	}
	
//	/**
//	 * Gets all translation rules stored in this tree.
//	 * 
//	 * @return all translation rules stored in this tree
//	 */
//	public List<Rule> getAllRules() {
//		
//		return root.getAllRules();
//		
//	}

	/* See Javadoc for java.lang.Object#toString. */
	public String toString() {
		return root.toTreeString("", vocab);
	}

	/**
	 * Gets the number of nodes in this tree.
	 * <p>
	 * This method recursively traverses through all nodes
	 * in the tree every time this method is called.
	 * 
	 * @return the number of nodes in this tree
	 */
	public int size() {
		return root.size();
	}

	
	/**
	 * Constructs an invalid, dummy prefix tree.
	 * <p>
	 * The unit tests for Node require a dummy PrefixTree.
	 */
	private PrefixTree() {
		root = null;
		parallelCorpus = null;
		suffixArray = null;
		targetCorpus = null;
		alignments = null;
		lexProbs = null;
		xnode = null;
		ruleExtractor = null;
		this.epsilon = null;
		this.vocab = null;
		this.maxPhraseSpan = Integer.MIN_VALUE;
		this.maxPhraseLength = Integer.MIN_VALUE;
		this.maxNonterminals = Integer.MIN_VALUE;
		this.minNonterminalSpan = Integer.MAX_VALUE;
		this.ruleOwner = Integer.MIN_VALUE;
		this.defaultLHS = Integer.MIN_VALUE;
		this.oovFeatureCost = Float.NaN;
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
	
	
	public Rule constructManualRule(int lhs, int[] sourceWords,
			int[] targetWords, float[] scores, int arity) {
		return new BilingualRule(lhs, sourceWords, targetWords, scores, arity, this.ruleOwner, 0, getOOVRuleID());
	}

	public Rule constructOOVRule(int numFeatures, int sourceWord, int targetWord,
			boolean hasLM) {
		int[] french      = new int[1];
		french[0]         = sourceWord;
		int[] english       = new int[1];
		english[0]          = targetWord;
		float[] feat_scores = new float[numFeatures];
		
		// TODO: This is a hack to make the decoding without a LM works
		/**when a ngram LM is used, the OOV word will have a cost 100.
		 * if no LM is used for decoding, so we should set the cost of some
		 * TM feature to be maximum
		 * */
		if ( (!hasLM) && numFeatures > 0) { 
			feat_scores[0] = oovFeatureCost;
		}
		
		return new BilingualRule(
				this.defaultLHS, french, english, 
				feat_scores, 0, this.ruleOwner, 
				0, getOOVRuleID());

	}

	public int getNumRules() {
		return root.getNumRules();
	}

	public int getOOVRuleID() {
		return MemoryBasedBatchGrammar.OOV_RULE_ID;
	}

	public Trie getTrieRoot() {
		return root;
	}

	public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
		return (endIndex - startIndex <= this.maxPhraseSpan);
	}
	
}
