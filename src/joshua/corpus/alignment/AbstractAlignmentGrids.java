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

import joshua.corpus.Corpus;
import joshua.corpus.Span;

/**
 * Abstract implementation of <code>Alignments</code> interface
 * that includes code likely to be common to implementations which
 * conceptually view alignment points as a grid.
 * <p>
 * This class class implements all methods defined by the 
 * <code>Alignments</code> interface except for {@link #size()}. 
 * 
 * Any concrete child class need only implement that method and
 * the two abstract protected methods defined here.
 * 
 * @author Lane Schwartz
 */
public abstract class AbstractAlignmentGrids extends AbstractAlignments {

	/** Source language corpus. */
	protected final Corpus sourceCorpus;
	
	/** Target language corpus. */
	protected final Corpus targetCorpus;
	
	/**
	 * Constructs an abstract alignments grid.
	 * 
	 * @param sourceCorpus Source language corpus
	 * @param targetCorpus Target language corpus
	 * @param requireTightSpans Indicates whether tight spans 
	 *                          are required during phrase extraction
	 */
	public AbstractAlignmentGrids(Corpus sourceCorpus, Corpus targetCorpus, boolean requireTightSpans) {
		super(requireTightSpans);
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
	}
	
	/**
	 * Gets the indices of all source words aligned to the
	 * specified span in the specified sentence.
	 * <p>
	 * All indices in this method are zero-based.
	 * <p>
	 * The span parameters of this method are relative to the
	 * sentene. So, for example, calling this method to get the
	 * source indices for a target span covering the first three
	 * words of the eight sentence in the parallel corpus, the
	 * following parameter values would be used:
	 * 
	 * <code>getSourcePoints(7, 0, 3)</code>
	 * 
	 * @param sentenceID Index of a sentence in the aligned parallel corpus
	 * @param targetSpanStart Inclusive start index in the target sentence
	 * @param targetSpanEnd Exclusive end index in the target sentence
	 * @return the indices of all source words aligned to the
	 *         specified span in the specified sentence
	 */
	protected abstract int[] getSourcePoints(int sentenceID, int targetSpanStart, int targetSpanEnd);
	
	/**
	 * Gets the indices of all target words aligned to the
	 * specified span in the specified sentence.
	 * <p>
	 * All indices in this method are zero-based.
	 * <p>
	 * The span parameters of this method are relative to the
	 * sentence. So, for example, calling this method to get
	 * the target indices for a source span covering the first
	 * three words of the eight sentence in the parallel corpus,
	 * the following parameter values would be used:
	 * 
	 * <code>getSourcePoints(7, 0, 3)</code>
	 * 
	 * @param sentenceID Index of a sentence in the aligned parallel corpus
	 * @param sourceSpanStart Inclusive start index in the source sentence
	 * @param sourceSpanEnd Exclusive end index in the source sentence
	 * @return the indices of all target words aligned to the
	 *         specified span in the specified sentence
	 */
	protected abstract int[] getTargetPoints(int sentenceID, int sourceSpanStart, int sourceSpanEnd);
	
	/* See Javadoc for Alignments interface. */
	public int[] getAlignedSourceIndices(int targetIndex) {
		
		int sentenceID = targetCorpus.getSentenceIndex(targetIndex);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int normalizedTargetIndex = targetIndex - targetOffset;
				
		int[] sourceIndices = getSourcePoints(sentenceID, normalizedTargetIndex, normalizedTargetIndex+1);
		for (int i=0; i<sourceIndices.length; i++) {
			sourceIndices[i] += sourceOffset;
		}
		
		if (sourceIndices.length==0) {
			return null;
		} else {
			return sourceIndices;
		}
	}

	/* See Javadoc for Alignments interface. */
	public Span getAlignedSourceSpan(int startTargetIndex, int endTargetIndex) {
		
		int sentenceID = targetCorpus.getSentenceIndex(startTargetIndex);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int normalizedTargetStartIndex = startTargetIndex - targetOffset;
		int normalizedTargetEndIndex = endTargetIndex - targetOffset;
				
		int[] sourceIndices = getSourcePoints(sentenceID, normalizedTargetStartIndex, normalizedTargetEndIndex);
		
		if (sourceIndices==null || sourceIndices.length==0) {
		
			return new Span(UNALIGNED, UNALIGNED);
		
		} else {
		
			int startSourceIndex = sourceOffset + sourceIndices[0];
			int endSourceIndex = sourceOffset + sourceIndices[sourceIndices.length-1]+1;
			
			return new Span(startSourceIndex, endSourceIndex);
			
		}
		
	}
	
	/* See Javadoc for Alignments interface. */
	public int[] getAlignedTargetIndices(int sourceIndex) {
		
		int sentenceID = sourceCorpus.getSentenceIndex(sourceIndex);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int normalizedSourceIndex = sourceIndex - sourceOffset;
				
		int[] targetIndices = getTargetPoints(sentenceID, normalizedSourceIndex, normalizedSourceIndex+1);
		for (int i=0; i<targetIndices.length; i++) {
			targetIndices[i] += targetOffset;
		}
		
		if (targetIndices.length==0) {
			return null;
		} else {
			return targetIndices;
		}
	}
	
	/* See Javadoc for Alignments interface. */
	public Span getAlignedTargetSpan(int startSourceIndex, int endSourceIndex) {
		
		int sentenceID = sourceCorpus.getSentenceIndex(startSourceIndex);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int normalizedSourceStartIndex = startSourceIndex - sourceOffset;
		int normalizedSourceEndIndex = endSourceIndex - sourceOffset;
		
		int[] targetIndices = getTargetPoints(sentenceID, normalizedSourceStartIndex, normalizedSourceEndIndex);
		
		int[] startPoints = getTargetPoints(sentenceID, normalizedSourceStartIndex, normalizedSourceStartIndex+1);
		
		int[] endPoints = getTargetPoints(sentenceID, normalizedSourceEndIndex-1, normalizedSourceEndIndex);
		
		if (targetIndices==null || targetIndices.length==0 || (requireTightSpans && (
				startPoints==null || startPoints.length==0 ||
				endPoints==null || endPoints.length==0))) {
		
			return new Span(UNALIGNED, UNALIGNED);
		
		} else {
		
			int startTargetIndex = targetOffset + targetIndices[0];
			int endTargetIndex = targetOffset + targetIndices[targetIndices.length-1]+1;
			
			return new Span(startTargetIndex, endTargetIndex);
		}
	}

}
