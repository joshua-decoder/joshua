package joshua.decoder.ff.tm.hash_based;

import java.util.Collection;
import java.util.HashMap;

import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;

import joshua.corpus.Vocabulary;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedTrie implements Trie {
  MemoryBasedRuleBin ruleBin = null;
  HashMap<Integer, MemoryBasedTrie> childrenTbl = null;
  boolean regexpMatch = false;

  public MemoryBasedTrie() { 
    boolean regexpMatch = false;
  }

  public MemoryBasedTrie(boolean regexpMatch) {
    this.regexpMatch = regexpMatch;
  }

  /* See Javadoc for Trie interface. */
  public MemoryBasedTrie match(int sym_id) {
    if (null == childrenTbl) {
      return null;
    } else if (childrenTbl.get(sym_id) != null) {
        return childrenTbl.get(sym_id);
    } else if (regexpMatch) {
      // get all the extensions, map to string, check for *, build regexp
      for (Integer arcID: childrenTbl.keySet()) {
        String arcWord = Vocabulary.word(arcID);
        if (Vocabulary.word(sym_id).matches(arcWord)) 
          return childrenTbl.get(arcID);
      }
    }
    return null;
  }

  /* This version only looks for exact matches.  For grammars with regular expressions enabled,
   * those should be applied only when traversing the completely-built trie structure.  When
   * building the structure, we do not want regular expressions supplied.  This is the version that
   * should be called when constructing the trie.
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
    // if (this.rule_bin==null) {
    // throw new
    // Error("Uninitialized RuleCollection encountered. Instead of returning a null pointer, this error is being thrown.");
    // } else {
    return this.ruleBin;
    // }
  }


  /*
   * //recursive call, to make sure all rules are sorted public void
   * ensure_sorted(ArrayList<FeatureFunction> l_models) { if (null != this.rule_bin) {
   * this.rule_bin.sortRules(l_models); } if (null != this.tbl_children) { Object[] tem =
   * this.tbl_children.values().toArray(); for (int i = 0; i < tem.length; i++) {
   * ((MemoryBasedTrie)tem[i]).ensure_sorted(l_models); } } }
   */

  /* See Javadoc for Trie interface. */
  public Collection<MemoryBasedTrie> getExtensions() {
    return this.childrenTbl.values();
  }

}
