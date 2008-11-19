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

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.Symbol;
import joshua.decoder.ff.DefaultStatefulFF;
import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.StatefulFFTransitionResult;
import joshua.decoder.ff.tm.Rule;

import java.util.ArrayList;


/**
 * this class implement 
 * (1) Get the additional LM score due to combinations of small items into larger ones by using rules
 * (2) get the LM state 
 * (3) get the left-side LM state estimation score
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */



/*Note: the LMGrammar returns LogP; while the LMFeatureFunction needs to return cost (i.e., -LogP)
 * */


public class LMFeatureFunction extends DefaultStatefulFF {
	static String START_SYM="<s>";
	public  int START_SYM_ID;
	static String STOP_SYM="</s>";
	public int STOP_SYM_ID;
	
	boolean add_start_and_end_symbol = true;
	
	/* we assume the LM is in ARPA format
	 * for equivalent state: 
	 * (1)we assume it is a backoff lm, and high-order ngram implies low-order ngram; absense of low-order ngram implies high-order ngram
	 * (2) for a ngram, existence of backoffweight => existence a probability
	Two ways of dealing with low counts:
	SRILM: don't multiply zeros in for unknown words
	Pharaoh: cap at a minimum score exp(-10), including unknown words
	*/
	
	private LMGrammar lmGrammar  = null;
	private int       ngramOrder = 3;//we always use this order of ngram, though the LMGrammar may provide higher order probability
	//boolean add_boundary=false; //this is needed unless the text already has <s> and </s>
	
	private Symbol p_symbol = null;
	
	public LMFeatureFunction(int feat_id_, int ngram_order, Symbol psymbol, LMGrammar lm_grammar, double weight_) {
		super(weight_, feat_id_);
		this.ngramOrder = ngram_order;
		this.lmGrammar  = lm_grammar;
		this.p_symbol = psymbol;
		this.START_SYM_ID = psymbol.addTerminalSymbol(START_SYM);
		this.STOP_SYM_ID = psymbol.addTerminalSymbol(STOP_SYM);		
	}
	
	/*the transition cost for LM: sum of the costs of the new ngrams created
	 * depends on the antstates and current rule*/
	//antstates: ArrayList of states of this model in ant items
	public StatefulFFTransitionResult transition(Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end) {
		//long start = Support.current_time();		
		StatefulFFTransitionResult res = this.lookup_words1_equv_state(rule.english, previous_states);	
		//	Chart.g_time_lm += Support.current_time()-start;
		return res;
	}


	
	/*depends on the rule only*/
	/*will consider all the complete ngrams, and all the incomplete-ngrams that will have sth fit into its left side*/
	public double estimate(Rule rule) {
		return estimate_rule_prob(rule.english);
	}
	
	//only called after a complete hyp for the whole input sentence is obtaned
	public double finalTransition(FFDPState state) {
		if (null != state) {
			return compute_equiv_state_final_transition((LMFFDPState)state);
		} else {
			return 0.0;
		}
	}
	
