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
package joshua.util.io;

import java.io.IOException;

import joshua.util.NullIterator;


/**
 * This class provides a null-object Reader. This is primarily useful for when you may or may not
 * have a {@link Reader}, and you don't want to check for null all the time. All operations are
 * no-ops.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2009-03-26 15:06:57 -0400 (Thu, 26 Mar 2009) $
 */
public class NullReader<E> extends NullIterator<E> implements Reader<E> {

  // ===============================================================
  // Constructors and destructors
  // ===============================================================

  // TODO: use static factory method and singleton?
  public NullReader() {}

  /** A no-op. */
  public void close() throws IOException {}


  // ===============================================================
  // Reader
  // ===============================================================

  /**
   * Always returns true. Is this correct? What are the semantics of ready()? We're always capable
   * of delivering nothing, but we're never capable of delivering anything...
   */
  public boolean ready() {
    return true;
  }

  /** Always returns null. */
  public E readLine() throws IOException {
    return null;
  }
}
