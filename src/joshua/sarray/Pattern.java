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

import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;


/**
 * Represents a pattern of terminals and nonterminals.
 * <p>
 * The integer representation of each terminal must be positive.
 * The integer representation of each nonterminal must be negative.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class Pattern extends BasicPhrase {

//===============================================================
// Member variables
//===============================================================


	final int arity;
//	final SuffixCase suffixCase;
//	final PrefixCase prefixCase;


//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructs a pattern of terminals and nonterminals.
	 * <p>
	 * The integer representation of each terminal must be positive.
	 * The integer representation of each nonterminal must be negative.
	 * 
	 * @param vocab Vocabulary capable of mapping between symbols and integers.
	 */
	public Pattern(Vocabulary vocab, int... words) {
		super(words, vocab);
		
		this.arity = calculateArity(this.words);
		
//		this.suffixCase = suffixCase(words);
//		this.prefixCase = prefixCase(words);
	}
	
	
	/**
	 * Constructs a pattern by copying an existing phrase.
	 * 
	 * @param phrase an existing phrase
	 */
	public Pattern(Phrase phrase) {
		this.words = new int[phrase.size()];
		this.vocab = phrase.getVocab();
		
		for(int i = 0 ; i < phrase.size(); i++) {
			words[i] = phrase.getWordID(i);
		}
		this.arity = calculateArity(this.words);
		
//		this.suffixCase = suffixCase(words);
//		this.prefixCase = prefixCase(words);
	}
	
	
	//TODO What does this constructor do?
	public Pattern(Pattern pattern, int... word) {
		super(PrefixTree.pattern(pattern.words, word),pattern.vocab);
		this.arity = calculateArity(this.words);
		
//		this.suffixCase = suffixCase(words);
//		this.prefixCase = prefixCase(words);
	}
	

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public boolean startsWithNonterminal() {
		// we assume that the nonterminal symbols will be denoted with negative numbers
		return words[0] < 0;
	}
	
	
	public boolean endsWithNonterminal() {
		// we assume that the nonterminal symbols will be denoted with negative numbers
		return words[words.length-1] < 0;
	}
	
	public boolean endsWithTwoTerminals() {
		if (words.length > 1 && words[words.length-1] >= 0 && words[words.length-2] >= 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Gets the lengths of each terminal sequence in this pattern.
	 * <p>
	 * The result of this method is not well-defined 
	 * for patterns that consist only of nonterminals.
	 * 
	 * TODO Write unit tests for this method.
	 * 
	 * @return
	 */
	public int[] getTerminalSequenceLengths() {
		
		int size = 0;
		boolean readyToStartSequence = true;
		
		for (int word : words) {
			if (word < 0) {
				readyToStartSequence = true;
			} else {
				if (readyToStartSequence) {
					size++;
					readyToStartSequence = false;
				}
			}
		}
		
		int[] result = new int[size];
		
		if (size > 0) {

			int index=0;
			int count=0;

			for (int word : words) {
				if (word < 0) {
					if (count > 0) {
						result[index] = count;
						index++;
						count = 0;
					}
				} else {
					count++;
				}
			}
			
			if (count > 0) {
				result[index] = count;
			}
		}
		return result;
		
	}
	
	//===========================================================
	// Methods
	//===========================================================

	public int arity() {
		return arity;
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();

		s.append('[');
		
		for (int i=0; i<words.length; i++) {
			
			if (i>0) {
				s.append(' ');
			}
			
			if (words[i] >= 0)
				if (vocab==null)
					s.append(words[i]);
				else
					s.append(vocab.getWord(words[i]));
			else
				s.append('X');
			
		}

		s.append(']');
		
		return s.toString();
		
	}

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================


	int[] getWords() {
		return words;
	}
	
//	enum PrefixCase {
//		EMPTY_PREFIX,
//		ENDS_WITH_NONTERMINAL,
//		ENDS_WITH_TWO_TERMINALS,
//		ENDS_WITH_NONTERMINAL_TERMINAL
//	}
//	
//	enum SuffixCase {
//		EMPTY_SUFFIX,
//		STARTS_WITH_NONTERMINAL,
//		STARTS_WITH_TWO_TERMINALS,
//		STARTS_WITH_TERMINAL_NONTERMINAL
//	}
	
//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
	private int calculateArity(int[] words) {
		
		int arity = 0;
		
		for (int element : words) {
//			if (element==PrefixTree.X) arity++;
			if (element < 0) arity++;
		}
		
		return arity;
	}
	
	
//	private PrefixCase prefixCase(int[] words) {
//		
//		if (words==null || words.length==0)
//			return PrefixCase.EMPTY_PREFIX;
//		
//		if (words[words.length-1] == PrefixTree.X) {
//			
//			return PrefixCase.ENDS_WITH_NONTERMINAL;
//			
//		} else {
//			
//			if (words.length > 1) {
//				
//				if (words[words.length-2] == PrefixTree.X) {
//					
//					return PrefixCase.ENDS_WITH_NONTERMINAL_TERMINAL;
//					
//				} else {
//					
//					return PrefixCase.ENDS_WITH_TWO_TERMINALS;
//					
//				}
//				
//			} else {
//				
//				return PrefixCase.EMPTY_PREFIX;
//				
//			}
//			
//		}
//		
//	}
	
	
//	private SuffixCase suffixCase(int[] words) {
//		
//		if (words==null || words.length==0)
//			return SuffixCase.EMPTY_SUFFIX;;
//		
//		if (words[0] == PrefixTree.X) {
//		
//			return SuffixCase.STARTS_WITH_NONTERMINAL;
//		
//		} else {
//			
//			if (words.length > 1) {
//				
//				if (words[1] == PrefixTree.X) {
//					
//					return SuffixCase.STARTS_WITH_TERMINAL_NONTERMINAL;
//					
//				} else {
//					
//					return SuffixCase.STARTS_WITH_TWO_TERMINALS;
//					
//				}
//				
//				
//			} else {
//				
//				return SuffixCase.EMPTY_SUFFIX;
//				
//			}
//			
//		}
//		
//	}
	
}
