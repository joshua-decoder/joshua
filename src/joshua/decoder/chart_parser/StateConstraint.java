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
package joshua.decoder.chart_parser;

import java.util.Collection;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;

/**
 * This class provides constraints on the sorts of states that are permitted in the chart. Its
 * original motivation was to be used as a means of doing forced decoding, which is accomplished by
 * forcing all n-gram states that are created to match the target string.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * 
 */
public class StateConstraint {
  private String target = null;

  public StateConstraint(String target) {
    this.target = " <s> " + target + " </s> ";
  }

  /**
   * Determines if all of the states passed in are legal in light of the input that was passed
   * earlier. Currently only defined for n-gram states.
   * 
   * @param dpStates
   * @return whether the states are legal in light of the target side sentence
   */
  public boolean isLegal(Collection<DPState> dpStates) {
    /*
     * Iterate over all the state-contributing objects associated with the new state, querying
     * n-gram ones (of which there is probably only one), allowing them to veto the move.
     */
    for (DPState dpState : dpStates) {
      if (dpState instanceof NgramDPState) {
        // Build a regular expression out of the state context.
        String leftWords = " "
            + Vocabulary.getWords(((NgramDPState) dpState).getLeftLMStateWords()) + " ";
        String rightWords = " "
            + Vocabulary.getWords(((NgramDPState) dpState).getRightLMStateWords()) + " ";

        int leftPos = this.target.indexOf(leftWords);
        int rightPos = this.target.lastIndexOf(rightWords);

        boolean legal = (leftPos != -1 && leftPos <= rightPos);
//        System.err.println(String.format("  isLegal(%s @ %d,%s @ %d) = %s", leftWords, leftPos,
//         rightWords, rightPos, legal));

        return legal;
      }
    }

    return true;
  }
}
