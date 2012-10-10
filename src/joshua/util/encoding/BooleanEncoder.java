package joshua.util.encoding;

import java.nio.ByteBuffer;

/**
 * Standard quantizer for boolean types.
 * 
 * @author jg
 * 
 */
public class BooleanEncoder extends StatelessEncoder {

  public final float read(ByteBuffer stream, int position) {
    return 1.0f;
  }

  public final void write(ByteBuffer stream, float value) {}

  public String getKey() {
    return "boolean";
  }

  public final int size() {
    return 0;
  }
}
