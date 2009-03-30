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


import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

import joshua.corpus.AbstractSymbolTable;
import joshua.corpus.SymbolTable;
import joshua.sarray.BasicPhrase;
import joshua.sarray.SuffixArrayFactory;
import joshua.util.io.BinaryIn;


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
public class Vocabulary extends AbstractSymbolTable implements Iterable<String>, SymbolTable, Externalizable {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	protected final Map<String,Integer> nonterminalToInt;
	protected final Map<String,Integer> terminalToInt;
	protected final Map<Integer,String> intToString;
	
	/** Determines whether new words may be added to the vocabulary. */
	protected boolean isFixed;
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor creates an empty vocabulary.
	 */
	public Vocabulary() {
		nonterminalToInt = new HashMap<String,Integer>();
		terminalToInt = new HashMap<String,Integer>();  
		intToString = new HashMap<Integer,String>();
		isFixed = false;
		terminalToInt.put(UNKNOWN_WORD_STRING, UNKNOWN_WORD);
		intToString.put(UNKNOWN_WORD, UNKNOWN_WORD_STRING);
	}

	/** 
	 * Constructor creates a fixed vocabulary from the given set of words.
	 */
	public Vocabulary(Set<String> words) {
		this();

		for (String word : words) {
			this.addTerminal(word);
		}

		alphabetize();
		isFixed = true;

	}
	
//	/**
//	 * Constructs a vocabulary from a text file.
//	 * 
//	 * @param fileName Filename of a text file.
//	 * @throws IOException
//	 */
//	public Vocabulary(String fileName) throws IOException {
//		this();
//		SuffixArrayFactory.createVocabulary(fileName, this);
////		UNKNOWN_WORD = vocabList.size();
//	}
	
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
		Integer ID = terminalToInt.get(wordString);
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
		if (wordID==UNKNOWN_WORD || wordID >= intToString.size() || wordID < 0) {
			return UNKNOWN_WORD_STRING;
		}
		return intToString.get(wordID);
	}
	
	
	/**
	 * @return an Iterator over all words in the Vocabulary.
	 */
	public Iterator<String> iterator() {
		return intToString.values().iterator();
	}
	
	public Collection<Integer> getAllIDs() {
		return terminalToInt.values(); 
	}
	
	/**
	 * Gets the list of all words represented by this vocabulary.
	 * 
	 * @return the list of all words represented by this vocabulary
	 */
	public Collection<String> getWords() {
		return intToString.values();
	}
	
	
	/** 
	 * Adds a word to the vocabulary.
	 * 
	 * @return the ID of the word, or UNKNOWN_WORD if 
	 *         the word is new and the vocabulary is fixed.
	 */
	public int addWord(String wordString) {
		return addTerminal(wordString);
	}
		
	/**
	 * Gets the number of unique words in the vocabulary.
	 * 
	 * @return the number of unique words in the vocabulary.
	 */
	public int size() {
		return intToString.size();
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
				String thisWord = (String) this.intToString.get(i);
				String otherWord = (String) other.intToString.get(i);
				if(!(thisWord.equals(otherWord))) return false;
				Integer thisID = this.terminalToInt.get(thisWord);
				Integer otherID = other.terminalToInt.get(otherWord);
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
		return intToString.toString();
	}
	
	
	
	/** 
	 * Sorts the vocabulary alphabetically and re-assigns IDs
	 * in ascending order.
	 */
	public void alphabetize() {
		
		ArrayList<String> wordList = new ArrayList<String>(intToString.values());
		
		// alphabetize 
		Collections.sort(wordList, new Comparator<String>(){
			public int compare(String o1, String o2) {
				if (UNKNOWN_WORD_STRING.equals(o1) || null==o1) {
					if (UNKNOWN_WORD_STRING.equals(o2) || null==o2) {
						return 0;
					} else {
						return -1;
					}
				} else if (UNKNOWN_WORD_STRING.equals(o2) || null==o2) {
					return 1;
				} else {
					return o1.compareTo(o2);
				}
			}	
		});
		
		// Clear current mappings
		terminalToInt.clear();
		intToString.clear();
		
		// Reassign mappings
		for(int i = 0; i < wordList.size(); i++) {
			String wordString = wordList.get(i);
			terminalToInt.put(wordString, i);
			intToString.put(i, wordString);
		}

	}
	
	public String getUnknownWord() {
		return UNKNOWN_WORD_STRING;
	}

	public int getUnknownWordID() {
		return UNKNOWN_WORD;
	}

	public boolean isNonterminal(int id) {
		if (id < 0) 
			return true;
		else
			return false;
	}

	public int getHighestID() {
		return terminalToInt.size() - 1;
	}

	public int getLowestID() {
		return 1;
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

	public int addNonterminal(String nonterminal) {
		
		Integer id = nonterminalToInt.get(nonterminal);
		
		if (id != null) {
			return id.intValue();
		} else if (! isFixed) {
			id = -(nonterminalToInt.size());
			nonterminalToInt.put(nonterminal, id);
			intToString.put(id, nonterminal);
			return id;
		} else {
			return UNKNOWN_WORD;
		}
		
	}

	public int addTerminal(String terminal) {
		Integer id = terminalToInt.get(terminal);
		if (id != null) {
			return id.intValue();
		} else if(!isFixed) {
			id = new Integer(terminalToInt.size());
			intToString.put(id, terminal);
			terminalToInt.put(terminal, id);
			return id.intValue();
		}  else {
			return UNKNOWN_WORD;
		}
	}

	public String getTerminal(int wordId) {
		return getWord(wordId);
	}

	public String getTerminals(int[] wordIDs) {
		return getWords(wordIDs, false);
	}

	public String getWords(int[] ids) {
		return getWords(ids, false);
	}
	
	public static Vocabulary readExternal(String binaryFileName) throws FileNotFoundException, IOException, ClassNotFoundException {
		Vocabulary vocab = new Vocabulary();
		ObjectInput in = BinaryIn.vocabulary(binaryFileName); 
		vocab.readExternal(in);
		return vocab;
	}
	
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		// First read the number of bytes required to store the vocabulary data
		int totalBytes = in.readInt();
		
		// Start by reading whether or not the vocabulary is fixed
		boolean isFixed = in.readBoolean();
		
		// Next, read the actual vocabulary data
		int bytesRemaining = totalBytes - 5;
				
		while (bytesRemaining > 0) {
			
			// Read the integer id of the word
			int id = in.readInt();
			
			// Read the number of bytes used to store the word string
			int wordLength = in.readInt();
			
			// We have now read eight more bytes (4 bytes per int)
			bytesRemaining -= 8;
			
			// Read the bytes used to store the word string
			byte[] wordBytes = new byte[wordLength];
			in.readFully(wordBytes);
			String word = new String(wordBytes, CHARACTER_ENCODING);
			
			// We have now read more bytes
			bytesRemaining -= wordBytes.length;
					
			// Store the word in the vocabulary
			intToString.put(id, word);
			terminalToInt.put(word, id);
			
		}
		
		// Now mark whether this vocabulary is fixed
		this.isFixed = isFixed;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		
		// First, calculate the number of bytes required to store the vocabulary data
		int totalBytes = 0;
		for (String word : intToString.values()) {
			byte[] wordBytes = word.getBytes(CHARACTER_ENCODING);
			totalBytes += 8 + wordBytes.length;
		}
		
		// Now, write the total number of bytes used to store the vocabulary data
		totalBytes += 4 + 1; // 4 bytes for this int plus 1 byte for the following boolean
		out.writeInt(totalBytes);
		
		// Start by marking whether or not the vocabulary is fixed
		out.writeBoolean(isFixed);
		
		// Next, write the actual vocabulary data
		for (Map.Entry<Integer, String> entry : intToString.entrySet()) {
			
			int id = entry.getKey();
			String word = entry.getValue();
			
			byte[] wordBytes = word.getBytes(CHARACTER_ENCODING);
				
			// Write the integer id of the word
			out.writeInt(id);
			
			// Write the number of bytes to store the word
			out.writeInt(wordBytes.length);
			
			// Write the byte data for the string
	    	out.write(wordBytes);
	    	
		}
	}
	
	static final long serialVersionUID = 1L;
}
