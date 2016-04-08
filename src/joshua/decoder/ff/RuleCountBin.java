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

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/*
 * This feature computes a bin for the rule and activates a feature for it. It requires access to
 * the index of the RarityPenalty field, from which the rule count can be computed.
 */
public class RuleCountBin extends StatelessFF {
  private int field = -1;

  public RuleCountBin(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "RuleCountBin", args, config);

    field = Integer.parseInt(parsedArgs.get("field"));
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    if (rule.getOwner() != Vocabulary.id("pt"))
      return null;
    
    float rarityPenalty = -rule.getFeatureVector().getSparse(String.format("tm_pt_%d", field));
    int count = (int) (1.0 - Math.log(rarityPenalty));

    String feature = "RuleCountBin_inf";

    int[] bins = { 1, 2, 4, 8, 16, 32, 64, 128, 1000, 10000 };
    for (int k : bins) {
      if (count <= k) {
        feature = String.format("RuleCountBin_%d", k);
        break;
      }
    }

    System.err.println(String.format("RuleCountBin(%f) = %d ==> %s", rarityPenalty, count, feature));
    
    acc.add(feature, 1.0f);

    return null;
  }
}
