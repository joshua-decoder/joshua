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
import java.io.ObjectInput;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.util.Cache;
import joshua.util.ReverseOrder;
import joshua.util.io.BinaryIn;
import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;

/**
 * Extra methods related to suffix array grammar extraction.
 * 
 * @author Chris Callison-Burch
 * @author Lane Schwartz
 */
public class FrequentPhrases {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(FrequentPhrases.class.getName());

	private final Suffixes suffixes;
	private final LinkedHashMap<Phrase,Integer> frequentPhrases;	

	private final Map<Phrase,Short> ranks;
//	private final List<Phrase> phraseList;

	private final short maxPhrases;
	
	public FrequentPhrases(
			Suffixes suffixes,		
			int minFrequency,
			short maxPhrases,
			int maxPhraseLength) {

		
		this.maxPhrases = maxPhrases;
		
		this.suffixes = suffixes;
		this.frequentPhrases = getMostFrequentPhrases(suffixes, minFrequency, maxPhrases, maxPhraseLength);

		this.ranks = getRanks(frequentPhrases);
//		this.phraseList = new ArrayList<Phrase>(frequentPhrases.keySet());

		
	}

	public static Map<Phrase,Short> getRanks(Map<Phrase,Integer> frequentPhrases) {
		
		logger.fine("Calculating ranks of frequent phrases");
		
		Map<Phrase,Short> ranks = new HashMap<Phrase,Short>(frequentPhrases.size());

		short i=0;
		for (Phrase phrase : frequentPhrases.keySet()) {
			ranks.put(phrase, i++);
		}
		
		logger.fine("Done calculating ranks");
		
		return ranks;
	}


