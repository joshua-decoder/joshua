package joshua.decoder.ff;

import java.util.HashMap;
import java.util.Set;

/**
 * An implementation of a sparse feature vector, using for representing both weights and feature
 * values.
 *
 * @author Matt Post <post@cs.jhu.edu>
 */

public class FeatureVector {
  private HashMap<String, Float> features;

  public FeatureVector() {
    features = new HashMap<String, Float>();
  }

  public FeatureVector(String feature, float value) {
    features = new HashMap<String, Float>();
    features.put(feature, value);
  }

	public Set<String> keySet() {
		return features.keySet();
	}


	/**
	 * Adds the weights in the other feature vector to this one.  This is set union, with values
	 * shared between the two being summed.
	 */
	public void subtract(FeatureVector other) {
		for (String key: other.keySet()) {
			if (features.containsKey(key))
				features.put(key, features.get(key) - other.get(key));
		}
	}


	/**
	 * Adds the weights in the other feature vector to this one.  This is set union, with values
	 * shared between the two being summed.
	 */
	public void add(FeatureVector other) {
		for (String key: other.keySet()) {
			if (! features.containsKey(key))
				features.put(key, other.get(key));
			else
				features.put(key, features.get(key) + other.get(key));
		}
	}

	public boolean containsKey(final String feature) {
		return features.containsKey(feature);
	}

  public float get(String feature) {
    if (features.containsKey(feature))
      return features.get(feature);

    return 0.0f;
  }

  public void put(String feature, float value) {
    features.put(feature, value);
  }
}
