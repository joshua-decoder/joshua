/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package joshua.zmert;
import java.io.*;

public class METEOR extends EvaluationMetric
{
  protected String targetLanguage;
  protected boolean normalize;
  protected boolean keepPunctuation;
  private int maxComputations;

  public METEOR(String[] Metric_options)
  {
    // M_o[0]: -l language, one of {en,cz,fr,de,es}
    // M_o[1]: -normalize, one of {norm_yes,norm_no}
    // M_o[2]: -keepPunctuation, one of {keepPunc,removePunc}
    // M_o[3]: maxComputations, positive integer

    // default in meteor v0.8: en, norm_no, removePunc

    if (Metric_options[0].equals("en")) {
      targetLanguage = "en";
    } else if (Metric_options[0].equals("cz")) {
      targetLanguage = "cz";
    } else if (Metric_options[0].equals("fr")) {
      targetLanguage = "fr";
    } else if (Metric_options[0].equals("de")) {
      targetLanguage = "de";
    } else if (Metric_options[0].equals("es")) {
      targetLanguage = "es";
    } else {
      System.out.println("Unknown language string " + Metric_options[0] + ".");
      System.out.println("Should be one of {en,cz,fr,de,es}.");
      System.exit(1);
    }

    if (Metric_options[1].equals("norm_yes")) {
      normalize = true;
    } else if (Metric_options[1].equals("norm_no")) {
      normalize = false;
    } else {
      System.out.println("Unknown normalize string " + Metric_options[1] + ".");
      System.out.println("Should be one of norm_yes or norm_no.");
      System.exit(1);
    }

    if (Metric_options[2].equals("keepPunc")) {
      keepPunctuation = true;
    } else if (Metric_options[1].equals("removePunk")) {
      keepPunctuation = false;
    } else {
      System.out.println("Unknown keepPunctuation string " + Metric_options[1] + ".");
      System.out.println("Should be one of keepPunc or removePunk.");
      System.exit(1);
    }

    maxComputations = Integer.parseInt(Metric_options[3]);
    if (maxComputations < 1) {
      System.out.println("Maximum computations must be positive");
      System.exit(2);
    }

    initialize(); // set the data members of the metric
  }

  protected void initialize()
  {
    metricName = "METEOR";
    toBeMinimized = false;
    suffStatsCount = 5;
  }

  public double bestPossibleScore() { return 1.0; }
  public double worstPossibleScore() { return 0.0; }

  public int[] suffStats(String cand_str, int i)
  {
    // this method should never be used when the metric is METEOR,
    // because METEOR.java overrides suffStats(String[],int[]) below,
    // which is the only method that calls suffStats(Sting,int).
    return null;
  }

  public int[][] suffStats(String[] cand_strings, int[] cand_indices)
  {
    // calculate sufficient statistics for each sentence in an arbitrary set of candidates

    int candCount = cand_strings.length;
    if (cand_indices.length != candCount) {
      System.out.println("Array lengths mismatch in suffStats(String[],int[]); returning null.");
      return null;
    }

    int[][] stats = new int[candCount][suffStatsCount];

    try {

      // 1) Create input files for meteor

      // 1a) Create hypothesis file
      FileOutputStream outStream = new FileOutputStream("hyp.txt.METEOR", false); // false: don't append
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        writeLine(cand_strings[d],outFile);
      }

      outFile.close();

      // 1b) Create reference file
      outStream = new FileOutputStream("ref.txt.METEOR", false); // false: don't append
      outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        for (int r = 0; r < refsPerSen; ++r) {
          writeLine(refSentences[cand_indices[d]][r],outFile);
        }
      }

      outFile.close();

      // 2) Launch meteor as an external process

      String cmd_str = "./meteor hyp.txt.METEOR ref.txt.METEOR";
      cmd_str += " -l " + targetLanguage;
      cmd_str += " -r " + refsPerSen;
      if (normalize) { cmd_str += " -normalize"; }
      if (keepPunctuation) { cmd_str += " -keepPunctuation"; }
      cmd_str += " -ssOut";

      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd_str);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 0);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 0);

      errorGobbler.start();
      outputGobbler.start();

      @SuppressWarnings("unused")
      int exitValue = p.waitFor();


      // 3) Read SS from output file produced by meteor

      BufferedReader inFile = new BufferedReader(new FileReader("TER_out.ter"));
      String line = "";

      line = inFile.readLine(); // skip hyp line
      line = inFile.readLine(); // skip ref line

      for (int d = 0; d < candCount; ++d) {
        line = inFile.readLine(); // read info
        String[] strA = line.split("\\s+");

        stats[d][0] = (int)Double.parseDouble(strA[0]);
        stats[d][1] = (int)Double.parseDouble(strA[1]);
        stats[d][2] = (int)Double.parseDouble(strA[2]);
        stats[d][3] = (int)Double.parseDouble(strA[3]);
        stats[d][4] = (int)Double.parseDouble(strA[4]);
      }
    } catch (IOException e) {
      System.err.println("IOException in METEOR.suffStats(String[],int[]): " + e.getMessage());
      System.exit(99902);
    } catch (InterruptedException e) {
      System.err.println("InterruptedException in METEOR.suffStats(String[],int[]): " + e.getMessage());
      System.exit(99903);
    }

    return stats;
  }

  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ") in METEOR.score(int[])");
      System.exit(1);
    }

    double sc = 0.0;

    // sc = ???

    return sc;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    if (oneLiner) {
      System.out.println("METEOR = METEOR(" + stats[0] + "," + stats[1] + "," + stats[2] + "," + stats[3] + "," + stats[4] + " = " + score(stats));
    } else {
      System.out.println("# matches = " + stats[0]);
      System.out.println("test length = " + stats[1]);
      System.out.println("ref length = " + stats[2]);
      System.out.println("# chunks = " + stats[3]);
      System.out.println("length cost = " + stats[4]);
      System.out.println("METEOR = " + score(stats));
    }
  }

  private void writeLine(String line, BufferedWriter writer) throws IOException
  {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

}

