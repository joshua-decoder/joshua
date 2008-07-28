package edu.jhu.util.sentence;

// Imports
import edu.jhu.util.sentence.*;
import java.util.Collection;

import java.io.*;

/**
 * PhraseExtractor is an interface that defines methods for extracting
 * phrase pairs from a word-level alignment.  A number of papers have
 * been published describing heuristics for doing phrase alignment, and
 * we have defined classes that implement their behavior and use
 * the PhraseExtractor interface.
 *
 * @author Chris Callison-Burch
 * @since  3 February 2005
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
public interface PhraseExtractor {
	
//===============================================================
// Method definitions
//===============================================================
	   
	/** Returns the set of all phrase alignments that are contained 
	  * in the alignment.
	  *
	  * @param alignment the Alignment to extract phrase alignments from
	  */ 
	public Collection getAllPhraseAlignments(Alignment alignment);

	/** Returns the set of all phrase alignments up to the specified
	  * maximum length that are contained in the alignment.
	  *
	  * @param alignment the Alignment to extract phrase alignments from
	  * @param maxLength the maximum length for both the source and target phrases 
	  */ 	
	public Collection getAllPhraseAlignments(Alignment alignment, int maxLength);
	
	
	/** Returns the phrase alignments for the specified target phrase.  If
	  * the source phrase is unaligned, returns an empty set.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param targetStart the first word of the target phrase (inclusive)
	  * @param targetEnd the final word of the target phrase (exclusive)
	  */
	public Collection getTargetAlignments(Alignment alignment, int targetStart, int targetEnd);
	
	
	/** Returns the phrase alignments for the specified source phrase.  If
	  * the source phrase is unaligned, returns an empty set.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param targetStart the first word of the source phrase (inclusive)
	  * @param targetEnd the final word of the source phrase (exclusive)
	  */
	public Collection getSourceAlignments(Alignment alignment, int sourceStart, int sourceEnd);

    /** Returns the set of all spans up to the specified
     * maximum length that are contained in the alignment.
     *
     * @param alignment the Alignment to extract phrase alignments from
     * @param maxLength the maximum length for both the source and target phrases 
     */
     public Collection getAllAlignmentSpans(Alignment alignment, int maxLength);

	/** Returns a collection of spans for the phrase alignments that match the specified
	  * source phrase.  If the source phrase is unaligned, returns an empty set.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param sourceStart the first word of the source phrase (inclusive)
	  * @param sourceEnd the first word of the source phrase (inclusive)
	  * @param sourceOffset the start position of the alignment's source sentence in the source corpus
	  * @param targetOffset the start position of the alignment's target sentence in the target corpus
	  */	
	public Collection getSourceAlignmentSpans(Grid alignmentPoints, int sourceStart, int sourceEnd, int sourceOffset, int targetOffset);
   
   
	/** Returns a collection of spans for the phrase alignments that match the specified
	  * target phrase.  If the target phrase is unaligned, returns an empty set.
	  *
	  * @param alignment the Alignment to use when extracting the aligned phrase
	  * @param targetStart the first word of the target phrase (inclusive)
	  * @param targetEnd the first word of the target phrase (inclusive)
	  * @param sourceOffset the start position of the alignment's source sentence in the source corpus
	  * @param targetOffset the start position of the alignment's target sentence in the target corpus
	  */	
	public Collection getTargetAlignmentSpans(Grid alignmentPoints, int targetStart, int targetEnd, int sourceOffset, int targetOffset);
   

}

