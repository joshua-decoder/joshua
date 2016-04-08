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
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.phrase.Hypothesis;
import joshua.decoder.segment_file.Sentence;

/**
 *  This feature just counts rules that are used. You can restrict it with a number of flags:
 * 
 *   -owner OWNER
 *    Only count rules owned by OWNER
 *   -target|-source
 *    Only count the target or source side (plus the LHS)
 *
 * TODO: add an option to separately provide a list of rule counts, restrict to counts above a threshold. 
 */
public class PhrasePenalty extends StatelessFF {

  private int owner = 0;
  private float value = 1.0f;
  
  public PhrasePenalty(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "PhrasePenalty", args, config);
    if (parsedArgs.containsKey("owner"))
      this.owner = Vocabulary.id(parsedArgs.get("owner"));
    else // default
      this.owner = Vocabulary.id("pt"); 
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule != null && rule != Hypothesis.BEGIN_RULE && rule != Hypothesis.END_RULE 
        && (owner == 0 || rule.getOwner() == owner))
      acc.add(denseFeatureIndex, value);

    return null;
  }
    
  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    ArrayList<String> names = new ArrayList<String>();
    names.add(name);
    return names;
  }
  
  /**
   * Returns the *weighted* estimate.
   * 
   */
  @Override
  public float estimateCost(Rule rule, Sentence sentence) {
    if (rule != null && rule != Hypothesis.BEGIN_RULE && rule != Hypothesis.END_RULE 
        && (owner == 0 || rule.getOwner() == owner))
      return weights.getDense(denseFeatureIndex) * value;
    return 0.0f;
  }
}
