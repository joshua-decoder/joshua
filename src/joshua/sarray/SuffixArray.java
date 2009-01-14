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

import joshua.util.ReverseOrder;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;
import joshua.util.Cache;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * SuffixArray is the main class for producing suffix arrays
 * from corpora, and manipulating them once created.  Suffix arrays
 * are a space economical way of storing a corpus and allowing
 * very quick searching of any substring within the corpus.  A
 * suffix array contains a list of references to every point in a
 * corpus, and each reference denotes the suffix starting at that
 * point and continuing to the end of the corpus.  The suffix array
 * is sorted alphabetically, so any substring within the corpus
 * can be found with a binary search in O(log n) time, where n is
 * the length of the corpus.
 *
 * @author  Colin Bannard
 * @since   10 December 2004
 * @author  Josh Schroeder
 * @since   2 Jan 2005
 * @author  Chris Callison-Burch
 * @since   9 February 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */
public class SuffixArray implements Corpus {

//===============================================================
// Constants
//===============================================================

	/**
	 * The maximum length suffix to consider during sorting.
	 */
	public static int MAX_COMPARISON_LENGTH = 20;
	
	/** 
	 * Maximum number of items that can be stored 
	 * in the cache of patterns and hierarchical phrases. 
	 */
	public static int CACHE_CAPACITY = 100000;
	
	/**
	 * Maps from patterns to lists of hierarchical phrases
	 * that match the corresponding pattern in the corpus.
	 * <p>
	 * This cache is a most-recently accessed map, 
	 * so commonly accessed patterns will remain in the cache,
	 * while rare patterns will eventually drop out of the cache.
	 */
	protected Cache<Pattern,HierarchicalPhrases> hierarchicalPhraseCache;
	
	
	/**
	 * A random number generator used in the quick sort
	 * implementation.
	 */
	private static final Random RAND = new Random();
	
	
	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(SuffixArray.class.getName());
	
	
//===============================================================
// Member variables
//===============================================================

	protected int[] suffixes;
	
	/** Integer array representation of the corpus for this suffix array. */
	protected CorpusArray corpus;
		
//===============================================================
// Constructor(s)
//===============================================================

	/** 
	 * Constructor takes a CorpusArray and creates a sorted
	 * suffix array from it.
	 */
	public SuffixArray(CorpusArray corpusArray) {
		this.corpus = corpusArray;
		suffixes = new int[corpusArray.size()];

		// Create an array of suffix IDs
		for (int i = 0; i < corpusArray.size(); i++) {
			suffixes[i] = i;
		}
		// Sort the array of suffixes
		sort(suffixes);
	
		this.hierarchicalPhraseCache = new Cache<Pattern,HierarchicalPhrases>(CACHE_CAPACITY);
	}
	
	
	/**
	 * Protected constructor takes in the already prepared
	 * member variables.
	 *
	 * @see SuffixArrayFactor.createSuffixArray(CorpusArray)
	 * @see SuffixArrayFactor.loadSuffixArray(String,String,String,CorpusArray)
	 */
	protected SuffixArray(int[] suffixes, CorpusArray corpusArray) {
		this.suffixes = suffixes;
		this.corpus = corpusArray;
		this.hierarchicalPhraseCache = new Cache<Pattern,HierarchicalPhrases>(CACHE_CAPACITY);
	}
	
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================

	/**
	 * Implemented for the Corpus interface. 
	 *
	 * @return the vocabulary that compriseds this corpus
	 */
	public Vocabulary getVocabulary() {
		return corpus.vocab;
	}
	
	
	/** Implemented for the Corpus interface. */
	public int getNumSentences() {
		return corpus.getNumSentences();
	}
	
	
	/** Implemented for the Corpus interface. */
	public int getNumWords() {
		return corpus.size();
	}
	
	
	/** Implemented for the Corpus interface. */
	public Phrase getSentence(int sentenceIndex) {
		return corpus.getSentence(sentenceIndex);
	}
	

	/**
	 * @return the phrase spanning the specified indices in the
	 *         corpus.
	 */
	public Phrase getPhrase(int startPosition, int endPosition) {
		return corpus.getPhrase(startPosition, endPosition);
	}
	
	
	/**
	 * Gets a list of phrases.
	 * 
	 * @param startPositions List of start positions in the
	 *                       corpus array.
	 * @param length Length of the phrase to be extracted.
	 * @return A list of phrases.
	 */
	public List<Phrase> getPhrases(int[] startPositions, int length) {
		List<Phrase> results = new ArrayList<Phrase>(startPositions.length);
		
		for (int start : startPositions) {
			results.add(corpus.getPhrase(start, start+length));
		}
		
		return results;
	}
	
	protected int getWord(int position) {
		int corpusIndex = getCorpusIndex(position);
		return corpus.getWordID(corpusIndex);
	}
	
	/**
	 * @return the number of time that the specified phrase
	 *         occurs in the corpus.
	 */
	public int getNumOccurrences(Phrase phrase) {
		int[] bounds = findPhrase(phrase);
		if(bounds == null) return 0;
		int numOccurrences = (bounds[1]-bounds[0]) +1;
		return numOccurrences;
	}
	
	
	/**
	 * Returns the number of suffixes in the suffix array, which
	 * is identical to the length of the corpus.
	 */
	public int size() {
		return suffixes.length;
	}
	
	
	/** 
	 * @return the position in the corpus corresponding to the
	 *         specified index in the suffix array.
	 */
	public int getCorpusIndex(int suffixIndex) {
		return suffixes[suffixIndex];
	}
	
	
	/**
	 * @return the sentence number corresponding the specified
	 *         corpus index.
	 */
	public int getSentenceIndex(int corpusIndex) {
		return corpus.getSentenceIndex(corpusIndex);
	}
	
	
	/**
	 * @return the corpus index that corresponds to the start
	 *         of the sentence.
	 */
	public int getSentencePosition(int sentenceIndex) {
		return corpus.getSentencePosition(sentenceIndex);
	}
	
	
	//===========================================================
	// Methods
	//===========================================================
	
	
	
