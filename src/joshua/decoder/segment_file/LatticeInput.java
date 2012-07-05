/*
 * This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package joshua.decoder.segment_file;

import joshua.lattice.Lattice;

/**
 * This class represents lattice input. The lattice is contained on a single line and is reprented
 * in PLF (Python Lattice Format), e.g.,
 * 
 * ((('ein',0.1,1),('dieses',0.2,1),('haus',0.4,2),),(('haus',0.8,1),),)
 * 
 * @author Matt Post <post@cs.jhu.edu>
 * @version $LastChangedDate$
 */

public class LatticeInput extends Sentence {

  public LatticeInput(String input, int id) {
    super(input, id);
  }

  public Lattice<Integer> intLattice() {
    return Lattice.createIntLatticeFromString(sentence());
  }

  public Lattice<String> stringLattice() {
    return Lattice.createStringLatticeFromString(sentence());
  }
}
