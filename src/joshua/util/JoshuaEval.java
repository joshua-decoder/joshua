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

package joshua.util;
import joshua.ZMERT.*;
import java.math.*;
import java.util.*;
import java.io.*;
import java.text.DecimalFormat;

public class JoshuaEval
{
  static DecimalFormat f4 = new DecimalFormat("###0.0000");

  static int progress;

  static boolean verbose;
    // if true, evaluation is performed for each candidate translation as
    // well as on the entire candidate set

  static int numSentences;
    // number of candidate translations

  static int refsPerSen;
    // number of reference translations per sentence

  static String[][] refSentences;
    // refSentences[i][r] is the rth reference translation of the ith sentence

  static String metricName;
    // name of evaluation metric

  static String[] metricOptions;
    // options for the evaluation metric (e.g. for BLEU, maxGramLength and effLengthMethod)

  static EvaluationMetric evalMetric;
    // the evaluation metric

  static boolean evaluateRefs;
    // if true, the reference set(s) is (are) evaluated

  static String refFileName, candFileName;
    // file names for input files.  When refsPerSen > 1, refFileName can be
    // the name of a single file, or a file name prefix.

  static String candFileFormat;
    // format of the candidate file: "plain" if one candidate per sentence, and "nbest" if a decoder output

  static int candRank;
    // if format is nbest, evaluate the r'th candidate of each sentence

  public static void main(String[] args) throws Exception
  {
    if (args.length == 0) {
      printUsage(args.length);
      System.exit(0);
    } else {
      processArgsAndInitialize(args);
    }
    // non-specified args will be set to default values in processArgsAndInitialize

    if (candFileFormat.equals("plain")) {
      println("Evaluating candidate translations in plain file " + candFileName + "...");
      evaluateCands_plain(candFileName);
    } else if (candFileFormat.equals("nbest")) {
      println("Evaluating set of " + candRank + "'th candidate translations from " + candFileName + "...");
      evaluateCands_nbest(candFileName,candRank);
    }
    println("");

    if (evaluateRefs) {
      // evaluate the references themselves; useful if developing a new evaluation metric

      println("");
      println("PERFORMING SANITY CHECK:");
      println("------------------------");
      println("");
      println("This metric's scores range from "
            + evalMetric.worstPossibleScore() + " (worst) to "
            + evalMetric.bestPossibleScore() + " (best).");

      for (int r = 1; r <= refsPerSen; ++r) {
        println("");
        println("(*) Evaluating reference set " + r + ":");
        println("");
        evaluateRefSet(r);
        println("");
      }
    }

    System.exit(0);

  } // main(String[] args)

  private static void evaluateCands_plain(String inFileName) throws Exception
  {
    evaluate(candFileName, "plain", 1, 1);
  }

  private static void evaluateCands_nbest(String inFileName, int testIndex) throws Exception
  {
    evaluate(candFileName, "nbest", -1, testIndex);
  }

  private static void evaluateRefSet(int r) throws Exception
  {
    evaluate(refFileName, "plain", refsPerSen, r);
  }

  private static void evaluate(String inFileName, String inFileFormat, int candPerSen, int testIndex) throws Exception
  {
    // candPerSen: how many candidates are provided per sentence?
    //             (if inFileFormat is nbest, then candPerSen is ignored, since it is variable)
    // testIndex: which of the candidates (for each sentence) should be tested?
    //            e.g. testIndex=1 means first candidate should be evaluated
    //                 testIndex=candPerSen means last candidate should be evaluated

    if (inFileFormat.equals("plain") && candPerSen < 1) {
      println("candPerSen must be positive for a file in plain format.");
      System.exit(30);
    }

    if (inFileFormat.equals("plain") && (testIndex < 1 || testIndex > candPerSen)) {
      println("For the plain format, testIndex must be in [1,candPerSen]");
      System.exit(31);
    }

    // read the candidates
    String[] topCand_str = new String[numSentences];

    BufferedReader inFile = new BufferedReader(new FileReader(inFileName));
    String line, candidate_str;

    if (inFileFormat.equals("plain")) {

      for (int i = 0; i < numSentences; ++i) {

        // skip candidates 1 through testIndex-1
        for (int n = 1; n < testIndex; ++n){
          line = inFile.readLine();
        }

        // read testIndex'th candidate
        candidate_str = inFile.readLine();

        topCand_str[i] = candidate_str;

        for (int n = testIndex+1; n <= candPerSen; ++n){
        // skip candidates testIndex+1 through candPerSen-1
          line = inFile.readLine();
        }

      } // for (i)

    } else { // nbest format

      int i = 0; int n = 1;
      line = inFile.readLine();

      while (line != null && i < numSentences) {

/*
line format:

.* ||| words of candidate translation . ||| feat-1_val feat-2_val ... feat-numParams_val .*

*/

        while (n < candRank) {
          line = inFile.readLine();
          ++n;
        }

        // at the moment, line stores the candRank'th candidate (1-indexed) of the i'th sentence (0-indexed)

        if (line == null) {
          println("Not enough candidates in " + inFileName + " to extract the " + candRank + "'th candidate for each sentence.");
          println("(Failed to extract one for the " + i + "'th sentence (0-indexed).)");
          System.exit(32);
        }

        int read_i = Integer.parseInt(line.substring(0,line.indexOf(" |||")));
        if (read_i == i) {
          line = line.substring(line.indexOf("||| ")+4); // get rid of initial text
          candidate_str = line.substring(0,line.indexOf(" |||"));
          topCand_str[i] = candidate_str;
          if (i < numSentences-1) {
            while (read_i == i) {
              line = inFile.readLine();
              read_i = Integer.parseInt(line.substring(0,line.indexOf(" |||")));
            }
          }
          n = 1;
          i += 1;
        } else {
          println("Not enough candidates in " + inFileName + " to extract the " + candRank + "'th candidate for each sentence.");
          println("(Failed to extract one for the " + i + "'th sentence (0-indexed).)");
          System.exit(32);
        }

      } // while (line != null)

      if (i != numSentences) {
        println("Not enough candidates were found (i = " + i + "; was expecting " + numSentences + ")");
        System.exit(33);
      }

    }

    inFile.close();

    evalMetric.printDetailedScore(topCand_str,false);

    if (verbose) {
      println("");
      println("Printing detailed scores for individual sentences...");
      for (int i = 0; i < numSentences; ++i) {
        print("Sentence #" + i + ": ");
        evalMetric.printDetailedScore(topCand_str[i],i,true);
          // already prints a \n
      }
    }

  } // void evaluate(...)



