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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joshua.corpus.Phrase;
import joshua.corpus.vocab.SymbolTable;


/**
 * Represents a pattern of terminals and nonterminals.
 * <p>
 * The integer representation of each terminal must be positive.
 * The integer representation of each nonterminal must be negative.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class Pattern extends BasicPhrase implements PatternFormat {

//===============================================================
// Member variables
//===============================================================

	/** The number of nonterminals in this pattern. */
	final int arity;


//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructs a pattern of terminals and nonterminals.
	 * <p>
	 * The integer representation of each terminal must be
	 * positive. The integer representation of each nonterminal
	 * must be negative.
	 * 
	 * @param vocab Vocabulary capable of mapping between symbols
	 *              and integers.
	 */
	public Pattern(SymbolTable vocab, int... words) {
		super(words, vocab);
		
		this.arity = calculateArity(this.words);
	}
	
	
	/**
	 * Constructs a pattern by copying an existing phrase.
	 *
	 * @param phrase an existing phrase
	 */
	public Pattern(Phrase phrase) {
		super(new int[phrase.size()], phrase.getVocab());
		
		for(int i = 0 ; i < phrase.size(); i++) {
			words[i] = phrase.getWordID(i);
		}
		this.arity = calculateArity(this.words);
	}
	
	
	/**
	 * Constructs a pattern by copying an existing pattern, and
	 * then appending additional words to the new pattern.
	 * 
	 * @param pattern Existing pattern to copy.
	 * @param word Words to append to the new pattern.
	 */
	public Pattern(Pattern pattern, int... word) {
		super(pattern(pattern.words, word),pattern.vocab);
		this.arity = calculateArity(this.words);
	}
	
	/**
	 * Constructs a pattern by copying an int[] pattern, and
	 * then appending additional words to the new pattern.
	 * 
	 * @param vocab Vocabulary capable of mapping between symbols
	 *              and integers.
	 * @param patternStart Existing pattern to copy.
	 * @param patternEnd Words to append to the new pattern.
	 */
	public Pattern(SymbolTable vocab, int[] patternStart, int... patternEnd) {
		super(pattern(patternStart,patternEnd), vocab);
		this.arity = calculateArity(this.words);
	}
	
	/**
	 * Constructs a new integer array by concatenating two
	 * existing integer arrays together.
	 *  
	 * @param oldPattern
	 * @param newPattern
	 * @return a new integer array representing two existing
	 *         integer arrays concatenated together
	 */
	protected static int[] pattern(int[] oldPattern, int... newPattern) {
		int[] pattern = new int[oldPattern.length + newPattern.length];

		for (int index=0; index<oldPattern.length; index++) {
			pattern[index] = oldPattern[index];
		}
		for (int index=oldPattern.length; index<oldPattern.length+newPattern.length; index++) {
			pattern[index] = newPattern[index - oldPattern.length];
		}

		return pattern;
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public boolean startsWithNonterminal() {
		if (words.length > 0) {
			// we assume that the nonterminal symbols will be denoted with negative numbers
			return words[0] < 0;
		} else {
			return false;
		}
	}
	
	
	public boolean endsWithNonterminal() {
		if (words.length > 0) {
			// we assume that the nonterminal symbols will be denoted with negative numbers
			return words[words.length-1] < 0;
		} else {
			return false;
		}
	}
	
	public boolean endsWithTwoTerminals() {
		return (words.length > 1 && words[words.length-1] >= 0 && words[words.length-2] >= 0);
	}
	
	public boolean secondTokenIsTerminal() {
		return (words.length > 1 && words[1] >= 0);
	}
	
	/**
	 * Gets the lengths of each terminal sequence in this
	 * pattern.
	 * <p>
	 * The result of this method is not well-defined for patterns
	 * that consist only of nonterminals.
	 * 
	 * @return the lengths of each terminal sequence in this pattern
	 */
	// TODO Write unit tests for this method.
	public byte[] getTerminalSequenceLengths() {
		
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
		
		byte[] result = new byte[size];
		
		if (size > 0) {

			int index=0;
			byte count=0;

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

	public List<Pattern> split() {
		int arity = this.arity();
		if (arity==0) {
			return Collections.singletonList(this);
		} else {
			
			List<Pattern> patternList = new ArrayList<Pattern>(arity);
			List<Integer> tokenList = new ArrayList<Integer>(this.size());
			
			for (int token : this.getWordIDs()) {
				if (token < 0) {
					if (! tokenList.isEmpty()) {
						int[] tokens = new int[tokenList.size()];
						patternList.add(new Pattern(this.getVocab(), tokens));
						tokenList.clear();
					}
				} else {
					tokenList.add(token);
				}
			}
			
			if (! tokenList.isEmpty()) {
				int[] tokens = new int[tokenList.size()];
				patternList.add(new Pattern(this.getVocab(), tokens));
				tokenList.clear();
			}
			
			return patternList;
		}
	}
	
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
			
			if (words[i] >= 0) {
				if (vocab==null) {
					s.append(words[i]);
				} else {
					s.append(vocab.getWord(words[i]));
				}
			} else {
				s.append('X');
			}
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


	
//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
	/**
	 * Gets the number of nonterminals in this pattern.
	 *
	 * @return the number of nonterminals in this pattern.
	 */
	private static int calculateArity(int[] words) {
		
		int arity = 0;
		
		for (int element : words) {
			if (element < 0) arity++;
		}
		
		return arity;
	}
	
	
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(arity);
		
		for (int word : words) {
			out.writeInt(word);
		}
	}
}

