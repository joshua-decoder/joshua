package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import joshua.corpus.Vocabulary;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.tm.Trie;

/**
 * This is the Factory class for the RuleMatcher Interface, producing different flavors of
 * RuleMatcher corresponding to strict (basic) matching, Regular Expression matching and soft
 * syntactic matching. Notice that regular expression matching and soft constraint matching can in
 * fact be combined, getting the 'loosest' way of matching possible.
 * 
 * @author Gideon Maillette de Buy Wenniger <gemdbw AT gmail DOT com>
 * 
 */
public class NonterminalMatcherFactory {

  /**
   * How much nonterminals there may be maximal to use targeted querying rather than matching to
   * find the alternate nonterminals from the children table when doing soft syntactic translation.
   * Note that there is a tradeoff here: looping over all children and matching is rather is
   * expensive if there are many children, which is typically the case for big grammars as there are
   * many possible words at each level in the Trie. Targeted querying is only cheap provided the
   * number of nonterminals is small, otherwise it may in fact become more expensive than just
   * looping and matching.
   */
  private static final int MAX_TOTAL_NON_TERMINALS_FOR_TARGETED_QUERYING = 1000;

  private static String OOV_LABEL = "[OOV]";

  protected static boolean isOOVLabelOrGoalLabel(String label,
      JoshuaConfiguration joshuaConfiguration) {
    return (label.equals(OOV_LABEL) || label.equals(joshuaConfiguration.goal_symbol));
  }

  private static boolean useTargetdQuerying(List<Integer> nonterminalIndicesExceptForGoalAndOOV) {
    if (nonterminalIndicesExceptForGoalAndOOV.size() <= MAX_TOTAL_NON_TERMINALS_FOR_TARGETED_QUERYING) {
      return true;
    }
    return false;
  }

  /**
   * This method returns a list of all indices corresponding to Nonterminals in the Vocabulary
   * 
   * @return
   */
  public static List<Integer> getAllNonterminalIndicesExceptForGoalAndOOV(
      JoshuaConfiguration joshuaConfiguration) {
    List<Integer> result = new ArrayList<Integer>();
    List<Integer> nonterminalIndices = Vocabulary.getNonterminalIndices();
    for (Integer nonterminalIndex : nonterminalIndices) {
      if (!isOOVLabelOrGoalLabel(Vocabulary.word(nonterminalIndex), joshuaConfiguration)) {
        result.add(nonterminalIndex);
      }
    }
    return result;
  }

  public static NonterminalMatcher createNonterminalMatcher(Logger logger,
      JoshuaConfiguration joshuaConfiguration) {
    List<Integer> allNonterminalIndicesExceptForGoalAndOOV = getAllNonterminalIndicesExceptForGoalAndOOV(joshuaConfiguration);

    if (allNonterminalIndicesExceptForGoalAndOOV.isEmpty()) {
      throw new RuntimeException(
          "Error: NonterminalMatcherFactory. createNonterminalMatcher -  empty nonterminal indices table");
    }

    if (joshuaConfiguration.softSyntacticConstraintDecoding) {
      return new StandardNonterminalMatcherSoftConstraints(logger, joshuaConfiguration,
          allNonterminalIndicesExceptForGoalAndOOV,
          useTargetdQuerying(allNonterminalIndicesExceptForGoalAndOOV));
    } else {
      return new StandardNonterminalMatcherStrict(logger, joshuaConfiguration,
          allNonterminalIndicesExceptForGoalAndOOV,
          useTargetdQuerying(allNonterminalIndicesExceptForGoalAndOOV));
    }
  }

  protected abstract static class AbstractNonterminalMatcher implements NonterminalMatcher {
    // A list of nonTerminalIndices, to be used for faster retrieval of Nonterminals in
    // soft syntactic matching
    private final List<Integer> nonterminalIndicesExceptForGoalAndOOV;

    protected final Logger logger;
    protected final JoshuaConfiguration joshuaConfiguration;
    private final boolean useTargetQueryingToCollectAlternateNonterminals;

    protected AbstractNonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration,
        List<Integer> nonterminalIndicesExceptForGoalAndOOV,
        boolean useTargetQueryingToCollectAlternateNonterminals) {
      this.logger = logger;
      this.joshuaConfiguration = joshuaConfiguration;
      this.nonterminalIndicesExceptForGoalAndOOV = nonterminalIndicesExceptForGoalAndOOV;
      this.useTargetQueryingToCollectAlternateNonterminals = useTargetQueryingToCollectAlternateNonterminals;
    }

    private static boolean isNonterminal(int wordIndex) {
      return wordIndex < 0;
    }

    /**
     * This method finds Nonterminal entries from the children in the Children HashMap using a
     * targeted querying strategy, based on knowledge of what the Nonterminals are. Storing the
     * Nonterminals and Terminals in the Trie separately would be an even smarter strategy perhaps,
     * but requires a more thorough refactoring of the code
     * 
     * @param childrenTbl
     * @return
     */
    private List<Trie> getNonTerminalsListFromChildrenByTargetedQuerying(
        HashMap<Integer, ? extends Trie> childrenTbl) {
      List<Trie> trieList = new ArrayList<Trie>();

      if (childrenTbl != null) {
        // get all the extensions, map to string, check for *, build regexp

        for (Integer index : this.nonterminalIndicesExceptForGoalAndOOV) {

          int nonterminalIndexTrieFormat = -index;
          if (childrenTbl.containsKey(nonterminalIndexTrieFormat)) {
            trieList.add(childrenTbl.get(nonterminalIndexTrieFormat));
          }
        }
      }
      return trieList;

    }