	/**
	 * Returns a list of the sentence numbers which contain the
	 * specified phrase.
	 *
	 * @param phrase the phrase to look for
	 * @return a list of the sentence numbers
	 */
	public int[] findSentencesContaining(Phrase phrase) {
		return findSentencesContaining(phrase, Integer.MAX_VALUE);
	}
	
	
	/**
	 * Returns a list of the sentence numbers which contain the
	 * specified phrase.
	 *
	 * @param phrase the phrase to look for
	 * @param maxSentences the maximum number of sentences to return
	 * @return a list of the sentence numbers
	 */
	public int[] findSentencesContaining(Phrase phrase, int maxSentences) {
		int[] bounds = findPhrase(phrase);
		if (bounds == null) return null;
		int numOccurrences = (bounds[1]-bounds[0]) +1;
		
		int[] sentences = new int[Math.min(maxSentences, numOccurrences)];
		for (int i = 0; i < sentences.length; i++) {
			sentences[i] = corpus.getSentenceIndex(getCorpusIndex(bounds[0]+i));
		}
		return sentences;
	}
	
	
	/**
	 * Finds a phrase in the suffix array.
	 *
	 * @param phrase the search phrase
	 * @return a tuple containing the (inclusive) start and the (inclusive) end bounds
	 *         in the suffix array for the phrase
	 */
	public int[] findPhrase(Phrase phrase) {
		return findPhrase(phrase, 0, phrase.size());	
	}
	
	

	
//	/**
//	 * This method creates a list of trivially HierarchicalPhrases
//	 * (i.e. they're really just contiguous phrases, but we
//	 * will want to perform some of the HierarchialPhrase
//	 * operations on them). Sorts the positions. Adds the results
//	 * to the cache.  
//	 *<p>
//	 * The construction of more complex hierarchical phrases is handled
//	 * within the prefix tree. 
//	 * <p>
//	 * This method performs deterministic sampling, as described in Lopez (2008) p59:
//	 * <blockquote>
//	 * To resolve this issue, we used deterministic sampling. Whenever a source phrase occurs 
//more frequently than the maximum sample size, we take our samples at uniform intervals over 
//the set of locations returned by the sufﬁx array. With this strategy in place, hypotheses receive the 
//same feature weights between different runs of the decoder, the results are deterministic, and the 
//MERT algorithm converges at the same rate as it does without sampling.
//	 * </blockquote>
//	 * 
//	 * @param startPositions an unsorted list of the positions
//	 *                in the corpus where the matched phrases begin
//	 * @param pattern a contiguous phrase
//	 * @return a list of trivially hierarchical phrases
//	 */ 
//	protected List<HierarchicalPhrase> createHierarchicalPhrases(int[] startPositions, Pattern pattern) {
//		if (startPositions == null) {
//			return Collections.emptyList();
//		} else if (hierarchicalPhraseCache.containsKey(pattern)) {
//			return hierarchicalPhraseCache.get(pattern);
//		} else {
//			Arrays.sort(startPositions);
//			int length = pattern.size();
//			ArrayList<HierarchicalPhrase> hierarchicalPhrases = new ArrayList<HierarchicalPhrase>(startPositions.length);
//			//XXX Should we do sampling here or not?
//			int step = //(startPositions.length<sampleSize) ? 1 : startPositions.length / sampleSize;
//				1;
//			for(int i = 0; i < startPositions.length; i+=step) { 
//				int[] position = {startPositions[i]};
//				int[] endPosition = {startPositions[i] + length};
//				HierarchicalPhrase hierarchicalPhrase = new HierarchicalPhrase(pattern, position, endPosition, corpus, length);
//				hierarchicalPhrases.add(hierarchicalPhrase);
//			}	
//			hierarchicalPhraseCache.put(pattern, hierarchicalPhrases);
//			return hierarchicalPhrases;
//		}
//	}
	
	/**
	 * This method creates a list of trivially HierarchicalPhrases
	 * (i.e. they're really just contiguous phrases, but we
	 * will want to perform some of the HierarchialPhrase
	 * operations on them). Sorts the positions. Adds the results
	 * to the cache.  
	 *<p>
	 * The construction of more complex hierarchical phrases is handled
	 * within the prefix tree. 
	 * <p>
	 * This method performs deterministic sampling, as described in Lopez (2008) p59:
	 * <blockquote>
	 * To resolve this issue, we used deterministic sampling. Whenever a source phrase occurs 
more frequently than the maximum sample size, we take our samples at uniform intervals over 
the set of locations returned by the sufﬁx array. With this strategy in place, hypotheses receive the 
same feature weights between different runs of the decoder, the results are deterministic, and the 
MERT algorithm converges at the same rate as it does without sampling.
	 * </blockquote>
	 * 
	 * @param startPositions an unsorted list of the positions
	 *                in the corpus where the matched phrases begin
	 * @param pattern a contiguous phrase
	 * @return a list of trivially hierarchical phrases
	 */ 
	protected HierarchicalPhrases createHierarchicalPhrases(int[] startPositions, Pattern pattern, PrefixTree prefixTree) {
		if (startPositions == null) {
			return HierarchicalPhrases.emptyList(prefixTree);
		} else if (hierarchicalPhraseCache.containsKey(pattern)) {
			return hierarchicalPhraseCache.get(pattern);
		} else {
			Arrays.sort(startPositions);
//			int length = pattern.size();
			HierarchicalPhrases hierarchicalPhrases = new HierarchicalPhrases(pattern, startPositions, prefixTree);
//			ArrayList<HierarchicalPhrase> hierarchicalPhrases = new ArrayList<HierarchicalPhrase>(startPositions.length);
//			 
//			int step = //(startPositions.length<sampleSize) ? 1 : startPositions.length / sampleSize;
//				1;
//			for(int i = 0; i < startPositions.length; i+=step) { 
//				int[] position = {startPositions[i]};
//				int[] endPosition = {startPositions[i] + length};
//				HierarchicalPhrase hierarchicalPhrase = new HierarchicalPhrase(pattern, position, endPosition, corpus, length);
//				hierarchicalPhrases.add(hierarchicalPhrase);
//			}	
			hierarchicalPhraseCache.put(pattern, hierarchicalPhrases);
			return hierarchicalPhrases;
		}
	}
	
