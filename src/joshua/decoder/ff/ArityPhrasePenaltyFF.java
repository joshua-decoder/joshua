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
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */
public final class ArityPhrasePenaltyFF extends DefaultStatelessFF {
	private static final double ALPHA = Math.log10(Math.E);
	
	// when the rule.arity is in the range, then this feature is activated
	private final int min_arity;
	private final int max_arity;
	
	
	public ArityPhrasePenaltyFF(final int feat_id_, final double weight_, final int owner_,	final int min, final int max) {
		super(weight_, owner_, feat_id_);
		this.min_arity = min;
		this.max_arity = max;
	}
	
	
	public double estimate(final Rule rule) {
		if (this.owner == rule.owner && this.min_arity <= rule.arity && this.max_arity >= rule.arity) {
			return ALPHA;
		} else {
			return 0.0;
		}
	}


}
