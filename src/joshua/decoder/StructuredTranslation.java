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
package joshua.decoder;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static joshua.decoder.hypergraph.ViterbiExtractor.getViterbiFeatures;
import static joshua.decoder.hypergraph.ViterbiExtractor.getViterbiString;
import static joshua.decoder.hypergraph.ViterbiExtractor.getViterbiWordAlignmentList;
import static joshua.util.FormatUtils.removeSentenceMarkers;

import java.util.List;
import java.util.Map;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.Sentence;

/**
 * structuredTranslation provides a more structured access to translation
 * results than the Translation class.
 * Members of instances of this class can be used upstream.
 * <br/>
 * TODO:
 * Enable K-Best extraction.
 * 
 * @author fhieber
 */
public class StructuredTranslation {
  
  private final Sentence sourceSentence;
  private final String translationString;
  private final List<String> translationTokens;
  private final float translationScore;
  private final List<List<Integer>> translationWordAlignments;
  private final Map<String,Float> translationFeatures;
  private final float extractionTime;
  
  public StructuredTranslation(final Sentence sourceSentence,
      final HyperGraph hypergraph,
      final List<FeatureFunction> featureFunctions) {
    
      final long startTime = System.currentTimeMillis();
      
      this.sourceSentence = sourceSentence;
      this.translationString = removeSentenceMarkers(getViterbiString(hypergraph));
      this.translationTokens = extractTranslationTokens();
      this.translationScore = extractTranslationScore(hypergraph);
      this.translationFeatures = getViterbiFeatures(hypergraph, featureFunctions, sourceSentence).getMap();
      this.translationWordAlignments = getViterbiWordAlignmentList(hypergraph);
      this.extractionTime = (System.currentTimeMillis() - startTime) / 1000.0f;
  }
  
  private float extractTranslationScore(final HyperGraph hypergraph) {
    if (hypergraph == null) {
      return 0;
    } else {
      return hypergraph.goalNode.getScore();
    }
  }
  
  private List<String> extractTranslationTokens() {
    if (translationString.isEmpty()) {
      return emptyList();
    } else {
      return asList(translationString.split("\\s+"));
    }
  }
  
  // Getters to use upstream
  
  public Sentence getSourceSentence() {
    return sourceSentence;
  }

  public int getSentenceId() {
    return sourceSentence.id();
  }

  public String getTranslationString() {
    return translationString;
  }

  public List<String> getTranslationTokens() {
    return translationTokens;
  }

  public float getTranslationScore() {
    return translationScore;
  }

  /**
   * Returns a list of target to source alignments.
   */
  public List<List<Integer>> getTranslationWordAlignments() {
    return translationWordAlignments;
  }
  
  public Map<String,Float> getTranslationFeatures() {
    return translationFeatures;
  }
  
  /**
   * Time taken to build output information from the hypergraph.
   */
  public Float getExtractionTime() {
    return extractionTime;
  }
}
