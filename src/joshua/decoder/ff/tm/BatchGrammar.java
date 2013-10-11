package joshua.decoder.ff.tm;

import joshua.decoder.segment_file.Sentence;
import joshua.decoder.JoshuaConfiguration;

/**
 * This class provides an abstract factory with provisions for sentence-level filtering. If
 * sentence-level filtering is enabled (via the "filter-grammar" parameter), a new grammar is
 * constructed that has been pruned of all rules that are not applicable to the current sentence.
 * This is implemented by constructing a new grammar trie from which we have pruned all nodes that
 * aren't reachable by the current sentence.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public abstract class BatchGrammar extends AbstractGrammar implements GrammarFactory {
  public final JoshuaConfiguration joshuaConfiguration;

  protected BatchGrammar(JoshuaConfiguration joshuaConfiguration) {
    super();
    this.joshuaConfiguration = joshuaConfiguration;
  }

  /**
   * Returns a grammar that has been adapted to the current sentence, subject to the
   * "filter-grammar" runtime parameter.
   * 
   * @param sentence the sentence to be translated
   * @return a grammar that represents a set of translation rules
   */
  @Override
  public Grammar getGrammarForSentence(Sentence sentence) {
    if (this.joshuaConfiguration.filter_grammar)
      return new SentenceFilteredGrammar(this, sentence);
    else
      return this;
  }

  /**
   * Returns the grammar itself.
   */
  @Override
  public Grammar getGrammar() {
    return this;
  }
}
