package joshua.util.encoding;

import java.nio.ByteBuffer;

public class ByteEncoder extends StatelessEncoder {

  public final float read(ByteBuffer stream, int position) {
    return (float) stream.get(position + 4);
  }

  public final void write(ByteBuffer stream, float value) {
    stream.put((byte) value);
  }

  public String getKey() {
    return "byte";
  }

  public final int size() {
    return 1;
  }
}
