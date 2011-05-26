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

import joshua.decoder.ff.lm.kenlm.jni.VocabCallback;

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

public class KenSymbol extends DefaultSymbol implements VocabCallback {
	
	private static final Logger logger = Logger.getLogger(KenSymbol.class.getName());
	
	private HashMap<String,Integer> strToIntTbl = new HashMap<String,Integer>();
	private HashMap<Integer,String> intToStrTbl = new HashMap<Integer,String>();
	
	private int curTerminalID = lmStartSymID ;//must be positive
	
	public KenSymbol() {}
	
	public int addTerminal(String terminal) {
		return getID(terminal);
	}
	
	/** Get int for string (initial, or recover) */
	public int getID(String str) {
		Integer resID = strToIntTbl.get(str);
		if (null != resID) { // already have this symbol
			if (isNonterminal(resID)) {
				throw new RuntimeException("terminal symbol mix with non-terminal, Sym: " + str + "; id: " + resID);
			}
			return resID;
		} else {
			strToIntTbl.put(str, curTerminalID);
			intToStrTbl.put(curTerminalID, str);
			curTerminalID++;
			if (curTerminalID > lmEndSymID) {
				throw new RuntimeException("curTerminalID is greater than lmEndSymID");
			}
			//logger.info("Sym: " + str + "; id: " + positive_id);
			return (curTerminalID-1);
		}
	}
	
	
	public String getTerminal(int id) {
		String res = intToStrTbl.get(id);
		if (res == null) {
			//throw new RuntimeException("try to query the string for non exist id, must exit, id is " + id);
			logger.warning("null string for id="+id);
			//System.exit(1);
		}
		
		return  res;
	}
	
	
	public Collection<Integer> getAllIDs() {
		return intToStrTbl.keySet();
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
    Integer old_id = strToIntTbl.put(str, id);
    String old_word = intToStrTbl.put(id, str);
    if (old_word != null && !old_word.equals(str)) {
      throw new RuntimeException("Conflict for id " + id + " of " + old_word + " and " + str);
    }
    if (old_id != null && !old_id.equals(id)) {
      throw new RuntimeException("Conflict for word " + str + " with ids " + old_id + " and " + id);
    }
    // Kenlm always calls in order
    if (id >= curTerminalID) {
      curTerminalID = id + 1;
			if (curTerminalID > lmEndSymID) {
				throw new RuntimeException("curTerminalID is greater than lmEndSymID");
			}
    }
  }
}
