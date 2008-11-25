package joshua.util;
import joshua.MERT.*;
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

  static SentenceInfo[][] refSentenceInfo;
    // sentence information for the reference translations
    // refSentenceInfo[i][r] stores the information for the rth reference
    // translation of the ith sentence

  static int maxGramLength;
    // maximum gram length; needed for the SentenceInfo class

  static String metricName;
    // name of evaluation metric

  static EvaluationMetric evalMetric;
    // the evaluation metric

  static boolean evaluateRefs;
    // if true, the reference set(s) is (are) evaluated

  static String refFileName, candFileName;
    // file names for input files.  When refsPerSen > 1, refFileName can be
    // the name of a single file, or a file name prefix.

  public static void main(String[] args) throws Exception
  {
    if (args.length == 0) {
      printUsage(args.length);
      System.exit(0);
    } else {
      processArgsAndInitialize(args);
    }
    // non-specified args will be set to default values in processArgsAndInitialize

    println("Evaluating candidate translations in " + candFileName + "...");
    evaluate(candFileName, 1, 0);
    println("");

    if (evaluateRefs) {
      // evaluate the references themselves; useful if developing a new evaluation metric

      println("PERFORMING SANITY CHECK:");
      println("This metric's scores range from "
            + evalMetric.worstPossibleScore() + " (worst) to "
            + evalMetric.bestPossibleScore() + " (best).");
      println("");

      for (int r = 0; r < refsPerSen; ++r) {
        println("Evaluating reference set " + r + ":");
        evaluate(refFileName, refsPerSen, r);
        println("");
      }
    }

    System.exit(0);

  } // main(String[] args)



  private static void evaluate(String inFileName, int candPerSen, int testIndex) throws Exception
  {
    // test that the translations in inFileName get the expected scores

    // candPerSen: how many candidates are provided per sentence?
    // testIndex: which of the candidates (for each sentence) should be tested?
    //            e.g. testIndex=0 means first candidate should be evaluated
    //                 testIndex=candPerSen-1 means last candidate should be evaluated

    if (candPerSen < 0) {
      println("candPerSen must be positive.");
      System.exit(30);
    }

    if (testIndex < 0 || testIndex > candPerSen-1) {
      println("testIndex must be in [0,candPerSen-1]");
      System.exit(31);
    }

    // read the candidates
    SentenceInfo[] candSentenceInfo = new SentenceInfo[numSentences];

    BufferedReader inFile = new BufferedReader(new FileReader(inFileName));
    
    String candidate_str;

    //TODO The variable line is never used. Perhaps it should be removed?
    @SuppressWarnings("unused")
	String line;
    
    for (int i = 0; i < numSentences; ++i) {

      for (int n = 0; n < testIndex; ++n){
      // skip candidates 0 through testIndex-1
        line = inFile.readLine();
      }

      // read candidate testIndex
      candidate_str = inFile.readLine();

      candSentenceInfo[i] = new SentenceInfo(candidate_str);

      for (int n = testIndex+1; n < candPerSen; ++n){
      // skip candidates testIndex+1 through candPerSen-1
        line = inFile.readLine();
      }

    }

    inFile.close();

    evalMetric.printDetailedScore(candSentenceInfo,false);

    if (verbose) {
      println("Printing detailed scores for individual sentences...");
      for (int i = 0; i < numSentences; ++i) {
        print("Sentence #" + i + ": ");
        evalMetric.printDetailedScore(candSentenceInfo[i],i,true);
          // already prints a \n
      }
    }

  } // void evaluate(...)



  private static void printUsage(int argsLen)
  {
    println("Oops, you provided " + argsLen + " args!");
    println("");
    println("Usage:");
    println(" JoshuaEval [-cand candFile] [-ref refFile] [-rps refsPerSen]\n            [-maxGL maxGramLength] [-m metricName] [-evr evalRefs] \n            [-v verbose]");
    println("");
    println(" (*) -cand candFile: candidate translations\n       [[default: candidates.txt]]");
    println(" (*) -ref refFile: reference translations\n       [[default: references.txt]]");
    println(" (*) -rps refsPerSen: number of reference translations per sentence\n       [[default: 1]]");
    println(" (*) -maxGL maxGramLength: maximum word gram length to collect statistics for\n       [[default: 4]]");
    println(" (*) -m metricName: name of evaluation metric\n       [[default: BLEU]]");
    println(" (*) -evr evalRefs: evaluate references (1) or not (0) (sanity check)\n       [[default: 0]]");
    println(" (*) -v verbose: evaluate individual sentences (1) or not (0)\n       [[default: 0]]");
    println("");
    println("Ex.: java JoshuaEval -cand output.txt -ref refFile -rps 4");
  }


  private static void processArgsAndInitialize(String[] args) throws Exception
  {
    EvaluationMetric.set_knownNames();

    // set default values
    candFileName = "candidates.txt";
    refFileName = "reference.txt";
    refsPerSen = 1;
    maxGramLength = 4;
    metricName = "BLEU";
    evaluateRefs = false;
    verbose = false;

    int i = 0;

    while (i < args.length) {
      String option = args[i];
      if (option.equals("-cand")) { candFileName = args[i+1]; }
      else if (option.equals("-ref")) { refFileName = args[i+1]; }
      else if (option.equals("-rps")) {
        refsPerSen = Integer.parseInt(args[i+1]);
        if (refsPerSen < 1) { println("refsPerSen must be positive."); System.exit(10); }
      }
      else if (option.equals("-maxGL")) {
        maxGramLength = Integer.parseInt(args[i+1]);
        if (maxGramLength < 1) { println("maxGramLength must be positive."); System.exit(10); }
      }
      else if (option.equals("-m")) {
        metricName = args[i+1];
        if (!EvaluationMetric.knownMetricName(metricName)) { println("Unknown metric name " + metricName + "."); System.exit(10); }
      }
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
    numSentences = countLines(candFileName);

    if (numSentences * refsPerSen != countLines(refFileName)) {
      println("Line count mismatch between " + candFileName + " and " + refFileName);
      System.exit(20);
    }

//    SentenceInfo.setNumParams(1); // dummy value for numParams

    // read in reference sentences
    refSentenceInfo = new SentenceInfo[numSentences][refsPerSen];
    BufferedReader inFile_refs = new BufferedReader(new FileReader(refFileName));
    String line;

    for (i = 0; i < numSentences; ++i) {
      for (int r = 0; r < refsPerSen; ++r) {
        // read the rth reference translation for the ith sentence
        line = inFile_refs.readLine();
        refSentenceInfo[i][r] = new SentenceInfo(line);
      }
    }

    inFile_refs.close();


    // set static data members for the EvaluationMetric class
    EvaluationMetric.set_numSentences(numSentences);
    EvaluationMetric.set_refsPerSen(refsPerSen);
    EvaluationMetric.set_refSentenceInfo(refSentenceInfo);

    // do necessary initialization for the evaluation metric
    if (metricName.equals("BLEU")) {
      evalMetric = new BLEU(maxGramLength,"closest");
    } else if (metricName.equals("01LOSS")) {
      evalMetric = new ZeroOneLoss();
    }

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

// TODO This method is never used - perhaps it should be removed
//  private static void showProgress()
//  {
//    ++progress;
//    if (progress % 1000 == 0) print(".");
//  }

}
