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
package joshua.decoder.ff.lm.buildin_lm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.lm.ArpaFile;
import joshua.decoder.ff.lm.ArpaNgram;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.util.Bits;
import joshua.util.Regex;

/**
 * Relatively memory-compact language model
 * stored as a reversed-word-order trie.
 * <p>
 * The trie itself represents language model context.
 * <p>
 * Conceptually, each node in the trie stores a map 
 * from conditioning word to log probability.
 * <p>
 * Additionally, each node in the trie stores 
 * the backoff weight for that context.
 * 
 * @author Lane Schwartz
 * @see <a href="http://www.speech.sri.com/projects/srilm/manpages/ngram-discount.7.html">SRILM ngram-discount documentation</a>
 */
public class TrieLM extends DefaultNGramLanguageModel {

	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(TrieLM.class.getName());
	
	/**
	 * Node ID for the root node.
	 */
	private static final int ROOT_NODE_ID = 0;
	
	
	/** 
	 * Maps from (node id, word id for child) --> node id of child. 
	 */
	private final Map<Long,Integer> children;
	
	/**
	 * Maps from (node id, word id for lookup word) --> 
	 * log prob of lookup word given context 
	 * 
	 * (the context is defined by where you are in the tree).
	 */
	private final Map<Long,Float> logProbs;
	
	/**
	 * Maps from (node id) --> 
	 * backoff weight for that context 
	 * 
	 * (the context is defined by where you are in the tree).
	 */
	private final Map<Integer,Float> backoffs;
	
	/**
	 * Constructs a language model object from the specified ARPA file.
	 * 
	 * @param arpaFile
	 * @throws FileNotFoundException 
	 */
	public TrieLM(ArpaFile arpaFile) throws FileNotFoundException {
		super(arpaFile.getVocab(), arpaFile.getOrder());
		
		int ngramCounts = arpaFile.size();
		if (logger.isLoggable(Level.FINE)) logger.fine("ARPA file contains " + ngramCounts + " n-grams");
		
		this.children = new HashMap<Long,Integer>(ngramCounts);
		this.logProbs = new HashMap<Long,Float>(ngramCounts);
		this.backoffs = new HashMap<Integer,Float>(ngramCounts);
		
		int nodeCounter = 0;
		
		int lineNumber = 0;
		for (ArpaNgram ngram : arpaFile) {
			lineNumber += 1;
			if (lineNumber%100000==0) logger.info("Line: " + lineNumber);
			
			if (logger.isLoggable(Level.FINEST)) logger.finest(ngram.order() + "-gram: (" + ngram.getWord() + " | " + Arrays.toString(ngram.getContext()) + ")");
			int word = ngram.getWord();

			int[] context = ngram.getContext();
			
			{
				// Find where the log prob should be stored
				int contextNodeID = ROOT_NODE_ID;
				{
					for (int i=context.length-1; i>=0; i--) {
						long key = Bits.encodeAsLong(contextNodeID, context[i]);
						int childID;
						if (children.containsKey(key)) {
							childID = children.get(key);
						} else {
							childID = ++nodeCounter;
							if (logger.isLoggable(Level.FINEST)) logger.finest("children.put(" + contextNodeID + ":"+context[i] + " , " + childID + ")");
							children.put(key, childID);
						}
						contextNodeID = childID;
					}
				}

				// Store the log prob for this n-gram at this node in the trie
				{
					long key = Bits.encodeAsLong(contextNodeID, word);
					float logProb = ngram.getValue();
					if (logger.isLoggable(Level.FINEST)) logger.finest("logProbs.put(" + contextNodeID + ":"+word + " , " + logProb);
					this.logProbs.put(key, logProb);
				}
			}
			
			{
				// Find where the backoff should be stored
				int backoffNodeID = ROOT_NODE_ID;
				{	
					long backoffNodeKey = Bits.encodeAsLong(backoffNodeID, word);
					int wordChildID;
					if (children.containsKey(backoffNodeKey)) {
						wordChildID = children.get(backoffNodeKey);
					} else {
						wordChildID = ++nodeCounter;
						if (logger.isLoggable(Level.FINEST)) logger.finest("children.put(" + backoffNodeID + ":"+word + " , " + wordChildID + ")");
						children.put(backoffNodeKey, wordChildID);
					}
					backoffNodeID = wordChildID;

					for (int i=context.length-1; i>=0; i--) {
						long key = Bits.encodeAsLong(backoffNodeID, context[i]);
						int childID;
						if (children.containsKey(key)) {
							childID = children.get(key);
						} else {
							childID = ++nodeCounter;
							if (logger.isLoggable(Level.FINEST)) logger.finest("children.put(" + backoffNodeID + ":"+context[i] + " , " + childID + ")");
							children.put(key, childID);
						}
						backoffNodeID = childID;
					}
				}
				
				// Store the backoff for this n-gram at this node in the trie
				{
					float backoff = ngram.getBackoff();
					if (logger.isLoggable(Level.FINEST)) logger.finest("backoffs.put(" + backoffNodeID + ":" +word+" , " + backoff + ")");
					this.backoffs.put(backoffNodeID, backoff);
				}
			}
			
		}
	}
	
