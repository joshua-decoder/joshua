package joshua.sarray;

import java.io.IOException;

import joshua.sarray.CorpusArray;
import joshua.sarray.MemoryMappedCorpusArray;
import joshua.sarray.SuffixArrayFactory;
import joshua.util.sentence.Vocabulary;


import org.testng.Assert;
import org.testng.annotations.Test;




public class CorpusArrayTest {

//	@Test
//	public void writePartsToDisk() {
//		
//		String filename = "data/tiny.en";
//		int numSentences = 5;  // Should be 5 sentences in tiny.en
//		int numWords = 89;     // Should be 89 words in tiny.en
//		
//		
//		try {
//			
//			// FIX: can't use createVocabulary(String) because we set numWords and numSentences
//			Vocabulary vocab = new Vocabulary();
//			SuffixArrayFactory.createVocabulary(filename, vocab);
//			CorpusArray corpus = SuffixArrayFactory.createCorpusArray(filename, vocab, numWords, numSentences);
//			
//			corpus.writeWordIDsToFile(filename+".bin");
//			corpus.writeSentenceLengthsToFile(filename+".sbin");
//			
//			MemoryMappedCorpusArray mmCorpus = new MemoryMappedCorpusArray(corpus.getVocabulary(), filename+".bin", numWords*4, filename+".sbin", numSentences*4);
//			
//			// For each word in the corpus,
//			for (int i=0; i<corpus.size(); i++) {
//				
//				// Verify that the memory-mapped corpus and the in-memory corpus have the same value
//				Assert.assertEquals(mmCorpus.getWordID(i), corpus.getWordID(i));
//			}
//			
//			
//			// For each sentence in the corpus
//			for (int i=0; i<corpus.sentences.length; i++) {
//				
//				// Verify that the sentence position in the memory-mapped corpus and the in-memory corpus have the same value
//				Assert.assertEquals(corpus.getSentencePosition(i), mmCorpus.getSentencePosition(i));
//			}
//			
//		} catch (IOException e) {
//			Assert.fail(e.getLocalizedMessage());
//		}
//		
//	}
	
	
	@Test
	public void writeAllToDisk() {
		
		String filename = "data/tiny.en";
		int numSentences = 5;  // Should be 5 sentences in tiny.en
		int numWords = 89;     // Should be 89 words in tiny.en
		
		
		try {
			
			// FIX: can't use createVocabulary(String) because we set numWords and numSentences
			Vocabulary vocab = new Vocabulary();
			SuffixArrayFactory.createVocabulary(filename, vocab);
			CorpusArray corpus = SuffixArrayFactory.createCorpusArray(filename, vocab, numWords, numSentences);
			
			corpus.write(filename+".corpus");
			
			MemoryMappedCorpusArray mmCorpus = new MemoryMappedCorpusArray(corpus.getVocabulary(), filename+".corpus");
			
			// For each word in the corpus,
			for (int i=0; i<corpus.size(); i++) {
				
				// Verify that the memory-mapped corpus and the in-memory corpus have the same value
				Assert.assertEquals(mmCorpus.getWordID(i), corpus.getWordID(i));
			}
			
			
			// For each sentence in the corpus
			for (int i=0; i<corpus.sentences.length; i++) {
				
				// Verify that the sentence position in the memory-mapped corpus and the in-memory corpus have the same value
				Assert.assertEquals(corpus.getSentencePosition(i), mmCorpus.getSentencePosition(i));
			}
			
		} catch (IOException e) {
			Assert.fail(e.getLocalizedMessage());
		}
		
	}
	
}
