package joshua.decoder.ff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of a sparse feature vector, using for representing both weights and feature
 * values.
 * 
 * This class is used to hold both the decoder weights and the feature values accumulated across
 * each edge. When features are read in upon decoder startup, they all start out as sparse features
 * and are stored in the hash table. After the feature functions have been loaded, the decoder
 * queries each of them for their sparse features via {@link registerDenseFeatures}. Those features
 * returned by each decoder are then *removed* from the sparse feature hash and placed in the dense
 * feature array. Therefore, when a feature registers a dense feature, it should take care to
 * query either {@link getDense()} or {@link getSparse} when asking for the feature values later on. 
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */

public class FeatureVector {
  /*
   * A list of the dense feature names. Increased via calls to registerDenseFeatures()
   */
  public static ArrayList<String> DENSE_FEATURE_NAMES = new ArrayList<String>();

  /*
   * The values of each of the dense features, defaulting to 0.
   */
  private ArrayList<Float> denseFeatures = null;

  /*
   * Value of sparse features.
   */
  private HashMap<String, Float> sparseFeatures;

  public FeatureVector() {
    sparseFeatures = new HashMap<String, Float>();
    denseFeatures = new ArrayList<Float>(DENSE_FEATURE_NAMES.size());
    for (int i = 0; i < denseFeatures.size(); i++)
      denseFeatures.set(i, 0.0f);
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
   * **IMPORTANT** The feature values are inverted, for historical reasons, which leads to a lot
   * of confusion. They have to be inverted here and when the score is actually computed. They 
   * are inverted here (which is used to build the feature vector representation of a rule's dense
   * features) and in {@link BilingualRule::estimateRuleCost()}, where the rule's precomputable
   * (weighted) score is cached.
   * 
   * @param featureString, the string of labeled and unlabeled features (probably straight from the
   *          grammar text file)
   * @param prefix, the prefix to use for unlabeled features (probably "tm_OWNER_")
   */
  public FeatureVector(String featureString, String prefix) {

//    System.err.println(String.format("FEATURES_OF(%s, %s)", featureString, prefix));
    
    /*
     * Read through the features on this rule, adding them to the feature vector. Unlabeled features
     * are converted to a canonical form.
     * 
     * Note that it's bad form to mix unlabeled features and the named feature index they are mapped
     * to, but we are being liberal in what we accept.
     * 
     * IMPORTANT: Note that, for historical reasons, the sign is reversed on all *dense* scores.
     * This is the source of *no end* of confusion and should be done away with.
     */
    sparseFeatures = new HashMap<String, Float>();
    denseFeatures = new ArrayList<Float>(DENSE_FEATURE_NAMES.size());
    for (int i = 0; i < denseFeatures.size(); i++)
      denseFeatures.set(i, 0.0f);
    
    int denseFeatureIndex = 0;

    if (!featureString.trim().equals("")) {
      for (String token : featureString.split("\\s+")) {
        if (token.indexOf('=') == -1) {
          /*
           * If we encounter an unlabeled feature, it is the next dense feature
           */
          while (denseFeatures.size() <= denseFeatureIndex)
            denseFeatures.add(0.0f);
          denseFeatures.set(denseFeatureIndex, -Float.parseFloat(token));
          denseFeatureIndex++;
        } else {
          /*
           * Labeled features are of two types: if they start with the prefix, they are actually
           * dense feature in disguise; otherwise, they are proper sparse features.
           */
          int splitPoint = token.indexOf('=');
          if (token.startsWith(prefix)) {
//            System.err.println(String.format("  PREFIX=%s '%s'.substring(%d,%d) = %s", prefix, token, prefix.length(), splitPoint,
//                token.substring(prefix.length(), splitPoint)));
            int index = Integer.parseInt(token.substring(prefix.length(), splitPoint));
            while (denseFeatures.size() <= index)
              denseFeatures.add(0.0f);
            denseFeatures.set(index, 1.0f * Float.parseFloat(token.substring(splitPoint + 1)));
          } else {
            sparseFeatures.put(token.substring(0, splitPoint),
                Float.parseFloat(token.substring(splitPoint + 1)));
          }
        }
      }
    }
  }
  
  /**
   * Register one or more dense features with the global weight vector. This assumes them global
   * IDs, and then returns the index of the first feature (from which the calling feature function
   * can infer them all). This *must* be called by every feature function wishing to register
   * dense features!
   * 
   * @param names
   * @return
   */
  public void registerDenseFeatures(ArrayList<FeatureFunction> featureFunctions) {
    for (FeatureFunction feature: featureFunctions) {
      ArrayList<String> names = feature.reportDenseFeatures(denseFeatures.size());
      for (String name: names) {
        DENSE_FEATURE_NAMES.add(name);
        denseFeatures.add(getSparse(name));
        sparseFeatures.remove(name);
      }
    }
  }
  
  public ArrayList<Float> getDenseFeatures() {
    return denseFeatures;
  }
  
  public HashMap<String,Float> getSparseFeatures() {
    return sparseFeatures;
  }

  public Set<String> keySet() {
    return sparseFeatures.keySet();
  }

  public int size() {
    return sparseFeatures.size() + denseFeatures.size();
  }

  public FeatureVector clone() {
    FeatureVector newOne = new FeatureVector();
    for (String key : this.sparseFeatures.keySet())
      newOne.set(key, this.sparseFeatures.get(key));
    for (int i = 0; i < denseFeatures.size(); i++)
      newOne.set(i, getDense(i));
    return newOne;
  }

  /**
   * Subtracts the weights in the other feature vector from this one. Note that this is not set
   * subtraction; keys found in the other FeatureVector but not in this one will be initialized with
   * a value of 0.0f before subtraction.
   */
  public void subtract(FeatureVector other) {
    for (int i = 0; i < denseFeatures.size(); i++)
      denseFeatures.set(i, getDense(i) - other.getDense(i));
    
    for (String key : other.keySet()) {
      float oldValue = (sparseFeatures.containsKey(key)) ? sparseFeatures.get(key) : 0.0f;
      sparseFeatures.put(key, oldValue - other.getSparse(key));
    }
  }

  /**
   * Adds the weights in the other feature vector to this one. This is set union, with values shared
   * between the two being summed.
   */
  public void add(FeatureVector other) {
    while (denseFeatures.size() < other.denseFeatures.size())
      denseFeatures.add(0.0f);
    
    for (int i = 0; i < other.denseFeatures.size(); i++)
      increment(i, other.getDense(i));
    
    for (String key : other.keySet()) {
      if (!sparseFeatures.containsKey(key))
        sparseFeatures.put(key, other.getSparse(key));
      else
        sparseFeatures.put(key, sparseFeatures.get(key) + other.getSparse(key));
    }
  }
  
  /**
   * Return the weight of a feature by name, after checking to determine if it is sparse or dense.
   * 
   */
  public float getWeight(String feature) {
    for (int i = 0; i < DENSE_FEATURE_NAMES.size(); i++) {
      if (DENSE_FEATURE_NAMES.get(i).equals(feature)) {
        return getDense(i);
      }
    }
    return getSparse(feature);
  }

  /**
   * Return the weight of a sparse feature, indexed by its name.
   * 
   * @param feature
   * @return the sparse feature's weight, or 0 if not found.
   */
  public float getSparse(String feature) {
    if (sparseFeatures.containsKey(feature))
      return sparseFeatures.get(feature);
    return 0.0f;
  }
  
  public boolean hasValue(String name) {
    return sparseFeatures.containsKey(name);
  }
  
  /**
   * Return the weight of a dense feature, indexed by its feature index, or 0.0f, if the feature
   * is not found. In other words, this is a safe way to query the dense feature vector.
   * 
   * @param id
   * @return the dense feature's value, or 0 if not found.
   */
  public float getDense(int id) {
    if (id < denseFeatures.size())
      return denseFeatures.get(id);
    return 0.0f;
  }

  public void increment(String feature, float value) {
    sparseFeatures.put(feature, getSparse(feature) + value);
  }
  
  public void increment(int id, float value) {
    while (id >= denseFeatures.size())
      denseFeatures.add(0.0f);
    denseFeatures.set(id, getDense(id) + value);
  }

  /**
   * Set the value of a feature. We need to first determine whether the feature is a dense or
   * sparse one, then set accordingly.
   * 
   * @param feature
   * @param value
   */
  public void set(String feature, float value) {
    for (int i = 0; i < DENSE_FEATURE_NAMES.size(); i++) {
      if (DENSE_FEATURE_NAMES.get(i).equals(feature)) {
        denseFeatures.set(i, value);
        return;
      }
    }
    // No dense feature was found; assume it's sparse
    sparseFeatures.put(feature, value);
  }
  
  public void set(int id, float value) {
    while (id >= denseFeatures.size())
      denseFeatures.add(0.0f);
    denseFeatures.set(id, value);
  }

  public Map<String, Float> getMap() {
    return sparseFeatures;
  }

  /**
   * Computes the inner product between this feature vector and another one.
   */
  public float innerProduct(FeatureVector other) {
    float cost = 0.0f;
    for (int i = 0; i < DENSE_FEATURE_NAMES.size(); i++)
      cost += getDense(i) * other.getDense(i);
    
    for (String key : sparseFeatures.keySet())
      cost += sparseFeatures.get(key) * other.getSparse(key);

    return cost;
  }

  public void times(float value) {
    for (String key : sparseFeatures.keySet())
      sparseFeatures.put(key, sparseFeatures.get(key) * value);
  }

  /***
   * Moses distinguishes sparse features as those containing an underscore, so we have to fake it
   * to be compatible with their tuners.
   */
  public String mosesString() {
    String outputString = "";
    
    HashSet<String> printed_keys = new HashSet<String>();
    
    // First print all the dense feature names in order
    for (int i = 0; i < DENSE_FEATURE_NAMES.size(); i++) {
      outputString += String.format("%s=%.3f ", DENSE_FEATURE_NAMES.get(i).replaceAll("_", "-"), getDense(i));
      printed_keys.add(DENSE_FEATURE_NAMES.get(i));
    }
    
    // Now print the sparse features
    ArrayList<String> keys = new ArrayList<String>(sparseFeatures.keySet());
    Collections.sort(keys);
    for (String key: keys) {
      if (! printed_keys.contains(key)) {
        float value = sparseFeatures.get(key);
        if (key.equals("OOVPenalty"))
          // force moses to see it as sparse
          key = "OOV_Penalty";
        outputString += String.format("%s=%.3f ", key, value);
      }
    }
    return outputString.trim();
  }
    
  /***
   * Outputs a list of feature names. All dense features are printed. Feature names are printed
   * in the order they were read in.
   */
  @Override
  public String toString() {
    String outputString = "";
    
    HashSet<String> printed_keys = new HashSet<String>();
    
    // First print all the dense feature names in order
    for (int i = 0; i < DENSE_FEATURE_NAMES.size(); i++) {
      outputString += String.format("%s=%.3f ", DENSE_FEATURE_NAMES.get(i), getDense(i));
      printed_keys.add(DENSE_FEATURE_NAMES.get(i));
    }
    
    // Now print the rest of the features
    ArrayList<String> keys = new ArrayList<String>(sparseFeatures.keySet());
    Collections.sort(keys);
    for (String key: keys)
      if (! printed_keys.contains(key))
        outputString += String.format("%s=%.3f ", key, sparseFeatures.get(key));

    return outputString.trim();
  }
}
