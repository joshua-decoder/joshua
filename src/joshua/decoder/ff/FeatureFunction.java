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

import java.util.List;

import joshua.decoder.chart_parser.SourcePath;
import joshua.decoder.ff.state_maintenance.DPState;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;


/**
 * This interface provide ways to calculate logP based on rule and
 * state information. In order to implement a new feature function
 * you must (1) implement this FeatureFunction interface, and (2)
 * implement the FFTransitionResult and FFDPState interfaces. BUG:
 * the distinction between those latter two interfaces is unclear.
 *
 * @author wren ng thornton <wren@users.sourceforge.net>
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public interface FeatureFunction {

//===============================================================
// Attributes
//===============================================================
	
	/** It is essential to make sure the feature ID is unique
	 * for each feature. */
	void    setFeatureID(int id);
	int     getFeatureID();
	
	void    setWeight(double weight);
	double  getWeight();
	
	boolean isStateful();
	
	void    setStateID(int stateID);
	int     getStateID();
	
//===============================================================
// Methods
//===============================================================
	/**sentID might be useful for sentence-specific features (e.g., oralce model)
	 * */
	
	/**
	 * It is used when initializing translation grammars (for
	 * pruning purpose, and to get stateless logP for each rule).
	 * This is also required to sort the rules (required by Cube-pruning).
	 */
	double estimateLogP(Rule rule, int sentID);
	
		
	/**estimate future logP, e.g., the logPs of partial n-grams
	 * asscociated with the left-edge ngram state
	 * */
	double estimateFutureLogP(Rule rule, DPState curDPState, int sentID);	
	
	double transitionLogP(Rule rule, List<HGNode> antNodes, int spanStart, int spanEnd, SourcePath srcPath, int sentID);
	
	double transitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID);
	
	/**Edges calling finalTransition do not have concret rules associated with them. 
	 * */
	double finalTransitionLogP(HGNode antNode, int spanStart, int spanEnd, SourcePath srcPath, int sentID);
	
	double finalTransitionLogP(HyperEdge edge, int spanStart, int spanEnd, int sentID);
	
}
