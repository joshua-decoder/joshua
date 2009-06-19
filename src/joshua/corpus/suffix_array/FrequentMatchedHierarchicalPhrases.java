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
import joshua.corpus.Span;

/**
 *
 *
 * @author Lane Schwartz
 */
public class FrequentMatchedHierarchicalPhrases extends
		AbstractHierarchicalPhrases {

	private final FrequentMatches frequentMatches;
	private final Pattern pattern;
	
	public FrequentMatchedHierarchicalPhrases(Pattern pattern, FrequentMatches frequentMatches) {
		this.pattern = pattern;
		this.frequentMatches = frequentMatches;
	}
	
	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#containsTerminalAt(int, int)
	 */
	public boolean containsTerminalAt(int phraseIndex, int alignmentPointIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#copyWithFinalX()
	 */
	public MatchedHierarchicalPhrases copyWithFinalX() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#copyWithInitialX()
	 */
	public MatchedHierarchicalPhrases copyWithInitialX() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getEndPosition(int, int)
	 */
	public int getEndPosition(int phraseIndex, int positionNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getFirstTerminalIndex(int)
	 */
	public int getFirstTerminalIndex(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getLastTerminalIndex(int)
	 */
	public int getLastTerminalIndex(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getNumberOfTerminalSequences()
	 */
	public int getNumberOfTerminalSequences() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* @see joshua.corpus.MatchedHierarchicalPhrases#getPattern() */
	public Pattern getPattern() {
		return pattern;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getSentenceNumber(int)
	 */
	public int getSentenceNumber(int phraseIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getSpan(int)
	 */
	public Span getSpan(int phraseIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getStartPosition(int, int)
	 */
	public int getStartPosition(int phraseIndex, int positionNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getTerminalSequenceEndIndex(int, int)
	 */
	public int getTerminalSequenceEndIndex(int phraseIndex, int sequenceIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getTerminalSequenceLength(int)
	 */
	public int getTerminalSequenceLength(int i) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#getTerminalSequenceStartIndex(int, int)
	 */
	public int getTerminalSequenceStartIndex(int phraseIndex, int sequenceIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#isEmpty()
	 */
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see joshua.corpus.MatchedHierarchicalPhrases#size()
	 */
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}



}
