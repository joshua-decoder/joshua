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
import java.util.ArrayList;

/**
 * Grids is a class that provides access to a list of Grids objects.  It 
 * mainly acts as an interface for the GridServer / RmiGrids, but we have
 * also provided a simple implementation here that as a wrapper around an 
 * an ArrayList for less advance tasks.
 *
 * @author Chris Callison-Burch
 * @since  10 October 2005
 * @version $LastChangedDate$
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

