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
package joshua.ui;

import java.awt.Dimension;
import java.io.File;
import java.io.ObjectInput;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import joshua.corpus.Corpus;
import joshua.corpus.SymbolTable;
import joshua.corpus.Vocabulary;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.alignment.mm.MemoryMappedAlignmentGrids;
import joshua.corpus.mm.MemoryMappedCorpusArray;
import joshua.ui.AlignmentGridPanel;
import joshua.util.io.BinaryIn;

/**
 * 
 * @author Lane Schwartz
 */
public class ScrollableGridPanel extends JPanel {

	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(ScrollableGridPanel.class.getName());

	/** Header containing target language words. */
	private final GridPanelHeader columnHeader;

	/** Header containing source language words. */
	private final GridPanelHeader rowHeader;

	/**
	 * Constructs a scrollable panel wrapping an alignment grid.
	 * 
	 * @param gridPanel An alignment grid
	 */
	public ScrollableGridPanel(AlignmentGridPanel gridPanel) {
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

		int headerCellBreadth = gridPanel.getScaleFactor();
		int headerCellDepth = headerCellBreadth * 3;
		
		String[] sourceWords = gridPanel.getSourceWords();
		String[] targetWords = gridPanel.getTargetWords();

		this.columnHeader = new GridPanelHeader(targetWords, Orientation.HORIZONTAL, headerCellBreadth, headerCellDepth);
		this.rowHeader = new GridPanelHeader(sourceWords, Orientation.VERTICAL, headerCellBreadth, headerCellDepth);
		
		
		JScrollPane pictureScrollPane = new JScrollPane(gridPanel);
		pictureScrollPane.setPreferredSize(new Dimension(300, 250));
		
		pictureScrollPane.setColumnHeaderView(columnHeader);
		pictureScrollPane.setRowHeaderView(rowHeader);

		this.add(pictureScrollPane);
		this.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
	}


	/**
	 * Constructs a runnable capable of 
	 * initializing and displaying an alignment grid.
	 * 
	 * @param joshDirName
	 * @param sentenceNumber
	 * @return a runnable capable of 
	 *         initializing and displaying an alignment grid.
	 */
	public static Runnable displayGrid(final String joshDirName, final int sentenceNumber) {

		return new Runnable() {
			public void run() {

				try {

					String binaryVocabFileName = joshDirName + File.separator + "common.vocab";
					String binarySourceFileName = joshDirName + File.separator + "source.corpus";
					String binaryTargetFileName = joshDirName + File.separator + "target.corpus";
					String binaryAlignmentFileName = joshDirName + File.separator + "alignment.grids";

					logger.fine("Loading vocabulary...");
					SymbolTable commonVocab = new Vocabulary();
					ObjectInput in = BinaryIn.vocabulary(binaryVocabFileName);
					commonVocab.readExternal(in);

					logger.fine("Loading source corpus...");
					Corpus sourceCorpus = new MemoryMappedCorpusArray(commonVocab, binarySourceFileName);

					logger.fine("Loading target corpus...");		
					Corpus targetCorpus = new MemoryMappedCorpusArray(commonVocab, binaryTargetFileName);

					logger.fine("Loading alignment grids...");
					Alignments alignments = new MemoryMappedAlignmentGrids(binaryAlignmentFileName, sourceCorpus, targetCorpus);

					logger.fine("Constructing panel...");
					AlignmentGridPanel gridPanel = new AlignmentGridPanel(sourceCorpus, targetCorpus, alignments, sentenceNumber);

					//Create and set up the window.
					JFrame frame = new JFrame("Sentence Alignment");
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

					//Create and set up the content pane.
					JComponent newContentPane = new ScrollableGridPanel(gridPanel);
					newContentPane.setOpaque(true); //content panes must be opaque
					frame.setContentPane(newContentPane);

					//Display the window.
					frame.pack();
					frame.setVisible(true);

				} catch (Throwable e) {
					logger.severe("Unable to start program: " + e.getLocalizedMessage());
				}

			} 

		};

	}


	public static void main(String[] args) {

		String joshDirName = args[0];

		int sentenceNumber = 0;
		if (args.length > 1) sentenceNumber = Integer.valueOf(args[1]);

		// Ask Swing to start the user interface
		javax.swing.SwingUtilities.invokeLater(
				displayGrid(joshDirName, sentenceNumber)
		);


	}
}
