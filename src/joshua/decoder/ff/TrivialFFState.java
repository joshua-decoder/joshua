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
package joshua.decoder.ff;

import java.util.Map;

/**
 * This state contains nothing more than the transition cost. We
 * can't extend Double because it's final, but this is the same.
 * We're also final in order to improve performance, even though
 * this means others can't do the obvious thing and just extend
 * TrivialFFState to be non-trivial.
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public final class TrivialFFState
implements FFState {
	private final double transitionCost;
	
	TrivialFFState(final double transition_cost) {
		this.transitionCost = transition_cost;
	}
	
	public double getTransitionCost() {
		return this.transitionCost;
	}
	
	public Map getStateForItem() {
		return null;
	}
	
	public double getFutureCostEstimation() {
		return 0.0;
	}
}
