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
package joshua.decoder.hypergraph;

import joshua.decoder.ff.lm.LMFFDPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.HieroGrammar.MemoryBasedBatchGrammar;
import joshua.decoder.ff.tm.HieroGrammar.MemoryBasedBatchGrammarWithPrune;
import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.JoshuaConfiguration;
import joshua.corpus.SymbolTable;
import joshua.util.FileUtility;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;


/**
 * this class implements functions of writting/reading hypergraph
 * on disk. Limitations of this version
 * (1) cannot recover each individual feature, notably the LM feature
 * (2) assume we only have one stateful featuure, which must be a
 *     LM feature
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate$
 */


//Bottom-up
//line: SENTENCE_TAG, sent_id, sent_len, num_items, num_deductions (in average, num_deductions is about 10 times larger than the num_items, which is in average about 4000)
//line: ITEM_TAG, item id, i, j, lhs, num_deductions, tbl_state;
//line: best_cost, num_items, item_ids, rule id, OOV-Non-Terminal (optional), OOV (optional),
public class DiskHyperGraph {
	
//===============================================================
// Fields
//===============================================================
	private int         UNTRANSLATED_WORD_ID = 0;
	private int         LMFeatureID          = 0;
	private SymbolTable symbolTable;
	
	//when saving the hg, we simply compute all the model cost on the fly and store them on the disk
	//TODO: when reading the hg, we read thm into a WithModelCostsHyperEdge; now, we let a program outside this class to figure out which model cost corresponds which feature function, we should avoid this in the future
	private ArrayList<FeatureFunction> featureFunctions;
	
	// Whether to store the costs at each HyperEdge
	private boolean storeModelCosts = false;
	
	// This will be set if the previous sentence is skipped
	private String startLine;
	
	private HashMap<HGNode,Integer> itemToID
		= new HashMap<HGNode,Integer>(); // for saving hypergraph
	
	private HashMap<Integer,HGNode> idToItem
		= new HashMap<Integer,HGNode>(); // for reading hypergraph
	
	private int currentItemID = 1;
	private int qtyDeductions = 0;
	
	
//	Shared by many hypergraphs, via the initialization functions
	private HashMap<Integer,Rule> associatedGrammar	= new HashMap<Integer,Rule>();
	
	private BufferedWriter    writer;
	private BufferedReader    reader;
	private HyperGraphPruning pruner;
	
	// Set in init_read(...), used in read_hyper_graph()
	private HashMap<Integer,?> selectedSentences;
	
	
//===============================================================
// Static Fields
//===============================================================
	private static final String SENTENCE_TAG    = "#SENT: ";
	private static final String ITEM_TAG        = "#I";
	private static final String ITEM_STATE_TAG  = " ST ";
	private static final String NULL_ITEM_STATE = "nullstate";
	private static final String RULE_TBL_SEP    = " -LZF- ";
	
	// TODO: this should be changed
	private static final String nonterminalRegexp
		= "^\\[[A-Z]+\\,[0-9]*\\]$";
	private static final String nonterminalReplaceRegexp
		= "[\\[\\]\\,0-9]+";
	
	/* three kinds of rule:
	 *     (>0) regular rule
	 *     (0)  oov rule
	 *     (-1) null rule
	 */
	private static int NULL_RULE_ID = -1;
	
	private static final Logger logger =
		Logger.getLogger(DiskHyperGraph.class.getName());
	
	
//===============================================================
// Constructors
//===============================================================
	
	public DiskHyperGraph(SymbolTable symbolTable, int LMFeatureID) {
		this.symbolTable          = symbolTable;
		this.LMFeatureID          = LMFeatureID;
		this.UNTRANSLATED_WORD_ID =
			this.symbolTable.addTerminal(JoshuaConfiguration.untranslated_owner);
		this.storeModelCosts      = false;
	}
	
	
	/**
	 * For saving purpose, one needs to specify the featureFunctions.
	 * For reading purpose, one does not need to provide the list.
	 */
	public DiskHyperGraph(SymbolTable symbolTable, int LMFeatureID,
		boolean storeModelCosts, ArrayList<FeatureFunction> featureFunctions
	) {
		this(symbolTable, LMFeatureID);
		this.storeModelCosts  = storeModelCosts;
		this.featureFunctions = featureFunctions;
	}
	
	
//===============================================================
// Initialization Methods
//===============================================================
	
