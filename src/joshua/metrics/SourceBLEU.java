/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package joshua.metrics;

import java.util.HashMap;
import java.util.logging.Logger;

public class SourceBLEU extends BLEU {
  private static final Logger logger = Logger.getLogger(SourceBLEU.class.getName());

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
