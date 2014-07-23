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
