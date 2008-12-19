/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */
package joshua.util.sentence.alignment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Scanner;

import joshua.sarray.CorpusArray;
import joshua.sarray.HierarchicalPhrase;
import joshua.sarray.Pattern;
import joshua.sarray.PrefixTree;
import joshua.sarray.SuffixArray;
import joshua.sarray.SuffixArrayFactory;
import joshua.util.sentence.Span;
import joshua.util.sentence.Vocabulary;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class AlignmentsTest {

	Alignments alignments;
	
	CorpusArray sourceCorpusArray;
	CorpusArray targetCorpusArray;
	
	@Parameters({"alignmentsType"})
	@Test
	public void setup(String alignmentsType) throws IOException {

		String alignmentString = 
			"0-0 0-1 1-1 2-1 3-1 0-2 0-3 5-4 4-5 6-5 8-6 8-7 7-8 10-9 12-10 11-11 12-11 13-12 14-13 15-13 16-13 16-14 17-15 18-16 19-17 19-18 19-19 19-20 19-21 20-22 21-24 22-24 25-29 24-31 26-32 27-33 28-34 30-35 31-36 29-37 30-37 31-37 31-38 32-39" + "\n" +
			"0-0 0-1 0-2 1-3 2-5 3-6 4-6 5-7 6-8 7-9 8-10 10-11 12-11 9-12 11-12 12-12 13-13 14-14 18-16 21-17 22-19 22-20 23-20 24-21 25-22 25-23 26-24 27-25 28-25 29-26 30-26 31-26 31-28 32-29 34-30 33-31 35-33 36-34 36-35 37-36" + "\n" +
			"0-0 1-0 2-1 3-2 4-3 5-4 6-5 7-6 8-7 9-11 10-12 11-13 12-14 10-15 11-15 12-15 13-16 14-17 15-17 16-17 19-17 18-18 21-19 22-20";

		String sourceCorpusString = 
			"declaro reanudado el período de sesiones del parlamento europeo , interrumpido el viernes 17 de diciembre pasado , y reitero a sus señorías mi deseo de que hayan tenido unas buenas vacaciones ." + "\n" + 
			"como todos han podido comprobar , el gran `` efecto del año 2000 '' no se ha producido . en cambio , los ciudadanos de varios de nuestros países han sido víctimas de catástrofes naturales verdaderamente terribles ." + "\n" +
			"sus señorías han solicitado un debate sobre el tema para los próximos días , en el curso de este período de sesiones .";

		String targetCorpusString = 
			"i declare resumed the session of the european parliament adjourned on friday 17 december 1999 , and i would like once again to wish you a happy new year in the hope that you enjoyed a pleasant festive period ." + "\n" +
			"although , as you will have seen , the dreaded ` millennium bug ' failed to materialise , still the people in a number of countries suffered a series of natural disasters that truly were dreadful ." + "\n" +
			"you have requested a debate on this subject in the course of the next few days , during this part-session .";


		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			sourcePrintStream.println(sourceCorpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}

		String targetFileName;
		{
			File targetFile = File.createTempFile("target", new Date().toString());
			PrintWriter targetPrintStream = new PrintWriter(targetFile, "UTF-8");
			targetPrintStream.println(targetCorpusString);
			targetPrintStream.close();
			targetFileName = targetFile.getAbsolutePath();
		}

		String alignmentFileName;
		{
			File alignmentFile = File.createTempFile("alignment", new Date().toString());
			PrintStream alignmentPrintStream = new PrintStream(alignmentFile);
			alignmentPrintStream.println(alignmentString);
			alignmentPrintStream.close();
			alignmentFileName = alignmentFile.getAbsolutePath();
		}


		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);

		Vocabulary targetVocab = new Vocabulary();
		int[] targetWordsSentences = SuffixArrayFactory.createVocabulary(targetFileName, targetVocab);
		targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, targetWordsSentences[0], targetWordsSentences[1]);

		if (alignmentsType.equals("AlignmentArray")) {
			SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray);
			SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray);
			alignments = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);
		} else {
			alignments = new AlignmentGrids(new Scanner(new File(alignmentFileName)), sourceCorpusArray, targetCorpusArray, 3);			
		}
		
		
	}


	@Test(dependsOnMethods={"setup"})
	public void testHasAlignedTerminal() {

		Vocabulary vocab = sourceCorpusArray.getVocabulary();
		
		{
			Pattern     pattern = new Pattern(vocab, vocab.getIDs("de sesiones del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {4};
			int[]       terminalSequenceEndIndices = {9};
			int         length = 5;

			HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

			Assert.assertFalse(alignments.hasAlignedTerminal(0 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(1 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(2 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(3 , phrase));

			Assert.assertTrue(alignments.hasAlignedTerminal(4 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(5 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(6 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(7 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(8 , phrase));

			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrase));

		}
		
		{
			Pattern     pattern = new Pattern(vocab, vocab.getIDs(","));
			int[]       terminalSequenceStartIndices = {9};
			int[]       terminalSequenceEndIndices = {10};
			int         length = 1;

			HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

			Assert.assertFalse(alignments.hasAlignedTerminal(0 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(1 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(2 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(3 , phrase));

			Assert.assertFalse(alignments.hasAlignedTerminal(4 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(5 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(6 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(7 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrase));

			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrase));

		}
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testHasAlignedTerminalHierarchical() {
	
		Vocabulary vocab = sourceCorpusArray.getVocabulary();
		
		{
			Pattern     pattern = new Pattern(new Pattern(new Pattern(vocab, vocab.getIDs("de sesiones")), PrefixTree.X), vocab.getIDs("europo"));// del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {4,8};
			int[]       terminalSequenceEndIndices = {6,9};
			int         length = 5;

			HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

			Assert.assertFalse(alignments.hasAlignedTerminal(0 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(1 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(2 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(3 , phrase));

			Assert.assertTrue(alignments.hasAlignedTerminal(4 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(5 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(6 , phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(7 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(8 , phrase));

			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrase));

		}
		
		{
			Pattern     pattern = new Pattern(new Pattern(new Pattern(vocab, vocab.getIDs(", y")), PrefixTree.X), vocab.getIDs("sus"));// del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {17,21};
			int[]       terminalSequenceEndIndices = {19,22};
			int         length = 5;

			HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

			Assert.assertFalse(alignments.hasAlignedTerminal(12 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(13 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(14 , phrase));

			Assert.assertTrue(alignments.hasAlignedTerminal(15, phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(16, phrase));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(17, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(18, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(19, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(20, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(21, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(22, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(23, phrase));
			
			Assert.assertTrue(alignments.hasAlignedTerminal(24, phrase));

			Assert.assertFalse(alignments.hasAlignedTerminal(25, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(26, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(27, phrase));
			
		}
		
		{

			Pattern     pattern = new Pattern(new Pattern(new Pattern(vocab, vocab.getIDs(",")), PrefixTree.X), vocab.getIDs(", y"));// del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {9,17};
			int[]       terminalSequenceEndIndices = {10,19};
			int         length = 10;

			HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

			Assert.assertFalse(alignments.hasAlignedTerminal(9, phrase));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrase));			
			Assert.assertFalse(alignments.hasAlignedTerminal(12 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(13 , phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(14 , phrase));

			Assert.assertTrue(alignments.hasAlignedTerminal(15, phrase));
			Assert.assertTrue(alignments.hasAlignedTerminal(16, phrase));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(17, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(18, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(19, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(20, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(21, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(22, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(23, phrase));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(24, phrase));

			Assert.assertFalse(alignments.hasAlignedTerminal(25, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(26, phrase));
			Assert.assertFalse(alignments.hasAlignedTerminal(27, phrase));			
		
		}
	}
	
	
	@Test(dependsOnMethods={"setup"})
	public void testGetAlignedTargetSpan() {

		Span targetSpan;
		
		// Sentence 0
		{
			int sourceOffset = sourceCorpusArray.getSentencePosition(0);
			int targetOffset = targetCorpusArray.getSentencePosition(0);
			int sourceIndex;
			
			sourceIndex = sourceOffset+0;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+0);
			Assert.assertEquals(targetSpan.end, targetOffset+4);

			sourceIndex = sourceOffset+1;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+1);
			Assert.assertEquals(targetSpan.end, targetOffset+2);
			
			sourceIndex = sourceOffset+2;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+1);
			Assert.assertEquals(targetSpan.end, targetOffset+2);
			
			sourceIndex = sourceOffset+3;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+1);
			Assert.assertEquals(targetSpan.end, targetOffset+2);
			
			sourceIndex = sourceOffset+4;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+5);
			Assert.assertEquals(targetSpan.end, targetOffset+6);
			
			sourceIndex = sourceOffset+5;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+4);
			Assert.assertEquals(targetSpan.end, targetOffset+5);
			
			sourceIndex = sourceOffset+6;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+5);
			Assert.assertEquals(targetSpan.end, targetOffset+6);
			
			sourceIndex = sourceOffset+7;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+8);
			Assert.assertEquals(targetSpan.end, targetOffset+9);
			
			sourceIndex = sourceOffset+8;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+6);
			Assert.assertEquals(targetSpan.end, targetOffset+8);
			
			sourceIndex = sourceOffset+9;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, Alignments.UNALIGNED);
//			Assert.assertEquals(targetSpan.start, Alignments.UNALIGNED);
		
			sourceIndex = sourceOffset+10;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+9);
			Assert.assertEquals(targetSpan.end, targetOffset+10);
			
			//TODO Test the rest of the points for this sentence here
			
		}
		
		// Sentence 1
		{
			int sourceOffset = sourceCorpusArray.getSentencePosition(1);
			int targetOffset = targetCorpusArray.getSentencePosition(1);
			int sourceIndex;
			
			sourceIndex = sourceOffset+0;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+0);
			Assert.assertEquals(targetSpan.end, targetOffset+3);
			
			//TODO Test the rest of the points for this sentence here
			
		}
		
		
		// Sentence 2
		{
			int sourceOffset = sourceCorpusArray.getSentencePosition(2);
			int targetOffset = targetCorpusArray.getSentencePosition(2);
			int sourceIndex;
			
			//TODO Test the rest of the points for this sentence here
			
			sourceIndex = sourceOffset+22;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+20);
			Assert.assertEquals(targetSpan.end, targetOffset+21);
					
		}
		
	}
	
	
	@Test(dependsOnMethods={"setup"})
	public void testGetAlignedSourceSpan() {
		
		Span sourceSpan;
		
		// Sentence 0
		{
			int sourceOffset = sourceCorpusArray.getSentencePosition(0);
			int targetOffset = targetCorpusArray.getSentencePosition(0);
			int targetIndex;
			
			targetIndex = targetOffset+0;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+0);
			Assert.assertEquals(sourceSpan.end, sourceOffset+1);

			targetIndex = targetOffset+1;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+0);
			Assert.assertEquals(sourceSpan.end, sourceOffset+4);

			targetIndex = targetOffset+2;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+0);
			Assert.assertEquals(sourceSpan.end, sourceOffset+1);

			targetIndex = targetOffset+3;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+0);
			Assert.assertEquals(sourceSpan.end, sourceOffset+1);
			
			targetIndex = targetOffset+4;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+5);
			Assert.assertEquals(sourceSpan.end, sourceOffset+6);
			
			targetIndex = targetOffset+5;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+4);
			Assert.assertEquals(sourceSpan.end, sourceOffset+7);
			
			//TODO Test the rest of the points for this sentence here
		}
		
		// Sentence 1
		{
			int sourceOffset = sourceCorpusArray.getSentencePosition(1);
			int targetOffset = targetCorpusArray.getSentencePosition(1);
			int targetIndex;
			
			targetIndex = targetOffset+0;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+0);
			Assert.assertEquals(sourceSpan.end, sourceOffset+1);
		
			//TODO Test the rest of the points for this sentence here
		}
		
		// Sentence 2
		{
			int sourceOffset = sourceCorpusArray.getSentencePosition(2);
			int targetOffset = targetCorpusArray.getSentencePosition(2);
			int targetIndex;
			
			targetIndex = targetOffset+0;
			sourceSpan = alignments.getAlignedSourceSpan(targetIndex, targetIndex+1);
			Assert.assertNotNull(sourceSpan);
			Assert.assertEquals(sourceSpan.start, sourceOffset+0);
			Assert.assertEquals(sourceSpan.end, sourceOffset+2);
		
			//TODO Test the rest of the points for this sentence here
		}
		
	}
	
	
	@Test(dependsOnMethods={"setup"})
	public void testGetAlignedSourceIndices() throws IOException {

		int[] sourcePoints;

		// Sentence 0
		{
			int offset = 0;
			
			sourcePoints = alignments.getAlignedSourceIndices(offset+0);
			Assert.assertEquals(sourcePoints.length, 1);
			Assert.assertEquals(sourcePoints[0], 0);
			
			sourcePoints = alignments.getAlignedSourceIndices(offset+1);
			Assert.assertEquals(sourcePoints.length, 4);
			Assert.assertEquals(sourcePoints[0], 0);
			Assert.assertEquals(sourcePoints[1], 1);
			Assert.assertEquals(sourcePoints[2], 2);
			Assert.assertEquals(sourcePoints[3], 3);
			
			sourcePoints = alignments.getAlignedSourceIndices(offset+2);
			Assert.assertEquals(sourcePoints.length, 1);
			Assert.assertEquals(sourcePoints[0], 0);
			
			sourcePoints = alignments.getAlignedSourceIndices(offset+3);
			Assert.assertEquals(sourcePoints.length, 1);
			Assert.assertEquals(sourcePoints[0], 0);	
			
			sourcePoints = alignments.getAlignedSourceIndices(offset+4);
			Assert.assertEquals(sourcePoints.length, 1);
			Assert.assertEquals(sourcePoints[0], 5);
			
			sourcePoints = alignments.getAlignedSourceIndices(offset+5);
			Assert.assertEquals(sourcePoints.length, 2);
			Assert.assertEquals(sourcePoints[0], 4);
			Assert.assertEquals(sourcePoints[1], 6);
			
			//TODO Test the rest of the points for this sentence here
			
		}
		
		// Sentence 1
		{	
			int sourceOffset = sourceCorpusArray.getSentencePosition(1);
			int targetOffset = targetCorpusArray.getSentencePosition(1);
			
			sourcePoints = alignments.getAlignedSourceIndices(targetOffset+0);
			Assert.assertEquals(sourcePoints.length, 1);
			Assert.assertEquals(sourcePoints[0], sourceOffset+0);
			
			//TODO Test the rest of the points for this sentence here
		}
		
		// Sentence 2
		{	
			int sourceOffset = sourceCorpusArray.getSentencePosition(2);
			int targetOffset = targetCorpusArray.getSentencePosition(2);
			
			sourcePoints = alignments.getAlignedSourceIndices(targetOffset+0);
			Assert.assertEquals(sourcePoints.length, 2);
			Assert.assertEquals(sourcePoints[0], sourceOffset+0);
			Assert.assertEquals(sourcePoints[1], sourceOffset+1);

			//TODO Test the rest of the points for this sentence here
		}
	}
	
	

	@Test(dependsOnMethods={"setup"})
	public void testGetAlignedTargetIndices() throws IOException {

		int[] targetPoints;

		// Sentence 0
		{
			int offset = 0;
			
			targetPoints = alignments.getAlignedTargetIndices(offset+0);
			Assert.assertEquals(targetPoints.length, 4);
			Assert.assertEquals(targetPoints[0], 0);
			Assert.assertEquals(targetPoints[1], 1);
			Assert.assertEquals(targetPoints[2], 2);
			Assert.assertEquals(targetPoints[3], 3);

			targetPoints = alignments.getAlignedTargetIndices(offset+1);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 1);

			targetPoints = alignments.getAlignedTargetIndices(offset+2);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 1);

			targetPoints = alignments.getAlignedTargetIndices(offset+3);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 1);

			targetPoints = alignments.getAlignedTargetIndices(offset+4);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 5);

			targetPoints = alignments.getAlignedTargetIndices(offset+5);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 4);

			targetPoints = alignments.getAlignedTargetIndices(offset+6);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 5);

			targetPoints = alignments.getAlignedTargetIndices(offset+7);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 8);

			targetPoints = alignments.getAlignedTargetIndices(offset+8);
			Assert.assertEquals(targetPoints.length, 2);
			Assert.assertEquals(targetPoints[0], 6);
			Assert.assertEquals(targetPoints[1], 7);

			targetPoints = alignments.getAlignedTargetIndices(offset+9);
			Assert.assertNull(targetPoints);
			//			Assert.assertEquals(targetPoints.length, 0);

			targetPoints = alignments.getAlignedTargetIndices(offset+10);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 9);

			targetPoints = alignments.getAlignedTargetIndices(offset+11);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 11);

			targetPoints = alignments.getAlignedTargetIndices(offset+12);
			Assert.assertEquals(targetPoints.length, 2);
			Assert.assertEquals(targetPoints[0], 10);
			Assert.assertEquals(targetPoints[1], 11);

			targetPoints = alignments.getAlignedTargetIndices(offset+13);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 12);

			targetPoints = alignments.getAlignedTargetIndices(offset+14);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 13);

			targetPoints = alignments.getAlignedTargetIndices(offset+15);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 13);

			targetPoints = alignments.getAlignedTargetIndices(offset+16);
			Assert.assertEquals(targetPoints.length, 2);
			Assert.assertEquals(targetPoints[0], 13);
			Assert.assertEquals(targetPoints[1], 14);

			targetPoints = alignments.getAlignedTargetIndices(offset+17);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 15);

			targetPoints = alignments.getAlignedTargetIndices(offset+18);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 16);

			targetPoints = alignments.getAlignedTargetIndices(offset+19);
			Assert.assertEquals(targetPoints.length, 5);
			Assert.assertEquals(targetPoints[0], 17);
			Assert.assertEquals(targetPoints[1], 18);
			Assert.assertEquals(targetPoints[2], 19);
			Assert.assertEquals(targetPoints[3], 20);
			Assert.assertEquals(targetPoints[4], 21);

			targetPoints = alignments.getAlignedTargetIndices(offset+20);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 22);

			targetPoints = alignments.getAlignedTargetIndices(offset+21);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 24);

			targetPoints = alignments.getAlignedTargetIndices(offset+22);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 24);

			targetPoints = alignments.getAlignedTargetIndices(offset+23);
			Assert.assertNull(targetPoints);
