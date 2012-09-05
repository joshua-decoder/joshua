package joshua.util.quantization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;

import joshua.util.io.LineReader;

public class EightBitQuantizer implements Quantizer {

  private float[] buckets;

  private transient TreeMap<Float, Integer> histogram;
  private transient int total;

  public EightBitQuantizer() {
    buckets = new float[256];
    histogram = new TreeMap<Float, Integer>();
  }

  @Override
  public void initialize() {
    histogram.clear();
    histogram.put(0.0f, 0);
    total = 0;
  }

  @Override
  public void add(float key) {
    if (histogram.containsKey(key))
      histogram.put(key, histogram.get(key) + 1);
    else
      histogram.put(key, 1);
    total++;
  }

  @Override
  public void finalize() {
    // We make sure that 0.0f always has its own bucket, so the bucket
    // size is determined excluding the zero values.
    int size = (total - histogram.get(0.0f)) / (buckets.length - 1);
    buckets[0] = 0.0f;

    int old_size = -1;
    while (old_size != size) {
      int sum = 0;
      int count = 255;
      for (float key : histogram.keySet()) {
        int entry_count = histogram.get(key);
        if (entry_count < size && key != 0)
          sum += entry_count;
        else
          count--;
      }
      old_size = size;
      size = sum / count;
    }

    float last_key = Float.MAX_VALUE;

    int index = 1;
    int count = 0;
    float sum = 0.0f;

    int value;
    for (float key : histogram.keySet()) {
      value = histogram.get(key);
      // Special bucket termination cases: zero boundary and histogram spikes.
      if (key == 0 || (last_key < 0 && key > 0) || (value >= size)) {
        // If the count is not 0, i.e. there were negative values, we should
        // not bucket them with the positive ones. Close out the bucket now.
        if (count != 0 && index < 254) {
          buckets[index++] = (float) sum / count;
          count = 0;
          sum = 0;
        }
        if (key == 0) continue;
      }
      count += value;
      sum += key * value;
      // Check if the bucket is full.
      if (count >= size && index < 254) {
        buckets[index++] = (float) sum / count;
        count = 0;
        sum = 0;
      }
      last_key = key;
    }
    if (index < 255) buckets[index++] = (float) sum / count;
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

  public static void main(String[] args) throws IOException {
    LineReader reader = new LineReader(args[0]);
    ArrayList<Float> s = new ArrayList<Float>();

    System.out.println("Initialized.");
    while (reader.hasNext())
      s.add(Float.parseFloat(reader.next().trim()));
    System.out.println("Data read.");
    int n = s.size();
    byte[] c = new byte[n];
    ByteBuffer b = ByteBuffer.wrap(c);
    EightBitQuantizer q = new EightBitQuantizer();

    q.initialize();
    for (int i = 0; i < n; i++)
      q.add(s.get(i));
    q.finalize();
    System.out.println("Quantizer learned.");

    for (int i = 0; i < n; i++)
      q.write(b, s.get(i));
    b.rewind();
    System.out.println("Quantization complete.");

    float avg_error = 0;
    float error = 0;
    int count = 0;
    for (int i = 0; i < n; i++) {
      float coded = q.read(b, i);
      if (s.get(i) != 0) {
        error = Math.abs(s.get(i) - coded);
        avg_error += error;
        count++;
      }
    }
    avg_error /= count;
    System.out.println("Evaluation complete.");

    System.out.println("Average quanitization error over " + n + " samples is: " + avg_error);
  }
}