	/**
	 * @return a list of hierarchical phrases that match the pattern if they are already cached
	 *         or null if the pattern is not in the cache.
	 */
	public HierarchicalPhrases getMatchingPhrases(Pattern pattern) {
		return hierarchicalPhraseCache.get(pattern);
	}
	
	/** 
	 * Caches the matching hierarchical phrases for the pattern. 
	 */
	public void setMatchingPhrases(Pattern pattern, HierarchicalPhrases matchings) {
		hierarchicalPhraseCache.put(pattern, matchings);
	}
	
	
	
	/**
	 * Gets all of the positions in the corpus for the bounds
	 * in the suffix array, sorting the corpus position.
	 * 
	 * @param bounds Inclusive bounds in the suffix array
	 * @return
	 */
	public int[] getAllPositions(int[] bounds) {
		if (bounds != null) {
			int startInSuffixArray = bounds[0];
			int endInSuffixArray = bounds[1];
			int length = endInSuffixArray - startInSuffixArray + 1;
			int[] positions = new int[length];
			for (int i = 0; i < length; i++) {
				positions[i] = getCorpusIndex(i+startInSuffixArray);
			}
			Arrays.sort(positions);
			return positions;
		} else {
			return new int[0];
		}
	}
	
	
	/**
	 * Calculates the most frequent phrases in the corpus.
	 * Populates the phrases list with them, and the frequencies
	 * list with their frequenies.  Allows a threshold to be
	 * set for the minimum frequency to remember, as well as
	 * the maximum number of phrases.
	 *
	 * @param frequencies  a list of the phrases frequencies
	 * @param minFrequency the minimum frequency required to
	 *                     retain phrases
	 * @param maxPhrases   the maximum number of phrases to return
	 * @return the most frequent phrases
	 */
	public List<Phrase> getMostFrequentPhrases(
		List<Integer> frequencies,
		int minFrequency,
		int maxPhrases,
		int maxPhraseLength
	) {
		return getMostFrequentPhrases(new ArrayList<Phrase>(), frequencies, minFrequency, maxPhrases, maxPhraseLength);
	}
	
