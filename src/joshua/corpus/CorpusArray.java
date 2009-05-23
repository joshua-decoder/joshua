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

import joshua.corpus.suffix_array.SuffixArray;
import joshua.corpus.suffix_array.SuffixArrayFactory;
import joshua.corpus.vocab.ExternalizableSymbolTable;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.io.BinaryOut;

import java.io.Externalizable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;



/**
 * A compact int[] based representation of a corpus. The class keeps
 * all of the words in their int form in a single array. It also
 * maintains a separate int[] array that lists the start index for
 * each sentence in the corpus. This second array allows us to
 * quickly determine the source sentence of any given position in
 * the corpus using a binary search.
 *
 * @author  Josh Schroeder
 * @since  29 Dec 2004
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */
public class CorpusArray extends AbstractCorpus<ExternalizableSymbolTable> implements Corpus, Externalizable {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	/**
	 * Stores an integer based representation of each word in
	 * the corpus.
	 */
	protected int[] corpus;
	
	
	/**
	 * Keeps the starting position in the corpus array for each
	 * of the sentences. The length of the sentences array is
	 * equal to the number of sentences in the corpus.
	 */
	protected int[] sentences;
	
	
	/**
	 * The alphabetized vocabulary which maps between the String
	 * and int representation of words in the corpus.
	 */
//	protected SymbolTable symbolTable;
	
	
//===============================================================
// Constructor(s)
//===============================================================

