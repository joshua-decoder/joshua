package joshua.util;

import java.util.HashMap;
import java.util.List;

public class Ngram {

	//TODO: consider merge with Discriminative merge
	
	public static void getNgrams(HashMap<String, Integer> tbl, int order, int[] wrds){
		
		for(int i=0; i<wrds.length; i++)
			for(int j=0; j<order && j+i<wrds.length; j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					
					ngram.append(wrds[k]);
					if(k<i+j) 
						ngram.append(" ");
				}
			
				String ngramStr = ngram.toString();
				if(tbl.containsKey(ngramStr))
					tbl.put(ngramStr,  tbl.get(ngramStr)+1);
				else
					tbl.put(ngramStr, 1);
			}
	}
	
//	accumulate ngram counts into tbl
	public static void getNgrams(HashMap<String, Integer>  tbl, int order, List<Integer> wrds){
		
		for(int i=0; i<wrds.size(); i++)
			for(int j=0; j<order && j+i<wrds.size(); j++){//ngram: [i,i+j]
				StringBuffer ngram = new StringBuffer();
				for(int k=i; k<=i+j; k++){
					int t_wrd = wrds.get(k);
					ngram.append(t_wrd);
					if(k<i+j) 
						ngram.append(" ");
				}
				
				String ngramStr = ngram.toString();
				if(tbl.containsKey(ngramStr))
					tbl.put(ngramStr, tbl.get(ngramStr)+1);
				else
					tbl.put(ngramStr, 1);
			}
	}
	
//	accumulate ngram counts into tbl
	public static void getHighestOrderNgrams(HashMap<String, Integer>  tbl, int order, List<Integer> wrds){
		if(wrds.size()<order)
			return;
		
		for(int i=0; i<=wrds.size()-order; i++){
			int j = order-1;
			
			//== one ngram [i,i+j]
			StringBuffer ngram = new StringBuffer();
			for(int k=i; k<=i+j; k++){
				int t_wrd = wrds.get(k);
				ngram.append(t_wrd);
				if(k<i+j) 
					ngram.append(" ");
			}
			
			String ngramStr = ngram.toString();
			if(tbl.containsKey(ngramStr))
				tbl.put(ngramStr, tbl.get(ngramStr)+1);
			else
				tbl.put(ngramStr, 1);
				
		}
	}
	
	public static void incrementOneNgram(List<Integer> words, HashMap<String, Integer> tbl, int count){
		
		StringBuffer ngram = new StringBuffer();
		for(int i=0; i<words.size(); i++){
			int word = words.get(i);
			ngram.append(word);
			if(i<words.size()-1) 
				ngram.append(" ");
		}
		
		String ngramStr = ngram.toString();
		if(tbl.containsKey(ngramStr))
			tbl.put(ngramStr, tbl.get(ngramStr)+count);
		else
			tbl.put(ngramStr, count);
	}
	
	
}
