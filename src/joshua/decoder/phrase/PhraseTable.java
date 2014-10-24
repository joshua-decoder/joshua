package joshua.decoder.phrase;

import java.io.IOException;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.format.HieroFormatReader;
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
  
  public PhraseTable(String owner, JoshuaConfiguration config) {
    super(owner, config);
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
  public RuleCollection getPhrases(int[] sourceWords) {
    if (sourceWords.length != 0) {
      Trie pointer = getTrieRoot().match(Vocabulary.id("[X]"));
      int i = 0;
      while (pointer != null && i < sourceWords.length)
        pointer = pointer.match(sourceWords[i++]);

      if (pointer != null && pointer.hasRules())
        return pointer.getRuleCollection();
    }

    return null;
  }
  
  @Override
  public void addOOVRules(int sourceWord, List<FeatureFunction> featureFunctions) {
    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now almost
    // certainly is)
    int targetWord = joshuaConfiguration.mark_oovs
        ? Vocabulary.id(Vocabulary.word(sourceWord) + "_OOV")
        : sourceWord;   

    String ruleString = String.format("[X] ||| [X,1] %s ||| [X,1] %s ||| -1 ||| 0-0 1-1", 
        Vocabulary.word(sourceWord), Vocabulary.word(targetWord));
    BilingualRule oovRule = new HieroFormatReader().parseLine(ruleString);
    oovRule.setOwner(Vocabulary.id("oov"));
    addRule(oovRule);
    oovRule.estimateRuleCost(featureFunctions);
  }
}
