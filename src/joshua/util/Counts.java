/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */
package joshua.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Maintains element co-occurrence data.
 * 
 * @author Lane Schwartz
 * @author Chris Callison-Burch
 * @param <A>
 * @param <B>
 */
public class Counts<A, B> implements Iterable<Pair<A, B>> {

  /**
   * Stores the number of times instances of A and B co-occur.
   */
  private Map<A, Map<B, Integer>> counts;

  /** Stores the number of times instances of B occur. */
  private Map<B, Integer> bTotals;

  /** Stores relative frequency estimates for p(A | B). */
  private Map<A, Map<B, Float>> probabilities;

  /** Stores relative frequency estimates for p(B | A). */
  private Map<B, Map<A, Float>> reverseProbabilities;

  /** Stores the value to return when an unseen pair is queried. */
  private float floorProbability;

  /**
   * Constructs an initially empty co-occurrence counter, with floor probability set to
   * <code>Float.MIN_VALUE</code>.
   */
  public Counts() {
    this(Float.MIN_VALUE);
  }

  /**
   * Constructs an initially empty co-occurrence counter.
   * 
   * @param floorProbability Floor probability to use when an unseen pair is queried.
   */
  public Counts(float floorProbability) {
    this.floorProbability = floorProbability;
    this.counts = new HashMap<A, Map<B, Integer>>();
    this.bTotals = new HashMap<B, Integer>();
    this.probabilities = new HashMap<A, Map<B, Float>>();
    this.reverseProbabilities = new HashMap<B, Map<A, Float>>();
  }


  /**
   * Increments the co-occurrence count of the provided objects.
   * 
   * @param a
   * @param b
   */
  public void incrementCount(A a, B b) {
    // increment the count and handle the adding of objects to the map if they aren't already there
    {
      Map<B, Integer> bMap;
      if (counts.containsKey(a)) {
        bMap = counts.get(a);
      } else {
        bMap = new HashMap<B, Integer>();
        counts.put(a, bMap);
      }

      Integer previousCount;
      if (bMap.containsKey(b)) {
        previousCount = bMap.get(b);
      } else {
        previousCount = 0;
      }
      bMap.put(b, previousCount + 1);
    }

    // increments total for o2.
    {
      Integer previousTotal;
      if (bTotals.containsKey(b)) {
        previousTotal = bTotals.get(b);
      } else {
        previousTotal = 0;
      }
      bTotals.put(b, previousTotal + 1);
    }

    // Invalidate previously calculated probabilities
    {
      if (probabilities.containsKey(a)) {
        probabilities.get(a).clear();
      }

      if (reverseProbabilities.containsKey(b)) {
        reverseProbabilities.get(b).clear();
      }
    }
  }

  /**
   * Gets the co-occurrence count for the two elements.
   * 
   * @param a
   * @param b
   * @return the co-occurrence count for the two elements
   */
  public int getCount(A a, B b) {

    int count = 0;
    if (counts.containsKey(a)) {
      Map<B, Integer> bMap = counts.get(a);
      if (bMap.containsKey(b)) {
        count = bMap.get(b);
      }
    }

    return count;
  }

  /**
   * Gets the total number of times the specified element has been seen.
   * 
   * @param b
   * @return the total number of times the specified element has been seen
   */
  int getCount(B b) {

    return (bTotals.containsKey(b) ? bTotals.get(b) : 0);

  }

  /**
   * Gets the probability of a given b.
   * <p>
   * This value is the relative frequency estimate.
   * 
   * @param a
   * @param b
   * @return the probability of a given b.
   */
  public float getProbability(A a, B b) {

    int count = getCount(a, b);
    int bCount = getCount(b);

    Float value;
    if (count == 0 || bCount == 0) {

      value = floorProbability;

    } else {

      Map<B, Float> bMap;
      if (probabilities.containsKey(a)) {
        bMap = probabilities.get(a);
      } else {
        bMap = new HashMap<B, Float>();
      }


      if (bMap.containsKey(b)) {
        value = bMap.get(b);
      } else {
        value = (float) count / (float) getCount(b);
        bMap.put(b, value);
      }

    }

    return value;
  }

  /**
   * Gets the probability of b given a.
   * <p>
   * This value is the relative frequency estimate in the reverse direction.
   * 
   * @param b
   * @param a
   * @return the probability of b given a.
   */
  public float getReverseProbability(B b, A a) {

    int count = getCount(a, b);

    Float value = floorProbability;

    if (count > 0) {

      int aCount = 0;
      for (Integer aValue : counts.get(a).values()) {
        aCount += aValue;
      }

      if (aCount > 0) {

        Map<A, Float> aMap;
        if (reverseProbabilities.containsKey(b)) {
          aMap = reverseProbabilities.get(b);
        } else {
          aMap = new HashMap<A, Float>();
        }

        if (aMap.containsKey(a)) {
          value = aMap.get(a);
        } else {
          value = (float) count / (float) aCount;
        }

      }

    }

    return value;

  }

  /**
   * Gets the floor probability that is returned whenever an unseen pair is queried.
   * 
   * @return The floor probability that is returned whenever an unseen pair is queried
   */
  public float getFloorProbability() {
    return this.floorProbability;
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(counts);
    out.writeObject(bTotals);
    out.writeObject(probabilities);
    out.writeObject(reverseProbabilities);
    out.writeFloat(floorProbability);
    // out.close();
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    this.counts = (HashMap<A, Map<B, Integer>>) in.readObject();
    this.bTotals = (HashMap<B, Integer>) in.readObject();
    this.probabilities = (HashMap<A, Map<B, Float>>) in.readObject();
    this.reverseProbabilities = (HashMap<B, Map<A, Float>>) in.readObject();
    this.floorProbability = in.readFloat();
  }

  /**
   * Gets an iterator over all counted pairs.
   * <p>
   * The pairs are not guaranteed to be iterated over in any particular order.
   * 
   * @return an iterator over all counted pairs
   */
  public Iterator<Pair<A, B>> iterator() {

    final Iterator<Entry<A, Map<B, Integer>>> aIterator = counts.entrySet().iterator();

    return new Iterator<Pair<A, B>>() {

      Entry<A, Map<B, Integer>> entry = null;
      Iterator<B> bIterator = null;

      public boolean hasNext() {
        return (bIterator != null && bIterator.hasNext()) || aIterator.hasNext();
      }

      public Pair<A, B> next() {
        if (bIterator == null || !bIterator.hasNext()) {
          entry = aIterator.next();
          bIterator = entry.getValue().keySet().iterator();
        }

        return new Pair<A, B>(entry.getKey(), bIterator.next());
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

}
