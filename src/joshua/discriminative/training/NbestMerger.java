package joshua.discriminative.training;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import joshua.discriminative.bleu_approximater.NbestReader;
import joshua.util.FileUtility;
import joshua.util.Regex;

public class NbestMerger {
	
	public static int mergeNbest(String nbestFile1, String nbestFile2, String nbestOutFile){
		int totalNumHyp = 0;
		try {
			NbestReader nbestReader1 = new NbestReader(nbestFile1);
			NbestReader nbestReader2 = new NbestReader(nbestFile2);
			BufferedWriter outWriter = FileUtility.getWriteFileStream(nbestOutFile);
			
			while(nbestReader1.hasNext()){
				List<String> nbest1 = nbestReader1.next();
				List<String> nbest2 = nbestReader2.next();
		 
				List<String> newNbest = processOneSentence(nbest1, nbest2);
				for(String hyp : newNbest){
					outWriter.write(hyp+"\n");
				}
				totalNumHyp += newNbest.size();
			}
			outWriter.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("totalNumHyp="+totalNumHyp);
		return totalNumHyp;
	}
	
	private static List<String> processOneSentence(List<String> nbest1, List<String> nbest2){
		
		List<String> newNbest = new ArrayList<String>();
		Set<String> uniqueNbests = new HashSet<String>();
		processOneNbest(nbest1, uniqueNbests, newNbest);
		processOneNbest(nbest2, uniqueNbests, newNbest);
		return newNbest;
	}
	
	private static void processOneNbest(List<String> nbest, Set<String> uniqueNbests, List<String> newNbest){
		for(String line : nbest){
			String[] fds = Regex.threeBarsWithSpace.split(line);
			String hypItself = fds[1];
			
			if(uniqueNbests.contains(hypItself)){
				//skip
			}else{
				uniqueNbests.add(hypItself);
				newNbest.add(line);
			}
		}
	}
	
}
