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
package joshua.decoder.ff.lm.bloomfilter_lm;

import java.util.Random;
import java.util.BitSet;
import java.math.BigInteger;

public class BloomFilter {
	private BitSet bitSet;
	private int expectedNumberOfObjects;
	//private BigInteger bigPrime;
	//private BigInteger filterSize;
	private long bigPrime;
	private int filterSize;
	private Random RANDOM = new Random();

	public BloomFilter(int filterSize, int expectedNumberOfObjects) {
		bitSet = new BitSet(filterSize);
		//this.filterSize = convertIntToBigInteger(filterSize);
		//this.filterSize = BigInteger.valueOf((long) filterSize);
		this.filterSize = filterSize;
		this.expectedNumberOfObjects = expectedNumberOfObjects;
		bigPrime = getPrimeLargerThan(filterSize);
	}

	/** Adds an item (represented by an integer) to the bloom filter.
	 */
	public void add(int objectToAdd, long [][] hashFunctions) {
		for (long [] h : hashFunctions) {
			int i = hash(h, (long) objectToAdd);
			bitSet.set(i);
		}
	}

	public void add(long objectToAdd, long [][] hashFunctions) {
		for (long [] h : hashFunctions) {
			int i = hash(h, objectToAdd);
			bitSet.set(i);
		}
	}

	/** Determines whether an item (represented by an integer) is present
	 * in the bloom filter. Returns its value.
	 */
	public boolean query(int objectToQuery, long [][] hashFunctions) {
		for (long [] h : hashFunctions) {
			int i = hash(h, (long) objectToQuery);
			if (!bitSet.get(i))
				return false;
		}
		return true;
	}

	public boolean query(long objectToQuery, long [][] hashFunctions){
		for (long [] h : hashFunctions) {
			int i = hash(h, objectToQuery);
			if (!bitSet.get(i))
				return false;
		}
		return true;
	}

	public long [][] initializeHashFunctions() {
		int numberOfHashFunctions;
		int bigPrimeInt = (int) bigPrime;
		numberOfHashFunctions = (int) Math.floor(Math.log(2) * bitSet.length() / expectedNumberOfObjects);
		if (numberOfHashFunctions == 0) numberOfHashFunctions = 1;
		long [][] hashFunctions = new long[numberOfHashFunctions][2];
		for (long [] h : hashFunctions) {
			//h[0] = convertIntToBigInteger(RANDOM.nextInt(bigPrimeInt) + 1);
			//h[1] = convertIntToBigInteger(RANDOM.nextInt(bigPrimeInt) + 1);
			h[0] = (long) RANDOM.nextInt(bigPrimeInt) + 1;
			h[1] = (long) RANDOM.nextInt(bigPrimeInt) + 1;
		}
		return hashFunctions;
	}

	private int hash(long [] h, long objectToHash) {
		//BigInteger result;
		//BigInteger bigObjectToHash;
		//bigObjectToHash = convertIntToBigInteger(objectToHash);
		//bigObjectToHash = BigInteger.valueOf(objectToHash);
		//result = h[1].add(h[0].multiply(bigObjectToHash));
		//return result.mod(bigPrime).mod(filterSize).intValue();
//		System.out.println("bigPrime: " + bigPrime);
//		System.out.println("object: " + objectToHash);
//		System.out.println("Integer.MAX_VALUE: " + Integer.MAX_VALUE);
//		System.out.println("h[0]: " + h[0]);
//		System.out.println("h[1]: " + h[1]);
		long obj = (objectToHash < Integer.MAX_VALUE) ? objectToHash : objectToHash - bigPrime;
		long h0 = h[0];
		long h1 = (h[1] < (Long.MAX_VALUE / 2)) ? h[1] : h[1] - bigPrime;
//		System.out.println("object: " + objectToHash);
//		System.out.println("h[0]: " + h[0]);
//		System.out.println("h[1]: " + h[1]);
//		System.out.println("product: " + obj*h0);
		long ret = (obj * h0) % bigPrime;
		ret = (ret < (Long.MAX_VALUE / 2)) ? ret : ret - bigPrime;
		return (int) (((ret + h1) % bigPrime) % (long) filterSize);
	}

	private long getPrimeLargerThan(int n) {
		BigInteger ret;
		BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
		int numBits = BigInteger.valueOf(n).bitLength() + 1;
		do {
			ret = BigInteger.probablePrime(numBits, RANDOM);
		} while (ret.compareTo(maxLong) > 1);
		return ret.longValue();
	}

	private BigInteger convertIntToBigInteger(int n)
	{
		return new BigInteger(new Integer(n).toString());
	}
}
