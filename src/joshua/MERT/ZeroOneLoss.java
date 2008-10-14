import java.math.*;
import java.util.*;
import java.io.*;

public class ZeroOneLoss extends ErrorMetric
{
  public ZeroOneLoss()
  {
    initialize();
  }

  protected void initialize()
  {
    metricName = "01LOSS";
    toBeMinimized = true;
    set_suffStatsCount();
  }

  public double bestPossibleScore() { return 0.0; }
  public double worstPossibleScore() { return 1.0; }

  protected void set_suffStatsCount()
  {
    suffStatsCount = 1;
  }

  public double[] suffStats(SentenceInfo cand, int i)
  {
    double[] stats = new double[suffStatsCount];

    String candSentence = cand.getSentence();

    boolean matchFound = false;

    for (int r = 0; r < refsPerSen; ++r) {
      if (candSentence.equals(refSentenceInfo[i][r].getSentence())) {
        matchFound = true;
        break;
      }
    }

    if (matchFound) stats[0] = 1;
    else stats[0] = 0;

    return stats;
  }

  public double score(double[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(1);
    }

    return 1.0 - (stats[0]/numSentences);
  }

  public void print_detailed_score(SentenceInfo[] candSentenceInfo)
  {
    double[] stats = suffStats(candSentenceInfo);

    System.out.println("01LOSS = 1.0 - " + (int)stats[0] + " / " + numSentences + " = " + f4.format(1.0 - (stats[0]/numSentences)));
  }

}
