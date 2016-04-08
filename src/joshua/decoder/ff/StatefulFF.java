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

import java.util.List;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/**
 * Stateful features contribute dynamic programming state. Unlike earlier versions of Joshua, the
 * stateful feature itself is responsible for computing and return its updated state. Each
 * state-computing feature function is assigned a global index, which is used to index the list of
 * state-contributing objects in each HGNode. State can no longer be shared among different feature
 * functions.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevich <juri@cs.jhu.edu>
 */
public abstract class StatefulFF extends FeatureFunction {

  /* Every stateful FF takes a unique index value and increments this. */
  static int GLOBAL_STATE_INDEX = 0;

  /* This records the state index for each instantiated stateful feature function. */
  protected int stateIndex = 0;

  public StatefulFF(FeatureVector weights, String name, String[] args, JoshuaConfiguration config) {
    super(weights, name, args, config);

    Decoder.LOG(1, "Stateful object with state index " + GLOBAL_STATE_INDEX);
    stateIndex = GLOBAL_STATE_INDEX++;
  }

  public static void resetGlobalStateIndex() {
    GLOBAL_STATE_INDEX = 0;
  }

  public final boolean isStateful() {
    return true;
  }

  public final int getStateIndex() {
    return stateIndex;
  }

  /**
   * Function computing the features that this function fires when a rule is applied. Must return
   * its updated DPState. The accumulator is used to record every feature that fires.
   */
  @Override
  public abstract DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j,
      SourcePath sourcePath, Sentence sentence, Accumulator acc);

  @Override
  public abstract DPState computeFinal(HGNode tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc);

  /**
   * Computes an estimated future cost of this rule. Note that this is not compute as part of the
   * score but is used for pruning.
   */
  @Override
  public abstract float estimateFutureCost(Rule rule, DPState state, Sentence sentence);
}
