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
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.ContiguousPhrase;
import joshua.corpus.Corpus;
import joshua.corpus.Phrase;

/**
 * Represents all locations in a corpus where the most frequent
 * phrases are located.
 * 
 * @author Lane Schwartz
 * @author Chris Callison-Burch
 * @version $LastChangedDate$
 */
public class FrequentMatches {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(FrequentMatches.class.getName());

	/** 
	 * Stores the frequency rank for each phrase.
	 * <p>
	 * For a given phrase p, this variable stores the value of
	 * n indicating that p is the <i>n</i>th most frequent phrase in
	 * the corpus.
	 * <p>
	 * The iteration order of this map should start with the
	 * most frequent phrase and end with the least frequent
	 * phrase stored in the map.
	 * <p>
	 * The key set for this map should be identical to the key
	 * set in the <code>FrequentPhrases.frequentPhrases</code>
	 * map.
	 */
	private final LinkedHashMap<Phrase,Short> ranks;

	/** 
	 * Maximum number of phrases of which this object is aware. 
	 * <p>
	 * This is <em>not</em> the number of times such phrases 
	 * occur in the corpus. Rather, it is the number of unique
	 * phrase patterns of which this object is aware.
	 */
	private final short maxPhrases;
	
	/**
	 * List of collocation identifiers that have been added to
	 * this object.
	 * <p>
	 * The values for these identifiers are of the format
	 * returned by the <code>getKey</code> method.
	 */
//	final int[] keys;
	
	/**
	 * For each of the n most frequent phrases,
	 * this array holds the starting index into 
	 * the position1 and position2 arrays where
	 * the positions for that phrase are stored.
	 * <p>
	 * The length of this array is equal to <code>maxPhrases</code>.
	 */
	int[] bucketIndex;
	
	/**
	 * List of positions in a corpus where the first phrase in
	 * a collocation starts.
	 */
	final int[] position1;
	
	/**
	 * List of positions in a corpus where the second phrase
	 * in a collocation starts.
	 */
	final int[] position2;
	
	/**
	 * The number of collocations that have been added to this
	 * object.
	 */
	int counter = 0;
	
