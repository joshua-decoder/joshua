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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generic progress meter for reading files (compressed or not). Pass it the raw input file stream
 * and it will keep track for you.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */
public class ProgressInputStream extends FilterInputStream {

  private long totalBytes = -1;
  private long bytesRead = 0;
  
  protected ProgressInputStream(InputStream in, long totalBytes) {
    super(in);

    this.totalBytes = totalBytes;
  }
  
  @Override
  public int read() throws IOException {
    int value = super.read();
    bytesRead += 1;
    return value;
  }
  
  @Override
  public int read(byte[] b) throws IOException {
    int value = super.read(b);
    bytesRead += value;
    return value;
  }
  
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int value = super.read(b, off, len);
    bytesRead += value;
    return value;
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    bytesRead = 0;
  }
  
  @Override
  public long skip(long bytesRead) throws IOException {
    long skip = super.skip(bytesRead);
    bytesRead += skip;
    return skip;
  }
  
  /** 
   * @return progress through the file, as an integer (0..100).
   */
  public int progress() {
    return (int)(100.0 * (float)bytesRead / (float)totalBytes);
  }
}
