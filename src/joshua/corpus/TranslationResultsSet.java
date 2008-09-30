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
import edu.jhu.util.math.Probability;
import edu.jhu.util.Counts;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * TranslationResultsSet is the class that keeps track of the translations
 * for a set of phrases that are retrieved from a parallel corpus.  Provides
 * basic functionality for calculating the maximum likelihood estimation
 * probabilities, iterating over the translations for each source phrase.
 *
 * @author Chris Callison-Burch
 * @since  14 February 2005
 * @version $LastChangedDate$
 */
public class TranslationResultsSet {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	protected HashMap phraseToTranslationMap;
	protected boolean keyWithSource;
	
	/** Optionally stores an array list of sentence pair numbers
	  * that the translations occur in. */
	protected HashMap sentencePairs;
	protected boolean retainSentencePairs;
	
	
//===============================================================
// Constructor(s)
//===============================================================


	public TranslationResultsSet() {
		this(true);
	}


	/** Allows the user to specify whether the translations should be 
	  * stored using the source phrase or the target phrase as a key.
	  */
	public TranslationResultsSet(boolean keyWithSource) {
		phraseToTranslationMap = new HashMap();
		this.keyWithSource = keyWithSource;
		sentencePairs = new HashMap();
		retainSentencePairs = false;
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/**
	 * Determines whether to retains the sentence pair numbers of each
	 * translation.
	 */
	// ccb - todo - might want to have it take an int value that indcates how
	// many sentence pairs to retain instead of a boolean
	public void setRetainSentencePairs(boolean retainSentencePairs) {
		this.retainSentencePairs = retainSentencePairs;
	}
	
	/**
	 * @return a list of indicies of the sentence pairs that the translation occurred in,
	 * or an empty list if none
	 */
	public int[] getSentencePairIndicies(Alignment translation) {
		ArrayList indicies = (ArrayList) sentencePairs.get(translation);
		if(indicies == null) return new int[0];
		int[] intIndicies = new int[indicies.size()]; 
		for(int i = 0; i < indicies.size(); i++) {
			Integer index = (Integer) indicies.get(i);
			intIndicies[i] = index.intValue();
		}
		return intIndicies;
	}
	
	

	/**
	 * @return the number of times that we've seen this translation
	 */
	public int getCount(Alignment translation) {
		Counts translations = (Counts) phraseToTranslationMap.get(getKey(translation));
		if(translations == null) return 0;
		return translations.getCount(translation);
	}
	
	
	/**
	 * @return the number of times that the specified phrase occurred
	 */
	public int getTotalCount(Phrase phrase) {
		Counts translations = (Counts) phraseToTranslationMap.get(phrase);
		if(translations == null) return 0;
		return translations.getTotalCount();
	}
	
		
	/**
	 * @return the number of unique translations for the specified phrase
	 */
	public int getNumTranslations(Phrase phrase) {
		Counts translations = (Counts) phraseToTranslationMap.get(phrase);
		if(translations == null) return 0;
		return translations.getNumObjects();
	}
	
	
	/**
	 * Calculates the translation probability using maximum likelihood estimation.
	 */
	public Probability getProbability(Alignment translation) {
		double numerator = (double) getCount(translation);
		double denominator = (double) getTotalCount(getKey(translation));
		if(denominator == 0) return new Probability(0.0);
		double probability =  numerator / denominator;
		return new Probability(probability);
	}


	
	
	//===========================================================
	// Methods
	//===========================================================


	
	/** 
	  * Increments the number of times we have seen this translation.
	  *
	  * @param translation the translation that is observed
	  * @param sentenceIndex the index of the sentence pair that it was drawn from
	  * @param span the span of the sentence pair that the translation was drawn from
	  */
	public void incrementCount(Alignment translation, int sentenceIndex, Span span) {
		incrementCount(getKey(translation), translation);
		//if we are retraining the sentence pair occurrences, then record sentenceIndex.
		if(retainSentencePairs) {
			occursInSentencePair(translation, sentenceIndex);
		}
	}


	/** 
	 * @return the translations of the phrase. 
	 *
	 */
	public ArrayList getTranslations(Phrase phrase) {
		Counts translations = (Counts) phraseToTranslationMap.get(phrase);
		if(translations == null) return new ArrayList();
		return translations.keys();
	}
	
	
	/** 
	 * @return the translations of the phrase, sorted by their translation probabilities. 
	 */
	public ArrayList getTranslations(Phrase phrase, boolean sortAscending) {
		Counts translations = (Counts) phraseToTranslationMap.get(phrase);
		if(translations == null) return new ArrayList();
		return translations.keys(sortAscending);
	}
	
	
	/** 
	 * @return an iterator over the phrase keys which have 
	 * translations stored in this set.
	 */
	public Iterator iterator() {
		return phraseToTranslationMap.keySet().iterator();
	}
	
	
	/** 
	 * @return an iterator over the translations of the 
	 * specified phrase key.
	 */
	public Iterator translationIterator(Phrase phrase) {
		Counts translations = (Counts) phraseToTranslationMap.get(phrase);
		if(translations == null) translations = new Counts();
		return translations.iterator();
	}
	
	
	public String toString() {
		return this.toString(Probability.ZERO);
	}


	/**
	 * Prints all phrases and translations in the results set above the specified threshold.
	 * Format is:
	 * phrase	translation	probability
	 */
	public String toString(Probability threshold) {
		StringBuffer buffer = new StringBuffer();
		Iterator it = iterator();
		while(it.hasNext()) {
			Phrase phrase = (Phrase) it.next();
			Counts translations = (Counts) phraseToTranslationMap.get(phrase);
			
			Iterator jt = translations.iterator(false);
			while(jt.hasNext()) {
				Alignment translation = (Alignment) jt.next();
				Probability prob = getProbability(translation);
				
				if(prob.isGreaterThan(threshold)) {
					buffer.append(phrase.toString());
					buffer.append("\t");
					buffer.append(translation.getTarget().toString());
					buffer.append("\t");
					buffer.append(prob.toString());
					buffer.append("\n");
				}
			}
		}
		return buffer.toString();
	}

//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

	/** 
	 * Records the indicies of the sentence pairs that the translation occurs in.
	 */
	protected void occursInSentencePair(Alignment translation, int sentenceIndex) {
		ArrayList indicies = (ArrayList) sentencePairs.get(translation);
		if(indicies == null) indicies = new ArrayList();
		indicies.add(new Integer(sentenceIndex));
		sentencePairs.put(translation, indicies);
	}
	

	protected Phrase getKey(Alignment translation) {
		if(keyWithSource) {
			return translation.getSource();
		} else {
			return translation.getTarget();
		}
	}
	
	
	protected void incrementCount(Phrase phrase, Alignment translation) {
		Counts translations = (Counts) phraseToTranslationMap.get(phrase);
		if(translations == null) translations = new Counts();
		translations.incrementCount(translation);
		phraseToTranslationMap.put(phrase, translations);
	}
	
	
	protected Counts getCounts(Phrase phrase, Alignment translation) {
		return (Counts) phraseToTranslationMap.get(phrase);
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

	public static void main(String[] args)
	{

	}
}

