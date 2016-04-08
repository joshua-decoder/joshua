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
package joshua.decoder.ff.tm.hash_based;

import java.util.HashMap;
import java.util.Iterator;

public class ExtensionIterator implements Iterator<Integer> {

  private Iterator<Integer> iterator;
  private boolean terminal;
  private boolean done;
  private int next;

  public ExtensionIterator(HashMap<Integer, ?> map, boolean terminal) {
    this.terminal = terminal;
    done = false;
    if (map == null) {
      done = true;
    } else {
      this.iterator = map.keySet().iterator();
      forward();
    }
  }

  private void forward() {
    if (done)
      return;
    while (iterator.hasNext()) {
      int candidate = iterator.next();
      if ((terminal && candidate > 0) || (!terminal && candidate < 0)) {
        next = candidate;
        return;
      }
    }
    done = true;
  }

  @Override
  public boolean hasNext() {
    return !done;
  }

  @Override
  public Integer next() {
    if (done)
      throw new RuntimeException();
    int consumed = next;
    forward();
    return consumed;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
