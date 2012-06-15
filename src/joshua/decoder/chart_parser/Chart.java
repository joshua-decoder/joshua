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
import joshua.decoder.chart_parser.CubePruneCombiner.CubePruneState;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.PhraseModelFF;
import joshua.decoder.ff.SourceDependentFF;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.ParsedSentence;
import joshua.decoder.segment_file.Sentence;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;


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
 * @author Matt Post <post@jhu.edu>
 */

public class Chart {

  // ===========================================================
  // Statistics
  // ===========================================================

  /**
   * how many items have been pruned away because its cost is greater than the cutoff in calling
   * chart.add_deduction_in_chart()
   */
  int nPreprunedEdges = 0;

  int nPreprunedFuzz1 = 0;
  int nPreprunedFuzz2 = 0;
  int nPrunedItems = 0;
  int nMerged = 0;
  int nAdded = 0;
  int nDotitemAdded = 0; // note: there is no pruning in dot-item
  int nCalledComputeNode = 0;

  int segmentID;

  // ===============================================================
  // Private instance fields (maybe could be protected instead)
  // ===============================================================
  private Cell[][] cells; // note that in some cell, it might be null
  private int sourceLength;
  private List<FeatureFunction> featureFunctions;
  private List<FeatureFunction> nonPhrasalFeatureFunctions;
  private List<StateComputer> stateComputers;
  private Grammar[] grammars;
  private DotChart[] dotcharts; // each grammar should have a dotchart associated with it
  private Cell goalBin;
  private int goalSymbolID = -1;
  private Lattice<Integer> inputLattice;

  private SyntaxTree parseTree;

  private Combiner combiner = null;
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

