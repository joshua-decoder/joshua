package joshua.util.encoding;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public enum PrimitiveFloatEncoder implements FloatEncoder {

  BYTE("byte", 1) {
    public final float read(ByteBuffer stream, int position) {
      return (float) stream.get(position + EncoderConfiguration.ID_SIZE);
    }

    public final void write(ByteBuffer stream, float value) {
      stream.put((byte) value);
    }
  },

  BOOLEAN("boolean", 0) {
    public final float read(ByteBuffer stream, int position) {
      return 1.0f;
    }

    public final void write(ByteBuffer stream, float value) {
    }
  },

  CHAR("char", 2) {
    public final float read(ByteBuffer stream, int position) {
      return (float) stream.getChar(position + EncoderConfiguration.ID_SIZE);
    }

    public final void write(ByteBuffer stream, float value) {
      stream.putChar((char) value);
    }
  },

  FLOAT("float", 4) {
    public final float read(ByteBuffer stream, int position) {
      return stream.getFloat(position + EncoderConfiguration.ID_SIZE);
    }

    public final void write(ByteBuffer stream, float value) {
      stream.putFloat(value);
    }
  },

  INT("int", 4) {
    public final float read(ByteBuffer stream, int position) {
      return (float) stream.getInt(position + EncoderConfiguration.ID_SIZE);
    }

    public final void write(ByteBuffer stream, float value) {
      stream.putInt((int) value);
    }
  },

  SHORT("short", 2) {
    public final float read(ByteBuffer stream, int position) {
      return (float) stream.getShort(position + EncoderConfiguration.ID_SIZE);
    }

    public final void write(ByteBuffer stream, float value) {
      stream.putShort((short) value);
    }
  };

  private final String key;
  private final int size;

  private PrimitiveFloatEncoder(String k, int s) {
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

  public static PrimitiveFloatEncoder get(String k) {
    PrimitiveFloatEncoder encoder;
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
  public abstract float read(ByteBuffer stream, int position);

  @Override
  public abstract void write(ByteBuffer stream, float value);
}