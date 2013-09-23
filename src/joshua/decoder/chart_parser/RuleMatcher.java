package joshua.decoder.chart_parser;

import java.util.List;

import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

public interface RuleMatcher {

  List<Trie> produceMatchingChildTNodesTerminalevel(DotNode dotNode,int lastWordIndex); 
  List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,SuperNode superNode);
}
