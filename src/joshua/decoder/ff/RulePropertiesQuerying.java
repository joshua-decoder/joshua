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
package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.List;
import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class RulePropertiesQuerying {

  public static final String getLHSAsString(Rule rule) {
    return Vocabulary.word(rule.getLHS());
  }

  public static List<String> getRuleSourceNonterminalStrings(Rule rule) {
    List<String> result = new ArrayList<String>();
    for (int nonTerminalIndex : rule.getForeignNonTerminals()) {
      result.add(Vocabulary.word(nonTerminalIndex));
    }
    return result;
  }

  public static List<String> getSourceNonterminalStrings(List<HGNode> tailNodes) {
    List<String> result = new ArrayList<String>();
    for (HGNode tailNode : tailNodes) {
      result.add(Vocabulary.word(tailNode.lhs));
    }
    return result;
  }

}
