package joshua.decoder.phrase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.util.io.LineReader;

/**
 * Represents a phrase table. Inherits from grammars so we can code-share with the syntax-
 * based decoding work.
 * 
 * TODO: this should all be implemented as a two-level trie (source trie and target trie).
 *
 */

public class PhraseTable implements Grammar {
  
  private String grammarFile;
  private int owner;
  private JoshuaConfiguration config;  
  private HashMap<PhraseWrapper, RuleCollection> entries;
  private int numRules;
  private List<FeatureFunction> features;
  private int maxSourceLength;

  /**
   * Chain to the super with a number of defaults. For example, we only use a single nonterminal,
   * and there is no span limit.
   * 
   * @param grammarFile
   * @param owner
   * @param config
   * @throws IOException
   */
  public PhraseTable(String grammarFile, String owner, JoshuaConfiguration config, List<FeatureFunction> features) throws IOException {
    this.config = config;
    this.owner = Vocabulary.id(owner);
    this.grammarFile = grammarFile;
    this.features = features;
    this.maxSourceLength = 0;
    Vocabulary.id("[X]");
    
    this.entries = new HashMap<PhraseWrapper, RuleCollection>();

    loadPhraseTable();
  }
  
  public PhraseTable(String owner, JoshuaConfiguration config, List<FeatureFunction> features) {
    this.config = config;
    this.owner = Vocabulary.id(owner);
    this.features = features;
    this.maxSourceLength = 0;
    
    this.entries = new HashMap<PhraseWrapper, RuleCollection>();
  }
  
  private void loadPhraseTable() throws IOException {
    
    String prevSourceSide = null;
    List<String> rules = new ArrayList<String>(); 
    int[] french = null;
    
    for (String line: new LineReader(this.grammarFile)) {
      int sourceEnd = line.indexOf(" ||| ");
      String source = line.substring(0, sourceEnd);
      String rest = line.substring(sourceEnd + 5);

      rules.add(rest);
      
      if (prevSourceSide == null || ! source.equals(prevSourceSide)) {

        // New source side, store accumulated rules
        if (prevSourceSide != null) {
          System.err.println(String.format("loadPhraseTable: %s -> %d rules", Vocabulary.getWords(french), rules.size()));
          entries.put(new PhraseWrapper(french), new LazyRuleCollection(owner, 1, french, rules));
          rules = new ArrayList<String>();
        }
        
        String[] foreignWords = source.split("\\s+");
        french = new int[foreignWords.length];
        for (int i = 0; i < foreignWords.length; i++)
          french[i] = Vocabulary.id(foreignWords[i]);

        maxSourceLength = Math.max(french.length, getMaxSourcePhraseLength());
        
        prevSourceSide = source;
      }
    }
    
    if (french != null) {
      entries.put(new PhraseWrapper(french), new LazyRuleCollection(owner, 1, french, rules));
      System.err.println(String.format("loadPhraseTable: %s -> %d rules", Vocabulary.getWords(french), rules.size()));
    }
  }

  /**
   * Returns the longest source phrase read, subtracting off the nonterminal that was added.
   * 
   * @return
   */
  public int getMaxSourcePhraseLength() {
    return maxSourceLength;
  }

  /**
   * Collect the set of target-side phrases associated with a source phrase.
   * 
   * @param sourceWords the sequence of source words
   * @return the rules
   */
  public List<Rule> getPhrases(int[] sourceWords) {
    RuleCollection rules = entries.get(new PhraseWrapper(sourceWords));
    if (rules != null) {
//      System.err.println(String.format("PhraseTable::getPhrases(%s) = %d of them", Vocabulary.getWords(sourceWords),
//          rules.getRules().size()));
      return rules.getSortedRules(features);
    }
    return null;
  }
  
  public void addEOSRule() {
    int[] french = { Vocabulary.id("[X]"), Vocabulary.id("</s>") };
    
    maxSourceLength = Math.max(getMaxSourcePhraseLength(), 1);

    RuleCollection rules = new BasicRuleCollection(1, french);
    rules.getRules().add(Hypothesis.END_RULE);
    entries.put(new PhraseWrapper(new int[] { Vocabulary.id("</s>") }), rules); 
    
//    List<String> rules = new ArrayList<String>();
//    rules.add("[X,1] </s> ||| 0");
//    entries.put(new PhraseWrapper(new int[] { Vocabulary.id("</s>") }), new LazyRuleCollection(owner, 1, french, rules));
  }
  
  @Override
  public void addOOVRules(int sourceWord, List<FeatureFunction> features) {
    // TODO: _OOV shouldn't be outright added, since the word might not be OOV for the LM (but now almost
    // certainly is)
    int[] french = { Vocabulary.id("[X]"), sourceWord };
    
    String targetWord = (config.mark_oovs 
        ? Vocabulary.word(sourceWord) + "_OOV"
        : Vocabulary.word(sourceWord));

    int[] english = { -1, Vocabulary.id(targetWord) };
    final byte[] align = { 0, 0 };
    
    maxSourceLength = Math.max(getMaxSourcePhraseLength(), 1);
    
    BilingualRule oovRule = new BilingualRule(Vocabulary.id("[X]"), french, english, "", 1, align);
    oovRule.setOwner(owner);
    oovRule.estimateRuleCost(features);
    
//    List<String> rules = new ArrayList<String>();
//    rules.add(String.format("[X,1] %s ||| -1 ||| 0-0 1-1", targetWord));
//  entries.put(new PhraseWrapper(new int[] { sourceWord }), new LazyRuleCollection(owner, 1, french, rules));
    
    RuleCollection rules = new BasicRuleCollection(1, french);
    rules.getRules().add(oovRule);
    entries.put(new PhraseWrapper(new int[] { sourceWord }), rules); 
  }

  /**
   * The phrase table doesn't use a trie.
   */
  @Override
  public Trie getTrieRoot() {
    return null;
  }

  /**
   * We don't pre-sort grammars!
   */
  @Override
  public void sortGrammar(List<FeatureFunction> models) {
  }

  /**
   * We never pre-sort grammars! Why would you?
   */
  @Override
  public boolean isSorted() {
    return false;
  }

  @Override
  public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
    // No limit on maximum phrase length
    return true;
  }

  @Override
  public int getNumRules() {
    return numRules;
  }

  @Override
  public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores,
      int aritity) {
    return null;
  }

  @Override
  public void writeGrammarOnDisk(String file) {
  }

  @Override
  public boolean isRegexpGrammar() {
    return false;
  }
  
  /**
   * A simple wrapper around an int[] used for hashing
   */
  private class PhraseWrapper {
    public int[] words;

    /**
     * Initial from the source side of the rule. Delete the nonterminal that will be there, since
     * later indexing will not have it.
     * 
     * @param source the source phrase, e.g., [-1, 17, 91283]
     */
    public PhraseWrapper(int[] source) {
      this.words = Arrays.copyOfRange(source, 0, source.length);
    }
    
    @Override
    public int hashCode() {
      return Arrays.hashCode(words);
    }
    
    @Override
    public boolean equals(Object other) {
      if (other instanceof PhraseWrapper) {
        PhraseWrapper that = (PhraseWrapper) other;
        if (words.length == that.words.length) {
          for (int i = 0; i < words.length; i++)
            if (words[i] != that.words[i])
              return false;
          return true;
        }
      }
      return false;
    }
  }
}
