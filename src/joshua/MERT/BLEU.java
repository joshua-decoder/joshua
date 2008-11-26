package joshua.MERT;
import java.math.*;
import java.util.*;
import java.io.*;

public class BLEU extends EvaluationMetric
{
  private int maxGramLength;
  private int effLengthMethod;
    // 1: closest, 2: shortest, 3: average
  private HashMap[][] maxNgramCounts;

  public BLEU()
  {
    maxGramLength = 4; // default
    effLengthMethod = 1; // default
    initialize();
  }

  public BLEU(int mxGrmLn)
  {
    maxGramLength = mxGrmLn;
    effLengthMethod = 1; // default
    initialize();
  }

  public BLEU(String methodStr)
  {
    maxGramLength = 4; // default

    if (methodStr.equals("closest")) {
      effLengthMethod = 1;
    } else if (methodStr.equals("shortest")) {
      effLengthMethod = 2;
    } else if (methodStr.equals("average")) {
      effLengthMethod = 3;
    } else {
      System.out.println("Unknown effective length method string " + methodStr + ".");
      System.out.println("Should be one of closest, shortest, or average.");
      System.exit(1);
    }

    initialize();
  }

  public BLEU(int mxGrmLn,String methodStr)
  {
    maxGramLength = mxGrmLn;

    if (methodStr.equals("closest")) {
      effLengthMethod = 1;
    } else if (methodStr.equals("shortest")) {
      effLengthMethod = 2;
    } else if (methodStr.equals("average")) {
      effLengthMethod = 3;
    } else {
      System.out.println("Unknown effective length method string " + methodStr + ".");
      System.out.println("Should be one of closest, shortest, or average.");
      System.exit(1);
    }

    initialize();

  }



  protected void initialize()
  {
    metricName = "BLEU";
    toBeMinimized = false;
    set_suffStatsCount();
    set_maxNgramCounts();
  }

  public double bestPossibleScore() { return 1.0; }
  public double worstPossibleScore() { return 0.0; }

  protected void set_suffStatsCount()
  {
    suffStatsCount = 2*(maxGramLength) + 2;
      // 2 per gram length for its precision, and +2 for length info
  }

  private void set_maxNgramCounts()
  {
    maxNgramCounts = new HashMap[numSentences][1+maxGramLength];
    String gram = "";
    int oldCount = 0, nextCount = 0;

    for (int n = 1; n <= maxGramLength; ++n) {
      for (int i = 0; i < numSentences; ++i) {
        maxNgramCounts[i][n] = refSentenceInfo[i][0].getNgramCounts(n);
          // initialize to ngramCounts[n] of the first reference translation...

        // ...and update as necessary from the other reference translations
        for (int r = 1; r < refsPerSen; ++r) {
          HashMap nextNgramCounts = refSentenceInfo[i][r].getNgramCounts(n);
          Iterator it = (nextNgramCounts.keySet()).iterator();

          while (it.hasNext()) {
            gram = (String)it.next();
            nextCount = (Integer)nextNgramCounts.get(gram);

            if (maxNgramCounts[i][n].containsKey(gram)) { // update if necessary
              oldCount = (Integer)maxNgramCounts[i][n].get(gram);
              if (nextCount > oldCount) {
                maxNgramCounts[i][n].put(gram,nextCount);
              }
            } else { // add it
              maxNgramCounts[i][n].put(gram,nextCount);
            }

          }

        } // for (r)

      } // for (i)

    } // for (n)
  }




  public double[] suffStats(SentenceInfo cand, int i)
  {
    double[] stats = new double[suffStatsCount];

    double[] nextStats = new double[2];

    int s = 0;
    for (int n = 1; n <= maxGramLength; ++n) {
      nextStats = prec_suffStats(n,cand,i);
      stats[s] = nextStats[0];
      stats[s+1] = nextStats[1];
      s += 2;
    }

    nextStats = BP_suffStats(cand,i);
    stats[s] = nextStats[0];
    stats[s+1] = nextStats[1];

    return stats;
  }

  public double[] prec_suffStats(int gramLength, SentenceInfo cand, int i)
  {
    double[] counts = new double[2];

    int correctGramCount = 0;
    int totalGramCount = 0;
    String gram = "";
    int candGramCount = 0, maxRefGramCount = 0, clippedCount = 0;

    HashMap candCounts = cand.getNgramCounts(gramLength);

    Iterator it = (candCounts.keySet()).iterator();

    while (it.hasNext()) {
    // for each gram in the candidate
      gram = (String)it.next();
      candGramCount = (Integer)candCounts.get(gram);
      if (maxNgramCounts[i][gramLength].containsKey(gram)) {
        maxRefGramCount = (Integer)maxNgramCounts[i][gramLength].get(gram);
      } else {
        maxRefGramCount = 0;
      }

      clippedCount = Math.min(candGramCount,maxRefGramCount);

//      clippedCount = (candGramCount < maxRefGramCount ? candGramCount : maxRefGramCount);

      correctGramCount += clippedCount;
      totalGramCount += candGramCount;

    }

    counts[0] = correctGramCount;
    counts[1] = totalGramCount;

    return counts;
  }

