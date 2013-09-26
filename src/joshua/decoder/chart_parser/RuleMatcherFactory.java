package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.testng.Assert;

import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

/**
 * This is the Factory class for the RuleMatcher Interface, producing different flavors of 
 * RuleMatcher corresponding to strict (basic) matching, Regular Expression matching and 
 * soft syntactic matching. Notice that regular expression matching and soft constraint matching 
 * can in fact be combined, getting the 'loosest' way of matching possible. 
 * 
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 *
 */
public class RuleMatcherFactory {

  public static RuleMatcher createRuleMatcher(boolean useRegularExpressionGrammar, Logger logger,JoshuaConfiguration joshuaConfiguration) {
    if (useRegularExpressionGrammar) {
      if (joshuaConfiguration.softSyntacticConstraintDecoding) {
        return new RegularExpressionRuleMatcherSoftConstraints(logger,joshuaConfiguration);
      } else {
        return new RegularExpressionRuleMatcherStrict(logger,joshuaConfiguration);
      }
    } else {
      if (joshuaConfiguration.softSyntacticConstraintDecoding) {
        return new StandardRuleMatcherSoftConstraints(logger,joshuaConfiguration);
      } else {
        return new StandardRuleMatcherStrict(logger,joshuaConfiguration);
      }

    }
  }

  protected abstract static class AbstractRuleMatcher implements RuleMatcher {

    protected final Logger logger;
    protected final JoshuaConfiguration joshuaConfiguration;

    protected AbstractRuleMatcher(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      this.logger = logger;
      this.joshuaConfiguration = joshuaConfiguration;
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
      // + Vocabulary.word(Math.abs(wordID)));
      ;
      if (childrenTbl != null) {
        // get all the extensions, map to string, check for *, build regexp
        for (Integer arcID : childrenTbl.keySet()) {
          String arcWord = Vocabulary.word(arcID);
          // logger.info("Vocabulary.word(wordID), arcWord ||| " + Vocabulary.word(wordID) + " "+
          // arcWord);
          String wordIdWord = Vocabulary.word(wordID);
          if (wordIdWord.equals(arcWord)) {
            trieList.add(childrenTbl.get(arcID));
          } else if (isNonterminal(wordID) && isNonterminal(arcID) && !wordIdWord.equals("[OOV]")) {
            logger.info("Substituing : " + arcWord + " for " + wordIdWord);
            trieList.add(childrenTbl.get(arcID));
          }

          // logger.info("added node for arcWord: " + arcWord);
        }
      }
      // logger.info("trieList.size(): " + trieList.size());
      return trieList;
    }
  }

  protected static List<Trie> produceStandardMatchingChildTNodesTerminalevel(DotNode dotNode,
      int lastWordIndex) {
    Trie child_node = dotNode.getTrieNode().match(lastWordIndex);
    List<Trie> child_tnodes = Arrays.asList(child_node);
    return child_tnodes;
  }

  public static List<Trie> produceStandardMatchingChildTNodesNonterminalLevel(DotNode dotNode,
      SuperNode superNode) {
    Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
    List<Trie> child_tnodes = Arrays.asList(child_node);
    return child_tnodes;
  }

  protected abstract static class StandardRuleMatcher extends AbstractRuleMatcher {

    protected StandardRuleMatcher(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      super(logger,joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesTerminalevel(DotNode dotNode, int lastWordIndex) {
      return produceStandardMatchingChildTNodesTerminalevel(dotNode, lastWordIndex);
    }
  }

  protected abstract static class RegularExpressionRuleMatcher extends AbstractRuleMatcher {

    protected RegularExpressionRuleMatcher(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      super(logger,joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesTerminalevel(DotNode dotNode, int lastWordIndex) {
      if (lastWordIndex >= 0) {
        return matchAllRegularExpression(dotNode, lastWordIndex);
      } else {
        return produceStandardMatchingChildTNodesTerminalevel(dotNode, lastWordIndex);
      }
    }

  }

  protected static class RegularExpressionRuleMatcherStrict extends RegularExpressionRuleMatcher {

    protected RegularExpressionRuleMatcherStrict(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      super(logger,joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      /*
      if (superNode.lhs >= 0) {
        Assert.fail();
        return matchAllRegularExpression(dotNode, superNode.lhs);
      } else {*/
        // TODO : Presumably regular expression matching should only be allowed at the terminal level
        // Check with Matt Post if that is really the case
        logger.info("produceMatchingChildTnodesNonTerminalLevel" + Vocabulary.word(superNode.lhs));
        return produceStandardMatchingChildTNodesTerminalevel(dotNode, superNode.lhs);
      }
   
  }

  protected static class StandardRuleMatcherStrict extends StandardRuleMatcher {

    protected StandardRuleMatcherStrict(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      super(logger,joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return produceStandardMatchingChildTNodesNonterminalLevel(dotNode, superNode);
    }
  }

  protected static class RegularExpressionRuleMatcherSoftConstraints extends
      RegularExpressionRuleMatcher {

    protected RegularExpressionRuleMatcherSoftConstraints(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      super(logger,joshuaConfiguration);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return matchAllEqualOrBothNonTerminal(dotNode, superNode.lhs);
    }

  }

  protected static class StandardRuleMatcherSoftConstraints extends StandardRuleMatcher {

 

    /**
     * 
     * @param logger
     * @param joshuaConfiguration
     */
    protected StandardRuleMatcherSoftConstraints(Logger logger,JoshuaConfiguration joshuaConfiguration) {
      super(logger,joshuaConfiguration);
    }


    /**
     * This method will perform strict matching if the target node superNode is a 
     * Goal Symbol. Otherwise it will call a method that produces all available 
     * substitutions that correspond to Nonterminals. 
     * 
     * @param dotNode
     * @param superNode
     */
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {

      // We do not allow substitution of other things for GOAL labels
      if (Vocabulary.word(superNode.lhs).equals(joshuaConfiguration.goal_symbol)) {
        // logger.info("BLAA - Vocabulary.word(superNode.lhs)" + Vocabulary.word(superNode.lhs));
        Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
        // logger.info("child_node.toString()" + child_node);
        List<Trie> child_tnodes = Arrays.asList(child_node);
        return child_tnodes;
      } else {
        // logger.info("Vocabulary.word(superNode.lhs): " + Vocabulary.word(superNode.lhs));
        return matchAllEqualOrBothNonTerminal(dotNode, superNode.lhs);
      }
    }
  }

}
