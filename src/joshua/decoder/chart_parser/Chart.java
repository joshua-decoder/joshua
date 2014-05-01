package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.corpus.syntax.SyntaxTree;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.CubePruneState;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.SourceDependentFF;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.ParsedSentence;
import joshua.decoder.segment_file.Sentence;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;
import joshua.util.ChartSpan;

/**
 * Chart class this class implements chart-parsing: (1) seeding the chart (2) cky main loop over
 * bins, (3) identify applicable rules in each bin
 * 
 * Note: the combination operation will be done in Cell
 * 
 * Signatures of class: Cell: i, j SuperNode (used for CKY check): i,j, lhs HGNode ("or" node): i,j,
 * lhs, edge ngrams HyperEdge ("and" node)
 * 
 * index of sentences: start from zero index of cell: cell (i,j) represent span of words indexed
 * [i,j-1] where i is in [0,n-1] and j is in [1,n]
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 */

public class Chart {

  private final JoshuaConfiguration joshuaConfiguration;
  // ===========================================================
  // Statistics
  // ===========================================================

  /**
   * how many items have been pruned away because its cost is greater than the cutoff in calling
   * chart.add_deduction_in_chart()
   */
  int nMerged = 0;
  int nAdded = 0;
  int nDotitemAdded = 0; // note: there is no pruning in dot-item
  int nCalledComputeNode = 0;

  int segmentID;

  // ===============================================================
  // Private instance fields (maybe could be protected instead)
  // ===============================================================
  private ChartSpan<Cell> cells; // note that in some cell, it might be null
  private int sourceLength;
  private List<FeatureFunction> featureFunctions;
  private Grammar[] grammars;
  private DotChart[] dotcharts; // each grammar should have a dotchart associated with it
  private Cell goalBin;
  private int goalSymbolID = -1;
  private Lattice<Integer> inputLattice;

  private Sentence sentence = null;
  private SyntaxTree parseTree;

  private ManualConstraintsHandler manualConstraintsHandler;

  // ===============================================================
  // Static fields
  // ===============================================================

  // ===========================================================
  // Logger
  // ===========================================================
  private static final Logger logger = Logger.getLogger(Chart.class.getName());

  // ===============================================================
  // Constructors
  // ===============================================================

  /*
   * TODO: Once the Segment interface is adjusted to provide a Lattice<String> for the sentence()
   * method, we should just accept a Segment instead of the sentence, segmentID, and constraintSpans
   * parameters. We have the symbol table already, so we can do the integerization here instead of
   * in DecoderThread. GrammarFactory.getGrammarForSentence will want the integerized sentence as
   * well, but then we'll need to adjust that interface to deal with (non-trivial) lattices too. Of
   * course, we get passed the grammars too so we could move all of that into here.
   */

