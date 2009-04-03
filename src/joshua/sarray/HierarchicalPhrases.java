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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.SymbolTable;

/**
 * HierarchicalPhrases represents a list of matched hierarchical phrases.
 * <p>
 * 
 * TODO Add unit tests for this class.
 * 
 * @author Lane Schwartz 
 * @since Jan 9 2009
 * @version $LastChangedDate$
 */
public class HierarchicalPhrases extends AbstractHierarchicalPhrases implements MatchedHierarchicalPhrases, Externalizable {

	/** 
	 * Represents a sequence of terminal and nonterminals as
	 * integer IDs. The pattern is <em>not</em> rooted to a
	 * location in a corpus.
	 */
	private final Pattern pattern;

	/** 
	 * Represents the length of each 
	 * contiguous sequence of terminals in the pattern. 
	 * <p>
	 * To save memory, this information is stored as 
	 * bytes instead of integers. 
	 * 
	 * This means that the maximum value that can be
	 * stored here is 127. This should not be a problem
	 * unless a very large value is used for maximum phrase length.
	 */
	private final byte[] terminalSequenceLengths;
	
	
	/** Number of hierarchical phrases represented by this object. */
	private final int size;
	
	/**
	 * Represents all locations in the corpus
	 * that match the <code>pattern</code>.
	 * <p>
	 * Specifically, for each location in the corpus
	 * that matches the pattern, the corpus index of the
	 * of the first word in each terminal sequence is stored.
	 * <p>
	 * The length of this array should be 
	 * <code>size * terminalSequenceLengths.length</code>.
	 */
	private final int[] terminalSequenceStartIndices;
	
	/**
	 * Represents the sentence numbers of each location 
	 * in the corpus that matches the pattern.
	 * <p>
	 * To save memory, this variable could be deleted 
	 * if the actual calculation of this data were moved 
	 * from the constructor to the <code>getSentenceNumber</code> method.
	 */
	private final int[] sentenceNumber;
	
	// der aoeu X mann snth nth ouad
	// 7 8 13 16 78 79 81 84 
	
	/** Logger for this class. */
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(HierarchicalPhrases.class.getName());
	
	
	/**
	 * Constructs a list of hierarchical phrases.
	 * 
	 * @param pattern 
	 * @param startPositions Represents all locations in the corpus
	 * that match the pattern. Specifically, for each location in the corpus
	 * that matches the pattern, the corpus index of the
	 * of the first word in each terminal sequence is stored.
	 * @param prefixTree Prefix tree with which this object is associated
	 */
	public HierarchicalPhrases(Pattern pattern, int[] startPositions, Corpus corpus) {
		this.pattern = pattern;
		this.size = startPositions.length;
		this.terminalSequenceStartIndices = startPositions;
		this.sentenceNumber = new int[size];
		for (int i=0; i<size; i++) {
			this.sentenceNumber[i] = corpus.getSentenceIndex(startPositions[i]);
		}
		this.terminalSequenceLengths = pattern.getTerminalSequenceLengths();
		//
//		this.startsWithNonterminal = pattern.startsWithNonterminal();
//		this.endsWithNonterminal = pattern.endsWithNonterminal();
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
	
	public MatchedHierarchicalPhrases copyWith(Pattern pattern) {
		return new HierarchicalPhrases(pattern, this);
	}
	
	protected HierarchicalPhrases(Pattern pattern, List<Integer> data, List<Integer> sentenceNumbers) {
		this.pattern = pattern;
		this.terminalSequenceLengths = pattern.getTerminalSequenceLengths();
		
		int dataSize = data.size();
		int numberOfPhrases = (terminalSequenceLengths.length>0) ? data.size() / terminalSequenceLengths.length : 0;
		
		
		this.terminalSequenceStartIndices = new int[dataSize];
		for (int i=0; i<dataSize; i++) {
			this.terminalSequenceStartIndices[i] = data.get(i);
		}

		this.sentenceNumber = new int[numberOfPhrases];
		for (int i=0; i<numberOfPhrases; i++) {
			this.sentenceNumber[i] = sentenceNumbers.get(i);
		}
		
		this.size = numberOfPhrases;
		
		//
//		this.startsWithNonterminal = pattern.startsWithNonterminal();
//		this.endsWithNonterminal = pattern.endsWithNonterminal();
	}

	
	public int getNumberOfTerminalSequences() {
		return terminalSequenceLengths.length;
	}
	
	/**
	 * Gets the indicated hierarchical phrase from this collection.
	 * 
	 * @deprecated
	 */
	public HierarchicalPhrase get(int phraseIndex, Corpus corpus) {
		
		int n = terminalSequenceLengths.length;
		
		int[] terminalSequenceStartIndices = new int[n];
		int[] terminalSequenceEndIndices = new int[n];
		
		int nthPhraseIndex = phraseIndex*n;
		for (int index=0; index<n; index++) {
			terminalSequenceStartIndices[index] = this.terminalSequenceStartIndices[nthPhraseIndex+index];
			terminalSequenceEndIndices[index] = this.terminalSequenceStartIndices[nthPhraseIndex+index] + this.terminalSequenceLengths[index];
		}
		
		int length = terminalSequenceEndIndices[n-1] - terminalSequenceStartIndices[0];
		
		return new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, corpus, length);
	}
	

	
	/**
	 * 
	 * @param phraseIndex
	 * @param positionNumber
	 * @return
	 */
	public int getStartPosition(int phraseIndex, int positionNumber) {

		return terminalSequenceStartIndices[phraseIndex*(terminalSequenceLengths.length)+positionNumber];	
		
	}
	
