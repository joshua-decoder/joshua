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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements common algorithms used with hierarchical phrases.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public abstract class AbstractHierarchicalPhrases implements
		MatchedHierarchicalPhrases {

	
	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(AbstractHierarchicalPhrases.class.getName());
	
	/**
	 * Implements the dotted operators (<̈, =̈, >̈) from Lopez (2008), p78-79.
	 * <p>
	 * This method behaves as follows when provided prefix phrase m_a_alpha and suffix phrase m_alpha_b:
	 * <ul>
	 * <li>Returns 0 if m_a_alpha and m_alpha_b can be paired.</li>
	 * <li>Returns -1 if m_a_alpha and m_alpha_b cannot be paired, and m_a_alpha precedes m_alpha_b in the corpus.</li>
	 * <li>Returns  1 if m_a_alpha and m_alpha_b cannot be paired, and m_a_alpha follows m_alpha_b in the corpus.</li>
	 * </ul>
	 * 
	 * @param m_a_alpha Prefix phrase
	 * @param m_alpha_b Suffix phrase
	 * @return
	 * <ul>
	 * <li>0 if m_a_alpha and m_alpha_b can be paired.</li>
	 * <li>-1 if m_a_alpha and m_alpha_b cannot be paired, and m_a_alpha precedes m_alpha_b in the corpus.</li>
	 * <li> 1 if m_a_alpha and m_alpha_b cannot be paired, and m_a_alpha follows m_alpha_b in the corpus.</li>
	 * </ul>
	 */	
	protected static int compare(MatchedHierarchicalPhrases m_a_alpha, final int i, MatchedHierarchicalPhrases m_alpha_b, final int j, int minNonterminalSpan, int maxPhraseSpan) {
	
		int m_a_alphaTerminalSequenceLengths = m_a_alpha.getNumberOfTerminalSequences();//.terminalSequenceLengths.length;
		int m_alpha_bTerminalSequenceLengths = m_alpha_b.getNumberOfTerminalSequences();//.terminalSequenceLengths.length;
		
		boolean m_a_alpha_startsWithNonterminal = m_a_alpha.patternStartsWithNonterminal();
		boolean m_a_alpha_endsWithNonterminal = m_a_alpha.patternEndsWithNonterminal();
		boolean m_alpha_b_startsWithNonterminal = m_alpha_b.patternStartsWithNonterminal();
		boolean m_alpha_b_endsWithNonterminal = m_alpha_b.patternEndsWithNonterminal();		
		
		// Does the prefix (m_a_alpha) overlap with
		//      the suffix (m_alpha_b) on any words?
		boolean matchesOverlap;
		// we assume that the nonterminal symbols will be denoted with negative numbers
		if (m_a_alpha_endsWithNonterminal && 
				m_alpha_b_startsWithNonterminal && 
				m_a_alpha.getArity()==1 &&
				m_alpha_b.getArity()==1 &&
				m_a_alpha.getTerminalSequenceLength(0)==1 &&
				m_alpha_b.getTerminalSequenceLength(0)==1)
			matchesOverlap = false;
		else
			matchesOverlap = true;

		int prefixStartPosition = m_a_alpha.getStartPosition(i, 0);
		int suffixStartPosition = m_alpha_b.getStartPosition(j, 0);
		
		
		if (matchesOverlap) {

			int m_alpha_b_prefix_start = j*m_alpha_bTerminalSequenceLengths;
			int m_alpha_b_prefix_end;

			// If the m_alpha_b pattern ends with a nonterminal
			if (m_alpha_b_endsWithNonterminal || 
					// ...or if the m_alpha_b pattern ends with two terminals
					m_alpha_b.patternEndsWithTwoTerminals()) {

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
			} else if (m_a_alpha.patternSecondTokenIsTerminal()) {
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
				throw new RuntimeException("Length of s(m_a_alpha) and p(m_alpha_b) do not match");
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
		else {

			if (m_a_alpha.getSentenceNumber(i) < m_alpha_b.getSentenceNumber(j))
				return -1;
			else if (m_a_alpha.getSentenceNumber(i) > m_alpha_b.getSentenceNumber(j))
				return 1;
			else {

				if (prefixStartPosition >= suffixStartPosition-1)
					return 1;
				else if (prefixStartPosition <= suffixStartPosition-maxPhraseSpan)
					return -1;
				else
					return 0;
			}

		}
	}
	
	/**
	 * Constructs the data to represent the hierarchical phrase, 
	 * formed by intersecting the <code>i<code>th phrase of <code>M_a_alpha</code>
	 * with the <code>j<code>th phrase of <code>M_alpha_b</code>
	 * and appends this new data to the <code>data</code> list.
	 * 
	 * @param pattern Pattern for the new hierarchical phrase
	 * @param M_a_alpha List of prefix hierarchical phrases
	 * @param i Index into M_a_alpha
	 * @param M_alpha_b List of suffix hierarchical phrases
	 * @param j Index into M_alpha_b
	 * @param list List where new data will be added
	 */
//	protected static void partiallyConstruct(Pattern pattern, MatchedHierarchicalPhrases M_a_alpha, int i, MatchedHierarchicalPhrases M_alpha_b, int j, List<Integer> list) {
	protected static void partiallyConstruct(MatchedHierarchicalPhrases M_a_alpha, int i, MatchedHierarchicalPhrases M_alpha_b, int j, List<Integer> list) {
		boolean prefixEndsWithNonterminal = M_a_alpha.patternEndsWithNonterminal();
		
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
	 * Implements the QUERY_INTERSECT algorithm from Adam Lopez's thesis (Lopez 2008).
	 * This implementation follows a corrected algorithm (Lopez, personal communication).
	 * 
	 * @param pattern
	 * @param M_a_alpha
	 * @param M_alpha_b
	 * @return
	 */
	public static MatchedHierarchicalPhrases queryIntersect(Pattern pattern, MatchedHierarchicalPhrases M_a_alpha, MatchedHierarchicalPhrases M_alpha_b, int minNonterminalSpan, int maxPhraseSpan) {

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("queryIntersect("+pattern+" M_a_alpha.size=="+M_a_alpha.size() + ", M_alpha_b.size=="+M_alpha_b.size());			
		}
		
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

			if (j>=J) break;
			
			int l = j;					
			
			// Process all matchings in M_alpha_b with same first element
			ProcessMatchings:
			for (int jth_StartPosition = M_alpha_b.getStartPosition(j, 0),
					 lth_StartPosition = M_alpha_b.getStartPosition(l, 0);
					
					jth_StartPosition == lth_StartPosition;
					
					jth_StartPosition = M_alpha_b.getStartPosition(j, 0),
					lth_StartPosition = M_alpha_b.getStartPosition(l, 0)) {
				
				int compare_i_l = compare(M_a_alpha, i, M_alpha_b, l,  minNonterminalSpan, maxPhraseSpan);
				while (compare_i_l >= 0) {
					
					if (compare_i_l == 0) {
						
						// append M_a_alpha[i] |><| M_alpha_b[l] to M_a_alpha_b
						partiallyConstruct(M_a_alpha, i, M_alpha_b, l, data);
						sentenceNumbers.add(M_a_alpha.getSentenceNumber(i));
						
					} // end if
					
					l++; // we can visit m_alpha_b[l] again, but only next time through outermost loop
					
					if (l < J) {
						compare_i_l = compare(M_a_alpha, i, M_alpha_b, l,  minNonterminalSpan, maxPhraseSpan);
					} else {
						i++;
						break ProcessMatchings;
					}
					
				} // end while
				
				i++; // advance i past no longer needed item in M_a_alpha
				
				if (i >= I) break;
				
			} // end while
			
		} // end while
		
		return new HierarchicalPhrases(pattern, data, sentenceNumbers);
		
	}
	
}
