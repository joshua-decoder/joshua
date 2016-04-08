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

import joshua.decoder.ff.tm.BasicRuleCollection;
import joshua.decoder.ff.tm.Rule;

/**
 * Stores a collection of all rules with the same french side (and thus same arity).
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedRuleBin extends BasicRuleCollection {

  /**
   * Constructs an initially empty rule collection.
   * 
   * @param arity Number of nonterminals in the source pattern
   * @param sourceTokens Sequence of terminals and nonterminals in the source pattern
   */
  public MemoryBasedRuleBin(int arity, int[] sourceTokens) {
    super(arity, sourceTokens);
  }

  /**
   * Adds a rule to this collection.
   * 
   * @param rule Rule to add to this collection.
   */
  public void addRule(Rule rule) {
    // XXX This if clause seems bogus.
    if (rules.size() <= 0) { // first time
      this.arity = rule.getArity();
      this.sourceTokens = rule.getFrench();
    }
    if (rule.getArity() != this.arity) {
      return;
    }
    rules.add(rule);
    sorted = false;
    rule.setFrench(this.sourceTokens);
  }
}
