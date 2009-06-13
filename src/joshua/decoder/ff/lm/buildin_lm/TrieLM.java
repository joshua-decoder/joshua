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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.lm.DefaultNGramLanguageModel;
import joshua.util.Regex;
import joshua.util.io.LineReader;

/**
 * 
 * 
 * @author Lane Schwartz
 * @author Zhifei Li
 */
public class TrieLM extends DefaultNGramLanguageModel {

	/** Logger for this class. */
	private static Logger logger =
		Logger.getLogger(TrieLM.class.getName());
	
	/** Indicates an invalid probability value. */
	public static final float INVALID_VALUE = Float.NaN;
	
	/** Indicates that a node has no children. */
	public static final int NO_CHILDREN = -1;
	
	/** 
	 * Stores the language model back-off weight for all nodes.
	 * <p>
	 * The n'th float in this array is the back-off weight
	 * for the node with node id n.
	 * <p>
	 * If a back-off weight for a particular node 
	 * is not specified in the language model file,
	 * the back-off weight for that node should be 1.0f.
	 */
	private final float[] backoffs;
	
	/** 
	 * Stores the language model n-gram probability values for all nodes.
	 * <p>
	 * The <i>n</i>'th float in this array is the n-gram probability value
	 * for the node with node id n.
	 * <p>
	 * If an n-gram probability value for a particular node 
	 * is not specified in the language model file,
	 * the n-gram probability value for that node is not valid.
     * 
     * The value INVALID_VALUE is used to indicate that a value is invalid.
	 */
	private final float[] values;
	
	/** 
	 * Maps from (node id, word id for child) --> node id of child. 
	 */
	private final Map<Long,Integer> children;
	
	
//	private final int[] childrenStartIndex;
//	
//	/**
//	 * For each node, stores the node id of the first child of that node.
//	 * <p>
//	 * The <i>n</i>'th int in this array is
//	 * the node id of the first child of the node with node id n.
//	 * <p>
//	 * If a node is a leaf node, 
//	 * the constant NO_CHILDREN should be stored for that node.
//	 */
//	private final int[] firstChildID;
	
	public TrieLM(SymbolTable symbolTable, int order, String arpaFile) throws IOException {
		super(symbolTable, order);
		
		int ngramCounts = countNGrams(order, arpaFile);
		if (logger.isLoggable(Level.FINE)) logger.fine("ARPA file contains " + ngramCounts + " n-grams");
		
		
		this.backoffs = new float[ngramCounts];
		this.values = new float[ngramCounts];
//		this.childrenStartIndex = new int[ngramCounts];
//		this.firstChildID = new int[ngramCounts];
		
		this.children = new HashMap<Long,Integer>(ngramCounts);
//		this.children = getKids(ngramCounts);//new ArrayList<Integer>(ngramCounts);
		

	}

//	static int[] getKids(int ngramCounter) {
//		
//		ArrayList<Integer> kids = new ArrayList<Integer>(ngramCounter);
//		
//		//FIXME Implement this part
//		
//		
//		
//		
//		
//		int[] children = new int[kids.size()];
//		for (int i=0, n=kids.size(); i<n; i++) {
//			children[i] = kids.get(i);
//		}
//		
//		return children;
//	}
	
	/**
	 * 
	 * 
	 */
	static int countNGrams(int ngramOrder, String arpaFile) throws IOException {

		int counter = 0;

		boolean start = false;
		int order = 0;

		Regex blankLine  = new Regex("^\\s*$");
		Regex ngramsLine = new Regex("^\\\\\\d-grams:\\s*$");

		LineReader grammarReader = new LineReader(arpaFile);
		try { 
			for (String line : grammarReader) {
				line = line.trim();
				if (blankLine.matches(line)) {
					continue;
				}
				if (ngramsLine.matches(line)) { // \1-grams:
					start = true;
					order = Integer.parseInt(line.substring(1, 2));
					if (order > ngramOrder) {
						break;
					}
					if (logger.isLoggable(Level.INFO))
						logger.info("begin to read ngrams with order " + order);

					continue; //skip this line
				}

				if (start) {
					counter++;
				}
			} 
		} finally { 
			grammarReader.close(); 
		}

		return counter;

	}
	
	@Override
	public double ngramLogProbability(int[] ngram, int order) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
