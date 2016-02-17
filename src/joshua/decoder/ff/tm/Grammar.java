package joshua.decoder.ff.tm;

import java.util.List;

import joshua.decoder.ff.FeatureFunction;

/**
 * Grammar is a class for wrapping a trie of TrieGrammar in order to store holistic metadata.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public interface Grammar {

  /**
   * Gets the root of the <code>Trie</code> backing this grammar.
   * <p>
   * <em>Note</em>: This method should run as a small constant-time function.
   * 
   * @return the root of the <code>Trie</code> backing this grammar
   */
  Trie getTrieRoot();

  /**
   * After calling this method, the rules in this grammar are guaranteed to be sorted based on the
   * latest feature function values.
   * <p>
   * Cube-pruning requires that the grammar be sorted based on the latest feature functions.
   * 
   * @param weights The model weights.
   */
  void sortGrammar(List<FeatureFunction> models);

  /**
   * Determines whether the rules in this grammar have been sorted based on the latest feature
   * function values.
   * <p>
   * This method is needed for the cube-pruning algorithm.
   * 
   * @return <code>true</code> if the rules in this grammar have been sorted based on the latest
   *         feature function values, <code>false</code> otherwise
   */
  boolean isSorted();

  /**
   * Returns whether this grammar has any valid rules for covering a particular span of a sentence.
   * Hiero's "glue" grammar will only say True if the span is longer than our span limit, and is
   * anchored at startIndex==0. Hiero's "regular" grammar will only say True if the span is less
   * than the span limit. Other grammars, e.g. for rule-based systems, may have different behaviors.
   * 
   * @param startIndex Indicates the starting index of a phrase in a source input phrase, or a
   *          starting node identifier in a source input lattice
   * @param endIndex Indicates the ending index of a phrase in a source input phrase, or an ending
   *          node identifier in a source input lattice
   * @param pathLength Length of the input path in a source input lattice. If a source input phrase
   *          is used instead of a lattice, this value will likely be ignored by the underlying
   *          implementation, but would normally be defined as <code>endIndex-startIndex</code>
   */
  boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength);

  /**
   * Gets the number of rules stored in the grammar.
   * 
   * @return the number of rules stored in the grammar
   */
  int getNumRules();
  
  /**
   * Returns the number of dense features.
   * 
   * @return the number of dense features
   */
  int getNumDenseFeatures();

  /**
   * This is used to construct a manual rule supported from outside the grammar, but the owner
   * should be the same as the grammar. Rule ID will the same as OOVRuleId, and no lattice cost
   */
  @Deprecated
  Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores, int arity);

  /**
   * Dump the grammar to disk.
   * 
   * @param file
   */
  @Deprecated
  void writeGrammarOnDisk(String file);

  /**
   * This returns true if the grammar contains rules that are regular expressions, possibly matching
   * many different inputs.
   * 
   * @return true if the grammar's rules may contain regular expressions.
   */
  boolean isRegexpGrammar();

  /**
   * Return the grammar's owner.
   */
  int getOwner();

  /**
   * Return the maximum source phrase length (terminals + nonterminals).
   */
  int getMaxSourcePhraseLength();
  
  /**
   * Add an OOV rule for the requested word for the grammar.
   * 
   * @param word
   * @param featureFunctions
   */
  void addOOVRules(int word, List<FeatureFunction> featureFunctions);
  
  /**
   * Add a rule to the grammar.
   *
   * @param Rule the rule
   */
  void addRule(Rule rule);
}
