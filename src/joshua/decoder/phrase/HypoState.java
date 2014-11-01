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
  private float futureDelta;
  
  public String toString() {
    return String.format("HYPO[%s, %.5f]", history, score());
  }

  public HypoState(Hypothesis hypothesis, float future_delta) {
    history = hypothesis;
    futureDelta = future_delta;
  }
  
  public float score() {
    return history.getScore() + futureDelta;
  }

  /**
   * Reverse sort (highest scores first).
   */
  @Override
  public int compareTo(HypoState o) {
    return Float.compare(o.score(), score());
  }

  public float future() {
    return futureDelta;
  }
}
