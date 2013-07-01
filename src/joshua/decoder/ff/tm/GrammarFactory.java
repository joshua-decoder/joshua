package joshua.decoder.ff.tm;

import joshua.decoder.segment_file.Sentence;

/**
 * Factory capable of getting a grammar for use in translating a sentence.
 * <p>
 * Developers interested in implementing a new type of grammar must:
 * <ol>
 * <li>Implement <code>GrammarFactory</code>
 * <li>Implement <code>Grammar</code>
 * <li>Implement <code>TrieGrammar</code>
 * <li>Implement <code>RuleCollection</code>
 * </ol>
 * 
 * Also, attention should be directed to the <code>Rule</code> class.
 * 
 * @author Lane Schwartz
 */
public interface GrammarFactory {

  /**
   * Returns a grammar which is adapted to the specified sentence. Depending on the implementation
   * this grammar may be generated online, partially loaded from disk, remain unchanged etc.
   * 
   * @param sentence A sentence to be translated
   * 
   * @return A grammar that represents a set of translation rules, relevant for translating (at
   *         least) the given sentence.
   */
  Grammar getGrammarForSentence(Sentence sentence);

  /**
   * Returns the entire grammar represented by this sentence.
   * 
   * @return the whole grammar
   */
  Grammar getGrammar();
}
