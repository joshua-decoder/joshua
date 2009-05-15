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
package joshua.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import joshua.decoder.ff.tm.hiero.HieroFormatReader;

/**
 * This class allows two grammars (loaded from disk) to be compared.
 * 
 * @author Lane Schwartz
 */
public class CompareGrammars {

	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(CompareGrammars.class.getName());
	
	public static Set<String> getFields(File grammarFile, int fieldNumber) throws FileNotFoundException {
		
		Scanner grammarScanner = new Scanner(grammarFile);
		
		Set<String> set = new HashSet<String>();
		
		String fieldDelimiter = HieroFormatReader.getFieldDelimiter();
		
		while (grammarScanner.hasNextLine()) {
		
			String line = grammarScanner.nextLine();
			
			String[] fields = line.split(fieldDelimiter);
			
			set.add(fields[fieldNumber]);
		}
		
		return set;
	}
	
	/**
	 * Main method.
	 * 
	 * @param args names of the two grammars to be compared
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		if (args.length != 2) {
			logger.severe("Usage: " + CompareGrammars.class.toString() + " grammarFile1 grammarFile2");
			System.exit(-1);
		}
		
		logger.info("Comparing grammar files " + args[0] + " and " + args[1]);
		
		File grammarFile1 = new File(args[0]);
		File grammarFile2 = new File(args[1]);

		// Compare left-hand sides
		{
			Set<String> leftHandSides1 = getFields(grammarFile1, 0);
			Set<String> leftHandSides2 = getFields(grammarFile2, 0);

			if (leftHandSides1.equals(leftHandSides2)) {
				logger.info("Grammar files have the same set of left-hand sides");
			} else {
				logger.warning("Grammar files have differing sets of left-hand sides");
			}
		}
		
		// Compare source right-hand sides
		{
			Set<String> sourceRHSs1 = getFields(grammarFile1, 1);
			Set<String> sourceRHSs2 = getFields(grammarFile2, 1);

			if (sourceRHSs1.equals(sourceRHSs2)) {
				logger.info("Grammar files have the same set of source right-hand sides");
			} else {
				logger.warning("Grammar files have differing sets of source right-hand sides");
			}
		}
		
		
		// Compare target right-hand sides
		{
			Set<String> targetRHSs1 = getFields(grammarFile1, 2);
			Set<String> targetRHSs2 = getFields(grammarFile2, 2);

			if (targetRHSs1.equals(targetRHSs2)) {
				logger.info("Grammar files have the same set of target right-hand sides");
			} else {
				logger.warning("Grammar files have differing sets of target right-hand sides");
			}
		}
		
		
	}

}
