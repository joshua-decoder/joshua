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
  private int[][] refWordCount;
  private double[] weights;

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
    set_weightsArray();
    set_maxNgramCounts();
  }

  public double bestPossibleScore() { return 1.0; }
  public double worstPossibleScore() { return 0.0; }

  protected void set_suffStatsCount()
  {
    suffStatsCount = 1 + maxGramLength + 2;
      // 1 for number of segments, 1 per gram length for its precision, and 2 for length info
  }

  private void set_weightsArray()
  {
    weights = new double[1+maxGramLength];
    for (int n = 1; n <= maxGramLength; ++n) {
      weights[n] = 1.0/maxGramLength;
    }
  }

  private void set_maxNgramCounts()
  {
    maxNgramCounts = new HashMap[numSentences][1+maxGramLength];
    String gram = "";
    int oldCount = 0, nextCount = 0;

    for (int n = 1; n <= maxGramLength; ++n) {
      for (int i = 0; i < numSentences; ++i) {
        maxNgramCounts[i][n] = getNgramCounts(refSentences[i][0],n);
          // initialize to ngramCounts[n] of the first reference translation...

        // ...and update as necessary from the other reference translations
        for (int r = 1; r < refsPerSen; ++r) {
          HashMap nextNgramCounts = getNgramCounts(refSentences[i][r],n);
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

    // Reference sentences are not needed anymore, since the gram counts are stored.
    // The only thing we need are their lenghts, to be used in BP_suffStats, so store
    // the lengths before discarding the reference sentences...

    refWordCount = new int[numSentences][refsPerSen];
    for (int i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        refWordCount[i][r] = wordCount(refSentences[i][r]);
        refSentences[i][r] = null;
      }
      refSentences[i] = null;
    }

    refSentences = null;

  }




  public int[] suffStats(String cand_str, int i)
  {
    int[] stats = new int[suffStatsCount];
    stats[0] = 1;

    String[] words = cand_str.split("\\s+");

int wordCount = words.length;
for (int j = 0; j < wordCount; ++j) { words[j] = words[j].intern(); }

    for (int n = 1; n <= maxGramLength; ++n) {
      stats[n] = prec_suffStats(n,words,i);
    }

    stats[maxGramLength+1] = words.length;
    stats[maxGramLength+2] = BP_suffStats(words.length,i);

    return stats;
  }

  public int prec_suffStats(int gramLength, String[] words, int i)
  {
    int correctGramCount = 0;
    String gram = "";
    int candGramCount = 0, maxRefGramCount = 0, clippedCount = 0;

    HashMap candCounts = getNgramCounts(words,gramLength);

    Iterator it = (candCounts.keySet()).iterator();

    while (it.hasNext()) {
    // for each gram type in the candidate
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

    }

    return correctGramCount;

  }

  public int BP_suffStats(int candLength, int i)
  {
    if (effLengthMethod == 1) { // closest

      int closestRefLength = refWordCount[i][0];
      int minDiff = Math.abs(candLength-closestRefLength);

//int minDiff = candLength-closestRefLength;
//if (minDiff < 0) minDiff = -minDiff;

      for (int r = 1; r < refsPerSen; ++r) {
        int nextRefLength = refWordCount[i][r];
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

      return closestRefLength;

    } else if (effLengthMethod == 2) { // shortest

      int shortestRefLength = refWordCount[i][0];

      for (int r = 1; r < refsPerSen; ++r) {
        int nextRefLength = refWordCount[i][r];
        if (nextRefLength < shortestRefLength) {
          shortestRefLength = nextRefLength;
        }
      }

      return shortestRefLength;

    }
/* // commented out because it needs sufficient statistics to be doubles
else { // average

      int totalRefLength = refWordCount[i][0];

      for (int r = 1; r < refsPerSen; ++r) {
        totalRefLength += refWordCount[i][r];
      }

      return totalRefLength/(double)refsPerSen;

    }
*/
    return candLength; // should never get here anyway

  }

  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(2);
    }

    double BLEUsum = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl
    double c_len = stats[suffStatsCount-2];
    double r_len = stats[suffStatsCount-1];
    double numSegments = stats[0];

    double correctGramCount, totalGramCount;

    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[n];
      totalGramCount = c_len-((n-1)*numSegments);

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

      BLEUsum += weights[n] * Math.log(prec_n);

    }

    double BP = 1.0;
    if (c_len < r_len) BP = Math.exp(1-(r_len/c_len));
      // if c_len > r_len, no penalty applies

    return BP*Math.exp(BLEUsum);

  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    double BLEUsum = 0.0;
    double smooth_addition = 1.0; // following bleu-1.04.pl
    double c_len = stats[suffStatsCount-2];
    double r_len = stats[suffStatsCount-1];
    double numSegments = stats[0];

    double correctGramCount, totalGramCount;

    if (oneLiner) {
      System.out.print("Precisions: ");
    }

    for (int n = 1; n <= maxGramLength; ++n) {
      correctGramCount = stats[n];
      totalGramCount = c_len-((n-1)*numSegments);
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

      BLEUsum += weights[n] * Math.log(prec_n);

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


  public HashMap getNgramCounts(String cand_str, int n)
  {
    return getNgramCounts(cand_str.split("\\s+"),n);
  }

  public HashMap getNgramCounts(String[] words, int n)
  {
    HashMap ngramCounts = new HashMap();
    int wordCount = words.length;

    if (wordCount >= n) {
      if (n > 1) { // for n == 1, less processing is needed
        // build the first n-gram
        int start = 0; int end = n-1;
        String gram = "";
        for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
        gram = gram + words[end];
        ngramCounts.put(gram,1);

        for (start = 1; start <= wordCount-n; ++start) {
        // process n-gram starting at start and ending at start+(n-1)

          end = start + (n-1);
          // build the n-gram from words[start] to words[end]

/*
// old way of doing it
          gram = "";
          for (int i = start; i < end; ++i) { gram = gram + words[i] + " "; }
          gram = gram + words[end];
*/

          gram = gram.substring(gram.indexOf(' ')+1) + " " + words[end];

          if (ngramCounts.containsKey(gram)) {
            int oldCount = (Integer)ngramCounts.get(gram);
            ngramCounts.put(gram,oldCount+1);
          } else {
            ngramCounts.put(gram,1);
          }

        } // for (start)

      } else { // if (n == 1)

        String gram = "";
        for (int j = 0; j < wordCount; ++j) {
          gram = words[j];

          if (ngramCounts.containsKey(gram)) {
            int oldCount = (Integer)ngramCounts.get(gram);
            ngramCounts.put(gram,oldCount+1);
          } else {
            ngramCounts.put(gram,1);
          }

        }
      }
    } // if (wordCount >= n)

    return ngramCounts;
  }

  private int wordCount(String cand_str)
  {
    return cand_str.split("\\s+").length;
  }




/*
  // The following two functions are nice to have, I suppose, but they're never
  // used, so they're commented out at the moment for clarity's sake
  public int prec_suffStats(int gramLength, String[] topCand_str)
  {
    int totCount = 0;

    for (int i = 0; i < numSentences; ++i) {
      String[] words = topCand_str[i].split("\\s+");
      totCount += prec_suffStats(gramLength,words,i);
    } // for (i)

    return totCount;
  }

  public int BP_suffStats(String[] topCand_str)
  {
    int totLength = 0;

    for (int i = 0; i < numSentences; ++i) {
      String[] words = topCand_str[i].split("\\s+");
      totLength += BP_suffStats(words.length,i);
    } // for (i)

    return totLength;
  }
*/


}
