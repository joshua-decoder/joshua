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
package joshua.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import joshua.util.io.BinaryIn;

/**
 * Presents a visual display of an alignment grid
 * for one aligned sentence.
 * 
 * @author Lane Schwartz
 */
public class AlignmentGridPanel extends JPanel {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(AlignmentGridPanel.class.getName());
	
	private final Corpus sourceCorpus;
	private final Corpus targetCorpus;
	private final Alignments alignments;
	
	private Dimension preferredSize;
	private int numSentences;
	private int sentenceNumber;
	private int numSourceWords;
	private int numTargetWords;
	
	public AlignmentGridPanel(Corpus sourceCorpus, Corpus targetCorpus, Alignments alignments, int sentenceNumber) {
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.numSentences = alignments.size();
		
		this.setSentenceNumber(sentenceNumber);
	}
	
	public void setSentenceNumber(int sentenceNumber) {
		this.sentenceNumber = sentenceNumber;
		
		this.numSourceWords = 
			sourceCorpus.getSentenceEndPosition(sentenceNumber) -
			sourceCorpus.getSentencePosition(sentenceNumber);
		
		this.numTargetWords = 
			targetCorpus.getSentenceEndPosition(sentenceNumber) -
			targetCorpus.getSentencePosition(sentenceNumber);
		
		int multiplier = 50;
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		int preferredWidth = numTargetWords*multiplier;
		int preferredHeight = numSourceWords*multiplier;
		
		if (preferredWidth > screenSize.width) {
			preferredWidth = screenSize.width;
		}
		
		if (preferredHeight > screenSize.height) {
			preferredHeight = screenSize.height;
		}	
		
		preferredSize = new Dimension(preferredWidth, preferredHeight);
		
//		this.setPreferredSize(d);
//		this.setMinimumSize(d);
	}
	
	public Dimension getPreferredSize() {
		return preferredSize;
	}
	
	public Dimension getMinimumSize() {
		return preferredSize;
	}
	
	protected void paintComponent(Graphics graphics) {
		
		Dimension d = preferredSize;
		
		Graphics2D g = (Graphics2D) graphics;
		
		g.setBackground(Color.WHITE);
		g.setColor(Color.WHITE);
		
		g.fillRect(0, 0, d.width, d.height);
		
		if (sentenceNumber < numSentences) {
			
			g.setColor(Color.BLACK);
			
			int widthStep = d.width / numTargetWords;
			int heightStep = d.height / numSourceWords;
			
			if (logger.isLoggable(Level.FINER)) {
				logger.finer("widthStep = " + numTargetWords + "/" + d.width + " = "+ widthStep);
				logger.finer("heightStep = " + numSourceWords + "/" + d.height + " = "+ heightStep);
			}
			
			int minX = 0;
			int minY = 0;
			
			// Draw vertical grid lines
			for (int x=minX, maxX=d.width, maxY=d.height; x<=maxX; x+=widthStep) {	
				g.drawLine(x, minY, x, maxY);	
			}
			
			// Draw horizontal grid lines
			for (int y=minY, maxX=d.width, maxY=d.height; y<=maxY; y+=heightStep) {	
				g.drawLine(minX, y, maxX, y);	
			}
			
			// Draw aligned points
			for (int sourceIndex=0; sourceIndex<numSourceWords; sourceIndex++) {
				int[] targetPoints = alignments.getAlignedTargetIndices(sourceIndex);
				
				if (targetPoints != null) {
					for (int targetIndex : targetPoints) {

						int x = targetIndex * widthStep;

						int y = sourceIndex * heightStep;

						g.fillRect(x, y, widthStep, heightStep);

					}
				}
			}
			
		}
		
		drawHere = g.getClipBounds();
	}
	
	Rectangle drawHere;
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		String joshDirName = args[0];
		
		int sentenceNumber = 0;
		if (args.length > 0) sentenceNumber = Integer.valueOf(args[1]);
		
		String binaryVocabFileName = joshDirName + File.separator + "common.vocab";
		String binarySourceFileName = joshDirName + File.separator + "source.corpus";
		String binaryTargetFileName = joshDirName + File.separator + "target.corpus";
		String binaryAlignmentFileName = joshDirName + File.separator + "alignment.grids";
		
