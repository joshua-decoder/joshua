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
package joshua.decoder.ff.tm;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.FeatureFunction;
import joshua.discriminative.FileUtilityOld;

/**
 * Partial implementation of the <code>Grammar</code> interface
 * that provides logic for sorting a grammar.
 * <p>
 * <em>Note</em>: New classes implementing the <code>Grammar</code>
 * interface should probably inherit from this class, unless a
 * specific sorting technique different from that implemented by
 * this class is required.
 *
 * @author Zhifei Li
 * @author Lane Schwartz
 */
public abstract class AbstractGrammar implements Grammar {
 
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(AbstractGrammar.class.getName());
	
	
	/** 
	 * Indicates whether the rules in this grammar have been
	 * sorted based on the latest feature function values.
	 */
	protected boolean sorted;
	
	/**
	 * Constructs an empty, unsorted grammar.
	 *
	 * @see Grammar#isSorted()
	 */
	public AbstractGrammar() {
		this.sorted = false;
	}
	
	/**
	 * Cube-pruning requires that the grammar be sorted based
	 * on the latest feature functions. To avoid synchronization,
	 * this method should be called before multiple threads are
	 * initialized for parallel decoding
	 */
	public void sortGrammar(List<FeatureFunction> models) {
		logger.info("sort grammar");
		Trie root = getTrieRoot();
		if(root!=null){
			sort(root, models);
			setSorted(true);
		}
	}

	/* See Javadoc comments for Grammar interface. */
	public boolean isSorted() {
		return sorted;
	}
	
	/**
	 * Sets the flag indicating whether this grammar is sorted.
	 * <p>
	 * This method is called by {@link #sortGrammar(ArrayList)}
	 * to indicate that the grammar has been sorted.
	 * 
	 * Its scope is protected so that child classes that override
	 * <code>sortGrammar</code> will also be able to call this
	 * method to indicate that the grammar has been sorted.
	 * 
	 * @param sorted
	 */
	protected void setSorted(boolean sorted) {
		this.sorted = sorted;
		logger.fine("This grammar is now sorted: " + this);
	}
	
	/**
	 * Recursively sorts the grammar using the provided feature
	 * functions.
	 * <p>
	 * This method first sorts the rules stored at the provided
	 * node, then recursively calls itself on the child nodes
	 * of the provided node.
	 * 
	 * @param node   Grammar node in the <code>Trie</code> whose
	 *               rules should be sorted.
	 * @param models Feature function models to use during
	 *               sorting.
	 */
	private void sort(Trie node, List<FeatureFunction> models) {
	
		if (node != null) {			
			if(node.hasRules()) {
				RuleCollection rules = node.getRules();
				if (logger.isLoggable(Level.FINE)) 
					logger.fine("Sorting node " + Arrays.toString(rules.getSourceSide()));	
				
				rules.sortRules(models);
				
				if (logger.isLoggable(Level.FINEST)) {
					StringBuilder s = new StringBuilder();
					if (rules == null) {
						s.append("\n\tNo rules");
					} else {
						
						for (Rule r : rules.getSortedRules()) {
							s.append(
									"\n\t" 
									+ r.getLHS()
									+ " ||| "
									+ Arrays.toString(r.getFrench())
									+ " ||| "
									+ Arrays.toString(r.getEnglish())
									+ " ||| "
									+ Arrays.toString(r.getFeatureScores())
									+ " ||| " 
									+ r.getEstCost()
									+ "  "
									+ r.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(r)));
						}
					}
					logger.finest(s.toString());
				}
			}
			
			/*TODO: why is this necessary?
			if (node instanceof AbstractGrammar) {
				((AbstractGrammar) node).setSorted(true);
			}*/
			
			if(node.hasExtensions()){
				for (Trie child : node.getExtensions()) {
					sort(child, models);
				}
			} else if (logger.isLoggable(Level.FINE)) {
				logger.fine("Node has 0 children to extend: " + node);
			}
		}
	}
	
	
	
	//write grammar to disk
	public void writeGrammarOnDisk(String file, SymbolTable symbolTable) {
		BufferedWriter writer = FileUtilityOld.getWriteFileStream(file);
		writeGrammarOnDisk(this.getTrieRoot(), writer, symbolTable);
		FileUtilityOld.closeWriteFile(writer);
	}
	
	private void writeGrammarOnDisk(Trie trie, BufferedWriter writer, SymbolTable symbolTable) {
		if(trie.hasRules()){
			RuleCollection rlCollection = trie.getRules();
			for(Rule rl : rlCollection.getSortedRules()){
				FileUtilityOld.writeLzf(writer, rl.toString(symbolTable));
				FileUtilityOld.writeLzf(writer,"\n");
			}
		}
		
		if (trie.hasExtensions()) {
			Object[] tem = trie.getExtensions().toArray();
			
			for (int i = 0; i < tem.length; i++) {
				writeGrammarOnDisk((Trie)tem[i], writer, symbolTable);
			}
		}
	}
	

	//change the feature weight in the grammar
	public void changeGrammarCosts(Map<String, Double> weightTbl, HashMap<String, Integer> featureMap, double[] scores, String prefix, int column, boolean negate) {		
		changeGrammarCosts(this.getTrieRoot(), featureMap, scores, prefix, column, negate);
	}
	
	private void changeGrammarCosts(Trie trie, HashMap<String, Integer> featureMap, double[] scores, String prefix, int column, boolean negate) {
		if(trie.hasRules()){
			RuleCollection rlCollection = trie.getRules();
			for(Rule rl : rlCollection.getSortedRules()){
				String featName = prefix + rl.getRuleID();
				float weight = (float)scores[featureMap.get(featName)];
				if(negate)
					weight *= -1.0;
				rl.setFeatureCost(column, weight);				
			}
		}
		
		if (trie.hasExtensions()) {
			Object[] tem = trie.getExtensions().toArray();
			
			for (int i = 0; i < tem.length; i++) {
				changeGrammarCosts((Trie)tem[i], featureMap, scores, prefix, column, negate);
			}
		}
	}
	
	
	//obtain RulesIDTable in the grammar, accumalative 
	public void obtainRulesIDTable(Map<String, Integer> rulesIDTable,  SymbolTable symbolTable) {		
		obtainRulesIDTable(this.getTrieRoot(), rulesIDTable, symbolTable);
	}
	
	private void obtainRulesIDTable(Trie trie, Map<String, Integer> rulesIDTable,  SymbolTable symbolTable) {
		if(trie.hasRules()){
			RuleCollection rlCollection = trie.getRules();
			for(Rule rl : rlCollection.getRules()){
				rulesIDTable.put( rl.toStringWithoutFeatScores(symbolTable), rl.getRuleID());
			}
		}
		
		if (trie.hasExtensions()) {
			Object[] tem = trie.getExtensions().toArray();
			
			for (int i = 0; i < tem.length; i++) {
				obtainRulesIDTable((Trie)tem[i], rulesIDTable, symbolTable);
			}
		}
	}
	
}

	