package edu.jhu.sa.util.suffix_array;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import joshua.util.Pair;
import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SampledLexProbsTest {

	SampledLexProbs lexProbs;
	Vocabulary sourceVocab, targetVocab;
	AlignmentArray alignmentArray;
	CorpusArray sourceCorpusArray;
	
	@Test
	public void setup() throws IOException {
		
		String sourceCorpusString = 
			"it makes him and it mars him , it sets him on yet it takes him off ." + "\n" +
			"resumption of the session ." + "\n" +
			"of the session" + "\n" + 
			"of the session";
		
		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile);
			sourcePrintStream.println(sourceCorpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}
	
		String targetCorpusString = 
			"das macht ihn und es beschädigt ihn , es setzt ihn auf und es führt ihn aus ." + "\n" +
			"wiederaufnahme der sitzungsperiode ." + "\n" +
			"von dem sitzung" + "\n" + 
			"von dem sitzung";
		
		String targetFileName;
		{
			File targetFile = File.createTempFile("target", new Date().toString());
			PrintStream targetPrintStream = new PrintStream(targetFile);
			targetPrintStream.println(targetCorpusString);
			targetPrintStream.close();
			targetFileName = targetFile.getAbsolutePath();
		}
		
		String alignmentString = 
			"0-0 1-1 2-2 3-3 4-4 5-5 6-6 7-7 8-8 9-9 10-10 11-11 12-12 13-13 14-14 15-15 16-16 17-17" + "\n" +
			"0-0 1-1 2-1 3-2 4-3" + "\n" +
			"0-0 1-1 2-2" + "\n" +
			"0-0 1-1 2-2";
		String alignmentFileName;
		{
			File alignmentFile = File.createTempFile("alignment", new Date().toString());
			PrintStream alignmentPrintStream = new PrintStream(alignmentFile);
			alignmentPrintStream.println(alignmentString);
			alignmentPrintStream.close();
			alignmentFileName = alignmentFile.getAbsolutePath();
		}
		
		sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray);

		targetVocab = new Vocabulary();
		int[] targetWordsSentences = SuffixArrayFactory.createVocabulary(targetFileName, targetVocab);
		CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, targetWordsSentences[0], targetWordsSentences[1]);
		SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray);

		alignmentArray = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);

		lexProbs = 
			new SampledLexProbs(Integer.MAX_VALUE, sourceSuffixArray, targetSuffixArray, alignmentArray, false);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testAlignmentPoints() {
	
		for (int i=0; i<18; i++) {
			
			int[] sourceIndices = alignmentArray.getAlignedSourceIndices(i);
			Assert.assertNotNull(sourceIndices);
			Assert.assertEquals(sourceIndices.length, 1);
			Assert.assertEquals(sourceIndices[0], i);
			
			int[] targetIndices = alignmentArray.getAlignedTargetIndices(i);
			Assert.assertNotNull(targetIndices);
			Assert.assertEquals(targetIndices.length, 1);
			Assert.assertEquals(targetIndices[0], i);			
			
		}
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void calculateLexProbs() {
	
		Pair<Float,Float> results;
		
		//	"it makes him and it mars him , it sets him on yet it takes him off .";
		// "das macht ihn und es beschädigt ihn , es setzt ihn auf und es führt ihn aus ."
		
		results = lexProbs.calculateLexProbs(getPhrase("it", 0, 1));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);  // lex P(it | das)
		Assert.assertEquals(results.second, 0.25f);// lex P(das | it)
		
		results = lexProbs.calculateLexProbs(getPhrase("makes", 1, 2));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f); // lex P(makes | macht)
		Assert.assertEquals(results.second, 1.0f);// lex P(macht | makes)
		
		results = lexProbs.calculateLexProbs(getPhrase("him", 2, 3));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("and", 3, 4));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 0.5f); // P(and | und)
		Assert.assertEquals(results.second, 1.0f);// P(und | and)
		
		results = lexProbs.calculateLexProbs(getPhrase("it", 4, 5));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);  // lex P(it | es)
		Assert.assertEquals(results.second, 0.75f);// lex P(es | it)
		
		results = lexProbs.calculateLexProbs(getPhrase("mars", 5, 6));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("him", 6, 7));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase(",", 7, 8));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("it", 8, 9));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);  // lex P(it | es)
		Assert.assertEquals(results.second, 0.75f);// lex P(es | it)
		
		results = lexProbs.calculateLexProbs(getPhrase("sets", 9, 10));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("him", 10, 11));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("on", 11, 12));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("yet", 12, 13));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 0.5f); // P(yet | und)
		Assert.assertEquals(results.second, 1.0f);// P(und | yet)
		
		results = lexProbs.calculateLexProbs(getPhrase("it", 13, 14));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);  // lex P(it | es)
		Assert.assertEquals(results.second, 0.75f);// lex P(es | it)
		
		results = lexProbs.calculateLexProbs(getPhrase("takes", 14, 15));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("him", 15, 16));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase("off", 16, 17));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		results = lexProbs.calculateLexProbs(getPhrase(".", 17, 18));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f);
		Assert.assertEquals(results.second, 1.0f);	
		
		///////////
		
		results = lexProbs.calculateLexProbs(getPhrase("yet it", 12, 14));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 0.5f * 1.0f);  // lex P(yet it | und es)
		Assert.assertEquals(results.second, 1.0f * 0.75f);// lex P(und es | yet it)
		
		///////////
		
		results = lexProbs.calculateLexProbs(getPhrase("of the session", 19, 22));
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 0.5f * 0.5f * 1.0f);  // lex P(of the session | der sitzungsperiode)
		Assert.assertEquals(results.second, 0.5f*((1.0f/3.0f) + (1.0f/3.0f)) * (1.0f/3.0f));// lex P(der sitzungsperiode | of the session)
		
		
	}
	
	/**
	 * Unit test to verify correct calculation of
	 * lexical translation probabilities for phrases with gaps.
	 */
	@Test(dependsOnMethods={"setup"})
	public void calculateHieroLexProbs() {
		
		Pattern pattern = new Pattern(sourceVocab, 
				sourceVocab.getID("it"), 
				PrefixTree.X, 
				sourceVocab.getID("and"), 
				sourceVocab.getID("it"));
		
		int[] terminalSequenceStartIndices = {0,3};
		int[] terminalSequenceEndIndices = {1,5};
		
		HierarchicalPhrase phrase = new HierarchicalPhrase(
				pattern, 
				terminalSequenceStartIndices,
				terminalSequenceEndIndices,
				sourceCorpusArray,
				terminalSequenceEndIndices[terminalSequenceEndIndices.length-1] - terminalSequenceStartIndices[0]);
		
		Pair<Float,Float> results;
		
		//	"it makes him and it mars him , it sets him on yet it takes him off .";
		// "das macht ihn und es beschädigt ihn , es setzt ihn auf und es führt ihn aus ."
		
		results = lexProbs.calculateLexProbs(phrase);
		Assert.assertNotNull(results);
		Assert.assertEquals(results.first, 1.0f * 0.5f * 1.0f);   // lex P(it X and it | das X und es)
		Assert.assertEquals(results.second, 0.25f * 1.0f * 0.75f);// lex P(das X und es | it X and it)
		
	}
	
	private HierarchicalPhrase getPhrase(String sourcePhrase, int startIndex, int endIndex) {
		Pattern pattern = new Pattern(sourceVocab, sourceVocab.getIDs(sourcePhrase));
		int[] terminalSequenceStartIndices = {startIndex};
		int[] terminalSequenceEndIndices = {endIndex};
		
		return new HierarchicalPhrase(
				pattern, 
				terminalSequenceStartIndices,
				terminalSequenceEndIndices,
				sourceCorpusArray,
				1);
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testSourceGivenTargetString() {
		
		// In this example, English is the source & German is the target

		Assert.assertEquals(lexProbs.sourceGivenTarget(",", ","), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(".", "."), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("on", "auf"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("off", "aus"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("mars", "beschädigt"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("it", "das"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("it", "es"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("takes", "führt"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("him", "ihn"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("makes", "macht"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("sets", "setzt"), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("and", "und"), 0.5f);
		Assert.assertEquals(lexProbs.sourceGivenTarget("yet", "und"), 0.5f);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testSourceGivenTarget() {
		
		// In this example, English is the source & German is the target
		
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID(","), targetVocab.getID(",")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("."), targetVocab.getID(".")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("on"), targetVocab.getID("auf")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("off"), targetVocab.getID("aus")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("mars"), targetVocab.getID("beschädigt")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("it"), targetVocab.getID("das")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("it"), targetVocab.getID("es")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("takes"), targetVocab.getID("führt")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("him"), targetVocab.getID("ihn")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("makes"), targetVocab.getID("macht")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("sets"), targetVocab.getID("setzt")), 1.0f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("and"), targetVocab.getID("und")), 0.5f);
		Assert.assertEquals(lexProbs.sourceGivenTarget(sourceVocab.getID("yet"), targetVocab.getID("und")), 0.5f);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testTargetGivenSourceString() {

		Assert.assertEquals(lexProbs.targetGivenSource(",", ","), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(".", "."), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("und", "and"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("ihn", "him"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("das", "it"), 0.25f);
		Assert.assertEquals(lexProbs.targetGivenSource("es", "it"), 0.75f);
		Assert.assertEquals(lexProbs.targetGivenSource("macht", "makes"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("beschädigt", "mars"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("aus", "off"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("auf", "on"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("setzt", "sets"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("führt", "takes"), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource("und", "yet"), 1.0f);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testTargetGivenSource() {

		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID(","), sourceVocab.getID(",")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("."), sourceVocab.getID(".")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("und"), sourceVocab.getID("and")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("ihn"), sourceVocab.getID("him")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("das"), sourceVocab.getID("it")), 0.25f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("es"), sourceVocab.getID("it")), 0.75f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("macht"), sourceVocab.getID("makes")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("beschädigt"), sourceVocab.getID("mars")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("aus"), sourceVocab.getID("off")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("auf"), sourceVocab.getID("on")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("setzt"), sourceVocab.getID("sets")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("führt"), sourceVocab.getID("takes")), 1.0f);
		Assert.assertEquals(lexProbs.targetGivenSource(targetVocab.getID("und"), sourceVocab.getID("yet")), 1.0f);
		
	}
}
