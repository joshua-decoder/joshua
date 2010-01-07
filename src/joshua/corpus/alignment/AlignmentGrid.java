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
package joshua.corpus.alignment;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Representation of a 2-dimensional grid. This implementation is
 * designed to be as memory-efficient as possible for storing many
 * grids in memory. Most JVMs use 32bit ints to store byte, short,
 * and boolean individual primitives, but will actually make efficient
 * use of memory when these small primitives are stored in arrays.
 * Therefore, we create a single sorted array of shorts, where both
 * the X and Y coordinate for a given "true" point are the grid are
 * encoded in one short.
 *
 * @author Josh Schroeder
 * @since  09 Dec 2004
 * @author Lane Schwartz
 * @since  15 Dec 2008
 */
public class AlignmentGrid implements Externalizable {

	//===============================================================
	// Constants
	//===============================================================

	/**
	 * Maximum size of a dimension.
	 */
	static public final int MAX_LENGTH = 100;

	/**
	 * Constant used for generating coordinate short value.
	 */
	static private final short X_SHIFT = 100;

	//===============================================================
	// Member variables
	//===============================================================

	/** Width of the grid. */
	protected int width;
	
	/** Height of the grid. */
	protected int height;
	
	/** 
	 * Array of alignment points, encoded as shorts.
	 * 
	 * @see #getKey(int, int)
	 * @see #getLocation(short)
	 */
	protected short[] coordinates;
	
	/** 
	 * Array of reverse alignment points, encoded as shorts.
	 * 
	 * @see #getKey(int, int)
	 * @see #getLocation(short)
	 */	
	protected short[] transposedCoordinates;

	/**
	 * Constructor takes the small string representation of
	 * alignment points.
	 * 
	 * @param alignmentPoints the string representation of
	 *                        alignments.
	 */
	public AlignmentGrid(String alignmentPoints) {
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>();
		String[] alignmentPointsArray = alignmentPoints.split(",|\\s");
		for (int i = 0; i < alignmentPointsArray.length; i++) {
			if (!alignmentPointsArray[i].trim().equals("")) {
				Coordinate coord = new Coordinate(alignmentPointsArray[i]);
				width = Math.max(width, coord.x+1);
				height = Math.max(height, coord.y+1);
				if (width>MAX_LENGTH || height>MAX_LENGTH) {
					throw new RuntimeException("Encountered alignment point " + coord + " which exceeds the maximum that can be represented " + new Coordinate(MAX_LENGTH-1, MAX_LENGTH-1) + ". Please ensure that each training sentence contains fewer than " + MAX_LENGTH + " words.");
				}
				coordinates.add(coord);
			}
		}
		initializeCoordinates(coordinates);
	}

	/**
	 * Constructs a completely empty, utterly uninitialized
	 * alignment grid, containing <em>absolutely nothing</em>.
	 * <p>
	 * This constructor only exists to allow this class to be
	 * properly <code>Externalizable</code>.
	 */
	public AlignmentGrid() {
		// This method intentionally left blank.
	}
	
	//===============================================================
	// Public
	//===============================================================

	//===========================================================
	// Accessor methods (set/get)
	//===========================================================


	/**
	 * Gets the width of this object.
	 * 
	 * @return the width (X size) of the Grid
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Gets the height of this object.
	 * 
	 * @return the height (Y size) of the Grid
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Checks if a coordinate's values fall within the bounds
	 * of the Grid. DOES NOT check for the existance of the
	 * coordinate in the grid. Use contains for that purpose.
	 * 
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
	 * Compares this object to another. If it is also a grid,
	 * first checks height and width compatibility, then checks
	 * that all coordinates are equal.
	 * 
	 * @param o object to comare to
	 * @return <code>true</code> if o is a Grid of the same
	 *         size and containing the same points as this one,
	 *         <code>false</code> otherwise
	 */
	public boolean equals(Object o) {
		if (this==o) {
			return true;
		} else if (o instanceof AlignmentGrid) {
			AlignmentGrid other = (AlignmentGrid)o;
			return (this.getWidth()==other.getWidth() &&
					this.getHeight()==other.getHeight() &&
					Arrays.equals(this.getCoordinates(),other.getCoordinates()));
		} else {
			return false;
		}		
	}

	/* See Javadoc for java.lang.Object#hashCode */
	public int hashCode() {
		
		return 
			Arrays.hashCode(this.getCoordinates()) +
			this.getHeight()*31 +
			this.getWidth()*317;
		
	}
	
	
	/**
	 * Checks if a given location is present in the grid.
	 * 
	 * @param x the x value of the coordinate to check for
	 * @param y the y value of the coordinate to check for
	 * @return <code>true</code> if the specified coordinate
	 *         is in bounds and exists, <code>false</code>
	 *         otherwise
	 */
	public boolean contains(int x, int y) {
		if (isValid(x,y)) {
			int index = Arrays.binarySearch(getCoordinates(), getKey(x,y));
			//the index returned by a binarySearch is positive if the number exists
			return (index >= 0);
		} else {
			throw new ArrayIndexOutOfBoundsException("("+x+","+y+")");
		}
	}

