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

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.decoder.ff.lm.ArpaFile;
import joshua.decoder.ff.lm.ArpaNgram;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.util.Bits;

/**
 * 
 * 
 * @author Lane Schwartz
 */
public class TrieLM extends DefaultNGramLanguageModel {

	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(TrieLM.class.getName());
	
//	/** Indicates that a node has no children. */
//	public static final int NO_CHILDREN = -1;
	
	/**
	 * Node ID for the root node.
	 */
	private static final int ROOT_NODE_ID = 0;
	
//	private static final int BACKOFF=Integer.MIN_VALUE;
	
//	/** 
//	 * Stores the language model back-off weight for all nodes.
//	 * <p>
//	 * The n'th float in this array is the back-off weight
//	 * for the node with node id n.
//	 * <p>
//	 * If a back-off weight for a particular node 
//	 * is not specified in the language model file,
//	 * the back-off weight for that node should be 1.0f.
//	 */
//	private final float[] backoffs;
//	
//	/** 
//	 * Stores the language model n-gram probability values for all nodes.
//	 * <p>
//	 * The <i>n</i>'th float in this array is the n-gram probability value
//	 * for the node with node id n.
//	 * <p>
//	 * If an n-gram probability value for a particular node 
//	 * is not specified in the language model file,
//	 * the n-gram probability value for that node is not valid.
//     * 
//     * The value {@link joshua.decoder.ff.lm.ArpaNgram#INVALID_VALUE} 
//     * is used to indicate that a value is invalid.
//	 */
//	private final float[] values;
	
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
	 * Maps from (node id, word id for lookup word) --> 
	 * log prob of lookup word given context 
	 * 
	 * (the context is defined by where you are in the tree).
	 */
	private final Map<Long,Float> backoffs;
	
	/**
	 * Constructs a language model object from the specified ARPA file.
	 * 
	 * @param arpaFile
	 * @throws FileNotFoundException 
	 */
	public TrieLM(ArpaFile arpaFile) throws FileNotFoundException {
		super(arpaFile.getVocab(), arpaFile.getOrder());
//	public TrieLM(SymbolTable symbolTable, int order, String arpaFileName) throws IOException {
//		super(symbolTable, order);
//		
//		ArpaFile arpaFile = new ArpaFile(arpaFileName, symbolTable);
		
		int ngramCounts = arpaFile.size();
		if (logger.isLoggable(Level.FINE)) logger.fine("ARPA file contains " + ngramCounts + " n-grams");
		
		this.children = new HashMap<Long,Integer>(ngramCounts);
		this.logProbs = new HashMap<Long,Float>(ngramCounts);
		this.backoffs = new HashMap<Long,Float>(ngramCounts);
		
		int nodeCounter = 0;
		
		for (ArpaNgram ngram : arpaFile) {
			
			logger.info(ngram.order() + "-gram: (" + ngram.getWord() + " | " + Arrays.toString(ngram.getContext()) + ")");
//			int nodeID = ngram.getID();
			int word = ngram.getWord();

			int[] context = ngram.getContext();
			
			// Find where the log prob and backoff should be stored
			int contextNodeID = ROOT_NODE_ID;
			{
				for (int i=context.length-1; i>=0; i--) {
					long key = Bits.encodeAsLong(contextNodeID, context[i]);
					int childID;
					if (children.containsKey(key)) {
						childID = children.get(key);
					} else {
						childID = ++nodeCounter;
						logger.info("children.put(" + contextNodeID + ":"+context[i] + " , " + childID + ")");
						children.put(key, childID);
					}
					contextNodeID = childID;
				}
			}
			
			// Store the log prob for this n-gram at this node in the trie
			{
				long key = Bits.encodeAsLong(contextNodeID, word);
				float logProb = ngram.getValue();
				logger.info("logProbs.put(" + contextNodeID + ":"+word + " , " + logProb);
				this.logProbs.put(key, logProb);
			}

			// Store the backoff for this n-gram at this node in the trie
			{
				long key = Bits.encodeAsLong(contextNodeID, word);
				float backoff = ngram.getBackoff();
				logger.info("backoffs.put(" + contextNodeID + ":" +word+" , " + backoff + ")");
				this.backoffs.put(key, backoff);
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
				if (backoffs.containsKey(key)) {
					backoff += backoffs.get(key);
				}
				
				if (children.containsKey(key)) {
					nodeID = children.get(key);
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
	
}