	/**
	 * Calculates the most frequent phrases in the corpus.
	 * Populates the phrases list with them, and the frequencies
	 * list with their frequencies.  Allows a threshold to be
	 * set for the minimum frequency to remember, as well as
	 * the maximum number of phrases.
	 *
	 * @param phrases      a list to store the most frequent phrases; this object will be returned
	 * @param frequencies  a list of the phrases frequencies
	 * @param minFrequency the minimum frequency required to
	 *                     retain phrases
	 * @param maxPhrases   the maximum number of phrases to return
	 * @return the most frequent phrases (same object as the <code>phrases</code> parameter
	 */
	public List<Phrase> getMostFrequentPhrases(
		List<Phrase> phrases,
		List<Integer> frequencies,
		int minFrequency,
		int maxPhrases,
		int maxPhraseLength
	) {
		
		phrases.clear();
		frequencies.clear();
		Comparator<Integer> comparator = new ReverseOrder<Integer>();
		
		// calculate the longest common prefix delimited intervals...
		// This is taken from the Yamamoto and Church Compuational Linguistics article.
		int[] longestCommonPrefixes = calculateLongestCommonPrefixes();
		Stack<Integer> startIndices = new Stack<Integer>();
		Stack<Integer> shortestInteriorLCPIndices = new Stack<Integer>();
		startIndices.push(0);
		shortestInteriorLCPIndices.push(0);
		for (int j = 0; j < suffixes.length; j++) {			
			// trivial interval i==j, frequecy=1
			recordPhraseFrequencies(
				longestCommonPrefixes, j, j, 0, phrases, frequencies,
				minFrequency, maxPhrases, maxPhraseLength, comparator);
			while (longestCommonPrefixes[j+1] < longestCommonPrefixes[shortestInteriorLCPIndices.peek()]) {
				// non-trivial interval 
				recordPhraseFrequencies(longestCommonPrefixes, startIndices.pop(), j, shortestInteriorLCPIndices.pop(),
										   phrases, frequencies, minFrequency, maxPhrases, maxPhraseLength, comparator);
			}
			startIndices.push(shortestInteriorLCPIndices.peek());
			shortestInteriorLCPIndices.push(j+1);
		
		
			// trim the lists if they're too long...
			if (phrases.size() > maxPhrases) {
				int frequency = frequencies.get(maxPhrases);
				int cutPoint = maxPhrases;
				if (phrases.size() >= maxPhrases
				&& frequency == frequencies.get(maxPhrases)) {
					cutPoint = Collections.binarySearch(frequencies, frequency, comparator);
					if (cutPoint < 0) {
						cutPoint = -1 * cutPoint;
						cutPoint--;
					}
					while (cutPoint > 0 && frequencies.get(cutPoint-1) == frequency) {
						cutPoint--;
					}
				}
				// ccb - not sure why the subList operation didn't carry over outside this method...
				//phrases = phrases.subList(0, cutPoint);
				//frequencies = frequencies.subList(0, cutPoint);
				// for now it seems that we have to explicity remove the elements. 
				for (int i = phrases.size()-1; i >= cutPoint; i--) {
					phrases.remove(i);
					frequencies.remove(i);
				}
			}
		}
		return phrases;
	}
	
	
	/**
	 * This method performs a one-pass computation of the
	 * collocation of two frequent subphrases. It is used for
	 * the precalculation of the translations of hierarchical
	 * phrases which are problematic to calculate on the fly.
	 * This proceedure is described in "Hierarchical Phrase-Based
	 * Transslation with Suffix Arrays" by Adam Lopez.
	 *
	 * @param phrases         the phrases which are to be checked
	 *                        for their collocation
	 * @param maxPhraseLength the maximum length of any phrase
	 *                        in the phrases
	 * @param windowSize      the maximum allowable space between
	 *                        phrases for them to still be
	 *                        considered collocated
	 */
	public Collocations getCollocations(
		HashSet<Phrase> phrases,
		int maxPhraseLength,
		int windowSize
	) {
		ArrayList<Phrase> phrasesInWindow = new ArrayList<Phrase>();
		ArrayList<Integer> positions = new ArrayList<Integer>();
		int sentenceNumber = 1;
		int endOfSentence = getSentencePosition(sentenceNumber)-1;
		
		// ccb - debugging
		if (logger.isLoggable(Level.FINEST)) logger.finest("END OF SENT: " + endOfSentence);
		
		// collocations maps Phrase->Phrase->a list of
		// positions in corpus
		Collocations collocations = new Collocations();
		
		for (int currentPosition = 0; currentPosition < suffixes.length; currentPosition++) {
			
			for (int i = 1, endOfPhrase = currentPosition + i; 
				i < maxPhraseLength  &&  endOfPhrase < endOfSentence  &&  endOfPhrase <= suffixes.length; 
				i++, endOfPhrase = currentPosition + i) {
				
				Phrase phrase = new ContiguousPhrase(currentPosition, endOfPhrase, corpus);
				if (phrases.contains(phrase)) {
					// ccb - debugging 
					if (logger.isLoggable(Level.FINEST)) logger.finest("\"" + phrase + "\" found at currentPosition " + currentPosition);
					phrasesInWindow.add(phrase);
					positions.add(currentPosition);
				}
			}
			
			// check whether we're at the end of the sentence and dequeue...
			if (currentPosition == endOfSentence) {
				// ccb - debugging
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("REACHED END OF SENT: " + currentPosition);
					logger.finest("PHRASES:   " + phrasesInWindow);
					logger.finest("POSITIONS: " + positions);
				}

				// empty the whole queue...
				for (int i = 0; i < phrasesInWindow.size()-1; i++) {
					Phrase phrase1 = phrasesInWindow.get(i);
					int position1 = positions.get(i);
					for (int j = i+1; j < phrasesInWindow.size(); j++) {
						Phrase phrase2 = phrasesInWindow.get(j);
						int position2 = positions.get(j);
						collocations.add(phrase1, phrase2, position1, position2);
						// ccb - debugging
						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE1: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
					}
				}
				// clear the queues
				phrasesInWindow.clear();
				positions.clear();
				// update the end of sentence marker
				sentenceNumber++;
				endOfSentence = getSentencePosition(sentenceNumber)-1;
				
				// ccb - debugging
				if (logger.isLoggable(Level.FINEST)) logger.finest("END OF SENT: " + endOfSentence);
			}
			
			// check whether the initial elements are
			// outside the window size...
			if (phrasesInWindow.size() > 0) {
				int position1 = positions.get(0);
				// deque the first element and
				// calculate its collocations...
				while ((position1+windowSize < currentPosition)
				&& phrasesInWindow.size() > 0) {
					// ccb - debugging
					if (logger.isLoggable(Level.FINEST)) logger.finest("OUTSIDE OF WINDOW: " + position1 + " " +  currentPosition + " " + windowSize);
					Phrase phrase1 = phrasesInWindow.remove(0);
					positions.remove(0);
					for (int j = 0; j < phrasesInWindow.size(); j++) {
						Phrase phrase2 = phrasesInWindow.get(j);
						int position2 = positions.get(j);
						collocations.add(phrase1, phrase2, position1, position2);
						// ccb - debugging
						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE2: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
					}
					if (phrasesInWindow.size() > 0) {
						position1 = positions.get(0);
					} else {
						position1 = currentPosition;
					}
				}
			}
		}
		return collocations;
	 }
	 
	 
	 /*
	 public List<int[]> queryIntersect(List<int[]> prefixMatches, List<int[]> suffixMatches) {
		 
		 List<int[]> result = new ArrayList<int[]>();
		 
		 int I = prefixMatches.size();
		 int J = suffixMatches.size();
		 
		 int j = 0;
		 int m1 = -1;
		 
		 for (int i=0; i<I; i++) {
			 
			 int[] prefixMatch = prefixMatches.get(i);
			 int[] suffixMatch = suffixMatches.get(j);
			 
			 if (prefixMatch[0] != m1) {
				 
			 }
			 
		 }
		 
		 return result;
	 }
	 */
	 
	 
	 /*
	 public boolean matchPrecedes(int[] matchA, int[] matchB) {
		 int sentenceIndexA = corpus.getSentenceIndex(matchA[0]);
		 int sentenceIndexB = corpus.getSentenceIndex(matchB[0]);
		 
		 if (matchA[matchA.length-1] != matchB[0]) {
			 
			 if (sentenceIndexA > sentenceIndexB) {
				 return true;
			 } else if (sentenceIndexA == sentenceIndexB) {
				 if (matchA[0] > matchB[0]-1) {
					 return true;
				 } else {
					 return false;
				 }
			 } else {
				 return false;
			 }
			 
		 } else {
			 
			 
		 }
	 }
	 */
	 
	 
	 /**
	  * Scratch method for Lane to work on Fast Intersection
	  */
	 public static void fastIntersection() {
		
		SortedSet<Integer> setA = new TreeSet<Integer>();
		SortedSet<Integer> setB = new TreeSet<Integer>();
		
		Random rand = new Random();
		
		
		for (int i = 0; i < 100000; i++) {
			setA.add(rand.nextInt());
		}
		
		List<Integer> dataSet = new ArrayList<Integer>(setA);
		
		for (int i = 0; i < 50; i++) {
			setB.add(rand.nextInt());
			setB.add(dataSet.get(rand.nextInt(setA.size())));
		}
		
		List<Integer> querySet = new ArrayList<Integer>(setB);
		
		//for (int i=0, n=dataSet.size(); i < n; i++, n++) dataSet.get(i);
		
		// Calculate intersection
		
		//List<Integer> intersection = new ArrayList<Integer>();
		/*
		for (int query : querySet) {
			//System.out.println("Querying for " + query);
			if (Collections.binarySearch(dataSet, query) >= 0) {
				intersection.add(query);
			}
		}
		*/
		
		SortedSet<Integer> intersection = new TreeSet<Integer>();
		
		fastIntersect(dataSet, querySet, intersection);
		
		System.out.println(intersection.size());
		//int medianQuery = querySet.get(querySet.size()/2);
	 }
	 
	 
	 /**
	  * Private helper method for performing fast intersection.
	  * 
	  * @param <E>
	  * @param sortedData
	  * @param sortedQueries
	  * @param result
	  */
	 private static <E extends Comparable<E>> void fastIntersect(List<E> sortedData, List<E> sortedQueries, SortedSet<E> result) {
		 
		 int medianQueryIndex = sortedQueries.size() / 2;
		 E medianQuery = sortedQueries.get(medianQueryIndex);
		 
		 int index = Collections.binarySearch(sortedData, medianQuery);
		 
		 if (index >= 0) {
			 result.add(medianQuery);
		 } else {
			 index = (-1 * index) + 1;
		 }
		 
		 if (index-1 >= 0 && medianQueryIndex-1 >=0) {
			 fastIntersect(sortedData.subList(0, index), sortedQueries.subList(0, medianQueryIndex), result);
		 }
		 
		 if (index+1 < sortedData.size()  &&  medianQueryIndex+1 < sortedQueries.size()) {
			 fastIntersect(sortedData.subList(index+1, sortedData.size()), sortedQueries.subList(medianQueryIndex+1, sortedQueries.size()), result);
		 }
	 }
	 
	 
	 //TODO This comparator appears to not be used. What is it,
	 // and why isn't it used if it's still here? Should it be
	 // deleted?
	 /*
	 private final Comparator<int[]> matchXComparator = new Comparator<int[]>() {
		 public int compare(int[] m1, int[] m2) {
			 
			 if (m1==null || m2==null || m1.length<1 || m2.length<1) throw new NullPointerException("Null and empty matches are not defined for this comparator");
			 
			 int sentence1 = getSentenceIndex(m1[0]);
			 int sentence2 = getSentenceIndex(m2[0]);
			 
			 if (sentence1 > sentence2) return 1;
			 else if (sentence1 < sentence2) return -1;
			 else {
				 if (m1[0] >= m2[0]-1) return 1;
				 
				 throw new RuntimeException("Unimplemented section: need to check for MAX_PHRASE_SPAN");
				//TODO UNCOMMENT THE FOLLOWING 2 LINES
				 //else if (m1[0] <= m2[0]-MAX_PHRASE_SPAN) return -1;
				 //else return 0;
			 }
		 } 
	 };
	 */
	
	
	/**
	 * Builds a HashMap of all the occurrences of the phrase,
	 * keying them based on the index of the sentence that they
	 * occur in. Since we iterate over all occurrences of the
	 * phrase, this method is linear with respect to the number
	 * of occurrences, and should not be used for very frequent
	 * phrases. This is part of the baseline method described
	 * in Section 4.1 of Adam Lopez's EMNLP paper.
	 */
	public HashMap<Integer,HashSet<Integer>> keyPositionsWithSentenceNumber(Phrase phrase) {
		// keys are the sentence numbers of partial matches
		HashMap<Integer,HashSet<Integer>> positionsKeyedWithSentenceNumber = new HashMap<Integer,HashSet<Integer>>(size());
		int[] bounds = findPhrase(phrase);
		if (bounds == null) return positionsKeyedWithSentenceNumber;
		
		int[] positions = getAllPositions(bounds);
		for (int i = 0; i < positions.length; i++) {
			int sentenceNumber = getSentenceIndex(positions[i]);
			HashSet<Integer> positionsInSentence = positionsKeyedWithSentenceNumber.get(sentenceNumber);
			if (positionsInSentence == null) {
				positionsInSentence = new HashSet<Integer>();
			}
			positionsInSentence.add(positions[i]);
			positionsKeyedWithSentenceNumber.put(sentenceNumber, positionsInSentence);
		}
		return positionsKeyedWithSentenceNumber;
	}

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

	/**
	 * Constructs an auxiliary array that stores longest common
	 * prefixes. The length of the array is the corpus size+1.
	 * Each elements lcp[i] indicates the length of the common
	 * prefix between two positions s[i-1] and s[i] in the
	 * suffix array.
	 */
	protected int[] calculateLongestCommonPrefixes() {
		int[] longestCommonPrefixes = new int[suffixes.length +1];
		for (int i = 1; i < suffixes.length; i++) {
			int commonPrefixSize = 0;
			while(suffixes[i]+commonPrefixSize < size() && suffixes[i-1]+ commonPrefixSize< size() &&
				  (corpus.getWordID(suffixes[i]  + commonPrefixSize) == corpus.getWordID(suffixes[i-1]+ commonPrefixSize)
				  && commonPrefixSize <= MAX_COMPARISON_LENGTH)) {
				commonPrefixSize++;
			}
			longestCommonPrefixes[i] = commonPrefixSize;
		}
		longestCommonPrefixes[0] = 0;
		longestCommonPrefixes[suffixes.length] = 0;
		return longestCommonPrefixes;
	}
	
	
	/**
	 * This method extracts phrases which reach the specified
	 * minimum frequency. It uses the equivalency classes for
	 * substrings in the interval i-j in the suffix array, as
	 * defined in section 2.3 of the the Yamamoto and Church
	 * CL article. This is a helper function for the
	 * getMostFrequentPhrases method.
	 */
	protected void recordPhraseFrequencies(
		int[]               longestCommonPrefixes,
		int                 i,
		int                 j,
		int                 k,
		List<Phrase>        phrases,
		List<Integer>       frequencies,
		int                 minFrequency,
		int                 maxPhrases,
		int                 maxPhraseLength,
		Comparator<Integer> comparator
	) {
		int longestBoundingLCP = Math.max(longestCommonPrefixes[i], longestCommonPrefixes[j+1]); //(longestCommonPrefixes[i] > longestCommonPrefixes[j+1]) ? longestCommonPrefixes[i] : longestCommonPrefixes[j+1];//
		int shortestInteriorLCP = longestCommonPrefixes[k];
		if(shortestInteriorLCP == 0) {
			shortestInteriorLCP = size() - suffixes[i];
		}
		
		int frequency = 0;
		if (i == j) {
			frequency = 1;
		} else if (longestBoundingLCP < shortestInteriorLCP) {
			frequency = j-i+1;
		}
		
		// increment the phrase frequencies, if we're above
		// the frequency threshold...
		if (frequency >= minFrequency) {
			int position = Collections.binarySearch(frequencies, frequency, comparator);
			if (position < 0) {
				position = -1 * position;
				position--;
			}
			
			
			// only increment if we've not already filled out the phrases
			// with phrases with higher frequencies...
			if (position < maxPhrases) {
				int startIndex = suffixes[i];
				int sentenceNumber = getSentenceIndex(startIndex);
				int endOfSentence = getSentencePosition(sentenceNumber+1);
				int distanceToEndOfSentence = endOfSentence-startIndex;
				int maxLength = Math.min(shortestInteriorLCP-1, distanceToEndOfSentence); //(shortestInteriorLCP-1 < distanceToEndOfSentence) ? shortestInteriorLCP-1 : distanceToEndOfSentence;//
				maxLength = Math.min(maxLength, maxPhraseLength); //(maxLength<maxPhraseLength) ? maxLength : maxPhraseLength; //
				
				// ccb - should this be < maxLength or <= maxLength
				for(int length = longestBoundingLCP; length <= maxLength; length++) {
					int endIndex = startIndex + length+1;
					Phrase phrase = new ContiguousPhrase(startIndex, endIndex, corpus);
					phrases.add(position, phrase);
					frequencies.add(position, frequency);
					position++;
				}
			}
		}
	}
	
	
	/**
	 * Finds a phrase in the suffix array. The phrase is extracted
	 * from the sentence given the start and end points.
	 *
	 * @param sentence    the sentence/superphrase to draw the
	 *                    search phrase from
	 * @param phraseStart the start of the phrase in the sentence
	 *                    (inclusive)
	 * @param phraseEnd   the end of the phrase in the sentence
	 *                    (exclusive)
	 * @return a tuple containing the (inclusive) start and the (inclusive) end bounds
	 *         in the suffix array for the phrase
	 */
	protected int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd) {
		return findPhrase(sentence, phraseStart, phraseEnd, 0, suffixes.length-1);
	}
	
	
	/**
	 * Finds a phrase in the suffix array. The phrase is extracted
	 * from the sentence given the start and end points. This
	 * version of the method allows bounds to be specified in
	 * the suffix array, which is useful when searching for
	 * increasingly longer subrphases in a sentences.
	 *
	 * @param sentence    the sentence/superphrase to draw the
	 *                    search phrase from
	 * @param phraseStart the start of the phrase in the sentence
	 *                    (inclusive)
	 * @param phraseEnd   the end of the phrase in the sentence
	 *                    (exclusive)
	 * @param lowerBound  the first index in the suffix array
	 *                    that will bound the search
	 * @param upperBound  the last index in the suffix array
	 *                    that will bound the search
	 * @return a tuple containing the (inclusive) start and the (inclusive) end bounds 
	 *         in the suffix array for the phrase, or null if
	 *         the phrase is not found.
	 */
	protected int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd, int lowerBound, int upperBound) {
		int[] bounds = new int[2];
		lowerBound = findPhraseBound(sentence, phraseStart, phraseEnd, lowerBound, upperBound, true);
		if (lowerBound < 0) return null;
		upperBound = findPhraseBound(sentence, phraseStart, phraseEnd, lowerBound, upperBound, false);
		bounds[0]=lowerBound;
		bounds[1]=upperBound;
		return bounds;	
	}
	
	
	
