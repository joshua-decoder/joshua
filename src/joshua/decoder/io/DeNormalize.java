package joshua.decoder.io;

/**
 * Denormalize a(n English) string in a collection of ways:
 * <UL>
 * <LI>Capitalize the first character in the string</LI>
 * <LI>Detokenize</LI>
 * <UL>
 * <LI>Combine conjunctions</LI>
 * <LI>Delete whitespace in front of periods and commas</LI>
 * <LI>TODO: Join contractions</LI>
 * <LI>TODO: Handle surrounding characters ([{<"''">}])</LI>
 * <LI>TODO: Capitalize titles (Mr* Ms Miss Dr)</LI>
 * </UL>
 * </UL>
 */
public class DeNormalize {

  public static String processSingleLine(String normalized) {
    String deNormalized = normalized;
    deNormalized = capitalizeFirstLetter(deNormalized);
    deNormalized = joinPeriodsCommas(deNormalized);
    deNormalized = joinHyphen(deNormalized);
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

}