	/** 
	 * Constructs an empty corpus.
	 * <p>
	 * NOTE: Primarily needed for Externalizable interface.
	 */
	public CorpusArray() {
		super(new Vocabulary());
//		this.symbolTable = new Vocabulary();
		this.sentences = new int[]{};
		this.corpus = new int[]{};
	}
	
	
	/** 
	 * Protected constructor takes in the already prepared
	 * member variables.
	 *
	 * @see SuffixArrayFactory#createCorpusArray
	 */
	public CorpusArray (int[] corpus, int[] sentences, ExternalizableSymbolTable vocab) {
		super(vocab);
		this.corpus = corpus;
		this.sentences = sentences;
//		this.symbolTable = vocab;
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	

	
	/**
	 * @return the integer representation of the Word at the
	 *         specified position in the corpus.
	 */
	public int getWordID(int position) {
		return corpus[position];	
	}
	
	
	/**
	 * @return the sentence index associated with the specified
	 *         position in the corpus.
	 */
	public int getSentenceIndex(int position) {
		int index = Arrays.binarySearch(sentences, position);
		// if index is positive, then the position searched
		// for is the first word of a sentence. we return
		// the exact value.
		if (index >= 0) {
				return index;
		} else {
		// otherwise, we are given an negative version of
		// the first number higher than our position. that
		// is the position of where this would be inserted
		// if it was its own sentence, so we make the number
		// positive and subtract 2 (one since since it is
		// by ith element instead of position, one to get
		// the previous index)
			return (index*(-1))-2;
		}
	}
	
	
	/**
	 * @return the position in the corpus of the first word of
	 *         the specified sentence. If the sentenceID is
	 *         outside of the bounds of the sentences, then it
	 *         returns the last position in the corpus + 1.
	 */
	public int getSentencePosition(int sentenceID) {
		if (sentenceID >= sentences.length) {
			return corpus.length;
		}
		return sentences[sentenceID];
	}
	
	/**
	 * Gets the exclusive end position of a sentence in the
	 * corpus.
	 *
	 * @return the position in the corpus one past the last
	 *         word of the specified sentence. If the sentenceID
	 *         is outside of the bounds of the sentences, then
	 *         it returns one past the last position in the
	 *         corpus.
	 */
	public int getSentenceEndPosition(int sentenceID) {
		if (sentenceID >= sentences.length-1) {
			return corpus.length;
		}
		return sentences[sentenceID+1];
	}
	
	/** 
	 * Gets the sentence at the specified index (starting from
	 * zero).
	 *
	 * @return the sentence, or null if the specified sentence
	 *         number doesn't exist
	 */
	public Phrase getSentence(int sentenceIndex) {
		if (sentenceIndex >= sentences.length) {
			return null;
		} else if (sentenceIndex == sentences.length - 1) {
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
	
	/**
	 * Sets the symbol table to the provided object, and changes
	 * migrates all internal data to use the new mappings
	 * provided by that object.
	 */
	public void setSymbolTable(ExternalizableSymbolTable vocab) {
		SymbolTable oldVocab = this.symbolTable;
		
		for (int i=0; i<corpus.length; i++) {
			
			int oldID = corpus[i];
			String word = oldVocab.getWord(oldID);
			int newID = vocab.getID(word);
			
			corpus[i] = newID;
		}
		
		this.symbolTable = vocab;
		oldVocab = null;
	}
	
	
	//===========================================================
	// Methods
	//===========================================================
	
	
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
		for (int i = 0; i < phraseEnd-phraseStart; i++) {
			if (i + corpusStart >= corpus.length) {
				return -1;
			}
			diff = corpus[i+corpusStart] - phrase.getWordID(i+phraseStart);
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
	
	public SymbolTable getVocabulary() {
		return symbolTable;
	}
	
	
	/** 
	 * Compares the suffixes starting a positions index1 and
	 * index2.
	 *
	 * @param position1 the position in the corpus where the
	 *                  first suffix begins
	 * @param position2 the position in the corpus where the
	 *                  second suffix begins
	 * @param maxComparisonLength a cutoff point to stop the
	 *                            comparison
	 * @return an int that follows the conventions of
	 *         java.util.Comparator.compareTo()
	 */
    public int compareSuffixes(int position1, int position2, int maxComparisonLength){
		for (int i = 0; i < maxComparisonLength; i++) {
			if (position1 + i < (corpus.length)
					&& position2 + i >= (corpus.length)) {
				return 1;
			}
			if (position2 + i < (corpus.length)
					&& position1 + i >= (corpus.length)) {
				return -1;
			}
			
			int diff;
			try {
				diff = corpus[position1 + i] - corpus[position2 + i];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new Error("Bug in CorpusArray method compareSuffixes: " + e.getMessage());
			}
			
			if (diff != 0) {
				return diff;
			}
		}
		return 0;
    }

    public void write(String corpusFilename, String vocabFilename, String charset) throws IOException {
    
    	ObjectOutput vocabOut =
    		new BinaryOut(new FileOutputStream(vocabFilename), true);
//    		new ObjectOutputStream(new FileOutputStream(vocabFilename));
    	symbolTable.setExternalizableEncoding(charset);
    	symbolTable.writeExternal(vocabOut);
    	vocabOut.flush();
    	
    	BinaryOut corpusOut = new BinaryOut(new FileOutputStream(corpusFilename), false);
    	this.writeExternal(corpusOut);	
    	corpusOut.flush();
    	
    }
	
	public ContiguousPhrase getPhrase(int startPosition, int endPosition) {
		return new ContiguousPhrase(startPosition, endPosition, this);
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
		
		if (args.length < 4) {
			System.err.println("Usage: java " + SuffixArray.class.getName() + " corpus vocab.jbin corpus.bin");
			System.exit(0);
		}
		
		String corpusFileName = args[0];
		String binaryVocabFilename = args[1];
		String binaryCorpusFilename = args[2];
		String charset = (args.length > 3) ? args[3] : "UTF-8";
		
		Vocabulary symbolTable = new Vocabulary();
		int[] lengths = Vocabulary.initializeVocabulary(corpusFileName, symbolTable, true);
		
		CorpusArray corpusArray = SuffixArrayFactory.createCorpusArray(corpusFileName, symbolTable, lengths[0], lengths[1]);
		
		corpusArray.write(binaryCorpusFilename, binaryVocabFilename, charset);
		
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		// Read the vocabulary
		symbolTable.readExternal(in);
		
		int numSentences = in.readInt();
		this.sentences = new int[numSentences];
		for (int i=0; i<numSentences; i++) {
			this.sentences[i] = in.readInt();
		}
		
		int numWords = in.readInt();
		this.corpus = new int[numWords];
		for (int i=0; i<numWords; i++) {
			this.corpus[i] = in.readInt();
		}
		
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		
		// Write the vocabulary
		out.writeObject(symbolTable);
		
		out.writeInt(sentences.length);
		for (int sentencePosition : sentences) {
			out.writeInt(sentencePosition);
		}
		
		out.writeInt(corpus.length);
		for (int word : corpus) {
			out.writeInt(word);
		}
		
	}

//	static final long serialVersionUID = 1L;
}