// ccb - debugging -- writing a method to find all subphrases within
// a sentence, using the "bounds" trick that recognizes that all
// phrases starting with the same sequence will be found within a
// particular bounds.
	/*
	public HashMap findAllSubphrases(Phrase sentence) {
		HashMap phraseToBoundsMap = new HashMap();
		int[] wordIDs = sentence.getWordIDs();
		for(int phraseStart = 0; phraseStart < wordIDs.length; phraseStart++) {
			// the one word case...
			int phraseEnd = phraseStart+1;
			Phrase subphrase = sentence.subPhrase(phraseStart, phraseEnd);
			// note that the bounds may be null if the word doesn't occur in the corpus
			int[] bounds = findPhrase(wordIDs, phraseStart, phraseEnd, 0, suffixes.length-1);
			phraseToBoundsMap.put(subphrase, bounds);

			// multi-word phrases...
			for(phraseEnd = phraseStart+2; phraseEnd <= wordIDs.length && bounds != null; phraseEnd++) {
				// incrementally longer subphrases occur in the suffix array within
				//  the bounds of the previous subphrase, giving improved efficiency 
				int lowerBound = bounds[0];
				int upperBound = bounds[1];
				bounds = findPhrase(wordIDs, phraseStart, phraseEnd, lowerBound, upperBound);
				if(bounds != null) {
					subphrase = sentence.subPhrase(phraseStart, phraseEnd);
					phraseToBoundsMap.put(subphrase, bounds);
				}
			} 
		}
		return phraseToBoundsMap;
	}
*/

	
	
	/** 
	 * Sorts the initalized, unsorted suffixes. Uses quick sort
	 * and the compareSuffixes method defined in CorpusArray.
	 */ 
    protected void sort(int[] suffixes) {
        qsort(suffixes, 0, suffixes.length - 1);
    }
	
	
	/**
	 * Creates a string of the semi-infinite strings in the
	 * corpus array. Only use this on small suffixArrays!
	 */
	public String toString() {
		String str = "";
		for(int i = 0; i < suffixes.length; i++) {
			Phrase phrase = corpus.getPhrase(getCorpusIndex(i), corpus.size());
			str += phrase.toString() + "\n";
		}
		return str;
	}
	
