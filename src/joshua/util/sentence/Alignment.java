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
package joshua.util.sentence;

import joshua.util.sentence.phrase_extraction.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;

/**
 * An alignment is composed of source phrase, target phrase,
 * and a word-level alignment between the two.  
 *
 * @author Chris Callison-Burch
 * @since  31 January 2005
 * @version $LastChangedDate$
 */
public class Alignment {

//===============================================================
// Constants
//===============================================================

	/**
	 * seed used in hash code generation
	 */
	public static final int HASH_SEED = 17;
	
	/**
	 * offset used in has code generation
	 */
	public static final int HASH_OFFSET = 37;


//===============================================================
// Member variables
//===============================================================

	protected Phrase source;
	protected Phrase target;
	
	/**
	 * The grid of alignment points. X axis (columns) is source,
	 * Y axis (rows) is the target.
	 */
	protected Grid grid;
	
	
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * Constructor that takes in already parsed alignment data.
	 *
	 * @param source the source sentence or phrase
	 * @param target  the target sentence or phrase
	 * @param grid a Grid representing the word-level alignment between the sentences.
	 */
	public Alignment(Phrase source, Phrase target, Grid grid) {
		this.source = source;
		this.target = target;
		this.grid = grid;
	}

	
	
//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/**
	 * @return the source Phrase
	 */
	public Phrase getSource() {
		return source;
	}
	
	
	/**
	 * @return the target Phrase
	 */
	public Phrase getTarget() {
		return target;
	}
	
	
	/**
	 * @return the number of words in the source sentence
	 */
	public int getSourceLength() {
		return source.size();
	}
	
	
	/**
	 * @return the number of words in the target sentence
	 */
	public int getTargetLength() {
		return target.size();
	}
	
	
	/**
	 * Sets the current alignment points stored with this phrase to the
	 * Grid.
	 *
	 * @param newAlignmentPoints the grid to use for this phrase
	 */
	public void setAlignmentPoints(Grid newAlignmentPoints) {
		grid = newAlignmentPoints;
	}
	
	
	/**
	 * Gets the current alignment points stored with this phrase.
	 *
	 * @return Grid the grid of alignment points.
	 */
	public Grid getAlignmentPoints() {
		return grid;
	}
	
	
	/**
	 * Returns a portion of this alignment deliniated by the
	 * span. Creates subphrases spanning the sourceStart-sourceEnd
	 * and targetStart-targetEnd indexes, and creates a MaskedGrid
	 * for the area. These operations are all reasonably compact
	 * in memory, since they reuse the superphrases and the larger
	 * grid.
	 */
	public Alignment getSubAlignment(Span span) {
		Phrase sourcePhrase = getSource().subPhrase(span.getSourceStart(), span.getSourceEnd());
		Phrase targetPhrase = getTarget().subPhrase(span.getTargetStart(), span.getTargetEnd());
		Grid subgrid = new MaskedGrid(grid, span.getSourceStart(), sourcePhrase.size(), span.getTargetStart(), targetPhrase.size());
		return new Alignment(sourcePhrase, targetPhrase, subgrid);
	}
	
	
	//===========================================================
	// Methods
	//===========================================================

	/**
	 * Transposes the Alignment. Source becomes target and vice versa,
	 * @see joshua.util.sentence.Grid#transpose()
	 */
	public void transpose() {
		Phrase oldSource = source;
		source = target;
		target = oldSource;
		grid.transpose();
	}