  public Chart(Sentence sentence, List<FeatureFunction> featureFunctions, Grammar[] grammars,
      String goalSymbol, JoshuaConfiguration joshuaConfiguration) {
    this.joshuaConfiguration = joshuaConfiguration;
    this.inputLattice = sentence.intLattice();
    this.sourceLength = inputLattice.size() - 1;
    this.featureFunctions = featureFunctions;

    this.sentence = sentence;
    this.parseTree = null;
    if (sentence instanceof ParsedSentence)
      this.parseTree = ((ParsedSentence) sentence).syntaxTree();

    this.cells = new ChartSpan<Cell>(sourceLength, null);

    this.segmentID = sentence.id();
    this.goalSymbolID = Vocabulary.id(goalSymbol);
    this.goalBin = new Cell(this, this.goalSymbolID);

    /* Create the grammars, leaving space for the OOV grammar. */
    this.grammars = new Grammar[grammars.length + 1];
    for (int i = 0; i < grammars.length; i++)
      this.grammars[i] = grammars[i];
    MemoryBasedBatchGrammar oovGrammar = new MemoryBasedBatchGrammar("oov", joshuaConfiguration);
    this.grammars[this.grammars.length - 1] = oovGrammar;

    // each grammar will have a dot chart
    this.dotcharts = new DotChart[this.grammars.length];
    for (int i = 0; i < this.grammars.length; i++)
      this.dotcharts[i] = new DotChart(this.inputLattice, this.grammars[i], this,
          NonterminalMatcher.createNonterminalMatcher(logger, joshuaConfiguration),
          this.grammars[i].isRegexpGrammar());

    // Begin to do initialization work

    // TODO: which grammar should we use to create a manual rule?
    manualConstraintsHandler = new ManualConstraintsHandler(this, grammars[grammars.length - 1],
        sentence.constraints());

    /*
     * Add OOV rules; This should be called after the manual constraints have been set up.
     */
		final byte [] oovAlignment = { 0, 0 };
    for (Node<Integer> node : inputLattice) {
      for (Arc<Integer> arc : node.getOutgoingArcs()) {
        // create a rule, but do not add into the grammar trie
        // TODO: which grammar should we use to create an OOV rule?
        int sourceWord = arc.getLabel();
        if (sourceWord == Vocabulary.id(Vocabulary.START_SYM)
            || sourceWord == Vocabulary.id(Vocabulary.STOP_SYM))
          continue;

        // Determine if word is actual OOV.
        if (joshuaConfiguration.true_oovs_only) {
          boolean true_oov = true;
          for (Grammar g : grammars) {
            if (g.getTrieRoot().match(sourceWord) != null
                && g.getTrieRoot().match(sourceWord).hasRules()) {
              true_oov = false;
              break;
            }
          }
          if (!true_oov)
            continue;
        }

        final int targetWord;
        if (joshuaConfiguration.mark_oovs) {
          targetWord = Vocabulary.id(Vocabulary.word(sourceWord) + "_OOV");
        } else {
          targetWord = sourceWord;
        }

        List<BilingualRule> oovRules = new ArrayList<BilingualRule>();
        int[] sourceWords = { sourceWord };
        int[] targetWords = { targetWord };
        if (parseTree != null
            && (joshuaConfiguration.constrain_parse || joshuaConfiguration.use_pos_labels)) {
          Collection<Integer> labels = parseTree.getConstituentLabels(node.getNumber() - 1,
              node.getNumber());
          for (int label : labels) {
            BilingualRule oovRule = new BilingualRule(label, sourceWords, targetWords, "", 0, oovAlignment);
            oovRules.add(oovRule);
            oovGrammar.addRule(oovRule);
            oovRule.estimateRuleCost(featureFunctions);
          }

        }

        if (joshuaConfiguration.oov_list != null && joshuaConfiguration.oov_list.length != 0) {
          for (int i = 0; i < joshuaConfiguration.oov_list.length; i++) {
            BilingualRule oovRule = new BilingualRule(
                Vocabulary.id(joshuaConfiguration.oov_list[i]), sourceWords, targetWords, "", 0, oovAlignment);
            oovRules.add(oovRule);
            oovGrammar.addRule(oovRule);
            oovRule.estimateRuleCost(featureFunctions);
//            System.err.println(String.format("ADDING OOV RULE %s", oovRule));
          }
        } else {
          int nt_i = Vocabulary.id(joshuaConfiguration.default_non_terminal);
          BilingualRule oovRule = new BilingualRule(nt_i, sourceWords, targetWords, "", 0, oovAlignment);
          oovRules.add(oovRule);
          oovGrammar.addRule(oovRule);
          oovRule.estimateRuleCost(featureFunctions);
//          System.err.println(String.format("ADDING OOV RULE %s", oovRule));
        }

        if (manualConstraintsHandler.containHardRuleConstraint(node.getNumber(), arc.getHead()
            .getNumber())) {
          // do not add the oov axiom
          logger.fine("Using hard rule constraint for span " + node.getNumber() + ", "
              + arc.getHead().getNumber());
        }
      }
    }

    // Grammars must be sorted.
    oovGrammar.sortGrammar(this.featureFunctions);

    /* Find the SourceDependent feature and give it access to the sentence. */
    for (FeatureFunction ff : this.featureFunctions)
      if (ff instanceof SourceDependentFF)
        ((SourceDependentFF) ff).setSource(sentence);

    logger.fine("Finished seeding chart.");
  }

  /**
   * Manually set the goal symbol ID. The constructor expects a String representing the goal symbol,
   * but there may be time (say, for example, in the second pass of a synchronous parse) where we
   * want to set the goal symbol to a particular ID (regardless of String representation).
   * <p>
   * This method should be called before expanding the chart, as chart expansion depends on the goal
   * symbol ID.
   * 
   * @param i the id of the goal symbol to use
   */
  public void setGoalSymbolID(int i) {
    this.goalSymbolID = i;
    this.goalBin = new Cell(this, i);
    return;
  }

