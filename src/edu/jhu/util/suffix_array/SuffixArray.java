package edu.jhu.util.suffix_array;

// Imports
import edu.jhu.util.sentence.*;
import edu.jhu.joshua.corpus.*;

import java.util.*;
import java.io.*;

/**
 * SuffixArray is the main class for producing suffix arrays from corpora,
 * and manipulating them once created.  Suffix arrays are a space economical
 * way of storing a corpus and allowing very quick searching of any substring
 * within the corpus.  A suffix array contains a list of references to every
 * point in a corpus, and each reference denotes the suffix starting at that
 * point and continuing to the end of the corpus.  The suffix array is sorted
 * alphabetically, so any substring within the corpus can be found with a 
 * binary search in O(log n) time, were n is the length of the corpus.
 *
 * @author  Colin Bannard
 * @since   10 December 2004
 * @author  Josh Schroeder
 * @since   2 Jan 2005
 * @author  Chris Callison-Burch
 * @since   9 February 2005
 *
 * @todo - method for calculating the set of unique phrases of a certain
 * length and/or frequency
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

public class SuffixArray implements Corpus {

//===============================================================
// Constants
//===============================================================

	/**
	 * A random number generator used in the quick sort implementation.
	 */
	public static final Random RAND = new Random();   
	
	/**
	 * The maximum length suffix to consider during sorting. 
	 */
	public static int MAX_COMPARISON_LENGTH = 20;

//===============================================================
// Member variables
//===============================================================

	protected int[] suffixes;
	protected CorpusArray corpus;
	
//===============================================================
// Constructor(s)
//===============================================================

	/** 
	 * Constructor takes a CorpusArray and creates a sorted 
	 * suffix array from it.
	 */
	public SuffixArray(CorpusArray corpusArray) {
		this.corpus = corpusArray;
		suffixes = new int[corpusArray.size()];

		// Create an array of suffix IDs
		for(int i = 0; i < corpusArray.size(); i++) {
			suffixes[i] = i;
		}
		// Sort the array of suffixe
		sort(suffixes);
	}


	/**
	 * Protected constructor takes in the already prepared member variables.
	 * @see SuffixArrayFactor.createSuffixArray(CorpusArray)
	 * @see SuffixArrayFactor.loadSuffixArray(String,String,String,CorpusArray)
	 */
	protected SuffixArray(int[] suffixes, CorpusArray corpusArray) {
		this.suffixes = suffixes;
		this.corpus = corpusArray;
	}
	
	// ccb - an empty constructor allows us to have the RmiSuffixArray
	// be a subclass of the SuffixArray.
	protected SuffixArray() {
	
	}

	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	
	/**
	 * Implemented for the Corpus interface. 
	 * @returns the vocabulary that compriseds this corpus
	 */
	public Vocabulary getVocabulary() {
		return corpus.vocab;
	}
	
	
	/** Implemented for the Corpus interface.
	 */
	public int getNumSentences() {
		return corpus.getNumSentences();
	}
	
	
	
	/** Implemented for the Corpus interface.
	 */
	public int getNumWords() {
		return corpus.size();
	}
	
	
	
	/** Implemented for the Corpus interface.
	 */
	public Phrase getSentence(int sentenceIndex) {
		return corpus.getSentence(sentenceIndex);
	}
	
	
	/**
	 * @return the phrase spanning the specificied indicies in the corpus.
	 */
	public Phrase getPhrase(int startPosition, int endPosition) {
		return corpus.getPhrase(startPosition, endPosition);
	}
	
	
		
	/**
	 * @returns the number of time that the specified phrase occurs 
	 * in the corpus.
	 */
	public int getNumOccurrences(Phrase phrase) {
		int[] bounds = findPhrase(phrase);
		if(bounds == null) return 0;
		int numOccurrences = (bounds[1]-bounds[0]) +1;
		return numOccurrences;
	}
	

	/**
	 * Returns the number of suffixes in the suffix array, which is
	 * identical to the length of the corpus.
	 */
	public int size() {
		return suffixes.length;
	}
	
	/** 
	 * @return the position in the corpus corresponding to the 
	 * specified index in the suffix array.
	 */
	public int getCorpusIndex(int suffixIndex) {
		return suffixes[suffixIndex];
	}
	
	/**
	 * @return the sentence number corresponding the specified 
	 * corpus index.
	 */
	public int getSentenceIndex(int corpusIndex) {
		return corpus.getSentenceIndex(corpusIndex);
	}
	
	/**
	 * @return the corpus index that corresponds to the start
	 * of the sentence.
	 */
	public int getSentencePosition(int sentenceIndex) {
		return corpus.getSentencePosition(sentenceIndex);
	}
	

	//===========================================================
	// Methods
	//===========================================================
	
	
	
	/** Returns a list of the sentence numbers which contain 
	  * the specified phrase.
	  * @param phrase the phrase to look for
	  * @return a list of the sentence numbers
	  */
	public int[] findSentencesContaining(Phrase phrase) {
		return findSentencesContaining(phrase, Integer.MAX_VALUE);
	}
	
	
	/** Returns a list of the sentence numbers which contain 
	  * the specified phrase.
	  * @param phrase the phrase to look for
	  * @param maxSentences the maximum number of sentences to return
	  * @return a list of the sentence numbers
	  */
	public int[] findSentencesContaining(Phrase phrase, int maxSentences) {
		int[] bounds = findPhrase(phrase);
		if(bounds == null) return null;
		int numOccurrences = (bounds[1]-bounds[0]) +1;

		int[] sentences = new int[Math.min(maxSentences, numOccurrences)];
		for(int i = 0; i < sentences.length; i++) {
			sentences[i] = corpus.getSentenceIndex(getCorpusIndex(bounds[0]+i));
		}
		return sentences;
	}


	
	/**
	 * Finds a phrase in the suffix array.  
	 * @param phrase the search phrase
	 * @return a tuple containing the start and the end bounds in the suffix array
	 * for the phrase
	 */
	public int[] findPhrase(Phrase phrase) {
		return findPhrase(phrase, 0, phrase.size());	
	}
	


