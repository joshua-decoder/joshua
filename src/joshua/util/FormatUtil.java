/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.util;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

/**
 * Utility class for format issues.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class FormatUtil {

	/**
	 * Returns true if the String parameter represents a valid number.
	 * <p>
	 * The body of this method is taken from the Javadoc documentation
	 * for the Java Double class.
	 * 
	 * @param string
	 * @see http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Double.html
	 * @return
	 */
	public static boolean isNumber(String string) {
		final String Digits     = "(\\p{Digit}+)";
		final String HexDigits  = "(\\p{XDigit}+)";
		// an exponent is 'e' or 'E' followed by an optionally 
		// signed decimal integer.
		final String Exp        = "[eE][+-]?"+Digits;
		final String fpRegex    =
			("[\\x00-\\x20]*"+  // Optional leading "whitespace"
					"[+-]?(" + // Optional sign character
					"NaN|" +           // "NaN" string
					"Infinity|" +      // "Infinity" string

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
					"((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

					// . Digits ExponentPart_opt FloatTypeSuffix_opt
					"(\\.("+Digits+")("+Exp+")?)|"+

					// Hexadecimal strings
					"((" +
					// 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
					"(0[xX]" + HexDigits + "(\\.)?)|" +

					// 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
					"(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

					")[pP][+-]?" + Digits + "))" +
					"[fFdD]?))" +
			"[\\x00-\\x20]*");// Optional trailing "whitespace"

		if (Pattern.matches(fpRegex, string))
			return true;
		else
			return false;
	}
	
	/**
	 * Set System.out and System.err to use the UTF8 character encoding.
	 * 
	 * @return <code>true</code> if both System.out and System.err 
	 *         were successfully set to use UTF8,
	 *         <code>false</code> otherwise.
	 */
	public static boolean useUTF8() {
		
		try {
			System.setOut(new PrintStream(System.out, true, "UTF8"));
			System.setErr(new PrintStream(System.err, true, "UTF8"));
			return true;
		} catch (UnsupportedEncodingException e1) {
			System.err.println("UTF8 is not a valid encoding; using system default encoding for System.out and System.err.");
			return false;
		} catch (SecurityException e2) {
			System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
			return false;
		}
	}
}

