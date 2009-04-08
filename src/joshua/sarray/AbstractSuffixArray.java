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

import java.util.Arrays;

import joshua.corpus.SymbolTable;
import joshua.util.Cache;
import joshua.util.sentence.Phrase;

public abstract class AbstractSuffixArray implements Suffixes {
	
		
	/**
	 * Maps from patterns to lists of hierarchical phrases
	 * that match the corresponding pattern in the corpus.
	 * <p>
	 * This cache is a most-recently accessed map, 
	 * so commonly accessed patterns will remain in the cache,
	 * while rare patterns will eventually drop out of the cache.
	 */
	protected final Cache<Pattern,MatchedHierarchicalPhrases> hierarchicalPhraseCache;
	
	
	/** Integer array representation of the corpus for this suffix array. */
	protected final Corpus corpus;
	
	public AbstractSuffixArray(Corpus corpus, Cache<Pattern,MatchedHierarchicalPhrases> hierarchicalPhraseCache) {
		this.hierarchicalPhraseCache = hierarchicalPhraseCache;
		this.corpus = corpus;
	}
	
	public Cache<Pattern,MatchedHierarchicalPhrases> getCachedHierarchicalPhrases() {
		return hierarchicalPhraseCache;
	}
	
	public MatchedHierarchicalPhrases createHierarchicalPhrases(int[] startPositions,
			Pattern pattern, SymbolTable vocab) {

		if (startPositions == null) {
			return HierarchicalPhrases.emptyList(vocab);
		} else if (hierarchicalPhraseCache==null) {
			Arrays.sort(startPositions);
			HierarchicalPhrases hierarchicalPhrases = new HierarchicalPhrases(pattern, startPositions, getCorpus().getSentenceIndices(startPositions));	
			return hierarchicalPhrases;
		} else {
			if (hierarchicalPhraseCache.containsKey(pattern)) {
				return hierarchicalPhraseCache.get(pattern);
			} else {
				// In the case of contiguous phrases, the hpCache is essentially acting as Adam's Inverted Index, because it stores 
				// the corpus-sorted indexes of each of the phrases.  It differs because it creates a HierarhicalPhrases object rather 
				// than just int[].
				Arrays.sort(startPositions);
				HierarchicalPhrases hierarchicalPhrases = new HierarchicalPhrases(pattern, startPositions, getCorpus().getSentenceIndices(startPositions));	
				hierarchicalPhraseCache.put(pattern, hierarchicalPhrases);
				return hierarchicalPhrases;
			}
		}
		
	}

	public int[] findPhrase(Phrase phrase) {
		return findPhrase(phrase, 0, phrase.size());
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
		return findPhrase(sentence, phraseStart, phraseEnd, 0, size()-1);
	}
	
	public int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd,
			int lowerBound, int upperBound) {

		int[] bounds = new int[2];
		lowerBound = findPhraseBound(sentence, phraseStart, phraseEnd, lowerBound, upperBound, true);
		if (lowerBound < 0) return null;
		upperBound = findPhraseBound(sentence, phraseStart, phraseEnd, lowerBound, upperBound, false);
		bounds[0]=lowerBound;
		bounds[1]=upperBound;
		return bounds;	
		
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
			int mid = (low + high) >>> 1;
			int start = getCorpusIndex(mid);
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
					int nextDiff = corpus.comparePhrase(getCorpusIndex(neighbor), sentence, phraseStart, phraseEnd);
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

	public Corpus getCorpus() {
		return corpus;
	}

	public abstract int getCorpusIndex(int suffixIndex);

	public MatchedHierarchicalPhrases getMatchingPhrases(Pattern pattern) {
		if (hierarchicalPhraseCache==null) {
			return null;
		} else {
			return hierarchicalPhraseCache.get(pattern);
		}
	}

	public int getSentenceIndex(int corpusIndex) {
		return corpus.getSentenceIndex(corpusIndex);
	}

	public int getSentencePosition(int sentenceIndex) {
		return corpus.getSentencePosition(sentenceIndex);
	}

	public SymbolTable getVocabulary() {
		return corpus.getVocabulary();
	}

	public void setMatchingPhrases(Pattern pattern,
			MatchedHierarchicalPhrases matchings) {
		
		if (hierarchicalPhraseCache==null) {
			return;
		} else {
			hierarchicalPhraseCache.put(pattern, matchings);
		}
		
	}

	public abstract int size();

}
