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

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.util.io.BinaryOut;

/**
 * 
 * 
 * @author Lane Schwartz
 */
public class AlignmentGrids extends AbstractAlignmentGrids {

	private static final Logger logger = Logger.getLogger(AlignmentGrids.class.getName()); 
	
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
	 * @param expectedSize Expected number of training sentences. This parameter merely specifies the initial capacity of an array list.
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
	 * @param expectedSize Expected number of training sentences. This parameter merely specifies the initial capacity of an array list.
	 * @param requireTightSpans 
	 */
	public AlignmentGrids(Scanner alignmentScanner, Corpus sourceCorpus, Corpus targetCorpus, int expectedSize, boolean requireTightSpans) {
		super(sourceCorpus, targetCorpus, requireTightSpans);
		
		this.alignments = new ArrayList<AlignmentGrid>(expectedSize);
		
		boolean finest = logger.isLoggable(Level.FINEST);
		int tenthSize = expectedSize / 10;
		
		int lineNumber = 0;
		while (alignmentScanner.hasNextLine()) {
			
			String line = alignmentScanner.nextLine();
			
			alignments.add(new AlignmentGrid(line));
			
			lineNumber++;
			if (finest && (lineNumber%tenthSize==0)) logger.finest("AlignmentGrids construction " + (lineNumber/tenthSize)+"0% complete");
			
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


	public void writeExternal(ObjectOutput out) throws IOException {
		
		// Start by writing the number of alignments
		int size = alignments.size();
		logger.fine("Exporting size = " + size + ": 1 integer (4 bytes)");
		out.writeInt(size);
		
		// Write the widths of each grid
		logger.fine("Exporting widths: " + size + " integers (" + size*4 + ") bytes");
		for (AlignmentGrid grid : alignments) {
			out.writeInt(grid.width);
		}
		
		// Write the widths of each grid
		logger.fine("Exporting widths: " + size + " integers (" + size*4 + ") bytes");
		for (AlignmentGrid grid : alignments) {
			out.writeInt(grid.height);
		}
		
		// Write the number of alignment points in each grid
		logger.fine("Exporting pointCounters: " + (size+1) + " integers (" + (size+1)*4 + ") bytes");
		int pointCounter = 0;
		out.writeInt(pointCounter);
		for (AlignmentGrid grid : alignments) {
			pointCounter += grid.coordinates.length; 
			out.writeInt(pointCounter);
		}
		logger.finer("\tfinal pointCounter value was: " + pointCounter);

		
		// Write the alignment points
		logger.fine("Exporting grid coordinates: " + pointCounter + " shorts (" + pointCounter*2 + ") bytes");
		for (AlignmentGrid grid : alignments) {
			for (short point : grid.coordinates) {
				out.writeShort(point);
			}
		}
		
		// Write the reverse alignment points
		logger.fine("Exporting reverse grid coordinates: " + pointCounter + " shorts (" + pointCounter*2 + ") bytes");
		for (AlignmentGrid grid : alignments) {
			for (short point : grid.transposedCoordinates) {
				out.writeShort(point);
			}
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		
		if (args.length != 2) {
			System.err.println("Usage: java " + AlignmentGrids.class.getName() + " alignments alignments.bin");
			System.exit(0);
		}
		
		String alignmentsFileName = args[0];
		String binaryAlignmentsFileName = args[1];

		File alignmentsFile = new File(alignmentsFileName);
		Scanner scanner = new Scanner(alignmentsFile);
		
		AlignmentGrids grids = new AlignmentGrids(scanner, null, null, 10);
		
		BinaryOut out = new BinaryOut(binaryAlignmentsFileName);
		grids.writeExternal(out);
		out.flush();
		out.close();
	}

	public int size() {
		return this.alignments.size();
	}
	
}

