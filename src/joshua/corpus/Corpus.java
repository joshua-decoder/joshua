package joshua.corpus;



/**
 * Corpus is an interface that contains methods for accessing the information within a monolingual
 * corpus.
 * 
 * @author Chris Callison-Burch
 * @since 7 February 2005
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */

public interface Corpus { // extends Externalizable {

  // ===============================================================
  // Attribute definitions
  // ===============================================================

  /**
   * @return the integer representation of the Word at the specified position in the corpus.
   */
  int getWordID(int position);


  /**
   * Gets the sentence index associated with the specified position in the corpus.
   * 
   * @param position Index into the corpus
   * @return the sentence index associated with the specified position in the corpus.
   */
  int getSentenceIndex(int position);


  /**
   * Gets the sentence index of each specified position.
   * 
   * @param position Index into the corpus
   * @return array of the sentence indices associated with the specified positions in the corpus.
   */
  int[] getSentenceIndices(int[] positions);

  /**
   * Gets the position in the corpus of the first word of the specified sentence. If the sentenceID
   * is outside of the bounds of the sentences, then it returns the last position in the corpus + 1.
   * 
   * @return the position in the corpus of the first word of the specified sentence. If the
   *         sentenceID is outside of the bounds of the sentences, then it returns the last position
   *         in the corpus + 1.
   */
  int getSentencePosition(int sentenceID);

  /**
   * Gets the exclusive end position of a sentence in the corpus.
   * 
   * @return the position in the corpus one past the last word of the specified sentence. If the
   *         sentenceID is outside of the bounds of the sentences, then it returns one past the last
   *         position in the corpus.
   */
  int getSentenceEndPosition(int sentenceID);

  /**
   * Gets the specified sentence as a phrase.
   * 
   * @param sentenceIndex Zero-based sentence index
   * @return the sentence, or null if the specified sentence number doesn't exist
   */
  Phrase getSentence(int sentenceIndex);


  /**
   * Gets the number of words in the corpus.
   * 
   * @return the number of words in the corpus.
   */
  int size();


  /**
   * Gets the number of sentences in the corpus.
   * 
   * @return the number of sentences in the corpus.
   */
  int getNumSentences();


  // ===========================================================
  // Methods
  // ===========================================================


  /**
   * Compares the phrase that starts at position start with the subphrase indicated by the start and
   * end points of the phrase.
   * 
   * @param corpusStart the point in the corpus where the comparison begins
   * @param phrase the superphrase that the comparsion phrase is drawn from
   * @param phraseStart the point in the phrase where the comparison begins (inclusive)
   * @param phraseEnd the point in the phrase where the comparison ends (exclusive)
   * @return an int that follows the conventions of java.util.Comparator.compareTo()
   */
  int comparePhrase(int corpusStart, Phrase phrase, int phraseStart, int phraseEnd);


  /**
   * Compares the phrase that starts at position start with the phrase passed in. Compares the
   * entire phrase.
   * 
   * @param corpusStart
   * @param phrase
   * @return
   */
  int comparePhrase(int corpusStart, Phrase phrase);

  /**
   * Compares the suffixes starting a positions index1 and index2.
   * 
   * @param position1 the position in the corpus where the first suffix begins
   * @param position2 the position in the corpus where the second suffix begins
   * @param maxComparisonLength a cutoff point to stop the comparison
   * @return an int that follows the conventions of java.util.Comparator.compareTo()
   */
  int compareSuffixes(int position1, int position2, int maxComparisonLength);

  /**
   * 
   * @param startPosition
   * @param endPosition
   * @return
   */
  ContiguousPhrase getPhrase(int startPosition, int endPosition);

  /**
   * Gets an object capable of iterating over all positions in the corpus, in order.
   * 
   * @return An object capable of iterating over all positions in the corpus, in order.
   */
  Iterable<Integer> corpusPositions();

  // void write(String corpusFilename, String vocabFilename, String charset) throws IOException;
}
