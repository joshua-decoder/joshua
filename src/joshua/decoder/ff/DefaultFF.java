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
package edu.jhu.joshua.decoder.feature_function;
import  edu.jhu.joshua.decoder.feature_function.MapFFState;
import  edu.jhu.joshua.decoder.feature_function.FeatureFunction;

import edu.jhu.joshua.decoder.feature_function.translation_model.Rule;
import java.util.ArrayList;


/**
 * This class provides the "Model" version of FeatureFunction from
 * before the reorganization. The final methods are to enable
 * inlining whenever it's possible.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public abstract class DefaultFF
implements FeatureFunction<MapFFState> {
	private   boolean stateful = true;
	protected double  weight   = 0.0;
	
	public DefaultFF(double weight_) {
		this.weight = weight_;
	}
	
	public final void setStateless() {
		this.stateful = false;
	}
	
	public final boolean isStateful() {
		return this.stateful;
	}
	
	public final double getWeight() {
		return this.weight;
	}
	
	public void putWeight(final double weight_) {
		this.weight = weight_;
	}
	
	
	/**
	 * Generic estimator for FeatureFunctions which are Stateless
	 */
	public double estimate(final Rule rule) {
		if (this.stateful) {
			// TODO: Throw exception?
			return 0.0;
		} else {
			final FFState state = this.transition(rule, null, -1, -1);
			if (null == state) {
				return 0.0;
			} else {
				return state.getTransitionCost();
			}
		}
	}
	
	
	//depends on finate state informations
	//return transition_cost, and state
	//antstates: states of this model in ant items
	//NOTE: for model that is non-stateless/non-contexual, it must return a tbl with Symbol.TRANSITION_COST and Symbol.ITEM_STATES
	//only used by chart.nbest_extract, chart.compute_item,  chart.transition_final,  chart.prepare_rulebin
	
	//// TODO: use java.utils.Collections.unmodifiableMap(Map map) to ensure sanity
	/**
	 * Generic transition function assuming our estimator is correct
	 */
	public MapFFState transition(
		final Rule rule,
		final ArrayList<MapFFState> previous_states,
		final int i,
		final int j
	) {
		MapFFState state = new MapFFState();
		state.putTransitionCost(this.estimate(rule));
		return state;
	}
	
	
	public double finalTransition(final MapFFState state) {
		return 0.0;
	}
}
