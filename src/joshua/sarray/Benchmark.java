package joshua.sarray;
import java.io.IOException;
import java.util.Date;

import joshua.util.sentence.Vocabulary;


public class Benchmark {


	public static void main(String[] args) throws IOException {

		
		System.err.println(new Date() + " Constructing source language vocabulary.");
		String sourceFileName = (args.length==0) ? "data/europarl001.en" : args[0];
		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		System.err.println(new Date() + " Constructing source language corpus array.");
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		System.err.println(new Date() + " Constructing source language suffix array.");
		//SuffixArray sourceSuffixArray = 
			SuffixArrayFactory.createSuffixArray(sourceCorpusArray);
		System.err.println(new Date() + " Done");
	}

}