  public Chart(Sentence sentence, List<FeatureFunction> featureFunctions,
      List<StateComputer> stateComputers, Grammar[] grammars, boolean useMaxLMCostForOOV,
      String goalSymbol) {
    this.inputLattice = sentence.intLattice();
    this.sourceLength = inputLattice.size() - 1;
    this.featureFunctions = featureFunctions;
    this.stateComputers = stateComputers;

    this.nonPhrasalFeatureFunctions = new ArrayList<FeatureFunction>();
    for (FeatureFunction ff : this.featureFunctions) {
      if (!(ff instanceof PhraseModelFF)) this.nonPhrasalFeatureFunctions.add(ff);
    }

    this.parseTree = null;
    if (sentence instanceof ParsedSentence)
      this.parseTree = ((ParsedSentence) sentence).syntaxTree();

    this.cells = new Cell[sourceLength][sourceLength + 1];

    this.segmentID = sentence.id();
    this.goalSymbolID = Vocabulary.id(goalSymbol);
    this.goalBin = new Cell(this, this.goalSymbolID);
    this.grammars = grammars;

    // each grammar will have a dot chart
    this.dotcharts = new DotChart[this.grammars.length];
    for (int i = 0; i < this.grammars.length; i++)
      this.dotcharts[i] = new DotChart(this.inputLattice, this.grammars[i], this);

    /*
     * The CubePruneCombiner defined here is fairly complicated. It is designed to work both at the
     * cell level and at the span level, using a beam and threshold to have rules compete amongst
     * each other for a single node. This can be turned off in favor of a simple cube pruning
     * pop-limit at the span limit by setting pop-limit to a nonzero value. Otherwise, if
     * useBeamAndThresholdPrune is set to true, the item-level cube-pruning + beam and threshold
     * pruning is used. Finally, exhaustive pruning is used.
     */

    if (JoshuaConfiguration.pop_limit > 0)
      combiner = null;
    else if (JoshuaConfiguration.useBeamAndThresholdPrune)
      combiner = new CubePruneCombiner(this.featureFunctions, this.stateComputers);
    else
      combiner = new ExhaustiveCombiner(this.featureFunctions, this.stateComputers);

    // Begin to do initialization work

    // TODO: which grammar should we use to create a manual rule?
    manualConstraintsHandler =
        new ManualConstraintsHandler(this, grammars[grammars.length - 1], sentence.constraints());

    /*
     * Add OOV rules; This should be called after the manual constraints have been set up. Different
     * grammar differ in hasRuleForSpan, defaultOwner, and defaultLHSSymbol
     */

    // TODO: the transition cost for phrase model, arity penalty, word penalty are all zero, except
    // the LM cost
    for (Node<Integer> node : inputLattice) {
      for (Arc<Integer> arc : node.getOutgoingArcs()) {
        // create a rule, but do not add into the grammar trie
        // TODO: which grammar should we use to create an OOV rule?
        int sourceWord = arc.getLabel();

        // Determine if word is actual OOV.
        if (JoshuaConfiguration.true_oovs_only) {
          boolean true_oov = true;
          for (Grammar g : grammars) {
            if (g.getTrieRoot().match(sourceWord) != null
                && g.getTrieRoot().match(sourceWord).hasRules()) {
              true_oov = false;
              break;
            }
          }
          if (!true_oov) continue;
        }

        final int targetWord;
        if (JoshuaConfiguration.mark_oovs) {
          targetWord = Vocabulary.id(Vocabulary.word(sourceWord) + "_OOV");
        } else {
          targetWord = sourceWord;
        }

        Rule oov_rule = null;
        if (parseTree != null
            && (JoshuaConfiguration.constrain_parse || JoshuaConfiguration.use_pos_labels)) {
          Collection<Integer> labels =
              parseTree.getConstituentLabels(node.getNumber(), node.getNumber() + 1);
          for (int l : labels)
            oov_rule =
                this.grammars[grammars.length - 1].constructLabeledOOVRule(
                    this.featureFunctions.size(), sourceWord, targetWord, l, useMaxLMCostForOOV);
        } else {
          oov_rule =
              this.grammars[grammars.length - 1].constructOOVRule(this.featureFunctions.size(),
                  sourceWord, targetWord, useMaxLMCostForOOV);
          oov_rule.estimateRuleCost(featureFunctions);
        }

        if (manualConstraintsHandler.containHardRuleConstraint(node.getNumber(), arc.getTail()
            .getNumber())) {
          // do not add the oov axiom
          logger.fine("Using hard rule constraint for span " + node.getNumber() + ", "
              + arc.getTail().getNumber());
        } else {
          addAxiom(node.getNumber(), arc.getTail().getNumber(), oov_rule,
              new SourcePath().extend(arc));
          logger.finer("Adding OOV rule:\t" + oov_rule.toString());
        }
      }
    }

    for (FeatureFunction ff : this.featureFunctions)
      if (ff instanceof SourceDependentFF) ((SourceDependentFF) ff).setSource(sentence);

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

    logger.fine("[" + segmentID + "] SPAN(" + i + "," + j + ")");

    if (JoshuaConfiguration.pop_limit > 0) {
      /*
       * We want to implement proper cube-pruning at the span level, with pruning controlled with
       * the specification of a single parameter, a pop-limit on the number of items. This pruning
       * would be across all DotCharts (that is, across all grammars) and across all items in the
       * span, regardless of other state (such as language model state or the lefthand side).
       * 
       * The existing implementation prunes in a much less straightforward fashion. Each Dotnode
       * within each span is examined, and applicable rules compete amongst each other. The number
       * of them that is kept is not absolute, but is determined by some combination of the maximum
       * heap size and the score differences. The score differences occur across items in the whole
       * span.
       */

      /* STEP 1: create the heap, and seed it with all of the candidate states */
      PriorityQueue<CubePruneState> candidates = new PriorityQueue<CubePruneState>();

      // this records states we have already visited
      HashSet<CubePruneState> visitedStates = new HashSet<CubePruneState>();

      // seed it with the beginning states
      // for each applicable grammar
      for (int g = 0; g < grammars.length; g++) {
        if (!grammars[g].hasRuleForSpan(i, j, sourceLength)
            || null == dotcharts[g].getDotCell(i, j)) continue;
        // for each rule with applicable rules
        for (DotNode dotNode : dotcharts[g].getDotCell(i, j).getDotNodes()) {
          RuleCollection ruleCollection = dotNode.getApplicableRules();
          if (ruleCollection == null) continue;

          // create span if required
          if (cells[i][j] == null) cells[i][j] = new Cell(this, goalSymbolID);

          List<Rule> sortedAndFilteredRules =
              manualConstraintsHandler.filterRules(i, j, ruleCollection.getSortedRules());
          SourcePath sourcePath = dotNode.getSourcePath();

          if (null == sortedAndFilteredRules || sortedAndFilteredRules.size() <= 0) continue;

          int arity = ruleCollection.getArity();

          // Rules that have no nonterminals in them so far
          // are added to the chart with no pruning
          if (arity == 0) {
            for (Rule rule : sortedAndFilteredRules) {
              cells[i][j].addHyperEdgeInCell(new ComputeNodeResult(this.featureFunctions, rule,
                  null, i, j, sourcePath, stateComputers, this.segmentID), rule, i, j, null,
                  sourcePath, true);
            }
          } else {

            Rule bestRule = sortedAndFilteredRules.get(0);
            List<HGNode> currentAntNodes = new ArrayList<HGNode>();
            List<SuperNode> superNodes = dotNode.getAntSuperNodes();
            for (SuperNode si : superNodes) {
              // TODO: si.nodes must be sorted
              currentAntNodes.add(si.nodes.get(0));
            }

            ComputeNodeResult result =
                new ComputeNodeResult(featureFunctions, bestRule, currentAntNodes, i, j,
                    sourcePath, this.stateComputers, this.segmentID);

            int[] ranks = new int[1 + superNodes.size()];
            for (int r = 0; r < ranks.length; r++)
              ranks[r] = 1;

            CubePruneState bestState = new CubePruneState(result, ranks, bestRule, currentAntNodes);
            bestState.setDotNode(dotNode);
            candidates.add(bestState);
            visitedStates.add(bestState);
          }
        }
      }

      int popCount = 0;
      while (candidates.size() > 0 && ++popCount <= JoshuaConfiguration.pop_limit) {
        CubePruneState state = candidates.poll();
        DotNode dotNode = state.getDotNode();
        Rule currentRule = state.rule;
        SourcePath sourcePath = dotNode.getSourcePath();
        List<SuperNode> superNodes = dotNode.getAntSuperNodes();
        List<Rule> rules =
            manualConstraintsHandler.filterRules(i, j, dotNode.getApplicableRules()
                .getSortedRules());

        List<HGNode> currentAntNodes = new ArrayList<HGNode>(state.antNodes);

        // add the hypothesis to the chart
        cells[i][j].addHyperEdgeInCell(state.nodeStatesTbl, state.rule, i, j, state.antNodes,
            sourcePath, true);

        // expand the hypothesis
        for (int k = 0; k < state.ranks.length; k++) {

          // get new_ranks, which is the same as the old
          // ranks, with the current index extended
          int[] newRanks = new int[state.ranks.length];
          for (int d = 0; d < state.ranks.length; d++)
            newRanks[d] = state.ranks[d];

          newRanks[k] = state.ranks[k] + 1;

          // can't extend
          if ((k == 0 && newRanks[k] > rules.size())
              || (k != 0 && newRanks[k] > superNodes.get(k - 1).nodes.size())) continue;

          // k = 0 means we extend the rule being used
          // k > 0 means we extend one of the nodes

          Rule oldRule = null;
          HGNode oldItem = null;
          if (k == 0) { // slide rule
            oldRule = currentRule;
            currentRule = rules.get(newRanks[k] - 1);
          } else { // slide ant
            oldItem = currentAntNodes.get(k - 1); // conside k == 0 is rule
            currentAntNodes.set(k - 1, superNodes.get(k - 1).nodes.get(newRanks[k] - 1));
          }

          CubePruneState nextState =
              new CubePruneState(new ComputeNodeResult(featureFunctions, currentRule,
                  currentAntNodes, i, j, sourcePath, stateComputers, this.segmentID), newRanks,
                  currentRule, currentAntNodes);
          nextState.setDotNode(dotNode);

          if (visitedStates.contains(nextState)) // explored before
            continue;

          visitedStates.add(nextState);
          candidates.add(nextState);

          // recover
          if (k == 0)
            currentRule = oldRule;
          else
            currentAntNodes.set(k - 1, oldItem);

        }
      }

    } else {
      for (int k = 0; k < this.grammars.length; k++) {
        // grammars have a maximum input span they'll apply to
        if (this.grammars[k].hasRuleForSpan(i, j, sourceLength)
            && null != this.dotcharts[k].getDotCell(i, j)) {

          // foreach dotnode in the span
          for (DotNode dotNode : this.dotcharts[k].getDotCell(i, j).getDotNodes()) {
            // foreach applicable rule (rules with the same source side)
            RuleCollection ruleCollection = dotNode.getTrieNode().getRuleCollection();
            if (ruleCollection != null) { // have rules under this trienode
              // TODO: filter the rule according to LHS constraint
              // complete the cell
              completeCell(i, j, dotNode, ruleCollection.getSortedRules(),
                  ruleCollection.getArity(), dotNode.getSourcePath());

            }
          }
        }
      }
    }
  }

