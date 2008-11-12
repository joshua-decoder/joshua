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
import joshua.decoder.hypergraph.HyperEdge;

import java.util.ArrayList; //// BUG: should be List but that causes bugs


/**
 * This interface provide ways to calculate cost based on rule and
 * state information.
 *
 * 
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */

/*How to add a featureFunction
 * (1) implement FeatureFunction
 * (2) implement FFState
 * */

public interface FeatureFunction {

	/* Attributes */
	
	/* It is essential to make sure the feature ID is unique for each feature
	 */
	void    putFeatureID(int id);
	
	int     getFeatureID();
	
	void    putWeight(double weight);
	
	double  getWeight();
	
	boolean isStateful();
	
	
	/* Methods */
	/** Only used when initializing Translation Grammars (for pruning purpose, and to get stateless cost for each rule) */
	double estimate(Rule rule);
	
	
	/* Functions:
	 * (1) calculate transition cost
	 * (2) estimate future cost
	 * (3) extract dynamical programming state
	 * */
	FFTransitionResult transition(Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end);
	
	/*In general, it is quite possible that the edge is not created yet when this function is called. In this case, simply pass a null pointer*/
	FFTransitionResult transition(HyperEdge edge, Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end);
	
	
	double finalTransition(FFDPState state);
	/*In general, it is quite possible that the edge is not created yet when this function is called. In this case, simply pass a null pointer*/
	double finalTransition(HyperEdge edge, FFDPState state);
}