//			Assert.assertEquals(targetPoints.length, 0);

			targetPoints = alignments.getAlignedTargetIndices(offset+24);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 31);

			targetPoints = alignments.getAlignedTargetIndices(offset+25);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 29);

			targetPoints = alignments.getAlignedTargetIndices(offset+26);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 32);

			targetPoints = alignments.getAlignedTargetIndices(offset+27);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 33);

			targetPoints = alignments.getAlignedTargetIndices(offset+28);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 34);

			targetPoints = alignments.getAlignedTargetIndices(offset+29);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 37);

			targetPoints = alignments.getAlignedTargetIndices(offset+30);
			Assert.assertEquals(targetPoints.length, 2);
			Assert.assertEquals(targetPoints[0], 35);
			Assert.assertEquals(targetPoints[1], 37);

			targetPoints = alignments.getAlignedTargetIndices(offset+31);
			Assert.assertEquals(targetPoints.length, 3);
			Assert.assertEquals(targetPoints[0], 36);
			Assert.assertEquals(targetPoints[1], 37);
			Assert.assertEquals(targetPoints[2], 38);

			targetPoints = alignments.getAlignedTargetIndices(offset+32);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], 39);
		}
		
		// Sentence 1
		{	
			int sourceOffset = sourceCorpusArray.getSentencePosition(1);
			int targetOffset = targetCorpusArray.getSentencePosition(1);

			//TODO Test the rest of the points for this sentence here
			
			targetPoints = alignments.getAlignedTargetIndices(sourceOffset+0);
			Assert.assertEquals(targetPoints.length, 3);
			Assert.assertEquals(targetPoints[0], targetOffset+0);
			Assert.assertEquals(targetPoints[1], targetOffset+1);
			Assert.assertEquals(targetPoints[2], targetOffset+2);
		}
		
		// Sentence 2
		{	
			int sourceOffset = sourceCorpusArray.getSentencePosition(2);
			int targetOffset = targetCorpusArray.getSentencePosition(2);
			
			//TODO Test the rest of the points for this sentence here
			
			targetPoints = alignments.getAlignedTargetIndices(sourceOffset+22);
			Assert.assertEquals(targetPoints.length, 1);
			Assert.assertEquals(targetPoints[0], targetOffset+20);
		}
	}

}
