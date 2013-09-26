package joshua.decoder.chart_parser;

import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

/**
 * This Interface serves to refine the behavior of DotChart using strategy.
 * Basically there are different ways that rules can be matched, either strict,
 * or soft syntactic (nonterminals can all match each other) or based on 
 * regular expression matching. This interface defines two methods that 
 * produce matching nodes for the terminal or nonterminal level. The interface 
 * is then implemented in different classes for the different types of matching
 * 
 * 
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 *
 */
public interface RuleMatcher {

  List<Trie> produceMatchingChildTNodesTerminalevel(DotNode dotNode,int lastWordIndex); 
  List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,SuperNode superNode);
}
