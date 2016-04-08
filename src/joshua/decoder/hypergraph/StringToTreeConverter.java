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
package joshua.decoder.hypergraph;

import java.util.Stack;

// example: (ROOT ([S] ([X] ([X] scientists completed ([X] for ([X] ([X] chromosome) related to ([X]
// early ([X] OOV))))) sequencing)))

public class StringToTreeConverter {

  static private final String beginSymbol = "(b";
  static private final String nodeSymbol = "node";

  HyperGraph convert(String inputStr) {

    HyperGraph tree = null;

    Stack<String> stack = new Stack<String>();
    for (int i = 0; i < inputStr.length(); i++) {
      char curChar = inputStr.charAt(i);

      if (curChar == ')' && inputStr.charAt(i - 1) != ' ') {// end of a rule
        StringBuffer ruleString = new StringBuffer();

        while (stack.empty() == false) {
          String cur = stack.pop();
          if (cur.equals(beginSymbol)) {// stop
            // setup a node
            // HGNode(int i, int j, int lhs, HashMap<Integer,DPState> dpStates, HyperEdge
            // initHyperedge, double estTotalLogP)
            // public HyperEdge(Rule rule, double bestDerivationLogP, Double transitionLogP,
            // List<HGNode> antNodes, SourcePath srcPath)
            // public BilingualRule(int lhs, int[] sourceRhs, int[] targetRhs, float[]
            // featureScores, int arity, int owner, float latticeCost, int ruleID)


            stack.add(nodeSymbol);// TODO: should be lHS+id
            break;
          } else if (cur.equals(nodeSymbol)) {

          } else {
            ruleString.append(cur);
          }
        }
      } else if (curChar == '(' && inputStr.charAt(i + 1) != ' ') {// begin of a rule
        stack.add(beginSymbol);
      } else {
        stack.add("" + curChar);
      }
    }



    return tree;
  }

}
