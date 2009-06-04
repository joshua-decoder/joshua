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
package joshua.corpus.lexprob;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Ant task to compare two human-readable lexical probability tables.
 * 
 * @author Lane Schwartz
 */
public class CompareLexProbs {

	private String first, second;
	private float delta = 0.0001f;
	
	public void setDelta(float delta) {
		this.delta = delta;
	}
	
	public void setFirst(String file) {
		this.first = file;
	}
	
	public void setSecond(String file) {
		this.second = file;
	}
	
	public void execute() throws FileNotFoundException {
		
		Scanner firstScanner = new Scanner(new File(first));
		Scanner secondScanner = new Scanner(new File(second));	
		
		boolean match = false;
		for (int lineNumber=1; 
				firstScanner.hasNextLine() && secondScanner.hasNextLine(); 
				lineNumber++) {

			String firstLine = firstScanner.nextLine().trim();
			String secondLine = secondScanner.nextLine().trim();

			String[] firstParts = firstLine.split("\\s+");
			String[] secondParts = secondLine.split("\\s+");

			if (firstParts.length==secondParts.length &&
					firstParts.length==4) {

				if (firstParts[0].equals(secondParts[0])) {
					if (firstParts[1].equals(secondParts[1])) {

						float firstReverseLexProb = Float.valueOf(firstParts[2]);
						float secondReverseLexProb = Float.valueOf(secondParts[2]);

						float firstLexProb = Float.valueOf(firstParts[3]);
						float secondLexProb = Float.valueOf(secondParts[3]);

						if (Math.abs(firstReverseLexProb-secondReverseLexProb) < delta &&
								Math.abs(firstLexProb-secondLexProb) < delta ) {
							match = true;
						}
					}
				}

			}

			if (! match) {
				System.out.println("Line " + lineNumber + " does not match: \n" +
						firstLine + "\n" + secondLine);
				break;
			}


		}
		
		if (match) {
			System.out.println("Files " + first + " and " + second + " match");
		}

	}
	
}
