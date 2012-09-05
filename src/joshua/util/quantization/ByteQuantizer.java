package joshua.util.quantization;

import java.nio.ByteBuffer;

public class ByteQuantizer extends StatelessQuantizer {

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
