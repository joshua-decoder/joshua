package joshua.ui.hypergraph_visualizer;

import javax.swing.DefaultListModel;

import joshua.decoder.hypergraph.HyperEdge;
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
		if (!(v instanceof NodeVertex))
			return false;
		NodeVertex node =  (NodeVertex) v; //(NodeVertex) viewer.graph.getPredecessors(v).toArray()[0];
		if (viewer.graph.picked != null)
			viewer.graph.setSubTreeColor(viewer.graph.picked, 0);
		viewer.graph.picked = node;
	//	viewer.graph.incrementHyperEdge(node);
	//	viewer.setGraphLayout(new StaticLayout<Vertex,Edge>(viewer.graph, new HyperGraphTransformer(viewer.graph)));
		DefaultListModel edgeListModel = (DefaultListModel) viewer.edgeList.getModel();
		edgeListModel.removeAllElements();
		for (HyperEdge e : node.getNode().hyperedges) {
			edgeListModel.addElement(e);
		}
		if (node.getNode().hyperedges.size() == 0)
			return true;
		HyperEdgeVertex currentEdge = (HyperEdgeVertex) viewer.graph.getSuccessors(v).toArray()[0];
		viewer.edgeList.setSelectedValue(currentEdge.getHyperEdge(), true);
		v.setColor(1);
		return true;
	}
}
