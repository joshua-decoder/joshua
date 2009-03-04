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
package joshua.ZMERT;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

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
		BLEU bleu = new BLEU();
		
		Assert.assertEquals(bleu.get_metricName(), "BLEU");
	}
	
	@Test
	public void defaultConstructor() {
		
		BLEU bleu = new BLEU();
		
		// Default constructor should use a maximum n-gram length of 4
		Assert.assertEquals(bleu.maxGramLength, 4);
		
		// Default constructor should use the closest reference
		Assert.assertEquals(bleu.effLengthMethod, 1);

	}
	
	
	@Test
	public void simpleTest() {

		String ref = "this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .";
		String test = "this is the fourth chromosome to be fully sequenced up till now and it comprises of over 87 million pairs of deoxyribonucleic acid ( dna ) .";
		
/*		// OZ
		String[] refWords = ref.split("\\s+");
		String[][] refSentences = {refWords};
*/		
		// refSentences[i][r] stores the r'th reference of the i'th sentence
		String[][] refSentences = new String[1][1]; // OZ
		refSentences[0][0] = ref; // OZ
		
		EvaluationMetric.set_numSentences(1); // OZ
		EvaluationMetric.set_refsPerSen(1); // OZ
		EvaluationMetric.set_refSentences(refSentences);
			// set_refSentences expects a 2-D array because there could be
			// multiple references per sentence
		
		BLEU bleu = new BLEU();
		
/*		// OZ
		String[] testWords = test.split("\\s+");
*/		
		// testSentences[i] stores the candidate translation for the i'th sentence
		String[] testSentences = new String[1]; // OZ
		testSentences[0] = test; // OZ
		
		double actual = bleu.score(testSentences);
			// score expexts a 1-D array because the test corpus
			// could consist of multiple sentences
		double expected = 0.2513;
		double acceptableDelta = 0.00001f;
		
		Assert.assertEquals(actual, expected, acceptableDelta);
		
	}
	
	@Parameters({"referenceFile","testFile"})
	@Test
	public void fileTest(String referenceFile, String testFile) throws FileNotFoundException {

		//TODO You can now read in the files, and do something useful with them.
		
		Scanner refScanner = new Scanner(new File(referenceFile));
		
		while (refScanner.hasNextLine()) {
			System.out.println(refScanner.nextLine());
		}
		
		System.out.println(referenceFile);
		System.out.println(testFile);
	}
	
}
