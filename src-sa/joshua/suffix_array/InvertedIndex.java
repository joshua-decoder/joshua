/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.suffix_array;

// Imports

import java.util.*;

import joshua.util.Cache;
import joshua.util.sentence.Phrase;



/**
 * InvertedIndex stores the positions in the corpus for phrases.  
 * Instead of returning positions of phrases that sorted lexographically
 * as the suffix array does, the InvertedIndex returns them sorted by their
 * position in the corpus.  This is necessary for the fast intersection 
 * algorithms used by Lopez (2007) when finding occurrences of discountinous 
 * phrases. 
 *
 * @author Chris Callison-Burch
 * @since  July 13, 2008
 */
public class InvertedIndex {

//===============================================================
// Constants
//===============================================================

	public static int MAX_PHRASE_LENGTH_INITIAL = 5;

//===============================================================
// Member variables
//===============================================================

	protected Cache<Pattern,List<HierarchicalPhrase>> matchingPhrases;

	protected SuffixArray suffixArray;
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * @param suffixArray the suffix array to index
	 * @param capacity the number of phrases to store in memory
	 * @param preloadMostFrequentItems a flag that tells whether to do a pre-pass 
	 *        to load the most frequent items into memory. 
	 */
	public InvertedIndex(SuffixArray suffixArray, int capacity, boolean preloadMostFrequentItems) {
		this.matchingPhrases = new Cache<Pattern,List<HierarchicalPhrase>>(capacity);
		this.suffixArray = suffixArray;
		if(preloadMostFrequentItems) {
			List<Phrase> phrases = new ArrayList<Phrase>(capacity);
			List<Integer> frequencies = new ArrayList<Integer>(capacity);
			int minFrequency = 2;
			suffixArray.getMostFrequentPhrases(phrases, frequencies, minFrequency, capacity, MAX_PHRASE_LENGTH_INITIAL);
			
			//Iterator<Phrase> it = phrases.iterator();
			//while(it.hasNext()) {
			//	Phrase phrase = it.next();
			for (Phrase phrase : phrases) {
				int[] boundsInSuffixArray = suffixArray.findPhrase(phrase);
				int[] positions = suffixArray.getAllPositions(boundsInSuffixArray);
				int length = phrase.size();
				Pattern pattern = new Pattern(phrase);
				List<HierarchicalPhrase> hierarchicalPhrases = getHierarchicalPhrases(positions, pattern);
				matchingPhrases.put(pattern, hierarchicalPhrases);
			}
		}
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public List<HierarchicalPhrase> getMatchingPhrases(Pattern pattern) {
		return matchingPhrases.get(pattern);
	}
		
	public void setMatchingPhrases(Pattern pattern, List<HierarchicalPhrase> matchings) {
		matchingPhrases.put(pattern, matchings);
	}
	
		
	/**
	 * This method creates a list of trivially HierarchicalPhrases (i.e. they're really just contiguous phrases, but we
	 * will want to perform some of the HierarchialPhrase operations on them).  Sorts the positions. 
	 * Adds the results to the cache. 
	 *
	 * @param pattern a contiguous phrase
	 * @param startPositions an unsorted list of the positions in the corpus where the matched phrases begin
	 * @return a list of trivially hierarchical phrases
	 */ 
	public List<HierarchicalPhrase> getHierarchicalPhrases(int[] startPositions, Pattern pattern) {
		if(startPositions == null) return Collections.emptyList();
		Arrays.sort(startPositions);
		int length = pattern.size();
		ArrayList<HierarchicalPhrase> hierarchicalPhrases = new ArrayList<HierarchicalPhrase>(startPositions.length);
		for(int i = 0; i < startPositions.length; i++) { 
			int[] position = {startPositions[i]};
			int[] endPosition = {startPositions[i] + length};
			HierarchicalPhrase hierarchicalPhrase = new HierarchicalPhrase(pattern, position, endPosition, suffixArray.corpus, length);
			hierarchicalPhrases.add(hierarchicalPhrase);
		}	
		matchingPhrases.put(pattern, hierarchicalPhrases);
		return hierarchicalPhrases;
	}
		
	
	//===========================================================
	// Methods
	//===========================================================


//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================


//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

	public static void main(String[] args)
	{

	}
}

