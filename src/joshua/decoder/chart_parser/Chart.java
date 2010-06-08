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
package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.DotChart.DotNode;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.state_maintenance.StateComputer;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.ConstraintSpan;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;


/**
 * Chart class this class implements chart-parsing:
 * (1) seeding the chart 
 * (2) cky main loop over bins, 
 * (3) identify applicable rules in each bin
 * 
 * Note: the combination operation will be done in Cell
 * 
 * Signatures of class:
 * Cell: i, j
 * SuperNode (used for CKY check): i,j, lhs
 * HGNode ("or" node): i,j, lhs, edge ngrams
 * HyperEdge ("and" node)
 * 
 * index of sentences: start from zero
 * index of cell: cell (i,j) represent span of words indexed [i,j-1]
 * where i is in [0,n-1] and j is in [1,n]
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */

public class Chart {
		

	//===========================================================
	// Satistics
	//===========================================================
	
	/**
	 * how many items have been pruned away because its cost
	 * is greater than the cutoff in calling
	 * chart.add_deduction_in_chart()
	 */
	int nPreprunedEdges           = 0;
	
	int nPreprunedFuzz1     = 0;
	int nPreprunedFuzz2     = 0;
	int nPrunedItems              = 0;
	int nMerged              = 0;
	int nAdded               = 0;
	int nDotitemAdded       = 0; // note: there is no pruning in dot-item
	int nCalledComputeNode = 0;
	
	int              segmentID;
	
//===============================================================
// Private instance fields (maybe could be protected instead)
//===============================================================
	private Cell[][] cells; // note that in some cell, it might be null
	private int foreignSentenceLength;
	private List<FeatureFunction> featureFunctions;	
	private List<StateComputer> stateComputers;	
	private  Grammar[]       grammars;
	private  DotChart[]      dotcharts; // each grammar should have a dotchart associated with it
	private Cell              goalBin;
	private int              goalSymbolID = -1;
	private Lattice<Integer> sentence; // a list of foreign words
	
	
		
	private Combiner combiner = null;
	private ManualConstraintsHandler manualConstraintsHandler;
	
	//===========================================================
	// Decoder-wide fields
	//===========================================================
	
	/**
	 * Shared symbol table for source language terminals, target
	 * language terminals, and shared nonterminals.
	 * <p>
	 * It may be that separate tables should be maintained for
	 * the source and target languages.
	 * <p>
	 * This class adds an untranslated word ID to the symbol
	 * table. The Bin class adds a goal symbol nonterminal to
	 * the symbol table.
	 * <p>
	 */
	private SymbolTable symbolTable;
	
	
//===============================================================
// Static fields
//===============================================================
	
	//===========================================================
	// Time-profiling variables for debugging
	//===========================================================
	// These are only referenced in a commented out logger. They are never set.
	//private static long g_time_lm                = 0;
	//private static long g_time_score_sent        = 0;
	//private static long g_time_check_nonterminal = 0;
	
	
	//===========================================================
	// Logger
	//===========================================================
	private static final Logger logger = 
		Logger.getLogger(Chart.class.getName());
	
	
	
	
//===============================================================
// Constructors
//===============================================================
	
	/**TODO: Once the Segment interface is adjusted to provide a Latice<String> for the sentence() method, 
	 * we should just accept a Segment instead of the sentence, segmentID, and constraintSpans parameters.
	 * We have the symbol table already, so we can do the integerization here instead of in DecoderThread. 
	 * GrammarFactory.getGrammarForSentence will want the integerized sentence as well, 
	 * but then we'll need to adjust that interface to deal with (non-trivial) lattices too. Of course, 
	 * we get passed the grammars too so we could move all of that into here.
	 */
	
