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
package joshua.zmert;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import joshua.zmert.BLEU;
import joshua.zmert.EvaluationMetric;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Unit tests for BLEU class.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class BLEUTest {

	@Test
	public void metricName() {

		// Setup the EvaluationMetric class
		EvaluationMetric.set_numSentences(0);
		EvaluationMetric.set_refsPerSen(1);
		EvaluationMetric.set_refSentences(null);

		BLEU bleu = new BLEU();
		
		Assert.assertEquals(bleu.get_metricName(), "BLEU");

	}
	
	@Test
	public void defaultConstructor() {

		// Setup the EvaluationMetric class
		EvaluationMetric.set_numSentences(0);
		EvaluationMetric.set_refsPerSen(1);
		EvaluationMetric.set_refSentences(null);

		BLEU bleu = new BLEU();
		
		// Default constructor should use a maximum n-gram length of 4
		Assert.assertEquals(bleu.maxGramLength, 4);
		
		// Default constructor should use the closest reference
		Assert.assertEquals(bleu.effLengthMethod, BLEU.EffectiveLengthMethod.CLOSEST);

	}
	
	@Test
	public void simpleTest() {

		String ref = "this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .";
		String test = "this is the fourth chromosome to be fully sequenced up till now and it comprises of over 87 million pairs of deoxyribonucleic acid ( dna ) .";
		
		// refSentences[i][r] stores the r'th reference of the i'th sentence
		String[][] refSentences = new String[1][1];
		refSentences[0][0] = ref;
		
		EvaluationMetric.set_numSentences(1);
		EvaluationMetric.set_refsPerSen(1);
		EvaluationMetric.set_refSentences(refSentences);
		
		BLEU bleu = new BLEU();
		
		// testSentences[i] stores the candidate translation for the i'th sentence
		String[] testSentences = new String[1];
		testSentences[0] = test;
		try {
			// Check BLEU score matches
			double actualScore = bleu.score(testSentences);
			double expectedScore = 0.2513;
			double acceptableScoreDelta = 0.00001f;

			Assert.assertEquals(actualScore, expectedScore, acceptableScoreDelta);

			// Check sufficient statistics match
			int[] actualSS = bleu.suffStats(testSentences);
			int[] expectedSS = {1,14,8,5,3,27,23};

			Assert.assertEquals(actualSS[0], expectedSS[0], 0); // # sentences
			Assert.assertEquals(actualSS[1], expectedSS[1], 0); // 1-gram matches
			Assert.assertEquals(actualSS[2], expectedSS[2], 0); // 2-gram matches
			Assert.assertEquals(actualSS[3], expectedSS[3], 0); // 3-gram matches
			Assert.assertEquals(actualSS[4], expectedSS[4], 0); // 4-gram matches
			Assert.assertEquals(actualSS[5], expectedSS[5], 0); // candidate length
			Assert.assertEquals(actualSS[6], expectedSS[6], 0); // reference length
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@Parameters({"referenceFile","testFile"})
	@Test
	public void fileTest(String referenceFile, String testFile) throws FileNotFoundException {

		//TODO You can now read in the files, and do something useful with them.
		
		Scanner refScanner = new Scanner(new File(referenceFile));
		
		while (refScanner.hasNextLine()) {
			
			String refLine = refScanner.nextLine();
			
		}
	

	}
	
}
