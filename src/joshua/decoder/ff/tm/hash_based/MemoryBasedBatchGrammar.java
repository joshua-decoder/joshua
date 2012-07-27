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
 * @version $LastChangedDate$
 */
public class MemoryBasedBatchGrammar extends BatchGrammar {

  // ===============================================================
  // Instance Fields
  // ===============================================================

  static private double temEstcost = 0.0;

  private int qtyRulesRead = 0;
  private int qtyRuleBins = 0;
  private MemoryBasedTrie root = null;

  // protected ArrayList<FeatureFunction> featureFunctions = null;
  private int defaultOwner;

  private float oovFeatureCost = 100;

  /**
   * the OOV rule should have this lhs, this should be grammar specific as only the grammar knows
   * what LHS symbol can be combined with other rules
   */
  private int defaultLHS;

  private String grammarFile;

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

  public MemoryBasedBatchGrammar() {}

  public MemoryBasedBatchGrammar(GrammarReader<BilingualRule> gr) {
    // this.defaultOwner = Vocabulary.id(defaultOwner);
    // this.defaultLHS = Vocabulary.id(defaultLHSSymbol);
    this.root = new MemoryBasedTrie(JoshuaConfiguration.regexpGrammar.equals(Vocabulary.word(defaultOwner)));
    modelReader = gr;
  }

  public MemoryBasedBatchGrammar(String formatKeyword, String grammarFile, String defaultOwner,
      String defaultLHSSymbol, int spanLimit, float oovFeatureCost_) throws IOException {

    this.defaultOwner = Vocabulary.id(defaultOwner);
    this.defaultLHS = Vocabulary.id(defaultLHSSymbol);
    this.spanLimit = spanLimit;
    this.oovFeatureCost = oovFeatureCost_;
    this.root = new MemoryBasedTrie(JoshuaConfiguration.regexpGrammar.equals(defaultOwner));
    this.grammarFile = grammarFile;

    // ==== loading grammar
    this.modelReader = createReader(formatKeyword, grammarFile);
    if (modelReader != null) {
      modelReader.initialize();
      for (BilingualRule rule : modelReader)
        if (rule != null) addRule(rule);
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

  public Rule constructOOVRule(int num_features, int source_word, int target_word,
      boolean use_max_lm_cost) {
    int[] french = {source_word};
    int[] english = {target_word};
    float[] feat_scores = new float[JoshuaConfiguration.num_phrasal_features];

    // TODO: This is a hack to make the decoding without a LM works
    /*
     * When a ngram LM is used, the OOV word will have a cost 100. if no LM is used for decoding, so
     * we should set the cost of some TM feature to be maximum
     */
    if (JoshuaConfiguration.oov_feature_index != -1) {
      feat_scores[JoshuaConfiguration.oov_feature_index] = oovFeatureCost; // 1.0f;
    } else if ((!use_max_lm_cost) && num_features > 0) {
      feat_scores[0] = oovFeatureCost;
    }

    return new BilingualRule(this.defaultLHS, french, english, feat_scores, 0, this.defaultOwner,
        0, getOOVRuleID());
  }

  public Rule constructLabeledOOVRule(int num_features, int source_word, int target_word, int lhs,
      boolean use_max_lm_cost) {
    int[] french = {source_word};
    int[] english = {target_word};
    // HACK: this is making sure the OOV rules get the right number of feature values
    float[] feat_scores = new float[JoshuaConfiguration.num_phrasal_features];

    // TODO: This is a hack to make the decoding without a LM work
    /*
     * When a ngram LM is used, the OOV word will have a cost 100. if no LM is used for decoding, so
     * we should set the cost of some TM feature to be maximum
     */
    if (JoshuaConfiguration.oov_feature_index != -1) {
      feat_scores[JoshuaConfiguration.oov_feature_index] = oovFeatureCost; // 1.0f;
    } else if ((!use_max_lm_cost) && num_features > 0) {
      feat_scores[0] = oovFeatureCost;
    }

    return new BilingualRule(lhs, french, english, feat_scores, 0, this.defaultOwner, 0,
        getOOVRuleID());
  }

  public Rule constructManualRule(int lhs, int[] sourceWords, int[] targetWords, float[] scores,
      int arity) {
    return new BilingualRule(lhs, sourceWords, targetWords, scores, arity, this.defaultOwner, 0,
        getOOVRuleID());
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

  public void addRule(BilingualRule rule) {

    // TODO: Why two increments?
    this.qtyRulesRead++;
    ruleIDCount++;

    rule.setRuleID(ruleIDCount);
    rule.setOwner(defaultOwner);

    // TODO: make sure costs are calculated here or in reader
    temEstcost += rule.getEstCost();

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
        nextLayer = new MemoryBasedTrie(JoshuaConfiguration.regexpGrammar.equals(Vocabulary.word(defaultOwner)));
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



  // BUG: This always prints 0 for all fields
  protected void printGrammar() {
    logger.info("Grammar '" + grammarFile + "'");
    logger.info(String.format("   num_rules: %d; num_bins: %d; num_pruned: %d; sumest_cost: %.5f",
        this.qtyRulesRead, this.qtyRuleBins, 0, temEstcost));
  }

}