  // ===============================================================
  // The primary method for filling in the chart
  // ===============================================================

  /**
   * Construct the hypergraph with the help from DotChart.
   */
  private void completeSpan(int i, int j) {

    // System.err.println("[" + segmentID + "] SPAN(" + i + "," + j + ")");

    // StateConstraint stateConstraint = sentence.target() != null ? new StateConstraint(
    // Vocabulary.START_SYM + " " + sentence.target() + " " + Vocabulary.STOP_SYM) : null;

    StateConstraint stateConstraint = null;
    if (sentence.target() != null)
      // stateConstraint = new StateConstraint(sentence.target());
      stateConstraint = new StateConstraint(Vocabulary.START_SYM + " " + sentence.target() + " "
          + Vocabulary.STOP_SYM);

    /*
     * We want to implement proper cube-pruning at the span level, with pruning controlled with the
     * specification of a single parameter, a pop-limit on the number of items. This pruning would
     * be across all DotCharts (that is, across all grammars) and across all items in the span,
     * regardless of other state (such as language model state or the lefthand side).
     * 
     * The existing implementation prunes in a much less straightforward fashion. Each Dotnode
     * within each span is examined, and applicable rules compete amongst each other. The number of
     * them that is kept is not absolute, but is determined by some combination of the maximum heap
     * size and the score differences. The score differences occur across items in the whole span.
     */

    /* STEP 1: create the heap, and seed it with all of the candidate states */
    PriorityQueue<CubePruneState> candidates = new PriorityQueue<CubePruneState>();

    // this records states we have already visited
    HashSet<CubePruneState> visitedStates = new HashSet<CubePruneState>();

    // seed it with the beginning states
    // for each applicable grammar
    for (int g = 0; g < grammars.length; g++) {
      if (!grammars[g].hasRuleForSpan(i, j, inputLattice.distance(i, j))
          || null == dotcharts[g].getDotCell(i, j))
        continue;
      // for each rule with applicable rules
      for (DotNode dotNode : dotcharts[g].getDotCell(i, j).getDotNodes()) {
        RuleCollection ruleCollection = dotNode.getApplicableRules();
        if (ruleCollection == null)
          continue;

        // Create the Cell if necessary.
        if (cells.get(i, j) == null)
          cells.set(i, j, new Cell(this, goalSymbolID));

        /*
         * TODO: This causes the whole list of rules to be copied, which is unnecessary when there
         * are not actually any constraints in play.
         */
        // List<Rule> sortedAndFilteredRules = manualConstraintsHandler.filterRules(i, j,
        // ruleCollection.getSortedRules(this.featureFunctions));
        List<Rule> rules = ruleCollection.getSortedRules(this.featureFunctions);
        SourcePath sourcePath = dotNode.getSourcePath();

        if (null == rules || rules.size() <= 0)
          continue;

        int arity = ruleCollection.getArity();

        // Rules that have no nonterminals in them so far
        // are added to the chart with no pruning
        if (arity == 0) {
          for (Rule rule : rules) {
            ComputeNodeResult result = new ComputeNodeResult(this.featureFunctions, rule, null, i,
                j, sourcePath, this.segmentID);
            if (stateConstraint == null || stateConstraint.isLegal(result.getDPStates()))
              cells.get(i, j).addHyperEdgeInCell(result, rule, i, j, null, sourcePath, true);
          }
        } else {

          Rule bestRule = rules.get(0);

          List<HGNode> currentAntNodes = new ArrayList<HGNode>();
          List<SuperNode> superNodes = dotNode.getAntSuperNodes();
          for (SuperNode si : superNodes) {
            // TODO: si.nodes must be sorted
            currentAntNodes.add(si.nodes.get(0));
          }

          ComputeNodeResult result = new ComputeNodeResult(featureFunctions, bestRule,
              currentAntNodes, i, j, sourcePath, this.segmentID);

          int[] ranks = new int[1 + superNodes.size()];
          for (int r = 0; r < ranks.length; r++)
            ranks[r] = 1;

          CubePruneState bestState = new CubePruneState(result, ranks, rules, currentAntNodes);

          bestState.setDotNode(dotNode);
          candidates.add(bestState);
          visitedStates.add(bestState);
        }
      }
    }

    int popLimit = joshuaConfiguration.pop_limit;
    int popCount = 0;
    while (candidates.size() > 0 && ((++popCount <= popLimit) || popLimit == 0)) {
      CubePruneState state = candidates.poll();

      DotNode dotNode = state.getDotNode();
      List<Rule> rules = state.rules;
      SourcePath sourcePath = dotNode.getSourcePath();
      List<SuperNode> superNodes = dotNode.getAntSuperNodes();

      /*
       * Add the hypothesis to the chart. This can only happen if (a) we're not doing constrained
       * decoding or (b) we are and the state is legal.
       */
      if (stateConstraint == null || stateConstraint.isLegal(state.getDPStates())) {
        cells.get(i, j).addHyperEdgeInCell(state.computeNodeResult, state.getRule(), i, j,
            state.antNodes, sourcePath, true);
      }

      /*
       * Expand the hypothesis by walking down a step along each dimension of the cube, in turn. k =
       * 0 means we extend the rule being used; k > 0 expands the corresponding tail node.
       */
      for (int k = 0; k < state.ranks.length; k++) {

        /* Copy the current ranks, then extend the one we're looking at. */
        int[] newRanks = new int[state.ranks.length];
        System.arraycopy(state.ranks, 0, newRanks, 0, state.ranks.length);
        newRanks[k]++;

        /* We might have reached the end of something (list of rules or tail nodes) */
        if ((k == 0 && newRanks[k] > rules.size())
            || (k != 0 && newRanks[k] > superNodes.get(k - 1).nodes.size()))
          continue;

        /* Use the updated ranks to assign the next rule and tail node. */
        Rule nextRule = rules.get(newRanks[0] - 1);
        // HGNode[] nextAntNodes = new HGNode[state.antNodes.size()];
        List<HGNode> nextAntNodes = new ArrayList<HGNode>();
        for (int x = 0; x < state.ranks.length - 1; x++)
          nextAntNodes.add(superNodes.get(x).nodes.get(newRanks[x + 1] - 1));

        /* Create the next state. */
        CubePruneState nextState = new CubePruneState(new ComputeNodeResult(featureFunctions,
            nextRule, nextAntNodes, i, j, sourcePath, this.segmentID), newRanks, rules,
            nextAntNodes);
        nextState.setDotNode(dotNode);

        /* Skip states that have been explored before. */
        if (visitedStates.contains(nextState))
          continue;

        visitedStates.add(nextState);
        candidates.add(nextState);
      }
    }
  }

