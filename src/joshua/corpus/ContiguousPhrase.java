package joshua.corpus;


import java.util.ArrayList;
import java.util.List;


/**
 * ContiguousPhrase implements the Phrase interface by linking into indices within a corpus. This is
 * intended to be a very low-memory implementation of the class.
 * 
 * @author Chris Callison-Burch
 * @since 29 May 2008
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class ContiguousPhrase extends AbstractPhrase {

  // ===============================================================
  // Constants
  // ===============================================================

  // ===============================================================
  // Member variables
  // ===============================================================

  protected int startIndex;
  protected int endIndex;
  protected Corpus corpusArray;

  // ===============================================================
  // Constructor(s)
  // ===============================================================

  public ContiguousPhrase(int startIndex, int endIndex, Corpus corpusArray) {
    this.startIndex = startIndex;
    this.endIndex = endIndex;
    this.corpusArray = corpusArray;
  }


  // ===============================================================
  // Public
  // ===============================================================

  // ===========================================================
  // Accessor methods (set/get)
  // ===========================================================

  /**
   * This method copies the phrase into an array of ints. This method should be avoided if possible.
   * 
   * @return an int[] corresponding to the ID of each word in the phrase
   */
  public int[] getWordIDs() {
    int[] words = new int[endIndex - startIndex];
    for (int i = startIndex; i < endIndex; i++) {
      words[i - startIndex] = corpusArray.getWordID(i); // corpusArray.corpus[i];
    }
    return words;
  }


  public int getWordID(int position) {
    return corpusArray.getWordID(startIndex + position);
    // return corpusArray.corpus[startIndex+position];
  }


  public int size() {
    return endIndex - startIndex;
  }


  // ===========================================================
  // Methods
  // ===========================================================


  /**
   * Gets all possible subphrases of this phrase, up to and including the phrase itself. For
   * example, the phrase "I like cheese ." would return the following:
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
   * 
   * @return ArrayList of all possible subphrases.
   */
  public List<Phrase> getSubPhrases() {
    return getSubPhrases(size());
  }


  /**
   * Returns a list of subphrases only of length <code>maxLength</code> or smaller.
   * 
   * @param maxLength the maximum length phrase to return.
   * @return ArrayList of all possible subphrases of length maxLength or less
   * @see #getSubPhrases()
   */
  public List<Phrase> getSubPhrases(int maxLength) {
    if (maxLength > size()) return getSubPhrases(size());
    List<Phrase> phrases = new ArrayList<Phrase>();
    for (int i = 0; i < size(); i++) {
      for (int j = i + 1; (j <= size()) && (j - i <= maxLength); j++) {
        Phrase subPhrase = subPhrase(i, j);
        phrases.add(subPhrase);
      }
    }
    return phrases;
  }


  /**
   * creates a new phrase object from the indexes provided.
   * <P>
   * NOTE: subList merely creates a "view" of the existing Phrase object. Memory taken up by other
   * Words in the Phrase is not freed since the underlying subList object still points to the
   * complete Phrase List.
   * 
   * @see ArrayList#subList(int, int)
   */
  public Phrase subPhrase(int start, int end) {
    return new ContiguousPhrase(startIndex + start, startIndex + end, corpusArray);
  }


  // ===============================================================
  // Protected
  // ===============================================================

  // ===============================================================
  // Methods
  // ===============================================================


  // ===============================================================
  // Private
  // ===============================================================

  // ===============================================================
  // Methods
  // ===============================================================


  // ===============================================================
  // Static
  // ===============================================================


  // ===============================================================
  // Main
  // ===============================================================

  /**
   * Main contains test code
   */
  public static void main(String[] args) {

  }
}
