package joshua.util.quantization;

import java.nio.ByteBuffer;

public class IntQuantizer extends StatelessQuantizer {

  public final float read(ByteBuffer stream, int position) {
    return (float) stream.getInt(position + 4);
  }

  public final void write(ByteBuffer stream, float value) {
    stream.putInt((int) value);
  }

  public String getKey() {
    return "int";
  }

  public final int size() {
    return 4;
  }
}
