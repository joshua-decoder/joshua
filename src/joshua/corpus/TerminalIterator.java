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
package joshua.corpus;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator capable of iterating over those word identifiers in a phrase which represent terminals.
 * <p>
 * <em>Note</em>: This class is <em>not</em> thread-safe.
 * 
 * @author Lane Schwartz
 */
public class TerminalIterator implements Iterator<Integer> {

  private final int[] words;

  private int nextIndex = -1;
  private int next = Integer.MIN_VALUE;
  private boolean dirty = true;

  /**
   * Constructs an iterator for the terminals in the given list of words.
   * 
   * @param vocab
   * @param words
   */
  public TerminalIterator(int[] words) {
    this.words = words;
  }

  /* See Javadoc for java.util.Iterator#next(). */
  public boolean hasNext() {

    while (dirty || Vocabulary.nt(next)) {
      nextIndex++;
      if (nextIndex < words.length) {
        next = words[nextIndex];
        dirty = false;
      } else {
        return false;
      }
    }

    return true;
  }

  /* See Javadoc for java.util.Iterator#next(). */
  public Integer next() {
    if (hasNext()) {
      dirty = true;
      return next;
    } else {
      throw new NoSuchElementException();
    }
  }

  /**
   * Unsupported operation, guaranteed to throw an UnsupportedOperationException.
   * 
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
