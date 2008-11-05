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
public final class SourceLatticeArcCostFF extends FeatureFunction {

	public SourceLatticeArcCostFF(final int feat_id_, final double weight_) {
		super(weight_, feat_id_, 1);
	}

	public double transition(Rule rule,
		Context prev_state1,
		Context prev_state2,
		int span_start,
		int span_end,
		TransitionCosts out_costs,
		Context res_state) {
		res_state.insertByte(this.getStateOffset(), 0);
		return super.transition(rule, null, null, span_start, span_end, out_costs, null);
	}
}
