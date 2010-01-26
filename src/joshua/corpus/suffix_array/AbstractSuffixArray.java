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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Phrase;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.util.Cache;

/**
 * This class provides a mostly-complete implementation of the
 * <code>Suffixes</code> interface, designed to minimize the effort
 * required to build a concrete implementation of a suffix array
 * data structure.
 * <p>
 * To implement a concrete suffix array, the programmer need only
 * implement the <code>getCorpusIndex(int suffixIndex)</code> and
 * <code>size()</code> methods.
 * 
 * @author Lane Schwartz
 * @author Chris Callison-Burch
 */
public abstract class AbstractSuffixArray implements Suffixes {
	
	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(AbstractSuffixArray.class.getName());
	
	/**
	 * Maps from patterns to lists of hierarchical phrases that
	 * match the corresponding pattern in the corpus.
	 * <p>
	 * This cache is a most-recently accessed map, so commonly
	 * accessed patterns will remain in the cache, while rare
	 * patterns will eventually drop out of the cache.
	 */
	protected final Cache<Pattern,MatchedHierarchicalPhrases> hierarchicalPhraseCache;
	
	/**
	 * Maps from patterns to lists of hierarchical phrases that
	 * match the corresponding pattern in the corpus.
	 * <p>
	 * This cache is a most-recently accessed map, so commonly
	 * accessed patterns will remain in the cache, while rare
	 * patterns will eventually drop out of the cache.
	 */
	protected final Cache<Pattern,List<Rule>> ruleCache;
	
	/**
	 * Integer array representation of the corpus for this
	 * suffix array.
	 */
	protected final Corpus corpus;
	
	/**
	 * Constructs an abstract suffix array based on the provided
	 * corpus.
	 * 
	 * The specified cache will be used to store matched
	 * hierarchical phrases for frequently accessed patterns.
	 * 
	 * @param corpus Corpus upon which this suffix array is based.
	 * @param hierarchicalPhraseCache Cache to store matched
	 *               hierarchical phrases for frequently accessed
	 *               patterns
	 */
	public AbstractSuffixArray(
			Corpus corpus, 
			Cache<Pattern,MatchedHierarchicalPhrases> hierarchicalPhraseCache, 
			Cache<Pattern,List<Rule>> ruleCache) {
		
		this.hierarchicalPhraseCache = hierarchicalPhraseCache;
		this.ruleCache = ruleCache;
		this.corpus = corpus;
	}
	
	/* See Javadoc for Suffixes interface.*/
	public Cache<Pattern,MatchedHierarchicalPhrases> getCachedHierarchicalPhrases() {
		return hierarchicalPhraseCache;
	}

	/* See Javadoc for Suffixes interface.*/
	public Cache<Pattern,List<Rule>> getCachedRules() {
		return this.ruleCache;
	}
	
	/* See Javadoc for Suffixes interface.*/
	public MatchedHierarchicalPhrases createHierarchicalPhrases(Pattern pattern, int minNonterminalSpan, int maxPhraseSpan) {
		
		if (hierarchicalPhraseCache.containsKey(pattern)) {
			return hierarchicalPhraseCache.get(pattern);
		} else {

			int arity = pattern.arity();
			int size = pattern.size();
			int[] patternTokens = pattern.getWordIDs();
			
			SymbolTable vocab = corpus.getVocabulary();
			
			if (arity==0) {
				int[] bounds = this.findPhrase(pattern, 0, pattern.size(), 0, this.size()-1);
				int[] startPositions = this.getAllPositions(bounds);
				MatchedHierarchicalPhrases result = this.createTriviallyHierarchicalPhrases(startPositions, pattern, vocab);
				return result;
			} else if (arity==size) {
				int[] startPositions = new int[]{};
				MatchedHierarchicalPhrases result = this.createTriviallyHierarchicalPhrases(startPositions, pattern, vocab);
				return result;
			} else if (arity==1 && pattern.startsWithNonterminal()) {
				int[] terminals = new int[size-1];
				for (int i=1; i<size; i++) {
					terminals[i-1] = patternTokens[i];
				}
				Pattern terminalsPattern = new Pattern(vocab, terminals);
				MatchedHierarchicalPhrases terminalsMatch = this.createHierarchicalPhrases(terminalsPattern, minNonterminalSpan, maxPhraseSpan);
				MatchedHierarchicalPhrases result = terminalsMatch.copyWithInitialX();
				hierarchicalPhraseCache.put(pattern, result);
				return result;
			} else if (arity==1 && pattern.endsWithNonterminal()) {
				int[] terminals = new int[size-1];
				for (int i=0, n=size-1; i<n; i++) {
					terminals[i] = patternTokens[i];
				}
				Pattern terminalsPattern = new Pattern(vocab, terminals);
				MatchedHierarchicalPhrases terminalsMatch = this.createHierarchicalPhrases(terminalsPattern, minNonterminalSpan, maxPhraseSpan);
				MatchedHierarchicalPhrases result = terminalsMatch.copyWithFinalX();
				hierarchicalPhraseCache.put(pattern, result);
				return result;
//				int[] bounds = this.findPhrase(pattern, 0, size, 0, this.size());
//				int[] startPositions = this.getAllPositions(bounds);
////				Pattern patternX = new Pattern(pattern, PrefixTree.X);
//				MatchedHierarchicalPhrases result = this.createHierarchicalPhrases(startPositions, pattern, vocab);
//				return result;
			}  else {
				
				int[] prefixTokens = new int[patternTokens.length - 1];
				for (int i=0, n=patternTokens.length-1; i<n; i++) {
					prefixTokens[i] = patternTokens[i];
				}
				
				int[] suffixTokens = new int[patternTokens.length - 1];
				for (int i=1, n=patternTokens.length; i<n; i++) {
					suffixTokens[i-1] = patternTokens[i];
				}
				
				Pattern prefix = new Pattern(vocab, prefixTokens);
				Pattern suffix = new Pattern(vocab, suffixTokens);
				
				MatchedHierarchicalPhrases prefixMatches = createHierarchicalPhrases(prefix, minNonterminalSpan, maxPhraseSpan);
				MatchedHierarchicalPhrases suffixMatches = createHierarchicalPhrases(suffix, minNonterminalSpan, maxPhraseSpan);
				
				MatchedHierarchicalPhrases result = 
					HierarchicalPhrases.queryIntersect(
							pattern, prefixMatches, suffixMatches, 
							minNonterminalSpan, maxPhraseSpan, this);
			
				hierarchicalPhraseCache.put(pattern, result);
				return result;
			}
		}
		
	}
	
