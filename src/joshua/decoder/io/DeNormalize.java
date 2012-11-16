package joshua.decoder.io;

/**
 * Denormalize a(n English) string in a collection of ways listed below.
 * <UL>
 * <LI>Capitalize the first character in the string</LI>
 * <LI>Detokenize</LI>
 * <UL>
 * <LI>Combine conjunctions</LI>
 * <LI>Delete whitespace in front of periods and commas</LI>
 * <LI>Join contractions</LI>
 * <LI>TODO: Handle surrounding characters ([{<"''">}])</LI>
 * <LI>TODO: Capitalize titles (Mr* Ms Miss Dr)</LI>
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

  private final static String[] CONTRACTION_SUFFIXES =
      new String[] {
          "'d",
          "'ll",
          "'m",
          "n't",
          "'re",
          "'s",
          "'ve",
  };

  public static String processSingleLine(String normalized) {
    String deNormalized = normalized;
    deNormalized = capitalizeFirstLetter(deNormalized);
    deNormalized = joinPeriodsCommas(deNormalized);
    deNormalized = joinHyphen(deNormalized);
    deNormalized = joinContractions(deNormalized);
    return deNormalized;
  }

  public static String capitalizeFirstLetter(String line) {
    String result = null;
    if (line.length() > 0) {
      result = Character.toUpperCase(line.charAt(0)) + line.substring(1);
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
  public static String joinPeriodsCommas(String line) {
    String result = line;
    result = result.replace(" ,", ",");
    result = result.replace(" .", ".");
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
   * Scanning from left-to-right, a contraction suffix preceded by a space will become just the
   * contraction.
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

}
