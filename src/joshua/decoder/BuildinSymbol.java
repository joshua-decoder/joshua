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
package joshua.decoder;

import java.util.Collection;
import java.util.HashMap;
import java.io.IOException;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/

public class BuildinSymbol extends DefaultSymbol {
	private HashMap<String,Integer> str_2_num_tbl = new HashMap<String,Integer>();
	private HashMap<Integer,String> num_2_str_tbl = new HashMap<Integer,String>();
	
	private int cur_terminal_id = lm_start_sym_id ;//must be positive
	
	public BuildinSymbol() throws IOException {
		this(null);
	}
	
	public BuildinSymbol(String fname)  {
		if (null != fname) {
			System.out.println("Construct the symbol table from a file " + fname);
			try {
				initializeSymTblFromFile(fname);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		} else {
			System.out.println("Construct the symbol table on the fly");
		}
	}
	
	public int addTerminal(String terminal) {
		return getID(terminal);
	}
	
	/** Get int for string (initial, or recover) */
	public int getID(String str) {
	//public int addTerminalSymbol(String str){
		Integer res_id = (Integer)str_2_num_tbl.get(str);
		if (null != res_id) { // already have this symbol
			if (isNonterminal(res_id)) {
				System.out.println("Error, terminal symbol mix with non-terminal, Sym: " + str + "; id: " + res_id);
				System.exit(1);
			}
			return res_id;
		} else {
			str_2_num_tbl.put(str, cur_terminal_id);
			num_2_str_tbl.put(cur_terminal_id, str);
			cur_terminal_id++;
			if(cur_terminal_id>lm_end_sym_id){
				System.out.println("cur_terminal_id is greater than lm_end_sym_id");
				System.exit(0);
			}
			//System.out.println("Sym: " + str + "; id: " + positive_id);
			return (cur_terminal_id-1);
		}
	}


	public String getTerminal(int id) {
	//protected String getTerminalWord(int id){
		 String res = (String)num_2_str_tbl.get(id);
		 if(res == null){
				System.out.println("try to query the string for non exist id, must exit, id is " + id);
				System.exit(0);
			}
		
		 return  res;
	 }

	public Collection<Integer> getAllIDs() {
		return num_2_str_tbl.keySet();
	}
	
	public String getUnknownWord() {
		//TODO Implement this method
		throw new RuntimeException("Method not yet implemented");
	}

	public int getUnknownWordID() {
		//TODO Implement this method
		throw new RuntimeException("Method not yet implemented");	
	}
	
}
