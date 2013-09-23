package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import joshua.corpus.Vocabulary;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

public class RuleMatcherFactory {

  public static RuleMatcher createRuleMatcher(boolean useRegularExpressionGrammar,
      boolean useSoftConstraintMatching, Logger logger) {
    if (useRegularExpressionGrammar) {
      if (useSoftConstraintMatching) {
        return new RegularExpressionRuleMatcherSoftConstraints(logger);
      } else {
        return new RegularExpressionRuleMatcherStrict(logger);
      }
    } else {
      if (useSoftConstraintMatching) {
        return new StandardRuleMatcherSoftConstraints(logger);
      } else {
        return new StandardRuleMatcherStrict(logger);
      }

    }
  }

  protected abstract static class AbstractRuleMatcher implements RuleMatcher {

    protected final Logger logger;

    protected AbstractRuleMatcher(Logger logger) {
      this.logger = logger;
    }

    /*
     * We introduced the ability to have regular expressions in rules. When this is enabled for a
     * grammar, we first check whether there are any children. If there are, we need to try to match
     * _every rule_ against the symbol we're querying. This is expensive, which is an argument for
     * keeping your set of regular expression s small and limited to a separate grammar.
     */
    protected ArrayList<Trie> matchAllRegularExpression(DotNode dotNode, int wordID) {
      ArrayList<Trie> trieList = new ArrayList<Trie>();
      HashMap<Integer, ? extends Trie> childrenTbl = dotNode.getTrieNode().getChildren();

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

    private boolean isNonterminal(int wordIndex) {
      return wordIndex < 0;
    }

    protected List<Trie> matchAllEqualOrBothNonTerminal(DotNode dotNode, int wordID) {
      List<Trie> trieList = new ArrayList<Trie>();
      HashMap<Integer, ? extends Trie> childrenTbl = dotNode.getTrieNode().getChildren();

     // logger.info("wordID: " + wordID + " Vocabulary.word(Math.abs(wordID)) "
     //     + Vocabulary.word(Math.abs(wordID)));
      ;
      if (childrenTbl != null) {
        // get all the extensions, map to string, check for *, build regexp
        for (Integer arcID : childrenTbl.keySet()) {
          String arcWord = Vocabulary.word(arcID);
        //  logger.info("Vocabulary.word(wordID), arcWord ||| " + Vocabulary.word(wordID) + " "+ arcWord);
          String wordIdWord = Vocabulary.word(wordID);
          if (wordIdWord.equals(arcWord)) {
            trieList.add(childrenTbl.get(arcID));
          } else if (isNonterminal(wordID) && isNonterminal(arcID) && !wordIdWord.equals("[OOV]")) {
            logger.info("Substituing : " + arcWord + " for " + wordIdWord);
            trieList.add(childrenTbl.get(arcID));
          }

          //logger.info("added node for arcWord: " + arcWord);
        }
      }
     // logger.info("trieList.size(): " + trieList.size());
      return trieList;
    }
  }

  protected abstract static class StandardRuleMatcher extends AbstractRuleMatcher {

    protected StandardRuleMatcher(Logger logger) {
      super(logger);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesTerminalevel(DotNode dotNode, int lastWordIndex) {
      Trie child_node = dotNode.getTrieNode().match(lastWordIndex);
      List<Trie> child_tnodes = Arrays.asList(child_node);
      return child_tnodes;
    }
  }

  protected abstract static class RegularExpressionRuleMatcher extends AbstractRuleMatcher {

    protected RegularExpressionRuleMatcher(Logger logger) {
      super(logger);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesTerminalevel(DotNode dotNode, int lastWordIndex) {
      return matchAllRegularExpression(dotNode, lastWordIndex);
    }

  }

  protected static class RegularExpressionRuleMatcherStrict extends RegularExpressionRuleMatcher {

    protected RegularExpressionRuleMatcherStrict(Logger logger) {
      super(logger);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return matchAllRegularExpression(dotNode, superNode.lhs);
    }

  }

  protected static class StandardRuleMatcherStrict extends StandardRuleMatcher {

    protected StandardRuleMatcherStrict(Logger logger) {
      super(logger);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
      List<Trie> child_tnodes = Arrays.asList(child_node);
      return child_tnodes;
    }
  }

  protected static class RegularExpressionRuleMatcherSoftConstraints extends
      RegularExpressionRuleMatcher {

    protected RegularExpressionRuleMatcherSoftConstraints(Logger logger) {
      super(logger);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return matchAllEqualOrBothNonTerminal(dotNode, superNode.lhs);
    }

  }

  protected static class StandardRuleMatcherSoftConstraints extends StandardRuleMatcher {

    protected StandardRuleMatcherSoftConstraints(Logger logger) {
      super(logger);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {

      if (Vocabulary.word(superNode.lhs).equals("[GOAL]")) {
        //logger.info("BLAA - Vocabulary.word(superNode.lhs)" + Vocabulary.word(superNode.lhs));
        Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
        //logger.info("child_node.toString()" + child_node);
        List<Trie> child_tnodes = Arrays.asList(child_node);
        return child_tnodes;
      } else {

        //logger.info("Vocabulary.word(superNode.lhs): " + Vocabulary.word(superNode.lhs));
        return matchAllEqualOrBothNonTerminal(dotNode, superNode.lhs);
      }
      // Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
      // List<Trie> child_tnodes = Arrays.asList(child_node);
      // return child_tnodes;
    }
  }

}
