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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import joshua.util.FormatUtil;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Front to end tests to extract rules from a sample corpus using suffix arrays.
 *
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class ExtractRulesTest {

	// The following ruby one-liner can convert a Hiero grammar 
	//     into useful Java assertions for unit testing:
	//
	// cat hiero.grammar | sort | ruby -e 'STDIN.each_line{ |line| words=line.split(" ||| "); puts "verifyLine(lines.get(n++), \"#{words[0]}\", \"#{words[1]}\", \"#{words[2]}\");"; }' > java-rules.txt
	
	String sourceFileName;
	String targetFileName;
	String alignmentFileName;
	
	// ä == \u00E4
	// ü == \u00FC
	
	@Test
	public void setup() throws IOException {
		
		// Tell System.out and System.err to use UTF8
		FormatUtil.useUTF8();
				
		String sourceCorpusString = 
			"it makes him and it mars him , it sets him on yet it takes him off .";
		
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile, "UTF-8");
			sourcePrintStream.println(sourceCorpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}
	
		String targetCorpusString = 
			"das macht ihn und es besch\u00E4digt ihn , es setzt ihn auf und es f\u00FChrt ihn aus .";
		
		
		{
			File targetFile = File.createTempFile("target", new Date().toString());
			PrintWriter targetPrintStream = new PrintWriter(targetFile, "UTF-8");
//			PrintStream targetPrintStream = new PrintStream(targetFile, "UTF-8");
			targetPrintStream.println(targetCorpusString);
			targetPrintStream.close();
			targetFileName = targetFile.getAbsolutePath();
		}
		
		String alignmentString = 
			"0-0 1-1 2-2 3-3 4-4 5-5 6-6 7-7 8-8 9-9 10-10 11-11 12-12 13-13 14-14 15-15 16-16 17-17";
		
		{
			File alignmentFile = File.createTempFile("alignment", new Date().toString());
			PrintStream alignmentPrintStream = new PrintStream(alignmentFile);
			alignmentPrintStream.println(alignmentString);
			alignmentPrintStream.close();
			alignmentFileName = alignmentFile.getAbsolutePath();
		}
		
	}

	/**
	 * Extracts rules and returns the file name where the extracted rules are stored.
	 * 
	 * @param testCorpusString
	 * @param sentenceInitialX TODO
	 * @param sentenceFinalX TODO
	 * @return
	 * @throws IOException 
	 */
	private List<String> extractRules(String testCorpusString, boolean sentenceInitialX, boolean sentenceFinalX) throws IOException {
		
		String testFileName;
		{
			File testFile = File.createTempFile("test", new Date().toString());
			PrintStream testPrintStream = new PrintStream(testFile, "UTF-8");
			testPrintStream.println(testCorpusString);
			testPrintStream.close();
			testFileName = testFile.getAbsolutePath();
		}
		
		// Filename of the extracted rules file.
		String rulesFileName;
		{
			File rulesFile = File.createTempFile("rules", new Date().toString());
			rulesFileName = rulesFile.getAbsolutePath();
		}
		
		String[] args = {
				"--sentence-initial-X="+sentenceInitialX,
				"--sentence-final-X="+sentenceFinalX,
				"--maxPhraseLength=5",
				"--source="+sourceFileName,
				"--target="+targetFileName,
				"--alignments="+alignmentFileName,
				"--test="+testFileName,
				"--output="+rulesFileName
		};
		
		ExtractRules.main(args);
		
		
		Scanner scanner = new Scanner(new File(rulesFileName));
		
		Set<String> lineSet = new HashSet<String>();
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			lineSet.add(line);
			
		}
		
		List<String> lines = new ArrayList<String>(lineSet);
		Collections.sort(lines);
		
		return lines;
	}
	
	public void verifyLine(String line, String lhs, String sourceRHS, String targetRHS)  {
		String[] part = line.split(" \\|\\|\\| ");
		Assert.assertEquals(part[0], lhs);
		Assert.assertEquals(part[1], sourceRHS);
		Assert.assertEquals(part[2], targetRHS);
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet1() throws IOException {
		
		List<String> lines = extractRules("it", false, false);
		
		Assert.assertEquals(lines.size(), 2);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		
//		From Hiero:
//		[X] ||| [X,1] it ||| [X,1] es ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| es [X,1] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| das [X,1] ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| [X,1] it [X,2] ||| [X,1] es [X,2] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it ||| das ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| it ||| es ||| 0.124938733876 -0.0 0.124938733876

	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet1Expanded() throws IOException {
		
		List<String> lines = extractRules("it", true, true);
		
		Assert.assertEquals(lines.size(), 6);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2]", "[X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it", "[X,1] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "das [X,1]");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "es [X,1]");
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		
//		From Hiero:
//		[X] ||| [X,1] it ||| [X,1] es ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| es [X,1] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| das [X,1] ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| [X,1] it [X,2] ||| [X,1] es [X,2] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it ||| das ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| it ||| es ||| 0.124938733876 -0.0 0.124938733876

	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet2() throws IOException {
		
		List<String> lines = extractRules("it makes", false, false);
		
		Assert.assertEquals(lines.size(), 6);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "das [X,1]");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "es [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes", "das macht");
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		verifyLine(lines.get(n++), "[X]", "makes", "macht");
		
		//From Hiero:
//		[X] ||| [X,1] it ||| [X,1] es ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| es [X,1] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| das [X,1] ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| [X,1] it [X,2] ||| [X,1] es [X,2] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it ||| das ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| it ||| es ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| makes ||| macht ||| -0.0 -0.0 -0.0
//		[X] ||| makes [X,1] ||| macht [X,1] ||| -0.0 -0.0 -0.0
//		[X] ||| it makes [X,1] ||| das macht [X,1] ||| -0.0 -0.0 0.60206001997
//		[X] ||| it makes ||| das macht ||| -0.0 -0.0 0.60206001997

	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet3() throws IOException {
		
		List<String> lines = extractRules("it makes him", false, false);
		
		Assert.assertEquals(lines.size(), 23-9);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", "[X,1] him", "[X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him", "ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him", "das [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him", "es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "das [X,1]");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "es [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1]", "das macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes him", "das macht ihn");
		verifyLine(lines.get(n++), "[X]", "it makes", "das macht");
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1]", "macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him", "macht ihn");
		verifyLine(lines.get(n++), "[X]", "makes", "macht");
		
