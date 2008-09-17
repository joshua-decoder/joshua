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

import edu.jhu.joshua.decoder.Symbol;
import java.util.Map;
import java.util.HashMap;

/**
 * This state contains an arbitrary dictionary of Objects.
 * The 'final' modifier makes all methods final which can allow for
 * inlining on the rare occasions it's possible.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public final class MapFFState
extends    HashMap<Integer,Object>
implements FFState {
	private static final int
		TRANSITION_COST        = Symbol.TRANSITION_COST_SYM_ID;
	private static final int
		STATE_FOR_ITEM         = Symbol.ITEM_STATES_SYM_ID;
	private static final int
		FUTURE_COST_ESTIMATION = Symbol.BONUS_SYM_ID;
	
	
	public MapFFState() {
		super();
	}
	
	/**
	 * A constructor function to clone from a HashMap into a MapFFState. 
	 * Necessary because we can't just cast it and update the vtable.
	 * Note that this is just a preliminary hack until we exorcise all the HashMaps
	 * 
	 * @deprecated
	 */
	@Deprecated
	public MapFFState(final Map<Integer,Object> map) {
		super(map);
	}
	
	
	public double getTransitionCost() {
		final Double transition_cost = (Double)
			this.get(TRANSITION_COST);
		
		if (null == transition_cost) {
			return 0.0;
		} else {
			return transition_cost.doubleValue();
		}
	}
	
	
	public void putTransitionCost(final double transition_cost) {
		this.put(TRANSITION_COST, transition_cost);
	}
	
	
	public HashMap getStateForItem() {
		return (HashMap) this.get(STATE_FOR_ITEM);
	}
	
	
	public double getFutureCostEstimation() {
		final Double future_cost_estimation = (Double)
			this.get(FUTURE_COST_ESTIMATION);
		
		if (null == future_cost_estimation) {
			return 0.0;
		} else {
			return future_cost_estimation.doubleValue();
		}
	}
}
