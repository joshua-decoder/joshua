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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.BuildinSymbol;
import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.chart_parser.ComputeNodeResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.state_maintenance.NgramDPState;
import joshua.decoder.ff.tm.BilingualRule;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.GrammarReader;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.hiero.DiskHyperGraphFormatReader;
import joshua.decoder.ff.tm.hiero.MemoryBasedBatchGrammar;
import joshua.util.FileUtility;
import joshua.util.Regex;


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
//line: SENTENCE_TAG, sent_id, sent_len, numNodes, numEdges (in average, numEdges is about 10 times larger than the numNodes, which is in average about 4000)
//line: ITEM_TAG, item id, i, j, lhs, numEdges, tbl_state;
//line: bestLogP, numNodes, item_ids, rule id, OOV-Non-Terminal (optional), OOV (optional), \newline feature scores
public class DiskHyperGraph {
	
//===============================================================
// Fields
//===============================================================
	private int         LMFeatureID          = 0;
	private SymbolTable symbolTable;
	
	//when saving the hg, we simply compute all the model logP on the fly and store them on the disk
	/*TODO: when reading the hg, we read thm into a WithModelCostsHyperEdge; 
	 *now, we let a program outside this class to figure out which model logP corresponds which feature function, we should avoid this in the future*/
	private List<FeatureFunction> featureFunctions;
	
	// Whether to store the logPs at each HyperEdge
	private boolean storeModelLogP = false;
	
	// This will be set if the previous sentence is skipped
	private String startLine;
	
	private HashMap<HGNode,Integer> itemToID
		= new HashMap<HGNode,Integer>(); // for saving hypergraph
	
	private HashMap<Integer,HGNode> idToItem
		= new HashMap<Integer,HGNode>(); // for reading hypergraph
	
	private int currentItemID = 1;
	private int qtyDeductions = 0;
	
	
//	Shared by many hypergraphs, via the initialization functions
	private HashMap<Integer,Rule> associatedGrammar = new HashMap<Integer, Rule>();
	
	private BufferedWriter    itemsWriter;
	private BufferedReader    itemsReader;
	private HyperGraphPruning pruner;
	
	// TODO: this is not pretty, but avoids re-allocation in writeRule()
	private GrammarReader<BilingualRule> ruleReader;
	
	// Set in init_read(...), used in read_hyper_graph()
	private HashMap<Integer,?> selectedSentences;
	
	private int sentID;
	
//===============================================================
// Static Fields
//===============================================================
	private static final String SENTENCE_TAG    = "#SENT: ";
	private static final String ITEM_TAG        = "#I";
	private static final String ITEM_STATE_TAG  = " ST ";
	private static final String NULL_ITEM_STATE = "nullstate";
	
	/* three kinds of rule:
	 *     (>0) regular rule
	 *     (0)  oov rule
	 *     (-1) null rule
	 */
	private static int NULL_RULE_ID = -1;
	
	//FIXME: this is a hack for us to create OOVRule, and OOVRuleID
	/**
	 * This is wrong as the default LHS and owner are not
	 * properly set. For this reason, the creation of OOV rule
	 * may cause bugs
	 */
	private static Grammar pGrammar = new MemoryBasedBatchGrammar();
	
	private static final Logger logger =
		Logger.getLogger(DiskHyperGraph.class.getName());
	
	
//===============================================================
// Constructors
//===============================================================
	/**
	 * For saving purpose, one needs to specify the featureFunctions.
	 * For reading purpose, one does not need to provide the
	 * list.
	 */
	public DiskHyperGraph(SymbolTable symbolTable, int LMFeatureID,
		boolean storeModelCosts, List<FeatureFunction> featureFunctions) 
	{
		this.symbolTable      = symbolTable;
		this.LMFeatureID      = LMFeatureID;
		this.storeModelLogP  = storeModelCosts;
		this.featureFunctions = featureFunctions;
	}
	
	
	
	
//===============================================================
// Initialization Methods
//===============================================================
	
