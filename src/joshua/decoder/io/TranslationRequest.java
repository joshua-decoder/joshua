/** 
 * 
 */
package joshua.decoder.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;

import joshua.decoder.segment_file.LatticeInput;
import joshua.decoder.segment_file.ParsedSentence;
import joshua.decoder.segment_file.Sentence;

/**
 * @author Matt Post <post@cs.jhu.edu>
 * @author orluke
 *
 */
public class TranslationRequest implements Iterator<Sentence> {

  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");
    
  private BufferedReader reader = null;
  private BufferedWriter writer = null;
  
  private int sentenceNo = -1;
  
  private Sentence nextSentence = null;
  
  public TranslationRequest(InputStream in, OutputStream out) {
    reader = new BufferedReader(new InputStreamReader(in, FILE_ENCODING));
    writer = new BufferedWriter(new OutputStreamWriter(out, FILE_ENCODING));

    prepareNextLine();
  }
    
  /**
   * This is called only from (a) the constructor and (b) the next() function. Since the Constructor
   * is called only once, and the call to prepareNextLine() in next() happens within a lock, this
   * function does not require synchronization.
   */
  private void prepareNextLine() {
    try {
      String line = reader.readLine();
      sentenceNo++;
      if (line == null) {
        nextSentence = null;
      } else {
        // TODO: This should be replace with a single Input object type that knows about all kinds of expected inputs
        if (line.replaceAll("\\s", "").startsWith("(((")) {
          nextSentence = new LatticeInput(line, sentenceNo);
        } else if (ParsedSentence.matches(line)) {
          nextSentence = new ParsedSentence(line, sentenceNo);
        } else {
          nextSentence = new Sentence(line, sentenceNo);
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized boolean hasNext() {
    return nextSentence != null;
  }

  /*
   * Returns the next sentence item.
   */
  public synchronized Sentence next() {
    Sentence currentSentence = this.nextSentence;
    prepareNextLine();
    return currentSentence;
  }

  public void remove() {
    // unimplemented
  }
}
