package joshua.util.encoding;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class EightBitQuantizer implements Encoder {

  private float[] buckets;

  public EightBitQuantizer(float[] buckets) {
    if (buckets.length != 256)
      throw new RuntimeException("Incompatible number of buckets: " + buckets.length);
    this.buckets = buckets;
  }

  public final float read(ByteBuffer stream, int position) {
    byte index = stream.get(position + 4);
    return buckets[index + 128];
  }

  public final void write(ByteBuffer stream, float value) {
    byte index = -128;

    // We search for the bucket best matching the value. Only zeroes will be
    // mapped to the zero bucket.
    if (value != 0) {
      index++;
      while (index < 126
          && (Math.abs(buckets[index + 128] - value) > (Math.abs(buckets[index + 129] - value)))) {
        index++;
      }
    }
    stream.put(index);
  }

  public String getKey() {
    return "8bit";
  }

  @Override
  public void writeState(DataOutputStream out) throws IOException {
    out.writeUTF(getKey());
    out.writeInt(buckets.length);
    for (int i = 0; i < buckets.length; i++)
      out.writeFloat(buckets[i]);
  }

  @Override
  public void readState(DataInputStream in) throws IOException {
    int length = in.readInt();
    buckets = new float[length];
    for (int i = 0; i < buckets.length; i++)
      buckets[i] = in.readFloat();
  }

  public final int size() {
    return 1;
  }
}
