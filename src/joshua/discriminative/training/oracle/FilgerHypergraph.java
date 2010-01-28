package joshua.discriminative.training.oracle;

import java.util.ArrayList;
import java.util.HashMap;

import joshua.decoder.ff.tm.Rule;
import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;


/**Given a hypergraph and a string, generate a new hypergraph
 * that contains only derivations that lead to the string*/
public class FilgerHypergraph {
	
	HashMap<HGNode, Boolean> processedNodesTbl = new HashMap<HGNode, Boolean>();
	
	HashMap<HGNode, Boolean> onNodesTbl = new HashMap<HGNode, Boolean>();
	HashMap<HyperEdge, Boolean> onEdgesTbl = new HashMap<HyperEdge, Boolean>();
	
	HashMap<HGNode, ArrayList<Span>> matchSpansTbl = new HashMap<HGNode, ArrayList<Span>>();
	
	
	/**for each unique substring in the sentence,
	 * associate it with a list of possible spans in the sentence*/
	HashMap<String, ArrayList<Span>> senteceSubstringsTbl = new HashMap<String, ArrayList<Span>>();
	
	protected  void operationHypergraph(HyperGraph hg){		
	
		processedNodesTbl.clear();
		operationNode(hg.goalNode);
	}
	
	
	private boolean operationNode(HGNode it){
		
		if(processedNodesTbl.containsKey(it)){
			return onNodesTbl.containsKey(it) ? true : false;
		}
		processedNodesTbl.put(it, true);
		
		boolean onNode = false;
		
		//=== recursive call on each hyperedge
		for(HyperEdge dt : it.hyperedges){
			boolean onEdge = operationHyperedge(dt);
			
			if(onEdge == true)//a node is on if any of his incoming edge is on
				onNode = true;
		}
		
		//=== node-specific operation
		if(onNode)
			onNodesTbl.put(it, true);
		
		return onNode;
	}
	
	
	private boolean operationHyperedge(HyperEdge dt){
			
		//=== recursive call on each ant node
		if(dt.getAntNodes() != null){
			for(HGNode ant_it : dt.getAntNodes()){
				if( operationNode(ant_it) )//if any ant node is off, then the edge is off
					return false;
			}
		}
		
		
		//=== hyperedge-specific operation				
		if ( matchTargetString(dt) == false )
			return false;
		
		//== survive
		onEdgesTbl.put(dt, true);
		return true;		
	}
	
	
	
	
	/**assuming all the ant nodes are on, 
	 * now we see if the application of the rule matches a larger span 
	 * */
	private boolean matchTargetString(HyperEdge dt){
		
		Rule rl = dt.getRule();
		if(rl!=null){
			
			return false;
		}
		
		//== survive
		return true;
	}
	
	
	
	//========== Span class ==========
	private class Span{
		int startPos;
		int endPos;
	}

}