	/* See Javadoc for Suffixes interface.*/
	public MatchedHierarchicalPhrases createTriviallyHierarchicalPhrases(int[] startPositions,
			Pattern pattern, SymbolTable vocab) {

			if (hierarchicalPhraseCache.containsKey(pattern)) {
				if (logger.isLoggable(Level.FINEST)) logger.finest("Cache has " + hierarchicalPhraseCache.size() + " entries, and did contain pattern:    	" + pattern.toString());
				return hierarchicalPhraseCache.get(pattern);
			} else {
				if (logger.isLoggable(Level.FINEST)) logger.finest("Cache has " + hierarchicalPhraseCache.size() + " entries, but did not contain pattern:	" + pattern.toString());
				// In the case of contiguous phrases, 
				//   the hpCache is essentially acting as Adam's Inverted Index, 
				//   because it stores the corpus-sorted indexes of each of the phrases.  
				// It differs because it creates a HierarchicalPhrases object rather than just int[].
				Arrays.sort(startPositions);
				HierarchicalPhrases hierarchicalPhrases = new HierarchicalPhrases(pattern, startPositions, getCorpus().getSentenceIndices(startPositions));	
				hierarchicalPhraseCache.put(pattern, hierarchicalPhrases);
				return hierarchicalPhrases;
			}
			
	}

	/* See Javadoc for Suffixes interface.*/
	public int[] findPhrase(Phrase phrase) {
		return findPhrase(phrase, 0, phrase.size());
	}

	/* See Javadoc for Suffixes interface.*/
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

	/* See Javadoc for Suffixes interface.*/
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

	/* See Javadoc for Suffixes interface.*/
	public Corpus getCorpus() {
		return corpus;
	}

	/* See Javadoc for Suffixes interface.*/
	public abstract int getCorpusIndex(int suffixIndex);

	/* See Javadoc for Suffixes interface.*/
	public MatchedHierarchicalPhrases getMatchingPhrases(Pattern pattern) {
		return hierarchicalPhraseCache.get(pattern);
	}

	/* See Javadoc for Suffixes interface.*/
	public int getSentenceIndex(int corpusIndex) {
		return corpus.getSentenceIndex(corpusIndex);
	}

	/* See Javadoc for Suffixes interface.*/
	public int getSentencePosition(int sentenceIndex) {
		return corpus.getSentencePosition(sentenceIndex);
	}

	/* See Javadoc for Suffixes interface.*/
	public SymbolTable getVocabulary() {
		return corpus.getVocabulary();
	}

	/* See Javadoc for Suffixes interface.*/
	public void cacheMatchingPhrases(MatchedHierarchicalPhrases matchings) {	
		hierarchicalPhraseCache.put(matchings.getPattern(), matchings);	
	}

	/* See Javadoc for Suffixes interface.*/
	public abstract int size();

	
	
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
	 * @return a tuple containing the (inclusive) start and the
	 *         (inclusive) end bounds in the suffix array for
	 *         the phrase
	 */
	protected int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd) {
		return findPhrase(sentence, phraseStart, phraseEnd, 0, size()-1);
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
	 * @param sentence    the sentence/superphrase in int
	 *                    representation to draw the search
	 *                    phrase from
	 * @param phraseStart the start of the phrase in the sentence
	 *                    (inclusive)
	 * @param phraseEnd   the end of the phrase in the sentence
	 *                    (exclusive)
	 * @param suffixArrayStart the point at which to start the
	 *                    search in the suffix array
	 * @param suffixArrayEnd the end point in the suffix array
	 *                    beyond which the search doesn't need
	 *                    to take place
	 * @param findFirst   a flag that indicates whether we
	 *                    should find the first or last occurrence
	 *                    of the phrase
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
						if (findFirst) {
							diff = 1; //search lower
						} else {
							diff = -1; //search higher
						}
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
}
