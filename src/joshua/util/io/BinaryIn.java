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

import java.io.DataInput;
import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectStreamConstants;
import java.io.RandomAccessFile;

public class BinaryIn<E extends Externalizable> extends RandomAccessFile
    implements
      DataInput,
      ObjectInput {

  private final Class<E> type;

  public BinaryIn(String filename, Class<E> type) throws FileNotFoundException {
    super(filename, "r");
    this.type = type;
  }

  public int available() throws IOException {
    long pos = getFilePointer();
    long length = length();

    long bytesAvailable = length - pos;

    if (bytesAvailable > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    } else {
      return (int) bytesAvailable;
    }
  }

  public E readObject() throws ClassNotFoundException, IOException {

    int b = peek();

    if (b == ObjectStreamConstants.TC_NULL) {

      return null;

    } else {

      E obj;
      try {
        obj = type.newInstance();
        obj.readExternal(this);
        return obj;
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }


    }
  }

  public long skip(long n) throws IOException {

    long bytesSkipped = 0;

    while (n > 0) {
      if (n > Integer.MAX_VALUE) {
        bytesSkipped += skipBytes(Integer.MAX_VALUE);
        n -= Integer.MAX_VALUE;
      } else {
        bytesSkipped = skipBytes((int) n);
        n = 0;
      }
    }

    return bytesSkipped;
  }



  private int peek() throws IOException {
    long pos = getFilePointer();
    int b = read();
    seek(pos);
    return b;
  }
}
