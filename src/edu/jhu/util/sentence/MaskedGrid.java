package edu.jhu.util.sentence;

import java.util.*;

/**
 * Subclass of Grid that uses an existing set of coordinate arrays
 * and an offset into them for retrieving points. 
 *
 * @author Josh Schroeder
 * @since 10 Jan 2005
 *
 * The contents of this file are subject to the Linear B Community Research 
 * License Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.linearb.co.uk/developer/. Software distributed under the License
 * is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either 
 * express or implied. See the License for the specific language governing 
 * rights and limitations under the License. 
 *
 * Copyright (c) Linear B Ltd., 2002-2005. All rights reserved.
 */
public class MaskedGrid extends Grid {


//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	protected int xOffset;
	protected int yOffset;


	
//===============================================================
// Constructor(s)
//===============================================================

	public MaskedGrid(Grid fullGrid, int xOffset, int width, int yOffset, int height) {
		super(fullGrid);
		if(!fullGrid.transposed) {
			this.width = width;
			this.height = height;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
		} else {
			this.width = height;
			this.height = width;
			this.xOffset = yOffset;
			this.yOffset = xOffset;
		}
		if ((xOffset+width)>fullGrid.getWidth() ||
			(yOffset+height)>fullGrid.getHeight()) throw new ArrayIndexOutOfBoundsException("invalid parent grid: "+getXOffset()+" "+getWidth()+" "+fullGrid.getWidth()+" "+getYOffset()+" "+getHeight()+" "+fullGrid.getHeight());
	}
			

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	//===========================================================
	// Methods
	//===========================================================

	/**
	 * compares this object to another. If it is also a grid, first
	 * checks height and width compatibility, then checks that all
	 * coordinates are equal.
	 * @param o object to comare to
	 * @return true if o is a Grid of the same size and containing the same points
	 * as this one
	 * @TODO make this smarter
	 */
	public boolean equals(Object o) {
		if (o==null) return false;
        if (!o.getClass().isInstance(this)) return false;
		MaskedGrid other = (MaskedGrid)o;
		return (this.getWidth()==other.getWidth() &&
				this.getHeight()==other.getHeight() &&
				Arrays.equals(this.getCoordinates(),other.getCoordinates()));
	}

	
	/**
	 * Returns a sorted list of the row points that are items for the column span
	 * end is exclusive
	 */
	public int[] getRowPoints(int columnStart, int columnEnd) {
		int[] points = super.getRowPoints(columnStart + getXOffset(), columnEnd + getXOffset());
		//the resulting list is populated with offset points which we need to move to our coordinate system
		return offsetPoints(points, getYOffset());
	}

	/**
	 * Returns a sorted list of the column points that are items for the row span
	 * end is exclusive
	 */
	public int[] getColumnPoints (int rowStart, int rowEnd) {
		int[] points =  super.getColumnPoints(rowStart + getYOffset(), rowEnd + getYOffset());
		return offsetPoints(points, getXOffset());
	}
	
	public int[] offsetPoints(int[] points, int offset) {
		//count final size
		int numPoints = 0;
		for (int i=0;i<points.length;i++) {
			if (points[i]>=offset) numPoints++;
		}
		int[] offsetPoints = new int[numPoints];
		int index=0;
		for (int i=0;i<points.length;i++) {
			if (points[i]>=offset) {
				offsetPoints[index++]=points[i]-offset;
			}
		}	
		return offsetPoints;		
	}
	
	

	
	
//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

	protected int getXOffset() {
		if (!transposed) return xOffset;
		else return yOffset;
	}
	
	protected int getYOffset() {
		if (!transposed) return yOffset;
		else return xOffset;
	}
	
	/**
	 * generates the short value stored for a given x,y pair
	 */
	protected short getKey(int x, int y) {
		int storedX = x+getXOffset();
		int storedY = y+getYOffset();
		return super.getKey(storedX,storedY);
	}
	
	/**
	 * generates the location of a coordinate from a key
	 */
	protected short[] getLocation(short key) {
		short[] location = super.getLocation(key);
		location[0] = (short)(location[0]-getXOffset());
		location[1] = (short)(location[1]-getYOffset());
		if (isValid(location[0],location[1])) return location;
		else return null;
	}	
	


//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
//===============================================================
// Static
//===============================================================

  

//===============================================================
// Main 
//===============================================================

	static public void main(String[] args) {

		
	}
	
}
