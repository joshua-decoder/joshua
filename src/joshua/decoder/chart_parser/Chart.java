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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.chart_parser.Bin.ComputeItemResult;
import joshua.decoder.chart_parser.DotChart.DotItem;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.ff.tm.RuleCollection;
import joshua.decoder.ff.tm.Trie;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperGraph;
import joshua.decoder.segment_file.ConstraintRule;
import joshua.decoder.segment_file.ConstraintSpan;
import joshua.lattice.Arc;
import joshua.lattice.Lattice;
import joshua.lattice.Node;


/**
 * Chart class this class implements chart-parsing:
 * (1) seeding the chart 
 * (2) cky main loop over bins, 
 * (3) identify applicable rules in each bin
 * Note: the combination operation will be done in Bin
 * 
 * Signatures of class:
 * Bin: i, j
 * SuperItem (used for CKY check): i,j, lhs
 * Item ("or" node): i,j, lhs, edge ngrams
 * Deduction ("and" node)
 * 
 * index of sentences: start from zero
 * index of cell: cell (i,j) represent span of words indexed [i,j-1]
 * where i is in [0,n-1] and j is in [1,n]
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class Chart {
	
//===============================================================
// Package-protected instance fields
//===============================================================
	
	Bin[][] bins; // note that in some cell, it might be null
	
	/** This is the length of the foreign side input sentence. */
	int sentenceLength;
	
	
	//===========================================================
	// Decoder-wide fields
	//===========================================================
	ArrayList<FeatureFunction> featureFunctions;
	
	
	//===========================================================
	// Satistics
	//===========================================================
	
	/**
	 * how many items have been pruned away because its cost
	 * is greater than the cutoff in calling
	 * chart.add_deduction_in_chart()
	 */
	int n_prepruned           = 0;
	
	int n_prepruned_fuzz1     = 0;
	int n_prepruned_fuzz2     = 0;
	int n_pruned              = 0;
	int n_merged              = 0;
	int n_added               = 0;
	int n_dotitem_added       = 0; // note: there is no pruning in dot-item
	int n_called_compute_item = 0;
	
	
	//===========================================================
	// Time-profiling variables for debugging
	//===========================================================
	long g_time_compute_item  = 0;
	long g_time_add_deduction = 0;
	
	
