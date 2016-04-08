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

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * This class provides a null-object Iterator. That is, an iterator over an empty collection.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class NullIterator<E> implements Iterable<E>, Iterator<E> {

  // ===============================================================
  // Iterable -- for foreach loops, because sometimes Java can be very stupid
  // ===============================================================

  /**
   * Return self as an iterator. We restrict the return type because some code is written to accept
   * both Iterable and Iterator, and the fact that we are both confuses Java. So this is just an
   * upcast, but more succinct to type.
   */
  public Iterator<E> iterator() {
    return this;
  }


  // ===============================================================
  // Iterator
  // ===============================================================

  /** Always returns false. */
  public boolean hasNext() {
    return false;
  }

  /** Always throws {@link NoSuchElementException}. */
  public E next() throws NoSuchElementException {
    throw new NoSuchElementException();
  }

  /** Unsupported. */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
}
