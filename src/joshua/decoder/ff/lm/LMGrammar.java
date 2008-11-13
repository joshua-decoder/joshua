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

import java.util.ArrayList;

import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;
import joshua.decoder.Symbol;

/**
 * this class implement 
 * (1) LMGrammar interface 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-17 01:41:03 -0400 (星期五, 17 十月 2008) $
 */


/*All the function here returns LogP, not the cost
 * */
public abstract class LMGrammar {
	
	static String BACKOFF_LEFT_LM_STATE_SYM="<lzfbo>";
	public int BACKOFF_LEFT_LM_STATE_SYM_ID;//used for equivelant state
	static String NULL_RIGHT_LM_STATE_SYM="<lzfrnull>";
	public int NULL_RIGHT_LM_STATE_SYM_ID;//used for equivelant state
	 
	
	protected  final Symbol p_symbol;
	
	protected final int  g_order;
	
	protected long start_loading_time;
	
	public LMGrammar(Symbol symbol_, int order_){
		p_symbol = symbol_;
		g_order = order_;
		this.BACKOFF_LEFT_LM_STATE_SYM_ID = p_symbol.addTerminalSymbol(BACKOFF_LEFT_LM_STATE_SYM);
		this.NULL_RIGHT_LM_STATE_SYM_ID = p_symbol.addTerminalSymbol(NULL_RIGHT_LM_STATE_SYM);
	}
	
	
	public void end_lm_grammar() {
		//do nothing
	}
	
	
	public abstract void read_lm_grammar_from_file(String grammar_file);
	
	
	public abstract void write_vocab_map_srilm(String fname);
	
	
	//return LogP
	public final double score_a_sent(ArrayList<Integer> words_in, int order, int  start_index){ //1-indexed
		return score_a_sent(Support.sub_int_array(words_in, 0, words_in.size()),	order,	start_index);
	}
	
	
	private final double score_a_sent(int[] words, int order, int start_index) {//1-indexed
		//long start = Support.current_time();
		double res = 0.0;
		if (null == words
		|| words.length <= 0) {
			return res;
		}
		
		int[] ngram_words;
		//extra partial-ngrams at the begining
		for (int j = start_index; j < order && j <= words.length; j++) {
			//TODO: start_index dependents on the order, e.g., g_order-1 (in srilm, for 3-gram lm, start_index=2. othercase, need to check)
			ngram_words = Support.sub_int_array(words, 0, j);
			res += get_prob(ngram_words, order, true);
		}
		//regular order-ngram
		for (int i = 0; i <= words.length-order; i++) {
			ngram_words = Support.sub_int_array(words, i, i + order);
			res += get_prob(ngram_words, order, true);
		}
		//Chart.g_time_score_sent += Support.current_time()-start;
		return res;
	}
	
	
	//Note: it seems the List or ArrayList is much slower than the int array, e.g., from 11 to 9 seconds
	//so try to avoid call this function
	public final double get_prob(ArrayList<Integer> ngram_words, int order,	boolean check_bad_stuff) {
		return get_prob(Support.sub_int_array(ngram_words, 0, ngram_words.size()),	order,	check_bad_stuff);
	}
	
	
	public final double get_prob(int[]   ngram_words,	int     order,		boolean check_bad_stuff) {
		if (ngram_words.length > order) {
			System.out.println("ngram length is greather than the max order");
			System.exit(1);
		}
		int hist_size = ngram_words.length - 1;
		if (hist_size >= order
		|| hist_size < 0) {
			System.out.println("Error: hist size is " + hist_size);
			return 0;//TODO: zero cost?
		}
		double res = get_prob_specific(ngram_words, order, check_bad_stuff);
		if (res < -JoshuaConfiguration.lm_ceiling_cost) {
			res = -JoshuaConfiguration.lm_ceiling_cost;
		}
			
		//System.out.println("Prob: "+ Symbol.get_string(ngram_words) + "; " + res);
		
		return res;
	}
	
	
	protected abstract double get_prob_specific(int[]   ngram_words, int     order, boolean check_bad_stuff);
	
	
	// called by LMModel to calculate additional bow for backoff Symbol.BACKOFF_LEFT_LM_STATE_SYM_ID
	//must be: ngram_words.length <= order
	//	 called by LMModel to calculate additional bow for backoff Symbol.BACKOFF_LEFT_LM_STATE_SYM_ID
	//must be: ngram_words.length <= order
	
	public final double get_prob_backoff_state(
		ArrayList<Integer> ngram_words,
		int       order,
		int       n_additional_bow
	) {
		return get_prob_backoff_state(
			Support.sub_int_array(ngram_words, 0, ngram_words.size()),
			order,
			n_additional_bow);
	}
	
	
	public final double get_prob_backoff_state(
		int[] ngram_words,
		int   order,
		int   n_additional_bow
	) {
		if (ngram_words.length > order) {
			System.out.println("ngram length is greather than the max order");
			System.exit(1);
		}
		if (ngram_words[ngram_words.length-1] != BACKOFF_LEFT_LM_STATE_SYM_ID) {
			System.out.println("last wrd is not <bow>");
			System.exit(1);
		}
		if (n_additional_bow > 0) {
			return get_prob_backoff_state_specific(
				ngram_words,
				order,
				n_additional_bow);
		} else {
			return 0.0;
		}
	}
	
	
	protected abstract double get_prob_backoff_state_specific(
		int[] ngram_words,
		int   order,
		int   n_additional_bow);
	
	
	public abstract int[] get_left_equi_state(
		int[]    original_state_wrds,
		int      order,
		double[] cost);
	
	
	//idea: from right to left, if a span does not have a backoff weight, which means all ngram having this span will backoff, and we can safely remove this state
	//the absence of backoff weight for low-order ngram implies the absence of higher-order ngram
	//the absence of backoff weight for low-order ngram implies the absence of backoff weight for high order ngram
	public abstract int[] get_right_equi_state(
		int[]   original_state,
		int     order,
		boolean check_bad_stuff);
	
	
	public final int[] replace_with_unk(int[] in) {
		int[] res = new int[in.length];
		for (int i = 0; i < in.length; i++) {
			res[i] = replace_with_unk(in[i]);
		}
		return res;
	}
	
	
	protected abstract int replace_with_unk(int in);

}
