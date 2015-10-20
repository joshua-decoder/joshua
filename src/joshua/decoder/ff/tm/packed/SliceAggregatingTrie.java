package joshua.decoder.ff.tm.packed;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hash_based.ExtensionIterator;

/**
 * SliceAggregatingTrie collapses multiple tries
 * with the same source root (i.e. tries from multiple packed slices).
 * 
 * Consider the example below.
 * Without SliceAggregatingTries, the following grammar rules could have only
 * partitioned by splitting rule lists when the first word of SOURCE changes. (">" markers).
 * 
 * Using a SliceAggregatingTrie allows splitting at changes of second SOURCE words (">>" marker).
 * 
 * EXAMPLE: (LHS ||| SOURCE ||| TARGET)
 * [X] ||| - ||| -
 * >
 * [X] ||| [X] ||| [X]
 * >>
 * [X] ||| [X] a ||| [X] A
 * [X] ||| [X] a ||| [X] A
 * >>
 * [X] ||| [X] b ||| [X] B
 * >
 * [X] ||| u ||| u
 * 
 * A SliceAggregatingTrie node behaves just like a regular Trie node but subsumes a list of extensions/children.
 * This class hides the complexity of having multiple tries with the same root
 * from nodes one level up.
 * Similar to PackedRoot, it maintains a lookup table of children's
 * source-side words to know
 * in which subtrie (i.e. packedSlice) it needs to traverse into when 
 * match() is called.
 * A SliceAggregatingTrie never holds any rules associated with it, thus
 * rules with the source-side represented by the SliceAggregatingTrie node
 * must be found in exactly one of the subtries.
 * (!) This assumption relies on the sort order of the packed grammar.
 * If the grammar was incorrectly sorted and then packed, construction
 * of SliceAggregatingTrie nodes fails. 
 * 
 * @author fhieber
 */
public class SliceAggregatingTrie implements Trie, RuleCollection {
  
  /**
   * A multitude of packedTries with the same source-side
   * firstword. The order is induced by the
   * sorting order of the text grammar that was input to the GrammarPacker.
   * This implies that rules for the node represented by this SliceAggregatingTrie
   * instance must be found in ONE of the sub tries.
   * This is checked below in the constructor. 
   */
  private final List<Trie> tries;
  /** reference to the only subtrie that can contain rules. Set by buildLookupTable() */
  private Trie trieWithRules = null;
  
  /** Maintains an index of all children of all sub tries */
  private final HashMap<Integer, Trie> lookup = new HashMap<>();
  
  public SliceAggregatingTrie(final List<Trie> tries) {
    if (tries == null || tries.isEmpty()) {
      throw new RuntimeException(
          "SliceAggregatingTrie node requires at least one packedTrie");
    }
    this.tries = unmodifiableList(tries);
    buildLookupTable();
  }
  
  /**
   * Fills the lookup table for child nodes.
   * Also performs various checks to ensure correctness of the 
   * PackedTrie aggregation. 
   */
  private void buildLookupTable() {
    final Set<Integer> seen_child_ids = new HashSet<>();
    Trie previous_trie = null;
    boolean first = true;
    for (final Trie trie : this.tries) {
      /*
       * perform some checks to make sure tries are correctly split.
       */
      if (!first) {
        if (!haveSameSourceSide(previous_trie, trie) || !haveSameArity(previous_trie, trie)) {
          throw new RuntimeException("SliceAggregatingTrie's subtries differ in sourceSide or arity. Was the text grammar sorted insufficiently?");
        }
      } else {
        first = false;
      }
      previous_trie = trie;
      
      if (trie.hasRules()) {
        if (trieWithRules != null) {
          throw new RuntimeException("SliceAggregatingTrie can only have one subtrie with rules. Was the text grammar sorted insufficiently?");
        }
        trieWithRules = trie;
      }

      final HashMap<Integer, ? extends Trie> children = trie.getChildren();
      for (int id : children.keySet()) {
        if (seen_child_ids.contains(id)) {
          throw new RuntimeException("SliceAggregatingTrie's subtries contain non-disjoint child words. Was the text grammar sorted insufficiently?");
        }
        seen_child_ids.add(id);
        lookup.put(id, children.get(id));
      }
    }
  }
  
  private boolean haveSameSourceSide(final Trie t1, final Trie t2) {
    return Arrays.equals(
        t1.getRuleCollection().getSourceSide(),
        t2.getRuleCollection().getSourceSide());
  }
  
  private boolean haveSameArity(final Trie t1, final Trie t2) {
    return t1.getRuleCollection().getArity() == t2.getRuleCollection().getArity();
  }
  
  @Override
  public Trie match(int wordId) {
    return lookup.get(wordId);
  }

  @Override
  public boolean hasExtensions() {
    return !lookup.isEmpty();
  }

  @Override
  public Collection<? extends Trie> getExtensions() {
    return new ArrayList<>(lookup.values());
  }

  @Override
  public HashMap<Integer, ? extends Trie> getChildren() {
    return lookup;
  }

  @Override
  public Iterator<Integer> getTerminalExtensionIterator() {
    return new ExtensionIterator(lookup, true);
  }

  @Override
  public Iterator<Integer> getNonterminalExtensionIterator() {
    return new ExtensionIterator(lookup, true);
  }
  
  @Override
  public RuleCollection getRuleCollection() {
    return this;
  }
  
  /*
   * The following method's return values depend on whether there is 
   * a single subtrie encoding rules (trieWithRules).
   * All other subtries can only contain rules some levels deeper.
   */ 
  
  @Override
  public boolean hasRules() {
    return trieWithRules == null ? false : trieWithRules.hasRules();
  }
  
  @Override
  public List<Rule> getRules() {
    if (!hasRules()) {
      return emptyList();
    }
    return trieWithRules.getRuleCollection().getRules();
  }
  
  @Override
  public List<Rule> getSortedRules(List<FeatureFunction> models) {
    if (!hasRules()) {
      return emptyList();
    }
    return trieWithRules.getRuleCollection().getSortedRules(models);
  }

  @Override
  public boolean isSorted() {
    return !hasRules() ? false : trieWithRules.getRuleCollection().isSorted();
  }

  /*
   * The constructor checked that all sub tries have the same arity and sourceSide.
   * We can thus simply return the value from the first in list.
   */

  @Override
  public int[] getSourceSide() {
    return tries.get(0).getRuleCollection().getSourceSide();
  }

  @Override
  public int getArity() {
    return tries.get(0).getRuleCollection().getArity();
  }

}
