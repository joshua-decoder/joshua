package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class ZeroOneLoss extends EvaluationMetric
{
  public ZeroOneLoss()
  {
    initialize();
  }

  public ZeroOneLoss(String[] ZOL_options)
  {
    this();
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

  public int[] suffStats(String cand_str, int i)
  {
    int[] stats = new int[suffStatsCount];

//    String candSentence = cand.toString();

    boolean matchFound = false;

    for (int r = 0; r < refsPerSen; ++r) {
      if (cand_str.equals(refSentences[i][r])) {
        matchFound = true;
        break;
      }
    }

    if (matchFound) stats[0] = 1;
    else stats[0] = 0;

    return stats;
  }

  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(1);
    }

    return 1.0 - (stats[0]/(double)numSentences);
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    if (oneLiner) {
      System.out.println("01LOSS = 1.0 - " + stats[0] + "/" + numSentences + " = " + f4.format(1.0 - (stats[0]/(double)numSentences)));
    } else {
      System.out.println("# correct = " + stats[0]);
      System.out.println("# sentences = " + numSentences);
      System.out.println("01LOSS = 1.0 - " + stats[0] + "/" + numSentences + " = " + f4.format(1.0 - (stats[0]/(double)numSentences)));
    }
  }

}
