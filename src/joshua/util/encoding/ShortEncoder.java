package joshua.util.encoding;

import java.nio.ByteBuffer;

public class ShortEncoder extends StatelessEncoder {

  public final float read(ByteBuffer stream, int position) {
    return (float) stream.getShort(position + 4);
  }

  public final void write(ByteBuffer stream, float value) {
    stream.putShort((short) value);
  }

  public String getKey() {
    return "short";
  }

  public final int size() {
    return 2;
  }
}
