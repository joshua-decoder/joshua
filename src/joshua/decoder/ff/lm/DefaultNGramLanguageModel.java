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

import joshua.corpus.SymbolTable;
import joshua.decoder.JoshuaConfiguration;
import joshua.decoder.Support;

import java.util.ArrayList;

/**
 * this class implement 
 * (1) LMGrammar interface 
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-10-17 01:41:03 -0400 (星期五, 17 十月 2008) $
 */


/*All the function here returns LogP, not the cost
 * */

/*The only thing you need to extend is getNgramProbabilityHelper*/
public abstract class DefaultNGramLanguageModel implements NGramLanguageModel{
	
	protected final SymbolTable p_symbolTable;	
	protected final int  g_order;	
	protected long start_loading_time;
	
	public DefaultNGramLanguageModel(SymbolTable symbolTable, int order_){
		p_symbolTable = symbolTable;
		g_order = order_;
	}
	
	protected abstract double getNgramProbabilityHelper(int[] ngram_words, int order);
	
	
	public final int getOrder(){
		return g_order;
	}
	
	//return LogP of the whole sentence for a given order n-gram LM
	//start_index: the index of first event-word we want to get its probability; if we want to get the prob for the whole sentence, then start_index should be 1
	public final double getSentenceProbability(ArrayList<Integer> words, int order, int  start_index){ //1-indexed
		double res = 0.0;
		if (null == words || words.size() <= 0) {
			return res;
		}
		
		int[] ngram_words;
		//extra partial-ngrams at the begining
		for (int j = start_index; j < order && j <= words.size(); j++) {
			//TODO: start_index dependents on the order, e.g., g_order-1 (in srilm, for 3-gram lm, start_index=2. othercase, need to check)
			ngram_words = Support.sub_int_array(words, 0, j);
			res += getNgramProbability(ngram_words, order);
		}
		//regular order-ngram
		for (int i = 0; i <= words.size()-order; i++) {
			ngram_words = Support.sub_int_array(words, i, i + order);
			res += getNgramProbability(ngram_words, order);
		}
		return res;
	}
	
		
	//Note: it seems the List or ArrayList is much slower than the int array, e.g., from 11 to 9 seconds
	//so try to avoid call this function
	public final double getNgramProbability(ArrayList<Integer> ngram_words, int order) {
		return getNgramProbability(Support.sub_int_array(ngram_words, 0, ngram_words.size()),	order);
	}
	
	
	public final double getNgramProbability(int[]   ngram_words,	int order) {
		if (ngram_words.length > order) {
			System.out.println("ngram length is greather than the max order");
			System.exit(1);
		}
		int hist_size = ngram_words.length - 1;
		if (hist_size >= order || hist_size < 0) {
			System.out.println("Error: hist size is " + hist_size);
			return 0;//TODO: zero cost?
		}
		double res = getNgramProbabilityHelper(ngram_words, order);
		if (res < -JoshuaConfiguration.lm_ceiling_cost) {
			res = -JoshuaConfiguration.lm_ceiling_cost;
		}	
		return res;
	}
	

	//	 called by LMModel to calculate additional bow for backoff Symbol.BACKOFF_LEFT_LM_STATE_SYM_ID
	//must be: ngram_words.length <= order	
	public final double getProbabilityOfBackoffState(ArrayList<Integer> ngram_words, int order,	int  n_additional_bow) {
		return getProbabilityOfBackoffState( Support.sub_int_array(ngram_words, 0, ngram_words.size()), order, n_additional_bow);
	}
	
	
	public final double getProbabilityOfBackoffState(int[] ngram_words,	int order, int n_additional_bow) {
		if (ngram_words.length > order) {
			System.out.println("ngram length is greather than the max order");
			System.exit(1);
		}
		if (ngram_words[ngram_words.length-1] != LanguageModelFF.BACKOFF_LEFT_LM_STATE_SYM_ID) {
			System.out.println("last wrd is not <bow>");
			System.exit(1);
		}
		if (n_additional_bow > 0) {
			return getProbabilityOfBackoffStateHelper(ngram_words,	order, n_additional_bow);
		} else {
			return 0.0;
		}
	}
	
	
	//default implementation
	protected double getProbabilityOfBackoffStateHelper(int[] ngram_wrds, int order, int n_additional_bow){
		System.out.println("Error: call getProbabilityOfBackoffStateHelper,  but the function is not overwritten by a concret class, must exit");
		System.exit(1);
		return 0;
	}
	
	
	//default implementation
	public int[] getLeftEquivalentState(int[] original_state_wrds, int order, double[] cost){
		if(JoshuaConfiguration.use_left_equivalent_state==false){
			return original_state_wrds;
		}else{
			System.out.println("Error: call getLeftEquivalentState, but the function is not overwritten by a concret class, must exit");
			System.exit(1);
			return null;
		}
	}
	
	//default implementation
	public int[] getRightEquivalentState(int[] original_state, int order){
		if(JoshuaConfiguration.use_right_equivalent_state==false || original_state.length!=g_order-1){
			return original_state;
		}else{		
			System.out.println("Error: call getRightEquivalentState, but the function is not overwritten by a concret class, must exit");
			System.exit(1);
			return null;
		}
	}

}
