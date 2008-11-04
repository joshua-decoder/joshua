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

import java.util.List;

/**
 * this class implement 
 * (1) initialize the symbol table
 * (2) provide conversion between symbol and integers
 *
 * How to initialize the Symbol
 * Having multiple LM modes complicate the class, we have four LM mode: JAVA_LM, SRILM, Distributed_LM, and NONE_LM. The NONE_LM and JAVA_LM will be treated as same. 
 *JAVA_LM and NONE_LM: call add_global_symbols(true) to initialize
 *SRILM: the SRILM must first be initialized, then call add_global_symbols(false)
 *DistributedLM (from decoder): call init_sym_tbl_from_file(true)
 *DistributedLM (from LMServer): call init_sym_tbl_from_file(true/false)
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

public interface Symbol {
	public int addTerminalSymbol(String wordString);
	public int[] addTerminalSymbols(String sentence);
	public int[] addTerminalSymbols(String[] words);

	public int getLMEndID();
	public int getLMStartID();
	
	public int addNonTerminalSymbol(String wordString);

	public boolean isNonterminal(int id);
	
	public int getEngNonTerminalIndex(int id);//return the index of a nonterminal, e.g., input X1 will return 1; input X0 will return 0 
	public int getEngNonTerminalIndex(String word);
	
	/**
	 * @return the ID for wordString
	 */
	//public int getID(String wordString);//can be terminal or non-terminal
	 
	//public int[] getIDs(String sentence);//can be mix of nonterminal and terminal
	

	/**
	 * @return the String for a word ID
	 */
	public String getWord(int wordID);//can be terminal or non-terminal
	
	public String getWords(int[] wordIDs);//can be mix of terminal and non-terminal
	
	public String getWords(Integer[] wordIDs);//can be mix of terminal and non-terminal

	public String getWords(List<Integer> wordIDs);//can be mix of terminal and non-terminal
}
