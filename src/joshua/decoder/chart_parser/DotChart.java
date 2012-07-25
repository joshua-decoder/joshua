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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;

/**
 * The DotChart handles the building of the -LM forest. The set of (Dot)Items over a span represent
 * the rules that have been completely applied through the implicit binarization. These DotItems are
 * available to be incorporated into the main chart after the LM score has been applied (assuming
 * they pass pruning).
 * 
 * DotItem represent (possibly partial) application of synchronous rules that have been implicitly
 * binarized. As spans are considered, the next symbol in the rule's source right-hand side is
 * matched against proved items (items in the +LM chart, Chart.java) or input symbols. Once the rule
 * is complete, it is entered into the DotChart.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 */
class DotChart {

  // ===============================================================
  // Package-protected instance fields
  // ===============================================================
  /**
   * Two-dimensional chart of cells. Some cells might be null. This could definitely be represented
   * more efficiently, especially since access is already mitigated through an accessor function.
   */
  private DotCell[][] dotcells;

  public DotCell getDotCell(int i, int j) {
    return dotcells[i][j];
  }


  // ===============================================================
  // Private instance fields (maybe could be protected instead)
  // ===============================================================

  /**
   * CKY+ style parse chart in which completed span entries are stored.
   */
  private Chart dotChart;

  /**
   * Translation grammar which contains the translation rules.
   */
  private Grammar pGrammar;


  /** Length of input sentence. */
  private final int sentLen;

  /** Represents the input sentence being translated. */
  private final Lattice<Integer> input;


  // ===============================================================
  // Static fields
  // ===============================================================

  private static final Logger logger = Logger.getLogger(DotChart.class.getName());


  // ===============================================================
  // Constructors
  // ===============================================================

  // TODO: Maybe this should be a non-static inner class of Chart. That would give us implicit
  // access to all the arguments of this constructor. Though we would need to take an argument, i,
  // to know which Chart.this.grammars[i] to use.

  /**
   * Constructs a new dot chart from a specified input lattice, a translation grammar, and a parse
   * chart.
   * 
   * @param input A lattice which represents an input sentence.
   * @param grammar A translation grammar.
   * @param chart A CKY+ style chart in which completed span entries are stored.
   */
  public DotChart(Lattice<Integer> input, Grammar grammar, Chart chart) {
    this.dotChart = chart;
    this.pGrammar = grammar;
    this.input = input;
    this.sentLen = input.size();
    this.dotcells = new DotCell[sentLen][sentLen + 1];

    // seeding the dotChart
    seed();
  }


  /**
   * Constructs a new dot chart from a specified input sentence, a translation grammar, and a parse
   * chart.
   * 
   * @param input An array of integers which represents an input sentence.
   * @param grammar A translation grammar.
   * @param chart A CKY+ style chart in which completed span entries are stored.
   */
  /*
   * public DotChart(int[] inputSentence, Grammar grammar, Chart chart) {
   * 
   * if (logger.isLoggable(Level.FINEST))
   * logger.finest("Constructing DotChart from input sentence: " + Arrays.toString(inputSentence));
   * 
   * this.p_chart = chart; this.p_grammar = grammar; this.sent_len = inputSentence.length;
   * this.l_dot_bins = new DotBin[sent_len][sent_len+1];
   * 
   * Integer[] input = new Integer[inputSentence.length]; for (int i = 0; i < inputSentence.length;
   * i++) { input[i] = inputSentence[i]; }
   * 
   * this.input = new Lattice<Integer>(input); }
   */


  // ===============================================================
  // Package-protected methods
  // ===============================================================

  /**
   * add intial dot items: dot-items pointer to the root of the grammar trie.
   */
  void seed() {
    for (int j = 0; j <= sentLen - 1; j++) {
      if (pGrammar.hasRuleForSpan(j, j, sentLen)) {
        if (null == pGrammar.getTrieRoot()) {
          throw new RuntimeException("trie root is null");
        }
        addDotItem(pGrammar.getTrieRoot(), j, j, null, null, new SourcePath());
      }
    }
  }


