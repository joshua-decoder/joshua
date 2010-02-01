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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.vocab.SymbolTable;

/**
 * HierarchicalPhrases represents a list of matched hierarchical
 * phrases.
 * <p>
 * 
 * TODO Add unit tests for this class.
 * 
 * @author Lane Schwartz 
 * @since Jan 9 2009
 * @version $LastChangedDate$
 */
public class HierarchicalPhrases extends AbstractHierarchicalPhrases {
	
	/**
	 * Represents all locations in the corpus that match the
	 * <code>pattern</code>.
	 * <p>
	 * Specifically, for each location in the corpus that matches
	 * the pattern, the corpus index of the of the first word
	 * in each terminal sequence is stored.
	 * <p>
	 * The length of this array should be 
	 * <code>size * terminalSequenceLengths.length</code>.
	 */
	final int[] terminalSequenceStartIndices;
	
	/**
	 * Represents the sentence numbers of each location in the
	 * corpus that matches the pattern.
	 * <p>
	 * To save memory, this variable could be deleted if the
	 * actual calculation of this data were moved from the
	 * constructor to the <code>getSentenceNumber</code> method.
	 */
	final int[] sentenceNumber;
	
	/** Logger for this class. */
	@SuppressWarnings("unused")
	private static final Logger logger = 
		Logger.getLogger(HierarchicalPhrases.class.getName());
	
	
	/**
	 * Constructs a list of hierarchical phrases.
	 * 
	 * @param pattern Pattern common to the list of phrases
	 * @param startPositions  Represents all locations in the
	 *            corpus that match the pattern. Specifically,
	 *            for each location in the corpus that matches
	 *            the pattern, the corpus index of the of the
	 *            first word in each terminal sequence is stored.
	 * @param sentenceNumbers Represents the sentence number
	 *            of each matched phrase location
	 */
	public HierarchicalPhrases(Pattern pattern, int[] startPositions, int[] sentenceNumbers) {
//		super(pattern, startPositions.length);
		super(pattern, 
				(pattern.getTerminalSequenceLengths().length>0) 
				? startPositions.length / pattern.getTerminalSequenceLengths().length 
				: 0);
//		this.size = sentenceNumbers.length;//sentenceNumbers.length;
		this.terminalSequenceStartIndices = startPositions;
//		this.sentenceNumber = new int[size];
		this.sentenceNumber = sentenceNumbers;
		publicCounter += 1;
	}
	
	public static int publicCounter = 0;
	public static int protectedCounter = 0; 
	public static int privateCounter = 0;
	public static int emptyListCounter = 0;
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		s.append(this.pattern.toString());
		s.append('\t');
		s.append(this.size());
		s.append(" locations");
		
		return s.toString();
	}
	
	/**
	 * Constructs a list of hierarchical phrases.
	 * 
	 * @param pattern Pattern common to the list of phrases
	 * @param terminalSequenceStartIndices  Represents all locations in the corpus
	 *                        that match the pattern. 
	 *                        Specifically, for each location in the corpus
	 *                        that matches the pattern, the corpus index of the
	 *                        of the first word in each terminal sequence is stored.
	 * @param sentenceNumbers Represents the sentence number 
	 *                        of each matched phrase location
	 */
	protected HierarchicalPhrases(Pattern pattern, 
			List<Integer> terminalSequenceStartIndices, 
			List<Integer> sentenceNumbers) {
		
//		super(pattern);
		super(pattern,
				(pattern.getTerminalSequenceLengths().length>0) 
				? terminalSequenceStartIndices.size() / pattern.getTerminalSequenceLengths().length 
				: 0);

//		int numberOfPhrases = ;
		
		int dataSize = terminalSequenceStartIndices.size();
//		this.size = (terminalSequenceLengths.length>0) ? dataSize / terminalSequenceLengths.length : 0;
		
		this.terminalSequenceStartIndices = new int[dataSize];
		for (int i=0; i<dataSize; i++) {
			this.terminalSequenceStartIndices[i] = terminalSequenceStartIndices.get(i);
		}

		this.sentenceNumber = new int[size];
		for (int i=0; i<size; i++) {
			this.sentenceNumber[i] = sentenceNumbers.get(i);
		}
		
		protectedCounter += 1;
//		this.size = size;	
	}
	
//	/* See Javadoc for MatchedHierarchicalPhrases interface. */
//	public int size() {
//		return size;
//	}
	
	/**
	 * Constructs a list of hierarchical phrases
	 * identical to the provided list of phrases,
	 * except that it uses the provided pattern.
	 * 
	 * @param pattern
	 * @param phrases
	 */
	private HierarchicalPhrases(Pattern pattern, HierarchicalPhrases phrases) {
		super(pattern, phrases.size);
//		super(pattern);
//		this.size = phrases.size;
		this.terminalSequenceStartIndices = phrases.terminalSequenceStartIndices;
		this.sentenceNumber = phrases.sentenceNumber;
		privateCounter += 1;
	}

	/**
	 * Gets an empty list of hierarchical phrases.
	 * 
	 * @param vocab Symbol table to associate with the list
	 * @return an empty list of hierarchical phrases
	 */
	public static HierarchicalPhrases emptyList(SymbolTable vocab, int... words) {	
		return emptyList(new Pattern(vocab, words));
	}
	
	/**
	 * Gets an empty list of hierarchical phrases.
	 * 
	 * @param vocab Symbol table to associate with the list
	 * @return an empty list of hierarchical phrases
	 */
	public static HierarchicalPhrases emptyList(Pattern pattern) {	
		emptyListCounter += 1;
		return new HierarchicalPhrases(
				pattern, 
				Collections.<Integer>emptyList(), 
				Collections.<Integer>emptyList()
			);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public MatchedHierarchicalPhrases copyWithInitialX() {
		return new HierarchicalPhrases(getPatternWithInitialX(), this);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public MatchedHierarchicalPhrases copyWithFinalX() {
		return new HierarchicalPhrases(getPatternWithFinalX(), this);
	}


	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getStartPosition(int phraseIndex, int positionNumber) {

		return terminalSequenceStartIndices[phraseIndex*(terminalSequenceLengths.length)+positionNumber];	
		
	}
	
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public boolean isEmpty() {
		return ! (size > 0);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getSentenceNumber(int phraseIndex) {
		
		return this.sentenceNumber[phraseIndex];
	}
	
}
