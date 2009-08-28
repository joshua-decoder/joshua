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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;

import joshua.ui.Orientation;

/**
 * User interface for displaying words as a header of an
 * AlignmentGridPanel.
 * 
 * @author Lane Schwartz
 */
public class GridScrollPanelHeader extends JComponent {
	
	/** Logger for this class. */
	private static final Logger logger =
		Logger.getLogger(GridScrollPanelHeader.class.getName());
	
    /** Indicates whether the header is horizontal or vertical. */
    public Orientation orientation;
	
    /**
	 * In a horizontal orientation, breadth corresponds to cell
	 * height. In a vertical orientation, breadth corresponds
	 * to cell width.
	 */
    private int breadth;
    
	/**
	 * In a horizontal orientation, depth corresponds to cell
	 * width. In a vertical orientation, depth corresponds to
	 * cell height.
	 */
    private final int depth;

    /** Words to display in this header. */
    private String[] words;

    private Color backgroundColor;
    
    void setWords(String[] words) {
    	this.words = words;
    	Dimension size;
        if (orientation==Orientation.HORIZONTAL) {
        	size = new Dimension(breadth*words.length, depth);
        } else {
        	size = new Dimension(depth, breadth*words.length);
        }
        
        this.setPreferredSize(size);
        this.setSize(size);
        this.setMaximumSize(size);
    }
    
    /** 
	 * Represents the total width and height of this component.
     */
//    private Dimension size;
    
    /**
	 * Constructs a new scroll panel header.
	 *
	 * @param orientation Orientation of the header.
	 * @param breadth In a horizontal orientation, depth
	 *            corresponds to cell width. In a vertical
	 *            orientation, depth corresponds to cell
	 *            height.
	 * @param depth In a horizontal orientation, depth corresponds
	 *            to cell width. In a vertical orientation,
	 *            depth corresponds to cell height.
     */
    public GridScrollPanelHeader(String[] words, Orientation orientation, int breadth, int depth) {
        this.orientation = orientation;
        this.breadth = breadth;
        this.depth = depth;
        this.words = words;
        
        if (logger.isLoggable(Level.FINE)) logger.fine(Arrays.toString(words));
        
        Dimension size;
        if (orientation==Orientation.HORIZONTAL) {
        	size = new Dimension(breadth*words.length, depth);
        } else {
        	size = new Dimension(depth, breadth*words.length);
        }
        
        this.setPreferredSize(size);
        this.setSize(size);
        this.setMaximumSize(size);
        
        backgroundColor = new Color(230, 163, 4);
//        Border innerBorder = BorderFactory.createLineBorder(Color.BLACK);
//		this.setBorder(innerBorder);
    }

    /**
	 * Gets the breadth of this header.
     * <p>
	 * In a horizontal orientation, depth corresponds to cell
	 * width. In a vertical orientation, depth corresponds to
	 * cell height.
	 *
	 * @return breadth of this header
     */
    public int getBreadth() {
        return breadth;
    }

	/* See Javadoc for javax.swing.JComponent#printComponent(Graphics) */
    @Override
    protected void printComponent(Graphics graphics) {
    	paintComponent(graphics);
//		Dimension d = this.getSize();
//		graphics.setColor(Color.BLUE);
//		graphics.fillRect(0, 0, d.width, d.height);
    }
    
    @Override
    protected void printBorder(Graphics graphics) {
    	printOrPaintBorder(graphics);
    }
    
    @Override
    protected void paintBorder(Graphics graphics) {
    	printOrPaintBorder(graphics);
    }
    
    protected void printOrPaintBorder(Graphics graphics) {
    	graphics.setColor(Color.BLACK);
    	int breadth = this.breadth*words.length;
    	if (orientation == Orientation.HORIZONTAL) {
    		graphics.drawLine(0, 0, breadth, 0);
    	} else {
    		graphics.drawLine(0, 0, 0, breadth);
    	}
    }
    
    
    
	/* See Javadoc for javax.swing.JComponent#paintComponent(Graphics) */
    @Override
    protected void paintComponent(Graphics graphics) {
    	Graphics2D g = (Graphics2D) graphics;
    	
        Rectangle clipBounds = g.getClipBounds();
        logger.fine("Clip bounds = " + clipBounds);
        
        int width, height;
        if (orientation == Orientation.HORIZONTAL) {
        	width = breadth*words.length;
        	height = depth;
        } else {
        	width = depth;
        	height = breadth*words.length;
        }
        
        g.setColor(backgroundColor);
//        g.fillRect(0, 0, size.width, size.height);
        g.fillRect(0, 0, width, height);
        
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(Color.black);

        int start,end;

        if (orientation == Orientation.HORIZONTAL) {
            start = (clipBounds.x / breadth) * breadth;
            end = (((clipBounds.x + clipBounds.width) / breadth) + 1) * breadth;
            if (end > width + 1) end = width + 1;
        } else {
            start = (clipBounds.y / breadth) * breadth;
            end = (((clipBounds.y + clipBounds.height) / breadth) + 1) * breadth;
            if (end > height + 1) end = height + 1;
        }

    	double theta = Math.PI / -2.0;
        for (int i = start; i < end; i += breadth) {

        	int textIndex = i / (breadth);
        	String text = (textIndex < words.length) ? text = words[textIndex] : null;
        	
        	if (orientation == Orientation.HORIZONTAL) {
        		g.drawLine(i, depth, i, 0);
        		if (logger.isLoggable(Level.FINEST)) logger.finest("targetIndex = " + i + "/" + breadth + " = " + textIndex + "  ==  " + text);
        		if (text != null) {
        			int x = i+(2*breadth/3);
        			int y = depth - 2;
        			g.rotate(theta, x, y);
    				g.drawString(text, x, y);
    				g.rotate(-theta, x, y);
        		}
        	} else {
        		g.drawLine(depth, i, 0, i);
        		if (logger.isLoggable(Level.FINEST)) logger.finest("sourceIndex = " + i + "/" + breadth + " = " + textIndex + "  ==  " + text);
        		if (text != null) {
        			g.drawString(text, 2, i+(2*breadth/3));
        		}
        	}

        }
    }
}

