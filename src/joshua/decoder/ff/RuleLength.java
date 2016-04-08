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

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/*
 * This feature computes three feature templates: a feature indicating the length of the rule's
 * source side, its target side, and a feature that pairs them.
 */
public class RuleLength extends StatelessFF {

  public RuleLength(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "RuleLength", args, config);
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    int sourceLen = rule.getFrench().length;
    int targetLen = rule.getEnglish().length;
    acc.add(String.format("%s_sourceLength%d", name, sourceLen), 1);
    acc.add(String.format("%s_targetLength%d", name, targetLen), 1);
    acc.add(String.format("%s_pairLength%d-%d", name, sourceLen, targetLen), 1);

    return null;
  }
}
