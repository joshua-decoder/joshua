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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
//import java.util.logging.Level;
//import java.util.logging.Logger;

//import joshua.util.CommandLineParser;
//import joshua.util.CommandLineParser.Option;
import joshua.sarray.FileUtil;
import joshua.sarray.HierarchicalPhrase;
import joshua.util.Pair;
import joshua.util.sentence.Vocabulary;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;


/**
 * Lexical translation probability distribution, stored as a Berkeley DB JE database.
 * <p>
 * This class works only with regular probabilities, not with log probabibilities.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class LexProbsDB implements LexicalProbabilities {

	/** Use the non-printable character ASCII UNIT SEPARATOR (\u001F) as delimiter. */
	private static final char DELIMITER = '\u001F';
	
	/** Logger for this class. */
	//private static final Logger logger = Logger.getLogger(LexProbsDB.class.getName());
	
	private Environment dbEnvironment;
	private Database targetGivenSourceDatabase;
	private Database sourceGivenTargetDatabase;
	
	private final String encoding;
	
	private final File dbDirectory;
	
	private final Vocabulary sourceVocab;
	private final Vocabulary targetVocab;
	
	/**
	 * Construct a LexicalTranslationProbabilityDistribution 
	 * by opening an existing Berkeley DB JE database which
	 * already contains the lexical translation probability distribution data.
	 * 
	 * @param dbDirectoryName location of the database
	 * @param dbEncoding text encoding used by the database
	 * @throws FileNotFoundException
	 */
	public LexProbsDB(String dbDirectoryName, String dbEncoding, Vocabulary sourceVocab, Vocabulary targetVocab) throws FileNotFoundException {
		
		this.sourceVocab = sourceVocab;
		this.targetVocab = targetVocab;
		
		dbDirectory = new File(dbDirectoryName);
		if (! dbDirectory.exists()) {
			throw new FileNotFoundException("Directory " + dbDirectoryName + " does not exist.");
		} 
		
		this.encoding = dbEncoding;
		
		try {		
			// Open the environment. Create it if it does not already exist.
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			dbEnvironment = new Environment(dbDirectory, envConfig);

		    // Open the database. Create it if it does not already exist.
		    DatabaseConfig dbConfig = new DatabaseConfig();
		    dbConfig.setAllowCreate(false);
		    dbConfig.setSortedDuplicates(false);
		    
		    targetGivenSourceDatabase = dbEnvironment.openDatabase(null, "lexicalTargetGivenSourceDatabase", dbConfig); 
		    sourceGivenTargetDatabase = dbEnvironment.openDatabase(null, "lexicalSourceGivenTargetDatabase", dbConfig); 
		 
		} catch (DatabaseException e) {
		    // Exception handling goes here
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Construct a LexicalTranslationProbabilityDistribution
	 * by reading word pair count data from 
	 * source-to-target word pair count data and target-to-source word pair count data.
	 * <p>
	 * The constructor calculates the lexical translation probability distributions 
	 * specified by the word pair count data,
	 * and stores the lexical translation probability distributions in a new Berkeley DB JE database 
	 * stored in the specified directory.
	 * 
	 * @param sourceToTargetFile source-to-target word pair counts
	 * @param targetToSourceFile target-to-source word pair counts
	 * @param dbDirectoryName location where the database will be created
	 * @param dbEncoding text encoding to be used by the database
	 */
	public LexProbsDB(Scanner sourceToTargetFile, Scanner targetToSourceFile, String dbDirectoryName, String dbEncoding, Vocabulary sourceVocab, Vocabulary targetVocab) {
		
		this.sourceVocab = sourceVocab;
		this.targetVocab = targetVocab;
		
		dbDirectory = new File(dbDirectoryName);
		if (dbDirectory.exists()) {
			System.err.println("Warning - directory exists - deleting it now");
			FileUtil.delete(dbDirectory);
		} 
		dbDirectory.mkdir();
		
		this.encoding = dbEncoding;
		
		try {		
			// Open the environment. Create it if it does not already exist.
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			dbEnvironment = new Environment(dbDirectory, envConfig);

			
		    // Open the database. Create it if it does not already exist.
		    DatabaseConfig dbConfig = new DatabaseConfig();
		    dbConfig.setAllowCreate(true);
		    dbConfig.setSortedDuplicates(false);
		    
		    targetGivenSourceDatabase = dbEnvironment.openDatabase(null, "lexicalTargetGivenSourceDatabase", dbConfig); 
		    sourceGivenTargetDatabase = dbEnvironment.openDatabase(null, "lexicalSourceGivenTargetDatabase", dbConfig); 
		 
		} catch (DatabaseException e) {
		    // Exception handling goes here
			e.printStackTrace();
		}
		
		//System.out.println("Calculating source given target");
		calculateDBProbabilities(sourceToTargetFile, this.targetGivenSourceDatabase);
		
		//System.out.println("Calculating target given source");
		calculateDBProbabilities(targetToSourceFile, this.sourceGivenTargetDatabase);
		
	}
	
	/**
	 * Close the underlying database.
	 * <p>
	 * This method <em>must</em> be called in order to cleanly close the underlying database.
	 * If this method is not called, there is significant risk of corrupting the underlying database.
	 */
	public void close() {
		try {
			
			if (targetGivenSourceDatabase != null) {
				targetGivenSourceDatabase.close();
	        }
			
			if (sourceGivenTargetDatabase != null) {
				sourceGivenTargetDatabase.close();
			}

	        if (dbEnvironment != null) {
	            dbEnvironment.close();
	        }
			
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
	
	
	
	private void calculateDBProbabilities(Scanner inputFile, Database probabilities) {

		boolean finished = false;
		String alpha = null;
		int denominator = 0;
		Map<String,Integer> word_pairs = new HashMap<String,Integer>();
		int count = 0;
		while (! finished) {

			if (inputFile.hasNextLine()) {
				count++;
				if (count % 10000==0) System.err.print(".");
				
				String[] data = inputFile.nextLine().trim().split("\\s+");
				
				if (data.length == 3) {
					int pair_count = Integer.valueOf(data[0]);
					String word = data[2];
					String given_word = data[1];
					
					if (!given_word.equals(alpha) && alpha!=null ) {

						for (Map.Entry<String, Integer> entry : word_pairs.entrySet()) {
							//System.out.println("Entering: " + entry.getKey() + DELIMITER + alpha + " == " +((double)entry.getValue() / (double)denominator));
							enterData(entry.getKey() + DELIMITER + alpha, ((double)entry.getValue() / (double)denominator), probabilities);
						}
						
						word_pairs.clear();
						denominator = 0;
						alpha = given_word;
						
					}
					
					// What was this supposed to do? I can't make sense of it, so I'm commenting it out.
					//if (! word_pairs.containsKey(word))
					//	word_pairs.put(given_word, 0);

					word_pairs.put(word, pair_count);
					denominator += pair_count;
					if (alpha==null) alpha = given_word;
					
				} else {
					System.out.println("Done reading input");
					finished = true;
				}
			} else {
				finished = true;
			}
			
			
		}
		
		if (alpha != null  &&  denominator > 0) {
			for (Map.Entry<String, Integer> entry : word_pairs.entrySet()) {
				enterData(entry.getKey() + DELIMITER + alpha, ((double)entry.getValue() / (double)denominator), probabilities);
			}
		}
	
		System.err.println();
	}
	
	private void enterData(String key, Double value, Database probabilities) {
		try {
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes(encoding));    

			// Now build the DatabaseEntry using a TupleBinding
			
			DatabaseEntry theData = new DatabaseEntry();
			EntryBinding myBinding = TupleBinding.getPrimitiveBinding(Double.class);
			myBinding.objectToEntry(value, theData);

			// Now store it
			probabilities.put(null, theKey, theData);
		} catch (UnsupportedEncodingException e) {
			System.err.println(encoding + " is not a valid database encoding format");
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Look up the lexical translation probability of a source language word given a target language word.
	 * 
	 * @param sourceWord the source language word
	 * @param targetWord the target language word
	 * @return the lexical translation probability
	 */
	public float sourceGivenTarget(String sourceWord, String targetWord) {
		return (float) lookup((sourceWord + DELIMITER + targetWord),this.sourceGivenTargetDatabase);
	}
	
	/**
	 * Look up the lexical translation probability of a target language word given a source language word.
	 * 
	 * @param targetWord the target language word
	 * @param sourceWord the source language word
	 * @return the lexical translation probability
	 */
	public float targetGivenSource(String targetWord,String sourceWord) {
		return (float) lookup((targetWord + DELIMITER + sourceWord),this.targetGivenSourceDatabase);
	}
	
	public float sourceGivenTarget(Integer sourceWord, Integer targetWord) {
		return sourceGivenTarget(sourceVocab.getWord(sourceWord), targetVocab.getWord(targetWord));
	}

	public float targetGivenSource(Integer targetWord, Integer sourceWord) {
		return targetGivenSource(targetVocab.getWord(targetWord), sourceVocab.getWord(sourceWord));
	}
	
	private double lookup(String key, Database probabilities) {
		try {
			// Need a key for the get
			DatabaseEntry theKey = new DatabaseEntry(key.getBytes(encoding));

			// Need a DatabaseEntry to hold the associated data.
			DatabaseEntry theData = new DatabaseEntry();

			// Bindings need only be created once for a given scope
			EntryBinding myBinding = TupleBinding.getPrimitiveBinding(Double.class);

			// Get it
			OperationStatus retVal = probabilities.get(null, theKey, theData,LockMode.DEFAULT);

			//String retKey = null;
			if (retVal == OperationStatus.SUCCESS) {
				// Recreate the data.
				// Use the binding to convert the byte array contained in theData
				// to a Long type.
				Double value = (Double) myBinding.entryToObject(theData);
				//retKey = new String(theKey.getData(), encoding);
				//System.out.println("For key: '" + retKey + "' found Double: '" + value + "'.");
				return value;
			} else {
				//System.out.println("No record found for key '" + retKey + "'.");
				//return Double.NEGATIVE_INFINITY;
				return 0.0;
			}
		} catch (UnsupportedEncodingException e) {
			System.err.println(encoding + " is not a valid database encoding format");
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		} 
		
		// This code should never be reached
		return Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Delete the database directory backing this object. Use with care!
	 * 
	 * @return <code>true</code> if the backing directory was deleted, <code>false</code> otherwise
	 */
	public boolean delete() {
		return FileUtil.delete(dbDirectory);
	}

	
	/**
	 * Sample code for querying a lexprob database
	 * 
	 * @param args
	 */
	/*
	public static void main(String[] args) {
		
		CommandLineParser commandLine = new CommandLineParser();
		
		Option<String> encoding = commandLine.addStringOption('e',"encoding","ENCODING","UTF-8","input file encoding");		
		Option<String> dbDirectory = commandLine.addStringOption('d',"db-directory","DIRECTORY_NAME","database directory");
		
		Option<String> source_word = commandLine.addStringOption('s',"source-word","WORD","source word to look up");
		Option<String> target_word = commandLine.addStringOption('t',"target-word","WORD","target word to look up");
		
		
		commandLine.parse(args);
		
		LexProbsDB lexProbs = null;
		
		try {
			
			lexProbs = 
				new LexProbsDB(commandLine.getValue(dbDirectory), commandLine.getValue(encoding));
			
			System.out.println(lexProbs.sourceGivenTarget(commandLine.getValue(source_word), commandLine.getValue(target_word)));
			
			System.out.println(lexProbs.targetGivenSource(commandLine.getValue(target_word), commandLine.getValue(source_word)));
						
		}  catch (FileNotFoundException e) {
			
			e.printStackTrace();
		
		} catch (Error e) {
			
			if (lexProbs != null) {
				lexProbs.close();
				lexProbs = null;
			}
			
			throw e;
			
		} catch (RuntimeException e) {
			
			if (lexProbs != null) {
				lexProbs.close();
				lexProbs = null;
			}
			
			throw e;
			
		} finally {
			
			if (lexProbs != null) {
				lexProbs.close();
			}
			
		}
	}
	*/
	
	public Pair<Float, Float> calculateLexProbs(HierarchicalPhrase sourcePhrase) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/**
	 * To be used only during unit testing.
	 */
	LexProbsDB(){
		this.sourceVocab = null;
		this.targetVocab = null;
		encoding = null;
		dbDirectory = null;
	}

}
