package joshua.decoder.ff.tm.hash_based;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.ff.tm.BatchGrammar;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.ff.tm.format.SamtFormatReader;

/**
 * This class implements a memory-based bilingual BatchGrammar.
 * <p>
 * The rules are stored in a trie. Each trie node has: (1) RuleBin: a list of rules matching the
 * french sides so far (2) A HashMap of next-layer trie nodes, the next french word used as the key
 * in HashMap
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
public class MemoryBasedBatchGrammar extends BatchGrammar {

  // ===============================================================
  // Instance Fields
  // ===============================================================

  /* The number of rules read. */
  private int qtyRulesRead = 0;

  /* The number of distinct source sides. */
  private int qtyRuleBins = 0;

  /* The trie root. */
  private MemoryBasedTrie root = null;

  /* The grammar's owner, used to determine which weights are applicable to the dense features found
   * within. 
   */
  private int owner = -1;

  /* The file containing the grammar. */
  private String grammarFile;

  /* The maximum span of the input this rule can be applied to. */
  private int spanLimit = JoshuaConfiguration.span_limit;

  private GrammarReader<BilingualRule> modelReader;

  // ===============================================================
  // Static Fields
  // ===============================================================

  /*
   * Three kinds of rules: regular rule (id>0) oov rule (id=0) null rule (id=-1)
   */

  static int ruleIDCount = 1;

  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(MemoryBasedBatchGrammar.class.getName());

  // ===============================================================
  // Constructors
  // ===============================================================

  public MemoryBasedBatchGrammar() {
    this.root = new MemoryBasedTrie();
  }

  public MemoryBasedBatchGrammar(String owner) {
    this.root = new MemoryBasedTrie();
    this.owner = Vocabulary.id(owner);
  }

  public MemoryBasedBatchGrammar(GrammarReader<BilingualRule> gr) {
    // this.defaultOwner = Vocabulary.id(defaultOwner);
    // this.defaultLHS = Vocabulary.id(defaultLHSSymbol);
    this.root =
        new MemoryBasedTrie(JoshuaConfiguration.regexpGrammar.equals(Vocabulary.word(owner)));
    modelReader = gr;
  }

  public MemoryBasedBatchGrammar(String formatKeyword, String grammarFile, String owner,
      String defaultLHSSymbol, int spanLimit) throws IOException {

    this.owner = Vocabulary.id(owner);
    Vocabulary.id(defaultLHSSymbol);
    this.spanLimit = spanLimit;
    this.root = new MemoryBasedTrie(JoshuaConfiguration.regexpGrammar.equals(owner));
    this.grammarFile = grammarFile;

    // ==== loading grammar
    this.modelReader = createReader(formatKeyword, grammarFile);
    if (modelReader != null) {
      modelReader.initialize();
      for (BilingualRule rule : modelReader)
        if (rule != null) {
          addRule(rule);
        }
    } else {
      if (logger.isLoggable(Level.WARNING))
        logger.warning("Couldn't create a GrammarReader for file " + grammarFile + " with format "
            + formatKeyword);
    }

    this.printGrammar();
  }

  protected GrammarReader<BilingualRule> createReader(String formatKeyword, String grammarFile) {

    if (grammarFile != null) {
      if ("hiero".equals(formatKeyword) || "thrax".equals(formatKeyword)) {
        return new HieroFormatReader(grammarFile);
      } else if ("samt".equals(formatKeyword)) {
        return new SamtFormatReader(grammarFile);
      } else {
        // TODO: throw something?
        // TODO: add special warning if "heiro" mispelling is used

        if (logger.isLoggable(Level.WARNING))
          logger.warning("Unknown GrammarReader format " + formatKeyword);
      }
    }

    return null;
  }


  // ===============================================================
  // Methods
  // ===============================================================

  public int getNumRules() {
    return this.qtyRulesRead;
  }

  public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords,
      float[] denseScores, int arity) {
    System.err.println("* WARNING: constructManualRule() not working");
    return new BilingualRule(lhs, sourceWords, targetWords, "", arity);
  }

  /**
   * if the span covered by the chart bin is greater than the limit, then return false
   */
  public boolean hasRuleForSpan(int startIndex, int endIndex, int pathLength) {
    if (this.spanLimit == -1) { // mono-glue grammar
      return (startIndex == 0);
    } else {
      return (endIndex - startIndex <= this.spanLimit);
    }
  }

  public Trie getTrieRoot() {
    return this.root;
  }


  /**
   * Adds a rule to the grammar
   */
  public void addRule(BilingualRule rule) {

    // TODO: Why two increments?
    this.qtyRulesRead++;
    ruleIDCount++;

    if (owner == -1) {
      System.err.println("* FATAL: MemoryBasedBatchGrammar::addRule(): owner not set for grammar");
      System.exit(1);
    }
    rule.setOwner(owner);

    // === identify the position, and insert the trie nodes as necessary
    MemoryBasedTrie pos = root;
    int[] french = rule.getFrench();
    for (int k = 0; k < french.length; k++) {
      int curSymID = french[k];

      if (logger.isLoggable(Level.FINEST)) logger.finest("Matching: " + curSymID);

      /*
       * Note that the nonTerminal symbol in the french is not cleaned (i.e., will be sth like
       * [X,1]), but the symbol in the Trie has to be cleaned, so that the match does not care about
       * the markup (i.e., [X,1] or [X,2] means the same thing, that is X) if
       * (Vocabulary.nt(french[k])) { curSymID = modelReader.cleanNonTerminal(french[k]); if
       * (logger.isLoggable(Level.FINEST)) logger.finest("Amended to: " + curSymID); }
       */

      // we call exactMatch() here to avoid applying regular expressions along the arc
      MemoryBasedTrie nextLayer = pos.exactMatch(curSymID);
      if (null == nextLayer) {
        nextLayer =
            new MemoryBasedTrie(JoshuaConfiguration.regexpGrammar.equals(Vocabulary.word(owner)));
        if (pos.hasExtensions() == false) {
          pos.childrenTbl = new HashMap<Integer, MemoryBasedTrie>();
        }
        pos.childrenTbl.put(curSymID, nextLayer);
      }
      pos = nextLayer;
    }


    // === add the rule into the trie node
    if (!pos.hasRules()) {
      pos.ruleBin = new MemoryBasedRuleBin(rule.getArity(), rule.getFrench());
      this.qtyRuleBins++;
    }
    pos.ruleBin.addRule(rule);
  }

  protected void printGrammar() {
    logger.info(String.format("MemoryBasedBatchGrammar: Read %d rules with %d distinct source sides from '%s'", this.qtyRulesRead, this.qtyRuleBins, grammarFile));
  }
}
