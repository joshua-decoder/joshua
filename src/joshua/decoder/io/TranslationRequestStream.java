package joshua.decoder.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import com.google.gson.stream.JsonReader;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.JoshuaConfiguration.INPUT_TYPE;
import joshua.decoder.MetaDataException;
import joshua.decoder.segment_file.Sentence;

/**
 * This class iterates over an input stream, looking for inputs to translate. By default, it
 * expects plain-text input, which can be plain sentences or PLF-encoded lattices. If
 * '-input-type json' is passed to the decoder, it will instead read JSON objects from the input
 * stream, with the following format:
 * 
 * {
 *   "data": {
 *     "translations": [
 *       { "sourceText": "sentence to be translated" },
 *       { "sourceText": "next sentence" },
 *       { "sourceText": "@some command to run" }
 *     ]
 *   }
 * }
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author orluke
 */
public class TranslationRequestStream {
  private final JoshuaConfiguration joshuaConfiguration;
  private int sentenceNo = -1;

  private Sentence nextSentence = null;

  /* Plain text or JSON input */ 
  private StreamHandler requestHandler = null;

  /* Whether the request has been killed by a broken client connection. */
  private boolean isShutDown = false;

  public TranslationRequestStream(BufferedReader reader, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    
    if (joshuaConfiguration.input_type == INPUT_TYPE.json) {
      this.requestHandler = new JSONStreamHandler(reader);
    } else {
      this.requestHandler = new PlaintextStreamHandler(reader);
    }
  }

  private interface StreamHandler {
    Sentence next() throws IOException, MetaDataException;
  }
  
  private class JSONStreamHandler implements StreamHandler {

    private JsonReader reader = null;
    private String line = null;
    
    public JSONStreamHandler(Reader in) {
      reader = new JsonReader(in);
      try {
        reader.beginObject();
        reader.nextName(); // "data"
        reader.beginObject();
        reader.nextName(); // "translations"
        reader.beginArray();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    @Override
    public Sentence next() throws IOException, MetaDataException {
      line = null;

      if (reader.hasNext()) {
        reader.beginObject();
        reader.nextName();
        line = reader.nextString();
        reader.endObject();
      }

      if (line == null)
        return null;

      if (line.startsWith("@"))
        throw new MetaDataException(line);

      return new Sentence(line, -1, joshuaConfiguration);
    }
  }
  
  private class PlaintextStreamHandler implements StreamHandler {

    private BufferedReader reader = null;
    
    public PlaintextStreamHandler(BufferedReader in) {
      reader = in;
    }
    
    @Override
    public Sentence next() throws IOException, MetaDataException {
      
      String line = reader.readLine();

      if (line != null) {
        if (line.startsWith("@"))
          throw new MetaDataException(line);

        return new Sentence(line, sentenceNo, joshuaConfiguration);
      }
      
      return null;
    }
  }
  
  public int size() {
    return sentenceNo + 1;
  }

  /*
   * Returns the next sentence item, then sets it to null, so that hasNext() will know to produce a
   * new one.
   */
  public synchronized Sentence next() throws MetaDataException {
    nextSentence = null;
    
    if (isShutDown)
      return null;
    
    try {
      nextSentence = requestHandler.next();
      if (nextSentence != null) {
        sentenceNo++;
        nextSentence.id = sentenceNo;
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
  
  public boolean isShutDown() {
    return isShutDown;
  }
}
