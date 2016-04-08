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
package joshua.subsample;

import java.io.BufferedWriter;
import java.io.IOException;


/**
 * A PhrasePair-parallel BufferedWriter. In an ideal world we could get the compiler to inline all
 * of this, to have zero-overhead while not duplicating code. Alas, Java's not that cool. The
 * "final" could help on JIT at least.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */
final public class PhraseWriter {
  // Making these final requires Java6, not Java5
  private final BufferedWriter wf;
  private final BufferedWriter we;
  private final BufferedWriter wa;

  // ===============================================================
  // Constructors
  // ===============================================================
  public PhraseWriter(BufferedWriter wf_, BufferedWriter we_) {
    this(wf_, we_, null);
  }

  public PhraseWriter(BufferedWriter wf, BufferedWriter we, BufferedWriter wa) {
    this.wf = wf;
    this.we = we;
    this.wa = wa;
  }


  // ===============================================================
  // Methods
  // ===============================================================
  public void write(PhrasePair pp) throws IOException {
    this.wf.write(pp.getF().toString());
    this.we.write(pp.getE().toString());
    if (null != this.wa) this.wa.write(pp.getAlignment().toString());
  }

  public void newLine() throws IOException {
    this.wf.newLine();
    this.we.newLine();
    if (null != this.wa) this.wa.newLine();
  }

  public void flush() throws IOException {
    this.wf.flush();
    this.we.flush();
    if (null != this.wa) this.wa.flush();
  }

  public void close() throws IOException {
    this.wf.close();
    this.we.close();
    if (null != this.wa) this.wa.close();
  }
}
