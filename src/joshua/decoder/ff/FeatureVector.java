package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  /**
   * This version of the constructor takes an uninitialized feature with potentially intermingled
   * labeled and unlabeled feature values, of the format:
   * 
   * [feature1=]value [feature2=]value
   * 
   * It produces a Feature Vector where all unlabeled features have been labeled by appending the
   * unlabeled feature index (starting at 0) to the defaultPrefix value.
   * 
   * @param featureString, the string of labeled and unlabeled features (probably straight from the
   *          grammar text file)
   * @param prefix, the prefix to use for unlabeled features (probably "tm_OWNER_")
   */
  public FeatureVector(String featureString, String prefix) {

    /*
     * Read through the features on this rule, adding them to the feature vector. Unlabeled features
     * are converted to a canonical form.
     * 
     * Note that it's bad form to mix unlabeled features and the named feature index they are mapped
     * to, but we are being liberal in what we accept.
     */
    features = new HashMap<String, Float>();
    int denseFeatureIndex = 0;

    if (!featureString.trim().equals("")) {
      for (String token : featureString.split("\\s+")) {
        if (token.indexOf('=') == -1) {
          features.put(String.format("%s%d", prefix, denseFeatureIndex), -Float.parseFloat(token));
          denseFeatureIndex++;
        } else {
          int splitPoint = token.indexOf('=');
          features.put(token.substring(0, splitPoint),
              Float.parseFloat(token.substring(splitPoint + 1)));
        }
      }
    }
  }

  public Set<String> keySet() {
    return features.keySet();
  }

  public int size() {
    return features.size();
  }

  public FeatureVector clone() {
    FeatureVector newOne = new FeatureVector();
    for (String key : this.features.keySet())
      newOne.put(key, this.features.get(key));
    return newOne;
  }

  /**
   * Subtracts the weights in the other feature vector from this one. Note that this is not set
   * subtraction; keys found in the other FeatureVector but not in this one will be initialized with
   * a value of 0.0f before subtraction.
   */
  public void subtract(FeatureVector other) {
    for (String key : other.keySet()) {
      float oldValue = (features.containsKey(key)) ? features.get(key) : 0.0f;
      features.put(key, oldValue - other.get(key));
    }
  }

  /**
   * Adds the weights in the other feature vector to this one. This is set union, with values shared
   * between the two being summed.
   */
  public void add(FeatureVector other) {
    for (String key : other.keySet()) {
      if (!features.containsKey(key))
        features.put(key, other.get(key));
      else
        features.put(key, features.get(key) + other.get(key));
    }
  }

  public boolean containsKey(final String feature) {
    return features.containsKey(feature);
  }

  /**
   * This method returns the weight of a feature if it exists and otherwise throws a runtime error.
   * It is the duty of the programmer to check using the method containsKey if a feature with a
   * certain name exists. Previously this method would return 0 if the key did not exists, but this
   * lead to bugs in other parts of the code because Feature Names are often specified in capitals
   * but then lowercased, but in using the get method the lowercase form is not used consistently.
   * It is therefore good defensive programming to just throw an error when someone tries to get a
   * feature that does not exist - this will automatically eliminate such hard to debug errors. This
   * is what is now implemented.
   * 
   * @param feature
   * @return
   */
  public float get(String feature) {
    if (features.containsKey(feature))
      return features.get(feature);

    throw new RuntimeException(
        "Error : unknown feature "
            + feature
            + " Beware: The behavior has been changed.\n"
            + "This method no longer returns 0 for non-present features. Instead it is the responsibility of the querying function to make "
            + "sure the value exists before requesting it");
  }

  public void put(String feature, float value) {
    features.put(feature, value);
  }

  public Map<String, Float> getMap() {
    return features;
  }

  /**
   * Computes the inner product between this feature vector and another one.
   */
  public float innerProduct(FeatureVector other) {
    float cost = 0.0f;
    for (String key : features.keySet())
      if (other.containsKey(key))
        cost += features.get(key) * other.get(key);

    return cost;
  }

  public void times(float value) {
    for (String key : features.keySet())
      features.put(key, features.get(key) * value);
  }

  public String toString() {
    String outputString = "";
    List<String> sortedKeys = new ArrayList<String>(features.keySet());
    Collections.sort(sortedKeys);
    for (String key : sortedKeys)
      if (features.get(key) != 0.0f)
        outputString += String.format("%s%s=%.3f", (outputString.length() > 0) ? " " : "", key,
            features.get(key));
    return outputString;
  }
}
