/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.util.sentence;

// Imports

/**
 * Span is the class that keeps track of the starting points and offsets of 
 * phrases extracted from an alignment.
 *
 * @author Chris Callison-Burch
 * @since  25 September 2005
 * @version $LastChangedDate$
 */
public class Span implements Comparable {

//===============================================================
// Constants
//===============================================================

//===============================================================
// Member variables
//===============================================================

	protected int sourceStart;
	protected int sourceEnd;
	protected int targetStart;
	protected int targetEnd;
	protected int sourceOffset;
	protected int targetOffset;
 
//===============================================================
// Constructor(s)
//===============================================================

	public Span(int sourceStart, int sourceEnd, int targetStart, int targetEnd, int sourceOffset, int targetOffset) {
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
		this.targetStart = targetStart;
		this.targetEnd = targetEnd;
		this.sourceOffset = sourceOffset;
		this.targetOffset = targetOffset;
	}
	
	public Span(int sourceStart, int sourceEnd, int targetStart, int targetEnd) {
		this(sourceStart, sourceEnd, targetStart, targetEnd, 0, 0);
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

	public int getSourceStart() {
		return sourceStart;
	}

	public int getSourceEnd() {
		return sourceEnd;
	}

	public int getSourceOffset() {
		return sourceOffset;
	}

	public int getTargetStart() {
		return targetStart;
	}

	public int getTargetEnd() {
		return targetEnd;
	}

	public int getTargetOffset() {
		return targetOffset;
	}
	
	/**
	 * Sort to order spans along source
	 * @param o
	 * @return
	 * @throws ClassCastException
	 */
	public int compareTo(Object o) throws ClassCastException {
		Span c = (Span) o; // If this doesn't work, ClassCastException is thrown
		if (sourceStart == c.sourceStart){
			return 0;
		}
		else {
			if (sourceStart > c.sourceStart) {
				return 1;
			}
		}
		return -1;
	}	



	/**
	 * @return a hashCode value 
	 */
	public int hashCode() {
		return sourceStart * sourceEnd * targetStart * targetEnd;
	}
	
	
	public boolean equals(Object o) {
		Span c = (Span) o;
		if (c.sourceStart == sourceStart &&  c.sourceEnd == sourceEnd &&
			c.targetStart == targetStart && c.targetEnd == targetEnd) {
				return true;
		} else {
			return false;
		}
	}
	
	
	public String toString() {
		return "[" + sourceStart + "-" + sourceEnd + ", " + targetStart + "-" + targetEnd + "]"; 
	}
//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================



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

	public static void main(String[] args)
	{

	}
}

