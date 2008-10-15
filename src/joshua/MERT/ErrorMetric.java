package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public abstract class ErrorMetric
{
  /* static data members */
  private static TreeSet knownNames; // set of valid metric names
  protected static int numSentences; // number of sentences in the MERT set
  protected static int refsPerSen;
  protected static SentenceInfo[][] refSentenceInfo;
  protected static DecimalFormat f0 = new DecimalFormat("###0");
  protected static DecimalFormat f4 = new DecimalFormat("###0.0000");

  /* non-static data members */
  protected int suffStatsCount; // number of sufficient statistics
  protected String metricName; // number of metric
  protected boolean toBeMinimized;
    // is this a metric that should be minimized?
    // e.g. toBeMinimized = true for 01LOSS, WER
    //      toBeMinimized = false for BLEU

  /* static (=> also non-abstract) methods */
  public static void set_knownNames()
  {
    knownNames = new TreeSet();
    knownNames.add("BLEU"); // implemented in BLEU.java
    knownNames.add("01LOSS"); // implemented in zero_one_loss.java
  }

  public static void set_numSentences(int n) { numSentences = n; }
  public static void set_refsPerSen(int n) { refsPerSen = n; }
  public static void set_refSentenceInfo(SentenceInfo[][] refs)
  {
    refSentenceInfo = new SentenceInfo[numSentences][refsPerSen];
    for (int i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        refSentenceInfo[i][r] = new SentenceInfo(refs[i][r]);
      }
    }
  }

  public static boolean knownMetricName(String name)
  {
    return knownNames.contains(name);
  }

  /* non-abstract, non-static methods */
  public int get_suffStatsCount() { return suffStatsCount; }
  public String get_metricName() { return metricName; }
  public boolean isBetter(double x, double y)
  {
    // return true if x is better than y
    if (toBeMinimized) {
      return (x < y);
    } else {
      return (x > y);
    }
  }

  public double score(SentenceInfo cand, int i)
  {
    double[] stats = suffStats(cand,i);

    return score(stats);
  }

  public double score(SentenceInfo[] candSentenceInfo)
  {
    double[] stats = suffStats(candSentenceInfo);

    return score(stats);
  }

  public double[] suffStats(SentenceInfo[] candSentenceInfo)
  {
    double[] totStats = new double[suffStatsCount];
    for (int s = 0; s < suffStatsCount; ++s) { totStats[s] = 0.0; }

    for (int i = 0; i < numSentences; ++i) {
      double[] stats = suffStats(candSentenceInfo[i],i);

      for (int s = 0; s < suffStatsCount; ++s) { totStats[s] += stats[s]; }
    } // for (i)

    return totStats;
  }

  /* abstract (=> also non-static) methods */
  protected abstract void initialize();
  public abstract double bestPossibleScore();
  public abstract double worstPossibleScore();
  protected abstract void set_suffStatsCount();
  public abstract double[] suffStats(SentenceInfo cand, int i);
  public abstract double score(double[] stats);
  public abstract void print_detailed_score(SentenceInfo[] candSentenceInfo);
}

