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
package joshua.prefix_tree;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import joshua.corpus.suffix_array.Compile;
import joshua.prefix_tree.ExtractRules;
import joshua.util.FormatUtil;

import org.testng.Assert;
import org.testng.annotations.Parameters;
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
	// cat hiero.grammar | sort | ruby -e 'STDIN.each_line{ |line| words=line.strip.split(" ||| "); puts "verifyLine(lines.get(n++), \"#{words[0]}\", \"#{words[1]}\", \"#{words[2]}\");"; }' > java-rules.txt
	
	// Also
	// cat hiero.grammar | sort | ruby -e 'STDIN.each_line{ |line| words=line.strip.split(" ||| "); puts "#{words[0]} ||| #{words[1]} ||| #{words[2]}"; }' > hiero.grammar.clean
	
	String sourceFileName;
	String targetFileName;
	String alignmentFileName;
	String alignmentsType;
	// ä == \u00E4
	// ü == \u00FC
	// ñ == \u00F1
	// í == \u00ED
	
	@Parameters({"alignmentsType"})
	@Test
	public void setup(String alignmentsType) throws IOException {
		
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

		this.alignmentsType = alignmentsType;
	}

	/**
	 * Extracts rules and returns the file name where the extracted rules are stored.
	 * 
	 * @param testCorpusString
	 * @param sentenceInitialX TODO
	 * @param sentenceFinalX TODO
	 * @param violatingX TODO
	 * @return
	 * @throws IOException 
	 */
	private List<String> extractRules(String testCorpusString, boolean sentenceInitialX, boolean sentenceFinalX, boolean violatingX) throws IOException {
		return extractRules(sourceFileName, targetFileName, alignmentFileName, testCorpusString, sentenceInitialX, sentenceFinalX, violatingX, false, 2);
	}
	
	private List<String> extractRules(String sourceFileName, String targetFileName, String alignmentFileName, String testCorpusString, boolean sentenceInitialX, boolean sentenceFinalX, boolean violatingX, boolean printPrefixTree, int minNonterminalSpan) throws IOException {
		
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
		
		String joshDirName;
		{
			File joshDir = File.createTempFile(new Date().toString(), "josh");
			joshDirName = joshDir.getAbsolutePath();
			joshDir.delete();
		}
		
		Compile compileJoshDir = new Compile();
		compileJoshDir.setSourceCorpus(sourceFileName);
		compileJoshDir.setTargetCorpus(targetFileName);
		compileJoshDir.setAlignments(alignmentFileName);
		compileJoshDir.setOutputDir(joshDirName);
		compileJoshDir.execute();
		
		ExtractRules extractRules = new ExtractRules();
		extractRules.setSentenceInitialX(sentenceInitialX);
		extractRules.setSentenceFinalX(sentenceFinalX);
		extractRules.setEdgeXViolates(violatingX);
		extractRules.setMaxPhraseLength(5);
		extractRules.setMinNonterminalSpan(minNonterminalSpan);
		extractRules.setJoshDir(joshDirName);
		extractRules.setTestFile(testFileName);
		extractRules.setOutputFile(rulesFileName);
		try {
			extractRules.execute();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			Assert.fail(e.getLocalizedMessage());
		}
		
		Scanner scanner = new Scanner(new File(rulesFileName));
		
		ArrayList<String> lines = new ArrayList<String>();
		
		while (scanner.hasNextLine()) {
			
			String line = scanner.nextLine();
			lines.add(line);
			
		}
		
		
		Collections.sort(lines);
		
		return lines;
	}
	
	public void verifyLine(String line, String lhs, String sourceRHS, String targetRHS)  {
		String[] part = line.split(" \\|\\|\\| ");
		Assert.assertEquals(part[0], lhs);
		Assert.assertEquals(part[1], sourceRHS);
		Assert.assertEquals(part[2], targetRHS);
	}
	
	@Test
	public void europarlSmall100() throws IOException {
		extractEuroparlSmall100(false);
	}
	
