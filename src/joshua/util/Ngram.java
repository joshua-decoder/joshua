package joshua.util;

import java.util.List;
import java.util.Map;

import joshua.corpus.vocab.SymbolTable;

public class Ngram {

	

	public static void getNgrams(Map<String, Integer> tbl, int startOrder, int endOrder, final List<Integer> wrds){
		getNgrams(null, tbl, startOrder, endOrder, wrds);
	}

	public static void getNgrams(Map<String, Integer> tbl,  int startOrder, int endOrder,  final int[] wrds){
		getNgrams(null, tbl, startOrder, endOrder, wrds);
	}

	public static void getNgrams(Map<String, Integer> tbl,  int startOrder, int endOrder, final  String[] wrds){
		getNgrams(null, tbl, startOrder, endOrder, wrds);
	}
	
	/**if symbolTbl!=null, then convert interger to String */
	public static void getNgrams(SymbolTable symbolTbl, Map<String, Integer> tbl,  int startOrder, int endOrder,  final int[] wrds){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=startOrder-1; j<endOrder  && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds[k];
					if(symbolTbl!=null)
						ngram.append(symbolTbl.getWord(t_wrd));
					else
						ngram.append(t_wrd);
					if(k<i+j) 
						ngram.append(" ");
				}
				String ngramStr = ngram.toString();
				increaseCount(tbl, ngramStr, 1);
			}
	}
	
	
	/**if symbolTbl!=null, then convert interger to String */
	public static void getNgrams(SymbolTable symbolTbl, Map<String, Integer> tbl, int startOrder, int endOrder, final List<Integer> wrds){
		
		for(int i=0; i<wrds.size(); i++)
			for(int j=startOrder-1; j<endOrder && j+i<wrds.size(); j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds.get(k);
					if(symbolTbl!=null)
						ngram.append(symbolTbl.getWord(t_wrd));
					else
						ngram.append(t_wrd);
					if(k<i+j) 
						ngram.append(" ");
				}
				String ngramStr = ngram.toString();
				increaseCount(tbl, ngramStr, 1);
			}
	}
	
	/**if symbolTbl!=null, then convert string to integer */
	public static void getNgrams(SymbolTable symbolTbl, Map<String, Integer> tbl, int startOrder, int endOrder, final String[] wrds){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=startOrder-1; j<endOrder && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					String t_wrd = wrds[k];
					if(symbolTbl!=null)
						ngram.append(symbolTbl.getID(t_wrd));
					else
						ngram.append(t_wrd);
					if(k<i+j) 
						ngram.append(" ");
				}
				String ngramStr = ngram.toString();
				increaseCount(tbl, ngramStr, 1);
			}
	}
	
	static private void increaseCount(Map<String, Integer> tbl, String feat, int increment){
		Integer oldCount = tbl.get(feat);
		if(oldCount!=null)
			tbl.put(feat, oldCount + increment);
		else
			tbl.put(feat, increment);
	}
	

	

	
}