//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================


	/** part of the quick sort implementation. */
    /*
	private void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    */
	
	
	/** part of the quick sort implementation. */	
    /*
	private int partition(int[] array, int begin, int end) {
        int index = begin + RAND.nextInt(end - begin + 1);
        int pivot = array[index];
        
        // swap(array, index, end);
        {
        	int tmp = array[index];
        	array[index] = array[end];
        	array[end] = tmp;
        }
        
        for (int i = index = begin; i < end; ++ i) {
            if (corpus.compareSuffixes(array[i], pivot, MAX_COMPARISON_LENGTH) <= 0) {
                
            	//swap(array, index++, i);
                {
                	int tmp = array[index];
                	array[index] = array[i];
                	array[i] = tmp;
                	index++;
                }
            }
        }
        // swap(array, index, end);
        {
        	int tmp = array[index];
        	array[index] = array[end];
        	array[end] = tmp;
        }
        
        
        return (index);
    }
	*/
	
	/** Quick sort */	
    private void qsort(int[] array, int begin, int end) {
        if (end > begin) {
        	
            int index; 
            // partition(array, begin, end);
            {	index = begin + RAND.nextInt(end - begin + 1);
                int pivot = array[index];
                
                // swap(array, index, end);
                {
                	int tmp = array[index];
                	array[index] = array[end];
                	array[end] = tmp;
                }
                
                for (int i = index = begin; i < end; ++ i) {
                    if (corpus.compareSuffixes(array[i], pivot, MAX_COMPARISON_LENGTH) <= 0) {
                        
                    	//swap(array, index++, i);
                        {
                        	int tmp = array[index];
                        	array[index] = array[i];
                        	array[i] = tmp;
                        	index++;
                        }
                    }
                }
                // swap(array, index, end);
                {
                	int tmp = array[index];
                	array[index] = array[end];
                	array[end] = tmp;
                }
            }
            
            qsort(array, begin, index - 1);
            qsort(array, index + 1,  end);
        }
    }
	
	
	/**
	 * Finds the first or last occurrence of a phrase in the
	 * suffix array, within a subset of the suffix array that
	 * is bounded by suffixArrayStart and suffixArrayEnd. For
	 * efficiency of looking up all subphrases in a sentence
	 * we do not require that multplie int[]s be created for
	 * each subphrase. Instead this method will look for the
	 * subphrase within the sentence between phraseStart and
	 * phraseEnd.
	 *
	 * @param sentence         the sentence/superphrase in int
	 *                         representation to draw the search
	 *                         phrase from
	 * @param phraseStart      the start of the phrase in the
	 *                         sentence (inclusive)
	 * @param phraseEnd        the end of the phrase in the
	 *                         sentence (exclusive)
	 * @param suffixArrayStart the point at which to start the
	 *                         search in the suffix array
	 * @param suffixArrayEnd   the end point in the suffix array
	 *                         beyond which the search doesn't
	 *                         need to take place
	 * @param findFirst        a flag that indicates whether
	 *                         we should find the first or last
	 *                         occurrence of the phrase
	 */
	private int findPhraseBound(
		Phrase  sentence,
		int     phraseStart,
		int     phraseEnd,
		int     suffixArrayStart,
		int     suffixArrayEnd,
		boolean findFirst
	) {
		int low = suffixArrayStart;
		int high = suffixArrayEnd;
		
		// Do a binary search between the low and high points
		while (low <= high) {
			int mid = (low + high) >> 1;
			int start = suffixes[mid];
			int diff = corpus.comparePhrase(start, sentence, phraseStart, phraseEnd);
			if (diff == 0) {
				// If the difference between the search phrase and the phrase in the corpus 
				// is 0, then we have found it.  However, there might be multiple matches in
				// the corpus, so we need to continue searching until we find the end point
				int neighbor = mid;
				if (findFirst) {
					neighbor--;
				} else {
					neighbor++;
				}
				if (neighbor >= suffixArrayStart && neighbor <= suffixArrayEnd) {
					int nextDiff = corpus.comparePhrase(suffixes[neighbor], sentence, phraseStart, phraseEnd);
					if (nextDiff == 0) {
						// There's another equivalent phrase, so we need to specify 
						// in which direction to continue searching
						if (findFirst) diff = 1; //search lower
						else diff = -1; //search higher
					}
				}
			}
			if (diff < 0) {
				low = mid + 1;
			} else if (diff > 0) {
				high = mid - 1;
			} else {
				return mid; //this is the edge
			}
		}
		return -1; // key not found.
	}
	