//	@Test
//	public void europarlSmall100ViolatingX() throws IOException {
//		extractEuroparlSmall100(true);
//	}
	
	private static class WTF extends RuntimeException {
		WTF(String msg) { super(msg); }
	}
	public void extractEuroparlSmall100(boolean violatingX) throws IOException {
		
		String sourceFileName = "data/europarl.es.small.100";
		String targetFileName = "data/europarl.en.small.100";
		String alignmentFileName = "data/es_en_europarl_alignments.txt.small.100";
		
		String testSentence = "declaro reanudado el per\u00EDodo de sesiones del parlamento europeo , interrumpido el viernes 17 de diciembre pasado , y reitero a sus se\u00F1or\u00EDas mi deseo de que hayan tenido unas buenas vacaciones .";
		
		boolean printPrefixTree = false;
		
		int minNonterminalSpan = 2;
		
		List<String> lines = extractRules(sourceFileName, targetFileName, alignmentFileName, testSentence, true, true, violatingX, printPrefixTree, minNonterminalSpan);

		int expectedLines = 525;
		if (violatingX) expectedLines += 14;
		
//		for (String line : lines) {
//			System.err.println(line);
//		}
		
//		if (lines.size() != expectedLines) {
//			throw new WTF("lines.size()=="+lines.size() + " but expectedLines=="+expectedLines);
//		}
		Assert.assertEquals(lines.size(), expectedLines);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", ", [X,1] , [X,2]", ", [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] , [X,2]", ", [X,1] [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] , [X,2]", ", [X,2] [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] , [X,2]", ", you [X,1] [X,2]");
		if (violatingX) verifyLine(lines.get(n++), "[X]", ", [X,1] , y [X,2]", ", [X,1] , and [X,2]"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", ", [X,1] , y", ", [X,1] , and");
		verifyLine(lines.get(n++), "[X]", ", [X,1] , y", ", you [X,1] and");
		verifyLine(lines.get(n++), "[X]", ", [X,1] ,", ", [X,1] ,");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a [X,2]", ", [X,1] to wish [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a [X,2]", "too [X,1] to [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a sus se\u00F1or\u00EDas", ", [X,1] to wish you");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a", ", [X,1] to");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a", "too [X,1] to");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de [X,2]", ", [X,1] [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de [X,2]", ", [X,1] in [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de [X,2]", ", [X,1] of [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de [X,2]", ", [X,1] shall do [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de [X,2]", ", still [X,1] in [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de [X,2]", ", still [X,1] of [X,2]");
		if (violatingX) verifyLine(lines.get(n++), "[X]", ", [X,1] de que [X,2]", ", [X,1] that [X,2]"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", ", [X,1] de que", ", [X,1] that");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de que", ", [X,1] to the start of");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de", ", [X,1] attention to");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de", ", [X,1] in");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de", ", [X,1] of");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de", ", still [X,1] in");
		verifyLine(lines.get(n++), "[X]", ", [X,1] de", ", still [X,1] of");
		verifyLine(lines.get(n++), "[X]", ", [X,1] deseo [X,2]", ", [X,1] wish [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] deseo de [X,2]", ", [X,1] wish of [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] deseo de", ", [X,1] wish of");
		verifyLine(lines.get(n++), "[X]", ", [X,1] deseo", ", [X,1] wish");
		verifyLine(lines.get(n++), "[X]", ", [X,1] el [X,2] ,", ", [X,1] [X,2] ,");
		verifyLine(lines.get(n++), "[X]", ", [X,1] el [X,2]", ", [X,1] [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] el [X,2]", ", [X,1] the [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] el", ", [X,1] the");
		verifyLine(lines.get(n++), "[X]", ", [X,1] el", ", [X,1] there");
		if (violatingX) verifyLine(lines.get(n++), "[X]", ", [X,1] que [X,2]", ", [X,1] that [X,2]"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", ", [X,1] que", ", [X,1] that");
		verifyLine(lines.get(n++), "[X]", ", [X,1] que", ", [X,1] which");
		verifyLine(lines.get(n++), "[X]", ", [X,1] sus se\u00F1or\u00EDas", ", [X,1] wish you");
		if (violatingX) verifyLine(lines.get(n++), "[X]", ", [X,1] y [X,2]", ", [X,1] and [X,2]"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", ", [X,1] y", ", [X,1] and");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", and [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", still [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", there is [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", which [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", you [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", "and urging [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", "i [X,1]");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", "too [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y [X,1] de que", ", and [X,1] that");
		verifyLine(lines.get(n++), "[X]", ", y [X,1] sus se\u00F1or\u00EDas", ", and [X,1] wish you");
		verifyLine(lines.get(n++), "[X]", ", y [X,1]", ", and [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y reitero [X,1]", ", and i would like once again [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y reitero a [X,1]", ", and i would like once again to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y reitero a", ", and i would like once again to");
		verifyLine(lines.get(n++), "[X]", ", y reitero", ", and i would like once again");
		verifyLine(lines.get(n++), "[X]", ", y", ", and");
		verifyLine(lines.get(n++), "[X]", ",", ", which");
		verifyLine(lines.get(n++), "[X]", ",", ",");
		verifyLine(lines.get(n++), "[X]", ",", "and");
		verifyLine(lines.get(n++), "[X]", ",", "i");
		verifyLine(lines.get(n++), "[X]", ",", "too");
		verifyLine(lines.get(n++), "[X]", ".", ".");
		verifyLine(lines.get(n++), "[X]", ".", "?");
		verifyLine(lines.get(n++), "[X]", ".", "materialise");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] , [X,2]", "17 [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] , y reitero", "17 [X,1] , and i would like once again");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] , y", "17 [X,1] , and");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] ,", "17 [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] reitero", "17 [X,1] i would like once again");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] y reitero", "17 [X,1] and i would like once again");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] y", "17 [X,1] and");
		verifyLine(lines.get(n++), "[X]", "17 [X,1]", "17 [X,1]");
		verifyLine(lines.get(n++), "[X]", "17 de diciembre pasado ,", "17 december 1999 ,");
		verifyLine(lines.get(n++), "[X]", "17 de diciembre pasado [X,1]", "17 december 1999 [X,1]");
		verifyLine(lines.get(n++), "[X]", "17 de diciembre pasado", "17 december 1999");
		verifyLine(lines.get(n++), "[X]", "17", "17");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] , y", "[X,1] , [X,2] , and"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] ,", ", [X,2] [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] ,", "[X,1] , [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] ,", "[X,1] [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] 17", "[X,1] [X,2] 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] a", "[X,1] , [X,2] to");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] de que", "[X,1] , [X,2] that"); // Added in buggy
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] de que", "[X,1] [X,2] to the start of"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] de", "[X,1] , [X,2] in");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] de", "[X,1] , [X,2] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] de", "[X,1] [X,2] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] deseo de", "[X,1] , [X,2] wish of");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] deseo", "[X,1] , [X,2] wish");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] el", "[X,1] , [X,2] the");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] el", "[X,1] , [X,2] there");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] que", "[X,1] , [X,2] that"); // Added in buggy start
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] que", "[X,1] [X,2] that"); // Added in buggy start
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] y", "[X,1] , [X,2] and"); // Added in buggy start
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", ", [X,2] [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] , you [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] i [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] in sri lanka and urging [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] one , there is [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido [X,2] 17", "[X,1] adjourned [X,2] 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido [X,2]", "[X,1] adjourned [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido el viernes", "[X,1] adjourned on friday");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido", "[X,1] adjourned");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y [X,2]", "[X,1] , and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y [X,2]", "[X,1] and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y reitero a", "[X,1] , and i would like once again to");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y reitero", "[X,1] , and i would like once again");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y", "[X,1] , and");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y", "[X,1] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] i");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] in sri lanka and");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] one ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] to point out ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] .", "[X,1] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] .", "[X,1] ?");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2] , y", "[X,1] 17 [X,2] , and");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2] ,", "[X,1] 17 [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2] y", "[X,1] 17 [X,2] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2]", "[X,1] 17 [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 de diciembre pasado", "[X,1] 17 december 1999");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17", "[X,1] 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2] de", "[X,1] to [X,2] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2] que", "[X,1] for [X,2] which");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2] que", "[X,1] on [X,2] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2]", "[X,1] on [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2]", "[X,1] to [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2]", "[X,1] to wish [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] a sus se\u00F1or\u00EDas", "[X,1] to wish you");
		verifyLine(lines.get(n++), "[X]", "[X,1] a", "[X,1] on");
		verifyLine(lines.get(n++), "[X]", "[X,1] a", "[X,1] to");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] ,", "[X,1] [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] ,", "[X,1] in [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] ,", "[X,1] of [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] .", "[X,1] [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] .", "[X,1] for topical [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] .", "[X,1] of [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] .", "[X,1] shall do [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] .", "[X,2] [X,1] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] del", "[X,2] 's [X,1] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] el", "[X,1] [X,2] the");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] el", "[X,1] of [X,2] the");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] parlamento", "[X,1] with [X,2] house");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] y", "[X,1] in [X,2] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2] y", "[X,1] of [X,2] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] 's [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] [X,2] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] attention to [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] by [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] for topical [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] in [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] of [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] to [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,1] with [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,2] 's [X,1] for");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,2] 's [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "[X,2] [X,1] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "of [X,1] [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de [X,2]", "of [X,2] [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de diciembre pasado ,", "[X,1] december 1999 ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] de diciembre pasado [X,2]", "[X,1] december 1999 [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de diciembre pasado", "[X,1] december 1999");
		verifyLine(lines.get(n++), "[X]", "[X,1] de que [X,2]", "[X,1] that [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de que", "[X,1] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] de que", "[X,1] to the start of");
		verifyLine(lines.get(n++), "[X]", "[X,1] de sesiones del [X,2]", "[X,1] session of [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de sesiones del", "[X,1] session of");
		verifyLine(lines.get(n++), "[X]", "[X,1] de sesiones", "[X,1] part-session");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "'s [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] 's");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] attention to");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] by");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] for topical");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] for");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] in");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] on");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] to");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "[X,1] with");
		verifyLine(lines.get(n++), "[X]", "[X,1] de", "of [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2] ,", "[X,1] by [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2] ,", "[X,1] of [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2]", "[X,1] by [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2]", "[X,1] from [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2]", "[X,1] of [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2]", "[X,1] of the [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del [X,2]", "[X,1] of this [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del parlamento [X,2]", "[X,2] parliament [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del parlamento", "parliament [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del", "'s [X,1]");
		verifyLine(lines.get(n++), "[X]", "[X,1] del", "[X,1] 's");
		verifyLine(lines.get(n++), "[X]", "[X,1] del", "[X,1] by");
		verifyLine(lines.get(n++), "[X]", "[X,1] del", "[X,1] from");
		verifyLine(lines.get(n++), "[X]", "[X,1] del", "[X,1] of the");
		verifyLine(lines.get(n++), "[X]", "[X,1] del", "[X,1] of");
		verifyLine(lines.get(n++), "[X]", "[X,1] deseo [X,2] .", "[X,1] wish [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] deseo [X,2]", "[X,1] wish [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] deseo de [X,2] .", "[X,1] wish of [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] deseo de [X,2]", "[X,1] wish of [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] deseo de", "[X,1] wish of");
		verifyLine(lines.get(n++), "[X]", "[X,1] deseo", "[X,1] wish");
		verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2] ,", "[X,1] [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2] ,", "[X,1] the [X,2] ,");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2] a", "[X,1] the [X,2] on"); // Added in buggy start
		verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2] de", "[X,1] [X,2] in");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2] el", "[X,1] [X,2] the"); // Added in buggy start
		verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2] y", "[X,1] the [X,2] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2]", "[X,1] the [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] el [X,2]", "[X,1] this [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes 17 [X,2]", "[X,1] on friday 17 [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes 17", "[X,1] on friday 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes [X,2]", "[X,1] on friday [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes", "[X,1] on friday");
		verifyLine(lines.get(n++), "[X]", "[X,1] el", "[X,1] the");
		verifyLine(lines.get(n++), "[X]", "[X,1] el", "[X,1] there");
		verifyLine(lines.get(n++), "[X]", "[X,1] el", "[X,1] this");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan [X,2]", "[X,1] you [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan tenido [X,2]", "[X,1] you enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan tenido", "[X,1] you enjoyed");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan", "[X,1] you");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2] hayan", "[X,1] a happy new year [X,2] you");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2] que hayan", "[X,1] a happy new year [X,2] that you");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2] que", "[X,1] a happy new year [X,2] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2]", "[X,1] my [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi deseo de [X,2]", "[X,1] a happy new year in the hope [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi deseo de que", "[X,1] a happy new year in the hope that");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi deseo de", "[X,1] a happy new year in the hope");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi", "[X,1] my");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento [X,2] ,", "[X,1] parliament [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento [X,2]", "[X,1] house [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento [X,2]", "[X,1] parliament [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo , [X,2]", "[X,1] the european parliament [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo , interrumpido", "[X,1] the european parliament adjourned");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo", "[X,1] european parliament");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo", "[X,1] the european parliament");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento", "[X,1] house");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento", "[X,1] parliament");
		verifyLine(lines.get(n++), "[X]", "[X,1] pasado [X,2]", "[X,2] [X,1] past");
		verifyLine(lines.get(n++), "[X]", "[X,1] pasado", "[X,1] past");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2] .", "[X,1] [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2] .", "[X,1] that [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2] .", "[X,1] to [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2] .", "[X,1] which [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] , as expressed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] , that [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] for [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] that , [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] that [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] that since [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] to [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] which [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] who [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan [X,2]", "[X,1] that you [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan tenido [X,2]", "[X,1] that you enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan tenido", "[X,1] that you enjoyed");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan", "[X,1] that you");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] , as");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] , that");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] for");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] that since");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] to");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] which");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] who");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero [X,2]", "[X,1] i would like once again [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero a [X,2]", "[X,1] i would like once again to wish [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero a sus se\u00F1or\u00EDas", "[X,1] i would like once again to wish you");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero a", "[X,1] i would like once again to");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero", "[X,1] i would like once again");
		verifyLine(lines.get(n++), "[X]", "[X,1] sus se\u00F1or\u00EDas", "[X,1] wish you");
		verifyLine(lines.get(n++), "[X]", "[X,1] sus", "[X,1] its");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido [X,2] .", "[X,1] enjoyed [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido [X,2]", "[X,1] enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido unas buenas vacaciones", "[X,1] enjoyed a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido", "[X,1] enjoyed");
		verifyLine(lines.get(n++), "[X]", "[X,1] unas buenas vacaciones .", "[X,1] a pleasant festive period .");
		verifyLine(lines.get(n++), "[X]", "[X,1] unas buenas vacaciones", "[X,1] a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2] que", "[X,1] and [X,2] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] , and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] , that conference [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] activity and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] and which [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] with it and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y reitero a", "[X,1] and i would like once again to");
		verifyLine(lines.get(n++), "[X]", "[X,1] y reitero", "[X,1] and i would like once again");
		verifyLine(lines.get(n++), "[X]", "[X,1] y", "[X,1] , and");
		verifyLine(lines.get(n++), "[X]", "[X,1] y", "[X,1] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] y", "[X,1] activity and");
		verifyLine(lines.get(n++), "[X]", "[X,1] y", "[X,1] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] y", "[X,1] with it and");
		verifyLine(lines.get(n++), "[X]", "a [X,1] de", "on [X,1] in");
		verifyLine(lines.get(n++), "[X]", "a [X,1] de", "to [X,1] of");
		verifyLine(lines.get(n++), "[X]", "a [X,1] mi [X,2]", "to wish [X,1] a happy new year [X,2]");
		verifyLine(lines.get(n++), "[X]", "a [X,1] mi deseo de", "to wish [X,1] a happy new year in the hope");
		verifyLine(lines.get(n++), "[X]", "a [X,1] que [X,2]", "on [X,1] that [X,2]");
		verifyLine(lines.get(n++), "[X]", "a [X,1] que", "on [X,1] that");
		verifyLine(lines.get(n++), "[X]", "a [X,1]", "[X,1] to");
		verifyLine(lines.get(n++), "[X]", "a [X,1]", "on [X,1]");
		verifyLine(lines.get(n++), "[X]", "a [X,1]", "to [X,1]");
		verifyLine(lines.get(n++), "[X]", "a [X,1]", "to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "a sus se\u00F1or\u00EDas mi [X,1]", "to wish you a happy new year [X,1]");
		verifyLine(lines.get(n++), "[X]", "a sus se\u00F1or\u00EDas", "to wish you");
		verifyLine(lines.get(n++), "[X]", "a", "as");
		verifyLine(lines.get(n++), "[X]", "a", "on");
		verifyLine(lines.get(n++), "[X]", "a", "to");
		verifyLine(lines.get(n++), "[X]", "de [X,1] , [X,2]", "in [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] , [X,2]", "of [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] ,", "in [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "de [X,1] ,", "of [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "de [X,1] .", "for topical [X,1] .");
		verifyLine(lines.get(n++), "[X]", "de [X,1] .", "of [X,1] .");
		verifyLine(lines.get(n++), "[X]", "de [X,1] .", "with [X,1] .");
		verifyLine(lines.get(n++), "[X]", "de [X,1] del", "of [X,1] of");
		verifyLine(lines.get(n++), "[X]", "de [X,1] el [X,2]", "before the courts once [X,1] the [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] el [X,2]", "of [X,1] the [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] el", "before the courts once [X,1] the");
		verifyLine(lines.get(n++), "[X]", "de [X,1] el", "of [X,1] the");
		verifyLine(lines.get(n++), "[X]", "de [X,1] parlamento [X,2]", "with [X,1] house [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] parlamento", "with [X,1] house");
		verifyLine(lines.get(n++), "[X]", "de [X,1] y [X,2]", "in [X,1] and [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] y [X,2]", "of [X,1] and [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] y [X,2]", "of [X,1] and which [X,2]");
		verifyLine(lines.get(n++), "[X]", "de [X,1] y", "by [X,1] and");
		verifyLine(lines.get(n++), "[X]", "de [X,1] y", "in [X,1] and");
		verifyLine(lines.get(n++), "[X]", "de [X,1] y", "of [X,1] and");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "'s [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "[X,1] 's");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "[X,1] of");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "before the courts once [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "by [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "for topical [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "in [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "of [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "on [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "to [X,1]");
		verifyLine(lines.get(n++), "[X]", "de [X,1]", "with [X,1]");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado , [X,1]", "december 1999 , [X,1]");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado , y", "december 1999 , and");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado ,", "december 1999 ,");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado [X,1] a", "december 1999 [X,1] to");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado [X,1] reitero", "december 1999 [X,1] i would like once again");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado [X,1]", "december 1999 [X,1]");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado", "december 1999");
		verifyLine(lines.get(n++), "[X]", "de que", "to the start of");
		verifyLine(lines.get(n++), "[X]", "de sesiones del [X,1] 17", "session of [X,1] 17");
		verifyLine(lines.get(n++), "[X]", "de sesiones del [X,1]", "session of [X,1]");
		verifyLine(lines.get(n++), "[X]", "de sesiones del parlamento europeo", "session of the european parliament");
		verifyLine(lines.get(n++), "[X]", "de sesiones del", "session of");
		verifyLine(lines.get(n++), "[X]", "de", "'s");
		verifyLine(lines.get(n++), "[X]", "de", "before");
		verifyLine(lines.get(n++), "[X]", "de", "by");
		verifyLine(lines.get(n++), "[X]", "de", "for topical");
		verifyLine(lines.get(n++), "[X]", "de", "for");
		verifyLine(lines.get(n++), "[X]", "de", "from");
		verifyLine(lines.get(n++), "[X]", "de", "in");
		verifyLine(lines.get(n++), "[X]", "de", "of");
		verifyLine(lines.get(n++), "[X]", "de", "on");
		verifyLine(lines.get(n++), "[X]", "de", "to");
		verifyLine(lines.get(n++), "[X]", "de", "with");
		verifyLine(lines.get(n++), "[X]", "declaro reanudado el per\u00EDodo [X,1]", "i declare resumed the [X,1]");
		verifyLine(lines.get(n++), "[X]", "declaro reanudado el per\u00EDodo", "i declare resumed the");
		verifyLine(lines.get(n++), "[X]", "del [X,1] , [X,2]", "by [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "del [X,1] ,", "by [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "del [X,1] ,", "of [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "del [X,1]", "by [X,1]");
		verifyLine(lines.get(n++), "[X]", "del [X,1]", "from [X,1]");
		verifyLine(lines.get(n++), "[X]", "del [X,1]", "of [X,1]");
		verifyLine(lines.get(n++), "[X]", "del [X,1]", "of the [X,1]");
		verifyLine(lines.get(n++), "[X]", "del [X,1]", "of this [X,1]");
		verifyLine(lines.get(n++), "[X]", "del parlamento [X,1]", "[X,1] parliament");
		verifyLine(lines.get(n++), "[X]", "del parlamento", "parliament 's");
		verifyLine(lines.get(n++), "[X]", "del parlamento", "parliament");
		verifyLine(lines.get(n++), "[X]", "del", "'s");
		verifyLine(lines.get(n++), "[X]", "del", "by");
		verifyLine(lines.get(n++), "[X]", "del", "from");
		verifyLine(lines.get(n++), "[X]", "del", "of the");
		verifyLine(lines.get(n++), "[X]", "del", "of");
		verifyLine(lines.get(n++), "[X]", "del", "parliament");
		verifyLine(lines.get(n++), "[X]", "deseo [X,1] .", "wish [X,1] .");
		verifyLine(lines.get(n++), "[X]", "deseo [X,1]", "wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1] .", "wish of [X,1] .");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1] tenido [X,2]", "in the hope [X,1] enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1] tenido", "in the hope [X,1] enjoyed");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1]", "in the hope [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1]", "wish of [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de que [X,1]", "in the hope that [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de que hayan [X,1]", "in the hope that you [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de que hayan tenido", "in the hope that you enjoyed");
		verifyLine(lines.get(n++), "[X]", "deseo de que hayan", "in the hope that you");
		verifyLine(lines.get(n++), "[X]", "deseo de que", "in the hope that");
		verifyLine(lines.get(n++), "[X]", "deseo de", "in the hope");
		verifyLine(lines.get(n++), "[X]", "deseo de", "wish of");
		verifyLine(lines.get(n++), "[X]", "deseo", "hope");
		verifyLine(lines.get(n++), "[X]", "deseo", "wish");
		verifyLine(lines.get(n++), "[X]", "el [X,1] , [X,2]", "the [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "el [X,1] ,", "the [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "el [X,1] ,", "this [X,1] ,");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "el [X,1] a [X,2]", "the [X,1] on [X,2]"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", "el [X,1] a", "the [X,1] on");
		verifyLine(lines.get(n++), "[X]", "el [X,1] de [X,2] ,", "the [X,1] [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "el [X,1] de [X,2]", "the [X,1] [X,2]");
		verifyLine(lines.get(n++), "[X]", "el [X,1] de", "the [X,1] 's");
		if (violatingX) verifyLine(lines.get(n++), "[X]", "el [X,1] el [X,2]", "the [X,1] [X,2]"); // Added in buggy
		verifyLine(lines.get(n++), "[X]", "el [X,1] y [X,2]", "the [X,1] and [X,2]");
		verifyLine(lines.get(n++), "[X]", "el [X,1] y", "the [X,1] and");
		verifyLine(lines.get(n++), "[X]", "el [X,1]", "the [X,1]");
		verifyLine(lines.get(n++), "[X]", "el [X,1]", "this [X,1]");
		verifyLine(lines.get(n++), "[X]", "el viernes 17 [X,1] ,", "on friday 17 [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "el viernes 17 [X,1] y", "on friday 17 [X,1] and");
		verifyLine(lines.get(n++), "[X]", "el viernes 17 [X,1]", "on friday 17 [X,1]");
		verifyLine(lines.get(n++), "[X]", "el viernes 17", "on friday 17");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1] , y", "on friday [X,1] , and");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1] ,", "on friday [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1] y", "on friday [X,1] and");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1]", "on friday [X,1]");
		verifyLine(lines.get(n++), "[X]", "el viernes", "on friday");
		verifyLine(lines.get(n++), "[X]", "el", "the");
		verifyLine(lines.get(n++), "[X]", "el", "there");
		verifyLine(lines.get(n++), "[X]", "el", "this");
		verifyLine(lines.get(n++), "[X]", "europeo", "european");
		verifyLine(lines.get(n++), "[X]", "europeo", "the european");
		verifyLine(lines.get(n++), "[X]", "hayan [X,1] .", "you [X,1] .");
		verifyLine(lines.get(n++), "[X]", "hayan [X,1]", "you [X,1]");
		verifyLine(lines.get(n++), "[X]", "hayan tenido [X,1] .", "you enjoyed [X,1] .");
		verifyLine(lines.get(n++), "[X]", "hayan tenido [X,1]", "you enjoyed [X,1]");
		verifyLine(lines.get(n++), "[X]", "hayan tenido unas buenas vacaciones", "you enjoyed a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "hayan tenido", "you enjoyed");
		verifyLine(lines.get(n++), "[X]", "hayan", "you");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] , y", "adjourned [X,1] , and");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] ,", "adjourned [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17 [X,2] ,", "adjourned [X,1] 17 [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17 [X,2] y", "adjourned [X,1] 17 [X,2] and");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17 [X,2]", "adjourned [X,1] 17 [X,2]");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17", "adjourned [X,1] 17");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] de diciembre pasado", "adjourned [X,1] december 1999");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] y", "adjourned [X,1] and");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1]", "adjourned [X,1]");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes 17 [X,1]", "adjourned on friday 17 [X,1]");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes 17", "adjourned on friday 17");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes [X,1] ,", "adjourned on friday [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes [X,1] y", "adjourned on friday [X,1] and");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes [X,1]", "adjourned on friday [X,1]");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes", "adjourned on friday");
		verifyLine(lines.get(n++), "[X]", "interrumpido", "adjourned");
		verifyLine(lines.get(n++), "[X]", "mi [X,1] que [X,2]", "my [X,1] that since [X,2]");
		verifyLine(lines.get(n++), "[X]", "mi [X,1] que", "my [X,1] that since");
		verifyLine(lines.get(n++), "[X]", "mi [X,1]", "my [X,1]");
		verifyLine(lines.get(n++), "[X]", "mi", "my");
		verifyLine(lines.get(n++), "[X]", "parlamento [X,1] , [X,2]", "parliament [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "parlamento [X,1] ,", "parliament [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "parlamento [X,1] de", "house [X,1] 's");
		verifyLine(lines.get(n++), "[X]", "parlamento [X,1]", "house [X,1]");
		verifyLine(lines.get(n++), "[X]", "parlamento [X,1]", "parliament [X,1]");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , [X,1] 17", "the european parliament [X,1] 17");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , [X,1]", "the european parliament [X,1]");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , interrumpido [X,1]", "the european parliament adjourned [X,1]");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , interrumpido", "the european parliament adjourned");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo", "european parliament");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo", "the european parliament");
		verifyLine(lines.get(n++), "[X]", "parlamento", "house");
		verifyLine(lines.get(n++), "[X]", "parlamento", "parliament");
		verifyLine(lines.get(n++), "[X]", "pasado", "past");
		verifyLine(lines.get(n++), "[X]", "que [X,1] .", "that [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que [X,1] .", "to [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que [X,1] .", "which [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que [X,1] .", "who [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que [X,1] unas buenas vacaciones", "that [X,1] a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "[X,1] that");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "as expressed [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "because [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "for [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "that , [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "that [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "that since [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "to [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "which [X,1]");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "who [X,1]");
		verifyLine(lines.get(n++), "[X]", "que hayan [X,1] .", "that you [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que hayan [X,1]", "that you [X,1]");
		verifyLine(lines.get(n++), "[X]", "que hayan tenido [X,1] .", "that you enjoyed [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que hayan tenido [X,1]", "that you enjoyed [X,1]");
		verifyLine(lines.get(n++), "[X]", "que hayan tenido", "that you enjoyed");
		verifyLine(lines.get(n++), "[X]", "que hayan", "that you");
		verifyLine(lines.get(n++), "[X]", "que", "as");
		verifyLine(lines.get(n++), "[X]", "que", "because");
		verifyLine(lines.get(n++), "[X]", "que", "for");
		verifyLine(lines.get(n++), "[X]", "que", "that since");
		verifyLine(lines.get(n++), "[X]", "que", "that");
		verifyLine(lines.get(n++), "[X]", "que", "to");
		verifyLine(lines.get(n++), "[X]", "que", "which");
		verifyLine(lines.get(n++), "[X]", "que", "who");
		verifyLine(lines.get(n++), "[X]", "reitero [X,1]", "i would like once again [X,1]");
		verifyLine(lines.get(n++), "[X]", "reitero a [X,1]", "i would like once again to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "reitero a sus se\u00F1or\u00EDas", "i would like once again to wish you");
		verifyLine(lines.get(n++), "[X]", "reitero a", "i would like once again to");
		verifyLine(lines.get(n++), "[X]", "reitero", "i would like once again");
		verifyLine(lines.get(n++), "[X]", "sesiones", "part-session");
		verifyLine(lines.get(n++), "[X]", "sesiones", "session");
		verifyLine(lines.get(n++), "[X]", "sus [X,1]", "its [X,1]");
		verifyLine(lines.get(n++), "[X]", "sus [X,1]", "their [X,1]");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas [X,1]", "you [X,1]");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi [X,1] hayan", "you a happy new year [X,1] you");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi [X,1] que", "you a happy new year [X,1] that");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi [X,1]", "you a happy new year [X,1]");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi deseo de", "you a happy new year in the hope");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas", "you");
		verifyLine(lines.get(n++), "[X]", "sus", "his");
		verifyLine(lines.get(n++), "[X]", "sus", "its");
		verifyLine(lines.get(n++), "[X]", "sus", "their");
		verifyLine(lines.get(n++), "[X]", "tenido [X,1] .", "enjoyed [X,1] .");
		verifyLine(lines.get(n++), "[X]", "tenido [X,1]", "enjoyed [X,1]");
		verifyLine(lines.get(n++), "[X]", "tenido unas buenas vacaciones .", "enjoyed a pleasant festive period .");
		verifyLine(lines.get(n++), "[X]", "tenido unas buenas vacaciones", "enjoyed a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "tenido", "enjoyed");
		verifyLine(lines.get(n++), "[X]", "tenido", "has");
		verifyLine(lines.get(n++), "[X]", "unas buenas vacaciones .", "a pleasant festive period .");
		verifyLine(lines.get(n++), "[X]", "unas buenas vacaciones", "a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "y [X,1] de [X,2]", "and [X,1] [X,2]");
		verifyLine(lines.get(n++), "[X]", "y [X,1] de que", "and [X,1] that");
		verifyLine(lines.get(n++), "[X]", "y [X,1] de", "and [X,1] of");
		verifyLine(lines.get(n++), "[X]", "y [X,1] que [X,2]", "and [X,1] that [X,2]");
		verifyLine(lines.get(n++), "[X]", "y [X,1] que", "and [X,1] that");
		verifyLine(lines.get(n++), "[X]", "y [X,1] sus se\u00F1or\u00EDas", "and [X,1] wish you");
		verifyLine(lines.get(n++), "[X]", "y [X,1]", ", that conference [X,1]");
		verifyLine(lines.get(n++), "[X]", "y [X,1]", "and [X,1]");
		verifyLine(lines.get(n++), "[X]", "y [X,1]", "and the [X,1]");
		verifyLine(lines.get(n++), "[X]", "y [X,1]", "and which [X,1]");
		verifyLine(lines.get(n++), "[X]", "y reitero [X,1]", "and i would like once again [X,1]");
		verifyLine(lines.get(n++), "[X]", "y reitero a [X,1]", "and i would like once again to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "y reitero a sus se\u00F1or\u00EDas", "and i would like once again to wish you");
		verifyLine(lines.get(n++), "[X]", "y reitero a", "and i would like once again to");
		verifyLine(lines.get(n++), "[X]", "y reitero", "and i would like once again");
		verifyLine(lines.get(n++), "[X]", "y", ",");
		verifyLine(lines.get(n++), "[X]", "y", "and");
	
		Assert.assertEquals(n, expectedLines);
		
	}
	
	@Test
	public void europarlSmall1() throws IOException {
		
		String sourceFileName = "data/europarl.es.small.1";
		String targetFileName = "data/europarl.en.small.1";
		String alignmentFileName = "data/es_en_europarl_alignments.txt.small.1";
		
		boolean printPrefixTree = false;
				
		List<String> lines = extractRules(sourceFileName, targetFileName, alignmentFileName, "declaro reanudado el per\u00EDodo de sesiones del parlamento europeo , interrumpido el viernes 17 de diciembre pasado , y reitero a sus se\u00F1or\u00EDas mi deseo de que hayan tenido unas buenas vacaciones .", true, true, false, printPrefixTree, 2);

//		Assert.assertEquals(lines.size(), 197);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", ", [X,1] a [X,2]", ", [X,1] to wish [X,2]");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a sus se\u00F1or\u00EDas", ", [X,1] to wish you");
		verifyLine(lines.get(n++), "[X]", ", [X,1] a", ", [X,1] to");
		verifyLine(lines.get(n++), "[X]", ", [X,1] sus se\u00F1or\u00EDas", ", [X,1] wish you");
		verifyLine(lines.get(n++), "[X]", ", [X,1]", ", [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y [X,1] sus se\u00F1or\u00EDas", ", and [X,1] wish you");
		verifyLine(lines.get(n++), "[X]", ", y [X,1]", ", and [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y reitero [X,1]", ", and i would like once again [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y reitero a [X,1]", ", and i would like once again to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", ", y reitero a", ", and i would like once again to");
		verifyLine(lines.get(n++), "[X]", ", y reitero", ", and i would like once again");
		verifyLine(lines.get(n++), "[X]", ", y", ", and");
		verifyLine(lines.get(n++), "[X]", ",", ",");
		verifyLine(lines.get(n++), "[X]", ".", ".");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] , [X,2]", "17 [X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] , y reitero", "17 [X,1] , and i would like once again");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] , y", "17 [X,1] , and");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] ,", "17 [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] reitero", "17 [X,1] i would like once again");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] y reitero", "17 [X,1] and i would like once again");
		verifyLine(lines.get(n++), "[X]", "17 [X,1] y", "17 [X,1] and");
		verifyLine(lines.get(n++), "[X]", "17 [X,1]", "17 [X,1]");
		verifyLine(lines.get(n++), "[X]", "17 de diciembre pasado ,", "17 december 1999 ,");
		verifyLine(lines.get(n++), "[X]", "17 de diciembre pasado [X,1]", "17 december 1999 [X,1]");
		verifyLine(lines.get(n++), "[X]", "17 de diciembre pasado", "17 december 1999");
		verifyLine(lines.get(n++), "[X]", "17", "17");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] 17", "[X,1] [X,2] 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2] a", "[X,1] , [X,2] to");
		verifyLine(lines.get(n++), "[X]", "[X,1] , [X,2]", "[X,1] , [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido [X,2] 17", "[X,1] adjourned [X,2] 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido [X,2]", "[X,1] adjourned [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido el viernes", "[X,1] adjourned on friday");
		verifyLine(lines.get(n++), "[X]", "[X,1] , interrumpido", "[X,1] adjourned");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y [X,2]", "[X,1] , and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y reitero a", "[X,1] , and i would like once again to");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y reitero", "[X,1] , and i would like once again");
		verifyLine(lines.get(n++), "[X]", "[X,1] , y", "[X,1] , and");
		verifyLine(lines.get(n++), "[X]", "[X,1] ,", "[X,1] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] .", "[X,1] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2] , y", "[X,1] 17 [X,2] , and");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2] ,", "[X,1] 17 [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2] y", "[X,1] 17 [X,2] and");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 [X,2]", "[X,1] 17 [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17 de diciembre pasado", "[X,1] 17 december 1999");
		verifyLine(lines.get(n++), "[X]", "[X,1] 17", "[X,1] 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] a [X,2]", "[X,1] to wish [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] a sus se\u00F1or\u00EDas", "[X,1] to wish you");
		verifyLine(lines.get(n++), "[X]", "[X,1] a", "[X,1] to");
		verifyLine(lines.get(n++), "[X]", "[X,1] de diciembre pasado ,", "[X,1] december 1999 ,");
		verifyLine(lines.get(n++), "[X]", "[X,1] de diciembre pasado [X,2]", "[X,1] december 1999 [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de diciembre pasado", "[X,1] december 1999");
		verifyLine(lines.get(n++), "[X]", "[X,1] de sesiones del [X,2]", "[X,1] session of [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] de sesiones del", "[X,1] session of");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes 17 [X,2]", "[X,1] on friday 17 [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes 17", "[X,1] on friday 17");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes [X,2]", "[X,1] on friday [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] el viernes", "[X,1] on friday");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan [X,2]", "[X,1] you [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan tenido [X,2]", "[X,1] you enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan tenido", "[X,1] you enjoyed");
		verifyLine(lines.get(n++), "[X]", "[X,1] hayan", "[X,1] you");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2] hayan", "[X,1] a happy new year [X,2] you");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2] que hayan", "[X,1] a happy new year [X,2] that you");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi [X,2] que", "[X,1] a happy new year [X,2] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi deseo de [X,2]", "[X,1] a happy new year in the hope [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi deseo de que", "[X,1] a happy new year in the hope that");
		verifyLine(lines.get(n++), "[X]", "[X,1] mi deseo de", "[X,1] a happy new year in the hope");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo , [X,2]", "[X,1] the european parliament [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo , interrumpido", "[X,1] the european parliament adjourned");
		verifyLine(lines.get(n++), "[X]", "[X,1] parlamento europeo", "[X,1] the european parliament");
		verifyLine(lines.get(n++), "[X]", "[X,1] que [X,2]", "[X,1] that [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan [X,2]", "[X,1] that you [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan tenido [X,2]", "[X,1] that you enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan tenido", "[X,1] that you enjoyed");
		verifyLine(lines.get(n++), "[X]", "[X,1] que hayan", "[X,1] that you");
		verifyLine(lines.get(n++), "[X]", "[X,1] que", "[X,1] that");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero [X,2]", "[X,1] i would like once again [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero a [X,2]", "[X,1] i would like once again to wish [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero a sus se\u00F1or\u00EDas", "[X,1] i would like once again to wish you");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero a", "[X,1] i would like once again to");
		verifyLine(lines.get(n++), "[X]", "[X,1] reitero", "[X,1] i would like once again");
		verifyLine(lines.get(n++), "[X]", "[X,1] sus se\u00F1or\u00EDas", "[X,1] wish you");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido [X,2] .", "[X,1] enjoyed [X,2] .");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido [X,2]", "[X,1] enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido unas buenas vacaciones", "[X,1] enjoyed a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "[X,1] tenido", "[X,1] enjoyed");
		verifyLine(lines.get(n++), "[X]", "[X,1] unas buenas vacaciones .", "[X,1] a pleasant festive period .");
		verifyLine(lines.get(n++), "[X]", "[X,1] unas buenas vacaciones", "[X,1] a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "[X,1] y [X,2]", "[X,1] and [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] y reitero a", "[X,1] and i would like once again to");
		verifyLine(lines.get(n++), "[X]", "[X,1] y reitero", "[X,1] and i would like once again");
		verifyLine(lines.get(n++), "[X]", "[X,1] y", "[X,1] and");
		verifyLine(lines.get(n++), "[X]", "a [X,1] mi [X,2]", "to wish [X,1] a happy new year [X,2]");
		verifyLine(lines.get(n++), "[X]", "a [X,1] mi deseo de", "to wish [X,1] a happy new year in the hope");
		verifyLine(lines.get(n++), "[X]", "a [X,1]", "to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "a sus se\u00F1or\u00EDas mi [X,1]", "to wish you a happy new year [X,1]");
		verifyLine(lines.get(n++), "[X]", "a sus se\u00F1or\u00EDas", "to wish you");
		verifyLine(lines.get(n++), "[X]", "a", "to");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado , [X,1]", "december 1999 , [X,1]");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado , y", "december 1999 , and");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado ,", "december 1999 ,");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado [X,1] a", "december 1999 [X,1] to");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado [X,1] reitero", "december 1999 [X,1] i would like once again");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado [X,1]", "december 1999 [X,1]");
		verifyLine(lines.get(n++), "[X]", "de diciembre pasado", "december 1999");
		verifyLine(lines.get(n++), "[X]", "de sesiones del [X,1] 17", "session of [X,1] 17");
		verifyLine(lines.get(n++), "[X]", "de sesiones del [X,1]", "session of [X,1]");
		verifyLine(lines.get(n++), "[X]", "de sesiones del parlamento europeo", "session of the european parliament");
		verifyLine(lines.get(n++), "[X]", "de sesiones del", "session of");
		verifyLine(lines.get(n++), "[X]", "de", "in");
		verifyLine(lines.get(n++), "[X]", "declaro reanudado el per\u00EDodo [X,1]", "i declare resumed the [X,1]");
		verifyLine(lines.get(n++), "[X]", "declaro reanudado el per\u00EDodo", "i declare resumed the");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1] tenido [X,2]", "in the hope [X,1] enjoyed [X,2]");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1] tenido", "in the hope [X,1] enjoyed");
		verifyLine(lines.get(n++), "[X]", "deseo de [X,1]", "in the hope [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de que [X,1]", "in the hope that [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de que hayan [X,1]", "in the hope that you [X,1]");
		verifyLine(lines.get(n++), "[X]", "deseo de que hayan tenido", "in the hope that you enjoyed");
		verifyLine(lines.get(n++), "[X]", "deseo de que hayan", "in the hope that you");
		verifyLine(lines.get(n++), "[X]", "deseo de que", "in the hope that");
		verifyLine(lines.get(n++), "[X]", "deseo de", "in the hope");
		verifyLine(lines.get(n++), "[X]", "deseo", "hope");
		verifyLine(lines.get(n++), "[X]", "el viernes 17 [X,1] ,", "on friday 17 [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "el viernes 17 [X,1] y", "on friday 17 [X,1] and");
		verifyLine(lines.get(n++), "[X]", "el viernes 17 [X,1]", "on friday 17 [X,1]");
		verifyLine(lines.get(n++), "[X]", "el viernes 17", "on friday 17");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1] , y", "on friday [X,1] , and");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1] ,", "on friday [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1] y", "on friday [X,1] and");
		verifyLine(lines.get(n++), "[X]", "el viernes [X,1]", "on friday [X,1]");
		verifyLine(lines.get(n++), "[X]", "el viernes", "on friday");
		verifyLine(lines.get(n++), "[X]", "europeo", "the european");
		verifyLine(lines.get(n++), "[X]", "hayan [X,1] .", "you [X,1] .");
		verifyLine(lines.get(n++), "[X]", "hayan [X,1]", "you [X,1]");
		verifyLine(lines.get(n++), "[X]", "hayan tenido [X,1] .", "you enjoyed [X,1] .");
		verifyLine(lines.get(n++), "[X]", "hayan tenido [X,1]", "you enjoyed [X,1]");
		verifyLine(lines.get(n++), "[X]", "hayan tenido unas buenas vacaciones", "you enjoyed a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "hayan tenido", "you enjoyed");
		verifyLine(lines.get(n++), "[X]", "hayan", "you");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] , y", "adjourned [X,1] , and");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] ,", "adjourned [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17 [X,2] ,", "adjourned [X,1] 17 [X,2] ,");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17 [X,2] y", "adjourned [X,1] 17 [X,2] and");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17 [X,2]", "adjourned [X,1] 17 [X,2]");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] 17", "adjourned [X,1] 17");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] de diciembre pasado", "adjourned [X,1] december 1999");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1] y", "adjourned [X,1] and");
		verifyLine(lines.get(n++), "[X]", "interrumpido [X,1]", "adjourned [X,1]");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes 17 [X,1]", "adjourned on friday 17 [X,1]");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes 17", "adjourned on friday 17");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes [X,1] ,", "adjourned on friday [X,1] ,");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes [X,1] y", "adjourned on friday [X,1] and");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes [X,1]", "adjourned on friday [X,1]");
		verifyLine(lines.get(n++), "[X]", "interrumpido el viernes", "adjourned on friday");
		verifyLine(lines.get(n++), "[X]", "interrumpido", "adjourned");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , [X,1] 17", "the european parliament [X,1] 17");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , [X,1]", "the european parliament [X,1]");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , interrumpido [X,1]", "the european parliament adjourned [X,1]");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo , interrumpido", "the european parliament adjourned");
		verifyLine(lines.get(n++), "[X]", "parlamento europeo", "the european parliament");
		verifyLine(lines.get(n++), "[X]", "parlamento", "parliament");
		verifyLine(lines.get(n++), "[X]", "que [X,1] .", "that [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que [X,1] unas buenas vacaciones", "that [X,1] a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "que [X,1]", "that [X,1]");
		verifyLine(lines.get(n++), "[X]", "que hayan [X,1] .", "that you [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que hayan [X,1]", "that you [X,1]");
		verifyLine(lines.get(n++), "[X]", "que hayan tenido [X,1] .", "that you enjoyed [X,1] .");
		verifyLine(lines.get(n++), "[X]", "que hayan tenido [X,1]", "that you enjoyed [X,1]");
		verifyLine(lines.get(n++), "[X]", "que hayan tenido", "that you enjoyed");
		verifyLine(lines.get(n++), "[X]", "que hayan", "that you");
		verifyLine(lines.get(n++), "[X]", "que", "that");
		verifyLine(lines.get(n++), "[X]", "reitero [X,1]", "i would like once again [X,1]");
		verifyLine(lines.get(n++), "[X]", "reitero a [X,1]", "i would like once again to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "reitero a sus se\u00F1or\u00EDas", "i would like once again to wish you");
		verifyLine(lines.get(n++), "[X]", "reitero a", "i would like once again to");
		verifyLine(lines.get(n++), "[X]", "reitero", "i would like once again");
		verifyLine(lines.get(n++), "[X]", "sesiones", "session");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi [X,1] hayan", "you a happy new year [X,1] you");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi [X,1] que", "you a happy new year [X,1] that");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi [X,1]", "you a happy new year [X,1]");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas mi deseo de", "you a happy new year in the hope");
		verifyLine(lines.get(n++), "[X]", "sus se\u00F1or\u00EDas", "you");
		verifyLine(lines.get(n++), "[X]", "tenido [X,1] .", "enjoyed [X,1] .");
		verifyLine(lines.get(n++), "[X]", "tenido [X,1]", "enjoyed [X,1]");
		verifyLine(lines.get(n++), "[X]", "tenido unas buenas vacaciones .", "enjoyed a pleasant festive period .");
		verifyLine(lines.get(n++), "[X]", "tenido unas buenas vacaciones", "enjoyed a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "tenido", "enjoyed");
		verifyLine(lines.get(n++), "[X]", "unas buenas vacaciones .", "a pleasant festive period .");
		verifyLine(lines.get(n++), "[X]", "unas buenas vacaciones", "a pleasant festive period");
		verifyLine(lines.get(n++), "[X]", "y [X,1] sus se\u00F1or\u00EDas", "and [X,1] wish you");
		verifyLine(lines.get(n++), "[X]", "y [X,1]", "and [X,1]");
		verifyLine(lines.get(n++), "[X]", "y reitero [X,1]", "and i would like once again [X,1]");
		verifyLine(lines.get(n++), "[X]", "y reitero a [X,1]", "and i would like once again to wish [X,1]");
		verifyLine(lines.get(n++), "[X]", "y reitero a sus se\u00F1or\u00EDas", "and i would like once again to wish you");
		verifyLine(lines.get(n++), "[X]", "y reitero a", "and i would like once again to");
		verifyLine(lines.get(n++), "[X]", "y reitero", "and i would like once again");
		verifyLine(lines.get(n++), "[X]", "y", "and");

	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet1() throws IOException {
		
		List<String> lines = extractRules("it", false, false, false);
		
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
		
		List<String> lines = extractRules("it", true, true, false);
//		
//		for (String line : lines) {
//			System.err.println(line);
//		}
		
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
		
		List<String> lines = extractRules("it makes", false, false, false);
		
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
	public void testRuleSet2Expanded() throws IOException {
		
		List<String> lines = extractRules("it makes", true, true, false);
		
		Assert.assertEquals(lines.size(), 10);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2]", "[X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it", "[X,1] es");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "das [X,1]");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "es [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1]", "das macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes", "das macht");
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1]", "macht [X,1]");
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
		
		List<String> lines = extractRules("it makes him", false, false, false);
		
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
	public void testRuleSet3Expanded() throws IOException {
		
		List<String> lines = extractRules("it makes him", true, true, false);
		
		final int expectedLines = 23;
		Assert.assertEquals(lines.size(), expectedLines);
		
		int n = 0;
		verifyLine(lines.get(n++), "[X]", "[X,1] him [X,2]", "[X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] him", "[X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2] him", "[X,1] es [X,2] ihn");
		verifyLine(lines.get(n++), "[X]", "[X,1] it [X,2]", "[X,1] es [X,2]");
		verifyLine(lines.get(n++), "[X]", "[X,1] it", "[X,1] es");
		verifyLine(lines.get(n++), "[X]", "him [X,1]", "ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "him", "ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him [X,2]", "das [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him [X,2]", "es [X,1] ihn [X,2]");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him", "das [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1] him", "es [X,1] ihn");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "das [X,1]");
		verifyLine(lines.get(n++), "[X]", "it [X,1]", "es [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes [X,1]", "das macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes him [X,1]", "das macht ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "it makes him", "das macht ihn");
		verifyLine(lines.get(n++), "[X]", "it makes", "das macht");
		verifyLine(lines.get(n++), "[X]", "it", "das");
		verifyLine(lines.get(n++), "[X]", "it", "es");
		verifyLine(lines.get(n++), "[X]", "makes [X,1]", "macht [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him [X,1]", "macht ihn [X,1]");
		verifyLine(lines.get(n++), "[X]", "makes him", "macht ihn");
		verifyLine(lines.get(n++), "[X]", "makes", "macht");
		
		Assert.assertEquals(n, expectedLines);
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
		extractFull(false, false, false, false);
	}
	
	@Test(dependsOnMethods={"setup"})
	public void testRuleSet18Expanded() throws IOException {
		extractFull(true, true, true, true);
	}
	
	
	private void extractFull(boolean sentenceInitialX, boolean sentenceFinalX, boolean initialXViolates, boolean finalXViolates) throws IOException {
		
		List<String> lines = extractRules("it makes him and it mars him , it sets him on yet it takes him off .", sentenceInitialX, sentenceFinalX, initialXViolates);
		
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
