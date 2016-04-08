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

import java.util.AbstractList;
import java.util.List;

/**
 * List that performs sampling at specified intervals.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SampledList<E> extends AbstractList<E> implements List<E> {

  private final List<E> list;
  private final int size;
  private final int stepSize;

  /**
   * Constructs a sampled list backed by a provided list.
   * <p>
   * The maximum size of this list will be no greater than the provided sample size.
   * 
   * @param list List from which to sample.
   * @param sampleSize Maximum number of items to include in the new sampled list.
   */
  public SampledList(List<E> list, int sampleSize) {
    this.list = list;

    int listSize = list.size();

    if (listSize <= sampleSize) {
      this.size = listSize;
      this.stepSize = 1;
    } else {
      this.size = sampleSize;
      this.stepSize = listSize / sampleSize;
    }

  }

  @Override
  public E get(int index) {
    return list.get(index * stepSize);
  }

  @Override
  public int size() {
    return size;
  }

}
