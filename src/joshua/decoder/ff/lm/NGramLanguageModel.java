package joshua.decoder.ff.lm;

// BUG: At best we should use List, but we use int[] everywhere to
// represent phrases therefore these additional methods are excessive.
import java.util.List;

/**
 * An interface for new language models to implement. An object of this type is passed to
 * LanguageModelFF, which will handle all the dynamic programming and state maintinence.
 * 
 * All the function here should return LogP, not the cost.
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
   *        get the prob for the whole sentence, then startIndex should be 1
   * @return the LogP of the whole sentence
   */
  float sentenceLogProbability(int[] sentence, int order, int startIndex);

  float ngramLogProbability(int[] ngram, int order);

  float ngramLogProbability(int[] ngram);


  // ===============================================================
  // Equivalent LM State (use DefaultNGramLanguageModel if you don't care)
  // ===============================================================

  /**
   * This returns the log probability of the special backoff symbol used to fill out contexts which
   * have been backed-off. The LanguageModelFF implementation is to call this unigram probability
   * for each such token, and then call ngramLogProbability for the remaining actual N-gram.
   */

}
