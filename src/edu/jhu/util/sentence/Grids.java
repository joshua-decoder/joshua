package edu.jhu.util.sentence;

// Imports
import java.util.ArrayList;

/**
 * Grids is a class that provides access to a list of Grids objects.  It 
 * mainly acts as an interface for the GridServer / RmiGrids, but we have
 * also provided a simple implementation here that as a wrapper around an 
 * an ArrayList for less advance tasks.
 *
 * @author Chris Callison-Burch
 * @since  10 October 2005
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
public class Grids {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================

	private ArrayList grids;
	
	private boolean transpose;
 
//===============================================================
// Constructor(s)
//===============================================================

	public Grids(ArrayList grids, boolean transpose) {
		this.grids = grids;
		this.transpose = transpose;
	}

	/** Creates a transposed set of Grids from the given set. */
	public Grids(Grids other) {
		this.grids = other.grids;
		this.transpose = !other.transpose;
	 } 
	
	// this protected constructor is provided so that RmiGrids
	// can extend this class.
	protected Grids() {
	
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	public Grid getGrid(int i) {
		Grid grid = (Grid) grids.get(i);
		if(grid == null) return null;
		// ccb - todo - this method might not be safe if we're having multiple threads accessing
		// the same set of alignments...
		if(transpose != grid.isTransposed()) grid.transpose();
		return grid;
	}
	
	public int size() {
		return grids.size();
	}
	
	public void setGrid(int i, Grid grid) {
		grids.set(i, grid);
	}
	
	public boolean isTransposed() {
		return transpose;
	}
	
	//===========================================================
	// Methods
	//===========================================================

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

