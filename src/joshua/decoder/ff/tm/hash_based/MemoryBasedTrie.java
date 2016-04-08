/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.decoder.ff.tm.hash_based;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

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

  @Override
  public Iterator<Integer> getTerminalExtensionIterator() {
    return new ExtensionIterator(childrenTbl, true);
  }

  @Override
  public Iterator<Integer> getNonterminalExtensionIterator() {
    return new ExtensionIterator(childrenTbl, false);
  }
}
