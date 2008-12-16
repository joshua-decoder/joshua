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
package joshua.util.sentence.alignment;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Representation of a 2-dimensional grid. This implementation is designed to 
 * be as memory-efficient as possible for storing many grids in memory. Most JVMs
 * use 32bit ints to store byte, short, and boolean individual primitives, but will actually
 * make efficient use of memory when these small primitives are stored in arrays. Therefore,
 * we create a single sorted array of shorts, where both the X and Y coordinate for a given 
 * "true" point are the grid are encoded in one short.
 *
 * @author Josh Schroeder
 * @since  09 Dec 2004
 * @author Lane Schwartz
 * @since  15 Dec 2008
 */
public class AlignmentGrid {

	//===============================================================
	// Constants
	//===============================================================

	/**
	 * Maximum size of a dimension
	 */
	static public final int MAX_LENGTH = 100;

	/**
	 * Constant used for generating coordinate short value
	 */
	static private final short X_SHIFT = 100;

	//===============================================================
	// Member variables
	//===============================================================


	protected int width;
	protected int height;
	protected short[] coordinates;
	protected short[] transposedCoordinates;
	protected boolean transposed = false;

	/**
	 * Constructor takes the small string representation of alignment points.
	 * @param alignmentPoints the string representation of alignments.
	 */
	public AlignmentGrid(String alignmentPoints) {
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>();
		String[] alignmentPointsArray = alignmentPoints.split(",|\\s");
		for(int i = 0; i < alignmentPointsArray.length; i++) {
			if(!alignmentPointsArray[i].trim().equals("")) {
				Coordinate coord = new Coordinate(alignmentPointsArray[i]);
				width = Math.max(width, coord.x+1);
				height = Math.max(height, coord.y+1);
				coordinates.add(coord);
			}
		}
		initializeCoordinates(coordinates);
	}

	//===============================================================
	// Public
	//===============================================================

	//===========================================================
	// Accessor methods (set/get)
	//===========================================================

	/**
	 * @return the number of points in this grid
	 * TODO Possibly delete this method, since it's not currently used.
	 */
//	public int cardinality() {
//		return coordinates.length;
//	}

	/**
	 * @return the width (X size) of the Grid
	 */
	public int getWidth() {
		if (transposed) return height;
		else return width;
	}

	/**
	 * @return the height (Y size) of the Grid
	 */
	public int getHeight() {
		if (transposed) return width;
		else return height;
	}

	// TODO Possibly delete this method, since it's not currently used.
	public boolean isTransposed() {
		return transposed;
	}


	/**
	 * Checks if a coordinate's values fall within the bounds of the Grid.
	 * DOES NOT check for the existance of the coordinate in the grid. Use
	 * contains for that purpose.
	 * @param x the x value of the location to check validity for
	 * @param y the y value of the location to check validity for
	 * @return true if the location is in bounds
	 * @see #contains(int,int)
	 */
	public boolean isValid(int x, int y) {
		return (x>=0 &&
				y>=0 &&
				x<getWidth() &&
				y<getHeight());
	}


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
	 */
	public boolean equals(Object o) {
		if (o==null) return false;
		if (!o.getClass().isInstance(this)) return false;
		AlignmentGrid other = (AlignmentGrid)o;
		return (this.getWidth()==other.getWidth() &&
				this.getHeight()==other.getHeight() &&
				Arrays.equals(this.getCoordinates(),other.getCoordinates()));
	}

	/**
	 * Checks if the given location is occupied in this grid.
	 * @param location the coordinate location to check for.
	 * @return true if the coordinate is within the bounds of the Grid and exists (is set to true)
	 * 
	 * TODO Possibly delete this method, since it's not currently used.
	 */
	public boolean contains(Coordinate location) {
		return contains(location.x,location.y);
	}

	/**
	 * Checks if a given location is present in the grid.
	 * @param x the x value of the coordinate to check for
	 * @param y the y value of the coordinate to check for
	 * @return true if the coordinate represented by the integers is in bounds and exists
	 */
	public boolean contains(int x, int y) {
		if (isValid(x,y)) {
			int index = Arrays.binarySearch(getCoordinates(), getKey(x,y));
			//the index returned by a binarySearch is positive if the number exists
			return (index>=0);			
		}
		else throw new ArrayIndexOutOfBoundsException("("+x+","+y+")");
	}

	/**
	 * Exports the contents of this grid to a 2-d boolean array, of the size
	 * array[width][height]. Coordinates contained in this grid will be set to
	 * true, all others false.
	 * @return boolean[][] a 2-d boolean array representation of this grid.
	 */
	public boolean[][] generateBooleanArray() {
		int width = getWidth();
		int height = getHeight();
		boolean[][] array = new boolean[width][height];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				array[x][y]=false;
		for (int i=0;i<getCoordinates().length;i++) {
			short[] location = getLocation(getCoordinates()[i]);
			//location may be null if some coordinates are masked (see MaskedGrid)
			if (location !=null) array[location[0]][location[1]] = true;
		}
		return array;	
	}

