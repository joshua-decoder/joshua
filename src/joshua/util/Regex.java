/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class provides a repository for common regex patterns so
 * that we don't keep recompiling them over and over again. Some
 * convenience methods are provided to make the interface more
 * similar to the convenience functions on String. The convenience
 * methods on String are deprecated except for one-shot patterns
 * (which, by definition, are not in loops).
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-28 07:40:25 -0400 (Sat, 28 Mar 2009) $
 */
public class Regex {
	// Alas, Pattern is final, thus no subclassing and this indirection
	private final Pattern pattern;
	
//===============================================================
// Singletons -- add all common patterns here
//===============================================================
	public static final Regex commentOrEmptyLine =
		new Regex("^\\s*(?:\\#.*)?$");
		
	// BUG: this should be replaced by a real regex for numbers.
	// Perhaps "^[\\+\\-]?\\d+(?:\\.\\d+)?$" is enough.
	// This is only used by JoshuaDecoder.writeConfigFile so far.
	public static final Regex floatingNumber = new Regex("^[\\d\\.\\-\\+]+");
	
	// Common patterns for splitting
	public static final Regex spaces             = new Regex("\\s+");
	public static final Regex equalsWithSpaces   = new Regex("\\s*=\\s*");
	public static final Regex threeBarsWithSpace = new Regex("\\s+\\|{3}\\s+");
	
	
//===============================================================
// Constructor
//===============================================================

	public Regex(String regex) throws PatternSyntaxException {
		this.pattern = Pattern.compile(regex);
	}
	
	
//===============================================================
// Convenience Methods
//===============================================================
	public final boolean matches(String input) {
		return this.pattern.matcher(input).matches();
	}
	
	public final String[] split(CharSequence input) {
		return this.pattern.split(input);
	}
	
	public final String[] split(CharSequence input, int limit) {
		return this.pattern.split(input, limit);
	}
	
	public final String replaceAll(String input, String replacement) {
		return this.pattern.matcher(input).replaceAll(replacement);
	}
	
	public final String replaceFirst(String input, String replacement) {
		return this.pattern.matcher(input).replaceFirst(replacement);
	}
}
