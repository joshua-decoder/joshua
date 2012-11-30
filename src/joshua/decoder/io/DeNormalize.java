package joshua.decoder.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Denormalize a(n English) string in a collection of ways listed below.
 * <UL>
 * <LI>Capitalize the first character in the string</LI>
 * <LI>Detokenize</LI>
 * <UL>
 * <LI>Delete whitespace in front of periods and commas</LI>
 * <LI>Join contractions</LI>
 * <LI>Capitalize name titles (Mr Ms Miss Dr etc.)</LI>
 * <LI>TODO: Handle surrounding characters ([{<"''">}])</LI>
 * <LI>TODO: Join multi-period abbreviations (e.g. M.Phil. i.e.)</LI>
 * <LI>TODO: Handle ambiguities like "st.", which can be an abbreviation for both "Saint" and
 * "street"</LI>
 * <LI>TODO: Capitalize both the title and the name of a person, e.g. Mr. Morton (named entities
 * should be demarcated).</LI>
 * </UL>
 * </UL> <bold>N.B.</bold> These methods all assume that every translation result that will be
 * denormalized has the following format:
 * <UL>
 * <LI>There is only one space between every pair of tokens</LI>
 * <LI>There is no whitespace before the first token</LI>
 * <LI>There is no whitespace after the final token</LI>
 * <LI>Standard spaces are the only type of whitespace</LI>
 * </UL>
 * </UL>
 */

public class DeNormalize {

  private static final String[] CONTRACTION_SUFFIXES =
      new String[] {
          "'d",
          "'ll",
          "'m",
          "n't",
          "'re",
          "'s",
          "'ve",
  };

  /** Abbreviations of titles for names that capitalize only the first letter */
  private static final String[] NAME_TITLES_CAP_FIRST_LETTER =
      new String[] {
          "dr",
          "miss",
          "mr",
          "mrs",
          "ms",
          "prof",
          //"st", There is too much ambiguity between the abbreviations for Saint and street.
  };

  /**
   * Keys are token representations of abbreviations of titles for names that capitalize more than
   * just the first letter.<br>
   * Values are the capitalized version.
   */
  @SuppressWarnings("serial")
  private static final Map<String, String> NAME_TITLES_COMPLEX_CAPITALIZATION = Collections
      .unmodifiableMap(new HashMap<String, String>() {
        {
          put("phd", "PhD");
          put("mphil", "MPhil");
        }
      });

  /**
   * Apply all the denormalization methods to the normalized input line.
   * 
   * @param normalized
   * @return
   */
  public static String processSingleLine(String normalized) {
    // The order in which the methods are applied could matter in some situations. E.g., a token to
    // be matched is "phd", but if it is the first token in the line, it might have already been
    // capitalized to "Phd" by the capitalizeFirstLetter method, and because the "phd" token won't
    // match, "Phd" won't be corrected to "PhD".
    String deNormalized = normalized;
    deNormalized = capitalizeNameTitleAbbrvs(deNormalized);
    deNormalized = replaceBracketTokens(deNormalized);
    deNormalized = joinPunctuationMarks(deNormalized);
    deNormalized = joinHyphen(deNormalized);
    deNormalized = joinContractions(deNormalized);
    deNormalized = capitalizeLineFirstLetter(deNormalized);
    return deNormalized;
  }

  public static String capitalizeLineFirstLetter(String line) {
    String result = null;
    Pattern regexp = Pattern.compile("[^\\p{Punct}\\p{Space}¡¿]");
    Matcher matcher = regexp.matcher(line);
    if (matcher.find()) {
      String match = matcher.group(0);
      result = line.replaceFirst(match, match.toUpperCase());
    } else {
      result = line;
    }
    return result;
  }

  /**
   * Scanning from left-to-right, a comma or period preceded by a space will become just the
   * comma/period.
   * 
   * @param line The single-line input string
   * @return The input string modified as described above
   */
  public static String joinPunctuationMarks(String line) {
    String result = line;
    result = result.replace(" ,", ",");
    result = result.replace(" .", ".");
    result = result.replace(" !", "!");
    result = result.replace("¡ ", "¡");
    result = result.replace(" ?", "?");
    result = result.replace("¿ ", "¿");
    result = result.replace(" )", ")");
    result = result.replace(" ]", "]");
    result = result.replace(" }", "}");
    result = result.replace("( ", "(");
    result = result.replace("[ ", "[");
    result = result.replace("{ ", "{");
    return result;
  }

  /**
   * Scanning from left-to-right, a hyphen surrounded by a space before and after it will become
   * just the hyphen.
   * 
   * @param line The single-line input string
   * @return The input string modified as described above
   */
  public static String joinHyphen(String line) {
    return line.replace(" - ", "-");
  }

  /**
   * Scanning the line from left-to-right, a contraction suffix preceded by a space will become just
   * the contraction suffix. <br>
   * <br>
   * I.e., the preceding space will be deleting, joining the prefix to the suffix. <br>
   * <br>
   * E.g.
   * <pre>wo n't</pre>
   * becomes
   * <pre>won't</pre>
   *
   * @param line The single-line input string
   * @return The input string modified as described above
   */
  public static String joinContractions(String line) {
    String result = line;
    for (String suffix : CONTRACTION_SUFFIXES) {
      result = result.replace(" " + suffix, suffix);
    }
    return result;
  }

  /**
   * Capitalize the first character of the titles of names: Mr Mrs Ms Miss Dr Prof
   * 
   * @param line The single-line input string
   * @return The input string modified as described above
   */
  public static String capitalizeNameTitleAbbrvs(String line) {
    String result = line;

    // Capitalize only the first character of certain name titles.
    for (String title : NAME_TITLES_CAP_FIRST_LETTER) {
      result = result.replaceAll("\\b" + title + "\\b",
          Character.toUpperCase(title.charAt(0)) + title.substring(1));
    }

    // Capitalize the relevant characters of certain name titles.
    for (String title : NAME_TITLES_COMPLEX_CAPITALIZATION.keySet()) {
      result = result.replaceAll("\\b" + title + "\\b",
          NAME_TITLES_COMPLEX_CAPITALIZATION.get(title));
    }
    return result;
  }

  public static String capitalizeI(String line) {
    // Capitalize only the first character of certain name titles.
    return line.replaceAll("\\b" + "i" + "\\b", "I");
  }

  /**
   * Case-insensitively replace all of the character sequences that represent a bracket character.
   * 
   * Keys are token representations of abbreviations of titles for names that capitalize more than
   * just the first letter.<br>
   * Bracket token sequences: -lrb- -rrb- -lsb- -rsb- -lcb- -rcb- <br>
   * <br>
   * See http://www.cis.upenn.edu/~treebank/tokenization.html
   * 
   * @param line The single-line input string
   * @return The input string modified as described above
   */
  public static String replaceBracketTokens(String line) {
    String result = line;
    result = result.replaceAll("(?iu)" + "-lrb-", "(");
    result = result.replaceAll("(?iu)" + "-rrb-", ")");
    result = result.replaceAll("(?iu)" + "-lsb-", "[");
    result = result.replaceAll("(?iu)" + "-rsb-", "]");
    result = result.replaceAll("(?iu)" + "-lcb-", "{");
    result = result.replaceAll("(?iu)" + "-rcb-", "}");
    return result;
  }

}
