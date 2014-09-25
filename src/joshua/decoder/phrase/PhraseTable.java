package joshua.decoder.phrase;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;
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
  
  public PhraseTable(GrammarReader<BilingualRule> gr, JoshuaConfiguration joshuaConfiguration) {
    super(gr, joshuaConfiguration);
  }
  
  /**
   * Returns the longest source phrase read.
   * 
   * @return
   */
  public int getMaxSourcePhraseLength() {
    /* We added a nonterminal to all source sides, so subtract that off. */
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
