package joshua.MERT;
import java.util.*;
import java.io.*;

public class MERT_runner
{
  public static void main(String[] args) throws Exception
  {
    boolean limitedMem = false;

    if (args.length == 1) {
      if (args[0].equals("-h")) {
        printUsage(args.length,true);
        System.exit(2);
      } else {
        limitedMem = false;
      }
    } else if (args.length == 2) {
      limitedMem = true;
    } else {
      printUsage(args.length,false);
      System.exit(1);
    }


    if (!limitedMem) {

      MERT myMERT = new MERT(args[0]);

      myMERT.run_MERT();
        // optimize lambda[]!!!

      myMERT.finish();

    } else {

      boolean done = false;
      int iteration = 0;
      while (!done) {
        ++iteration;

        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec("java -cp bin joshua.MERT.MERT " + args[0] + " " + args[1] + " " + iteration);

        BufferedReader br_i = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader br_e = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        String dummy_line = null;

        while ((dummy_line = br_i.readLine()) != null) {
          println(dummy_line);
        }
        while ((dummy_line = br_e.readLine()) != null) {
          println(dummy_line);
        }

        int status = p.waitFor();

        if (status == 90) {
          done = true;
        } else if (status == 91) {
          done = false;
        } else {
          println("ZMERT exiting prematurely (MERT returned " + status + ")...");
          break;
        }
      }

    }

    System.exit(0);


  } // main(String[] args)


  private static void printUsage(int argsLen, boolean detailed)
  {
//    println("Usage:");
//    println(" ZMERT MERT_configFile");
//    println("");
//    println("   OR:");
//    println("");
//    println(" ZMERT [-dir dirPrefix] [-s sourceFile] [-r refFile] [-rps refsPerSen]\n       [-p paramsFile] [-fin finalLambda] [-m metricName metric options]\n       [-prevIt prevMERTIts] [-maxIt maxMERTIts] [-minIt minMERTIts]\n       [-sig sigValue] [] [] [stopIt itCount]\n       [-save saveInter] [-ipi initsPerIt] [-opi oncePerIt] [-rand randInit]\n       [-seed seed] [-ud useDisk] [-cmd commandFile] [-decOut decoderOutFile]\n       [-decExit validExit] [-dcfg decConfigFile] [-N N]\n       [-xx xxx] [-v verbosity] [-decV decVerbosity]");
//    println("");
//    println("Ex.: java MERT_runner MERT_config.txt");
//    println("             OR:");
//    println("     java MERT_runner -s DEV07_es.txt -r DEV07_en.txt -rps 4 -init initFile.txt -N 500 -maxIt 50 -v 0");

    if (!detailed) {
      println("Oops, you provided " + argsLen + " args!");
      println("");
      println("Usage:");
      println("           ZMERT MERT_configFile");
      println("");
      println("Where the configuration file contains any subset of MERT's 20-some parameters,");
      println("one per line.  Run   ZMERT -h   for more details.");
    } else {
      println("Usage:");
      println("           ZMERT MERT_configFile");
      println("");
      println("Where the configuration file contains any subset of MERT's 20-some parameters,");
      println("one per line.  MERT's parameters, and their default values, are:");
      println("");
      println("Relevant files:");
      println("  -dir dirPrefix: working directory\n    [[default: null string (i.e. they are in the current directory)]]");
      println("  -s sourceFile: source sentences (foreign sentences) of the MERT dataset\n    [[default: null string (i.e. file name is not needed by MERT)]]");
      println("  -r refFile: target sentences (reference translations) of the MERT dataset\n    [[default: reference.txt]]");
      println("  -rps refsPerSen: number of reference translations per sentence\n    [[default: 1]]");
      println("  -p paramsFile: file containing parameter names, initial values, and ranges\n    [[default: params.txt]]");
      println("  -fin finalLambda: file name for final lambda[] values\n    [[default: null string (i.e. no such file will be created)]]");
      println("");
      println("MERT specs:");
      println("  -m metricName metric options: name of evaluation metric and its options\n    [[default: BLEU 4 closest]]");
      println("  -maxIt maxMERTIts: maximum number of MERT iterations\n    [[default: 20]]");
      println("  -minIt minMERTIts: number of iterations before considering an early exit\n    [[default: 5]]");
      println("  -prevIt prevMERTIts: maximum number of previous MERT iterations to\n    construct candidate sets from\n    [[default: 20]]");
      println("  -stopSig sigValue: early MERT exit if no weight changes by more than sigValue\n    [[default: -1 (i.e. this criterion is never investigated)]]");
      println("  -stopIt stopMinIts: some early stopping criterion must be satisfied in\n    stopMinIts *consecutive* iterations before an early exit\n    [[default: 3]]");
      println("  -save saveInter: save intermediate cfg files (1) or decoder outputs (2)\n    or both (3) or neither (0)\n    [[default: 3]]");
      println("  -ipi initsPerIt: number of intermediate initial points per iteration\n    [[default: 20]]");
      println("  -opi oncePerIt: modify a parameter only once per iteration (1) or not (0)\n    [[default: 0]]");
      println("  -rand randInit: choose initial point randomly (1) or from paramsFile (0)\n    [[default: 0]]");
      println("  -seed seed: seed used to initialize random number generator\n    [[default: time (i.e. value returned by System.currentTimeMillis()]]");
      println("  -ud useDisk: reliance on disk (0-2; higher value => more reliance)\n    [[default: 2]]");
      println("");
      println("Decoder specs:");
      println("  -cmd commandFile: name of file containing command to run the decoder\n    [[default: null string (i.e. decoder is a JoshuaDecoder object)]]");
      println("  -decOut decoderOutFile: name of the output file produced by your decoder\n    [[default: output.nbest]]");
      println("  -decExit validExit: value returned by decoder to indicate success\n    [[default: 0]]");
      println("  -dcfg decConfigFile: name of decoder config file\n    [[default: config_file.txt]]");
      println("  -N N: size of N-best list (per sentence) generated in each MERT iteration\n    [[default: 100]]");
      println("");
      println("Output specs:");
      println("  -v verbosity: MERT verbosity level (0-4; higher value => more verbose)\n    [[default: 1]]");
      println("  -decV decVerbosity: should decoder output be printed (1) or ignored (0)\n    [[default: 0]]");
      println("");
    }
  }

  private static void println(Object obj) { System.out.println(obj); }
  private static void print(Object obj) { System.out.print(obj); }

}
