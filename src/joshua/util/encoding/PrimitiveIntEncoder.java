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
package joshua.util.encoding;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public enum PrimitiveIntEncoder implements IntEncoder {

  // TODO: the inconsistency with FloatEncoders is dangerous.
  BYTE("byte", 1) {
    public final int read(ByteBuffer stream, int position) {
      return (int) stream.get(position);
    }

    public final void write(ByteBuffer stream, int value) {
      stream.put((byte) value);
    }
  },

  CHAR("char", 2) {
    public final int read(ByteBuffer stream, int position) {
      return (int) stream.getChar(position);
    }

    public final void write(ByteBuffer stream, int value) {
      stream.putChar((char) value);
    }
  },

  INT("int", 4) {
    public final int read(ByteBuffer stream, int position) {
      return (int) stream.getInt(position);
    }

    public final void write(ByteBuffer stream, int value) {
      stream.putInt((int) value);
    }
  },

  SHORT("short", 2) {
    public final int read(ByteBuffer stream, int position) {
      return (int) stream.getShort(position);
    }

    public final void write(ByteBuffer stream, int value) {
      stream.putShort((short) value);
    }
  };

  private final String key;
  private final int size;

  private PrimitiveIntEncoder(String k, int s) {
    key = k;
    size = s;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public int size() {
    return size;
  }

  public static PrimitiveIntEncoder get(String k) {
    PrimitiveIntEncoder encoder;
    try {
      encoder = valueOf(k);
    } catch (IllegalArgumentException e) {
      return null;
    }
    return encoder;
  }

  @Override
  public void readState(DataInputStream in) throws IOException {
  }

  @Override
  public void writeState(DataOutputStream out) throws IOException {
    out.writeUTF(getKey());
  }

  @Override
  public abstract int read(ByteBuffer stream, int position);

  @Override
  public abstract void write(ByteBuffer stream, int value);
}