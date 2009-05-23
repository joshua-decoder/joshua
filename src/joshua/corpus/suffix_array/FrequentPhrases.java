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
package joshua.corpus.suffix_array;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.ContiguousPhrase;
import joshua.corpus.Corpus;
import joshua.corpus.Phrase;
import joshua.corpus.mm.MemoryMappedCorpusArray;
import joshua.corpus.suffix_array.mm.MemoryMappedSuffixArray;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.Cache;
import joshua.util.ReverseOrder;
import joshua.util.io.BinaryIn;

/**
 * Represents the most frequent phrases in a corpus.
 * 
 * @author Chris Callison-Burch
 * @author Lane Schwartz
 */
public class FrequentPhrases {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(FrequentPhrases.class.getName());
	
	/** Suffix array in which frequent phrases are located. */
	private final Suffixes suffixes;
	
	/** 
	 * Stores the number of times a phrase occurred in the
	 * corpus.
	 * <p>
	 * The iteration order of this map should start with the
	 * most frequent phrase and end with the least frequent
	 * phrase stored in the map.
	 * <p>
	 * The key set for this map should be identical to the key
	 * set in the <code>ranks</code> map.
	 */
	private final LinkedHashMap<Phrase,Integer> frequentPhrases;
	
	/** Maximum number of phrases of which this object is aware. */
	private final short maxPhrases;
	
	
	/**
	 * Constructs data regarding the frequencies of the <em>n</em>
	 * most frequent phrases found in the corpus backed by the
	 * provided suffix array.
	 * 
	 * @param suffixes   Suffix array corresponding to a corpus.
	 * @param minFrequency The minimum frequency required to
	 *                   for a phrase to be considered frequent.
	 * @param maxPhrases The number of phrases to consider.
	 * @param maxPhraseLength Maximum phrase length to consider.
	 */
	public FrequentPhrases(
			Suffixes suffixes,
			int minFrequency,
			short maxPhrases,
			int maxPhraseLength) {
		
		this.maxPhrases = maxPhrases;
		
		this.suffixes = suffixes;
		this.frequentPhrases = getMostFrequentPhrases(suffixes, minFrequency, maxPhrases, maxPhraseLength);
		
	}


