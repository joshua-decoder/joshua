package joshua.pro;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import joshua.decoder.Decoder;
import joshua.decoder.JoshuaConfiguration;
import joshua.metrics.EvaluationMetric;
import joshua.util.StreamGobbler;
import joshua.corpus.Vocabulary;

/**
 * This code was originally written by Yuan Cao, who copied the MERT code to produce this file.
 */

public class PROCore {
  private final JoshuaConfiguration joshuaConfiguration;
  private TreeSet<Integer>[] indicesOfInterest_all;

  private final static DecimalFormat f4 = new DecimalFormat("###0.0000");
  private final Runtime myRuntime = Runtime.getRuntime();

  private final static double NegInf = (-1.0 / 0.0);
  private final static double PosInf = (+1.0 / 0.0);
  private final static double epsilon = 1.0 / 1000000;

  private int progress;

  private int verbosity; // anything of priority <= verbosity will be printed
                         // (lower value for priority means more important)

  private Random randGen;
  private int generatedRands;

  private int numSentences;
  // number of sentences in the dev set
  // (aka the "MERT training" set)

  private int numDocuments;
  // number of documents in the dev set
  // this should be 1, unless doing doc-level optimization

  private int[] docOfSentence;
  // docOfSentence[i] stores which document contains the i'th sentence.
  // docOfSentence is 0-indexed, as are the documents (i.e. first doc is indexed 0)

  private int[] docSubsetInfo;
  // stores information regarding which subset of the documents are evaluated
  // [0]: method (0-6)
  // [1]: first (1-indexed)
  // [2]: last (1-indexed)
  // [3]: size
  // [4]: center
  // [5]: arg1
  // [6]: arg2
  // [1-6] are 0 for method 0, [6] is 0 for methods 1-4 as well
  // only [1] and [2] are needed for optimization. The rest are only needed for an output message.

  private int refsPerSen;
  // number of reference translations per sentence

  private int textNormMethod;
  // 0: no normalization, 1: "NIST-style" tokenization, and also rejoin 'm, 're, *'s, 've, 'll, 'd,
  // and n't,
  // 2: apply 1 and also rejoin dashes between letters, 3: apply 1 and also drop non-ASCII
  // characters
  // 4: apply 1+2+3

  private int numParams;
  // total number of firing features
  // this number may increase overtime as new n-best lists are decoded
  // initially it is equal to the # of params in the parameter config file
  private int numParamsOld;
  // number of features before observing the new features fired in the current iteration

  private double[] normalizationOptions;
  // How should a lambda[] vector be normalized (before decoding)?
  // nO[0] = 0: no normalization
  // nO[0] = 1: scale so that parameter nO[2] has absolute value nO[1]
  // nO[0] = 2: scale so that the maximum absolute value is nO[1]
  // nO[0] = 3: scale so that the minimum absolute value is nO[1]
  // nO[0] = 4: scale so that the L-nO[1] norm equals nO[2]

  /* *********************************************************** */
  /* NOTE: indexing starts at 1 in the following few arrays: */
  /* *********************************************************** */

  // private double[] lambda;
  private ArrayList<Double> lambda = new ArrayList<Double>();
  // the current weight vector. NOTE: indexing starts at 1.
  private ArrayList<Double> bestLambda = new ArrayList<Double>();
  // the best weight vector across all iterations

  private boolean[] isOptimizable;
  // isOptimizable[c] = true iff lambda[c] should be optimized

  private double[] minRandValue;
  private double[] maxRandValue;
  // when choosing a random value for the lambda[c] parameter, it will be
  // chosen from the [minRandValue[c],maxRandValue[c]] range.
  // (*) minRandValue and maxRandValue must be real values, but not -Inf or +Inf

  private double[] defaultLambda;
  // "default" parameter values; simply the values read in the parameter file
  // USED FOR NON-OPTIMIZABLE (FIXED) FEATURES

  /* *********************************************************** */
  /* *********************************************************** */

  private Decoder myDecoder;
  // COMMENT OUT if decoder is not Joshua

  private String decoderCommand;
  // the command that runs the decoder; read from decoderCommandFileName

  private int decVerbosity;
  // verbosity level for decoder output. If 0, decoder output is ignored.
  // If 1, decoder output is printed.

  private int validDecoderExitValue;
  // return value from running the decoder command that indicates success

  private int numOptThreads;
  // number of threads to run things in parallel

  private int saveInterFiles;
  // 0: nothing, 1: only configs, 2: only n-bests, 3: both configs and n-bests

  private int compressFiles;
  // should PRO gzip the large files? If 0, no compression takes place.
  // If 1, compression is performed on: decoder output files, temp sents files,
  // and temp feats files.

  private int sizeOfNBest;
  // size of N-best list generated by decoder at each iteration
  // (aka simply N, but N is a bad variable name)

  private long seed;
  // seed used to create random number generators

  private boolean randInit;
  // if true, parameters are initialized randomly. If false, parameters
  // are initialized using values from parameter file.

  private int maxMERTIterations, minMERTIterations, prevMERTIterations;
  // max: maximum number of MERT iterations
  // min: minimum number of MERT iterations before an early MERT exit
  // prev: number of previous MERT iterations from which to consider candidates (in addition to
  // the candidates from the current iteration)

  private double stopSigValue;
  // early MERT exit if no weight changes by more than stopSigValue
  // (but see minMERTIterations above and stopMinIts below)

  private int stopMinIts;
  // some early stopping criterion must be satisfied in stopMinIts *consecutive* iterations
  // before an early exit (but see minMERTIterations above)

  private boolean oneModificationPerIteration;
  // if true, each MERT iteration performs at most one parameter modification.
  // If false, a new MERT iteration starts (i.e. a new N-best list is
  // generated) only after the previous iteration reaches a local maximum.

  private String metricName;
  // name of evaluation metric optimized by MERT

  private String metricName_display;
  // name of evaluation metric optimized by MERT, possibly with "doc-level " prefixed

  private String[] metricOptions;
  // options for the evaluation metric (e.g. for BLEU, maxGramLength and effLengthMethod)

  private EvaluationMetric evalMetric;
  // the evaluation metric used by MERT

  private int suffStatsCount;
  // number of sufficient statistics for the evaluation metric

  private String tmpDirPrefix;
  // prefix for the PRO.temp.* files

  private boolean passIterationToDecoder;
  // should the iteration number be passed as an argument to decoderCommandFileName?

  // used for pro
  private String classifierAlg; // the classification algorithm(percep, megam, maxent ...)
  private String[] classifierParams = null; // the param array for each classifier
  private int Tau;
  private int Xi;
  private double interCoef;
  private double metricDiff;
  private double prevMetricScore = 0; // final metric score of the previous iteration, used only
                                      // when returnBest = true
  private boolean returnBest = false; // return the best weight during tuning

  private String dirPrefix; // where are all these files located?
  private String paramsFileName, docInfoFileName, finalLambdaFileName;
  private String sourceFileName, refFileName, decoderOutFileName;
  private String decoderConfigFileName, decoderCommandFileName;
  private String fakeFileNameTemplate, fakeFileNamePrefix, fakeFileNameSuffix;

  // e.g. output.it[1-x].someOldRun would be specified as:
  // output.it?.someOldRun
  // and we'd have prefix = "output.it" and suffix = ".sameOldRun"

  // private int useDisk;

  public PROCore(JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
  }

  public PROCore(String[] args, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    EvaluationMetric.set_knownMetrics();
    processArgsArray(args);
    initialize(0);
  }

  public PROCore(String configFileName, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    EvaluationMetric.set_knownMetrics();
    processArgsArray(cfgFileToArgsArray(configFileName));
    initialize(0);
  }

  private void initialize(int randsToSkip) {
    println("NegInf: " + NegInf + ", PosInf: " + PosInf + ", epsilon: " + epsilon, 4);

    randGen = new Random(seed);
    for (int r = 1; r <= randsToSkip; ++r) {
      randGen.nextDouble();
    }
    generatedRands = randsToSkip;

    if (randsToSkip == 0) {
      println("----------------------------------------------------", 1);
      println("Initializing...", 1);
      println("----------------------------------------------------", 1);
      println("", 1);

      println("Random number generator initialized using seed: " + seed, 1);
      println("", 1);
    }

    // COUNT THE TOTAL NUM OF SENTENCES TO BE DECODED, refFileName IS THE COMBINED REFERENCE FILE
    // NAME(AUTO GENERATED)
    numSentences = countLines(refFileName) / refsPerSen;

    // ??
    processDocInfo();
    // sets numDocuments and docOfSentence[]

    if (numDocuments > 1)
      metricName_display = "doc-level " + metricName;

    // ??
    set_docSubsetInfo(docSubsetInfo);

    // count the number of initial features
    numParams = countNonEmptyLines(paramsFileName) - 1;
    numParamsOld = numParams;

    // read parameter config file
    try {
      // read dense parameter names
      BufferedReader inFile_names = new BufferedReader(new FileReader(paramsFileName));

      for (int c = 1; c <= numParams; ++c) {
        String line = "";
        while (line != null && line.length() == 0) { // skip empty lines
          line = inFile_names.readLine();
        }

        // save feature names
        String paramName = (line.substring(0, line.indexOf("|||"))).trim();
        Vocabulary.id(paramName);
        // System.err.println(String.format("VOCAB(%s) = %d", paramName, id));
      }

      inFile_names.close();
    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in PROCore.initialize(int): " + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in PROCore.initialize(int): " + e.getMessage());
      System.exit(99902);
    }

    // the parameter file contains one line per parameter
    // and one line for the normalization method
    // indexing starts at 1 in these arrays
    for (int p = 0; p <= numParams; ++p)
      lambda.add(new Double(0));
    bestLambda.add(new Double(0));
    // why only lambda is a list? because the size of lambda
    // may increase over time, but other arrays are specified in
    // the param config file, only used for initialization
    isOptimizable = new boolean[1 + numParams];
    minRandValue = new double[1 + numParams];
    maxRandValue = new double[1 + numParams];
    defaultLambda = new double[1 + numParams];
    normalizationOptions = new double[3];

    // read initial param values
    processParamFile();
    // sets the arrays declared just above

    // SentenceInfo.createV(); // uncomment ONLY IF using vocabulary implementation of SentenceInfo

    String[][] refSentences = new String[numSentences][refsPerSen];

