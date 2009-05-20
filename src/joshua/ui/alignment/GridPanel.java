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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.Alignments;

/**
 * Presents a visual display of an alignment grid
 * for one aligned sentence.
 * 
 * @author Lane Schwartz
 */
public class GridPanel extends JPanel {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(GridPanel.class.getName());
	
	/** Source side of an aligned parallel corpus. */
	private final Corpus sourceCorpus;
	
	/** Target side of an aligned parallel corpus. */
	private final Corpus targetCorpus;
	
	/** Alignment data for an aligned parallel corpus. */
	private final Alignments alignments;
	
	private Dimension preferredSize;
	private int numSentences;
	private int sentenceNumber;
	private int numSourceWords;
	private int numTargetWords;
	
	/** 
	 * Represents the breadth and height of a cell
	 * when displayed on screen.
	 */
	private int screenScaleFactor = 25;
	
	/** 
	 * Represents the breadth and height of a cell
	 * when printed.
	 */
	private int printerScaleFactor = 10;
	
	/**
	 * Constructs a panel to display an aligned sentence pair.
	 * 
	 * @param sourceCorpus Source corpus
	 * @param targetCorpus Target corpus
	 * @param alignments Sentence alignments for the parallel corpus
	 * @param sentenceNumber Index of the sentence to display
	 */
	public GridPanel(Corpus sourceCorpus, Corpus targetCorpus, Alignments alignments, int sentenceNumber) {
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
		this.alignments = alignments;
		this.numSentences = alignments.size();
		
		this.setSentenceNumber(sentenceNumber);
	}
		
	/**
	 * Sets the aligned sentence to be displayed by this component.
	 * 
	 * @param sentenceNumber Index of the sentence to display
	 */
	public void setSentenceNumber(int sentenceNumber) {
		this.sentenceNumber = sentenceNumber;
		
		this.numSourceWords = 
			sourceCorpus.getSentenceEndPosition(sentenceNumber) -
			sourceCorpus.getSentencePosition(sentenceNumber);
		
		this.numTargetWords = 
			targetCorpus.getSentenceEndPosition(sentenceNumber) -
			targetCorpus.getSentencePosition(sentenceNumber);
		
		int preferredWidth = numTargetWords*screenScaleFactor;
		int preferredHeight = numSourceWords*screenScaleFactor;
				
		preferredSize = new Dimension(preferredWidth, preferredHeight);
		
	}
		
	/* See Javadoc for javax.swing.JComponent#getPreferredSize */
	@Override
	public Dimension getPreferredSize() {
		return preferredSize;
	}
	
	/* See Javadoc for javax.swing.JComponent#getMinimumSize */
	@Override
	public Dimension getMinimumSize() {
		return preferredSize;
	}

	public int getScreenScaleFactor() {
		return this.screenScaleFactor;
	}
	
	public int getPrinterScaleFactor() {
		return this.printerScaleFactor;
	}
	
	/* See Javadoc for javax.swing.JComponent#paintComponent(Graphics) */
	@Override
	protected void paintComponent(Graphics graphics) {
		paintSomething(graphics, screenScaleFactor);
	}
	
	/* See Javadoc for javax.swing.JComponent#printComponent(Graphics) */
	@Override
	protected void printComponent(Graphics graphics) { 

	    this.paintSomething(graphics, printerScaleFactor);

	}
	
	protected void paintSomething(Graphics graphics, int scaleFactor) {
		
//		Dimension d = preferredSize;
		
		int width = numTargetWords*scaleFactor;
		int height = numSourceWords*scaleFactor;
		
		Graphics2D g = (Graphics2D) graphics;
		
		g.setBackground(Color.WHITE);
		g.setColor(Color.WHITE);
		
//		g.fillRect(0, 0, d.width, d.height);
		g.fillRect(0, 0, width, height);
		
		if (sentenceNumber < numSentences) {
			
			g.setColor(Color.BLACK);
			
			int widthStep = scaleFactor; //d.width / numTargetWords;
			int heightStep = scaleFactor; //d.height / numSourceWords;
			
			if (logger.isLoggable(Level.FINER)) {
//				logger.finer("widthStep = " + numTargetWords + "/" + d.width + " = "+ widthStep);
//				logger.finer("heightStep = " + numSourceWords + "/" + d.height + " = "+ heightStep);
				logger.finer("widthStep = " + numTargetWords + "/" + width + " = "+ widthStep);
				logger.finer("heightStep = " + numSourceWords + "/" + height + " = "+ heightStep);
			}
			
			int minX = 0;
			int minY = 0;
			
			// Draw vertical grid lines
//			for (int x=minX, maxX=d.width, maxY=d.height; x<=maxX; x+=widthStep) {	
			for (int x=minX, maxX=width, maxY=height; x<=maxX; x+=widthStep) {	
				g.drawLine(x, minY, x, maxY);	
			}
			
			// Draw horizontal grid lines
//			for (int y=minY, maxX=d.width, maxY=d.height; y<=maxY; y+=heightStep) {	
			for (int y=minY, maxX=width, maxY=height; y<=maxY; y+=heightStep) {	
				g.drawLine(minX, y, maxX, y);	
			}
			
			int sourceSentenceStart = sourceCorpus.getSentencePosition(sentenceNumber);
			int targetSentenceStart = targetCorpus.getSentencePosition(sentenceNumber);
			
			// Draw aligned points
			for (int sourceSentenceIndex=0, sourceCorpusIndex=sourceSentenceStart+sourceSentenceIndex; sourceSentenceIndex<numSourceWords; sourceSentenceIndex++, sourceCorpusIndex++) {
				
				int y = sourceSentenceIndex * heightStep;
				
				int[] targetPoints = alignments.getAlignedTargetIndices(sourceCorpusIndex);
				
				if (targetPoints != null) {
					for (int targetCorpusIndex : targetPoints) {

						int targetSentenceIndex = targetCorpusIndex - targetSentenceStart;
						int x = targetSentenceIndex * widthStep;

						g.fillRect(x, y, widthStep, heightStep);
						
						if (logger.isLoggable(Level.FINEST)) logger.finest("Filling rectangle for " + sourceSentenceIndex + "-" + targetSentenceIndex);

					}
				}
			}
			
		}
		
	}
	
	
	public String[] getSourceWords() {
		
		String[] words = new String[numSourceWords];
		
		int sentenceStart = sourceCorpus.getSentencePosition(sentenceNumber);
		
		for (int sourceIndex=0; sourceIndex<numSourceWords; sourceIndex++) {

			int token = sourceCorpus.getWordID(sentenceStart + sourceIndex);
			words[sourceIndex] = sourceCorpus.getVocabulary().getTerminal(token);
			
		}
		
		return words;
	}
	
	public String[] getTargetWords() {

		String[] words = new String[numTargetWords];

		int sentenceStart = targetCorpus.getSentencePosition(sentenceNumber);

		for (int targetIndex=0; targetIndex<numTargetWords; targetIndex++) {

			int token = targetCorpus.getWordID(sentenceStart + targetIndex);
			words[targetIndex] = targetCorpus.getVocabulary().getTerminal(token);

		}

		return words;
	}

}
