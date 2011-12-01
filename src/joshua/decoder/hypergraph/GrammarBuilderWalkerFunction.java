package joshua.decoder.hypergraph;

import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.format.HieroFormatReader;
import joshua.decoder.ff.tm.hash_based.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Rule;
import joshua.corpus.Vocabulary;

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

    public GrammarBuilderWalkerFunction()
    {
        grammar = new MemoryBasedBatchGrammar(reader);
    }

    public void apply(HGNode node)
    {
        for (HyperEdge e : node.hyperedges) {
            BilingualRule r = getRuleWithSpans(e, node);
            if (r != null)
                grammar.addRule(r);
        }
    }

    private static String getLabelWithSpan(HGNode node)
    {
        String lhs = Vocabulary.word(node.lhs);
        String cleanLhs = reader.cleanNonTerminal(lhs);
        String label = cleanLhs.substring(1, cleanLhs.length() - 1);
        return String.format("%d-%s-%d", node.i, label, node.j);
    }

    private static BilingualRule getRuleWithSpans(HyperEdge edge, HGNode head)
    {
        Rule edgeRule = edge.getRule();
        System.err.printf("EdgeRule: %s\n", edgeRule);
        if (!(edgeRule instanceof BilingualRule)) {
            System.err.println("edge rule is not a bilingual rule");
            return null;
        }
        String headLabel = getLabelWithSpan(head);
        System.err.printf("Head label: %s\n", headLabel);
        if (edge.getAntNodes() != null) {
            for (HGNode n : edge.getAntNodes())
                System.err.printf("> %s\n", getLabelWithSpan(n));
        }
        getNewSource(edge);
        return (BilingualRule) edgeRule;
    }

    private static int [] getNewSource(HyperEdge edge)
    {
        BilingualRule rule = (BilingualRule) edge.getRule();
        int [] english = rule.getEnglish();
        int [] result = new int[english.length];
        for (int i = 0; i < english.length; i++) {
            int curr = english[i];
            if (!Vocabulary.nt(curr)) {
                result[i] = curr;
            }
            else {
                int index = -curr - 1;
                String label = getLabelWithSpan(edge.getAntNodes().get(index));
                result[i] = Vocabulary.id(String.format("[%s,%d]", label, -curr));
            }
        }
        System.err.printf("source: %s\n", Vocabulary.getWords(result));
        return result;
    }


    public Grammar getGrammar()
    {
        return grammar;
    }
}

