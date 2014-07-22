package joshua.ui.tree_visualizer;

import java.awt.Color;

import javax.swing.JApplet;

import joshua.ui.tree_visualizer.tree.Tree;

/**
 * An applet for viewing DerivationTrees. It consists of a DerivationViewer inside of the applet's
 * Panel.
 * 
 * @author Jonathan Weese
 * 
 */
@SuppressWarnings("serial")
public class DerivationViewerApplet extends JApplet {
  /**
   * Initializes the applet by getting the source sentence and the tree representation from the
   * applet tag in a web page.
   */
  public void init() {
    String source = getParameter("sourceSentence");
    String derivation = getParameter("derivationTree");
		Tree tree = new Tree(derivation);

    add(new DerivationViewer(new DerivationTree(tree, source),
					                   getSize(),
														 Color.red,
														 DerivationViewer.AnchorType.ANCHOR_ROOT));
    return;
  }
}
