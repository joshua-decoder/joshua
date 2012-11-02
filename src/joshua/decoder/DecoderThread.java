package joshua.decoder;

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
import joshua.decoder.hypergraph.ForestWalker;
import joshua.decoder.hypergraph.GrammarBuilderWalkerFunction;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.Sentence;

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

  private static final Logger logger = Logger.getLogger(DecoderThread.class.getName());

  // ===============================================================
  // Constructor
  // ===============================================================
  public DecoderThread(List<GrammarFactory> grammarFactories, FeatureVector weights,
      List<FeatureFunction> featureFunctions, List<StateComputer> stateComputers)
          throws IOException {

    this.grammarFactories = grammarFactories;
    this.stateComputers = stateComputers;

    this.featureFunctions = new ArrayList<FeatureFunction>();
    for (FeatureFunction ff : featureFunctions) {
      if (ff instanceof SourceDependentFF) {
        this.featureFunctions.add(((SourceDependentFF) ff).clone());
      } else {
        this.featureFunctions.add(ff);
      }
    }
  }

  // ===============================================================
  // Methods
  // ===============================================================

  @Override
  public void run() {
    // Nothing to do but wait.
  }

  /**
   * Translate a sentence.
   * 
   * @param sentence The sentence to be translated.
   */
  public Translation translate(Sentence sentence) {

    logger.info("Translating sentence #" + sentence.id() + " [thread " + getId() + "]\n"
        + sentence.source());
    if (sentence.target() != null)
      logger.info("Constraining to target sentence '" + sentence.target() + "'");

    // skip blank sentences
    if (sentence.isEmpty()) {
      logger.info("translation of sentence " + sentence.id() + " took 0 seconds [" + getId() + "]");
      return null;
    }

    long startTime = System.currentTimeMillis();

    int numGrammars = grammarFactories.size();
    Grammar[] grammars = new Grammar[numGrammars];

    for (int i = 0; i < grammarFactories.size(); i++)
      grammars[i] = grammarFactories.get(i).getGrammarForSentence(sentence);

    /* Seeding: the chart only sees the grammars, not the factories */
    Chart chart = new Chart(sentence, this.featureFunctions, this.stateComputers, grammars,
        JoshuaConfiguration.goal_symbol);

    /* Parsing */
    HyperGraph hypergraph = chart.expand();

    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;
    logger.info(String.format("translation of sentence %d took %.3f seconds [thread %d]",
        sentence.id(), seconds, getId()));

    if (!JoshuaConfiguration.parse || hypergraph == null) {
      return new Translation(sentence, hypergraph, featureFunctions);
    }

    /*
     * Synchronous parsing.
     * 
     * Step 1. Traverse the hypergraph to create a grammar for the second-pass parse.
     */
    Grammar newGrammar = getGrammarFromHyperGraph(JoshuaConfiguration.goal_symbol, hypergraph);
    newGrammar.sortGrammar(this.featureFunctions);
    long sortTime = System.currentTimeMillis();
    logger.info(String.format("New grammar has %d rules.\n", newGrammar.getNumRules()));

    /* Step 2. Create a new chart and parse with the instantiated grammar. */
    Grammar[] newGrammarArray = new Grammar[] { newGrammar };
    Sentence targetSentence = new Sentence(sentence.target(), sentence.id());
    chart = new Chart(targetSentence, featureFunctions, stateComputers, newGrammarArray, "GOAL");
    int goalSymbol = GrammarBuilderWalkerFunction.goalSymbol(hypergraph);
    logger.info(String.format("goal symbol is %d.\n", goalSymbol));
    chart.setGoalSymbolID(goalSymbol);

    /* Parsing */
    HyperGraph englishParse = chart.expand();
    long secondParseTime = System.currentTimeMillis();
    logger.info(String.format("Finished second chart expansion (%d seconds).\n",
        (secondParseTime - sortTime) / 1000));
    logger.info(String.format("Total time: %d seconds.\n", (secondParseTime - startTime) / 1000));

    return new Translation(sentence, englishParse, featureFunctions); // or do something else

  }

  private static Grammar getGrammarFromHyperGraph(String goal, HyperGraph hg) {
    GrammarBuilderWalkerFunction f = new GrammarBuilderWalkerFunction(goal);
    ForestWalker walker = new ForestWalker();
    walker.walk(hg.goalNode, f);
    return f.getGrammar();
  }
}
