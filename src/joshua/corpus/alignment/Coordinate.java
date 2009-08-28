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

/**
 * Simple data object class containing and x and y coordinate.
 * 
 * @author Josh Schroeder
 * @since 2 July 2003
 * @author Lane Schwartz
 * @since 15 Dec 2008
 */
public class Coordinate implements Comparable<Coordinate> {

	/**
	 * X value of the coordinate.
	 * <p>
	 * The scope of this field is package-private so that it
	 * can be efficiently accessed by {@link AlignmentGrid}
	 */
	final int x;
	
	/**
	 * Y value of the coordinate.
	 * <p>
	 * The scope of this field is package-private so that it
	 * can be efficiently accessed by {@link AlignmentGrid}
	 */
	final int y;

	/**
	 * Constructs a coordinate with the specified x and y values.
	 * 
	 * @param x X value of the coordinate
	 * @param y Y value of the coordinate
	 */
	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructs a coordinate from a String in the format
	 * "X.Y", or in the format "X-Y"
	 * 
	 * @param coordinate String in the format "X.Y", or in the
	 *                   format "X-Y"
	 */
	public Coordinate(String coordinate) {
		String points[] = coordinate.split("\\.|-");
		this.x = Integer.parseInt(points[0]);
		this.y = Integer.parseInt(points[1]);
	}

	/**
	 * Equal if a coordinate's X and Y are the same.
	 * 
	 * @param o the Object to test for equality
	 * @return true if the object is a coordinate with equal X
	 *         and Y
	 */
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!o.getClass().isInstance(this)) {
			return false;
		} else {
			Coordinate other = (Coordinate) o;
			return (other.x == this.x && other.y == this.y);
		}
	}

	/**
	 * Generates this objects integer hash code. Since we may
	 * transpose objects within Sets, we need their hashcode
	 * to stay the same after transposition. To do this, we
	 * take the smaller of x or y and multiply it by 100, then
	 * add it to the larger value. This is used as opposed to
	 * a more complex and unique calculation since most coordinates
	 * we will work with will be less than 100.
	 * 
	 * @return int a unique hashcode for this object
	 */
	public int hashCode() {
		if (x <= y) {
			return x * 100 + y;
		} else {
			return y * 100 + x;
		}
	}

	/**
	 * Returns a string representation of this coordinate, of
	 * the form (x,y).
	 * 
	 * @return string representation of the coordinate
	 */
	public String toString() {
		return ("(" + x + "," + y + ")");
	}

	/**
	 * Comparison for coordinates relies on X, then Y. If the
	 * X values are not the same, Y values are used.
	 * 
	 * @return -1 if this object is ranked lower than the
	 *         comparison object; +1 if this obejct is ranked
	 *         higher; zero if they are the same.
	 */
	public int compareTo(Coordinate location) {
		if (this.x < location.x || this.y < location.y) {
			return -1;
		} else if (this.x > location.x || this.y > location.y) {
			return 1;
		} else {
			return 0;
		}
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

}
