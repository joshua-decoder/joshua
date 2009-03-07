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
import java.math.*;
import java.util.*;
import java.io.*;

public class TER extends EvaluationMetric
{
  private boolean caseSensitive;
  private boolean withPunctuation;
  private int beamWidth;
  private int maxShiftDist;

  public TER(String[] Metric_options)
  {
    // M_o[0]: case sensitivity, case/nocase
    // M_o[1]: with-punctuation, punc/nopunc
    // M_o[2]: beam width, positive integer
    // M_o[3]: maximum shift distance, positive integer

    // default in tercom-0.7.25: nocase, punc, 20, 50

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
      System.exit(2);
    }

    maxShiftDist = Integer.parseInt(Metric_options[3]);
    if (maxShiftDist < 1) {
      System.out.println("Maximum shift distance must be positive");
      System.exit(2);
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

  public int[] suffStats(String cand_str, int i) throws Exception
  {
    int[] stats = new int[suffStatsCount];

    // 1) Create input files for tercom.7.25.jar

    // 1a) Create hypothesis file
    FileOutputStream outStream = new FileOutputStream("hyp.txt.TER", false); // false: don't append
    OutputStreamWriter outStreamWriter = new OutputStreamWriter(outStream, "utf8");
    BufferedWriter outFile = new BufferedWriter(outStreamWriter);

    writeLine(cand_str + " (ID)",outFile);

    outFile.close();

    // 1b) Create reference file
    outStream = new FileOutputStream("ref.txt.TER", false); // false: don't append
    outStreamWriter = new OutputStreamWriter(outStream, "utf8");
    outFile = new BufferedWriter(outStreamWriter);

    for (int r = 0; r < refsPerSen; ++r) {
      writeLine(refSentences[i][r] + " (ID)",outFile);
    }

    outFile.close();


    // 2) Launch tercom.7.25.jar as an external process

    String cmd_str = "java -jar tercom.7.25.jar -r ref.txt.TER -h hyp.txt.TER -o ter -n TER_out";
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

    int exitValue = p.waitFor();


    // 3) Read SS from output file produced by tercom.7.25.jar

    BufferedReader inFile = new BufferedReader(new FileReader("TER_out.ter"));
    String line = "";

    line = inFile.readLine(); // skip hyp line
    line = inFile.readLine(); // skip ref line

    line = inFile.readLine(); // read info
    String[] strA = line.split("\\s+");

    stats[0] = (int)Double.parseDouble(strA[1]);
    stats[1] = (int)Double.parseDouble(strA[2]);

    return stats;
  }

  public double score(int[] stats)
  {
    if (stats.length != suffStatsCount) {
      System.out.println("Mismatch between stats.length and suffStatsCount (" + stats.length + " vs. " + suffStatsCount + ")");
      System.exit(1);
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

