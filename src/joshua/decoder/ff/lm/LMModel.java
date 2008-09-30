/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.joshua.decoder.feature_function.language_model;

import java.util.ArrayList;
import java.util.HashMap;

import edu.jhu.joshua.decoder.Decoder;
import edu.jhu.joshua.decoder.Support;
import edu.jhu.joshua.decoder.Symbol;
import edu.jhu.joshua.decoder.feature_function.DefaultFF;
import edu.jhu.joshua.decoder.feature_function.MapFFState;
import edu.jhu.joshua.decoder.feature_function.translation_model.Rule;

/**
 * this class implement 
 * (1) Get the additional LM score due to cominations of small items into larger ones by using rules
 * (2) get the LM state 
 * (3) get the left-side LM state estimation score
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class LMModel
extends DefaultFF {
	/* we assume the LM is in ARPA format
	 * for equivalent state: 
	 * (1)we assume it is a backoff lm, and high-order ngram implies low-order ngram; absense of low-order ngram implies high-order ngram
	 * (2) for a ngram, existence of backoffweight => existence a probability
	Two ways of dealing with low counts:
	SRILM: don't multiply zeros in for unknown words
	Pharaoh: cap at a minimum score exp(-10), including unknown words
	*/
	
	protected LMGrammar lmGrammar  = null;
	protected int       ngramOrder = 3;
	//boolean add_boundary=false; //this is needed unless the text already has <s> and </s>
	
	public LMModel(int ngram_order, LMGrammar lm_grammar, double weight_) {
		super(weight_);
		this.ngramOrder = ngram_order;
		this.lmGrammar  = lm_grammar;
	}
	
	
	/*when calculate transition prob: when saw a <bo>, then need to add backoff weights, start from non-state words*/
	//	return states and cost
	private MapFFState lookup_words1_equv_state(
		int[] en_words, ArrayList<MapFFState> previous_states
	) {
		//long start_step1 = Support.current_time();
		//for left state
		ArrayList left_state_org_wrds = new ArrayList();
		//boolean keep_left_state = true;//stop if: (1) end of rule; (2) left_state_org_wrds.size()==this.ngramOrder-1; (3) seperating point;
		
		//before l_context finish, left state words are in current_ngram, after that, all words will be replaced with right state words
		ArrayList<Integer> current_ngram = new ArrayList<Integer>();
		double           transition_cost = 0.0;
		
		for (int c = 0; c < en_words.length; c++) {
			int c_id = en_words[c];
			if (Symbol.is_nonterminal(c_id)) {
				if (null == previous_states) {
					System.out.println("LMModel>>lookup_words1_equv_state: null previous_states");
					System.exit(0);
				}
				
				int     index     = Symbol.get_eng_non_terminal_id(c_id);
				HashMap state     = (HashMap) previous_states.get(index);
				int[]   l_context = (int[])state.get(Symbol.LM_L_STATE_SYM_ID);
				int[]   r_context = (int[])state.get(Symbol.LM_R_STATE_SYM_ID);
				if (l_context.length != r_context.length) {
					System.out.println("LMModel>>lookup_words1_equv_state: left and right contexts have unequal lengths");
					System.exit(0);
				}
				
				//##################left context
				//System.out.println("left context: " + Symbol.get_string(l_context));
				for (int i = 0; i < l_context.length; i++) {
					int t = l_context[i];
					current_ngram.add(t);
					
					//always calculate cost for <bo>: additional backoff weight
					if (t == Symbol.BACKOFF_LEFT_LM_STATE_SYM_ID) {
						int additional_backoff_weight
							= current_ngram.size() - (i+1);
						
						//compute additional backoff weight
						transition_cost
							-= this.lmGrammar.get_prob_backoff_state(
								current_ngram, current_ngram.size(), additional_backoff_weight);
						
						if (current_ngram.size() == this.ngramOrder) {
							current_ngram.remove(0);
						}
					} else if (current_ngram.size() == this.ngramOrder) {
						// compute the current word probablity, and remove it
						transition_cost -= this.lmGrammar.get_prob(
							current_ngram, this.ngramOrder, false);
						
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
					transition_cost -= this.lmGrammar.get_prob(
						current_ngram, this.ngramOrder, true);
					
					current_ngram.remove(0);
				}
				if (left_state_org_wrds.size() < this.ngramOrder - 1) {
					left_state_org_wrds.add(c_id);
				}
			}
		}
		//### create tabl
		MapFFState res_tbl = new MapFFState();
		HashMap  model_states = new HashMap();
		res_tbl.put(Symbol.ITEM_STATES_SYM_ID, model_states);
		
		//##### get left euquiv state 
		double[] lm_l_cost = new double[2];
		int[] equiv_l_state = this.lmGrammar.get_left_equi_state(
			Support.sub_int_array(
				left_state_org_wrds, 0, left_state_org_wrds.size()),
			this.ngramOrder,
			lm_l_cost);
		model_states.put(Symbol.LM_L_STATE_SYM_ID, equiv_l_state);
		//System.out.println("left state: " + Symbol.get_string(equiv_l_state));
		
		//##### trabsition and estimate cost
		transition_cost += lm_l_cost[0];//add finalized cost for the left state words
		res_tbl.put(Symbol.TRANSITION_COST_SYM_ID, transition_cost);
		//System.out.println("##tran cost: " + transition_cost +" lm_l_cost[0]: " + lm_l_cost[0]);
		double estimated_future_cost;
		if (Decoder.use_left_euqivalent_state) {
			estimated_future_cost = lm_l_cost[1];
		} else {
			estimated_future_cost = estimate_state_prob(model_states,false,false);//bonus function
		}
		res_tbl.put(Symbol.BONUS_SYM_ID, estimated_future_cost);
		//##### get right equiv state
		//if(current_ngram.size()>this.ngramOrder-1 || equiv_l_state.length>this.ngramOrder-1)	System.exit(0);
		int[] equiv_r_state = this.lmGrammar.get_right_equi_state(
			Support.sub_int_array(current_ngram, 0, current_ngram.size()),
			this.ngramOrder,
			true);
		model_states.put(Symbol.LM_R_STATE_SYM_ID, equiv_r_state);
		//System.out.println("right state: " + Symbol.get_string(right_state));
		
		//time_step2 += Support.current_time()-start_step2;
		return res_tbl;	
	}
	
	
	private double score_chunk(
		ArrayList words,
		boolean consider_incomplete_ngrams,
		boolean skip_start
	) {
		if (words.size() <= 0) {
			return 0.0;
		}
		if (consider_incomplete_ngrams == true) {
			if (skip_start == true) {
				return -this.lmGrammar.score_a_sent(
					words, this.ngramOrder, 2);
			} else {
				return -this.lmGrammar.score_a_sent(
					words, this.ngramOrder, 1);
			}
		} else {
			return -this.lmGrammar.score_a_sent(
				words, this.ngramOrder, this.ngramOrder);
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
		ArrayList words      = new ArrayList();
		boolean   skip_start = (en_words[0] == Symbol.START_SYM_ID);
		
		for (int c = 0; c < en_words.length; c++) {
			int c_wrd = en_words[c];
			/*if (c_wrd == Symbol.ECLIPS_SYM_ID) {
				estimate += score_chunk(
					words, consider_incomplete_ngrams, skip_start);
				consider_incomplete_ngrams = false;//for the LM bonus function: this simply means the right state will not be considered at all because all the ngrams in right-context will be incomplete
				words.clear();
				skip_start = false;
			} else*/ if (Symbol.is_nonterminal(c_wrd)) {
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
	private double estimate_state_prob(
		HashMap state,
		boolean add_start,
		boolean add_end
	) {
		double res = 0.0;
		int[] l_context = (int[])state.get(Symbol.LM_L_STATE_SYM_ID);
		
		if (null != l_context) {
			ArrayList list;
			if (add_start == true) {
				list = new ArrayList(l_context.length + 1);
				list.add(Symbol.START_SYM_ID);
			} else {
				list = new ArrayList(l_context.length);
			}
			for (int k = 0; k < l_context.length; k++) {
				//if(l_context[k]!=Symbol.LM_STATE_OVERLAP_SYM_ID)
					list.add(l_context[k]);
			}
			boolean consider_incomplete_ngrams = true;
			boolean skip_start = true;
			if ((Integer)list.get(0) != Symbol.START_SYM_ID) {
				skip_start = false;
			}
			res += score_chunk(list, consider_incomplete_ngrams, skip_start);
		}
		/*if (add_start == true) {
			System.out.println("left context: " +Symbol.get_string(l_context) + ";prob "+res);
		}*/
		if (add_end == true) {//only when add_end is true, we get a complete ngram, otherwise, all ngrams in r_state are incomplete and we should do nothing
			int[] r_context = (int[])state.get(Symbol.LM_R_STATE_SYM_ID);
			ArrayList list = new ArrayList(r_context.length+1);
			for (int k = 0; k < r_context.length; k++) {
				list.add(r_context[k]);
			}
			list.add(Symbol.STOP_SYM_ID);
			double tem = score_chunk(list, false, false);
			res += tem;
			//System.out.println("right context:"+ Symbol.get_string(r_context) + "; score: "  + tem);
		}
		return res;
	}
	
	
	private double compute_equiv_state_final_transition(HashMap state) {
		double res = 0.0;
		ArrayList current_ngram = new ArrayList();
		int[] l_context = (int[])state.get(Symbol.LM_L_STATE_SYM_ID);
		int[] r_context = (int[])state.get(Symbol.LM_R_STATE_SYM_ID);
		if (l_context.length != r_context.length) {
			System.out.println("LMModel>>compute_equiv_state_final_transition: left and right contexts have unequal lengths");
			System.exit(0);
		}
		
		//##################left context
		current_ngram.add(Symbol.START_SYM_ID);
		for (int i = 0; i < l_context.length; i++) {
			int t = l_context[i];
			current_ngram.add(t);
			
			if (t == Symbol.BACKOFF_LEFT_LM_STATE_SYM_ID) {//calculate cost for <bo>: additional backoff weight
				int additional_backoff_weight = current_ngram.size() - (i+1);
				//compute additional backoff weight
				res -= this.lmGrammar.get_prob_backoff_state(
					current_ngram, current_ngram.size(), additional_backoff_weight);
			} else {//partial ngram
				//compute the current word probablity
				res -= this.lmGrammar.get_prob(
					current_ngram, current_ngram.size(), false);
			}
			if (current_ngram.size() == this.ngramOrder) {
				current_ngram.remove(0);
			}
		}
		
		//####################right context
		//switch context, we will never score the right context probablity because they are either duplicate or partional ngram
		int t_size = current_ngram.size();
		for (int i = 0; i < r_context.length; i++) {
			//replace context
			current_ngram.set(t_size - r_context.length + i, r_context[i]);
		}
		
		current_ngram.add(Symbol.STOP_SYM_ID);
		res -= this.lmGrammar.get_prob(
			current_ngram, current_ngram.size(), false);
		return res;
	}


	/*the transition cost for LM: sum of the costs of the new ngrams created
	 * depends on the antstates and current rule*/
	//antstates: ArrayList of states of this model in ant items
	public MapFFState transition(
		Rule rule, ArrayList<MapFFState> previous_states, int i, int j
	) {
		//long start = Support.current_time();
		
		MapFFState state = lookup_words1_equv_state(rule.english, previous_states);
		
		//	Chart.g_time_lm += Support.current_time()-start;
		return state;
	}




	/*depends on the rule only*/
	/*will consider all the complete ngrams, and all the incomplete-ngrams that will have sth fit into its left side*/
	public double estimate(Rule rule) {
		return estimate_rule_prob(rule.english);
	}
	
	//only called after a complete hyp for the whole input sentence is obtaned
	public double finalTransition(HashMap state) {
		if (null != state) {
			return compute_equiv_state_final_transition(state);
		} else {
			return 0.0;
		}
	}
}

