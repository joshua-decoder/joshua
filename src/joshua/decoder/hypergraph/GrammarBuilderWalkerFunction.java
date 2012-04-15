package joshua.decoder.hypergraph;

import java.io.PrintStream;
import java.util.HashSet;

import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;

/**
 * This walker function builds up a new context-free grammar by visiting each node in a hypergraph.
 * For a quick overview, see Chris Dyer's 2010 NAACL paper
 * "Two monlingual parses are better than one (synchronous parse)".
 * <p>
 * From a functional-programming point of view, this walker really wants to calculate a fold over
 * the entire hypergraph: the initial value is an empty grammar, and as we visit each node, we add
 * more rules to the grammar. After we have traversed the whole hypergraph, the resulting grammar
 * will contain all rules needed for synchronous parsing.
 * <p>
 * These rules look just like the rules already present in the hypergraph, except that each
 * non-terminal symbol is annotated with the span of its node.
 */
public class GrammarBuilderWalkerFunction implements WalkerFunction {
  private MemoryBasedBatchGrammar grammar;
  private static HieroFormatReader reader = new HieroFormatReader();
  private PrintStream outStream;
  private int goalSymbol;
  private HashSet<Rule> rules;

  public GrammarBuilderWalkerFunction(String goal) {
    grammar = new MemoryBasedBatchGrammar(reader);
    outStream = null;
    goalSymbol = Vocabulary.id(goal);
    rules = new HashSet<Rule>();
  }

  public GrammarBuilderWalkerFunction(String goal, PrintStream out) {
    this(goal);
    outStream = out;
  }

  public void apply(HGNode node) {
    // System.err.printf("VISITING NODE: %s\n", getLabelWithSpan(node));
    for (HyperEdge e : node.hyperedges) {
      BilingualRule r = getRuleWithSpans(e, node);
      if (r != null && !rules.contains(r)) {
        if (outStream != null) outStream.println(r);
        grammar.addRule(r);
        rules.add(r);
      }
    }
  }

  /*
   * TODO: this can break silently and dangerously if we do try to parse sentences longer than the
   * max length. If a sentence is longer than this length, then the IDs for labeled spans aren't
   * guaranteed to be unique.
   */
  private static final int MAX_SENTENCE_LENGTH = 64;
  public static final int MAX_NTS = 500000;

  private static int getLabelWithSpan(HGNode node) {
    int x = node.i * MAX_SENTENCE_LENGTH;
    x = (x + node.j) * MAX_NTS;
    x = node.lhs - x;
    if (x > 0) {
      System.err.println("WARNING: integer overflow in node label!");
      System.err.printf("lhs = %d, i = %d, j = %d, result = %d\n", node.lhs, node.i, node.j, x);
    }
    return x;
  }

  private static String getLabelWithSpanAsString(HGNode node) {
    String label = Vocabulary.word(node.lhs);
    String cleanLabel = reader.cleanNonTerminal(label);
    String unBracketedCleanLabel = cleanLabel.substring(1, cleanLabel.length() - 1);
    return String.format("%d-%s-%d", node.i, unBracketedCleanLabel, node.j);
  }

  private boolean nodeHasGoalSymbol(HGNode node) {
    return node.lhs == goalSymbol;
  }

  private BilingualRule getRuleWithSpans(HyperEdge edge, HGNode head) {
    Rule edgeRule = edge.getRule();
    // System.err.printf("EdgeRule: %s\n", edgeRule);
    if (!(edgeRule instanceof BilingualRule)) {
      // System.err.println("edge rule is not a bilingual rule");
      return null;
    }
    int headLabel = getLabelWithSpan(head);
    // System.err.printf("Head label: %s\n", headLabel);
    // if (edge.getAntNodes() != null) {
    // for (HGNode n : edge.getAntNodes())
    // System.err.printf("> %s\n", getLabelWithSpan(n));
    // }
    int[] source = getNewSource(nodeHasGoalSymbol(head), edge);
    // if this would be unary abstract, getNewSource will be null
    if (source == null) return null;
    int[] target = getNewTargetFromSource(source);
    BilingualRule result =
        new BilingualRule(headLabel, source, target, edgeRule.getFeatureScores(),
            edgeRule.getArity());
    // System.err.printf("new rule is %s\n", result);
    return result;
  }

  private static int[] getNewSource(boolean isGlue, HyperEdge edge) {
    BilingualRule rule = (BilingualRule) edge.getRule();
    int[] english = rule.getEnglish();
    // if this is a unary abstract rule, just return null
    // TODO: except glue rules!
    if (english.length == 1 && english[0] < 0 && !isGlue) return null;
    int currNT = 1;
    int[] result = new int[english.length];
    for (int i = 0; i < english.length; i++) {
      int curr = english[i];
      if (!Vocabulary.nt(curr)) {
        result[i] = curr;
      } else {
        int index = -curr - 1;
        int label = getLabelWithSpan(edge.getAntNodes().get(index));
        result[i] = label * 2 - currNT;
        currNT++;
      }
    }
    // System.err.printf("source: %s\n", result);
    return result;
  }

  private static int[] getNewTargetFromSource(int[] source) {
    int[] result = new int[source.length];
    for (int i = 0; i < source.length; i++) {
      result[i] = source[i];
      if (Vocabulary.nt(result[i])) {
        int currNT = (Math.abs(result[i]) % 2) - 2;
        result[i] = currNT;
        source[i] -= currNT;
        source[i] /= 2;
      }
    }
    // System.err.printf("target: %s\n", result);
    return result;
  }

  private static HGNode getGoalSymbolNode(HGNode root) {
    if (root.hyperedges == null || root.hyperedges.size() == 0) {
      System.err.println("getGoalSymbolNode: root node has no hyperedges");
      return null;
    }
    return root.hyperedges.get(0).getAntNodes().get(0);
  }


  public static int goalSymbol(HyperGraph hg) {
    if (hg.goalNode == null) {
      System.err.println("goalSymbol: goalNode of hypergraph is null");
      return -1;
    }
    HGNode symbolNode = getGoalSymbolNode(hg.goalNode);
    if (symbolNode == null) return -1;
    // System.err.printf("goalSymbol: %s\n", result);
    // System.err.printf("symbol node LHS is %d\n", symbolNode.lhs);
    // System.err.printf("i = %d, j = %d\n", symbolNode.i, symbolNode.j);
    return getLabelWithSpan(symbolNode);
  }

  public Grammar getGrammar() {
    return grammar;
  }
}