	//for writting hyper-graph: (1) saving each hyper-graph; (2) remember each regualar rule used; (3) dump the rule jointly (in case parallel decoding)
	public void init_write(String itemsFile, boolean useForestPruning, double threshold)
	throws IOException {
		this.writer =
			(null == itemsFile)
			? new BufferedWriter(new OutputStreamWriter(System.out))
			: FileUtility.getWriteFileStream(itemsFile) ;
		
		
		if (useForestPruning) {
			this.pruner = new HyperGraphPruning(
					this.symbolTable, true, threshold, threshold, 1, 1);
		}
	}
	
	
	public void init_read(String hypergraphsFile, String rulesFile, HashMap<Integer,?> selectedSentences) {
		try { 
		this.reader = FileUtility.getReadFileStream(hypergraphsFile);
		this.selectedSentences = selectedSentences;
		
		/* Reload the rule table */
		if (logger.isLoggable(Level.INFO)) 
			logger.info("Reading rules from file " + rulesFile);
		this.associatedGrammar.clear();
		BufferedReader rulesReader =
			FileUtility.getReadFileStream(rulesFile);
		String line;
		while ((line = FileUtility.read_line_lzf(rulesReader)) != null) {
			// line format: ruleID owner RULE_TBL_SEP rule
			String[] fds = line.split(RULE_TBL_SEP);
			if (fds.length != 2) {
				logger.severe("wrong RULE line");
				System.exit(1);
			}
			String[] words   = fds[0].split("\\s+");
			int ruleID       = Integer.parseInt(words[0]);
			int defaultOwner = this.symbolTable.addTerminal(words[1]);
			
			// stateless cost is not properly set, so cannot extract individual features during kbest extraction
			this.associatedGrammar.put(ruleID,
					MemoryBasedBatchGrammarWithPrune.createRule(this.symbolTable, null, nonterminalRegexp,
					nonterminalReplaceRegexp, ruleID, fds[1], defaultOwner));
		}
		rulesReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public HashMap<Integer,Rule> getAssocatedGrammar(){
		return associatedGrammar;
	}
	
	private void resetStates() {
		this.itemToID.clear();
		this.idToItem.clear();
		this.currentItemID = 1;
		this.qtyDeductions = 0;
	}
	
	
//===============================================================
// Methods
//===============================================================
	
	public void save_hyper_graph(HyperGraph hg) throws IOException {
		resetStates();
		if (null != this.pruner) this.pruner.pruning_hg(hg);
		constructItemTables(hg);
		if (logger.isLoggable(Level.INFO)) 
			logger.info("Number of Items is: " + this.itemToID.size());
		this.writer.write(
			SENTENCE_TAG + hg.sent_id
			+ " " + hg.sent_len
			+ " " + this.itemToID.size()
			+ " " + this.qtyDeductions
			+ "\n" );
		
		
		// we save the hypergraph in a bottom-up way: so that reading is easy
		if (this.idToItem.size() != this.itemToID.size()) {
			logger.severe("Number of Items is not equal");
			System.exit(1);
		}
		for (int i = 1; i <= this.idToItem.size(); i++) {
			writeItem(this.idToItem.get(i));
		}
		if (null != this.pruner) this.pruner.clear_state();
	}
	
	/**
	 * Assign IDs to all HGNodes in the hypergraph. We do a
	 * depth-first traversal starting at the goal item, and
	 * assign IDs from the bottom up. BUG: this code could stack
	 * overflow for deep trees.
	 */
	private void constructItemTables(HyperGraph hg) {
		resetStates();
		constructItemTables(hg.goal_item);
	}
	
	/**
	 * This method is <i>really</i> private, and should only
	 * be called by constructItemTables(HyperGraph).
	 */
	private void constructItemTables(HGNode item) {
		if (this.itemToID.containsKey(item)) return;
		
		// first: assign IDs to all my antecedents
		for (HyperEdge hyperEdge : item.l_hyperedges) {
			this.qtyDeductions++;
			if (null != hyperEdge.get_ant_items()) {
				for (HGNode antecedentItem : hyperEdge.get_ant_items()) {
					constructItemTables(antecedentItem);
				}
			}
		}
		
		// second: assign ID to "myself"
		this.idToItem.put(this.currentItemID, item);
		this.itemToID.put(item, this.currentItemID);
		this.currentItemID++;
	}
	
	private void writeItem(HGNode item) throws IOException {
		this.writer.write(
			new StringBuffer()
				.append(ITEM_TAG)
				.append(" ")
				.append(this.itemToID.get(item))
				.append(" ")
				.append(item.i)
				.append(" ")
				.append(item.j)
				.append(" ")
				.append(this.symbolTable.getWord(item.lhs))
				.append(" ")
				.append(
					null == item.l_hyperedges
					? 0
					: item.l_hyperedges.size() )
				.append(ITEM_STATE_TAG)
				.append(
					// Assume LM is the only stateful feature
					null != item.getTblFeatDPStates()
					? item.getTblFeatDPStates()
						.get(this.LMFeatureID)
						.getSignature(this.symbolTable, true)
					: NULL_ITEM_STATE )
				.append("\n")
				.toString()
			);
		
		if (null != item.l_hyperedges) {
			for (HyperEdge hyperEdge : item.l_hyperedges) {
				writeDeduction(item, hyperEdge);
			}
		}
		this.writer.flush();
	}
	
	
	private final boolean isOutOfVocabularyRule(Rule rl) {
		return (rl.getRuleID() == MemoryBasedBatchGrammar.OOV_RULE_ID);
	}
	
	private void writeDeduction(HGNode item, HyperEdge deduction)
	throws IOException {
		//get rule id
		int ruleID = NULL_RULE_ID;
		final Rule deduction_rule = deduction.get_rule();
		if (null != deduction_rule) {
			ruleID = deduction_rule.getRuleID();
			if	(! isOutOfVocabularyRule(deduction_rule)) {
				this.associatedGrammar.put(ruleID, deduction_rule); //remember used regular rule
			}
		}
		
		StringBuffer s = new StringBuffer();
		//line: best_cost, num_items, item_ids, rule id, OOV-Non-Terminal (optional), OOV (optional),
		s.append(String.format("%.4f ", deduction.best_cost));
		//s.append(" ").append(cur_d.best_cost).append(" ");//this 1.2 faster than the previous statement
		
		//s.append(String.format("%.4f ", cur_d.get_transition_cost(false)));
		//s.append(cur_d.get_transition_cost(false)).append(" ");//this 1.2 faster than the previous statement, but cost 1.4 larger disk space
		
		if (null == deduction.get_ant_items()) {
			s.append(0);
		} else {
			final int qtyItems = deduction.get_ant_items().size();
			s.append(qtyItems);
			for (int i = 0; i < qtyItems; i++) {
				s.append(" ")
					.append(this.itemToID.get(
						deduction.get_ant_items().get(i) ));
			}
		}
		s.append(" ")
			.append(ruleID);
		if (ruleID == MemoryBasedBatchGrammar.OOV_RULE_ID) {
			s.append(" ")
				.append(this.symbolTable.getWord(deduction_rule.getLHS()))
				.append(" ")
				.append(this.symbolTable.getWords(deduction_rule.getEnglish()));
		}
		s.append("\n");
		
		// save model cost as a seprate line; optional
		if (this.storeModelCosts) {
			for (int k = 0; k < this.featureFunctions.size(); k++) {
				FeatureFunction m = this.featureFunctions.get(k);
				s.append(String.format("%.4f ",
					null != deduction.get_rule()
					? // deductions under goal item do not have rules
						HyperGraph
							.computeTransition(deduction, m, item.i, item.j)
							.getTransitionCost()
					: HyperGraph.computeFinalTransition(deduction, m)
					))
					.append(
						k < this.featureFunctions.size() - 1
						? " "
						: "\n");
			}
		}
		
		this.writer.write(s.toString());
	}
	
// End save_hyper_graph()
//===============================================================
	
	
	public HyperGraph read_hyper_graph() {
		resetStates();
		//read first line: SENTENCE_TAG, sent_id, sent_len, num_items, num_deduct
		String line = null;
		if (null != this.startLine) { // the previous sentence is skipped
			line = this.startLine;
			this.startLine = null;
		} else {
			line = FileUtility.read_line_lzf(this.reader);
		}
		
		if (! line.startsWith(SENTENCE_TAG)) {
			logger.severe("wrong sent tag line: " + line);
			System.exit(1);
		}
		
		// Test if we should skip this sentence
		if (null != this.selectedSentences
		&& (! this.selectedSentences.containsKey(
				Integer.parseInt(line.split("\\s+")[1]) ))
		) {
			while ((line = FileUtility.read_line_lzf(this.reader)) != null) {
				if (line.startsWith(SENTENCE_TAG)) break;
			}
			this.startLine = line;
			System.out.println("sentence is skipped");
			return null;
			
		} else {
			String[] fds       = line.split("\\s+");
			int sentenceID     = Integer.parseInt(fds[1]);
			int sentenceLength = Integer.parseInt(fds[2]);
			int qtyItems       = Integer.parseInt(fds[3]);
			int qtyDeductions  = Integer.parseInt(fds[4]);
			
			System.out.println(
				"num_items: "       + qtyItems
				+ "; num_deducts: " + qtyDeductions);
			
			for (int i = 0; i < qtyItems; i++) readItem();
			//TODO check if the file reaches EOF, or if the num_deducts matches 
			
			//create hyper graph
			HGNode goalItem = this.idToItem.get(qtyItems);
			if (null == goalItem) {
				logger.severe("no goal item");
				System.exit(1);
			}
			return new HyperGraph(goalItem, qtyItems, qtyDeductions, sentenceID, sentenceLength);
		}
	}
	
	private HGNode readItem() {
		//line: ITEM_TAG itemID i j lhs qtyDeductions ITEM_STATE_TAG item_state
		String  line = FileUtility.read_line_lzf(this.reader);
		String[] fds = line.split(ITEM_STATE_TAG);
		if (fds.length != 2) {
			logger.severe("wrong item line");
			System.exit(1);
		}
		String[] words    = fds[0].split("\\s+");
		int itemID        = Integer.parseInt(words[1]);
		int i             = Integer.parseInt(words[2]);
		int j             = Integer.parseInt(words[3]);
		int lhs           = this.symbolTable.addNonterminal(words[4]);
		int qtyDeductions = Integer.parseInt(words[5]);
		
		//item state: signature (created from HashMap tbl_states)
		HashMap<Integer,FFDPState> dpStates = null;
		
		if (fds[1].compareTo(NULL_ITEM_STATE) != 0) {
			// Assume the only stateful feature is lm feature
			dpStates = new HashMap<Integer,FFDPState>();
			dpStates.put(this.LMFeatureID,
					new LMFFDPState(this.symbolTable, fds[1]));
		}
		
		ArrayList<HyperEdge> deductions = null;
		HyperEdge         bestDeduction = null;
		double bestCost = Double.POSITIVE_INFINITY;
		if (qtyDeductions > 0) {
			deductions = new ArrayList<HyperEdge>();
			for (int t = 0; t < qtyDeductions; t++) {
				HyperEdge deduction = readDeduction();
				deductions.add(deduction);
				if (deduction.best_cost < bestCost) {
					bestCost      = deduction.best_cost;
					bestDeduction = deduction;
				}
			}
		}
		
		HGNode item = new HGNode(i, j, lhs, deductions, bestDeduction, dpStates);
		this.idToItem.put(itemID, item);
		return item;
	}
	
	// Assumption: has this.associatedGrammar and this.idToItem
	private HyperEdge readDeduction() {
		//line: flag, best_cost, num_items, item_ids, rule id, OOV-Non-Terminal (optional), OOV (optional)
		String  line = FileUtility.read_line_lzf(this.reader);
		String[] fds = line.split("\\s+");
		
		//best_cost transition_cost num_items item_ids
		double bestCost = Double.parseDouble(fds[0]);
		ArrayList<HGNode> antecedentItems = null;
		final int qtyAntecedents = Integer.parseInt(fds[1]);
		if (qtyAntecedents > 0) {
			antecedentItems = new ArrayList<HGNode>();
			for (int t = 0; t < qtyAntecedents; t++) {
				final int itemID = Integer.parseInt(fds[2+t]);
				HGNode item = this.idToItem.get(itemID);
				if (null == item) {
					logger.severe("item is null for id: " + itemID);
					System.exit(1);
				}
				antecedentItems.add(item);
			}
		}
		
		//rule_id
		Rule rule = null;
		final int ruleID = new Integer(fds[2+qtyAntecedents]);
		if (ruleID != NULL_RULE_ID) {
			if (ruleID != MemoryBasedBatchGrammar.OOV_RULE_ID) {
				rule = this.associatedGrammar.get(ruleID);
				if (null == rule) {
					logger.severe("rule is null but id is " + ruleID);
					System.exit(1);
				}
			} else {
				//stateless cost is not properly set, so cannot extract individual features during kbest extraction
				rule = constructOOVRule(
					1,
					MemoryBasedBatchGrammar.OOV_RULE_ID,
					this.symbolTable.addNonterminal(fds[3+qtyAntecedents]),
					this.symbolTable.addTerminal(fds[4+qtyAntecedents]),
					this.UNTRANSLATED_WORD_ID,
					false);
			}
		} else {
			// Do nothing: goal item has null rule
		}
		
		HyperEdge hyperEdge;
		//read model costs
		if (this.storeModelCosts) {
			String[] costs_s =
				FileUtility.read_line_lzf(this.reader).split("\\s+");
			double[] costs = new double[costs_s.length];
			for (int i = 0; i < costs_s.length; i++) {
				costs[i] = Double.parseDouble(costs_s[i]);
			}
			hyperEdge = new WithModelCostsHyperEdge(rule, bestCost, null, antecedentItems, costs);
		} else {
			hyperEdge = new HyperEdge(rule, bestCost, null, antecedentItems);
		}
		hyperEdge.get_transition_cost(true); // to set the transition cost
		return hyperEdge;
	}
	

	/** 
	 * only called when creating oov rule in Chart or DiskHypergraph, all
	 * others should call the other contructors; the
	 * transition cost for phrase model, arity penalty,
	 * word penalty are all zero, except the LM cost or the first feature if no LM feature is used
	 */
	private static Rule constructOOVRule(int num_feats, int oov_rule_id, int lhs_in, int fr_in, int owner_in, boolean have_lm_model) {		
		int[] p_french     = new int[1];
	   	p_french[0]  = fr_in;
	   	int[] english    = new int[1];
	   	english[0] = fr_in;
	   	float[] feat_scores     = new float[num_feats];
	   	
	   	/**TODO
	   	 * This is a hack to make the decoding without a LM works
	   	 * */
	   	if(have_lm_model==false){//no LM is used for decoding, so we should set the stateless cost
	   		//this.feat_scores[0]=100.0/((FeatureFunction)p_l_models.get(0)).getWeight();//TODO
	   		feat_scores[0]=100;//TODO
	   	}
	   	
		return new Rule(lhs_in, p_french, english, feat_scores,  0, owner_in, 0, oov_rule_id);
	}
	
// End read_hyper_graph()
//===============================================================
	
	
	public void write_rules_non_parallel(String rulesFile)
	throws IOException {
		BufferedWriter out = 
			(null == rulesFile)
			? new BufferedWriter(new OutputStreamWriter(System.out))
			: FileUtility.getWriteFileStream(rulesFile) ;
		
		logger.info("writing rules");
		for (int ruleID : this.associatedGrammar.keySet()) {
			writeRule(out, this.associatedGrammar.get(ruleID), ruleID);
		}
		out.flush();
		out.close();
	}
	
	// writtenRules: remember what kind of rules have already been saved
	public void write_rules_parallel(
		BufferedWriter out, HashMap<Integer,Integer> writtenRules
	) throws IOException {
		logger.info("writing rules in a partition");
		for (int ruleID : this.associatedGrammar.keySet()) {
			if (! writtenRules.containsKey(ruleID)) {
				writtenRules.put(ruleID, 1);
				writeRule(out, this.associatedGrammar.get(ruleID), ruleID);
			}
		}
		out.flush();
	}
	
	private void writeRule(BufferedWriter out, Rule rule, int ruleID)
	throws IOException {
		out.write(
			ruleID
			+ " "
			+ this.symbolTable.getWord(rule.getOwner())
			+ RULE_TBL_SEP
			+ rule.toString(this.symbolTable)
			+ "\n");
	}
}
