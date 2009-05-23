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
package joshua.corpus.alignment.mm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.AbstractAlignmentGrids;
import joshua.corpus.alignment.AlignmentGrid;

/**
 * Memory-mapped list of alignment grids representing all alignment
 * data for an aligned parallel corpus.
 * <p>
 * Instances of this class are created from binary alignment files,
 * which are typically created using
 * the {@link joshua.corpus.alignment.AlignmentGrids#writeExternal} method.
 * 
 * @author Lane Schwartz
 */
public class MemoryMappedAlignmentGrids extends AbstractAlignmentGrids {

	/** Number of alignment grids in the aligned parallel corpus. */
	private final int size;
	
	/** 
	 * Memory-mapped buffer containing the width of each alignment
	 * grid in the aligned parallel corpus.
	 * <p>
	 * The number of integers in this buffer should be equal
	 * to the number of sentences in the aligned parallel corpus.
	 */
	private final IntBuffer widths;
	
	/** 
	 * Memory-mapped buffer containing the height of each
	 * alignment grid in the aligned parallel corpus.
	 * <p>
	 * The number of integers in this buffer should be equal
	 * to the number of sentences in the aligned parallel corpus.
	 */
	private final IntBuffer heights;
	
	/** 
	 * Memory-mapped buffer containing the number of alignment
	 * points in each alignment grid in the aligned parallel
	 * corpus.
	 * <p>
	 * The number of integers in this buffer should be equal
	 * to the number of sentences in the aligned parallel corpus.
	 */
	private final IntBuffer pointCounts;
	
	/**
	 * Memory-mapped buffer containing all alignment points in
	 * the aligned parallel corpus. Each alignment point is
	 * encoded as a short.
	 * <p>
	 * The number of shorts in this buffer should be equal to
	 * the number of alignment points in the aligned parallel
	 * corpus.
	 * 
	 * @see joshua.corpus.alignment.AlignmentGrid#getKey
	 * @see joshua.corpus.alignment.AlignmentGrid#getLocation
	 */
	private final ShortBuffer alignmentPoints;
	
	/**
	 * Memory-mapped buffer containing all reverse alignment
	 * points in the aligned parallel corpus. Each reverse
	 * alignment point is encoded as a short.
	 * <p>
	 * The number of shorts in this buffer should be equal to
	 * the number of alignment points in the aligned parallel
	 * corpus.
	 * 
	 * @see joshua.corpus.alignment.AlignmentGrid#getKey
	 * @see oshua.corpus.alignment.AlignmentGrid#getLocation
	 */
	private final ShortBuffer reverseAlignmentPoints;
	
	/**
	 * Constructs memory-mapped alignment grids from the specified
	 * binary file and the specified source and target corpora.
	 * <p>
	 * The object returned by this constructor will require
	 * tight spans.
	 * 
	 * @param binaryAlignmentsFilename Name of binary file
	 *                     containing encoded alignment points
	 * @param sourceCorpus Source language corpus
	 * @param targetCorpus Target language corpus
	 * @throws IOException Any I/O exception that was encountered
	 * @see joshua.corpus.alignment.AlignmentGrids#writeExternal
	 */
	public MemoryMappedAlignmentGrids(String binaryAlignmentsFilename, Corpus sourceCorpus, Corpus targetCorpus) throws IOException {
		this(binaryAlignmentsFilename, sourceCorpus, targetCorpus, true);
	}
	 
	/**
	 * Constructs memory-mapped alignment grids from the specified
	 * binary file and the specified source and target corpora.
	 * 
	 * @param binaryAlignmentsFilename Name of binary file
	 *                     containing encoded alignment points
	 * @param sourceCorpus Source language corpus
	 * @param targetCorpus Target language corpus
	 * @param requireTightSpans Indicates whether tight alignment
	 *                     spans are required
	 * @throws IOException Any I/O exception that was encountered
	 * @see joshua.corpus.alignment.AlignmentGrids#writeExternal
	 */
	public MemoryMappedAlignmentGrids(String binaryAlignmentsFilename, Corpus sourceCorpus, Corpus targetCorpus, boolean requireTightSpans) throws IOException {
		super(sourceCorpus, targetCorpus, requireTightSpans);

		RandomAccessFile binaryFile = new RandomAccessFile( binaryAlignmentsFilename, "r" );
	    FileChannel binaryChannel = binaryFile.getChannel();
		
	    IntBuffer tmp;
	    
	    // Read the number of alignment grids
	    int start = 0;
	    int length = 4;
	    tmp = binaryChannel.map( FileChannel.MapMode.READ_ONLY, start, length).asIntBuffer().asReadOnlyBuffer();
	    this.size = tmp.get();
	    
	    // Memory map the widths of all grids
	    start += length;
	    length = 4*size;
	    this.widths = binaryChannel.map( FileChannel.MapMode.READ_ONLY, start, length).asIntBuffer().asReadOnlyBuffer();
	  
	    // Memory map the heights of all grids
	    start += length;
	    length = 4*size;
	    this.heights = binaryChannel.map( FileChannel.MapMode.READ_ONLY, start, length ).asIntBuffer().asReadOnlyBuffer();
	    
	    // Memory map the cumulative counts for alignment points
	    start += length;
	    length = 4*(size+1);
	    this.pointCounts = binaryChannel.map( FileChannel.MapMode.READ_ONLY, start, length ).asIntBuffer().asReadOnlyBuffer();
	
	    int totalPoints = pointCounts.get(size);
	    
	    start += length;
	    length = 2*totalPoints;
	    this.alignmentPoints = binaryChannel.map( FileChannel.MapMode.READ_ONLY, start, length ).asShortBuffer().asReadOnlyBuffer();
	    
	    start += length;
	    length = 2*totalPoints;
	    this.reverseAlignmentPoints = binaryChannel.map( FileChannel.MapMode.READ_ONLY, start, length ).asShortBuffer().asReadOnlyBuffer();
	}
	
	/* See Javadoc for AbstractAlignmentGrids. */
	@Override
	protected int[] getSourcePoints(int sentenceId, int targetSpanStart,
			int targetSpanEnd) {

		int start = pointCounts.get(sentenceId);
		int end = pointCounts.get(sentenceId+1);
		int numPoints = end - start;
		short[] reversePoints = new short[numPoints];
		reverseAlignmentPoints.position(start);
		reverseAlignmentPoints.get(reversePoints);
		
		return AlignmentGrid.getPoints(targetSpanStart, targetSpanEnd, widths.get(sentenceId), reversePoints);
		
	}

	/* See Javadoc for AbstractAlignmentGrids. */
	@Override
	protected int[] getTargetPoints(int sentenceId, int sourceSpanStart,
			int sourceSpanEnd) {
		
		int start = pointCounts.get(sentenceId);
		int end = pointCounts.get(sentenceId+1);
		int numPoints = end - start;
		short[] points = new short[numPoints];
		alignmentPoints.position(start);
		alignmentPoints.get(points);
		
		return AlignmentGrid.getPoints(sourceSpanStart, sourceSpanEnd, heights.get(sentenceId), points);
		
	}

	/* See Javadoc for Alignments. */
	public int size() {
		return this.size;
	}

}
