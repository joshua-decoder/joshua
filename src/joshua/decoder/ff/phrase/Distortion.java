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
package joshua.decoder.ff.phrase;

import java.util.ArrayList;
import java.util.List;	

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.StatelessFF;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.phrase.Hypothesis;
import joshua.decoder.segment_file.Sentence;

public class Distortion extends StatelessFF {

  public Distortion(FeatureVector weights, String[] args, JoshuaConfiguration config) {
    super(weights, "Distortion", args, config);
    
    if (! config.search_algorithm.equals("stack")) {
      System.err.println("* FATAL: Distortion feature only application for phrase-based decoding");
      System.err.println("         Use -search phrase or remove this feature");
      System.exit(1);
    }
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

    if (rule != Hypothesis.BEGIN_RULE && rule != Hypothesis.END_RULE) {
        int start_point = j - rule.getFrench().length + rule.getArity();

        int jump_size = Math.abs(tailNodes.get(0).j - start_point);
//        acc.add(name, -jump_size);
        acc.add(denseFeatureIndex, -jump_size); 
    }
    
//    System.err.println(String.format("DISTORTION(%d, %d) from %d = %d", i, j, tailNodes != null ? tailNodes.get(0).j : -1, jump_size));

    return null;
  }
}
