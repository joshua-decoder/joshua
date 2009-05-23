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
 * Abstract implementation of <code>Alignments</code> interface.
 * <p>
 * This class includes code that is likely to be common to all
 * concrete implementations of the interface.
 * 
 * This includes common code to test for consistent alignment spans
 * and to test for aligned terminals.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public abstract class AbstractAlignments implements Alignments {

	/**
	 * Indicates whether tight spans are required during phrase
	 * extraction.
	 */
	protected final boolean requireTightSpans;
	
	/**
	 * Constructs an abstract alignments object where
	 * <code>requiredTightSpans</code> is true.
	 */
	public AbstractAlignments() {
		this.requireTightSpans = true;
	}
	
	/**
	 * Constructs an abstract alignments object.
	 * 
	 * @param requireTightSpans Indicates whether tight spans
	 *                          are required during phrase extraction
	 */
	public AbstractAlignments(boolean requireTightSpans) {
		this.requireTightSpans = requireTightSpans;
	}
	
	/* See Javadoc for Alignments interface. */
	public Span getConsistentTargetSpan(Span sourceSpan) {
		Span targetSpan = getAlignedTargetSpan(sourceSpan);
		
		if (targetSpan.start == UNALIGNED) return null;
		
		// check back to see what sourceSpan the targetSpan
		// aligns back to, so that we can check that it's
		// within bounds
		Span correspondingSourceSpan = getAlignedSourceSpan(targetSpan.start, targetSpan.end);
		
		if (correspondingSourceSpan.start < sourceSpan.start
				|| correspondingSourceSpan.end > sourceSpan.end) {
			return null;
		} else {
			return targetSpan;
		}
	}

	/* See Javadoc for Alignments interface. */
	public boolean hasAlignedTerminal(int targetIndex, MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex) {
		
		int[] sourceIndices = getAlignedSourceIndices(targetIndex);
		
		if (sourceIndices==null || sourceIndices.length==0) {
			
			return false;
			
		} else {
			
			for (int alignedSourceIndex : sourceIndices) {
				if (sourcePhrases.containsTerminalAt(sourcePhraseIndex, alignedSourceIndex)) {
					return true;
				}
			}
			
			return false;
			
		}
		
	}
	
	/* See Javadoc for Alignments interface. */
	public Span getAlignedTargetSpan(Span sourceSpan) {
		return getAlignedTargetSpan(sourceSpan.start, sourceSpan.end);
	}
	
}
