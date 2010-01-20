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
package joshua.decoder.ff.lm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * Utility class for reading ARPA language model files.
 * 
 * @author Lane Schwartz
 */
public class ArpaFile implements Iterable<ArpaNgram> {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(ArpaFile.class.getName());
	
	/** Regular expression representing a blank line. */
	public static final Regex BLANK_LINE  = new Regex("^\\s*$");
	
	/** 
	 * Regular expression representing a line 
	 * starting a new section of n-grams in an ARPA language model file. 
	 */
	public static final Regex NGRAM_HEADER = new Regex("^\\\\\\d-grams:\\s*$");
	
	/** 
	 * Regular expression representing a line 
	 * ending an ARPA language model file. 
	 */
	public static final Regex NGRAM_END = new Regex("^\\\\end\\\\s*$");
	
	/** ARPA file for this object. */
	private final File arpaFile;
	
	/** The symbol table associated with this object. */
	private final SymbolTable vocab;
	
	/**
	 * Constructs an object that represents an ARPA language model file.
	 * 
	 * @param arpaFileName File name of an ARPA language model file
	 * @param vocab Symbol table to be used by this object
	 */
	public ArpaFile(String arpaFileName, SymbolTable vocab) {
		this.arpaFile = new File(arpaFileName);
		this.vocab = vocab;
	}

	public ArpaFile(String arpaFileName) throws IOException {
		this.arpaFile = new File(arpaFileName);
		this.vocab = new Vocabulary();
		
//		final Scanner scanner = new Scanner(arpaFile);
		
//		// Eat initial header lines
//		while (scanner.hasNextLine()) {
//			String line = scanner.nextLine();
//			logger.finest("Discarding line: " + line);
//			if (NGRAM_HEADER.matches(line)) {
//				break;
//			}
//		}
		
//		int ngramOrder = 1;
		
		LineReader grammarReader = new LineReader(arpaFileName);
		
		try {
			for (String line : grammarReader) {


//		while (scanner.hasNext()) {
//			
//			String line = scanner.nextLine();

				String[] parts = Regex.spaces.split(line);
				if (parts.length > 1) {
					String[] words = Regex.spaces.split(parts[1]);

					for (String word : words) {
						if (logger.isLoggable(Level.FINE)) logger.fine("Adding to vocab: " + word);
						vocab.addTerminal(word);	
					}

				} else {
					logger.info(line);
				}

			}
		} finally { 
			grammarReader.close(); 
		}

//			
//			boolean lineIsHeader = NGRAM_HEADER.matches(line);
//			
//			while (lineIsHeader || BLANK_LINE.matches(line)) {
//				
//				if (lineIsHeader) {
//					ngramOrder++;
//				}
//				
//				if (scanner.hasNext()) {
//					line = scanner.nextLine().trim();
//					lineIsHeader = NGRAM_HEADER.matches(line);
//				} else {
//					logger.severe("Ran out of lines!");
//					return;
//				}
//			}
			
			
//			
//			// Add word to vocab
//			if (logger.isLoggable(Level.FINE)) logger.fine("Adding word to vocab: " + parts[ngramOrder]);
//			vocab.addTerminal(parts[ngramOrder]);
//			
//			// Add context words to vocab
//			for (int i=1; i<ngramOrder; i++) {
//				if (logger.isLoggable(Level.FINE)) logger.fine("Adding context word to vocab: " + parts[i]);
//				vocab.addTerminal(parts[i]);
//			}
			
//		}
		
		logger.info("Done constructing ArpaFile");
		
	}
	
	/**
	 * Gets the symbol table associated with this object.
	 * 
	 * @return the symbol table associated with this object
	 */
	public SymbolTable getVocab() {
		return vocab;
	}
	
	/**
	 * Gets the total number of n-grams 
	 * in this ARPA language model file.
	 * 
	 * @return total number of n-grams 
	 *         in this ARPA language model file
	 */
	@SuppressWarnings("unused")
	public int size() {

		logger.fine("Counting n-grams in ARPA file");
		int count=0;
		
		for (ArpaNgram ngram : this) {
			count++;
		}
		logger.fine("Done counting n-grams in ARPA file");
		
		return count;
	}
	
	public int getOrder() throws FileNotFoundException {

		Pattern pattern = Pattern.compile("^ngram (\\d+)=\\d+$");
		if (logger.isLoggable(Level.FINEST)) logger.finest("Pattern is " + pattern.toString());
		final Scanner scanner = new Scanner(arpaFile);

		int order = 0;
		
		// Eat initial header lines
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			
			if (NGRAM_HEADER.matches(line)) {
				break;
			} else {
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()) {
					if (logger.isLoggable(Level.FINEST)) logger.finest("DOES   match: \'" + line + "\'");
					order = Integer.valueOf(matcher.group(1));
				} else if (logger.isLoggable(Level.FINEST)) {
					logger.finest("Doesn't match: \'" + line + "\'");
				}
			}
		}
		
		return order;
	}
	
	/**
	 * Gets an iterator capable of iterating 
	 * over all n-grams in the ARPA file.
	 * 
	 * @return an iterator capable of iterating 
	 *         over all n-grams in the ARPA file
	 */
	public Iterator<ArpaNgram> iterator() {

		try {
			final Scanner scanner;
			
			if (arpaFile.getName().endsWith("gz")) {
				InputStream in = new GZIPInputStream(
						new FileInputStream(arpaFile));
				scanner = new Scanner(in);
			} else {
				scanner = new Scanner(arpaFile);
			}
			
			// Eat initial header lines
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				logger.finest("Discarding line: " + line);
				if (NGRAM_HEADER.matches(line)) {
					break;
				}
			}
			
			return new Iterator<ArpaNgram>() {
				
				String nextLine = null;
				int ngramOrder = 1;
//				int id = 0;
				
				public boolean hasNext() {
					
					if (scanner.hasNext()) {
						
						String line = scanner.nextLine();
						
						boolean lineIsHeader = NGRAM_HEADER.matches(line) || NGRAM_END.matches(line);
						
						while (lineIsHeader || BLANK_LINE.matches(line)) {
							
							if (lineIsHeader) {
								ngramOrder++;
							}
							
							if (scanner.hasNext()) {
								line = scanner.nextLine().trim();
								lineIsHeader = NGRAM_HEADER.matches(line) || NGRAM_END.matches(line);
							} else {
								nextLine = null;
								return false;
							}
						}
						
						nextLine = line;
						return true;
						
					} else {
						nextLine = null;
						return false;
					}
					
				}

				public ArpaNgram next() {
					if (nextLine!=null) {
						
						String[] parts = Regex.spaces.split(nextLine);

						float value = Float.valueOf(parts[0]);
						
						int word = vocab.getID(parts[ngramOrder]);
						
						int[] context = new int[ngramOrder-1];
						for (int i=1; i<ngramOrder; i++) {
							context[i-1] = vocab.getID(parts[i]);
						}
						
						float backoff;
						if (parts.length > ngramOrder+1) {
							backoff = Float.valueOf(parts[parts.length-1]);
						} else {
							backoff = ArpaNgram.DEFAULT_BACKOFF;
						}
						
						nextLine = null;
						return new ArpaNgram(word, context, value, backoff);
						
					} else {
						throw new NoSuchElementException();
					}
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
		} catch (FileNotFoundException e) {
			logger.severe(e.toString());
			return null;
		} catch (IOException e) {
			logger.severe(e.toString());
			return null;
		}
		
	}
}
