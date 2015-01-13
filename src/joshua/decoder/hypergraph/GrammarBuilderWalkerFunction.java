package joshua.decoder.hypergraph;

import java.io.PrintStream;
import java.util.HashSet;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
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

  public GrammarBuilderWalkerFunction(String goal,JoshuaConfiguration joshuaConfiguration) {
    grammar = new MemoryBasedBatchGrammar(reader,joshuaConfiguration);
    grammar.setSpanLimit(1000);
    outStream = null;
    goalSymbol = Vocabulary.id(goal);
    rules = new HashSet<Rule>();
  }

  public GrammarBuilderWalkerFunction(String goal, PrintStream out,JoshuaConfiguration joshuaConfiguration) {
    this(goal,joshuaConfiguration);
    outStream = out;
  }

  public void apply(HGNode node) {
    // System.err.printf("VISITING NODE: %s\n", getLabelWithSpan(node));
    for (HyperEdge e : node.hyperedges) {
      Rule r = getRuleWithSpans(e, node);
      if (r != null && !rules.contains(r)) {
        if (outStream != null) outStream.println(r);
        grammar.addRule(r);
        rules.add(r);
      }
    }
  }

  private static int getLabelWithSpan(HGNode node) {
    return Vocabulary.id(getLabelWithSpanAsString(node));
  }

  private static String getLabelWithSpanAsString(HGNode node) {
    String label = Vocabulary.word(node.lhs);
    String cleanLabel = HieroFormatReader.cleanNonTerminal(label);
    String unBracketedCleanLabel = cleanLabel.substring(1, cleanLabel.length() - 1);
    return String.format("[%d-%s-%d]", node.i, unBracketedCleanLabel, node.j);
  }

  private boolean nodeHasGoalSymbol(HGNode node) {
    return node.lhs == goalSymbol;
  }

  private Rule getRuleWithSpans(HyperEdge edge, HGNode head) {
    Rule edgeRule = edge.getRule();
    // System.err.printf("EdgeRule: %s\n", edgeRule);
    if (!(edgeRule instanceof Rule)) {
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
    Rule result =
        new Rule(headLabel, source, target, edgeRule.getFeatureString(), edgeRule.getArity());
    // System.err.printf("new rule is %s\n", result);
    return result;
  }

  private static int[] getNewSource(boolean isGlue, HyperEdge edge) {
    Rule rule = edge.getRule();
    int[] english = rule.getEnglish();
    // if this is a unary abstract rule, just return null
    // TODO: except glue rules!
    if (english.length == 1 && english[0] < 0 && !isGlue) return null;
    int[] result = new int[english.length];
    for (int i = 0; i < english.length; i++) {
      int curr = english[i];
      if (!Vocabulary.nt(curr)) {
				// If it's a terminal symbol, we just copy it into the new rule.
        result[i] = curr;
      } else {
				// If it's a nonterminal, its value is -N, where N is the index
				// of the nonterminal on the source side.
				//
				// That is, if we would call a nonterminal "[X,2]", the value of
				// curr at this point is -2. And the tail node that it points at
				// is #1 (since getTailNodes() is 0-indexed).
        int index = -curr - 1;
        result[i] = getLabelWithSpan(edge.getTailNodes().get(index));
      }
    }
    // System.err.printf("source: %s\n", result);
    return result;
  }

  private static int[] getNewTargetFromSource(int[] source) {
    int[] result = new int[source.length];
		int currNT = -1; // value to stick into NT slots
    for (int i = 0; i < source.length; i++) {
      result[i] = source[i];
      if (Vocabulary.nt(result[i])) {
        result[i] = currNT;
				currNT--;
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
    return root.hyperedges.get(0).getTailNodes().get(0);
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