	public Chart(
		Lattice<Integer>           sentence,
		List<FeatureFunction> featureFunctions,
		List<StateComputer> stateComputers,
		SymbolTable                symbolTable,
		int                        segmentID,
		Grammar[]                  grammars,
		boolean                    useMaxLMCostForOOV,
		String                     goalSymbol,
		List<ConstraintSpan>       constraintSpans
		)
	{
		this.sentence         = sentence;
		this.foreignSentenceLength   = sentence.size() - 1;
		this.featureFunctions = featureFunctions;
		this.stateComputers = stateComputers;
		this.symbolTable      = symbolTable;
		
		// TODO: this is very memory-expensive
		this.cells         = new Cell[foreignSentenceLength][foreignSentenceLength+1];
		
		this.segmentID    = segmentID;
		this.goalSymbolID = this.symbolTable.addNonterminal(goalSymbol);
		this.goalBin      = new Cell(this, this.goalSymbolID);
		this.grammars = grammars;
		
		// each grammar will have a dot chart
		this.dotcharts = new DotChart[this.grammars.length];
		for (int i = 0; i < this.grammars.length; i++)
			this.dotcharts[i] = new DotChart(this.sentence, this.grammars[i], this);
		
		
		if(JoshuaConfiguration.useCubePrune)//TODO: should not directly refer to JoshuaConfiguration
			combiner = new CubePruneCombiner(this.featureFunctions, this.stateComputers);
		else
			combiner = new ExhaustiveCombiner(this.featureFunctions, this.stateComputers);

		//============== begin to do initialization work

		//TODO: which grammar should we use to create a mannual rule?, grammar[1] is the regular grammar
		manualConstraintsHandler = new ManualConstraintsHandler(symbolTable, this, grammars[1], constraintSpans);
		
		
		/**add OOV rules; 
		 * this should be called after the manual constraints have been set up
		 * Different grammar differ in hasRuleForSpan, defaultOwner, and defaultLHSSymbol
		 **/
		// TODO: the transition cost for phrase model, arity penalty, word penalty are all zero, except the LM cost
		for (Node<Integer> node : sentence) {
			for (Arc<Integer> arc : node.getOutgoingArcs()) {
				// create a rule, but do not add into the grammar trie
				// TODO: which grammar should we use to create an OOV rule?
//				this is the regular grammar
				int sourceWord = arc.getLabel();
				final int targetWord;
				if (JoshuaConfiguration.mark_oovs) {
					targetWord = symbolTable.addTerminal(symbolTable.getWord(sourceWord) + "_OOV");
				} else {
					targetWord = sourceWord;
				}
				Rule rule = this.grammars[1].constructOOVRule(
					this.featureFunctions.size(), sourceWord, targetWord, useMaxLMCostForOOV);
			
				if (manualConstraintsHandler.containHardRuleConstraint(node.getNumber(), arc.getTail().getNumber())) {
					//do not add the oov axiom
					if (logger.isLoggable(Level.FINE))
						logger.fine("Using hard rule constraint for span " + node.getNumber() + ", " + arc.getTail().getNumber());
				} else {
					//System.out.println(rule.toString(symbolTable));
					addAxiom(node.getNumber(), arc.getTail().getNumber(), rule, new SourcePath().extend(arc));
				}
			}
		}
		
	
		
		if (logger.isLoggable(Level.FINE))
			logger.fine("Finished seeding chart.");
	}

	
//===============================================================
// The primary method for filling in the chart
//===============================================================
	
	/**
	 * Construct the hypergraph with the help from DotChart.
	 */
	
	/** a parser that can handle:
	 * - multiple grammars
	 * - on the fly binarization
	 * - unary rules (without cycle)
	 * */
	
