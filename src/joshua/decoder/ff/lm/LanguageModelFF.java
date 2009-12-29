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
import java.util.logging.Level;
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
	//antstates: ArrayList of states of this model in ant items
	public StatefulFFTransitionResult transition(Rule rule, ArrayList<FFDPState> previousStates, int spanStart, int spanEnd, SourcePath srcPath) {
		//long start = Support.current_time();		
		StatefulFFTransitionResult res = this.lookupWordsEquvState(rule.getEnglish(), previousStates);	
		//	Chart.g_time_lm += Support.current_time()-start;
		return res;
	}

	
	/*depends on the rule only*/
	/*will consider all the complete ngrams, and all the incomplete-ngrams that will have sth fit into its left side*/
	public double estimate(Rule rule) {
		return estimateRuleProb(rule.getEnglish());
	}
	
	//only called after a complete hyp for the whole input sentence is obtaned
	public double finalTransition(FFDPState state) {
		if (null != state) {
			return computeEquivStateFinalTransition((LMFFDPState)state);
		} else {
			return 0.0;
		}
	}
	
	/*when calculate transition prob: when saw a <bo>, then need to add backoff weights, start from non-state words*/
	//	return states and cost
	private StatefulFFTransitionResult lookupWordsEquvState(int[] enWords,	ArrayList<FFDPState> previousStates) {
		//long start_step1 = Support.current_time();
		//for left state
		ArrayList<Integer> leftLMStateWrds = new ArrayList<Integer>();
		//boolean keep_left_state = true;//stop if: (1) end of rule; (2) left_state_org_wrds.size()==this.ngramOrder-1; (3) seperating point;
		
		//before l_context finish, left state words are in current_ngram, after that, all words will be replaced with right state words
		ArrayList<Integer> currentNgram   = new ArrayList<Integer>();
		double             transition_cost = 0.0;
		
		for (int c = 0; c < enWords.length; c++) {
			int c_id = enWords[c];
			if (symbolTable.isNonterminal(c_id)) {
				if (null == previousStates) {
					throw new IllegalArgumentException(
						"LMModel.lookup_words1_equv_state: null previous_states");
				}
				
				int index = symbolTable.getTargetNonterminalIndex(c_id);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest(
						"Target symbol " + symbolTable.getWord(c_id) 
						+ " with id " + c_id 
						+ " and nonterminal index of " + index);
				}
				if (index >= previousStates.size()) {
					logger.severe(
							"Target symbol " + symbolTable.getWord(c_id) 
							+ " with id " + c_id 
							+ " and nonterminal index of " + index 
							+ " is about to trigger an IndexOutOfBoundsException");
				}
				LMFFDPState state = (LMFFDPState) previousStates.get(index);
				int[] l_context = state.getLeftLMStateWords();
				int[] r_context = state.getRightLMStateWords();
				if (l_context.length != r_context.length) {
					throw new RuntimeException(
						"LMModel.lookup_words1_equv_state: left and right contexts have unequal lengths");
				}
				
				//##################left context
				//System.out.println("left context: " + Symbol.get_string(l_context));
				for (int i = 0; i < l_context.length; i++) {
					int t = l_context[i];
					currentNgram.add(t);
					
					//always calculate cost for <bo>: additional backoff weight
					if (t == BACKOFF_LEFT_LM_STATE_SYM_ID) {
						int additional_backoff_weight = currentNgram.size() - (i+1);
						
						//compute additional backoff weight
						transition_cost	-= this.lmGrammar.logProbabilityOfBackoffState(currentNgram, currentNgram.size(), additional_backoff_weight);
						
						if (currentNgram.size() == this.ngramOrder) {
							currentNgram.remove(0);
						}
					} else if (currentNgram.size() == this.ngramOrder) {
						// compute the current word probablity, and remove it
						transition_cost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);
						
						currentNgram.remove(0);
					}
					
					if (leftLMStateWrds.size() < this.ngramOrder - 1) {
						leftLMStateWrds.add(t);
					}
				}
				
				//####################right context
				//note: left_state_org_wrds will never take words from right context because it is either duplicate or out of range
				//also, we will never score the right context probablity because they are either duplicate or partional ngram
				//System.out.println("right context: " + Symbol.get_string(r_context));
				int t_size = currentNgram.size();
				for (int i = 0; i < r_context.length; i++) {
					// replace context
					currentNgram.set(t_size - r_context.length + i, r_context[i]);
				}
			
			} else {//terminal words
				//System.out.println("terminal: " + Symbol.get_string(c_id));
				currentNgram.add(c_id);
				if (currentNgram.size() == this.ngramOrder) {
					// compute the current word probablity, and remove it
					transition_cost -= this.lmGrammar.ngramLogProbability(currentNgram, this.ngramOrder);
					
					currentNgram.remove(0);
				}
				if (leftLMStateWrds.size() < this.ngramOrder - 1) {
					leftLMStateWrds.add(c_id);
				}
			}
		}
		//===== create tabl
		StatefulFFTransitionResult resTbl = new StatefulFFTransitionResult();
		LMFFDPState  modelStates = new LMFFDPState();
		resTbl.setStateForNode(modelStates);
		
		//===== get left euquiv state 
		double[] lm_l_cost = new double[2];
		int[] equiv_l_state = this.lmGrammar.leftEquivalentState(Support.sub_int_array(leftLMStateWrds, 0, leftLMStateWrds.size()),	this.ngramOrder, lm_l_cost);
		modelStates.setLeftLMStateWords(equiv_l_state);
		//System.out.println("left state: " + Symbol.get_string(equiv_l_state));
		
		//===== trabsition and estimate cost
		transition_cost += lm_l_cost[0];//add finalized cost for the left state words
		resTbl.setTransitionCost(transition_cost);
		//System.out.println("##tran cost: " + transition_cost +" lm_l_cost[0]: " + lm_l_cost[0]);
		double estimatedFutureCost=0.0;
		if(this.ngramOrder>1){//no estiamtion for unigram lm
			if (JoshuaConfiguration.use_left_equivalent_state) {
				estimatedFutureCost = lm_l_cost[1];
			} else {
				estimatedFutureCost = estimateStateProb(modelStates, false, false);//bonus function
			}
		}
		resTbl.setFutureCostEstimation(estimatedFutureCost);
		
		//===== get right equiv state
		//if(current_ngram.size()>this.ngramOrder-1 || equiv_l_state.length>this.ngramOrder-1) throw new RuntimeException();
		int[] equiv_r_state = this.lmGrammar.rightEquivalentState(Support.sub_int_array(currentNgram, 0, currentNgram.size()), this.ngramOrder);
		modelStates.setRightLMStateWords(equiv_r_state);
		//System.out.println("right state: " + Symbol.get_string(right_state));
		
		//time_step2 += Support.current_time()-start_step2;
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
		boolean   consider_incomplete_ngrams = true;
		ArrayList<Integer> words      = new ArrayList<Integer>();
		boolean   skip_start = (enWords[0] == START_SYM_ID);
		
		for (int c = 0; c < enWords.length; c++) {
			int c_wrd = enWords[c];
			/*if (c_wrd == Symbol.ECLIPS_SYM_ID) {
				estimate += score_chunk(
					words, consider_incomplete_ngrams, skip_start);
				consider_incomplete_ngrams = false;//for the LM bonus function: this simply means the right state will not be considered at all because all the ngrams in right-context will be incomplete
				words.clear();
				skip_start = false;
			} else*/ if (symbolTable.isNonterminal(c_wrd)) {
				estimate += scoreChunk(
					words, consider_incomplete_ngrams, skip_start);
				consider_incomplete_ngrams = true;
				words.clear();
				skip_start = false;
			} else {
				words.add(c_wrd);
			}
		}
		estimate += scoreChunk(
			words, consider_incomplete_ngrams, skip_start);
		return estimate;
	}
	
	
	//this function is called when left_equiv state is NOT used
	//in state, all the ngrams are incomplete
	//only get the estimation for the left-state
	//get the true prob for right-state, if add_end==true
	private double estimateStateProb(LMFFDPState state, boolean addStart, boolean addEnd) {
		double res = 0.0;
		
		int[]   l_context = state.getLeftLMStateWords();
		
		if (null != l_context) {
			ArrayList<Integer> list;
			if (addStart == true) {
				list = new ArrayList<Integer>(l_context.length + 1);
				list.add(START_SYM_ID);
			} else {
				list = new ArrayList<Integer>(l_context.length);
			}
			for (int k = 0; k < l_context.length; k++) {
				//if(l_context[k]!=Symbol.LM_STATE_OVERLAP_SYM_ID)
					list.add(l_context[k]);
			}
			boolean consider_incomplete_ngrams = true;
			boolean skip_start = true;
			if ((Integer)list.get(0) != START_SYM_ID) {
				skip_start = false;
			}
			res += scoreChunk(list, consider_incomplete_ngrams, skip_start);
		}
		/*if (add_start == true) {
			System.out.println("left context: " +Symbol.get_string(l_context) + ";prob "+res);
		}*/
		if (addEnd == true) {//only when add_end is true, we get a complete ngram, otherwise, all ngrams in r_state are incomplete and we should do nothing
			int[]   r_context = state.getRightLMStateWords();
			ArrayList<Integer> list = new ArrayList<Integer>(r_context.length+1);
			for (int k = 0; k < r_context.length; k++) {
				list.add(r_context[k]);
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
		ArrayList<Integer> current_ngram = new ArrayList<Integer>();
		int[]   l_context = state.getLeftLMStateWords();		
		int[]   r_context = state.getRightLMStateWords();
		if (l_context.length != r_context.length) {
			throw new RuntimeException(
				"LMModel.compute_equiv_state_final_transition: left and right contexts have unequal lengths");
		}
		
		//##################left context
		if (addStartAndEndSymbol) current_ngram.add(START_SYM_ID);
		
		for (int i = 0; i < l_context.length; i++) {
			int t = l_context[i];
			current_ngram.add(t);
			
			if (t == BACKOFF_LEFT_LM_STATE_SYM_ID) {//calculate cost for <bo>: additional backoff weight
				int additional_backoff_weight = current_ngram.size() - (i+1);
				//compute additional backoff weight
				//TOTO: may not work with the case that add_start_and_end_symbol=false
				res -= this.lmGrammar.logProbabilityOfBackoffState(
					current_ngram, current_ngram.size(), additional_backoff_weight);
				
			} else { // partial ngram
				//compute the current word probablity
				if (current_ngram.size() >= 2) { // start from bigram
					res -= this.lmGrammar.ngramLogProbability(
						current_ngram, current_ngram.size());
				}
			}
			if (current_ngram.size() == this.ngramOrder) {
				current_ngram.remove(0);
			}
		}
		
		//####################right context
		//switch context, we will never score the right context probablity because they are either duplicate or partional ngram
		if(addStartAndEndSymbol){
			int t_size = current_ngram.size();
			for (int i = 0; i < r_context.length; i++) {
				//replace context
				current_ngram.set(t_size - r_context.length + i, r_context[i]);
			}
			
			current_ngram.add(STOP_SYM_ID);
			res -= this.lmGrammar.ngramLogProbability(current_ngram, current_ngram.size());
		}
		return res;
	}

	

}

