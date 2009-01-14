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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.util.sentence.Vocabulary;

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
public class HierarchicalPhrases {

	/** 
	 * Represents a sequence of terminal and nonterminals as
	 * integer IDs. The pattern is <em>not</em> rooted to a
	 * location in a corpus.
	 */
	private final Pattern pattern;

	/** 
	 * Represents the length of each 
	 * contiguous sequence of terminals in the pattern. 
	 */
	private final int[] terminalSequenceLengths;
	
	
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
	
	/**
	 * Prefix tree for which this object was created.
	 * <p>
	 * This reference is needed primarily to get access to 
	 * <code>minNonterminalSpan</code> and <code>maxPhraseSpan</code>. 
	 */
	private final PrefixTree prefixTree;
	
	// der aoeu X mann snth nth ouad
	// 7 8 13 16 78 79 81 84 
	
	/** Logger for this class. */
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
	public HierarchicalPhrases(Pattern pattern, int[] startPositions, PrefixTree prefixTree) {
		this.pattern = pattern;
		this.size = startPositions.length;
		this.terminalSequenceStartIndices = startPositions;
		this.sentenceNumber = new int[size];
		for (int i=0; i<size; i++) {
			this.sentenceNumber[i] = prefixTree.suffixArray.corpus.getSentenceIndex(startPositions[i]);
		}
		this.terminalSequenceLengths = pattern.getTerminalSequenceLengths();
		this.prefixTree = prefixTree;
	}
	
	/**
	 * Constructs a list of hierarchical phrases
	 * identical to the provided list of phrases,
	 * except that it uses the provided pattern.
	 * 
	 * @param pattern
	 * @param phrases
	 */
	public HierarchicalPhrases(Pattern pattern, HierarchicalPhrases phrases) {
		this.pattern = pattern;
		this.size = phrases.size;
		this.terminalSequenceStartIndices = phrases.terminalSequenceStartIndices;
		this.terminalSequenceLengths = phrases.terminalSequenceLengths;
		this.sentenceNumber = phrases.sentenceNumber;
		this.prefixTree = phrases.prefixTree;
	}
	
	protected HierarchicalPhrases(Pattern pattern, List<Integer> data, List<Integer> sentenceNumbers, PrefixTree prefixTree) {
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
		
		this.prefixTree = prefixTree;
		this.size = numberOfPhrases;
	}

	
	public int getNumberOfTerminalSequences() {
		return terminalSequenceLengths.length;
	}
	
