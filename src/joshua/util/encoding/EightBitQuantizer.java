package joshua.util.encoding;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class EightBitQuantizer implements FloatEncoder {

  private float[] buckets;

  public EightBitQuantizer() {
    this.buckets = new float[256];
  }

  public EightBitQuantizer(float[] buckets) {
    if (buckets.length > 256)
      throw new RuntimeException("Incompatible number of buckets: " + buckets.length);
    this.buckets = buckets;
  }

  @Override
  public final float read(ByteBuffer stream, int position) {
    byte index = stream.get(position + EncoderConfiguration.ID_SIZE);
    return buckets[index + 128];
  }

  @Override
  public final void write(ByteBuffer stream, float val) {
    byte index = -128;

    // We search for the bucket best matching the value. Only zeroes will be
    // mapped to the zero bucket.
    if (val != 0 && buckets.length > 1) {
      int t = 1;
      int b = buckets.length - 1;
      while ((b - t) > 1) {
        int half = (t + b) / 2;
        if (val >= buckets[half])
          t = half;
        if (val <= buckets[half])
          b = half;
      }
      index = (byte) ((Math.abs(buckets[t] - val) > (Math.abs(buckets[b] - val)) ? b : t) - 128);
    }
    stream.put(index);
  }

  @Override
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

  @Override
  public final int size() {
    return 1;
  }
}