  /**
   * This function performs the main work of decoding.
   * 
   * @return the hypergraph containing the translated sentence.
   */
  public HyperGraph expand() {

    for (int width = 1; width <= sourceLength; width++) {
      for (int i = 0; i <= sourceLength - width; i++) {
        int j = i + width;
        if (logger.isLoggable(Level.FINEST))
          logger.finest(String.format("Processing span (%d, %d)", i, j));

        /* Skips spans for which no path exists (possible in lattices). */
        if (inputLattice.distance(i, j) == Float.POSITIVE_INFINITY) {
          continue;
        }

        /*
         * 1. Expand the dot through all rules. This is a matter of (a) look for rules over (i,j-1)
         * that need the terminal at (j-1,j) and looking at all split points k to expand
         * nonterminals.
         */
        logger.finest("Expanding cell");
        for (int k = 0; k < this.grammars.length; k++) {
          /**
           * Each dotChart can act individually (without consulting other dotCharts) because it
           * either consumes the source input or the complete nonTerminals, which are both
           * grammar-independent.
           **/
          this.dotcharts[k].expandDotCell(i, j);
        }

        /* 2. The regular CKY part: add completed items onto the chart via cube pruning. */
        logger.finest("Adding complete items into chart");
        completeSpan(i, j);

        /* 3. Process unary rules. */
        logger.finest("Adding unary items into chart");
        addUnaryNodes(this.grammars, i, j);

        // (4)=== in dot_cell(i,j), add dot-nodes that start from the /complete/
        // superIterms in
        // chart_cell(i,j)
        logger.finest("Initializing new dot-items that start from complete items in this cell");
        for (int k = 0; k < this.grammars.length; k++) {
          if (this.grammars[k].hasRuleForSpan(i, j, inputLattice.distance(i, j))) {
            this.dotcharts[k].startDotItems(i, j);
          }
        }

        /*
         * 5. Sort the nodes in the cell.
         * 
         * Sort the nodes in this span, to make them usable for future applications of cube pruning.
         */
        if (null != this.cells.get(i, j)) {
          this.cells.get(i, j).getSortedNodes();
        }
      }
    }

    logStatistics(Level.INFO);

    // transition_final: setup a goal item, which may have many deductions
    if (null == this.cells.get(0, sourceLength)
        || !this.goalBin.transitToGoal(this.cells.get(0, sourceLength), this.featureFunctions,
            this.sourceLength)) {
      logger.severe("No complete item in the Cell[0," + sourceLength + "]; possible reasons: "
          + "(1) your grammar does not have any valid derivation for the source sentence; "
          + "(2) too aggressive pruning.");
      return null;
    }

    logger.fine("Finished expand");
    return new HyperGraph(this.goalBin.getSortedNodes().get(0), -1, -1, this.segmentID,
        sourceLength);
  }

