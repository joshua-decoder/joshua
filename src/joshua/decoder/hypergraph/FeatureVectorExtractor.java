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
package joshua.decoder.hypergraph;

import static joshua.decoder.chart_parser.ComputeNodeResult.computeTransitionFeatures;

import java.util.List;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.hypergraph.KBestExtractor.DerivationState;
import joshua.decoder.hypergraph.KBestExtractor.DerivationVisitor;
import joshua.decoder.segment_file.Sentence;

/**
 * During decoding, individual features values are not stored, only the model score on each edge.
 * This saves space. If you want to print the actual feature values, they have to be assembled
 * from the edges of the derivation, which means replaying the feature functions. This visitor
 * does just that, using the generic derivation visitor.
 */
public class FeatureVectorExtractor implements WalkerFunction, DerivationVisitor {
  
  private final FeatureVector features;
  private final List<FeatureFunction> featureFunctions;
  private final Sentence sourceSentence;
  
  public FeatureVectorExtractor(
      final List<FeatureFunction> featureFunctions,
      final Sentence sourceSentence) {
    this.features = new FeatureVector();
    this.featureFunctions = featureFunctions;
    this.sourceSentence = sourceSentence;
  }

  /** Accumulate edge features from Viterbi path */
  @Override
  public void apply(HGNode node, int nodeIndex) {
    features.add(
        computeTransitionFeatures(
          featureFunctions,
          node.bestHyperedge,
          node.i, node.j,
          sourceSentence));
  }

  /** Accumulate edge features for that DerivationState */
  @Override
  public void before(DerivationState state, int level, int tailNodeIndex) {
    features.add(
        computeTransitionFeatures(
          featureFunctions,
          state.edge,
          state.parentNode.i, state.parentNode.j,
          sourceSentence));
  }
  
  /** Nothing to do */
  @Override
  public void after(DerivationState state, int level, int tailNodeIndex) {}
  
  public FeatureVector getFeatures() {
    return features;
  }
}