//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

	/**
	 * Constructs an auxiliary array that stores longest common prefixes.  
	 * The length of the array is the corpus size+1.  Each elements lcp[i]
	 * indicates the length of the common prefix between two positions 
	 * s[i-1] and s[i] in the suffix array.
	 *
	 */
	protected int[] calculateLongestCommonPrefixes() {
		int[] longestCommonPrefixes = new int[suffixes.length +1];
		for(int i = 1; i < suffixes.length; i++) {
			int commonPrefixSize = 0;
			while((corpus.getWordID(suffixes[i]  + commonPrefixSize) ==
				   corpus.getWordID(suffixes[i-1]+ commonPrefixSize)
				  && commonPrefixSize <= MAX_COMPARISON_LENGTH)) {
				commonPrefixSize++;
			}
			longestCommonPrefixes[i] = commonPrefixSize;
		}
		longestCommonPrefixes[0] = 0;
		longestCommonPrefixes[suffixes.length] = 0;
		return longestCommonPrefixes;
	}

	
	// ccb - debugging - temporarially granting public access to findPhrase 
	// should be protected
	
	/**
	 * Finds a phrase in the suffix array.  The phrase is extracted
	 * from the sentence given the start and end points.     
	 * @param sentence the sentence/superphrase to draw the search phrase from
	 * @param phraseStart the start of the phrase in the sentence (inclusive)
	 * @param phraseEnd the end of the phrase in the sentence (exclusive)
	 * @return a tuple containing the start and the end bounds in the suffix array
	 * for the phrase
	 */
	public int[] findPhrase(Phrase sentence, int phraseStart, int phraseEnd) {
		return findPhrase(sentence.getWordIDArray(), phraseStart, phraseEnd);
	}
	
	
	/**
	 * Finds a phrase in the suffix array.  The phrase is extracted
	 * from the sentence given the start and end points.     
	 * @param wordIDs an in representatino of the sentence/superphrase to draw the search phrase from
	 * @param phraseStart the start of the phrase in the sentence (inclusive)
	 * @param phraseEnd the end of the phrase in the sentence (exclusive)
	 * @return a tuple containing the start and the end bounds in the suffix array
	 * for the phrase, or null if the phrase is not found.
	 */
	public int[] findPhrase(int[] wordIDs, int phraseStart, int phraseEnd) {
		return findPhrase(wordIDs, phraseStart, phraseEnd, 0, suffixes.length-1);
	}
	
	
	
	/**
	 * Finds a phrase in the suffix array.  The phrase is extracted
	 * from the sentence given the start and end points. This version
	 * of the method allows bounds to be specified in the suffix array,
	 * which is useful when searching for increasingly longer subrphases 
	 * in a sentences. 
	 *
	 * @param wordIDs an in representatino of the sentence/superphrase to draw the search phrase from
	 * @param phraseStart the start of the phrase in the sentence (inclusive)
	 * @param phraseEnd the end of the phrase in the sentence (exclusive)
	 * @param lowerBound the first index in the suffix array that will bound the search 
	 * @param upperBound the last index in the suffix array that will bound the search
	 * @return a tuple containing the start and the end bounds in the suffix array
	 * for the phrase, or null if the phrase is not found.
	 */
	public int[] findPhrase(int[] wordIDs, int phraseStart, int phraseEnd, int lowerBound, int upperBound) {
		int[] bounds = new int[2];
		lowerBound = findPhraseBound(wordIDs, phraseStart, phraseEnd, lowerBound, upperBound, true);
		if (lowerBound < 0) return null;
		upperBound = findPhraseBound(wordIDs, phraseStart, phraseEnd, lowerBound, upperBound, false);
		bounds[0]=lowerBound;
		bounds[1]=upperBound;
		return bounds;	
	}
	


