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

import java.util.logging.Logger;
import java.util.ArrayList;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.chart_parser.SourcePath;

/**
 *
 * @author Chris Dyer, <redpony@umd.edu>
 * @version $LastChangedDate: 2009-05-11 11:31:33 -0400 (Mon, 11 May 2009) $
 */
public final class SourcePathFF extends DefaultStatelessFF {
	
	public SourcePathFF(final int featureID, final double weight) {
		super(weight, -1, featureID);
	}

	public StatelessFFTransitionResult transition(Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end, SourcePath srcPath) {
		if (null != previous_states) {
			throw new IllegalArgumentException("transition: previous states for a stateless feature is NOT null");
		}
		StatelessFFTransitionResult result = new StatelessFFTransitionResult();
		result.setTransitionCost(srcPath.getPathCost());
		return result;
	}

	public double estimate(final Rule rule) {
		return 0.0;
	}

}
