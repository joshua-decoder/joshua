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
package joshua.decoder.phrase;

import java.util.ArrayList;	
import java.util.Collections;
import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.tm.Rule;

/**
 * Represents a sorted collection of target-side phrases. Typically, these are phrases
 * generated from the same source word sequence. The list of options is reduced to the number
 * of translation options.
 * 
 * @author Matt Post
 */

public class TargetPhrases extends ArrayList<Rule> {

  private static final long serialVersionUID = 1L;

  public TargetPhrases() {
    super();
  }
  
  /**
   * Initialize with a collection of rules.
   * 
   * @param list
   */
  public TargetPhrases(List<Rule> list) {
    super();
    
    for (Rule rule: list) {
      add(rule);
    }
  }
  
  /**
   * Score the rules and sort them. Scoring is necessary because rules are only scored if they
   * are used, in an effort to make reading in rules more efficient. This is starting to create
   * some trouble and should probably be reworked.
   */
  public void finish(List<FeatureFunction> features, FeatureVector weights, int num_options) {
    for (Rule rule: this) { 
      rule.estimateRuleCost(features);
//      System.err.println("TargetPhrases:finish(): " + rule);
    }
    Collections.sort(this, Rule.EstimatedCostComparator);
    
    if (this.size() > num_options)
      this.removeRange(num_options, this.size());
    
//    System.err.println("TargetPhrases::finish()");
//    for (Rule rule: this) 
//      System.err.println("  " + rule);
  }
}
