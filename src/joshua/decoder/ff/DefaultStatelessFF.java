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

import java.util.ArrayList;

import joshua.decoder.ff.tm.Rule;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */

public abstract class DefaultStatelessFF implements FeatureFunction<StatelessFFTransitionResult, FFDPState> {
	private   double  weight   = 0.0;	
	protected final int owner;
	
	public DefaultStatelessFF(final double weight_, final int owner_) {
		this.weight = weight_;
		this.owner = owner_;
	}
	
	public final boolean isStateful() {
		return false;
	}

	public final double getWeight() {
		return this.weight;
	}
	
	public final void putWeight(final double weight_) {
		this.weight = weight_;
	}

	
	/**
	 * Generic transition for FeatureFunctions which are Stateless
	 * (1) use estimate() to get transition cost
	 * (2) no future cost estimation
	 */
	public StatelessFFTransitionResult transition(Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end) {
		if(previous_states!=null){
			System.out.println("transition: previous states for a stateless feature is NOT null");
			System.exit(0);
		}
		StatelessFFTransitionResult res = new StatelessFFTransitionResult();
		res.putTransitionCost(this.estimate(rule));
		return res;
	}
	
	/* default implementation of finalTransition
	 * */
	public double finalTransition(FFDPState state){
		if(state!=null){
			System.out.println("finalTransition: state for a stateless feature is NOT null");
			System.exit(0);
		}
		return 0.0;
	}
}
