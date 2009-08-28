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
package joshua.ui.alignment;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import joshua.ui.Orientation;
import joshua.ui.alignment.GridPanel;

/**
 * 
 * @author Lane Schwartz
 */
public class GridScrollPanel extends JScrollPane implements Printable {
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(GridScrollPanel.class.getName());
	
	/** Panel containing alignment grid. */
	private final GridPanel gridPanel;
	
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
		super(gridPanel);

		this.gridPanel = gridPanel;
		
		int headerCellBreadth = gridPanel.getScreenScaleFactor();
		int headerCellDepth = headerCellBreadth * 3;
		
		String[] sourceWords = gridPanel.getSourceWords();
		String[] targetWords = gridPanel.getTargetWords();

		this.columnHeader = new GridScrollPanelHeader(targetWords, Orientation.HORIZONTAL, headerCellBreadth, headerCellDepth);
		this.rowHeader = new GridScrollPanelHeader(sourceWords, Orientation.VERTICAL, headerCellBreadth, headerCellDepth);
		
		this.setPreferredSize(new Dimension(300, 250));
		
		this.setColumnHeaderView(columnHeader);
		this.setRowHeaderView(rowHeader);

		Border outerBorder = BorderFactory.createEmptyBorder(20,20,20,20);
		Border innerBorder = BorderFactory.createLineBorder(Color.BLACK);
		this.setBorder(
				BorderFactory.createCompoundBorder(outerBorder, innerBorder));
	}

	
	void gotoNextSentence() {
		
		int sentenceNumber = gridPanel.getSentenceNumber();
		int numSentences = gridPanel.getNumSentences();
		
		int nextSentence = sentenceNumber+1;
		if (nextSentence < numSentences) {
			gridPanel.setSentenceNumber(nextSentence);
		} else {
			logger.warning("Attempted to navigate to sentence beyond the end of the corpus");
		}
		
		refreshHeaders();
		this.repaint();
	}
		
	void gotoPreviousSentence() {
		int sentenceNumber = gridPanel.getSentenceNumber();
		
		int prevSentence = sentenceNumber-1;
		if (prevSentence >= 0) {
			gridPanel.setSentenceNumber(prevSentence);
		} else {
			logger.warning("Attempted to navigate to sentence beyond the start of the corpus");
		}
		refreshHeaders();
		this.repaint();
	}
	
	void setSentenceNumber(int sentenceNumber) { 
		gridPanel.setSentenceNumber(sentenceNumber);
		refreshHeaders();
		this.repaint();
	}
	
	private void refreshHeaders() {

		String[] sourceWords = gridPanel.getSourceWords();
		String[] targetWords = gridPanel.getTargetWords();
	
		columnHeader.setWords(targetWords);
		rowHeader.setWords(sourceWords);
		
		this.invalidate();
	}
	
	/* See Javadoc for javax.swing.JComponent#printComponent(Graphics) */
	@Override
	protected void printComponent(Graphics g) {
		// The background of the page should be white
		Dimension d = this.getSize();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, d.width, d.height);
	}
	
	/* See Javadoc for javax.swing.JComponent#paintChildren(Graphics) */
	@Override
	protected void printChildren(Graphics g) {
//		super.paintChildren(g);
		columnHeader.printAll(g);
		gridPanel.printAll(g);
	}
	
	/* See Javadoc for java.awt.print.Printable#printBorder(Graphics) */
	@Override
	protected void printBorder(Graphics g) {
		// This method intentionally left blank.
	}
	
	/* See Javadoc for java.awt.print.Printable#print(Graphics,PageFormat,int) */
	public int print(Graphics graphics, PageFormat pageFormat, int page) throws PrinterException {

		// We have only one page, and 'page' is zero-based
		if (page > 0) { 
	         return NO_SUCH_PAGE;
	    } 
	
		// User (0,0) is typically outside the imageable area, so we must
	    // translate by the X and Y values in the PageFormat to avoid clipping
	    Graphics2D g = (Graphics2D) graphics;
	    g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

	    this.printAll(g);

		// tell the caller that this page is part of the printed document
	    return PAGE_EXISTS;
		
	}
	
	GridPanel getGridPanel() {
		return gridPanel;
	}
}
