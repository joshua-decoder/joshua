/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package joshua.sarray;

import java.io.IOException;

import joshua.sarray.AlignmentArray;
import joshua.sarray.CorpusArray;
import joshua.sarray.SuffixArray;
import joshua.sarray.SuffixArrayFactory;
import joshua.util.sentence.Span;
import joshua.util.sentence.Vocabulary;


import org.testng.Assert;
import org.testng.annotations.Test;



/**
 * Unit tests for PrefixTree.Node
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class AlignmentArrayTest {

	private SuffixArray sourceCorpus;
	private SuffixArray targetCorpus;
	private AlignmentArray alignmentArray;
	
	@Test
	public void setup() throws IOException {
		
		String sourceFilename = "data/tiny.fr";
		String targetFilename = "data/tiny.en";
		String alignmentsFilename = "data/tiny.fr-en.alignment";
		
		Vocabulary sourceVocab = new Vocabulary();
		int[] numberOfSourceWordsAndSentences = SuffixArrayFactory.createVocabulary(sourceFilename, sourceVocab);
		CorpusArray sourceArray = SuffixArrayFactory.createCorpusArray(sourceFilename, sourceVocab, numberOfSourceWordsAndSentences[0], numberOfSourceWordsAndSentences[1]);
		sourceCorpus = SuffixArrayFactory.createSuffixArray(sourceArray);//SuffixArrayFactory.loadSuffixArray(sourceLang, corpusName, directory);
		
		Vocabulary targetVocab = new Vocabulary();
		int[] numberOfTargetWordsAndSentences = SuffixArrayFactory.createVocabulary(targetFilename, targetVocab);
		CorpusArray targetArray = SuffixArrayFactory.createCorpusArray(targetFilename, targetVocab, numberOfTargetWordsAndSentences[0], numberOfTargetWordsAndSentences[1]);
		targetCorpus = SuffixArrayFactory.createSuffixArray(targetArray);//SuffixArrayFactory.loadSuffixArray(targetLang, corpusName, directory);
		
		alignmentArray = (AlignmentArray) SuffixArrayFactory.createAlignmentArray(alignmentsFilename, sourceCorpus, targetCorpus); //SuffixArrayFactory.loadAlignmentArray(sourceLang, targetLang, corpusName, directory);
		
	}
	
	@Test(dependsOnMethods={"setup"})
	public void test() {
		Assert.assertEquals(alignmentArray.getAlignedTargetSpan(0, 1),new Span(0,1));
		Assert.assertEquals(alignmentArray.getAlignedTargetSpan(1, 2),new Span(0,2));
		Assert.assertEquals(alignmentArray.getAlignedTargetSpan(2, 3),new Span(0,3));
		Assert.assertEquals(alignmentArray.getAlignedTargetSpan(3, 4),new Span(0,4));
		
		Assert.assertEquals(alignmentArray.getAlignedSourceSpan(0, 1),new Span(0,4));
		Assert.assertEquals(alignmentArray.getAlignedSourceSpan(1, 2),new Span(1,2));
		Assert.assertEquals(alignmentArray.getAlignedSourceSpan(2, 3),new Span(2,3));
		Assert.assertEquals(alignmentArray.getAlignedSourceSpan(3, 4),new Span(3,4));
	}
	/*
	@Test(dependsOnMethods={"setup"})
	public void testAll() {
		// ccb - debugging
		for(int i = 0; i < sourceCorpus.getNumWords(); i++) {
			for(int j = i+1; j <= sourceCorpus.getNumWords(); j++) {
				//int[] alignedTargetWords = alignmentArray.getAlignedTargetSpan(i, j);
				Span alignedTargetWords = alignmentArray.getAlignedTargetSpan(i, j);
				if(alignmentArray.hasConsistentAlignment(i, j)) {
					//System.out.println(sourceCorpus.getPhrase(i, j) + " [" + i + "," + j + "]\t" + targetCorpus.getPhrase(alignedTargetWords.start, alignedTargetWords.end)  + " [" + alignedTargetWords.start + "," + (alignedTargetWords.end) + "]");
				}
			}
		}

	}
	*/
}
