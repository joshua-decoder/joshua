package edu.jhu.joshua.VariationalDecoder;

import java.util.ArrayList;
import java.util.HashMap;

import joshua.decoder.Symbol;
import joshua.decoder.ff.lm.LMFFDPState;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import edu.jhu.joshua.discriminative_training.feature_related.NgramFeatureTemplate;

public class GenerativeNgramFeatureTemplate extends NgramFeatureTemplate {

	public GenerativeNgramFeatureTemplate(Symbol symbol_, boolean use_integer_ngram_, int lm_feat_id_, int baseline_lm_order, int start_order, int end_order) {
		super(symbol_, use_integer_ngram_, lm_feat_id_, baseline_lm_order, start_order,	end_order);
		if(baseline_lm_order!=end_order){
			System.out.println("baseline lm order is not equal end_lm_order");
			System.exit(0);
		}
	}

	
	/*when calculate transition prob: when saw a <bo>, then need to add backoff weights, start from non-state words*/
	//	return states and cost
	protected HashMap get_all_ngrams(HyperEdge dt, int baseline_lm_order, int start_ngram_order, int end_ngram_order){
    	if(baseline_lm_order<=1){System.out.println("lm order is too small"); System.exit(0);}
	
    	//##### deductions under "goal item" does not have rule
		if(dt.get_rule()==null){
			if(dt.get_ant_items().size()!=1) {System.out.println("error deduction under goal item have more than one item"); System.exit(0);}
			return compute_final_transition(dt, baseline_lm_order, start_ngram_order, end_ngram_order);
		}
	
		//#### not goal item
		HashMap res = new HashMap();
		
		//before l_context finish, left state words are in current_ngram, after that, all words will be replaced with right state words
		ArrayList<Integer> current_ngram   = new ArrayList<Integer>();
		
		int[] en_words = dt.get_rule().english;
		for (int c = 0; c < en_words.length; c++) {
			int c_id = en_words[c];
			if (p_symbol.isNonterminal(c_id)) {
				int index= p_symbol.getEngNonTerminalIndex(c_id);
    			HGNode ant_item = (HGNode) dt.get_ant_items().get(index);      
    			LMFFDPState state     = (LMFFDPState) ant_item.getFeatDPState(this.lm_feat_id);
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
					
					if (current_ngram.size() == baseline_lm_order) {//TODO: should be end_order?
						// compute the current word probablity, and remove it
						this.getNgrams(res, current_ngram.size(), current_ngram.size(), current_ngram);
						current_ngram.remove(0);
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
				if (current_ngram.size() == baseline_lm_order) {//TODO: should be end_order?
					// compute the current word probablity, and remove it
					this.getNgrams(res, current_ngram.size(), current_ngram.size(), current_ngram);
					current_ngram.remove(0);
				}				 
			}
		}
	
		return res;	
	}
	

	private HashMap compute_final_transition(HyperEdge dt, int baseline_lm_order, int start_ngram_order, int end_ngram_order) {
		HashMap res = new HashMap();
		HGNode ant_item = (HGNode) dt.get_ant_items().get(0);  
		LMFFDPState state     = (LMFFDPState) ant_item.getFeatDPState(this.lm_feat_id);
		
		ArrayList<Integer> current_ngram = new ArrayList<Integer>();
		int[]   l_context = state.getLeftLMStateWords();		
		int[]   r_context = state.getRightLMStateWords();
		if (l_context.length != r_context.length) {
			System.out.println("LMModel>>compute_equiv_state_final_transition: left and right contexts have unequal lengths");
			System.exit(1);
		}
		
		//##################left context
		current_ngram.add(START_SYM_ID);
		for (int i = 0; i < l_context.length; i++) {
			int t = l_context[i];
			current_ngram.add(t);
			
			//compute the current word probablity
			if(current_ngram.size()>=start_ngram_order && current_ngram.size()<=end_ngram_order)
				this.getNgrams(res, current_ngram.size(), current_ngram.size(), current_ngram);
			
			if (current_ngram.size() == baseline_lm_order) {//TODO: should be end_order?
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
		
		current_ngram.add(STOP_SYM_ID);
		if(current_ngram.size()>=start_ngram_order && current_ngram.size()<=end_ngram_order)
			this.getNgrams(res, current_ngram.size(), current_ngram.size(), current_ngram);
		
		return res;
	}


	protected HashMap get_all_uni_and_bigrams(HyperEdge dt, int baseline_lm_order) {
		System.out.println("get_all_uni_and_bigrams: This function should never be called");
		System.exit(0);
		return null;
	}


}
