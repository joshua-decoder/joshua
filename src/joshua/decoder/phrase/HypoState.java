package joshua.decoder.phrase;

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
