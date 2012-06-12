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

public class ZeroOneLoss extends EvaluationMetric {
  public ZeroOneLoss() {
    initialize();
  }

  public ZeroOneLoss(String[] ZOL_options) {
    this();
  }

  protected void initialize() {
    metricName = "01LOSS";
    toBeMinimized = true;
    suffStatsCount = 2;
  }

  public double bestPossibleScore() {
    return 0.0;
  }

  public double worstPossibleScore() {
    return 1.0;
  }

  public int[] suffStats(String cand_str, int i) {
    int[] stats = new int[suffStatsCount];

    boolean matchFound = false;

    for (int r = 0; r < refsPerSen; ++r) {
      if (cand_str.equals(refSentences[i][r])) {
        matchFound = true;
        break;
      }
    }

    if (matchFound) {
      stats[0] = 1;
    } else {
      stats[0] = 0;
    }

    stats[1] = 1;

    return stats;
  }

  public double score(int[] stats) {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length
          + " vs. " + suffStatsCount + ") in ZeroOneLoss.score(int[])");
      System.exit(1);
    }

    return 1.0 - (stats[0] / (double) stats[1]);
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    if (oneLiner) {
      System.out.println("01LOSS = 1.0 - " + stats[0] + "/" + stats[1] + " = "
          + f4.format(1.0 - (stats[0] / (double) stats[1])));
    } else {
      System.out.println("# correct = " + stats[0]);
      System.out.println("# sentences = " + stats[1]);
      System.out.println("01LOSS = 1.0 - " + stats[0] + "/" + stats[1] + " = "
          + f4.format(1.0 - (stats[0] / (double) stats[1])));
    }
  }

}
