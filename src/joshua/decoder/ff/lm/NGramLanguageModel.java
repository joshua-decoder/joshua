package joshua.decoder.ff.lm;

/**
 * An interface for new language models to implement. An object of this type is passed to
 * LanguageModelFF, which will handle all the dynamic programming and state maintenance.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public interface NGramLanguageModel {

  // ===============================================================
  // Attributes
  // ===============================================================
  int getOrder();

  // ===============================================================
  // Methods
  // ===============================================================

  /**
   * Language models may have their own private vocabulary mapping strings to integers; for example,
   * if they make use of a compile format (as KenLM and BerkeleyLM do). This mapping is likely
   * different from the global mapping containing in joshua.corpus.Vocabulary, which is used to
   * convert the input string and grammars. This function is used to tell the language model what
   * the global mapping is, so that the language model can convert it into its own private mapping.
   * 
   * @param word
   * @param id
   * @return Whether any collisions were detected.
   */
  boolean registerWord(String token, int id);

  /**
   * @param sentence the sentence to be scored
   * @param order the order of N-grams for the LM
   * @param startIndex the index of first event-word we want to get its probability; if we want to
   *          get the prob for the whole sentence, then startIndex should be 1
   * @return the LogP of the whole sentence
   */
  float sentenceLogProbability(int[] sentence, int order, int startIndex);

  /**
   * Compute the probability of a single word given its context.
   * 
   * @param ngram
   * @param order
   * @return
   */
  float ngramLogProbability(int[] ngram, int order);

  float ngramLogProbability(int[] ngram);
}
