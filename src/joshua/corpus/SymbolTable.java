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

import java.util.Collection;
import java.util.List;

/**
 * Represents a symbol table capable
 * of mapping between strings and symbols.
 * 
 * @author Lane Schwartz
 * @author Zhifei Li
 * @version $LastChangedDate$
 */
public interface SymbolTable {

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
	final int UNKNOWN_WORD = 0;

	/** String representation for out-of-vocabulary words. */
	final String UNKNOWN_WORD_STRING = "UNK";
	
	public int addNonterminal(String nonterminal);
	public int addTerminal(String terminal);
	public int[] addTerminals(String[] words);
	public int[] addTerminals(String sentence);
	
	/**
	 * Gets an integer identifier for the word.
	 * <p>
	 * If the word is in the vocabulary,
	 * the integer returned will uniquely identify that word.
	 * <p>
	 * If the word is not in the vocabulary,
	 * the integer returned by <code>getUnknownWordID</code> may be returned.
	 * 
	 * Alternatively, implementations may, if they choose, 
	 * add unknown words and assign them a symbol ID
	 * instead of returning <code>getUnknownWordID</code>.
	 * 
	 * @see getUnknownWordID
	 * @return the unique integer identifier for wordString, 
	 *         or the result of <code>getUnknownWordID<code> 
	 *         if wordString is not in the vocabulary
	 */
	public int getID(String wordString);
	
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
	public int[] getIDs(String sentence);
	
	/**
	 * Gets the String that corresponds to the specified integer identifier.
	 * <p>
	 * If the identifier is in the symbol vocabulary,
	 * the String returned will correspond to that identifier.
	 * 
	 * Otherwise, the String returned by <code>getUnknownWord<code> will be returned.
	 * 
	 * @return the String that corresponds to the specified integer identifier,
	 *         or the result of <code>getUnknownWord</code> if the identifier 
	 *         does not correspond to a word in the vocabulary
	 */
	public String getTerminal(int wordID);
	
	/**
	 * Gets the String that corresponds to the specified integer identifier.
	 * <p>
	 * This method can be called for terminals or nonterminals.
	 * 
	 * @param tokenID
	 * @return
	 */
	public String getWord(int tokenID);
	
	public String getWords(int[] ids);
	
	public String getTerminals(int[] wordIDs);
	
	/**
	 * Gets a collection over all symbol identifiers for the vocabulary.
	 * @return a collection over all symbol identifiers for the vocabulary
	 */
	public Collection<Integer> getAllIDs();
	
	/**
	 * Gets the list of all words represented by this vocabulary.
	 * 
	 * @return the list of all words represented by this vocabulary
	 */
	public List<String> getWords();
	
	/**
	 * Gets the number of unique words in the vocabulary.
	 * 
	 * @return the number of unique words in the vocabulary.
	 */
	public int size();
	
	/**
	 * Gets the integer symbol representation of the unknown word.
	 * @return the integer symbol representation of the unknown word.
	 */
	public int getUnknownWordID();
	
	/**
	 * Gets the string representation of the unknown word.
	 * @return the string representation of the unknown word.
	 */
	public String getUnknownWord();
	
	/**
	 * Returns <code>true</code> if the symbol id 
	 * represents a nonterminal, <code>false</code> otherwise.
	 * 
	 * @param id
	 * @return <code>true</code> if the symbol id 
	 * represents a nonterminal, <code>false</code> otherwise.
	 */
	public boolean isNonterminal(int id);
	
	/**
	 * Gets the lowest-valued allowable terminal symbol id in this table.
	 * 
	 * @return the lowest-valued allowable terminal symbol id in this table.
	 */
	public int getLowestID();

	
	/**
	 * Gets the highest-valued allowable terminal symbol id in this table.
	 * <p>
	 * NOTE: This may or may not return the same value as <code>size</code>.
	 * 
	 * @return the highest-valued allowable terminal symbol id in this table.
	 */	
	public int getHighestID();
	
	public int getTargetNonterminalIndex(int id);//first convert id to its String maping, then call the function below
	
	public int getTargetNonterminalIndex(String word);
	
	public String getWords(int[] wordIDs, boolean ntIndexIncrements);
	
}

