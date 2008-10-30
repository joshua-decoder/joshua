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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;

/**
 * Utility to extract aligned word pairs from an aligned corpus.
 * <p>
 * The files used must use Unix-style newlines. 
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 * @see Section 4.4 of "Statistical Phrase-Based Translation" by Philipp Koehn, Franz Josef Och, & Daniel Marcu (HLT-NAACL, 2003)
 */
public class ExtractWordPairs {

	private static final Logger logger = Logger.getLogger(ExtractWordPairs.class.getName());
	
	/** Special marker to use with unaligned words */
	public static final String UNALIGNED_MARKER = "NULL";
	
	/**
	 * Extract aligned word pairs from an aligned corpus.
	 * <p>
	 * This method does not convert from upper case to lower case. All input needs to already be in the proper case.
	 * <p>
	 * NOTE: The scanners provided for source text, target text, and alignments must all be backed by data that uses Unix-style newlines.
	 * 
	 * @param number_of_lines The number of lines to process from the aligned corpus.
	 * @param source_text Scanner backed by the source language text
	 * @param target_text Scanner backed by the target language text
	 * @param alignments Scanner backed by the sentence alignment data
	 * @param outputFile Writer to use when producing output results
	 * @throws IOException Thrown if an I/O error occurs when writing results
	 */
	public static void extract(int number_of_lines, Scanner source_text, Scanner target_text, Scanner alignments, Writer outputFile) throws IOException {
		
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Extracting aligned word pairs from aligned sentences...");
		}
		
		// Iterate over all lines of input
		for (int line_number=1; line_number<=number_of_lines; line_number++) {

			// Read in the next line from the files
			String[] source_words = source_text.nextLine().split("\\s+");
			String[] target_words = target_text.nextLine().split("\\s+");
			String[] raw_alignment_points = alignments.nextLine().split("\\s+");

			try {
				// We have a new sentence pair.
				//    Initially assume that all words are unaligned.
				//    As each alignment point is processed, aligned words will be removed from the appropriate set
				Set<Integer> unaligned_source_words = new HashSet<Integer>(source_words.length);
				Set<Integer> unaligned_target_words = new HashSet<Integer>(target_words.length);

				for (int i=0; i<source_words.length; i++) { unaligned_source_words.add(i); }
				for (int i=0; i<target_words.length; i++) { unaligned_target_words.add(i); }

				// Iterate over each alignment point in the aligned sentence pair
				for (String raw_alignment_point : raw_alignment_points) {

					// Alignment points must be of the format #-#, where # is a number
					int split_point = raw_alignment_point.indexOf('-');

					int x = Integer.valueOf(raw_alignment_point.substring(0,split_point));
					int y = Integer.valueOf(raw_alignment_point.substring(split_point+1));

					// Remove this source word from the set of unaligned source words
					unaligned_source_words.remove(x);

					// Remove this target word from the set of unaligned target words
					unaligned_target_words.remove(y);


					// Lowercase the words,
					//    then print the word pair to the output file
					outputFile.write(source_words[x].toLowerCase() + " " + target_words[y].toLowerCase() + "\n");

				}		

				// For each unaligned source word,
				//    lowercase the word,
				//    then print the word, aligned with the special token NULL
				for (int source_word_index : unaligned_source_words) {
					outputFile.write(source_words[source_word_index].toLowerCase() + " " + UNALIGNED_MARKER + "\n");					
				}

				// For each unaligned target word,
				//    lowercase the word,
				//    then print the word, aligned with the special token NULL
				for (int target_word_index : unaligned_target_words) {
					outputFile.write(UNALIGNED_MARKER + " " + target_words[target_word_index].toLowerCase() + "\n");					
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				if (logger.isLoggable(Level.SEVERE)) {
					logger.severe("ArrayIndexOutOfBoundsException at sentence pair:\n" + Arrays.toString(source_words) + "\n"+Arrays.toString(target_words) +"\n"+Arrays.toString(raw_alignment_points) + "\n");
				}
				throw e;
			}
				

		}

		// Tidy up
		outputFile.flush();
		outputFile.close();

		if (logger.isLoggable(Level.INFO)) {
			logger.info("...done.");
		}
	}
	