//===============================================================
// Private instance fields (maybe could be protected instead)
//===============================================================
	
	private  Grammar[]       grammars;
	private  DotChart[]      dotcharts; // each grammar should have a dotchart associated with it
	private Bin              goalBin;
	private int              goalSymbolID = -1;
	private Lattice<Integer> sentence; // a list of foreign words
	private int              segmentID;
	
	
	//===========================================================
	// Manual constraint annotations
	//===========================================================
	
	// TODO: each span only has one ConstraintSpan
	// contain spans that have LHS or RHS constraints (they are always hard)
	private HashMap<String,ConstraintSpan> constraintSpansForFiltering;
	
	// contain spans that have hard "rule" constraint; key: start_span; value: end_span
	private ArrayList<Span> spansWithHardRuleConstraint;
	
	
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
	
	// TODO: Once the Segment interface is adjusted to provide a Latice<String> for the sentence() method, we should just accept a Segment instead of the sentence, segmentID, and constraintSpans parameters. We have the symbol table already, so we can do the integerization here instead of in DecoderThread. GrammarFactory.getGrammarForSentence will want the integerized sentence as well, but then we'll need to adjust that interface to deal with (non-trivial) lattices too. Of course, we get passed the grammars too so we could move all of that into here.
	
	public Chart(
		Lattice<Integer>           sentence,
		ArrayList<FeatureFunction> featureFunctions,
		SymbolTable                symbolTable,
		int                        segmentID,
		Grammar[]                  grammars,
		boolean                    hasLM,
		String                     goalSymbol,
		List<ConstraintSpan>       constraintSpans)
	{
		this.sentence         = sentence;
		this.sentenceLength   = sentence.size() - 1;
		this.featureFunctions = featureFunctions;
		this.symbolTable      = symbolTable;
		
		// TODO: this is very expensive
		this.bins         = new Bin[sentenceLength][sentenceLength+1];
		this.segmentID    = segmentID;
		this.goalSymbolID = this.symbolTable.addNonterminal(goalSymbol);
		this.goalBin      = new Bin(this, this.goalSymbolID);
		
		// add un-translated words into the chart as item (with large cost)
		// TODO: grammar specific?
		this.grammars = grammars;
		
		// each grammar will have a dot chart
		this.dotcharts = new DotChart[this.grammars.length];
		for (int i = 0; i < this.grammars.length; i++) {
			this.dotcharts[i] = new DotChart(this.sentence, this.grammars[i], this);
			this.dotcharts[i].seed(); // TODO: should fold into the constructor
		}
		
		
		/** Note that below is not required for seeding
		 * */
		
		/**
		 * (1) add manual rule (only allow flat rules) into the
		 *     chart as constraints
		 * (2) add RHS or LHS constraint into
		 *     constraintSpansForFiltering
		 * (3) add span signature into setOfSpansWithHardRuleConstraint; if the span contains a hard "RULE" constraint
		 */
		
		
		if (null != constraintSpans) {
		
			for (ConstraintSpan cSpan : constraintSpans) {
				if (null != cSpan.rules()) {
					boolean shouldAdd = false; // contain LHS or RHS constraints?
					for (ConstraintRule cRule : cSpan.rules()) {
						/** Note that LHS and RHS constraints are always hard, 
						 * while Rule constraint can be soft or hard
						 **/
						switch (cRule.type()){
						case RULE:
							//== prepare the feature scores 
							float[] featureScores = new float[cRule.features().length];//TODO: this require the input always specify the right number of features
							for (int i = 0; i < featureScores.length; i++) {
								if (cSpan.isHard()) {
									featureScores[i] = 0;	// force the feature cost as zero
								} else {
									featureScores[i] = cRule.features()[i];
								}
							}
							
							/**If the RULE constraint is hard, then we should filter all out all consituents (within this span), 
							 * which are contructed from regular grammar*/
							if (cSpan.isHard()) {
								if (null == this.spansWithHardRuleConstraint) {
									this.spansWithHardRuleConstraint = new ArrayList<Span>();
								}
								this.spansWithHardRuleConstraint.add(new Span(cSpan.start(), cSpan.end()));								
							}
							
							//TODO: which grammar should we use to create a mannual rule?
							int arity = 0; // only allow flat rule (i.e. arity=0)
							Rule rule = this.grammars[0].constructManualRule(
									symbolTable.addNonterminal(cRule.lhs()), 
									symbolTable.addTerminals(cRule.foreignRhs()),
									symbolTable.addTerminals(cRule.nativeRhs()),
									featureScores, 
									arity);
							//add to the chart
							addAxiom(cSpan.start(), cSpan.end(), rule, new SourcePath());
							if (logger.isLoggable(Level.INFO))
								logger.info("Adding RULE constraint for span " + cSpan.start() + ", " + cSpan.end() + "; isHard=" + cSpan.isHard() +rule.getLHS());
							break;
							
						default: 
							shouldAdd = true;
						}
					}
					if (shouldAdd) {
						if (logger.isLoggable(Level.INFO))
							logger.info("Adding LHS or RHS constraint for span " + cSpan.start() + ", " + cSpan.end());
						if (null == this.constraintSpansForFiltering) {
							this.constraintSpansForFiltering = new HashMap<String, ConstraintSpan>();
						}
						this.constraintSpansForFiltering.put(getSpanSignature(cSpan.start(), cSpan.end()), cSpan);
					}
				}
			}
		}
		
		/**add OOV rules; 
		 * this should be called after the manual constraints have been set up
		 **/
		// TODO: the transition cost for phrase model, arity penalty, word penalty are all zero, except the LM cost
		for (Node<Integer> node : sentence) {
			for (Arc<Integer> arc : node.getOutgoingArcs()) {
				// create a rule, but do not add into the grammar trie
				// TODO: which grammar should we use to create an OOV rule?
				Rule rule = this.grammars[0].constructOOVRule(
					this.featureFunctions.size(), arc.getLabel(), hasLM);
				
				// tail and head are switched - FIX names:
				if (containsHardRuleConstraint(node.getNumber(), arc.getTail().getNumber())) {
					//do not add the oov axiom
					if (logger.isLoggable(Level.INFO))
						logger.info("Using hard rule constraint for span " + node.getNumber() + ", " + arc.getTail().getNumber());
				} else {
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
	public HyperGraph expand() {
//		long start = System.currentTimeMillis();
//		long time_step1 = 0;
//		long time_step2 = 0;
//		long time_step3 = 0;
//		long time_step4 = 0;
		logger.info("Begin expand");
		for (int width = 1; width <= sentenceLength; width++) {
			for (int i = 0; i <= sentenceLength - width; i++) {
				int j = i + width;
				if (logger.isLoggable(Level.FINEST)) 
					logger.finest(String.format("Processing span (%d, %d)",i,j));
				
				//(1)### expand the cell in dotchart
				//long start_step1= Support.current_time();
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Expanding cell");
				for (int k = 0; k < this.grammars.length; k++) {
					this.dotcharts[k].expand_cell(i,j);
				}
				if (logger.isLoggable(Level.FINEST)) 
					logger.finest(String.format("n_dotitem= %d", n_dotitem_added));
				//time_step1 += Support.current_time()-start_step1;
				
				//(2)### populate COMPLETE rules into Chart: the regular CKY part
				//long start_step2= Support.current_time();
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Adding complete items into chart");
				for (int k = 0; k < this.grammars.length; k++) {
					if (this.grammars[k].hasRuleForSpan(i, j, sentenceLength)
					&& null != this.dotcharts[k].l_dot_bins[i][j]) {
						
						for (DotItem dt: this.dotcharts[k].l_dot_bins[i][j].l_dot_items) {
							SourcePath srcPath = dt.srcPath;
							RuleCollection rules = dt.tnode.getRules();
							
							if (logger.isLoggable(Level.FINEST))
								logger.finest("Checking DotItem for matched rules. " + srcPath);
							
							if (null != rules) { // have rules under this trienode
								// TODO: filter the rule according to LHS constraint
								if (rules.getArity() == 0) { // rules without any non-terminal
									addAxioms(i, j, rules, srcPath);
								} else { // rules with non-terminal
									if (JoshuaConfiguration.use_cube_prune) {
										completeCellCubePrune(i, j, dt, rules, srcPath);
									} else {
										// populate chart.bin[i][j] with rules from dotchart[i][j]
										completeCell(i, j, dt, rules, srcPath);
									}
								}
							}
						}
					}
				}
				//time_step2 += Support.current_time()-start_step2;
				
				//(3)### process unary rules (e.g., S->X, NP->NN), just add these items in chart, assume acyclic
				//long start_step3= Support.current_time();
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Adding unary items into chart");
				
				/**zhifei replaced the following code to address an interaction problem between different grammars
				 * the problem is: if [X]->[NT,1],[NT,1] in a regular grammar, but  [S]->[X,1],[X,1] is in a glue grammar; then [S]->[NT,1],[NT,1] is not achievable
				for (int k = 0; k < this.grammars.length; k++) {
					if (this.grammars[k].hasRuleForSpan(i, j, sentenceLength)) {
						addUnaryItems(this.grammars[k],i,j);//single-branch path
					}
				}*/
				addUnaryItems(this.grammars,i,j);//single-branch path
				
				
				//time_step3 += Support.current_time()-start_step3;
				
				//(4)### in dot_cell(i,j), add dot-items that start from the /complete/ superIterms in chart_cell(i,j)
				//long start_step4= Support.current_time();
				if (logger.isLoggable(Level.FINEST))
					logger.finest("Initializing new dot-items that start from complete items in this cell");
				for (int k = 0; k < this.grammars.length; k++) {
					if (this.grammars[k].hasRuleForSpan(i, j, sentenceLength)) {
						this.dotcharts[k].start_dotitems(i,j);
					}
				}
				//time_step4 += Support.current_time()-start_step4;
				
				//(5)### sort the items in the cell: for pruning purpose
				if (logger.isLoggable(Level.FINEST)) 
					logger.finest(String.format(
						"After Process span (%d, %d), called:= %d",
						i, j, n_called_compute_item));
				if (null != this.bins[i][j]) {
					// this.bins[i][j].logStatistics(Level.INFO);

					// this is required
					@SuppressWarnings("unused")
					ArrayList<HGNode> l_s_its = this.bins[i][j].get_sorted_items();
					
					/*
					// sanity check with this cell
					int sum_d = 0;
					double sum_c = 0.0;
					double sum_total=0.0;
					for (Item t_item : l_s_its) {
						if (null != t_item.l_deductions)
							sum_d += t_item.l_deductions.size();
						sum_c += t_item.best_deduction.best_cost;
						sum_total += t_item.est_total_cost;
					}
					*/
				}
			}
		}
		logStatistics(Level.FINE);

		// transition_final: setup a goal item, which may have many deductions
		if (null != this.bins[0][sentenceLength]) {
			this.goalBin.transit_to_goal(this.bins[0][sentenceLength]); // update goalBin				
		} else {
			throw new RuntimeException(
				"No complete item in the cell(0," + sentenceLength + "); possible reasons: " +
				"(1) your grammar does not have any valid derivation for the source sentence; " +
				"(2) too aggressive pruning");
		}
		
		// For debugging
		//long sec_consumed = (System.currentTimeMillis() -start)/1000;
		//logger.info("######Expand time consumption: "+ sec_consumed);
		//logger.info(String.format("Step1: %d; step2: %d; step3: %d; step4: %d", time_step1, time_step2, time_step3, time_step4));
		
		/*logger.info(String.format("t_compute_item: %d; t_add_deduction: %d;", g_time_compute_item / 1000, g_time_add_deduction / 1000));
		for (FeatureFunction m: this.models) {
			logger.info("FeatureFunction cost: " + m.time_consumed/1000);
		}*/

		//logger.info(String.format("t_lm: %d; t_score_lm: %d; t_check_nonterminal: %d", g_time_lm, g_time_score_sent, g_time_check_nonterminal));
		//LMModel tm_lm = (LMModel)this.models.get(0);
		//logger.info(String.format("LM lookupwords1, step1: %d; step2: %d; step3: %d", tm_lm.time_step1, tm_lm.time_step2, tm_lm.time_step3));
		//debug end
		logger.info("Finished expand");
		return new HyperGraph(this.goalBin.get_sorted_items().get(0), -1, -1, this.segmentID, sentenceLength); // num_items/deductions : -1
	}
	
	
//===============================================================
// Private methods
//===============================================================
	
	private void logStatistics(Level level) {
		if (logger.isLoggable(level)) {
			logger.log(level,
				String.format("ADDED: %d; MERGED: %d; PRUNED: %d; PRE-PRUNED: %d, FUZZ1: %d, FUZZ2: %d; DOT-ITEMS ADDED: %d",
					this.n_added,
					this.n_merged,
					this.n_pruned,
					this.n_prepruned,
					this.n_prepruned_fuzz1,
					this.n_prepruned_fuzz2,
					this.n_dotitem_added));
		}
	}
	
	
	/**
	 * agenda based extension: this is necessary in case more
	 * than two unary rules can be applied in topological order
	 * s->x; ss->s for unary rules like s->x, once x is complete,
	 * then s is also complete
	 */
	private int addUnaryItems(Grammar[] grs, int i, int j) {
		Bin chartBin = this.bins[i][j];
		if (null == chartBin) {
			return 0;
		}
		int qtyAdditionsToQueue = 0;
		ArrayList<HGNode> queue
			= new ArrayList<HGNode>(chartBin.get_sorted_items());
		
		while (queue.size() > 0) {
			HGNode item = queue.remove(0);
			for(Grammar gr : grs){
				Trie childNode = gr.getTrieRoot().matchOne(item.lhs); // match rule and complete part
				if (childNode != null
				&& childNode.getRules() != null
				&& childNode.getRules().getArity() == 1) { // have unary rules under this trienode
					ArrayList<HGNode> antecedents = new ArrayList<HGNode>();
					antecedents.add(item);
					List<Rule> rules = childNode.getRules().getSortedRules();
					
					for (Rule rule : rules) { // for each unary rules								
						ComputeItemResult tbl_states = chartBin.compute_item(rule, antecedents, i, j, new SourcePath());
						//System.out.println("add unary rule " +i +", " + j + rule.toString(this.symbolTable));
						HGNode res_item = chartBin.add_deduction_in_bin(tbl_states, rule, i, j, antecedents, new SourcePath());
						if (null != res_item) {
							queue.add(res_item);
							qtyAdditionsToQueue++;
							//System.out.println("Unary Item's lhs " + res_item.lhs + "; string=" + this.symbolTable.getWord(res_item.lhs));
						}else{
							//System.out.println("!!!!!!!!!!!!!!!!!!!!!!! pruned unary rule " +i +", " + j + rule.toString(this.symbolTable));
						}
					}
				}
			}
		}
		return qtyAdditionsToQueue;
	}
	
	
	private void addAxioms(int i, int j, RuleCollection rb, SourcePath srcPath) {
		if (containsHardRuleConstraint(i, j)) {
			if (logger.isLoggable(Level.FINE)) logger.fine("Hard rule constraint for span " +i +", " + j);
			return; //do not add any axioms
		} else {

			List<Rule> rules = filterRules(i,j, rb.getSortedRules());
			for (Rule rule : rules) {
				addAxiom(i, j, rule, srcPath);
			}
		}
	}
	
	
	/** axiom is for rules with zero-arity */
	private void addAxiom(int i, int j, Rule rule, SourcePath srcPath) {
		if (null == this.bins[i][j]) {
			this.bins[i][j] = new Bin(this, this.goalSymbolID);
		}
		this.bins[i][j].add_axiom(i, j, rule, srcPath);
	}
	
	
	private void completeCell(int i, int j, DotItem dt, RuleCollection rb, SourcePath srcPath) {
		if (containsHardRuleConstraint(i, j)) {
			System.out.println("having hard rule constraint in span " +i +", " + j);
			return; //do not add any axioms
		}
		
		if (null == this.bins[i][j]) {
			this.bins[i][j] = new Bin(this, this.goalSymbolID);
		}
		// combinations: rules, antecent items
		this.bins[i][j].complete_cell(i, j, dt.l_ant_super_items, filterRules(i,j,rb.getSortedRules()), rb.getArity(), srcPath);
	}
	
	
	private void completeCellCubePrune(int i, int j, DotItem dt, RuleCollection rb, SourcePath srcPath) {
		if (containsHardRuleConstraint(i, j)) {
			System.out.println("having hard rule constraints in span " +i +", " + j);
			return; //do not add any axioms
		}
		
		if (null == this.bins[i][j]) {
			this.bins[i][j] = new Bin(this, this.goalSymbolID);
		}
		
		this.bins[i][j].complete_cell_cube_prune(i, j, dt.l_ant_super_items, filterRules(i,j, rb.getSortedRules()), srcPath);//combinations: rules, antecent items
	}
	

	
	

//	===============================================================
//	 Manual constraint annotation methods and classes
//	===============================================================
	
	
	/**
	 * if there are any LHS or RHS constraints for a span, then
	 * all the applicable grammar rules in that span will have
	 * to pass the filter.
	 */
	private List<Rule> filterRules(int i, int j, List<Rule> rulesIn) {
		if (null == this.constraintSpansForFiltering)
			return rulesIn;
		ConstraintSpan cSpan = this.constraintSpansForFiltering.get( getSpanSignature(i,j));
		if (null == cSpan) { // no filtering
			return rulesIn;
		} else {
			
			List<Rule> rulesOut = new ArrayList<Rule>();
			for (Rule gRule : rulesIn) {
				//gRule will survive, if any constraint (LHS or RHS) lets it survive 
				for (ConstraintRule cRule : cSpan.rules()) {
					if (shouldSurvive(cRule, gRule)) {
						rulesOut.add(gRule);
						break;
					}
				}
			}
			return rulesOut;
		}
	}
	
	//should we filter out the gRule based on the manually provided constraint cRule
	private boolean shouldSurvive(ConstraintRule cRule, Rule gRule) {
		
		switch (cRule.type()) {
		case LHS:
			return (gRule.getLHS() == this.symbolTable.addNonterminal(cRule.lhs()));
		case RHS:
			int[] targetWords = this.symbolTable.addTerminals(cRule.nativeRhs());
			
			if (targetWords.length != gRule.getEnglish().length)
				return false;
			
			for (int t = 0; t < targetWords.length; t++) {
				if (targetWords[t] != gRule.getEnglish()[t])
					return false;
			}
			
			return true;
		default: // not surviving
			return false;
		}
	}
	
	
	/**
	 * if a span is *within* the coverage of a *hard* rule constraint, 
	 * then this span will be only allowed to use the mannual rules 
	 */
	private boolean containsHardRuleConstraint(int startSpan, int endSpan) {
		if (null != this.spansWithHardRuleConstraint) {
			for (Span span : this.spansWithHardRuleConstraint) {
				if (startSpan >= span.startPos && endSpan <= span.endPos)
					return true;
			}
		}
		return false;
	}
		
	
	
	
	private static class Span {
		int startPos;
		int endPos;
		public Span(int startPos, int endPos) {
			this.startPos = startPos;
			this.endPos = endPos;
		}
	}
	
	private String getSpanSignature(int i, int j) {
		return i + " " + j;
	}
}