  /**
   * a parser that can handle: - multiple grammars - on the fly binarization - unary rules (without
   * cycle)
   * */

  public HyperGraph expand() {
    logger.fine("Begin expand.");

    for (int width = 1; width <= sourceLength; width++) {
      for (int i = 0; i <= sourceLength - width; i++) {
        int j = i + width;
        if (logger.isLoggable(Level.FINEST))
          logger.finest(String.format("Processing span (%d, %d)", i, j));


        // (1)=== expand the cell in dotchart
        logger.finest("Expanding cell");
        for (int k = 0; k < this.grammars.length; k++) {
          /**
           * each dotChart can act individually (without consulting other dotCharts) because it
           * either consumes the source input or the complete nonTerminals, which are both
           * grammar-independent
           **/
          this.dotcharts[k].expandDotCell(i, j);
        }


        // (2)=== populate COMPLETE rules into Chart: the regular CKY part
        logger.finest("Adding complete items into chart");

        completeSpan(i, j);

        // (3)=== process unary rules (e.g., S->X, NP->NN), just add these items in chart, assume
        // acyclic
        logger.finest("Adding unary items into chart");
        addUnaryNodes(this.grammars, i, j);


        // (4)=== in dot_cell(i,j), add dot-nodes that start from the /complete/ superIterms in
        // chart_cell(i,j)
        logger.finest("Initializing new dot-items that start from complete items in this cell");
        for (int k = 0; k < this.grammars.length; k++) {
          if (this.grammars[k].hasRuleForSpan(i, j, sourceLength)) {
            this.dotcharts[k].startDotItems(i, j);
          }
        }

        // (5)=== sort the nodes in the cell
        /**
         * Cube-pruning requires the nodes being sorted, when prunning for later/wider cell.
         * Cuebe-pruning will see superNode, which contains a list of nodes. getSortedNodes() will
         * make the nodes in the superNode get sorted
         */
        if (null != this.cells[i][j]) {
          this.cells[i][j].getSortedNodes();
        }
      }
    }

    logStatistics(Level.INFO);

    // transition_final: setup a goal item, which may have many deductions
    if (null == this.cells[0][sourceLength]
        || !this.goalBin.transitToGoal(this.cells[0][sourceLength], this.featureFunctions,
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
    return this.cells[i][j];
  }


  // ===============================================================
  // Private methods
  // ===============================================================

  private void logStatistics(Level level) {
    logger
        .log(
            level,
            String
                .format(
                    "ADDED: %d; MERGED: %d; PRUNED: %d; PRE-PRUNED: %d, FUZZ1: %d, FUZZ2: %d; DOT-ITEMS ADDED: %d",
                    this.nAdded, this.nMerged, this.nPrunedItems, this.nPreprunedEdges,
                    this.nPreprunedFuzz1, this.nPreprunedFuzz2, this.nDotitemAdded));
  }


  /**
   * agenda based extension: this is necessary in case more than two unary rules can be applied in
   * topological order s->x; ss->s for unary rules like s->x, once x is complete, then s is also
   * complete
   */
  private int addUnaryNodes(Grammar[] grs, int i, int j) {

    Cell chartBin = this.cells[i][j];
    if (null == chartBin) {
      return 0;
    }
    int qtyAdditionsToQueue = 0;
    ArrayList<HGNode> queue = new ArrayList<HGNode>(chartBin.getSortedNodes());
    HashSet<Integer> seen_lhs = new HashSet<Integer>();

    logger.finest("Adding unary to [" + i + ", " + j + "]");

    while (queue.size() > 0) {
      HGNode node = queue.remove(0);
      seen_lhs.add(node.lhs);

      for (Grammar gr : grs) {
        if (!gr.hasRuleForSpan(i, j, sourceLength)) continue;

        Trie childNode = gr.getTrieRoot().match(node.lhs); // match rule and complete part
        if (childNode != null && childNode.getRuleCollection() != null
            && childNode.getRuleCollection().getArity() == 1) { // have unary rules under this
                                                                // trienode

          ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
          antecedents.add(node);
          List<Rule> rules = childNode.getRuleCollection().getSortedRules();

          for (Rule rule : rules) { // for each unary rules
            ComputeNodeResult states =
                new ComputeNodeResult(this.featureFunctions, rule, antecedents, i, j,
                    new SourcePath(), stateComputers, this.segmentID);
            HGNode resNode =
                chartBin
                    .addHyperEdgeInCell(states, rule, i, j, antecedents, new SourcePath(), true);

            if (logger.isLoggable(Level.FINEST)) logger.finest(rule.toString());

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


  /** axiom is for rules with zero-arity */
  public void addAxiom(int i, int j, Rule rule, SourcePath srcPath) {
    if (null == this.cells[i][j]) {
      this.cells[i][j] = new Cell(this, this.goalSymbolID);
    }

    this.cells[i][j].addHyperEdgeInCell(new ComputeNodeResult(this.featureFunctions, rule, null, i,
        j, srcPath, stateComputers, segmentID), rule, i, j, null, srcPath, false);

    // combiner.addAxiom(this, this.cells[i][j], i, j, rule, srcPath);
  }



  private void completeCell(int i, int j, DotNode dotNode, List<Rule> sortedRules, int arity,
      SourcePath srcPath) {
    if (logger.isLoggable(Level.FINEST)) logger.finest("\n\n CELL (" + i + ", " + j + ")");

    if (manualConstraintsHandler.containHardRuleConstraint(i, j)) {
      logger.fine("Hard rule constraint for span " + i + ", " + j);
      return; // do not add any nodes
    }

    if (null == this.cells[i][j]) {
      this.cells[i][j] = new Cell(this, this.goalSymbolID);
    }

    List<Rule> filteredRules;
    if (JoshuaConfiguration.constrain_parse) {
      Collection<Integer> labels = parseTree.getConstituentLabels(i, j);
      labels.addAll(parseTree.getConcatenatedLabels(i, j));
      labels.addAll(parseTree.getCcgLabels(i, j));

      for (int l : labels)
        logger.finest("Allowing label: " + Vocabulary.word(l));

      filteredRules = new ArrayList<Rule>(sortedRules.size());
      for (Rule r : sortedRules)
        if (r.getLHS() == goalSymbolID || labels.contains(r.getLHS())) filteredRules.add(r);
    } else {
      // combinations: rules, antecedent items
      filteredRules = manualConstraintsHandler.filterRules(i, j, sortedRules);
    }

    if (logger.isLoggable(Level.FINEST)) for (Rule r : filteredRules)
      logger.finest(r.toString() + " num_feats: " + r.getFeatureScores().length);

    if (arity == 0)
      combiner.addAxioms(this, this.cells[i][j], i, j, filteredRules, srcPath);
    else
      combiner.combine(this, this.cells[i][j], i, j, dotNode.getAntSuperNodes(), filteredRules,
          arity, srcPath);
  }
}
