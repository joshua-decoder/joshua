package joshua.decoder.ff.tm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * An interface for trie-like data structures.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public interface Trie {

  /**
   * Traverse one ply further down the trie. If there is no match, the result is null.
   * 
   * @param wordID
   * @return Child node of this trie
   */
  Trie match(int wordID);

  
  /**
   * Returns whether matchOne(Symbol) could succeed for any symbol.
   * 
   * @return <code>true</code> if {@link #match(int)} could succeed for some symbol,
   *         <code>false</code> otherwise
   */
  boolean hasExtensions();


  /**
   * If the trie node has extensions, then return a list of extended trie nodes, otherwise return
   * null.
   * 
   * @return A list of extended <code>Trie</code> nodes if this node has extensions,
   *         <code>null<code>
   *         otherwise
   */
  Collection<? extends Trie> getExtensions();


  /**
   * If the trie node has extensions, get a list of their labels.
   * 
   * @return
   */
  HashMap<Integer,? extends Trie> getChildren();

  /**
   * Returns an iterator over the trie node's extensions with terminal labels.
   * 
   * @return
   */
  Iterator<Integer> getTerminalExtensionIterator();
  
  /**
   * Returns an iterator over the trie node's extensions with nonterminal labels.
   * 
   * @return
   */
  Iterator<Integer> getNonterminalExtensionIterator();
  
  
  /**
   * Gets whether the current node/state is a "final state" that has matching rules.
   * 
   * @return <code>true</code> if the current node/state is a "final state" that has matching rules,
   *         <code>false</code> otherwise
   */
  boolean hasRules();


  /**
   * Retrieve the rules at the current node/state. The implementation of this method must adhere to
   * the following laws:
   * 
   * <ol>
   * <li>The return value is always non-null. The collection may be empty however.</li>
   * <li>The collection must be empty if hasRules() is false, and must be non-empty if hasRules() is
   * true.</li>
   * <li>The collection must be sorted (at least as used by TMGrammar)</li>
   * </ol>
   */
  RuleCollection getRuleCollection();

}
