package joshua.decoder.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.segment_file.Sentence;

/**
 * This class is an iterator over inputs, which are turned into Sentence objects.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author orluke
 * 
 */
public class TranslationRequest {
  private final JoshuaConfiguration joshuaConfiguration;
  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");

  private BufferedReader reader = null;

  private int sentenceNo = -1;

  private Sentence nextSentence = null;

  /* Whether the request has been killed by a broken client connection. */
  private boolean isShutDown = false;

  public TranslationRequest(InputStream in, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    reader = new BufferedReader(new InputStreamReader(in, FILE_ENCODING));
  }

  public int size() {
    return sentenceNo + 1;
  }

  /*
   * Returns the next sentence item, then sets it to null, so that hasNext() will know to produce a
   * new one.
   */
  public synchronized Sentence next() {
    nextSentence = null;
    
    if (isShutDown)
      return null;
    
    try {
      String line = reader.readLine();

      if (line != null) {
        sentenceNo++;
        nextSentence = new Sentence(line, sentenceNo, joshuaConfiguration);
//        } else if (ParsedSentence.matches(line)) {
//          nextSentence = new ParsedSentence(line, sentenceNo, joshuaConfiguration);
      }
    } catch (IOException e) {
      this.shutdown();
    }

    return nextSentence;
  }

  /**
   * When the client socket is interrupted, we need to shut things down. On the source side, the
   * TranslationRequest could easily have buffered a lot of lines and so will keep discovering
   * sentences to translate, but the output Translation objects will start throwing exceptions when
   * trying to print to the closed socket. When that happens, we call this function() so that we can
   * tell next() to stop returning translations, which in turn will cause it to stop asking for
   * them.
   * 
   * Note that we don't go to the trouble of shutting down existing DecoderThreads. This would be
   * good to do, but for the moment would require more bookkeeping than we want to do.
   */

  public void shutdown() {
    isShutDown = true;
  }
}
