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

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class AlignmentGridTest {

	AlignmentGrid grid;
	
	@Test
	public void setup() throws IOException {

		grid = new AlignmentGrid("0-0 0-1 1-1 2-1 3-1 0-2 0-3 5-4 4-5 6-5 8-6 8-7 7-8 10-9 12-10 11-11 12-11 13-12 14-13 15-13 16-13 16-14 17-15 18-16 19-17 19-18 19-19 19-20 19-21 20-22 21-24 22-24 25-29 24-31 26-32 27-33 28-34 30-35 31-36 29-37 30-37 31-37 31-38 32-39");
		
	}


	@Test(dependsOnMethods={"setup"})
	public void testIndividualTargetPoints() {

		int[] targetPoints;

		targetPoints = grid.getTargetPoints(0, 1);
		Assert.assertEquals(targetPoints.length, 4);
		Assert.assertEquals(targetPoints[0], 0);
		Assert.assertEquals(targetPoints[1], 1);
		Assert.assertEquals(targetPoints[2], 2);
		Assert.assertEquals(targetPoints[3], 3);

		targetPoints = grid.getTargetPoints(1, 2);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 1);

		targetPoints = grid.getTargetPoints(2, 3);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 1);

		targetPoints = grid.getTargetPoints(3, 4);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 1);

		targetPoints = grid.getTargetPoints(4, 5);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 5);

		targetPoints = grid.getTargetPoints(5, 6);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 4);

		targetPoints = grid.getTargetPoints(6, 7);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 5);

		targetPoints = grid.getTargetPoints(7, 8);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 8);

		targetPoints = grid.getTargetPoints(8, 9);
		Assert.assertEquals(targetPoints.length, 2);
		Assert.assertEquals(targetPoints[0], 6);
		Assert.assertEquals(targetPoints[1], 7);

		targetPoints = grid.getTargetPoints(9, 10);
		Assert.assertEquals(targetPoints.length, 0);

		targetPoints = grid.getTargetPoints(10, 11);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 9);

		targetPoints = grid.getTargetPoints(11, 12);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 11);

		targetPoints = grid.getTargetPoints(12, 13);
		Assert.assertEquals(targetPoints.length, 2);
		Assert.assertEquals(targetPoints[0], 10);
		Assert.assertEquals(targetPoints[1], 11);

		targetPoints = grid.getTargetPoints(13, 14);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 12);

		targetPoints = grid.getTargetPoints(14, 15);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 13);

		targetPoints = grid.getTargetPoints(15, 16);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 13);

		targetPoints = grid.getTargetPoints(16, 17);
		Assert.assertEquals(targetPoints.length, 2);
		Assert.assertEquals(targetPoints[0], 13);
		Assert.assertEquals(targetPoints[1], 14);

		targetPoints = grid.getTargetPoints(17, 18);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 15);

		targetPoints = grid.getTargetPoints(18, 19);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 16);

		targetPoints = grid.getTargetPoints(19, 20);
		Assert.assertEquals(targetPoints.length, 5);
		Assert.assertEquals(targetPoints[0], 17);
		Assert.assertEquals(targetPoints[1], 18);
		Assert.assertEquals(targetPoints[2], 19);
		Assert.assertEquals(targetPoints[3], 20);
		Assert.assertEquals(targetPoints[4], 21);

		targetPoints = grid.getTargetPoints(20, 21);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 22);

		targetPoints = grid.getTargetPoints(21, 22);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 24);

		targetPoints = grid.getTargetPoints(22, 23);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 24);

		targetPoints = grid.getTargetPoints(23, 24);
		Assert.assertEquals(targetPoints.length, 0);

		targetPoints = grid.getTargetPoints(24, 25);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 31);

		targetPoints = grid.getTargetPoints(25, 26);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 29);

		targetPoints = grid.getTargetPoints(26, 27);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 32);

		targetPoints = grid.getTargetPoints(27, 28);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 33);

		targetPoints = grid.getTargetPoints(28, 29);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 34);

		targetPoints = grid.getTargetPoints(29, 30);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 37);

		targetPoints = grid.getTargetPoints(30, 31);
		Assert.assertEquals(targetPoints.length, 2);
		Assert.assertEquals(targetPoints[0], 35);
		Assert.assertEquals(targetPoints[1], 37);

		targetPoints = grid.getTargetPoints(31, 32);
		Assert.assertEquals(targetPoints.length, 3);
		Assert.assertEquals(targetPoints[0], 36);
		Assert.assertEquals(targetPoints[1], 37);
		Assert.assertEquals(targetPoints[2], 38);

		targetPoints = grid.getTargetPoints(32, 33);
		Assert.assertEquals(targetPoints.length, 1);
		Assert.assertEquals(targetPoints[0], 39);
	}

	//	@Test(dependsOnMethods={"setup"})
	//	public void testTargetPoints() {
	//
	//		int[] rowPoints;
	//		
	//		rowPoints = grid.getTargetPoints(0, 2);
	//		System.out.println(Arrays.toString(rowPoints));
	//		Assert.assertEquals(rowPoints.length, 4);
	//		Assert.assertEquals(rowPoints[0], 0);
	//		Assert.assertEquals(rowPoints[1], 1);
	//		Assert.assertEquals(rowPoints[2], 2);
	//		Assert.assertEquals(rowPoints[3], 3);
	//
	//		rowPoints = grid.getTargetPoints(1, 3);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 1);
	//
	//		rowPoints = grid.getTargetPoints(2, 4);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 1);
	//
	//		rowPoints = grid.getTargetPoints(3, 5);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 1);
	//		Assert.assertEquals(rowPoints[1], 5);
	//
	//		rowPoints = grid.getTargetPoints(4, 6);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 4);
	//		Assert.assertEquals(rowPoints[1], 5);
	//
	//		rowPoints = grid.getTargetPoints(5, 7);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 4);
	//		Assert.assertEquals(rowPoints[1], 5);
	//		
	//		rowPoints = grid.getTargetPoints(6, 8);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 5);
	//		Assert.assertEquals(rowPoints[1], 8);
	//		
	//		rowPoints = grid.getTargetPoints(7, 9);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 8);
	//
	//		rowPoints = grid.getTargetPoints(8, 10);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 6);
	//		Assert.assertEquals(rowPoints[1], 7);
	//
	//		rowPoints = grid.getTargetPoints(9, 11);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 9);
	//		
	//		rowPoints = grid.getTargetPoints(10, 12);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 9);
	//		Assert.assertEquals(rowPoints[1], 11);
	//
	//		rowPoints = grid.getTargetPoints(11, 13);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 10);
	//		Assert.assertEquals(rowPoints[1], 11);
	//
	//		rowPoints = grid.getTargetPoints(12, 14);
	//		Assert.assertEquals(rowPoints.length, 3);
	//		Assert.assertEquals(rowPoints[0], 10);
	//		Assert.assertEquals(rowPoints[1], 11);
	//		Assert.assertEquals(rowPoints[2], 12);
	//
	//		rowPoints = grid.getTargetPoints(13, 15);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 12);
	//		Assert.assertEquals(rowPoints[1], 13);
	//
	//		rowPoints = grid.getTargetPoints(14, 16);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 13);
	//
	//		rowPoints = grid.getTargetPoints(15, 17);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 13);
	//		Assert.assertEquals(rowPoints[1], 14);
	//
	//		rowPoints = grid.getTargetPoints(16, 18);
	//		Assert.assertEquals(rowPoints.length, 3);
	//		Assert.assertEquals(rowPoints[0], 13);
	//		Assert.assertEquals(rowPoints[1], 14);
	//		Assert.assertEquals(rowPoints[2], 15);
	//
	//		rowPoints = grid.getTargetPoints(17, 19);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 15);
	//		Assert.assertEquals(rowPoints[1], 16);
	//
	//		rowPoints = grid.getTargetPoints(18, 20);
	//		Assert.assertEquals(rowPoints.length, 6);
	//		Assert.assertEquals(rowPoints[0], 16);
	//		Assert.assertEquals(rowPoints[1], 17);
	//		Assert.assertEquals(rowPoints[2], 18);
	//		Assert.assertEquals(rowPoints[3], 19);
	//		Assert.assertEquals(rowPoints[4], 20);
	//		Assert.assertEquals(rowPoints[5], 21);
	//
	//
	//		rowPoints = grid.getTargetPoints(19, 21);
	//		Assert.assertEquals(rowPoints.length, 6);
	//		Assert.assertEquals(rowPoints[0], 17);
	//		Assert.assertEquals(rowPoints[1], 18);
	//		Assert.assertEquals(rowPoints[2], 19);
	//		Assert.assertEquals(rowPoints[3], 20);
	//		Assert.assertEquals(rowPoints[4], 21);
	//		Assert.assertEquals(rowPoints[5], 22);
	//
	//		rowPoints = grid.getTargetPoints(20, 22);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 22);
	//		Assert.assertEquals(rowPoints[1], 24);
	//
	//		rowPoints = grid.getTargetPoints(21, 23);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 24);
	//
	//		rowPoints = grid.getTargetPoints(22, 24);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 24);
	//
	//		rowPoints = grid.getTargetPoints(23, 25);
	//		Assert.assertEquals(rowPoints.length, 1);
	//		Assert.assertEquals(rowPoints[0], 31);
	//
	//		rowPoints = grid.getTargetPoints(24, 26);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 29);
	//		Assert.assertEquals(rowPoints[1], 31);
	//
	//		rowPoints = grid.getTargetPoints(25, 27);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 29);
	//		Assert.assertEquals(rowPoints[1], 32);
	//
	//		rowPoints = grid.getTargetPoints(26, 28);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 32);
	//		Assert.assertEquals(rowPoints[1], 33);
	//
	//		rowPoints = grid.getTargetPoints(27, 29);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 33);
	//		Assert.assertEquals(rowPoints[1], 34);
	//
	//		rowPoints = grid.getTargetPoints(28, 30);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 34);
	//		Assert.assertEquals(rowPoints[1], 37);
	//
	//		rowPoints = grid.getTargetPoints(29, 31);
	//		Assert.assertEquals(rowPoints.length, 2);
	//		Assert.assertEquals(rowPoints[0], 35);
	//		Assert.assertEquals(rowPoints[1], 37);
	//
	//		rowPoints = grid.getTargetPoints(30, 32);
	//		Assert.assertEquals(rowPoints.length, 4);
	//		Assert.assertEquals(rowPoints[0], 35);
	//		Assert.assertEquals(rowPoints[1], 36);
	//		Assert.assertEquals(rowPoints[2], 37);
	//		Assert.assertEquals(rowPoints[3], 38);
	//
	//		rowPoints = grid.getTargetPoints(31, 33);
	//		Assert.assertEquals(rowPoints.length, 4);
	//		Assert.assertEquals(rowPoints[0], 36);
	//		Assert.assertEquals(rowPoints[1], 37);
	//		Assert.assertEquals(rowPoints[2], 38);
	//		Assert.assertEquals(rowPoints[3], 39);
	//	}

}