	/**
	 * Exports the contents of this grid to a 2-d boolean array,
	 * of the size array[width][height]. Coordinates contained
	 * in this grid will be set to true, all others false.
	 * 
	 * @return a 2-d boolean array representation of this grid
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
	 * Returns a sorted list (includes any duplicates) of the
	 * target language indices that align with the given source
	 * language span.
	 * 
	 * @param sourceSpanStart Inclusive start index
	 *                        into the source language sentence
	 * @param sourceSpanEnd Exclusive end index into
	 *                      the source language sentence.
	 *                      
	 * @return a sorted list (includes any duplicates) of the
	 *         target language indices that align with the given
	 *         source language span
	 */
	public int[] getTargetPoints(int sourceSpanStart, int sourceSpanEnd) {
		return getPoints(sourceSpanStart, sourceSpanEnd, getHeight(), getCoordinates());
	}

	/**
	 * Returns a sorted list (includes any duplicates) of the
	 * source language indices that align with the given target
	 * language span.
	 *
	 * @param targetSpanStart Inclusive start index
	 *                        into the target language sentence.
	 * @param targetSpanEnd Exclusive end index into
	 *                      the target language sentence.
	 *                     
	 * @return a sorted list (includes any duplicates) of the
	 *         source language indices that align with the given
	 *         target language span.
	 */
	public int[] getSourcePoints (int targetSpanStart, int targetSpanEnd) {
		return getPoints(targetSpanStart, targetSpanEnd, getWidth(), getTransposedCoordinates());
	}


	/**
	 * Returns a sorted list (includes any duplicates) of
	 * alignment indices for the given span, constructed from
	 * the provided array of encoded points.
	 *
	 * @param start Inclusive start index
	 * @param end Exclusive end index
	 * @param maxKey Maximum allowed coordinate value
	 * @param points Encoded alignment points
	 *
	 * @return a sorted list (includes any duplicates) of
	 *         alignment indices for the given span, constructed
	 *         from the provided array of encoded points.
	 */
	public static int[] getPoints(int start, int end, int maxKey, short[] points) {
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
	 * Gets a String representation of the grid's contents in
	 * the smallest number of characters.
	 * 
	 * The format is <code>x1.y1,x2.y2,....xN.yN</code>.
	 * <em>Note</em>: The returned String does <em>not</em>
	 * imply the full width and height of the array, only the
	 * points contained in the grid.
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
	
	
	
	
	/** 
	 * Gets a String representation of the grid represented as
	 * an ASCII graph of the grid.
	 * 
	 * @return String displaying the grid as an ASCII graph
	 */
	public String toAsciiGraph() {
		StringBuffer buffer = new StringBuffer();
		boolean[][] array = generateBooleanArray();
		if(array.length == 0) return "";
		
		for(int y = 0; y < array[0].length; y++) {
			buffer.append('|');
			for(int x = 0; x < array.length; x++) {
				if(array[x][y]) {
					buffer.append("XX");
				} else {
					buffer.append("  ");
				}
				buffer.append('|');
			}
			buffer.append('\n');
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
	 * Called by the constructor to load a set of coordinates.
	 * 
	 * @param coordinates Coordinates to be used during initialization
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
	 * Gets an encoded short value for a given x,y pair.
	 * 
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @return an encoded short value for a given x,y pair.
	 */
	protected static short getKey(int x, int y) {
		int key = x*X_SHIFT+y;
		return (short)key;
	}
	
	
	/**
	 * Generates the location of a coordinate from a key.
	 * 
	 * @param key Encoded short value
	 * @return the coordinate from the key
	 */
	protected short[] getLocation(short key) {
		short[] location = new short[2];
		location[0] = (short)(key / X_SHIFT);
		location[1] = (short)(key % X_SHIFT);
		return location;
	}	

	/**
	 * Gets the encoded coordinates for this grid.
	 * 
	 * @return the encoded coordinates for this grid
	 */
	protected short[] getCoordinates() {
		return coordinates;
	}
	
	/**
	 * Gets the encoded reverse coordinates for this grid.
	 * 
	 * @return the encoded reverse coordinates for this grid
	 */
	protected short[] getTransposedCoordinates() {
		return transposedCoordinates;
	}

	/* See Javadoc for java.io.Externalizable interface. */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {

		// Read the width and height of the grid
		this.width = in.readInt();
		this.height = in.readInt();
		
		// Read the number of alignment points
		int numPoints = in.readInt();
		
		// Read the alignment points
		this.coordinates = new short[numPoints];
		for (int i=0; i<numPoints; i++) {
			coordinates[i] = in.readShort();
		}
		
		// Read the reverse alignment points
		this.transposedCoordinates = new short[numPoints];
		for (int i=0; i<numPoints; i++) {
			transposedCoordinates[i] = in.readShort();
		}
	}

	/* See Javadoc for java.io.Externalizable interface. */
	public void writeExternal(ObjectOutput out) throws IOException {
		
		// Write the width and height of the grid
		out.writeInt(width);
		out.writeInt(height);
		
		// Write the number of alignment points
		out.writeInt(coordinates.length);
		
		// Write the alignment points
		for (short point : coordinates) {
			out.writeShort(point);
		}
		
		// Write the reverse alignment points
		for (short point : transposedCoordinates) {
			out.writeShort(point);
		}
	}
}
