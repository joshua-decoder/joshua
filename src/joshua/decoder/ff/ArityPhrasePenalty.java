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
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;
import joshua.decoder.chart_parser.SourcePath;
import joshua.corpus.Vocabulary;

/**
 * This feature function counts rules from a particular grammar (identified by the owner) having an
 * arity within a specific range. It expects three parameters upon initialization: the owner, the
 * minimum arity, and the maximum arity.
 * 
 * @author Matt Post <post@cs.jhu.edu
 * @author Zhifei Li <zhifei.work@gmail.com>
 */
public class ArityPhrasePenalty extends StatelessFF {

  // when the rule.arity is in the range, then this feature is activated
  private final int owner;
  private final int minArity;
  private final int maxArity;

  public ArityPhrasePenalty(final FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "ArityPenalty", args, config);

    this.owner = Vocabulary.id(parsedArgs.get("owner"));
    this.minArity = Integer.parseInt(parsedArgs.get("min-arity"));
    this.maxArity = Integer.parseInt(parsedArgs.get("max-arity"));
  }

  /**
   * Returns 1 if the arity penalty feature applies to the current rule.
   */
  private int isEligible(final Rule rule) {
    if (this.owner == rule.getOwner() && rule.getArity() >= this.minArity
        && rule.getArity() <= this.maxArity)
      return 1;

    return 0;
  }

  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {
    acc.add(name, isEligible(rule));
    
    return null;
  }
}
