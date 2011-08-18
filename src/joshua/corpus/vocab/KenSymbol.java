/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.corpus.vocab;

import joshua.decoder.ff.lm.kenlm.jni.KenLM;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/

public class KenSymbol extends DefaultSymbol {
		private static final Logger logger = Logger.getLogger(KenSymbol.class.getName());

  private final KenLM backend;
	
	public KenSymbol(KenLM backend_in) {
    backend = backend_in;
  }
	
	public int addTerminal(String terminal) {
		return backend.vocabFindOrAdd(terminal);
	}
	
	/** Get int for string (initial, or recover) */
	public int getID(String str) {
    return backend.vocabFindOrAdd(str);
	}
	
	
	public String getTerminal(int id) {
    return backend.vocabWord(id);
	}
	
	
	public Collection<Integer> getAllIDs() {
		throw new RuntimeException("Method not yet implemented");
	}
	
	public String getUnknownWord() {
    return "<unk>";
	}

	public int getUnknownWordID() {
    return 0;
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		throw new RuntimeException("Method not yet implemented");
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		throw new RuntimeException("Method not yet implemented");
	}

  public void set(int id, String str) {
    backend.vocabAdd(id, str);
  }
}
