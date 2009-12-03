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
package joshua.regression;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joshua.util.JoshuaEval;

/**
 * Calculates the change in BLEU score between
 * two translation results.
 *
 * @author Lane Schwartz
 */
public class BleuRegression {

	/** Logger for this class. */
	public static Logger logger =
		Logger.getLogger(BleuRegression.class.getName());
	
	public static float getBleuResults(File results) throws FileNotFoundException {
		Scanner scanner = new Scanner(results);
	
		Pattern pattern = Pattern.compile("^BLEU = (.*)");
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				return Float.valueOf(matcher.group(1)) * 100;
			}
		}
		
		throw new RuntimeException();
	}
	
	public static float test(String priorResultsFile, String newResultsFile, String referencesFile, int refsPerSentence) throws IOException {
		
		PrintStream originalOut = System.out;
		
		File priorResults = File.createTempFile("prior", "bleu"); {
			PrintStream out = new PrintStream(priorResults);
			System.setOut(out);
			String[] args = {
					"-cand", priorResultsFile,
					"-format", "nbest",
					"-ref", referencesFile,
					"-rps", String.valueOf(refsPerSentence)
			};
			JoshuaEval.main(args);
			out.flush();
			out.close();
		}
		float priorBleu = getBleuResults(priorResults);
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Previous BLEU score: " + priorBleu);
		
		
		File newResults = File.createTempFile("current", "bleu"); {
			PrintStream out = new PrintStream(newResults);
			System.setOut(out);
			String[] args = {
					"-cand", newResultsFile,
					"-format", "nbest",
					"-ref", referencesFile,
					"-rps", String.valueOf(refsPerSentence)
			};
			JoshuaEval.main(args);
			out.flush();
			out.close();
		}
		float newBleu = getBleuResults(newResults);
		if (logger.isLoggable(Level.FINE)) logger.fine("Current BLEU score: " + newBleu);
				
		
		System.setOut(originalOut);
		
		return newBleu - priorBleu;
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		
		if (args.length != 4) {
			System.err.println("Need four arguments");
		} else {

			float results = test(args[0], args[1], args[2], Integer.valueOf(args[3]));

			if (logger.isLoggable(Level.INFO)) logger.info("BLEU score change: " + results);
			
		}
	}
	
}