	/*when calculate transition prob: when saw a <bo>, then need to add backoff weights, start from non-state words*/
	//	return states and cost
	private StatefulFFTransitionResult lookup_words1_equv_state(int[] en_words,	ArrayList<FFDPState> previous_states) {
		//long start_step1 = Support.current_time();
		//for left state
		ArrayList<Integer> left_state_org_wrds = new ArrayList<Integer>();
		//boolean keep_left_state = true;//stop if: (1) end of rule; (2) left_state_org_wrds.size()==this.ngramOrder-1; (3) seperating point;
		
		//before l_context finish, left state words are in current_ngram, after that, all words will be replaced with right state words
		ArrayList<Integer> current_ngram   = new ArrayList<Integer>();
		double             transition_cost = 0.0;
		
		for (int c = 0; c < en_words.length; c++) {
			int c_id = en_words[c];
			if (p_symbol.isNonterminal(c_id)) {
				if (null == previous_states) {
					System.out.println("LMModel>>lookup_words1_equv_state: null previous_states");
					System.exit(1);
				}
				
				int     index     = p_symbol.getEngNonTerminalIndex(c_id);
				LMFFDPState state     = (LMFFDPState) previous_states.get(index);
				int[]   l_context = state.getLeftLMStateWords();
				int[]   r_context = state.getRightLMStateWords();
				if (l_context.length != r_context.length) {
					System.out.println("LMModel>>lookup_words1_equv_state: left and right contexts have unequal lengths");
					System.exit(1);
				}
				
				//##################left context
				//System.out.println("left context: " + Symbol.get_string(l_context));
				for (int i = 0; i < l_context.length; i++) {
					int t = l_context[i];
					current_ngram.add(t);
					
					//always calculate cost for <bo>: additional backoff weight
					if (t == lmGrammar.BACKOFF_LEFT_LM_STATE_SYM_ID) {
						int additional_backoff_weight = current_ngram.size() - (i+1);
						
						//compute additional backoff weight
						transition_cost	-= this.lmGrammar.get_prob_backoff_state(current_ngram, current_ngram.size(), additional_backoff_weight);
						
						if (current_ngram.size() == this.ngramOrder) {
							current_ngram.remove(0);
						}
					} else if (current_ngram.size() == this.ngramOrder) {
						// compute the current word probablity, and remove it
						transition_cost -= this.lmGrammar.get_prob(current_ngram, this.ngramOrder, false);
						
						current_ngram.remove(0);
					}
					
					if (left_state_org_wrds.size() < this.ngramOrder - 1) {
						left_state_org_wrds.add(t);
					}
				}
				
				//####################right context
				//note: left_state_org_wrds will never take words from right context because it is either duplicate or out of range
				//also, we will never score the right context probablity because they are either duplicate or partional ngram
				//System.out.println("right context: " + Symbol.get_string(r_context));
				int t_size = current_ngram.size();
				for (int i = 0; i < r_context.length; i++) {
					// replace context
					current_ngram.set(t_size - r_context.length + i, r_context[i]);
				}
			
			} else {//terminal words
				//System.out.println("terminal: " + Symbol.get_string(c_id));
				current_ngram.add(c_id);
				if (current_ngram.size() == this.ngramOrder) {
					// compute the current word probablity, and remove it
					transition_cost -= this.lmGrammar.get_prob(current_ngram, this.ngramOrder, true);
					
					current_ngram.remove(0);
				}
				if (left_state_org_wrds.size() < this.ngramOrder - 1) {
					left_state_org_wrds.add(c_id);
				}
			}
		}
		//### create tabl
		StatefulFFTransitionResult res_tbl = new StatefulFFTransitionResult();
		LMFFDPState  model_states = new LMFFDPState();
		res_tbl.putStateForItem(model_states);
		
		//##### get left euquiv state 
		double[] lm_l_cost = new double[2];
		int[] equiv_l_state = this.lmGrammar.get_left_equi_state(Support.sub_int_array(left_state_org_wrds, 0, left_state_org_wrds.size()),	this.ngramOrder, lm_l_cost);
		model_states.setLeftLMStateWords(equiv_l_state);
		//System.out.println("left state: " + Symbol.get_string(equiv_l_state));
		
		//##### trabsition and estimate cost
		transition_cost += lm_l_cost[0];//add finalized cost for the left state words
		res_tbl.putTransitionCost(transition_cost);
		//System.out.println("##tran cost: " + transition_cost +" lm_l_cost[0]: " + lm_l_cost[0]);
		double estimated_future_cost=0.0;
		if(this.ngramOrder>1){//no estiamtion for unigram lm
			if (JoshuaConfiguration.use_left_euqivalent_state) {
				estimated_future_cost = lm_l_cost[1];
			} else {
				estimated_future_cost = estimate_state_prob(model_states,false,false);//bonus function
			}
		}
		res_tbl.putFutureCostEstimation(estimated_future_cost);
		//##### get right equiv state
		//if(current_ngram.size()>this.ngramOrder-1 || equiv_l_state.length>this.ngramOrder-1)	System.exit(1);
		int[] equiv_r_state = this.lmGrammar.get_right_equi_state(Support.sub_int_array(current_ngram, 0, current_ngram.size()), this.ngramOrder, true);
		model_states.setRightLMStateWords(equiv_r_state);
		//System.out.println("right state: " + Symbol.get_string(right_state));
		
		//time_step2 += Support.current_time()-start_step2;
		return res_tbl;	
	}
	
	
	private double score_chunk(ArrayList<Integer> words, boolean consider_incomplete_ngrams, boolean skip_start) {
		if (words.size() <= 0) {
			return 0.0;
		}
		if (consider_incomplete_ngrams == true) {
			if (skip_start == true) {
				return -this.lmGrammar.score_a_sent(words, this.ngramOrder, 2);
			} else {
				return -this.lmGrammar.score_a_sent(words, this.ngramOrder, 1);
			}
		} else {
			return -this.lmGrammar.score_a_sent(words, this.ngramOrder, this.ngramOrder);
		}
	}
	
	
	//return cost, including partial ngrams
	/*in general: consider all the complete ngrams, and all the incomplete-ngrams that WILL have sth fit into its left side, so
	*if the left side of incomplete-ngrams is a ECLIPS, then ignore the incomplete-ngrams
	*if the left side of incomplete-ngrams is a Non-Terminal, then consider the incomplete-ngrams  
	*if the left side of incomplete-ngrams is boundary of a rule, then consider the incomplete-ngrams*/
	private double estimate_rule_prob(int[] en_words) {
		double    estimate   = 0.0;
		boolean   consider_incomplete_ngrams = true;
		ArrayList<Integer> words      = new ArrayList<Integer>();
		boolean   skip_start = (en_words[0] == START_SYM_ID);
		
		for (int c = 0; c < en_words.length; c++) {
			int c_wrd = en_words[c];
			/*if (c_wrd == Symbol.ECLIPS_SYM_ID) {
				estimate += score_chunk(
					words, consider_incomplete_ngrams, skip_start);
				consider_incomplete_ngrams = false;//for the LM bonus function: this simply means the right state will not be considered at all because all the ngrams in right-context will be incomplete
				words.clear();
				skip_start = false;
			} else*/ if (p_symbol.isNonterminal(c_wrd)) {
				estimate += score_chunk(
					words, consider_incomplete_ngrams, skip_start);
				consider_incomplete_ngrams = true;
				words.clear();
				skip_start = false;
			} else {
				words.add(c_wrd);
			}
		}
		estimate += score_chunk(
			words, consider_incomplete_ngrams, skip_start);
		return estimate;
	}
	
	
	//this function is called when left_equiv state is NOT used
	//in state, all the ngrams are incomplete
	//only get the estimation for the left-state
	//get the true prob for right-state, if add_end==true
	private double estimate_state_prob(LMFFDPState state, boolean add_start, boolean add_end) {
		double res = 0.0;
		
		int[]   l_context = state.getLeftLMStateWords();		
		
		if (null != l_context) {
			ArrayList<Integer> list;
			if (add_start == true) {
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
			res += score_chunk(list, consider_incomplete_ngrams, skip_start);
		}
		/*if (add_start == true) {
			System.out.println("left context: " +Symbol.get_string(l_context) + ";prob "+res);
		}*/
		if (add_end == true) {//only when add_end is true, we get a complete ngram, otherwise, all ngrams in r_state are incomplete and we should do nothing
			int[]   r_context = state.getRightLMStateWords();
			ArrayList<Integer> list = new ArrayList<Integer>(r_context.length+1);
			for (int k = 0; k < r_context.length; k++) {
				list.add(r_context[k]);
			}
			list.add(STOP_SYM_ID);
			double tem = score_chunk(list, false, false);
			res += tem;
			//System.out.println("right context:"+ Symbol.get_string(r_context) + "; score: "  + tem);
		}
		return res;
	}
	
	
	private double compute_equiv_state_final_transition(LMFFDPState state) {
		double res = 0.0;
		ArrayList<Integer> current_ngram = new ArrayList<Integer>();
		int[]   l_context = state.getLeftLMStateWords();		
		int[]   r_context = state.getRightLMStateWords();
		if (l_context.length != r_context.length) {
			System.out.println("LMModel>>compute_equiv_state_final_transition: left and right contexts have unequal lengths");
			System.exit(1);
		}
		
		//##################left context
		if(add_start_and_end_symbol)
			current_ngram.add(START_SYM_ID);
		for (int i = 0; i < l_context.length; i++) {
			int t = l_context[i];
			current_ngram.add(t);
			
			if (t == lmGrammar.BACKOFF_LEFT_LM_STATE_SYM_ID) {//calculate cost for <bo>: additional backoff weight
				int additional_backoff_weight = current_ngram.size() - (i+1);
				//compute additional backoff weight
				//TOTO: may not work with the case that add_start_and_end_symbol=false
				res -= this.lmGrammar.get_prob_backoff_state(current_ngram, current_ngram.size(), additional_backoff_weight);
			} else {//partial ngram
				//compute the current word probablity
				if(current_ngram.size()>=2)//start from bigram
					res -= this.lmGrammar.get_prob(current_ngram, current_ngram.size(), false);
			}
			if (current_ngram.size() == this.ngramOrder) {
				current_ngram.remove(0);
			}
		}
		
		//####################right context
		//switch context, we will never score the right context probablity because they are either duplicate or partional ngram
		if(add_start_and_end_symbol){
			int t_size = current_ngram.size();
			for (int i = 0; i < r_context.length; i++) {
				//replace context
				current_ngram.set(t_size - r_context.length + i, r_context[i]);
			}
			
			current_ngram.add(STOP_SYM_ID);
			res -= this.lmGrammar.get_prob(current_ngram, current_ngram.size(), false);
		}
		return res;
	}

	

}