	public HyperGraph expand() {
		
		if (logger.isLoggable(Level.FINE))
			logger.fine("Begin expand.");
		
		for (int width = 1; width <= foreignSentenceLength; width++) {
			for (int i = 0; i <= foreignSentenceLength - width; i++) {
				int j = i + width;
				if (logger.isLoggable(Level.FINEST)) 
					logger.finest(String.format("Processing span (%d, %d)",i,j));
				
				
				//(1)=== expand the cell in dotchart
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Expanding cell");
				for (int k = 0; k < this.grammars.length; k++) {
					/**each dotChart can act individually (without consulting other dotCharts)
					 * because it either consumes the source input or the complete nonTerminals, 
					 * which are both grammar-independent
					 **/
					this.dotcharts[k].expandDotCell(i,j);
				}
			
				
				//(2)=== populate COMPLETE rules into Chart: the regular CKY part
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Adding complete items into chart");
				for (int k = 0; k < this.grammars.length; k++) {
					
					if (this.grammars[k].hasRuleForSpan(i, j, foreignSentenceLength)
						&& null != this.dotcharts[k].getDotCell(i, j)) {
						
						for (DotNode dotNode: this.dotcharts[k].getDotCell(i, j).getDotNodes()) {
							RuleCollection ruleCollection = dotNode.getTrieNode().getRules();
							if (ruleCollection != null) { // have rules under this trienode
								// TODO: filter the rule according to LHS constraint								
								completeCell(i, j, dotNode, ruleCollection.getSortedRules(), ruleCollection.getArity(), dotNode.getSourcePath());									
								
							}
						}
					}
				}				
				
				//(3)=== process unary rules (e.g., S->X, NP->NN), just add these items in chart, assume acyclic
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Adding unary items into chart");
				
				/**zhifei replaced the following code to address an interaction problem between different grammars
				 * the problem is: if [X]->[NT,1],[NT,1] in a regular grammar, but  [S]->[X,1],[X,1] is in a glue grammar; 
				 * then [S]->[NT,1],[NT,1] may not be achievable, depending on which grammar is processed first.
				 */
				if(false){//behavior depend on the order of the grammars got processed, which is bad 
					for (int k = 0; k < this.grammars.length; k++) {
						if (this.grammars[k].hasRuleForSpan(i, j, foreignSentenceLength)) {
							addUnaryNodesPerGrammar(this.grammars[k],i,j);//single-branch path
						}
					}
				}else{//behavior does not depend on the order of the grammars got processed
					addUnaryNodes(this.grammars,i,j);
				}				
				
				
				//(4)=== in dot_cell(i,j), add dot-nodes that start from the /complete/ superIterms in chart_cell(i,j)
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Initializing new dot-items that start from complete items in this cell");
				for (int k = 0; k < this.grammars.length; k++) {
					if (this.grammars[k].hasRuleForSpan(i, j, foreignSentenceLength)) {
						this.dotcharts[k].startDotItems(i,j);
					}
				}				
				
				//(5)=== sort the nodes in the cell
				/**Cube-pruning requires the nodes being sorted, when prunning for later/wider cell.
				 * Cuebe-pruning will see superNode, which contains a list of nodes.
				 * getSortedNodes() will make the nodes in the superNode get sorted*/
				if (null != this.cells[i][j]) {
					this.cells[i][j].getSortedNodes();
				}
			}
		}
		
		logStatistics(Level.INFO);

		// transition_final: setup a goal item, which may have many deductions
		if (null != this.cells[0][foreignSentenceLength]) {
			this.goalBin.transitToGoal(this.cells[0][foreignSentenceLength], this.featureFunctions, this.foreignSentenceLength);				
		} else {
			logger.severe(
				"No complete item in the cell(0," + foreignSentenceLength + "); possible reasons: " +
				"(1) your grammar does not have any valid derivation for the source sentence; " +
				"(2) too aggressive pruning");
			System.exit(1);
		}
		
		if(logger.isLoggable(Level.FINE))
			logger.fine("Finished expand");
		return new HyperGraph(this.goalBin.getSortedNodes().get(0), -1, -1, this.segmentID, foreignSentenceLength);
	}
	
	
	public Cell getCell(int i, int j){
		return this.cells[i][j];
	}
	
	
//===============================================================
// Private methods
//===============================================================
	
