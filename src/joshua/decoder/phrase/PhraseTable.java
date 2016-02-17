package joshua.decoder.phrase;

import java.io.File;
import java.io.IOException;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.packed.PackedGrammar;

/**
 * Represents a phrase table, and is implemented as a wrapper around either a {@link PackedGrammar}
 * or a {@link MemoryBasedBatchGrammar}.
 * 
 * TODO: this should all be implemented as a two-level trie (source trie and target trie).
 */
public class PhraseTable implements Grammar {
  
  private JoshuaConfiguration config;
  private Grammar backend;
  
  /**
   * Chain to the super with a number of defaults. For example, we only use a single nonterminal,
   * and there is no span limit.
   * 
   * @param grammarFile
   * @param owner
   * @param config
   * @throws IOException
   */
  public PhraseTable(String grammarFile, String owner, String type, JoshuaConfiguration config, int maxSource) 
      throws IOException {
    this.config = config;
    int spanLimit = 0;
    
    if (grammarFile != null && new File(grammarFile).isDirectory()) {
      this.backend = new PackedGrammar(grammarFile, spanLimit, owner, type, config);
      if (this.backend.getMaxSourcePhraseLength() == -1) {
        System.err.println("FATAL: Using a packed grammar for a phrase table backend requires that you");
        System.err.println("       packed the grammar with Joshua 6.0.2 or greater");
        System.exit(-1);
      }

    } else {
      this.backend = new MemoryBasedBatchGrammar(type, grammarFile, owner, "[X]", spanLimit, config);
    }
  }
  
  public PhraseTable(String owner, JoshuaConfiguration config) {
    this.config = config;
    
    this.backend = new MemoryBasedBatchGrammar(owner, config);
  }
      
  /**
   * Returns the longest source phrase read. For {@link MemoryBasedBatchGrammar}s, we subtract 1
   * since the grammar includes the nonterminal. For {@link PackedGrammar}s, the value was either
   * in the packed config file (Joshua 6.0.2+) or was passed in via the TM config line.
   * 
   * @return
   */
  @Override
  public int getMaxSourcePhraseLength() {
    if (backend instanceof MemoryBasedBatchGrammar)
      return this.backend.getMaxSourcePhraseLength() - 1;
    else
      return this.backend.getMaxSourcePhraseLength();
  }

  /**
   * Collect the set of target-side phrases associated with a source phrase.
   * 
   * @param sourceWords the sequence of source words
   * @return the rules
   */
  public RuleCollection getPhrases(int[] sourceWords) {
    if (sourceWords.length != 0) {
      Trie pointer = getTrieRoot();
      if (! (backend instanceof PackedGrammar))
        pointer = pointer.match(Vocabulary.id("[X]"));
      int i = 0;
      while (pointer != null && i < sourceWords.length)
        pointer = pointer.match(sourceWords[i++]);

      if (pointer != null && pointer.hasRules()) {
        return pointer.getRuleCollection();
      }
    }

    return null;
  }

  /**
   * Adds a rule to the grammar. Only supported when the backend is a MemoryBasedBatchGrammar.
   * 
   * @param rule the rule to add
   */
  public void addRule(Rule rule) {
    ((MemoryBasedBatchGrammar)backend).addRule(rule);
  }
  
  @Override
  public void addOOVRules(int sourceWord, List<FeatureFunction> featureFunctions) {
    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now almost
    // certainly is)
    int targetWord = config.mark_oovs
        ? Vocabulary.id(Vocabulary.word(sourceWord) + "_OOV")
        : sourceWord;   

    int nt_i = Vocabulary.id("[X]");
    Rule oovRule = new Rule(nt_i, new int[] { nt_i, sourceWord },
        new int[] { -1, targetWord }, "", 1, null);
    addRule(oovRule);
    oovRule.estimateRuleCost(featureFunctions);
        
//    String ruleString = String.format("[X] ||| [X,1] %s ||| [X,1] %s", 
//        Vocabulary.word(sourceWord), Vocabulary.word(targetWord));
//    BilingualRule oovRule = new HieroFormatReader().parseLine(ruleString);
//    oovRule.setOwner(Vocabulary.id("oov"));
//    addRule(oovRule);
//    oovRule.estimateRuleCost(featureFunctions);
  }

  @Override
  public Trie getTrieRoot() {
    return backend.getTrieRoot();
  }

  @Override
  public void sortGrammar(List<FeatureFunction> models) {
    backend.sortGrammar(models);    
  }

  @Override
  public boolean isSorted() {
    return backend.isSorted();
  }

  /**
   * This should never be called. 
   */
  @Override
  public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
    return true;
  }

  @Override
  public int getNumRules() {
    return backend.getNumRules();
  }

  @Override
  public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores,
      int arity) {
    return backend.constructManualRule(lhs,  sourceWords, targetWords, scores, arity);
  }

  @Override
  public void writeGrammarOnDisk(String file) {
    backend.writeGrammarOnDisk(file);
  }

  @Override
  public boolean isRegexpGrammar() {
    return backend.isRegexpGrammar();
  }

  @Override
  public int getOwner() {
    return backend.getOwner();
  }

  @Override
  public int getNumDenseFeatures() {
    return backend.getNumDenseFeatures();
  }
}
