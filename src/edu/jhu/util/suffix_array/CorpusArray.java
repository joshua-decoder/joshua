package edu.jhu.util.suffix_array;

// Imports
import edu.jhu.joshua.corpus.Corpus;
import edu.jhu.util.sentence.*;
import java.util.*;
import java.io.*;

/**
 * A compact int[] based representation of a corpus.  The class keeps all of the words in their int 
 * form in a single array.  It also maintains a seperate int[] array that lists the start index for
 * each sentence in the corpus. This second array allows us to quickly determine the source sentence
 * of any given position in the corpus using a binary search.
 *
 * @author  Josh Schroeder
 * @since  29 Dec 2004
 *
 * The contents of this file are subject to the Linear B Community Research 
 * License Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.linearb.co.uk/developer/. Software distributed under the License
 * is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either 
 * express or implied. See the License for the specific language governing 
 * rights and limitations under the License. 
 *
 * Copyright (c) 2004-2005 Linear B Ltd. All rights reserved.
 */

public class CorpusArray {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	/** Stores an integer based representation of each word
	  * in the corpus. 
	  */
	protected int[] corpus;

	/** Keeps the starting position in the corpus array for each
	  * of the sentences.  The length of the sentences array is
	  * equal to the number of sentences in the corpus.
	  */
	protected int[] sentences;
	
	/** The alphabetized vocabulary which maps between the
	  * String and int representation of words in the corpus.
	  */
	protected Vocabulary vocab;
	
//===============================================================
// Constructor(s)
//===============================================================

	/** 
	 * Protected constructor takes in the already prepared member variables.
	 * @see SuffixArrayFactor.createCorpusArray(String,String,Vocabulary)
	 * @see SuffixArrayFactor.loadCorpusArray(String,String,String,Vocabulary)
	 */
	protected CorpusArray (int[] corpus, int[] sentences, Vocabulary vocab) {
		this.corpus = corpus;
		this.sentences = sentences;
		this.vocab = vocab;
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	

	
	/**
	 * @returns the integer representation of the Word at the 
	 * specified position in the corpus.
	 */
	public int getWordID(int position) {
		return corpus[position];	
	}
	
	
	/**
	 * @returns the sentence index associated with the specified
	 * position in the corpus.
	 */
	public int getSentenceIndex(int position) {
		int index = Arrays.binarySearch(sentences, position);
		//if index is positive, then the position searched for is the first word of a sentence.
		//we return the exact value.
		if (index>=0) return index;
		//otherwise, we are given an negative version of the first number higher than our position.
		//that is the position of where this would be inserted if it was its own sentence, 
		//so we make the number positive and subtract 2 (one since since it is by ith element instead of position,
		//one to get the previous index)
		else return (index*(-1))-2;
	}
	
	/**
	 * @return the position in the corpus of the first word of
	 * the specified sentence.
	 */
	public int getSentencePosition(int sentenceID) {
		return sentences[sentenceID];
	}
		
	
	/** 
	 *Gets the sentence at the specified index (starting from zero).
	 * @returns the sentence, or null if the specified sentence number doesn't exist
	 */
	public Phrase getSentence(int sentenceIndex) {
		if(sentenceIndex >= sentences.length) {
			return null;
		} else if(sentenceIndex == sentences.length-1) {
			return getPhrase(sentences[sentenceIndex], corpus.length);
		} else {
			return getPhrase(sentences[sentenceIndex], sentences[sentenceIndex+1]);
		} 
	}
	
	

	/**
	 * @return the number of words in the corpus. 
	 */
	public int size() {
		return corpus.length;
	}
	
	/**
	 * @return the number of sentences in the corpus.
	 */
	public int getNumSentences() {
		return sentences.length;
	}
	

	//===========================================================
	// Methods
	//===========================================================
	
	
	/**
	 * Compares the phrase that starts at position start with 
	 * the subphrase indicated by the start and end points of
	 * the phrase.
	 *
	 * @param corpusStart the point in the corpus where the comparison begins
	 * @param phrase the superphrase that the comparsion phrase is drawn from
	 * @param phraseStart the point in the phrase where the comparison begins (inclusive)
	 * @param phraseEnd the point in the phrase where the comparison ends (exclusive)
	 * @return an int that follows the conventions of java.util.Comparator.compareTo()
	 */
	public int comparePhrase(int corpusStart, int[] phrase, int phraseStart, int phraseEnd) {
		int diff = -1;
		for (int i = 0; i < phraseEnd-phraseStart; i++) {
			if (i+corpusStart>=corpus.length) return -1;
			diff = corpus[i+corpusStart] - phrase[i+phraseStart];
			if (diff != 0) return diff;
		}
		return 0;
	}
	
	
	/**
	 * compares the phrase that starts at position start with 
	 * the phrase passed in.  Compares the entire phrase.
	 */
	public int comparePhrase(int corpusStart, int[] phrase) {
		return comparePhrase(corpusStart, phrase, 0, phrase.length);
	}
	
	

	/** 
	 * Compares the suffixes starting a positions index1 and index2.  
	 * @param position1 the position in the corpus where the first suffix begins
	 * @param position2 the position in the corpus where the second suffix begins
	 * @param maxComparisonLength a cutoff point to stop the comparison
	 * @return an int that follows the conventions of java.util.Comparator.compareTo()
	 */
    public int compareSuffixes(int position1, int position2, int maxComparisonLength){
		for(int i = 0;i < maxComparisonLength;i++){
			if(position1 + i < (corpus.length -1) && position2 + i > (corpus.length -1)) {
				return 1;
			}
			if(position2 + i < (corpus.length - 1) && position1 + i > (corpus.length -1)) {
				return -1;
			}
			int diff = corpus[position1 + i] - corpus[position2 + i];
			
			if(diff != 0) {
				return diff;
			}
		}
		return 0;
    }

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	protected Phrase getPhrase(int startPosition, int endPosition) {
		Phrase phrase = new Phrase(vocab);
		for(int i = startPosition; i < endPosition && i < corpus.length; i++) {
			phrase.addWord(vocab.getWord(corpus[i]));
		}
		return phrase;
	}
	
	
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


	public static void main(String[] args) throws Exception {

	}

}

