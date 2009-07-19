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
package joshua.ui.tree_visualizer.browser;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

class SynchronizedPairListListener implements ListSelectionListener {
	private JList first;
	private JList second;
	
	public SynchronizedPairListListener(JList listOne, JList listTwo)
	{
		first = listOne;
		second = listTwo;
		
		first.getSelectionModel().addListSelectionListener(this);
		second.getSelectionModel().addListSelectionListener(this);
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(second.getSelectionModel())) {
			if (first.getSelectedIndex() != second.getSelectedIndex()) {
				first.setSelectedIndex(second.getSelectedIndex());
			}
			return;
		}
		else {
			if (second.getSelectedIndex() != first.getSelectedIndex()) {
				second.setSelectedIndex(first.getSelectedIndex());
			}
			Browser.setCurrentSourceIndex(first.getSelectedIndex());
			for (DerivationTreeFrame dtf : Browser.activeFrame)
				dtf.drawGraph();
		}
		return;
	}
}
