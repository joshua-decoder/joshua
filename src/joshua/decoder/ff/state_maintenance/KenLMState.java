package joshua.decoder.ff.state_maintenance;

/**
 * Maintains a state pointer used by KenLM to implement left-state minimization. The state object
 * may also retain the score computed by KenLM when the state was computed, since KenLM does both.
 * However, the score is only used as a caching mechanism, and isn't part of the actual state (and
 * is therefore not used when comparing KenLM states). States are equivalent only if the underlying
 * long (used to hold the KenLM pointer) are equivalent.
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public class KenLMState extends DPState {

  private long state = 0;
  private float prob = 0.0f;

  public KenLMState() {
  }

  public KenLMState(long stateId, float prob) {
    this.state = stateId;
    this.prob = prob;
  }

  public long getState() {
    return state;
  }

  public void setState(long state) {
    this.state = state;
  }

  public float getProb() {
    return prob;
  }

  @Override
  public int hashCode() {
    return (int) getState();
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof KenLMState && this.getState() == ((KenLMState) other).getState());
  }

  @Override
  public String toString() {
    return String.format("[KenLMState 0x%ld]", getState());
  }
}