	/**
	 * This method performs a one-pass computation of the
	 * collocation of two frequent subphrases. It is used for
	 * the precalculation of the translations of hierarchical
	 * phrases which are problematic to calculate on the fly.
	 * This procedure is described in "Hierarchical Phrase-Based
	 * Translation with Suffix Arrays" by Adam Lopez.
	 *
	 * @param maxPhraseLength the maximum length of any phrase
	 *                   in the phrases
	 * @param windowSize the maximum allowable space between
	 *                   phrases for them to still be considered
	 *                   collocated
	 * @param minNonterminalSpan Minimum span allowed for a nonterminal 
	 */
	public FrequentMatches getCollocations(
			int maxPhraseLength,
			int windowSize,
			short minNonterminalSpan
	) {

		logger.fine("Calculating number of frequent collocations");
		int totalCollocations = countCollocations(maxPhraseLength, windowSize);
		logger.fine("Total collocations: " + totalCollocations);


		// Get an initially empty collocations object
		FrequentMatches collocations =
			new FrequentMatches(
					getRanks(frequentPhrases),
					maxPhrases,
					totalCollocations,
					minNonterminalSpan);

		LinkedList<Phrase> phrasesInWindow = new LinkedList<Phrase>();
		LinkedList<Integer> positions = new LinkedList<Integer>();
		
		int sentenceNumber = 1;
		int endOfSentence = suffixes.getSentencePosition(sentenceNumber);

		if (logger.isLoggable(Level.FINER)) logger.finer("END OF SENT: " + sentenceNumber + " at position " + endOfSentence);

		Corpus corpus = suffixes.getCorpus();

		// Start at the beginning of the corpus...
		for (int currentPosition = 0, endOfCorpus=suffixes.size(); 
				// ...and iterate through the end of the corpus
				currentPosition < endOfCorpus; currentPosition++) {

			// Start with a phrase length of 1, at the current position...
			for (int i = 1, endOfPhrase = currentPosition + i; 
					// ...ensure the phrase length isn't too long...
					i < maxPhraseLength  &&  
					// ...and that the phrase doesn't extend past the end of the sentence...
					endOfPhrase <= endOfSentence  &&  
					// ...or past the end of the corpus
					endOfPhrase <= endOfCorpus; 
					// ...then increment the phrase length and end of phrase marker.
					i++, endOfPhrase = currentPosition + i) {

				// Get the current phrase
				Phrase phrase = new ContiguousPhrase(currentPosition, endOfPhrase, corpus);

				if (logger.isLoggable(Level.FINEST)) logger.finest("Found phrase (" +currentPosition + ","+endOfPhrase+") "  + phrase);

				// If the phrase is one we care about...
				if (frequentPhrases.containsKey(phrase)) {

					if (logger.isLoggable(Level.FINER)) logger.finer("\"" + phrase + "\" found at currentPosition " + currentPosition);

					// Remember the phrase...
					phrasesInWindow.add(phrase);

					// ...and its starting position
					positions.add(currentPosition);
				}

			} // end iterating over various phrase lengths


			// check whether we're at the end of the sentence and dequeue...
			if (currentPosition == endOfSentence) {

				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("REACHED END OF SENT: " + currentPosition);
					logger.finest("PHRASES:   " + phrasesInWindow);
					logger.finest("POSITIONS: " + positions);
				}

				// empty the whole queue...
				for (int i = 0, n=phrasesInWindow.size(); i < n; i++) {

					Phrase phrase1 = phrasesInWindow.remove();
					int position1 = positions.remove();

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = i+1; j < n; j++) {

						Phrase phrase2 = phraseIterator.next();//phrasesInWindow.get(j);
						int position2 = positionIterator.next();//positions.get(j);

						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE1: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
						collocations.add(phrase1, phrase2, position1, position2);

					}

				}
				// clear the queues
				phrasesInWindow.clear();
				positions.clear();

				// update the end of sentence marker
				sentenceNumber++;
				endOfSentence = suffixes.getSentencePosition(sentenceNumber)-1;

				if (logger.isLoggable(Level.FINER)) logger.finer("END OF SENT: " + sentenceNumber + " at position " + endOfSentence);

			} // Done processing end of sentence.