//===============================================================
// Static
//===============================================================



//===============================================================
// Inner classes
//===============================================================

	/**
	 * This inner-class is a wrapper for a complex data type,
	 * which maps Phrase->Phrase->a list of tuples containing
	 * the starting positions of the two phrases in the corpus.
	 */
	protected class Collocations extends HashMap<Phrase,HashMap<Phrase,ArrayList<int[]>>> {
	
		/**
		 * Adds a collocated pair of phrases to this
		 * container, along with their respective positions
		 * in the corpus.
		 */
		protected void add(Phrase phrase1, Phrase phrase2, int position1, int position2) {
			// check to make sure that the phrase2 isn't simply a subphrase of phrase1
			if(position2-position1 >= phrase1.size()) {
				int[] position = new int[2];
				position[0] = position1;
				position[1] = position2;
				
				// use the first phrase as a key
				HashMap<Phrase,ArrayList<int[]>> phrase2ToPositionsMap = this.get(phrase1);
				// if we don't have any previous instances of the first phrase, 
				// then initialize a new map for it...
				if(phrase2ToPositionsMap == null) {
					phrase2ToPositionsMap = new HashMap<Phrase,ArrayList<int[]>>();
				}
				
				// use the second phrase as a key
				ArrayList<int[]> positions = phrase2ToPositionsMap.get(phrase2);
				// if we don't have any instances of the second phrase collocating with first phrase,
				// then initialize a new list of positions for it...
				if(positions == null) {
					positions = new ArrayList<int[]>();
				}
				
				// add everything to their respective containers
				positions.add(position);
				phrase2ToPositionsMap.put(phrase2, positions);
				this.put(phrase1, phrase2ToPositionsMap);
			}
		}
		

		/**
		 * Gets the list of positions for a pair of phrases
		 */
		protected ArrayList<int[]> getColocations(Phrase phrase1, Phrase phrase2) {
			// use the first phrase as a key
			HashMap<Phrase,ArrayList<int[]>> phrase2ToPositionsMap = this.get(phrase1);

			// if we don't have any instances of the first phrase, return an empty list...
			if(phrase2ToPositionsMap == null) return new ArrayList<int[]>();
			
			// use the second phrase as a key
			ArrayList<int[]> positions = phrase2ToPositionsMap.get(phrase2);
			if(positions == null) {
				// if we don't have any collocations for the pair of phrases, return an empty list...
				return new ArrayList<int[]>();
			} else {
				return positions;
			}
		}
		
		public String toString() {
			String str = "";
			Iterator<Phrase> it = keySet().iterator();
			while(it.hasNext()) {
				Phrase phrase1 = it.next();
				HashMap<Phrase,ArrayList<int[]>> phrase2ToPositionsMap = this.get(phrase1);
				Iterator<Phrase> jt = phrase2ToPositionsMap.keySet().iterator();
				while(jt.hasNext()) {
					Phrase phrase2 = jt.next();
					ArrayList<int[]> positions = phrase2ToPositionsMap.get(phrase2);
					str += phrase1 + "\t" + phrase2 + "\t(" + positions.size() + ")\n";
				}
			}
			return str;
		}
	}



