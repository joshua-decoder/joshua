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
package edu.jhu.util.sentence.phrase_extraction;

// Imports
import edu.jhu.util.sentence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * ConstrainedConsistent is an alternative implementation implementation
 * of the Consistent phrase extraction method which does not add the
 * additional potential alignments by adding unaligned target points.
 *
 * @author Chris Callison-Burch
 * @since  17 September 2005
 * @version $LastChangedDate$
 */
public class ConstrainedConsistent implements PhraseExtractor {

//===============================================================
// Constants
//===============================================================


//===============================================================
// Member variables
//===============================================================


//===============================================================
// Constructor(s)
//===============================================================

	public ConstrainedConsistent() {
	
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

	/** Returns the set of all phrase alignments that are contained 
	  * in the alignment.
	  *
	  * @param alignment the Alignment to extract phrase alignments from
	  */ 
	public Collection getAllPhraseAlignments(Alignment alignment) {
		return getAllPhraseAlignments(alignment, Math.max(alignment.getSourceLength(), alignment.getTargetLength()));
	}

	/** Returns the set of all phrase alignments up to the specified
	  * maximum length that are contained in the alignment.
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
	
	/** Returns a set of phrase alignments for the specified target phrase.  The 
	  * extraction algorithm checks that all aligned points fall within the bounding
	  * box, and adds additional alignment points if there are adjacent unaligned 
	  * source words.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param targetStart the first word of the target phrase (inclusive)
	  * @param targetEnd the final word of the target phrase (exclusive)
	  */
	public Collection getTargetAlignments(Alignment alignment, int targetStart, int targetEnd) {
		Collection spans = getTargetAlignmentSpans(alignment.getAlignmentPoints(), targetStart, targetEnd, 0, 0);
		ArrayList alignments = new ArrayList(spans.size());
		Iterator it = spans.iterator();
		while(it.hasNext()) {
			Span span = (Span) it.next();
			int sourceStart = span.getSourceStart();
			int sourceEnd = span.getSourceEnd();
			
			Phrase source = alignment.getSource().subPhrase(sourceStart, sourceEnd);
			Phrase target = alignment.getTarget().subPhrase(targetStart, targetEnd);
			Grid grid = new MaskedGrid(alignment.getAlignmentPoints(), sourceStart, source.size(), targetStart, target.size());
			alignments.add(new Alignment(source, target, grid));
		}
		return alignments;
	}

	
	/** Returns a set of phrase alignments for the specified source phrase.  The 
	  * extraction algorithm checks that all aligned points fall within the bounding
	  * box, and adds additional alignment points if there are adjacent unaligned 
	  * target words.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param targetStart the first word of the source phrase (inclusive)
	  * @param targetEnd the final word of the source phrase (exclusive)
	  */
	public Collection getSourceAlignments(Alignment alignment, int sourceStart, int sourceEnd) {
		Collection spans = getSourceAlignmentSpans(alignment.getAlignmentPoints(), sourceStart, sourceEnd, 0, 0);
		ArrayList alignments = new ArrayList(spans.size());
		Iterator it = spans.iterator();
		while(it.hasNext()) {
			Span span = (Span) it.next();
			int targetStart = span.getTargetStart();
			int targetEnd = span.getTargetEnd();
			
			Phrase source = alignment.getSource().subPhrase(sourceStart, sourceEnd);
			Phrase target = alignment.getTarget().subPhrase(targetStart, targetEnd);
			Grid grid = new MaskedGrid(alignment.getAlignmentPoints(), sourceStart, source.size(), targetStart, target.size());
			alignments.add(new Alignment(source, target, grid));
		}
		return alignments;
	}


	/** Returns the set of all spans of phasal alignments up to the specified
	 * maximum length that are contained in the alignment.
	 *
	 * @param alignment the Alignment to extract phrase alignments from
	 * @param maxLength the maximum length for both the source and target phrases 
	 */
	//TODO
	public Collection getAllAlignmentSpans(Alignment alignment, int maxLength){
		ArrayList phraseAlignments = new ArrayList();
		return phraseAlignments;
	}


	/** Returns a collection of spans for the phrase alignments that match the specified
	  * source phrase.  If the source phrase is unaligned, returns an empty set.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param sourceStart the first word of the source phrase (inclusive)
	  * @param sourceEnd the first word of the source phrase (inclusive)
	  * @param sourceOffset the start position of the alignment's source sentence in the source corpus
	  * @param targetOffset the start position of the alignment's target sentence in the target corpus
	  */	
	public Collection getSourceAlignmentSpans(Grid alignmentPoints, int sourceStart, int sourceEnd, int sourceOffset, int targetOffset) {
		ArrayList spans = new ArrayList();
	
		//Consistency check:
		//check that none of the target words within the target span
		//are aligned with source words outside of the source span
		boolean isConsistent = true;
		int[] targetPoints = alignmentPoints.getRowPoints(sourceStart, sourceEnd);
		int targetStart = 0;
		int targetEnd = 0;
		if(targetPoints != null && targetPoints.length != 0) {
			targetStart = targetPoints[0];
			targetEnd = targetPoints[targetPoints.length-1]+1;
		} else {
			// if the phrase isn't aligned with anything then 
			// don't extract it.
			isConsistent = false;
		}
		for(int i = targetStart; i < targetEnd; i++) {
			int[] sourcePoints = alignmentPoints.getColumnPoints(i, i+1);
			if(sourcePoints != null && sourcePoints.length != 0) {
				if(sourcePoints[0] < sourceStart || sourcePoints[sourcePoints.length-1] >= sourceEnd) {
					isConsistent = false;
				}
			} 
		}
		
		if(isConsistent) {
			spans.add(new Span(sourceStart, sourceEnd, targetStart, targetEnd, sourceOffset, targetOffset));
		}
		return spans;
	}
   
   
	/** Returns a collection of spans for the phrase alignments that match the specified
	  * target phrase.  If the target phrase is unaligned, returns an empty set.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param targetStart the first word of the target phrase (inclusive)
	  * @param targetEnd the first word of the target phrase (inclusive)
	  * @param sourceOffset the start position of the alignment's source sentence in the source corpus
	  * @param targetOffset the start position of the alignment's target sentence in the target corpus
	  */	
	public Collection getTargetAlignmentSpans(Grid alignmentPoints, int targetStart, int targetEnd, int sourceOffset, int targetOffset) {
		ArrayList spans = new ArrayList();
	
		//Consistency check:
		//check that none of the target words within the target span
		//are aligned with source words outside of the source span
		boolean isConsistent = true;
		int[] sourcePoints = alignmentPoints.getColumnPoints(targetStart, targetEnd);
		int sourceStart = 0;
		int sourceEnd = 0;
		if(sourcePoints != null && sourcePoints.length != 0) {
			sourceStart = sourcePoints[0];
			sourceEnd = sourcePoints[sourcePoints.length-1]+1;
		} else {
			// if the phrase isn't aligned with anything then 
			// don't extract it.
			isConsistent = false;
		}
		for(int i = sourceStart; i < sourceEnd; i++) {
			int[] targetPoints = alignmentPoints.getRowPoints(i, i+1);
			if(targetPoints != null && targetPoints.length != 0) {
				if(targetPoints[0] < targetStart || targetPoints[targetPoints.length-1] >= targetEnd) {
					isConsistent = false;
				}
			} 
		}
		
		if(isConsistent) {
			spans.add(new Span(sourceStart, sourceEnd, targetStart, targetEnd, sourceOffset, targetOffset));
		}
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

	public static void main(String[] args)
	{
		// ccb - debugging
		Phrase source = new Phrase("AsbAnyA tnfy tjmyd AlmsAEdp AlmmnwHp llmgrb", new Vocabulary());
		Phrase target = new Phrase("spain denies suspending aid to morocco", new Vocabulary());
		Grid grid = new Grid(6, 6, "0.0,1.1,2.2,3.3,4.4,5.5,1.2,4.2");
		
		Alignment alignment = new Alignment(source, target, grid);
		PhraseExtractor extractor = new ConstrainedConsistent();
		Iterator it = extractor.getAllPhraseAlignments(alignment).iterator();

		alignment.transpose();
		System.out.println(alignment.toAsciiGraph());

		while(it.hasNext()) {
			Alignment phraseAlignment = (Alignment) it.next();
			System.out.println(phraseAlignment.getSource() + " ||| " + phraseAlignment.getTarget());
		}
	}
}

