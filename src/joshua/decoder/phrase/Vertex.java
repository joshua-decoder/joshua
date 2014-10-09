package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Collections;

public class Vertex extends ArrayList<HypoState> {

  private static final long serialVersionUID = 1L;

  public Vertex() {
    super();
  }

  public void finish() {
    Collections.sort(this);
  }
}
