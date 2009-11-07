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

public class TER extends EvaluationMetric
{
  private boolean caseSensitive;
  private boolean withPunctuation;
  private int beamWidth;
  private int maxShiftDist;
  private String tercomJarFileName;

  public TER(String[] Metric_options)
  {
    // M_o[0]: case sensitivity, case/nocase
    // M_o[1]: with-punctuation, punc/nopunc
    // M_o[2]: beam width, positive integer
    // M_o[3]: maximum shift distance, positive integer
    // M_o[4]: filename of tercom jar file

    // for 0-3, default values in tercom-0.7.25 are: nocase, punc, 20, 50

    if (Metric_options[0].equals("case")) {
      caseSensitive = true;
    } else if (Metric_options[0].equals("nocase")) {
      caseSensitive = false;
    } else {
      System.out.println("Unknown case sensitivity string " + Metric_options[0] + ".");
      System.out.println("Should be one of case or nocase.");
      System.exit(1);
    }

    if (Metric_options[1].equals("punc")) {
      withPunctuation = true;
    } else if (Metric_options[1].equals("nopunc")) {
      withPunctuation = false;
    } else {
      System.out.println("Unknown with-punctuation string " + Metric_options[1] + ".");
      System.out.println("Should be one of punc or nopunc.");
      System.exit(1);
    }

    beamWidth = Integer.parseInt(Metric_options[2]);
    if (beamWidth < 1) {
      System.out.println("Beam width must be positive");
      System.exit(1);
    }

    maxShiftDist = Integer.parseInt(Metric_options[3]);
    if (maxShiftDist < 1) {
      System.out.println("Maximum shift distance must be positive");
      System.exit(1);
    }

    tercomJarFileName = Metric_options[4];

    if (tercomJarFileName == null || tercomJarFileName.equals("")) {
      System.out.println("Problem processing tercom's jar filename");
      System.exit(1);
    } else {
      File checker = new File(tercomJarFileName);
      if (!checker.exists()) {
        System.out.println("Could not find tercom jar file " + tercomJarFileName);
        System.out.println("(Please make sure you use the full path in the filename)");
        System.exit(1);
      }
    }

    initialize(); // set the data members of the metric
  }

  protected void initialize()
  {
    metricName = "TER";
    toBeMinimized = true;
    suffStatsCount = 2;
  }

  public double bestPossibleScore() { return 0.0; }
  public double worstPossibleScore() { return (+1.0 / 0.0); }

  public int[] suffStats(String cand_str, int i)
  {
    // this method should never be used when the metric is TER,
    // because TER.java overrides suffStats(String[],int[]) below,
    // which is the only method that calls suffStats(String,int).
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

      // 1) Create input files for tercom

      // 1a) Create hypothesis file
      FileOutputStream outStream = new FileOutputStream("hyp.txt.TER", false); // false: don't append
      OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      BufferedWriter outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        writeLine(cand_strings[d] + " (ID" + d + ")",outFile);
      }

      outFile.close();

      // 1b) Create reference file
      outStream = new FileOutputStream("ref.txt.TER", false); // false: don't append
      outStreamWriter = new OutputStreamWriter(outStream, "utf8");
      outFile = new BufferedWriter(outStreamWriter);

      for (int d = 0; d < candCount; ++d) {
        for (int r = 0; r < refsPerSen; ++r) {
          writeLine(refSentences[cand_indices[d]][r] + " (ID" + d + ")",outFile);
        }
      }

      outFile.close();

      // 2) Launch tercom as an external process

      String cmd_str = "java -Dfile.encoding=utf8 -jar " + tercomJarFileName + " -r ref.txt.TER -h hyp.txt.TER -o ter -n TER_out";
      cmd_str += " -b " + beamWidth;
      cmd_str += " -d " + maxShiftDist;
      if (caseSensitive) { cmd_str += " -S"; }
      if (withPunctuation) { cmd_str += " -P"; }

      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd_str);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 0);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 0);

      errorGobbler.start();
      outputGobbler.start();

      @SuppressWarnings("unused")
      int exitValue = p.waitFor();


      // 3) Read SS from output file produced by tercom.7.25.jar

      BufferedReader inFile = new BufferedReader(new FileReader("TER_out.ter"));
      String line = "";

      line = inFile.readLine(); // skip hyp line
      line = inFile.readLine(); // skip ref line

      for (int d = 0; d < candCount; ++d) {
        line = inFile.readLine(); // read info
        String[] strA = line.split("\\s+");

        stats[d][0] = (int)Double.parseDouble(strA[1]);
        stats[d][1] = (int)Double.parseDouble(strA[2]);
      }


      // 4) Delete TER files

      File fd;
      fd = new File("hyp.txt.TER"); if (fd.exists()) fd.delete();
      fd = new File("ref.txt.TER"); if (fd.exists()) fd.delete();
      fd = new File("TER_out.ter"); if (fd.exists()) fd.delete();

    } catch (IOException e) {
      System.err.println("IOException in TER.suffStats(String[],int[]): " + e.getMessage());
      System.exit(99902);
    } catch (InterruptedException e) {
      System.err.println("InterruptedException in TER.suffStats(String[],int[]): " + e.getMessage());
      System.exit(99903);
    }

    return stats;
  }

  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ") in TER.score(int[])");
      System.exit(2);
    }

    double sc = 0.0;

    sc = stats[0]/(double)stats[1];

    return sc;
  }

  public void printDetailedScore_fromStats(int[] stats, boolean oneLiner)
  {
    if (oneLiner) {
      System.out.println("TER = " + stats[0] + " / " + stats[1] + " = " + score(stats));
    } else {
      System.out.println("# edits = " + stats[0]);
      System.out.println("Reference length = " + stats[1]);
      System.out.println("TER = " + stats[0] + " / " + stats[1] + " = " + score(stats));
    }
  }

  private void writeLine(String line, BufferedWriter writer) throws IOException
  {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

}

