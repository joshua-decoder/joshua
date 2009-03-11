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

import joshua.decoder.ff.lm.srilm.SWIGTYPE_p_Ngram;
import joshua.decoder.ff.lm.srilm.srilm;

import java.io.IOException;
import java.util.Collection;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate: 2009-03-09 12:52:29 -0400 (星期一, 09 三月 2009) $
*/

public class SrilmSymbol extends DefaultSymbol {
	private SWIGTYPE_p_Ngram p_srilm=null;
	
	/*it is somewhat strange */
	public SrilmSymbol(String fname, int lm_order) throws IOException {
		/*we have to call the following two funcitons before we add any symbol into the SRILM table
		 * This is unfortunate as we need to provide lm_order, which seems unrelated*/
		
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lm_order, lm_start_sym_id, lm_end_sym_id );
		
		//now we can begin to add symbols
		if(fname !=null){
			System.out.println("Construct the symbol table from a file " +fname);
			initializeSymTblFromFile(fname);
		}else{
			System.out.println("Construct the symbol table on the fly");
		}
	}
	
	public SWIGTYPE_p_Ngram getSrilmPointer(){
		return this.p_srilm;
	}
	
	 /* This will automatically add str into srilm table if it is not there
	  * */
	 public int addTerminal(String str){	
		return (int)srilm.getIndexForWord(str);
	 }

	 
	 public  String  getTerminal(int id){
		 String res = (String) srilm.getWordForIndex(id);
		 if(res == null){
				System.out.println("try to query the string for non exist id, must exit");
				System.exit(0);
			}
		
		 return  res;
	 }

	public Collection<Integer> getAllIDs() {
		//TODO Implement this method
		throw new RuntimeException("Method not yet implemented");
	}

	public int getID(String wordString) {
		return addTerminal(wordString);
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
