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

import joshua.corpus.AbstractSymbolTable;
import joshua.corpus.SymbolTable;
import joshua.util.io.LineReader;
import joshua.util.Regex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

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





/*############# How to initialize the Symbol
 * Having multiple LM modes complicate the class, we have four LM mode: JAVA_LM, SRILM, Distributed_LM, and NONE_LM. The NONE_LM and JAVA_LM will be treated as same. 
 *JAVA_LM and NONE_LM: call add_global_symbols(true) to initialize
 *SRILM: the SRILM must first be initialized, then call add_global_symbols(false)
 *DistributedLM (from decoder): call init_sym_tbl_from_file(true)
 *DistributedLM (from LMServer): call init_sym_tbl_from_file(true/false)
 * */


public abstract class DefaultSymbol extends AbstractSymbolTable implements SymbolTable { // implements Symbol {
	
	private static final Logger logger = Logger.getLogger(DefaultSymbol.class.getName());
	
	//terminal symbol may get from a tbl file, srilm, or a lm file
	//**non-terminal symbol is always from myself, and the integer should always be negative	
	private HashMap<String,Integer> nonterminal_str_2_num_tbl = new HashMap<String,Integer>();
	private HashMap<Integer,String> nonterminal_num_2_str_tbl = new HashMap<Integer,String>();
	private  int nonterminal_cur_id=-1;//start from -1

	protected  int lm_start_sym_id = 10000;//1-10000 reserved for special purpose
	protected  int lm_end_sym_id = 5000001;//max vocab 1000k
	
	public boolean is_reading_from_file = false;
	 
	//protected abstract String getTerminalWord(int id);

	
	public DefaultSymbol(){
		//do nothing here, because we want the sub-class doing specific things
	}
	
	
	final public  String getWord(int id) {
		if ( isNonterminal(id)) {
			return getNonterminal(id);
		}else{
			return getTerminal(id);
		}
	}
	
	final public int getLowestID(){
		return lm_start_sym_id;
	}
	
	final public int getHighestID(){
		return lm_end_sym_id;
	}
	
	final public String  getNonterminal(int id){
		String res =  (String)nonterminal_num_2_str_tbl.get(id);
		if(res == null){
			logger.severe("try to query the string for non exist id, must exit");
			System.exit(0);
		}
		return res;
	}
	
    final public String getWords(Integer[] ids){
		String res = "";
		for(int t=0; t<ids.length; t++){
			if(t==0)
				res += getWord(ids[t]);
			else
				res += " " + getWord(ids[t]);
		}
		return res;
	}
	
    final public String getWords(int[] ids){
		String res = "";
		for(int t=0; t<ids.length; t++){
			if(t==0)
				res += getWord(ids[t]);
			else
				res += " " + getWord(ids[t]);
		}
		return res;
	}
	
	 final public String getWords(List<Integer> ids){
		String res = "";
		for(int t=0; t<ids.size(); t++){
			if(t==0)
				res += getWord(ids.get(t));
			else
				res += " " + getWord(ids.get(t));
		}
		return res;
	}
	
	
		
//	####### following functions used for TM only
	final public int addNonterminal(String str){
		Integer res_id = (Integer)nonterminal_str_2_num_tbl.get(str);
		if (null != res_id) { // already have this symbol
			if (! isNonterminal(res_id)) {
				logger.severe("Error, NONTSym: " + str + "; id: " + res_id);
				System.exit(1);
			}
			return res_id;
		} else {
			nonterminal_str_2_num_tbl.put(str, nonterminal_cur_id);
			nonterminal_num_2_str_tbl.put(nonterminal_cur_id, str);
			nonterminal_cur_id--;
			return (nonterminal_cur_id+1);
		}
	}
	
	
	final public boolean isNonterminal(int id) {
		return (id < 0);
	}
	
	protected void initializeSymTblFromFile(String fname)
	throws IOException {
		is_reading_from_file =true;
		//### read file into tbls
		HashMap<String, Integer> tbl_str_2_id = new HashMap<String, Integer>();
		HashMap<Integer, String> tbl_id_2_str = new HashMap<Integer, String>();
		
		LineReader symboltableReader = new LineReader(fname);
		try { for (String line : symboltableReader) {
			String[] fds = Regex.spaces.split(line);
			if(fds.length!=2){
			    logger.warning("read index, bad line: " + line);
			    continue;
			}
			String str = fds[0].trim();
			int id = Integer.parseInt(fds[1]);

			String uqniue_str;
			if (null != tbl_str_2_id.get(str)) { // it is quite possible that java will treat two stings as the same when other language (e.g., C or perl) treat them differently, due to unprintable symbols
				 logger.warning("duplicate string (add fake): " + line);
				 uqniue_str = str + id;//fake string
				 //System.exit(1);//TODO
			} else {
				uqniue_str = str;
			}
			tbl_str_2_id.put(uqniue_str,id);
			
			//it is guranteed that the strings in tbl_id_2_str are different
			if (null != tbl_id_2_str.get(id)) {
				 logger.severe("Error: duplicate id, have to exit; " + line);
				 System.exit(1);
			} else {
				tbl_id_2_str.put(id, uqniue_str);
			}
		} } finally { symboltableReader.close(); }
		
		/*if (tbl_id_2_str.size() >= lm_end_sym_id - lm_start_sym_id) {
			System.out.println("Error: read symbol tbl, tlb is too big");
			System.exit(1);
		}*/
		
		//#### now add the tbl into srilm/java-tbl
		/*int n_added = 0;
		int i=0;
		while (n_added < tbl_id_2_str.size()) {
			String str = (String) tbl_id_2_str.get(i); // it is guaranteed that the strings in tbl_id_2_str are different
			int res_id;
			if (null != str) {
				res_id = addTerminalSymbol(str);
				n_added++;
			} else { // non-continuous index
				logger.warning("add fake symbol, be alert");
				res_id = addTerminalSymbol("lzf"+i);
			}	
			if (res_id != i) {
				System.out.println("id supposed: " + i +" != assinged " + res_id + " symbol:" + str);
				System.exit(1);
			}		
			i++;
		}*/
		int n_added=0;
        for(int i=lm_start_sym_id; i<lm_end_sym_id; i++){
                String str = (String) tbl_id_2_str.get(i);//it is guranteed that the strings in tbl_id_2_str are different
                int res_id;
                if(str!=null){
                        res_id = addTerminal(str);
                        n_added++;
                }else{//non-continous index
                        logger.warning("add fake symbol, be alert");
                        res_id = addTerminal("lzf"+i);
                }
                if(res_id!=i){
                        logger.severe("id supposed: " + i +" != assinged " + res_id + " symbol:" + str);
                        System.exit(0);
                }
                if(n_added>=tbl_id_2_str.size())
                        break;
        }

		
	}
	



	public int[] getIDs(String sentence) {
		return addTerminals(sentence);
	}

	public String getTerminals(int[] wordIDs) {
		return getWords(wordIDs);
	}

	public List<String> getWords() {
		return new ArrayList<String>(nonterminal_num_2_str_tbl.values());
	}

	public int size() {
		return nonterminal_num_2_str_tbl.size();
	}

	
}

