package joshua.corpus.alignment.mm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

import joshua.corpus.Corpus;
import joshua.corpus.alignment.AbstractAlignmentGrids;
import joshua.corpus.alignment.AlignmentGrid;

public class MemoryMappedAlignmentGrids extends AbstractAlignmentGrids {

	/** Number of alignment grids */
	private final int size;
		
	private final IntBuffer widths;
	private final IntBuffer heights;
	private final IntBuffer pointCounts;
	
	private final ShortBuffer alignmentPoints;
	private final ShortBuffer reverseAlignmentPoints;
	
	public MemoryMappedAlignmentGrids(String binaryAlignmentsFilename, Corpus sourceCorpus, Corpus targetCorpus) throws IOException {
		this(binaryAlignmentsFilename, sourceCorpus, targetCorpus, true);
	}
	
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

	public int size() {
		return this.size;
	}

}
