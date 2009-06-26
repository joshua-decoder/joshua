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

import joshua.corpus.MatchedHierarchicalPhrases;

/**
 *
 *
 * @author Lane Schwartz
 */
public class FrequentMatchedHierarchicalPhrases extends
		AbstractHierarchicalPhrases {

	private final FrequentMatches frequentMatches;
	
	
	public FrequentMatchedHierarchicalPhrases(Pattern pattern, FrequentMatches frequentMatches) {
		super(pattern, frequentMatches.getMatchCount(pattern));
//		super(pattern);
		
		this.frequentMatches = frequentMatches;
	}


	/* @see joshua.corpus.MatchedHierarchicalPhrases#copyWithFinalX() */
	public MatchedHierarchicalPhrases copyWithFinalX() {
		// TODO Auto-generated method stub
		return null;
	}

	/* @see joshua.corpus.MatchedHierarchicalPhrases#copyWithInitialX() */
	public MatchedHierarchicalPhrases copyWithInitialX() {
		// TODO Auto-generated method stub
		return null;
	}



	
	/* @see joshua.corpus.MatchedHierarchicalPhrases#getSentenceNumber(int) */
	public int getSentenceNumber(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}



	
	
	
	
	
	
	/* @see joshua.corpus.MatchedHierarchicalPhrases#getStartPosition(int, int) */
	public int getStartPosition(int phraseIndex, int positionNumber) {
		
		return frequentMatches.getStartPosition(pattern, phraseIndex, positionNumber);
		
	}
	
	/* @see joshua.corpus.MatchedHierarchicalPhrases#isEmpty() */
	public boolean isEmpty() {
		return ! frequentMatches.contains(pattern);
	}

}