	@Override
	public double ngramLogProbability(int[] ngram, int order) {
		
		float logProb = Float.NEGATIVE_INFINITY; // log(0.0f)
		float backoff = 0.0f; // log(1.0f)
		
		int i = ngram.length - 1;
		int word = ngram[i];
		i -= 1;
		
		int nodeID = ROOT_NODE_ID;
		
		while (true) {
		
			{
				long key = Bits.encodeAsLong(nodeID, word);
				if (logProbs.containsKey(key)) {
					logProb = logProbs.get(key);
					backoff = 0.0f; // log(0.0f)
				}
			}
			
			if (i < 0) {
				break;
			}
			
			{
				long key = Bits.encodeAsLong(nodeID, ngram[i]);
				
				if (children.containsKey(key)) {
					nodeID = children.get(key);
					
					backoff += backoffs.get(nodeID);
				
					i -= 1;
					
				} else {
					break;
				}
			}
			
		}
		
		return logProb + backoff;
	}
	
	public Map<Long,Integer> getChildren() {
		return this.children;
	}

	public static void main(String[] args) throws FileNotFoundException {
		
		logger.info("Constructing ARPA file");
		ArpaFile arpaFile = new ArpaFile(args[0]);
		
		logger.info("Getting symbol table");
		SymbolTable vocab = arpaFile.getVocab();
		
		logger.info("Constructing TrieLM");
		TrieLM lm = new TrieLM(arpaFile);
		
		int n = Integer.valueOf(args[2]);
		logger.info("N-gram order will be " + n);
		
		Scanner scanner = new Scanner(new File(args[1]));
		
		LinkedList<String> wordList = new LinkedList<String>();
		LinkedList<String> window = new LinkedList<String>();
		
		logger.info("Starting to scan " + args[1]);
		while (scanner.hasNext()) {
			
			logger.info("Getting next line...");
			String line = scanner.nextLine();
			logger.info("Line: " + line);
			
			String[] words = Regex.spaces.split(line);
			wordList.clear();
			
			wordList.add("<s>");
			for (String word : words) {
				wordList.add(word);
			}
			wordList.add("</s>");
			
			while (! wordList.isEmpty()) {
				window.clear();

				{
					int i=0;
					for (String word : wordList) {
						if (i>=n) break;
						window.add(word);
						i++;
					}
					wordList.remove();
				}

				{
					int i=0;
					int[] wordIDs = new int[window.size()];
					for (String word : window) {
						wordIDs[i] = vocab.getID(word);
						i++;
					}

					logger.info("logProb " + window.toString() + " = " + lm.ngramLogProbability(wordIDs, n));
				}
			}
		}
		
	}
	
}