		logger.info("Loading vocabulary...");
		SymbolTable commonVocab = new Vocabulary();
		ObjectInput in = BinaryIn.vocabulary(binaryVocabFileName);
		commonVocab.readExternal(in);
		
		logger.info("Loading source corpus...");
		Corpus sourceCorpus = new MemoryMappedCorpusArray(commonVocab, binarySourceFileName);
		
		logger.info("Loading target corpus...");		
		Corpus targetCorpus = new MemoryMappedCorpusArray(commonVocab, binaryTargetFileName);
		
		logger.info("Loading alignment grids...");
		Alignments alignments = new MemoryMappedAlignmentGrids(binaryAlignmentFileName, sourceCorpus, targetCorpus);
		
		logger.info("Constructing panel...");
		AlignmentGridPanel panel = new AlignmentGridPanel(sourceCorpus, targetCorpus, alignments, sentenceNumber);
		
		logger.finer("Panel preferred size: " + panel.getPreferredSize());
		
		Dimension scrollSize = new Dimension(panel.getPreferredSize().width + 50, panel.getPreferredSize().height + 50);
		

		
		JScrollPane scrollPanel = new JScrollPane(panel);
		scrollPanel.setPreferredSize(scrollSize);

//		JComponent columnHeaderView = panel.new ColumnHeader();//new Rule(Rule.HORIZONTAL, true);
//		JComponent rowHeaderView = panel.new RowHeader();
//		scrollPanel.setRowHeaderView(rowHeaderView);
//		scrollPanel.setColumnHeaderView(columnHeaderView);

		
		logger.info("Displaying frame...");
		JFrame frame = new JFrame("Aligned Sentences");
		frame.setContentPane(scrollPanel);
//		frame.setMinimumSize(panel.getMinimumSize());
		frame.setPreferredSize(panel.getPreferredSize());
		frame.setSize(scrollPanel.getPreferredSize());
		frame.setVisible(true);
		
	}
	
	class ColumnHeader extends JComponent {
		
		private final Dimension minimumDimension;
		
		ColumnHeader() {
			minimumDimension = new Dimension(100,100);
		}
			
		public Dimension getPreferredSize() {
			return this.minimumDimension;
		}
		
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics;
			
			Dimension d = AlignmentGridPanel.this.getSize(); //getSize();
			
			g.setColor(Color.WHITE);
			
			g.fillRect(0, 0, d.width, d.height);
			
			g.setColor(Color.BLACK);
			
			int widthStep = d.width / numTargetWords;
			
			int y = d.height - 5;
			double theta = -45.0;
			
			int sentenceStart = targetCorpus.getSentencePosition(sentenceNumber);
			
			for (int targetIndex=0; targetIndex<numTargetWords; targetIndex++) {
				
				int x = targetIndex * widthStep + widthStep/3;
				logger.finest("x==" + x);
				
				int token = targetCorpus.getWordID(sentenceStart + targetIndex);
				String text = targetCorpus.getVocabulary().getTerminal(token);
				logger.finer(text);
				
				g.rotate(theta, x, y);
				g.drawString(text, x, y);
				g.rotate(-theta, x, y);
				
			}
			
		}
		
	}
	
	class RowHeader extends JComponent {
		
		private final Dimension minimumDimension;
		
		RowHeader() {
			minimumDimension = new Dimension(100,100);
		}
			
		public Dimension getPreferredSize() {
			return this.minimumDimension;
		}
		
		protected void paintComponent(Graphics graphics) {
			AlignmentGridPanel.this.paint(graphics);
			
			Graphics2D g = (Graphics2D) graphics;
			
			
			
			g.setColor(Color.RED);
			g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);
			
			Dimension d =  AlignmentGridPanel.this.getSize(); //minimumDimension; //getPreferredSize();
			
			
			
//			g.fillRect(0, 0, d.width, d.height);
			
			g.setColor(Color.BLACK);
			
			int heightStep = d.height / numSourceWords;
			int increment = heightStep;
			
			logger.info("drawHere.x = " + drawHere.x);
			logger.info("drawHere.y = " + drawHere.y);
			logger.info("drawHere.height = " + drawHere.height);
			logger.info("drawHere.width = " + drawHere.width);
			logger.info("increment = " + increment);
			
			int start = (drawHere.y / increment) * increment;
            int end = (((drawHere.y + drawHere.height) / increment) + 1)
                  * increment;
			
            logger.info("start = " + start);
            logger.info("end = " + end);
            
            for (int i = start; i < end; i += increment) {
            	
            	g.drawLine(0, i, 100, i);
            }
			
//			int x = 0; //d.width - 5;
////			double theta = -45.0;
//			
//			int sentenceStart = sourceCorpus.getSentencePosition(sentenceNumber);
//			
//			for (int sourceIndex=0, y=(sourceIndex+1) * heightStep - heightStep/3; 
//					sourceIndex<numSourceWords && y<drawHere.height; 
//					sourceIndex++, y=(sourceIndex+1) * heightStep - heightStep/3) {
//				
////				int y = (sourceIndex+1) * heightStep - heightStep/3;
//				logger.finest("y==" + y);
//				
//				int token = sourceCorpus.getWordID(sentenceStart + sourceIndex);
//				String text = sourceCorpus.getVocabulary().getTerminal(token);
//				logger.finer(text);
//				
////				g.rotate(theta, x, y);
//				g.drawString(text, x, y);
////				g.rotate(-theta, x, y);
//				
//			}
			
		}
		
	}
	
	static class Rule extends JComponent {
		public static final int INCH = Toolkit.getDefaultToolkit().
		getScreenResolution();
		public static final int HORIZONTAL = 0;
		public static final int VERTICAL = 1;
		public static final int SIZE = 35;

		public int orientation;
		public boolean isMetric;
		private int increment;
		private int units;

		public Rule(int o, boolean m) {
			orientation = o;
			isMetric = m;
			setIncrementAndUnits();
		}

		public void setIsMetric(boolean isMetric) {
			this.isMetric = isMetric;
			setIncrementAndUnits();
			repaint();
		}

		private void setIncrementAndUnits() {
			if (isMetric) {
				units = (int)((double)INCH / (double)2.54); // dots per centimeter
				increment = units;
			} else {
				units = INCH;
				increment = units / 2;
			}
		}

		public boolean isMetric() {
			return this.isMetric;
		}

		public int getIncrement() {
			return increment;
		}

		public void setPreferredHeight(int ph) {
			setPreferredSize(new Dimension(SIZE, ph));
		}

		public void setPreferredWidth(int pw) {
			setPreferredSize(new Dimension(pw, SIZE));
		}

		protected void paintComponent(Graphics g) {
			Rectangle drawHere = g.getClipBounds();

			// Fill clipping area with dirty brown/orange.
			g.setColor(new Color(230, 163, 4));
			g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

			// Do the ruler labels in a small font that's black.
			g.setFont(new Font("SansSerif", Font.PLAIN, 10));
			g.setColor(Color.black);

			// Some vars we need.
			int end = 0;
			int start = 0;
			int tickLength = 0;
			String text = null;

			// Use clipping bounds to calculate first and last tick locations.
			if (orientation == HORIZONTAL) {
				start = (drawHere.x / increment) * increment;
				end = (((drawHere.x + drawHere.width) / increment) + 1)
				* increment;
			} else {
				start = (drawHere.y / increment) * increment;
				end = (((drawHere.y + drawHere.height) / increment) + 1)
				* increment;
			}

			// Make a special case of 0 to display the number
			// within the rule and draw a units label.
			if (start == 0) {
				text = Integer.toString(0) + (isMetric ? " cm" : " in");
				tickLength = 10;
				if (orientation == HORIZONTAL) {
					g.drawLine(0, SIZE-1, 0, SIZE-tickLength-1);
					g.drawString(text, 2, 21);
				} else {
					g.drawLine(SIZE-1, 0, SIZE-tickLength-1, 0);
					g.drawString(text, 9, 10);
				}
				text = null;
				start = increment;
			}

			// ticks and labels
			for (int i = start; i < end; i += increment) {
				if (i % units == 0)  {
					tickLength = 10;
					text = Integer.toString(i/units);
				} else {
					tickLength = 7;
					text = null;
				}

				if (tickLength != 0) {
					if (orientation == HORIZONTAL) {
						g.drawLine(i, SIZE-1, i, SIZE-tickLength-1);
						if (text != null)
							g.drawString(text, i-3, 21);
					} else {
						g.drawLine(SIZE-1, i, SIZE-tickLength-1, i);
						if (text != null)
							g.drawString(text, 9, i+3);
					}
				}
			}
		}
}

}
