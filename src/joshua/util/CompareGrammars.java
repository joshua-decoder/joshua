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
import java.util.logging.Level;
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
	
	/**
	 * Gets a set containing all unique instances of the specified
	 * field.
	 *
	 * @param grammarFile File containing a grammar.
	 * @param fieldDelimiter Regular expression to split each
	 *                       line
	 * @param fieldNumber Field from each rule to extract
	 * @return set containing all unique instances of the
	 *         specified field
	 * @throws FileNotFoundException
	 */
	public static Set<String> getFields(File grammarFile, String fieldDelimiter, int fieldNumber) throws FileNotFoundException {
		
		Scanner grammarScanner = new Scanner(grammarFile);
		
		Set<String> set = new HashSet<String>();
				
		while (grammarScanner.hasNextLine()) {
		
			String line = grammarScanner.nextLine();
			
			String[] fields = line.split(fieldDelimiter);
			
			set.add(fields[fieldNumber]);
		}
		
		return set;
	}
	
	public static void compareValues(File grammarFile1, File grammarFile2, String fieldDelimiter, int fieldNumber, String scoresDelimiter, int scoresFieldNumber, float delta) throws FileNotFoundException {
		
		Scanner grammarScanner1 = new Scanner(grammarFile1);
		Scanner grammarScanner2 = new Scanner(grammarFile2);
		
		Set<String> set = new HashSet<String>();
				
		int counter = 0;
		float totalOverDiffs = 0.0f;
		while (grammarScanner1.hasNextLine() && grammarScanner2.hasNextLine()) {
		
			counter++;
			
			String line1 = grammarScanner1.nextLine();
			String[] fields1 = line1.split(fieldDelimiter);
			String[] scores1 = fields1[fieldNumber].split(scoresDelimiter);
			float score1 = Float.valueOf(scores1[scoresFieldNumber]);
			
			String line2 = grammarScanner2.nextLine();
			String[] fields2 = line2.split(fieldDelimiter);
			String[] scores2 = fields2[fieldNumber].split(scoresDelimiter);
			float score2 = Float.valueOf(scores2[scoresFieldNumber]);			
			
			if (fields1[0].endsWith(fields2[0]) && fields1[1].endsWith(fields2[1]) && fields1[1].endsWith(fields2[1])) {
			
				float diff1 = Math.abs(score1-score2);
				float diff2 = Math.abs(score2-score1);
				float diff = (diff1 < diff2) ? diff1 : diff2;
								
				if (diff > delta) {
					logger.fine("Line " + counter + ":  Score mismatch: " + score1 + " vs " + score2);
					set.add(line1);
					totalOverDiffs += diff;
				} else if (logger.isLoggable(Level.FINEST)) {
				    logger.finest("Line " + counter + ": Scores MATCH: " + score1 + " vs " + score2);
				}
				
			} else {
				throw new RuntimeException("Lines don't match: " + line1 + " and " + line2);
			}
		}
		
		if (set.isEmpty()) {
			logger.info("No score mismatches");
		} else {
			logger.warning("Number of mismatches: " + set.size() + " out of " + counter);
			logger.warning("Total mismatch logProb mass: " + totalOverDiffs + " (" + totalOverDiffs/set.size() + ") (" + totalOverDiffs/counter+")");
		}
		
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
		
		// Tell standard in and out to use UTF-8
		FormatUtil.useUTF8();
		logger.finest("Using UTF-8");
		
		logger.info("Comparing grammar files " + args[0] + " and " + args[1]);
		
		File grammarFile1 = new File(args[0]);
		File grammarFile2 = new File(args[1]);

		String fieldDelimiter = HieroFormatReader.getFieldDelimiter();
		
		boolean compareScores = true;
		
		// Compare left-hand sides
		{
			Set<String> leftHandSides1 = getFields(grammarFile1, fieldDelimiter, 0);
			Set<String> leftHandSides2 = getFields(grammarFile2, fieldDelimiter, 0);

			if (leftHandSides1.equals(leftHandSides2)) {
				logger.info("Grammar files have the same set of left-hand sides");
			} else {
				logger.warning("Grammar files have differing sets of left-hand sides");
				compareScores = false;
			}
		}
		
		// Compare source right-hand sides
		{
			Set<String> sourceRHSs1 = getFields(grammarFile1, fieldDelimiter, 1);
			Set<String> sourceRHSs2 = getFields(grammarFile2, fieldDelimiter, 1);

			if (sourceRHSs1.equals(sourceRHSs2)) {
				logger.info("Grammar files have the same set of source right-hand sides");
			} else {
				logger.warning("Grammar files have differing sets of source right-hand sides");
				compareScores = false;
			}
		}
		
		
		// Compare target right-hand sides
		{
			Set<String> targetRHSs1 = getFields(grammarFile1, fieldDelimiter, 2);
			Set<String> targetRHSs2 = getFields(grammarFile2, fieldDelimiter, 2);

			if (targetRHSs1.equals(targetRHSs2)) {
				logger.info("Grammar files have the same set of target right-hand sides");
			} else {
				logger.warning("Grammar files have differing sets of target right-hand sides");
				compareScores = false;
			}
		}
		
		// Compare translation probs
		if (compareScores) {
			float delta = 0.001f;
			compareValues(grammarFile1, grammarFile2, fieldDelimiter, 3, "\\s+", 0, delta);
			compareValues(grammarFile1, grammarFile2, fieldDelimiter, 3, "\\s+", 1, delta);
			compareValues(grammarFile1, grammarFile2, fieldDelimiter, 3, "\\s+", 2, delta);
			
		}
		
	}
	
	

}
