package edu.jhu.util.sentence;

/**
 * Simple data object class containing and x and y coordinate.
 *
 * @author Josh Schroeder
 * @since  2 July 2003
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
public class Coordinate implements Comparable {
    
    public int x;
    public int y;
    
    /**
     * Default constructor takes in an x and y.
     * @param x the X coordinate
     * @param y the Y coordinate
     */
    public Coordinate (int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * This constructor creates a duplicate of the coordinate passed to it.
     * @param c the coordinate to copy x and y information from.
     */
    public Coordinate (Coordinate c) {
        this.x = c.x;
        this.y = c.y;
    }
    
    /**
     * Switches the X and Y values. Useful when "rotating" a grid.
     */
    public void transpose() {
        int oldX = this.x;
        this.x = this.y;
        this.y = oldX;
    }
    
    /**
     * Equal if a coordinate's X and Y are the same.
     * @param o the Object to test for equality
     * @return true if the object is a coordinate with equal X and Y
     */
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!o.getClass().isInstance(this)) return false;
        else {
            Coordinate other = (Coordinate)o;
            return (other.x==this.x && other.y==this.y);
        }
    }
    
    /**
     * Generates this objects integer hash code. Since we may transpose 
     * objects within Sets, we need their hashcode to stay the same after
     * transposition. To do this, we take the smaller of x or y and
     * multiply it by 100, then add it to the larger value. This is used
     * as opposed to a more complex and unique calculation since most 
     * coordinates we will work with will be less than 100.
     * @return int a unique hashcode for this object
     */
    public int hashCode() {
        if (x<=y)
            return x*100+y;
        else
            return y*100+x;
    }
    
    /**
     * Returns a string representation of this coordinate, of the form (x,y).
     * @return string representation of the coordinate
     */
    public String toString() {
        return ("("+x+","+y+")");
    }
	
	/**
	 * Comparison for coordinates relies on X, then Y. If the X values are not the
	 * same, Y values are used.
	 * @return -1 if this object is ranked lower than the comparison object;
     * +1 if this obejct is ranked higher; zero if they are the same.
     * @exception ClassCastException iff the object is not of (sub)type Alignment.
     */
	public int compareTo(Object o)  throws ClassCastException {
        Coordinate location = (Coordinate) o;
		if (this.x < location.x) return -1;
		else if (this.x > location.x) return 1;
		else if (this.y < location.y) return -1;
		else if (this.y > location.y) return 1;
		else return 0;
	}
	
	/**
	 * @param other the other coordinate
	 * @return true if the X values of the coordinates are equal
	 */
	public boolean sameColumn(Coordinate other) {
		return (this.x == other.x);
	}
	
	/**
	 * @param other the other coordinate
	 * @return true if the Y values of the coordinates are equal
	 */
	public boolean sameRow(Coordinate other) {
		return (this.y == other.y);
	}
	
	/**
	 * Static method to create a coordinate from a String in the format "X.Y",
	 * or in the format "X-Y"
	 * @param coordinate the String
	 * @return a new coordinate object
	 */
	public static Coordinate toCoordinate(String coordinate) {
		String points[] = coordinate.split("\\.|-");
		int x = Integer.parseInt(points[0]);
		int y = Integer.parseInt(points[1]);
		return new Coordinate(x,y);
	}


}
        