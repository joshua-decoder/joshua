package edu.jhu.joshua.corpus;

// Imports
import edu.jhu.util.sentence.*;

/**
 * ParallelCorpus is an interface that contains methods for accessing
 * the information in a bilingual aligned corpus.
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

public interface ParallelCorpus {

//===============================================================
// Method definitions
//===============================================================
	
	/**
	 * @return the source corpus 
	 */
	public Corpus getSourceCorpus();

	/** 
	 * @return the target corpus 
	 */	
	public Corpus getTargetCorpus();
	
	
	/**
	 * @returns the number of sentences in the corpus.
	 */
	public int getNumSentences();
	
	/** 
	 * @return the alignment for the specified sentence pair.
	 */
	public Alignment getSentencePair(int sentencePairNumber);
	
	
	/** 
	 * @return the phrase extractor currently in use by the getTranslations methods.
	 */
	public PhraseExtractor getPhraseExtractor();
	
			
	/** 
	 * @return the phrase extractor to use in the getTranslations methods.
	 */
	public void setPhraseExtractor(PhraseExtractor phraseExtractor);


	/** Collects all of the translations for the specified source phrase
	  * and returns a collection of the translations (in the form of 
	  * Alignments) associated with their frequency.
	  * @param sourcePhrase the phrase to find translations of
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfSource(Phrase sourcePhrase, TranslationResultsSet resultsSet);
	
	
	/** Collects up to a specified number of translations of the 
	  * source phrase and returns their frequencies. 
	  * @param sourcePhrase the phrase to find translations of
	  * @param maxInstances the maximum number of translations to collect
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfSource(Phrase sourcePhrase, int maxInstances, TranslationResultsSet resultsSet);



	/** Collects all of the translations for the specified target phrase
	  * and returns a collection of the translations (in the form of 
	  * Alignments) associated with their frequency.
	  * @param targetPhrase the phrase to find translations of
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfTarget(Phrase targetPhrase, TranslationResultsSet resultsSet);
	
	
	/** Collects up to a specified number of translations of the 
	  * target phrase and returns their frequencies. 
	  * @param targetPhrase the phrase to find translations of
	  * @param maxInstances the maximum number of translations to collect
	  * @param resultsSet the TranslationResultsSet to add the translation to
	  */
	public void getTranslationsOfTarget(Phrase targetPhrase, int maxInstances, TranslationResultsSet resultsSet);

	

}

