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

import joshua.decoder.ff.lm.srilm.SWIGTYPE_p_Ngram;
import joshua.decoder.ff.lm.srilm.UnknownSrilmSymbolException;
import joshua.decoder.ff.lm.srilm.srilm;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.logging.Logger;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/

public class SrilmSymbol extends DefaultSymbol {
	private final SWIGTYPE_p_Ngram p_srilm;
	
	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(SrilmSymbol.class.getName());
	

	/**
	 * Construct an empty SRILM symbol table.
	 * 
	 * @param lm_order Language model n-gram order
	 */
	public SrilmSymbol(int lm_order) {
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lm_order, lm_start_sym_id, lm_end_sym_id );
		logger.info("Construct the symbol table on the fly");
	}
	
	
	/**
	 * Construct an SRILM symbol table using the provided file.
	 * 
	 * @param fname File name
	 * @param lm_order Language model n-gram order
	 * @throws IOException
	 */
	public SrilmSymbol(String fname, int lm_order) throws IOException {
		
		// We have to call the following two functions before we add any symbol into the SRILM table
		// This is unfortunate as we need to provide lm_order, which seems unrelated
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lm_order, lm_start_sym_id, lm_end_sym_id );
		
		//now we can begin to add symbols
		if(fname !=null){
			logger.info("Construct the symbol table from a file " +fname);
			initializeSymTblFromFile(fname);
		}else{
			logger.info("Construct the symbol table on the fly");
		}
	}
	
	/**
	 * Construct an SRILM symbol table using 
	 * the symbol mapping from the provided symbol table.
	 * 
	 * @param vocab Existing symbol table
	 * @param lm_order Language model n-gram order
	 */
	public SrilmSymbol(SymbolTable vocab, int lm_order) {
		
		int vocabLow = vocab.getLowestID();
		int vocabHigh = vocab.getHighestID();
		
		int start = vocabLow - 1;
		int end = lm_end_sym_id - lm_start_sym_id;
		
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lm_order, start, end);
		
		// Add all symbols from the supplied symbol table, in order
		for (int i=vocabLow; i<=vocabHigh; i++) {
			
			String symbol = vocab.getWord(i);
			
			if (symbol != null) {
				if (vocab.isNonterminal(i)) {
					this.addNonterminal(symbol);
				} else {
					this.addTerminal(symbol);
				}
			}
			
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
			 throw new UnknownSrilmSymbolException(id);			
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

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		//TODO Implement this method
		throw new RuntimeException("Method not yet implemented");
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		//TODO Implement this method
		throw new RuntimeException("Method not yet implemented");
	}
	 
}