  /**
   * This function computes all possible expansions of all rules over the provided span (i,j).  By
   * expansions, we mean the moving of the dot forward (from left to right) over a nonterminal or
   * terminal symbol on the rule's source side.
   *
   * There are two kinds of expansions:
   *
   * 1. Expansion over a nonterminal symbol.  For this kind of expansion, a rule has a dot
   *    immediately prior to a source-side nonterminal.  The main Chart is consulted to see whether
   *    there exists a completed nonterminal with the same label.  If so, the dot is advanced.
   *
   *    Discovering nonterminal expansions is a matter of enumerating all split points k such that
   *    i < k and k < j.  The nonterminal symbol must exist in the main Chart over (k,j).
   *
   * 2. Expansion over a terminal symbol.  In this case, expansion is a simple matter of determing
   *    whether the input symbol at position j (the end of the span) matches the next symbol in the
   *    rule.  This is equivalent to choosing a split point k = j - 1 and looking for terminal
   *    symbols over (k,j).  Note that phrases in the input rule are handled one-by-one as we
   *    consider longer spans.
   */
  void expandDotCell(int i, int j) {
    if (logger.isLoggable(Level.FINEST)) logger.finest("Expanding dot cell (" + i + "," + j + ")");

    /* (1) If the dot is just to the left of a non-terminal variable, we look for theorems or axioms
     * in the Chart that may apply and extend the dot position.  We look for existing axioms over
     * all spans (k,j), i < k < j.
     */
    for (int k = i + 1; k < j; k++) {
      extendDotItemsWithProvedItems(i, k, j, false);
    }

    /* (2) If the the dot-item is looking for a source-side terminal symbol, we simply match against
     * the input sentence and advance the dot.
     */
    Node<Integer> node = input.getNode(j - 1);
    for (Arc<Integer> arc : node.getOutgoingArcs()) {

      // TODO: Tail and Head are backward! FIX names!
      int last_word = arc.getLabel();
      int arc_len = arc.getTail().getNumber() - arc.getHead().getNumber();

      // int last_word=foreign_sent[j-1]; // input.getNode(j-1).getNumber(); //

      if (null != dotcells[i][j - 1]) {
        // dotitem in dot_bins[i][k]: looking for an item in the right to the dot
        for (DotNode dotNode : dotcells[i][j - 1].getDotNodes()) {
          if (null == dotNode.trieNode) {
            // We'll get one anyways in the else branch
            // TODO: better debugging.
            throw new NullPointerException("DotChart.expand_cell(" + i + "," + j + "): "
                + "Null tnode for DotItem");

          } else {
            // match the terminal
            Trie child_tnode = dotNode.trieNode.match(last_word);
            if (null != child_tnode) {
              // we do not have an ant for the terminal
              addDotItem(child_tnode, i, j - 1 + arc_len, dotNode.antSuperNodes, null,
                  dotNode.srcPath.extend(arc));
            }
          }
        }
      }
    }
  }


  /**
   * note: (i,j) is a non-terminal, this cannot be a cn-side terminal, which have been handled in
   * case2 of dotchart.expand_cell add dotitems that start with the complete super-items in
   * cell(i,j)
   */
  void startDotItems(int i, int j) {
    extendDotItemsWithProvedItems(i, i, j, true);
  }


  // ===============================================================
  // Private methods
  // ===============================================================

  /**
   * Attempt to combine an item in the dot chart with an item in the chart to create a new item in
   * the dot chart.
   * <p>
   * In other words, this method looks for (proved) theorems or axioms in the completed chart that
   * may apply and extend the dot position.
   * 
   * @param i Start index of a dot chart item
   * @param k End index of a dot chart item; start index of a completed chart item
   * @param j End index of a completed chart item
   * @param startDotItems
   */
  private void extendDotItemsWithProvedItems(int i, int k, int j, boolean startDotItems) {
    if (this.dotcells[i][k] == null || this.dotChart.getCell(k, j) == null) {
      return;
    }

    // complete super-items (items over the same span with different LHSs)
    List<SuperNode> t_ArrayList =
        new ArrayList<SuperNode>(this.dotChart.getCell(k, j).getSortedSuperItems().values());

    // dotitem in dot_bins[i][k]: looking for an item in the right to the dot
    for (DotNode dotNode : dotcells[i][k].dotNodes) {
      // see if it matches what the dotitem is looking for
      for (SuperNode superNode : t_ArrayList) {
        Trie child_tnode = dotNode.trieNode.match(superNode.lhs);
        if (null != child_tnode) {
          if (true == startDotItems && !child_tnode.hasExtensions()) {
            continue; // TODO
          }
          addDotItem(child_tnode, i, j, dotNode.getAntSuperNodes(), superNode, dotNode
              .getSourcePath().extendNonTerminal());
        }
      }
    }
  }


