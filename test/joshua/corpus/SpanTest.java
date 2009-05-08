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
package joshua.corpus;

import joshua.corpus.Span;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 *
 * 
 * @author Lane Schwartz
 */
public class SpanTest {

	@Test
	public void iterator() {
		
		Span span = new Span(1,10);
		
		int expected = 1;
		
		for (int actual : span) {
			Assert.assertEquals(actual, expected);
			expected++;
		}
		
	}
	
}
