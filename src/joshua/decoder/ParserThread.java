/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package joshua.decoder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.chart_parser.Chart;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarFactory;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.ForestWalker;
import joshua.decoder.hypergraph.GrammarBuilderWalkerFunction;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.hypergraph.KBestExtractor;
import joshua.decoder.segment_file.Sentence;
import joshua.lattice.Lattice;
import joshua.oracle.OracleExtractor;
import edu.jhu.thrax.util.TestSetFilter;

/**
 * This class handles parsing of individual Sentence objects (which can represent plain sentences or
 * lattices). A single sentence can be decoded by a call to translate() and, if an InputHandler is
 * used, many sentences can be decoded in a thread-safe manner via a single call to translateAll(),
 * which continually queries the InputHandler for sentences until they have all been consumed and
 * translated.
 * 
 * The DecoderFactory class is responsible for launching the threads.
 * 
 * @author Jonny Weese <jonny@cs.jhu.edu>
 * @author Matt Post <post@jhu.edu>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2010-05-02 11:19:17 -0400 (Sun, 02 May 2010) $
 */
// BUG: known synchronization problem: LM cache; srilm call;
public class ParserThread extends Thread {
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
  DiskHyperGraph hypergraphSerializer; // package-private for DecoderFactory


  private static final Logger logger = Logger.getLogger(ParserThread.class.getName());


  // ===============================================================
  // Constructor
  // ===============================================================
  public ParserThread(List<GrammarFactory> grammarFactories,
      List<FeatureFunction> featureFunctions, List<StateComputer> stateComputers,
      InputHandler inputHandler) throws IOException {

    this.grammarFactories = grammarFactories;
    this.featureFunctions = featureFunctions;
    this.stateComputers = stateComputers;

    this.inputHandler = inputHandler;

    this.kbestExtractor =
        new KBestExtractor(JoshuaConfiguration.use_unique_nbest,
            JoshuaConfiguration.use_tree_nbest, JoshuaConfiguration.include_align_index,
            JoshuaConfiguration.add_combined_cost, false, true);

    // if (JoshuaConfiguration.save_disk_hg) {
    // FeatureFunction languageModel = null;
    // for (FeatureFunction ff : this.featureFunctions) {
    // if (ff instanceof LanguageModelFF) {
    // languageModel = ff;
    // break;
    // }
    // }
    // int lmFeatID = -1;
    // if (null == languageModel) {
    // logger.warning("No language model feature function found, but save disk hg");
    // } else {
    // lmFeatID = languageModel.getFeatureID();
    // }

    // this.hypergraphSerializer = new DiskHyperGraph(
    // Vocabulary,
    // lmFeatID,
    // true, // always store model cost
    // this.featureFunctions);

    // this.hypergraphSerializer.initWrite(
    // "out." + Integer.toString(sentence.id()) + ".hg.items",
    // JoshuaConfiguration.forest_pruning,
    // JoshuaConfiguration.forest_pruning_threshold);
    // }
  }