	/**
	 * Utility to extract aligned word pairs from an aligned corpus
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {

		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> source_file = commandLine.addStringOption('s',"source-text","SOURCE_FILENAME","name of file containing source language corpus");
		//Option<String> source_file_encoding = commandLine.addStringOption("source-encoding","SOURCE_ENCODING","ISO-8859-1","source language file encoding");
		Option<String> source_file_encoding = commandLine.addStringOption("source-encoding","SOURCE_ENCODING","UTF-8","source language file encoding");
		Option<Boolean> source_file_gz = commandLine.addBooleanOption("source-text-gzipped",false,"is the source text gzipped");
		
		Option<String> target_file = commandLine.addStringOption('t',"target-text","TARGET_FILENAME","name of file containing target language corpus");
		//Option<String> target_file_encoding = commandLine.addStringOption("target-encoding","TARGET_ENCODING","ISO-8859-1","target language file encoding");
		Option<String> target_file_encoding = commandLine.addStringOption("target-encoding","TARGET_ENCODING","UTF-8","target language file encoding");
		Option<Boolean> target_file_gz = commandLine.addBooleanOption("target-text-gzipped",false,"is the target text gzipped");
		
		Option<String> alignment_file = commandLine.addStringOption('a',"alignment","ALIGNMENT_FILENAME","name of file containing word alignments for the sentences in the corpus");
		Option<Boolean> alignment_file_gz = commandLine.addBooleanOption("alignment-file-gzipped",false,"is the alignment file gzipped");

		Option<Integer> num_lines = commandLine.addIntegerOption('l',"lines","LINE_COUNT","number of aligned sentences in the corpus");
		
		Option<String> output_file = commandLine.addStringOption('o',"output","OUTPUT_FILENAME","file where aligned word pairs will be written");
		Option<String> output_file_encoding = commandLine.addStringOption("output-encoding","OUTPUT_ENCODING","UTF-8","output file encoding");
		Option<Boolean> output_file_gz = commandLine.addBooleanOption("output-text-gzipped",false,"should the output file be gzipped");
		
		commandLine.parse(args);
		
		
		try {
			
			// Set System.out and System.err to use the provided character encoding
			try {
				System.setOut(new PrintStream(System.out, true, commandLine.getValue(source_file_encoding)));
				System.setErr(new PrintStream(System.err, true, commandLine.getValue(source_file_encoding)));
			} catch (UnsupportedEncodingException e1) {
				System.err.println(commandLine.getValue(source_file_encoding) + " is not a valid encoding; using system default encoding for System.out and System.err.");
			} catch (SecurityException e2) {
				System.err.println("Security manager is configured to disallow changes to System.out or System.err; using system default encoding.");
			}
			
			// The number of lines to read
			int number_of_lines = commandLine.getValue(num_lines);

			// Set up the source text for reading
			Scanner source_text;
			if (commandLine.getValue(source_file).endsWith(".gz") || commandLine.getValue(source_file_gz))
				source_text = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(source_file))),commandLine.getValue(source_file_encoding))));
			else
				source_text = new Scanner( new File(commandLine.getValue(source_file)), commandLine.getValue(source_file_encoding));
			
			// Set up the target text for reading
			Scanner target_text;
			if (commandLine.getValue(target_file).endsWith(".gz") || commandLine.getValue(target_file_gz))
				target_text = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(target_file))),commandLine.getValue(target_file_encoding))));
			else
				target_text = new Scanner( new File(commandLine.getValue(target_file)), commandLine.getValue(target_file_encoding));
						
			// Set up the alignment file for reading
			Scanner alignments;
			if (commandLine.getValue(alignment_file).endsWith(".gz") || commandLine.getValue(alignment_file_gz))
				alignments = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(alignment_file))))));
			else
				alignments = new Scanner( new File(commandLine.getValue(alignment_file)));
			
			
			// Set up the output file for writing
			Writer outputFile;
			if (commandLine.getValue(output_file).endsWith(".gz") || commandLine.getValue(output_file_gz))
				outputFile = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(commandLine.getValue(output_file))),commandLine.getValue(output_file_encoding));
			else
				outputFile = new OutputStreamWriter(new FileOutputStream(commandLine.getValue(output_file)),commandLine.getValue(output_file_encoding));
			
			try {
				extract(number_of_lines, source_text, target_text, alignments, outputFile);
			} catch (NoSuchElementException e) {
				System.err.println("There are more than " + number_of_lines + " lines of input. Please determine the actual number of lines of input, and re-run with the appropriate command line flag.");
				commandLine.printUsage();
				System.exit(-1);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
