package edu.jhu.util.sentence;

// Imports

/**
 * Word. Contains of String.
 *
 * @author  Josh Schroeder
 * @since  30 July 2003
 *
 * The contents of this file are subject to the Linear B Community Research 
 * License Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.linearb.co.uk/developer/. Software distributed under the License
 * is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either 
 * express or implied. See the License for the specific language governing 
 * rights and limitations under the License. 
 *
 * Copyright (c) Linear B Ltd., 2002-2005. All rights reserved.
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

