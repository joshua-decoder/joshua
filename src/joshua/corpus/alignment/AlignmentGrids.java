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
 * List of alignment grids representing all alignment data for an
 * aligned parallel corpus.
 * <p>
 * Instances of this class are created from human-readable alignment
 * text files.
 * 
 * @author Lane Schwartz
 */
public class AlignmentGrids extends AbstractAlignmentGrids {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(AlignmentGrids.class.getName()); 
	
	/** List of individual alignment grids. */
	private final List<AlignmentGrid> alignments;
	
	/**
	 * Constructs a list of AlignmentGrid objects.
	 * <p>
	 * The size parameter is used to allocate the initial
	 * capacity of the backing list. If this number is off,
	 * things will still work, but memory usage may be less
	 * optimal.
	 * <p>
	 * The object returned by this constructor will required
	 * tight spans.
	 * 
	 * @param alignmentScanner
	 * @param sourceCorpus
	 * @param targetCorpus
	 * @param expectedSize Expected number of training sentences.
	 *            This parameter merely specifies the initial
	 *            capacity of an array list.
	 */
	public AlignmentGrids(Scanner alignmentScanner, Corpus sourceCorpus, Corpus targetCorpus, int expectedSize) {
		this(alignmentScanner, sourceCorpus, targetCorpus, expectedSize, true);
	}
	
	/**
	 * Constructs a list of AlignmentGrid objects.
	 * <p>
	 * The size parameter is used to allocate the initial
	 * capacity of the backing list. If this number is off,
	 * things will still work, but memory usage may be less
	 * optimal.
	 * 
	 * @param alignmentScanner
	 * @param sourceCorpus
	 * @param targetCorpus
	 * @param expectedSize Expected number of training sentences.
	 *            This parameter merely specifies the initial
	 *            capacity of an array list.
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
			
			try {
			    AlignmentGrid grid = new AlignmentGrid(line);
			    alignments.add(grid);
			} catch (Exception e) {
			    logger.warning("Sentence pair number " + lineNumber + " was too long, skipping this item");
			    alignments.add(null);
			}
			
			lineNumber++;
			if (finest && (lineNumber%tenthSize==0)) {
				logger.finest("AlignmentGrids construction " + 
						(lineNumber/tenthSize)+"0% complete");
			}
			
		}
	}
	
	/* See Javadoc for AbstractAlignmentGrids. */
	protected int[] getSourcePoints(int sentenceID, int targetSpanStart, int targetSpanEnd) {
		AlignmentGrid grid = alignments.get(sentenceID);
		if(grid != null) {
		    return grid.getSourcePoints(targetSpanStart, targetSpanEnd);
		} else {
		    return new int[0];
		}
	}
	
	/* See Javadoc for AbstractAlignmentGrids. */
	protected int[] getTargetPoints(int sentenceID, int sourceSpanStart, int sourceSpanEnd) {
		AlignmentGrid grid = alignments.get(sentenceID);
		if(grid != null) {
		    return grid.getTargetPoints(sourceSpanStart, sourceSpanEnd);
		} else {
                    return new int[0];
		}
	}

	/**
	 * Serializes this object as binary data.
	 * 
	 * @param out The stream to write this object to.
	 * @throws IOException Includes any I/O exceptions that may occur
	 * @see java.io.Externalizable#writeExternal
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		
		// Start by writing the number of alignments
		int size = alignments.size();
		logger.fine("Exporting size = " + size + ": 1 integer (4 bytes)");
		out.writeInt(size);
		
		// Write the widths of each grid
		logger.fine("Exporting widths: " + size + " integers (" + size*4 + ") bytes");
		for (AlignmentGrid grid : alignments) {
		    if(grid != null) {
			out.writeInt(grid.width);
		    } else {
			out.writeInt(0);
		    }
		}
		
		// Write the widths of each grid
		logger.fine("Exporting widths: " + size + " integers (" + size*4 + ") bytes");
		for (AlignmentGrid grid : alignments) {
		    if(grid != null) {
			out.writeInt(grid.height);
		    } else {
			out.writeInt(0);
		    }
		}
		
		// Write the number of alignment points in each grid
		logger.fine("Exporting pointCounters: " + (size+1) + " integers (" + (size+1)*4 + ") bytes");
		int pointCounter = 0;
		out.writeInt(pointCounter);
		for (AlignmentGrid grid : alignments) {
		    if(grid != null) {
			pointCounter += grid.coordinates.length; 
			out.writeInt(pointCounter);
		    } else {
			out.writeInt(0);
		    }
		}
		logger.finer("\tfinal pointCounter value was: " + pointCounter);

		
		// Write the alignment points
		logger.fine("Exporting grid coordinates: " + pointCounter + " shorts (" + pointCounter*2 + ") bytes");
		for (AlignmentGrid grid : alignments) {
		    if(grid != null) {
			for (short point : grid.coordinates) {
				out.writeShort(point);
			}
		    }
		}
		
		// Write the reverse alignment points
		logger.fine("Exporting reverse grid coordinates: " + pointCounter + " shorts (" + pointCounter*2 + ") bytes");
		for (AlignmentGrid grid : alignments) {
		    if(grid != null) {
			for (short point : grid.transposedCoordinates) {
				out.writeShort(point);
			}
		    }
		}
		
	}

	/* See Javadoc for Alignments interface. */
	public int size() {
		return this.alignments.size();
	}
	
	/**
	 * Main method used to read a human-readable alignments
	 * file and write it to disk as binary data.
	 * 
	 * @param args File names for an existing human-readable
	 *             alignments file and for the binary data file
	 *             to be written
	 * @throws IOException Includes any I/O exceptions that may occur
	 */
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
	
}
