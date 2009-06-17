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

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Span;
import joshua.corpus.vocab.SymbolTable;
import joshua.prefix_tree.PrefixTree;

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
	 * Represents a sequence of terminal and nonterminals as
	 * integer IDs. The pattern is <em>not</em> rooted to a
	 * location in a corpus.
	 */
	final Pattern pattern;

	/** 
	 * Represents the length of each contiguous sequence of
	 * terminals in the pattern.
	 * <p>
	 * To save memory, this information is stored as bytes
	 * instead of integers.
	 * 
	 * This means that the maximum value that can be stored
	 * here is 127. This should not be a problem unless a very
	 * large value is used for maximum phrase length.
	 */
	private final byte[] terminalSequenceLengths;
	
	
	/**
	 * Number of hierarchical phrases represented by this object.
	 */
	private final int size;
	
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
	private final int[] terminalSequenceStartIndices;
	
	/**
	 * Represents the sentence numbers of each location in the
	 * corpus that matches the pattern.
	 * <p>
	 * To save memory, this variable could be deleted if the
	 * actual calculation of this data were moved from the
	 * constructor to the <code>getSentenceNumber</code> method.
	 */
	private final int[] sentenceNumber;
	
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
		this.pattern = pattern;
		this.size = sentenceNumbers.length;
		this.terminalSequenceStartIndices = startPositions;
		this.sentenceNumber = sentenceNumbers;
		this.terminalSequenceLengths = pattern.getTerminalSequenceLengths();
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
		
		this.pattern = pattern;
		this.terminalSequenceLengths = pattern.getTerminalSequenceLengths();
		
		int dataSize = terminalSequenceStartIndices.size();
		int numberOfPhrases = (terminalSequenceLengths.length>0) ? terminalSequenceStartIndices.size() / terminalSequenceLengths.length : 0;
		
		this.terminalSequenceStartIndices = new int[dataSize];
		for (int i=0; i<dataSize; i++) {
			this.terminalSequenceStartIndices[i] = terminalSequenceStartIndices.get(i);
		}

		this.sentenceNumber = new int[numberOfPhrases];
		for (int i=0; i<numberOfPhrases; i++) {
			this.sentenceNumber[i] = sentenceNumbers.get(i);
		}
		
		this.size = numberOfPhrases;	
	}
	
	/**
	 * Constructs a list of hierarchical phrases
	 * identical to the provided list of phrases,
	 * except that it uses the provided pattern.
	 * 
	 * @param pattern
	 * @param phrases
	 */
	private HierarchicalPhrases(Pattern pattern, HierarchicalPhrases phrases) {
		this.pattern = pattern;
		this.size = phrases.size;
		this.terminalSequenceStartIndices = phrases.terminalSequenceStartIndices;
		this.terminalSequenceLengths = phrases.terminalSequenceLengths;
		this.sentenceNumber = phrases.sentenceNumber;
	}

	/**
	 * Gets an empty list of hierarchical phrases.
	 * 
	 * @param vocab Symbol table to associate with the list
	 * @return an empty list of hierarchical phrases
	 */
	public static HierarchicalPhrases emptyList(SymbolTable vocab, int... words) {		
		return new HierarchicalPhrases(
				new Pattern(vocab, words), 
				Collections.<Integer>emptyList(), 
				Collections.<Integer>emptyList()
			);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public Pattern getPattern() {
		return this.pattern;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public MatchedHierarchicalPhrases copyWithInitialX() {
		int[] xwords = new int[pattern.words.length+1];
		xwords[0] = PrefixTree.X;
		for (int i=0; i<pattern.words.length; i++) {
			xwords[i+1] = pattern.words[i];
		}
		Pattern xpattern = new Pattern(pattern.vocab, xwords);
		return new HierarchicalPhrases(xpattern, this);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public MatchedHierarchicalPhrases copyWithFinalX() {
		return new HierarchicalPhrases(new Pattern(pattern.vocab, pattern.words, PrefixTree.X), this);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getNumberOfTerminalSequences() {
		return terminalSequenceLengths.length;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getTerminalSequenceStartIndex(int phraseIndex, int sequenceIndex) {
		int n = terminalSequenceLengths.length;
		int nthPhraseIndex = phraseIndex*n;
		
		int start = this.terminalSequenceStartIndices[nthPhraseIndex+sequenceIndex];
		return start;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getTerminalSequenceEndIndex(int phraseIndex, int sequenceIndex) {
		int n = terminalSequenceLengths.length;
		int nthPhraseIndex = phraseIndex*n;
		
		int start = this.terminalSequenceStartIndices[nthPhraseIndex+sequenceIndex];
		int end = start + this.terminalSequenceLengths[sequenceIndex];
		
		return end;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getFirstTerminalIndex(int phraseIndex) {
		int n = terminalSequenceLengths.length;
		int nthPhraseIndex = phraseIndex*n;
		int index = 0;
		
		int start = this.terminalSequenceStartIndices[nthPhraseIndex+index];
		return start;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getLastTerminalIndex(int phraseIndex) {
		int n = terminalSequenceLengths.length;
		int nthPhraseIndex = phraseIndex*n;
		int index = n-1;
		
		int start = this.terminalSequenceStartIndices[nthPhraseIndex+index];
		int end = start + this.terminalSequenceLengths[n-1];
		
		return end;
		
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public boolean containsTerminalAt(int phraseIndex,
			int alignedPointIndex) {
		
		int n = terminalSequenceLengths.length;
		int nthPhraseIndex = phraseIndex*n;
		
		for (int index=0; index<n; index++) {
			int start = this.terminalSequenceStartIndices[nthPhraseIndex+index];
			if (alignedPointIndex >= start &&
					alignedPointIndex < start + this.terminalSequenceLengths[index]) {
				return true;
			}
		}		
		
		return false;

	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public Span getSpan(int phraseIndex) {
		
		int n = terminalSequenceLengths.length;
		int nthPhraseIndex = phraseIndex*n;
		
		int lastIndex = n-1;
		
		int start = this.terminalSequenceStartIndices[nthPhraseIndex+0];
		int lastStart = this.terminalSequenceStartIndices[nthPhraseIndex+lastIndex];
		int lastLength = this.terminalSequenceLengths[lastIndex];
		int end = lastStart + lastLength;		
		
		return new Span(start, end);
	}
	
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getStartPosition(int phraseIndex, int positionNumber) {

		return terminalSequenceStartIndices[phraseIndex*(terminalSequenceLengths.length)+positionNumber];	
		
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getEndPosition(int phraseIndex, int positionNumber) {
		
		return terminalSequenceStartIndices[phraseIndex*(terminalSequenceLengths.length)+positionNumber] + terminalSequenceLengths[positionNumber];
				
	}

	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int size() {
		return size;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public boolean isEmpty() {
		return ! (size > 0);
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getSentenceNumber(int phraseIndex) {
		return this.sentenceNumber[phraseIndex];
	}


	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getTerminalSequenceLength(int i) {
		return this.terminalSequenceLengths[i];
	}

	//////
	
	/* See Javadoc for PatternFormat interface. */
	public boolean endsWithNonterminal() {
		return pattern.endsWithNonterminal();
	}
	
	/* See Javadoc for PatternFormat interface. */
	public boolean startsWithNonterminal() {
		return pattern.startsWithNonterminal();
	}
	
	/* See Javadoc for PatternFormat interface. */
	public boolean endsWithTwoTerminals() {
		return pattern.endsWithTwoTerminals();
	}
	
	/* See Javadoc for PatternFormat interface. */
	public boolean secondTokenIsTerminal() {
		return pattern.secondTokenIsTerminal();
	}
	
	/**
	 * Gets the number of nonterminals in this object's pattern.
	 * 
	 * @return the number of nonterminals
	 */
	public int arity() {
		return pattern.arity;
	}
	

	public void writeExternal(ObjectOutput out) throws IOException {
		//TODO Finish and test this method
		
		
		// Write the pattern
		int[] words = pattern.getWordIDs();
		out.writeInt(words.length);
		for (int token : pattern.getWordIDs()) {
			out.writeInt(token);
		}
		out.writeInt(pattern.arity());
		
		out.writeInt(terminalSequenceLengths.length);
		for (int l : terminalSequenceLengths) {
			out.writeInt(l);
		}
		
		// Write the number of corpus matches
		out.writeInt(size);
		
		// Next, write the sentence numbers
		// There should be size of these
		for (int n : sentenceNumber) {
			out.writeInt(n);
		}
		
		// Next, write the start index of each corpus match
		// There should be size of these
		for (int startIndex : terminalSequenceStartIndices) {
			out.writeInt(startIndex);
		}
		
		
	}
	
}