	/**
	 * This method performs a one-pass computation of the
	 * collocation of two frequent subphrases. It is used for
	 * the precalculation of the translations of hierarchical
	 * phrases which are problematic to calculate on the fly.
	 * This procedure is described in "Hierarchical Phrase-Based
	 * Translation with Suffix Arrays" by Adam Lopez.
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
			int maxPhraseLength,
			int windowSize
	) {

		logger.fine("Calculating number of frequent collocations");
		int totalCollocations = countCollocations(maxPhraseLength, windowSize);
		logger.fine("Total collocations: " + totalCollocations);


		
		Collocations collocations = new Collocations(totalCollocations);
		logger.fine("Done allocating memory");

		LinkedList<Phrase> phrasesInWindow = new LinkedList<Phrase>();
		LinkedList<Integer> positions = new LinkedList<Integer>();
		int sentenceNumber = 1;
		int endOfSentence = suffixes.getSentencePosition(sentenceNumber);

		// ccb - debugging
		if (logger.isLoggable(Level.FINEST)) logger.finest("END OF SENT: " + endOfSentence);

		// collocations maps Phrase->Phrase->a list of
		// positions in corpus
		//		Collocations collocations = new Collocations();

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

					//					Phrase phrase1 = phrasesInWindow.get(i);
					//					int position1 = positions.get(i);

					Phrase phrase1 = phrasesInWindow.removeFirst();
					int position1 = positions.removeFirst();

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

				//				break;
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
					//					LinkedList l = new LinkedList(); l.remove(0);
					Phrase phrase1 = phrasesInWindow.removeFirst(); //phrasesInWindow.remove(0);
					positions.removeFirst();
					//					positions.remove(0);

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = 0, n=phrasesInWindow.size(); j < n; j++) {

						Phrase phrase2 = phraseIterator.next();//phrasesInWindow.get(j);
						int position2 = positionIterator.next();//positions.get(j);

						collocations.add(phrase1, phrase2, position1, position2);
						// ccb - debugging
						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE2: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
					}
					if (phrasesInWindow.size() > 0) {
						position1 = positions.getFirst();//.get(0);
					} else {
						position1 = currentPosition;
					}
				}
			}

		} // end iterating over positions in the corpus

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
	 * @param phrases         the phrases which are to be checked
	 *                        for their collocation
	 * @param maxPhraseLength the maximum length of any phrase
	 *                        in the phrases
	 * @param windowSize      the maximum allowable space between
	 *                        phrases for them to still be
	 *                        considered collocated
	 */
	private int countCollocations(
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

		// collocations maps Phrase->Phrase->a list of
		// positions in corpus
		//		Collocations collocations = new Collocations();

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

					//					Phrase phrase1 = phrasesInWindow.get(i);
					//					int position1 = positions.get(i);

					Phrase phrase1 = phrasesInWindow.removeFirst();
					int position1 = positions.removeFirst();

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = i+1; j < n; j++) {

						Phrase phrase2 = phraseIterator.next();//phrasesInWindow.get(j);
						int position2 = positionIterator.next();//positions.get(j);

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

				//				break;
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
					//					LinkedList l = new LinkedList(); l.remove(0);
					Phrase phrase1 = phrasesInWindow.removeFirst(); //phrasesInWindow.remove(0);
					positions.removeFirst();
					//					positions.remove(0);

					Iterator<Phrase> phraseIterator = phrasesInWindow.iterator();
					Iterator<Integer> positionIterator = positions.iterator();

					for (int j = 0, n=phrasesInWindow.size(); j < n; j++) {

						Phrase phrase2 = phraseIterator.next();//phrasesInWindow.get(j);
						int position2 = positionIterator.next();//positions.get(j);

						count++;
						//						collocations.add(phrase1, phrase2, position1, position2);
						// ccb - debugging
						if (logger.isLoggable(Level.FINEST)) logger.finest("CASE2: " + phrase1 + "\t" + phrase2 + "\t" + position1 + "\t" + position2);
					}
					if (phrasesInWindow.size() > 0) {
						position1 = positions.getFirst();//.get(0);
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
	protected static LinkedHashMap<Phrase,Integer> getMostFrequentPhrases(
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

	/**
	 * This inner-class is a wrapper for a complex data type,
	 * which maps Phrase->Phrase->a list of tuples containing
	 * the starting positions of the two phrases in the corpus.
	 */
	//	protected class Collocations extends HashMap<Phrase,HashMap<Phrase,ArrayList<int[]>>> {
	protected class Collocations { //extends ArrayList<Integer> { //extends HashMap<Integer,ArrayList<int[]>>{

		//		 ArrayList<Integer> list = new ArrayList<Integer>(); 
		final int[] keys;
		final int[] position1;
		final int[] position2;
		
//		final Map<Phrase,Short> ranks;
//		final short maxPhrases;
		
		public Collocations(int totalCollocations) {
//				, Map<Phrase,Short> ranks, short maxPhrases) {
			
			logger.fine("Allocating " + ((int)(totalCollocations*4 / 1024.0 / 1024.0)) + "MB for collocation keys");
			keys = new int[totalCollocations];
			logger.fine("Allocating " + ((int)(totalCollocations*4 / 1024.0 / 1024.0)) + "MB for collocation position1");
			position1 = new int[totalCollocations];
			logger.fine("Allocating " + ((int)(totalCollocations*4 / 1024.0 / 1024.0)) + "MB for collocation position2");
			position2 = new int[totalCollocations];
			logger.fine("Done allocating memory for collocations data");
//			this.ranks = ranks;
//			this.maxPhrases = maxPhrases;
		}

		int counter = 0;

		protected int getKey(Phrase phrase1, Phrase phrase2) {

			short rank1 = ranks.get(phrase1);
			short rank2 = ranks.get(phrase2);

			int rank = rank1*maxPhrases + rank2;
//			int rank  = (rank1 << 8);
//			rank |=  rank2;

			return rank;
		}
		
		/**
		 * Adds a collocated pair of phrases to this
		 * container, along with their respective positions
		 * in the corpus.
		 */
		protected void add(Phrase phrase1, Phrase phrase2, int position1, int position2) {


			// check to make sure that the phrase2 isn't simply a subphrase of phrase1
			if(position2-position1 >= phrase1.size()) {

				int key = getKey(phrase1, phrase2);

				this.keys[counter] = key;
				this.position1[counter] = position1;
				this.position2[counter] = position2;

				counter++;
			}
		}

		protected void histogramSort(int maxPhrases) {
			int maxBuckets = maxPhrases*maxPhrases;
		
			logger.fine("Calculating histograms");
//			Map<Integer,Integer> histogram = calculateHistogram(keys, maxBuckets);
			int[] histogram = calculateHistogram(keys, maxBuckets);
//			Map<Integer,Integer> offsets = new HashMap<Integer,Integer>(maxBuckets);
			int[] offsets = new int[maxBuckets];
//			Arrays.fill(offsets, 0);
			
			logger.fine("Calculating offsets");
//			int counter = 0;
			for (int key=0, counter=0; key<maxBuckets; key++) {
				
				offsets[key] = 0;
				
				int value = histogram[key];
				histogram[key] = counter;
				counter += value;
				
			}
			
//			for (int key : histogram.keySet()) {
//				
//				int value = histogram.get(key);
//				histogram.put(key, counter);
//				counter += value;
//				
//				offsets.put(key, 0);
//			}
			
			logger.fine("Allocating temporary memory for keys: " + ((keys.length)*4/1024/1024) + "MB");
			int[] tmpKeys = new int[keys.length];
			logger.fine("Allocating temporary memory for position1: " + ((keys.length)*4/1024/1024) + "MB");
			int[] tmpPosition1 = new int[keys.length];
			logger.fine("Allocating temporary memory for position2: " + ((keys.length)*4/1024/1024) + "MB");
			int[] tmpPosition2 = new int[keys.length];
			
			logger.fine("Placing data into buckets");
			for (int i=0, n=keys.length; i < n; i++) {
				
				int key = keys[i];
//				int offset = offsets.get(key);
//				int location = histogram.get(key) + offset;
				int offset = offsets[key]++;
				int location = histogram[key] + offset;
				
				tmpKeys[location] = key;
				tmpPosition1[location] = position1[i];
				tmpPosition2[location] = position2[i];
				
//				offsets.put(key, offset+1);
//				offsets[key] += 1;
			}
			
			logger.fine("Copying sorted keys to final location");
			System.arraycopy(tmpKeys, 0, keys, 0, keys.length);
			
			logger.fine("Copying sorted position1 data to final location");
			System.arraycopy(tmpPosition1, 0, position1, 0, keys.length);
			
			logger.fine("Copying sorted position1 data to final location");
			System.arraycopy(tmpPosition2, 0, position2, 0, keys.length);
			
			// Try and help the garbage collector know we're done with these
			histogram = null;
			offsets = null;
			tmpKeys = null;
			tmpPosition1 = null;
			tmpPosition2 = null;			
			
		}

		protected int[] calculateHistogram(int[] keys, int maxBuckets) {
//		protected Map<Integer,Integer> calculateHistogram(int[] keys, int maxBuckets) {
//			Map<Integer,Integer> histogram = new HashMap<Integer,Integer>(maxBuckets);
			int[] histogram = new int[maxBuckets];
			Arrays.fill(histogram, 0);
			
			for (int key : keys) {
				
//				int count = (histogram.containsKey(keys)) ? histogram.get(key) : 0;
				
//				histogram.put(key, ++count);
				histogram[key] += 1;
			}
			
			return histogram;
		}
		
//		protected void quickSort() {
//			quickSort(0, keys.length-1);
//		}
//
//		private void quickSort(int left, int right) {
//
//			int pivot = keys[left];
//			int pivotPosition1 = position1[left];
//			int pivotPosition2 = position2[left];
//			
//			int oldLeft = left;
//			int oldRight = right;
//
//			while (left < right) {
//
//				while ((keys[right] >= pivot) && (left < right)) {
//					right--;
//				}
//
//				if (left != right) {
//					keys[left] = keys[right];
//					position1[left] = position1[right];
//					position2[left] = position2[right];
//					left++;
//				}
//				
//				while ((keys[left] <= pivot) && (left < right)) {
//					left++;
//				}
//				
//				if (left != right) {
//					keys[right] = keys[left];
//					position1[right] = position1[left];
//					position2[right] = position2[left];
//					right--;
//				}
//			}
//			
//			keys[left] = pivot;
//			position1[left] = pivotPosition1;
//			position2[left] = pivotPosition2;
//			
//			pivot = left;
//			left = oldLeft;
//			right = oldRight;
//			
//			if (left < pivot) {
//				quickSort(left, pivot-1);
//			}
//			
//			if (right > pivot) {
//				quickSort(pivot+1, right);
//			}
//
//
//		}



		//			/**
		//			 * Adds a collocated pair of phrases to this
		//			 * container, along with their respective positions
		//			 * in the corpus.
		//			 */
		//			protected void add(Phrase phrase1, Phrase phrase2, int position1, int position2) {
		//				// check to make sure that the phrase2 isn't simply a subphrase of phrase1
		//				if(position2-position1 >= phrase1.size()) {
		//					int[] position = new int[2];
		//					position[0] = position1;
		//					position[1] = position2;
		//					
		//					int key = getKey(phrase1, phrase2);
		//					
		//					// use the second phrase as a key
		//					ArrayList<int[]> positions = get(key);
		//					// if we don't have any instances of the second phrase collocating with first phrase,
		//					// then initialize a new list of positions for it...
		//					if(positions == null) {
		//						positions = new ArrayList<int[]>();
		//						this.put(key, positions);
		//					}
		//					
		//					// add everything to their respective containers
		//					positions.add(position);
		//
		//				}
		//			}


		//		/**
		//		 * Adds a collocated pair of phrases to this
		//		 * container, along with their respective positions
		//		 * in the corpus.
		//		 */
		//		protected void add(Phrase phrase1, Phrase phrase2, int position1, int position2) {
		//			// check to make sure that the phrase2 isn't simply a subphrase of phrase1
		//			if(position2-position1 >= phrase1.size()) {
		//				int[] position = new int[2];
		//				position[0] = position1;
		//				position[1] = position2;
		//				
		//				// use the first phrase as a key
		//				HashMap<Phrase,ArrayList<int[]>> phrase2ToPositionsMap = this.get(phrase1);
		//				// if we don't have any previous instances of the first phrase, 
		//				// then initialize a new map for it...
		//				if(phrase2ToPositionsMap == null) {
		//					phrase2ToPositionsMap = new HashMap<Phrase,ArrayList<int[]>>();
		//				}
		//				
		//				// use the second phrase as a key
		//				ArrayList<int[]> positions = phrase2ToPositionsMap.get(phrase2);
		//				// if we don't have any instances of the second phrase collocating with first phrase,
		//				// then initialize a new list of positions for it...
		//				if(positions == null) {
		//					positions = new ArrayList<int[]>();
		//				}
		//				
		//				// add everything to their respective containers
		//				positions.add(position);
		//				phrase2ToPositionsMap.put(phrase2, positions);
		//				this.put(phrase1, phrase2ToPositionsMap);
		//			}
		//		}


		//		/**
		//		 * Gets the list of positions for a pair of phrases
		//		 */
		//		protected ArrayList<int[]> getCollocations(Phrase phrase1, Phrase phrase2) {
		//			// use the first phrase as a key
		//			HashMap<Phrase,ArrayList<int[]>> phrase2ToPositionsMap = this.get(phrase1);
		//
		//			// if we don't have any instances of the first phrase, return an empty list...
		//			if(phrase2ToPositionsMap == null) return new ArrayList<int[]>();
		//			
		//			// use the second phrase as a key
		//			ArrayList<int[]> positions = phrase2ToPositionsMap.get(phrase2);
		//			if(positions == null) {
		//				// if we don't have any collocations for the pair of phrases, return an empty list...
		//				return new ArrayList<int[]>();
		//			} else {
		//				return positions;
		//			}
		//		}
		//		
		//		public String toString() {
		//			
		//			String str = "";
		//			Iterator<Phrase> it = keySet().iterator();
		//			while(it.hasNext()) {
		//				Phrase phrase1 = it.next();
		//				HashMap<Phrase,ArrayList<int[]>> phrase2ToPositionsMap = this.get(phrase1);
		//				Iterator<Phrase> jt = phrase2ToPositionsMap.keySet().iterator();
		//				while(jt.hasNext()) {
		//					Phrase phrase2 = jt.next();
		//					ArrayList<int[]> positions = phrase2ToPositionsMap.get(phrase2);
		//					str += phrase1 + "\t" + phrase2 + "\t(" + positions.size() + ")\n";
		//				}
		//			}
		//			return str;
		//			
		//		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {


		Vocabulary symbolTable;
		Corpus corpusArray;
		Suffixes suffixArray;
		FrequentPhrases frequentPhrases;

		if (args.length == 1) {

			String corpusFileName = args[0];

			logger.info("Constructing vocabulary from file " + corpusFileName);
			symbolTable = new Vocabulary();
			int[] lengths = Vocabulary.createVocabulary(corpusFileName, symbolTable);

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


		logger.info("Calculating " + maxPhrases + " most frequent phrases");
		frequentPhrases = new FrequentPhrases(suffixArray, minFrequency, maxPhrases, maxPhraseLength);

		logger.info("Frequent phrases: \n" + frequentPhrases.toString());


		logger.info("Calculating collocations for most frequent phrases");
		Collocations collocations = frequentPhrases.getCollocations(maxPhraseLength, windowSize);

//		logger.info("Clearing memory");
//		symbolTable = null;
//		corpusArray = null;
//		suffixArray = null;
//		frequentPhrases = null;
//		System.gc();
		
		logger.info("Sorting collocations");
		collocations.histogramSort(maxPhrases);
		
		logger.info("Printing collocations for most frequent phrases");		
		logger.info("Total collocations: " + collocations.counter);
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
