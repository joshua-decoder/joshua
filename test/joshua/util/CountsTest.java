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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for Counts class.
 * 
 * @author Lane Schwartz
 */
public class CountsTest {

	@Test
	public void verifyCounts() {
		
		Counts<Integer,Integer> counts = new Counts<Integer,Integer>();
		
		int maxA = 100;
		int maxB = 100;
		
		// Increment counts
		for (int a=0; a<maxA; a++) {
			for (int b=0; b<maxB; b++) {
				
				for (int n=0, times=b%10; n<=times; n++) {
					counts.incrementCount(a,b);
					counts.incrementCount(null, b);
				}
				
			}
			
			for (int n=0, times=10-a%10; n<times; n++) {
				counts.incrementCount(a,null);
			}
		}
		
		// Verify co-occurrence counts
		for (int a=0; a<maxA; a++) {
			for (int b=0; b<maxB; b++) {
				int expected = b%10 + 1;
				Assert.assertEquals(counts.getCount(a, b), expected);
				Assert.assertEquals(counts.getCount(null, b), maxA*expected);
			}
			
			int expected = 10 - a%10;
			Assert.assertEquals(counts.getCount(a, null), expected);
		}
		
		// Verify totals for B counts
		for (int b=0; b<maxB; b++) {
			int expected = maxA * 2 * (b%10 + 1);
			Assert.assertEquals(counts.getCount(b), expected);
		}
		
		// Verify probabilities
		for (int a=0; a<maxA; a++) {
			for (int b=0; b<maxB; b++) {
				float expected = 1.0f / (maxA*2);
				Assert.assertEquals(counts.getProbability(a, b), expected);
				Assert.assertEquals(counts.getProbability(null, b), 0.5f);
			}
			
			int aCounter = 0;
			for (int b=0; b<maxB; b++) {
				for (int n=0, times=b%10; n<=times; n++) {
					aCounter++;
				}
			}
			for (int n=0, times=10-a%10; n<times; n++) {
				aCounter++;
			}
				
			float nullExpected = (float) (10-a%10) / (float) (aCounter);
			Assert.assertEquals(counts.getReverseProbability(null, a), nullExpected);
		
		}
			
	}
	
}
