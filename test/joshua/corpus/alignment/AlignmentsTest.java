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
package joshua.corpus.alignment;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Scanner;

import joshua.corpus.CorpusArray;
import joshua.corpus.Span;
import joshua.corpus.alignment.AlignmentGrids;
import joshua.corpus.alignment.mm.MemoryMappedAlignmentGrids;
import joshua.corpus.suffix_array.HierarchicalPhrases;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.suffix_array.SuffixArray;
import joshua.corpus.suffix_array.SuffixArrayFactory;
import joshua.corpus.vocab.SymbolTable;
import joshua.prefix_tree.PrefixTree;
import joshua.util.io.BinaryOut;

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
//	String type;
	@Parameters({"alignmentsType"})
	@Test
	public void setup(String alignmentsType) throws IOException {
//		type = alignmentsType;
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
		
		
		sourceCorpusArray =
			SuffixArrayFactory.createCorpusArray(sourceFileName);
		
		targetCorpusArray =
			SuffixArrayFactory.createCorpusArray(targetFileName);

		if (alignmentsType.equals("AlignmentArray")) {
			SuffixArray targetSuffixArray = 
				SuffixArrayFactory.createSuffixArray(targetCorpusArray, SuffixArray.DEFAULT_CACHE_CAPACITY);
			SuffixArray sourceSuffixArray = 
				SuffixArrayFactory.createSuffixArray(sourceCorpusArray, SuffixArray.DEFAULT_CACHE_CAPACITY);
			alignments = 
				SuffixArrayFactory.createAlignments(alignmentFileName, sourceSuffixArray, targetSuffixArray);
		} else if (alignmentsType.equals("AlignmentGrids")) {
			alignments = new AlignmentGrids(new Scanner(new File(alignmentFileName)), sourceCorpusArray, targetCorpusArray, 3);			
		} else if (alignmentsType.equals("MemoryMappedAlignmentGrids")) {
			AlignmentGrids grids = new AlignmentGrids(new Scanner(new File(alignmentFileName)), sourceCorpusArray, targetCorpusArray, 3);
			
			File mmAlignmentFile = File.createTempFile("memoryMappedAlignment", new Date().toString());
			ObjectOutput out = new BinaryOut(mmAlignmentFile);
			grids.writeExternal(out);
			out.flush();
			out.close();
			
			alignments = new MemoryMappedAlignmentGrids(mmAlignmentFile.getAbsolutePath(), sourceCorpusArray, targetCorpusArray);
		} else {
			Assert.fail(alignmentsType + " is not a known alignment type.");
		}
		
		
	}

