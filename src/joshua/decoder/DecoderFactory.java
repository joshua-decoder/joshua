package joshua.decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.Sentence;

/**
 * this class implements: (1) parallel decoding: split the test file, initiate DecoderThread, wait
 * and merge the decoding results (2) non-parallel decoding is a special case of parallel decoding
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class DecoderFactory {
  private List<GrammarFactory> grammarFactories = null;
  private List<FeatureFunction> featureFunctions = null;
  private FeatureVector weights = null;
  private List<StateComputer> stateComputers;

  private Thread[] decoderThreads;

  private static final Logger logger = Logger.getLogger(DecoderFactory.class.getName());

  private List<Translation> translations;

  public DecoderFactory(List<GrammarFactory> grammarFactories,
      List<FeatureFunction> featureFunctions, FeatureVector weights,
      List<StateComputer> stateComputers) {
    this.grammarFactories = grammarFactories;
    this.featureFunctions = featureFunctions;
    this.weights = weights;
    this.stateComputers = stateComputers;
    this.translations = new ArrayList<Translation>();
  }


  /**
   * This is the public-facing method to decode a set of sentences. This automatically detects
   * whether we should run the decoder in parallel or not.
   * 
   * (Matt Post, August 2011) This needs to be rewritten. The proper way to do it is to put all the
   * sentences in a queue or wrap access to them in a thread-safe class. Then start the decoder
   * threads. Each thread obtains the sentece to decode and deposits it somewhere. Deposits are then
   * accumulated and output sequentially.
   */
  public void decodeTestSet(String testFile, String nbestFile, String oracleFile) {

    // create the input manager
    InputHandler inputHandler = new InputHandler(testFile, oracleFile);
    if (JoshuaConfiguration.parse)
      this.decoderThreads = new ParserThread[JoshuaConfiguration.num_parallel_decoders];
    else
      this.decoderThreads = new DecoderThread[JoshuaConfiguration.num_parallel_decoders];

    for (int threadno = 0; threadno < decoderThreads.length; threadno++) {
      try {
        Thread thread;
        if (JoshuaConfiguration.parse) {
          thread =
              new ParserThread(this.grammarFactories, this.weights, this.featureFunctions,
                  this.stateComputers, inputHandler);
        } else {
          thread =
              new DecoderThread(this.grammarFactories, this.weights, this.featureFunctions,
                  this.stateComputers, inputHandler);
        }

        this.decoderThreads[threadno] = thread;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // start them all
    for (int threadno = 0; threadno < decoderThreads.length; threadno++) {
      this.decoderThreads[threadno].start();
    }


    // wait for them to complete
    for (int threadno = 0; threadno < decoderThreads.length; threadno++) {
      if (JoshuaConfiguration.parse) {
        ParserThread thread = (ParserThread) this.decoderThreads[threadno];
        try {
          thread.join();
        } catch (InterruptedException e) {
          if (logger.isLoggable(Level.WARNING))
            logger.warning("thread " + threadno + " was interupted");
        }
      } else {
        DecoderThread thread = (DecoderThread) this.decoderThreads[threadno];
        try {
          thread.join();
          for (Translation tr : thread.getTranslations()) {
            this.translations.add(tr);
          }
        } catch (InterruptedException e) {
          if (logger.isLoggable(Level.WARNING))
            logger.warning("thread " + threadno + " was interupted");
        }
      }
    }
  }

  /**
   * Decode a single sentence and return its hypergraph.
   **/
  public HyperGraph getHyperGraphForSentence(String sentence) {
    try {
      DecoderThread decoder =
          new DecoderThread(this.grammarFactories, this.weights, this.featureFunctions,
              this.stateComputers, null);
      return decoder.translate(new Sentence(sentence, 0), null);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * @return the translations
   */
  public List<Translation> getTranslations() {
    return translations;
  }

}
