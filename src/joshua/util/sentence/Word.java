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
package edu.jhu.util.sentence;

// Imports

/**
 * Word. Contains of String.
 *
 * @author  Josh Schroeder
 * @since  30 July 2003
 * @version $LastChangedDate$
 */
public class Word implements Comparable {

//===============================================================
// Constants
//===============================================================
	
	
//===============================================================
// Member variables
//===============================================================

	/** The underlying String */
	private String word;
	
	private Vocabulary vocab;
		
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor takes in a String.
	 *
	 * @param word the String of this word
	 */
	public Word(String word, Vocabulary vocab){
		this.word = word;
		this.vocab = vocab;
		vocab.addWord(this);
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	
	/**
	 * Gets the String represented by this word.
	 * @return String version of this word
	 */
	public String getString() {
		return word;
	}
	
	
	/**
	 * Gets the word ID
	 * @return the ID of this word
	 */
	public int getID() {
		return vocab.getID(this);
	}
	
	
	/**
	 * @resuts the vocabulary that this word is included in.
	 */
	public Vocabulary getVocab() {
		return vocab;
	}
	
	
	//===========================================================
	// Methods
	//===========================================================

	/**
	 * Uses comparison of underlying String object stored by word
	 * @return the value 0 if the argument string is equal to this string; a value less than 0 if this string is lexicographically less than the string argument; and a value greater than 0 if this string is lexicographically greater than the string argument.
	 * @see java.lang.String#compareTo(String)
	 */
	public int compareTo(Object obj) throws ClassCastException {
		Word otherWord = (Word) obj;
		return word.compareTo(otherWord.word);
	}
	
	/**
	 * Uses String's hashcode
	 * @return hashCode value of the underlying word String
	 */
	public int hashCode() {
		return word.hashCode();
	}
	
	/**
	 * Checks that the strings are the same, assuming the other
	 * object is a Word
	 * @param o the object to comapre to
	 * @return true if the other object is a Word representing the same String
	 */
	public boolean equals(Object o) {
		if (o==null) return false;
        if (!o.getClass().isInstance(this)) return false;
        else {
            Word other = (Word)o;
			return (this.word.equals(other.word));
        }
	}
	
	/**
	 * Returns the String represented by this word.
	 * @return String of the word
	 */
	public String toString() {
		return word;
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

	public static void main(String[] args)
	{

	}
}

