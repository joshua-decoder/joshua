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
import joshua.decoder.hypergraph.ForestWalker;
import joshua.decoder.hypergraph.GrammarBuilderWalkerFunction;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.phrase.Stacks;
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
  private final List<Grammar> allGrammars;
  private final List<FeatureFunction> featureFunctions;

  private static final Logger logger = Logger.getLogger(DecoderThread.class.getName());

  // ===============================================================
  // Constructor
  // ===============================================================
  public DecoderThread(List<Grammar> grammars, FeatureVector weights,
      List<FeatureFunction> featureFunctions, JoshuaConfiguration joshuaConfiguration) throws IOException {

    this.joshuaConfiguration = joshuaConfiguration;
    this.allGrammars = grammars;

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

    Decoder.LOG(1, String.format("Input %d: %s", sentence.id(), sentence.fullSource()));

    if (sentence.target() != null)
      Decoder.LOG(1, String.format("Input %d: Constraining to target sentence '%s'", 
          sentence.id(), sentence.target()));

    // skip blank sentences
    if (sentence.isEmpty()) {
      Decoder.LOG(1, String.format("Translation %d: Translation took 0 seconds", sentence.id()));
      return new Translation(sentence, null, featureFunctions, joshuaConfiguration);
    }
    
    long startTime = System.currentTimeMillis();

    int numGrammars = allGrammars.size();
    Grammar[] grammars = new Grammar[numGrammars];

    for (int i = 0; i < allGrammars.size(); i++)
      grammars[i] = allGrammars.get(i);
    
    if (joshuaConfiguration.segment_oovs)
      sentence.segmentOOVs(grammars);

    /**
     * Joshua supports (as of September 2014) both phrase-based and hierarchical decoding. Here
     * we build the appropriate chart. The output of both systems is a hypergraph, which is then
     * used for further processing (e.g., k-best extraction).
     */
    HyperGraph hypergraph = null;
    try {

      if (joshuaConfiguration.search_algorithm.equals("stack")) {
        Stacks stacks = new Stacks(sentence, this.featureFunctions, grammars, joshuaConfiguration);
        
        hypergraph = stacks.search();
      } else {
        /* Seeding: the chart only sees the grammars, not the factories */
        Chart chart = new Chart(sentence, this.featureFunctions, grammars,
            joshuaConfiguration.goal_symbol, joshuaConfiguration);

        hypergraph = (joshuaConfiguration.use_dot_chart) 
          ? chart.expand() 
          : chart.expandSansDotChart();
      }
      
    } catch (java.lang.OutOfMemoryError e) {
      Decoder.LOG(1, String.format("Input %d: out of memory", sentence.id()));
      hypergraph = null;
    }

    float seconds = (System.currentTimeMillis() - startTime) / 1000.0f;
    Decoder.LOG(1, String.format("Input %d: Translation took %.3f seconds", sentence.id(), seconds));
    Decoder.LOG(1, String.format("Memory used after sentence %d is %.1f MB", sentence.id(), (Runtime
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
    Chart chart = new Chart(targetSentence, featureFunctions, newGrammarArray, "GOAL",joshuaConfiguration);
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
