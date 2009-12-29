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

import joshua.corpus.vocab.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.ff.DefaultStatefulFF;
import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.StatefulFFTransitionResult;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.chart_parser.SourcePath;

import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * This class performs the following:
 * <ol> 
 * <li> Gets the additional LM score due to combinations of small
 *      items into larger ones by using rules
 * <li> Gets the LM state 
 * <li> Gets the left-side LM state estimation score
 * </ol>
 * 
 * <em>Note</em>: the LMGrammar returns LogP; while the LanguageModelFF
 * needs to return cost (i.e., -LogP)
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class LanguageModelFF extends DefaultStatefulFF {
	
	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(LanguageModelFF.class.getName());
	
	private final String START_SYM="<s>";
	private final int START_SYM_ID;
	private final String STOP_SYM="</s>";
	private final int STOP_SYM_ID;
	
	private final boolean addStartAndEndSymbol = true;
	
	
	/* These must be static (for now) for LMGrammar, but they shouldn't be! in case of multiple LM features */
	static String BACKOFF_LEFT_LM_STATE_SYM="<lzfbo>";
	static public int BACKOFF_LEFT_LM_STATE_SYM_ID;//used for equivelant state
	static String NULL_RIGHT_LM_STATE_SYM="<lzfrnull>";
	static public int NULL_RIGHT_LM_STATE_SYM_ID;//used for equivelant state
	
	/** 
	 * N-gram language model. We assume the language model is
	 * in ARPA format for equivalent state:
	 * 
	 * <ol>
	 * <li>We assume it is a backoff lm, and high-order ngram
	 *     implies low-order ngram; absense of low-order ngram
	 *     implies high-order ngram</li>
	 * <li>For a ngram, existence of backoffweight => existence
	 *     a probability Two ways of dealing with low counts:
	 *     <ul>
	 *       <li>SRILM: don't multiply zeros in for unknown
	 *           words</li>
	 *       <li>Pharaoh: cap at a minimum score exp(-10),
	 *           including unknown words</li>
	 *     </ul>
	 * </li>
	 */
	private final NGramLanguageModel lmGrammar;
	
	/**
	 * We always use this order of ngram, though the LMGrammar
	 * may provide higher order probability.
	 */
	private final int ngramOrder;// = 3;
	//boolean add_boundary=false; //this is needed unless the text already has <s> and </s>
	
	/** Symbol table that maps between Strings and integers. */
	private final SymbolTable symbolTable;
	
	public LanguageModelFF(int featID, int ngramOrder, SymbolTable psymbol, NGramLanguageModel lmGrammar, double weight) {
		super(weight, featID);
		this.ngramOrder = ngramOrder;
		this.lmGrammar  = lmGrammar;
		this.symbolTable = psymbol;
		this.START_SYM_ID = psymbol.addTerminal(START_SYM);
		this.STOP_SYM_ID = psymbol.addTerminal(STOP_SYM);
		
		LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID = symbolTable.addTerminal(BACKOFF_LEFT_LM_STATE_SYM);
		LanguageModelFF.NULL_RIGHT_LM_STATE_SYM_ID = symbolTable.addTerminal(NULL_RIGHT_LM_STATE_SYM);
	}
	
	
	/*the transition cost for LM: sum of the costs of the new ngrams created
	 * depends on the antstates and current rule*/
	public StatefulFFTransitionResult transition(Rule rule, ArrayList<FFDPState> previousStates, int spanStart, int spanEnd, SourcePath srcPath) {
		StatefulFFTransitionResult res = this.lookupWordsEquvState(rule.getEnglish(), previousStates);	
		return res;
	}

	
	/**will consider all the complete ngrams, 
	 * and all the incomplete-ngrams that will have sth fit into its left side*/
	public double estimate(Rule rule) {
		return estimateRuleProb(rule.getEnglish());
	}
	
	public double finalTransition(FFDPState state) {
		if (null != state) {
			return computeEquivStateFinalTransition((LMFFDPState)state);
		} else {
			return 0.0;
		}
	}
	
	
	/**when calculate transition prob: when saw a <bo>, then need to add backoff weights, start from non-state words
	 * */
	private StatefulFFTransitionResult lookupWordsEquvState(int[] enWords,	ArrayList<FFDPState> previousStates) {
		
		ArrayList<Integer> leftLMStateWrds = new ArrayList<Integer>();
		ArrayList<Integer> currentNgram   = new ArrayList<Integer>();
		double             transitionCost = 0.0;
		
		for (int c = 0; c < enWords.length; c++) {
			int curID = enWords[c];
			if (symbolTable.isNonterminal(curID)) {
				if (null == previousStates) {
					throw new IllegalArgumentException("LMModel.lookup_words1_equv_state: null previous_states");
				}
				
				int index = symbolTable.getTargetNonterminalIndex(curID);
				if (index >= previousStates.size()) {
					logger.severe(
							"Target symbol " + symbolTable.getWord(curID) 
							+ " with id " + curID 
							+ " and nonterminal index of " + index 
							+ " is about to trigger an IndexOutOfBoundsException");
				}
				LMFFDPState state = (LMFFDPState) previousStates.get(index);
				int[] leftContext = state.getLeftLMStateWords();
				int[] rightContext = state.getRightLMStateWords();
				if (leftContext.length != rightContext.length) {
					throw new RuntimeException("LMModel.lookup_words1_equv_state: left and right contexts have unequal lengths");
				}
				
				//================ left context
				for (int i = 0; i < leftContext.length; i++) {
					int t = leftContext[i];
					currentNgram.add(t);
					
					//always calculate cost for <bo>: additional backoff weight
					if (t == BACKOFF_LEFT_LM_STATE_SYM_ID) {
						int numAdditionalBackoffWeight = currentNgram.size() - (i+1);//number of non-state words
						
						//compute additional backoff weight
						transitionCost	-= this.lmGrammar.logProbOfBackoffState(currentNgram, currentNgram.size(), numAdditionalBackoffWeight);
						
						if (currentNgram.size() == this.ngramOrder) {
							currentNgram.remove(0);
						}
					} else if (currentNgram.size() == this.ngramOrder) {
						// compute the current word probablity, and remove it
						transitionCost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);
						
						currentNgram.remove(0);
					}
					
					if (leftLMStateWrds.size() < this.ngramOrder - 1) {
						leftLMStateWrds.add(t);
					}
				}
				
				//================  right context
				//note: left_state_org_wrds will never take words from right context because it is either duplicate or out of range
				//also, we will never score the right context probablity because they are either duplicate or partional ngram
				int tSize = currentNgram.size();
				for (int i = 0; i < rightContext.length; i++) {
					// replace context
					currentNgram.set(tSize - rightContext.length + i, rightContext[i]);
				}
			
			} else {//terminal words
				currentNgram.add(curID);
				if (currentNgram.size() == this.ngramOrder) {
					// compute the current word probablity, and remove it
					transitionCost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);
					
					currentNgram.remove(0);
				}
				if (leftLMStateWrds.size() < this.ngramOrder - 1) {
					leftLMStateWrds.add(curID);
				}
			}
		}
		//===== create tabl
		StatefulFFTransitionResult resTbl = new StatefulFFTransitionResult();
		LMFFDPState  modelStates = new LMFFDPState();
		resTbl.setStateForNode(modelStates);
		
		//===== get left euquiv state 
		double[] lmLeftCost = new double[2];
		int[] equivLeftState = this.lmGrammar.leftEquivalentState(Support.subIntArray(leftLMStateWrds, 0, leftLMStateWrds.size()),	this.ngramOrder, lmLeftCost);
		modelStates.setLeftLMStateWords(equivLeftState);
		
		//===== trabsition and estimate cost
		transitionCost += lmLeftCost[0];//add finalized cost for the left state words
		resTbl.setTransitionCost(transitionCost);

		double estimatedFutureCost=0.0;
		if(this.ngramOrder>1){//no estiamtion for unigram lm
			if (JoshuaConfiguration.use_left_equivalent_state) {
				estimatedFutureCost = lmLeftCost[1];
			} else {
				estimatedFutureCost = estimateStateProb(modelStates, false, false);
			}
		}
		resTbl.setFutureCostEstimation(estimatedFutureCost);
		
		//===== get right equiv state
		int[] equivRightState = this.lmGrammar.rightEquivalentState(Support.subIntArray(currentNgram, 0, currentNgram.size()), this.ngramOrder);
		modelStates.setRightLMStateWords(equivRightState);

		return resTbl;	
	}
	
	
	private double scoreChunk(ArrayList<Integer> words, boolean considerIncompleteNgrams, boolean skipStart) {
		if (words.size() <= 0) {
			return 0.0;
		} else {
			int startIndex;
			if (! considerIncompleteNgrams) {
				startIndex = this.ngramOrder;
			} else if (skipStart) {
				startIndex = 2;
			} else {
				startIndex = 1;
			}
			
			return -this.lmGrammar.sentenceLogProbability(
				words, this.ngramOrder, startIndex);
		}
	}
	
	
	//return cost, including partial ngrams
	/*in general: consider all the complete ngrams, and all the incomplete-ngrams that WILL have sth fit into its left side, so
	*if the left side of incomplete-ngrams is a ECLIPS, then ignore the incomplete-ngrams
	*if the left side of incomplete-ngrams is a Non-Terminal, then consider the incomplete-ngrams  
	*if the left side of incomplete-ngrams is boundary of a rule, then consider the incomplete-ngrams*/
	private double estimateRuleProb(int[] enWords) {
		double    estimate   = 0.0;
		boolean   considerIncompleteNgrams = true;
		ArrayList<Integer> words      = new ArrayList<Integer>();
		boolean   skip_start = (enWords[0] == START_SYM_ID);
		
		for (int c = 0; c < enWords.length; c++) {
			int curWrd = enWords[c];
			/*if (c_wrd == Symbol.ECLIPS_SYM_ID) {
				estimate += score_chunk(
					words, consider_incomplete_ngrams, skip_start);
				consider_incomplete_ngrams = false;//for the LM bonus function: this simply means the right state will not be considered at all because all the ngrams in right-context will be incomplete
				words.clear();
				skip_start = false;
			} else*/ if (symbolTable.isNonterminal(curWrd)) {
				estimate += scoreChunk(
					words, considerIncompleteNgrams, skip_start);
				considerIncompleteNgrams = true;
				words.clear();
				skip_start = false;
			} else {
				words.add(curWrd);
			}
		}
		estimate += scoreChunk(
			words, considerIncompleteNgrams, skip_start);
		return estimate;
	}
	
	
	//this function is called when left_equiv state is NOT used
	//in state, all the ngrams are incomplete
	//only get the estimation for the left-state
	//get the true prob for right-state, if add_end==true
	private double estimateStateProb(LMFFDPState state, boolean addStart, boolean addEnd) {
		double res = 0.0;
		
		int[]   leftContext = state.getLeftLMStateWords();
		
		if (null != leftContext) {
			ArrayList<Integer> list;
			if (addStart == true) {
				list = new ArrayList<Integer>(leftContext.length + 1);
				list.add(START_SYM_ID);
			} else {
				list = new ArrayList<Integer>(leftContext.length);
			}
			for (int k = 0; k < leftContext.length; k++) {
				//if(l_context[k]!=Symbol.LM_STATE_OVERLAP_SYM_ID)
					list.add(leftContext[k]);
			}
			boolean consider_incomplete_ngrams = true;
			boolean skip_start = true;
			if (list.get(0) != START_SYM_ID) {
				skip_start = false;
			}
			res += scoreChunk(list, consider_incomplete_ngrams, skip_start);
		}
		/*if (add_start == true) {
			System.out.println("left context: " +Symbol.get_string(l_context) + ";prob "+res);
		}*/
		if (addEnd == true) {//only when add_end is true, we get a complete ngram, otherwise, all ngrams in r_state are incomplete and we should do nothing
			int[]   rightContext = state.getRightLMStateWords();
			ArrayList<Integer> list = new ArrayList<Integer>(rightContext.length+1);
			for (int k = 0; k < rightContext.length; k++) {
				list.add(rightContext[k]);
			}
			list.add(STOP_SYM_ID);
			double tem = scoreChunk(list, false, false);
			res += tem;
			//System.out.println("right context:"+ Symbol.get_string(r_context) + "; score: "  + tem);
		}
		return res;
	}
	
	
	private double computeEquivStateFinalTransition(LMFFDPState state) {
		
		double res = 0.0;
		ArrayList<Integer> currentNgram = new ArrayList<Integer>();
		int[]   leftContext = state.getLeftLMStateWords();		
		int[]   rightContext = state.getRightLMStateWords();
		if (leftContext.length != rightContext.length) {
			throw new RuntimeException(
				"LMModel.compute_equiv_state_final_transition: left and right contexts have unequal lengths");
		}
		
		//================ left context
		if (addStartAndEndSymbol) 
			currentNgram.add(START_SYM_ID);
		
		for (int i = 0; i < leftContext.length; i++) {
			int t = leftContext[i];
			currentNgram.add(t);
			
			if (t == BACKOFF_LEFT_LM_STATE_SYM_ID) {//calculate cost for <bo>: additional backoff weight
				int additional_backoff_weight = currentNgram.size() - (i+1);
				//compute additional backoff weight
				//TOTO: may not work with the case that add_start_and_end_symbol=false
				res -= this.lmGrammar.logProbOfBackoffState(
					currentNgram, currentNgram.size(), additional_backoff_weight);
				
			} else { // partial ngram
				//compute the current word probablity
				if (currentNgram.size() >= 2) { // start from bigram
					res -= this.lmGrammar.ngramLogProbability(
						currentNgram, currentNgram.size());
				}
			}
			if (currentNgram.size() == this.ngramOrder) {
				currentNgram.remove(0);
			}
		}
		
		//================ right context
		//switch context, we will never score the right context probablity because they are either duplicate or partional ngram
		if(addStartAndEndSymbol){
			int tSize = currentNgram.size();
			for (int i = 0; i < rightContext.length; i++) {
				//replace context
				currentNgram.set(tSize - rightContext.length + i, rightContext[i]);
			}
			
			currentNgram.add(STOP_SYM_ID);
			res -= this.lmGrammar.ngramLogProbability(currentNgram, currentNgram.size());
		}
		return res;
	}

	

}

