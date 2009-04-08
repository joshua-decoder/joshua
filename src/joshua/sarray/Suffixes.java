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

import joshua.corpus.SymbolTable;
import joshua.util.Cache;
import joshua.util.sentence.Phrase;

public interface Suffixes {
 

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
		public static final int DEFAULT_CACHE_CAPACITY = 100000;
	
	/**
	 * Gets the symbol table for this object.
	 * 
	 * @return the symbol table for this object.
	 */
	public SymbolTable getVocabulary();
	
	/**
	 * Gets the corpus for this object.
	 * 
	 * @return
	 */
	public Corpus getCorpus();
	
	/**
	 * This method creates a list of trivially HierarchicalPhrases
	 * (i.e. they're really just contiguous phrases, but we
	 * will want to perform some of the HierarchialPhrase
	 * operations on them). Sorts the positions. Adds the results
	 * to the cache.  
	 *<p>
	 * The construction of more complex hierarchical phrases is handled
	 * within the prefix tree. 
	 * 
	 * @param startPositions an unsorted list of the positions
	 *                in the corpus where the matched phrases begin
	 * @param pattern a contiguous phrase
	 * @return a list of trivially hierarchical phrases
	 */ 
	public MatchedHierarchicalPhrases createHierarchicalPhrases(int[] startPositions, Pattern pattern, SymbolTable vocab);
	
	/**
	 * Returns the number of suffixes in the suffix array, which
	 * is identical to the length of the corpus.
	 */
	public int size();
	
	/** 
	 * @return the position in the corpus corresponding to the
	 *         specified index in the suffix array.
	 */
	public int getCorpusIndex(int suffixIndex);
	
	/**
	 * 
	 * @param corpusIndex
	 * @return
	 */
	public int getSentenceIndex(int corpusIndex);
	
	/**
	 * 
	 * @param sentenceIndex
	 * @return
	 */
	public int getSentencePosition(int sentenceIndex);
	
	/**
	 * Finds a phrase in the suffix array.
	 *
	 * @param phrase the search phrase
	 * @return a tuple containing the (inclusive) start and the (inclusive) end bounds
	 *         in the suffix array for the phrase
	 */
	public int[] findPhrase(Phrase phrase);
	
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
	public int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd, int lowerBound, int upperBound);
	
	/**
	 * @return a list of hierarchical phrases that match the pattern if they are already cached
	 *         or null if the pattern is not in the cache.
	 */
	public MatchedHierarchicalPhrases getMatchingPhrases(Pattern pattern);
	
	
	/** 
	 * Caches the matching hierarchical phrases for the pattern. 
	 */
	public void setMatchingPhrases(Pattern pattern, MatchedHierarchicalPhrases matchings);
	
	/**
	 * Gets all of the positions in the corpus for the bounds
	 * in the suffix array, sorting the corpus position.
	 * 
	 * @param bounds Inclusive bounds in the suffix array
	 * @return
	 */
	public int[] getAllPositions(int[] bounds);

	/**
	 * Gets the hierarchical phrase objects cached by this suffix array.
	 * 
	 * @return the hierarchical phrase objects cached by this suffix array
	 */
	public Cache<Pattern,MatchedHierarchicalPhrases> getCachedHierarchicalPhrases();
	
}