	private void logStatistics(Level level) {
		if (logger.isLoggable(level)) {
			logger.log(level,
				String.format("ADDED: %d; MERGED: %d; PRUNED: %d; PRE-PRUNED: %d, FUZZ1: %d, FUZZ2: %d; DOT-ITEMS ADDED: %d",
					this.nAdded,
					this.nMerged,
					this.nPrunedItems,
					this.nPreprunedEdges,
					this.nPreprunedFuzz1,
					this.nPreprunedFuzz2,
					this.nDotitemAdded));
		}
	}
	
	
	/**
	 * agenda based extension: this is necessary in case more
	 * than two unary rules can be applied in topological order
	 * s->x; ss->s for unary rules like s->x, once x is complete,
	 * then s is also complete
	 */
	private int addUnaryNodes(Grammar[] grs, int i, int j) {		
		
		Cell chartBin = this.cells[i][j];
		if (null == chartBin) {
			return 0;
		}
		int qtyAdditionsToQueue = 0;
		ArrayList<HGNode> queue	= new ArrayList<HGNode>( chartBin.getSortedNodes() );
		
		while (queue.size() > 0) {
			HGNode node = queue.remove(0);
			for(Grammar gr : grs){
				if (! gr.hasRuleForSpan(i, j, foreignSentenceLength))
					continue;
				
				Trie childNode = gr.getTrieRoot().matchOne(node.lhs); // match rule and complete part
				if (childNode != null
					&& childNode.getRules() != null
					&& childNode.getRules().getArity() == 1) { // have unary rules under this trienode
					
					ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
					antecedents.add(node);
					List<Rule> rules = childNode.getRules().getSortedRules();
					
					for (Rule rule : rules) { // for each unary rules								
						ComputeNodeResult states = new ComputeNodeResult(this.featureFunctions, rule, antecedents, i, j, new SourcePath(), stateComputers, this.segmentID);
						HGNode resNode = chartBin.addHyperEdgeInCell(states, rule, i, j, antecedents, new SourcePath(), true);
						if (null != resNode) {
							queue.add(resNode);
							qtyAdditionsToQueue++;
						}
					}
				}
			}
		}
		return qtyAdditionsToQueue;
	}
	
	/**
     * agenda based extension: this is necessary in case more than two unary rules can be applied in topological order s->x; ss->s
     * for unary rules like s->x, once x is complete, then s is also complete
     */
    private int addUnaryNodesPerGrammar(Grammar gr, int i, int j) {
    	
            Cell chartCell = this.cells[i][j];
            if (null == chartCell) {
                    return 0;
            }
            int qtyAdditionsToQueue = 0;
            ArrayList<HGNode> queue
                    = new ArrayList<HGNode>(chartCell.getSortedNodes());


            while (queue.size() > 0) {
            	HGNode item = (HGNode)queue.remove(0);
                Trie child_tnode = gr.getTrieRoot().matchOne(item.lhs);//match rule and complete part
                if (child_tnode != null
                && child_tnode.getRules() != null
                && child_tnode.getRules().getArity() == 1) {//have unary rules under this trienode
                        ArrayList<HGNode> l_ants = new ArrayList<HGNode>();
                        l_ants.add(item);
                        List<Rule> rules =
                                child_tnode.getRules().getSortedRules();

                        for (Rule rule : rules){//for each unary rules
                        	ComputeNodeResult states = new ComputeNodeResult(this.featureFunctions, rule, l_ants, i, j, new SourcePath(), stateComputers, this.segmentID);
                            HGNode res_item = chartCell.addHyperEdgeInCell(states, rule, i, j, l_ants, new SourcePath(), false);
                            if (null != res_item) {
                                    queue.add(res_item);
                                    qtyAdditionsToQueue++;
                            }
                        }
                }
            }
            return qtyAdditionsToQueue;
    }

	
	/** axiom is for rules with zero-arity */
	public void addAxiom(int i, int j, Rule rule, SourcePath srcPath) {
		if (null == this.cells[i][j]) {
			this.cells[i][j] = new Cell(this, this.goalSymbolID);
		}		
		combiner.addAxiom(this, this.cells[i][j], i, j, rule, srcPath);
	}
	
	
	
	private void completeCell(int i, int j, DotNode dotNode, List<Rule> sortedRules, int arity, SourcePath srcPath) {
		
		if (manualConstraintsHandler.containHardRuleConstraint(i, j)) {
			if (logger.isLoggable(Level.FINE)) 
				logger.fine("Hard rule constraint for span " +i +", " + j);
			return; //do not add any nodes
		}
		
		if (null == this.cells[i][j]) {
			this.cells[i][j] = new Cell(this, this.goalSymbolID);
		}
		// combinations: rules, antecent items
		List<Rule> filteredRules =  manualConstraintsHandler.filterRules(i,j, sortedRules);
		if(arity==0)
			combiner.addAxioms(this, this.cells[i][j], i, j, filteredRules, srcPath);
		else
			//this.cells[i][j].completeCell(i, j, dt.l_ant_super_items, filterRules(i,j,rb.getSortedRules()), rb.getArity(), srcPath);
			combiner.combine(this, this.cells[i][j], i, j,  dotNode.getAntSuperNodes(), filteredRules, arity, srcPath);
	}	
	
	
	
}
