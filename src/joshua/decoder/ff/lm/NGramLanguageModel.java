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

/**
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public interface NGramLanguageModel {
	
	int getOrder();
	
	// The ngram_order specified here is used to temporarily
	// reduce the order used by the model.
	double getProbability(int[] phrase);
	double getProbability(int[] phrase, int ngram_order);
	double getProbability(int[] phrase, int ngram_order,
		int start_index, int end_index);
	
	
//===============================================================
// Equivalent LM State (use DefaultLM if you don't care)
//===============================================================
	double getProbabilityOfBackoffState(
		int[] ngram_words,
		int   order,
		int   n_additional_back_off_weight);
	
	int[] getLeftEquivalentState(
		int[]    original_state_wrds,
		int      order,
		double[] cost);
	
	int[] getRightEquivalentState(
		int[]   original_state,
		int     order,
		boolean check_bad_stuff);
}
