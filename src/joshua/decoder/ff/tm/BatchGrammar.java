package joshua.decoder.ff.tm;

import joshua.decoder.segment_file.Sentence;

/**
 * This class provides an abstract factory that will return itself as a batch grammar.
 * <p>
 * This means that the grammar produced by this class will be constant over any test set, and will
 * not be specific to any provided sentence.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public abstract class BatchGrammar extends AbstractGrammar implements GrammarFactory {

  /**
   * Returns a grammar which is <em>not</em> adapted to the specified sentence.
   * <p>
   * This method always ignores the provided parameter.
   * 
   * The grammar returned will always be the same, regardless of the value of the sentence
   * parameter.
   * 
   * @param sentence the next sentence to be translated
   * @return a grammar that represents a set of translation rules
   */
  public Grammar getGrammarForSentence(Sentence sentence) {
    return this;
  }

}
