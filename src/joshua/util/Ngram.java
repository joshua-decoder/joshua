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
package joshua.util;

import java.util.List;
import java.util.Map;

import joshua.corpus.Vocabulary;

/**
 * Provides convenience functions for extracting all ngrams from a sentence, represented as an array
 * of words.
 */
public class Ngram {

  public static void getNgrams(Map<String, Integer> tbl, int startOrder, int endOrder,
      final int[] wrds) {

    for (int i = 0; i < wrds.length; i++)
      for (int j = startOrder - 1; j < endOrder && j + i < wrds.length; j++) {// ngram: [i,i+j]
        StringBuffer ngram = new StringBuffer();
        for (int k = i; k <= i + j; k++) {
          int t_wrd = wrds[k];
          ngram.append(Vocabulary.word(t_wrd));
          if (k < i + j)
            ngram.append(" ");
        }
        String ngramStr = ngram.toString();
        increaseCount(tbl, ngramStr, 1);
      }
  }

  /** if symbolTbl!=null, then convert interger to String */
  public static void getNgrams(Map<String, Integer> tbl, int startOrder, int endOrder,
      final List<Integer> wrds) {

    for (int i = 0; i < wrds.size(); i++)
      for (int j = startOrder - 1; j < endOrder && j + i < wrds.size(); j++) {// ngram: [i,i+j]
        StringBuffer ngram = new StringBuffer();
        for (int k = i; k <= i + j; k++) {
          int t_wrd = wrds.get(k);
          ngram.append(Vocabulary.word(t_wrd));
          if (k < i + j)
            ngram.append(" ");
        }
        String ngramStr = ngram.toString();
        increaseCount(tbl, ngramStr, 1);
      }
  }

  /** if symbolTbl!=null, then convert string to integer */
  public static void getNgrams(Map<String, Integer> tbl, int startOrder, int endOrder,
      final String[] wrds) {

    for (int i = 0; i < wrds.length; i++)
      for (int j = startOrder - 1; j < endOrder && j + i < wrds.length; j++) {// ngram: [i,i+j]
        StringBuffer ngram = new StringBuffer();
        for (int k = i; k <= i + j; k++) {
          String t_wrd = wrds[k];
          ngram.append(t_wrd);
          if (k < i + j)
            ngram.append(" ");
        }
        String ngramStr = ngram.toString();
        increaseCount(tbl, ngramStr, 1);
      }
  }

  static private void increaseCount(Map<String, Integer> tbl, String feat, int increment) {
    Integer oldCount = tbl.get(feat);
    if (oldCount != null)
      tbl.put(feat, oldCount + increment);
    else
      tbl.put(feat, increment);
  }

}
