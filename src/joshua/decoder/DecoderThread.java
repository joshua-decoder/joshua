package joshua.decoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.SourceDependentFF;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.hypergraph.ForestWalker;
import joshua.decoder.hypergraph.GrammarBuilderWalkerFunction;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.Sentence;
import joshua.corpus.Vocabulary;

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

public class DecoderThread extends Thread {
  private final JoshuaConfiguration joshuaConfiguration;
  /*
   * these variables may be the same across all threads (e.g., just copy from DecoderFactory), or
   * differ from thread to thread
   */
  private final List<GrammarFactory> grammarFactories;
  private final List<FeatureFunction> featureFunctions;

  private static final Logger logger = Logger.getLogger(DecoderThread.class.getName());

  // ===============================================================
  // Constructor
  // ===============================================================
  public DecoderThread(List<GrammarFactory> grammarFactories, FeatureVector weights,
      List<FeatureFunction> featureFunctions,JoshuaConfiguration joshuaConfiguration) throws IOException {

    this.joshuaConfiguration = joshuaConfiguration;
    this.grammarFactories = grammarFactories;

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

    logger.info(String.format("Translating sentence #%d [thread %d]: '%s'", sentence.id(), getId(),
        sentence.source()));

    if (sentence.target() != null)
      logger.info("Constraining to target sentence '" + sentence.target() + "'");

    // skip blank sentences
    if (sentence.isEmpty()) {
      logger.info("translation of sentence " + sentence.id() + " took 0 seconds [" + getId() + "]");
      return new Translation(sentence, null, featureFunctions, joshuaConfiguration);
    }
    
    long startTime = System.currentTimeMillis();

    int numGrammars = grammarFactories.size();
    Grammar[] grammars = new Grammar[numGrammars];

    for (int i = 0; i < grammarFactories.size(); i++)
      grammars[i] = grammarFactories.get(i).getGrammarForSentence(sentence);
    
    if (joshuaConfiguration.segment_oovs)
      sentence.segmentOOVs(grammars);

    /* Seeding: the chart only sees the grammars, not the factories */
    Chart chart = new Chart(sentence, this.featureFunctions, grammars,
        joshuaConfiguration.goal_symbol, joshuaConfiguration);

    /* Parsing */
    HyperGraph hypergraph = null;
    try {
      hypergraph = chart.expand();
    } catch (java.lang.OutOfMemoryError e) {
      logger.warning(String.format("sentence %d: out of memory", sentence.id()));
      hypergraph = null;
    }

    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;
    logger.info(String.format("translation of sentence %d took %.3f seconds [thread %d]",
        sentence.id(), seconds, getId()));
    logger.info(String.format("Memory used after sentence %d is %.1f MB", sentence.id(), (Runtime
        .getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0));

    /* Return the translation unless we're doing synchronous parsing. */
    if (!joshuaConfiguration.parse || hypergraph == null) {
      return new Translation(sentence, hypergraph, featureFunctions, joshuaConfiguration);
    }

    /*****************************************************************************************/
    
    /*
     * Synchronous parsing.
     * 
     * Step 1. Traverse the hypergraph to create a grammar for the second-pass parse.
     */
    Grammar newGrammar = getGrammarFromHyperGraph(joshuaConfiguration.goal_symbol, hypergraph);
    newGrammar.sortGrammar(this.featureFunctions);
    long sortTime = System.currentTimeMillis();
    logger.info(String.format("Sentence %d: New grammar has %d rules.", sentence.id(),
        newGrammar.getNumRules()));

    /* Step 2. Create a new chart and parse with the instantiated grammar. */
    Grammar[] newGrammarArray = new Grammar[] { newGrammar };
    Sentence targetSentence = new Sentence(sentence.target(), sentence.id(), joshuaConfiguration);
    chart = new Chart(targetSentence, featureFunctions, newGrammarArray, "GOAL",joshuaConfiguration);
    int goalSymbol = GrammarBuilderWalkerFunction.goalSymbol(hypergraph);
    String goalSymbolString = Vocabulary.word(goalSymbol);
    logger.info(String.format("Sentence %d: goal symbol is %s (%d).", sentence.id(),
        goalSymbolString, goalSymbol));
    chart.setGoalSymbolID(goalSymbol);

    /* Parsing */
    HyperGraph englishParse = chart.expand();
    long secondParseTime = System.currentTimeMillis();
    logger.info(String.format("Sentence %d: Finished second chart expansion (%d seconds).",
        sentence.id(), (secondParseTime - sortTime) / 1000));
    logger.info(String.format("Sentence %d total time: %d seconds.\n", sentence.id(),
        (secondParseTime - startTime) / 1000));
    logger.info(String.format("Memory used after sentence %d is %.1f MB", sentence.id(), (Runtime
        .getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0));

    return new Translation(sentence, englishParse, featureFunctions, joshuaConfiguration); // or do something else
  }

  private Grammar getGrammarFromHyperGraph(String goal, HyperGraph hg) {
    GrammarBuilderWalkerFunction f = new GrammarBuilderWalkerFunction(goal,joshuaConfiguration);
    ForestWalker walker = new ForestWalker();
    walker.walk(hg.goalNode, f);
    return f.getGrammar();
  }
}
