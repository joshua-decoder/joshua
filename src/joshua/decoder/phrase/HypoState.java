package joshua.decoder.phrase;

/***
 * This class wraps a {@link Hypothesis} (an item already on a stack) with a future cost
 * estimate. This cost estimate reflects the length and location of the (unidentified) phrase used
 * to extend the hypothesis. This lets us more fairly sort Hypotheses when considering extending
 * them, since hypothesis h1 covering 2 words and extended with a 3-word phrase will now be nearer
 * in score to hypothesis h2 covering 4 words and extended with a 1-word phrase.
 */

public class HypoState implements Comparable<HypoState> {

  public Hypothesis history = null;

  // The hypothesis score plus compensations from the future cost estimate
  public float score = 0.0f;
  
  public String toString() {
    return String.format("HYPO[%s, %.5f]", history, score);
  }

  public HypoState() {
    history = null;
    score = 0.0f;
  }

  public HypoState(Hypothesis hypothesis, float score_delta) {
    history = hypothesis;
    score = hypothesis.getScore() + score_delta;
  }

  /**
   * Reverse sort (highest scores first).
   */
  @Override
  public int compareTo(HypoState o) {
    return Float.compare(o.score, score);
  }
}
