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
package joshua.sarray;

import joshua.corpus.SymbolTable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;


/**
 *
 * 
 * @author Lane Schwartz
 */
public class MemoryMappedCorpusArray extends AbstractCorpus { 

	private final IntBuffer binaryCorpusBuffer;
	private final IntBuffer binarySentenceBuffer;
	
	private final int numberOfWords;
	private final int numberOfSentences;
	
	/**
	 * Constructs a memory mapped corpus array
	 * from existing binary files.
	 * 
	 * @param vocabulary
	 * @param binaryCorpusFileName
	 * @param binaryCorpusFileSize
	 * @param binarySentenceFileName
	 * @param binarySentenceFileSize
	 * @throws IOException
	 */
	public MemoryMappedCorpusArray(
		SymbolTable symbolTable,
		String     binaryCorpusFileName,
		int        binaryCorpusFileSize,
		String     binarySentenceFileName,
		int        binarySentenceFileSize
	) throws IOException {
		
		super(symbolTable);
		
		RandomAccessFile binarySentenceFile = new RandomAccessFile( binarySentenceFileName, "r" );
	    FileChannel binarySentenceFileChannel = binarySentenceFile.getChannel();
	    this.binarySentenceBuffer = binarySentenceFileChannel.map( FileChannel.MapMode.READ_ONLY, 0, binarySentenceFileSize ).asIntBuffer();

		RandomAccessFile binaryCorpusFile = new RandomAccessFile( binaryCorpusFileName, "r" );
	    FileChannel binaryCorpusFileChannel = binaryCorpusFile.getChannel();
	    this.binaryCorpusBuffer = binaryCorpusFileChannel.map( FileChannel.MapMode.READ_ONLY, 0, binaryCorpusFileSize ).asIntBuffer();

	    this.numberOfWords = binaryCorpusFileSize;
	    this.numberOfSentences = binarySentenceFileSize;
	}
	
	public MemoryMappedCorpusArray(
			SymbolTable symbolTable,
			String     binaryFileName
		) throws IOException {
			
			super(symbolTable);
			
			IntBuffer tmp;
			
			RandomAccessFile binaryFile = new RandomAccessFile( binaryFileName, "r" );
		    FileChannel binaryChannel = binaryFile.getChannel();
		    tmp = binaryChannel.map( FileChannel.MapMode.READ_ONLY, 0, 4).asIntBuffer().asReadOnlyBuffer();
		    this.numberOfSentences = tmp.get();
		    this.binarySentenceBuffer = binaryChannel.map( FileChannel.MapMode.READ_ONLY, 4, 4*numberOfSentences ).asIntBuffer().asReadOnlyBuffer();
		    
		    tmp = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (4 + 4*numberOfSentences), 4).asIntBuffer().asReadOnlyBuffer();
		    this.numberOfWords = tmp.get();
		    this.binaryCorpusBuffer = binaryChannel.map( FileChannel.MapMode.READ_ONLY, (4 + 4*numberOfSentences + 4), 4*numberOfWords ).asIntBuffer().asReadOnlyBuffer();

		}


	@Override
	public int getNumSentences() {
		return numberOfSentences; 
	}

	@Override
	public int getSentenceIndex(int position) {
		
		int index = binarySearch(position);
		// if index is positive, then the position searched
		// for is the first word of a sentence. we return
		// the exact value.
		if (index >= 0) {
				return index;
		} else {
		// otherwise, we are given an negative version of
		// the first number higher than our position. that
		// is the position of where this would be inserted
		// if it was its own sentence, so we make the number
		// positive and subtract 2 (one since since it is
		// by ith element instead of position, one to get
		// the previous index)
			return (index*(-1))-2;
		}
	}

	private int binarySearch(int value) {

		int low = 0;
		int high = size() - 1;
		
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midValue = binarySentenceBuffer.get(mid);
			
			if (midValue < value) {
				low = mid + 1;
			} else if (midValue > value) {
				high = mid -1;
			} else {
				return mid;
			}
		}
		
		return -(low+1);
		
	}
	
	@Override
	public int getSentencePosition(int sentenceID) {
		if (sentenceID >= numberOfSentences) {
			return numberOfWords-1;
		}
		return binarySentenceBuffer.get(sentenceID);
		//return binarySentenceBuffer.getInt(sentenceID*4);
	}

	@Override
	public int getWordID(int position) {
		return binaryCorpusBuffer.get(position);
		//return binaryCorpusBuffer.getInt(position*4);	
	}

	@Override
	public int size() {
		return numberOfWords;
	}
	
	

}
