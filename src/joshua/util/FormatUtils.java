package joshua.util;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

/**
 * Utility class for format issues.
 * 
 * @author Juri Ganitkevitch
 * @author Lane Schwartz
 */
public class FormatUtils {
  
  private static final String INDEX_SEPARATOR = ",";

  /**
   * Determines whether the string is a nonterminal by checking that the first character is [
   * and the last character is ].
   * 
   * @param token
   * @return true if it's a nonterminal symbol, false otherwise
   */
  public static boolean isNonterminal(String token) {
    return (token.length() >=3 && token.charAt(0) == '[') && (token.charAt(token.length() - 1) == ']');
  }

  /**
   * Nonterminals are stored in the vocabulary in square brackets. This removes them when you 
   * just want the raw nonterminal word.
   * Supports indexed and non-indexed nonTerminals:
   * [GOAL] -> GOAL
   * [X,1] -> [X]
   * 
   * @param nt the nonterminal, e.g., "[GOAL]"
   * @return the cleaned nonterminal, e.g., "GOAL"
   */
  public static String cleanNonTerminal(String nt) {
    if (isNonterminal(nt)) {
      if (isIndexedNonTerminal(nt)) {
        // strip ",.*]"
        return nt.substring(1, nt.indexOf(INDEX_SEPARATOR));
      }
      // strip "]"
      return nt.substring(1, nt.length() - 1);
    }
    return nt;
  }
  
  private static boolean isIndexedNonTerminal(String nt) {
    return nt.contains(INDEX_SEPARATOR);
  }

  /**
   * Removes the index from a nonTerminal: [X,1] -> [X].
   */
  public static String stripNonTerminalIndex(String nt) {
    return markup(cleanNonTerminal(nt));
  }

  public static int getNonterminalIndex(String nt) {
    return Integer.parseInt(nt.substring(nt.indexOf(INDEX_SEPARATOR) + 1, nt.length() - 1));
  }

  /**
   * Ensures that a string looks like what the system considers a nonterminal to be.
   * 
   * @param nt the nonterminal string
   * @return the nonterminal string surrounded in square brackets (if not already)
   */
  public static String markup(String nt) {
    if (isNonterminal(nt)) 
      return nt;
    else 
      return "[" + nt + "]";
  }

  public static String markup(String nt, int index) {
    return "[" + nt + INDEX_SEPARATOR + index + "]";
  }

  /**
   * Returns true if the String parameter represents a valid number.
   * <p>
   * The body of this method is taken from the Javadoc documentation for the Java Double class.
   * 
   * @param string
   * @see java.lang.Double
   * @return <code>true</code> if the string represents a valid number, <code>false</code> otherwise
   */
  public static boolean isNumber(String string) {
    final String Digits = "(\\p{Digit}+)";
    final String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    final String Exp = "[eE][+-]?" + Digits;
    final String fpRegex = ("[\\x00-\\x20]*" + // Optional leading "whitespace"
        "[+-]?(" + // Optional sign character
        "NaN|" + // "NaN" string
        "Infinity|" + // "Infinity" string

        // A decimal floating-point string representing a finite positive
        // number without a leading sign has at most five basic pieces:
        // Digits . Digits ExponentPart FloatTypeSuffix
        //
        // Since this method allows integer-only strings as input
        // in addition to strings of floating-point literals, the
        // two sub-patterns below are simplifications of the grammar
        // productions from the Java Language Specification, 2nd
        // edition, section 3.10.2.

        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
        "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

    // . Digits ExponentPart_opt FloatTypeSuffix_opt
        "(\\.(" + Digits + ")(" + Exp + ")?)|" +

        // Hexadecimal strings
        "((" +
        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
        "(0[xX]" + HexDigits + "(\\.)?)|" +

        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

        ")[pP][+-]?" + Digits + "))" + "[fFdD]?))" + "[\\x00-\\x20]*");// Optional
                                                                       // trailing
                                                                       // "whitespace"

    return Pattern.matches(fpRegex, string);
  }

  /**
   * Set System.out and System.err to use the UTF8 character encoding.
   * 
   * @return <code>true</code> if both System.out and System.err were successfully set to use UTF8,
   *         <code>false</code> otherwise.
   */
  public static boolean useUTF8() {

    try {
      System.setOut(new PrintStream(System.out, true, "UTF8"));
      System.setErr(new PrintStream(System.err, true, "UTF8"));
      return true;
    } catch (UnsupportedEncodingException e1) {
      System.err
          .println("UTF8 is not a valid encoding; using system default encoding for System.out and System.err.");
      return false;
    } catch (SecurityException e2) {
      System.err
          .println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
      return false;
    }
  }
}
