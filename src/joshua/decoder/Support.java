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

import java.util.List;

/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class Support {

  public static double findMin(double a, double b) {
    return (a <= b) ? a : b;
  }

  public static double findMax(double a, double b) {
    return (a > b) ? a : b;
  }

  
  public static int[] toArray(List<Integer> in) {
    return subIntArray(in, 0, in.size());
  }

  /**
   * @param start inclusive
   * @param end exclusive
   */
  public static int[] subIntArray(List<Integer> in, int start, int end) {
    int[] res = new int[end - start];
    for (int i = start; i < end; i++) {
      res[i - start] = in.get(i);
    }
    return res;
  }

  public static long current_time() {
    return 0;
    // return System.currentTimeMillis();
    // return System.nanoTime();
  }

  // Only used in LMGrammarJAVA
  public static long getMemoryUse() {
    putOutTheGarbage();
    long totalMemory = Runtime.getRuntime().totalMemory();// all the memory I get from the system
    putOutTheGarbage();
    long freeMemory = Runtime.getRuntime().freeMemory();
    return (totalMemory - freeMemory) / 1024;// in terms of kb
  }

  private static void putOutTheGarbage() {
    collectGarbage();
    collectGarbage();
  }

  private static void collectGarbage() {
    long fSLEEP_INTERVAL = 100;
    try {
      System.gc();
      Thread.sleep(fSLEEP_INTERVAL);
      System.runFinalization();
      Thread.sleep(fSLEEP_INTERVAL);

    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }
}