    try {

      // read in reference sentences
      InputStream inStream_refs = new FileInputStream(new File(refFileName));
      BufferedReader inFile_refs = new BufferedReader(new InputStreamReader(inStream_refs, "utf8"));

      for (int i = 0; i < numSentences; ++i) {
        for (int r = 0; r < refsPerSen; ++r) {
          // read the rth reference translation for the ith sentence
          refSentences[i][r] = inFile_refs.readLine();
        }
      }

      inFile_refs.close();

      // normalize reference sentences
      for (int i = 0; i < numSentences; ++i) {
        for (int r = 0; r < refsPerSen; ++r) {
          // normalize the rth reference translation for the ith sentence
          refSentences[i][r] = normalize(refSentences[i][r], textNormMethod);
        }
      }

      // read in decoder command, if any
      decoderCommand = null;
      if (decoderCommandFileName != null) {
        if (fileExists(decoderCommandFileName)) {
          BufferedReader inFile_comm = new BufferedReader(new FileReader(decoderCommandFileName));
          decoderCommand = inFile_comm.readLine(); // READ IN DECODE COMMAND
          inFile_comm.close();
        }
      }
    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in PROCore.initialize(int): " + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in PROCore.initialize(int): " + e.getMessage());
      System.exit(99902);
    }

    // set static data members for the EvaluationMetric class
    EvaluationMetric.set_numSentences(numSentences);
    EvaluationMetric.set_numDocuments(numDocuments);
    EvaluationMetric.set_refsPerSen(refsPerSen);
    EvaluationMetric.set_refSentences(refSentences);
    EvaluationMetric.set_tmpDirPrefix(tmpDirPrefix);

    evalMetric = EvaluationMetric.getMetric(metricName, metricOptions);
    // used only if returnBest = true
    prevMetricScore = evalMetric.getToBeMinimized() ? PosInf : NegInf;

    // length of sufficient statistics
    // for bleu: suffstatscount=8 (2*ngram+2)
    suffStatsCount = evalMetric.get_suffStatsCount();

    // set static data members for the IntermediateOptimizer class
    /*
     * IntermediateOptimizer.set_MERTparams(numSentences, numDocuments, docOfSentence,
     * docSubsetInfo, numParams, normalizationOptions, isOptimizable oneModificationPerIteration,
     * evalMetric, tmpDirPrefix, verbosity);
     */

    // print info
    if (randsToSkip == 0) { // i.e. first iteration
      println("Number of sentences: " + numSentences, 1);
      println("Number of documents: " + numDocuments, 1);
      println("Optimizing " + metricName_display, 1);

      /*
       * print("docSubsetInfo: {", 1); for (int f = 0; f < 6; ++f) print(docSubsetInfo[f] + ", ",
       * 1); println(docSubsetInfo[6] + "}", 1);
       */

      println("Number of initial features: " + numParams, 1);
      print("Initial feature names: {", 1);

      for (int c = 1; c <= numParams; ++c)
        print("\"" + Vocabulary.word(c) + "\"", 1);
      println("}", 1);
      println("", 1);

      // TODO just print the correct info
      println("c    Default value\tOptimizable?\tRand. val. range", 1);

      for (int c = 1; c <= numParams; ++c) {
        print(c + "     " + f4.format(lambda.get(c).doubleValue()) + "\t\t", 1);

        if (!isOptimizable[c]) {
          println(" No", 1);
        } else {
          print(" Yes\t\t", 1);
          print(" [" + minRandValue[c] + "," + maxRandValue[c] + "]", 1);
          println("", 1);
        }
      }

      println("", 1);
      print("Weight vector normalization method: ", 1);
      if (normalizationOptions[0] == 0) {
        println("none.", 1);
      } else if (normalizationOptions[0] == 1) {
        println(
            "weights will be scaled so that the \""
                + Vocabulary.word((int) normalizationOptions[2])
                + "\" weight has an absolute value of " + normalizationOptions[1] + ".", 1);
      } else if (normalizationOptions[0] == 2) {
        println("weights will be scaled so that the maximum absolute value is "
            + normalizationOptions[1] + ".", 1);
      } else if (normalizationOptions[0] == 3) {
        println("weights will be scaled so that the minimum absolute value is "
            + normalizationOptions[1] + ".", 1);
      } else if (normalizationOptions[0] == 4) {
        println("weights will be scaled so that the L-" + normalizationOptions[1] + " norm is "
            + normalizationOptions[2] + ".", 1);
      }

      println("", 1);

      println("----------------------------------------------------", 1);
      println("", 1);

      // rename original config file so it doesn't get overwritten
      // (original name will be restored in finish())
      renameFile(decoderConfigFileName, decoderConfigFileName + ".PRO.orig");
    } // if (randsToSkip == 0)

    // by default, load joshua decoder
    if (decoderCommand == null && fakeFileNameTemplate == null) {
      println("Loading Joshua decoder...", 1);
      myDecoder = new Decoder(joshuaConfiguration, decoderConfigFileName + ".PRO.orig");
      println("...finished loading @ " + (new Date()), 1);
      println("");
    } else {
      myDecoder = null;
    }

    @SuppressWarnings("unchecked")
    TreeSet<Integer>[] temp_TSA = new TreeSet[numSentences];
    indicesOfInterest_all = temp_TSA;

    for (int i = 0; i < numSentences; ++i) {
      indicesOfInterest_all[i] = new TreeSet<Integer>();
    }
  } // void initialize(...)

  // -------------------------

  public void run_PRO() {
    run_PRO(minMERTIterations, maxMERTIterations, prevMERTIterations);
  }

  public void run_PRO(int minIts, int maxIts, int prevIts) {
    // FIRST, CLEAN ALL PREVIOUS TEMP FILES
    String dir;
    int k = tmpDirPrefix.lastIndexOf("/");
    if (k >= 0) {
      dir = tmpDirPrefix.substring(0, k + 1);
    } else {
      dir = "./";
    }
    String files;
    File folder = new File(dir);

    if (folder.exists()) {
      File[] listOfFiles = folder.listFiles();

      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          files = listOfFiles[i].getName();
          if (files.startsWith("PRO.temp")) {
            deleteFile(files);
          }
        }
      }
    }

    println("----------------------------------------------------", 1);
    println("PRO run started @ " + (new Date()), 1);
    // printMemoryUsage();
    println("----------------------------------------------------", 1);
    println("", 1);

    // if no default lambda is provided
    if (randInit) {
      println("Initializing lambda[] randomly.", 1);
      // initialize optimizable parameters randomly (sampling uniformly from
      // that parameter's random value range)
      lambda = randomLambda();
    }

    println("Initial lambda[]: " + lambdaToString(lambda), 1);
    println("", 1);

    int[] maxIndex = new int[numSentences];

    // HashMap<Integer,int[]>[] suffStats_array = new HashMap[numSentences];
    // suffStats_array[i] maps candidates of interest for sentence i to an array
    // storing the sufficient statistics for that candidate

    int earlyStop = 0;
    // number of consecutive iteration an early stopping criterion was satisfied

    for (int iteration = 1;; ++iteration) {

      // what does "A" contain?
      // retA[0]: FINAL_score
      // retA[1]: earlyStop
      // retA[2]: should this be the last iteration?
      double[] A = run_single_iteration(iteration, minIts, maxIts, prevIts, earlyStop, maxIndex);
      if (A != null) {
        earlyStop = (int) A[1];
        if (A[2] == 1)
          break;
      } else {
        break;
      }

    } // for (iteration)

    println("", 1);

    println("----------------------------------------------------", 1);
    println("PRO run ended @ " + (new Date()), 1);
    // printMemoryUsage();
    println("----------------------------------------------------", 1);
    println("", 1);

    if (!returnBest)
      println("FINAL lambda: " + lambdaToString(lambda), 1);
    // + " (" + metricName_display + ": " + FINAL_score + ")",1);
    else
      println("BEST lambda: " + lambdaToString(lambda), 1);
    // + " (" + metricName_display + ": " + FINAL_score + ")",1);

    // delete intermediate .temp.*.it* decoder output files
    for (int iteration = 1; iteration <= maxIts; ++iteration) {
      if (compressFiles == 1) {
        deleteFile(tmpDirPrefix + "temp.sents.it" + iteration + ".gz");
        deleteFile(tmpDirPrefix + "temp.feats.it" + iteration + ".gz");
        if (fileExists(tmpDirPrefix + "temp.stats.it" + iteration + ".copy.gz")) {
          deleteFile(tmpDirPrefix + "temp.stats.it" + iteration + ".copy.gz");
        } else {
          deleteFile(tmpDirPrefix + "temp.stats.it" + iteration + ".gz");
        }
      } else {
        deleteFile(tmpDirPrefix + "temp.sents.it" + iteration);
        deleteFile(tmpDirPrefix + "temp.feats.it" + iteration);
        if (fileExists(tmpDirPrefix + "temp.stats.it" + iteration + ".copy")) {
          deleteFile(tmpDirPrefix + "temp.stats.it" + iteration + ".copy");
        } else {
          deleteFile(tmpDirPrefix + "temp.stats.it" + iteration);
        }
      }
    }
  } // void run_PRO(int maxIts)

  // this is the key function!
  @SuppressWarnings("unchecked")
  public double[] run_single_iteration(int iteration, int minIts, int maxIts, int prevIts,
      int earlyStop, int[] maxIndex) {
    double FINAL_score = 0;

    double[] retA = new double[3];
    // retA[0]: FINAL_score
    // retA[1]: earlyStop
    // retA[2]: should this be the last iteration?

    boolean done = false;
    retA[2] = 1; // will only be made 0 if we don't break from the following loop

    // save feats and stats for all candidates(old & new)
    HashMap<String, String>[] feat_hash = new HashMap[numSentences];
    for (int i = 0; i < numSentences; i++)
      feat_hash[i] = new HashMap<String, String>();

    HashMap<String, String>[] stats_hash = new HashMap[numSentences];
    for (int i = 0; i < numSentences; i++)
      stats_hash[i] = new HashMap<String, String>();

    while (!done) { // NOTE: this "loop" will only be carried out once
      println("--- Starting PRO iteration #" + iteration + " @ " + (new Date()) + " ---", 1);

      // printMemoryUsage();

      /******************************/
      // CREATE DECODER CONFIG FILE //
      /******************************/

      createConfigFile(lambda, decoderConfigFileName, decoderConfigFileName + ".PRO.orig");
      // i.e. use the original config file as a template

      /***************/
      // RUN DECODER //
      /***************/

      if (iteration == 1) {
        println("Decoding using initial weight vector " + lambdaToString(lambda), 1);
      } else {
        println("Redecoding using weight vector " + lambdaToString(lambda), 1);
      }

      // generate the n-best file after decoding
      String[] decRunResult = run_decoder(iteration); // iteration passed in case fake decoder will
                                                      // be used
      // [0] name of file to be processed
      // [1] indicates how the output file was obtained:
      // 1: external decoder
      // 2: fake decoder
      // 3: internal decoder

      if (!decRunResult[1].equals("2")) {
        println("...finished decoding @ " + (new Date()), 1);
      }

      checkFile(decRunResult[0]);

      /************* END OF DECODING **************/

      println("Producing temp files for iteration " + iteration, 3);

      produceTempFiles(decRunResult[0], iteration);

      // save intermedidate output files
      // save joshua.config.pro.it*
      if (saveInterFiles == 1 || saveInterFiles == 3) { // make copy of intermediate config file
        if (!copyFile(decoderConfigFileName, decoderConfigFileName + ".PRO.it" + iteration)) {
          println("Warning: attempt to make copy of decoder config file (to create"
              + decoderConfigFileName + ".PRO.it" + iteration + ") was unsuccessful!", 1);
        }
      }

      // save output.nest.PRO.it*
      if (saveInterFiles == 2 || saveInterFiles == 3) { // make copy of intermediate decoder output
                                                        // file...

        if (!decRunResult[1].equals("2")) { // ...but only if no fake decoder
          if (!decRunResult[0].endsWith(".gz")) {
            if (!copyFile(decRunResult[0], decRunResult[0] + ".PRO.it" + iteration)) {
              println("Warning: attempt to make copy of decoder output file (to create"
                  + decRunResult[0] + ".PRO.it" + iteration + ") was unsuccessful!", 1);
            }
          } else {
            String prefix = decRunResult[0].substring(0, decRunResult[0].length() - 3);
            if (!copyFile(prefix + ".gz", prefix + ".PRO.it" + iteration + ".gz")) {
              println("Warning: attempt to make copy of decoder output file (to create" + prefix
                  + ".PRO.it" + iteration + ".gz" + ") was unsuccessful!", 1);
            }
          }

          if (compressFiles == 1 && !decRunResult[0].endsWith(".gz")) {
            gzipFile(decRunResult[0] + ".PRO.it" + iteration);
          }
        } // if (!fake)
      }

      // ------------- end of saving .pro.it* files ---------------

      int[] candCount = new int[numSentences];
      int[] lastUsedIndex = new int[numSentences];

      ConcurrentHashMap<Integer, int[]>[] suffStats_array = new ConcurrentHashMap[numSentences];
      for (int i = 0; i < numSentences; ++i) {
        candCount[i] = 0;
        lastUsedIndex[i] = -1;
        // suffStats_array[i].clear();
        suffStats_array[i] = new ConcurrentHashMap<Integer, int[]>();
      }

      // initLambda[0] is not used!
      double[] initialLambda = new double[1 + numParams];
      for (int i = 1; i <= numParams; ++i)
        initialLambda[i] = lambda.get(i);

      // the "score" in initialScore refers to that
      // assigned by the evaluation metric)

      // you may consider all candidates from iter 1, or from iter (iteration-prevIts) to current
      // iteration
      int firstIt = Math.max(1, iteration - prevIts);
      // i.e. only process candidates from the current iteration and candidates
      // from up to prevIts previous iterations.
      println("Reading candidate translations from iterations " + firstIt + "-" + iteration, 1);
      println("(and computing " + metricName
          + " sufficient statistics for previously unseen candidates)", 1);
      print("  Progress: ");

      int[] newCandidatesAdded = new int[1 + iteration];
      for (int it = 1; it <= iteration; ++it)
        newCandidatesAdded[it] = 0;

      try {
        // read temp files from all past iterations
        // 3 types of temp files:
        // 1. output hypo at iter i
        // 2. feature value of each hypo at iter i
        // 3. suff stats of each hypo at iter i

        // each inFile corresponds to the output of an iteration
        // (index 0 is not used; no corresponding index for the current iteration)
        BufferedReader[] inFile_sents = new BufferedReader[iteration];
        BufferedReader[] inFile_feats = new BufferedReader[iteration];
        BufferedReader[] inFile_stats = new BufferedReader[iteration];

        // temp file(array) from previous iterations
        for (int it = firstIt; it < iteration; ++it) {
          InputStream inStream_sents, inStream_feats, inStream_stats;
          if (compressFiles == 0) {
            inStream_sents = new FileInputStream(tmpDirPrefix + "temp.sents.it" + it);
            inStream_feats = new FileInputStream(tmpDirPrefix + "temp.feats.it" + it);
            inStream_stats = new FileInputStream(tmpDirPrefix + "temp.stats.it" + it);
          } else {
            inStream_sents = new GZIPInputStream(new FileInputStream(tmpDirPrefix + "temp.sents.it"
                + it + ".gz"));
            inStream_feats = new GZIPInputStream(new FileInputStream(tmpDirPrefix + "temp.feats.it"
                + it + ".gz"));
            inStream_stats = new GZIPInputStream(new FileInputStream(tmpDirPrefix + "temp.stats.it"
                + it + ".gz"));
          }

          inFile_sents[it] = new BufferedReader(new InputStreamReader(inStream_sents, "utf8"));
          inFile_feats[it] = new BufferedReader(new InputStreamReader(inStream_feats, "utf8"));
          inFile_stats[it] = new BufferedReader(new InputStreamReader(inStream_stats, "utf8"));
        }

        InputStream inStream_sentsCurrIt, inStream_featsCurrIt, inStream_statsCurrIt;
        // temp file for current iteration!
        if (compressFiles == 0) {
          inStream_sentsCurrIt = new FileInputStream(tmpDirPrefix + "temp.sents.it" + iteration);
          inStream_featsCurrIt = new FileInputStream(tmpDirPrefix + "temp.feats.it" + iteration);
        } else {
          inStream_sentsCurrIt = new GZIPInputStream(new FileInputStream(tmpDirPrefix
              + "temp.sents.it" + iteration + ".gz"));
          inStream_featsCurrIt = new GZIPInputStream(new FileInputStream(tmpDirPrefix
              + "temp.feats.it" + iteration + ".gz"));
        }

        BufferedReader inFile_sentsCurrIt = new BufferedReader(new InputStreamReader(
            inStream_sentsCurrIt, "utf8"));
        BufferedReader inFile_featsCurrIt = new BufferedReader(new InputStreamReader(
            inStream_featsCurrIt, "utf8"));

        BufferedReader inFile_statsCurrIt = null; // will only be used if statsCurrIt_exists below
                                                  // is set to true
        PrintWriter outFile_statsCurrIt = null; // will only be used if statsCurrIt_exists below is
                                                // set to false

        // just to check if temp.stat.it.iteration exists
        boolean statsCurrIt_exists = false;

        if (fileExists(tmpDirPrefix + "temp.stats.it" + iteration)) {
          inStream_statsCurrIt = new FileInputStream(tmpDirPrefix + "temp.stats.it" + iteration);
          inFile_statsCurrIt = new BufferedReader(new InputStreamReader(inStream_statsCurrIt,
              "utf8"));
          statsCurrIt_exists = true;
          copyFile(tmpDirPrefix + "temp.stats.it" + iteration, tmpDirPrefix + "temp.stats.it"
              + iteration + ".copy");
        } else if (fileExists(tmpDirPrefix + "temp.stats.it" + iteration + ".gz")) {
          inStream_statsCurrIt = new GZIPInputStream(new FileInputStream(tmpDirPrefix
              + "temp.stats.it" + iteration + ".gz"));
          inFile_statsCurrIt = new BufferedReader(new InputStreamReader(inStream_statsCurrIt,
              "utf8"));
          statsCurrIt_exists = true;
          copyFile(tmpDirPrefix + "temp.stats.it" + iteration + ".gz", tmpDirPrefix
              + "temp.stats.it" + iteration + ".copy.gz");
        } else {
          outFile_statsCurrIt = new PrintWriter(tmpDirPrefix + "temp.stats.it" + iteration);
        }

        // output the 4^th temp file: *.temp.stats.merged
        PrintWriter outFile_statsMerged = new PrintWriter(tmpDirPrefix + "temp.stats.merged");
        // write sufficient statistics from all the sentences
        // from the output files into a single file
        PrintWriter outFile_statsMergedKnown = new PrintWriter(tmpDirPrefix
            + "temp.stats.mergedKnown");
        // write sufficient statistics from all the sentences
        // from the output files into a single file

        // output the 5^th 6^th temp file, but will be deleted at the end of the function
        FileOutputStream outStream_unknownCands = new FileOutputStream(tmpDirPrefix
            + "temp.currIt.unknownCands", false);
        OutputStreamWriter outStreamWriter_unknownCands = new OutputStreamWriter(
            outStream_unknownCands, "utf8");
        BufferedWriter outFile_unknownCands = new BufferedWriter(outStreamWriter_unknownCands);

        PrintWriter outFile_unknownIndices = new PrintWriter(tmpDirPrefix
            + "temp.currIt.unknownIndices");

        String sents_str, feats_str, stats_str;

        // BUG: this assumes a candidate string cannot be produced for two
        // different source sentences, which is not necessarily true
        // (It's not actually a bug, but only because existingCandStats gets
        // cleared before moving to the next source sentence.)
        // FIX: should be made an array, indexed by i
        HashMap<String, String> existingCandStats = new HashMap<String, String>();
        // VERY IMPORTANT:
        // A CANDIDATE X MAY APPEARED IN ITER 1, ITER 3
        // BUT IF THE USER SPECIFIED TO CONSIDER ITERATIONS FROM ONLY ITER 2, THEN
        // X IS NOT A "REPEATED" CANDIDATE IN ITER 3. THEREFORE WE WANT TO KEEP THE
        // SUFF STATS FOR EACH CANDIDATE(TO SAVE COMPUTATION IN THE FUTURE)

        // Stores precalculated sufficient statistics for candidates, in case
        // the same candidate is seen again. (SS stored as a String.)
        // Q: Why do we care? If we see the same candidate again, aren't we going
        // to ignore it? So, why do we care about the SS of this repeat candidate?
        // A: A "repeat" candidate may not be a repeat candidate in later
        // iterations if the user specifies a value for prevMERTIterations
        // that causes MERT to skip candidates from early iterations.

        String[] featVal_str;

        int totalCandidateCount = 0;

        // new candidate size for each sentence
        int[] sizeUnknown_currIt = new int[numSentences];

        for (int i = 0; i < numSentences; ++i) {
          // process candidates from previous iterations
          // low efficiency? for each iteration, it reads in all previous iteration outputs
          // therefore a lot of overlapping jobs
          // this is an easy implementation to deal with the situation in which user only specified
          // "previt" and hopes to consider only the previous previt
          // iterations, then for each iteration the existing candadites will be different
          for (int it = firstIt; it < iteration; ++it) {
            // Why up to but *excluding* iteration?
            // Because the last iteration is handled a little differently, since
            // the SS must be calculated (and the corresponding file created),
            // which is not true for previous iterations.

            for (int n = 0; n <= sizeOfNBest; ++n) {
              // note that in all temp files, "||||||" is a separator between 2 n-best lists

              // Why up to and *including* sizeOfNBest?
              // So that it would read the "||||||" separator even if there is
              // a complete list of sizeOfNBest candidates.

              // for the nth candidate for the ith sentence, read the sentence, feature values,
              // and sufficient statistics from the various temp files

              // read one line of temp.sent, temp.feat, temp.stats from iteration it
              sents_str = inFile_sents[it].readLine();
              feats_str = inFile_feats[it].readLine();
              stats_str = inFile_stats[it].readLine();

              if (sents_str.equals("||||||")) {
                n = sizeOfNBest + 1; // move on to the next n-best list
              } else if (!existingCandStats.containsKey(sents_str)) // if this candidate does not
                                                                    // exist
              {
                outFile_statsMergedKnown.println(stats_str);

                // save feats & stats
                feat_hash[i].put(sents_str, feats_str);
                stats_hash[i].put(sents_str, stats_str);

                // extract feature value
                featVal_str = feats_str.split("\\s+");

                if (feats_str.indexOf('=') != -1) {
                  for (String featurePair : featVal_str) {
                    String[] pair = featurePair.split("=");
                    String name = pair[0];
                    Double value = Double.parseDouble(pair[1]);
                    int featId = Vocabulary.id(name);
                    // need to identify newly fired feats here
                    if (featId > numParams) {
                      ++numParams;
                      lambda.add(new Double(0));
                    }
                  }
                }
                existingCandStats.put(sents_str, stats_str);
                candCount[i] += 1;
                newCandidatesAdded[it] += 1;
              } // if unseen candidate
            } // for (n)
          } // for (it)

          outFile_statsMergedKnown.println("||||||");

          // ---------- end of processing previous iterations ----------
          // ---------- now start processing new candidates ----------

          // now process the candidates of the current iteration
          // now determine the new candidates of the current iteration

          /*
           * remember: BufferedReader inFile_sentsCurrIt BufferedReader inFile_featsCurrIt
           * PrintWriter outFile_statsCurrIt
           */

          String[] sentsCurrIt_currSrcSent = new String[sizeOfNBest + 1];

          Vector<String> unknownCands_V = new Vector<String>();
          // which candidates (of the i'th source sentence) have not been seen before
          // this iteration?

          for (int n = 0; n <= sizeOfNBest; ++n) {
            // Why up to and *including* sizeOfNBest?
            // So that it would read the "||||||" separator even if there is
            // a complete list of sizeOfNBest candidates.

            // for the nth candidate for the ith sentence, read the sentence,
            // and store it in the sentsCurrIt_currSrcSent array

            sents_str = inFile_sentsCurrIt.readLine(); // read one candidate from the current
                                                       // iteration
            sentsCurrIt_currSrcSent[n] = sents_str; // Note: possibly "||||||"

            if (sents_str.equals("||||||")) {
              n = sizeOfNBest + 1;
            } else if (!existingCandStats.containsKey(sents_str)) {
              unknownCands_V.add(sents_str); // NEW CANDIDATE FROM THIS ITERATION
              writeLine(sents_str, outFile_unknownCands);
              outFile_unknownIndices.println(i); // INDEX OF THE NEW CANDIDATES
              newCandidatesAdded[iteration] += 1;
              existingCandStats.put(sents_str, "U"); // i.e. unknown
              // we add sents_str to avoid duplicate entries in unknownCands_V
            }
          } // for (n)

          // only compute suff stats for new candidates
          // now unknownCands_V has the candidates for which we need to calculate
          // sufficient statistics (for the i'th source sentence)
          int sizeUnknown = unknownCands_V.size();
          sizeUnknown_currIt[i] = sizeUnknown;

          existingCandStats.clear();

        } // for (i) each sentence

        // ---------- end of merging candidates stats from previous iterations
        // and finding new candidates ------------

        /*
         * int[][] newSuffStats = null; if (!statsCurrIt_exists && sizeUnknown > 0) { newSuffStats =
         * evalMetric.suffStats(unknownCands, indices); }
         */

        outFile_statsMergedKnown.close();
        outFile_unknownCands.close();
        outFile_unknownIndices.close();

        // want to re-open all temp files and start from scratch again?
        for (int it = firstIt; it < iteration; ++it) // previous iterations temp files
        {
          inFile_sents[it].close();
          inFile_stats[it].close();

          InputStream inStream_sents, inStream_stats;
          if (compressFiles == 0) {
            inStream_sents = new FileInputStream(tmpDirPrefix + "temp.sents.it" + it);
            inStream_stats = new FileInputStream(tmpDirPrefix + "temp.stats.it" + it);
          } else {
            inStream_sents = new GZIPInputStream(new FileInputStream(tmpDirPrefix + "temp.sents.it"
                + it + ".gz"));
            inStream_stats = new GZIPInputStream(new FileInputStream(tmpDirPrefix + "temp.stats.it"
                + it + ".gz"));
          }

          inFile_sents[it] = new BufferedReader(new InputStreamReader(inStream_sents, "utf8"));
          inFile_stats[it] = new BufferedReader(new InputStreamReader(inStream_stats, "utf8"));
        }

        inFile_sentsCurrIt.close();
        // current iteration temp files
        if (compressFiles == 0) {
          inStream_sentsCurrIt = new FileInputStream(tmpDirPrefix + "temp.sents.it" + iteration);
        } else {
          inStream_sentsCurrIt = new GZIPInputStream(new FileInputStream(tmpDirPrefix
              + "temp.sents.it" + iteration + ".gz"));
        }
        inFile_sentsCurrIt = new BufferedReader(new InputStreamReader(inStream_sentsCurrIt, "utf8"));

        // calculate SS for unseen candidates and write them to file
        FileInputStream inStream_statsCurrIt_unknown = null;
        BufferedReader inFile_statsCurrIt_unknown = null;

        if (!statsCurrIt_exists && newCandidatesAdded[iteration] > 0) {
          // create the file...
          evalMetric.createSuffStatsFile(tmpDirPrefix + "temp.currIt.unknownCands", tmpDirPrefix
              + "temp.currIt.unknownIndices", tmpDirPrefix + "temp.stats.unknown", sizeOfNBest);

          // ...and open it
          inStream_statsCurrIt_unknown = new FileInputStream(tmpDirPrefix + "temp.stats.unknown");
          inFile_statsCurrIt_unknown = new BufferedReader(new InputStreamReader(
              inStream_statsCurrIt_unknown, "utf8"));
        }

        // open mergedKnown file
        // newly created by the big loop above
        FileInputStream instream_statsMergedKnown = new FileInputStream(tmpDirPrefix
            + "temp.stats.mergedKnown");
        BufferedReader inFile_statsMergedKnown = new BufferedReader(new InputStreamReader(
            instream_statsMergedKnown, "utf8"));

        // num of features before observing new firing features from this iteration
        numParamsOld = numParams;

        for (int i = 0; i < numSentences; ++i) {
          // reprocess candidates from previous iterations
          for (int it = firstIt; it < iteration; ++it) {
            for (int n = 0; n <= sizeOfNBest; ++n) {
              sents_str = inFile_sents[it].readLine();
              stats_str = inFile_stats[it].readLine();

              if (sents_str.equals("||||||")) {
                n = sizeOfNBest + 1;
              } else if (!existingCandStats.containsKey(sents_str)) {
                existingCandStats.put(sents_str, stats_str);
              } // if unseen candidate
            } // for (n)
          } // for (it)

          // copy relevant portion from mergedKnown to the merged file
          String line_mergedKnown = inFile_statsMergedKnown.readLine();
          while (!line_mergedKnown.equals("||||||")) {
            outFile_statsMerged.println(line_mergedKnown);
            line_mergedKnown = inFile_statsMergedKnown.readLine();
          }

          int[] stats = new int[suffStatsCount];

          for (int n = 0; n <= sizeOfNBest; ++n) {
            sents_str = inFile_sentsCurrIt.readLine();
            feats_str = inFile_featsCurrIt.readLine();

            if (sents_str.equals("||||||")) {
              n = sizeOfNBest + 1;
            } else if (!existingCandStats.containsKey(sents_str)) {

              if (!statsCurrIt_exists) {
                stats_str = inFile_statsCurrIt_unknown.readLine();

                String[] temp_stats = stats_str.split("\\s+");
                for (int s = 0; s < suffStatsCount; ++s) {
                  stats[s] = Integer.parseInt(temp_stats[s]);
                }

                outFile_statsCurrIt.println(stats_str);
              } else {
                stats_str = inFile_statsCurrIt.readLine();

                String[] temp_stats = stats_str.split("\\s+");
                for (int s = 0; s < suffStatsCount; ++s) {
                  stats[s] = Integer.parseInt(temp_stats[s]);
                }
              }

              outFile_statsMerged.println(stats_str);

              // save feats & stats
              // System.out.println(sents_str+" "+feats_str);

              feat_hash[i].put(sents_str, feats_str);
              stats_hash[i].put(sents_str, stats_str);

              featVal_str = feats_str.split("\\s+");

              if (feats_str.indexOf('=') != -1) {
                for (String featurePair : featVal_str) {
                  String[] pair = featurePair.split("=");
                  String name = pair[0];
                  int featId = Vocabulary.id(name);
                  // need to identify newly fired feats here
                  if (featId > numParams) {
                    ++numParams;
                    lambda.add(new Double(0));
                  }
                }
              }
              existingCandStats.put(sents_str, stats_str);
              candCount[i] += 1;

              // newCandidatesAdded[iteration] += 1;
              // moved to code above detecting new candidates
            } else {
              if (statsCurrIt_exists)
                inFile_statsCurrIt.readLine();
              else {
                // write SS to outFile_statsCurrIt
                stats_str = existingCandStats.get(sents_str);
                outFile_statsCurrIt.println(stats_str);
              }
            }

          } // for (n)

          // now d = sizeUnknown_currIt[i] - 1

          if (statsCurrIt_exists)
            inFile_statsCurrIt.readLine();
          else
            outFile_statsCurrIt.println("||||||");

          existingCandStats.clear();
          totalCandidateCount += candCount[i];

          // output sentence progress
          if ((i + 1) % 500 == 0) {
            print((i + 1) + "\n" + "            ", 1);
          } else if ((i + 1) % 100 == 0) {
            print("+", 1);
          } else if ((i + 1) % 25 == 0) {
            print(".", 1);
          }

        } // for (i)

        inFile_statsMergedKnown.close();
        outFile_statsMerged.close();

        // for testing
        /*
         * int total_sent = 0; for( int i=0; i<numSentences; i++ ) {
         * System.out.println(feat_hash[i].size()+" "+candCount[i]); total_sent +=
         * feat_hash[i].size(); feat_hash[i].clear(); }
         * System.out.println("----------------total sent: "+total_sent); total_sent = 0; for( int
         * i=0; i<numSentences; i++ ) { System.out.println(stats_hash[i].size()+" "+candCount[i]);
         * total_sent += stats_hash[i].size(); stats_hash[i].clear(); }
         * System.out.println("*****************total sent: "+total_sent);
         */

        println("", 1); // finish progress line

        for (int it = firstIt; it < iteration; ++it) {
          inFile_sents[it].close();
          inFile_feats[it].close();
          inFile_stats[it].close();
        }

        inFile_sentsCurrIt.close();
        inFile_featsCurrIt.close();
        if (statsCurrIt_exists)
          inFile_statsCurrIt.close();
        else
          outFile_statsCurrIt.close();

        if (compressFiles == 1 && !statsCurrIt_exists) {
          gzipFile(tmpDirPrefix + "temp.stats.it" + iteration);
        }

        // clear temp files
        deleteFile(tmpDirPrefix + "temp.currIt.unknownCands");
        deleteFile(tmpDirPrefix + "temp.currIt.unknownIndices");
        deleteFile(tmpDirPrefix + "temp.stats.unknown");
        deleteFile(tmpDirPrefix + "temp.stats.mergedKnown");

        // cleanupMemory();

        println("Processed " + totalCandidateCount + " distinct candidates " + "(about "
            + totalCandidateCount / numSentences + " per sentence):", 1);
        for (int it = firstIt; it <= iteration; ++it) {
          println("newCandidatesAdded[it=" + it + "] = " + newCandidatesAdded[it] + " (about "
              + newCandidatesAdded[it] / numSentences + " per sentence)", 1);
        }

        println("", 1);

        println("Number of features observed so far: " + numParams);
        println("", 1);

      } catch (FileNotFoundException e) {
        System.err.println("FileNotFoundException in PROCore.run_single_iteration(6): "
            + e.getMessage());
        System.exit(99901);
      } catch (IOException e) {
        System.err.println("IOException in PROCore.run_single_iteration(6): " + e.getMessage());
        System.exit(99902);
      }

      // n-best list converges
      if (newCandidatesAdded[iteration] == 0) {
        if (!oneModificationPerIteration) {
          println("No new candidates added in this iteration; exiting PRO.", 1);
          println("", 1);
          println("---  PRO iteration #" + iteration + " ending @ " + (new Date()) + "  ---", 1);
          println("", 1);
          deleteFile(tmpDirPrefix + "temp.stats.merged");

          if (returnBest) {
            // note that bestLambda.size() <= lambda.size()
            for (int p = 1; p < bestLambda.size(); ++p)
              lambda.set(p, bestLambda.get(p));
            // and set the rest of lambda to be 0
            for (int p = 0; p < lambda.size() - bestLambda.size(); ++p)
              lambda.set(p + bestLambda.size(), new Double(0));
          }

          return null; // this means that the old values should be kept by the caller
        } else {
          println("Note: No new candidates added in this iteration.", 1);
        }
      }

      /************* start optimization **************/

      /*
       * for( int v=1; v<initialLambda[1].length; v++ ) System.out.print(initialLambda[1][v]+" ");
       * System.exit(0);
       */

      Vector<String> output = new Vector<String>();

      // note: initialLambda[] has length = numParamsOld
      // augmented with new feature weights, initial values are 0
      double[] initialLambdaNew = new double[1 + numParams];
      System.arraycopy(initialLambda, 1, initialLambdaNew, 1, numParamsOld);

      // finalLambda[] has length = numParams (considering new features)
      double[] finalLambda = new double[1 + numParams];

      Optimizer opt = new Optimizer(seed + iteration, isOptimizable, output, initialLambdaNew,
          feat_hash, stats_hash, evalMetric, Tau, Xi, metricDiff, normalizationOptions,
          classifierAlg, classifierParams);
      finalLambda = opt.run_Optimizer();

      if (returnBest) {
        double metricScore = opt.getMetricScore();
        if (!evalMetric.getToBeMinimized()) {
          if (metricScore > prevMetricScore) {
            prevMetricScore = metricScore;
            for (int p = 1; p < bestLambda.size(); ++p)
              bestLambda.set(p, finalLambda[p]);
            if (1 + numParams > bestLambda.size()) {
              for (int p = bestLambda.size(); p <= numParams; ++p)
                bestLambda.add(p, finalLambda[p]);
            }
          }
        } else {
          if (metricScore < prevMetricScore) {
            prevMetricScore = metricScore;
            for (int p = 1; p < bestLambda.size(); ++p)
              bestLambda.set(p, finalLambda[p]);
            if (1 + numParams > bestLambda.size()) {
              for (int p = bestLambda.size(); p <= numParams; ++p)
                bestLambda.add(p, finalLambda[p]);
            }
          }
        }
      }

      // System.out.println(finalLambda.length);
      // for( int i=0; i<finalLambda.length-1; i++ )
      // System.out.print(finalLambda[i+1]+" ");
      // System.out.println();

      /************* end optimization **************/

      for (int i = 0; i < output.size(); i++)
        println(output.get(i));

      // check if any parameter has been updated
      boolean anyParamChanged = false;
      boolean anyParamChangedSignificantly = false;

      for (int c = 1; c <= numParams; ++c) {
        if (finalLambda[c] != lambda.get(c)) {
          anyParamChanged = true;
        }
        if (Math.abs(finalLambda[c] - lambda.get(c)) > stopSigValue) {
          anyParamChangedSignificantly = true;
        }
      }

      // System.arraycopy(finalLambda,1,lambda,1,numParams);

      println("---  PRO iteration #" + iteration + " ending @ " + (new Date()) + "  ---", 1);
      println("", 1);

      if (!anyParamChanged) {
        println("No parameter value changed in this iteration; exiting PRO.", 1);
        println("", 1);
        break; // exit for (iteration) loop preemptively
      }

      // was an early stopping criterion satisfied?
      boolean critSatisfied = false;
      if (!anyParamChangedSignificantly && stopSigValue >= 0) {
        println("Note: No parameter value changed significantly " + "(i.e. by more than "
            + stopSigValue + ") in this iteration.", 1);
        critSatisfied = true;
      }

      if (critSatisfied) {
        ++earlyStop;
        println("", 1);
      } else {
        earlyStop = 0;
      }

      // if min number of iterations executed, investigate if early exit should happen
      if (iteration >= minIts && earlyStop >= stopMinIts) {
        println("Some early stopping criteria has been observed " + "in " + stopMinIts
            + " consecutive iterations; exiting PRO.", 1);
        println("", 1);

        if (returnBest) {
          // note that numParams >= bestLamba.size()-1 here!
          for (int f = 1; f <= bestLambda.size() - 1; ++f)
            lambda.set(f, bestLambda.get(f));
        } else {
          for (int f = 1; f <= numParams; ++f)
            lambda.set(f, finalLambda[f]);
        }

        break; // exit for (iteration) loop preemptively
      }

      // if max number of iterations executed, exit
      if (iteration >= maxIts) {
        println("Maximum number of PRO iterations reached; exiting PRO.", 1);
        println("", 1);

        if (returnBest) {
          // note that numParams >= bestLamba.size()-1 here!
          for (int f = 1; f <= bestLambda.size() - 1; ++f)
            lambda.set(f, bestLambda.get(f));
        } else {
          for (int f = 1; f <= numParams; ++f)
            lambda.set(f, finalLambda[f]);
        }

        break; // exit for (iteration) loop
      }

      // use the new wt vector to decode the next iteration
      // (interpolation with previous wt vector)
      for (int i = 1; i <= numParams; i++)
        lambda.set(i, interCoef * finalLambda[i] + (1 - interCoef) * lambda.get(i).doubleValue());

      println("Next iteration will decode with lambda: " + lambdaToString(lambda), 1);
      println("", 1);

      // printMemoryUsage();
      for (int i = 0; i < numSentences; ++i) {
        suffStats_array[i].clear();
      }
      // cleanupMemory();
      // println("",2);

      retA[2] = 0; // i.e. this should NOT be the last iteration
      done = true;

    } // while (!done) // NOTE: this "loop" will only be carried out once

    // delete .temp.stats.merged file, since it is not needed in the next
    // iteration (it will be recreated from scratch)
    deleteFile(tmpDirPrefix + "temp.stats.merged");

    retA[0] = FINAL_score;
    retA[1] = earlyStop;
    return retA;

  } // run_single_iteration

  private String lambdaToString(ArrayList<Double> lambdaA) {
    String retStr = "{";
    int featToPrint = numParams > 15 ? 15 : numParams;
    // print at most the first 15 features

    retStr += "(listing the first " + featToPrint + " lambdas)";
    for (int c = 1; c <= featToPrint - 1; ++c) {
      retStr += "" + String.format("%.4f", lambdaA.get(c).doubleValue()) + ", ";
    }
    retStr += "" + String.format("%.4f", lambdaA.get(numParams).doubleValue()) + "}";

    return retStr;
  }

  private String[] run_decoder(int iteration) {
    String[] retSA = new String[2];

    // retsa saves the output file name(nbest-file)
    // and the decoder type

    // [0] name of file to be processed
    // [1] indicates how the output file was obtained:
    // 1: external decoder
    // 2: fake decoder
    // 3: internal decoder

    // use fake decoder
    if (fakeFileNameTemplate != null
        && fileExists(fakeFileNamePrefix + iteration + fakeFileNameSuffix)) {
      String fakeFileName = fakeFileNamePrefix + iteration + fakeFileNameSuffix;
      println("Not running decoder; using " + fakeFileName + " instead.", 1);
      /*
       * if (fakeFileName.endsWith(".gz")) { copyFile(fakeFileName,decoderOutFileName+".gz");
       * gunzipFile(decoderOutFileName+".gz"); } else { copyFile(fakeFileName,decoderOutFileName); }
       */
      retSA[0] = fakeFileName;
      retSA[1] = "2";

    } else {
      println("Running external decoder...", 1);

      try {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(decoderCommandFileName);

        if (passIterationToDecoder)
          cmd.add(Integer.toString(iteration));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // this merges the error and output streams of the subprocess
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // capture the sub-command's output
        new StreamGobbler(p.getInputStream(), decVerbosity).start();

        int decStatus = p.waitFor();
        if (decStatus != validDecoderExitValue) {
          println("Call to decoder returned " + decStatus + "; was expecting "
              + validDecoderExitValue + ".");
          System.exit(30);
        }
      } catch (IOException e) {
        System.err.println("IOException in PROCore.run_decoder(int): " + e.getMessage());
        System.exit(99902);
      } catch (InterruptedException e) {
        System.err.println("InterruptedException in PROCore.run_decoder(int): " + e.getMessage());
        System.exit(99903);
      }

      retSA[0] = decoderOutFileName;
      retSA[1] = "1";

    }

    return retSA;
  }

  private void produceTempFiles(String nbestFileName, int iteration) {
    try {
      String sentsFileName = tmpDirPrefix + "temp.sents.it" + iteration;
      String featsFileName = tmpDirPrefix + "temp.feats.it" + iteration;

      FileOutputStream outStream_sents = new FileOutputStream(sentsFileName, false);
      OutputStreamWriter outStreamWriter_sents = new OutputStreamWriter(outStream_sents, "utf8");
      BufferedWriter outFile_sents = new BufferedWriter(outStreamWriter_sents);

      PrintWriter outFile_feats = new PrintWriter(featsFileName);

      InputStream inStream_nbest = null;
      if (nbestFileName.endsWith(".gz")) {
        inStream_nbest = new GZIPInputStream(new FileInputStream(nbestFileName));
      } else {
        inStream_nbest = new FileInputStream(nbestFileName);
      }
      BufferedReader inFile_nbest = new BufferedReader(
          new InputStreamReader(inStream_nbest, "utf8"));

      String line; // , prevLine;
      String candidate_str = "";
      String feats_str = "";

      int i = 0;
      int n = 0;
      line = inFile_nbest.readLine();

      while (line != null) {

        /*
         * line format:
         * 
         * i ||| words of candidate translation . ||| feat-1_val feat-2_val ... feat-numParams_val
         * .*
         */

        // in a well formed file, we'd find the nth candidate for the ith sentence

        int read_i = Integer.parseInt((line.substring(0, line.indexOf("|||"))).trim());

        if (read_i != i) {
          writeLine("||||||", outFile_sents);
          outFile_feats.println("||||||");
          n = 0;
          ++i;
        }

        line = (line.substring(line.indexOf("|||") + 3)).trim(); // get rid of initial text

        candidate_str = (line.substring(0, line.indexOf("|||"))).trim();
        feats_str = (line.substring(line.indexOf("|||") + 3)).trim();
        // get rid of candidate string

        int junk_i = feats_str.indexOf("|||");
        if (junk_i >= 0) {
          feats_str = (feats_str.substring(0, junk_i)).trim();
        }

        writeLine(normalize(candidate_str, textNormMethod), outFile_sents);
        outFile_feats.println(feats_str);

        ++n;
        if (n == sizeOfNBest) {
          writeLine("||||||", outFile_sents);
          outFile_feats.println("||||||");
          n = 0;
          ++i;
        }

        line = inFile_nbest.readLine();
      }

      if (i != numSentences) { // last sentence had too few candidates
        writeLine("||||||", outFile_sents);
        outFile_feats.println("||||||");
      }

      inFile_nbest.close();
      outFile_sents.close();
      outFile_feats.close();

      if (compressFiles == 1) {
        gzipFile(sentsFileName);
        gzipFile(featsFileName);
      }

    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in PROCore.produceTempFiles(int): "
          + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in PROCore.produceTempFiles(int): " + e.getMessage());
      System.exit(99902);
    }

  }

  private void createConfigFile(ArrayList<Double> params, String cfgFileName,
      String templateFileName) {
    try {
      // i.e. create cfgFileName, which is similar to templateFileName, but with
      // params[] as parameter values

      BufferedReader inFile = new BufferedReader(new FileReader(templateFileName));
      PrintWriter outFile = new PrintWriter(cfgFileName);

      BufferedReader inFeatDefFile = null;
      PrintWriter outFeatDefFile = null;
      int origFeatNum = 0; // feat num in the template file

      String line = inFile.readLine();
      while (line != null) {
        int c_match = -1;
        for (int c = 1; c <= numParams; ++c) {
          if (line.startsWith(Vocabulary.word(c) + " ")) {
            c_match = c;
            ++origFeatNum;
            break;
          }
        }

        if (c_match == -1) {
          outFile.println(line);
        } else {
          if (Math.abs(params.get(c_match).doubleValue()) > 1e-20)
            outFile.println(Vocabulary.word(c_match) + " " + params.get(c_match));
        }

        line = inFile.readLine();
      }

      // now append weights of new features
      for (int c = origFeatNum + 1; c <= numParams; ++c) {
        if (Math.abs(params.get(c).doubleValue()) > 1e-20)
          outFile.println(Vocabulary.word(c) + " " + params.get(c));
      }

      inFile.close();
      outFile.close();
    } catch (IOException e) {
      System.err.println("IOException in PROCore.createConfigFile(double[],String,String): "
          + e.getMessage());
      System.exit(99902);
    }
  }

  private void processParamFile() {
    // process parameter file
    Scanner inFile_init = null;
    try {
      inFile_init = new Scanner(new FileReader(paramsFileName));
    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in PROCore.processParamFile(): " + e.getMessage());
      System.exit(99901);
    }

    String dummy = "";

    // initialize lambda[] and other related arrays
    for (int c = 1; c <= numParams; ++c) {
      // skip parameter name
      while (!dummy.equals("|||")) {
        dummy = inFile_init.next();
      }

      // read default value
      lambda.set(c, inFile_init.nextDouble());
      defaultLambda[c] = lambda.get(c).doubleValue();

      // read isOptimizable
      dummy = inFile_init.next();
      if (dummy.equals("Opt")) {
        isOptimizable[c] = true;
      } else if (dummy.equals("Fix")) {
        isOptimizable[c] = false;
      } else {
        println("Unknown isOptimizable string " + dummy + " (must be either Opt or Fix)");
        System.exit(21);
      }

      if (!isOptimizable[c]) { // skip next two values
        dummy = inFile_init.next();
        dummy = inFile_init.next();
        dummy = inFile_init.next();
        dummy = inFile_init.next();
      } else {
        // the next two values are not used, only to be consistent with ZMERT's params file format
        dummy = inFile_init.next();
        dummy = inFile_init.next();
        // set minRandValue[c] and maxRandValue[c] (range for random values)
        dummy = inFile_init.next();
        if (dummy.equals("-Inf") || dummy.equals("+Inf")) {
          println("minRandValue[" + c + "] cannot be -Inf or +Inf!");
          System.exit(21);
        } else {
          minRandValue[c] = Double.parseDouble(dummy);
        }

        dummy = inFile_init.next();
        if (dummy.equals("-Inf") || dummy.equals("+Inf")) {
          println("maxRandValue[" + c + "] cannot be -Inf or +Inf!");
          System.exit(21);
        } else {
          maxRandValue[c] = Double.parseDouble(dummy);
        }

        // check for illogical values
        if (minRandValue[c] > maxRandValue[c]) {
          println("minRandValue[" + c + "]=" + minRandValue[c] + " > " + maxRandValue[c]
              + "=maxRandValue[" + c + "]!");
          System.exit(21);
        }

        // check for odd values
        if (minRandValue[c] == maxRandValue[c]) {
          println("Warning: lambda[" + c + "] has " + "minRandValue = maxRandValue = "
              + minRandValue[c] + ".", 1);
        }
      } // if (!isOptimizable[c])

      /*
       * precision[c] = inFile_init.nextDouble(); if (precision[c] < 0) { println("precision[" + c +
       * "]=" + precision[c] + " < 0!  Must be non-negative."); System.exit(21); }
       */

    }

    // set normalizationOptions[]
    String origLine = "";
    while (origLine != null && origLine.length() == 0) {
      origLine = inFile_init.nextLine();
    }

    // How should a lambda[] vector be normalized (before decoding)?
    // nO[0] = 0: no normalization
    // nO[0] = 1: scale so that parameter nO[2] has absolute value nO[1]
    // nO[0] = 2: scale so that the maximum absolute value is nO[1]
    // nO[0] = 3: scale so that the minimum absolute value is nO[1]
    // nO[0] = 4: scale so that the L-nO[1] norm equals nO[2]

    // normalization = none
    // normalization = absval 1 lm
    // normalization = maxabsval 1
    // normalization = minabsval 1
    // normalization = LNorm 2 1

    dummy = (origLine.substring(origLine.indexOf("=") + 1)).trim();
    String[] dummyA = dummy.split("\\s+");

    if (dummyA[0].equals("none")) {
      normalizationOptions[0] = 0;
    } else if (dummyA[0].equals("absval")) {
      normalizationOptions[0] = 1;
      normalizationOptions[1] = Double.parseDouble(dummyA[1]);
      String pName = dummyA[2];
      for (int i = 3; i < dummyA.length; ++i) { // in case parameter name has multiple words
        pName = pName + " " + dummyA[i];
      }
      normalizationOptions[2] = Vocabulary.id(pName);

      if (normalizationOptions[1] <= 0) {
        println("Value for the absval normalization method must be positive.");
        System.exit(21);
      }
      if (normalizationOptions[2] == 0) {
        println("Unrecognized feature name " + normalizationOptions[2]
            + " for absval normalization method.", 1);
        System.exit(21);
      }
    } else if (dummyA[0].equals("maxabsval")) {
      normalizationOptions[0] = 2;
      normalizationOptions[1] = Double.parseDouble(dummyA[1]);
      if (normalizationOptions[1] <= 0) {
        println("Value for the maxabsval normalization method must be positive.");
        System.exit(21);
      }
    } else if (dummyA[0].equals("minabsval")) {
      normalizationOptions[0] = 3;
      normalizationOptions[1] = Double.parseDouble(dummyA[1]);
      if (normalizationOptions[1] <= 0) {
        println("Value for the minabsval normalization method must be positive.");
        System.exit(21);
      }
    } else if (dummyA[0].equals("LNorm")) {
      normalizationOptions[0] = 4;
      normalizationOptions[1] = Double.parseDouble(dummyA[1]);
      normalizationOptions[2] = Double.parseDouble(dummyA[2]);
      if (normalizationOptions[1] <= 0 || normalizationOptions[2] <= 0) {
        println("Both values for the LNorm normalization method must be positive.");
        System.exit(21);
      }
    } else {
      println("Unrecognized normalization method " + dummyA[0] + "; "
          + "must be one of none, absval, maxabsval, and LNorm.");
      System.exit(21);
    } // if (dummyA[0])

    inFile_init.close();
  } // processParamFile()

  private void processDocInfo() {
    // sets numDocuments and docOfSentence[]
    docOfSentence = new int[numSentences];

    if (docInfoFileName == null) {
      for (int i = 0; i < numSentences; ++i)
        docOfSentence[i] = 0;
      numDocuments = 1;
    } else {

      try {

        // 4 possible formats:
        // 1) List of numbers, one per document, indicating # sentences in each document.
        // 2) List of "docName size" pairs, one per document, indicating name of document and #
        // sentences.
        // 3) List of docName's, one per sentence, indicating which doument each sentence belongs
        // to.
        // 4) List of docName_number's, one per sentence, indicating which doument each sentence
        // belongs to,
        // and its order in that document. (can also use '-' instead of '_')

        int docInfoSize = countNonEmptyLines(docInfoFileName);

        if (docInfoSize < numSentences) { // format #1 or #2
          numDocuments = docInfoSize;
          int i = 0;

          BufferedReader inFile = new BufferedReader(new FileReader(docInfoFileName));
          String line = inFile.readLine();
          boolean format1 = (!(line.contains(" ")));

          for (int doc = 0; doc < numDocuments; ++doc) {

            if (doc != 0)
              line = inFile.readLine();

            int docSize = 0;
            if (format1) {
              docSize = Integer.parseInt(line);
            } else {
              docSize = Integer.parseInt(line.split("\\s+")[1]);
            }

            for (int i2 = 1; i2 <= docSize; ++i2) {
              docOfSentence[i] = doc;
              ++i;
            }

          }

          // now i == numSentences

          inFile.close();

        } else if (docInfoSize == numSentences) { // format #3 or #4

          boolean format3 = false;

          HashSet<String> seenStrings = new HashSet<String>();
          BufferedReader inFile = new BufferedReader(new FileReader(docInfoFileName));
          for (int i = 0; i < numSentences; ++i) {
            // set format3 = true if a duplicate is found
            String line = inFile.readLine();
            if (seenStrings.contains(line))
              format3 = true;
            seenStrings.add(line);
          }

          inFile.close();

          HashSet<String> seenDocNames = new HashSet<String>();
          HashMap<String, Integer> docOrder = new HashMap<String, Integer>();
          // maps a document name to the order (0-indexed) in which it was seen

          inFile = new BufferedReader(new FileReader(docInfoFileName));
          for (int i = 0; i < numSentences; ++i) {
            String line = inFile.readLine();

            String docName = "";
            if (format3) {
              docName = line;
            } else {
              int sep_i = Math.max(line.lastIndexOf('_'), line.lastIndexOf('-'));
              docName = line.substring(0, sep_i);
            }

            if (!seenDocNames.contains(docName)) {
              seenDocNames.add(docName);
              docOrder.put(docName, seenDocNames.size() - 1);
            }

            int docOrder_i = docOrder.get(docName);

            docOfSentence[i] = docOrder_i;

          }

          inFile.close();

          numDocuments = seenDocNames.size();

        } else { // badly formatted

        }

      } catch (FileNotFoundException e) {
        System.err.println("FileNotFoundException in PROCore.processDocInfo(): " + e.getMessage());
        System.exit(99901);
      } catch (IOException e) {
        System.err.println("IOException in PROCore.processDocInfo(): " + e.getMessage());
        System.exit(99902);
      }
    }

  }

  private boolean copyFile(String origFileName, String newFileName) {
    try {
      File inputFile = new File(origFileName);
      File outputFile = new File(newFileName);

      InputStream in = new FileInputStream(inputFile);
      OutputStream out = new FileOutputStream(outputFile);

      byte[] buffer = new byte[1024];
      int len;
      while ((len = in.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }
      in.close();
      out.close();

      /*
       * InputStream inStream = new FileInputStream(new File(origFileName)); BufferedReader inFile =
       * new BufferedReader(new InputStreamReader(inStream, "utf8"));
       * 
       * FileOutputStream outStream = new FileOutputStream(newFileName, false); OutputStreamWriter
       * outStreamWriter = new OutputStreamWriter(outStream, "utf8"); BufferedWriter outFile = new
       * BufferedWriter(outStreamWriter);
       * 
       * String line; while(inFile.ready()) { line = inFile.readLine(); writeLine(line, outFile); }
       * 
       * inFile.close(); outFile.close();
       */
      return true;
    } catch (FileNotFoundException e) {
      System.err.println("FileNotFoundException in PROCore.copyFile(String,String): "
          + e.getMessage());
      return false;
    } catch (IOException e) {
      System.err.println("IOException in PROCore.copyFile(String,String): " + e.getMessage());
      return false;
    }
  }

  private void renameFile(String origFileName, String newFileName) {
    if (fileExists(origFileName)) {
      deleteFile(newFileName);
      File oldFile = new File(origFileName);
      File newFile = new File(newFileName);
      if (!oldFile.renameTo(newFile)) {
        println("Warning: attempt to rename " + origFileName + " to " + newFileName
            + " was unsuccessful!", 1);
      }
    } else {
      println("Warning: file " + origFileName + " does not exist! (in PROCore.renameFile)", 1);
    }
  }

  private void deleteFile(String fileName) {
    if (fileExists(fileName)) {
      File fd = new File(fileName);
      if (!fd.delete()) {
        println("Warning: attempt to delete " + fileName + " was unsuccessful!", 1);
      }
    }
  }

  private void writeLine(String line, BufferedWriter writer) throws IOException {
    writer.write(line, 0, line.length());
    writer.newLine();
    writer.flush();
  }

  // need to re-write to handle different forms of lambda
  public void finish() {
    if (myDecoder != null) {
      myDecoder.cleanUp();
    }

    // create config file with final values
    createConfigFile(lambda, decoderConfigFileName + ".PRO.final", decoderConfigFileName
        + ".PRO.orig");

    // delete current decoder config file and decoder output
    deleteFile(decoderConfigFileName);
    deleteFile(decoderOutFileName);

    // restore original name for config file (name was changed
    // in initialize() so it doesn't get overwritten)
    renameFile(decoderConfigFileName + ".PRO.orig", decoderConfigFileName);

    if (finalLambdaFileName != null) {
      try {
        PrintWriter outFile_lambdas = new PrintWriter(finalLambdaFileName);
        for (int c = 1; c <= numParams; ++c) {
          outFile_lambdas.println(Vocabulary.word(c) + " ||| " + lambda.get(c).doubleValue());
        }
        outFile_lambdas.close();

      } catch (IOException e) {
        System.err.println("IOException in PROCore.finish(): " + e.getMessage());
        System.exit(99902);
      }
    }

  }

  private String[] cfgFileToArgsArray(String fileName) {
    checkFile(fileName);

    Vector<String> argsVector = new Vector<String>();

    BufferedReader inFile = null;
    try {
      inFile = new BufferedReader(new FileReader(fileName));
      String line, origLine;
      do {
        line = inFile.readLine();
        origLine = line; // for error reporting purposes

        if (line != null && line.length() > 0 && line.charAt(0) != '#') {

          if (line.indexOf("#") != -1) { // discard comment
            line = line.substring(0, line.indexOf("#"));
          }

          line = line.trim();

          // now line should look like "-xxx XXX"

          /*
           * OBSOLETE MODIFICATION //SPECIAL HANDLING FOR PRO CLASSIFIER PARAMETERS String[] paramA
           * = line.split("\\s+");
           * 
           * if( paramA[0].equals("-classifierParams") ) { String classifierParam = ""; for(int p=1;
           * p<=paramA.length-1; p++) classifierParam += paramA[p]+" ";
           * 
           * if(paramA.length>=2) { String[] tmpParamA = new String[2]; tmpParamA[0] = paramA[0];
           * tmpParamA[1] = classifierParam; paramA = tmpParamA; } else {
           * println("Malformed line in config file:"); println(origLine); System.exit(70); } }//END
           * MODIFICATION
           */

          // CMU MODIFICATION(FROM METEOR FOR ZMERT)
          // Parse args
          ArrayList<String> argList = new ArrayList<String>();
          StringBuilder arg = new StringBuilder();
          boolean quoted = false;
          for (int i = 0; i < line.length(); i++) {
            if (Character.isWhitespace(line.charAt(i))) {
              if (quoted)
                arg.append(line.charAt(i));
              else if (arg.length() > 0) {
                argList.add(arg.toString());
                arg = new StringBuilder();
              }
            } else if (line.charAt(i) == '\'') {
              if (quoted) {
                argList.add(arg.toString());
                arg = new StringBuilder();
              }
              quoted = !quoted;
            } else
              arg.append(line.charAt(i));
          }
          if (arg.length() > 0)
            argList.add(arg.toString());
          // Create paramA
          String[] paramA = new String[argList.size()];
          for (int i = 0; i < paramA.length; paramA[i] = argList.get(i++))
            ;
          // END CMU MODIFICATION

          if (paramA.length == 2 && paramA[0].charAt(0) == '-') {
            argsVector.add(paramA[0]);
            argsVector.add(paramA[1]);
          } else if (paramA.length > 2 && (paramA[0].equals("-m") || paramA[0].equals("-docSet"))) {
            // -m (metricName), -docSet are allowed to have extra optinos
            for (int opt = 0; opt < paramA.length; ++opt) {
              argsVector.add(paramA[opt]);
            }
          } else {
            println("Malformed line in config file:");
            println(origLine);
            System.exit(70);
          }

        }
      } while (line != null);

      inFile.close();
    } catch (FileNotFoundException e) {
      println("PRO configuration file " + fileName + " was not found!");
      System.err.println("FileNotFoundException in PROCore.cfgFileToArgsArray(String): "
          + e.getMessage());
      System.exit(99901);
    } catch (IOException e) {
      System.err.println("IOException in PROCore.cfgFileToArgsArray(String): " + e.getMessage());
      System.exit(99902);
    }

    String[] argsArray = new String[argsVector.size()];

    for (int i = 0; i < argsVector.size(); ++i) {
      argsArray[i] = argsVector.elementAt(i);
    }

    return argsArray;
  }

  private void processArgsArray(String[] args) {
    processArgsArray(args, true);
  }

  private void processArgsArray(String[] args, boolean firstTime) {
    /* set default values */
    // Relevant files
    dirPrefix = null;
    sourceFileName = null;
    refFileName = "reference.txt";
    refsPerSen = 1;
    textNormMethod = 1;
    paramsFileName = "params.txt";
    docInfoFileName = null;
    finalLambdaFileName = null;
    // MERT specs
    metricName = "BLEU";
    metricName_display = metricName;
    metricOptions = new String[2];
    metricOptions[0] = "4";
    metricOptions[1] = "closest";
    docSubsetInfo = new int[7];
    docSubsetInfo[0] = 0;
    maxMERTIterations = 20;
    prevMERTIterations = 20;
    minMERTIterations = 5;
    stopMinIts = 3;
    stopSigValue = -1;
    //
    // /* possibly other early stopping criteria here */
    //
    numOptThreads = 1;
    saveInterFiles = 3;
    compressFiles = 0;
    oneModificationPerIteration = false;
    randInit = false;
    seed = System.currentTimeMillis();
    // useDisk = 2;
    // Decoder specs
    decoderCommandFileName = null;
    passIterationToDecoder = false;
    decoderOutFileName = "output.nbest";
    validDecoderExitValue = 0;
    decoderConfigFileName = "dec_cfg.txt";
    sizeOfNBest = 100;
    fakeFileNameTemplate = null;
    fakeFileNamePrefix = null;
    fakeFileNameSuffix = null;
    // Output specs
    verbosity = 1;
    decVerbosity = 0;

    int i = 0;

    while (i < args.length) {
      String option = args[i];
      // Relevant files
      if (option.equals("-dir")) {
        dirPrefix = args[i + 1];
      } else if (option.equals("-s")) {
        sourceFileName = args[i + 1];
      } else if (option.equals("-r")) {
        refFileName = args[i + 1];
      } else if (option.equals("-rps")) {
        refsPerSen = Integer.parseInt(args[i + 1]);
        if (refsPerSen < 1) {
          println("refsPerSen must be positive.");
          System.exit(10);
        }
      } else if (option.equals("-txtNrm")) {
        textNormMethod = Integer.parseInt(args[i + 1]);
        if (textNormMethod < 0 || textNormMethod > 4) {
          println("textNormMethod should be between 0 and 4");
          System.exit(10);
        }
      } else if (option.equals("-p")) {
        paramsFileName = args[i + 1];
      } else if (option.equals("-docInfo")) {
        docInfoFileName = args[i + 1];
      } else if (option.equals("-fin")) {
        finalLambdaFileName = args[i + 1];
        // MERT specs
      } else if (option.equals("-m")) {
        metricName = args[i + 1];
        metricName_display = metricName;
        if (EvaluationMetric.knownMetricName(metricName)) {
          int optionCount = EvaluationMetric.metricOptionCount(metricName);
          metricOptions = new String[optionCount];
          for (int opt = 0; opt < optionCount; ++opt) {
            metricOptions[opt] = args[i + opt + 2];
          }
          i += optionCount;
        } else {
          println("Unknown metric name " + metricName + ".");
          System.exit(10);
        }
      } else if (option.equals("-docSet")) {
        String method = args[i + 1];

        if (method.equals("all")) {
          docSubsetInfo[0] = 0;
          i += 0;
        } else if (method.equals("bottom")) {
          String a = args[i + 2];
          if (a.endsWith("d")) {
            docSubsetInfo[0] = 1;
            a = a.substring(0, a.indexOf("d"));
          } else {
            docSubsetInfo[0] = 2;
            a = a.substring(0, a.indexOf("%"));
          }
          docSubsetInfo[5] = Integer.parseInt(a);
          i += 1;
        } else if (method.equals("top")) {
          String a = args[i + 2];
          if (a.endsWith("d")) {
            docSubsetInfo[0] = 3;
            a = a.substring(0, a.indexOf("d"));
          } else {
            docSubsetInfo[0] = 4;
            a = a.substring(0, a.indexOf("%"));
          }
          docSubsetInfo[5] = Integer.parseInt(a);
          i += 1;
        } else if (method.equals("window")) {
          String a1 = args[i + 2];
          a1 = a1.substring(0, a1.indexOf("d")); // size of window
          String a2 = args[i + 4];
          if (a2.indexOf("p") > 0) {
            docSubsetInfo[0] = 5;
            a2 = a2.substring(0, a2.indexOf("p"));
          } else {
            docSubsetInfo[0] = 6;
            a2 = a2.substring(0, a2.indexOf("r"));
          }
          docSubsetInfo[5] = Integer.parseInt(a1);
          docSubsetInfo[6] = Integer.parseInt(a2);
          i += 3;
        } else {
          println("Unknown docSet method " + method + ".");
          System.exit(10);
        }
      } else if (option.equals("-maxIt")) {
        maxMERTIterations = Integer.parseInt(args[i + 1]);
        if (maxMERTIterations < 1) {
          println("maxMERTIts must be positive.");
          System.exit(10);
        }
      } else if (option.equals("-minIt")) {
        minMERTIterations = Integer.parseInt(args[i + 1]);
        if (minMERTIterations < 1) {
          println("minMERTIts must be positive.");
          System.exit(10);
        }
      } else if (option.equals("-prevIt")) {
        prevMERTIterations = Integer.parseInt(args[i + 1]);
        if (prevMERTIterations < 0) {
          println("prevMERTIts must be non-negative.");
          System.exit(10);
        }
      } else if (option.equals("-stopIt")) {
        stopMinIts = Integer.parseInt(args[i + 1]);
        if (stopMinIts < 1) {
          println("stopMinIts must be positive.");
          System.exit(10);
        }
      } else if (option.equals("-stopSig")) {
        stopSigValue = Double.parseDouble(args[i + 1]);
      }
      //
      // /* possibly other early stopping criteria here */
      //
      else if (option.equals("-thrCnt")) {
        numOptThreads = Integer.parseInt(args[i + 1]);
        if (numOptThreads < 1) {
          println("threadCount must be positive.");
          System.exit(10);
        }
      } else if (option.equals("-save")) {
        saveInterFiles = Integer.parseInt(args[i + 1]);
        if (saveInterFiles < 0 || saveInterFiles > 3) {
          println("save should be between 0 and 3");
          System.exit(10);
        }
      } else if (option.equals("-compress")) {
        compressFiles = Integer.parseInt(args[i + 1]);
        if (compressFiles < 0 || compressFiles > 1) {
          println("compressFiles should be either 0 or 1");
          System.exit(10);
        }
      } else if (option.equals("-opi")) {
        int opi = Integer.parseInt(args[i + 1]);
        if (opi == 1) {
          oneModificationPerIteration = true;
        } else if (opi == 0) {
          oneModificationPerIteration = false;
        } else {
          println("oncePerIt must be either 0 or 1.");
          System.exit(10);
        }
      } else if (option.equals("-rand")) {
        int rand = Integer.parseInt(args[i + 1]);
        if (rand == 1) {
          randInit = true;
        } else if (rand == 0) {
          randInit = false;
        } else {
          println("randInit must be either 0 or 1.");
          System.exit(10);
        }
      } else if (option.equals("-seed")) {
        if (args[i + 1].equals("time")) {
          seed = System.currentTimeMillis();
        } else {
          seed = Long.parseLong(args[i + 1]);
        }
      }
      /*
       * else if (option.equals("-ud")) { useDisk = Integer.parseInt(args[i+1]); if (useDisk < 0 ||
       * useDisk > 2) { println("useDisk should be between 0 and 2"); System.exit(10); } }
       */

      // for pro:
      // classification algorithm class path
      else if (option.equals("-classifierClass")) {
        classifierAlg = args[i + 1];
      }
      // params for the specified classifier
      else if (option.equals("-classifierParams")) {
        classifierParams = args[i + 1].split("\\s+");
      }
      // tau: num of randomly generated candidates
      else if (option.equals("-Tau")) {
        Tau = Integer.parseInt(args[i + 1]);
      }
      // xi: top-xi candidates to be accepted
      else if (option.equals("-Xi")) {
        Xi = Integer.parseInt(args[i + 1]);
      }
      // return the best weight during tuning or not
      else if (option.equals("-returnBest")) {
        int retBest = Integer.parseInt(args[i + 1]);
        if (retBest == 1)
          returnBest = true;
        else if (retBest == 0)
          returnBest = false;
        else {
          println("-returnBest must be either 0 or 1.");
          System.exit(10);
        }
      }
      // interpolation coefficient between current & previous weights
      else if (option.equals("-interCoef")) {
        interCoef = Double.parseDouble(args[i + 1]);
      }
      // metric(eg. bleu) diff threshold(to select sampled candidates)
      else if (option.equals("-metricDiff")) {
        metricDiff = Double.parseDouble(args[i + 1]);
      }

      // Decoder specs
      else if (option.equals("-cmd")) {
        decoderCommandFileName = args[i + 1];
      } else if (option.equals("-passIt")) {
        int val = Integer.parseInt(args[i + 1]);
        if (val < 0 || val > 1) {
          println("passIterationToDecoder should be either 0 or 1");
          System.exit(10);
        }
        passIterationToDecoder = (val == 1) ? true : false;
      } else if (option.equals("-decOut")) {
        decoderOutFileName = args[i + 1];
      } else if (option.equals("-decExit")) {
        validDecoderExitValue = Integer.parseInt(args[i + 1]);
      } else if (option.equals("-dcfg")) {
        decoderConfigFileName = args[i + 1];
      } else if (option.equals("-N")) {
        sizeOfNBest = Integer.parseInt(args[i + 1]);
        if (sizeOfNBest < 1) {
          println("N must be positive.");
          System.exit(10);
        }
      }
      // Output specs
      else if (option.equals("-v")) {
        verbosity = Integer.parseInt(args[i + 1]);
        if (verbosity < 0 || verbosity > 4) {
          println("verbosity should be between 0 and 4");
          System.exit(10);
        }
      } else if (option.equals("-decV")) {
        decVerbosity = Integer.parseInt(args[i + 1]);
        if (decVerbosity < 0 || decVerbosity > 1) {
          println("decVerbosity should be either 0 or 1");
          System.exit(10);
        }
      } else if (option.equals("-fake")) {
        fakeFileNameTemplate = args[i + 1];
        int QM_i = fakeFileNameTemplate.indexOf("?");
        if (QM_i <= 0) {
          println("fakeFileNameTemplate must contain '?' to indicate position of iteration number");
          System.exit(10);
        }
        fakeFileNamePrefix = fakeFileNameTemplate.substring(0, QM_i);
        fakeFileNameSuffix = fakeFileNameTemplate.substring(QM_i + 1);
      } else {
        println("Unknown option " + option);
        System.exit(10);
      }

      i += 2;

    } // while (i)

    if (maxMERTIterations < minMERTIterations) {

      if (firstTime)
        println("Warning: maxMERTIts is smaller than minMERTIts; " + "decreasing minMERTIts from "
            + minMERTIterations + " to maxMERTIts " + "(i.e. " + maxMERTIterations + ").", 1);

      minMERTIterations = maxMERTIterations;
    }

    if (dirPrefix != null) { // append dirPrefix to file names
      refFileName = fullPath(dirPrefix, refFileName);
      decoderOutFileName = fullPath(dirPrefix, decoderOutFileName);
      paramsFileName = fullPath(dirPrefix, paramsFileName);
      decoderConfigFileName = fullPath(dirPrefix, decoderConfigFileName);

      if (sourceFileName != null) {
        sourceFileName = fullPath(dirPrefix, sourceFileName);
      }
      if (docInfoFileName != null) {
        docInfoFileName = fullPath(dirPrefix, docInfoFileName);
      }
      if (finalLambdaFileName != null) {
        finalLambdaFileName = fullPath(dirPrefix, finalLambdaFileName);
      }
      if (decoderCommandFileName != null) {
        decoderCommandFileName = fullPath(dirPrefix, decoderCommandFileName);
      }
      if (fakeFileNamePrefix != null) {
        fakeFileNamePrefix = fullPath(dirPrefix, fakeFileNamePrefix);
      }
    }

    // TODO: make this an argument
    // TODO: also use this for the state file? could be tricky, since that file is created by
    // ZMERT.java
    // TODO: change name from tmpDirPrefix to tmpFilePrefix?
    int k = decoderOutFileName.lastIndexOf("/");
    if (k >= 0) {
      tmpDirPrefix = decoderOutFileName.substring(0, k + 1) + "PRO.";
    } else {
      tmpDirPrefix = "PRO.";
    }
    println("tmpDirPrefix: " + tmpDirPrefix);

    checkFile(paramsFileName);
    checkFile(decoderConfigFileName);

    boolean canRunCommand = fileExists(decoderCommandFileName);
    if (decoderCommandFileName != null && !canRunCommand) {
      // i.e. a decoder command file was specified, but it was not found
      if (firstTime)
        println("Warning: specified decoder command file " + decoderCommandFileName
            + " was not found.", 1);
    }
    boolean canRunJoshua = fileExists(sourceFileName);
    if (sourceFileName != null && !canRunJoshua) {
      // i.e. a source file was specified, but it was not found
      if (firstTime)
        println("Warning: specified source file " + sourceFileName + " was not found.", 1);
    }
    boolean canRunFake = (fakeFileNameTemplate != null);

    if (!canRunCommand && !canRunJoshua) { // can only run fake decoder

      if (!canRunFake) {
        println("PRO cannot decode; must provide one of: command file (for external decoder),");
        println("                                           source file (for Joshua decoder),");
        println("                                        or prefix for existing output files (for fake decoder).");
        System.exit(12);
      }

      int lastGoodIt = 0;
      for (int it = 1; it <= maxMERTIterations; ++it) {
        if (fileExists(fakeFileNamePrefix + it + fakeFileNameSuffix)) {
          lastGoodIt = it;
        } else {
          break; // from for (it) loop
        }
      }

      if (lastGoodIt == 0) {
        println("Fake decoder cannot find first output file "
            + (fakeFileNamePrefix + 1 + fakeFileNameSuffix));
        System.exit(13);
      } else if (lastGoodIt < maxMERTIterations) {
        if (firstTime)
          println("Warning: can only run fake decoder; existing output files "
              + "are only available for the first " + lastGoodIt + " iteration(s).", 1);
      }

    }

    if (refsPerSen > 1) {
      // the provided refFileName might be a prefix
      File dummy = new File(refFileName);
      if (!dummy.exists()) {
        refFileName = createUnifiedRefFile(refFileName, refsPerSen);
      }
    } else {
      checkFile(refFileName);
    }

    if (firstTime) {
      println("Processed the following args array:", 1);
      print("  ", 1);
      for (i = 0; i < args.length; ++i) {
        print(args[i] + " ", 1);
      }
      println("", 1);
      println("", 1);
    }

  } // processArgs(String[] args)

  private void set_docSubsetInfo(int[] info) {

    /*
     * 1: -docSet bottom 8d 2: -docSet bottom 25% the bottom ceil(0.20*numDocs) documents 3: -docSet
     * top 8d 4: -docSet top 25% the top ceil(0.20*numDocs) documents
     * 
     * 5: -docSet window 11d around 90percentile 11 docs centered around 80th percentile (complain
     * if not enough docs; don't adjust) 6: -docSet window 11d around 40rank 11 docs centered around
     * doc ranked 50 (complain if not enough docs; don't adjust)
     * 
     * 
     * [0]: method (0-6) [1]: first (1-indexed) [2]: last (1-indexed) [3]: size [4]: center [5]:
     * arg1 (-1 for method 0) [6]: arg2 (-1 for methods 0-4)
     */
    if (info[0] == 0) { // all
      info[1] = 1;
      info[2] = numDocuments;
      info[3] = numDocuments;
      info[4] = (info[1] + info[2]) / 2;
    }
    if (info[0] == 1) { // bottom d
      info[3] = info[5];
      info[2] = numDocuments;
      info[1] = numDocuments - info[3] + 1;
      info[4] = (info[1] + info[2]) / 2;
    }
    if (info[0] == 2) { // bottom p
      info[3] = (int) (Math.ceil((info[5] / 100.0) * numDocuments));
      info[2] = numDocuments;
      info[1] = numDocuments - info[3] + 1;
      info[4] = (info[1] + info[2]) / 2;
    }
    if (info[0] == 3) { // top d
      info[3] = info[5];
      info[1] = 1;
      info[2] = info[3];
      info[4] = (info[1] + info[2]) / 2;
    }
    if (info[0] == 4) { // top p
      info[3] = (int) (Math.ceil((info[5] / 100.0) * numDocuments));
      info[1] = 1;
      info[2] = info[3];
      info[4] = (info[1] + info[2]) / 2;
    }
    if (info[0] == 5) { // window around percentile
      info[3] = info[5];
      info[4] = (int) (Math.floor((info[6] / 100.0) * numDocuments));
      info[1] = info[4] - ((info[3] - 1) / 2);
      info[2] = info[4] + ((info[3] - 1) / 2);
    }
    if (info[0] == 6) { // window around rank
      info[3] = info[5];
      info[4] = info[6];
      info[1] = info[4] - ((info[3] - 1) / 2);
      info[2] = info[4] + ((info[3] - 1) / 2);
    }

  }

  private void checkFile(String fileName) {
    if (!fileExists(fileName)) {
      println("The file " + fileName + " was not found!");
      System.exit(40);
    }
  }

  private boolean fileExists(String fileName) {
    if (fileName == null)
      return false;
    File checker = new File(fileName);
    return checker.exists();
  }

  private void gzipFile(String inputFileName) {
    gzipFile(inputFileName, inputFileName + ".gz");
  }

  private void gzipFile(String inputFileName, String gzippedFileName) {
    // NOTE: this will delete the original file

    try {
      FileInputStream in = new FileInputStream(inputFileName);
      GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzippedFileName));

      byte[] buffer = new byte[4096];
      int len;
      while ((len = in.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }

      in.close();
      out.finish();
      out.close();

      deleteFile(inputFileName);

    } catch (IOException e) {
      System.err.println("IOException in PROCore.gzipFile(String,String): " + e.getMessage());
      System.exit(99902);
    }
  }

  @SuppressWarnings("unused")
  private void gunzipFile(String gzippedFileName) {
    if (gzippedFileName.endsWith(".gz")) {
      gunzipFile(gzippedFileName, gzippedFileName.substring(0, gzippedFileName.length() - 3));
    } else {
      gunzipFile(gzippedFileName, gzippedFileName + ".dec");
    }
  }

  private void gunzipFile(String gzippedFileName, String outputFileName) {
    // NOTE: this will delete the original file

    try {
      GZIPInputStream in = new GZIPInputStream(new FileInputStream(gzippedFileName));
      FileOutputStream out = new FileOutputStream(outputFileName);

      byte[] buffer = new byte[4096];
      int len;
      while ((len = in.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }

      in.close();
      out.close();

      deleteFile(gzippedFileName);

    } catch (IOException e) {
      System.err.println("IOException in PROCore.gunzipFile(String,String): " + e.getMessage());
      System.exit(99902);
    }
  }

  private String createUnifiedRefFile(String prefix, int numFiles) {
    if (numFiles < 2) {
      println("Warning: createUnifiedRefFile called with numFiles = " + numFiles + "; "
          + "doing nothing.", 1);
      return prefix;
    } else {
      File checker;
      checker = new File(prefix + "1");

      if (!checker.exists()) {
        checker = new File(prefix + ".1");
        if (!checker.exists()) {
          println("Can't find reference files.");
          System.exit(50);
        } else {
          prefix = prefix + ".";
        }
      }

      String outFileName;
      if (prefix.endsWith(".")) {
        outFileName = prefix + "all";
      } else {
        outFileName = prefix + ".all";
      }

      try {
        PrintWriter outFile = new PrintWriter(outFileName);

        BufferedReader[] inFile = new BufferedReader[numFiles];

        int nextIndex;
        checker = new File(prefix + "0");
        if (checker.exists()) {
          nextIndex = 0;
        } else {
          nextIndex = 1;
        }
        int lineCount = countLines(prefix + nextIndex);

        for (int r = 0; r < numFiles; ++r) {
          if (countLines(prefix + nextIndex) != lineCount) {
            println("Line count mismatch in " + (prefix + nextIndex) + ".");
            System.exit(60);
          }
          InputStream inStream = new FileInputStream(new File(prefix + nextIndex));
          inFile[r] = new BufferedReader(new InputStreamReader(inStream, "utf8"));
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

        for (int r = 0; r < numFiles; ++r) {
          inFile[r].close();
        }
      } catch (FileNotFoundException e) {
        System.err.println("FileNotFoundException in PROCore.createUnifiedRefFile(String,int): "
            + e.getMessage());
        System.exit(99901);
      } catch (IOException e) {
        System.err.println("IOException in PROCore.createUnifiedRefFile(String,int): "
            + e.getMessage());
        System.exit(99902);
      }

      return outFileName;

    }

  } // createUnifiedRefFile(String prefix, int numFiles)

  private String normalize(String str, int normMethod) {
    if (normMethod == 0)
      return str;

    // replace HTML/SGML
    str = str.replaceAll("&quot;", "\"");
    str = str.replaceAll("&amp;", "&");
    str = str.replaceAll("&lt;", "<");
    str = str.replaceAll("&gt;", ">");
    str = str.replaceAll("&apos;", "'");

    // split on these characters:
    // ! " # $ % & ( ) * + / : ; < = > ? @ [ \ ] ^ _ ` { | } ~
    // i.e. ASCII 33-126, except alphanumeric, and except "," "-" "." "'"

    // ! "# $%& ( ) * +/:;<=> ?@ [ \ ] ^_` { | }~
    String split_on = "!\"#\\$%&\\(\\)\\*\\+/:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~";

    // println("split_on: " + split_on);

    for (int k = 0; k < split_on.length(); ++k) {
      // for each split character, reprocess the string
      String regex = "" + split_on.charAt(k);
      if (regex.equals("\\")) {
        ++k;
        regex += split_on.charAt(k);
      }
      str = str.replaceAll(regex, " " + regex + " ");
    }

    // split on "." and "," and "-", conditioned on proper context

    str = " " + str + " ";
    str = str.replaceAll("\\s+", " ");

    TreeSet<Integer> splitIndices = new TreeSet<Integer>();

    for (int i = 0; i < str.length(); ++i) {
      char ch = str.charAt(i);
      if (ch == '.' || ch == ',') {
        // split if either of the previous or next characters is a non-digit
        char prev_ch = str.charAt(i - 1);
        char next_ch = str.charAt(i + 1);
        if (prev_ch < '0' || prev_ch > '9' || next_ch < '0' || next_ch > '9') {
          splitIndices.add(i);
        }
      } else if (ch == '-') {
        // split if preceded by a digit
        char prev_ch = str.charAt(i - 1);
        if (prev_ch >= '0' && prev_ch <= '9') {
          splitIndices.add(i);
        }
      }
    }

    String str0 = str;
    str = "";

    for (int i = 0; i < str0.length(); ++i) {
      if (splitIndices.contains(i)) {
        str += " " + str0.charAt(i) + " ";
      } else {
        str += str0.charAt(i);
      }
    }

    // rejoin i'm, we're, *'s, won't, don't, etc

    str = " " + str + " ";
    str = str.replaceAll("\\s+", " ");

    str = str.replaceAll(" i 'm ", " i'm ");
    str = str.replaceAll(" we 're ", " we're ");
    str = str.replaceAll(" 's ", "'s ");
    str = str.replaceAll(" 've ", "'ve ");
    str = str.replaceAll(" 'll ", "'ll ");
    str = str.replaceAll(" 'd ", "'d ");
    str = str.replaceAll(" n't ", "n't ");

    // remove spaces around dashes
    if (normMethod == 2 || normMethod == 4) {

      TreeSet<Integer> skipIndices = new TreeSet<Integer>();
      str = " " + str + " ";

      for (int i = 0; i < str.length(); ++i) {
        char ch = str.charAt(i);
        if (ch == '-') {
          // rejoin if surrounded by spaces, and then letters
          if (str.charAt(i - 1) == ' ' && str.charAt(i + 1) == ' ') {
            if (Character.isLetter(str.charAt(i - 2)) && Character.isLetter(str.charAt(i + 2))) {
              skipIndices.add(i - 1);
              skipIndices.add(i + 1);
            }
          }
        }
      }

      str0 = str;
      str = "";

      for (int i = 0; i < str0.length(); ++i) {
        if (!skipIndices.contains(i)) {
          str += str0.charAt(i);
        }
      }
    }

    // drop non-ASCII characters
    if (normMethod == 3 || normMethod == 4) {

      str0 = str;
      str = "";

      for (int i = 0; i < str0.length(); ++i) {
        char ch = str0.charAt(i);
        if (ch <= 127) { // i.e. if ASCII
          str += ch;
        }
      }
    }

    str = str.replaceAll("\\s+", " ");

    str = str.trim();

    return str;
  }

  private int countLines(String fileName) {
    int count = 0;

    try {
      BufferedReader inFile = new BufferedReader(new FileReader(fileName));

      String line;
      do {
        line = inFile.readLine();
        if (line != null)
          ++count;
      } while (line != null);

      inFile.close();
    } catch (IOException e) {
      System.err.println("IOException in PROCore.countLines(String): " + e.getMessage());
      System.exit(99902);
    }

    return count;
  }

  private int countNonEmptyLines(String fileName) {
    int count = 0;

    try {
      BufferedReader inFile = new BufferedReader(new FileReader(fileName));

      String line;
      do {
        line = inFile.readLine();
        if (line != null && line.length() > 0)
          ++count;
      } while (line != null);

      inFile.close();
    } catch (IOException e) {
      System.err.println("IOException in PROCore.countNonEmptyLines(String): " + e.getMessage());
      System.exit(99902);
    }

    return count;
  }

  private String fullPath(String dir, String fileName) {
    File dummyFile = new File(dir, fileName);
    return dummyFile.getAbsolutePath();
  }

  @SuppressWarnings("unused")
  private void cleanupMemory() {
    cleanupMemory(100, false);
  }

  @SuppressWarnings("unused")
  private void cleanupMemorySilently() {
    cleanupMemory(100, true);
  }

  @SuppressWarnings("static-access")
  private void cleanupMemory(int reps, boolean silent) {
    int bytesPerMB = 1024 * 1024;

    long totalMemBefore = myRuntime.totalMemory();
    long freeMemBefore = myRuntime.freeMemory();
    long usedMemBefore = totalMemBefore - freeMemBefore;

    long usedCurr = usedMemBefore;
    long usedPrev = usedCurr;

    // perform garbage collection repeatedly, until there is no decrease in
    // the amount of used memory
    for (int i = 1; i <= reps; ++i) {
      myRuntime.runFinalization();
      myRuntime.gc();
      (Thread.currentThread()).yield();

      usedPrev = usedCurr;
      usedCurr = myRuntime.totalMemory() - myRuntime.freeMemory();

      if (usedCurr == usedPrev)
        break;
    }

    if (!silent) {
      long totalMemAfter = myRuntime.totalMemory();
      long freeMemAfter = myRuntime.freeMemory();
      long usedMemAfter = totalMemAfter - freeMemAfter;

      println("GC: d_used = " + ((usedMemAfter - usedMemBefore) / bytesPerMB) + " MB "
          + "(d_tot = " + ((totalMemAfter - totalMemBefore) / bytesPerMB) + " MB).", 2);
    }
  }

  @SuppressWarnings("unused")
  private void printMemoryUsage() {
    int bytesPerMB = 1024 * 1024;
    long totalMem = myRuntime.totalMemory();
    long freeMem = myRuntime.freeMemory();
    long usedMem = totalMem - freeMem;

    println("Allocated memory: " + (totalMem / bytesPerMB) + " MB " + "(of which "
        + (usedMem / bytesPerMB) + " MB is being used).", 2);
  }

  private void println(Object obj, int priority) {
    if (priority <= verbosity)
      println(obj);
  }

  private void print(Object obj, int priority) {
    if (priority <= verbosity)
      print(obj);
  }

  private void println(Object obj) {
    System.out.println(obj);
  }

  private void print(Object obj) {
    System.out.print(obj);
  }

  @SuppressWarnings("unused")
  private void showProgress() {
    ++progress;
    if (progress % 100000 == 0)
      print(".", 2);
  }

  private ArrayList<Double> randomLambda() {
    ArrayList<Double> retLambda = new ArrayList<Double>(1 + numParams);

    for (int c = 1; c <= numParams; ++c) {
      if (isOptimizable[c]) {
        double randVal = randGen.nextDouble(); // number in [0.0,1.0]
        ++generatedRands;
        randVal = randVal * (maxRandValue[c] - minRandValue[c]); // number in [0.0,max-min]
        randVal = minRandValue[c] + randVal; // number in [min,max]
        retLambda.set(c, randVal);
      } else {
        retLambda.set(c, defaultLambda[c]);
      }
    }

    return retLambda;
  }

  private double[] randomPerturbation(double[] origLambda, int i, double method, double param,
      double mult) {
    double sigma = 0.0;
    if (method == 1) {
      sigma = 1.0 / Math.pow(i, param);
    } else if (method == 2) {
      sigma = Math.exp(-param * i);
    } else if (method == 3) {
      sigma = Math.max(0.0, 1.0 - (i / param));
    }

    sigma = mult * sigma;

    double[] retLambda = new double[1 + numParams];

    for (int c = 1; c <= numParams; ++c) {
      if (isOptimizable[c]) {
        double randVal = 2 * randGen.nextDouble() - 1.0; // number in [-1.0,1.0]
        ++generatedRands;
        randVal = randVal * sigma; // number in [-sigma,sigma]
        randVal = randVal * origLambda[c]; // number in [-sigma*orig[c],sigma*orig[c]]
        randVal = randVal + origLambda[c]; // number in
                                           // [orig[c]-sigma*orig[c],orig[c]+sigma*orig[c]]
                                           // = [orig[c]*(1-sigma),orig[c]*(1+sigma)]
        retLambda[c] = randVal;
      } else {
        retLambda[c] = origLambda[c];
      }
    }

    return retLambda;
  }

  @SuppressWarnings("unused")
  private HashSet<Integer> indicesToDiscard(double[] slope, double[] offset) {
    // some lines can be eliminated: the ones that have a lower offset
    // than some other line with the same slope.
    // That is, for any k1 and k2:
    // if slope[k1] = slope[k2] and offset[k1] > offset[k2],
    // then k2 can be eliminated.
    // (This is actually important to do as it eliminates a bug.)
    // print("discarding: ",4);

    int numCandidates = slope.length;
    HashSet<Integer> discardedIndices = new HashSet<Integer>();
    HashMap<Double, Integer> indicesOfSlopes = new HashMap<Double, Integer>();
    // maps slope to index of best candidate that has that slope.
    // ("best" as in the one with the highest offset)

    for (int k1 = 0; k1 < numCandidates; ++k1) {
      double currSlope = slope[k1];
      if (!indicesOfSlopes.containsKey(currSlope)) {
        indicesOfSlopes.put(currSlope, k1);
      } else {
        int existingIndex = indicesOfSlopes.get(currSlope);
        if (offset[existingIndex] > offset[k1]) {
          discardedIndices.add(k1);
          // print(k1 + " ",4);
        } else if (offset[k1] > offset[existingIndex]) {
          indicesOfSlopes.put(currSlope, k1);
          discardedIndices.add(existingIndex);
          // print(existingIndex + " ",4);
        }
      }
    }

    // old way of doing it; takes quadratic time (vs. linear time above)
    /*
     * for (int k1 = 0; k1 < numCandidates; ++k1) { for (int k2 = 0; k2 < numCandidates; ++k2) { if
     * (k1 != k2 && slope[k1] == slope[k2] && offset[k1] > offset[k2]) { discardedIndices.add(k2);
     * // print(k2 + " ",4); } } }
     */

    // println("",4);
    return discardedIndices;
  } // indicesToDiscard(double[] slope, double[] offset)
}
