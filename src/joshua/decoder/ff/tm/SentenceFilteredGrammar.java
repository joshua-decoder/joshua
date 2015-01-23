package joshua.decoder.ff.tm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import joshua.decoder.ff.tm.hash_based.ExtensionIterator;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.segment_file.Sentence;

/**
 * This class implements dynamic sentence-level filtering. This is accomplished with a parallel
 * trie, a subset of the original trie, that only contains trie paths that are reachable from
 * traversals of the current sentence.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class SentenceFilteredGrammar extends MemoryBasedBatchGrammar {
  private AbstractGrammar baseGrammar;
  private SentenceFilteredTrie filteredTrie;
  private int[] tokens;
  private Sentence sentence;

  /**
   * Construct a new sentence-filtered grammar. The main work is done in the enclosed trie (obtained
   * from the base grammar, which contains the complete grammar).
   * 
   * @param baseGrammar
   * @param sentence
   */
  SentenceFilteredGrammar(AbstractGrammar baseGrammar, Sentence sentence) {
    super(baseGrammar.joshuaConfiguration);
    this.baseGrammar = baseGrammar;
    this.sentence = sentence;
    this.tokens = sentence.getWordIDs();

    int origCount = getNumRules(baseGrammar.getTrieRoot());
    long startTime = System.currentTimeMillis();

    /* Filter the rules */
    this.filteredTrie = filter(baseGrammar.getTrieRoot());
    if (filteredTrie == null)
      filteredTrie = new SentenceFilteredTrie(baseGrammar.getTrieRoot());
    int filteredCount = getNumRules();

    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;

    System.err.println(String.format(
        "Sentence-level filtering of sentence %d (%d -> %d rules) in %.3f seconds", sentence.id(),
        origCount, filteredCount, seconds));
  }

  @Override
  public Trie getTrieRoot() {
    return filteredTrie;
  }

  /**
   * This function is poorly named: it doesn't mean whether a rule exists in the grammar for the
   * current span, but whether the grammar is permitted to apply rules to the current span (a
   * grammar-level parameter). As such we can just chain to the underlying grammar.
   */
  @Override
  public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
    return baseGrammar.hasRuleForSpan(startIndex, endIndex, pathLength);
  }

  @Override
  public int getNumRules() {
    return getNumRules(getTrieRoot());
  }

  /**
   * A convenience function that counts the number of rules in a grammar's trie.
   * 
   * @param node
   * @return
   */
  public int getNumRules(Trie node) {
    int numRules = 0;
    if (node != null) {
      if (node.getRuleCollection() != null)
        numRules += node.getRuleCollection().getRules().size();

      if (node.getExtensions() != null)
        for (Trie child : node.getExtensions())
          numRules += getNumRules(child);
    }

    return numRules;
  }

  @Override
  public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores,
      int aritity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isRegexpGrammar() {
    return false;
  }

  /**
   * What is the algorithm?
   * 
   * Take the first word of the sentence, and start at the root of the trie. There are two things to
   * consider: (a) word matches and (b) nonterminal matches.
   * 
   * For a word match, simply follow that arc along the trie. We create a parallel arc in our
   * filtered grammar to represent it. Each arc in the filtered trie knows about its
   * corresponding/underlying node in the unfiltered grammar trie.
   * 
   * A nonterminal is always permitted to match. The question then is how much of the input sentence
   * we imagine it consumed. The answer is that it could have been any amount. So the recursive call
   * has to be a set of calls, one each to the next trie node with different lengths of the sentence
   * remaining.
   * 
   * A problem occurs when we have multiple sequential nonterminals. For scope-3 grammars, there can
   * be four sequential nonterminals (in the case when they are grounded by terminals on both ends
   * of the nonterminal chain). We'd like to avoid looking at all possible ways to split up the
   * subsequence, because with respect to filtering rules, they are all the same.
   * 
   * We accomplish this with the following restriction: for purposes of grammar filtering, only the
   * first in a sequence of nonterminal traversals can consume more than one word. Each of the
   * subsequent ones would have to consume just one word. We then just have to record in the
   * recursive call whether the last traversal was a nonterminal or not.
   * 
   * @return the root of the filtered trie
   */
  private SentenceFilteredTrie filter(Trie unfilteredTrieRoot) {
    SentenceFilteredTrie filteredTrieRoot = new SentenceFilteredTrie(unfilteredTrieRoot);

    // System.err.println(String.format("FILTERING TO SENTENCE\n  %s\n",
    // Vocabulary.getWords(tokens)));

    /*
     * The root of the trie is where rule applications start, so we simply try all possible
     * positions in the sentence.
     */
    for (int i = 0; i < tokens.length; i++) {
      filter(i, filteredTrieRoot, false);
    }

    return filteredTrieRoot;
  }

  /**
   * Matches rules against the sentence. Intelligently handles chains of sequential nonterminals.
   * Marks arcs that are traversable for this sentence.
   * 
   * @param i the position in the sentence to start matching
   * @param trie the trie node to match against
   * @param lastWasNT true if the match that brought us here was against a nonterminal
   */
  private void filter(int i, SentenceFilteredTrie trieNode, boolean lastWasNT) {
    if (i >= tokens.length)
      return;

    /* Make sure the underlying unfiltered node has children. */
    Trie unfilteredTrieNode = trieNode.unfilteredTrieNode;
    if (unfilteredTrieNode.getChildren() == null) {
      // trieNode.path.retreat();
      return;
    }

    /* Match a word */
    Trie trie = unfilteredTrieNode.match(tokens[i]);
    if (trie != null) {
      /*
       * The current filtered node might already have an arc for this label. If so, retrieve it
       * (since we still need to follow it); if not, create it.
       */
      SentenceFilteredTrie nextFilteredTrie = trieNode.match(tokens[i]);
      if (nextFilteredTrie == null) {
        nextFilteredTrie = new SentenceFilteredTrie(trie);
        trieNode.children.put(tokens[i], nextFilteredTrie);
      }

      /*
       * Now continue, trying to match the child node against the next position in the sentence. The
       * third argument records that this match was not against a nonterminal.
       */
      filter(i + 1, nextFilteredTrie, false);
    }

    /*
     * Now we attempt to match nonterminals. Any nonterminal is permitted to match any region of the
     * sentence, up to the maximum span for that grammar. So we enumerate all children of the
     * current (unfiltered) trie grammar node, looking for nonterminals (items whose label value is
     * less than 0), then recurse.
     * 
     * There is one subtlely. Adjacent nonterminals in a grammar rule can match a span (i, j) in (j
     * - i - 1) ways, but for purposes of determining whether a rule fits, this is all wasted
     * effort. To handle this, we allow the first nonterminal in a sequence to record 1, 2, 3, ...
     * terminals (up to the grammar's span limit, or the rest of the sentence, whichever is
     * shorter). Subsequent adjacent nonterminals are permitted to consume only a single terminal.
     */
    HashMap<Integer, ? extends Trie> children = unfilteredTrieNode.getChildren();
    if (children != null) {
      for (int label : children.keySet()) {
        if (label < 0) {
          SentenceFilteredTrie nextFilteredTrie = trieNode.match(label);
          if (nextFilteredTrie == null) {
            nextFilteredTrie = new SentenceFilteredTrie(unfilteredTrieNode.match(label));
            trieNode.children.put(label, nextFilteredTrie);
          }

          /*
           * Recurse. If the last match was a nonterminal, we can only consume one more token.
           * 
           * TODO: This goes too far by looking at the whole sentence; each grammar has a maximum
           * span limit which should be consulted. What we should be doing is passing the point
           * where we started matching the current sentence, so we can apply this span limit, which
           * is easily accessible (baseGrammar.spanLimit).
           */
          int maxJ = lastWasNT ? (i + 1) : tokens.length;
          for (int j = i + 1; j <= maxJ; j++) {
            filter(j, nextFilteredTrie, true);
          }
        }
      }
    }
  }

  /**
   * Alternate filter that uses regular expressions, walking the grammar trie and matching the
   * source side of each rule collection against the input sentence. Failed matches are discarded,
   * and trie nodes extending from that position need not be explored.
   * 
   * @return the root of the filtered trie if any rules were retained, otherwise null
   */
  @SuppressWarnings("unused")
  private SentenceFilteredTrie filter_regexp(Trie unfilteredTrie) {
    SentenceFilteredTrie trie = null;

    /* Case 1: keep the trie node if it has a rule collection that matches the sentence */
    if (unfilteredTrie.hasRules())
      if (matchesSentence(unfilteredTrie))
        trie = new SentenceFilteredTrie(unfilteredTrie);
      else
        return null;

    /* Case 2: keep the trie node if it has children who have valid rule collections */
    if (unfilteredTrie.hasExtensions())
      for (Entry<Integer, ? extends Trie> arc : unfilteredTrie.getChildren().entrySet()) {
        Trie unfilteredChildTrie = arc.getValue();
        SentenceFilteredTrie nextTrie = filter_regexp(unfilteredChildTrie);
        if (nextTrie != null) {
          if (trie == null)
            trie = new SentenceFilteredTrie(unfilteredTrie);
          trie.children.put(arc.getKey(), nextTrie);
        }
      }

    return trie;
  }

  private boolean matchesSentence(Trie childTrie) {
    Rule rule = childTrie.getRuleCollection().getRules().get(0);
    return rule.matches(sentence);
  }

  /**
   * Implements a filtered trie, by sitting on top of a base trie and annotating nodes that match
   * the given input sentence.
   * 
   * @author Matt Post <post@cs.jhu.edu>
   * 
   */
  public class SentenceFilteredTrie implements Trie {

    /* The underlying unfiltered trie node. */
    private Trie unfilteredTrieNode;

    /* The child nodes in the filtered trie. */
    private HashMap<Integer, SentenceFilteredTrie> children = null;

    /**
     * Constructor.
     * 
     * @param trieRoot
     * @param source
     */
    public SentenceFilteredTrie(Trie unfilteredTrieNode) {
      this.unfilteredTrieNode = unfilteredTrieNode;
      this.children = new HashMap<Integer, SentenceFilteredTrie>();
    }

    @Override
    public SentenceFilteredTrie match(int wordID) {
      if (children != null)
        return children.get(wordID);
      return null;
    }

    @Override
    public boolean hasExtensions() {
      return children != null;
    }

    @Override
    public Collection<SentenceFilteredTrie> getExtensions() {
      if (children != null)
        return children.values();

      return null;
    }

    @Override
    public HashMap<Integer, SentenceFilteredTrie> getChildren() {
      return children;
    }

    @Override
    public boolean hasRules() {
      // Chain to the underlying unfiltered node.
      return unfilteredTrieNode.hasRules();
    }

    @Override
    public RuleCollection getRuleCollection() {
      // Chain to the underlying unfiltered node, since the rule collection just varies by target
      // side.
      return unfilteredTrieNode.getRuleCollection();
    }

    /**
     * Counts the number of rules.
     * 
     * @return the number of rules rooted at this node.
     */
    public int getNumRules() {
      int numRules = 0;
      if (getTrieRoot() != null)
        if (getTrieRoot().getRuleCollection() != null)
          numRules += getTrieRoot().getRuleCollection().getRules().size();

      for (SentenceFilteredTrie node : getExtensions())
        numRules += node.getNumRules();

      return numRules;
    }

    @Override
    public Iterator<Integer> getTerminalExtensionIterator() {
      return new ExtensionIterator(children, true);
    }

    @Override
    public Iterator<Integer> getNonterminalExtensionIterator() {
      return new ExtensionIterator(children, false);
    }
  }
}