	/** 
	 * This method is used in decoding to incrementally build
	 * up larger alignments by expands the target phrase. The
	 * source sentence is given from the outset, so this alignment
	 * will aready have all the source words. We simply append
	 * the target phrase and integrate its alignments points at
	 * the appropriate offset.
	 */
	public Alignment expandTarget(Alignment expandedAlignment, int sourcePosition) {
		int targetLength = getTargetLength();
		
		// create the new phrase.
		target.append(expandedAlignment.getTarget());
		Phrase expandedTarget = this.target;
		
		// mask off the target phrase from the expanded one.
		this.target = expandedTarget.subPhrase(0, targetLength);
		expandedAlignment.target = expandedTarget.subPhrase(targetLength, expandedTarget.size());
		
		Grid expandedGrid = Grid.extend(source.size(), expandedTarget.size(), this.grid, 0, 0,
										expandedAlignment.grid, sourcePosition, targetLength);

		// ccb - todo - should probably mask off this grid.
										
		return new Alignment(this.source, expandedTarget, expandedGrid);
	}
	
	
	/**
	 * Two alignments are equal if they have the same source
	 * and target. Note that grid is not used in the calculation.
	 */
	public boolean equals(Object o) {
		if (o == null || !o.getClass().isInstance(this)) return false;
		Alignment other = (Alignment)o;
		return (this.source.equals(other.source) &&
					  this.target.equals(other.target));
	}
	
	
	/**
	 * Uses the standard java approach of calculating hashCode.
	 * Start with a seed, add in every value multiplying the
	 * exsiting hash times an offset.
	 *
	 *  @return int hashCode for the list
	 */
	public int hashCode() {
		int result = HASH_SEED;
		for (int i=0; i < source.size(); i++) {
			result = HASH_OFFSET*result + source.getWord(i).getID();
		}
		for (int i=0; i < target.size(); i++) {
			result = HASH_OFFSET*result + target.getWord(i).getID();
		}
		return result;
	}
	
	
	/**
	 * Returns a string representation of the Alignment.
	 * 
	 * @return a String representation.
	 */
	public String toString() {
		return toString(true);
	}
	
	
	/**
	 * Returns a string representation of the Alignment.
	 * 
	 * @param includeGrid a flag that allows the grid to be optionally printed
	 * @return a String representation.
	 */
	public String toString(boolean includeGrid) {
		StringBuffer buf = new StringBuffer();
		buf.append("<\"");
		buf.append(source.toString());
		buf.append("\", \"");
		buf.append(target.toString());
		buf.append("\"");
		if (includeGrid) {
			buf.append(", ");
			buf.append(grid.toString());
		}
		buf.append(">");
		return buf.toString();
	}
	
	
	/** 
	 * Returns an ASCII graph of the Alignment.
	 */
	public String toAsciiGraph() {
		StringBuffer buffer = new StringBuffer();
		
		int longestTargetWord = 0;
		for (int i = 0; i < getTargetLength(); i++) {
			longestTargetWord = Math.max(longestTargetWord, target.get(i).toString().length());
		}
		
		char[] space = new char[longestTargetWord];
		Arrays.fill(space, ' ');

		// print the source words
		for (int i = 0; i < getSourceLength(); i++) {
			buffer.append(space);
			for (int j = 0; j <= i; j++) {
				buffer.append("  |");
			}
			buffer.append(source.get(i));
			buffer.append('\n');
		}
		
		buffer.append(space);
		buffer.append("  ");
		space = new char[(getSourceLength() * 3)+1];
		Arrays.fill(space, '-');
		buffer.append(space);
		buffer.append('\n');
		
		// print the target words and the graph
		boolean[][] array = grid.generateBooleanArray();
		for (int row = 0; row < getTargetLength(); row++) {
			String targetWord = target.get(row).toString();
			space = new char[longestTargetWord - targetWord.length()];
			Arrays.fill(space, ' ');
			buffer.append(space);
			buffer.append(targetWord);
			buffer.append("  |");
			for(int column = 0; column < getSourceLength(); column++) {
				if(array[column][row]) {
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

	public static void main(String[] args) throws Exception {
		Vocabulary enVocab = new Vocabulary();
		Vocabulary deVocab = new Vocabulary();
		Phrase source = new Phrase("the minutes of yesterday ' s sitting have been distributed .", enVocab);
		Phrase target = new Phrase("das protokoll der letzten sitzung", deVocab);
		Grid grid = new Grid(source.size(), target.size(), "0.0,1.1,2.2,3.3,6.4");
		
		// Phrase target = new Phrase("das protokoll der letzten sitzung", deVocab);
		// Grid grid = new Grid(source.size(), target.size(), "0.0,1.1,2.2,3.3,6.4,8.5,9.6,10.7");
		
		Alignment alignment = new Alignment(source, target, grid);
		System.out.println(alignment.toAsciiGraph());
	}
}

