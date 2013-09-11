package joshua.decoder.ff.tm;

import java.util.ArrayList;
import java.util.List;

import joshua.corpus.Vocabulary;

/**
 * This class contains static Methods for getting properties (words, nonterminals etc) from rules.
 * This class was motivated by removing code duplication in various classes (BilingualRule and
 * PackedRule (in PackedGrammar) that all need to implement the interface methods in Rule. They do
 * this in the same way based on lower level methods in the interface so this code duplication can
 * be prevented in this way by using this shared static class and re-using it to implement the
 * methods in both classes
 * 
 * @author Gideon Wennniger
 * 
 */
public class RuleMethods {

  public static List<String> getForeignNonTerminals(Rule rule) {
    List<String> foreignNTs = new ArrayList<String>();
    for (int i = 0; i < rule.getFrench().length; i++) {
      if (rule.getFrench()[i] < 0)
        foreignNTs.add(Vocabulary.word(rule.getFrench()[i]));
    }
    return foreignNTs;
  }

  private static int foreignNonTerminalIndexForEnglishIndex(int index) {
    return Math.abs(index) - 1;
  }

  public static String getEnglishWords(Rule rule) {
    List<String> foreignNTs = getForeignNonTerminals(rule);

    StringBuilder sb = new StringBuilder();
    for (Integer index : rule.getEnglish()) {
      if (index >= 0)
        sb.append(Vocabulary.word(index) + " ");
      else
        sb.append(foreignNTs.get(foreignNonTerminalIndexForEnglishIndex(index)) + ","
            + Math.abs(index) + " ");
    }

    return sb.toString().trim();
  }

  public static List<String> getEnglishNonTerminals(Rule rule) {
    List<String> result = new ArrayList<String>();
    List<String> foreignNTs = getForeignNonTerminals(rule);

    // for (int i = 0; i < this.getEnglish().length; i++) {
    for (Integer index : rule.getEnglish()) {
      if (index < 0) {
        result.add(foreignNTs.get(foreignNonTerminalIndexForEnglishIndex(index)) + ","
            + Math.abs(index));
      }
    }

    return result;
  }

  private static List<Integer> getNormalizedEnglishNonterminalIndices(Rule rule) {
    List<Integer> result = new ArrayList<Integer>();

    for (Integer index : rule.getEnglish()) {
      if (index < 0) {
        result.add(foreignNonTerminalIndexForEnglishIndex(index));
      }
    }

    return result;
  }

  public static boolean ruleIsInverting(Rule rule) {
    List<Integer> normalizedEnglishNonTerminalIndices = getNormalizedEnglishNonterminalIndices(rule);
    if (normalizedEnglishNonTerminalIndices.size() == 2) {
      if (normalizedEnglishNonTerminalIndices.get(0) == 1) {
        return true;
      }
    }
    return false;
  }
}
