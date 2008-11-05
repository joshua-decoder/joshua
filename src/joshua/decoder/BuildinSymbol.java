package joshua.decoder;

import java.util.HashMap;


public class BuildinSymbol extends DefaultSymbol {
	private HashMap<String,Integer> str_2_num_tbl = new HashMap<String,Integer>();
	private HashMap<Integer,String> num_2_str_tbl = new HashMap<Integer,String>();
	
	private int cur_terminal_id = lm_start_sym_id ;//must be positive
	
	public BuildinSymbol(String fname){
		if(fname !=null){
			System.out.println("Construct the symbol table from a file " +fname);
			initializeSymTblFromFile(fname);
		}else{
			System.out.println("Construct the symbol table on the fly");
		}
	}

	/** Get int for string (initial, or recover) */
	public int addTerminalSymbol(String str){
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


	protected String getTerminalWord(int id){
		 String res = (String)num_2_str_tbl.get(id);
		 if(res == null){
				System.out.println("try to query the string for non exist id, must exit, id is " + id);
				System.exit(0);
			}
		
		 return  res;
	 }
	
	
}
