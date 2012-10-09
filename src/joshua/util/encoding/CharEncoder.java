package joshua.util.encoding;

import java.nio.ByteBuffer;

public class CharEncoder extends StatelessEncoder {

  public final float read(ByteBuffer stream, int position) {
    return (float) stream.getChar(position + 4);
  }

  public final void write(ByteBuffer stream, float value) {
    stream.putChar((char) value);
  }

  public String getKey() {
    return "char";
  }

  public final int size() {
    return 2;
  }
}
