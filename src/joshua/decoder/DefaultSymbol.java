package joshua.decoder;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;

import joshua.util.FileUtility;

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
 * @version $LastChangedDate: 2008-10-31 08:56:14 -0400 (Fri, 31 Oct 2008) $
 */





/*############# How to initialize the Symbol
 * Having multiple LM modes complicate the class, we have four LM mode: JAVA_LM, SRILM, Distributed_LM, and NONE_LM. The NONE_LM and JAVA_LM will be treated as same. 
 *JAVA_LM and NONE_LM: call add_global_symbols(true) to initialize
 *SRILM: the SRILM must first be initialized, then call add_global_symbols(false)
 *DistributedLM (from decoder): call init_sym_tbl_from_file(true)
 *DistributedLM (from LMServer): call init_sym_tbl_from_file(true/false)
 * */


public abstract class DefaultSymbol implements Symbol {
	//terminal symbol may get from a tbl file, srilm, or a lm file
	//**non-terminal symbol is always from myself, and the integer should always be negative	
	private HashMap<String,Integer> nonterminal_str_2_num_tbl = new HashMap<String,Integer>();
	private HashMap<Integer,String> nonterminal_num_2_str_tbl = new HashMap<Integer,String>();
	private  int nonterminal_cur_id=-1;//start from -1

	protected  int lm_start_sym_id = 10000;//1-10000 reserved for special purpose
	protected  int lm_end_sym_id = 5000001;//max vocab 1000k
	
	public boolean is_reading_from_file = false;
	 
	protected abstract String  getTerminalWord(int id);

	
	public DefaultSymbol(){
		//do nothing here, because we want the sub-class doing specific things
	}
	
	
	final public  String getWord(int id) {
		if ( isNonterminal(id)) {
			return getNonTerminalWord(id);
		}else{
			return getTerminalWord(id);
		}
	}
	
	final public int getLMStartID(){
		return lm_start_sym_id;
	}
	
	final public int getLMEndID(){
		return lm_end_sym_id;
	}
	
	final public String  getNonTerminalWord(int id){
		String res =  (String)nonterminal_num_2_str_tbl.get(id);
		if(res == null){
			System.out.println("try to query the string for non exist id, must exit");
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
	
	
	final public int[] addTerminalSymbols(String sentence){
		String[] sent_wrds = sentence.split("\\s+");		
		return addTerminalSymbols(sent_wrds);
	}	
	
	
    final public int[] addTerminalSymbols(String[] strings){
		int[] res =new int[strings.length];
		for(int t=0; t<strings.length; t++)
			res[t]=addTerminalSymbol(strings[t]);
		return res;
	}	
	
	
	
//	####### following functions used for TM only
	final public int addNonTerminalSymbol(String str){
		Integer res_id = (Integer)nonterminal_str_2_num_tbl.get(str);
		if (null != res_id) { // already have this symbol
			if (! isNonterminal(res_id)) {
				System.out.println("Error, NONTSym: " + str + "; id: " + res_id);
				System.exit(1);
			}
			return res_id;
		} else {
			nonterminal_str_2_num_tbl.put(str, nonterminal_cur_id);
			nonterminal_num_2_str_tbl.put(nonterminal_cur_id, str);
			nonterminal_cur_id--;
			//System.out.println("Sym: " + str + "; id: " + negative_id);
			return (nonterminal_cur_id+1);
		}
	}
	
	
	final public boolean isNonterminal(int id) {
		return (id < 0);
	}
	
	
	final public int getEngNonTerminalIndex(int id) {
		if (! isNonterminal(id)) {
			return -1;
		} else {
			// TODO: get rid of this expensive interim object
			String symbol = getWord(id);
			
			return getEngNonTerminalIndex(symbol);
		}
	}
	
	final public int getEngNonTerminalIndex(String wrd) {
		// Assumes the last character is a digit
		// and extracts it, starting from one.
		// Assumes the whole prefix is the
		// nonterminal-ID portion of the string
		return Integer.parseInt( wrd.substring(wrd.length() - 2,	wrd.length() - 1) ) - 1;
	}
	
	protected void initializeSymTblFromFile(String fname){	
		is_reading_from_file =true;
		//### read file into tbls
		HashMap<String, Integer> tbl_str_2_id = new HashMap<String, Integer>();
		HashMap<Integer, String> tbl_id_2_str = new HashMap<Integer, String>();
		BufferedReader t_reader_sym = FileUtility.getReadFileStream(fname);
		String line;		
		while((line=FileUtility.read_line_lzf(t_reader_sym))!=null){
			String[] fds = line.split("\\s+");
			if(fds.length!=2){
			    System.out.println("Warning: read index, bad line: " + line);
			    continue;
			}
			String str = fds[0].trim();
			int id = new Integer(fds[1]);

			String uqniue_str;
			if (null != tbl_str_2_id.get(str)) { // it is quite possible that java will treat two stings as the same when other language (e.g., C or perl) treat them differently, due to unprintable symbols
				 System.out.println("Warning: duplicate string (add fake): " + line);
				 uqniue_str = str + id;//fake string
				 //System.exit(1);//TODO
			} else {
				uqniue_str = str;
			}
			tbl_str_2_id.put(uqniue_str,id);
			
			//it is guranteed that the strings in tbl_id_2_str are different
			if (null != tbl_id_2_str.get(id)) {
				 System.out.println("Error: duplicate id, have to exit; " + line);
				 System.exit(1);
			} else {
				tbl_id_2_str.put(id, uqniue_str);
			}
		}
		FileUtility.close_read_file(t_reader_sym);
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
				System.out.println("Warning: add fake symbol, be alert");
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
                        res_id = addTerminalSymbol(str);
                        n_added++;
                }else{//non-continous index
                        System.out.println("Warning: add fake symbol, be alert");
                        res_id = addTerminalSymbol("lzf"+i);
                }
                if(res_id!=i){
                        System.out.println("id supposed: " + i +" != assinged " + res_id + " symbol:" + str);
                        System.exit(0);
                }
                if(n_added>=tbl_id_2_str.size())
                        break;
        }

		
	}

}

