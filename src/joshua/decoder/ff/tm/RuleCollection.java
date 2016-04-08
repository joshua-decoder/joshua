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
package joshua.decoder.ff.tm;

import java.util.List;

import joshua.decoder.ff.FeatureFunction;

/**
 * A RuleCollection represents a set of rules that share the same source side (and hence the same
 * arity). These rules are likely stored together in a Trie data structure, although the interface
 * allows any implementation to be used.
 * 
 * @author Zhifei Li
 * @author Lane Schwartz
 * @author Matt Post <post@cs.jhu.edu>
 */
public interface RuleCollection {

  /**
   * Returns true if the rules are sorted. This is used to allow rules to be sorted in an amortized
   * fashion; rather than sorting all trie nodes when the grammar is originally loaded, we sort them
   * only as the decoder actually needs them.
   */
  boolean isSorted();

  /**
   * This returns a list of the rules, sorting them if necessary. 
   * 
   * Implementations of this function should be synchronized.  
   */
  List<Rule> getSortedRules(List<FeatureFunction> models);

  /**
   * Get the list of rules. There are no guarantees about whether they're sorted or not.
   */
  List<Rule> getRules();

  /**
   * Gets the source side for all rules in this RuleCollection. This source side is the same for all
   * the rules in the RuleCollection.
   * 
   * @return the (common) source side for all rules in this RuleCollection
   */
  int[] getSourceSide();

  /**
   * Gets the number of nonterminals in the source side of the rules in this RuleCollection. The
   * source side is the same for all the rules in the RuleCollection, so the arity will also be the
   * same for all of these rules.
   * 
   * @return the (common) number of nonterminals in the source side of the rules in this
   *         RuleCollection
   */
  int getArity();
}
