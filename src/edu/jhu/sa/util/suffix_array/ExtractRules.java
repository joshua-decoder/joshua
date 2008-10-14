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
package edu.jhu.sa.util.suffix_array;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import joshua.decoder.ff.tm.Rule;
import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;
import joshua.util.sentence.Vocabulary;


/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class ExtractRules {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> source = commandLine.addStringOption('f',"source","SOURCE_FILE","Source language training file");
		Option<String> target = commandLine.addStringOption('e',"target","TARGET_FILE","Target language training file");		
		Option<String> alignment = commandLine.addStringOption('a',"alignments","ALIGNMENTS_FILE","Source-target alignments training file");	
		Option<String> test = commandLine.addStringOption('t',"test","TEST_FILE","Source language test file");
		
		Option<String> encoding = commandLine.addStringOption("encoding","ENCODING","UTF-8","File encoding format");
		
		Option<Integer> maxPhraseSpan = commandLine.addIntegerOption("maxPhraseSpan","MAX_PHRASE_SPAN",10, "Max phrase span");
		Option<Integer> maxPhraseLength = commandLine.addIntegerOption("maxPhraseLength","MAX_PHRASE_LENGTH",10, "Max phrase length");
		Option<Integer> maxNonterminals = commandLine.addIntegerOption("maxNonterminals","MAX_NONTERMINALS",2, "Max nonterminals");
		Option<Integer> spanLimit = commandLine.addIntegerOption("spanLimit","SPAN_LIMIT",8, "Span limit");
		
		
		commandLine.parse(args);

		
		// Set System.out and System.err to use the provided character encoding
		try {
			System.setOut(new PrintStream(System.out, true, commandLine.getValue(encoding)));
			System.setErr(new PrintStream(System.err, true, commandLine.getValue(encoding)));
		} catch (UnsupportedEncodingException e1) {
			System.err.println(commandLine.getValue(encoding) + " is not a valid encoding; using system default encoding for System.out and System.err.");
		} catch (SecurityException e2) {
			System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
		}
		
		
		
		String sourceFileName = commandLine.getValue(source);
		Vocabulary sourceVocab = new Vocabulary();
		int[] sourceWordsSentences = SuffixArrayFactory.createVocabulary(sourceFileName, sourceVocab);
		CorpusArray sourceCorpusArray = SuffixArrayFactory.createCorpusArray(sourceFileName, sourceVocab, sourceWordsSentences[0], sourceWordsSentences[1]);
		SuffixArray sourceSuffixArray = SuffixArrayFactory.createSuffixArray(sourceCorpusArray);

		String targetFileName = commandLine.getValue(target);
		Vocabulary targetVocab = new Vocabulary();
		int[] targetWordsSentences = SuffixArrayFactory.createVocabulary(commandLine.getValue(target), targetVocab);
		CorpusArray targetCorpusArray = SuffixArrayFactory.createCorpusArray(targetFileName, targetVocab, targetWordsSentences[0], targetWordsSentences[1]);
		SuffixArray targetSuffixArray = SuffixArrayFactory.createSuffixArray(targetCorpusArray);
		
		String alignmentFileName = commandLine.getValue(alignment);
		AlignmentArray alignmentArray = SuffixArrayFactory.createAlignmentArray(alignmentFileName, sourceSuffixArray, targetSuffixArray);
		
		Map<Integer,String> ntVocab = new HashMap<Integer,String>();
		ntVocab.put(PrefixTree.X, "X");
		
		Scanner testFileScanner = new Scanner(new File(commandLine.getValue(test)), commandLine.getValue(encoding));
		
		while (testFileScanner.hasNextLine()) {
			String line = testFileScanner.nextLine();
			int[] words = sourceVocab.getIDs(line);
			PrefixTree prefixTree = new PrefixTree(sourceSuffixArray, targetCorpusArray, alignmentArray, words, commandLine.getValue(maxPhraseSpan), commandLine.getValue(maxPhraseLength), commandLine.getValue(maxNonterminals), commandLine.getValue(spanLimit));
			
			for (Rule rule : prefixTree.getAllRules()) {
				System.out.println(rule.toString(ntVocab, sourceVocab, targetVocab));
			}
		}
		
	}

}
