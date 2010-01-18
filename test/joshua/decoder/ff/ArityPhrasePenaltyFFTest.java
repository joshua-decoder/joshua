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
package joshua.decoder.ff;

import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.MonolingualRule;
import joshua.decoder.ff.tm.Rule;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for ArityPhrasePenaltyFF.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class ArityPhrasePenaltyFFTest {

	@Test
	public void alpha() {
		Assert.assertEquals(ArityPhrasePenaltyFF.ALPHA, - Math.log10(Math.E));
	}
	
	@Test
	public void estimate() {
		
		int featureID = 0;
		double weight = 0.0;
		int owner = MonolingualRule.DUMMY_OWNER;
		int min = 1;
		int max = 5;
		
		ArityPhrasePenaltyFF featureFunction = new ArityPhrasePenaltyFF(featureID, weight, owner, min, max);
		
		int lhs = -1;
		int[] sourceRHS = {24, -1, 42, 738};
		int[] targetRHS = {-1, 7, 8};
		float[] featureScores = {-2.35f, -1.78f, -0.52f};
		int arity = 1;
		
		Rule dummyRule = new BilingualRule(lhs, sourceRHS, targetRHS, featureScores, arity);
		
		Assert.assertEquals(featureFunction.estimateLogP(dummyRule, -1), ArityPhrasePenaltyFF.ALPHA);
		
	}
	
}