// ccb - debugging -- writing a method to find all subphrases within a sentence, using the "bounds" trick 
// that recognizes that all phrases starting with the same sequence will be found within a particular bounds.

	public HashMap findAllSubphrases(Phrase sentence) {
		HashMap phraseToBoundsMap = new HashMap();
		int[] wordIDs = sentence.getWordIDArray();
		for(int phraseStart = 0; phraseStart < wordIDs.length; phraseStart++) {
			// the one word case...
			int phraseEnd = phraseStart+1;
			Phrase subphrase = sentence.subPhrase(phraseStart, phraseEnd);
			// note that the bounds may be null if the word doesn't occur in the corpus
			int[] bounds = findPhrase(wordIDs, phraseStart, phraseEnd, 0, suffixes.length-1);
			phraseToBoundsMap.put(subphrase, bounds);

			// multi-word phrases...
			for(phraseEnd = phraseStart+2; phraseEnd <= wordIDs.length && bounds != null; phraseEnd++) {
				// incrementally longer subphrases occur in the suffix array within
				//  the bounds of the previous subphrase, giving improved efficiency 
				int lowerBound = bounds[0];
				int upperBound = bounds[1];
				bounds = findPhrase(wordIDs, phraseStart, phraseEnd, lowerBound, upperBound);
				if(bounds != null) {
					subphrase = sentence.subPhrase(phraseStart, phraseEnd);
					phraseToBoundsMap.put(subphrase, bounds);
				}
			} 
		}
		return phraseToBoundsMap;
	}



	
	/** 
	 * Sorts the initalized, unsorted suffixes.  Uses quick sort and
	 * the compareSuffixes method defined in CorpusArray.
	 */ 
    protected void sort(int[] suffixes) {
        qsort(suffixes, 0, suffixes.length - 1);
    }


	/**
	 * Creates a string of the semi-infinite strings in the corpus array.
	 * Only use this on small suffixArrays!
	 */
	public String toString() {
		String str = "";
		for(int i = 0; i < suffixes.length; i++) {
			Phrase phrase = corpus.getPhrase(getCorpusIndex(i), corpus.size());
			str += phrase.toString() + "\n";
		}
		return str;
	}
	
