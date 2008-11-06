package edu.jhu.sa.util.suffix_array;
import java.io.IOException;
import java.util.Date;

import joshua.util.sentence.Vocabulary;


public class Benchmark {


	public static void main(String[] args) throws IOException {

		int cachePrecomputationFrequencyThreshold = 1000;
		
		System.err.println(new Date() + " Constructing source language vocabulary.");
		String sourceFileName = "data/europarl001.en";
		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		System.err.println(new Date() + " Constructing source language corpus array.");
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		System.err.println(new Date() + " Constructing source language suffix array.");
		//SuffixArray sourceSuffixArray = 
			SuffixArrayFactory.createSuffixArray(sourceCorpusArray, cachePrecomputationFrequencyThreshold);
		System.err.println(new Date() + " Done");
	}

}