	/**
	 * Exports the contents of this grid to a collection of Coordinates.
	 * 
	 * TODO Possibly delete this method, since it's not currently used.
	 */
	public Collection<Coordinate> generateCoordinates() {
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>();
		short[] points = getCoordinates();
		for(int i = 0; i < points.length; i++) {
			short[] coordTuple = getLocation(points[i]);
			Coordinate coord = new Coordinate((int) coordTuple[0], (int) coordTuple[1]);
			coordinates.add(coord);
		}
		return coordinates;
	}

	/**
	 * Grid's transpose just sets a boolean flag. Due to dual arrays, neither
	 * direction is faster than the other.
	 * 
	 * TODO Possibly delete this method, since it's not currently used.
	 */
	public void transpose() {
		transposed = !transposed;
	}

	/**
	 * Returns a sorted list (includes any duplicates) of the target language indices that align with the given source language span.
	 * 
	 * @param sourceSpanStart Inclusive start index into the source language sentence.
	 * @param sourceSpanEnd Exclusive end index into the source language sentence.
	 */
	public int[] getTargetPoints(int sourceSpanStart, int sourceSpanEnd) {
		return getPoints(sourceSpanStart, sourceSpanEnd, getHeight(), getCoordinates());
	}

	/**
	 * Returns a sorted list (includes any duplicates) of the source language indices that align with the given target language span.
	 * 
	 * @param targetSpanStart Inclusive start index into the target language sentence.
	 * @param targetSpanEnd Exclusive end index into the target language sentence.
	 */
	public int[] getSourcePoints (int targetSpanStart, int targetSpanEnd) {
		return getPoints(targetSpanStart, targetSpanEnd, getWidth(), getTransposedCoordinates());
	}


	public int[] getPoints(int start, int end, int maxKey, short[] points) {
		short startKey = getKey(start,0);
		short endKey = getKey(end-1,maxKey);
		int startIndex = Arrays.binarySearch(points, startKey);
		int endIndex = Arrays.binarySearch(points,endKey);
		if (startIndex < 0) startIndex = (startIndex+1)*(-1);
		if (endIndex < 0) endIndex = (endIndex+1)*(-1);
		int[] result = new int[endIndex-startIndex];
		for (int i=startIndex;i<endIndex;i++) {
			result[i-startIndex]=points[i] % X_SHIFT;
		}
		Arrays.sort(result);
		return result;

	}
	
	/**
	 * Returns a representation of the grid's contents in the smallest number of 
	 * characters. The format is <code>x1.y1,x2.y2,....xN.yN</code>. NOTE: This does
	 * NOT tell you the full width and height of the array, only the points
	 *
	 * @return a "thin" String representation of the grid.
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		for (int i=0;i<getCoordinates().length;i++) {
			short[] location = getLocation(getCoordinates()[i]);
			//location may be null if some coordinates are masked (see MaskedGrid)
			if (location !=null){
				buf.append(location[0]);
				buf.append('-');
				buf.append(location[1]);
				buf.append(' ');
			}
		}
		if(buf.length() > 0) buf.deleteCharAt(buf.length()-1);
		return buf.toString();
	}
	
	
	
	
	/** Prints an ASCII graph of the grid.
	  */
	public String toAsciiGraph() {
		StringBuffer buffer = new StringBuffer();
		boolean[][] array = generateBooleanArray();
		if(array.length == 0) return "";
		
		for(int y = 0; y < array[0].length; y++) {
			buffer.append("|");
			for(int x = 0; x < array.length; x++) {
				if(array[x][y]) {
					buffer.append("XX");
				} else {
					buffer.append("  ");
				}
				buffer.append("|");
			}
			buffer.append("\n");
		}
		return buffer.toString();
	}

	//===============================================================
	// Protected 
	//===============================================================

	//===============================================================
	// Methods
	//===============================================================


	/**
	 * Called by the constructor to load a set of coordinates
	 */
	protected void initializeCoordinates(Collection<Coordinate> coordinates) {
		Iterator<Coordinate> it = coordinates.iterator();
		this.coordinates = new short[coordinates.size()];
		this.transposedCoordinates = new short[coordinates.size()];
		int index=0;
		while (it.hasNext()) {
			Coordinate coordinate = it.next();
			this.coordinates[index]=getKey(coordinate.x,coordinate.y);
			this.transposedCoordinates[index]=getKey(coordinate.y,coordinate.x);
			index++;
		}
		Arrays.sort(this.coordinates);
		Arrays.sort(this.transposedCoordinates);
	}




	/**
	 * generates the short value stored for a given x,y pair
	 */
	protected short getKey(int x, int y) {
		int key = x*X_SHIFT+y;
		return (short)key;
	}
	
	
	/**
	 * generates the location of a coordinate from a key
	 * @return the coordinate from the key, or null if coordinate would be invalid (used in MaskedGrid)
	 */
	protected short[] getLocation(short key) {
		short[] location = new short[2];
		location[0] = (short)(key / X_SHIFT);
		location[1] = (short)(key % X_SHIFT);
		return location;
	}	

	protected short[] getCoordinates() {
		if (transposed) return transposedCoordinates;
		else return coordinates;
	}
	
	protected short[] getTransposedCoordinates() {
		if (transposed) return coordinates;
		else return transposedCoordinates;
	}
}
