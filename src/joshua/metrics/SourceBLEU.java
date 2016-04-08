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
package joshua.metrics;

import java.util.HashMap;

public class SourceBLEU extends BLEU {
  // We assume that the source for the paraphrasing run is
  // part of the set of references
  private int sourceReferenceIndex;

  private int[] sourceWordCount;
  private boolean useBrevityPenalty;

  public SourceBLEU() {
    super();
    this.sourceReferenceIndex = 0;
    this.useBrevityPenalty = true;
    initialize();
  }

  public SourceBLEU(String[] options) {
    super(options);
    this.sourceReferenceIndex = Integer.parseInt(options[2]);
    this.useBrevityPenalty = Boolean.parseBoolean(options[3]);
    initialize();
  }

  public SourceBLEU(int num_references, String method, int source_index, boolean use_brevity_penalty) {
    super(num_references, method);
    this.sourceReferenceIndex = source_index;
    this.useBrevityPenalty = use_brevity_penalty;
    initialize();
  }

  protected void initialize() {
    metricName = "SRC_BLEU";
    toBeMinimized = true;
    suffStatsCount = 2 * maxGramLength + 2;

    set_weightsArray();
    set_maxNgramCounts();
  }

  public double bestPossibleScore() {
    return 0.0;
  }

  public double worstPossibleScore() {
    return 1.0;
  }

  protected void set_maxNgramCounts() {
    @SuppressWarnings("unchecked")
    HashMap<String, Integer>[] temp_HMA = new HashMap[numSentences];
    maxNgramCounts = temp_HMA;
    sourceWordCount = new int[numSentences];

    for (int i = 0; i < numSentences; ++i) {
      sourceWordCount[i] = wordCount(refSentences[i][sourceReferenceIndex]);
      maxNgramCounts[i] = getNgramCountsAll(refSentences[i][sourceReferenceIndex]);
    }
  }

  public int[] suffStats(String cand_str, int i) {
    int[] stats = new int[suffStatsCount];

    String[] candidate_words;
    if (!cand_str.equals(""))
      candidate_words = cand_str.split("\\s+");
    else
      candidate_words = new String[0];

    set_prec_suffStats(stats, candidate_words, i);
    if (this.useBrevityPenalty)
      stats[suffStatsCount - 1] = effLength(candidate_words.length, i);
    else
      stats[suffStatsCount - 1] = candidate_words.length;
    stats[suffStatsCount - 2] = candidate_words.length;

    return stats;
  }

  public int effLength(int candLength, int i) {
    return sourceWordCount[i];
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    System.out.println(String.format("SRC_BLEU = %.4f", score(stats)));
  }
}
