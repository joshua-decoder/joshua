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

  public float get(String feature) {
    if (features.containsKey(feature))
      return features.get(feature);

    return 0.0f;
  }

  public void put(String feature, float value) {
    features.put(feature, value);
  }
}
