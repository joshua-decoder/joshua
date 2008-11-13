/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.sarray;

import joshua.util.sentence.Phrase;
import joshua.util.sentence.Vocabulary;


/**
 * Corpus is an interface that contains methods for accessing the
 * information within a monolingual corpus.
 *
 * @author Chris Callison-Burch
 * @since  7 February 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */

public interface Corpus {

//===============================================================
// Method definitions
//===============================================================
	
	/**
	 * @return the vocabulary that this corpus is comprised of.
	 */
	public Vocabulary getVocabulary();
	
	
	/**
	 * @return the number of sentences in the corpus.
	 */
	public int getNumSentences();
	
	
	/**
	 * @return the number of words in the corpus.
	 */
	public int getNumWords();
	
	
	/**
	 * @return the sentence at the specified index
	 */
	public Phrase getSentence(int sentenceIndex);
	
	
	/**
	 * @return the number of time that the specified phrase
	 *         occurs in the corpus.
	 */
	public int getNumOccurrences(Phrase phrase);
	

	/**
	 * Returns a list of the sentence numbers which contain the
	 * specified phrase.
	 *
	 * @param the phrase to look for
	 * @return a list of the sentence numbers
	 */
	public int[] findSentencesContaining(Phrase phrase);
	
	
	/**
	 * Returns a list of the sentence numbers which contain the
	 * specified phrase.
	 *
	 * @param the phrase to look for
	 * @param the maximum number of sentences to return
	 * @return a list of the sentence numbers
	 */
	public int[] findSentencesContaining(Phrase phrase, int maxSentences);
	
}

