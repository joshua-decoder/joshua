/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package joshua.util.encoding;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.TreeMap;

import joshua.util.io.LineReader;

public class Analyzer {

  private TreeMap<Float, Integer> histogram;
  private int total;

  public Analyzer() {
    histogram = new TreeMap<Float, Integer>();
    initialize();
  }

  public void initialize() {
    histogram.clear();
    // TODO: drop zero bucket; we won't encode zero-valued features anyway.
    histogram.put(0.0f, 0);
    total = 0;
  }

  public void add(float key) {
    if (histogram.containsKey(key))
      histogram.put(key, histogram.get(key) + 1);
    else
      histogram.put(key, 1);
    total++;
  }

  public float[] quantize(int num_bits) {
    float[] buckets = new float[1 << num_bits];

    // We make sure that 0.0f always has its own bucket, so the bucket
    // size is determined excluding the zero values.
    int size = (total - histogram.get(0.0f)) / (buckets.length - 1);
    buckets[0] = 0.0f;

    int old_size = -1;
    while (old_size != size) {
      int sum = 0;
      int count = buckets.length - 1;
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
        if (count != 0 && index < buckets.length - 2) {
          buckets[index++] = (float) sum / count;
          count = 0;
          sum = 0;
        }
        if (key == 0)
          continue;
      }
      count += value;
      sum += key * value;
      // Check if the bucket is full.
      if (count >= size && index < buckets.length - 2) {
        buckets[index++] = (float) sum / count;
        count = 0;
        sum = 0;
      }
      last_key = key;
    }
    if (count > 0 && index < buckets.length - 1)
      buckets[index++] = (float) sum / count;
    
    float[] shortened = new float[index];
    for (int i = 0; i < shortened.length; ++i)
      shortened[i] = buckets[i];
    return shortened;
  }

  public boolean isBoolean() {
    for (float value : histogram.keySet())
      if (value != 0 && value != 1)
        return false;
    return true;
  }

  public boolean isByte() {
    for (float value : histogram.keySet())
      if (Math.ceil(value) != value || value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)
        return false;
    return true;
  }

  public boolean isShort() {
    for (float value : histogram.keySet())
      if (Math.ceil(value) != value || value < Short.MIN_VALUE || value > Short.MAX_VALUE)
        return false;
    return true;
  }

  public boolean isChar() {
    for (float value : histogram.keySet())
      if (Math.ceil(value) != value || value < Character.MIN_VALUE || value > Character.MAX_VALUE)
        return false;
    return true;
  }

  public boolean isInt() {
    for (float value : histogram.keySet())
      if (Math.ceil(value) != value)
        return false;
    return true;
  }

  public boolean is8Bit() {
    return (histogram.keySet().size() <= 256);
  }

  public FloatEncoder inferUncompressedType() {
    if (isBoolean())
      return PrimitiveFloatEncoder.BOOLEAN;
    if (isByte())
      return PrimitiveFloatEncoder.BYTE;
    if (is8Bit())
      return new EightBitQuantizer(this.quantize(8));
    if (isChar())
      return PrimitiveFloatEncoder.CHAR;
    if (isShort())
      return PrimitiveFloatEncoder.SHORT;
    if (isInt())
      return PrimitiveFloatEncoder.INT;
    return PrimitiveFloatEncoder.FLOAT;
  }
  
  public FloatEncoder inferType(int bits) {
    if (isBoolean())
      return PrimitiveFloatEncoder.BOOLEAN;
    if (isByte())
      return PrimitiveFloatEncoder.BYTE;
    if (bits == 8 || is8Bit())
      return new EightBitQuantizer(this.quantize(8));
    // TODO: Could add sub-8-bit encoding here (or larger).
    if (isChar())
      return PrimitiveFloatEncoder.CHAR;
    if (isShort())
      return PrimitiveFloatEncoder.SHORT;
    if (isInt())
      return PrimitiveFloatEncoder.INT;
    return PrimitiveFloatEncoder.FLOAT;
  }

  public String toString(String label) {
    StringBuilder sb = new StringBuilder();
    for (float val : histogram.keySet())
      sb.append(label + "\t" + String.format("%.5f", val) + "\t" + histogram.get(val) + "\n");
    return sb.toString();
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
    Analyzer q = new Analyzer();

    q.initialize();
    for (int i = 0; i < n; i++)
      q.add(s.get(i));
    EightBitQuantizer eb = new EightBitQuantizer(q.quantize(8));
    System.out.println("Quantizer learned.");

    for (int i = 0; i < n; i++)
      eb.write(b, s.get(i));
    b.rewind();
    System.out.println("Quantization complete.");

    float avg_error = 0;
    float error = 0;
    int count = 0;
    for (int i = -4; i < n - 4; i++) {
      float coded = eb.read(b, i);
      if (s.get(i + 4) != 0) {
        error = Math.abs(s.get(i + 4) - coded);
        avg_error += error;
        count++;
      }
    }
    avg_error /= count;
    System.out.println("Evaluation complete.");

    System.out.println("Average quanitization error over " + n + " samples is: " + avg_error);
  }
}
