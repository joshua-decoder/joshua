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
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

/**
 * A Bloom filter: a lossy data structure for set representation. A Bloom
 * filter consists of a bit set and a set of hash functions. A Bloom filter has
 * two operations: add and query. We can add an object to a Bloom filter to
 * indicate that it should be considered part of the set that the Bloom filter
 * represents. We can query the Bloom filter to see if a given object is
 * considered part of its set.
 * <p>
 * An object is added by sending it through a number of hash functions, each
 * of which returns an index into the bit set. The bit at each of the indices
 * is flipped on. We can query for an abject by sending it through the same
 * hash functions. Then we look the bit at each index that was returned by a
 * hash function. If any of the bits is unset, we know that the object is not
 * in the Bloom filter (for otherwise all the bits should have already been
 * set). If all the bits are set, we assume that the object is present in the
 * Bloom filter.
 * <p>
 * We cannot know for sure that an object is in the bloom filter just because
 * all its bits were set. There may be many collisions in the hash space, and
 * all the bits for some object might be set by chance, rather than by adding
 * that particular object.
 * <p>
 * The advantage of a Bloom filter is that its set representation can be stored
 * in a significantly smaller space than information-theoretic lossless lower
 * bounds. The price we pay for this is a certain amount of error in the query
 * function. One nice feature of the Bloom filter is that its error is
 * one-sided. This means that while the query function may return false
 * positives (saying an object is present when it really isn't), it can never
 * return false negatives (saying that an object is not present when it was
 * already added.
 */
public class BloomFilter implements Externalizable {
	/**
	 * The main bit set of the Bloom filter.
	 */
	private BitSet bitSet;

	/**
	 * The number of objects expected to be stored in the Bloom filter.
	 * The optimal number of hash functions depends on this number.
	 */
	int expectedNumberOfObjects;

	/**
	 * A prime number that should be bigger than the size of the bit set.
	 */
	long bigPrime;

	/**
	 * The size of the bit set, in bits.
	 */
	int filterSize;

	/**
	 * A random number generator for building hash functions.
	 */
	transient private Random RANDOM = new Random();

	/**
	 * Builds an empty Bloom filter, ready to build hash functions
	 * and store objects.
	 *
	 * @param filterSize the size of Bloom filter to make, in bits
	 * @param expectedNumberOfObjects the number of objects expected
	 *                                to be stored in the Bloom filter
	 */
	public BloomFilter(int filterSize, int expectedNumberOfObjects) {
		bitSet = new BitSet(filterSize);
		this.filterSize = filterSize;
		this.expectedNumberOfObjects = expectedNumberOfObjects;
		bigPrime = getPrimeLargerThan(filterSize);
	}

	/**
	 * Adds an item (represented by an integer) to the bloom
	 * filter.
	 *
	 * @param objectToAdd the object to add
	 * @param hashFunctions an array of pairs of long, representing the
	 *                      hash functions to be used on the object
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

	/**
	 * Determines whether an item (represented by an integer)
	 * is present in the bloom filter.
	 *
	 * @param objectToQuery the object we want to query for membership
	 * @param hashFunctions an array of pairs of long, representing the
	 *                      hash functions to be used
	 *
	 * @return true if the objects is assumed to be present in the Bloom
	 *         filter, false if it is definitely not present
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

	/**
	 * Builds an array of pairs of long that can be used as hash functions
	 * for this Bloom filter.
	 *
	 * @return an array of pairs of long suitable for use as hash functions
	 */
	public long [][] initializeHashFunctions() {
		int numberOfHashFunctions;
		int bigPrimeInt = (int) bigPrime;
		numberOfHashFunctions = (int) Math.floor(Math.log(2) * bitSet.length() / expectedNumberOfObjects);
		if (numberOfHashFunctions == 0) numberOfHashFunctions = 1;
		long [][] hashFunctions = new long[numberOfHashFunctions][2];
		for (long [] h : hashFunctions) {
			h[0] = (long) RANDOM.nextInt(bigPrimeInt) + 1;
			h[1] = (long) RANDOM.nextInt(bigPrimeInt) + 1;
		}
		return hashFunctions;
	}

	/**
	 * Determines which bit of the bit set should be either set, for add
	 * operations, or checked, for query operations.
	 *
	 * @param h a length-2 array of long used as a hash function
	 * @param objectToHash the object of interest
	 *
	 * @return an index into the bit set of the Bloom filter
	 */
	private int hash(long [] h, long objectToHash) {
		long obj = (objectToHash < Integer.MAX_VALUE) ? objectToHash : objectToHash - bigPrime;
		long h0 = h[0];
		long h1 = (h[1] < (Long.MAX_VALUE / 2)) ? h[1] : h[1] - bigPrime;
		long ret = (obj * h0) % bigPrime;
		ret = (ret < (Long.MAX_VALUE / 2)) ? ret : ret - bigPrime;
		return (int) (((ret + h1) % bigPrime) % (long) filterSize);
	}

	/**
	 * Finds a prime number that is larger than the given number.
	 * This is used to find bigPrime, a prime that has to be larger than
	 * the size of the Bloom filter.
	 *
	 * @param n an integer
	 *
	 * @return a prime number larger than n
	 */
	private long getPrimeLargerThan(int n) {
		BigInteger ret;
		BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
		int numBits = BigInteger.valueOf(n).bitLength() + 1;
		do {
			ret = BigInteger.probablePrime(numBits, RANDOM);
		} while (ret.compareTo(maxLong) > 1);
		return ret.longValue();
	}

	/*
	 * functions for interface externalizable
	 */

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		expectedNumberOfObjects = in.readInt();
		filterSize = in.readInt();
		bigPrime = in.readLong();
		bitSet = (BitSet) in.readObject();
	}

	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeInt(expectedNumberOfObjects);
		out.writeInt(filterSize);
		out.writeLong(bigPrime);
		out.writeObject(bitSet);
	}

	// only used for reconstruction via Externalizable
	public BloomFilter()
	{
	}
}
