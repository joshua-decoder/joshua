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


/**
*
* 
* @author Lane Schwartz
*/
public class Matching {
	
	final int[] matchingIndices;
	final int suffixArrayLowerBound;
	final int suffixArrayUpperBound;
	final int sentenceNumber;
	
	
	public Matching(
		int[] matchingIndices,
		int suffixArrayLowerBound,
		int suffixArrayUpperBound,
		int sentenceNumber
	) {
		this.matchingIndices = matchingIndices;
		this.suffixArrayLowerBound = suffixArrayLowerBound;
		this.suffixArrayUpperBound = suffixArrayUpperBound;
		this.sentenceNumber = sentenceNumber;
	}
	
	
	/**
	 * Construct a new matching from two existing matchings.
	 * <p>
	 * This method does NOT perform a sanity check to verify
	 * that the sentence number of the prefix and suffix matchings
	 * are the same.
	 * <p>
	 * The sentence number of the new matching will be copied
	 * from the prefix matching.
	 * 
	 * @param prefixMatching
	 * @param suffixMatching
	 */
	/*public Matching(Matching prefixMatching, Matching suffixMatching, Phrase sentence, SuffixArray suffixArray) {
		
		//suffixArray.findPhrase(sentence, phraseStart, phraseEnd, prefixMatching.lowerBound, prefixMatching.upperBound);
		
		this.suffixArrayLowerBound = suffixArrayLowerBound;
		this.suffixArrayUpperBound = suffixArrayUpperBound;
		this.sentenceNumber = prefixMatching.sentenceNumber;
		
		this.matchingIndices = new int[prefixMatching.matchingIndices.length+1];
		for (int i=0; i<prefixMatching.matchingIndices.length; i++) {
			matchingIndices[i] = prefixMatching.matchingIndices[i];
		}
		matchingIndices[prefixMatching.matchingIndices.length] = suffixMatching.matchingIndices[suffixMatching.matchingIndices.length-1];
	}*/
	
	
	public int get(int i) {
		return matchingIndices[i];
	}
	
	
	public int getFirstIndex() {
		return matchingIndices[0];
	}
	
}
