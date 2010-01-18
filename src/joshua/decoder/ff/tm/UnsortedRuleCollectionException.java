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
package joshua.decoder.ff.tm;

/**
 * Unchecked runtime exception thrown to indicate 
 * that a collection of rules has not been properly
 * sorted according to the feature functions in effect.
 *
 * @author Lane Schwartz
 */
public class UnsortedRuleCollectionException extends RuntimeException {

	/**
	 * Constructs an <code>UnsortedRuleCollectionException</code>
	 * with the specified detail message.
	 * 
	 * @param message the detail message
	 */
	public UnsortedRuleCollectionException(String message) {
		super(message);
	}
	
}
