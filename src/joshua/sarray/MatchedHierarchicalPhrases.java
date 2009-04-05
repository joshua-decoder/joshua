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

import joshua.util.sentence.Span;

/**
 * Represents a list of matched hierarchical phrases.
 * 
 * @author Lane Schwartz 
 * @since Apr 4 2009
 * @version $LastChangedDate$
 */
public interface MatchedHierarchicalPhrases {

	/**
	 * Gets the number of contiguous sequences of terminals
	 * in the pattern represented by this object.
	 * <p>
	 * The result returned by this method should always 
	 * equals <code>getArity()+1</code>.
	 * 
	 * @return The number of contiguous sequences of terminals
	 * in the pattern represented by this object.
	 */
	public int getNumberOfTerminalSequences();
	
	/**
	 * 
	 * @param phraseIndex
	 * @param positionNumber
	 * @return
	 */
	public int getStartPosition(int phraseIndex, int positionNumber);
	
	/**
	 * 
	 * @param phraseIndex
	 * @param positionNumber
	 * @return
	 */
	public int getEndPosition(int phraseIndex, int positionNumber);

	
	/**
	 * Gets the number of locations in the corpus 
	 * that match the pattern.
	 * 
	 * @return The number of locations in the corpus 
	 * that match the pattern.
	 */
	public int size();
	
	/**
	 * Tests if this list has no matches in the corpus.
	 * 
	 * @return <code>true</code> if this list has no matches in the corpus,
	 *         <code>false</code> otherwise.
	 */
	public boolean isEmpty();
	
	/** 
	 * Gets the index of the sentence from which the specified phrase was extracted.
	 *  
	 * @param phraseIndex Index of a phrase
	 * @return the index of the sentence from which the specified phrase was extracted.
	 */
	public int getSentenceNumber(int phraseIndex);
	
	/**
	 * Gets the number of terminal tokens in the specified terminal sequence.
	 * 
	 * @param i Index of a terminal sequence in this object's pattern.
	 * @return the number of terminal tokens in the specified terminal sequence
	 */
	public int getTerminalSequenceLength(int i);
	
	/**
	 * Constructs a new object exactly the same as this object
	 * (specifically, it contains the exact same list of corpus matches),
	 * but appends or prepends the nonterminal X to the pattern.
	 * 
	 * @param initial If <code>true</code> X is prepended to the pattern;
	 *                else X is appended to the pattern.
	 * @return list of matched phrases identical with updated pattern
	 */
	public MatchedHierarchicalPhrases copyWithX(boolean initial);
	
	
	
	
	////////////
	
	
	public boolean patternEndsWithNonterminal();
	public boolean patternStartsWithNonterminal();
	public boolean patternEndsWithTwoTerminals();
	public boolean patternSecondTokenIsTerminal();
	public int getArity();
	public Span getSpan(int phraseIndex);
	public boolean containsTerminalAt(int phraseIndex, int alignmentPointIndex);
	public int getFirstTerminalIndex(int phraseIndex);
	public int getLastTerminalIndex(int phraseIndex);
	public int getTerminalSequenceStartIndex(int phraseIndex, int sequenceIndex);
	public int getTerminalSequenceEndIndex(int phraseIndex, int sequenceIndex);
}
