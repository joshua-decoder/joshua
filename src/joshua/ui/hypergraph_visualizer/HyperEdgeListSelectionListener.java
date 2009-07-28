package joshua.ui.hypergraph_visualizer;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;

public class HyperEdgeListSelectionListener implements ListSelectionListener {
	private HyperGraphViewer viewer;
	
	public HyperEdgeListSelectionListener(HyperGraphViewer vv)
	{
		this.viewer = vv;
	}
	
	public void valueChanged(ListSelectionEvent e) {
		JList list = (JList) e.getSource();
		viewer.graph.replaceSubtrees(list.getSelectedValues());
		viewer.setGraphLayout(new StaticLayout<Vertex,Edge>(viewer.graph, new HyperGraphTransformer(viewer.graph)));
		viewer.graph.addNonTreeEdges();
		return;
	}

}