  public Cell getCell(int i, int j) {
    return this.cells.get(i, j);
  }

  // ===============================================================
  // Private methods
  // ===============================================================

  private void logStatistics(Level level) {
    if (logger.isLoggable(level))
      logger.log(
          level,
          String.format("Sentence %d Chart: ADDED %d MERGED %d DOT-ITEMS ADDED: %d",
              this.sentence.id(), this.nAdded, this.nMerged, this.nDotitemAdded));
  }

  /**
   * Handles expansion of unary rules. Rules are expanded in an agenda-based manner to avoid
   * constructing infinite unary chains. Assumes a triangle inequality of unary rule expansion
   * (e.g., A -> B will always be cheaper than A -> C -> B), which is not a true assumption.
   * 
   * @param grammars A list of the grammars for the sentence
   * @param i
   * @param j
   * @return the number of nodes added
   */
  private int addUnaryNodes(Grammar[] grammars, int i, int j) {

    Cell chartBin = this.cells.get(i, j);
    if (null == chartBin) {
      return 0;
    }
    int qtyAdditionsToQueue = 0;
    ArrayList<HGNode> queue = new ArrayList<HGNode>(chartBin.getSortedNodes());
    HashSet<Integer> seen_lhs = new HashSet<Integer>();

    if (logger.isLoggable(Level.FINEST))
      logger.finest("Adding unary to [" + i + ", " + j + "]");

    while (queue.size() > 0) {
      HGNode node = queue.remove(0);
      seen_lhs.add(node.lhs);

      for (Grammar gr : grammars) {
        if (!gr.hasRuleForSpan(i, j, inputLattice.distance(i, j)))
          continue;

        /* Match against the node's LHS, and then make sure the rule collection has unary rules */
        Trie childNode = gr.getTrieRoot().match(node.lhs);
        if (childNode != null && childNode.getRuleCollection() != null
            && childNode.getRuleCollection().getArity() == 1) {

          ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
          antecedents.add(node);
          List<Rule> rules = childNode.getRuleCollection().getSortedRules(this.featureFunctions);

          for (Rule rule : rules) { // for each unary rules
            ComputeNodeResult states = new ComputeNodeResult(this.featureFunctions, rule,
                antecedents, i, j, new SourcePath(), this.segmentID);
            HGNode resNode = chartBin.addHyperEdgeInCell(states, rule, i, j, antecedents,
                new SourcePath(), true);

            if (logger.isLoggable(Level.FINEST))
              logger.finest(rule.toString());

            if (null != resNode && !seen_lhs.contains(resNode.lhs)) {
              queue.add(resNode);
              qtyAdditionsToQueue++;
            }
          }
        }
      }
    }
    return qtyAdditionsToQueue;
  }

  /**
   * This functions add to the hypergraph rules with zero arity (i.e., terminal rules).
   */
  public void addAxiom(int i, int j, Rule rule, SourcePath srcPath) {
    if (null == this.cells.get(i, j)) {
      this.cells.set(i, j, new Cell(this, this.goalSymbolID));
    }

    // System.err.println(String.format("ADDAXIOM(%d,%d,%s,%s", i, j, rule,
    // srcPath));

    this.cells.get(i, j).addHyperEdgeInCell(
        new ComputeNodeResult(this.featureFunctions, rule, null, i, j, srcPath, segmentID), rule,
        i, j, null, srcPath, false);
  }
}
