package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;
import joshua.util.ChartSpan;

/**
 * The DotChart handles Earley-style implicit binarization of translation rules.
 * 
 * The {@link DotNode} object represents the (possibly partial) application of a synchronous rule.
 * The implicit binarization is maintained with a pointer to the {@link Trie} node in the grammar,
 * for easy retrieval of the next symbol to be matched. At every span (i,j) of the input sentence,
 * every incomplete DotNode is examined to see whether it (a) needs a terminal and matches against
 * the final terminal of the span or (b) needs a nonterminal and matches against a completed
 * nonterminal in the main chart at some split point (k,j).
 * 
 * Once a rule is completed, it is entered into the {@link DotChart}. {@link DotCell} objects are
 * used to group completed DotNodes over a span.
 * 
 * There is a separate DotChart for every grammar.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Matt Post <post@cs.jhu.edu>
 * @author Kristy Hollingshead Seitz
 */
class DotChart {

  // ===============================================================
  // Package-protected instance fields
  // ===============================================================
  /**
   * Two-dimensional chart of cells. Some cells might be null. This could definitely be represented
   * more efficiently, since only the upper half of this triangle is every used.
   */
  private ChartSpan<DotCell> dotcells;

  public DotCell getDotCell(int i, int j) {
    return dotcells.get(i, j);
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

  /* Length of input sentence. */
  private final int sentLen;

  /* Represents the input sentence being translated. */
  private final Lattice<Integer> input;

  /* If enabled, rule terminals are treated as regular expressions. */
  private final boolean regexpMatching;
  /*
   * nonTerminalMatcher determines the behavior of nonterminal matching: strict or soft-syntactic
   * matching
   */
  private final NonterminalMatcher nonTerminalMatcher;


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



  public DotChart(Lattice<Integer> input, Grammar grammar, Chart chart,
      NonterminalMatcher nonTerminalMatcher, boolean regExpMatching) {

    this.dotChart = chart;
    this.pGrammar = grammar;
    this.input = input;
    this.sentLen = input.size();

    this.dotcells = new ChartSpan<DotCell>(sentLen, null);
    this.nonTerminalMatcher = nonTerminalMatcher;
    this.regexpMatching = regExpMatching;

    // seeding the dotChart
    seed();
  }


  /*
   * public DotChart(Lattice<Integer> input, Grammar grammar, Chart chart, boolean
   * useRegexpMatching) { this.dotChart = chart; this.pGrammar = grammar; this.input = input;
   * this.sentLen = input.size(); this.dotcells = new ChartSpan<DotCell>(sentLen, null);
   * 
   * // seeding the dotChart seed();
   * 
   * this.regexpMatching = useRegexpMatching; }
   */

  // ===============================================================
  // Package-protected methods
  // ===============================================================

  /**
   * Add initial dot items: dot-items pointer to the root of the grammar trie.
   */
  void seed() {
    for (int j = 0; j <= sentLen - 1; j++) {
      if (pGrammar.hasRuleForSpan(j, j, input.distance(j, j))) {
        if (null == pGrammar.getTrieRoot()) {
          throw new RuntimeException("trie root is null");
        }
        addDotItem(pGrammar.getTrieRoot(), j, j, null, null, new SourcePath());
      }
    }
  }

  /**
   * This function computes all possible expansions of all rules over the provided span (i,j). By
   * expansions, we mean the moving of the dot forward (from left to right) over a nonterminal or
   * terminal symbol on the rule's source side.
   * 
   * There are two kinds of expansions:
   * 
   * <ol>
   * <li>Expansion over a nonterminal symbol. For this kind of expansion, a rule has a dot
   * immediately prior to a source-side nonterminal. The main Chart is consulted to see whether
   * there exists a completed nonterminal with the same label. If so, the dot is advanced.
   * 
   * Discovering nonterminal expansions is a matter of enumerating all split points k such that i <
   * k and k < j. The nonterminal symbol must exist in the main Chart over (k,j).
   * 
   * <li>Expansion over a terminal symbol. In this case, expansion is a simple matter of determing
   * whether the input symbol at position j (the end of the span) matches the next symbol in the
   * rule. This is equivalent to choosing a split point k = j - 1 and looking for terminal symbols
   * over (k,j). Note that phrases in the input rule are handled one-by-one as we consider longer
   * spans.
   * </ol>
   */
  void expandDotCell(int i, int j) {
    if (logger.isLoggable(Level.FINEST))
      logger.finest("Expanding dot cell (" + i + "," + j + ")");

    /*
     * (1) If the dot is just to the left of a non-terminal variable, we look for theorems or axioms
     * in the Chart that may apply and extend the dot position. We look for existing axioms over all
     * spans (k,j), i < k < j.
     */
    for (int k = i + 1; k < j; k++) {
      extendDotItemsWithProvedItems(i, k, j, false);
    }

    /*
     * (2) If the the dot-item is looking for a source-side terminal symbol, we simply match against
     * the input sentence and advance the dot.
     */
    Node<Integer> node = input.getNode(j - 1);
    for (Arc<Integer> arc : node.getOutgoingArcs()) {

      int last_word = arc.getLabel();
      int arc_len = arc.getHead().getNumber() - arc.getTail().getNumber();

      // int last_word=foreign_sent[j-1]; // input.getNode(j-1).getNumber(); //

      if (null != dotcells.get(i, j - 1)) {
        // dotitem in dot_bins[i][k]: looking for an item in the right to the dot


        for (DotNode dotNode : dotcells.get(i, j - 1).getDotNodes()) {

          // String arcWord = Vocabulary.word(last_word);
          // Assert.assertFalse(arcWord.endsWith("]"));
          // Assert.assertFalse(arcWord.startsWith("["));
          // logger.info("DotChart.expandDotCell: " + arcWord);

          // List<Trie> child_tnodes = ruleMatcher.produceMatchingChildTNodesTerminalevel(dotNode,
          // last_word);


          List<Trie> child_tnodes = null;



          if (this.regexpMatching) {
            child_tnodes = matchAll(dotNode, last_word);
          } else {
            Trie child_node = dotNode.trieNode.match(last_word);
            child_tnodes = Arrays.asList(child_node);
          }


          if (!(child_tnodes == null || child_tnodes.isEmpty())) {
            for (Trie child_tnode : child_tnodes) {
              if (null != child_tnode) {
                addDotItem(child_tnode, i, j - 1 + arc_len, dotNode.antSuperNodes, null,
                    dotNode.srcPath.extend(arc));


              }
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
   * Attempt to combine an item in the dot chart with an item in the main chart to create a new item
   * in the dot chart. The DotChart item is a {@link DotNode} begun at position i with the dot
   * currently at position k, that is, a partially-applied rule.
   * 
   * In other words, this method looks for (proved) theorems or axioms in the completed chart that
   * may apply and extend the dot position.
   * 
   * @param i Start index of a dot chart item
   * @param k End index of a dot chart item; start index of a completed chart item
   * @param j End index of a completed chart item
   * @param skipUnary if true, don't extend unary rules
   */
  private void extendDotItemsWithProvedItems(int i, int k, int j, boolean skipUnary) {
    if (this.dotcells.get(i, k) == null || this.dotChart.getCell(k, j) == null) {
      return;
    }

    // complete super-items (items over the same span with different LHSs)
    List<SuperNode> superNodes = new ArrayList<SuperNode>(this.dotChart.getCell(k, j)
        .getSortedSuperItems().values());

    /* For every partially complete item over (i,k) */
    for (DotNode dotNode : dotcells.get(i, k).dotNodes) {
      /* For every completed nonterminal in the main chart */
      for (SuperNode superNode : superNodes) {


        // String arcWord = Vocabulary.word(superNode.lhs);
        // logger.info("DotChart.extendDotItemsWithProvedItems: " + arcWord);
        // Assert.assertTrue(arcWord.endsWith("]"));
        // Assert.assertTrue(arcWord.startsWith("["));



        /*
         * Regular Expression matching allows for a regular-expression style rules in the grammar,
         * which allows for a very primitive treatment of morphology. This is an advanced,
         * undocumented feature that introduces a complexity, in that the next "word" in the grammar
         * rule might match more than one outgoing arc in the grammar trie.
         */
        List<Trie> child_tnodes = nonTerminalMatcher.produceMatchingChildTNodesNonterminalLevel(
            dotNode, superNode);


        if (!child_tnodes.isEmpty()) {


          for (Trie child_tnode : child_tnodes) {
            if (child_tnode != null) {
              if ((!skipUnary) || (child_tnode.hasExtensions())) {
                addDotItem(child_tnode, i, j, dotNode.getAntSuperNodes(), superNode, dotNode
                    .getSourcePath().extendNonTerminal());
              }
            }
          }
        }
      }
    }
  }

  /*
   * We introduced the ability to have regular expressions in rules for matching against terminals.
   * For example, you could have the rule
   * 
   * <pre> [X] ||| l?s herman?s ||| siblings </pre>
   * 
   * When this is enabled for a grammar, we need to test against *all* (positive) outgoing arcs of
   * the grammar trie node to see if any of them match, and then return the whole set. This is quite
   * expensive, which is why you should only enable regular expressions for small grammars.
   */

  private ArrayList<Trie> matchAll(DotNode dotNode, int wordID) {
    ArrayList<Trie> trieList = new ArrayList<Trie>();
    HashMap<Integer, ? extends Trie> childrenTbl = dotNode.trieNode.getChildren();

    if (childrenTbl != null && wordID >= 0) {
      // get all the extensions, map to string, check for *, build regexp
      for (Integer arcID : childrenTbl.keySet()) {
        if (arcID == wordID) {
          trieList.add(childrenTbl.get(arcID));
        } else {
          String arcWord = Vocabulary.word(arcID);
          if (Vocabulary.word(wordID).matches(arcWord)) {
            trieList.add(childrenTbl.get(arcID));
          }
        }
      }
    }
    return trieList;
  }


  /**
   * Creates a {@link DotNode} and adds it into the {@link DotChart} at the correct place. These
   * are (possibly incomplete) rule applications. 
   * 
   * @param tnode the trie node pointing to the location ("dot") in the grammar trie
   * @param i
   * @param j
   * @param antSuperNodesIn the supernodes representing the rule's tail nodes
   * @param curSuperNode the lefthand side of the rule being created
   * @param srcPath the path taken through the input lattice
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
    if (dotcells.get(i, j) == null) {
      dotcells.set(i, j, new DotCell());
    }
    dotcells.get(i, j).addDotNode(item);
    dotChart.nDotitemAdded++;

    if (logger.isLoggable(Level.FINEST)) {
      logger.finest(String.format("Add a dotitem in cell (%d, %d), n_dotitem=%d, %s", i, j,
          dotChart.nDotitemAdded, srcPath));

      RuleCollection rules = tnode.getRuleCollection();
      if (rules != null) {
        for (Rule r : rules.getRules()) {
          // System.out.println("rule: "+r.toString());
          logger.finest(r.toString());
        }
      }
    }
  }

  // ===============================================================
  // Package-protected classes
  // ===============================================================

  /**
   * A DotCell groups together DotNodes that have been applied over a particular span. A DotNode, in
   * turn, is a partially-applied grammar rule, represented as a pointer into the grammar trie
   * structure.
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
   * A DotNode represents the partial application of a rule rooted to a particular span (i,j). It
   * maintains a pointer to the trie node in the grammar for efficient mapping.
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
      if (obj == null)
        return false;
      if (!this.getClass().equals(obj.getClass()))
        return false;
      DotNode state = (DotNode) obj;

      /*
       * Technically, we should be comparing the span inforamtion as well, but that would require us
       * to store it, increasing memory requirements, and we should be able to guarantee that we
       * won't be comparing DotNodes across spans.
       */
      // if (this.i != state.i || this.j != state.j)
      // return false;

      if (this.trieNode != state.trieNode)
        return false;

      return true;
    }

    /**
     * Technically the hash should include the span (i,j), but since DotNodes are grouped by span,
     * this isn't necessary, and we gain something by not having to store the span.
     */
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
