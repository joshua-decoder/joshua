package joshua.decoder;

import joshua.decoder.ff.lm.srilm.srilm;

public class SrilmSymbol extends DefaultSymbol {
 	 
	public SrilmSymbol(String fname){
		super(fname);
	}
	
	 /* This will automatically add str into srilm table if it is not there
	  * */
	 public int addTerminalSymbol(String str){	
		return (int)srilm.getIndexForWord(str);
	 }

	 
	 protected String  getTerminalWord(int id){
		 String res = (String) srilm.getWordForIndex(id);
		 if(res == null){
				System.out.println("try to query the string for non exist id, must exit");
				System.exit(0);
			}
		
		 return  res;
	 }
	 
}
