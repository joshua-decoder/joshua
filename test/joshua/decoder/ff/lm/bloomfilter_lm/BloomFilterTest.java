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
package joshua.decoder.ff.lm.bloomfilter_lm;

import java.math.BigInteger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BloomFilterTest {
	
	private final int size = 1024;
	private final int numObjects = 3;
	private BloomFilter bf;
	private long [][] hashes;

	@Test
	public void constructor()
	{
		bf = new BloomFilter(size, numObjects);
		Assert.assertEquals(bf.filterSize, size);
		Assert.assertEquals(bf.expectedNumberOfObjects, numObjects);
	}

	@Test(dependsOnMethods = { "constructor" })
	public void bigPrime()
	{
		BigInteger prime = new BigInteger(Long.valueOf(bf.bigPrime).toString());
		Assert.assertTrue(prime.isProbablePrime(100));
		Assert.assertTrue(bf.bigPrime > bf.filterSize);
	}

	@Test(dependsOnMethods = { "constructor" })
	public void hashFunctions()
	{
		hashes = bf.initializeHashFunctions();
		for (long [] h : hashes) {
			Assert.assertTrue(h[0] < bf.bigPrime);
			Assert.assertTrue(h[1] < bf.bigPrime);
		}
	}

	@Test(dependsOnMethods = { "constructor", "hashFunctions" })
	public void addAndQuery()
	{
		long a = 1;
		long b = 2;
		long c = 3;

		bf.add(a, hashes);
		bf.add(b, hashes);
		bf.add(c, hashes);

		Assert.assertTrue(bf.query(a, hashes));
		Assert.assertTrue(bf.query(b, hashes));
		Assert.assertTrue(bf.query(c, hashes));
	}
}
