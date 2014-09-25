package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;

/**
 * Represents a sorted collection of target-side phrases. Typically, these are phrases
 * generated from the same source word sequence. 
 * 
 * @author Matt Post
 */

public class TargetPhrases extends ArrayList<Phrase> {

  public String toString() {
    return "TargetPhrase::toString()";
  }

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
      add((Phrase)rule);
    }
  }
  
  /**
   * Sort the phrases.
   */
  public void finish() {
    Collections.sort(this);
  }
  
  /**
   * Extend the current list with items from another list
   * 
   * @param more the other list
   */
  public void extend(RuleCollection more) {
    for (Rule rule: more.getRules())
      add((Phrase)more);
  }
  
  public void MakePassThrough(Scorer scorer, int word) {
    Phrase target = new Phrase(word);
    float score = scorer.passThrough()
        + scorer.LM(word) 
        + scorer.TargetWordCount(1);
    target.setScore(score);
    add(target);
  }
  
}
