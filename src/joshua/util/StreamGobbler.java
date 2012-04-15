package joshua.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Based on: http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
 */
public class StreamGobbler extends Thread {
  InputStream istream;
  boolean verbose;

  public StreamGobbler(InputStream is, int p) {
    istream = is;
    verbose = (p != 0);
  }

  public void run() {
    try {
      InputStreamReader isreader = new InputStreamReader(istream);
      BufferedReader br = new BufferedReader(isreader);
      String line = null;
      while ((line = br.readLine()) != null) {
        if (verbose) System.out.println(line);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
