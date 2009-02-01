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
package joshua.util.sentence;


import java.io.IOException;
import java.util.*;

import joshua.sarray.BasicPhrase;
import joshua.sarray.SuffixArrayFactory;


/**
 * Vocabulary is the class that keeps track of the unique words
 * that occur in a corpus of text for a particular language.  
 * It assigns integer IDs to Words, which is useful when we are
 * creating suffix arrays or doing similar things.
 *
 * @author Chris Callison-Burch
 * @since  8 February 2005
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */
public class Vocabulary implements Iterable<String> {

//===============================================================
// Constants
//===============================================================

	/**
	 * The unknown word's ID will be the size of the vocabulary,
	 * ensuring that it is outside of the vocabulary. Note that
	 * for vocabularies which have not been fixed yet, this
	 * means the actual value is volatile and therefore a word
	 * ID can only be compared against UNKNOWN_WORD at the time
	 * the word ID is generated (otherwise unknown words can
	 * become "known" if new words are added to the vocabulary
	 * before testing).
	 *
	 * Negative IDs are reserved for non-terminals and therefore
	 * cannot be used to signify the UNKNOWN_WORD. @todo Perhaps
	 * we could use 0 as the UNKNOWN_WORD (leaving negatives
	 * for non-terminals, and positives for terminals), to
	 * enable us to get away from all the book-keeping.
	 */
	int UNKNOWN_WORD;

	/** String representation for out-of-vocabulary words. */
	public static final String UNKNOWN_WORD_STRING = "UNK";

//===============================================================
// Member variables
//===============================================================

	protected Map<String,Integer> wordToIDMap;
	protected List<String>        vocabList;
	
	/** Determines whether new words may be added to the vocabulary. */
	protected boolean isFixed;
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor creates an empty vocabulary.
	 */
	public Vocabulary() {
		//XXX Is wordToIdMap accessed by multiple threads? If not, should use HashMap instead of Hashtable.
		wordToIDMap = new Hashtable<String,Integer>();  
		vocabList = new Vector<String>();
		isFixed = false;
		UNKNOWN_WORD = 0; // Initially, the vocab size is zero
	}

	/** 
	 * Constructor creates a fixed vocabulary from the given set of words.
	 */
	public Vocabulary(Set<String> words) {
		//XXX Is wordToIdMap accessed by multiple threads? If not, should use HashMap instead of Hashtable.
		wordToIDMap = new Hashtable<String,Integer>();
		vocabList = new Vector<String>();
		vocabList.addAll(words);
		alphabetize();
		isFixed = true;
		UNKNOWN_WORD = vocabList.size();
	}
	
	/**
	 * Constructs a vocabulary from a text file.
	 * 
	 * @param fileName Filename of a text file.
	 * @throws IOException
	 */
	public Vocabulary(String fileName) throws IOException {
		this();
		SuffixArrayFactory.createVocabulary(fileName, this);
		UNKNOWN_WORD = vocabList.size();
	}
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	

	/**
	 * Gets an integer identifier for the word.
	 * <p>
	 * If the word is in the vocabulary,
	 * the integer returned will uniquely identify that word.
	 * 
	 * If the word is not in the vocabulary,
	 * the constant  <code>UNKNOWN_WORD</code> will be returned.
	 * 
	 * @return the unique integer identifier for wordString, 
	 *         or UNKNOWN_WORD if wordString is not in the vocabulary
	 */
	public int getID(String wordString) {
		Integer ID = wordToIDMap.get(wordString);
		if (ID == null) {
			return UNKNOWN_WORD;
		} else {
			return ID.intValue();
		}
	}

	/**
	 * Gets the integer identifiers 
	 * for all words in the provided sentence.
	 * <p>
	 * The sentence will be split (on spaces) into words,
	 * then the integer identifier for each word 
	 * will be retrieved using <code>getID</code>.
	 * 
	 * @see getID(String)
	 * @param sentence String of words, separated by spaces.
	 * @return Array of integer identifiers for each word in the sentence
	 */
	public int[] getIDs(String sentence) {
		String[] words = sentence.split(" ");
		int[] wordIDs = new int[words.length];
		
		for (int i=0; i<words.length; i++) {
			wordIDs[i] = getID(words[i]);
		}
		
		return wordIDs;
	}
	
	
	
	/**
	 * Gets the String that corresponds to the specified integer identifier.
	 * 
	 * @return the String that corresponds to the specified integer identifier,
	 *         or <code>UNKNOWN_WORD_STRING</code> if the identifier 
	 *         does not correspond to a word in the vocabulary
	 */
	public String getWord(int wordID) {
		if (wordID >= vocabList.size() || wordID < 0) {
			return UNKNOWN_WORD_STRING;
		}
		return vocabList.get(wordID);
	}
	
