package joshua.decoder.ff.tm.hash_based;

import java.util.HashMap;
import java.util.Iterator;

public class ExtensionIterator implements Iterator<Integer> {

  private Iterator<Integer> iterator;
  private boolean terminal;
  private boolean done;
  private int next;

  public ExtensionIterator(HashMap<Integer, ?> map, boolean terminal) {
    this.terminal = terminal;
    this.iterator = map.keySet().iterator();
    done = false;
    forward();
  }

  private void forward() {
    if (done)
      return;
    while (iterator.hasNext()) {
      int candidate = iterator.next();
      if ((terminal && candidate > 0) || (!terminal && candidate < 0)) {
        next = candidate;
        return;
      }
    }
    done = true;
  }

  @Override
  public boolean hasNext() {
    return !done;
  }

  @Override
  public Integer next() {
    if (done) throw new RuntimeException();
    int consumed = next;
    forward();
    return consumed;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
