package joshua.decoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.SourceDependentFF;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.segment_file.Sentence;
import joshua.oracle.OracleExtractor;

/**
 * This class handles decoding of individual Sentence objects (which can represent plain sentences
 * or lattices). A single sentence can be decoded by a call to translate() and, if an InputHandler
 * is used, many sentences can be decoded in a thread-safe manner via a single call to
 * translateAll(), which continually queries the InputHandler for sentences until they have all been
 * consumed and translated.
 * 
 * The DecoderFactory class is responsible for launching the threads.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
// BUG: known synchronization problem: LM cache; srilm call;
public class DecoderThread extends Thread {
  /*
   * these variables may be the same across all threads (e.g., just copy from DecoderFactory), or
   * differ from thread to thread
   */
  private final List<GrammarFactory> grammarFactories;
  private final List<FeatureFunction> featureFunctions;
  private final List<StateComputer> stateComputers;

  // more test set specific
  private final InputHandler inputHandler;
  // final String nbestFile; // package-private for DecoderFactory
  private BufferedWriter nbestWriter; // set in decodeTestFile
  private final KBestExtractor kbestExtractor;

  private static final Logger logger = Logger.getLogger(DecoderThread.class.getName());

  // ===============================================================
  // Constructor
  // ===============================================================
  public DecoderThread(List<GrammarFactory> grammarFactories, FeatureVector weights,
    List<FeatureFunction> featureFunctions, List<StateComputer> stateComputers,
    InputHandler inputHandler) throws IOException {

    this.grammarFactories = grammarFactories;
    this.stateComputers = stateComputers;
    this.inputHandler = inputHandler;

    this.featureFunctions = new ArrayList<FeatureFunction>();
    for (FeatureFunction ff : featureFunctions) {
      if (ff instanceof SourceDependentFF) {
        this.featureFunctions.add(((SourceDependentFF) ff).clone());
      } else {
        this.featureFunctions.add(ff);
      }
    }

    this.kbestExtractor =
      new KBestExtractor(weights, JoshuaConfiguration.use_unique_nbest,
        JoshuaConfiguration.use_tree_nbest, JoshuaConfiguration.include_align_index,
        JoshuaConfiguration.add_combined_cost, false, false);
  }


  // ===============================================================
  // Methods
  // ===============================================================
  // Overriding of Thread.run() cannot throw anything
  public void run() {
    try {
      this.translateAll();
      // this.hypergraphSerializer.closeReaders();
    } catch (Throwable e) {
      // if we throw anything (e.g. OutOfMemoryError)
      // we should stop all threads
      // because it is impossible for decoding
      // to finish successfully
      e.printStackTrace();
      System.exit(1);
    }
  }


  /**
   * Repeatedly fetches input sentences and calls translate() on them, registering the results with
   * the InputManager upon completion.
   */
  public void translateAll() throws IOException {

    for (;;) {

      Sentence sentence = inputHandler.next();
      if (sentence == null) break;

      HyperGraph hypergraph = translate(sentence, null);
      Translation translation = null;

      // if (JoshuaConfiguration.visualize_hypergraph) {
      //   HyperGraphViewer.visualizeHypergraphInFrame(hypergraph);
      // }

      String oracleSentence = inputHandler.oracleSentence(sentence.id());
      if (!sentence.isEmpty() && oracleSentence != null) {
        OracleExtractor extractor = new OracleExtractor();
        HyperGraph oracle =
            extractor.getOracle(hypergraph, JoshuaConfiguration.lm_order, oracleSentence);

        translation = new Translation(sentence, oracle, featureFunctions);

      } else {

        translation = new Translation(sentence, hypergraph, featureFunctions);

        // if (null != this.hypergraphSerializer) {
        // if(JoshuaConfiguration.use_kbest_hg){
        // HyperGraph kbestHG = this.kbestExtractor.extractKbestIntoHyperGraph(hypergraph,
        // JoshuaConfiguration.topN);
        // this.hypergraphSerializer.saveHyperGraph(kbestHG);
        // }else{
        // this.hypergraphSerializer.saveHyperGraph(hypergraph);
        // }
        // }

      }

      inputHandler.register(translation);

      /*
       * //debug if (JoshuaConfiguration.use_variational_decoding) { ConstituentVariationalDecoder
       * vd = new ConstituentVariationalDecoder(); vd.decoding(hypergraph);
       * System.out.println("#### new 1best is #####\n" +
       * HyperGraph.extract_best_string(p_main_controller.p_symbol, hypergraph.goal_item)); } // end
       */

      // debug
      // g_con.get_confusion_in_hyper_graph_cell_specific(hypergraph, hypergraph.sent_len);

    }
  }


  /**
   * Translate a sentence.
   * 
   * @param sentence The sentence to be translated.
   * @param oracleSentence
   */
  public HyperGraph translate(Sentence sentence, String oracleSentence) throws IOException {

    logger.info("Translating sentence #" + sentence.id() + " [thread " + getId() + "]\n"
        + sentence.sentence());
    if (sentence.target() != null)
      logger.info("Contraining to target sentence '" + sentence.target() + "'");

    if (sentence.isEmpty()) 
      return null;

    long startTime = System.currentTimeMillis();

    // skip blank sentences
    if (sentence.sentence().matches("^\\s*$")) {
      logger.info("translation of sentence " + sentence.id() + " took 0 seconds [" + getId() + "]");
      return null;
    }

    int numGrammars =
        (JoshuaConfiguration.use_sent_specific_tm) ? grammarFactories.size() + 1 : grammarFactories
            .size();

    Grammar[] grammars = new Grammar[numGrammars];

    for (int i = 0; i < grammarFactories.size(); i++)
      grammars[i] = grammarFactories.get(i).getGrammarForSentence(sentence);

    /* Seeding: the chart only sees the grammars, not the factories */
    Chart chart = new Chart(sentence, this.featureFunctions, this.stateComputers, grammars, JoshuaConfiguration.goal_symbol);

    /* Parsing */
    HyperGraph hypergraph = chart.expand();

    float seconds = (float)(System.currentTimeMillis() - startTime) / 1000.0f;
    logger.info(String.format("translation of sentence %d took %.3f seconds [thread %d]", sentence.id(), seconds, getId()));

    return hypergraph;
  }
}
