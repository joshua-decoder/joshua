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

import joshua.corpus.Corpus;
import joshua.corpus.LabeledSpan;
import joshua.corpus.Phrase;
import joshua.corpus.Span;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


/**
 * HierarchicalPhrase is a class that represents a single matched
 * hierarchical phrase, and provides the methods necessary for
 * accessing the Pattern that it matches and for computing the
 * intersection used in the suffix array lookups of discontinuous
 * phrases.
 * <p>
 * For efficiency and space savings, this class should only be used
 * when it is does not make sense to use MatchedHierarchicalPhrases.
 * 
 * In cases where many hierarchical phrases share a common pattern,
 * that class should be preferred, as it is able to store the phrases
 * in a much more memory-efficient manner compared to storing a
 * collection of HierarchicalPhrase objects.
 * 
 * @author Chris Callison-Burch and Lane Schwartz 
 * @since July 31 2008
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class HierarchicalPhrase extends Pattern {
	
	/**
	 * Represents one index into the source corpus for each
	 * terminal that starts a contiguous sequence of terminals.
	 */
	protected final int[] terminalSequenceStartIndices;
	
	
	/** Exclusive ending indices of each terminal sequence. */
	protected final int[] terminalSequenceEndIndices;
	
	
	/** Monolingual corpus from which this phrase was extracted. */
	protected final Corpus corpusArray;
	
	
	/**
	 * Index of the sentence in the corpus where this phrase
	 * was extracted.
	 */
	protected final int sentenceNumber;
	
	
	// We are NOT storing this, because of cases where the HP
	// starts with a nonterminal. In such cases, we want a
	// single HP to represent all HPs that start with that
	// nonterminal, and storing the startIndex would spuriously
	// require a list of HPs to store this.
	//
	//protected final int startIndex;
	
	
	/**
	 * Constructs a hierarchical phrase rooted at a location
	 * in a corpus.
	 * 
	 * @param patternWords Terminals and nonterminals comprising
	 *                     the pattern
	 * @param span         Span in the corpus from the first
	 *                     terminal  to the final terminal in the phrase
	 * @param nonterminalSpans Locations of all nonterminals
	 *                     in this phrase.
	 * @param corpus       Representation of a monolingual
	 *                     corpus.
	 */
	public HierarchicalPhrase(
			int[]             patternWords,
			Span              span,
			List<LabeledSpan> nonterminalSpans,
			Corpus corpus) {
		
		super(corpus.getVocabulary(), patternWords);
		
		// Compute the list of terminal spans 
		List<Span> terminalSpans = new ArrayList<Span>();
		{
			int possibleStart = span.start;

			int nonterminalIndex = 0;
			Span nonterminal = span;

			for (LabeledSpan labeledNTSpan : nonterminalSpans) {

				nonterminal = labeledNTSpan.getSpan();

				if (nonterminal.start > possibleStart) {
					terminalSpans.add(new Span(possibleStart, nonterminal.start));
				} 

				possibleStart = nonterminal.end;

				nonterminalIndex++;
			}

			if (span.end > possibleStart) {
				terminalSpans.add(new Span(possibleStart, span.end));
			}
		}

		// Initialize the sequence arrays
		this.terminalSequenceStartIndices = new int[terminalSpans.size()];
		this.terminalSequenceEndIndices = new int[terminalSpans.size()];
		{
			int terminalSpanNumber = 0;
			for (Span terminalSpan : terminalSpans) {
				terminalSequenceStartIndices[terminalSpanNumber] = terminalSpan.start;
				terminalSequenceEndIndices[terminalSpanNumber] = terminalSpan.end;
				terminalSpanNumber++;
			}
		}
		
		
		this.corpusArray = corpus;
		
		this.sentenceNumber = corpus.getSentenceIndex(terminalSequenceStartIndices[0]);
	}
	
	
	/**
	 * Returns the index in the corpus of the first terminal
	 * in this phrase.
	 * 
	 * @return the index in the corpus of the first terminal
	 *         in this phrase.
	 */
	public int getFirstTerminalCorpusIndex() {
		return terminalSequenceStartIndices[0];
	}
	
	
	/**
	 * Returns the index in the corpus of the last terminal in
	 * this phrase.
	 * 
	 * @return the index in the corpus of the last terminal in
	 *         this phrase.
	 */
	public int getLastTerminalCorpusIndex() {
		return terminalSequenceEndIndices[terminalSequenceEndIndices.length-1];
	}
	
	/**
	 * Gets whether this phrase contains a terminal at the
	 * specified index.
	 * 
	 * @return <code>true</code> this phrase contains a terminal
	 *         at the specified index <code>false</code> otherwise
	 */
	public boolean containsTerminalAt(int alignedPointIndex) {

		for (int i=0; i<terminalSequenceStartIndices.length; i++) {
			if (alignedPointIndex >= terminalSequenceStartIndices[i] &&
					alignedPointIndex < terminalSequenceEndIndices[i]) {
				return true;
			}
		}		
		
		return false;
	}
	
	
	/**
	 * Returns the index of the sentence in the corpus that the
	 * phrase is a part of.
	 * 
	 * @return the index of the sentence in the corpus that the
	 *         phrase is a part of.
	 */
	public int getSentenceNumber() {
		return sentenceNumber;
	}
	
	
	/**
	 * Gets all possible subphrases of this phrase, up to and
	 * including the phrase itself. For example, the phrase "I
	 * like cheese ." would return the following:
	 * <ul>
	 * <li>I
	 * <li>like
	 * <li>cheese
	 * <li>.
	 * <li>I like
	 * <li>like cheese
	 * <li>cheese .
	 * <li>I like cheese
	 * <li>like cheese .
	 * <li>I like cheese .
	 * </ul>
	 *
	 * @return ArrayList of all possible subphrases.
	 */
	public List<Phrase> getSubPhrases() {
		return getSubPhrases(size());
	}
	
	
	/**
	 * Returns a list of subphrases only of length
	 * <code>maxLength</code> or smaller.
	 *
	 * @param maxLength the maximum length phrase to return.
	 * @return ArrayList of all possible subphrases of length
	 *         maxLength or less
	 * @see #getSubPhrases()
	 */
	public List<Phrase> getSubPhrases(int maxLength) {
		if (maxLength > size()) {
			return getSubPhrases(size());
		}
		List<Phrase> phrases = new ArrayList<Phrase>();
		
		for (int i = 0; i < size(); i++) {
			for (int j = i + 1; (j <= size()) && (j - i <= maxLength); j++) {
				Phrase subPhrase = subPhrase(i,j);
				phrases.add(subPhrase);
			}
		}
		return phrases;
	}
	
	
	/**
	 * Returns a string representation of the phrase.
	 * 
	 * @return a space-delimited string of the terminals and
	 *         nonterminals in the phrase.
	 */
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		int[] wordIDs = getWordIDs();
		for (int i = 0; i < wordIDs.length; i++) {
			if (wordIDs[i]<0) {
				s.append('X');
			} else {
				s.append(corpusArray.getVocabulary().getWord(wordIDs[i]));
			}
			s.append(' ');
		}
		
		s.append(Arrays.toString(terminalSequenceStartIndices));
		
		return s.toString();
	}
	
	
	/**
	 * Gets the number of contiguous sequences of terminals
	 * in the pattern corresponding to this phrase.
	 * 
	 * @return the number of contiguous sequences of terminals
	 *         in the pattern corresponding to this phrase
	 */
	public int getNumberOfTerminalSequences() {
		return terminalSequenceStartIndices.length;
	}
	
	/**
	 * Gets the starting index in the corpus for a specified
	 * contiguous sequence of terminals in this phrase.
	 * 
	 * @param sequenceIndex Index specifying a contiguous sequence 
	 *                      of terminals in this pattern
	 * @return the starting index in the corpus for a specified
	 *         contiguous sequence of terminals in this phrase
	 */
	public int getTerminalSequenceStartIndex(int sequenceIndex) {
		return this.terminalSequenceStartIndices[sequenceIndex];
	}
	
	/**
	 * Gets the ending index in the corpus for a specified
	 * contiguous sequence of terminals in this phrase.
	 * 
	 * @param sequenceIndex Index specifying a contiguous sequence 
	 *                      of terminals in this pattern
	 * @return the ending index in the corpus for a specified
	 *         contiguous sequence of terminals in this phrase
	 */
	public int getTerminalSequenceEndIndex(int sequenceIndex) {
		
		return this.terminalSequenceEndIndices[sequenceIndex];
		
	}
	
	
	/**
	 * Indicates whether the first element of this phrase's
	 * pattern is a nonterminal.
	 *         
	 * @return <code>true</code> if this phrase starts with a
	 *         nonterminal, <code>false</code> otherwise.
	 */
	public boolean startsWithNonterminal() {
		return (words[0] < 0);
	}
	
	
	/**
	 * Indicates whether the last element of this phrase's
	 * pattern is a nonterminal.
	 *         
	 * @return <code>true</code> if this phrase ends with a
	 *         nonterminal, <code>false</code> otherwise.
	 */
	public boolean endsWithNonterminal() {
		return (words[words.length-1] < 0);
	}
	
	
	/**
	 * Creates a new phrase object from the indexes provided.
	 * <P>
	 * NOTE: subList merely creates a "view" of the existing
	 * Phrase object. Memory taken up by other Words in the
	 * Phrase is not freed since the underlying subList object
	 * still points to the complete Phrase List.
	 * <p>
	 * <em>This method is currently broken</em>, 
	 * and is guaranteed to throw an <code>Error</code>.
	 * 
	 * @see ArrayList#subList(int, int)
	 */
	public Phrase subPhrase(int start, int end) {
		
		// FIXME Lane This method should almost certainly return a HierarchicalPhrase in some cases
		
		//return new ContiguousPhrase(terminalSequenceStartIndices[0]+start, terminalSequenceStartIndices[0]+end, corpusArray);
		throw new Error("This code is broken. It needs to be fixed.");
	}
	
}
