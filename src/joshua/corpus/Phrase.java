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
package joshua.corpus;

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.vocab.SymbolTable;


/**
 * Representation of a sequence of tokens.
 *
 * @version $LastChangedDate:2008-09-18 10:31:54 -0500 (Thu, 18 Sep 2008) $
 */
public interface Phrase extends Comparable<Phrase> {

	/**
	 * Returns the vocabulary that the words in this phrase are
	 * drawn from.
	 *
	 * @return the vocabulary that the words in this phrase are
	 *         drawn from.
	 */
	SymbolTable getVocab();
	
	
	/**
	 * This method gets the integer IDs of the phrase as an
	 * array of ints.
	 * 
	 * @return an int[] corresponding to the ID of each word
	 *         in the phrase
	 */
	public int[] getWordIDs();
	
	/**
	 * Returns the integer word id of the word at the specified
	 * position.
	 *
	 * @param position Index of a word in this phrase.
	 * @return the integer word id of the word at the specified
	 *         position.
	 */
	int getWordID(int position);
	
	
	/**
	 * Returns the number of words in this phrase.
	 *
	 * @return the number of words in this phrase.
	 */
	int size();



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
	 * @return List of all possible subphrases.
	 */
	List<Phrase> getSubPhrases();
	
	
	/**
	 * Returns a list of subphrases only of length
	 * <code>maxLength</code> or smaller.
	 *
	 * @param maxLength the maximum length phrase to return.
	 * @return List of all possible subphrases of length maxLength
	 *         or less
	 * @see #getSubPhrases()
	 */
	List<Phrase> getSubPhrases(int maxLength);
	
	
	/**
	 * creates a new phrase object from the indexes provided.
	 * <P>
	 * NOTE: subList merely creates a "view" of the existing
	 * Phrase object. Memory taken up by other Words in the
	 * Phrase is not freed since the underlying subList object
	 * still points to the complete Phrase List.
	 *
	 * @see ArrayList#subList(int, int)
	 */
	Phrase subPhrase(int start, int end);
	
	
	/**
	 * Compares the two strings based on the lexicographic order
	 * of words defined in the Vocabulary.
	 *
	 * @param other the object to compare to
	 * @return -1 if this object is less than the parameter, 0
	 *         if equals, 1 if greater
	 */
	int compareTo(Phrase other);

	/**
	 * Returns a human-readable String representation of the
	 * phrase.
	 *
	 * @return a human-readable String representation of the
	 *         phrase.
	 */
	String toString();
}