//===============================================================
// Main 
//===============================================================

/*
	public static void main2(String[] args) throws IOException {
		if (args.length != 6) {
			System.out.println("Usage: java SuffixArray lang corpusName dir minFrequency maxPhrasesToRetain maxPhraseLength");
			System.exit(0);
		}
		String lang = args[0];
		String corpusName = args[1];
		String directory = args[2];
		int minFrequency = Integer.parseInt(args[3]);
		int maxPhrasesToRetain = Integer.parseInt(args[4]);
		int maxPhraseLength = Integer.parseInt(args[5]);

		
		SuffixArray suffixArray = SuffixArrayFactory.loadSuffixArray(lang, corpusName, directory);
		//ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		ArrayList<Integer> frequencies = new ArrayList<Integer>();
		List<Phrase> phrases = suffixArray.getMostFrequentPhrases(frequencies, minFrequency, maxPhrasesToRetain, maxPhraseLength);
			
		System.out.println("NUM PHRASES: " + phrases.size());
		System.out.println("FREQ\tPHRASE");
		for(int i = 0; i < phrases.size(); i++) {
			System.out.println(frequencies.get(i) + "\t" + phrases.get(i));
		}
		
		Collocations collocations = suffixArray.getCollocations(new HashSet<Phrase>(phrases), maxPhraseLength, 10);
		System.out.println(collocations);
	}
	*/
	
	/**
	 * This method tests out the suffix array using the example
	 * sentence given in the Yamamoto and Church CL article.
	 */
	/*
	public static void mainChris(String[] args) throws IOException {
		// this method creates a sample corpus ...
		//String corpusString = "t o _ b e _ o r _ n o t _ t o _ b e";
		//String corpusString = "to be or not to be";
		
		// Adam Lopez's example...
		String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";
		
		Vocabulary vocab = new Vocabulary();
		Phrase exampleSentence = new BasicPhrase(corpusString, vocab);
		vocab.alphabetize();
		vocab.fixVocabulary();
		
		int[] sentences = new int[1];
		sentences[0] = 0;
		int[] corpus = new int[exampleSentence.size()];
		for(int i = 0; i < exampleSentence.size(); i++) {
			corpus[i] = exampleSentence.getWordID(i);
		}
		
		CorpusArray corpusArray = new CorpusArray(corpus, sentences, vocab);
		SuffixArray suffixArray = new SuffixArray(corpusArray);
		int[] lcpArray = suffixArray.calculateLongestCommonPrefixes();
		
		System.out.println("I\tS[I]\tLCP\tSUFFIX");
		for(int i = 0; i < suffixArray.size(); i++) {
			Phrase phrase = new ContiguousPhrase(suffixArray.suffixes[i], suffixArray.size(), suffixArray.corpus);
			System.out.println(i + "\t" + suffixArray.suffixes[i] + "\t" + lcpArray[i] + "\t"+ phrase);
		}
		System.out.println();
		
		//ArrayList<Phrase> phrases = new ArrayList<Phrase>();
		ArrayList<Integer> frequencies = new ArrayList<Integer>();
		int minFrequency = 1;
		int maxPhrasesToRetain = 100;
		int maxPhraseLength = 100;
		List<Phrase> phrases = suffixArray.getMostFrequentPhrases(frequencies, minFrequency, maxPhrasesToRetain, maxPhraseLength);
		
		System.out.println("Frequency\tphrase");
		for(int i = 0; i < phrases.size(); i++) {
			System.out.println(frequencies.get(i) + "\t" + phrases.get(i));
		}
		System.out.println();
		
		
		System.out.println("Collocations");
		Collocations collocations = suffixArray.getCollocations(new HashSet<Phrase>(phrases), maxPhraseLength, 100);
		System.out.println(collocations);	
		
		Phrase phrase1 = new BasicPhrase("him", vocab);
		Phrase phrase2 = new BasicPhrase("it", vocab);
		
		int[] positions1 = suffixArray.getAllPositions(suffixArray.findPhrase(phrase1));
		int[] positions2 = suffixArray.getAllPositions(suffixArray.findPhrase(phrase2));
		
		System.out.print(phrase1 + " occurred at positions: ");
		for(int i = 0; i < positions1.length; i++) {
			System.out.print(positions1[i] + " ");
		}
		System.out.println();
		
		System.out.print(phrase2 + " occurred at positions: ");
		for(int i = 0; i < positions2.length; i++) {
			System.out.print(positions2[i] + " ");
		}
		System.out.println();
	}
	*/
	
	public static void main(String[] args) {
		fastIntersection();
		
		
	}

}

