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
		
		BLEU bleu = new BLEU();
		
		String ref = "this is the fourth chromosome whose sequence has been completed to date . it comprises more than 87 million pairs of dna .";
		String test = "this is the fourth chromosome to be fully sequenced up till now and it comprises of over 87 million pairs of deoxyribonucleic acid ( dna ) .";
		
		String[] refWords = ref.split("\\s+");
		String[][] refSentences = {refWords};
		EvaluationMetric.set_refSentences(refSentences);
		
		String[] testWords = test.split("\\s+");
		
		double actual = bleu.score(testWords);
		double expected = 0.2513;
		//double acceptableDelta = 0.0f;
		
		Assert.assertEquals(actual, expected);
		
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
