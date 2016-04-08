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

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.segment_file.Sentence;

/**
 * This feature returns the scored path through the source lattice, which is recorded in a
 * SourcePath object.
 * 
 * @author Chris Dyer <redpony@umd.edu>
 * @author Matt Post <post@cs.jhu.edu>
 */
public final class SourcePathFF extends StatelessFF {

  /*
   * This is a single-value feature template, so we cache the weight here.
   */
  public SourcePathFF(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "SourcePath", args, config);
  }

  @Override
  public ArrayList<String> reportDenseFeatures(int index) {
    denseFeatureIndex = index;
    
    ArrayList<String> names = new ArrayList<String>();
    names.add(name);
    return names;
  }
  
  @Override
  public DPState compute(Rule rule, List<HGNode> tailNodes, int i, int j, SourcePath sourcePath,
      Sentence sentence, Accumulator acc) {

    acc.add(denseFeatureIndex,  sourcePath.getPathCost());
    return null;
  }
}
