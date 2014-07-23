package joshua.decoder.ff.state_maintenance;

/**
 * Abstract class enforcing explicit implementation of the standard methods.
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author Juri Ganitkevitch, <juri@cs.jhu.edu>
 */
public abstract class DPState {

  public abstract String toString();

  public abstract int hashCode();

  public abstract boolean equals(Object other);
}
