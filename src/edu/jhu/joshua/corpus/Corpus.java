package edu.jhu.joshua.corpus;

// Imports
import edu.jhu.util.sentence.Phrase;
import edu.jhu.util.sentence.Vocabulary;

/**
 * Corpus is an interface that contains methods for accessing
 * the information within a monolingual corpus.
 *
 * @author Chris Callison-Burch
 * @since  7 February 2005
 *
 * The contents of this file are subject to the Linear B Community Research 
 * License Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.linearb.co.uk/developer/. Software distributed under the License
 * is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either 
 * express or implied. See the License for the specific language governing 
 * rights and limitations under the License. 
 *
 * Copyright (c) 2005 Linear B Ltd. All rights reserved.
 */

public interface Corpus {

//===============================================================
// Method definitions
//===============================================================
	
	/**
	 * @returns the vocabulary that this corpus is comprised of.
	 */
	public Vocabulary getVocabulary();
	
	
	/**
	 * @returns the number of sentences in the corpus.
	 */
	public int getNumSentences();
	
	
	/**
	 * @returns the number of words in the corpus.
	 */
	public int getNumWords();
	
	
	/**
	 * @returns the sentence at the specified index
	 */
	public Phrase getSentence(int sentenceIndex);
	
	
	/**
	 * @returns the number of time that the specified phrase occurs 
	 * in the corpus.
	 */
	public int getNumOccurrences(Phrase phrase);
	

	/** Returns a list of the sentence numbers which contain 
	  * the specified phrase.
	  * @param the phrase to look for
	  * @return a list of the sentence numbers
	  */
	public int[] findSentencesContaining(Phrase phrase);
	
	
	/** Returns a list of the sentence numbers which contain 
	  * the specified phrase.
	  * @param the phrase to look for
	  * @param the maximum number of sentences to return
	  * @return a list of the sentence numbers
	  */
	public int[] findSentencesContaining(Phrase phrase, int maxSentences);
		
}

