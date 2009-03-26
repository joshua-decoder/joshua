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

import joshua.sarray.Corpus;

/**
 * 
 * 
 * @author Lane Schwartz
 */
public class AlignmentGrids extends AbstractAlignmentGrids {

	private final List<AlignmentGrid> alignments;
	
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
	public AlignmentGrids(Scanner alignmentScanner, Corpus sourceCorpus, Corpus targetCorpus, int expectedSize) {
		this(alignmentScanner, sourceCorpus, targetCorpus, expectedSize, true);
	}
	
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
	 * @param requireTightSpans 
	 */
	public AlignmentGrids(Scanner alignmentScanner, Corpus sourceCorpus, Corpus targetCorpus, int expectedSize, boolean requireTightSpans) {
		super(sourceCorpus, targetCorpus, requireTightSpans);
		
		this.alignments = new ArrayList<AlignmentGrid>(expectedSize);
		
		while (alignmentScanner.hasNextLine()) {
			
			String line = alignmentScanner.nextLine();
			
			alignments.add(new AlignmentGrid(line));
			
		}
	}
	
	protected int[] getSourcePoints(int sentenceID, int targetSpanStart, int targetSpanEnd) {
		AlignmentGrid grid = alignments.get(sentenceID);
		
		return grid.getSourcePoints(targetSpanStart, targetSpanEnd);
	}
	
	protected int[] getTargetPoints(int sentenceID, int sourceSpanStart, int sourceSpanEnd) {
		AlignmentGrid grid = alignments.get(sentenceID);
		
		return grid.getTargetPoints(sourceSpanStart, sourceSpanEnd);
	}
	
}
