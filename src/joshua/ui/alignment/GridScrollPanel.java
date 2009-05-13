/*/* This file is part of the Joshua Machine Translation System.
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
package joshua.ui.alignment;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import joshua.ui.Orientation;
import joshua.ui.alignment.GridPanel;

/**
 * 
 * @author Lane Schwartz
 */
public class GridScrollPanel extends JPanel {
	
	/** Header containing target language words. */
	private final GridScrollPanelHeader columnHeader;

	/** Header containing source language words. */
	private final GridScrollPanelHeader rowHeader;

	/**
	 * Constructs a scrollable panel wrapping an alignment grid.
	 * 
	 * @param gridPanel An alignment grid
	 */
	public GridScrollPanel(GridPanel gridPanel) {
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

		int headerCellBreadth = gridPanel.getScaleFactor();
		int headerCellDepth = headerCellBreadth * 3;
		
		String[] sourceWords = gridPanel.getSourceWords();
		String[] targetWords = gridPanel.getTargetWords();

		this.columnHeader = new GridScrollPanelHeader(targetWords, Orientation.HORIZONTAL, headerCellBreadth, headerCellDepth);
		this.rowHeader = new GridScrollPanelHeader(sourceWords, Orientation.VERTICAL, headerCellBreadth, headerCellDepth);
		
		
		JScrollPane pictureScrollPane = new JScrollPane(gridPanel);
		pictureScrollPane.setPreferredSize(new Dimension(300, 250));
		
		pictureScrollPane.setColumnHeaderView(columnHeader);
		pictureScrollPane.setRowHeaderView(rowHeader);

		this.add(pictureScrollPane);
		this.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
	}

}
