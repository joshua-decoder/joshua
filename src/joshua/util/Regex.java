package joshua.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class provides a repository for common regex patterns so that we don't keep recompiling them
 * over and over again. Some convenience methods are provided to make the interface more similar to
 * the convenience functions on String. The convenience methods on String are deprecated except for
 * one-shot patterns (which, by definition, are not in loops).
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-28 07:40:25 -0400 (Sat, 28 Mar 2009) $
 */
public class Regex {
  // Alas, Pattern is final, thus no subclassing and this indirection
  private final Pattern pattern;

  // ===============================================================
  // Singletons -- add all common patterns here
  // ===============================================================
  /**
   * A pattern to match if the complete string is empty except for whitespace and end-of-line
   * comments beginning with an octothorpe (<code>#</code>).
   */
  public static final Regex commentOrEmptyLine = new Regex("^\\s*(?:\\#.*)?$");

  // BUG: this should be replaced by a real regex for numbers.
  // Perhaps "^[\\+\\-]?\\d+(?:\\.\\d+)?$" is enough.
  // This is only used by JoshuaDecoder.writeConfigFile so far.
  /**
   * A pattern to match floating point numbers. (Current implementation is overly permissive.)
   */
  public static final Regex floatingNumber = new Regex("^[\\d\\.\\-\\+]+");

  // Common patterns for splitting
  /**
   * A pattern for splitting on one or more whitespace.
   */
  public static final Regex spaces = new Regex("\\s+");

  /**
   * A pattern for splitting on one or more whitespace.
   */
  public static final Regex tabs = new Regex("\\t+");

  /**
   * A pattern for splitting on the equals character, with optional whitespace on each side.
   */
  public static final Regex equalsWithSpaces = new Regex("\\s*=\\s*");

  /**
   * A pattern for splitting on three vertical pipes, with one or more whitespace on each side.
   */
  public static final Regex threeBarsWithSpace = new Regex("\\s\\|{3}\\s");


  // ===============================================================
  // Constructor
  // ===============================================================

  public Regex(String regex) throws PatternSyntaxException {
    this.pattern = Pattern.compile(regex);
  }


  // ===============================================================
  // Convenience Methods
  // ===============================================================

  /**
   * Returns whether the input string matches this <code>Regex</code>.
   */
  public final boolean matches(String input) {
    return this.pattern.matcher(input).matches();
  }


  /**
   * Split a character sequence, removing instances of this <code>Regex</code>.
   */
  public final String[] split(CharSequence input) {
    return this.pattern.split(input);
  }


  /**
   * Split a character sequence, removing instances of this <code>Regex</code>, up to a limited
   * number of segments.
   */
  public final String[] split(CharSequence input, int limit) {
    return this.pattern.split(input, limit);
  }


  /**
   * Replace all substrings of the input which match this <code>Regex</code> with the specified
   * replacement string.
   */
  public final String replaceAll(String input, String replacement) {
    return this.pattern.matcher(input).replaceAll(replacement);
  }


  /**
   * Replace the first substring of the input which matches this <code>Regex</code> with the
   * specified replacement string.
   */
  public final String replaceFirst(String input, String replacement) {
    return this.pattern.matcher(input).replaceFirst(replacement);
  }
}
