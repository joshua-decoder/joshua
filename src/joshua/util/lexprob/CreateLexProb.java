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
package joshua.util.lexprob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;
import joshua.util.sentence.Vocabulary;


/**
 * Utility program to create a lexical translation probability database, 
 * given word pair count data.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class CreateLexProb {

	/**
	 * Create a lexical translation probability database, given word pair count data.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> target_given_source_counts = commandLine.addStringOption("target-given-source-counts","FILENAME","file containing co-occurence counts of source and target word pairs, sorted by source words");
		Option<String> source_given_target_counts = commandLine.addStringOption("source-given-target-counts","FILENAME","file containing co-occurence counts of target and source word pairs, sorted by target words");
		
		Option<Boolean> target_given_source_gz = commandLine.addBooleanOption("target-given-source-gzipped",false,"is the target given source word pair counts file gzipped");
		Option<Boolean> source_given_target_gz = commandLine.addBooleanOption("source-given-target-gzipped",false,"is the source given target word pair counts file gzipped");
		
		Option<String> source_corpus = commandLine.addStringOption("source-corpus","FILENAME","name of file containing source corpus");
		Option<String> target_corpus = commandLine.addStringOption("target-corpus","FILENAME","name of file containing target corpus");
		
		
		
		Option<String> encoding = commandLine.addStringOption('e',"encoding","ENCODING","UTF-8","input file encoding");
		
		
//		Option<String> source_word = commandLine.addStringOption('s',"source-word","WORD","source word to look up");
//		Option<String> target_word = commandLine.addStringOption('t',"target-word","WORD","target word to look up");
		

		
		commandLine.parse(args);
		
		//LexicalTranslationProbabilityDistribution lexProbs = null;
		
		try {
			
			// Set System.out and System.err to use the provided character encoding
			try {
				System.setOut(new PrintStream(System.out, true, commandLine.getValue(encoding)));
				System.setErr(new PrintStream(System.err, true, commandLine.getValue(encoding)));
			} catch (UnsupportedEncodingException e1) {
				System.err.println(commandLine.getValue(encoding) + " is not a valid encoding; using system default encoding for System.out and System.err.");
			} catch (SecurityException e2) {
				System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
			}
			
			// Set up the source text for reading
			Scanner target_given_source;
			if (commandLine.getValue(target_given_source_counts).endsWith(".gz") || commandLine.getValue(target_given_source_gz))
				target_given_source = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(target_given_source_counts))),commandLine.getValue(encoding))));
			else
				target_given_source = new Scanner( new File(commandLine.getValue(target_given_source_counts)), commandLine.getValue(encoding));

			
			// Set up the target text for reading
			Scanner source_given_target;
			if (commandLine.getValue(source_given_target_counts).endsWith(".gz") || commandLine.getValue(source_given_target_gz))
				source_given_target = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(source_given_target_counts))),commandLine.getValue(encoding))));
			else
				source_given_target = new Scanner( new File(commandLine.getValue(source_given_target_counts)), commandLine.getValue(encoding));
			
			Vocabulary sourceVocab = new Vocabulary(commandLine.getValue(source_corpus));
			Vocabulary targetVocab = new Vocabulary(commandLine.getValue(target_corpus));
			
			
			System.out.println("cette = " + sourceVocab.getID("cette"));
			System.out.println("minute = " + targetVocab.getID("minute"));
			System.out.println("this = " + targetVocab.getID("this"));
			
			LexProbs lexProbs = new LexProbs(source_given_target, target_given_source, sourceVocab, targetVocab);
			
			System.out.println("P( minute | cette ) = " + lexProbs.targetGivenSource("minute", "cette"));
			System.out.println("P( this | cette ) = " + lexProbs.targetGivenSource("this", "cette"));
			
			/*
			Map<Integer,Map<Integer,Float>> targetGivenSource = calculateLexProbs(target_given_source, targetVocab, sourceVocab);
			Map<Integer,Map<Integer,Float>> sourceGivenTarget = calculateLexProbs(source_given_target, sourceVocab, targetVocab);
			
			String sourceWord = commandLine.getValue(source_word);
			String targetWord = commandLine.getValue(target_word);
			
			int sourceID = sourceVocab.getID(sourceWord);
			int targetID = targetVocab.getID(targetWord);

			
			for (Map.Entry<Integer, Map<Integer,Float>> sourceEntry : targetGivenSource.entrySet()) {
				for (Map.Entry<Integer,Float> targetEntry : sourceEntry.getValue().entrySet()) { 
					System.out.println("P("+targetEntry.getKey() + " | " + sourceEntry.getKey()+" ) = " + targetEntry.getValue());
					//System.out.println("P("+targetVocab.getWord(targetEntry.getKey()) + " | " + sourceVocab.getWord(sourceEntry.getKey())+" ) = " + targetEntry.getValue());
				}
			}
			*/
			//System.out.println("P( " + sourceWord + " | " + targetWord + " ) = " + sourceGivenTarget.get(sourceID).get(targetID));//.get(targetID).get(sourceID));
			
			//System.out.println("P( " + targetWord + " | " + sourceWord + " ) = " + targetGivenSource.get(sourceID).get(targetID));
			
			//lexProbs = new LexicalTranslationProbabilityDistribution(source_to_target, target_to_source,commandLine.getValue(dbDirectory), commandLine.getValue(encoding));
			
		}  catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	
}