  private static void printUsage(int argsLen)
  {
    println("Oops, you provided " + argsLen + " args!");
    println("");
    println("Usage:");
    println(" JoshuaEval [-cand candFile] [-format candFileformat] [-rank r]\n            [-ref refFile] [-rps refsPerSen] [-m metricName metric options]\n            [-evr evalRefs] [-v verbose]");
    println("");
    println(" (*) -cand candFile: candidate translations\n       [[default: candidates.txt]]");
    println(" (*) -format candFileFormat: is the candidate file a plain file (one candidate\n       per sentence) or does it contain multiple candidates per sentence as\n       a decoder's output)?  For the first, use \"plain\".  For the second,\n       use \"nbest\".\n       [[default: plain]]");
    println(" (*) -rank r: if format=nbest, evaluate the set of r'th candidates.\n       [[default: 1]]");
    println(" (*) -ref refFile: reference translations (or file name prefix)\n       [[default: references.txt]]");
    println(" (*) -rps refsPerSen: number of reference translations per sentence\n       [[default: 1]]");
    println(" (*) -m metricName metric options: name of evaluation metric and its options\n       [[default: BLEU 4 closest]]");
    println(" (*) -evr evalRefs: evaluate references (1) or not (0) (sanity check)\n       [[default: 0]]");
    println(" (*) -v verbose: evaluate individual sentences (1) or not (0)\n       [[default: 0]]");
    println("");
    println("Ex.: java JoshuaEval -cand nbest.out -ref ref.all -rps 4 -m BLEU 4 shortest");
  }