  public double[] BP_suffStats(SentenceInfo cand, int i)
  {
    double[] lengths = new double[2];

    int candLength = cand.getWordCount();
    lengths[0] = candLength;

    if (effLengthMethod == 1) { // closest

      int closestRefLength = refSentenceInfo[i][0].getWordCount();
      int minDiff = Math.abs(candLength-closestRefLength);

//int minDiff = candLength-closestRefLength;
//if (minDiff < 0) minDiff = -minDiff;

      for (int r = 1; r < refsPerSen; ++r) {
        int nextRefLength = refSentenceInfo[i][r].getWordCount();
        int nextDiff = Math.abs(candLength-nextRefLength);

//int nextDiff = candLength-nextRefLength;
//if (nextDiff < 0) nextDiff = -nextDiff;

        if (nextDiff < minDiff) {
          closestRefLength = nextRefLength;
          minDiff = nextDiff;
        } else if (nextDiff == minDiff && nextRefLength < closestRefLength) {
          closestRefLength = nextRefLength;
          minDiff = nextDiff;
        }
      }

      lengths[1] = closestRefLength;

    } else if (effLengthMethod == 2) { // shortest

      int shortestRefLength = refSentenceInfo[i][0].getWordCount();

      for (int r = 1; r < refsPerSen; ++r) {
        int nextRefLength = refSentenceInfo[i][r].getWordCount();
        if (nextRefLength < shortestRefLength) {
          shortestRefLength = nextRefLength;
        }
      }

      lengths[1] = shortestRefLength;

    } else { // average

      int totalRefLength = refSentenceInfo[i][0].getWordCount();

      for (int r = 1; r < refsPerSen; ++r) {
        totalRefLength += refSentenceInfo[i][r].getWordCount();
      }

      lengths[1] = totalRefLength/(double)refsPerSen;

    }

    return lengths;

  }

  public double[] prec_suffStats(int gramLength, SentenceInfo[] candSentenceInfo)
  {
    double[] totCounts = new double[2];
    totCounts[0] = 0; totCounts[1] = 0;

    for (int i = 0; i < numSentences; ++i) {
      double[] counts = prec_suffStats(gramLength,candSentenceInfo[i],i);

      totCounts[0] += counts[0];
      totCounts[1] += counts[1];
    } // for (i)

    return totCounts;
  }

  public double[] BP_suffStats(SentenceInfo[] candSentenceInfo)
  {
    double[] totLengths = new double[2];
    totLengths[0] = 0; totLengths[1] = 0;

    for (int i = 0; i < numSentences; ++i) {
      double[] lengths = BP_suffStats(candSentenceInfo[i],i);

      totLengths[0] += lengths[0];
      totLengths[1] += lengths[1];
    } // for (i)

    return totLengths;
  }

  public double score(double[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(2);
    }

    double w_n = 1.0/maxGramLength;
    double BLEUsum = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl
    double c_len = stats[suffStatsCount-2];
    double r_len = stats[suffStatsCount-1];

    double correctGramCount, totalGramCount;
    int s = 0;

    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[s];
      totalGramCount = stats[s+1];

      double prec_n;
      if (totalGramCount > 0) {
        prec_n = correctGramCount/totalGramCount;
      } else {
        prec_n = 1; // following bleu-1.04.pl ???????
      }

      if (prec_n == 0) {
        smooth_addition *= 0.5;
        prec_n = smooth_addition / (c_len-n+1);
        // isn't c_len-n+1 just totalGramCount ???????
      }

      BLEUsum += w_n * Math.log(prec_n);

      s += 2;
    }

    double BP = 1.0;
    if (c_len < r_len) BP = Math.exp(1-(r_len/c_len));
      // if c_len > r_len, no penalty applies

    return BP*Math.exp(BLEUsum);

  }

  public void printDetailedScore_fromStats(double[] stats, boolean oneLiner)
  {
    double w_n = 1.0/maxGramLength;
    double BLEUsum = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl
    double c_len = stats[suffStatsCount-2];
    double r_len = stats[suffStatsCount-1];

    double correctGramCount, totalGramCount;
    int s = 0;

    if (oneLiner) {
      System.out.print("Precisions: ");
    }

    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[s];
      totalGramCount = stats[s+1];
      double prec_n = correctGramCount/totalGramCount;
        // what if totalGramCount is zero ???????????????????????????????

      if (prec_n > 0) {
        if (oneLiner) {
          System.out.print(n + "=" + f4.format(prec_n) + ", ");
        } else {
          System.out.println("BLEU_precision(" + n + ") = " + (int)correctGramCount + " / " + (int)totalGramCount + " = " + f4.format(prec_n));
        }
      } else {
        smooth_addition *= 0.5;
        prec_n = smooth_addition / (c_len-n+1);

        if (oneLiner) {
          System.out.print(n + "~" + f4.format(prec_n) + ", ");
        } else {
          System.out.println("BLEU_precision(" + n + ") = " + (int)correctGramCount + " / " + (int)totalGramCount + " ==smoothed==> " + f4.format(prec_n));
        }
      }

      BLEUsum += w_n * Math.log(prec_n);

      s += 2;
    }

    if (oneLiner) {
      System.out.print("(overall=" + f4.format(Math.exp(BLEUsum)) + "), ");
    } else {
      System.out.println("BLEU_precision = " + f4.format(Math.exp(BLEUsum)));
      System.out.println("");
    }

    double BP = 1.0;
    if (c_len < r_len) BP = Math.exp(1-(r_len/c_len));
      // if c_len > r_len, no penalty applies

    if (oneLiner) {
      System.out.print("BP=" + f4.format(BP) + ", ");
    } else {
      System.out.println("Length of candidate corpus = " + (int)c_len);
      System.out.println("Effective length of reference corpus = " + (int)r_len);
      System.out.println("BLEU_BP = " + f4.format(BP));
      System.out.println("");
    }

    System.out.println("BLEU = " + f4.format(BP*Math.exp(BLEUsum)));
  }
/*
  private int myMin(int x, int y)
  {
    return Math.min(x,y);
//    return (x < y ? x : y);
  }

  private int myAbs(int x)
  {
    return Math.abs(x);
//    return (x > 0 ? x : -x);
  }
*/
}
