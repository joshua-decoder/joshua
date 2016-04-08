/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.util;

import java.util.Comparator;

/**
 * Represents an object being counted, with the associated count.
 * 
 * @author Lane Schwartz
 */
public class Counted<E> implements Comparable<Counted<E>> {

  /** The element being counted. */
  private final E element;

  /** The count associated with the element. */
  private final Integer count;

  /**
   * Constructs an object wrapping an element and its associated count.
   * 
   * @param element An element being counted
   * @param count The count associated with the element
   */
  public Counted(E element, int count) {
    this.element = element;
    this.count = count;
  }

  /**
   * Gets the count associated with this object's element.
   * 
   * @return The count associated with this object's element
   */
  public int getCount() {
    return count;
  }

  /**
   * Gets the element associated with this object.
   * 
   * @return The element associated with this object
   */
  public E getElement() {
    return element;
  }

  /**
   * Compares this object to another counted object, according to the natural order of the counts
   * associated with each object.
   * 
   * @param o Another counted object
   * @return -1 if the count of this object is less than the count of the other object, 0 if the
   *         counts are equal, or 1 if the count of this object is greater than the count of the
   *         other object
   */
  public int compareTo(Counted<E> o) {
    return count.compareTo(o.count);
  }

  /**
   * Gets a comparator that compares two counted objects based on the reverse of the natural order
   * of the counts associated with each object.
   * 
   * @param <E>
   * @return A comparator that compares two counted objects based on the reverse of the natural
   *         order of the counts associated with each object
   */
  public static <E> Comparator<Counted<E>> getDescendingComparator() {
    return new Comparator<Counted<E>>() {
      public int compare(Counted<E> o1, Counted<E> o2) {
        return (o2.count.compareTo(o1.count));
      }
    };
  }
}
