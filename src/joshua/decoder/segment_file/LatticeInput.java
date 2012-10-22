package joshua.decoder.segment_file;

import joshua.lattice.Lattice;

/**
 * This class represents lattice input. The lattice is contained on a single line and is represented
 * in PLF (Python Lattice Format), e.g.,
 * 
 * ((('ein',0.1,1),('dieses',0.2,1),('haus',0.4,2),),(('haus',0.8,1),),)
 * 
 * @author Matt Post <post@cs.jhu.edu>
 */

public class LatticeInput extends Sentence {

  public LatticeInput(String input, int id) {
    super(input, id);
  }

  public Lattice<Integer> intLattice() {
    return Lattice.createIntLatticeFromString(source());
  }

  public Lattice<String> stringLattice() {
    return Lattice.createStringLatticeFromString(source());
  }
}
