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

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.Span;

/**
 * Represents alignment points for an aligned parallel corpus.
 * 
 * @author Lane Schwartz
 */
public interface Alignments {

	/** Constant used to indicate that a value is not aligned. */
	int UNALIGNED = Integer.MAX_VALUE;
	
	/**
	 * This method looks up target span for the given source
	 * span.
	 * 
	 * @param startSourceIndex the staring position in the
	 *                         source corpus (inclusive)
	 * @param endSourceIndex   the end position in the source
	 *                         corpus (exclusive)
	 * @return a span containing the min and max indices in
	 *         the target corpus, if the span is unaligned the
	 *         value will be <UNALIGNED, undefined>
	 */
	Span getAlignedTargetSpan(int startSourceIndex, int endSourceIndex);
	

	/**
	 * This method looks up target span for the given source
	 * span
	 * 
	 * @param sourceSpan Inclusive staring position 
	 *                   and exclusive end position
	 *                   in the source corpus (inclusive)
	 * @return a span containing the min and max indices in the
	 *         target corpus, if the span is unaligned the value
	 *         will be <UNALIGNED, undefined>
	 */
	Span getAlignedTargetSpan(Span sourceSpan);

	
	/**
	 * Gets the indices of all source words aligned with a
	 * particular location in the target corpus.
	 * 
	 * @param targetIndex Index into the target corpus
	 * @return The indices of all source words aligned with
	 *         the given location in the target corpus,
	 *         or <code>null</code> unaligned.
	 */
	int[] getAlignedSourceIndices(int targetIndex);

	/**
	 * Gets the indices of all target words aligned with a
	 * particular location in the source corpus.
	 * 
	 * @param sourceIndex Index into the source corpus
	 * @return The indices of all target words aligned with 
	 *         the given location in the source corpus,
	 *         or <code>null</code> unaligned.
	 */
	int[] getAlignedTargetIndices(int sourceIndex);
	
	/**
	 * This method looks up source span for the given target
	 * span.
	 * 
	 * @param startTargetIndex the staring position in the
	 *                         target corpus (inclusive)
	 * @param endTargetIndex   the end position in the target
	 *                         corpus (exclusive)
	 * @return a tuple containing the min and max indices in
	 *         the source corpus, if the span is unaligned the
	 *         value will be <UNALIGNED, undefined>
	 */
	Span getAlignedSourceSpan(int startTargetIndex, int endTargetIndex);
	
	/**
	 * Determines if any terminal in the source phrase aligns
	 * with the provided index into the target corpus.
	 * 
	 * @param targetIndex
	 * @param sourcePhrase
	 * @return <code>true</code> if any terminal in the source phrase 
	 *         aligns with the provided index into the target corpus,
	 *         <code>false</code> otherwise
	 */
	boolean hasAlignedTerminal(int targetIndex, MatchedHierarchicalPhrases sourcePhrase, int sourcePhraseIndex);

	/**
	 * Gets a target span that is consistent with the provided
	 * source span, if one exists. If no consistent span exists,
	 * returns null.
	 * 
	 * @param sourceSpan
	 * @return a target span that is consistent with the provided
	 *         source span, if one exists, null otherwise
	 */
	Span getConsistentTargetSpan(Span sourceSpan);

	
	/**
	 * Gets the number of aligned sentences.
	 * 
	 * @return the number of aligned sentences
	 */
	int size();
}
