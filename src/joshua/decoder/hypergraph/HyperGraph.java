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
package joshua.decoder.hypergraph;

import joshua.decoder.ff.FFDPState;
import joshua.decoder.ff.FFTransitionResult;
import joshua.decoder.ff.FeatureFunction;
import joshua.decoder.ff.tm.Rule;
import joshua.decoder.chart_parser.SourcePath;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * this class implement 
 * (1) HyperGraph-related data structures (Item and Hyper-edges)
 *
 * Note: to seed the kbest extraction, each deduction should have
 * the best_cost properly set. We do not require any list being
 * sorted
 *
 * @author Zhifei Li, <zhifei.work@gmail.com>
 * @version $LastChangedDate$
 */
public class HyperGraph {

	// pointer to goal HGNode
	public HGNode goalNode = null;
	
	public int numNodes = -1;
	public int numEdges = -1;
	public int sentID = -1;
	public int sentLen = -1;
	
	static final Logger logger = Logger.getLogger(HyperGraph.class.getName());
	
	public HyperGraph(HGNode g_item, int n_items, int n_deducts, int s_id, int s_len){
		goalNode = g_item;
		numNodes = n_items;
		numEdges = n_deducts;
		sentID = s_id;
		sentLen = s_len;
	}
	
	
	
	static public FFTransitionResult computeTransition(HyperEdge dt, FeatureFunction m, int startSpan, int endSpan){
		return computeTransition(dt, dt.getRule(), dt.getAntNodes(), m,  startSpan,  endSpan, dt.getSourcePath());
	}
	
	static public FFTransitionResult computeTransition(HyperEdge dt, Rule rl,  ArrayList<HGNode> antNodes, FeatureFunction m, int startSpan, int endSpan, SourcePath srcPath){
		FFTransitionResult res = null;
		if(m.isStateful() == false){//stateless feature
			res = m.transition(dt, rl, null, startSpan, endSpan, srcPath);
		}else{
			ArrayList<FFDPState> previousStates = null;
			if (antNodes != null) {
				previousStates = new ArrayList<FFDPState>();
				for (HGNode it : antNodes) {
					previousStates.add( it.getFeatDPState(m) );
				}
			}
			res = m.transition(dt, rl, previousStates, startSpan, endSpan, srcPath);
		}
		if (null == res) {
			logger.severe("compute_item: transition returned null state");
			System.exit(0);
		}
		return res;
	} 
	
	static public double computeFinalTransition(HyperEdge dt, FeatureFunction m){
		double res = 0;
		if(m.isStateful() == false){//stateless feature
			res = m.finalTransition(dt, null);
		}else{//stateful feature
			res = m.finalTransition(dt, dt.getAntNodes().get(0).getFeatDPState(m));
		}
		return res;
	} 
	
	
	
	//####### template to explore hypergraph #########################
	/*
	private HashSet<HGNode> processHGNodesTbl =  new HashSet<HGNode>();
	
	private void operationHG(HyperGraph hg){		
		processHGNodesTbl.clear(); 
		operationNode(hg.goalNode);
	}
	
	private void operationNode(HGNode it){
		if(processHGNodesTbl.contains(it))
			return;
		processHGNodesTbl.add(it);
		
		//=== recursive call on each edge
		for(HyperEdge dt : it.hyperedges){
			operationEdge(dt);
		}
		
		//=== node-specific operation
	}
	
	private void operationEdge(HyperEdge dt){
		
		//=== recursive call on each ant node
		if(dt.getAntNodes()!=null)
			for(HGNode ant_it : dt.getAntNodes())
				operationNode(ant_it);
		
		//=== edge-specific operation				
		Rule rl = dt.getRule();
		if(rl!=null){
		
		}
	}
	*/
	//############ end ##############################
}
