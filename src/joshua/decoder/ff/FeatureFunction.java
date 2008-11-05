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
package joshua.decoder.ff;

import joshua.decoder.ff.tm.Rule;

/**
 * This interface provide ways to calculate cost based on rule and
 * state information
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */

/*How to add a featureFunction
 * implement FeatureFunction by overriding estimate, transition, and finalTransition
 * */

public abstract class FeatureFunction {

	public static class TransitionCosts {
		public double transition_cost;
		public double estimated_future_cost;
	}

	/* Attributes */
	private double weight_;
	protected final int feat_id;
	private final int state_bytes;
	protected int state_offset;

	// by default, stateless
	public FeatureFunction(double weight, int feat_id) {
		this.weight_ = weight;
		this.feat_id = feat_id;
		state_bytes = 0;
	}
	public FeatureFunction(double weight, int feat_id, int num_bytes) {
		this.weight_ = weight;
		this.feat_id = feat_id;
		state_bytes = num_bytes;
	}
	public final int getStateOffset() {
		return state_offset;
	}
	public final int getStateEndOffset() {
		return state_offset + state_bytes;
	}
	public final int getNumStateBytes() {
		return state_bytes;
	}
	public void setOffset(int offset) {
		state_offset = offset;
	}

	public final int getFeatureID() {
		return feat_id;
	}
	
	public final void    putWeight(double weight) {
		weight_ = weight;
	}
	
	public final double  getWeight() {
		return weight_;
	}
	
	public final boolean isStateful() {
		return state_bytes > 0;
	}
	
	
	/* Methods */
	/** Only used when initializing Translation Grammars (for pruning purpose, and to get stateless cost for each rule) */
	public double estimate(Rule rule) {
		return 0.0;
	}

	/* Functions:
	 * (1) calculate transition cost
	 * (2) estimate future cost (if out_costs != null)
	 * (3) extract dynamical programming state
	 * @returns transition cost
	 **/
	public double transition(Rule rule,
		Context prev_state1,
		Context prev_state2,
		int span_start,
		int span_end,
		TransitionCosts out_costs,
		Context res_state) {
		double est = this.estimate(rule);
		if (out_costs != null) {
			out_costs.transition_cost = est;
			out_costs.estimated_future_cost = 0.0;
		}
		return est;
	}

	public double finalTransition(Context state) {
		return 0.0;
	}
}