  private static void processArgsAndInitialize(String[] args) throws Exception
  {
    EvaluationMetric.set_knownMetrics();

    // set default values
    candFileName = "candidates.txt";
    candFileFormat = "plain";
    candRank = 1;
    refFileName = "references.txt";
    refsPerSen = 1;
    metricName = "BLEU";
    metricOptions = new String[2];
    metricOptions[0] = "4";
    metricOptions[1] = "closest";
    evaluateRefs = false;
    verbose = false;

    int i = 0;

    while (i < args.length) {
      String option = args[i];
      if (option.equals("-cand")) { candFileName = args[i+1]; }
      else if (option.equals("-format")) {
        candFileFormat = args[i+1];
        if (!candFileFormat.equals("plain") && !candFileFormat.equals("nbest")) { println("candFileFormat must be either plain or nbest."); System.exit(10); }
      }
      else if (option.equals("-rank")) {
        candRank = Integer.parseInt(args[i+1]);
        if (refsPerSen < 1) { println("Argument for -rank must be positive."); System.exit(10); }
      }
      else if (option.equals("-ref")) { refFileName = args[i+1]; }
      else if (option.equals("-rps")) {
        refsPerSen = Integer.parseInt(args[i+1]);
        if (refsPerSen < 1) { println("refsPerSen must be positive."); System.exit(10); }
      }

      else if (option.equals("-m")) {
        metricName = args[i+1];
        if (EvaluationMetric.knownMetricName(metricName)) {
          int optionCount = EvaluationMetric.metricOptionCount(metricName);
          metricOptions = new String[optionCount];
          for (int opt = 0; opt < optionCount; ++opt) { metricOptions[opt] = args[i+opt+2]; }
          i += optionCount;
        } else {
          println("Unknown metric name " + metricName + "."); System.exit(10);
        }
      }

/*
      else if (option.equals("-m")) {
        metricName = args[i+1];
        if (!EvaluationMetric.knownMetricName(metricName)) { println("Unknown metric name " + metricName + "."); System.exit(10); }
        if (metricName.equals("BLEU")) {
          metricOptions = new String[2];
          metricOptions[0] = args[i+2];
          metricOptions[1] = args[i+3];
          i += 2;
        }
      }
*/
      else if (option.equals("-evr")) {
        int evr = Integer.parseInt(args[i+1]);
        if (evr == 1) evaluateRefs = true;
        else if (evr == 0) evaluateRefs = false;
        else { println("evalRefs must be either 0 or 1."); System.exit(10); }
      }
      else if (option.equals("-v")) {
        int v = Integer.parseInt(args[i+1]);
        if (v == 1) verbose = true;
        else if (v == 0) verbose = false;
        else { println("verbose must be either 0 or 1."); System.exit(10); }
      }
      else {
        println("Unknown option " + option); System.exit(10);
      }

      i += 2;

    } // while (i)

    if (refsPerSen > 1) {
      // the provided refFileName might be a prefix
      File dummy = new File(refFileName);
      if (!dummy.exists()) {
        refFileName = createUnifiedRefFile(refFileName,refsPerSen);
      }
    } else {
      checkFile(refFileName);
    }


    // initialize
    numSentences = countLines(refFileName) / refsPerSen;

    // read in reference sentences
    refSentences = new String[numSentences][refsPerSen];
    BufferedReader inFile_refs = new BufferedReader(new FileReader(refFileName));
    String line;

    for (i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        // read the rth reference translation for the ith sentence
        refSentences[i][r] = inFile_refs.readLine();
      }
    }

    inFile_refs.close();


    // set static data members for the EvaluationMetric class
    EvaluationMetric.set_numSentences(numSentences);
    EvaluationMetric.set_refsPerSen(refsPerSen);
    EvaluationMetric.set_refSentences(refSentences);

    // do necessary initialization for the evaluation metric
    evalMetric = EvaluationMetric.getMetric(metricName,metricOptions);

    println("Processing " + numSentences + " sentences...");

  } // processArgsAndInitialize(String[] args)

  private static void checkFile(String fileName)
  {
    if (!fileExists(fileName)) {
      println("The file " + fileName + " was not found!");
      System.exit(40);
    }
  }

  private static boolean fileExists(String fileName)
  {
    File checker = new File(fileName);
    return checker.exists();
  }

  private static String createUnifiedRefFile(String prefix, int numFiles) throws Exception
  {
    if (numFiles < 2) {
      println("Warning: createUnifiedRefFile called with numFiles = " + numFiles + "; doing nothing.");
      return prefix;
    } else {
      File checker;
      checker = new File(prefix+"1");

      if (!checker.exists()) {
        checker = new File(prefix+".1");
        if (!checker.exists()) {
          println("Can't find reference files.");
          System.exit(50);
        } else {
          prefix = prefix + ".";
        }
      }

      String outFileName;
      if (prefix.endsWith(".")) { outFileName = prefix+"all"; }
      else { outFileName = prefix+".all"; }

      PrintWriter outFile = new PrintWriter(outFileName);

      BufferedReader[] inFile = new BufferedReader[numFiles];

      int nextIndex;
      checker = new File(prefix+"0");
      if (checker.exists()) { nextIndex = 0; }
      else { nextIndex = 1; }
      int lineCount = countLines(prefix+nextIndex);

      for (int r = 0; r < numFiles; ++r) {
        if (countLines(prefix+nextIndex) != lineCount) {
          println("Line count mismatch in " + (prefix+nextIndex) + ".");
          System.exit(60);
        }
        inFile[r] = new BufferedReader(new FileReader(prefix+nextIndex));
        ++nextIndex;
      }

      String line;

      for (int i = 0; i < lineCount; ++i) {
        for (int r = 0; r < numFiles; ++r) {
          line = inFile[r].readLine();
          outFile.println(line);
        }
      }

      outFile.close();

      for (int r = 0; r < numFiles; ++r) { inFile[r].close(); }

      return outFileName;

    }

  } // createUnifiedRefFile(String prefix, int numFiles)

  private static int countLines(String fileName) throws Exception
  {
    BufferedReader inFile = new BufferedReader(new FileReader(fileName));

    String line;
    int count = 0;
    do {
      line = inFile.readLine();
      if (line != null) ++count;
    }  while (line != null);

    inFile.close();

    return count;
  }

  private static void println(Object obj) { System.out.println(obj); }
  private static void print(Object obj) { System.out.print(obj); }

}