	public HierarchicalPhrase get(int phraseIndex) {
		
		int n = terminalSequenceLengths.length;
		
		int[] terminalSequenceStartIndices = new int[n];
		int[] terminalSequenceEndIndices = new int[n];
		
		for (int index=0; index<n; index++) {
			terminalSequenceStartIndices[index] = getStartPosition(phraseIndex, index);
			terminalSequenceEndIndices[index] = getEndPosition(phraseIndex, index);
		}
		
		int length = terminalSequenceEndIndices[n-1] - terminalSequenceStartIndices[0];
		
		return new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, prefixTree.suffixArray.corpus, length);
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
	protected static void partiallyConstruct(Pattern pattern, HierarchicalPhrases M_a_alpha, int i, HierarchicalPhrases M_alpha_b, int j, List<Integer> list) {
		
		boolean prefixEndsWithNonterminal = M_a_alpha.pattern.endsWithNonterminal();
		
		// Get all start positions for the prefix phrase, and append them to the running list
//		M_a_alpha.extractStartPositions(i, list);
		{
			int inclusiveStart = i*M_a_alpha.terminalSequenceLengths.length;//(1+M_a_alpha.pattern.arity);
			int exclusiveEnd = inclusiveStart + M_a_alpha.terminalSequenceLengths.length;//(1+M_a_alpha.pattern.arity);
			
			for (int index=inclusiveStart; index<exclusiveEnd; index++) {
				list.add(M_a_alpha.terminalSequenceStartIndices[index]);
			}
		}
		
		
		if (prefixEndsWithNonterminal) {
//			M_alpha_b.extractFinalStartPosition(j, list);		
			// Get the final start positions for the suffix phrase, and append it to the running list
//			int index = j*(1+M_alpha_b.pattern.arity) + M_alpha_b.pattern.arity;
			int index = j*M_alpha_b.terminalSequenceLengths.length + (M_alpha_b.terminalSequenceLengths.length - 1);
			list.add(M_alpha_b.terminalSequenceStartIndices[index]);	
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
	static HierarchicalPhrases queryIntersect(Pattern pattern, HierarchicalPhrases M_a_alpha, HierarchicalPhrases M_alpha_b) {

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("queryIntersect("+pattern+" M_a_alpha.size=="+M_a_alpha.size() + ", M_alpha_b.size=="+M_alpha_b.size());			
		}
		
		// results is M_{a_alpha_b} in the paper
//		ArrayList<HierarchicalPhrase> results = new ArrayList<HierarchicalPhrase>();
		ArrayList<Integer> data = new ArrayList<Integer>();
		ArrayList<Integer> sentenceNumbers = new ArrayList<Integer>();
		
		int I = M_a_alpha.size();
		int J = M_alpha_b.size();

		int i = 0;
		int j = 0;

		while (i<I && j<J) {

//			HierarchicalPhrase m_a_alpha, m_alpha_b;
//			m_a_alpha = M_a_alpha.get(i);
//			m_alpha_b = M_alpha_b.get(j);
			
			while (j<J && compare(M_a_alpha, i, M_alpha_b, j) > 0) {
				j++; // advance j past no longer needed item in M_alpha_b
				//m_alpha_b = M_alpha_b.get(j);
			}

			if (j>=J) break;
			
			//int k = i;
			int l = j;
			
			// Process all matchings in M_alpha_b with same first element
			ProcessMatchings:
			while (M_alpha_b.getStartPosition(j, 0) == M_alpha_b.getStartPosition(l, 0)) {
				
				int compare_i_l = compare(M_a_alpha, i, M_alpha_b, l);
				while (compare_i_l >= 0) {
					
					if (compare_i_l == 0) {
						
						// append M_a_alpha[i] |><| M_alpha_b[l] to M_a_alpha_b
//						results.add(new HierarchicalPhrase(pattern, M_a_alpha.get(i), M_alpha_b.get(l)));
						HierarchicalPhrases.partiallyConstruct(pattern, M_a_alpha, i, M_alpha_b, l, data);
						sentenceNumbers.add(M_a_alpha.sentenceNumber[i]);
						
					} // end if
					
					l++; // we can visit m_alpha_b[l] again, but only next time through outermost loop
					
					if (l < J) {
						compare_i_l = compare(M_a_alpha, i, M_alpha_b, l);
					} else {
						i++;
						break ProcessMatchings;
					}
					
				} // end while
				
				i++; // advance i past no longer needed item in M_a_alpha
				
				if (i >= I) break;
				
			} // end while
			
		} // end while
		
		return new HierarchicalPhrases(pattern, data, sentenceNumbers, M_a_alpha.prefixTree);
		
	}
	
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
	static int compare(HierarchicalPhrases m_a_alpha, int i, HierarchicalPhrases m_alpha_b, int j) {
//		if (true) 
//			throw new RuntimeException();
		
		// Does the prefix (m_a_alpha) overlap with
		//      the suffix (m_alpha_b) on any words?
		boolean matchesOverlap;
		if (m_a_alpha.pattern.endsWithNonterminal() && 
				m_alpha_b.pattern.startsWithNonterminal() &&
				//m_a_alpha.terminalSequenceStartIndices.length==1 &&
				m_a_alpha.pattern.arity==1 &&
				//m_alpha_b.terminalSequenceStartIndices.length==1 &&
				m_alpha_b.pattern.arity==1 &&
				//m_a_alpha.terminalSequenceEndIndices[0]-m_a_alpha.terminalSequenceStartIndices[0]==1 &&
				//m_a_alpha.getEndPosition(i, 0) - m_a_alpha.getStartPosition(i, 0) == 1 &&
				m_a_alpha.terminalSequenceLengths[0] == 1 &&
				//m_alpha_b.terminalSequenceEndIndices[0]-m_alpha_b.terminalSequenceStartIndices[0]==1) 
				//m_alpha_b.getEndPosition(j, 0) - m_alpha_b.getStartPosition(j, 0) == 1)
				m_alpha_b.terminalSequenceLengths[0] == 1 )
			matchesOverlap = false;
		else
			matchesOverlap = true;
		
		if (matchesOverlap) {
			//return overlapping.compare(m_a_alpha, m_alpha_b);
//			int[] m_alpha_b_prefix;
			
			int m_alpha_b_prefix_start = j*m_alpha_b.terminalSequenceLengths.length;//(1+m_alpha_b.pattern.arity);
			int m_alpha_b_prefix_end;

			// If the m_alpha_b pattern ends with a nonterminal
			if (m_alpha_b.pattern.endsWithNonterminal() ||
					// ...or if the m_alpha_b pattern ends with two terminals
					m_alpha_b.pattern.words[m_alpha_b.pattern.words.length-2] >= 0) {
				
//				m_alpha_b_prefix = m_alpha_b.terminalSequenceStartIndices;
				m_alpha_b_prefix_end = m_alpha_b_prefix_start + m_alpha_b.terminalSequenceLengths.length;//m_alpha_b.pattern.arity;
				
			} else { // Then the m_alpha_b pattern ends with a nonterminal followed by a terminal
				
				m_alpha_b_prefix_end = m_alpha_b_prefix_start + m_alpha_b.terminalSequenceLengths.length - 1;//m_alpha_b.pattern.arity - 1;
				
//				int size = m_alpha_b.terminalSequenceStartIndices.length-1;
//				m_alpha_b_prefix = new int[size];
//				for (int index=0; index<size; index++) {
//					m_alpha_b_prefix[index] = m_alpha_b.terminalSequenceStartIndices[index];
//				}
			}
			
//			int[] m_a_alpha_suffix;
			int m_a_alpha_suffix_start;// = i*(1+m_a_alpha.pattern.arity);
			int m_a_alpha_suffix_end;
			boolean increment_m_a_alpha_suffix_start;
			
			// If the m_a_alpha pattern starts with a nonterminal
			if (m_a_alpha.pattern.startsWithNonterminal()) {
//				m_a_alpha_suffix = m_a_alpha.terminalSequenceStartIndices;	
				m_a_alpha_suffix_start = i*m_a_alpha.terminalSequenceLengths.length;//(1+m_a_alpha.pattern.arity);
				m_a_alpha_suffix_end = m_a_alpha_suffix_start + m_a_alpha.terminalSequenceLengths.length;//m_a_alpha.pattern.arity;
				increment_m_a_alpha_suffix_start = false;
			} else if (m_a_alpha.pattern.words[1] >= 0) { 
				// Then the m_a_alpha pattern starts with two terminals
				
				m_a_alpha_suffix_start = i*m_a_alpha.terminalSequenceLengths.length;//(1+m_a_alpha.pattern.arity);
				m_a_alpha_suffix_end = m_a_alpha_suffix_start + m_a_alpha.terminalSequenceLengths.length;//m_a_alpha.pattern.arity;
				
//				int size = m_a_alpha.terminalSequenceStartIndices.length;
//				m_a_alpha_suffix = new int[size];
//				for (int index=0; index<size; index++) {
//					m_a_alpha_suffix[index] = m_a_alpha.terminalSequenceStartIndices[index];
//				}
//				m_a_alpha_suffix[0]++;
				increment_m_a_alpha_suffix_start = true;
			} else {
				// Then the m_a_alpha pattern starts with a terminal followed by a nonterminal
				
				m_a_alpha_suffix_start = i*m_a_alpha.terminalSequenceLengths.length + 1;
				//m_a_alpha_suffix_end = m_a_alpha_suffix_start + m_a_alpha.pattern.arity - 1;
				m_a_alpha_suffix_end = i*m_a_alpha.terminalSequenceLengths.length + m_a_alpha.terminalSequenceLengths.length;
//				
//				int size = m_a_alpha.terminalSequenceStartIndices.length-1;
//				m_a_alpha_suffix = new int[size];
//				for (int index=0; index<size; index++) {
//					m_a_alpha_suffix[index] = m_a_alpha.terminalSequenceStartIndices[index+1];
//				}
				increment_m_a_alpha_suffix_start = false;
			}
			
			int m_a_alpha_suffix_length = m_a_alpha_suffix_end - m_a_alpha_suffix_start;
			int m_alpha_b_prefix_length = m_alpha_b_prefix_end - m_alpha_b_prefix_start;
			
			if (m_alpha_b_prefix_length != m_a_alpha_suffix_length) {
//			if (m_alpha_b_prefix.length != m_a_alpha_suffix.length) {
				throw new RuntimeException("Length of s(m_a_alpha) and p(m_alpha_b) do not match");
			} else {
				
				int result = 0;
				
				for (int index=0; index<m_a_alpha_suffix_length; index++) {
//				for (int index=0; index<m_a_alpha_suffix.length; index++) {
					
					int a = m_a_alpha.terminalSequenceStartIndices[m_a_alpha_suffix_start+index];
					if (increment_m_a_alpha_suffix_start && index==0) {
						a++;
					}
					int b = 0;
					try {
					b = m_alpha_b.terminalSequenceStartIndices[m_alpha_b_prefix_start+index];
					} catch (ArrayIndexOutOfBoundsException e) { 
						throw e;
					}					
					if (a > b) {
//					if (m_a_alpha_suffix[index] > m_alpha_b_prefix[index]) {
						result = 1;
						break;
					} else if (a < b) {
//					} else if (m_a_alpha_suffix[index] < m_alpha_b_prefix[index]) {
						result = -1;
						break;
					}
				}
				
				if (result==0) {
//					int length = m_alpha_b.terminalSequenceEndIndices[m_alpha_b.terminalSequenceEndIndices.length-1] - m_a_alpha.getStartPosition(i, 0);
					int length = m_alpha_b.getEndPosition(j, m_alpha_b.terminalSequenceLengths.length-1) - m_a_alpha.getStartPosition(i, 0);
					if (m_alpha_b.pattern.endsWithNonterminal())
						length += m_a_alpha.prefixTree.minNonterminalSpan;
					if (m_a_alpha.pattern.startsWithNonterminal())
						length += m_a_alpha.prefixTree.minNonterminalSpan;
					
					if (length > m_a_alpha.prefixTree.maxPhraseSpan) {
						result = -1;
					}
				}
				
				return result;
			}
			
		}
		else {
//			return nonOverlapping.compare(m_a_alpha, m_alpha_b);

			if (m_a_alpha.getSentenceNumber(i) < m_alpha_b.getSentenceNumber(j))
				return -1;
			else if (m_a_alpha.getSentenceNumber(i) > m_alpha_b.getSentenceNumber(j))
				return 1;
			else {
				int prefixStartPosition = m_a_alpha.getStartPosition(i, 0);
				int suffixStartPosition = m_alpha_b.getStartPosition(j, 0);
				
				if (prefixStartPosition >= suffixStartPosition-1)
					return 1;
				else if (prefixStartPosition <= suffixStartPosition-m_a_alpha.prefixTree.maxPhraseSpan)
					return -1;
				else
					return 0;
			}

		}
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
	 * Returns the index of the sentence in the corpus that the
	 * phrase is a part of.
	 * 
	 * @return the index of the sentence in the corpus that the
	 *         phrase is a part of.
	 */
	public int getSentenceNumber(int phraseIndex) {
		return sentenceNumber[phraseIndex];
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
	
	public static HierarchicalPhrases emptyList(PrefixTree prefixTree) {
		Vocabulary vocab = (prefixTree.suffixArray==null) ? null : prefixTree.suffixArray.getVocabulary();
		
		return new HierarchicalPhrases(new Pattern(vocab), Collections.<Integer>emptyList(), Collections.<Integer>emptyList(), prefixTree);
	}
	
}
