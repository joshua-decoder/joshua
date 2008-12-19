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
package joshua.util.sentence.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import joshua.sarray.CorpusArray;
import joshua.util.sentence.Span;

/**
 * 
 * 
 * @author Lane Schwartz
 */
public class AlignmentGrids extends AbstractAlignments {

	private final List<AlignmentGrid> alignments;
	private final CorpusArray sourceCorpus;
	private final CorpusArray targetCorpus;
	
	/**
	 * Constructs a list of AlignmentGrid objects.
	 * <p>
	 * The size parameter is used to allocate the initial capacity of the backing list.
	 * If this number is off, things will still work, but memory usage may be less optimal.
	 * 
	 * @param alignmentsFile
	 * @param sourceCorpus
	 * @param targetCorpus
	 * @param expectedSize Expected number of training sentences. 
	 */
	public AlignmentGrids(Scanner alignmentScanner, CorpusArray sourceCorpus, CorpusArray targetCorpus, int expectedSize) {
		
		this.alignments = new ArrayList<AlignmentGrid>(expectedSize);
		this.sourceCorpus = sourceCorpus;
		this.targetCorpus = targetCorpus;
		
		readAlignmentPoints(alignmentScanner);
	}
	
	
	public int[] getAlignedSourceIndices(int targetIndex) {
		
		int sentenceID = targetCorpus.getSentenceIndex(targetIndex);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int normalizedTargetIndex = targetIndex - targetOffset;
				
		AlignmentGrid grid = alignments.get(sentenceID);
		
		int[] sourceIndices = grid.getSourcePoints(normalizedTargetIndex, normalizedTargetIndex+1);
		
		for (int i=0; i<sourceIndices.length; i++) {
			sourceIndices[i] += sourceOffset;
		}
		
		if (sourceIndices.length==0)
			return null;
		else
			return sourceIndices;
	}
	

	public Span getAlignedSourceSpan(int startTargetIndex, int endTargetIndex) {
		
		int sentenceID = targetCorpus.getSentenceIndex(startTargetIndex);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int normalizedTargetStartIndex = startTargetIndex - targetOffset;
		int normalizedTargetEndIndex = endTargetIndex - targetOffset;
				
		AlignmentGrid grid = alignments.get(sentenceID);
		
		int[] sourceIndices = grid.getSourcePoints(normalizedTargetStartIndex, normalizedTargetEndIndex);
		
		if (sourceIndices==null || sourceIndices.length==0) {
		
			return new Span(UNALIGNED, UNALIGNED);
		
		} else {
		
			int startSourceIndex = sourceOffset + sourceIndices[0];
			int endSourceIndex = sourceOffset + sourceIndices[sourceIndices.length-1]+1;
			
			return new Span(startSourceIndex, endSourceIndex);
			
		}
		
	}


	public int[] getAlignedTargetIndices(int sourceIndex) {
		
		int sentenceID = sourceCorpus.getSentenceIndex(sourceIndex);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int normalizedSourceIndex = sourceIndex - sourceOffset;
				
		AlignmentGrid grid = alignments.get(sentenceID);
		
		int[] targetIndices = grid.getTargetPoints(normalizedSourceIndex, normalizedSourceIndex+1);
		
		for (int i=0; i<targetIndices.length; i++) {
			targetIndices[i] += targetOffset;
		}
		
		if (targetIndices.length==0) 
			return null;
		else
			return targetIndices;
		
	}


	public Span getAlignedTargetSpan(int startSourceIndex, int endSourceIndex) {
		
		int sentenceID = sourceCorpus.getSentenceIndex(startSourceIndex);
		int targetOffset = targetCorpus.getSentencePosition(sentenceID);
		int sourceOffset = sourceCorpus.getSentencePosition(sentenceID);
		int normalizedSourceStartIndex = startSourceIndex - sourceOffset;
		int normalizedSourceEndIndex = endSourceIndex - sourceOffset;
				
		AlignmentGrid grid = alignments.get(sentenceID);
		
		int[] targetIndices = grid.getTargetPoints(normalizedSourceStartIndex, normalizedSourceEndIndex);
		
		int[] startPoints = grid.getTargetPoints(normalizedSourceStartIndex, normalizedSourceStartIndex+1);
		int[] endPoints = grid.getTargetPoints(normalizedSourceEndIndex-1, normalizedSourceEndIndex);
		
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




	
	
	private void readAlignmentPoints(Scanner alignmentScanner) {
		
		while (alignmentScanner.hasNextLine()) {
			
			String line = alignmentScanner.nextLine();
			
			alignments.add(new AlignmentGrid(line));
			
		}
	}

}
