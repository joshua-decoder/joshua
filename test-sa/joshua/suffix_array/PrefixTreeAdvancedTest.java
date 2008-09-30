package joshua.suffix_array;

import java.util.HashSet;
import java.util.Set;

import joshua.suffix_array.AlignmentArray;
import joshua.suffix_array.BasicPhrase;
import joshua.suffix_array.CorpusArray;
import joshua.suffix_array.HierarchicalPhrase;
import joshua.suffix_array.PrefixTree;
import joshua.suffix_array.SuffixArray;
import joshua.suffix_array.PrefixTree.Node;


import org.testng.Assert;
import org.testng.annotations.Test;

import edu.jhu.sa.util.sentence.Span;
import edu.jhu.sa.util.sentence.Vocabulary;

public class PrefixTreeAdvancedTest {

	
	@Test
	public void test() {
		
		// Adam Lopez's example...
		String corpusString = "it makes him and it mars him , it sets him on and it takes him off .";
		String queryString = "it persuades him and it disheartens him";
		
		Set<String> words = new HashSet<String>();
		for (String word : corpusString.split("\\s+")) {
			words.add(word);
		}
		
		Vocabulary vocab = new Vocabulary(words);
		
		BasicPhrase corpusSentence = new BasicPhrase(corpusString, vocab);
		BasicPhrase querySentence = new BasicPhrase(queryString, vocab);
		
		Assert.assertEquals(querySentence.toString(), "it UNK him and it UNK him");
		Assert.assertEquals(corpusSentence.toString(), corpusString);
		
		
		// create the suffix array...
		int[] sentenceStartPositions = {0};
		
		Assert.assertEquals(corpusSentence.size(), 18);
		
		int[] corpus = new int[corpusSentence.size()];
		for(int i = 0; i < corpusSentence.size(); i++) {
			corpus[i] = corpusSentence.getWordID(i);
		}
		
		CorpusArray corpusArray = new CorpusArray(corpus, sentenceStartPositions, vocab);
		SuffixArray suffixArray = new SuffixArray(corpusArray);
		

		String targetCorpusString = "es macht ihn und es beschädigt ihn , es setzt ihn auf und es führt ihn aus .";
		Set<String> targetWords = new HashSet<String>();
		for (String targetWord : targetCorpusString.split("\\s+")) {
			targetWords.add(targetWord);
		}
		
		Vocabulary targetVocab = new Vocabulary(targetWords);
		int[] targetSentenceStartPositions = {0};
		
		BasicPhrase targetCorpusSentence = new BasicPhrase(targetCorpusString, targetVocab);
		Assert.assertEquals(targetCorpusSentence.size(), 18);
		
		int[] targetCorpus = new int[targetCorpusSentence.size()];
		for(int i = 0; i < targetCorpusSentence.size(); i++) {
			targetCorpus[i] = targetCorpusSentence.getWordID(i);
		}
		
		int maxPhraseSpan = 10;
		int maxPhraseLength = 10;
		int maxNonterminals = 2;
		
		CorpusArray targetCorpusArray = new CorpusArray(targetCorpus, targetSentenceStartPositions, targetVocab);
		
		int[] lowestAlignedTargetIndex = new int[corpusSentence.size()];
		int[] highestAlignedTargetIndex = new int[corpusSentence.size()];
		int[] lowestAlignedSourceIndex = new int[targetCorpusSentence.size()];
		int[] highestAlignedSourceIndex = new int[targetCorpusSentence.size()];
		
		{
			for (int i=0; i<18; i++) {
				lowestAlignedTargetIndex[i] = i;
				highestAlignedTargetIndex[i] = i;
				lowestAlignedSourceIndex[i] = i;
				highestAlignedSourceIndex[i] = i;
			}
		}
		
		
		AlignmentArray alignments = new AlignmentArray(lowestAlignedTargetIndex, highestAlignedTargetIndex, lowestAlignedSourceIndex, highestAlignedSourceIndex);
		
		PrefixTree prefixTree = new PrefixTree(suffixArray, targetCorpusArray, alignments, querySentence.getWordIDs(), maxPhraseSpan, maxPhraseLength, maxNonterminals);
		
		
		//System.out.println(prefixTree.toString(vocab));
		//System.out.println();
		//System.out.println(prefixTree.size());
		
		
		Assert.assertTrue(prefixTree.root.children.containsKey(PrefixTree.X));
		Assert.assertTrue(prefixTree.root.children.containsKey(vocab.getID("it")));
		Assert.assertTrue(prefixTree.root.children.containsKey(vocab.getID("him")));
		Assert.assertTrue(prefixTree.root.children.containsKey(vocab.getID("and")));
		Assert.assertTrue(prefixTree.root.children.containsKey(vocab.getID(Vocabulary.UNKNOWN_WORD_STRING)));

		Assert.assertEquals(prefixTree.root.children.size(), 5);
		
		Assert.assertFalse(prefixTree.root.getChild(PrefixTree.X).children.containsKey(PrefixTree.X));
		Assert.assertTrue(prefixTree.root.getChild(PrefixTree.X).children.containsKey(vocab.getID("it")));
		Assert.assertTrue(prefixTree.root.getChild(PrefixTree.X).children.containsKey(vocab.getID("him")));
		Assert.assertTrue(prefixTree.root.getChild(PrefixTree.X).children.containsKey(vocab.getID("and")));
		Assert.assertTrue(prefixTree.root.getChild(PrefixTree.X).children.containsKey(vocab.getID(Vocabulary.UNKNOWN_WORD_STRING)));
		
		Assert.assertEquals(prefixTree.root.getChild(PrefixTree.X).children.size(), 4);
		
		//////
		
		Assert.assertNotNull(prefixTree.root.getChild(vocab.getID("it")).hierarchicalPhrases);
		
		for (Node node : prefixTree.root.children.values()) {
			
			Assert.assertNotNull(node.hierarchicalPhrases);
			//System.out.println(node.hierarchicalPhrases.size());
			
			for (HierarchicalPhrase p : node.hierarchicalPhrases) {
				/*
				System.out.println(p);
				Span span = p.getSpan();
				
				Span targetSpan = alignments.getAlignedTargetSpan(span);
				System.out.println(span + " => " + targetSpan);
				*/
			}
			
		}
		
		//System.out.println("Children of 'him' (" + vocab.getID("him") + ") :\n\n");
		//System.out.println(prefixTree.root.getChild(vocab.getID("him")).toString(vocab));
		//System.out.println(prefixTree.root.getChild(vocab.getID("him")).children.size() + " children");
		
		for (Node node : prefixTree.root.getChild(vocab.getID("him")).children.values()) {
			
			Assert.assertNotNull(node.hierarchicalPhrases);
			//System.out.println("Current node: " + node.toString(vocab));
			
			for (HierarchicalPhrase p : node.hierarchicalPhrases) {
				/*
				System.out.println(p);
				Span span = p.getSpan();
				
				Span targetSpan = alignments.getAlignedTargetSpan(span);
				System.out.println(span + " => " + targetSpan);
				*/
			}
			
		}
	}
	
}
