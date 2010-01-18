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
package joshua.corpus.vocab;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.suffix_array.BasicPhrase;
import joshua.decoder.ff.tm.hiero.HieroFormatReader;
import joshua.util.io.BinaryIn;
import joshua.util.io.LineReader;


/**
 * Vocabulary is the class that keeps track of the unique words
 * that occur in a corpus of text for a particular language. It
 * assigns integer IDs to Words, which is useful when we are creating
 * suffix arrays or doing similar things.
 *
 * @author Chris Callison-Burch
 * @since  8 February 2005
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */
public class Vocabulary extends AbstractExternalizableSymbolTable 
		implements Iterable<String>, ExternalizableSymbolTable {

//===============================================================
// Constants
//===============================================================

	private static final Logger logger = Logger.getLogger(Vocabulary.class.getName());

//===============================================================
// Member variables
//===============================================================

	protected final Map<String,Integer> nonterminalToInt;
	protected final Map<String,Integer> terminalToInt;
	protected final Map<Integer,String> intToString;
	
//	/**
//	 * Determines whether new words may be added to the vocabulary.
//	 */
//	protected boolean isFixed;
	
	/**
	 * The value returned by this class's <code>hashCode</code>
	 * method.
	 */
	protected static final int HASH_CODE = 42;
	
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
//		isFixed = false;
//		terminalToInt.put(UNKNOWN_WORD_STRING, UNKNOWN_WORD);
//		intToString.put(UNKNOWN_WORD, UNKNOWN_WORD_STRING);
		addNonterminal(X_STRING);
		addNonterminal(X1_STRING);
		addNonterminal(X2_STRING);
		addNonterminal(S_STRING);
		addNonterminal(S1_STRING);
		this.addTerminal("<unk>");
		this.addTerminal("<s>");
		this.addTerminal("</s>");
		this.addTerminal("-pau-");
	}

	/** 
	 * Constructor creates a vocabulary initialized with the given
	 * set of words.
	 */
	public Vocabulary(Set<String> words) {
		this();

		for (String word : words) {
			this.addTerminal(word);
		}

//		alphabetize();
//		isFixed = true;

	}
	
	/**
	 * Constructs a vocabulary using the words from an SRILM
	 * language model file.
	 *
	 * @param scanner Scanner configured to read an SRILM
	 *                language model file.
	 * @return Vocabulary initialized with the words from the
	 *         SRILM language model file.
	 */
	public static Vocabulary getVocabFromSRILM(Scanner scanner) {
		
		Vocabulary vocab = new Vocabulary();
		
		int counter = 0;
		int ignored = 0;
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			
			String[] parts = line.split("\\s+");
			
			if (parts.length==2) {
				
				Integer id = Integer.valueOf(parts[0]);
				String word = parts[1];
				
				vocab.intToString.put(id, word);
				
				if (vocab.isNonterminal(id)) {
					vocab.nonterminalToInt.put(word, id);
				} else {
					vocab.terminalToInt.put(word, id);
				}
				
				counter += 1;
				
			} else {
				
				ignored += 1;
				logger.warning("Line is improperly formatted: " + line);
				
			}
			
			
		}
		
		if (logger.isLoggable(Level.FINE)) {
			int total = counter + ignored;
			logger.fine(total + " lines read, of which " + counter + " were included and " + ignored + " were ignored");
		}
		
		return vocab;
	}
	
	
	/**
	 * Initializes a Vocabulary by adding all words from a
	 * specified plain text file.
	 *
	 * @param inputFilename the plain text file
	 * @param vocab         the Vocabulary to which words should
	 *                      be added
	 * @param fixVocabulary Should the vocabulary be fixed and
	 *                      alphabetized at the end of
	 *                      initialization
	 * @return a tuple containing the number of words in the
	 *         corpus and number of sentences in the corpus
	 */
	public static int[] initializeVocabulary(String inputFilename, Vocabulary vocab, boolean fixVocabulary) throws IOException {
		int numSentences = 0;
		int numWords = 0;
		
		LineReader lineReader = new LineReader(inputFilename);
		
		for (String line : lineReader) {
			BasicPhrase sentence = new BasicPhrase(line, vocab);
			numWords += sentence.size();
			numSentences++;
			if(logger.isLoggable(Level.FINE) && numSentences % 10000==0) logger.fine(""+numWords);
		}
		
//		if (fixVocabulary) {
//			vocab.fixVocabulary();
//			vocab.alphabetize();
//		}
		
		int[] numberOfWordsSentences = { numWords, numSentences };
		return numberOfWordsSentences;
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
	 * If the word is in the vocabulary, the integer returned
	 * will uniquely identify that word.
	 * 
	 * If the word is not in the vocabulary, the constant
	 * <code>UNKNOWN_WORD</code> will be returned.
	 * 
	 * @return the unique integer identifier for wordString, 
	 *         or UNKNOWN_WORD if wordString is not in the
	 *         vocabulary
	 */
	public int getID(String wordString) {
//		String s = HieroFormatReader.getFieldDelimiter();
		if (SymbolTable.X_STRING.equals(wordString) ||
				SymbolTable.X1_STRING.equals(wordString) ||
				SymbolTable.X2_STRING.equals(wordString) ||
				SymbolTable.S_STRING.equals(wordString) ||
				SymbolTable.S1_STRING.equals(wordString) ||
				HieroFormatReader.isNonTerminal(wordString)) {
			return this.addNonterminal(wordString);
		} else {
			return this.addTerminal(wordString);
		}
//		return add
//		if (terminalToInt.containsKey(wordString)) {
//			return terminalToInt.get(wordString);
//		} else if (nonterminalToInt.containsKey(wordString)) {
//			return nonterminalToInt.get(wordString);
//		} else {
//			return UNKNOWN_WORD;
//		}
	}

	public int getNonterminalID(String nonterminalString) {
		return this.addNonterminal(nonterminalString);
//		return nonterminalToInt.get(nonterminalString);
	}
	
	/**
	 * Gets the integer identifiers for all words in the provided
	 * sentence.
	 * <p>
	 * The sentence will be split (on spaces) into words, then
	 * the integer identifier for each word will be retrieved
	 * using <code>getID</code>.
	 * 
	 * @see #getID(String)
	 * @param sentence String of words, separated by spaces.
	 * @return Array of integer identifiers for each word in
	 *         the sentence
	 */
	public int[] getIDs(String sentence) {
		if (sentence==null || sentence.trim().length()==0) {
			return new int[]{};
		} else {
			String[] words = sentence.trim().split(" ");
			int[] wordIDs = new int[words.length];

			for (int i=0; i<words.length; i++) {
				wordIDs[i] = getID(words[i]);
			}

			return wordIDs;
		}
	}
	
	
	
	/**
	 * Gets the String that corresponds to the specified integer
	 * identifier.
	 *
	 * @return the String that corresponds to the specified
	 *         integer identifier, or <code>UNKNOWN_WORD_STRING</code>
	 *         if the identifier does not correspond to a word
	 *         in the vocabulary
	 */
	public String getWord(int wordID) {
//		if (wordID==UNKNOWN_WORD || wordID >= terminalToInt.size() || wordID < -(nonterminalToInt.size())) {
////		if (wordID==UNKNOWN_WORD || wordID >= terminalToInt.size() || wordID < -(nonterminalToInt.size())) {
//			return UNKNOWN_WORD_STRING;
//		}
		String word = intToString.get(wordID);
		if (word==null) {
			word = UNKNOWN_WORD_STRING;
		}
		return word;
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
	 * @return the list of all words represented by this
	 *         vocabulary
	 */
	public Collection<String> getWords() {
		return intToString.values();
	}
	
	/**
	 * Gets the number of unique words in the vocabulary.
	 *
	 * @return the number of unique words in the vocabulary.
	 */
	public int size() {
		return intToString.size();
	}
	
//	/** 
//	 * Fixes the size of the vocabulary so that new words may
//	 * not be added.
//	 */
//	public void fixVocabulary() {
//		isFixed = true;
//	}
	
	/**
	 * Determines if the phrase contains any words that are not
	 * in the vocabulary.
	 * 
	 * @return <code>true</code> if there are unknown words in
	 *         the phrase, <code>false</code> otherwise
	 */
	public boolean containsUnknownWords(BasicPhrase phrase) {
		for(int i = 0; i < phrase.size(); i++) {
			if(phrase.getWordID(i) == UNKNOWN_WORD) return true;
		}
		return false;
	}
	
	
	/**
	 * Checks that the Vocabularies are the same, by first
	 * checking that they have the same number of terminals,
	 * and then checking that each word corresponds to the same
	 * ID.
	 * <p>
	 * XXX This method does NOT check to verify that the
	 * nonterminal vocabulary is the same.
	 *
	 * @param o the object to check equivalence with
	 * @return <code>true</code> if the other object is a
	 *         Vocabulary representing the same set of words
	 *         with identically assigned IDs, <code>false</code>
	 *         otherwise
	 */
	public boolean equals(Object o) {
		if (o==this) {
			return true;
		} else if (o instanceof SymbolTable) {
			SymbolTable other = (SymbolTable) o;
			if(other.size() != this.size()) return false;
			for (int i=-(nonterminalToInt.size()), n=terminalToInt.size(); i<n; i++) {
				String thisWord = this.getWord(i);//.intToString.get(i);
				String otherWord = other.getWord(i);
				if (thisWord==null && otherWord!=null) return false;
				if(!(thisWord.equals(otherWord))) return false;
				Integer thisID = (this.isNonterminal(i)) ? this.nonterminalToInt.get(thisWord) : this.terminalToInt.get(thisWord);
				Integer otherID;
				if (o instanceof Vocabulary) {
					Vocabulary otherVocab = (Vocabulary) other;
					otherID = (otherVocab.isNonterminal(i)) ? otherVocab.nonterminalToInt.get(thisWord) : otherVocab.terminalToInt.get(thisWord);
				} else {
					otherID = other.getID(otherWord);
				}
				if(thisID != null && otherID != null) {
					if(!(thisID.equals(otherID))) return false;
				}  
			}

			return true;

		} else {
			return false;
		}
	}
	
	/**
	 * It is expected that instances of this class will never
	 * be put into a hash table.
	 * <p>
	 * Therefore, this method always returns a constant value.
	 *
	 * @return a constant value
	 */
	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return HASH_CODE; 
	}
	
	//===========================================================
	// Methods
	//===========================================================
	
	
	public String toString() {
		return intToString.toString();
	}
	
	
	
//	/** 
//	 * Sorts the vocabulary alphabetically and re-assigns IDs
//	 * in ascending order.
//	 */
//	public void alphabetize() {
//		
//		ArrayList<String> wordList = new ArrayList<String>(terminalToInt.keySet());//intToString.values());
//		
//		// alphabetize 
//		Collections.sort(wordList, new Comparator<String>(){
//			public int compare(String o1, String o2) {
//				if (UNKNOWN_WORD_STRING.equals(o1) || null==o1) {
//					if (UNKNOWN_WORD_STRING.equals(o2) || null==o2) {
//						return 0;
//					} else {
//						return -1;
//					}
//				} else if (UNKNOWN_WORD_STRING.equals(o2) || null==o2) {
//					return 1;
//				} else {
//					return o1.compareTo(o2);
//				}
//			}	
//		});
//		
//		// Clear current mappings
//		terminalToInt.clear();
//		intToString.clear();
//		
//		// Reassign nonterminal mappings
//		for (Map.Entry<String, Integer> ntEntry : nonterminalToInt.entrySet()) {
//			intToString.put(ntEntry.getValue(), ntEntry.getKey());
//		}
//		
//		// Reassign terminal mappings
//		for(int i = 0; i < wordList.size(); i++) {
//			String wordString = wordList.get(i);
//			terminalToInt.put(wordString, i);
//			intToString.put(i, wordString);
//		}
//
//	}

	public int getHighestID() {
		return terminalToInt.size();
//		return terminalToInt.size() - 1;
	}

	public int getLowestID() {
		return -(nonterminalToInt.size());
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

	public int addNonterminal(String nonterminal) {
		
		Integer id = nonterminalToInt.get(nonterminal);
		
		if (id != null) {
			return id.intValue();
		} else {
			int size = nonterminalToInt.size();
			id = -(size+1);
			nonterminalToInt.put(nonterminal, id);
			intToString.put(id, nonterminal);
			return id;
		} 
	}

	public int addTerminal(String terminal) {
		Integer id = terminalToInt.get(terminal);
		if (id != null) {
			return id.intValue();
		} else {
			id = Integer.valueOf(terminalToInt.size()+1);
			intToString.put(id, terminal);
			terminalToInt.put(terminal, id);
			return id.intValue();
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
		
		String characterEncoding = getExternalizableEncoding();
		
		// First read the number of bytes required to store the vocabulary data
		int totalBytes = in.readInt();
		if (logger.isLoggable(Level.FINEST)) logger.finest("Read total bytes: " + totalBytes);
		
		// Next, read the actual vocabulary data
		int bytesRemaining = totalBytes - 4;
				
		while (bytesRemaining > 0) {
			
			// Read the integer id of the word
			int id = in.readInt();
			if (logger.isLoggable(Level.FINEST)) logger.finest("Read ID: " + id);
			
			// Read the number of bytes used to store the word string
			int wordLength = in.readInt();
			if (logger.isLoggable(Level.FINEST)) logger.finest("Read string length: " + wordLength);
			
			// We have now read eight more bytes (4 bytes per int)
			bytesRemaining -= 8;
			
			// Read the bytes used to store the word string
			byte[] wordBytes = new byte[wordLength];
			in.readFully(wordBytes);
			String word = new String(wordBytes, characterEncoding);
			if (logger.isLoggable(Level.FINEST)) logger.finest("Read string bytes: " + Arrays.toString(wordBytes) + " for \"" + word + "\"");
			
			// We have now read more bytes
			bytesRemaining -= wordBytes.length;
					
			// Store the word in the vocabulary
			intToString.put(id, word);
			if (logger.isLoggable(Level.FINEST)) logger.finest("Mapped int " + id + " to word \"" + word + "\"");
			
			if (isNonterminal(id)) {
				nonterminalToInt.put(word, id);
				if (logger.isLoggable(Level.FINEST)) logger.finest("Mapped word \"" + word + "\" to int " + id);
			} else {
				terminalToInt.put(word, id);
				if (logger.isLoggable(Level.FINEST)) logger.finest("Mapped word \"" + word + "\" to int " + id);
			}
		}
		
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		
		String characterEncoding = getExternalizableEncoding();
		
		// First, calculate the number of bytes required to store the vocabulary data
		int totalBytes = 0;
		for (String word : intToString.values()) {
			byte[] wordBytes = word.getBytes(characterEncoding);
			totalBytes += 8 + wordBytes.length;
		}
		
		// Now, write the total number of bytes used to store the vocabulary data
		totalBytes += 4; // 4 bytes for this int
		if (logger.isLoggable(Level.FINEST)) logger.finest("Writing total bytes: " + totalBytes);
		out.writeInt(totalBytes);
				
		// Next, write the actual vocabulary data
		for (Map.Entry<Integer, String> entry : intToString.entrySet()) {
			
			int id = entry.getKey();
			String word = entry.getValue();
			
			byte[] wordBytes = word.getBytes(characterEncoding);
				
			// Write the integer id of the word
			if (logger.isLoggable(Level.FINEST)) logger.finest("Writing ID: " + id);
			out.writeInt(id);
			
			// Write the number of bytes to store the word
			if (logger.isLoggable(Level.FINEST)) logger.finest("Writing string length: " + wordBytes.length);
			out.writeInt(wordBytes.length);
			
			// Write the byte data for the string
			if (logger.isLoggable(Level.FINEST)) logger.finest("Writing string bytes: " + Arrays.toString(wordBytes) + " for \"" + word + "\"");
	    	out.write(wordBytes);
	    	
		}
	}
	
	static final long serialVersionUID = 1L;
}
