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
import joshua.decoder.ff.lm.srilm.srilm;
import joshua.decoder.ff.tm.hiero.HieroFormatReader;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author Zhifei Li, <zhifei.work@gmail.com>
* @version $LastChangedDate$
*/

public class SrilmSymbol extends DefaultSymbol {
	private final SWIGTYPE_p_Ngram p_srilm;
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(SrilmSymbol.class.getName());
	

	/**
	 * Construct an empty SRILM symbol table.
	 * 
	 * @param lmOrder Language model n-gram order
	 */
	public SrilmSymbol(int lmOrder) {
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lmOrder, lmStartSymID, lmEndSymID );
		logger.info("Construct the symbol table on the fly");
		addNonterminal(X_STRING);
		addNonterminal(X1_STRING);
		addNonterminal(X2_STRING);
		addNonterminal(S_STRING);
		addNonterminal(S1_STRING);
	}
	
	
	/**
	 * Construct an SRILM symbol table using the provided file.
	 *
	 * @param fname File name
	 * @param lmOrder Language model n-gram order
	 * @throws IOException
	 */
	public SrilmSymbol(String fname, int lmOrder) throws IOException {
		
		// We have to call the following two functions before we add any symbol into the SRILM table
		// This is unfortunate as we need to provide lm_order, which seems unrelated
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lmOrder, lmStartSymID, lmEndSymID );
		
		//now we can begin to add symbols
		if(fname !=null){
			logger.info("Construct the symbol table from a file " +fname);
			initializeSymTblFromFile(fname);
		}else{
			logger.info("Construct the symbol table on the fly");
		}
	}
	
	/**
	 * Construct an SRILM symbol table using the symbol mapping
	 * from the provided symbol table.
	 * 
	 * @param vocab Existing symbol table
	 * @param lmOrder Language model n-gram order
	 */
	public SrilmSymbol(SymbolTable vocab, int lmOrder) {
		
		int vocabLow = vocab.getLowestID();
		int vocabHigh = vocab.getHighestID();
		
		if (logger.isLoggable(Level.FINEST)) logger.finest("In existing symbol table, lowestID=="+vocabLow+ " and highestID=="+vocabHigh);
		
		int start = 1;//(vocabLow>0) ? vocabLow - 1 : -4;
		int end = lmEndSymID - lmStartSymID;
		
		System.loadLibrary("srilm"); //load once		
		this.p_srilm = srilm.initLM(lmOrder, start, end);
		
//		if (logger.isLoggable(Level.FINEST)) {
//			logger.fine(this.getWord(1));
//			logger.fine(this.getWord(2));
//			logger.fine(this.getWord(3));
//			logger.fine(this.getWord(4));
//		}
		
		// Add all symbols from the supplied symbol table, in order
//		for (int i=vocabLow; i<=vocabHigh; i++) {
//			String symbol = vocab.getWord(i);
//			if (vocab.isNonterminal(i)) {
//				int id = this.addNonterminal(symbol);
//				logger.fine("Added symbol " + symbol + " with id " + id + "; original id was " + i + " " + this.getWord(id));
//			} else {
//				int id = this.addTerminal(symbol);
//				logger.fine("Added symbol " + symbol + " with id " + id + "; original id was " + i + " " + this.getWord(id));
//			}
//		}
		
		int lowestNonNegative = (vocabLow < 0) ? 1 : vocabLow;
		for (int i=lowestNonNegative; i<=vocabHigh; i++) {
			
			String symbol = vocab.getWord(i);
			
			if (symbol != null) {
				if (vocab.isNonterminal(i)) {
					int id = this.addNonterminal(symbol);
					logger.fine("Added symbol " + symbol + " with id " + id + "; original id was " + i + " " + this.getWord(id));
					if (id!=i || !symbol.equals(this.getWord(id))) { 
						throw new RuntimeException("Symbol mismatch between " + id + " and " + i + " for nonterminal symbol " + symbol);
					}
				} else {
					int id = this.addTerminal(symbol);
					logger.fine("Added symbol " + symbol + " with id " + id + "; original id was " + i + " " + this.getWord(id));
					if (id!=i || !symbol.equals(this.getWord(id))) { 
						throw new RuntimeException("Symbol mismatch between " + id + " and " + i + " for terminal symbol " + symbol);
					}
				}
			}
			
		}
		
		if (vocabLow < 0) {
			for (int i=-1; i>=vocabLow; i--) {
				String symbol = vocab.getWord(i);
				
				if (symbol != null) {
					if (vocab.isNonterminal(i)) {
						int id = this.addNonterminal(symbol);
						logger.fine("Added symbol " + symbol + " with id " + id + "; original id was " + i + " " + this.getWord(id));
						if (id!=i || !symbol.equals(this.getWord(id))) { 
							throw new RuntimeException("Symbol mismatch between " + id + " and " + i + " for nonterminal symbol " + symbol);
						}
					} else {
						int id = this.addTerminal(symbol);
						logger.fine("Added symbol " + symbol + " with id " + id + "; original id was " + i + " " + this.getWord(id));
						if (id!=i || !symbol.equals(this.getWord(id))) { 
							throw new RuntimeException("Symbol mismatch between " + id + " and " + i + " for terminal symbol " + symbol);
						}
					}
				}
			}
		}
		
		if (logger.isLoggable(Level.FINEST)) {
			for (int i=vocabLow+1; i<0; i++) {
				String symbol = this.getWord(i);
				logger.fine("ID " + i + " => " + symbol);
			}
			for (int i=1; i<=vocabHigh; i++) {
				String symbol = this.getWord(i);
				logger.fine("ID " + i + " => " + symbol);
			}
		}
		
	}
	
	public SWIGTYPE_p_Ngram getSrilmPointer(){
		return this.p_srilm;
	}
	
	 /* This will automatically add str into srilm table if it is not there
	  * */
	 public int addTerminal(String str){
//		 if (HieroFormatReader.isNonTerminal(str)) {
//			 throw new RuntimeException("Attempting to add nonterminal " + str + " as a terminal");
//		 }
		 
		 int id = (int) srilm.getIndexForWord(str); 
		return id;
	 }

	 
	 public  String  getTerminal(int id){
		 String res = (String) srilm.getWordForIndex(id);
		 
		 if(res == null){
			 //throw new UnknownSymbolException(id);
			 logger.warning("null string for id="+id);
		 }
		
		 return  res;
	 }

	 public Collection<Integer> getAllIDs() {
		//TODO Implement this method
		throw new RuntimeException("Method not yet implemented");
	}

	public int getID(String wordString) {
		 if (HieroFormatReader.isNonTerminal(wordString)) {//TODO: this is so wrong
			 return addNonterminal(wordString);
		 } else {
			 return addTerminal(wordString);
		 }
	}

}