  // ===============================================================
  // Methods
  // ===============================================================
  // Overriding of Thread.run() cannot throw anything
  public void run() {
    try {
      this.parseAll();
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
  public void parseAll() throws IOException {

    for (;;) {

      Sentence sentence = inputHandler.next();
      if (sentence == null) break;

      HyperGraph hypergraph = parse(sentence, null);
      Translation translation = null;

      String oracleSentence = inputHandler.oracleSentence(sentence.id());

      if (oracleSentence != null) {
        OracleExtractor extractor = new OracleExtractor();
        HyperGraph oracle = extractor.getOracle(hypergraph, 3, oracleSentence);

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
   * Parse a sentence.
   * 
   * @param segment The sentence to be parsed.
   * @param oracleSentence
   */
  public HyperGraph parse(Sentence sentence, String oracleSentence) throws IOException {

    logger.info("Parsing sentence pair #" + sentence.id() + " [thread " + getId() + "]\n"
        + sentence.sentence());

    long startTime = System.currentTimeMillis();

    Chart chart;

    String[] sentencePair = sentence.sentence().split("\\|\\|\\|");
    int sentenceId = sentence.id();
    for (int i = 0; i < sentencePair.length; i++)
      sentencePair[i] = sentencePair[i].trim();
    System.err.printf("FOREIGN: ``%s''\n", sentencePair[0]);
    System.err.printf("ENGLISH: ``%s''\n", sentencePair[1]);
    Sentence foreign = new Sentence(sentencePair[0], sentenceId);
    Sentence english = new Sentence(sentencePair[1], sentenceId);

    Lattice<Integer> input_lattice = foreign.intLattice();

    int numGrammars =
        (JoshuaConfiguration.use_sent_specific_tm) ? grammarFactories.size() + 1 : grammarFactories
            .size();

    Grammar[] grammars = new Grammar[numGrammars];

    for (int i = 0; i < grammarFactories.size(); i++)
      grammars[i] = grammarFactories.get(i).getGrammarForSentence(foreign);

    // load the sentence-specific grammar
    boolean alreadyExisted = true; // whether it already existed
    String tmFile = null;
    if (JoshuaConfiguration.use_sent_specific_tm) {
      // figure out the sentence-level file name
      tmFile = JoshuaConfiguration.tm_file;
      tmFile =
          tmFile.endsWith(".gz") ? tmFile.substring(0, tmFile.length() - 3) + "." + sentence.id()
              + ".gz" : tmFile + "." + sentence.id();

      // look in a subdirectory named "filtered" e.g.,
      // /some/path/grammar.gz will have sentence-level
      // grammars in /some/path/filtered/grammar.SENTNO.gz
      int lastSlashPos = tmFile.lastIndexOf('/');
      String dirPart = tmFile.substring(0, lastSlashPos + 1);
      String filePart = tmFile.substring(lastSlashPos + 1);
      tmFile = dirPart + "filtered/" + filePart;

      File filteredDir = new File(dirPart + "filtered");
      if (!filteredDir.exists()) {
        logger.info("Creating sentence-level grammar directory '" + dirPart + "filtered'");
        filteredDir.mkdirs();
      }

      logger.info("Using sentence-specific TM file '" + tmFile + "'");

      if (!new File(tmFile).exists()) {
        alreadyExisted = false;

        // filter grammar and write it to a file
        if (logger.isLoggable(Level.INFO)) logger.info("Automatically producing file " + tmFile);

        new TestSetFilter().filterGrammarToFile(JoshuaConfiguration.tm_file, sentence.sentence(),
            tmFile, true);
      } else {
        if (logger.isLoggable(Level.INFO))
          logger.info("Using existing sentence-specific tm file " + tmFile);
      }


      grammars[numGrammars - 1] =
          new MemoryBasedBatchGrammar(JoshuaConfiguration.tm_format, tmFile,
              JoshuaConfiguration.phrase_owner, JoshuaConfiguration.default_non_terminal,
              JoshuaConfiguration.span_limit, JoshuaConfiguration.oov_feature_cost);

      // sort the sentence-specific grammar
      grammars[numGrammars - 1].sortGrammar(this.featureFunctions);

    }

    /* Seeding: the chart only sees the grammars, not the factories */
    chart =
        new Chart(foreign, this.featureFunctions, this.stateComputers, grammars, false,
            JoshuaConfiguration.goal_symbol);

    /* Parsing */
    HyperGraph hypergraph = chart.expand();
    long firstParseTime = System.currentTimeMillis();
    System.err.printf("First-pass parse took %d seconds.\n", (firstParseTime - startTime) / 1000);
    if (hypergraph == null) {
      // we couldn't even do the first-pass parse
      return hypergraph;
    }

    Lattice<Integer> english_lattice = english.intLattice();
    Grammar[] newGrammar = new Grammar[1];
    newGrammar[0] = getGrammarFromHyperGraph(JoshuaConfiguration.goal_symbol, hypergraph);
    long traversalTime = System.currentTimeMillis();
    System.err.printf("Hypergraph traversal completed (%d seconds).\n",
        (traversalTime - firstParseTime) / 1000);
    newGrammar[0].sortGrammar(this.featureFunctions);
    long sortTime = System.currentTimeMillis();
    System.err.printf("Grammar sort completed (%d seconds).\n", (sortTime - traversalTime) / 1000);
    int numRules = newGrammar[0].getNumRules();
    System.err.printf("New grammar has %d rules.\n", numRules);
    System.err.println("Expanding second chart.\n");
    chart =
        new Chart(english, this.featureFunctions, this.stateComputers, newGrammar, false, "GOAL");
    int goalSymbol = GrammarBuilderWalkerFunction.goalSymbol(hypergraph);
    System.err.printf("goal symbol is %d.\n", goalSymbol);
    chart.setGoalSymbolID(goalSymbol);
    /* Parsing */
    HyperGraph englishParse = chart.expand();
    long secondParseTime = System.currentTimeMillis();
    System.err.printf("Finished second chart expansion (%d seconds).\n",
        (secondParseTime - sortTime) / 1000);
    System.err.printf("Total time: %d seconds.\n", (secondParseTime - startTime) / 1000);

    return englishParse; // or do something else
  }

  private static Grammar getGrammarFromHyperGraph(String goal, HyperGraph hg) throws IOException {
    // PrintStream out = new PrintStream(new File("hg.grammar"));
    GrammarBuilderWalkerFunction f = new GrammarBuilderWalkerFunction(goal);
    ForestWalker walker = new ForestWalker();
    walker.walk(hg.goalNode, f);
    // out.close();
    return f.getGrammar();
  }
}
