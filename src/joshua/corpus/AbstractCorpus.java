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
package joshua.corpus;

import java.util.Iterator;

import joshua.corpus.vocab.SymbolTable;


/**
 * This class provides a mostly-complete implementation of the
 * <code>Corpus</code> interface, designed to minimize the effort
 * required to build a concrete implementation of a corpus array
 * data structure.
 * 
 * @author Lane Schwartz
 * @author Chris Callison-Burch
 */
public abstract class AbstractCorpus<Vocab extends SymbolTable> implements Corpus {

	/** 
	 * Symbol table for the corpus, responsible for mapping
	 * between tokens in the corpus and the integer representations
	 * of those tokens.
	 */
	protected Vocab symbolTable;
	
	/**
	 * Constructs an abstract corpus with the specified symbol
	 * table.
	 *
	 * @param symbolTable Symbol table for the corpus, responsible
	 *            for mapping between tokens in the corpus and
	 *            the integer representations of those tokens
	 */
	public AbstractCorpus(Vocab symbolTable) {
		this.symbolTable = symbolTable;
	}
	
	
	/* See Javadoc for Corpus interface. */
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
	
	/* See Javadoc for Corpus interface. */
	public int comparePhrase(int corpusStart, Phrase phrase) {
		return comparePhrase(corpusStart, phrase, 0, phrase.size());
	}

	/* See Javadoc for Corpus interface. */
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

	/* See Javadoc for Corpus interface. */
	public abstract int getNumSentences();

	/* See Javadoc for Corpus interface. */
	public ContiguousPhrase getPhrase(int startPosition, int endPosition) {
		return new ContiguousPhrase(startPosition, endPosition, this);
	}

	/* See Javadoc for Corpus interface. */
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

	/* See Javadoc for Corpus interface. */
	public int getSentenceEndPosition(int sentenceId) {
		return getSentencePosition(sentenceId+1);
	}

	/* See Javadoc for Corpus interface. */
	public abstract int getSentenceIndex(int position);

	/* See Javadoc for Corpus interface. */
	public abstract int getSentencePosition(int sentenceId);

	/* See Javadoc for Corpus interface. */
	public int[] getSentenceIndices(int[] positions) {
		int size = positions.length;
		int[] sentenceNumber = new int[size];
		for (int i=0; i<size; i++) {
			sentenceNumber[i] = getSentenceIndex(positions[i]);
		}
		return sentenceNumber;
	}
	
	/* See Javadoc for Corpus interface. */
	public SymbolTable getVocabulary() {
		return symbolTable;
	}

	/* See Javadoc for Corpus interface. */
	public abstract int getWordID(int position);

	/* See Javadoc for Corpus interface. */
	public abstract int size();

	/* See Javadoc for Corpus interface. */
	public Iterable<Integer> corpusPositions() {	
		final int size = size();
		
		return new Iterable<Integer>() {
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {

					int position = 0;
					
					public boolean hasNext() {
						return position < size;
					}

					public Integer next() {
						int result = position;
						position += 1;
						return result;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}
			
		};
	}

}