    private List<Trie> getNonTerminalsListFromChildrenByMatching(
        HashMap<Integer, ? extends Trie> childrenTbl, int wordID) {
      List<Trie> trieList = new ArrayList<Trie>();

      if (childrenTbl != null) {
        // get all the extensions, map to string, check for *, build regexp

        // This has now been optimized.
        // It is not a good idea to first get the keyset, and then do get for each
        // entry See:
        // http://stackoverflow.com/questions/5826384/java-iteration-through-a-hashmap-which-is-more-efficient
        // Although if the set of nonterminals is small, it might not matter too much
        for (Entry<Integer, ? extends Trie> entry : childrenTbl.entrySet()) {
          // logger.info("Vocabulary.word(wordID), arcWord ||| " + Vocabulary.word(wordID) + " "+
          // arcWord);
          Integer arcID = entry.getKey();

          String wordIdWord = Vocabulary.word(wordID);

          if (isNonterminal(arcID) && !isOOVLabelOrGoalLabel(wordIdWord, joshuaConfiguration)) {
            Trie value = entry.getValue();

            // logger.info("Substituing : " + arcWord + " for " + wordIdWord);
            trieList.add(value);
          }
          // logger.info("added node for arcWord: " + arcWord);
        }
      }
      // logger.info("trieList.size(): " + trieList.size());
      return trieList;

    }

    protected List<Trie> matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID) {
      HashMap<Integer, ? extends Trie> childrenTbl = dotNode.getTrieNode().getChildren();

      // logger.info("wordID: " + wordID + " Vocabulary.word(Math.abs(wordID)) "
      // + Vocabulary.word(Math.abs(wordID)));

      if (!isNonterminal(wordID)) {
        throw new RuntimeException("Error : expexted nonterminal, but did not get it "
            + "in matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(DotNode dotNode, int wordID)");
      }

      if (useTargetQueryingToCollectAlternateNonterminals) {
        return getNonTerminalsListFromChildrenByTargetedQuerying(childrenTbl);
      } else {
        return getNonTerminalsListFromChildrenByMatching(childrenTbl, wordID);
      }

    }
  }

  public static List<Trie> produceStandardMatchingChildTNodesNonterminalLevel(DotNode dotNode,
      SuperNode superNode) {
    Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
    List<Trie> child_tnodes = Arrays.asList(child_node);
    return child_tnodes;
  }

  protected abstract static class StandardNonterminalMatcher extends AbstractNonterminalMatcher {

    protected StandardNonterminalMatcher(Logger logger, JoshuaConfiguration joshuaConfiguration,
        List<Integer> nonterminalIndicesExceptForGoalAndOOV,
        boolean useTargetQueryingToCollectAlternateNonterminals) {
      super(logger, joshuaConfiguration, nonterminalIndicesExceptForGoalAndOOV,
          useTargetQueryingToCollectAlternateNonterminals);
    }
  }

  protected static class StandardNonterminalMatcherStrict extends StandardNonterminalMatcher {

    protected StandardNonterminalMatcherStrict(Logger logger,
        JoshuaConfiguration joshuaConfiguration,
        List<Integer> nonterminalIndicesExceptForGoalAndOOV,
        boolean useTargetQueryingToCollectAlternateNonterminals) {
      super(logger, joshuaConfiguration, nonterminalIndicesExceptForGoalAndOOV,
          useTargetQueryingToCollectAlternateNonterminals);
    }

    @Override
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {
      return produceStandardMatchingChildTNodesNonterminalLevel(dotNode, superNode);
    }
  }

  protected static class StandardNonterminalMatcherSoftConstraints extends
      StandardNonterminalMatcher {

    /**
     * 
     * @param logger
     * @param joshuaConfiguration
     */
    protected StandardNonterminalMatcherSoftConstraints(Logger logger,
        JoshuaConfiguration joshuaConfiguration,
        List<Integer> nonterminalIndicesExceptForGoalAndOOV,
        boolean useTargetQueryingToCollectAlternateNonterminals) {
      super(logger, joshuaConfiguration, nonterminalIndicesExceptForGoalAndOOV,
          useTargetQueryingToCollectAlternateNonterminals);
    }

    /**
     * This method will perform strict matching if the target node superNode is a Goal Symbol.
     * Otherwise it will call a method that produces all available substitutions that correspond to
     * Nonterminals.
     * 
     * @param dotNode
     * @param superNode
     */
    public List<Trie> produceMatchingChildTNodesNonterminalLevel(DotNode dotNode,
        SuperNode superNode) {

      // We do not allow substitution of other things for GOAL labels or OOV symbols
      if (isOOVLabelOrGoalLabel(Vocabulary.word(superNode.lhs), joshuaConfiguration)) {
        // logger.info("BLAA - Vocabulary.word(superNode.lhs)" + Vocabulary.word(superNode.lhs));
        Trie child_node = dotNode.getTrieNode().match(superNode.lhs);
        // logger.info("child_node.toString()" + child_node);
        List<Trie> child_tnodes = Arrays.asList(child_node);
        return child_tnodes;
      } else {
        // logger.info("Vocabulary.word(superNode.lhs): " + Vocabulary.word(superNode.lhs));
        return matchAllEqualOrBothNonTerminalAndNotGoalOrOOV(dotNode, superNode.lhs);
      }
    }
  }
}
