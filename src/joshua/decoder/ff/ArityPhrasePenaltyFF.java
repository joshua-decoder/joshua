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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public final class ArityPhrasePenaltyFF extends DefaultStatelessFF {
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(ArityPhrasePenaltyFF.class.getName());
	
	/** 
	 * Estimated log probability value 
	 * of this feature function when active.
	 * 
	 * The value of this field should be equal to
	 * <code>- Math.log10(Math.E)</code>.
	 * <p>
	 * This field is package-private to allow access
	 * by unit tests for this class.
	 */
	static final double ALPHA = - Math.log10(Math.E);//-0.435
	
	// when the rule.arity is in the range, then this feature is activated
	private final int minArity;
	private final int maxArity;
	
	
	public ArityPhrasePenaltyFF(final int featureID, final double weight, final int owner,	final int min, final int max) {
		super(weight, owner, featureID);
		this.minArity = min;
		this.maxArity = max;
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("ArityPhrasePenaltyFF feature with owner=" + this.owner +"; minArity=" + this.minArity+ "; maxArity="+this.maxArity);
		}
	}
	
	/* See Javadoc for FeatureFunction interface. */
	public double estimateLogP(final Rule rule, int sentID) {
		
		if (this.owner == rule.getOwner()
				&& rule.getArity() >= this.minArity 
				&& rule.getArity() <= this.maxArity) {
			return ALPHA;
		} else {
			
			return 0.0;
		}
	}

}
