package edu.jhu.util.sentence;

import java.util.*;

/**
 * Representation of a 2-dimensional grid. This implementation is designed to 
 * be as memory-efficient as possible for storing many grids in memory. Most JVMs
 * use 32bit ints to store byte, short, and boolean individual primitives, but will actually
 * make efficient use of memory when these small primitives are stored in arrays. Therefore,
 * we create a single sorted array of shorts, where both the X and Y coordinate for a given 
 * "true" point are the grid are encoded in one short.
 *
 * @author Josh Schroeder
 * @since 09 Dec 2004
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
public class Grid {


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
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor takes in the size of the Grid to create and a set of coordinates
	 * @param width the width (X-value limit) of the grid
	 * @param height the height (Y-value limit) of the grid
	 * @param coordinates a Set of Coordinate objects representing all the points to be added
	 */
	public Grid(int width, int height, Collection coordinates) {
		this.width = width;
		this.height = height;
		initializeCoordinates(coordinates);
	}
	

	/**
	 * Constructor takes in the size of the Grid to create and a 2d array of booleans
	 * @param width the width (X-value limit) of the grid
	 * @param height the height (Y-value limit) of the grid
	 * @param array a 2d collection of booleans representing all the points to be added
	 */
	public Grid(boolean[][] array) {
		this.width = array.length;
		this.height = array[0].length;
		initializeCoordinates(array);
	}
	
	/**
	 * Constructor takes the dimensions of the Grid, and creates an initially empty
	 * grid.
	 * @param width the width (X-value limit) of the grid
	 * @param height the height (Y-value limit) of the grid
	 */
	public Grid(int width, int height) {
		this(width, height, false);
	}

	/**
	 * Constructor takes the dimensions of the Grid, and creates either an entirely
	 
	 * @param width the width (X-value limit) of the grid
	 * @param height the height (Y-value limit) of the grid
	 * @param fill true if the grid should be filled, false if it should be empty
	 */
	public Grid(int width, int height, boolean fill) {
		this.width = width;
		this.height = height;
		
		if(fill) {
			// create a filled grid
			initializeCoordinates(width, height);
		} else {
			// create an empty grid
			initializeCoordinates(new HashSet());
		}
	}
	
	/**
	 * Constructor takes the small string representation of alignment points.
	 * @param alignmentPoints the string representation of alignments.
	 */
	public Grid(String alignmentPoints) {
		HashSet coordinates = new HashSet();
		String[] alignmentPointsArray = alignmentPoints.split(",|\\s");
		for(int i = 0; i < alignmentPointsArray.length; i++) {
			if(!alignmentPointsArray[i].trim().equals("")) {
				Coordinate coord = Coordinate.toCoordinate(alignmentPointsArray[i]);
				width = Math.max(width, coord.x+1);
				height = Math.max(height, coord.y+1);
				coordinates.add(coord);
			}
		}
		initializeCoordinates(coordinates);
	}
	
	/**
	 * Constructor takes the small string representation of alignment points.
	 * @param alignmentPoints the string representation of alignments.
	 */
	public Grid(int width, int height, String alignmentPoints) {
		this.width = width;
		this.height = height;
		HashSet coordinates = new HashSet();
		String[] alignmentPointsArray = alignmentPoints.split(",|\\s");
		for(int i = 0; i < alignmentPointsArray.length; i++) {
			if(!alignmentPointsArray[i].trim().equals("")) {
				Coordinate coord = Coordinate.toCoordinate(alignmentPointsArray[i]);
				coordinates.add(coord);
			}
		}
		initializeCoordinates(coordinates);
	}
	
	/**
	 * This protected constructor is used in the initalization of the
	 * MaskedGrid subclass.
	 */
	protected Grid(Grid fullGrid) {
		this.coordinates = fullGrid.coordinates;
		this.transposedCoordinates = fullGrid.transposedCoordinates;
		this.transposed = fullGrid.transposed;
		
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================

	/**
	 * @return the number of points in this grid
	 */
	public int cardinality() {
		return coordinates.length;
	}
	
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
		Grid other = (Grid)o;
		return (this.getWidth()==other.getWidth() &&
				this.getHeight()==other.getHeight() &&
				Arrays.equals(this.getCoordinates(),other.getCoordinates()));
	}
	
	/**
	 * Checks if the given location is occupied in this grid.
	 * @param location the coordinate location to check for.
	 * @return true if the coordinate is within the bounds of the Grid and exists (is set to true)
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
	 */
	public Collection generateCoordinates() {
		HashSet coordinates = new HashSet();
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
	 * direction is faster than the other
	 */
	public void transpose() {
		transposed = !transposed;
	}
	
	/**
	 * Returns a sorted list of the row points that are items for the column span.
	 * @param columnEnd is exclusive
	 */
	public int[] getRowPoints(int columnStart, int columnEnd) {
		return getPoints(columnStart, columnEnd, getHeight(), getCoordinates());
	}
	

	/**
	 * Returns a sorted list of the column points that are items for the row span.
	 * @param rowEnd is exclusive
	 */
	public int[] getColumnPoints (int rowStart, int rowEnd) {
		return getPoints(rowStart, rowEnd, getWidth(), getTransposedCoordinates());
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
	
//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================
	
	
	/**
	 * Called by the constructor to load a set of coordinates
	 */
	protected void initializeCoordinates(Collection coordinates) {
		Iterator it = coordinates.iterator();
		this.coordinates = new short[coordinates.size()];
		this.transposedCoordinates = new short[coordinates.size()];
		int index=0;
		while (it.hasNext()) {
			Coordinate coordinate = (Coordinate)it.next();
			this.coordinates[index]=getKey(coordinate.x,coordinate.y);
			this.transposedCoordinates[index]=getKey(coordinate.y,coordinate.x);
			index++;
		}
		Arrays.sort(this.coordinates);
		Arrays.sort(this.transposedCoordinates);
	}
	
	
	/**
	 * Called by the constructor to load a set of coordinates
	 */
	protected void initializeCoordinates(boolean[][] array) {
		ArrayList coordinates = new ArrayList();
		for (int x=0;x<width;x++){
			for (int y=0;y<height;y++){
				if (array[x][y]) {
					Coordinate coordinate = new Coordinate(x,y);
					coordinates.add(coordinate);
				}
			}
		}
		initializeCoordinates(coordinates);
	}
	
	
	/**
	 * Called by the constructor to create an entirely filled 
	 * grid.  Note that this is not a desirable case, because
	 * this data structure is designed to be memory compact for
	 * sparse grids.     
	 */
	protected void initializeCoordinates(int width, int height) {
		this.coordinates = new short[width*height];
		this.transposedCoordinates = new short[width*height];
		
		int index = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				this.coordinates[index]=getKey(x,y);
				this.transposedCoordinates[index]=getKey(y,x);
				index++;
			}
		}
		
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

	protected short[] getCoordinates() {
		if (transposed) return transposedCoordinates;
		else return coordinates;
	}
	
	protected short[] getTransposedCoordinates() {
		if (transposed) return coordinates;
		else return transposedCoordinates;
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

	/** Returns a new grid extends this grid by the specified grid.  
	  * The width of the new grid is the width of this grid plus the
	  * width of the specified grid, and the same for the height.
	  * @param grid the grid to append to this one.
	  * @return a new Grid
	  */
	public static Grid extend(Grid grid1, Grid grid2) {
		int width = grid1.getWidth() + grid2.getWidth();
		int height = grid1.getHeight() + grid2.getHeight();

		ArrayList coordinates = new ArrayList();

		short[] points = grid1.getCoordinates();
		for(int i = 0; i < points.length; i++) {
			short[] coord = grid1.getLocation(points[i]);
			if(coord !=null) coordinates.add(new Coordinate(coord[0],coord[1]));
		}
		
		int xOffset = grid1.getWidth();
		int yOffset = grid1.getHeight();
		points = grid2.getCoordinates();
		for(int i = 0; i < points.length; i++) {
			short[] coord = grid2.getLocation(points[i]);
			if(coord !=null) coordinates.add(new Coordinate(coord[0]+xOffset, coord[1]+yOffset));
		}
													
		return new Grid(width, height, coordinates);	
	}
	
	
	/** A version of extend which allows for more flexibility in how the 
	  * grid is extended.
	  */
	protected static Grid extend(int width, int height, 
								Grid grid1, int xOffset1, int yOffset1,
								Grid grid2, int xOffset2, int yOffset2) {
		ArrayList coordinates = new ArrayList();
		short[] points = grid1.getCoordinates();
		for(int i = 0; i < points.length; i++) {
			short[] coord = grid1.getLocation(points[i]);
			if(coord !=null) coordinates.add(new Coordinate(coord[0]+xOffset1, coord[1]+yOffset1));
		}
		
		points = grid2.getCoordinates();
		for(int i = 0; i < points.length; i++) {
			short[] coord = grid2.getLocation(points[i]);
			if(coord !=null) coordinates.add(new Coordinate(coord[0]+xOffset2, coord[1]+yOffset2));
		}
													
		return new Grid(width, height, coordinates);									
	}

  
	/**
	 * Creates the intersection of two grids with identical dimensions.
	 */
	public static Grid intersection(Grid grid1, Grid grid2) {
		boolean[][] array1 = grid1.generateBooleanArray();
		boolean[][] array2 = grid2.generateBooleanArray();
		
		int width = grid1.getWidth();
		int height = grid1.getHeight();
		boolean[][] intersection = new boolean[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				intersection[x][y] = array1[x][y] && array2[x][y];
			}
		}
		return new Grid(intersection);	
	}
	
	/**
	 * Creates the union of two grids with identical dimensions.
	 */
	public static Grid union(Grid grid1, Grid grid2) {
		boolean[][] array1 = grid1.generateBooleanArray();
		boolean[][] array2 = grid2.generateBooleanArray();
		
		int width = grid1.getWidth();
		int height = grid1.getHeight();
		boolean[][] union = new boolean[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				union[x][y] = array1[x][y] || array2[x][y];
			}
		}
		return new Grid(union);	
	}
  


	/**
	 * Creates a new grid containing the delta of two grids. The delta is defined
	 * as the the set of points in one of the two grids but not both.
	 * Note that the underlying coordinates will be shared with the original grids
	 * to conserve memory.
	 * @param g1
	 * @param g2
	 * @return Grid a new Grid containing those points in the union but not the intersection
	 */
	public static Grid delta(Grid grid1, Grid grid2) {
		boolean[][] union = union(grid1, grid2).generateBooleanArray();
		boolean[][] intersection = intersection(grid1, grid2).generateBooleanArray();
		
		int width = grid1.getWidth();
		int height = grid1.getHeight();
		boolean[][] difference = new boolean[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				difference[x][y] = union[x][y] && !intersection[x][y];
			}
		}
		return new Grid(difference);	
	}
	
	
	
	
	/**
	 * Checks if a given row in the array is empty (contains no
	 * true values).
	 * @param y the row to check
	 * @param array the array of points
	 * @return true if array[*][y] is all false
	 */
	public static boolean rowEmpty(int y, boolean[][] array) {
		boolean empty=true;
		for(int x=0;x<array.length;x++) {
			empty = empty && !array[x][y];
			if (!empty) return false;
		}
		return true;
	}
	
	/**
	 * Checks if a given column in the array is empty (contains no
	 * true values).
	 * @param x the column to check
	 * @param array the array of points
	 * @return true if array[x][*] is all false
	 */
	public static boolean columnEmpty(int x, boolean[][] array) {
		boolean empty=true;
		for(int y=0;y<array[x].length;y++) {
			empty = empty && !array[x][y];
			if (!empty) return false;
		}
		return true;
	}
	
	
	
  /**
     * Utility method for checking around a point in a 2-d array to see
     * if the point itself or any of the nearby points have both horizontal
     * and vertical neighbors. Checks the area from x-1 to x+1 and y-1 to y+1
     * (9 points total)
     * @param x the x coordinate
     * @param y the y coordinate
     * @param points the boolean array of points
     * @return true if any of the points in the 9-element area surrounding x,y have both horizontal and vertical neighbors
     */
    static public boolean generatesBothNeighbors(int x, int y, boolean[][] points) {
        if (hasBothNeighbors(x,y,points)) return true;
        for (int i=x-1;i<=x+1;i++) {
            for (int j=y-1;j<y+1;j++) {
                if (inBounds(i,j,points)) {
                    if (hasBothNeighbors(i,j,points)) return true;
                }
            }
        }
        return false;           
    }
    
    /**
     * Checks if (x,y) has a horizonal or vertical neighbor in array[][].
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x,y) has a horizontal or vertical neighbor
     */
    static public boolean hasNeighbor(int x, int y, boolean[][] array) {
        return (hasVerticalNeighbor(x,y,array) || hasHorizontalNeighbor(x,y,array)
				|| hasDiagonalNeighbor(x,y,array)
				);
    }
    
    /**
     * Checks if (x,y) has a horizonal and vertical neighbor in array[][].
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x,y) has a horizontal and vertical neighbor
     */
    static public boolean hasBothNeighbors(int x, int y, boolean[][] array) {
        return (hasVerticalNeighbor(x,y,array) && hasHorizontalNeighbor(x,y,array));
    }    
    
    /**
     * Checks if (x,y) has a vertical neighbor in array[][]. 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if either (x, y-1) or (x, y+1) are in bounds and true
     */
    static public boolean hasVerticalNeighbor(int x, int y, boolean[][] array) {
        return (hasAboveNeighbor(x, y, array) || hasBelowNeighbor(x, y, array));
    }
    
    /**
     * Checks if (x,y) has a horizontal neighbor in array[][]. 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if either (x-1,y) or (x+1,y) are in bounds and true
     */    
    static public boolean hasHorizontalNeighbor(int x, int y, boolean[][] array) {
        return (hasLeftNeighbor(x, y, array) || hasRightNeighbor(x, y, array));
    }
    
    /**
     * Checks if (x,y) has a diagonal neighbor in array[][]. 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if any of {(x-1,y-1), (x+1,y-1), (x+1, y+1), (x-1,y+1)} are in bounds and true
     */    
    static public boolean hasDiagonalNeighbor(int x, int y, boolean[][] array) {
        for (int i=x-1;i<=x+1;i++) {
            for (int j=y-1;j<=y+1;j++) {
                if (i!=x && j!=y && inBounds(i,j,array)) {
                    if (array[i][j]) return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if (x,y) is within the bounds of the array.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if x and y are >=0 and less than the width and height (respectively) of the array
     */
    static public boolean inBounds(int x, int y, boolean[][] array) {
            return (x>=0 &&
                y>=0 &&
                x<array.length &&
                y<array[x].length);
    }
	
	/**
     * Checks if (x+1,y) is within the bounds of the array and true.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x+1,y) is true and in-bounds
     */
	static public boolean hasRightNeighbor(int x, int y, boolean[][] array) {
		if (x+1<array.length) {
            return array[x+1][y];
        }
        return false;
	}
	
	/**
     * Checks if (x-1,y) is within the bounds of the array and true.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x-1,y) is true and in-bounds
     */
	static public boolean hasLeftNeighbor(int x, int y, boolean[][] array) {
		if (x-1>=0) {
            return array[x-1][y];
        }
		return false;
	}
	
	/**
     * Checks if (x,y-1) is within the bounds of the array and true.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x,y-1) is true and in-bounds
     */
	static public boolean hasAboveNeighbor(int x, int y, boolean[][] array) {
		if (y-1>=0) {
            return array[x][y-1];
        }
		return false;
	}
	
	/**
     * Checks if (x,y+1) is within the bounds of the array and true.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x,y+1) is true and in-bounds
     */
	static public boolean hasBelowNeighbor(int x, int y, boolean[][] array) {
		if (y+1<array[x].length) {
            return array[x][y+1];
        }
        return false;
	}
	
	/**
     * Checks if (x+1,y+1) is within the bounds of the array and true.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x+1,y+1) is true and in-bounds
     */
	static public boolean hasBelowRightNeighbor(int x, int y, boolean[][] array) {
		if(x+1<array.length && y+1<array[x+1].length) {
			return array[x+1][y+1];
		}
		return false;
	}
	
	/**
     * Checks if (x,y) is the upper left corner of a rectangle, meaning that
	 * it has a right neighbor, a below neighbor, and a below-right diagonal neighbor.
     * @param x the x coordinate
     * @param y the y coordinate
     * @param array the boolean array of points
     * @return true if (x,y) has true points below, right, and below-right of it.
     */
	static public boolean isRectangleUpperLeft(int x, int y, boolean[][] array) {
		return (hasRightNeighbor(x, y, array) &&
				hasBelowNeighbor(x, y, array) &&
				hasBelowRightNeighbor(x, y, array));
	}
	

	
	
	

//===============================================================
// Main 
//===============================================================

	static public void main(String[] args) {
		ArrayList coords = new ArrayList();
		coords.add(new Coordinate(0,0));
		coords.add(new Coordinate(1,1));
		coords.add(new Coordinate(2,1));
		Grid grid = new Grid(3,3,coords);

		Grid grid2 = new Grid(3, 3, "0.0,1.1,2.1");
		Grid grid3 = new Grid(3, 3, "0-0 1-1 2-1");
		
		System.out.println(grid.toAsciiGraph());
		System.out.println(grid2.toAsciiGraph());
		System.out.println(grid3.toAsciiGraph());
		
	}
	
}