	public String getWords(int[] wordIDs, boolean ntIndexIncrements) {
		StringBuilder s = new StringBuilder();
		
		int nextNTIndex = 1;
		for(int t=0; t<wordIDs.length; t++){
			if(t>0) {
				s.append(' ');
			}
			
			int wordID = wordIDs[t];
			
			if (wordID >= vocabList.size()) { 
				s.append(UNKNOWN_WORD_STRING);
			} else if (wordID < 0) {
				s.append("[X,"); //XXX This should NOT be hardcoded here!
				if (ntIndexIncrements) {
					s.append(nextNTIndex++);
				} else {
					s.append(-1*wordID);
				}
				s.append(']');
			} else {
				s.append(vocabList.get(wordID));
			}

		}
		
		return s.toString();
	}
	
	
	/**
	 * @return an Interator over all words in the Vocabulary.
	 */
	public Iterator<String> iterator() {
		return vocabList.iterator();
	}
	
	public Collection<Integer> getAllIDs() {
		return wordToIDMap.values(); 
	}
	
	/**
	 * Gets the list of all words represented by this vocabulary.
	 * 
	 * @return the list of all words represented by this vocabulary
	 */
	public List<String> getWords() {
		return vocabList;
	}
	
	
	/** 
	 * Adds a word to the vocabulary.
	 * 
	 * @return the ID of the word, or UNKNOWN_WORD if 
	 *         the word is new and the vocabulary is fixed.
	 */
	public int addWord(String wordString) {
		Integer ID = wordToIDMap.get(wordString);
		if (ID != null) {
			return ID.intValue();
		} else if(!isFixed) {
			ID = new Integer(vocabList.size());
			vocabList.add(wordString);
			wordToIDMap.put(wordString, ID);
			UNKNOWN_WORD = vocabList.size();
			return ID.intValue();
		}  else {
			return UNKNOWN_WORD;
		}
	}
		
	/**
	 * Gets the number of unique words in the vocabulary.
	 * 
	 * @return the number of unique words in the vocabulary.
	 */
	public int size() {
		return vocabList.size();
	}
	
	/** 
	 * Fixes the size of the vocabulary so that new words
	 * may not be added.
	 */
	public void fixVocabulary() {
		isFixed = true;
	}
	
	/**
	 * Determines if the phrase contains any words
	 * that are not in the vocabulary.
	 * 
	 * @return <code>true</code> if there are unknown words in the phrase,
	 *         <code>false</code> otherwise
	 */
	public boolean containsUnknownWords(BasicPhrase phrase) {
		for(int i = 0; i < phrase.size(); i++) {
			if(phrase.getWordID(i) == UNKNOWN_WORD) return true;
		}
		return false;
	}
	
	
	/**
	 * Checks that the Vocabularies are the same, by first checking that they have
	 * the same number of items, and then checking that each word corresponds
	 * to the same ID.
	 * 
	 * @param o the object to check equivalence with
	 * @return <code>true</code> if the other object is 
	 *         a Vocabulary representing the same set of
	 *         words with identically assigned IDs,
	 *         <code>false</code> otherwise
	 */
	public boolean equals(Object o) {
		if (o==this) return true;
		else if (o==null) return false;
		else if (!o.getClass().isInstance(this)) return false;
        else {
            Vocabulary other = (Vocabulary) o;
			if(other.size() != this.size()) return false;
			for(int i = 0; i < this.size(); i++) {
				String thisWord = (String) this.vocabList.get(i);
				String otherWord = (String) other.vocabList.get(i);
				if(!(thisWord.equals(otherWord))) return false;
				Integer thisID = this.wordToIDMap.get(thisWord);
				Integer otherID = other.wordToIDMap.get(otherWord);
				if(thisID != null && otherID != null) {
					if(!(thisID.equals(otherID))) return false;
				}
			}
			return true;
        }
	}
	
	//===========================================================
	// Methods
	//===========================================================
	
	
	public String toString() {
		return vocabList.toString();
	}
	
	
	
	/** 
	 * Sorts the vocabulary alphabetically and re-assigns IDs
	 * in ascending order.
	 */
	public void alphabetize() {
		// alphabetize 
		Collections.sort(vocabList);
		// re-assign IDs
		//XXX Is wordToIdMap accessed by multiple threads? If not, should use HashMap instead of Hashtable.
		wordToIDMap = new Hashtable<String,Integer>();
		for(int i = 0; i < vocabList.size(); i++) {
			String wordString = vocabList.get(i);
			wordToIDMap.put(wordString, new Integer(i));
		}
		UNKNOWN_WORD = getID(UNKNOWN_WORD_STRING);
	}
	
//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
	
	
//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
//===============================================================
// Static
//===============================================================


	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.out.println("Usage: java Vocabulary file corpusName lang outputDir");
			System.exit(0);
		}
		
		String inputFilename = args[0];
		String corpusName = args[1];
		String lang = args[2];
		String outputDirectory = args[3];
		
		// Create a suffix array-style vocabulary file
		Vocabulary vocab = new Vocabulary();
		//int[] numberOfWordsSentences = 
			SuffixArrayFactory.createVocabulary(inputFilename, vocab);
		SuffixArrayFactory.saveVocabulary(vocab, lang, corpusName, outputDirectory);
	}

}

