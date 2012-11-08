package joshua.decoder.ff.tm.hash_based;

import java.util.Collection;
import java.util.HashMap;

import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedTrie implements Trie {
  MemoryBasedRuleBin ruleBin = null;
  HashMap<Integer, MemoryBasedTrie> childrenTbl = null;
  
  public MemoryBasedTrie() {
  }

  @Override
  public Trie match(int wordID) {
    if (childrenTbl != null)
      return childrenTbl.get(wordID);
    return null;
  }
  
  /* See Javadoc for Trie interface. */
  public boolean hasExtensions() {
    return (null != this.childrenTbl);
  }

  public HashMap<Integer, MemoryBasedTrie> getChildren() {
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
    if (this.childrenTbl != null)
      return this.childrenTbl.values();
    return null;
  }
}
