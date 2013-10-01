package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.List;
import joshua.corpus.Vocabulary;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;

public class RulePropertiesQuerying {

  public static final String getLHSAsString(Rule rule) {
    return Vocabulary.word(rule.getLHS());
  }

  public static List<String> getRuleSourceNonterminalStrings(Rule rule) {
    List<String> result = new ArrayList<String>();
    for (int nonTerminalIndex : rule.getForeignNonTerminals()) {
      result.add(Vocabulary.word(nonTerminalIndex));
    }
    return result;
  }

  public static List<String> getSourceNonterminalStrings(List<HGNode> tailNodes) {
    List<String> result = new ArrayList<String>();
    for (HGNode tailNode : tailNodes) {
      result.add(Vocabulary.word(tailNode.lhs));
    }
    return result;
  }

}
