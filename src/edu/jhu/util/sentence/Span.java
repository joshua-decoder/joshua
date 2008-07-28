package edu.jhu.util.sentence;

// Imports

/**
 * Span is the class that keeps track of the starting points and offsets of 
 * phrases extracted from an alignment.
 *
 * @author Chris Callison-Burch
 * @since  25 September 2005
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
 *
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

