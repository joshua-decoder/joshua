package edu.jhu.util.sentence;

// Imports
import edu.jhu.util.suffix_array.SuffixArrayFactory;
import java.util.*;

/**
 * Vocabulary is the class that keeps track of the unique words
 * that occur in a corpus of text for a particular language.  
 * It assigns integer IDs to Words, which is useful when we are
 * creating suffix arrays or doing similar things.
 *
 * @author Chris Callison-Burch
 * @since  8 February 2005
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

public class Vocabulary {

//===============================================================
// Constants
//===============================================================

	public static final int UNKNOWN_WORD = -1;

	public static final String UNKNOWN_WORD_STRING = "UNK";

//===============================================================
// Member variables
//===============================================================

	Hashtable wordToIDMap;
	Vector vocabList;
	
	/** Determines whether new words may be added to the vocabulary. */
	boolean isFixed;
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor creates an empty vocabulary.
	 */
	public Vocabulary() {
		wordToIDMap = new Hashtable();
		vocabList = new Vector();
		isFixed = false;
	}

	/** 
	 * Constructor creates a fixed vocabulary from the given set of words.
	 */
	public Vocabulary (Set words) {
		wordToIDMap = new Hashtable();
		vocabList = new Vector();
		vocabList.addAll(words);
		alphabetize();
		isFixed = true;
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/** 
	 * @return the ID of word
	 */
	public int getID(Word word) {
		return getID(word.getString());
	}


	/**
	 * @return the ID for wordString
	 */
	public int getID(String wordString) {
		Integer ID = (Integer) wordToIDMap.get(wordString);
		if (ID == null) {
			return UNKNOWN_WORD;
		} else {
			return ID.intValue();
		}
	}


	/** 
	 * @return the Word associated with the specified ID
	 */
	public Word getWord(int id) {
		if (id>=size() || id == -1) return new Word(UNKNOWN_WORD_STRING, this);
		return (Word) vocabList.get(id);
	}
	
	/** 
	 * @return the Word object associated with the specified string
	 */	
	public Word getWord(String wordString) {
		return getWord(getID(wordString));
	}
	
	/**
	 * @return an Interator over all words in the Vocabulary.
	 */
	public Iterator iterator() {
		return vocabList.iterator();
	}
	
	public List getWords() {
		return vocabList;
	}
	
	
	/**
	 * Gets IDs for each of the words in the phrase.
	 * @return an array of IDs of the same length as the phrase
	 */
	public int[] getIDs(Phrase phrase) {
		int[] ids = new int[phrase.size()];
		for (int i = 0; i < phrase.size(); i++) {
			Word word = phrase.getWord(i);
			int id = getID(word);
			if (id == UNKNOWN_WORD) return null;
			ids[i]=id;
		}
		return ids;
	}
	
	
	/** 
	 * Constructs a phrase out of the array of IDs.
	 */
	public Phrase toPhrase(int[] ids) {
		Phrase phrase = new Phrase(ids.length, this);
		for (int i = 0; i < ids.length; i++) {
			phrase.add(getWord(ids[i]));
		}
		return phrase;
	}
	
	
	/** 
	 * Adds a word to the vocabulary.
	 * @returns the ID of the word, or UNKNOWN_WORD if 
	 * the word is new and the vocabulary is fixed.
	 */
	public int addWord(Word word) {
		String wordString = word.getString();
		Integer ID = (Integer) wordToIDMap.get(wordString);
		if (ID != null) {
			return ID.intValue();
		} else if(!isFixed) {
			ID = new Integer(vocabList.size());
			vocabList.add(word);
			wordToIDMap.put(wordString, ID);
			return ID.intValue();
		}  else {
			return UNKNOWN_WORD;
		}
	}
		
	/**
	 * @return the number of unique words in the vocabulary.
	 */
	public int size() {
		return vocabList.size();
	}
	
	/** 
	 * Fixes the size of the vocabulary so that new words
	 * may not be added.
	 */
	public void fixVocabulary() {
		isFixed = true;
	}
	
	/**
	 * @return true if there are unknown words in the phrase
	 */
	public boolean containsUnkownWords(Phrase phrase) {
		for(int i = 0; i < phrase.size(); i++) {
			if(getID(phrase.getWord(i)) == UNKNOWN_WORD) return true;
		}
		return false;
	}
	
	
	/**
	 * Checks that the Vocabularies are the same, by first checking that they have
	 * the same number of items, and then checking that each word corresoponds
	 * to the same ID
	 * @param o the Vocabulary to check equivalence with
	 * @return true if the other object is a Vocabulary representing the same set of
	 * words with identically assigned IDs
	 */
	public boolean equals(Object o) {
		if (o==null) return false;
        if (!o.getClass().isInstance(this)) return false;
        else {
            Vocabulary other = (Vocabulary) o;
			if(other.size() != this.size()) return false;
			for(int i = 0; i < this.size(); i++) {
				Word thisWord = (Word) this.vocabList.get(i);
				Word otherWord = (Word) other.vocabList.get(i);
				if(!(thisWord.equals(otherWord))) return false;
				Integer thisID = (Integer) this.wordToIDMap.get(thisWord);
				Integer otherID = (Integer) other.wordToIDMap.get(otherWord);
				if(thisID != null && otherID != null) {
					if(!(thisID.equals(otherID))) return false;
				}
			}
			return true;
        }
	}
	
	//===========================================================
	// Methods
	//===========================================================
	
	/** 
	 * Sorts the vocabulary alphabetically and re-assigns IDs
	 * in ascending order.
	 */
	public void alphabetize() {
		// alphabetize 
		Collections.sort(vocabList);
		// re-assign IDs
		wordToIDMap = new Hashtable();
		for(int i = 0; i < vocabList.size(); i++) {
			Word word = (Word) vocabList.get(i);
			wordToIDMap.put(word.getString(), new Integer(i));
		}
	}
	
	public String toString() {
		return vocabList.toString();
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


	public static void main(String[] args) throws Exception {
		if(args.length != 4) {
			System.out.println("Usage: java Vocabulary file corpusName lang outputDir");
			System.exit(0);
		}
		
		String inputFilename = args[0];
		String corpusName = args[1];
		String lang = args[2];
		String outputDirectory = args[3];
		
		// Create a suffix array-style vocabulary file
		Vocabulary vocab = new Vocabulary();
		int[] numberOfWordsSentences = SuffixArrayFactory.createVocabulary(inputFilename, vocab);
		SuffixArrayFactory.saveVocabulary(vocab, lang, corpusName, outputDirectory);
	}

}