	/**
	 * 
	 * @param phraseIndex
	 * @param positionNumber
	 * @return
	 */
	public int getEndPosition(int phraseIndex, int positionNumber) {
		
		return terminalSequenceStartIndices[phraseIndex*(terminalSequenceLengths.length)+positionNumber] + terminalSequenceLengths[positionNumber];
				
	}

	
	/**
	 * Gets the number of locations in the corpus 
	 * that match the pattern.
	 * 
	 * @return The number of locations in the corpus 
	 * that match the pattern.
	 */
	public int size() {
		return size;
	}
	
	
	public boolean isEmpty() {
		if (size > 0)
			return false;
		else
			return true;
	}
	
	public static HierarchicalPhrases emptyList(SymbolTable vocab) {
//		SymbolTable vocab = (prefixTree.suffixArray==null) ? null : prefixTree.suffixArray.getVocabulary();
		
		return new HierarchicalPhrases(new Pattern(vocab), Collections.<Integer>emptyList(), Collections.<Integer>emptyList());
	}

	public int getSentenceNumber(int phraseIndex) {
		return this.sentenceNumber[phraseIndex];
	}

//	public Pattern getPattern() {
//		return this.pattern;
//	}

	public int getTerminalSequenceLength(int i) {
		return this.terminalSequenceLengths[i];
	}

	//////
	
//	private final boolean endsWithNonterminal;
//	private final boolean startsWithNonterminal;
	
	public boolean patternEndsWithNonterminal() {
		return pattern.endsWithNonterminal();
//		return endsWithNonterminal;
	}
	
	public boolean patternStartsWithNonterminal() {
		return pattern.startsWithNonterminal();
//		return startsWithNonterminal;
	}
	
	public boolean patternEndsWithTwoTerminals() {
		return pattern.endsWithTwoTerminals();
	}
	
	public boolean patternSecondTokenIsTerminal() {
		return pattern.secondTokenIsTerminal();
	}
	
	public int getArity() {
		return pattern.arity;
	}
	
	///
	
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		throw new RuntimeException("Not implemented");
		
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		//TODO Finish and test this method
		
		
		// Write the pattern
		int[] words = pattern.getWords();
		out.writeInt(words.length);
		for (int token : pattern.getWords()) {
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
