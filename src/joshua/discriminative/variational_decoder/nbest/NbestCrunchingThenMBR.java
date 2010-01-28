package joshua.discriminative.variational_decoder.nbest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;


import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.NbestMinRiskReranker;
import joshua.decoder.hypergraph.DiskHyperGraph;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.discriminative.FileUtilityOld;


public class NbestCrunchingThenMBR {
	
	public static void main(String[] args) throws InterruptedException, IOException {
		if(args.length!=6){
			System.out.println("Wrong number of parameters, it must be  5");
			System.exit(1);
		}		
		//long start_time = System.currentTimeMillis();		
		String testItemsFile=args[0].trim();
		String testRulesFile=args[1].trim();
		int numSents=new Integer(args[2].trim());
		String onebestFile=args[3].trim();//output
		int topN = new Integer(args[4].trim());
		double scalingFactor = new Double(args[5].trim()); 
	
		int ngramStateID = 0;
		SymbolTable symbolTbl = new BuildinSymbol(null);;
		
		NbestCrunching cruncher = new NbestCrunching(symbolTbl, scalingFactor, topN);
		NbestMinRiskReranker mbrReranker = new NbestMinRiskReranker(false, 1.0);
	
		BufferedWriter onebestWriter =	FileUtilityOld.getWriteFileStream(onebestFile);	
		
		System.out.println("############Process file  " + testItemsFile);
		DiskHyperGraph diskHG = new DiskHyperGraph(symbolTbl, ngramStateID, true, null); //have model costs stored
		diskHG.initRead(testItemsFile, testRulesFile,null);			
		for(int sentID=0; sentID < numSents; sentID ++){
			System.out.println("#Process sentence " + sentID);
			HyperGraph testHG = diskHG.readHyperGraph();
			
			List<String> nbest = cruncher.processOneSent(testHG, sentID, true);//produce the disorder nbest
			
			String bestHyp = mbrReranker.processOneSent(nbest, sentID);//nbest: list of unique strings
			
			FileUtilityOld.writeLzf(onebestWriter, bestHyp + "\n");
		}
		FileUtilityOld.closeWriteFile(onebestWriter);				
	}

}
