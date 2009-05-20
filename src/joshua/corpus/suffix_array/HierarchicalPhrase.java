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

import joshua.corpus.ContiguousPhrase;
import joshua.corpus.Corpus;
import joshua.corpus.LabeledSpan;
import joshua.corpus.Phrase;
import joshua.corpus.Span;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


/**
 * HierarchicalPhrase is a class that represents a single matched hierarchical
 * phrase, and provides the methods necessary for accessing the
 * Pattern that it matches and for computing the intersection used
 * in the suffix array lookups of discontinuous phrases.
 * <p>
 * For efficiency and space savings, this class should only be used 
 * when it is does not make sense to use MatchedHierarchicalPhrases.
 * 
 * In cases where many hierarchical phrases share a common pattern,
 * that class should be preferred, as it is able to store the phrases
 * in a much more memory-efficient manner compared to storing a collection
 * of HierarchicalPhrase objects.
 * 
 * @author Chris Callison-Burch and Lane Schwartz 
 * @since July 31 2008
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class HierarchicalPhrase extends Pattern {

	/** 
	 * Represents a sequence of terminal and nonterminals as
	 * integer IDs. The pattern is <em>not</em> rooted to a
	 * location in a corpus.
	 */
//	protected final Pattern pattern;
	
	
	/**
	 * Represents one index into the source corpus for each
	 * terminal that starts a contiguous sequence of terminals.
	 */
	protected final int[] terminalSequenceStartIndices;
	
	
	/**
	 * Exclusive ending indices of each terminal sequence.
	 */
	protected final int[] terminalSequenceEndIndices;
	
	
	/** */
	protected final Corpus corpusArray;
	
	
//	/**
//	 * the length of the matching phrase from the first terminal
//	 * to the last terminal; not the pattern; this also means
//	 * the length is invalid if the HP starts or ends with a
//	 * nonterminal.
//	 */
//	protected final int length;
	
	
	/** */
	protected final int sentenceNumber;
	
	
	// We are NOT storing this, because of cases where the HP
	// starts with a nonterminal. In such cases, we want a
	// single HP to represent all HPs that start with that
	// nonterminal, and storing the startIndex would spuriously
	// require a list of HPs to store this.
	
	//protected final int startIndex;
	
	
	/**
	 * Constructs a hierarchical phrase from a pattern and
	 * indices into a corpus.
	 * <p>
	 * A pattern does not contain any indices into an actual
	 * corpus. For each sequence of terminals in the pattern,
	 * an index into the corpus is required.
	 * 
	 * @param pattern     pattern of terminals and nonterminals
	 * @param corpusIndicesOfTerminalSequences the starting
	 *                    indices (in the corpus) of each
	 *                    contiguous sequence of terminals
	 * @param corpusArray integer array representation of a
	 *                    corpus
	 * @param length      the length (in the corpus) of the
	 *                    matched hierarchical phrase; this is
	 *                    <em>not</em> the same as the length
	 *                    of the pattern
	 */
	public HierarchicalPhrase(
			int[]       words,
			int[]       terminalSequenceStartIndices,
			int[]       terminalSequenceEndIndices,
			Corpus corpusArray,
			int         length) {
		
		super(corpusArray.getVocabulary(), words);
		
		this.terminalSequenceStartIndices = terminalSequenceStartIndices;
		this.terminalSequenceEndIndices = terminalSequenceEndIndices;
		this.corpusArray = corpusArray;
//		this.length = length;
		
		this.sentenceNumber = corpusArray.getSentenceIndex(terminalSequenceStartIndices[0]);
	}
		
	public HierarchicalPhrase(
			int[]             words,
			Span              span,
			List<LabeledSpan> nonterminalSpans,
			Corpus corpusArray) {
		
		super(corpusArray.getVocabulary(), words);
		
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
				terminalSpans.add(new Span(possibleStart, span.start));
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
			}
		}
		
		
		this.corpusArray = corpusArray;
//		this.length = super.size();//span.size();
		
		this.sentenceNumber = corpusArray.getSentenceIndex(terminalSequenceStartIndices[0]);
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
	 * Gets whether this phrase contains a terminal at the specified index.
	 * 
	 * @return <code>true</code> this phrase contains a terminal at the specified index
	 *         <code>false</code> otherwise
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
	
	
//	/**
//	 * Returns the vocabulary from which the words in this
//	 * phrase are drawn.
//	 * 
//	 * @return the vocabulary from which the words in this
//	 *         phrase are drawn.
//	 */
//	public SymbolTable getVocab() {
//		return corpusArray.getVocabulary();
//	}
	
	
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
	
	
//	/**
//	 * Returns the integer word id of the terminal or nonterminal
//	 * at the specified position in the phrase.
//	 * <p>
//	 * This method will throw an exception if the phrase starts
//	 * with a nonterminal.
//	 * 
//	 * @param position Index of a word in this phrase.
//	 * @throws UnableToGetWordIDException if the phrase starts with a nonterminal
//	 * @return the integer word id of the terminal or nonterminal
//	 *         at the specified position in this phrase.
//	 */
//	public int getWordID(int position) {
//		
//		if (startsWithNonterminal()) {
//			
//			// Ideally, a hierarchical phrase is a matched pattern.
//			//
//			// In this hypothetical ideal case, we would store the start position
//			//    in the corpus of the matched pattern.
//			// 
//			// If we had the start position in the corpus,
//			//    we could always determine the underlying word in the corpus
//			//    that is found at startPosition + position
//			//
//			// In cases where the pattern starts with a nonterminal,
//			//    if we stored the start position, 
//			//    we would have a list of hierarchical phrases 
//			//    which differed ONLY by start position.
//			//
//			// That is undesirable because it wastes memory.
//			//
//			// Since we do NOT store the start position,
//			//    this method is ill-defined when the hierarchical phrase
//			//    starts with a nonterminal.
//			//
//			// It may be that this method is never actually called.
//			//
//			// To help determine this, we throw an exception here.
//			//
//			// --Lane Schwartz
//			
//			throw new UnableToGetWordIDException("Getting the word ID of a hierarchical phrase that starts with a nonterminal is not defined");
//			
//		} else {
//			
//			return corpusArray.getWordID(terminalSequenceStartIndices[0]+position);
//			
//		}
//	}
	
	
	public static class UnableToGetWordIDException extends RuntimeException {
		public UnableToGetWordIDException(String message) {
			super(message);
		}
	}
	
	
//	/**
//	 * Returns the combined number of terminals and nonterminals
//	 * in this phrase.
//	 * 
//	 * @return the combined number of terminals and nonterminals
//	 *         in this phrase.
//	 */
//	public int size() {
//		return length;
//	}
	
	
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
	 *
	 * @see ArrayList#subList(int, int)
	 */
	public Phrase subPhrase(int start, int end) {
		//XXX lane Should this only return contiguous phrases?
		return new ContiguousPhrase(terminalSequenceStartIndices[0]+start, terminalSequenceStartIndices[0]+end, corpusArray);
	}
	
}