	/*
	 * for writting hyper-graph: 
	 * 		(1) saving each hyper-graph;  
	 * 		(2) remember each regualar rule used; 
	 * 		(3) dump the rule jointly (in case parallel decoding)
	 */
	public void initWrite(String itemsFile, boolean useForestPruning, 
			double threshold) throws IOException 
	{
		this.itemsWriter =
			(null == itemsFile)
			? new BufferedWriter(new OutputStreamWriter(System.out))
			: FileUtility.getWriteFileStream(itemsFile);
		
		if (ruleReader == null)
			ruleReader = new DiskHyperGraphFormatReader(null, this.symbolTable);
			
		if (useForestPruning) {
			this.pruner = new HyperGraphPruning(this.symbolTable, true, threshold, threshold);
		}
	}
	
	
	public void initRead(String hypergraphsFile, String rulesFile, HashMap<Integer,?> selectedSentences) {
		try {
			this.itemsReader = FileUtility.getReadFileStream(hypergraphsFile);
		} catch (IOException e) {
			logger.severe("Error opening hypergraph file: " + hypergraphsFile);
		}
		
		this.selectedSentences = selectedSentences;
		
		/* Reload the rule table */
		if (logger.isLoggable(Level.FINE)) 
			logger.fine("Reading rules from file " + rulesFile);
		
		this.associatedGrammar.clear();
		
		this.ruleReader = 
			new DiskHyperGraphFormatReader(rulesFile, this.symbolTable);
			
		if (ruleReader != null) {
			ruleReader.initialize();
			for (Rule rule : ruleReader) {				
				this.associatedGrammar.put(rule.getRuleID(), rule);
			}			
			ruleReader.close();			
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
	
	public void closeReaders(){
		try {
			if(this.itemsReader!=null)
				this.itemsReader.close();
			if(this.ruleReader!=null)
				this.ruleReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeItemsWriter(){
		try {
			if(this.itemsWriter!=null)
				this.itemsWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
//===============================================================
// Methods
//===============================================================
	
	public void saveHyperGraph(HyperGraph hg) throws IOException {
		resetStates();
		if (null != this.pruner) this.pruner.pruningHG(hg);
		constructItemTables(hg);
		if (logger.isLoggable(Level.FINE)) 
			logger.fine("Number of Items is: " + this.itemToID.size());
		this.itemsWriter.write(
			SENTENCE_TAG + hg.sentID
			+ " " + hg.sentLen
			+ " " + this.itemToID.size()
			+ " " + this.qtyDeductions
			+ "\n" );
		
		this.sentID = hg.sentID;
		// we save the hypergraph in a bottom-up way: so that reading is easy
		if (this.idToItem.size() != this.itemToID.size()) {
			throw new RuntimeException("Number of Items is not equal");
		}
		for (int i = 1; i <= this.idToItem.size(); i++) {
			writeItem(this.idToItem.get(i));
		}
		if (null != this.pruner) 
			this.pruner.clearState();
	}
	
	/**
	 * Assign IDs to all HGNodes in the hypergraph. We do a
	 * depth-first traversal starting at the goal item, and
	 * assign IDs from the bottom up. BUG: this code could stack
	 * overflow for deep trees.
	 */
	private void constructItemTables(HyperGraph hg) {
		resetStates();
		constructItemTables(hg.goalNode);
	}
	
	/**
	 * This method is <i>really</i> private, and should only
	 * be called by constructItemTables(HyperGraph).
	 */
	private void constructItemTables(HGNode item) {
		if (this.itemToID.containsKey(item)) return;
		
		// first: assign IDs to all my antecedents
		for (HyperEdge hyperEdge : item.hyperedges) {
			this.qtyDeductions++;
			if (null != hyperEdge.getAntNodes()) {
				for (HGNode antecedentItem : hyperEdge.getAntNodes()) {
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
		this.itemsWriter.write(
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
					null == item.hyperedges
					? 0
					: item.hyperedges.size() )
				.append(ITEM_STATE_TAG)
				.append(
					// Assume LM is the only stateful feature
					null != item.getDPStates()
					? item.getDPStates()
						.get(this.LMFeatureID)
						.getSignature(this.symbolTable, true)
					: NULL_ITEM_STATE )
				.append("\n")
				.toString()
			);
		
		if (null != item.hyperedges) {
			for (HyperEdge hyperEdge : item.hyperedges) {
				writeHyperedge(item, hyperEdge);
			}
		}
		this.itemsWriter.flush();
	}
	
	
	private final boolean isOutOfVocabularyRule(Rule rl) {
		return (rl.getRuleID() == MemoryBasedBatchGrammar.OOV_RULE_ID);//pGrammar.getOOVRuleID());
	}
	
	private void writeHyperedge(HGNode node, HyperEdge edge)
	throws IOException {
		//get rule id
		int ruleID = NULL_RULE_ID;
		final Rule edgeRule = edge.getRule();
		if (null != edgeRule) {
			ruleID = edgeRule.getRuleID();
			if	(! isOutOfVocabularyRule(edgeRule)) {
				this.associatedGrammar.put(ruleID, edgeRule); //remember used regular rule
			}
		}
		
		StringBuffer s = new StringBuffer();
		//line: bestLogP, numNodes, item_ids, rule id, OOV-Non-Terminal (optional), OOV (optional),
		s.append(String.format("%.4f ", edge.bestDerivationLogP));
		//s.append(" ").append(cur_d.bestDerivationLogP).append(" ");//this 1.2 faster than the previous statement
		
		//s.append(String.format("%.4f ", cur_d.get_transition_logP(false)));
		//s.append(cur_d.get_transition_logP(false)).append(" ");//this 1.2 faster than the previous statement, but cost 1.4 larger disk space
		
		if (null == edge.getAntNodes()) {
			s.append(0);
		} else {
			final int qtyItems = edge.getAntNodes().size();
			s.append(qtyItems);
			for (int i = 0; i < qtyItems; i++) {
				s.append(' ')
					.append(this.itemToID.get(
						edge.getAntNodes().get(i) ));
			}
		}
		s.append(' ')
			.append(ruleID);
		if (ruleID == MemoryBasedBatchGrammar.OOV_RULE_ID) {//pGrammar.getOOVRuleID()) {
			//System.out.println("lhs id: " + deduction_rule.getLHS());
			//System.out.println("rule words: " + deduction_rule.getEnglish());
			s.append(' ')
				.append(this.symbolTable.getWord(edgeRule.getLHS()))
				.append(' ')
				.append(this.symbolTable.getWords(edgeRule.getEnglish()));
		}
		s.append('\n');
		
		// save model logPs as a seprate line; optional
		if (this.storeModelLogP) {
			s.append( createModelLogPLine(node, edge) );
		}
		
		this.itemsWriter.write(s.toString());
	}
	
	/**
	 * Do not remove this function as it gives freedom for an
	 * extended class to override it
	 */
	public String createModelLogPLine(HGNode parentNode, HyperEdge edge){
		StringBuffer line = new StringBuffer();
		double[] transitionLogPs = null;
		if(this.featureFunctions!=null){
			transitionLogPs = ComputeNodeResult.computeModelTransitionLogPs(
					this.featureFunctions, edge, parentNode.i, parentNode.j, this.sentID);
		}else{
			transitionLogPs = ((WithModelLogPsHyperEdge) edge).modeLogPs;
		}
		
		for (int k = 0; k < transitionLogPs.length; k++) {
			line.append(String.format("%.4f", transitionLogPs[k]))
				.append(
					k < transitionLogPs.length - 1
					? " "
					: "\n");
		}
		return line.toString();
	}
	
// End save_hyper_graph()
//===============================================================
	
	
	public HyperGraph readHyperGraph() {
		resetStates();
		//read first line: SENTENCE_TAG, sent_id, sent_len, numNodes, num_deduct
		String line = null;
		if (null != this.startLine) { // the previous sentence is skipped
			line = this.startLine;
			this.startLine = null;
		} else {
			line = FileUtility.read_line_lzf(this.itemsReader);
		}
		
		if (! line.startsWith(SENTENCE_TAG)) {
			throw new RuntimeException("wrong sent tag line: " + line);
		}
		
		// Test if we should skip this sentence
		if (null != this.selectedSentences
		&& (! this.selectedSentences.containsKey(
				Integer.parseInt(Regex.spaces.split(line)[1]) ))
		) {
			while ((line = FileUtility.read_line_lzf(this.itemsReader)) != null) {
				if (line.startsWith(SENTENCE_TAG)) break;
			}
			this.startLine = line;
			System.out.println("sentence is skipped");
			return null;
			
		} else {
			String[] fds       = Regex.spaces.split(line);
			int sentenceID     = Integer.parseInt(fds[1]);
			int sentenceLength = Integer.parseInt(fds[2]);
			int qtyItems       = Integer.parseInt(fds[3]);
			int qtyDeductions  = Integer.parseInt(fds[4]);
			
			//System.out.println("numNodes: "+ qtyItems + "; num_deducts: " + qtyDeductions);
			
			for (int i = 0; i < qtyItems; i++) 
				readNode();
			//TODO check if the file reaches EOF, or if the num_deducts matches 
			
			//create hyper graph
			HGNode goalItem = this.idToItem.get(qtyItems);
			if (null == goalItem) {
				throw new RuntimeException("no goal item");
			}
			return new HyperGraph(goalItem, qtyItems, qtyDeductions, sentenceID, sentenceLength);
		}
	}
	
	private HGNode readNode() {
		//line: ITEM_TAG itemID i j lhs qtyDeductions ITEM_STATE_TAG item_state
		String  line = FileUtility.read_line_lzf(this.itemsReader);
		String[] fds = line.split(ITEM_STATE_TAG); // TODO: use joshua.util.Regex
		if (fds.length != 2) {
			throw new RuntimeException("wrong item line");
		}
		String[] words    = Regex.spaces.split(fds[0]);
		int itemID        = Integer.parseInt(words[1]);
		int i             = Integer.parseInt(words[2]);
		int j             = Integer.parseInt(words[3]);
		int lhs           = this.symbolTable.addNonterminal(words[4]);
		int qtyDeductions = Integer.parseInt(words[5]);
		
		//item state: signature (created from HashMap tbl_states)
		HashMap<Integer,DPState> dpStates = null;
		
		if (fds[1].compareTo(NULL_ITEM_STATE) != 0) {
			// Assume the only stateful feature is lm feature
			dpStates = new HashMap<Integer,DPState>();
			dpStates.put(this.LMFeatureID,	new NgramDPState(this.symbolTable, fds[1]));
		}
		
		List<HyperEdge> edges = null;
		HyperEdge         bestEdge = null;
		double bestLogP = Double.NEGATIVE_INFINITY;
		if (qtyDeductions > 0) {
			edges = new ArrayList<HyperEdge>();
			for (int t = 0; t < qtyDeductions; t++) {
				HyperEdge edge = readHyperedge();
				edges.add(edge);
				if (edge.bestDerivationLogP > bestLogP) {//semiring plus
					bestLogP      = edge.bestDerivationLogP;
					bestEdge = edge;
				}
			}
		}
		
		HGNode item = new HGNode(i, j, lhs, edges, bestEdge, dpStates);
		this.idToItem.put(itemID, item);
		return item;
	}
	
	// Assumption: has this.associatedGrammar and this.idToItem
	private HyperEdge readHyperedge() {
		//line: bestLogP, numNodes, item_ids, rule id, OOV-Non-Terminal (optional), OOV (optional),
		String  line = FileUtility.read_line_lzf(this.itemsReader);
		String[] fds = Regex.spaces.split(line);
		
		//bestLogP numNodes item_ids
		double bestLogP = Double.parseDouble(fds[0]);
		ArrayList<HGNode> antecedentItems = null;
		final int qtyAntecedents = Integer.parseInt(fds[1]);
		if (qtyAntecedents > 0) {
			antecedentItems = new ArrayList<HGNode>();
			for (int t = 0; t < qtyAntecedents; t++) {
				final int itemID = Integer.parseInt(fds[2+t]);
				HGNode item = this.idToItem.get(itemID);
				if (null == item) {
					throw new RuntimeException("item is null for id: " + itemID);
				}
				antecedentItems.add(item);
			}
		}
		
		//rule_id
		Rule rule = null;
		final int ruleID = Integer.parseInt(fds[2+qtyAntecedents]);
		if (ruleID != NULL_RULE_ID) {
			if (ruleID != MemoryBasedBatchGrammar.OOV_RULE_ID) {//pGrammar.getOOVRuleID()) {
				rule = this.associatedGrammar.get(ruleID);
				if (null == rule) {
					throw new RuntimeException("rule is null but id is " + ruleID);
				}
			} else {
				rule = pGrammar.constructOOVRule(1,	this.symbolTable.addTerminal(fds[4+qtyAntecedents]), this.symbolTable.addTerminal(fds[4+qtyAntecedents]), false);
				
				/**This is a hack. as the pGrammar does not set defaultLHS properly*/
				int lhs = this.symbolTable.addNonterminal(fds[3+qtyAntecedents]);
				rule.setLHS(lhs);
			}
		} else {
			// Do nothing: goal item has null rule
		}
		
		HyperEdge hyperEdge;
		if (this.storeModelLogP) {
			String[] logPString =
				Regex.spaces.split(FileUtility.read_line_lzf(this.itemsReader));
			double[] logPs = new double[logPString.length];
			for (int i = 0; i < logPString.length; i++) {
				logPs[i] = Double.parseDouble(logPString[i]);
			}
			hyperEdge = new WithModelLogPsHyperEdge(rule, bestLogP, null, antecedentItems, logPs, null);
		} else {
			hyperEdge = new HyperEdge(rule, bestLogP, null, antecedentItems, null);
		}
		hyperEdge.getTransitionLogP(true); // to set the transition logP
		return hyperEdge;
	}
	
// end readHyperGraph()
//===============================================================
	static public Map<String,Integer> obtainRuleStringToIDTable(String rulesFile) {
				
		SymbolTable symbolTable = new BuildinSymbol(null);
		GrammarReader<BilingualRule> ruleReader = new DiskHyperGraphFormatReader(rulesFile, symbolTable);
		Map<String,Integer> rulesIDTable = new HashMap<String,Integer>();
		
		ruleReader.initialize();
		for (Rule rule : ruleReader) {				
			rulesIDTable.put(rule.toStringWithoutFeatScores(symbolTable), rule.getRuleID());
		}			
		ruleReader.close();			
	
		return rulesIDTable;
	}
	

	static public int mergeDiskHyperGraphs(int ngramStateID, boolean saveModelCosts, int totalNumSent,
			boolean useUniqueNbest, boolean useTreeNbest,
			String filePrefix1, String filePrefix2, String filePrefixOut, boolean removeDuplicate) throws IOException{
		
		SymbolTable symbolTbl = new BuildinSymbol();
		
		DiskHyperGraph diskHG1 = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null); 
		diskHG1.initRead(filePrefix1+".hg.items", filePrefix1+".hg.rules", null);
		
		DiskHyperGraph diskHG2 = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null); 
		diskHG2.initRead(filePrefix2+".hg.items", filePrefix2+".hg.rules", null);
		
		DiskHyperGraph diskHGOut = new DiskHyperGraph(symbolTbl, ngramStateID, saveModelCosts, null);
		
		//TODO
		boolean forestPruning = false;
		double forestPruningThreshold = -1;		
		diskHGOut.initWrite(filePrefixOut + ".hg.items", forestPruning, forestPruningThreshold);
		
		KBestExtractor kbestExtrator = new KBestExtractor(symbolTbl, useUniqueNbest, useTreeNbest,
				false,	false,	false, false);
		
		int totalNumHyp = 0;
		for(int sentID=0; sentID < totalNumSent; sentID ++){
			//System.out.println("#Process sentence " + sentID);
			HyperGraph hg1 = diskHG1.readHyperGraph();
			HyperGraph hg2 = diskHG2.readHyperGraph();
			
			//filter hypergraphs by removing duplicate
			if(removeDuplicate){
				Set<String> uniqueHyps = new HashSet<String>();
				kbestExtrator.filterKbestHypergraph(hg1, uniqueHyps);
				kbestExtrator.filterKbestHypergraph(hg2, uniqueHyps);
			}
			
			HyperGraph mergedHG = HyperGraph.mergeTwoHyperGraphs(hg1, hg2);
			diskHGOut.saveHyperGraph(mergedHG);
			
			/*System.out.println("size1=" + hg1.goalNode.hyperedges.size() + 
							  "; size2=" + hg2.goalNode.hyperedges.size() +
							  "; mergedsize=" + mergedHG.goalNode.hyperedges.size());
			*/
			totalNumHyp += mergedHG.goalNode.hyperedges.size();
		}
		diskHGOut.writeRulesNonParallel(filePrefixOut + ".hg.rules");
		System.out.println("totalMergeSize="+totalNumHyp);
		
		diskHG1.closeReaders();
		diskHG2.closeReaders();
		diskHGOut.closeReaders();
		diskHGOut.closeItemsWriter();
		return totalNumHyp;
	}
	
	
	public void writeRulesNonParallel(String rulesFile)
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
		this.ruleReader.close();
	}
	
	// writtenRules: remember what kind of rules have already been saved
	public void writeRulesParallel(BufferedWriter out, 
			HashMap<Integer,Integer> writtenRules) throws IOException 
	{
		logger.info("writing rules in a partition");
		for (int ruleID : this.associatedGrammar.keySet()) {
			if (! writtenRules.containsKey(ruleID)) {//not been written on disk yet
				writtenRules.put(ruleID, 1);
				writeRule(out, this.associatedGrammar.get(ruleID), ruleID);
			}
		}
		out.flush();
	}
	
	private void writeRule(BufferedWriter out, Rule rule, 
			int ruleID) throws IOException 
	{
		// HACK: this is VERY wrong, but avoiding it seems to require major architectural changes
		out.write(this.ruleReader.toWords( (BilingualRule) rule));
		out.write("\n");
	}
}
