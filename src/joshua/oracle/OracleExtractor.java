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
package joshua.oracle;

import joshua.decoder.hypergraph.HyperGraph;

/**
 * Convenience wrapper class for oracle extraction code.
 * 
 * @author Lane Schwartz
 */
public class OracleExtractor {

  private final OracleExtractionHG extractor;

  /**
   * Constructs an object capable of extracting an oracle hypergraph.
   */
  public OracleExtractor() {

    int baselineLanguageModelFeatureID = 0;
    this.extractor = new OracleExtractionHG(baselineLanguageModelFeatureID);

  }

  /**
   * Extract a hypergraph that represents the translation from the original shared forest hypergraph
   * that is closest to the reference translation.
   * 
   * @param forest Original hypergraph representing a shared forest.
   * @param lmOrder N-gram order of the language model.
   * @param reference Reference sentence.
   * @return Hypergraph closest to the reference.
   */
  public HyperGraph getOracle(HyperGraph forest, int lmOrder, String reference) {
    if (reference != null)
      return extractor.oracle_extract_hg(forest, forest.sentLen(), lmOrder, reference);

    return null;
  }

}