			// check whether the initial elements are
			// outside the window size...
			if (phrasesInWindow.size() > 0) {
				int position1 = positions.peek();//.get(0);
				// deque the first element and
				// calculate its collocations...
				while ((position1+windowSize < currentPosition)
						&& phrasesInWindow.size() > 0) {

					if (logger.isLoggable(Level.FINEST)) logger.finest("OUTSIDE OF WINDOW: " + position1 + " " +  currentPosition + " " + windowSize);
					
					Phrase phrase1 = phrasesInWindow.remove();
					positions.remove();

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = 0, n=phrasesInWindow.size(); j < n; j++) {

						Phrase phrase2 = phraseIterator.next();
						int position2 = positionIterator.next();

						collocations.add(phrase1, phrase2, position1, position2);
						
						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE2: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
					}
					if (phrasesInWindow.size() > 0) {
						position1 = positions.peek();
					} else {
						position1 = currentPosition;
					}
				}
			}

		} // end iterating over positions in the corpus

		if (logger.isLoggable(Level.FINE)) logger.fine("Sorting collocations");
		collocations.histogramSort();
		
		return collocations;
	}


	/**
	 * This method performs a one-pass computation of the
	 * collocation of two frequent subphrases. It is used for
	 * the precalculation of the translations of hierarchical
	 * phrases which are problematic to calculate on the fly.
	 * This procedure is described in "Hierarchical Phrase-Based
	 * Translation with Suffix Arrays" by Adam Lopez.
	 *
	 * @param maxPhraseLength the maximum length of any phrase
	 *                   in the phrases
	 * @param windowSize the maximum allowable space between
	 *                   phrases for them to still be considered
	 *                   collocated
	 */
	protected int countCollocations(
			int maxPhraseLength,
			int windowSize
	) {
		int count = 0;

		LinkedList<Phrase> phrasesInWindow = new LinkedList<Phrase>();
		LinkedList<Integer> positions = new LinkedList<Integer>();
		int sentenceNumber = 1;
		int endOfSentence = suffixes.getSentencePosition(sentenceNumber);

		// ccb - debugging
		if (logger.isLoggable(Level.FINEST)) logger.finest("END OF SENT: " + endOfSentence);

		Corpus corpus = suffixes.getCorpus();

		// Start at the beginning of the corpus...
		for (int currentPosition = 0, endOfCorpus=suffixes.size(); 
		// ...and iterate through the end of the corpus
		currentPosition < endOfCorpus; currentPosition++) {

			// Start with a phrase length of 1, at the current position...
			for (int i = 1, endOfPhrase = currentPosition + i; 
			// ...ensure the phrase length isn't too long...
			i < maxPhraseLength  &&  
			// ...and that the phrase doesn't extend past the end of the sentence...
			endOfPhrase <= endOfSentence  &&  
			// ...or past the end of the corpus
			endOfPhrase <= endOfCorpus; 
			// ...then increment the phrase length and end of phrase marker.
			i++, endOfPhrase = currentPosition + i) {

				// Get the current phrase
				Phrase phrase = new ContiguousPhrase(currentPosition, endOfPhrase, corpus);

				if (logger.isLoggable(Level.FINEST)) logger.finest("Found phrase (" +currentPosition + ","+endOfPhrase+") "  + phrase);

				// If the phrase is one we care about...
				if (frequentPhrases.containsKey(phrase)) {

					if (logger.isLoggable(Level.FINER)) logger.finer("\"" + phrase + "\" found at currentPosition " + currentPosition);

					// Remember the phrase...
					phrasesInWindow.add(phrase);

					// ...and its starting position
					positions.add(currentPosition);
				}

			} // end iterating over various phrase lengths


			// check whether we're at the end of the sentence and dequeue...
			if (currentPosition == endOfSentence) {

				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("REACHED END OF SENT: " + currentPosition);
					logger.finest("PHRASES:   " + phrasesInWindow);
					logger.finest("POSITIONS: " + positions);
				}

				// empty the whole queue...
				for (int i = 0, n=phrasesInWindow.size(); i < n; i++) {

					Phrase phrase1 = phrasesInWindow.removeFirst();
					int position1 = positions.removeFirst();

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = i+1; j < n; j++) {

						Phrase phrase2 = phraseIterator.next();
						int position2 = positionIterator.next();

						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE1: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
						count++;

					}

				}
				// clear the queues
				phrasesInWindow.clear();
				positions.clear();

				// update the end of sentence marker
				sentenceNumber++;
				endOfSentence = suffixes.getSentencePosition(sentenceNumber)-1;

				if (logger.isLoggable(Level.FINER)) logger.finer("END OF SENT: " + sentenceNumber + " at position " + endOfSentence);

			} // Done processing end of sentence.


			// check whether the initial elements are
			// outside the window size...
			if (phrasesInWindow.size() > 0) {
				int position1 = positions.get(0);
				// deque the first element and
				// calculate its collocations...
				while ((position1+windowSize < currentPosition)
						&& phrasesInWindow.size() > 0) {

					if (logger.isLoggable(Level.FINEST)) logger.finest("OUTSIDE OF WINDOW: " + position1 + " " +  currentPosition + " " + windowSize);
					
					Phrase phrase1 = phrasesInWindow.removeFirst();
					positions.removeFirst();

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = 0, n=phrasesInWindow.size(); j < n; j++) {

						Phrase phrase2 = phraseIterator.next();
						int position2 = positionIterator.next();

						count++;

						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE2: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
					}
					if (phrasesInWindow.size() > 0) {
						position1 = positions.getFirst();
					} else {
						position1 = currentPosition;
					}
				}
			}

		} // end iterating over positions in the corpus

		return count;
	}



	//	/**
	//	 * Builds a HashMap of all the occurrences of the phrase,
	//	 * keying them based on the index of the sentence that they
	//	 * occur in. Since we iterate over all occurrences of the
	//	 * phrase, this method is linear with respect to the number
	//	 * of occurrences, and should not be used for very frequent
	//	 * phrases. This is part of the baseline method described
	//	 * in Section 4.1 of Adam Lopez's EMNLP paper.
	//	 */
	//	public HashMap<Integer,HashSet<Integer>> keyPositionsWithSentenceNumber(Phrase phrase) {
	//		// keys are the sentence numbers of partial matches
	//		HashMap<Integer,HashSet<Integer>> positionsKeyedWithSentenceNumber = new HashMap<Integer,HashSet<Integer>>(suffixes.size());
	//		int[] bounds = suffixes.findPhrase(phrase);
	//		if (bounds == null) return positionsKeyedWithSentenceNumber;
	//		
	//		int[] positions = suffixes.getAllPositions(bounds);
	//		for (int i = 0; i < positions.length; i++) {
	//			int sentenceNumber = suffixes.getSentenceIndex(positions[i]);
	//			HashSet<Integer> positionsInSentence = positionsKeyedWithSentenceNumber.get(sentenceNumber);
	//			if (positionsInSentence == null) {
	//				positionsInSentence = new HashSet<Integer>();
	//			}
	//			positionsInSentence.add(positions[i]);
	//			positionsKeyedWithSentenceNumber.put(sentenceNumber, positionsInSentence);
	//		}
	//		return positionsKeyedWithSentenceNumber;
	//	}

	//===============================================================
	// Protected 
	//===============================================================

	//===============================================================
	// Methods
	//===============================================================

	/**
	 * Calculates the frequency ranks of the provided phrases.
	 * <p>
	 * The iteration order of the <code>frequentPhrases</code>
	 * parameter is be used by this method to determine the
	 * rank of each phrase. Specifically, the first phrase
	 * returned by the map's iterator is taken to be the most
	 * frequent phrase; the last phrase returned by the map's
	 * iterator is taken to be the least frequent phrase.
	 * 
	 * @param frequentPhrases Map from phrase to frequency of
	 *                        that phrase in a corpus.
	 * @return the frequency ranks of the provided phrases
	 */
	protected LinkedHashMap<Phrase,Short> getRanks(LinkedHashMap<Phrase,Integer> frequentPhrases) {
		
		logger.fine("Calculating ranks of frequent phrases");
		
		LinkedHashMap<Phrase,Short> ranks = new LinkedHashMap<Phrase,Short>(frequentPhrases.size());

		short i=0;
		for (Phrase phrase : frequentPhrases.keySet()) {
			ranks.put(phrase, i++);
		}
		
		logger.fine("Done calculating ranks");
		
		return ranks;
	}
	
	/**
	 * Calculates the most frequent phrases in the corpus.
	 * <p>
	 * Allows a threshold to be set for the minimum frequency
	 * to remember, as well as the maximum number of phrases.
	 *
	 * @param suffixes     a suffix array for the corpus
	 * @param minFrequency the minimum frequency required to
	 *                     retain phrases
	 * @param maxPhrases   the maximum number of phrases to
	 *                     return
	 * @param maxPhraseLength the maximum phrase length to
	 *                     consider
	 * 
	 * @return A map from phrase to the number of times that phrase occurred in the corpus. The iteration order of the map will start with the most frequent phrase, and end with the least frequent calculated phrase.
	 */
	protected LinkedHashMap<Phrase,Integer> getMostFrequentPhrases(
			Suffixes suffixes,
			int minFrequency,
			int maxPhrases,
			int maxPhraseLength
	) {


		LinkedList<Phrase> phrases = new LinkedList<Phrase>();
		LinkedList<Integer> frequencies = new LinkedList<Integer>();

		phrases.clear();
		frequencies.clear();
		Comparator<Integer> comparator = new ReverseOrder<Integer>();

		// calculate the longest common prefix delimited intervals...
		// This is taken from the Yamamoto and Church Compuational Linguistics article.
		int[] longestCommonPrefixes = calculateLongestCommonPrefixes(suffixes);
		Stack<Integer> startIndices = new Stack<Integer>();
		Stack<Integer> shortestInteriorLCPIndices = new Stack<Integer>();
		startIndices.push(0);
		shortestInteriorLCPIndices.push(0);
		for (int j = 0, size=suffixes.size(); j < size; j++) {			
			// trivial interval i==j, frequecy=1
			recordPhraseFrequencies(suffixes,
					longestCommonPrefixes, j, j, 0, phrases, frequencies,
					minFrequency, maxPhrases, maxPhraseLength, comparator);
			while (longestCommonPrefixes[j+1] < longestCommonPrefixes[shortestInteriorLCPIndices.peek()]) {
				// non-trivial interval 
				recordPhraseFrequencies(suffixes, longestCommonPrefixes, startIndices.pop(), j, shortestInteriorLCPIndices.pop(),
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
					phrases.removeLast();
					frequencies.removeLast();
					//					phrases.remove(i);
					//					frequencies.remove(i);
				}
			}
		}

		LinkedHashMap<Phrase,Integer> results = new LinkedHashMap<Phrase,Integer>();

		for (int i=phrases.size(); i>0; i--) {
			Phrase phrase = phrases.removeFirst();
			Integer frequency = frequencies.removeFirst();
			results.put(phrase, frequency);
		}

		return results;
	}

	/**
	 * Constructs an auxiliary array that stores longest common
	 * prefixes. The length of the array is the corpus size+1.
	 * Each elements lcp[i] indicates the length of the common
	 * prefix between two positions s[i-1] and s[i] in the
	 * suffix array.
	 */
	protected static int[] calculateLongestCommonPrefixes(Suffixes suffixes) {

		int length = suffixes.size();
		Corpus corpus = suffixes.getCorpus();

		int[] longestCommonPrefixes = new int[length +1];
		for (int i = 1; i < length; i++) {
			int commonPrefixSize = 0;
			int corpusIndex = suffixes.getCorpusIndex(i);
			int prevCorpusIndex = suffixes.getCorpusIndex(i-1);

			while(corpusIndex+commonPrefixSize < length && 
					prevCorpusIndex + commonPrefixSize < length &&
					(corpus.getWordID(corpusIndex  + commonPrefixSize) == 
						corpus.getWordID(prevCorpusIndex + commonPrefixSize) && 
						commonPrefixSize <= Suffixes.MAX_COMPARISON_LENGTH)) {
				commonPrefixSize++;
			}
			longestCommonPrefixes[i] = commonPrefixSize;
		}
		longestCommonPrefixes[0] = 0;
		longestCommonPrefixes[length] = 0;
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
	protected static void recordPhraseFrequencies(
			Suffixes            suffixes,
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
		Corpus corpus = suffixes.getCorpus();

		// Math.max is slow when called a lot - use an ternary if..then instead
		int longestBoundingLCP = (longestCommonPrefixes[i] > longestCommonPrefixes[j+1]) ? longestCommonPrefixes[i] : longestCommonPrefixes[j+1];// Math.max(longestCommonPrefixes[i], longestCommonPrefixes[j+1])
		int shortestInteriorLCP = longestCommonPrefixes[k];
		if(shortestInteriorLCP == 0) {
			shortestInteriorLCP = suffixes.size() - suffixes.getCorpusIndex(i);
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
				int startIndex = suffixes.getCorpusIndex(i);
				int sentenceNumber = suffixes.getSentenceIndex(startIndex);
				int endOfSentence = suffixes.getSentencePosition(sentenceNumber+1);
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







	public String toString() {

		String format = null;

		StringBuilder s = new StringBuilder();

		for (Map.Entry<Phrase, Integer> entry : frequentPhrases.entrySet()) {

			Phrase phrase = entry.getKey();
			Integer frequency = entry.getValue();

			if (format==null) {
				int length = frequency.toString().length();
				format = "%1$" + length + "d";
			}

			s.append(String.format(format, frequency));
			s.append('\t');
			s.append(phrase.toString());
			s.append('\n');

		}

		return s.toString();
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



	//===============================================================
	// Static
	//===============================================================



	//===============================================================
	// Inner classes
	//===============================================================

	

	//===============================================================
	// Main method
	//===============================================================
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {


		Vocabulary symbolTable;
		Corpus corpusArray;
		Suffixes suffixArray;
		FrequentPhrases frequentPhrases;

		if (args.length == 1) {

			String corpusFileName = args[0];

			logger.info("Constructing vocabulary from file " + corpusFileName);
			symbolTable = new Vocabulary();
			int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);

			logger.info("Constructing corpus array from file " + corpusFileName);
			corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);

			logger.info("Constructing suffix array from file " + corpusFileName);
			suffixArray = new SuffixArray(corpusArray, Cache.DEFAULT_CAPACITY);

		} else if (args.length == 3) {

			String binarySourceVocabFileName = args[0];
			String binaryCorpusFileName = args[1];
			String binarySuffixArrayFileName = args[2];

			if (logger.isLoggable(Level.INFO)) logger.info("Constructing source language vocabulary from binary file " + binarySourceVocabFileName);
			ObjectInput in = BinaryIn.vocabulary(binarySourceVocabFileName);
			symbolTable = new Vocabulary();
			symbolTable.readExternal(in);

			logger.info("Constructing corpus array from file " + binaryCorpusFileName);
			if (logger.isLoggable(Level.INFO)) logger.info("Constructing memory mapped source language corpus array.");
			corpusArray = new MemoryMappedCorpusArray(symbolTable, binaryCorpusFileName);

			logger.info("Constructing suffix array from file " + binarySuffixArrayFileName);
			suffixArray = new MemoryMappedSuffixArray(binarySuffixArrayFileName, corpusArray, Cache.DEFAULT_CAPACITY);


		} else {

			System.err.println("Usage: java " + SuffixArray.class.getName() + " source.vocab source.corpus source.suffixes");
			System.exit(0);

			symbolTable = null;
			corpusArray = null;
			suffixArray = null;

		}

		int minFrequency = 0;
		short maxPhrases = 100;
		int maxPhraseLength = 10;
		int windowSize = 10;
		short minNonterminalSpan = 2;

		logger.info("Calculating " + maxPhrases + " most frequent phrases");
		frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength);

		logger.info("Frequent phrases: \n" + frequentPhrases.toString());


		logger.info("Calculating collocations for most frequent phrases");
		FrequentMatches matches = frequentPhrases.getCollocations(maxPhraseLength, windowSize, minNonterminalSpan);

		

//		matches.histogramSort(maxPhrases);
		
		logger.info("Printing collocations for most frequent phrases");		
		logger.info("Total collocations: " + matches.counter);
		//		for (int i=0, n=collocations.size(); i<n; i+=3) {
		//			
		//			int key = collocations.get(i);
		//			short rank2 = (short) key;
		//			short rank1 = (short) (key >> 8);
		//			Phrase phrase1 = frequentPhrases.phraseList.get(rank1);
		//			Phrase phrase2 = frequentPhrases.phraseList.get(rank2);
		//			
		//			String pattern = phrase1.toString() + " X " + phrase2.toString();
		//			
		//			int position1 = collocations.get(i+1);
		//			int position2 = collocations.get(i+2);
		//			
		//			System.out.println(pattern + " " + position1 + "," + position2);
		//		}



		//		for (Map.Entry<Integer, ArrayList<int[]>> entry : collocations.entrySet()) {
		//			
		//			int key = entry.getKey();
		//			ArrayList<int[]> values = entry.getValue();
		//			
		//			short rank2 = (short) key;
		//			short rank1 = (short) (key >> 8);
		//			
		//			Phrase phrase1 = frequentPhrases.phraseList.get(rank1);
		//			Phrase phrase2 = frequentPhrases.phraseList.get(rank2);
		//			
		//			String pattern = phrase1.toString() + " X " + phrase2.toString();
		//			
		//			for (int[] value : values) {
		//				System.out.println(value + "\t" + pattern);
		//			}
		//		}


	}

}
