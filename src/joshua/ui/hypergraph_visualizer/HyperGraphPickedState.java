package joshua.ui.hypergraph_visualizer;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.picking.MultiPickedState;

public class HyperGraphPickedState extends MultiPickedState<Vertex> {
	private HyperGraphViewer viewer;
	
	public HyperGraphPickedState(HyperGraphViewer vv) {
		super();
		this.viewer = vv;
	}
	
	public boolean pick(Vertex v, boolean state)
	{
		if (!(v instanceof HyperEdgeVertex))
			return false;
		NodeVertex node = (NodeVertex) viewer.graph.getPredecessors(v).toArray()[0];
		viewer.graph.incrementHyperEdge(node);
		viewer.setGraphLayout(new StaticLayout<Vertex,Edge>(viewer.graph, new HyperGraphTransformer(viewer.graph)));
		return true;
	}
}
