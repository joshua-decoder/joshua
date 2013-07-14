package joshua.decoder.ff.state_maintenance;

/**
 * Maintains a state pointer used by KenLM to implement left-state minimization. 
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public class KenLMState extends DPState {

  private long state = 0;
  private long hash = 0;

  public KenLMState() {
  }

  public KenLMState(long stateId, long hash) {
    this.state = stateId;
    this.hash = hash;
  }

  public long getState() {
    return state;
  }

  public long getHash() {
    return hash;
  }

  @Override
  public int hashCode() {
    return (int) ((getHash() >> 32) ^ getHash());
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof KenLMState && this.getHash() == ((KenLMState) other).getHash());
  }

  @Override
  public String toString() {
    return String.format("[KenLMState %d/%d]", getState(), getHash());
  }
}
