package joshua.decoder.phrase;

import java.util.ArrayList;
import java.util.Collections;

public class Vertex extends ArrayList<HypoState> {

  public Vertex() {
    super();
  }

  public void finish() {
    Collections.sort(this);
  }
}
