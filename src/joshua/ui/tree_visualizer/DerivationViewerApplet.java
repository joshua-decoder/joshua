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
package joshua.ui.tree_visualizer;

import java.awt.Color;

import javax.swing.JApplet;

/**
 * An applet for viewing DerivationTrees. It consists of a DerivationViewer inside
 * of the applet's Panel.
 * 
 * @author Jonathan Weese
 *
 */
public class DerivationViewerApplet extends JApplet {
	/**
	 * Initializes the applet by getting the source sentence and the tree representation from
	 * the applet tag in a web page.
	 */
	public void init() {
		String source = getParameter("sourceSentence");
		String derivation = getParameter("derivationTree");
		
		add(new DerivationViewer(new DerivationTree(derivation, source), getSize(), Color.red, DerivationViewer.AnchorType.ANCHOR_ROOT));
		return;
	}
}
