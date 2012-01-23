package joshua.decoder.hypergraph;

import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.corpus.Vocabulary;
import java.util.Set;
import java.util.HashSet;

import java.io.PrintStream;

/**
 * This walker function builds up a new context-free grammar by visiting
 * each node in a hypergraph. For a quick overview, see Chris Dyer's 2010
 * NAACL paper "Two monlingual parses are better than one (synchronous parse)".
 * <p>
 * From a functional-programming point of view, this walker really wants to
 * calculate a fold over the entire hypergraph: the initial value is an
 * empty grammar, and as we visit each node, we add more rules to the grammar.
 * After we have traversed the whole hypergraph, the resulting grammar
 * will contain all rules needed for synchronous parsing.
 * <p>
 * These rules look just like the rules already present in the hypergraph,
 * except that each non-terminal symbol is annotated with the span of its
 * node.
 */
public class GrammarBuilderWalkerFunction implements WalkerFunction
{
    private MemoryBasedBatchGrammar grammar;
    private static HieroFormatReader reader = new HieroFormatReader();
	private PrintStream outStream;
	private int goalSymbol;

    public GrammarBuilderWalkerFunction(String goal)
    {
        grammar = new MemoryBasedBatchGrammar(reader);
		outStream = null;
		goalSymbol = Vocabulary.id(goal);
    }

	public GrammarBuilderWalkerFunction(String goal, PrintStream out)
	{
		this(goal);
		outStream = out;
	}

    public void apply(HGNode node)
    {
//        System.err.printf("VISITING NODE: %s\n", getLabelWithSpan(node));
        for (HyperEdge e : node.hyperedges) {
            BilingualRule r = getRuleWithSpans(e, node);
            if (r != null) {
				if (outStream != null)
					outStream.println(r);
                grammar.addRule(r);
			}
        }
    }

    private static String getLabelWithSpan(HGNode node)
    {
        String lhs = Vocabulary.word(node.lhs);
        String cleanLhs = reader.cleanNonTerminal(lhs);
        String label = cleanLhs.substring(1, cleanLhs.length() - 1);
        return String.format("%d-%s-%d", node.i, label, node.j);
    }

	private boolean nodeHasGoalSymbol(HGNode node)
	{
		return node.lhs == goalSymbol;
	}

    private BilingualRule getRuleWithSpans(HyperEdge edge, HGNode head)
    {
        Rule edgeRule = edge.getRule();
//        System.err.printf("EdgeRule: %s\n", edgeRule);
        if (!(edgeRule instanceof BilingualRule)) {
//            System.err.println("edge rule is not a bilingual rule");
            return null;
        }
        String headLabel = String.format("[%s]", getLabelWithSpan(head));
//        System.err.printf("Head label: %s\n", headLabel);
//        if (edge.getAntNodes() != null) {
//            for (HGNode n : edge.getAntNodes())
//                System.err.printf("> %s\n", getLabelWithSpan(n));
//        }
        int [] source = getNewSource(nodeHasGoalSymbol(head), edge);
		// if this would be unary abstract, getNewSource will be null
		if (source == null)
			return null;
        int [] target = getNewTargetFromSource(source);
        BilingualRule result = new BilingualRule(Vocabulary.id(headLabel), source, target, edgeRule.getFeatureScores(), edgeRule.getArity());
//        System.err.printf("new rule is %s\n", result);
        return result;
    }

    private static int [] getNewSource(boolean isGlue, HyperEdge edge)
    {
        BilingualRule rule = (BilingualRule) edge.getRule();
        int [] english = rule.getEnglish();
		// if this is a unary abstract rule, just return null
		// TODO: except glue rules!
		if (english.length == 1 && english[0] < 0 && !isGlue)
			return null;
        int [] result = new int[english.length];
        for (int i = 0; i < english.length; i++) {
            int curr = english[i];
            if (curr >= 0) {
                result[i] = curr;
            }
            else {
                int index = -curr - 1;
                String label = getLabelWithSpan(edge.getAntNodes().get(index));
                result[i] = Vocabulary.id(String.format("[%s,%d]", label, -curr));
            }
        }
//        System.err.printf("source: %s\n", Vocabulary.getWords(result));
        return result;
    }

    private static int [] getNewTargetFromSource(int [] source)
    {
        int [] result = new int[source.length];
        int ntIndex = 1;
        for (int i = 0; i < source.length; i++) {
            int curr = source[i];
            if (!Vocabulary.nt(curr)) {
                result[i] = curr;
            }
            else {
                result[i] = -ntIndex;
                ntIndex++;
            }
        }
//        System.err.printf("target: %s\n", Vocabulary.getWords(result));
        return result;
    }

    private static HGNode getGoalSymbolNode(HGNode root)
    {
        if (root.hyperedges == null || root.hyperedges.size() == 0) {
            System.err.println("getGoalSymbolNode: root node has no hyperedges");
            return null;
        }
        return root.hyperedges.get(0).getAntNodes().get(0);
    }


    public static String goalSymbol(HyperGraph hg)
    {
        if (hg.goalNode == null) {
            System.err.println("goalSymbol: goalNode of hypergraph is null");
            return "[S]";
        }
        HGNode symbolNode = getGoalSymbolNode(hg.goalNode);
        if (symbolNode == null)
            return "[S]";
        String result = String.format("[%s]", getLabelWithSpan(symbolNode));
//        System.err.printf("goalSymbol: %s\n", result);
        return result;
    }

    public Grammar getGrammar()
    {
        return grammar;
    }
}

