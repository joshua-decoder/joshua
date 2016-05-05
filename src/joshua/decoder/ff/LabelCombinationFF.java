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

/***
 * @author Gideon Wenniger
 */

import java.util.List;	

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

public class LabelCombinationFF extends StatelessFF {

  public LabelCombinationFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "LabelCombination", args, config);
  }

  public String getLowerCasedFeatureName() {
    return name.toLowerCase();
  }

  private final String computeRuleLabelCombinationDescriptor(Rule rule) {
    StringBuilder result = new StringBuilder(getLowerCasedFeatureName() + "_");
    result.append(RulePropertiesQuerying.getLHSAsString(rule));
    // System.out.println("Rule: " + rule);
    for (String foreignNonterminalString : RulePropertiesQuerying.getRuleSourceNonterminalStrings(rule)) {
      result.append("_").append(foreignNonterminalString);
    }
    return result.toString();
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    if (rule != null)
      acc.add(computeRuleLabelCombinationDescriptor(rule), 1);

    return null;
  }

}
