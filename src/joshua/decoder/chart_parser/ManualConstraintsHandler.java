package joshua.decoder.chart_parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.ff.tm.Grammar;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.segment_file.ConstraintRule;
import joshua.decoder.segment_file.ConstraintSpan;


/**
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2009-12-31 11:37:41 -0500 (星期四, 31 十二月 2009) $
 */

public class ManualConstraintsHandler {

	// TODO: each span only has one ConstraintSpan
	// contain spans that have LHS or RHS constraints (they are always hard)
	private HashMap<String,ConstraintSpan> constraintSpansForFiltering;
	
	// contain spans that have hard "rule" constraint; key: start_span; value: end_span
	private ArrayList<Span> spansWithHardRuleConstraint;
	
	
	private SymbolTable symbolTable;
	private Chart chart;
	private Grammar grammarForConstructManualRule;
	
	private static final Logger logger = 
		Logger.getLogger(ManualConstraintsHandler.class.getName());
	
	public ManualConstraintsHandler(SymbolTable symbolTable, Chart chart, 
			Grammar grammarForConstructManualRule, List<ConstraintSpan>       constraintSpans){
		this.symbolTable = symbolTable;
		this.chart = chart;
		this.grammarForConstructManualRule = grammarForConstructManualRule;
		initialize(constraintSpans);
	}
	
	
	private void initialize(List<ConstraintSpan>       constraintSpans){
		/** Note that manual constraints or OOV handling is not part of seeding
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
							//TODO: this require the input always specify the right number of features
							float[] featureScores = new float[cRule.features().length];
							
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
							
							int arity = 0; // only allow flat rule (i.e. arity=0)
							Rule rule = this.grammarForConstructManualRule.constructManualRule(
									symbolTable.addNonterminal(cRule.lhs()), 
									symbolTable.addTerminals(cRule.foreignRhs()),
									symbolTable.addTerminals(cRule.nativeRhs()),
									featureScores, 
									arity);
							
							//add to the chart
							chart.addAxiom(cSpan.start(), cSpan.end(), rule, new SourcePath());
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
		
	}
	
	

//	===============================================================
//	 Manual constraint annotation methods and classes
//	===============================================================
	
	/**
	 * if there are any LHS or RHS constraints for a span, then
	 * all the applicable grammar rules in that span will have
	 * to pass the filter.
	 */
	public List<Rule> filterRules(int i, int j, List<Rule> rulesIn) {
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
	
	/**should we filter out the gRule 
	 * based on the manually provided constraint cRule*/
	public boolean shouldSurvive(ConstraintRule cRule, Rule gRule) {
		
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
	public boolean containHardRuleConstraint(int startSpan, int endSpan) {
		if (null != this.spansWithHardRuleConstraint) {
			for (Span span : this.spansWithHardRuleConstraint) {
				if (startSpan >= span.startPos && endSpan <= span.endPos)
					return true;
			}
		}
		return false;
	}
	
	
	
	private String getSpanSignature(int i, int j) {
		return i + " " + j;
	}	
	
	private static class Span {
		int startPos;
		int endPos;
		public Span(int startPos, int endPos) {
			this.startPos = startPos;
			this.endPos = endPos;
		}
	}
	

}
