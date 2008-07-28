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
package edu.jhu.joshua.corpus;

// Imports
import edu.jhu.util.sentence.*;

/**
 * ParallelCorpus is an interface that contains methods for accessing
 * the information in a bilingual aligned corpus.
 *
 * @author Chris Callison-Burch
 * @since  7 February 2005
 * @version $LastChangedDate$
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

