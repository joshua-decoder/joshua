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
package joshua.decoder.ff.lm.distributed_lm;

import java.util.ArrayList;

/**
 * 
 * @version $LastChangedDate$
 */
public abstract class LMClient {
	
	public LMClient() {
		
	}
	
	
	public LMClient(String hostname, int port) {
		
	}
	
	
	//TODO
	public void close_client() {
		
	}
	
	
	//cmd: prob order wrd1 wrd2 ...
	public abstract double get_prob(ArrayList<Integer> ngram, int order);
	
	//cmd: prob order wrd1 wrd2 ...
	public abstract double get_prob(int[] ngram, int order);
	
	//cmd: prob order wrd1 wrd2 ...
	public abstract double get_prob_backoff_state(int[] ngram, int n_additional_bow);
   
	public abstract int[] get_left_euqi_state(int[] original_state_wrds, int order, double[] cost);
   
	public abstract int[] get_right_euqi_state(int[] original_state, int order);
	
}
