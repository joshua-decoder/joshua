package joshua.util.quantization;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public class EightBitQuantizer implements Quantizer, Serializable {

	private static final long serialVersionUID = -5677248576836311734L;
	private float[] buckets;

	private transient TreeMap<Double, Integer> histogram;
	private transient int total;

	public EightBitQuantizer() {
		buckets = new float[256];
	}

	public void initialize() {
		if (histogram == null)
			histogram = new TreeMap<Double, Integer>();
		histogram.clear();
		total = 0;
	}

	public void add(double key) {
		if (histogram.containsKey(key))
			histogram.put(key, histogram.get(key) + 1);
		else
			histogram.put(key, 1);
		total++;
	}

	public void finalize() {
		// We make sure that 0.0f always has its own bucket, so the bucket
		// size is determined excluding the zero values.
		int size = (total - histogram.get(0.0)) / buckets.length;
		buckets[0] = 0.0f;

		boolean done = false;
		Map.Entry<Double, Integer> entry = histogram.firstEntry();
		double last_key = entry.getKey();

		int index = 1;
		int count = 0;
		double sum = 0.0;

		double key;
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
					sum = 0.0;
				}
				continue;
			}
			count += value;
			sum += key * value;
			// Check if the bucket is full.
			if (count >= size) {
				buckets[index++] = (float) sum / count;
				count = 0;
				sum = 0.0;
			}
			last_key = key;
			entry = histogram.higherEntry(key);
		}
	}

	public float retrieve(ByteBuffer stream) {
		return buckets[stream.get()];
	}
}
