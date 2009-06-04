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

import joshua.corpus.AbstractPhrase;
import joshua.corpus.Phrase;
import joshua.corpus.TerminalIterator;
import joshua.corpus.vocab.SymbolTable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Phrase encapsulates an int[] of word IDs, and provides some basic
 * functionality for manipulating phrases.
 *
 * @author Josh Schroeder
 * @since  30 July 2003
 * @author Chris Callison-Burch
 * @since  29 May 2008
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */
public class BasicPhrase extends AbstractPhrase {



//===============================================================
// Member variables
//===============================================================

	protected SymbolTable vocab;
	protected int[] words;
 
 
//===============================================================
// Constructor(s)
//===============================================================
	
	/**
	 * Constructs a basic phrase.
	 * 
	 * @param words Array of word symbols for this phrase.
	 * @param vocab Symbol table to use for this phrase.
	 */
	public BasicPhrase(int[] words, SymbolTable vocab) {
		this.vocab = vocab;
		this.words = words;
	}

	/**
	 * Constructs a basic phrase.
	 * 
	 * @param vocab Symbol table to use for this phrase.
	 * @param words Array of word symbols for this phrase.
	 */
	public BasicPhrase(SymbolTable vocab, int...words) {
		this.vocab = vocab;
		this.words = words;
	}
	
	/**
	 * Constructor tokenizes the phrase string at whitespace
	 * characters and looks up the IDs of the words using the
	 * Vocabulary.
	 * 
	 * @param phraseString a String of the format "Hello , world ."
	 * @param vocab Symbol table to use for this phrase
  	 */
	public BasicPhrase(String phraseString, SymbolTable vocab) {
		this.vocab = vocab;
		String[] wordStrings = phraseString.split("\\s+");
		words = new int[wordStrings.length];
		for (int i = 0; i < wordStrings.length; i++) {
			words[i] = vocab.addTerminal(wordStrings[i]);
		}
	}


//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/** 
	 * Gets the symbol table associated with this phrase.
	 * 
	 * @return the vocabulary that the words in this phrase are
	 *         drawn from.
	 */
	public SymbolTable getVocab() {
		return vocab;
	}
	
	/**
	 * Gets the integer identifier for the word at the specified
	 * position.
	 * 
	 * @param position Index into the corpus
	 * @return the integer identifier for the word at the
	 *         specified position
	 */
	public int getWordID(int position) {
		return words[position];
	}
	
	/**
	 * Gets the number of tokens in this phrase.
	 * 
	 * @return the number of tokens in this phrase
	 */
	public int size() {
		return words.length;
	}
	
	
	/**
	 * This method gets the integer IDs of the phrase as an
	 * array of ints.
	 * <p>
	 * This method does <emph>not</emph> copy the array, and
	 * so may be called very cheaply.
	 * 
	 * @return an int[] corresponding to the ID of each word
	 *         in the phrase
	 */
	public int[] getWordIDs() {
		return words;
	}
	
	
	//===========================================================
	// Methods
	//===========================================================
	
	
	/**
	 * Gets a space-delimited string representing the words in
	 * this Phrase
	 * 
	 * @return a space-delimited string of the words in this
	 *         Phrase
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
        for (int i = 0; i < size(); i++) {
			String word = vocab.getWord(words[i]);
            buf.append(word);
            if (i < size() - 1) {
				buf.append(' ');
			}
        }
        return buf.toString();
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
	 * @return List of all possible subphrases.
	 */
	public List<Phrase> getSubPhrases() {
		return getSubPhrases(size());
	}
	
	
	/**
	 * Returns a list of subphrases only of length
	 * <code>maxLength</code> or smaller.
	 * 
	 * @param maxLength the maximum length phrase to return.
	 * @return List of all possible subphrases of length maxLength
	 *         or less
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
	 * Creates a new phrase object from the indexes provided.
	 * <P>
	 * NOTE: subList merely creates a "view" of the existing
	 * Phrase object. Memory taken up by other Words in the
	 * Phrase is not  freed since the underlying subList object
	 * still points to the complete Phrase List.
	 *
	 * @param start Inclusive starting index
	 * @param end Exclusive ending index
	 * @return Phrase object representing the specified range
	 * @see java.util.ArrayList#subList(int, int)
	 */
	public Phrase subPhrase(int start, int end) {
		int subPhraseLength = end - start;
		int[] subPhraseWords = new int[subPhraseLength];
		for (int i = 0; i < subPhraseLength; i++) {
			subPhraseWords[i] = words[i+start];
		}
		return new BasicPhrase(subPhraseWords, vocab);
	}
	
	/**
	 * Gets an object capable of iterating over all terminals in this pattern.
	 * 
	 * @return an object capable of iterating 
	 *         over all terminals in this pattern
	 */
	public Iterable<Integer> getTerminals() {
		
		return new Iterable<Integer>() {	
			public Iterator<Integer> iterator() {
				return new TerminalIterator(vocab,words);
			}
			
		};
	}
	
	/**
	 * Compares the two strings based on the lexicographic order
	 * of words defined in the Vocabulary.
	 *
	 * @param other the object to compare to
	 * @return -1 if this object is less than the parameter, 0
	 *         if equals, 1 if greater
	 */
	public int compareTo(Phrase other) {
		for (int i = 0; i < words.length; i++) {
			if (i < other.size()) {
				int difference = words[i] - other.getWordID(i);
				if (difference != 0) {
					return difference;
				}
			} else {
				//same but other is shorter, so we are after
				return 1;
			}
		}
		if (size() < other.size()) {
			return -1;
		} else {
			return 0;
		}
	}


//===============================================================
// Private 
//===============================================================

}
