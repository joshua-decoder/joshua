package joshua.util.quantization;

import java.nio.ByteBuffer;

public class CharQuantizer extends StatelessQuantizer {

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
