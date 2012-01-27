package joshua.util.quantization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public class EightBitQuantizer implements Quantizer {

	private float[] buckets;

	private transient TreeMap<Float, Integer> histogram;
	private transient int total;

	public EightBitQuantizer() {
		buckets = new float[256];
	}

	@Override
	public void initialize() {
		if (histogram == null)
			histogram = new TreeMap<Float, Integer>();
		histogram.clear();
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
		int size = (total - histogram.get(0.0)) / buckets.length;
		buckets[0] = 0.0f;

		boolean done = false;
		Map.Entry<Float, Integer> entry = histogram.firstEntry();
		float last_key = entry.getKey();

		int index = 1;
		int count = 0;
		float sum = 0.0f;

		float key;
		int value;
		while (!done) {
			key = entry.getKey();
			value = entry.getValue();

			// Special handling of the zero value (or boundary) in the histogram.
			if (key == 0.0 || (last_key < 0 && key > 0)) {
				// If the count is not 0, i.e. there were negative values, we should
				// not bucket them with the positive ones. Close out the bucket now.
				if (count != 0) {
					buckets[index++] = (float) sum / count;
					count = 0;
					sum = 0.0f;
				}
				continue;
			}
			count += value;
			sum += key * value;
			// Check if the bucket is full.
			if (count >= size) {
				buckets[index++] = (float) sum / count;
				count = 0;
				sum = 0.0f;
			}
			last_key = key;
			entry = histogram.higherEntry(key);
			done = (entry == null);
		}
		if (count >= size) {
			buckets[index++] = (float) sum / count;
			count = 0;
			sum = 0.0f;
		}
	}

	public float read(ByteBuffer stream) {
		return buckets[stream.get()];
	}

	public void write(ByteBuffer stream, float value) {
		byte index = 0;

		// We search for the bucket best matching the value. Only zeroes will be
		// mapped to the zero bucket.
		if (value != 0.0f) {
			index = 1;
			while ((Math.abs(buckets[index] - value) > (Math.abs(buckets[index + 1]
					- value)))) {
				index++;
			}
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
}
