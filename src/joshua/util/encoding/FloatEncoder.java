package joshua.util.encoding;

import java.nio.ByteBuffer;

public class FloatEncoder extends StatelessEncoder {

  public final float read(ByteBuffer stream, int position) {
    return stream.getFloat(position + 4);
  }

  public final void write(ByteBuffer stream, float value) {
    stream.putFloat(value);
  }

  public String getKey() {
    return "float";
  }

  public final int size() {
    return 4;
  }
}
