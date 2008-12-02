package joshua.MERT;
import java.util.*;
import java.io.*;

public class MERT_runner
{
  public static void main(String[] args) throws Exception
  {
    MERT myMERT = null;

    if (args.length == 0) {
      printUsage(args.length);
      System.exit(0);
    } else if (args.length == 1) {
      myMERT = new MERT(args[0]);
    } else {
      myMERT = new MERT(args);
    }

    myMERT.run_MERT();
      // optimize lambda[]!!!

    myMERT.finish();

    System.exit(0);

  } // main(String[] args)


  private static void printUsage(int argsLen)
  {
    println("Oops, you provided " + argsLen + " args!");
    println("");
    println("Usage:");
    println(" MERT_runner MERT_configFile");
    println("");
    println("   OR:");
    println("");
    println(" MERT_runner [-dir dirPrefix] [-s sourceFile] [-r refFile] [-rps refsPerSen]\n      [-p paramsFile] [-fin finalLambda] [-m metricName] [-maxIt maxMERTIts]\n      [-save saveInter] [-ipi initsPerIt] [-opi oncePerIt] [-rand randInit]\n      [-seed seed] [-ud useDisk] [-cmd commandFile] [-decOut decoderOutFile]\n      [-decExit validExit] [-dcfg decConfigFile] [-N N]\n      [-xx xxx] [-v verbosity] [-decV decVerbosity]");
    println("");
    println("Relevant files:");
    println(" (*) -dir dirPrefix: location of relevant files\n       [[default: null string (i.e. they are in the current directory)]]");
    println(" (*) -s sourceFile: source sentences (foreign sentences) of the MERT dataset\n       [[default: null string (i.e. file name is not needed by MERT)]]");
    println(" (*) -r refFile: target sentences (reference translations) of the MERT dataset\n       [[default: reference.txt]]");
    println(" (*) -rps refsPerSen: number of reference translations per sentence\n       [[default: 1]]");
    println(" (*) -p paramsFile: file containing parameter names, initial values, and ranges\n       [[default: params.txt]]");
    println(" (*) -fin finalLambda: file name for final lambda[] values\n       [[default: null string (i.e. no such file is created)]]");
    println("");
    println("MERT specs:");
    println(" (*) -m metricName: name of the evaluation metric optimized by MERT\n       [[default: BLEU]]");
    println(" (*) -maxIt maxMERTIts: maximum number of MERT iterations\n       [[default: 20]]");
    println(" (*) -save saveInter: save intermediate cfg files (1) or decoder outputs (2)\n       or both (3) or neither (0)\n       [[default: 3]]");
    println(" (*) -ipi initsPerIt: number of intermediate initial points per iteration\n       [[default: 20]]");
    println(" (*) -opi oncePerIt: modify a parameter only once per iteration (1) or not (0)\n       [[default: 0]]");
    println(" (*) -rand randInit: choose initial point randomly (1) or from paramsFile (0)\n       [[default: 0]]");
    println(" (*) -seed seed: seed used to initialize random number generator\n       [[default: time (i.e. value returned by System.currentTimeMillis()]]");
    println(" (*) -ud useDisk: reliance on disk (0-2; higher value => more reliance)\n       [[default: 2]]");
    println("");
    println("Decoder specs:");
    println(" (*) -cmd commandFile: name of file containing command to run the decoder\n       [[default: null string (i.e. decoder is a JoshuaDecoder object)]]");
    println(" (*) -decOut decoderOutFile: name of the output file produced by your decoder\n       [[default: output.nbest]]");
    println(" (*) -decExit validExit: value returned by decoder to indicate success\n       [[default: 0]]");
    println(" (*) -dcfg decConfigFile: name of decoder config file\n       [[default: config_file.txt]]");
    println(" (*) -N N: size of N-best list (per sentence) generated in each MERT iteration\n       [[default: 100]]");
    println("");
    println("Output specs:");
    println(" (*) -v verbosity: MERT verbosity level (0-4; higher value => more verbose)\n       [[default: 1]]");
    println(" (*) -decV decVerbosity: should decoder output be printed (1) or ignored (0)\n       [[default: 0]]");
    println("");
    println("Ex.: java MERT_runner MERT_config.txt");
    println("             OR:");
    println("     java MERT_runner -s DEV07_es.txt -r DEV07_en.txt -rps 4 -init initFile.txt -N 500 -maxIt 50 -v 0");
  }

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

  private static void println(Object obj) { System.out.println(obj); }
  private static void print(Object obj) { System.out.print(obj); }

}
