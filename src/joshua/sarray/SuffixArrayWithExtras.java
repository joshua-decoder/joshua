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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.util.Cache;
import joshua.util.Pair;
import joshua.util.ReverseOrder;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;

/**
 * Extra methods related to suffix array grammar extraction.
 * 
 * @author Chris Callison-Burch
 * @author Lane Schwartz
 */
public class SuffixArrayWithExtras extends SuffixArray {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(SuffixArrayWithExtras.class.getName());
	
	public SuffixArrayWithExtras(Corpus corpus, int maxCacheSize) {
		super(corpus, maxCacheSize);
	}
	
	/**
	 * Calculates the most frequent phrases in the corpus.
	 * Populates the phrases list with them, and the frequencies
	 * list with their frequencies.  Allows a threshold to be
	 * set for the minimum frequency to remember, as well as
	 * the maximum number of phrases.
	 * 
	 * TODO Write unit tests for this method
	 *
	 * @param frequencies  a list to return of the phrases frequencies
	 * @param minFrequency the minimum frequency required to
	 *                     retain phrases
	 * @param maxPhrases   the maximum number of phrases to return
	 * @return the most frequent phrases
	 */
	public Pair<List<Phrase>,List<Integer>> getMostFrequentPhrases(
		int minFrequency,
		int maxPhrases,
		int maxPhraseLength
	) {

		List<Integer> frequencies = new ArrayList<Integer>();
		List<Phrase> phrases = getMostFrequentPhrases(new ArrayList<Phrase>(), frequencies, minFrequency, maxPhrases, maxPhraseLength);
		return new Pair<List<Phrase>,List<Integer>>(phrases, frequencies);
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
	
	public static class FrequentPhrase {
		public final Phrase phrase;
		public final int frequency;
		
		public FrequentPhrase(Phrase phrase, int frequency) {
			this.phrase = phrase;
			this.frequency = frequency;
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		
		if (args.length < 1) {
			System.err.println("Usage: java " + SuffixArray.class.getName() + " source.vocab source.corpus source.suffixes");
			System.exit(0);
		}
		
		String corpusFileName = args[0];
		
		logger.info("Constructing vocabulary from file " + corpusFileName);
		Vocabulary symbolTable = new Vocabulary();
		int[] lengths = SuffixArrayFactory.createVocabulary(corpusFileName, symbolTable);
		
		logger.info("Constructing corpus array from file " + corpusFileName);
		CorpusArray corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);
		
		logger.info("Constructing suffix array from file " + corpusFileName);
		SuffixArrayWithExtras suffixArray = new SuffixArrayWithExtras(corpusArray, Cache.DEFAULT_CAPACITY);

		int minFrequency = 0;
		int maxPhrases = 100;
		int maxPhraseLength = 10;
		
		Pair<List<Phrase>,List<Integer>> list = 
			suffixArray.getMostFrequentPhrases(minFrequency, maxPhrases, maxPhraseLength);
		
		List<Phrase> phrases = list.first;
//		List<>
//		
//		for (Pair<List<Phrase>,List<Integer>> pair : list) {
//			
//			System.out.println(phrase);
//		}
		
	}
	
}
