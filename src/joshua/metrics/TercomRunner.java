package joshua.metrics;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import joshua.util.StreamGobbler;


public class TercomRunner implements Runnable {
  /* non-static data members */
  private Semaphore blocker;

  private String refFileName;
  private String hypFileName;
  private String outFileNamePrefix;
  private int memSize;

  /* static data members */
  private static boolean caseSensitive;
  private static boolean withPunctuation;
  private static int beamWidth;
  private static int maxShiftDist;
  private static String tercomJarFileName;

  public static void set_TercomParams(boolean in_caseSensitive, boolean in_withPunctuation,
      int in_beamWidth, int in_maxShiftDist, String in_tercomJarFileName) {
    caseSensitive = in_caseSensitive;
    withPunctuation = in_withPunctuation;
    beamWidth = in_beamWidth;
    maxShiftDist = in_maxShiftDist;
    tercomJarFileName = in_tercomJarFileName;
  }

  public TercomRunner(Semaphore in_blocker, String in_refFileName, String in_hypFileName,
      String in_outFileNamePrefix, int in_memSize) {
    blocker = in_blocker;
    refFileName = in_refFileName;
    hypFileName = in_hypFileName;
    outFileNamePrefix = in_outFileNamePrefix;
    memSize = in_memSize;
  }

  private void real_run() {

    try {

      String cmd_str =
          "java -Xmx" + memSize + "m -Dfile.encoding=utf8 -jar " + tercomJarFileName + " -r "
              + refFileName + " -h " + hypFileName + " -o ter -n " + outFileNamePrefix;
      cmd_str += " -b " + beamWidth;
      cmd_str += " -d " + maxShiftDist;
      if (caseSensitive) {
        cmd_str += " -s";
      }
      if (!withPunctuation) {
        cmd_str += " -P";
      }
      /*
       * From tercom's README: -s case sensitivity, optional, default is insensitive -P no
       * punctuation, default is with punctuation.
       */

      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd_str);

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), 0);
      StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), 0);

      errorGobbler.start();
      outputGobbler.start();

      p.waitFor();

      File fd;
      fd = new File(hypFileName);
      if (fd.exists()) fd.delete();
      fd = new File(refFileName);
      if (fd.exists()) fd.delete();

    } catch (IOException e) {
      System.err.println("IOException in TER.runTercom(...): " + e.getMessage());
      System.exit(99902);
    } catch (InterruptedException e) {
      System.err.println("InterruptedException in TER.runTercom(...): " + e.getMessage());
      System.exit(99903);
    }

    blocker.release();

  }

  public void run() {
    try {
      real_run();
    } catch (Exception e) {
      System.err.println("Exception in TercomRunner.run(): " + e.getMessage());
      System.exit(99905);
    }
  }

}
