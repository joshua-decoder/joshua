package joshua.util.sentence;

import java.util.ArrayList;
import java.util.List;

import joshua.util.sentence.Vocabulary;




/**
 * 
 * @version $LastChangedDate:2008-09-18 10:31:54 -0500 (Thu, 18 Sep 2008) $
 */
public interface Phrase extends Comparable<Phrase> {

	/**
	 * Returns the vocabulary that the words in this phrase are drawn from.
	 * 
	 * @return the vocabulary that the words in this phrase are drawn from.
	 */
	public Vocabulary getVocab();


	/**
	 * Returns the integer word id of the word at the specified position.
	 * 
	 * @param position Index of a word in this phrase.
	 * @return the integer word id of the word at the specified position.
	 */
	public int getWordID(int position);

	/**
	 * Returns the number of words in this phrase.
	 * 
	 * @return the number of words in this phrase.
	 */
	public int size();



	/**
	 * Gets all possible subphrases of this phrase, up to and including
	 * the phrase itself. For example, the phrase "I like cheese ." would return
	 * the following:
	 * <ul>
	 * <li>I
	 * <li>like
	 * <li>cheese
	 * <li>.
	 * <li>I like
	 * <li>like cheese
	 * <li>cheese .
	 * <li>I like cheese
	 * <li>like cheese .
	 * <li>I like cheese .
	 * </ul>
	 * @return List of all possible subphrases.
	 */
	public List<Phrase> getSubPhrases();

	/**
	 * Returns a list of subphrases only of length <code>maxLength</code>
	 * or smaller. 
	 * @param maxLength the maximum length phrase to return.
	 * @return List of all possible subphrases of length maxLength or less
	 * @see #getSubPhrases()
	 */
	public List<Phrase> getSubPhrases(int maxLength);


	/**
	 * creates a new phrase object from the indexes provided.
	 * <P>
	 * NOTE: subList merely creates a "view" of the existing Phrase
	 * object. Memory taken up by other Words in the Phrase is not 
	 * freed since the underlying subList object still points to the 
	 * complete Phrase List.
	 *
	 * @see ArrayList#subList(int, int)
	 */
	public Phrase subPhrase(int start, int end);

	/**
	 * Compares the two strings based on the lexicographic order of words
	 * defined in the Vocabulary.  
	 *
	 * @param obj the object to compare to
	 * @return -1 if this object is less than the parameter, 0 if equals, 1 if greater
	 */
	public int compareTo(Phrase other);


	
}
