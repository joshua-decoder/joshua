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

public class BLEU_SBP extends BLEU {
  // constructors
  public BLEU_SBP() {
    super();
  }

  public BLEU_SBP(String[] BLEU_SBP_options) {
    super(BLEU_SBP_options);
  }

  public BLEU_SBP(int mxGrmLn, String methodStr) {
    super(mxGrmLn, methodStr);
  }



  public int[] suffStats(String cand_str, int i) {
    int[] stats = new int[suffStatsCount];
    stats[0] = 1;

    String[] words = cand_str.split("\\s+");

    // int wordCount = words.length;
    // for (int j = 0; j < wordCount; ++j) { words[j] = words[j].intern(); }

    set_prec_suffStats(stats, words, i);

    // the only place where BLEU_SBP differs from BLEU /* ~~~ */
    /* ~~~ */
    // stats[maxGramLength+1] = words.length;
    // stats[maxGramLength+2] = effLength(words.length,i);
    /* ~~~ */

    /* ~~~ */
    int effectiveLength = effLength(words.length, i);
    stats[maxGramLength + 1] = Math.min(words.length, effectiveLength);
    stats[maxGramLength + 2] = effectiveLength;
    /* ~~~ */

    return stats;
  }

}
