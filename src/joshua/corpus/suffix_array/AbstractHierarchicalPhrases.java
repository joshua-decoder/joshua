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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Span;
import joshua.corpus.vocab.SymbolTable;

/**
 * Implements common algorithms used with hierarchical phrases.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public abstract class AbstractHierarchicalPhrases implements
		MatchedHierarchicalPhrases {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(AbstractHierarchicalPhrases.class.getName());
	
	/** 
	 * Represents a sequence of terminal and nonterminals as
	 * integer IDs. The pattern is <em>not</em> rooted to a
	 * location in a corpus.
	 */
	protected final Pattern pattern;
	
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
	protected final byte[] terminalSequenceLengths;
	
	
	/**
	 * Number of hierarchical phrases represented by this object.
	 */
	protected final int size;
	
	public static int counter = 0;
	
	/**
	 * Constructs an abstract object representing
	 * locations in a corpus that match the hierarchical phrase
	 * represented by the specified pattern. 
	 * 
	 * @param pattern Pattern representing a hierarchical phrase
	 */
	protected AbstractHierarchicalPhrases(Pattern pattern, int numPhrases) {
		this.pattern = pattern;
		this.terminalSequenceLengths = pattern.getTerminalSequenceLengths();
		this.size = numPhrases;
		counter++;
	}
		
	/**
	 * Implements the dotted operators (<̈, =̈, >̈)
	 * from Lopez (2008), p78-79.
	 * <p>
	 * This method behaves as follows when provided prefix
	 * phrase m_a_alpha and suffix phrase m_alpha_b:
	 * <ul>
	 * <li>Returns 0 if m_a_alpha and m_alpha_b can be paired.</li>
	 * <li>Returns -1 if m_a_alpha and m_alpha_b cannot be
	 *     paired, and m_a_alpha precedes m_alpha_b in the
	 *     corpus.</li>
	 * <li>Returns  1 if m_a_alpha and m_alpha_b cannot be
	 *     paired, and m_a_alpha follows m_alpha_b in the
	 *     corpus.</li>
	 * </ul>
	 * 
     * @param m_a_alpha List of prefix hierarchical phrases
	 * @param i Index into m_a_alpha
	 * @param m_alpha_b List of suffix hierarchical phrases
	 * @param j Index into m_alpha_b
	 * @param minNonterminalSpan Minimum allowed nonterminal span
	 * @param maxPhraseSpan Maximum allowed phrase span
	 * @return
	 * <ul>
	 * <li>0 if m_a_alpha and m_alpha_b can be paired (=̈).</li>
	 * <li>-1 if m_a_alpha and m_alpha_b cannot be paired, and
	 *     m_a_alpha precedes m_alpha_b in the corpus (<̈).</li>
	 * <li> 1 if m_a_alpha and m_alpha_b cannot be paired, and
	 *     m_a_alpha follows m_alpha_b in the corpus. (>̈)</li>
	 * </ul>
	 */	
	protected static int compare(
			MatchedHierarchicalPhrases m_a_alpha, final int i, 
			MatchedHierarchicalPhrases m_alpha_b, final int j, 
			int minNonterminalSpan, int maxPhraseSpan) {
	
		// Try the cheapest check first: Are they in the same sentence?
		{
			int m_a_alpha_i_sentenceNumber = m_a_alpha.getSentenceNumber(i);
			int m_alpha_b_j_sentenceNumber = m_alpha_b.getSentenceNumber(j);

			if (m_a_alpha_i_sentenceNumber < m_alpha_b_j_sentenceNumber) {
				return -1;
			} else if (m_a_alpha_i_sentenceNumber > m_alpha_b_j_sentenceNumber) {
				return 1;
			}
		}

		
		int prefixStartPosition = m_a_alpha.getStartPosition(i, 0);
		int suffixStartPosition = m_alpha_b.getStartPosition(j, 0);
		
		if (prefixStartPosition > suffixStartPosition) {
			return 1;
		} else if (prefixStartPosition <= suffixStartPosition-maxPhraseSpan) {
			return -1;
		} else {

			// If we get to this point, we know:
			//
			// * prefix and suffix are in the same sentence
			// * prefix occurs before suffix in the sentence
			// * prefix and suffix are within maxPhraseSpan of each other
			
			boolean m_a_alpha_endsWithNonterminal = m_a_alpha.endsWithNonterminal();
			boolean m_alpha_b_startsWithNonterminal = m_alpha_b.startsWithNonterminal();
			
			// Does the prefix (m_a_alpha) overlap with
			//      the suffix (m_alpha_b) on any words?
			if (m_a_alpha_endsWithNonterminal && 
					m_alpha_b_startsWithNonterminal && 
					m_a_alpha.arity()==1 &&
					m_alpha_b.arity()==1 &&
					m_a_alpha.getTerminalSequenceLength(0)==1 &&
					m_alpha_b.getTerminalSequenceLength(0)==1) {
				
				return 0;
			
			} else {
				
				int m_a_alphaTerminalSequenceLengths = m_a_alpha.getNumberOfTerminalSequences();//.terminalSequenceLengths.length;
				int m_alpha_bTerminalSequenceLengths = m_alpha_b.getNumberOfTerminalSequences();//.terminalSequenceLengths.length;
				
				int m_alpha_b_prefix_start = j*m_alpha_bTerminalSequenceLengths;
				int m_alpha_b_prefix_end;

				boolean m_a_alpha_startsWithNonterminal = m_a_alpha.startsWithNonterminal();
				boolean m_alpha_b_endsWithNonterminal = m_alpha_b.endsWithNonterminal();		
				
				// If the m_alpha_b pattern ends with a nonterminal
				if (m_alpha_b_endsWithNonterminal || 
						// ...or if the m_alpha_b pattern ends with two terminals
						m_alpha_b.endsWithTwoTerminals()) {

					m_alpha_b_prefix_end = m_alpha_b_prefix_start + m_alpha_bTerminalSequenceLengths;

				} else { // Then the m_alpha_b pattern ends with a nonterminal followed by a terminal

					m_alpha_b_prefix_end = m_alpha_b_prefix_start + m_alpha_bTerminalSequenceLengths - 1;

				}

				int m_a_alpha_suffix_start;
				int m_a_alpha_suffix_end;
				boolean increment_m_a_alpha_suffix_start;

				int m_a_alphaExtra;

				// If the m_a_alpha pattern starts with a nonterminal
				if (m_a_alpha_startsWithNonterminal) { 
					m_a_alphaExtra = 0;
					m_a_alpha_suffix_start = i*m_a_alphaTerminalSequenceLengths;
					m_a_alpha_suffix_end = m_a_alpha_suffix_start + m_a_alphaTerminalSequenceLengths;
					increment_m_a_alpha_suffix_start = false;
				} else if (m_a_alpha.secondTokenIsTerminal()) {
					// Then the m_a_alpha pattern starts with two terminals
					m_a_alphaExtra = 0;
					m_a_alpha_suffix_start = i*m_a_alphaTerminalSequenceLengths;
					m_a_alpha_suffix_end = m_a_alpha_suffix_start + m_a_alphaTerminalSequenceLengths;

					increment_m_a_alpha_suffix_start = true;
				} else {
					// Then the m_a_alpha pattern starts with a terminal followed by a nonterminal
					m_a_alphaExtra = 1;
					m_a_alpha_suffix_start = i*m_a_alphaTerminalSequenceLengths + 1;
					m_a_alpha_suffix_end = i*m_a_alphaTerminalSequenceLengths + m_a_alphaTerminalSequenceLengths;

					increment_m_a_alpha_suffix_start = false;
				}

				int m_a_alpha_suffix_length = m_a_alpha_suffix_end - m_a_alpha_suffix_start;
				int m_alpha_b_prefix_length = m_alpha_b_prefix_end - m_alpha_b_prefix_start;

				if (m_alpha_b_prefix_length != m_a_alpha_suffix_length) {
					throw new MismatchedHierarchicalPhrasesException();
				} else {

					int result = 0;

					for (int index=0; index<m_a_alpha_suffix_length; index++) {

						int a = m_a_alpha.getStartPosition(i, index+m_a_alphaExtra);
						if (increment_m_a_alpha_suffix_start && index==0) {
							a++;
						}
						int b = m_alpha_b.getStartPosition(j, index);

						if (a > b) {
							result = 1;
							break;
						} else if (a < b) {
							result = -1;
							break;
						}
					}

					if (result==0) {
						int positionNumber = m_alpha_bTerminalSequenceLengths-1;
						int length = m_alpha_b.getStartPosition(j, positionNumber) + m_alpha_b.getTerminalSequenceLength(positionNumber) - prefixStartPosition;

						if (m_alpha_b_endsWithNonterminal)
							length += minNonterminalSpan;
						if (m_a_alpha_startsWithNonterminal)
							length += minNonterminalSpan;

						if (length > maxPhraseSpan) {
							result = -1;
						}
					}

					return result;
				}

			}
		}
	}
	
	/**
	 * Constructs the data to represent the hierarchical phrase,
	 * formed by intersecting the <code>i<code>th phrase of
	 * <code>M_a_alpha</code> with the <code>j<code>th phrase
	 * of <code>M_alpha_b</code> and appends this new data to
	 * the <code>data</code> list.
	 * 
	 * @param M_a_alpha List of prefix hierarchical phrases
	 * @param i Index into M_a_alpha
	 * @param M_alpha_b List of suffix hierarchical phrases
	 * @param j Index into M_alpha_b
	 * @param list List where new data will be added
	 */
	protected static void partiallyConstruct(
			MatchedHierarchicalPhrases M_a_alpha, int i, 
			MatchedHierarchicalPhrases M_alpha_b, int j, 
			List<Integer> list) {
		
		
		boolean prefixEndsWithNonterminal = M_a_alpha.endsWithNonterminal();
		
		// Get all start positions for the prefix phrase, and append them to the running list
		{
			int numTerminalSequences = M_a_alpha.getNumberOfTerminalSequences();
			
			for (int index=0; index<numTerminalSequences; index++) {
				list.add(M_a_alpha.getStartPosition(i, index));
			}
			
		}
		
		
		if (prefixEndsWithNonterminal) {
			// Get the final start positions for the suffix phrase, and append it to the running list
			int index = M_alpha_b.getNumberOfTerminalSequences() - 1;
			list.add(M_alpha_b.getStartPosition(j, index));
		} 
		
	}

	/**
	 * Implements the <tt>QUERY_INTERSECT</tt> algorithm from
	 * Adam Lopez's thesis (Lopez 2008). This implementation
	 * follows a corrected algorithm (Lopez, personal communication).
	 * 
	 * @param pattern Pattern which will be associated with the new list
	 *                of matched hierarchical phrases
	 * @param M_a_alpha Prefix list of matched hierarchical phrases
	 * @param M_alpha_b Suffix list of matched hierarchical phrases
	 * @param minNonterminalSpan Minimum allowed span for a nonterminal
	 * @param maxPhraseSpan Maximum allowed phrase span
	 * @return The list of matched hierarchical phrases resulting from
	 *         the intersection of the two provided lists 
	 *         of matched hierarchical phrases
	 */
	public static MatchedHierarchicalPhrases queryIntersect(Pattern pattern, 
			MatchedHierarchicalPhrases M_a_alpha, 
			MatchedHierarchicalPhrases M_alpha_b, 
			int minNonterminalSpan, int maxPhraseSpan, Suffixes sourceSuffixArray) {

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("queryIntersect("+pattern+" M_a_alpha.size=="+M_a_alpha.size() + ", M_alpha_b.size=="+M_alpha_b.size());			
		}
		
		if (sourceSuffixArray!=null && sourceSuffixArray.getCachedHierarchicalPhrases().containsKey(pattern)) {
			return sourceSuffixArray.getCachedHierarchicalPhrases().get(pattern);
		} else {

			// results is M_{a_alpha_b} in the paper
			ArrayList<Integer> data = new ArrayList<Integer>();
			ArrayList<Integer> sentenceNumbers = new ArrayList<Integer>();

			int I = M_a_alpha.size();
			int J = M_alpha_b.size();

			int i = 0;
			int j = 0;

			while (i<I && j<J) {

				while (j<J && compare(M_a_alpha, i, M_alpha_b, j, minNonterminalSpan, maxPhraseSpan) > 0) {
					j++; // advance j past no longer needed item in M_alpha_b
				}

				int k = i;					

				// Process all matchings in M_a_alpha with same first element
				int kth_startPosition = M_a_alpha.getStartPosition(k, 0);
				while (i<I && M_a_alpha.getStartPosition(i, 0) == kth_startPosition) {

					int l = j;

					// While not M_a_alpha[i] <̈ M_alpha_b[l]
					if (l < J) {
						int comparison = compare(M_a_alpha, i, M_alpha_b, l,  minNonterminalSpan, maxPhraseSpan);
						while (l < J && !(comparison < 0)) {

							// If M_a_alpha[i] =̈ M_alpha_b[l]
							if (comparison == 0) {

								// Append M_a_alpha[i] |><| M_alpha_b[l] to M_a_alpha_b
								partiallyConstruct(M_a_alpha, i, M_alpha_b, l, data);
								sentenceNumbers.add(M_a_alpha.getSentenceNumber(i));

							} // end if

							// We can visit m_alpha_b[l] again, but only next time through outermost loop
							l = l + 1;
							if (l < J) {
								comparison = compare(M_a_alpha, i, M_alpha_b, l,  minNonterminalSpan, maxPhraseSpan);
							}

						} // end while
					} // end if

					// advance i past no longer needed item in M_a_alpha
					i = i + 1;

				} // end while

			} // end while

			//		if (sourceSuffixArray==null) {
			return new HierarchicalPhrases(pattern, data, sentenceNumbers);
			//		} else {
			//			int[] startPositions = new int[data.size()];
			//			for (int index=0, n=data.size(); index<n; index++) {
			//				startPositions[index] = data.get(index);
			//			}
			//			
			//			return sourceSuffixArray.createHierarchicalPhrases(startPositions, pattern, sourceSuffixArray.getVocabulary());			
			//		}

		}		
	}

	/* See Javadoc for MatchedHierarchicalPhrase interface. */
	public int getTerminalSequenceLength(int i) {
		return terminalSequenceLengths[i];
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getNumberOfTerminalSequences() {
		return terminalSequenceLengths.length;
	}
	
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
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getEndPosition(int phraseIndex, int positionNumber) {
		
		return getStartPosition(phraseIndex, positionNumber) + getTerminalSequenceLength(positionNumber);
				
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getTerminalSequenceStartIndex(int phraseIndex, int sequenceIndex) {
//		int n = terminalSequenceLengths.length;
//		int nthPhraseIndex = phraseIndex*n;
		
		int start = this.getStartPosition(phraseIndex, sequenceIndex);//this.terminalSequenceStartIndices[nthPhraseIndex+sequenceIndex];
		return start;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getTerminalSequenceEndIndex(int phraseIndex, int sequenceIndex) {
//		int n = terminalSequenceLengths.length;
//		int nthPhraseIndex = phraseIndex*n;
		
		int start = this.getStartPosition(phraseIndex, sequenceIndex);//this.terminalSequenceStartIndices[nthPhraseIndex+sequenceIndex];
		int end = start + this.terminalSequenceLengths[sequenceIndex];
		
		return end;
	}
	

	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getFirstTerminalIndex(int phraseIndex) {
//		int n = terminalSequenceLengths.length;
//		int nthPhraseIndex = phraseIndex*n;
		int index = 0;
		
		int start = this.getStartPosition(phraseIndex, index);//this.terminalSequenceStartIndices[nthPhraseIndex+index];
		return start;
	}
	
	
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public boolean containsTerminalAt(int phraseIndex,
			int alignedPointIndex) {
		
		int n = terminalSequenceLengths.length;
//		int nthPhraseIndex = phraseIndex*n;
		
		for (int index=0; index<n; index++) {
			int start = this.getStartPosition(phraseIndex, index);//this.terminalSequenceStartIndices[nthPhraseIndex+index];
			if (alignedPointIndex >= start &&
					alignedPointIndex < start + this.terminalSequenceLengths[index]) {
				return true;
			}
		}		
		
		return false;

	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int getLastTerminalIndex(int phraseIndex) {
		int n = terminalSequenceLengths.length;
		int index = n-1;
		
		int start = getStartPosition(phraseIndex, index);
		int end = start + this.terminalSequenceLengths[n-1];
		
		return end;
		
	}
	
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public Span getSpan(int phraseIndex) {
		
		int n = terminalSequenceLengths.length;
//		int nthPhraseIndex = phraseIndex*n;
		
		int lastIndex = n-1;
		
		int start = this.getStartPosition(phraseIndex, 0);//this.terminalSequenceStartIndices[nthPhraseIndex+0];
		int lastStart = this.getStartPosition(phraseIndex, lastIndex);//this.terminalSequenceStartIndices[nthPhraseIndex+lastIndex];
		int lastLength = this.terminalSequenceLengths[lastIndex];
		int end = lastStart + lastLength;		
		
		return new Span(start, end);
	}
	
	/**
	 * Gets the number of nonterminals in this object's pattern.
	 * 
	 * @return the number of nonterminals
	 */
	public int arity() {
		return pattern.arity;
	}
	
	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public Pattern getPattern() {
		return this.pattern;
	}

	/* See Javadoc for MatchedHierarchicalPhrases interface. */
	public int size() {
		return size;
	}
	
	public boolean equals(Object o) {
		if (o instanceof AbstractHierarchicalPhrases) {
			AbstractHierarchicalPhrases other = (AbstractHierarchicalPhrases) o;
			
			if (this.getPattern().equals(other.getPattern()) 
					&& this.size()==other.size()
					&& this.arity()==other.arity() 
					&& this.getNumberOfTerminalSequences() == other.getNumberOfTerminalSequences()
					&& this.endsWithNonterminal()==other.endsWithNonterminal()
					&& this.startsWithNonterminal()==other.startsWithNonterminal()
					&& this.endsWithTwoTerminals()==other.endsWithTwoTerminals()
					&& this.secondTokenIsTerminal()==other.secondTokenIsTerminal()) {
			
				int n = getNumberOfTerminalSequences();
				for (int i=0, size=this.size(); i<size; i++) {
					for (int seq=0; seq<n; seq++) {
						if (this.getStartPosition(i, seq) != other.getStartPosition(i, seq) ||
								this.getEndPosition(i, seq) != other.getEndPosition(i, seq)) {
							return false;
						}
					}
				}
				
				return true;
			} else {
				return false;
			}
			
		} else {
			return false;
		}
	}
	
	
	protected static Pattern getPatternWithInitialX(Pattern pattern) {
		int[] xwords = new int[pattern.words.length+1];
		xwords[0] = SymbolTable.X;
		for (int i=0; i<pattern.words.length; i++) {
			xwords[i+1] = pattern.words[i];
		}
		return new Pattern(pattern.vocab, xwords);
	}
	
	protected Pattern getPatternWithInitialX() {
		return getPatternWithInitialX(pattern);
	}
	
	protected Pattern getPatternWithFinalX() {
		return new Pattern(pattern.vocab, pattern.words, SymbolTable.X);
	}
}
