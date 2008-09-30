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
package joshua.util.sentence.phrase_extraction;

import joshua.util.sentence.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * BoundingBox is a implementation of PhraseExtractor which
 * just selects phrases by taking their bounding points.
 *
 * @author Chris Callison-Burch
 * @since  5 February 2005
 * @version $LastChangedDate$
 */
public class BoundingBox implements PhraseExtractor {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================


//===============================================================
// Constructor(s)
//===============================================================

	public BoundingBox() {
	
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

	/**
	 * Returns the set of all phrase alignments that are contained
	 * in the alignment.
	 *
	 * @param alignment the Alignment to extract phrase alignments from
	 */ 
	public Collection getAllPhraseAlignments(Alignment alignment) {
		return getAllPhraseAlignments(alignment, Math.max(alignment.getSourceLength(), alignment.getTargetLength()));
	}
	
	
	/**
	 * Returns the set of all phrase alignments up to the
	 * specified maximum length that are contained in the alignment.
	 *
	 * @param alignment the Alignment to extract phrase alignments from
	 * @param maxLength the maximum length for both the source and target phrases 
	 */ 	
	public Collection getAllPhraseAlignments(Alignment alignment, int maxLength) {
		ArrayList phraseAlignments = new ArrayList();
		// i is the start point of the source phrase
		for(int i = 0; i < alignment.getSourceLength(); i++) {
			// j is the end point of the source phrase
			for(int j = i+1; j <= alignment.getSourceLength(); j++) {
				// i -j is the length of the source phrase
				if(j-i <= maxLength) {
					Collection alignments = getSourceAlignments(alignment, i, j);
					Iterator it = alignments.iterator();
					while(it.hasNext()) {
						Alignment phraseAlignment = (Alignment) it.next();
						if(phraseAlignment.getTargetLength() <= maxLength) {
							phraseAlignments.add(phraseAlignment);
						}
					}			
				}
			}
		}
		return phraseAlignments;
	}
	
	
	/**
	 * Returns the phrase alignments for the specified source
	 * phrase. If the source phrase is unaligned, returns an
	 * empty set.
	 *
	 * @param alignment the Alignment to use when extracting the aligned phrase
	 * @param targetStart the first word of the source phrase (inclusive)
	 * @param targetEnd the final word of the source phrase (exclusive)
	 */
	public Collection getSourceAlignments(Alignment alignment, int sourceStart, int sourceEnd) {
		int[] points = alignment.getAlignmentPoints().getRowPoints(sourceStart, sourceEnd);
		if(points == null || points.length == 0) return new ArrayList();
		
		int targetStart = points[0];
		int targetEnd = points[points.length-1] +1;
		Phrase source = alignment.getSource().subPhrase(sourceStart, sourceEnd);
		Phrase target = alignment.getTarget().subPhrase(targetStart, targetEnd);
		Grid grid = new MaskedGrid(alignment.getAlignmentPoints(), sourceStart, source.size(), targetStart, target.size());
				
		ArrayList alignments = new ArrayList();
		alignments.add(new Alignment(source, target, grid));
		return alignments;
	}
	
	
	
	/**
	 * Returns the phrase alignments for the specified target
	 * phrase. If the source phrase is unaligned, returns an
	 * empty set.
	 *
	 * @param alignment the Alignment to use when extracting the aligned phrase
	 * @param targetStart the first word of the target phrase (inclusive)
	 * @param targetEnd the final word of the target phrase (exclusive)
	 */
	public Collection getTargetAlignments(Alignment alignment, int targetStart, int targetEnd) {
		int[] points = alignment.getAlignmentPoints().getColumnPoints(targetStart, targetEnd);
		if(points == null || points.length == 0) return new ArrayList();
		int sourceStart = points[0];
		int sourceEnd = points[points.length-1] +1;

		Phrase target = alignment.getTarget().subPhrase(targetStart, targetEnd);
		Phrase source = alignment.getSource().subPhrase(sourceStart, sourceEnd);
		Grid grid = new MaskedGrid(alignment.getAlignmentPoints(), sourceStart, source.size(), targetStart, target.size());
	
		ArrayList alignments = new ArrayList();
		alignments.add(new Alignment(source, target, grid));
		return alignments;
	}



	/**
	 * Returns the set of all spans of phasal alignments up to
	 * the specified maximum length that are contained in the
	 * alignment.
	 *
	 * @param alignment the Alignment to extract phrase alignments from
	 * @param maxLength the maximum length for both the source and target phrases 
	 */
	//TODO
	public Collection getAllAlignmentSpans(Alignment alignment, int maxLength){
		ArrayList phraseAlignments = new ArrayList();
		return phraseAlignments;
	}



	/**
	 * Returns a collection of spans for the phrase alignments
	 * that match the specified source phrase. If the source
	 * phrase is unaligned, returns an empty set.
	 *
	 * @param alignment the Alignment to use when extracting the aligned phrase
	 * @param sourceStart the first word of the source phrase (inclusive)
	 * @param sourceEnd the first word of the source phrase (inclusive)
	 * @param sourceOffset the start position of the alignment's source sentence in the source corpus
	 * @param targetOffset the start position of the alignment's target sentence in the target corpus
	 */	
	public Collection getSourceAlignmentSpans(Grid alignmentPoints, int sourceStart, int sourceEnd, int sourceOffset, int targetOffset) {
		ArrayList spans = new ArrayList();
	
		int[] points = alignmentPoints.getRowPoints(sourceStart, sourceEnd);
		if(points == null || points.length == 0) return new ArrayList();
		
		int targetStart = points[0];
		int targetEnd = points[points.length-1] +1;
		
		Span span = new Span(sourceStart, sourceEnd, targetStart, targetEnd, sourceOffset, targetOffset);
		spans.add(span);
		
		return spans;
	}
   
   
	/**
	 * Returns a collection of spans for the phrase alignments
	 * that match the specified target phrase. If the target
	 * phrase is unaligned, returns an empty set.
	 *
	 * @param alignment the Alignment to use when extracting the aligned phrase
	 * @param targetStart the first word of the target phrase (inclusive)
	 * @param targetEnd the first word of the target phrase (inclusive)
	 * @param sourceOffset the start position of the alignment's source sentence in the source corpus
	 * @param targetOffset the start position of the alignment's target sentence in the target corpus
	 */	
	public Collection getTargetAlignmentSpans(Grid alignmentPoints, int targetStart, int targetEnd, int sourceOffset, int targetOffset) {
		ArrayList spans = new ArrayList();
	
		int[] points = alignmentPoints.getColumnPoints(targetStart, targetEnd);
		if(points == null || points.length == 0) return new ArrayList();
		
		int sourceStart = points[0];
		int sourceEnd = points[points.length-1] +1;
		
		Span span = new Span(sourceStart, sourceEnd, targetStart, targetEnd, sourceOffset, targetOffset);
		spans.add(span);
		
		return spans;
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

	public static void main(String[] args) {
		
	}
}

