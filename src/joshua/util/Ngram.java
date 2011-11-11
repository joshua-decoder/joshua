package joshua.util;

import java.util.List;
import java.util.Map;

import joshua.corpus.Vocabulary;

public class Ngram {

	public static void getNgrams(Map<String, Integer> tbl,  int startOrder, int endOrder,  final int[] wrds){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=startOrder-1; j<endOrder  && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds[k];
					ngram.append(Vocabulary.word(t_wrd));
					if(k<i+j) 
						ngram.append(" ");
				}
				String ngramStr = ngram.toString();
				increaseCount(tbl, ngramStr, 1);
			}
	}
	
	
	/**if symbolTbl!=null, then convert interger to String */
	public static void getNgrams(Map<String, Integer> tbl, int startOrder, int endOrder, final List<Integer> wrds){
		
		for(int i=0; i<wrds.size(); i++)
			for(int j=startOrder-1; j<endOrder && j+i<wrds.size(); j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds.get(k);
					ngram.append(Vocabulary.word(t_wrd));
					if(k<i+j) 
						ngram.append(" ");
				}
				String ngramStr = ngram.toString();
				increaseCount(tbl, ngramStr, 1);
			}
	}
	
	/**if symbolTbl!=null, then convert string to integer */
	public static void getNgrams(Map<String, Integer> tbl, int startOrder, int endOrder, final String[] wrds){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=startOrder-1; j<endOrder && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					String t_wrd = wrds[k];
					ngram.append(Vocabulary.id(t_wrd));
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