	/**
	 * The minimum allowed span for a nonterminal gap.
	 */
	final short minNonterminalSpan;
	
	
	/**
	 * Constructs an empty list of locations where collocations
	 * of frequent phrases are found in a corpus.
	 * 
	 * @param ranks Map from phrase to frequency rank of the phrase.
	 * @param maxPhrases The maximum number of frequent phrases.
	 * @param capacity The total number of matches expected.
	 */
	public FrequentMatches(FrequentPhrases frequentPhrases, 
			int maxPhraseLength,
			int windowSize,
			short minNonterminalSpan) {
		
		logger.fine("Calculating number of frequent collocations");
		int capacity = frequentPhrases.countCollocations(maxPhraseLength, windowSize);
		logger.fine("Total collocations: " + capacity);
		
		this.ranks = frequentPhrases.getRanks();
		this.maxPhrases = frequentPhrases.getMaxPhrases();
		this.minNonterminalSpan = minNonterminalSpan;
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Allocating " + ((int)(capacity*4 / 1024.0 / 1024.0)) + "MB for collocation keys");
		int[] keys = new int[capacity];
		if (logger.isLoggable(Level.FINE)) logger.fine("Allocating " + ((int)(capacity*4 / 1024.0 / 1024.0)) + "MB for collocation position1");
		position1 = new int[capacity];
		if (logger.isLoggable(Level.FINE)) logger.fine("Allocating " + ((int)(capacity*4 / 1024.0 / 1024.0)) + "MB for collocation position2");
		position2 = new int[capacity];
		if (logger.isLoggable(Level.FINE)) logger.fine("Done allocating memory for collocations data");
	
		initMatches(this, keys, frequentPhrases.getSuffixes(), this.ranks, maxPhraseLength, windowSize);
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Sorting collocations");
		this.bucketIndex = histogramSort(maxPhrases, keys, position1, position2);
	}

	
	private static void initMatches(FrequentMatches collocations, 
			int[] keys, Suffixes suffixes, 
			LinkedHashMap<Phrase,Short> frequentPhrases, 
			int maxPhraseLength, int windowSize) {
		
		
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
						collocations.add(keys, phrase1, phrase2, position1, position2);

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

						collocations.add(keys, phrase1, phrase2, position1, position2);
						
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

	}
	
	
	/**
	 * Adds a collocated pair of phrases to this container,
	 * along with their respective positions in the corpus.
	 */
	private void add(int[] keys, Phrase phrase1, Phrase phrase2, int position1, int position2) {

		// The second phrase must start after the first phrase ends,
		//     and there must be a minimum gap 
		//     (minNonterminalSpan) between the phrases
		if (position2 >= position1 + phrase1.size() + minNonterminalSpan) {

			int key = getKey(phrase1, phrase2);

			keys[counter] = key;
			this.position1[counter] = position1;
			this.position2[counter] = position2;

			counter++;
			
		}
	}

	
	/**
	 * Returns an integer identifier for the collocation of
	 * <code>phrase1</code> with <code>phrase2</code>.
	 * <p>
	 * If <code>rank1</code> is the rank of <code>phrase1</code>
	 * and <code>rank2</code> is the rank of <code>phrase2</code>,
	 * the identifier returned by this method is defined to be
	 * <code>rank1*maxPhrases + rank2</code>.
	 * <p>
	 * As such, the range of possible values returned by this
	 * method will be </code>0</code> through
	 * <code>maxPhrases*maxPhrases-1</code>.
	 *
	 * @param phrase1 First phrase in a collocation.
	 * @param phrase2 Second phrase in a collocation.
	 * @return a unique integer identifier for the collocation.
	 */
	private int getKey(Phrase phrase1, Phrase phrase2) {

		short rank1 = ranks.get(phrase1);
		short rank2 = ranks.get(phrase2);

		int rank = rank1*maxPhrases + rank2;

		return rank;
	}
	
	
	/**
	 * Sorts match data using a specialization of bucket sort.
	 * 
	 * @param maxPhrases
	 * @param keys
	 * @param position1
	 * @param position2
	 * @return Array containing the starting positions of each bucket
	 */
	private static int[] histogramSort(int maxPhrases, int[] keys, int[] position1, int[] position2) {
		
		int maxBuckets = maxPhrases*maxPhrases;
	
		logger.fine("Calculating histograms");
		int[] histogram = calculateHistogram(maxPhrases, keys, maxBuckets);

		if (logger.isLoggable(Level.FINEST)) logger.finest("Allocating memory for " + maxBuckets + " integers");
		int[] offsets = new int[maxBuckets];
		
		logger.fine("Calculating offsets");
		for (int key=0, offset=0; key<maxBuckets; key++) {
			
			offsets[key] = 0;
			
			int value = histogram[key];
			histogram[key] = offset;
			offset += value;
			
		}
		
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Allocating temporary memory for keys: " + ((keys.length)*4/1024/1024) + "MB");
		int[] tmpKeys = new int[keys.length];
		if (logger.isLoggable(Level.FINE)) logger.fine("Allocating temporary memory for position1: " + ((keys.length)*4/1024/1024) + "MB");
		int[] tmpPosition1 = new int[keys.length];
		if (logger.isLoggable(Level.FINE)) logger.fine("Allocating temporary memory for position2: " + ((keys.length)*4/1024/1024) + "MB");
		int[] tmpPosition2 = new int[keys.length];
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Placing data into buckets");
		for (int i=0, n=keys.length; i < n; i++) {
			
			int key = keys[i];
			int location = histogram[key] + offsets[key];
			offsets[key] += 1;
			
			tmpKeys[location] = key;
			tmpPosition1[location] = position1[i];
			tmpPosition2[location] = position2[i];
			
		}
		
		logger.fine("Copying sorted keys to final location");
		System.arraycopy(tmpKeys, 0, keys, 0, keys.length);
		
		logger.fine("Copying sorted position1 data to final location");
		System.arraycopy(tmpPosition1, 0, position1, 0, keys.length);
		
		logger.fine("Copying sorted position1 data to final location");
		System.arraycopy(tmpPosition2, 0, position2, 0, keys.length);
		
		// Try and help the garbage collector know we're done with these
//		histogram = null;
		offsets = null;
		tmpKeys = null;
		tmpPosition1 = null;
		tmpPosition2 = null;
		
		return histogram;
		
	}
	
	
	/**
	 * Calculate how many times each key occurred.
	 * 
	 * @param maxPhrases
	 * @param keys
	 * @param maxBuckets
	 * @return Histogram indicating how many times each key occurred
	 */
	private static int[] calculateHistogram(int maxPhrases, int[] keys, int maxBuckets) {

		int[] histogram = new int[maxBuckets];
		Arrays.fill(histogram, 0);
		
		for (int key : keys) {
			
			histogram[key] += 1;

		}
		
		if (logger.isLoggable(Level.FINE)) {
			
			int max = -1;
			int maxKey = -1;
			
			int least = Integer.MAX_VALUE;
			int leastKey = Integer.MAX_VALUE;
			
			for (int key=0; key<maxBuckets; key++) {
				if (histogram[key] > max) {
					max = histogram[key];
					maxKey = key;
				}
				if (histogram[key] < least) {
					least = histogram[key];
					leastKey = key;
				}
			}
			
			int a = maxKey / maxPhrases;
			int b = maxKey % maxPhrases;
			
			logger.fine("Most frequent collocation is key " + maxKey + " a=="+a+ " b=="+b+" (" +max+") times");
			
			int c = leastKey / maxPhrases;
			int d = leastKey % maxPhrases;
			logger.fine("Most frequent collocation is key " + leastKey + " c=="+c+ " d=="+d+" (" +least+") times");
		}
		
		return histogram;
	}

	/**
	 * Not supported; throws an UnsupportedOperationException.
	 * 
	 * @param in
	 * @throws UnsupportedOperationException
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		throw new UnsupportedOperationException();
		
	}

	/**
	 * Write the contents of this class as binary data to an
	 * output stream.
	 * 
	 * @param out
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
