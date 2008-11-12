package edu.jhu.sa.util.suffix_array;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SampledLexProbsTest {

	SampledLexProbs lexProbs;
	Vocabulary sourceVocab, targetVocab;
	AlignmentArray alignmentArray;
	
	@Test
	public void setup() throws IOException {
		
		String sourceCorpusString = "it makes him and it mars him , it sets him on yet it takes him off .";
		
		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile);
			sourcePrintStream.println(sourceCorpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}
	
		String targetCorpusString = "das macht ihn und es beschädigt ihn , es setzt ihn auf und es führt ihn aus .";
		String targetFileName;
		{
			File targetFile = File.createTempFile("target", new Date().toString());
			PrintStream targetPrintStream = new PrintStream(targetFile);
			targetPrintStream.println(targetCorpusString);
			targetPrintStream.close();
			targetFileName = targetFile.getAbsolutePath();
		}
		
		String alignmentString = "0-0 1-1 2-2 3-3 4-4 5-5 6-6 7-7 8-8 9-9 10-10 11-11 12-12 13-13 14-14 15-15 16-16 17-17";
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
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
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
//			Assert.assertNotNull(sourceIndices);
//			Assert.assertEquals(sourceIndices.length, 1);
			try {
				Assert.assertEquals(sourceIndices[0], i);
			} catch (NullPointerException e) {
				System.out.println(i);
				throw e;
			}
			
			int[] targetIndices = alignmentArray.getAlignedTargetIndices(i);
			Assert.assertNotNull(targetIndices);
			Assert.assertEquals(targetIndices.length, 1);
			Assert.assertEquals(targetIndices[0], i);			
			
		}
		
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
