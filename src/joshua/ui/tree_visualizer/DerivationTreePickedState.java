package joshua.ui.tree_visualizer;

import edu.uci.ics.jung.visualization.picking.MultiPickedState;

public class DerivationTreePickedState extends MultiPickedState<Node> {
	private DerivationTree tree;
	
	public DerivationTreePickedState(DerivationTree g)
	{
		super();
		this.tree = g;
	}
	
	public boolean pick(Node n, boolean state)
	{
		if (tree.picked != null) {
			tree.setSubtreeHighlight(tree.picked, false);
		}
		tree.picked = n;
		tree.setSubtreeHighlight(tree.picked, true);
		return true;
	}

}
