package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A small wrapper around a list of {@link HypoState} objects, which represent {@link Hypothesis}
 * items with a future cost estimate, for sorting during cube pruning.
 */
public class HypoStateList extends ArrayList<HypoState> {

  private static final long serialVersionUID = 1L;

  public HypoStateList() {
    super();
  }

  public void finish() {
    Collections.sort(this);
  }
}
