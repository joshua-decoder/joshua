import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.*;
import edu.uci.ics.jung.graph.*;

import java.awt.geom.Point2D;

public class DerivationTreeTransformer implements Transformer<Node,Point2D> {
	private TreeLayout<Node,DerivationTreeEdge> treeLayout;
	private DerivationTree graph;
	private Node root;
	private Node sourceRoot;

	static int Y_DIST = 50;

	public DerivationTreeTransformer(DerivationTree t)
	{
		graph = t;
		DelegateForest del = new DelegateForest(t);
		del.setRoot(t.getRoot());
		del.setRoot(t.getSourceRoot());
		root = t.getRoot();
		sourceRoot = t.getSourceRoot();
		treeLayout = new TreeLayout(del, 125);
	}

	public Point2D transform(Node n)
	{
		double x, y;
		Point2D t = treeLayout.transform(n);
		if (n.isSource()) {
			x = treeLayout.transform(root).getX() + (t.getX() - treeLayout.transform(sourceRoot).getX());
			y = Y_DIST * (distanceToLeaf(n) + 1);
		}
		else {
			x = t.getX();
			y = Y_DIST * (-1) * distanceToLeaf(n);
		}
		return new Point2D.Double(x, y);
	}

	private int distanceToLeaf(Node n)
	{
		if (graph.getSuccessors(n).isEmpty())
			return 0;
		int result = 0;
		for (Object x : graph.getSuccessors(n)) {
			int tmp = distanceToLeaf((Node) x);
			if (tmp > result)
				result = tmp;
		}
		return 1 + result;
	}
}