//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================


	/** part of the quick sort implementation. */
    private void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
	
	/** part of the quick sort implementation. */	
    private int partition(int[] array, int begin, int end) {
        int index = begin + RAND.nextInt(end - begin + 1);
        int pivot = array[index];
        swap(array, index, end);        
        for (int i = index = begin; i < end; ++ i) {
            if (corpus.compareSuffixes(array[i], pivot, MAX_COMPARISON_LENGTH) <= 0) {
                swap(array, index++, i);
            }
        }
        swap(array, index, end);        
        return (index);
    }

	/** Quick sort */	
    private void qsort(int[] array, int begin, int end) {
        if (end > begin) {
            int index = partition(array, begin, end);
            qsort(array, begin, index - 1);
            qsort(array, index + 1,  end);
        }
    }


	/** Finds the first or last occurrence of a phrase in the suffix array, within a subset
	  * of the suffix array that is bounded by suffixArrayStart and suffixArrayEnd.  For efficiency
	  * of looking up all subphrases in a sentence we do not require that multplie int[]s 
	  * be created for each subphrase.  Instead this method will look for the subphrase within
	  * the sentence between phraseStart and phraseEnd.  
	  *
	  * @param sentence the sentence/superphrase in int representation to draw the search phrase from
	  * @param phraseStart the start of the phrase in the sentence (inclusive)
	  * @param phraseEnd the end of the phrase in the sentence (exclusive)
	  * @param suffixArrayStart the point at which to start the search in the suffix array
	  * @param suffixArrayEnd the end point in the suffix array beyond which the search doesn't
	  *        need to take place
	  * @param findFirst a flag that indicates whether we should find the first or last 
	  *        occurrence of the phrase
	  */
	private int findPhraseBound(int[] sentence, int phraseStart, int phraseEnd,
								int suffixArrayStart, int suffixArrayEnd, boolean findFirst) {
		int low = suffixArrayStart;
		int high = suffixArrayEnd;
		
		// Do a binary search between the low and high points
		while (low <= high) {
			int mid = (low + high) >> 1;
			int start = suffixes[mid];
			int diff = corpus.comparePhrase(start, sentence, phraseStart, phraseEnd);
			if (diff==0) {
				// If the difference between the search phrase and the phrase in the corpus 
				// is 0, then we have found it.  However, there might be multiple matches in
				// the corpus, so we need to continue searching until we find the end point
				int neighbor = mid;
				if (findFirst) {
					neighbor--;
				} else {
					neighbor++;
				}
				if (neighbor>=suffixArrayStart && neighbor<=suffixArrayEnd) {
					int nextDiff = corpus.comparePhrase(suffixes[neighbor],sentence,phraseStart,phraseEnd);
					if (nextDiff == 0) {
						// There's another equivalent phrase, so we need to specify 
						// in which direction to continue searching
						if (findFirst) diff = 1; //search lower
						else diff = -1; //search higher
					}					
				}
			}
			if (diff < 0) {
				low = mid + 1;
			} else if (diff > 0) {
				high = mid - 1;
			} else {
				return mid; //this is the edge
			}
		}
		return -1; // key not found.
	}
	
//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

	public static void main(String[] args) throws IOException
	{
		// ccb - debugging
		String lang = args[0];
		String corpusName = args[1];
		String directory = args[2];
		int ngramLength = Integer.parseInt(args[3]);
		SuffixArray suffixArray = SuffixArrayFactory.loadSuffixArray(lang, corpusName, directory);
		for(int i = 0; i < suffixArray.suffixes.length; i++) {
			try {
				System.out.println(suffixArray.getPhrase(suffixArray.suffixes[i], suffixArray.suffixes[i] + ngramLength));
			} catch(Exception e) {
				// do nothing if we run out of the array bounds.
			}
		}
	}
}

