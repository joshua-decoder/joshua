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
package joshua.decoder.ff.lm.bdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import joshua.util.FileUtility;
import joshua.util.FormatUtil;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * NGram log probabilities distribution, stored as a Berkeley DB JE database.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SmoothedNGramDB extends LanguageModel {

	private Environment dbEnvironment;
	private Database ngramsdb;
	private final String encoding;
	private final TupleBinding binding;
	
	public static final String ngramsDatabaseName = "smoothedNgramsDatabase";
	
	/**
	 * Construct an SmoothedNGramDB object 
	 * by opening an existing Berkeley DB JE database which
	 * already contains the ngram data.
	 * 
	 * @param dbDirectoryName location of the database
	 * @param dbEncoding text encoding used by the database
	 * @throws FileNotFoundException
	 */
	public SmoothedNGramDB(String dbDirectoryName, String dbEncoding) throws FileNotFoundException {
		
		File dbDirectory = new File(dbDirectoryName);
		if (! dbDirectory.exists()) {
			throw new FileNotFoundException("Directory " + dbDirectoryName + " does not exist.");
		} 
		
		this.encoding = dbEncoding;
		this.binding = new NGramBinding();
		
		try {		
			// Open the environment. Create it if it does not already exist.
			EnvironmentConfig envConfig = new EnvironmentConfig();
			//envConfig.setAllowCreate(true);
			envConfig.setReadOnly(true);
			dbEnvironment = new Environment(dbDirectory, envConfig);

		    // Open the database. Create it if it does not already exist.
		    DatabaseConfig dbConfig = new DatabaseConfig();
		    dbConfig.setReadOnly(true);
		    dbConfig.setAllowCreate(false);
		    dbConfig.setSortedDuplicates(false);
		    
		    ngramsdb = dbEnvironment.openDatabase(null, ngramsDatabaseName, dbConfig); 
		    
		} catch (DatabaseException e) {
		    // Exception handling goes here
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Construct an object from ARPA backoff N-gram model file
	 * 
	 * @param scanner configured to read an ARPA backoff N-gram model file
	 * @param dbDirectoryName location where the database will be created
	 * @param dbEncoding text encoding to be used by the database
	 */
	public SmoothedNGramDB(Scanner scanner, String dbDirectoryName, String dbEncoding) {

		File dbDirectory = new File(dbDirectoryName);
		if (dbDirectory.exists()) {
			System.err.println("Warning - directory exists - deleting it now");
			FileUtility.delete(dbDirectory);
		} 
		dbDirectory.mkdir();
		
		this.encoding = dbEncoding;
		this.binding = new NGramBinding();
		
		try {		
			// Open the environment. Create it if it does not already exist.
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			dbEnvironment = new Environment(dbDirectory, envConfig);

			// Open the database. Create it if it does not already exist.
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setAllowCreate(true);
			dbConfig.setSortedDuplicates(false);

			ngramsdb = dbEnvironment.openDatabase(null, ngramsDatabaseName, dbConfig); 

			// Eat header up through \data\
			while (!scanner.nextLine().equals("\\data\\"));

			int maxOrder = 0;

			Map<Integer,Integer> ngramCounts = new HashMap<Integer,Integer>();
			
			for (String line=scanner.nextLine(); line.startsWith("ngram"); line=scanner.nextLine()) {
				int index = line.indexOf("=");
				int order = Integer.valueOf(line.substring(line.indexOf(' ')+1, index));
				int count = Integer.valueOf(line.substring(index+1, line.length()));
				maxOrder = order;
				ngramCounts.put(order, count);
			}

			for (int order=1; order <= maxOrder; order++) {
				
				// Eat header up through \order-grams:
				while (!scanner.nextLine().startsWith("\\" + order + "-grams:"));

				for (String line=scanner.nextLine().trim(); line.trim().length() > 0; line=scanner.nextLine().trim()) {

					String[] elements = line.split("\\s+");

					String ngram = elements[1];
					for (int i=2; i<=order; i++) {
						ngram += " " + elements[i];
					}

					// Read in the base-10 log, and convert to natural log
					double logProb = Math.log(Math.pow(10, Double.valueOf(elements[0])));
					
					double backoff;
					if (elements.length==order+2 && FormatUtil.isNumber(elements[elements.length-1])) {
						backoff = Math.log(Math.pow(10, Double.valueOf(elements[elements.length-1])));
					} else {
						backoff = 0.0; // Math.log(1)
					}

					// Enter the line into the appropriate map
					this.put(ngram, logProb, backoff);

				}
			}

			// Eat footer up through \data\
			while (!scanner.nextLine().equals("\\end\\"));

		} catch (DatabaseException e) {
		    // Exception handling goes here
			e.printStackTrace();
		} catch (NoSuchElementException e) {
			throw new RuntimeException("Input is not a properly formatted ARPA backoff N-gram model file format");
		}
	}
	
	/**
	 * Close the underlying database.
	 */
	public void close() {
		try {
			ngramsdb.close();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Store a key and its associated 
	 * log probability and backoff weight 
	 * in the Berkeley database.
	 *  
	 * @param key The phrase to store.
	 * @param logProb N-gram log probability of the phrase.
	 * @param backoff Backoff weight for the phrase.
	 */
	private void put(String key, double logProb, double backoff) {
		try {
			
			if (Double.isInfinite(logProb) || Double.isNaN(logProb)) {
				System.err.println("WARNING: " + key + " has logProb==" + logProb);
				System.exit(-1);
			}
				
			if (Double.isInfinite(backoff) || Double.isNaN(backoff)) {
				System.err.println("WARNING: " + key + " has backoff==" + backoff);
				System.exit(-1);
			}
			
			SmoothedNGram value = new SmoothedNGram(logProb, backoff);
			
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes(encoding));    

			// Now build the DatabaseEntry using a TupleBinding
			
			DatabaseEntry theData = new DatabaseEntry();
			binding.objectToEntry(value, theData);

			// Now store it
			ngramsdb.put(null, theKey, theData);
		} catch (UnsupportedEncodingException e) {
			System.err.println(encoding + " is not a valid database encoding format");
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the smoothed n-gram log probability of the specified key.
	 * <p>
	 * The algorithm used in this method is based on the one used 
	 * in the wordProbBO method of the SRILM toolkit's Ngram class.
	 * 
	 * @param key The phrase to look up
	 * @return the smoothed n-gram log probability of the provided phrase
	 * @see <a href="http://www.speech.sri.com/projects/srilm/">SRILM Toolkit</a>
	 */
	public double get(String key) {
				
/*		 

reading 10209 1-grams
reading 78195 2-grams
reading 20317 3-grams
the man in
        p( the | <s> )  = [2gram] 0.139956 [ -0.854009 ] / 1
        p( man | the ...)       = [2gram] 0.00014931 [ -3.82591 ] / 1
        p(the man) + bow(<s> the)
        
        p( in | man ...)        = [3gram] 0.221919 [ -0.653806 ] / 1
        p( </s> | in ...)       = [1gram] 0.000225094 [ -3.64764 ] / 1
        p(</s>) + bow(in)
        
1 sentences, 3 words, 0 OOVs
0 zeroprobs, logprob= -8.98136 ppl= 175.93 ppl1= 985.797

file sample.txt: 1 sentences, 3 words, 0 OOVs
0 zeroprobs, logprob= -8.98136 ppl= 175.93 ppl1= 985.797

*/
		
		String[] completePhrase = key.split("\\s+");
		
		int contextLength = completePhrase.length - 1;
		
		double logProb = Double.NEGATIVE_INFINITY; // Math.log(0)
		double backoffWeight = 0.0; // Math.log(1)
		int i = 0;
		String primaryPhrase = completePhrase[completePhrase.length - 1];
		String backoffPhrase = "";
		
		
		while (true) {
			
			SmoothedNGram ngram = getSmoothedNGram(primaryPhrase);
		
			if (ngram != null) {
				logProb = ngram.getLogProb();
				backoffWeight = 0.0; // Math.log(1)
			}
			
			if  (i >= contextLength) break;
			
			if (backoffPhrase.equals("")) {
				backoffPhrase = completePhrase[i];
			} else {
				backoffPhrase = backoffPhrase + " " + completePhrase[i];
			}
			
			SmoothedNGram backoffNGram = getSmoothedNGram(backoffPhrase);
			
			if (backoffNGram != null) {
				backoffWeight += backoffNGram.getBackoff();
				i++;
				
				primaryPhrase = completePhrase[completePhrase.length - 1 - i] + " " + primaryPhrase;
			} else {
				break;
			}
		}

		return logProb + backoffWeight;
	}

	/**
	 * Gets the smoothed ngram associated with the key.
	 * <p>
	 * This method has package-private scope so that unit tests can access it.
	 * 
	 * @param key The phrase to look up
	 * @return the smoothed n-gram log probability of the provided phrase
	 */
	SmoothedNGram getSmoothedNGram(String key) {
		// Read the db
		try {
		    // Create a pair of DatabaseEntry objects. theKey
		    // is used to perform the search. theData is used
		    // to store the data returned by the get() operation.
		    DatabaseEntry theKey = new DatabaseEntry(key.getBytes(encoding));
		    DatabaseEntry theData = new DatabaseEntry();
		    
		    // Perform the get.
		    if (ngramsdb.get(null, theKey, theData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {

		        // Recreate the data
		    	@SuppressWarnings("unchecked")
		    	SmoothedNGram ngram = (SmoothedNGram)binding.entryToObject(theData);
		        return ngram;

		    } else {
		        return null;
		    } 
		} catch (Exception e) {
		    // Exception handling goes here
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Represents a smoothed ngram.
	 * <p>
	 * This class has package-private scope so that unit tests can access it.
	 * 
	 * @author Lane Schwartz
	 */
	static class SmoothedNGram {
		
		private final double logProb;
		private final double backoff;
		
		SmoothedNGram(double logProb, double backoff) {
			this.logProb = logProb;
			this.backoff = backoff;
		}
		
		double getLogProb() {
			return logProb;
		}
		
		double getBackoff() {
			return backoff;
		}
	}
	
	private static class NGramBinding extends TupleBinding {

		@Override
		public SmoothedNGram entryToObject(TupleInput input) {
			double logProb = input.readDouble();
			double backoff = input.readDouble();
			
			return new SmoothedNGram(logProb, backoff);	
		}

		@Override
		public void objectToEntry(Object object, TupleOutput output) {
			SmoothedNGram rule = (SmoothedNGram) object;
			output.writeDouble(rule.getLogProb());
			output.writeDouble(rule.getBackoff());
		}
		
	}
	
}
