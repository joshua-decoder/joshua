package joshua.decoder;

import joshua.decoder.ff.lm.srilm.SWIGTYPE_p_Ngram;
import joshua.decoder.ff.lm.srilm.srilm;

public class SrilmSymbol extends DefaultSymbol {
	private SWIGTYPE_p_Ngram p_srilm=null;
	
	/*it is somewhat strange */
	public SrilmSymbol(String fname, int lm_order){	
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
	 public int addTerminalSymbol(String str){	
		return (int)srilm.getIndexForWord(str);
	 }

	 
	 protected  String  getTerminalWord(int id){
		 String res = (String) srilm.getWordForIndex(id);
		 if(res == null){
				System.out.println("try to query the string for non exist id, must exit");
				System.exit(0);
			}
		
		 return  res;
	 }
	 
}
