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
package joshua.decoder.ff.lm.bdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import joshua.util.CommandLineParser;
import joshua.util.CommandLineParser.Option;

/**
 * Utility program to create an interpolated ngram probability database, 
 * given an ARPA backoff N-gram model file trained using modified Kneser-Ney smoothing.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class CreateSmoothedNGramDB {

	/**
	 * Create a ngram probability database, given an ARPA backoff N-gram model file.
	 * 
	 * @param args command line arguments
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {

		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> directory = commandLine.addStringOption('d',"db-directory","DIRECTORY_NAME","database directory");
		Option<String> encoding = commandLine.addStringOption('e',"encoding","ENCODING","UTF-8","database encoding");		
		
		Option<String> ngram_file = commandLine.addStringOption('f',"ngram-file","FILE_NAME","ARPA N-gram model file");	
		Option<Boolean>ngram_file_gz = commandLine.addBooleanOption("ngram-file-gzipped",false,"is the ngram file gzipped");
		
		commandLine.parse(args);
		

		try {
			
			// Set up the source text for reading
			Scanner scanner;
			if (commandLine.getValue(ngram_file).endsWith(".gz") || commandLine.getValue(ngram_file_gz))
				scanner = new Scanner(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(commandLine.getValue(ngram_file))),commandLine.getValue(encoding))));
			else
				scanner = new Scanner( new File(commandLine.getValue(ngram_file)), commandLine.getValue(encoding));

			// Create a new unique temporary file
			//File tmpDB = File.createTempFile("tmp", "db");
			//String dbDirectoryName = tmpDB.getAbsolutePath();
			
			
			SmoothedNGramDB db = 
				new SmoothedNGramDB(scanner, commandLine.getValue(directory), commandLine.getValue(encoding));
			
			/*
			System.err.println("<s> the " + db.get("<s> the")); // -3.496579
			System.err.println("<s> the man " + db.get("<s> the man")); // -3.82591 = -3.496579 + -0.3293318
			System.err.println("the man in " + db.get("the man in"));
			System.err.println("in </s> " + db.get("in </s>"));
			*/
			
			db.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
					
		
		
	}
	
}
