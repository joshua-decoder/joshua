package joshua.decoder.phrase;

import java.io.IOException;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;

/**
 * Represents a phrase table. Inherits from grammars so we can code-share with the syntax-
 * based decoding work.
 * 
 * TODO: this should all be implemented as a two-level trie (source trie and target trie).
 *
 */

public class PhraseTable extends MemoryBasedBatchGrammar {
  
  /**
   * Chain to the super with a number of defaults. For example, we only use a single nonterminal,
   * and there is no span limit.
   * 
   * @param grammarFile
   * @param owner
   * @param config
   * @throws IOException
   */
  public PhraseTable(String grammarFile, String owner, JoshuaConfiguration config) throws IOException {
    super("phrase", grammarFile, owner, "[X]", -1, config);
  }
  
  /**
   * Returns the longest source phrase read, subtracting off the nonterminal that was added.
   * 
   * @return
   */
  @Override
  public int getMaxSourcePhraseLength() {
    return maxSourcePhraseLength - 1;
  }

  /**
   * Collect the set of target-side phrases associated with a source phrase.
   * 
   * @param sourceWords the sequence of source words
   * @return the rules
   */
  public RuleCollection Phrases(int[] sourceWords) {
    if (sourceWords.length >= 2) {
      Trie pointer = getTrieRoot().match(Vocabulary.id("X"));
      for (int i = 0; i < sourceWords.length; i++) {
        if ((pointer = pointer.match(sourceWords[i])) == null)
          return null;
      }
      
      if (pointer.hasRules())
        return pointer.getRuleCollection();
    }
    
    return null;
  }
}
