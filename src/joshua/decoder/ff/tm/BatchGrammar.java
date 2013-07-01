package joshua.decoder.ff.tm;

import java.util.Collection;
import java.util.HashMap;

import joshua.decoder.segment_file.Sentence;
import joshua.decoder.JoshuaConfiguration;
import joshua.corpus.Vocabulary;

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

  /**
   * Returns a grammar that has been adapted to the current sentence, subject to the
   * "filter-grammar" runtime parameter.
   * 
   * @param sentence the sentence to be translated
   * @return a grammar that represents a set of translation rules
   */
  @Override
  public Grammar getGrammarForSentence(Sentence sentence) {
    if (JoshuaConfiguration.filter_grammar)
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

  /**
   * This class implements dynamic sentence-level filtering. This is accomplished with a parallel
   * trie, a subset of the original trie, that only contains trie paths that are reachable from
   * traversals of the current sentence.
   * 
   * @author Matt Post <post@cs.jhu.edu>
   */
  public class SentenceFilteredGrammar extends BatchGrammar {
    private BatchGrammar baseGrammar;
    private SentenceFilteredTrie filteredTrie;
    private int[] tokens;

    /**
     * Construct a new sentence-filtered grammar. The main work is done in the enclosed trie
     * (obtained from the base grammar, which contains the complete grammar).
     * 
     * @param baseGrammar
     * @param sentence
     */
    SentenceFilteredGrammar(BatchGrammar baseGrammar, Sentence sentence) {
      this.baseGrammar = baseGrammar;
      this.tokens = sentence.intSentence();

      long startTime = System.currentTimeMillis();
      this.filteredTrie = filter();
      float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;

      int origCount = getNumRules(baseGrammar.getTrieRoot());
      int newCount = getNumRules();
      System.err.println(String.format(
          "Sentence-level filtering of sentence %d (from %d to %d rules) in %.3f seconds",
          sentence.id(), origCount, newCount, seconds));
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
     * Take the first word of the sentence, and start at the root of the trie. There are two things
     * to consider: (a) word matches and (b) nonterminal matches.
     * 
     * For a word match, simply follow that arc along the trie. We create a parallel arc in our
     * filtered grammar to represent it. Each arc in the filtered trie knows about its
     * corresponding/underlying node in the unfiltered grammar trie.
     * 
     * A nonterminal is always permitted to match. The question then is how much of the input
     * sentence we imagine it consumed. The answer is that it could have been any amount. So the
     * recursive call has to be a set of calls, one each to the next trie node with different
     * lengths of the sentence remaining.
     * 
     * A problem occurs when we have multiple sequential nonterminals. For scope-3 grammars, there
     * can be four sequential nonterminals (in the case when they are grounded by terminals on both
     * ends of the nonterminal chain). We'd like to avoid looking at all possible ways to split up
     * the subsequence, because with respect to filtering rules, they are all the same.
     * 
     * We accomplish this with the following restriction: for purposes of grammar filtering, only
     * the first in a sequence of nonterminal traversals can consume more than one word. Each of the
     * subsequent ones would have to consume just one word. We then just have to record in the
     * recursive call whether the last traversal was a nonterminal or not.
     * 
     * @return the root of the filtered trie
     */
    private SentenceFilteredTrie filter() {
      Trie unfilteredTrieRoot = baseGrammar.getTrieRoot();
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
    private void filter(int i, SentenceFilteredTrie filteredTrieNode, boolean lastWasNT) {
      /* Spans must be at least one word long. */
      if (i >= tokens.length)
        return;

      /* Make sure the underlying unfiltered node has children. */
      Trie unfilteredTrieNode = filteredTrieNode.unfilteredTrieNode;
      if (unfilteredTrieNode.getChildren() == null)
        return;

      /* Match a word */
      Trie trie = unfilteredTrieNode.match(tokens[i]);
      if (trie != null) {
        /*
         * The current filtered node might already have an arc for this label. If so, retrieve it
         * (since we still need to follow it; if not, create it.
         */
        SentenceFilteredTrie nextFilteredTrie = filteredTrieNode.match(tokens[i]);
        if (nextFilteredTrie == null) {
          nextFilteredTrie = new SentenceFilteredTrie(trie);
          filteredTrieNode.children.put(tokens[i], nextFilteredTrie);
        }

        /*
         * Now continue, trying to match the child node against the next position in the sentence.
         * The third argument records that this match was not against a nonterminal.
         */
        filter(i + 1, nextFilteredTrie, false);
      }

      /*
       * Now we attempt to match nonterminals. Any nonterminal is permitted to match any region of
       * the sentence, up to the maximum span for that grammar. So we enumerate all children of the
       * current (unfiltered) trie node, looking for nonterminals (items whose label value is less
       * than 0), then recurse, pretending we've consumed all possible spans starting at this
       * position.
       */
      HashMap<Integer, ? extends Trie> children = unfilteredTrieNode.getChildren();
      if (children != null) {
        for (int label : children.keySet()) {
          if (label < 0) {
            SentenceFilteredTrie nextFilteredTrie = filteredTrieNode.match(label);
            if (nextFilteredTrie == null) {
              nextFilteredTrie = new SentenceFilteredTrie(unfilteredTrieNode.match(label));
              filteredTrieNode.children.put(label, nextFilteredTrie);
            }

            /*
             * Recurse. If the last match was a nonterminal, we can only consume one more token.
             * 
             * TODO: This goes too far by looking at the whole sentence; each grammar has a maximum
             * span limit which should be consulted. What we should be doing is passing the point
             * where we started matching the current sentence, so we can apply this span limit,
             * which is easily accessible (baseGrammar.spanLimit).
             */
            int maxJ = lastWasNT ? (i + 1) : tokens.length;
            for (int j = i + 1; j <= maxJ; j++) {
              filter(j, nextFilteredTrie, true);
            }
          }
        }
      }
    }
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
    HashMap<Integer, SentenceFilteredTrie> children = null;

    /**
     * Constructor.
     * 
     * @param trieRoot
     * @param sentence
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
  }
}