//	@Test
//	public void compare() {
//		Assert.assertEquals(type, "MemoryMappedAlignmentGrids");
////		Assert.assertTrue(alignments instanceof AlignmentGrids);
////		Assert.assertTrue(alignments instanceof MemoryMappedAlignmentGrids);		
//	}
	

	@Test(dependsOnMethods={"setup"})
	public void testHasAlignedTerminal() {
		
		SymbolTable vocab = sourceCorpusArray.getVocabulary();
		
		{
			Pattern     pattern = new Pattern(vocab, vocab.getIDs("de sesiones del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {4};
			int[]       sentenceNumbers = {0};
			int phraseIndex = 0;
			
			HierarchicalPhrases phrases =
				new HierarchicalPhrases(pattern, terminalSequenceStartIndices, sentenceNumbers);
			
			
			
			Assert.assertFalse(alignments.hasAlignedTerminal(0 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(1 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(2 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(3 , phrases, phraseIndex));

			Assert.assertTrue(alignments.hasAlignedTerminal(4 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(5 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(6 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(7 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(8 , phrases, phraseIndex));

			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrases, phraseIndex));
		}
	
		
		{
			Pattern     pattern = new Pattern(vocab, vocab.getIDs(","));
			int[]       terminalSequenceStartIndices = {9};
			int[]       sentenceNumbers = {0};
			int phraseIndex = 0;

			HierarchicalPhrases phrases =
				new HierarchicalPhrases(pattern, terminalSequenceStartIndices, sentenceNumbers);
			
			Assert.assertFalse(alignments.hasAlignedTerminal(0 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(1 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(2 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(3 , phrases, phraseIndex));

			Assert.assertFalse(alignments.hasAlignedTerminal(4 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(5 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(6 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(7 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrases, phraseIndex));

			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrases, phraseIndex));

		}
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testHasAlignedTerminalHierarchical() {
		
		
	
		SymbolTable vocab = sourceCorpusArray.getVocabulary();
		
		{
			Pattern     pattern = new Pattern(new Pattern(new Pattern(vocab, vocab.getIDs("de sesiones")), SymbolTable.X), vocab.getIDs("europo"));// del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {4,8};
			int[]       sentenceNumbers = {0};
			int phraseIndex = 0;
			
			HierarchicalPhrases phrases =
				new HierarchicalPhrases(pattern, terminalSequenceStartIndices, sentenceNumbers);
//			HierarchicalPhrase phrase = new HierarchicalPhrase(pattern, terminalSequenceStartIndices, terminalSequenceEndIndices, sourceCorpusArray, length);

			Assert.assertFalse(alignments.hasAlignedTerminal(0 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(1 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(2 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(3 , phrases, phraseIndex));

			Assert.assertTrue(alignments.hasAlignedTerminal(4 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(5 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(6 , phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(7 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(8 , phrases, phraseIndex));

			Assert.assertFalse(alignments.hasAlignedTerminal(9 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrases, phraseIndex));

		}
		
		{
			Pattern     pattern = new Pattern(new Pattern(new Pattern(vocab, vocab.getIDs(", y")), PrefixTree.X), vocab.getIDs("sus"));// del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {17,21};
			int[]       sentenceNumbers = {0};
			int phraseIndex = 0;
			
			HierarchicalPhrases phrases =
				new HierarchicalPhrases(pattern, terminalSequenceStartIndices, sentenceNumbers);
			
			Assert.assertFalse(alignments.hasAlignedTerminal(12 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(13 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(14 , phrases, phraseIndex));

			Assert.assertTrue(alignments.hasAlignedTerminal(15, phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(16, phrases, phraseIndex));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(17, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(18, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(19, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(20, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(21, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(22, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(23, phrases, phraseIndex));
			
			Assert.assertTrue(alignments.hasAlignedTerminal(24, phrases, phraseIndex));

			Assert.assertFalse(alignments.hasAlignedTerminal(25, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(26, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(27, phrases, phraseIndex));
			
		}
		
		{

			Pattern     pattern = new Pattern(new Pattern(new Pattern(vocab, vocab.getIDs(",")), PrefixTree.X), vocab.getIDs(", y"));// del parlamento europeo"));
			int[]       terminalSequenceStartIndices = {9,17};
			int[]       sentenceNumbers = {0};
			int phraseIndex = 0;
			
			HierarchicalPhrases phrases =
				new HierarchicalPhrases(pattern, terminalSequenceStartIndices, sentenceNumbers);
			
			Assert.assertFalse(alignments.hasAlignedTerminal(9, phrases, phraseIndex));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(10 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(11 , phrases, phraseIndex));			
			Assert.assertFalse(alignments.hasAlignedTerminal(12 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(13 , phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(14 , phrases, phraseIndex));

			Assert.assertTrue(alignments.hasAlignedTerminal(15, phrases, phraseIndex));
			Assert.assertTrue(alignments.hasAlignedTerminal(16, phrases, phraseIndex));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(17, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(18, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(19, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(20, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(21, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(22, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(23, phrases, phraseIndex));
			
			Assert.assertFalse(alignments.hasAlignedTerminal(24, phrases, phraseIndex));

			Assert.assertFalse(alignments.hasAlignedTerminal(25, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(26, phrases, phraseIndex));
			Assert.assertFalse(alignments.hasAlignedTerminal(27, phrases, phraseIndex));			
		
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
		
			sourceIndex = sourceOffset+10;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+9);
			Assert.assertEquals(targetSpan.end, targetOffset+10);
			
			sourceIndex = sourceOffset+11;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+11);
			Assert.assertEquals(targetSpan.end, targetOffset+12);
			
			sourceIndex = sourceOffset+12;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+10);
			Assert.assertEquals(targetSpan.end, targetOffset+12);
			
			sourceIndex = sourceOffset+13;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+12);
			Assert.assertEquals(targetSpan.end, targetOffset+13);
			
			sourceIndex = sourceOffset+14;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+13);
			Assert.assertEquals(targetSpan.end, targetOffset+14);
			
			sourceIndex = sourceOffset+15;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+13);
			Assert.assertEquals(targetSpan.end, targetOffset+14);
			
			sourceIndex = sourceOffset+16;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+13);
			Assert.assertEquals(targetSpan.end, targetOffset+15);
			
			sourceIndex = sourceOffset+17;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+15);
			Assert.assertEquals(targetSpan.end, targetOffset+16);
			
			sourceIndex = sourceOffset+18;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+16);
			Assert.assertEquals(targetSpan.end, targetOffset+17);
			
			sourceIndex = sourceOffset+19;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+17);
			Assert.assertEquals(targetSpan.end, targetOffset+22);
			
			sourceIndex = sourceOffset+20;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+22);
			Assert.assertEquals(targetSpan.end, targetOffset+23);
			
			sourceIndex = sourceOffset+21;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+24);
			Assert.assertEquals(targetSpan.end, targetOffset+25);
			
			sourceIndex = sourceOffset+22;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+24);
			Assert.assertEquals(targetSpan.end, targetOffset+25);
			
			sourceIndex = sourceOffset+23;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, Alignments.UNALIGNED);

			sourceIndex = sourceOffset+24;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+31);
			Assert.assertEquals(targetSpan.end, targetOffset+32);
			
			sourceIndex = sourceOffset+25;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+29);
			Assert.assertEquals(targetSpan.end, targetOffset+30);
			
			sourceIndex = sourceOffset+26;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+32);
			Assert.assertEquals(targetSpan.end, targetOffset+33);
			
			sourceIndex = sourceOffset+27;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+33);
			Assert.assertEquals(targetSpan.end, targetOffset+34);
			
			sourceIndex = sourceOffset+28;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+34);
			Assert.assertEquals(targetSpan.end, targetOffset+35);
			
			sourceIndex = sourceOffset+29;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+37);
			Assert.assertEquals(targetSpan.end, targetOffset+38);

			sourceIndex = sourceOffset+30;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+35);
			Assert.assertEquals(targetSpan.end, targetOffset+38);

			sourceIndex = sourceOffset+31;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+36);
			Assert.assertEquals(targetSpan.end, targetOffset+39);	

			sourceIndex = sourceOffset+32;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+1);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+39);
			Assert.assertEquals(targetSpan.end, targetOffset+40);
			
			//TODO Test the rest of the spans for this sentence here
			
			sourceIndex = sourceOffset+0;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+5);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+0);
			Assert.assertEquals(targetSpan.end, targetOffset+6);
			
			sourceIndex = sourceOffset+1;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+5);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+1);
			Assert.assertEquals(targetSpan.end, targetOffset+6);
			
			
			sourceIndex = sourceOffset+0;
			targetSpan = alignments.getAlignedTargetSpan(sourceIndex, sourceIndex+33);
			Assert.assertNotNull(targetSpan);
			Assert.assertEquals(targetSpan.start, targetOffset+0);
			Assert.assertEquals(targetSpan.end, targetOffset+40);
			
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
