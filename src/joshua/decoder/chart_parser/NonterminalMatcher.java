package joshua.decoder.chart_parser;

import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

/**
 * This Interface serves to refine the behavior of DotChart using strategy.
 * Basically there are different ways that nonterminals of rules can be matched, 
 * either strict, or soft syntactic (nonterminals can all match each other). 
 * This interface defines a method that produce matching nodes for the nonterminal 
 * level. The interface is then implemented in different classes
 *  for the different types of matching (currently just strict or soft-syntactic)
 *
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 *
 */
public interface NonterminalMatcher {
  List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode, SuperNode superNode); 
}
