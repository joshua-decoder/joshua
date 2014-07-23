package joshua.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class PrecisMinusSourceBLEU extends EvaluationMetric {

  private Precis myPrecis;
  private SourceBLEU mySourceBLEU;

  private double bleuWeight;

  private int precisCount;
  private int sourceBleuCount;

  public PrecisMinusSourceBLEU(String[] options) {
    // Automatically deactivate Levenshtein penalty for Precis.
    bleuWeight = Double.parseDouble(options[5]);
    options[5] = "0";

    myPrecis = new Precis(options);
    mySourceBLEU =
        new SourceBLEU(Integer.parseInt(options[0]), options[1], Integer.parseInt(options[2]),
            false);

    initialize();
  }

  protected void initialize() {
    metricName = "PRECIS-SRC_BLEU";
    toBeMinimized = false;
    precisCount = myPrecis.suffStatsCount;
    sourceBleuCount = mySourceBLEU.suffStatsCount;
    suffStatsCount = precisCount + sourceBleuCount;
  }

  public double bestPossibleScore() {
    return 1.0;
  }

  public double worstPossibleScore() {
    return -1.0;
  }

  public int[] suffStats(String cand_str, int i) {
    return null;
  }

  public int[][] suffStats(String[] cand_strings, int[] cand_indices) {
    int candCount = cand_strings.length;
    if (cand_indices.length != candCount) {
      System.out.println("Array lengths mismatch in suffStats(String[],int[]); returning null.");
      return null;
    }

    int[][] stats = new int[candCount][suffStatsCount];

    int[][] precis_stats = myPrecis.suffStats(cand_strings, cand_indices);
    int[][] source_bleu_stats = mySourceBLEU.suffStats(cand_strings, cand_indices);

    for (int d = 0; d < candCount; ++d) {
      int s = 0;
      for (int s_T = 0; s_T < precisCount; s_T++) {
        stats[d][s] = precis_stats[d][s_T];
        ++s;
      }
      for (int s_B = 0; s_B < sourceBleuCount; s_B++) {
        stats[d][s] = source_bleu_stats[d][s_B];
        ++s;
      }
    }
    return stats;
  }

  public void createSuffStatsFile(String cand_strings_fileName, String cand_indices_fileName,
      String outputFileName, int maxBatchSize) {
    try {
      myPrecis.createSuffStatsFile(cand_strings_fileName, cand_indices_fileName, outputFileName
          + ".PRECIS", maxBatchSize);
      mySourceBLEU.createSuffStatsFile(cand_strings_fileName, cand_indices_fileName, outputFileName
          + ".SRC_BLEU", maxBatchSize);

      PrintWriter outFile = new PrintWriter(outputFileName);

      FileInputStream inStream_Precis = new FileInputStream(outputFileName + ".PRECIS");
      BufferedReader inFile_Precis =
          new BufferedReader(new InputStreamReader(inStream_Precis, "utf8"));

      FileInputStream inStream_SourceBLEU = new FileInputStream(outputFileName + ".SRC_BLEU");
      BufferedReader inFile_SourceBLEU =
          new BufferedReader(new InputStreamReader(inStream_SourceBLEU, "utf8"));

      String line_Precis = inFile_Precis.readLine();
      String line_SourceBLEU = inFile_SourceBLEU.readLine();

      // combine the two files into one
      while (line_Precis != null) {
        outFile.println(line_Precis + " " + line_SourceBLEU);
        line_Precis = inFile_Precis.readLine();
        line_SourceBLEU = inFile_SourceBLEU.readLine();
      }

      inFile_Precis.close();
      inFile_SourceBLEU.close();
      outFile.close();

      File fd;
      fd = new File(outputFileName + ".PRECIS");
      if (fd.exists()) fd.delete();
      fd = new File(outputFileName + ".SRC_BLEU");
      if (fd.exists()) fd.delete();
    } catch (IOException e) {
      System.err.println("IOException: " + e.getMessage());
      System.exit(99902);
    }
  }

  public double score(int[] stats) {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length
          + " vs. " + suffStatsCount + ") in PrecisMinusSourceBLEU.score(int[])");
      System.exit(1);
    }

    double sc = 0.0;

    int[] stats_Precis = new int[precisCount];
    int[] stats_SourceBLEU = new int[sourceBleuCount];
    for (int s = 0; s < precisCount; ++s) {
      stats_Precis[s] = stats[s];
    }
    for (int s = 0; s < sourceBleuCount; ++s) {
      stats_SourceBLEU[s] = stats[s + precisCount];
    }

    double sc_T = myPrecis.score(stats_Precis);
    double sc_B = mySourceBLEU.score(stats_SourceBLEU);

    sc = sc_T - (bleuWeight * sc_B);

    return sc;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner) {
    int[] stats_Precis = new int[precisCount];
    int[] stats_SourceBLEU = new int[sourceBleuCount];
    for (int s = 0; s < precisCount; ++s) {
      stats_Precis[s] = stats[s];
    }
    for (int s = 0; s < sourceBleuCount; ++s) {
      stats_SourceBLEU[s] = stats[s + precisCount];
    }

    System.out.println("---PRECIS---");
    myPrecis.printDetailedScore_fromStats(stats_Precis, oneLiner);
    System.out.println("---SRC_BLEU---");
    mySourceBLEU.printDetailedScore_fromStats(stats_SourceBLEU, oneLiner);
    System.out.println("---------");
    System.out.println("  => " + metricName + " = " + f4.format(score(stats)));
  }

}
