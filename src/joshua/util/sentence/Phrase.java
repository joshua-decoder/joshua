/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.util.sentence;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.List;

/**
 * Phrase is a subclass of ArrayList consisting of Words.
 *
 * @author  Josh Schroeder
 * @since  30 July 2003
 * @version $LastChangedDate$
 */
public class Phrase extends ArrayList implements Comparable {

//===============================================================
// Constants
//===============================================================
	
	/**
	 * characters defined as spaces for the purpose of tokenizing
	 * Strings
	 */
	static public String SPACE_CHARACTERS = "\u00a0 ";
			
//===============================================================
// Member variables
//===============================================================

	private Vocabulary vocab;
 
//===============================================================
// Constructor(s)
//===============================================================
	
	/**
	 * Basic constructor creates an empty phrase.
	 */
	public Phrase(Vocabulary vocab) {
		super();
		this.vocab = vocab;
	}
	
	
	/**
	 * Creates a phrase from an existing List object.
	 * 
	 * @param list a List of Word objects
	 */
	public Phrase(List list, Vocabulary vocab) {
		super(list);
		this.vocab = vocab;
	}
	
	
	/**
	 * creates a new phrase, allocating a specific amount of
	 * memory for the eventual size of this Phrase.
	 * 
	 * @param capacity how much space to allocate this Phrase
	 */
	public Phrase(int capacity, Vocabulary vocab) {
		super(capacity);
		this.vocab = vocab;
	}
	
	
	/**
	 * Contstructor takes in a String phrase. It is tokenized
	 * on the space character and Word objects are made from
	 * the tokens, then placed in the ArrayList.
	 * 
	 * @param phraseString a String of the format "Hello , world ."
  	 */
	public Phrase(String phraseString, Vocabulary vocab) {
		super();
		this.vocab = vocab;
		String[] wordStrings = phraseString.split("\\s+");
		for(int i=0; i < wordStrings.length; i++) {
			addWord(new Word(wordStrings[i], vocab));
		}
	}
	
	
	/**
	 * Creates a phrase from an existing phrase. This does not
	 * copy the underlying words.
	 * 
	 * @param phrase the contents for this phrase.
	 */
	public Phrase(Phrase phrase) {
		super(phrase);
		this.vocab = phrase.vocab;
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/**
	 * appends a word to the end of the Phrase
	 * 
	 * @param word the word to append
	 */
	public void addWord(Word word) {
		add(word);
	}
	
	
	/**
	 * appends another phrase to the end of this one
	 * 
	 * @param phrase the phrase to append
	 */
	public void append(Phrase phrase) {
		addAll(phrase);
	}
	
	
	/**
	 * Gets the word at the specific index.
	 * 
	 * @param index the index of the word.
	 * @exception ArrayIndexOutOfBoundsException if the index is out of bounds.
	 */
	public Word getWord(int index) {
		return (Word) get(index);
	}
	
	
	/**
	 * @return the vocabulary that the words in this phrase are
	 *         drawn from.
	 */
	public Vocabulary getVocab() {
		return vocab;
	}
	
	
	/**
	 * @return the index of the first occurrence of the subphrase
	 *         after the specified index, if the subphrase is
	 *         present, or -1 if it is not.
	 */
	public int indexOf(Phrase subPhrase, int startFrom) {
		if (subPhrase.size() == 0) {
			return -1;
		}
		Word firstWord = subPhrase.getWord(0);
		for (int i = startFrom; i < this.size(); i++) {
			Word word = getWord(i);
			if (word.equals(firstWord)) {
				boolean foundMatch = true;
				for (int j = 0; j < subPhrase.size(); j++) {
					if (i+j >= this.size()) {
						return -1;
					}
					if (! subPhrase.getWord(j).equals(this.getWord(i+j))) {
						foundMatch = false;
					}
				}
				if (foundMatch) {
					return i;
				}
			}
		}
		return -1;
	}
	
	
	/**
	 * @return the index of the first occurrence of the subphrase
	 *         if it is present, or -1 if it is not.
	 */
	public int indexOf(Phrase subPhrase) {
		return indexOf(subPhrase, 0);
	}

	//===========================================================
	// Methods
	//===========================================================
	
	
	/**
	 * @return a space-delimited string of the words in this Phrase
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			buf.append(get(i));
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
	 * @return ArrayList of all possible subphrases.
	 */
	public ArrayList getSubPhrases() {
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
	public ArrayList getSubPhrases(int maxLength) {
		if (maxLength > size()) {
			return getSubPhrases(size());
		}
		ArrayList phrases=new ArrayList();
		for (int i = 0; i < size(); i++) {
			for (int j = i + 1; (j <= size()) && (j-i <= maxLength); j++) {
				Phrase subPhrase = subPhrase(i,j);
				phrases.add(subPhrase);
			}
		}
		return phrases;
	}
	
	
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
	public Phrase subPhrase(int start, int end) {
		return new Phrase(subList(start, end), vocab);
	}
	
	
	/**
	 * uses comparisons of component words. Same as comparing
	 * the string results of Phrase.toString();
	 * 
	 * @param obj the object to compare to
	 * @return -1 if this object is less than the parameter, 0
	 *         if equals, 1 if greater
	 * @exception ClassCastException if the passed object is not of type Phrase
	 */
	public int compareTo(Object obj) throws ClassCastException {
		Phrase other = (Phrase) obj;
		for (int i = 0; i < size();i++) {
			if (i < other.size()) {
				Word thisWord = (Word)this.get(i);
				Word otherWord = (Word)other.get(i);
				int wordCompare = thisWord.compareTo(otherWord);
				if (wordCompare != 0) return wordCompare;
			} else {
				//same but other is shorter, so we are after
				return 1;
			}
		}
		if (size() < other.size()) return -1;
		else return 0;
	}
	

	/**
	 * @return an int[] corresponding to the ID of each word
	 *         in the phrase
	 */
	public int[] getWordIDArray() {
		int[] wordIDs = new int[size()];
		for(int i = 0; i < size(); i++) {
			wordIDs[i] = getWord(i).getID();
		}
		return wordIDs;
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
	
	
//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

	/**
	 * Main contains test code
	 */
	public static void main(String[] args) {
		Vocabulary vocab = new Vocabulary();
		Phrase a = new Phrase("hello world .", vocab);
		Phrase b = new Phrase("hello world .", vocab);
		Word c = new Word("hello", vocab);
		Word d = new Word("hello", vocab);
		System.out.println(c.equals(d));
		System.out.println(a);
		System.out.println(b);
		System.out.println(a.equals(b));
	}
}

