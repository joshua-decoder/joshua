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

import joshua.corpus.SymbolTable;
import joshua.util.sentence.Phrase;

public abstract class AbstractCorpus implements Corpus {

	protected final SymbolTable symbolTable;
	
	public AbstractCorpus(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
	}
	
	
	/**
	 * Compares the phrase that starts at position start with
	 * the subphrase indicated by the start and end points of
	 * the phrase.
	 *
	 * @param corpusStart the point in the corpus where the
	 *                    comparison begins
	 * @param phrase      the superphrase that the comparsion
	 *                    phrase is drawn from
	 * @param phraseStart the point in the phrase where the
	 *                    comparison begins (inclusive)
	 * @param phraseEnd   the point in the phrase where the
	 *                    comparison ends (exclusive)
	 * @return an int that follows the conventions of
	 *         java.util.Comparator.compareTo()
	 */
	public int comparePhrase(int corpusStart, Phrase phrase, int phraseStart, int phraseEnd) {
		int diff = -1;
		int size = size();
		for (int i = 0; i < phraseEnd-phraseStart; i++) {
			if (i + corpusStart >= size) {
				return -1;
			}
			diff = getWordID(i+corpusStart) - phrase.getWordID(i+phraseStart);
			if (diff != 0) {
				return diff;
			}
		}
		return 0;
	}
	
	/**
	 * compares the phrase that starts at position start with
	 * the phrase passed in. Compares the entire phrase.
	 */
	public int comparePhrase(int corpusStart, Phrase phrase) {
		return comparePhrase(corpusStart, phrase, 0, phrase.size());
	}

	public int compareSuffixes(int position1, int position2,
			int maxComparisonLength) {
		
		int size = size();
		for (int i = 0; i < maxComparisonLength; i++) {
			if (position1 + i < (size - 1)
			&& position2 + i > (size - 1)) {
				return 1;
			}
			if (position2 + i < (size - 1)
			&& position1 + i > (size - 1)) {
				return -1;
			}
			int diff = getWordID(position1 + i) - getWordID(position2 + i);
			
			if (diff != 0) {
				return diff;
			}
		}
		return 0;
	}

	public abstract int getNumSentences();

	public ContiguousPhrase getPhrase(int startPosition, int endPosition) {
		return new ContiguousPhrase(startPosition, endPosition, this);
	}

	public Phrase getSentence(int sentenceIndex) {
		int numSentences = getNumSentences();
		int numWords = size();
		if (sentenceIndex >= numSentences) {
			return null;
		} else if (sentenceIndex == numSentences - 1) {
			return getPhrase(getSentencePosition(sentenceIndex), numWords);
		} else {
			return getPhrase(getSentencePosition(sentenceIndex), getSentencePosition(sentenceIndex+1));
		} 
	}


	public int getSentenceEndPosition(int sentenceId) {
		return getSentencePosition(sentenceId+1);
	}

	public abstract int getSentenceIndex(int position);

	public abstract int getSentencePosition(int sentenceId);

	public SymbolTable getVocabulary() {
		return symbolTable;
	}

	public abstract int getWordID(int position);

	public abstract int size();

}
