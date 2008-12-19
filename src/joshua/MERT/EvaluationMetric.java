package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public abstract class EvaluationMetric
{
  /* static data members */
//  private static TreeSet<String> knownNames; // set of valid metric names
  private static TreeMap<String,Integer> metricOptionCount; // maps metric names -> number of options for that metric
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
  public static void set_knownMetrics()
  {
/*
    knownNames = new TreeSet<String>();
    knownNames.add("BLEU"); // implemented in BLEU.java
    knownNames.add("BLEU_SBP"); // implemented in BLEU_SBP.java
    knownNames.add("01LOSS"); // implemented in zero_one_loss.java
*/
    metricOptionCount = new TreeMap<String,Integer>();
    metricOptionCount.put("BLEU",2);         // the "BLEU" metric is implemented in BLEU.java
    metricOptionCount.put("BLEU_SBP",2);     // the "BLEU_SBP" metric is implemented in BLEU_SBP.java (which extends BLEU.java)
    metricOptionCount.put("01LOSS",0);       // the "01LOSS" metric is implemented in ZeroOneLoss.java
    metricOptionCount.put("UserMetric1",1);  // the "UserMetric1" metric is implemented in UserMetric1.java
    metricOptionCount.put("UserMetric2",1);  // the "UserMetric2" metric is implemented in UserMetric2.java
  }

  public static EvaluationMetric getMetric(String metricName, String[] metricOptions)
  {
    EvaluationMetric retMetric = null;

    if (metricName.equals("BLEU")) {
      retMetric = new BLEU(metricOptions);
    } else if (metricName.equals("BLEU_SBP")) {
      retMetric = new BLEU_SBP(metricOptions);
    } else if (metricName.equals("01LOSS")) {
      retMetric = new ZeroOneLoss(metricOptions);
    } else if (metricName.equals("UserMetric1")) {
      retMetric = new UserMetric1(metricOptions);
    } else if (metricName.equals("UserMetric2")) {
      retMetric = new UserMetric2(metricOptions);
    }

    return retMetric;
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
    return metricOptionCount.containsKey(name);
  }

  public static int metricOptionCount(String name)
  {
    return metricOptionCount.get(name);
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

