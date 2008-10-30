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


/**
 * Utility program to create a lexical translation probability database, 
 * given word pair count data.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class CreateLexProbDB {

	/**
	 * Create a lexical translation probability database, given word pair count data.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> source_to_target_counts = commandLine.addStringOption('s',"source-to-target-counts","FILENAME","name of file containing source to target word pair counts");
		Option<String> target_to_source_counts = commandLine.addStringOption('t',"target-to-source-counts","FILENAME","name of file containing target to source word pair counts");
		
		Option<String> encoding = commandLine.addStringOption('e',"encoding","ENCODING","UTF-8","input file encoding");
		
		Option<Boolean> source_to_target_gz = commandLine.addBooleanOption("source-to-target-gzipped",false,"is the source to target word pair counts file gzipped");
		Option<Boolean> target_to_source_gz = commandLine.addBooleanOption("target-to-source-gzipped",false,"is the target to source word pair counts file gzipped");
		
		Option<String> dbDirectory = commandLine.addStringOption('d',"db-directory","DIRECTORY_NAME","database directory");
		
		commandLine.parse(args);
		
		LexProbsDB lexProbs = null;
		
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
			Scanner source_to_target;
			if (commandLine.getValue(source_to_target_counts).endsWith(".gz") || commandLine.getValue(source_to_target_gz))
				source_to_target = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(source_to_target_counts))),commandLine.getValue(encoding))));
			else
				source_to_target = new Scanner( new File(commandLine.getValue(source_to_target_counts)), commandLine.getValue(encoding));

			
			// Set up the target text for reading
			Scanner target_to_source;
			if (commandLine.getValue(target_to_source_counts).endsWith(".gz") || commandLine.getValue(target_to_source_gz))
				target_to_source = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(target_to_source_counts))),commandLine.getValue(encoding))));
			else
				target_to_source = new Scanner( new File(commandLine.getValue(target_to_source_counts)), commandLine.getValue(encoding));
			
			
			lexProbs = new LexProbsDB(source_to_target, target_to_source,commandLine.getValue(dbDirectory), commandLine.getValue(encoding), null, null);
			
		}  catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
			if (lexProbs != null) {
				lexProbs.close();
			}
			
		}
	}
	
}
