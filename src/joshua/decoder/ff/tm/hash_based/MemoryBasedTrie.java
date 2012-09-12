package joshua.decoder.ff.tm.hash_based;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedTrie implements Trie {
  MemoryBasedRuleBin ruleBin = null;
  HashMap<Integer, MemoryBasedTrie> childrenTbl = null;
  boolean regexpMatch = false;
  private HashMap<Integer, ArrayList<Trie>> regexpCache = null;
  
  public MemoryBasedTrie() {
    this.regexpMatch = false;
  }
  
  public MemoryBasedTrie(boolean regexpMatch) {
    this.regexpMatch = regexpMatch;
    this.regexpCache = new HashMap<Integer, ArrayList<Trie>>();
  }

  /*
   * We introduced the ability to have regular expressions in rules. When this is enabled for a
   * grammar, we first check whether there are any children. If there are, we need to try to match
   * _every rule_ against the symbol we're querying. This is expensive, which is an argument for
   * keeping your set of regular expression s small and limited to a separate grammar.
   */
  public ArrayList<Trie> matchAll(int sym_id) {
    ArrayList<Trie> trieList = new ArrayList<Trie>();
    if (null == childrenTbl) {
      return trieList;
    }
    MemoryBasedTrie tbl = childrenTbl.get(sym_id);
    if (tbl != null) trieList.add(tbl);
    if (regexpMatch && sym_id >= 0) {
      if (!regexpCache.containsKey(sym_id)) {
        // get all the extensions, map to string, check for *, build regexp
        for (Integer arcID : childrenTbl.keySet()) {
          String arcWord = Vocabulary.word(arcID);
          if (arcWord.contains("*") || arcWord.contains("\\")
                || arcWord.contains(".")) {
            if (Vocabulary.word(sym_id).matches(arcWord)) {
              trieList.add(childrenTbl.get(arcID));
            }
          }
        }
        regexpCache.put(sym_id, trieList);
      } else {
        for (Trie trie : regexpCache.get(sym_id)) {
          trieList.add(trie);
        }
      }
    }
    return trieList;
  }
  
  @Override
  @Deprecated
  public Trie match(int wordID) {
    ArrayList<Trie> matches = matchAll(wordID);
    if (matches.isEmpty())
      return null;
    else
      return matches.get(0);
  }
  
  /*
   * This version only looks for exact matches. For grammars with regular expressions enabled, those
   * should be applied only when traversing the completely-built trie structure. When building the
   * structure, we do not want regular expressions supplied. This is the version that should be
   * called when constructing the trie.
   */
  public MemoryBasedTrie exactMatch(int sym_id) {
    if (null == childrenTbl) {
      return null;
    } else {
      return childrenTbl.get(sym_id);
    }
  }

  /* See Javadoc for Trie interface. */
  public boolean hasExtensions() {
    return (null != this.childrenTbl);
  }

  public HashMap<Integer, MemoryBasedTrie> getExtensionsTable() {
    return this.childrenTbl;
  }

  public void setExtensions(HashMap<Integer, MemoryBasedTrie> tbl_children_) {
    this.childrenTbl = tbl_children_;
  }

  /* See Javadoc for Trie interface. */
  public boolean hasRules() {
    return (null != this.ruleBin);
  }

  public void setRuleBin(MemoryBasedRuleBin rb) {
    ruleBin = rb;
  }

  /* See Javadoc for Trie interface. */
  public RuleCollection getRuleCollection() {
    return this.ruleBin;
  }

  /* See Javadoc for Trie interface. */
  public Collection<MemoryBasedTrie> getExtensions() {
    return this.childrenTbl.values();
  }

}
