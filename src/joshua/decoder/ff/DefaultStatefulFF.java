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

import joshua.decoder.hypergraph.HyperEdge;



/**
 * 
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public abstract class DefaultStatefulFF implements FeatureFunction {
	
	/** any integers exept -1*/
	private int stateID;
	
	private double weight = 0.0;
	private int    featureID; //the unique integer that identifies a feature
	
	public DefaultStatefulFF(int stateID, double weight, int featureID) {
		this.weight    = weight;
		this.featureID = featureID;
		this.stateID = stateID;
	}
	
	public boolean isStateful() {
		return true;
	}

	public final double getWeight() {
		return this.weight;
	}
	
	public final void setWeight(final double weight) {
		this.weight = weight;
	}
	
	public final int getFeatureID() {
		return this.featureID;
	}
	
	public final void setFeatureID(final int id) {
		this.featureID = id;
	}
	
	public final int getStateID() {
		return this.stateID;
	}
	
	public final void setStateID(final int id) {
		this.stateID = id;
	}
	
	public double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return transitionLogP(edge.getRule(), edge.getAntNodes(), spanStart, spanEnd, edge.getSourcePath(), sentID );
	}
	
	public double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID){
		return  finalTransitionLogP(edge.getAntNodes().get(0), spanStart, spanEnd, edge.getSourcePath(), sentID );
	}
}
