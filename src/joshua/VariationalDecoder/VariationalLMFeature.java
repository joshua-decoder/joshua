package edu.jhu.joshua.VariationalDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.jhu.joshua.discriminative_training.DiscriminativeSupport;

import joshua.decoder.Symbol;
import joshua.decoder.ff.DefaultStatefulFF;
import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.StatefulFFTransitionResult;
import joshua.decoder.ff.lm.LMFFDPState;
import joshua.decoder.ff.tm.Rule;

public class VariationalLMFeature extends DefaultStatefulFF {
	HashMap<String, Double> p_ngram_model; //normalized model
	public static String ZERO_GRAM = "lzfzerogram";
	static String START_SYM="<s>";
	public  int START_SYM_ID;
	static String STOP_SYM="</s>";
	public int STOP_SYM_ID;
	
	private int       ngramOrder = 3;//we always use this order of ngram, though the LMGrammar may provide higher order probability
	//boolean add_boundary=false; //this is needed unless the text already has <s> and </s>
	
	private Symbol p_symbol = null;
	
	public VariationalLMFeature(int feat_id_, int ngram_order, Symbol psymbol, HashMap<String, Double>  ngram_model, double weight_) {
		super(weight_, feat_id_);
		this.ngramOrder = ngram_order;
		this.p_ngram_model  = ngram_model;//this is the normalized table
		this.p_symbol = psymbol;
		this.START_SYM_ID = psymbol.addTerminalSymbol(START_SYM);
		this.STOP_SYM_ID = psymbol.addTerminalSymbol(STOP_SYM);		
		
		if(p_ngram_model==null){
			System.out.println("p_ngram_feat_count_tbl is null");
			System.exit(0);
		}
		//System.out.println("p_ngram_feat_count_tbl size is " + p_ngram_model.size() + "\n" +p_ngram_model.toString());
		System.out.println("p_ngram_feat_count_tbl size is " + p_ngram_model.size());
	
	}
	
	
	
	public  static void getNormalizedLM(HashMap ngram_feat_count_tbl, int order){
		HashMap tbl_denominator = new HashMap();
		int[] num_ngrams = new int[order];
		//### first get normalized constants
		System.out.println("#### Begin to get the normalization constants");
		for (Iterator iter = ngram_feat_count_tbl.entrySet().iterator(); iter.hasNext();)		{ 
		    Map.Entry entry = (Map.Entry)iter.next();
		    String ngram = (String) entry.getKey();
		    double count = (Double) entry.getValue();
		    String[] wrds = ngram.split("\\s+");
		    num_ngrams[wrds.length-1]++;
		    if(wrds.length==1){//unigram
		    	DiscriminativeSupport.increase_count(tbl_denominator, ZERO_GRAM, count);
		    }else{
		    	StringBuffer history = new StringBuffer();
		    	for(int i=0; i<wrds.length-1; i++){
		    		history.append(wrds[0]);
		    		if(i<wrds.length-2)
		    			history.append(" ");
		    	}
		    	DiscriminativeSupport.increase_count(tbl_denominator, history.toString(), count);
		    }
		}
		
		//### now change the orignal table
		System.out.println("#### Begin to get normalize the original ngram tbl");
		for (Iterator iter = ngram_feat_count_tbl.entrySet().iterator(); iter.hasNext();)		{ 
		    Map.Entry entry = (Map.Entry)iter.next();
		    String ngram = (String) entry.getKey();
		    double count = (Double) entry.getValue();
		    String[] wrds = ngram.split("\\s+");
		    if(wrds.length==1){//unigram
		    	entry.setValue(count*1.0/(Double) tbl_denominator.get(ZERO_GRAM));//change the value
		    }else{
		    	StringBuffer history = new StringBuffer();
		    	for(int i=0; i<wrds.length-1; i++){
		    		history.append(wrds[0]);
		    		if(i<wrds.length-2)
		    			history.append(" ");
		    	}
		    	entry.setValue(count*1.0/(Double) tbl_denominator.get(history.toString()));//change the value
		    }
		}
		//print stat
		for(int i=0; i<order; i++){
			System.out.println((i+1) + "-gram: " + num_ngrams[i]);
		}
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
 
	//only called after a complete hyp for the whole input sentence is obtaned
	public double finalTransition(FFDPState state) {
		if (null != state) {
			//System.out.println("final transtion ########");
			return compute_equiv_state_final_transition((LMFFDPState)state);
		} else {
			return 0.0;
		}
	}
	
	/*when calculate transition prob: when saw a <bo>, then need to add backoff weights, start from non-state words*/
	//	return states and cost
	private StatefulFFTransitionResult lookup_words1_equv_state(int[] en_words,	ArrayList<FFDPState> previous_states) {
		//long start_step1 = Support.current_time();
	 
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
					
					if (current_ngram.size() == this.ngramOrder) {
						// compute the current word probablity, and remove it
						transition_cost -= this.get_prob(current_ngram);
						
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
				if (current_ngram.size() == this.ngramOrder) {
					// compute the current word probablity, and remove it
					transition_cost -= this.get_prob(current_ngram);
					
					current_ngram.remove(0);
				}
				 
			}
		}
		//### create tabl
		StatefulFFTransitionResult res_tbl = new StatefulFFTransitionResult();
		LMFFDPState  model_states = new LMFFDPState();
		res_tbl.putStateForItem(model_states);
		
		res_tbl.putTransitionCost(transition_cost);
		//System.out.println("##tran cost: " + transition_cost +" lm_l_cost[0]: " + lm_l_cost[0]);
		
		//time_step2 += Support.current_time()-start_step2;
		return res_tbl;	
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
		current_ngram.add(START_SYM_ID);
		for (int i = 0; i < l_context.length; i++) {
			int t = l_context[i];
			current_ngram.add(t);
			
			//compute the current word probablity
			res -= this.get_prob(current_ngram);
		
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
		
		current_ngram.add(STOP_SYM_ID);
		res -= this.get_prob(current_ngram);
		return res;
	}

	

	public double estimate(Rule rule) {
		System.out.println("This function should not be called at here, must be wrong");
		System.exit(0);
		return 0;
	}

	
	private double get_prob(ArrayList<Integer> ngram_words){
		StringBuffer ngram = new StringBuffer();
		for(int i=0; i<ngram_words.size(); i++){
			ngram.append(ngram_words.get(i));
			if(i<ngram_words.size()-1)
				ngram.append(" ");
		}
		
		Double res = (Double)p_ngram_model.get(ngram.toString()); 
		if(res==null){
			System.out.println("p_ngram_model returns null for ngram: " + ngram.toString());
			System.exit(0);
		}
		return Math.log(res);
	}
	
}
