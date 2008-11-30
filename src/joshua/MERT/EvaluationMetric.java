package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public abstract class EvaluationMetric
{
  /* static data members */
  private static TreeSet knownNames; // set of valid metric names
  protected static int numSentences; // number of sentences in the MERT set
  protected static int refsPerSen;
  protected static String[][] refSentences;
  protected static DecimalFormat f0 = new DecimalFormat("###0");
  protected static DecimalFormat f4 = new DecimalFormat("###0.0000");

  /* non-static data members */
  protected int suffStatsCount; // number of sufficient statistics
  protected String metricName; // number of metric
  protected boolean toBeMinimized;
    // is this a metric that should be minimized?
    // e.g. toBeMinimized = true for 01LOSS, WER, TER
    //      toBeMinimized = false for BLEU

  /* static (=> also non-abstract) methods */
  public static void set_knownNames()
  {
    knownNames = new TreeSet();
    knownNames.add("BLEU"); // implemented in BLEU.java
    knownNames.add("01LOSS"); // implemented in zero_one_loss.java
  }

  public static void set_numSentences(int x) { numSentences = x; }
  public static void set_refsPerSen(int x) { refsPerSen = x; }
  public static void set_refSentences(String[][] refs)
  {
    refSentences = new String[numSentences][refsPerSen];
    for (int i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        refSentences[i][r] = refs[i][r];
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
  public boolean getToBeMinimized() { return toBeMinimized; }
  public boolean isBetter(double x, double y)
  {
    // return true if x is better than y
    if (toBeMinimized) {
      return (x < y);
    } else {
      return (x > y);
    }
  }

  public double score(String cand_str, int i)
  {
    int[] stats = suffStats(cand_str,i);
    return score(stats);
  }

  public double score(String[] topCand_str)
  {
    int[] stats = suffStats(topCand_str);
    return score(stats);
  }

  public int[] suffStats(String[] topCand_str)
  {
    int[] totStats = new int[suffStatsCount];
    for (int s = 0; s < suffStatsCount; ++s) { totStats[s] = 0; }

    for (int i = 0; i < numSentences; ++i) {
      int[] stats = suffStats(topCand_str[i],i);

      for (int s = 0; s < suffStatsCount; ++s) { totStats[s] += stats[s]; }
    } // for (i)

    return totStats;
  }

  public void printDetailedScore(String[] topCand_str, boolean oneLiner)
  {
    int[] stats = suffStats(topCand_str);
    printDetailedScore_fromStats(stats,oneLiner);
  }

  public void printDetailedScore(String cand_str, int i, boolean oneLiner)
  {
    int[] stats = suffStats(cand_str,i);
    printDetailedScore_fromStats(stats,oneLiner);
  }

  /* abstract (=> also non-static) methods */
  protected abstract void initialize();
  public abstract double bestPossibleScore();
  public abstract double worstPossibleScore();
  protected abstract void set_suffStatsCount();
  public abstract int[] suffStats(String cand_str, int i);
  public abstract double score(int[] stats);
  public abstract void printDetailedScore_fromStats(int[] stats, boolean oneLiner);
}

