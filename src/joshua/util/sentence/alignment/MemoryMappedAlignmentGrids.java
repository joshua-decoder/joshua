package joshua.util.sentence.alignment;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

import joshua.sarray.Corpus;

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
		
	    int headerSize = 0;
	    IntBuffer tmp;
	    
	    // Read the number of alignment grids
	    tmp = binaryChannel.map( FileChannel.MapMode.READ_ONLY, headerSize, 4).asIntBuffer().asReadOnlyBuffer();
	    this.size = tmp.get();
	    
	    // Memory map the widths of all grids
	    this.widths = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (headerSize+4), 4*size ).asIntBuffer().asReadOnlyBuffer();
	  
	    // Memory map the heights of all grids
	    this.heights = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (headerSize + 4 + 4*size), 4*size ).asIntBuffer().asReadOnlyBuffer();
	    
	    // Memory map the cumulative counts for alignment points
	    this.pointCounts = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (headerSize + 4 + 4*size + 4*size), 4*size ).asIntBuffer().asReadOnlyBuffer();
	
	    int totalPoints = pointCounts.get(size+1);
	    
	    this.alignmentPoints = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (headerSize + 4 + 4*size + 4*size + 4*size), 2*totalPoints ).asShortBuffer().asReadOnlyBuffer();
	    this.reverseAlignmentPoints = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (headerSize + 4 + 4*size + 4*size + 4*size + 2*totalPoints), 2*totalPoints ).asShortBuffer().asReadOnlyBuffer();
	}
	
	@Override
	protected int[] getSourcePoints(int sentenceId, int targetSpanStart,
			int targetSpanEnd) {

		int numPoints = pointCounts.get(sentenceId+1) - pointCounts.get(sentenceId);
		short[] reversePoints = new short[numPoints];
		reverseAlignmentPoints.get(reversePoints);
		
		return AlignmentGrid.getPoints(targetSpanStart, targetSpanEnd, widths.get(sentenceId), reversePoints);
		
	}

	@Override
	protected int[] getTargetPoints(int sentenceId, int sourceSpanStart,
			int sourceSpanEnd) {
		
		int numPoints = pointCounts.get(sentenceId+1) - pointCounts.get(sentenceId);
		short[] points = new short[numPoints];
		alignmentPoints.get(points);
		
		return AlignmentGrid.getPoints(sourceSpanStart, sourceSpanEnd, heights.get(sentenceId), points);
		
	}

}
