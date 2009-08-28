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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.suffix_array.Suffixes;
import joshua.util.FormatUtil;

/**
 * User interface for navigating between sentences.
 *
 * @author Lane Schwartz
 */
public class NavigatorPanel extends JPanel implements ActionListener {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(NavigatorPanel.class.getName());
	
	private final GridScrollPanel gridScrollPanel;
	
	private final JButton prev;	
	private final JButton next;
	private final JTextField text;
	private final JButton enter;

	private JLabel statusLabel;
	
	/**
	 * Constructs a panel for navigating between sentences.
	 * 
	 * @param gridScrollPanel Grid scroll panel
	 */
	public NavigatorPanel(GridScrollPanel gridScrollPanel) {
		this.gridScrollPanel = gridScrollPanel;
		this.setLayout(new BorderLayout());
				
		
		JPanel buttonPanel = new JPanel();
		JPanel statusPanel = new JPanel();
		
		this.prev = new JButton("Previous");
		this.next = new JButton("Next");
		this.enter = new JButton("Jump to sentence");
		this.text = new JTextField(10);
		
		this.prev.addActionListener(this);
		this.next.addActionListener(this);
		this.enter.addActionListener(this);
		this.text.addActionListener(this);
		
		buttonPanel.add(prev);
		buttonPanel.add(next);
		buttonPanel.add(enter);
		buttonPanel.add(text);
		
		GridPanel gridPanel = gridScrollPanel.getGridPanel();
		this.statusLabel = new JLabel("Sentence " + (gridPanel.getSentenceNumber()+1) + " of " + gridPanel.getNumSentences());
		statusPanel.add(statusLabel);
		updateStatusLabel();
		
		this.add(buttonPanel, BorderLayout.PAGE_START);
		this.add(statusPanel, BorderLayout.PAGE_END);
	}

	private void updateStatusLabel() {
		GridPanel gridPanel = gridScrollPanel.getGridPanel();
		int sentenceNumber = gridPanel.getSentenceNumber()+1;
		this.statusLabel.setText("Sentence " + sentenceNumber + " of " + gridPanel.getNumSentences());
		
		if (sentenceNumber==1) {
			prev.setEnabled(false);
		} else {
			prev.setEnabled(true);
		}
		
		if (sentenceNumber < gridPanel.getNumSentences()) {
			next.setEnabled(true);
		} else {
			next.setEnabled(false);
		}
	}
	
	/* See Javadoc for ActionListener. */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (prev.equals(source)) {
			logger.finest("Selecting previous sentence");
			gridScrollPanel.gotoPreviousSentence();
		} else if (next.equals(source)) {
			logger.finest("Selecting next sentence");
			gridScrollPanel.gotoNextSentence();
		} else if (enter.equals(source) || text.equals(source)) {
			String value = text.getText();
			if (FormatUtil.isNumber(value)) {
				int sentenceNumber = Double.valueOf(value).intValue() - 1;
				if (sentenceNumber < 0) {
					sentenceNumber = 0;
				} else if (sentenceNumber >= gridScrollPanel.getGridPanel().getNumSentences()) {
					sentenceNumber = gridScrollPanel.getGridPanel().getNumSentences() - 1;
				}
				gridScrollPanel.setSentenceNumber(sentenceNumber);
			} else {
				text.setText(null);
			}
		}
		

		updateStatusLabel();
	}
	
	
	
}
