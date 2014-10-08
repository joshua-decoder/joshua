package joshua.decoder.phrase;

import java.util.ArrayList;	
import java.util.Collections;
import java.util.List;

import joshua.decoder.ff.FeatureVector;
import joshua.decoder.ff.tm.Rule;

/**
 * Represents a sorted collection of target-side phrases. Typically, these are phrases
 * generated from the same source word sequence. 
 * 
 * @author Matt Post
 */

public class TargetPhrases extends ArrayList<Rule> {

  public TargetPhrases() {
    super();
  }
  
  /**
   * Initialize with a collection of rules.
   * 
   * @param list
   */
  public TargetPhrases(List<Rule> list) {
    super();
    
    for (Rule rule: list) {
      add(rule);
    }
  }
  
  /**
   * Score the rules and sort them. Scoring is necessary because rules are only scored if they
   * are used, in an effort to make reading in rules more efficient. This is starting to create
   * some trouble and should probably be reworked.
   */
  public void finish(FeatureVector weights) {
    for (Rule rule: this) { 
      if (rule.getPrecomputableCost() <= Float.NEGATIVE_INFINITY) {
        float score = rule.getFeatureVector().innerProduct(weights);
        rule.setPrecomputableCost(score);
      }
    }
    Collections.sort(this);
  }
}
