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
import joshua.decoder.hypergraph.HyperEdge;


/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate: 2008-07-28 18:44:45 -0400 (Mon, 28 Jul 2008) $
 */

public abstract class DefaultStatefulFF implements FeatureFunction {
	private double weight = 0.0;
	private int    feat_id; //the unique integer that identifies a feature
	
	public DefaultStatefulFF(double weight_, int id_) {
		this.weight = weight_;
		this.feat_id = id_;
	}
	
	public boolean isStateful() {
		return true;
	}

	public final double getWeight() {
		return this.weight;
	}
	
	public final void putWeight(final double weight_) {
		this.weight = weight_;
	}
	
	public final int getFeatureID() {
		return this.feat_id;
	}
	
	public final void putFeatureID(final int id_) {
		this.feat_id = id_;
	}
	
	/*default behavior: ignore "edge"*/
	public FFTransitionResult transition(HyperEdge edge, Rule rule, ArrayList<FFDPState> previous_states, int span_start, int span_end){
		return transition(rule, previous_states, span_start,span_end);
	}
	
	/*default behavior: ignore "edge"*/
	public double finalTransition(HyperEdge edge, FFDPState state){
		return finalTransition(state);
	}
}
