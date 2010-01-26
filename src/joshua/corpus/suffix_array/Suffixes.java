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

import java.util.List;

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Phrase;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Rule;
import joshua.util.Cache;

/**
 * A representation of the suffixes in a corpus.
 *
 * @author Lane Schwartz
 * @author Chris Callison-Burch
 */
public interface Suffixes {
 

	//===============================================================
	// Constants
	//===============================================================

	/**
	 * The maximum length suffix to consider during sorting.
	 */
	int MAX_COMPARISON_LENGTH = 20;

	/** 
	 * Maximum number of items that can be stored in the cache
	 * of patterns and hierarchical phrases.
	 */
	int DEFAULT_CACHE_CAPACITY = 100000;
	
	
	
	/**
	 * Gets the symbol table for this object.
	 *
	 * @return the symbol table for this object.
	 */
	SymbolTable getVocabulary();
	
	/**
	 * Gets the corpus for this object.
	 * 
	 * @return the corpus for this object
	 */
	Corpus getCorpus();
	
	/**
	 * This method creates a list of trivially HierarchicalPhrases
	 * (i.e.\ they're really just contiguous phrases, but we
	 * will want to perform some of the HierarchialPhrase
	 * operations on them). Sorts the positions. Adds the results
	 * to the cache.
	 * <p>
	 * The construction of more complex hierarchical phrases
	 * is handled within the prefix tree.
	 * 
	 * @param startPositions an unsorted list of the positions
	 *                in the corpus where the matched phrases begin
	 * @param pattern a contiguous phrase
	 * @return a list of trivially hierarchical phrases
	 */
	MatchedHierarchicalPhrases createTriviallyHierarchicalPhrases(int[] startPositions, Pattern pattern, SymbolTable vocab);
	
	/**
	 * Gets all locations in the corpus 
	 * of the specified hierarchical pattern,
	 * subject to the specified span constraints.
	 * <p>
	 * This method exists to provide an easy mechanism
	 * for getting all instances of arbitrary hierarchical phrases.
	 * 
	 * @param pattern Pattern of terminals and (optionally) nonterminals.
	 * @param minNonterminalSpan Minimum number of terminals 
	 * 		that a nonterminal is allowed to represent
	 * @param maxPhraseSpan Maximum length in the corpus 
	 * 		that an extracted phrase may represent
	 * @return
	 */
	MatchedHierarchicalPhrases createHierarchicalPhrases(Pattern pattern, int minNonterminalSpan, int maxPhraseSpan);
	
	/**
	 * Returns the number of suffixes in the suffix array, which
	 * is identical to the length of the corpus.
	 * 
	 * @return the number of suffixes in the suffix array
	 */
	int size();
	
	/** 
	 * Gets the position in the corpus corresponding to the
	 * specified index in the suffix array.
	 * 
	 * @return the position in the corpus corresponding to the
	 *         specified index in the suffix array.
	 */
	int getCorpusIndex(int suffixIndex);
	
	/**
	 * Gets the sentence number of the word at the specified
	 * position in the corpus.
	 * 
	 * @param corpusIndex Position of a word in the corpus
	 * @return the sentence number of the word at the specified
	 *         position in the corpus
	 */
	int getSentenceIndex(int corpusIndex);
	
	/**
	 * Gets the position in the corpus of the first word of
	 *         the specified sentence.  If the sentenceID is
	 *         outside of the bounds of the sentences, then it
	 *         returns the last position in the corpus + 1.
	 * 
	 * @return the position in the corpus of the first word of
	 *         the specified sentence.  If the sentenceID is
	 *         outside of the bounds of the sentences, then it
	 *         returns the last position in the corpus + 1.
	 */
	int getSentencePosition(int sentenceIndex);
	
	/**
	 * Finds a phrase in the suffix array.
	 *
	 * @param phrase the search phrase
	 * @return a tuple containing the (inclusive) start and the
	 *         (inclusive) end bounds in the suffix array for
	 *         the phrase
	 */
	int[] findPhrase(Phrase phrase);
	
	/**
	 * Finds a phrase in the suffix array. The phrase is extracted
	 * from the sentence given the start and end points. This
	 * version of the method allows bounds to be specified in
	 * the suffix array, which is useful when searching for
	 * increasingly longer sub-phrases in a sentences.
	 *
	 * @param sentence    the sentence/super-phrase to draw the
	 *                    search phrase from
	 * @param phraseStart the start of the phrase in the sentence
	 *                    (inclusive)
	 * @param phraseEnd   the end of the phrase in the sentence
	 *                    (exclusive)
	 * @param lowerBound  the first index in the suffix array
	 *                    that will bound the search
	 * @param upperBound  the last index in the suffix array
	 *                    that will bound the search
	 * @return a tuple containing the (inclusive) start and the
	 *         (inclusive) end bounds in the suffix array for
	 *         the phrase, or null if the phrase is not found.
	 */
	int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd, int lowerBound, int upperBound);
	
	/**
	 * Gets a list of hierarchical phrases that match the pattern
	 * if they are already cached or null if the pattern is not
	 * in the cache.
	 * 
	 * @return a list of hierarchical phrases that match the
	 *         pattern if they are already cached or null if
	 *         the pattern is not in the cache.
	 */
	MatchedHierarchicalPhrases getMatchingPhrases(Pattern pattern);
	
	
	/** 
	 * Caches the matching hierarchical phrases for the pattern.
	 * 
	 * @param matchings Hierarchical phrases located in the corpus
	 * 		that match a common pattern.
	 */
	void cacheMatchingPhrases(MatchedHierarchicalPhrases matchings);
	
	/**
	 * Gets all of the positions in the corpus for the bounds
	 * in the suffix array, sorting the corpus position.
	 * 
	 * @param bounds Inclusive bounds in the suffix array
	 * @return all positions in the corpus for the specified
	 *         bounds
	 */
	int[] getAllPositions(int[] bounds);

	/**
	 * Gets the hierarchical phrase objects cached by this
	 * suffix array.
	 *
	 * @return the hierarchical phrase objects cached by this
	 *         suffix array
	 */
	Cache<Pattern,MatchedHierarchicalPhrases> getCachedHierarchicalPhrases();
	
	/**
	 * Gets the list of rule objects cached by this
	 * suffix array.
	 *
	 * @return the list of rule objects cached by this
	 *         suffix array
	 */
	Cache<Pattern,List<Rule>> getCachedRules();

}