//		From Hiero:
//		[X] ||| [X,1] him [X,2] ||| [X,1] ihn [X,2] ||| -0.0 -0.0 -0.0
//		[X] ||| [X,1] him ||| [X,1] ihn ||| -0.0 -0.0 -0.0
//		[X] ||| [X,1] it [X,2] him ||| [X,1] es [X,2] ihn ||| 0.176091253757 -0.0 0.124938733876
//		[X] ||| [X,1] it [X,2] ||| [X,1] es [X,2] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| [X,1] it ||| [X,1] es ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| him [X,1] ||| ihn [X,1] ||| -0.0 -0.0 -0.0
//		[X] ||| him ||| ihn ||| -0.0 -0.0 -0.0
//		[X] ||| it [X,1] him [X,2] ||| das [X,1] ihn [X,2] ||| 0.477121263742 -0.0 0.60206001997
//		[X] ||| it [X,1] him [X,2] ||| es [X,1] ihn [X,2] ||| 0.176091253757 -0.0 0.124938733876
//		[X] ||| it [X,1] him ||| das [X,1] ihn ||| 0.477121263742 -0.0 0.60206001997
//		[X] ||| it [X,1] him ||| es [X,1] ihn ||| 0.176091253757 -0.0 0.124938733876
//		[X] ||| it [X,1] ||| das [X,1] ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| it [X,1] ||| es [X,1] ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| it makes [X,1] ||| das macht [X,1] ||| -0.0 -0.0 0.60206001997
//		[X] ||| it makes him [X,1] ||| das macht ihn [X,1] ||| -0.0 -0.0 0.60206001997
//		[X] ||| it makes him ||| das macht ihn ||| -0.0 -0.0 0.60206001997
//		[X] ||| it makes ||| das macht ||| -0.0 -0.0 0.60206001997
//		[X] ||| it ||| das ||| 0.60206001997 -0.0 0.60206001997
//		[X] ||| it ||| es ||| 0.124938733876 -0.0 0.124938733876
//		[X] ||| makes [X,1] ||| macht [X,1] ||| -0.0 -0.0 -0.0
//		[X] ||| makes him [X,1] ||| macht ihn [X,1] ||| -0.0 -0.0 -0.0
//		[X] ||| makes him ||| macht ihn ||| -0.0 -0.0 -0.0
//		[X] ||| makes ||| macht ||| -0.0 -0.0 -0.0

	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet18() throws IOException {
		
		List<String> lines = extractRules("it makes him and it mars him , it sets him on yet it takes him off .", false, false);
		
		
		Assert.assertEquals(lines.size(), 922);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", ", [X,1] him [X,2] him", ", [X,1] ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him [X,2] it", ", [X,1] ihn [X,2] es");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him [X,2] off", ", [X,1] ihn [X,2] aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him [X,2] takes", ", [X,1] ihn [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him [X,2]", ", [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him off", ", [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him on [X,2]", ", [X,1] ihn auf [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him on yet", ", [X,1] ihn auf und");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him on", ", [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", ", [X,1] him", ", [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", ", [X,1] it [X,2] off", ", [X,1] es [X,2] aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] it [X,2]", ", [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] it takes [X,2]", ", [X,1] es f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] it takes him", ", [X,1] es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", ", [X,1] it takes", ", [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", [X,1] it", ", [X,1] es");
		verifyLine(lines.get(n++), "[X]", ", [X,1] off", ", [X,1] aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on [X,2] him", ", [X,1] auf [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on [X,2] off", ", [X,1] auf [X,2] aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on [X,2] takes", ", [X,1] auf [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on [X,2]", ", [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on yet [X,2]", ", [X,1] auf und [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on yet it", ", [X,1] auf und es");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on yet", ", [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", ", [X,1] on", ", [X,1] auf");
		verifyLine(lines.get(n++), "[X]", ", [X,1] takes [X,2]", ", [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] takes him off", ", [X,1] f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] takes him", ", [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", ", [X,1] takes", ", [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet [X,2] him", ", [X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet [X,2] off", ", [X,1] und [X,2] aus");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet [X,2]", ", [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet it [X,2]", ", [X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet it takes", ", [X,1] und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet it", ", [X,1] und es");
		verifyLine(lines.get(n++), "[X]", ", [X,1] yet", ", [X,1] und");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", [X,1]");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] him off", ", es [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] him", ", es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] it [X,2]", ", es [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] it takes", ", es [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] it", ", es [X,1] es");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] off", ", es [X,1] aus");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] on [X,2]", ", es [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] on yet", ", es [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] on", ", es [X,1] auf");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] takes [X,2]", ", es [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] takes him", ", es [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] takes", ", es [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] yet [X,2]", ", es [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] yet it", ", es [X,1] und es");
		verifyLine(lines.get(n++), "[X]", ", it [X,1] yet", ", es [X,1] und");
		verifyLine(lines.get(n++), "[X]", ", it [X,1]", ", es [X,1]");
		verifyLine(lines.get(n++), "[X]", ", it sets [X,1] him", ", es setzt [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", ", it sets [X,1] it", ", es setzt [X,1] es");
		verifyLine(lines.get(n++), "[X]", ", it sets [X,1] off", ", es setzt [X,1] aus");
		verifyLine(lines.get(n++), "[X]", ", it sets [X,1] takes", ", es setzt [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", ", it sets [X,1] yet", ", es setzt [X,1] und");
		verifyLine(lines.get(n++), "[X]", ", it sets [X,1]", ", es setzt [X,1]");
		verifyLine(lines.get(n++), "[X]", ", it sets him [X,1]", ", es setzt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", ", it sets him on", ", es setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", ", it sets him", ", es setzt ihn");
		verifyLine(lines.get(n++), "[X]", ", it sets", ", es setzt");
		verifyLine(lines.get(n++), "[X]", ", it", ", es");
		verifyLine(lines.get(n++), "[X]", ",", ",");
		verifyLine(lines.get(n++), "[X]", ".", ".");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] him on", "[X,1] , [X,2] ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] him", "[X,1] , [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] it takes", "[X,1] , [X,2] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] it", "[X,1] , [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] on yet", "[X,1] , [X,2] auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] on", "[X,1] , [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] takes", "[X,1] , [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] yet it", "[X,1] , [X,2] und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] yet", "[X,1] , [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it [X,2] it", "[X,1] , es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it [X,2] on", "[X,1] , es [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it [X,2] takes", "[X,1] , es [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it [X,2] yet", "[X,1] , es [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it [X,2]", "[X,1] , es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it sets [X,2]", "[X,1] , es setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it sets him", "[X,1] , es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it sets", "[X,1] , es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] , it", "[X,1] , es");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] .", "[X,1] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] , it", "[X,1] und [X,2] , es");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] ,", "[X,1] und [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] him ,", "[X,1] und [X,2] ihn ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] him", "[X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] it sets", "[X,1] und [X,2] es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] it", "[X,1] und [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] sets him", "[X,1] und [X,2] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2] sets", "[X,1] und [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] and [X,2]", "[X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it [X,2] ,", "[X,1] und es [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it [X,2] him", "[X,1] und es [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it [X,2] it", "[X,1] und es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it [X,2] sets", "[X,1] und es [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it [X,2]", "[X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it mars [X,2]", "[X,1] und es besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it mars him", "[X,1] und es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it mars", "[X,1] und es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "[X,1] and it", "[X,1] und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] and", "[X,1] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , [X,2] him", "[X,1] ihn , [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , [X,2] it", "[X,1] ihn , [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , [X,2] on", "[X,1] ihn , [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , [X,2] yet", "[X,1] ihn , [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , [X,2]", "[X,1] ihn , [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , it [X,2]", "[X,1] ihn , es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , it sets", "[X,1] ihn , es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him , it", "[X,1] ihn , es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him ,", "[X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] , it", "[X,1] ihn [X,2] , es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] ,", "[X,1] ihn [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] .", "[X,1] ihn [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] him ,", "[X,1] ihn [X,2] ihn ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] him off", "[X,1] ihn [X,2] ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] him on", "[X,1] ihn [X,2] ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] him", "[X,1] ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] it sets", "[X,1] ihn [X,2] es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] it takes", "[X,1] ihn [X,2] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] it", "[X,1] ihn [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] mars him", "[X,1] ihn [X,2] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] mars", "[X,1] ihn [X,2] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] off .", "[X,1] ihn [X,2] aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] off", "[X,1] ihn [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] on yet", "[X,1] ihn [X,2] auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] on", "[X,1] ihn [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] sets him", "[X,1] ihn [X,2] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] sets", "[X,1] ihn [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] takes him", "[X,1] ihn [X,2] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] takes", "[X,1] ihn [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] yet it", "[X,1] ihn [X,2] und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2] yet", "[X,1] ihn [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2]", "[X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and [X,2] ,", "[X,1] ihn und [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and [X,2] him", "[X,1] ihn und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and [X,2] it", "[X,1] ihn und [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and [X,2] sets", "[X,1] ihn und [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and [X,2]", "[X,1] ihn und [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and it [X,2]", "[X,1] ihn und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and it mars", "[X,1] ihn und es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and it", "[X,1] ihn und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him and", "[X,1] ihn und");
		verifyLine(lines.get(n++), "[X]", "[X,1] him off .", "[X,1] ihn aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] him off", "[X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on [X,2] .", "[X,1] ihn auf [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on [X,2] him", "[X,1] ihn auf [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on [X,2] off", "[X,1] ihn auf [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on [X,2] takes", "[X,1] ihn auf [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on [X,2]", "[X,1] ihn auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on yet [X,2]", "[X,1] ihn auf und [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on yet it", "[X,1] ihn auf und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on yet", "[X,1] ihn auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] him on", "[X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] him", "[X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] , it", "[X,1] es [X,2] , es");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] ,", "[X,1] es [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] .", "[X,1] es [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] him on", "[X,1] es [X,2] ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] him", "[X,1] es [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] it sets", "[X,1] es [X,2] es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] it takes", "[X,1] es [X,2] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] it", "[X,1] es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] off .", "[X,1] es [X,2] aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] off", "[X,1] es [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] on yet", "[X,1] es [X,2] auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] on", "[X,1] es [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] sets him", "[X,1] es [X,2] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] sets", "[X,1] es [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] takes him", "[X,1] es [X,2] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] takes", "[X,1] es [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] yet it", "[X,1] es [X,2] und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] yet", "[X,1] es [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2]", "[X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars [X,2] him", "[X,1] es besch\u00E4digt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars [X,2] it", "[X,1] es besch\u00E4digt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars [X,2] on", "[X,1] es besch\u00E4digt [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars [X,2] sets", "[X,1] es besch\u00E4digt [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars [X,2]", "[X,1] es besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars him ,", "[X,1] es besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars him [X,2]", "[X,1] es besch\u00E4digt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars him", "[X,1] es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it mars", "[X,1] es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets [X,2] him", "[X,1] es setzt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets [X,2] it", "[X,1] es setzt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets [X,2] takes", "[X,1] es setzt [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets [X,2] yet", "[X,1] es setzt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets [X,2]", "[X,1] es setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets him [X,2]", "[X,1] es setzt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets him on", "[X,1] es setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets him", "[X,1] es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it sets", "[X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it takes [X,2] .", "[X,1] es f\u00FChrt [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] it takes [X,2]", "[X,1] es f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it takes him [X,2]", "[X,1] es f\u00FChrt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it takes him off", "[X,1] es f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] it takes him", "[X,1] es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it takes", "[X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] it", "[X,1] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] him on", "[X,1] besch\u00E4digt [X,2] ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] him", "[X,1] besch\u00E4digt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] it sets", "[X,1] besch\u00E4digt [X,2] es setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] it", "[X,1] besch\u00E4digt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] on yet", "[X,1] besch\u00E4digt [X,2] auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] on", "[X,1] besch\u00E4digt [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] sets him", "[X,1] besch\u00E4digt [X,2] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] sets", "[X,1] besch\u00E4digt [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2] yet", "[X,1] besch\u00E4digt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars [X,2]", "[X,1] besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him , [X,2]", "[X,1] besch\u00E4digt ihn , [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him , it", "[X,1] besch\u00E4digt ihn , es");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him ,", "[X,1] besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him [X,2] him", "[X,1] besch\u00E4digt ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him [X,2] on", "[X,1] besch\u00E4digt ihn [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him [X,2] sets", "[X,1] besch\u00E4digt ihn [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him [X,2] yet", "[X,1] besch\u00E4digt ihn [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him [X,2]", "[X,1] besch\u00E4digt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars him", "[X,1] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] mars", "[X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "[X,1] off .", "[X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] off", "[X,1] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] .", "[X,1] auf [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] him off", "[X,1] auf [X,2] ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] him", "[X,1] auf [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] off .", "[X,1] auf [X,2] aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] off", "[X,1] auf [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] takes him", "[X,1] auf [X,2] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2] takes", "[X,1] auf [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] on [X,2]", "[X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet [X,2] .", "[X,1] auf und [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet [X,2] him", "[X,1] auf und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet [X,2] off", "[X,1] auf und [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet [X,2]", "[X,1] auf und [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet it [X,2]", "[X,1] auf und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet it takes", "[X,1] auf und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet it", "[X,1] auf und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] on yet", "[X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] on", "[X,1] auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] him off", "[X,1] setzt [X,2] ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] him", "[X,1] setzt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] it takes", "[X,1] setzt [X,2] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] it", "[X,1] setzt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] off", "[X,1] setzt [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] takes him", "[X,1] setzt [X,2] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] takes", "[X,1] setzt [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] yet it", "[X,1] setzt [X,2] und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2] yet", "[X,1] setzt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets [X,2]", "[X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him [X,2] him", "[X,1] setzt ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him [X,2] it", "[X,1] setzt ihn [X,2] es");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him [X,2] off", "[X,1] setzt ihn [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him [X,2] takes", "[X,1] setzt ihn [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him [X,2]", "[X,1] setzt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him on [X,2]", "[X,1] setzt ihn auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him on yet", "[X,1] setzt ihn auf und");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him on", "[X,1] setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets him", "[X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] sets", "[X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes [X,2] .", "[X,1] f\u00FChrt [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes [X,2]", "[X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes him [X,2]", "[X,1] f\u00FChrt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes him off .", "[X,1] f\u00FChrt ihn aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes him off", "[X,1] f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes him", "[X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] takes", "[X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet [X,2] .", "[X,1] und [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet [X,2] him off", "[X,1] und [X,2] ihn aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet [X,2] him", "[X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet [X,2] off .", "[X,1] und [X,2] aus .");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet [X,2] off", "[X,1] und [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet [X,2]", "[X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it [X,2] .", "[X,1] und es [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it [X,2] off", "[X,1] und es [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it [X,2]", "[X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it takes [X,2]", "[X,1] und es f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it takes him", "[X,1] und es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it takes", "[X,1] und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet it", "[X,1] und es");
		verifyLine(lines.get(n++), "[X]", "[X,1] yet", "[X,1] und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , [X,2] him", "und [X,1] , [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , [X,2] on", "und [X,1] , [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , [X,2] yet", "und [X,1] , [X,2] und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , [X,2]", "und [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , it [X,2]", "und [X,1] , es [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , it sets", "und [X,1] , es setzt");
		verifyLine(lines.get(n++), "[X]", "and [X,1] , it", "und [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "and [X,1] ,", "und [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him , [X,2]", "und [X,1] ihn , [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him , it", "und [X,1] ihn , es");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him ,", "und [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him [X,2] him", "und [X,1] ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him [X,2] on", "und [X,1] ihn [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him [X,2] sets", "und [X,1] ihn [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him [X,2] yet", "und [X,1] ihn [X,2] und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him [X,2]", "und [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him on yet", "und [X,1] ihn auf und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him on", "und [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "and [X,1] him", "und [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it [X,2] on", "und [X,1] es [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it [X,2] yet", "und [X,1] es [X,2] und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it [X,2]", "und [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it sets [X,2]", "und [X,1] es setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it sets him", "und [X,1] es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it sets", "und [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "and [X,1] it", "und [X,1] es");
		verifyLine(lines.get(n++), "[X]", "and [X,1] on yet", "und [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] on", "und [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "and [X,1] sets [X,2] yet", "und [X,1] setzt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "and [X,1] sets [X,2]", "und [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] sets him [X,2]", "und [X,1] setzt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "and [X,1] sets him on", "und [X,1] setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "and [X,1] sets him", "und [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "and [X,1] sets", "und [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "and [X,1] yet", "und [X,1] und");
		verifyLine(lines.get(n++), "[X]", "and [X,1]", "und [X,1]");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] , [X,2]", "und es [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] , it", "und es [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] ,", "und es [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] him [X,2]", "und es [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] him on", "und es [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] him", "und es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] it [X,2]", "und es [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] it sets", "und es [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] it", "und es [X,1] es");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] on yet", "und es [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] on", "und es [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] sets [X,2]", "und es [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] sets him", "und es [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] sets", "und es [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "and it [X,1] yet", "und es [X,1] und");
		verifyLine(lines.get(n++), "[X]", "and it [X,1]", "und es [X,1]");
		verifyLine(lines.get(n++), "[X]", "and it mars [X,1] him", "und es besch\u00E4digt [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "and it mars [X,1] it", "und es besch\u00E4digt [X,1] es");
		verifyLine(lines.get(n++), "[X]", "and it mars [X,1] on", "und es besch\u00E4digt [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "and it mars [X,1] sets", "und es besch\u00E4digt [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "and it mars [X,1] yet", "und es besch\u00E4digt [X,1] und");
		verifyLine(lines.get(n++), "[X]", "and it mars [X,1]", "und es besch\u00E4digt [X,1]");
		verifyLine(lines.get(n++), "[X]", "and it mars him ,", "und es besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "and it mars him [X,1]", "und es besch\u00E4digt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "and it mars him", "und es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "and it mars", "und es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "and it", "und es");
		verifyLine(lines.get(n++), "[X]", "and", "und");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] him [X,2]", "ihn , [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] him on", "ihn , [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] him", "ihn , [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] it [X,2]", "ihn , [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] it takes", "ihn , [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] it", "ihn , [X,1] es");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] on [X,2]", "ihn , [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] on yet", "ihn , [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] on", "ihn , [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] takes him", "ihn , [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] takes", "ihn , [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] yet [X,2]", "ihn , [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] yet it", "ihn , [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "him , [X,1] yet", "ihn , [X,1] und");
		verifyLine(lines.get(n++), "[X]", "him , [X,1]", "ihn , [X,1]");
		verifyLine(lines.get(n++), "[X]", "him , it [X,1] him", "ihn , es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him , it [X,1] it", "ihn , es [X,1] es");
		verifyLine(lines.get(n++), "[X]", "him , it [X,1] on", "ihn , es [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "him , it [X,1] takes", "ihn , es [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him , it [X,1] yet", "ihn , es [X,1] und");
		verifyLine(lines.get(n++), "[X]", "him , it [X,1]", "ihn , es [X,1]");
		verifyLine(lines.get(n++), "[X]", "him , it sets [X,1]", "ihn , es setzt [X,1]");
		verifyLine(lines.get(n++), "[X]", "him , it sets him", "ihn , es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "him , it sets", "ihn , es setzt");
		verifyLine(lines.get(n++), "[X]", "him , it", "ihn , es");
		verifyLine(lines.get(n++), "[X]", "him ,", "ihn ,");
		verifyLine(lines.get(n++), "[X]", "him [X,1] , [X,2] him", "ihn [X,1] , [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] , [X,2] on", "ihn [X,1] , [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] , [X,2]", "ihn [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] , it [X,2]", "ihn [X,1] , es [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] , it sets", "ihn [X,1] , es setzt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] , it", "ihn [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] ,", "ihn [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "him [X,1] .", "ihn [X,1] .");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him , [X,2]", "ihn [X,1] ihn , [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him , it", "ihn [X,1] ihn , es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him ,", "ihn [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him [X,2] him", "ihn [X,1] ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him [X,2] it", "ihn [X,1] ihn [X,2] es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him [X,2] on", "ihn [X,1] ihn [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him [X,2] sets", "ihn [X,1] ihn [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him [X,2] takes", "ihn [X,1] ihn [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him [X,2]", "ihn [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him off .", "ihn [X,1] ihn aus .");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him off", "ihn [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him on [X,2]", "ihn [X,1] ihn auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him on yet", "ihn [X,1] ihn auf und");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him on", "ihn [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] him", "ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it [X,2] .", "ihn [X,1] es [X,2] .");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it [X,2] off", "ihn [X,1] es [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it [X,2] on", "ihn [X,1] es [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it [X,2]", "ihn [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it sets [X,2]", "ihn [X,1] es setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it sets him", "ihn [X,1] es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it sets", "ihn [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it takes [X,2]", "ihn [X,1] es f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it takes him", "ihn [X,1] es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it takes", "ihn [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] it", "ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars [X,2] him", "ihn [X,1] besch\u00E4digt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars [X,2] it", "ihn [X,1] besch\u00E4digt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars [X,2] on", "ihn [X,1] besch\u00E4digt [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars [X,2] sets", "ihn [X,1] besch\u00E4digt [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars [X,2]", "ihn [X,1] besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars him ,", "ihn [X,1] besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars him [X,2]", "ihn [X,1] besch\u00E4digt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars him", "ihn [X,1] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] mars", "ihn [X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] off .", "ihn [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "him [X,1] off", "ihn [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on [X,2] him", "ihn [X,1] auf [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on [X,2] takes", "ihn [X,1] auf [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on [X,2]", "ihn [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on yet [X,2]", "ihn [X,1] auf und [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on yet it", "ihn [X,1] auf und es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on yet", "ihn [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "him [X,1] on", "ihn [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets [X,2] him", "ihn [X,1] setzt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets [X,2] it", "ihn [X,1] setzt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets [X,2] takes", "ihn [X,1] setzt [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets [X,2] yet", "ihn [X,1] setzt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets [X,2]", "ihn [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets him [X,2]", "ihn [X,1] setzt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets him on", "ihn [X,1] setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets him", "ihn [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] sets", "ihn [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] takes [X,2] .", "ihn [X,1] f\u00FChrt [X,2] .");
		verifyLine(lines.get(n++), "[X]", "him [X,1] takes [X,2]", "ihn [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] takes him [X,2]", "ihn [X,1] f\u00FChrt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] takes him off", "ihn [X,1] f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "him [X,1] takes him", "ihn [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] takes", "ihn [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] yet [X,2] him", "ihn [X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "him [X,1] yet [X,2]", "ihn [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] yet it [X,2]", "ihn [X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "him [X,1] yet it takes", "ihn [X,1] und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him [X,1] yet it", "ihn [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "him [X,1] yet", "ihn [X,1] und");
		verifyLine(lines.get(n++), "[X]", "him [X,1]", "ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] , [X,2]", "ihn und [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] , it", "ihn und [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] ,", "ihn und [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] him ,", "ihn und [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] him [X,2]", "ihn und [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] him on", "ihn und [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] him", "ihn und [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] it [X,2]", "ihn und [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] it sets", "ihn und [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] it", "ihn und [X,1] es");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] on", "ihn und [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] sets [X,2]", "ihn und [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] sets him", "ihn und [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "him and [X,1] sets", "ihn und [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "him and [X,1]", "ihn und [X,1]");
		verifyLine(lines.get(n++), "[X]", "him and it [X,1] ,", "ihn und es [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "him and it [X,1] him", "ihn und es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him and it [X,1] it", "ihn und es [X,1] es");
		verifyLine(lines.get(n++), "[X]", "him and it [X,1] on", "ihn und es [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "him and it [X,1] sets", "ihn und es [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "him and it [X,1]", "ihn und es [X,1]");
		verifyLine(lines.get(n++), "[X]", "him and it mars [X,1]", "ihn und es besch\u00E4digt [X,1]");
		verifyLine(lines.get(n++), "[X]", "him and it mars him", "ihn und es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "him and it mars", "ihn und es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "him and it", "ihn und es");
		verifyLine(lines.get(n++), "[X]", "him and", "ihn und");
		verifyLine(lines.get(n++), "[X]", "him off .", "ihn aus .");
		verifyLine(lines.get(n++), "[X]", "him off", "ihn aus");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] .", "ihn auf [X,1] .");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] him [X,2]", "ihn auf [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] him off", "ihn auf [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] him", "ihn auf [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] off .", "ihn auf [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] off", "ihn auf [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] takes [X,2]", "ihn auf [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] takes him", "ihn auf [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "him on [X,1] takes", "ihn auf [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him on [X,1]", "ihn auf [X,1]");
		verifyLine(lines.get(n++), "[X]", "him on yet [X,1] .", "ihn auf und [X,1] .");
		verifyLine(lines.get(n++), "[X]", "him on yet [X,1] him", "ihn auf und [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "him on yet [X,1] off", "ihn auf und [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "him on yet [X,1]", "ihn auf und [X,1]");
		verifyLine(lines.get(n++), "[X]", "him on yet it [X,1]", "ihn auf und es [X,1]");
		verifyLine(lines.get(n++), "[X]", "him on yet it takes", "ihn auf und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "him on yet it", "ihn auf und es");
		verifyLine(lines.get(n++), "[X]", "him on yet", "ihn auf und");
		verifyLine(lines.get(n++), "[X]", "him on", "ihn auf");
		verifyLine(lines.get(n++), "[X]", "him", "ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , [X,2] him", "es [X,1] , [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , [X,2] it", "es [X,1] , [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , [X,2] on", "es [X,1] , [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , [X,2] yet", "es [X,1] , [X,2] und");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , [X,2]", "das [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , [X,2]", "es [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , it [X,2]", "es [X,1] , es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , it sets", "das [X,1] , es setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , it sets", "es [X,1] , es setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , it", "das [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] , it", "es [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] ,", "das [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "it [X,1] ,", "es [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "it [X,1] .", "es [X,1] .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and [X,2] ,", "das [X,1] und [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and [X,2] him", "das [X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and [X,2] it", "das [X,1] und [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and [X,2] sets", "das [X,1] und [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and [X,2]", "das [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and it [X,2]", "das [X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and it mars", "das [X,1] und es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and it", "das [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] and", "das [X,1] und");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him , [X,2]", "das [X,1] ihn , [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him , it", "das [X,1] ihn , es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him ,", "das [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him [X,2] it", "es [X,1] ihn [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him [X,2] sets", "das [X,1] ihn [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him [X,2]", "das [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him [X,2]", "es [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him off .", "es [X,1] ihn aus .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him off", "es [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him on [X,2]", "es [X,1] ihn auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him on yet", "es [X,1] ihn auf und");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him on", "es [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him", "das [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him", "es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] ,", "das [X,1] es [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] .", "es [X,1] es [X,2] .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] it", "das [X,1] es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] it", "es [X,1] es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] off", "es [X,1] es [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] on", "es [X,1] es [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] sets", "das [X,1] es [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2] yet", "es [X,1] es [X,2] und");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2]", "das [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it [X,2]", "es [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it mars [X,2]", "das [X,1] es besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it mars him", "das [X,1] es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it mars", "das [X,1] es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it sets [X,2]", "es [X,1] es setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it sets him", "es [X,1] es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it sets", "das [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it sets", "es [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it takes [X,2]", "es [X,1] es f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it takes him", "es [X,1] es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it takes", "es [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it", "das [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] it", "es [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars [X,2] it", "das [X,1] besch\u00E4digt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars [X,2] sets", "das [X,1] besch\u00E4digt [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars [X,2]", "das [X,1] besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars him ,", "das [X,1] besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars him [X,2]", "das [X,1] besch\u00E4digt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars him", "das [X,1] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] mars", "das [X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] off .", "es [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] off", "es [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on [X,2] .", "es [X,1] auf [X,2] .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on [X,2] him", "es [X,1] auf [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on [X,2] off", "es [X,1] auf [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on [X,2] takes", "es [X,1] auf [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on [X,2]", "es [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on yet [X,2]", "es [X,1] auf und [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on yet it", "es [X,1] auf und es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on yet", "es [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "it [X,1] on", "es [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets [X,2] it", "es [X,1] setzt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets [X,2] yet", "es [X,1] setzt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets [X,2]", "es [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets him [X,2]", "es [X,1] setzt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets him on", "es [X,1] setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets him", "es [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets", "das [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] sets", "es [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] takes [X,2] .", "es [X,1] f\u00FChrt [X,2] .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] takes [X,2]", "es [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] takes him [X,2]", "es [X,1] f\u00FChrt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] takes him off", "es [X,1] f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "it [X,1] takes him", "es [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] takes", "es [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet [X,2] .", "es [X,1] und [X,2] .");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet [X,2] him", "es [X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet [X,2] off", "es [X,1] und [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet [X,2]", "es [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet it [X,2]", "es [X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet it takes", "es [X,1] und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet it", "es [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "it [X,1] yet", "es [X,1] und");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "das [X,1]");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "es [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] , [X,2]", "das macht [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] , it", "das macht [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] ,", "das macht [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] him ,", "das macht [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] him [X,2]", "das macht [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] him", "das macht [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] it [X,2]", "das macht [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] it mars", "das macht [X,1] es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] it sets", "das macht [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] it", "das macht [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] mars [X,2]", "das macht [X,1] besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] mars him", "das macht [X,1] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] mars", "das macht [X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1] sets", "das macht [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1]", "das macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1] ,", "das macht ihn [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1] him", "das macht ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1] it", "das macht ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1] mars", "das macht ihn [X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1] sets", "das macht ihn [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1]", "das macht ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes him and [X,1]", "das macht ihn und [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes him and it", "das macht ihn und es");
		verifyLine(lines.get(n++), "[X]", "it makes him and", "das macht ihn und");
		verifyLine(lines.get(n++), "[X]", "it makes him", "das macht ihn");
		verifyLine(lines.get(n++), "[X]", "it makes", "das macht");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] him [X,2]", "es besch\u00E4digt [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] him on", "es besch\u00E4digt [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] him", "es besch\u00E4digt [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] it [X,2]", "es besch\u00E4digt [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] it sets", "es besch\u00E4digt [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] it", "es besch\u00E4digt [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] on [X,2]", "es besch\u00E4digt [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] on yet", "es besch\u00E4digt [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] on", "es besch\u00E4digt [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] sets [X,2]", "es besch\u00E4digt [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] sets him", "es besch\u00E4digt [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] sets", "es besch\u00E4digt [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] yet it", "es besch\u00E4digt [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1] yet", "es besch\u00E4digt [X,1] und");
		verifyLine(lines.get(n++), "[X]", "it mars [X,1]", "es besch\u00E4digt [X,1]");
		verifyLine(lines.get(n++), "[X]", "it mars him , [X,1]", "es besch\u00E4digt ihn , [X,1]");
		verifyLine(lines.get(n++), "[X]", "it mars him , it", "es besch\u00E4digt ihn , es");
		verifyLine(lines.get(n++), "[X]", "it mars him ,", "es besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "it mars him [X,1] him", "es besch\u00E4digt ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it mars him [X,1] it", "es besch\u00E4digt ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it mars him [X,1] on", "es besch\u00E4digt ihn [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "it mars him [X,1] sets", "es besch\u00E4digt ihn [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "it mars him [X,1] yet", "es besch\u00E4digt ihn [X,1] und");
		verifyLine(lines.get(n++), "[X]", "it mars him [X,1]", "es besch\u00E4digt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "it mars him", "es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "it mars", "es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] .", "es setzt [X,1] .");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] him [X,2]", "es setzt [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] him off", "es setzt [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] him", "es setzt [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] it [X,2]", "es setzt [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] it takes", "es setzt [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] it", "es setzt [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] off .", "es setzt [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] off", "es setzt [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] takes [X,2]", "es setzt [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] takes him", "es setzt [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] takes", "es setzt [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] yet [X,2]", "es setzt [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] yet it", "es setzt [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1] yet", "es setzt [X,1] und");
		verifyLine(lines.get(n++), "[X]", "it sets [X,1]", "es setzt [X,1]");
		verifyLine(lines.get(n++), "[X]", "it sets him [X,1] .", "es setzt ihn [X,1] .");
		verifyLine(lines.get(n++), "[X]", "it sets him [X,1] him", "es setzt ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it sets him [X,1] it", "es setzt ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "it sets him [X,1] off", "es setzt ihn [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "it sets him [X,1] takes", "es setzt ihn [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it sets him [X,1]", "es setzt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "it sets him on [X,1]", "es setzt ihn auf [X,1]");
		verifyLine(lines.get(n++), "[X]", "it sets him on yet", "es setzt ihn auf und");
		verifyLine(lines.get(n++), "[X]", "it sets him on", "es setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "it sets him", "es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "it sets", "es setzt");
		verifyLine(lines.get(n++), "[X]", "it takes [X,1] .", "es f\u00FChrt [X,1] .");
		verifyLine(lines.get(n++), "[X]", "it takes [X,1]", "es f\u00FChrt [X,1]");
		verifyLine(lines.get(n++), "[X]", "it takes him [X,1]", "es f\u00FChrt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "it takes him off .", "es f\u00FChrt ihn aus .");
		verifyLine(lines.get(n++), "[X]", "it takes him off", "es f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "it takes him", "es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "it takes", "es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] , [X,2] him", "macht [X,1] , [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] , [X,2]", "macht [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] , it [X,2]", "macht [X,1] , es [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] , it sets", "macht [X,1] , es setzt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] , it", "macht [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] ,", "macht [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him , [X,2]", "macht [X,1] ihn , [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him , it", "macht [X,1] ihn , es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him ,", "macht [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him [X,2] him", "macht [X,1] ihn [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him [X,2] sets", "macht [X,1] ihn [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him [X,2]", "macht [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] him", "macht [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it [X,2] ,", "macht [X,1] es [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it [X,2] him", "macht [X,1] es [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it [X,2] it", "macht [X,1] es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it [X,2] sets", "macht [X,1] es [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it [X,2]", "macht [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it mars [X,2]", "macht [X,1] es besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it mars him", "macht [X,1] es besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it mars", "macht [X,1] es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it sets him", "macht [X,1] es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it sets", "macht [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] it", "macht [X,1] es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars [X,2] him", "macht [X,1] besch\u00E4digt [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars [X,2] it", "macht [X,1] besch\u00E4digt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars [X,2] sets", "macht [X,1] besch\u00E4digt [X,2] setzt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars [X,2]", "macht [X,1] besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars him ,", "macht [X,1] besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars him [X,2]", "macht [X,1] besch\u00E4digt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars him", "macht [X,1] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] mars", "macht [X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] sets him", "macht [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "makes [X,1] sets", "macht [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "makes [X,1]", "macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] , [X,2]", "macht ihn [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] , it", "macht ihn [X,1] , es");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] ,", "macht ihn [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] him ,", "macht ihn [X,1] ihn ,");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] him [X,2]", "macht ihn [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] him", "macht ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] it [X,2]", "macht ihn [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] it sets", "macht ihn [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] it", "macht ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] mars [X,2]", "macht ihn [X,1] besch\u00E4digt [X,2]");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] mars him", "macht ihn [X,1] besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] mars", "macht ihn [X,1] besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] sets him", "macht ihn [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1] sets", "macht ihn [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1]", "macht ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him and [X,1] ,", "macht ihn und [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "makes him and [X,1] him", "macht ihn und [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "makes him and [X,1] it", "macht ihn und [X,1] es");
		verifyLine(lines.get(n++), "[X]", "makes him and [X,1] sets", "macht ihn und [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "makes him and [X,1]", "macht ihn und [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him and it [X,1]", "macht ihn und es [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him and it mars", "macht ihn und es besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "makes him and it", "macht ihn und es");
		verifyLine(lines.get(n++), "[X]", "makes him and", "macht ihn und");
		verifyLine(lines.get(n++), "[X]", "makes him", "macht ihn");
		verifyLine(lines.get(n++), "[X]", "makes", "macht");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him [X,2] it", "besch\u00E4digt [X,1] ihn [X,2] es");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him [X,2] takes", "besch\u00E4digt [X,1] ihn [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him [X,2]", "besch\u00E4digt [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him on [X,2]", "besch\u00E4digt [X,1] ihn auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him on yet", "besch\u00E4digt [X,1] ihn auf und");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him on", "besch\u00E4digt [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] him", "besch\u00E4digt [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it [X,2] it", "besch\u00E4digt [X,1] es [X,2] es");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it [X,2] on", "besch\u00E4digt [X,1] es [X,2] auf");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it [X,2] takes", "besch\u00E4digt [X,1] es [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it [X,2] yet", "besch\u00E4digt [X,1] es [X,2] und");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it [X,2]", "besch\u00E4digt [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it sets [X,2]", "besch\u00E4digt [X,1] es setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it sets him", "besch\u00E4digt [X,1] es setzt ihn");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it sets", "besch\u00E4digt [X,1] es setzt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it takes", "besch\u00E4digt [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] it", "besch\u00E4digt [X,1] es");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] on [X,2] takes", "besch\u00E4digt [X,1] auf [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] on [X,2]", "besch\u00E4digt [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] on yet [X,2]", "besch\u00E4digt [X,1] auf und [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] on yet it", "besch\u00E4digt [X,1] auf und es");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] on yet", "besch\u00E4digt [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] on", "besch\u00E4digt [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets [X,2] it", "besch\u00E4digt [X,1] setzt [X,2] es");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets [X,2] takes", "besch\u00E4digt [X,1] setzt [X,2] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets [X,2] yet", "besch\u00E4digt [X,1] setzt [X,2] und");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets [X,2]", "besch\u00E4digt [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets him [X,2]", "besch\u00E4digt [X,1] setzt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets him on", "besch\u00E4digt [X,1] setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets him", "besch\u00E4digt [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] sets", "besch\u00E4digt [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] takes", "besch\u00E4digt [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] yet [X,2]", "besch\u00E4digt [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] yet it takes", "besch\u00E4digt [X,1] und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] yet it", "besch\u00E4digt [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "mars [X,1] yet", "besch\u00E4digt [X,1] und");
		verifyLine(lines.get(n++), "[X]", "mars [X,1]", "besch\u00E4digt [X,1]");
		verifyLine(lines.get(n++), "[X]", "mars him , [X,1] him", "besch\u00E4digt ihn , [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "mars him , [X,1] it", "besch\u00E4digt ihn , [X,1] es");
		verifyLine(lines.get(n++), "[X]", "mars him , [X,1] on", "besch\u00E4digt ihn , [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "mars him , [X,1] takes", "besch\u00E4digt ihn , [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars him , [X,1] yet", "besch\u00E4digt ihn , [X,1] und");
		verifyLine(lines.get(n++), "[X]", "mars him , [X,1]", "besch\u00E4digt ihn , [X,1]");
		verifyLine(lines.get(n++), "[X]", "mars him , it [X,1]", "besch\u00E4digt ihn , es [X,1]");
		verifyLine(lines.get(n++), "[X]", "mars him , it sets", "besch\u00E4digt ihn , es setzt");
		verifyLine(lines.get(n++), "[X]", "mars him , it", "besch\u00E4digt ihn , es");
		verifyLine(lines.get(n++), "[X]", "mars him ,", "besch\u00E4digt ihn ,");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] him [X,2]", "besch\u00E4digt ihn [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] him on", "besch\u00E4digt ihn [X,1] ihn auf");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] him", "besch\u00E4digt ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] it takes", "besch\u00E4digt ihn [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] it", "besch\u00E4digt ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] on [X,2]", "besch\u00E4digt ihn [X,1] auf [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] on yet", "besch\u00E4digt ihn [X,1] auf und");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] on", "besch\u00E4digt ihn [X,1] auf");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] sets [X,2]", "besch\u00E4digt ihn [X,1] setzt [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] sets him", "besch\u00E4digt ihn [X,1] setzt ihn");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] sets", "besch\u00E4digt ihn [X,1] setzt");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] takes", "besch\u00E4digt ihn [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] yet [X,2]", "besch\u00E4digt ihn [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] yet it", "besch\u00E4digt ihn [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1] yet", "besch\u00E4digt ihn [X,1] und");
		verifyLine(lines.get(n++), "[X]", "mars him [X,1]", "besch\u00E4digt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "mars him", "besch\u00E4digt ihn");
		verifyLine(lines.get(n++), "[X]", "mars", "besch\u00E4digt");
		verifyLine(lines.get(n++), "[X]", "off .", "aus .");
		verifyLine(lines.get(n++), "[X]", "off", "aus");
		verifyLine(lines.get(n++), "[X]", "on [X,1] .", "auf [X,1] .");
		verifyLine(lines.get(n++), "[X]", "on [X,1] him [X,2]", "auf [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "on [X,1] him off .", "auf [X,1] ihn aus .");
		verifyLine(lines.get(n++), "[X]", "on [X,1] him off", "auf [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "on [X,1] him", "auf [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "on [X,1] off .", "auf [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "on [X,1] off", "auf [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "on [X,1] takes [X,2] .", "auf [X,1] f\u00FChrt [X,2] .");
		verifyLine(lines.get(n++), "[X]", "on [X,1] takes [X,2]", "auf [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "on [X,1] takes him [X,2]", "auf [X,1] f\u00FChrt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "on [X,1] takes him off", "auf [X,1] f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "on [X,1] takes him", "auf [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "on [X,1] takes", "auf [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "on [X,1]", "auf [X,1]");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1] .", "auf und [X,1] .");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1] him [X,2]", "auf und [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1] him off", "auf und [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1] him", "auf und [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1] off .", "auf und [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1] off", "auf und [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "on yet [X,1]", "auf und [X,1]");
		verifyLine(lines.get(n++), "[X]", "on yet it [X,1] .", "auf und es [X,1] .");
		verifyLine(lines.get(n++), "[X]", "on yet it [X,1] off", "auf und es [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "on yet it [X,1]", "auf und es [X,1]");
		verifyLine(lines.get(n++), "[X]", "on yet it takes [X,1]", "auf und es f\u00FChrt [X,1]");
		verifyLine(lines.get(n++), "[X]", "on yet it takes him", "auf und es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "on yet it takes", "auf und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "on yet it", "auf und es");
		verifyLine(lines.get(n++), "[X]", "on yet", "auf und");
		verifyLine(lines.get(n++), "[X]", "on", "auf");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] .", "setzt [X,1] .");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] him [X,2]", "setzt [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] him off .", "setzt [X,1] ihn aus .");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] him off", "setzt [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] him", "setzt [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it [X,2] .", "setzt [X,1] es [X,2] .");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it [X,2] off", "setzt [X,1] es [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it [X,2]", "setzt [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it takes [X,2]", "setzt [X,1] es f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it takes him", "setzt [X,1] es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it takes", "setzt [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] it", "setzt [X,1] es");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] off .", "setzt [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] off", "setzt [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] takes [X,2] .", "setzt [X,1] f\u00FChrt [X,2] .");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] takes [X,2]", "setzt [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] takes him [X,2]", "setzt [X,1] f\u00FChrt ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] takes him off", "setzt [X,1] f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] takes him", "setzt [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] takes", "setzt [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet [X,2] .", "setzt [X,1] und [X,2] .");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet [X,2] him", "setzt [X,1] und [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet [X,2] off", "setzt [X,1] und [X,2] aus");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet [X,2]", "setzt [X,1] und [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet it [X,2]", "setzt [X,1] und es [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet it takes", "setzt [X,1] und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet it", "setzt [X,1] und es");
		verifyLine(lines.get(n++), "[X]", "sets [X,1] yet", "setzt [X,1] und");
		verifyLine(lines.get(n++), "[X]", "sets [X,1]", "setzt [X,1]");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] .", "setzt ihn [X,1] .");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] him [X,2]", "setzt ihn [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] him off", "setzt ihn [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] him", "setzt ihn [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] it [X,2]", "setzt ihn [X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] it takes", "setzt ihn [X,1] es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] it", "setzt ihn [X,1] es");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] off .", "setzt ihn [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] off", "setzt ihn [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] takes [X,2]", "setzt ihn [X,1] f\u00FChrt [X,2]");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] takes him", "setzt ihn [X,1] f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1] takes", "setzt ihn [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "sets him [X,1]", "setzt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "sets him on [X,1] .", "setzt ihn auf [X,1] .");
		verifyLine(lines.get(n++), "[X]", "sets him on [X,1] him", "setzt ihn auf [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "sets him on [X,1] off", "setzt ihn auf [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "sets him on [X,1] takes", "setzt ihn auf [X,1] f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "sets him on [X,1]", "setzt ihn auf [X,1]");
		verifyLine(lines.get(n++), "[X]", "sets him on yet [X,1]", "setzt ihn auf und [X,1]");
		verifyLine(lines.get(n++), "[X]", "sets him on yet it", "setzt ihn auf und es");
		verifyLine(lines.get(n++), "[X]", "sets him on yet", "setzt ihn auf und");
		verifyLine(lines.get(n++), "[X]", "sets him on", "setzt ihn auf");
		verifyLine(lines.get(n++), "[X]", "sets him", "setzt ihn");
		verifyLine(lines.get(n++), "[X]", "sets", "setzt");
		verifyLine(lines.get(n++), "[X]", "takes [X,1] .", "f\u00FChrt [X,1] .");
		verifyLine(lines.get(n++), "[X]", "takes [X,1]", "f\u00FChrt [X,1]");
		verifyLine(lines.get(n++), "[X]", "takes him [X,1]", "f\u00FChrt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "takes him off .", "f\u00FChrt ihn aus .");
		verifyLine(lines.get(n++), "[X]", "takes him off", "f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "takes him", "f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "takes", "f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] .", "und [X,1] .");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] him [X,2]", "und [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] him off .", "und [X,1] ihn aus .");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] him off", "und [X,1] ihn aus");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] him", "und [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] off .", "und [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "yet [X,1] off", "und [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "yet [X,1]", "und [X,1]");
		verifyLine(lines.get(n++), "[X]", "yet it [X,1] .", "und es [X,1] .");
		verifyLine(lines.get(n++), "[X]", "yet it [X,1] off .", "und es [X,1] aus .");
		verifyLine(lines.get(n++), "[X]", "yet it [X,1] off", "und es [X,1] aus");
		verifyLine(lines.get(n++), "[X]", "yet it [X,1]", "und es [X,1]");
		verifyLine(lines.get(n++), "[X]", "yet it takes [X,1] .", "und es f\u00FChrt [X,1] .");
		verifyLine(lines.get(n++), "[X]", "yet it takes [X,1]", "und es f\u00FChrt [X,1]");
		verifyLine(lines.get(n++), "[X]", "yet it takes him [X,1]", "und es f\u00FChrt ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "yet it takes him off", "und es f\u00FChrt ihn aus");
		verifyLine(lines.get(n++), "[X]", "yet it takes him", "und es f\u00FChrt ihn");
		verifyLine(lines.get(n++), "[X]", "yet it takes", "und es f\u00FChrt");
		verifyLine(lines.get(n++), "[X]", "yet it", "und es");
		verifyLine(lines.get(n++), "[X]", "yet", "und");

	}
}
