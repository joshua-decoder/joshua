package joshua.decoder.ff.state_maintenance;

/**
 * Maintains a state pointer used by KenLM to implement left-state minimization. 
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @author Juri Ganitkevitch <juri@cs.jhu.edu>
 */
public class KenLMState extends DPState {

  private long state = 0;

  public KenLMState() {
  }

  public KenLMState(long stateId) {
    this.state = stateId;
  }

  public long getState() {
    return state;
  }

  public void setState(long state) {
    this.state = state;
  }

  @Override
  public int hashCode() {
    return (int) ((getState() >> 32) ^ getState());
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof KenLMState && this.getState() == ((KenLMState) other).getState());
  }

  @Override
  public String toString() {
    return String.format("[KenLMState 0x%d]", getState());
  }
}
