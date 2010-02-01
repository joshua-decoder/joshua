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
 * @version $LastChangedDate$
 */
public final class ArityPhrasePenaltyFF extends DefaultStatelessFF {
	
	static final double ALPHA = - Math.log10(Math.E);//-0.435
	
	// when the rule.arity is in the range, then this feature is activated
	private final int minArity;
	private final int maxArity;
	
	
	public ArityPhrasePenaltyFF(final int featureID, final double weight, final int owner,	final int min, final int max) {
		super(weight, owner, featureID);
		this.minArity = min;
		this.maxArity = max;
		System.out.println("ArityPhrasePenaltyFF feature with owner=" + this.owner +"; minArity=" + this.minArity+ "; maxArity="+this.maxArity);
	}
	
	
	public double estimateLogP(final Rule rule, int sentID) {
		
		if (this.owner == rule.getOwner()
			&& rule.getArity() >= this.minArity 
			&& rule.getArity() <= this.maxArity) {
			//System.out.println("y");
			//System.out.println(rule.getOwner() + "; " + rule.getArity() );
			//System.out.println("ArityPhrasePenaltyFF feature with owner=" + this.owner +"; minArity=" + this.minArity+ "; maxArity="+this.maxArity);
			return ALPHA;
		} else {
			
			return 0.0;
		}
	}

}
