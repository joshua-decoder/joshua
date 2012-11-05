package joshua.decoder.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;

import joshua.decoder.segment_file.LatticeInput;
import joshua.decoder.segment_file.ParsedSentence;
import joshua.decoder.segment_file.Sentence;

/**
 * This class is an iterator over inputs, which are turned into Sentence objects.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author orluke
 * 
 */
public class TranslationRequest implements Iterator<Sentence> {

  private static final Charset FILE_ENCODING = Charset.forName("UTF-8");

  private BufferedReader reader = null;

  private int sentenceNo = -1;

  private Sentence nextSentence = null;

  public TranslationRequest(InputStream in) {
    reader = new BufferedReader(new InputStreamReader(in, FILE_ENCODING));
  }

  public int size() {
    return sentenceNo + 1;
  }

  /**
   * Read the next line from the buffered reader (blocking if necessary). When the line becomes
   * available, turn it into a Sentence object.
   * 
   * N.B. When 'line' is a String with just whitespace, it is skipped.
   */
  private void prepareNextLine() throws NoSuchElementException {
    try {
      String line = reader.readLine();
      if (line == null || line.matches("\\s*")) {
        throw new NoSuchElementException();
      } else {
        sentenceNo++;

        // TODO: This should be replace with a single Input object type that knows about all kinds
        // of expected inputs
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

  /*
   * The logic behind this function is to prepare the next sentence. It checks if nextSentence is
   * null to support multiple calls to it, per the iterator contract.
   * 
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public synchronized boolean hasNext() {
    if (nextSentence == null) {
      try {
        prepareNextLine();
      } catch (NoSuchElementException e) {
        return false;
      }
    }
    return nextSentence != null;
  }

  /*
   * Returns the next sentence item, then sets it to null, so that hasNext() will know to produce a
   * new one.
   */
  @Override
  public synchronized Sentence next() throws NoSuchElementException {
    /* Check for null first, in case for some reason hasNext() was not called before next(). */
    if (nextSentence == null)
      prepareNextLine();
    Sentence sentence = nextSentence;
    nextSentence = null;
    return sentence;
  }

  @Override
  public void remove() {
    // unimplemented
  }
}