  /**
   * Creates a dot item and adds it into the cell(i,j) of this dot chart.
   * 
   * @param tnode
   * @param i
   * @param j
   * @param ant_s_items_in
   * @param curSuperNode
   */
  private void addDotItem(Trie tnode, int i, int j, List<SuperNode> antSuperNodesIn,
      SuperNode curSuperNode, SourcePath srcPath) {
    List<SuperNode> antSuperNodes = new ArrayList<SuperNode>();
    if (antSuperNodesIn != null) {
      antSuperNodes.addAll(antSuperNodesIn);
    }
    if (curSuperNode != null) {
      antSuperNodes.add(curSuperNode);
    }

    DotNode item = new DotNode(i, j, tnode, antSuperNodes, srcPath);
    if (dotcells[i][j] == null) {
      dotcells[i][j] = new DotCell();
    }
    dotcells[i][j].addDotNode(item);
    dotChart.nDotitemAdded++;

    if (logger.isLoggable(Level.FINEST)) {
      logger.finest(String.format("Add a dotitem in cell (%d, %d), n_dotitem=%d, %s", i, j,
          dotChart.nDotitemAdded, srcPath));

      RuleCollection rules = tnode.getRuleCollection();
      if (rules != null) {
        for (Rule r : rules.getRules()) {
          logger.finest(r.toString());
        }
      }
    }
  }


  // ===============================================================
  // Package-protected classes
  // ===============================================================

  /**
   * Bin is a cell in parsing terminology
   */
  static class DotCell {

    // Package-protected fields
    private List<DotNode> dotNodes = new ArrayList<DotNode>();

    public List<DotNode> getDotNodes() {
      return dotNodes;
    }

    private void addDotNode(DotNode dt) {
      /*
       * if(l_dot_items==null) l_dot_items= new ArrayList<DotItem>();
       */
      dotNodes.add(dt);
    }
  }


  /**
   * remember the dot position in which a rule has been applied so far, and remember the old
   * complete items.
   */
  static class DotNode {

    // =======================================================
    // Package-protected instance fields
    // =======================================================

    // int i, j; //start and end position in the chart
    private Trie trieNode = null; // dot_position, point to grammar trie node, this is the only
                                  // place that the DotChart points to the grammar
    private List<SuperNode> antSuperNodes = null; // pointer to SuperNode in Chart
    private SourcePath srcPath;

    public DotNode(int i, int j, Trie trieNode, List<SuperNode> antSuperNodes, SourcePath srcPath) {
      // i = i_in;
      // j = j_in;
      this.trieNode = trieNode;
      this.antSuperNodes = antSuperNodes;
      this.srcPath = srcPath;
    }

    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!this.getClass().equals(obj.getClass())) return false;
      DotNode state = (DotNode) obj;

      /*
       * Technically, we should be comparing the span inforamtion as well, but that would require us
       * to store it, increasing memory requirements, and we should be able to guarantee that we
       * won't be comparing DotNodes across spans.
       */
      // if (this.i != state.i || this.j != state.j)
      // return false;

      if (this.trieNode != state.trieNode) return false;

      return true;
    }

    public int hashCode() {
      return this.trieNode.hashCode();
    }

    // convenience function
    public RuleCollection getApplicableRules() {
      return getTrieNode().getRuleCollection();
    }

    public Trie getTrieNode() {
      return trieNode;
    }

    public SourcePath getSourcePath() {
      return srcPath;
    }

    public List<SuperNode> getAntSuperNodes() {
      return antSuperNodes;
    }
  }

}
