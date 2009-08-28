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
package joshua.corpus.suffix_array.mm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.suffix_array.AbstractSuffixArray;
import joshua.corpus.suffix_array.MismatchedCorpusException;
import joshua.corpus.suffix_array.Pattern;
import joshua.decoder.ff.tm.Rule;
import joshua.util.Cache;

public class MemoryMappedSuffixArray extends AbstractSuffixArray {

	private final IntBuffer binarySuffixBuffer;
	private final int size;

	
	public MemoryMappedSuffixArray(String suffixesFileName, Corpus corpus) throws IOException, ClassNotFoundException {
		this(suffixesFileName, corpus, Cache.DEFAULT_CAPACITY);
	}
	
	/** 
	 * Constructs a suffix array from an on-disk memory-mapped
	 * representation and an existing Corpus.
	 * 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public MemoryMappedSuffixArray(String suffixesFileName, Corpus corpus, int maxCacheSize) throws IOException, ClassNotFoundException {
		super(corpus, 
				new Cache<Pattern,MatchedHierarchicalPhrases>(maxCacheSize),
				new Cache<Pattern,List<Rule>>(maxCacheSize));
		
		RandomAccessFile binaryFile = new RandomAccessFile( suffixesFileName, "r" );
	    FileChannel binaryChannel = binaryFile.getChannel();
	    
	    // The first line specifies the number of entries in the suffix array
	    IntBuffer tmp = binaryChannel.map( FileChannel.MapMode.READ_ONLY, 0, 4).asIntBuffer().asReadOnlyBuffer();
	    size = tmp.get();
	    
	    if (size != corpus.size()) {
	    	throw new MismatchedCorpusException(this);
	    }
	    
	    this.binarySuffixBuffer = binaryChannel.map( FileChannel.MapMode.READ_ONLY, 4, 4*size ).asIntBuffer().asReadOnlyBuffer();
	    
	}
	
	@Override
	public int getCorpusIndex(int suffixIndex) {
		return binarySuffixBuffer.get(suffixIndex);
	}

	@Override
	public int size() {
		return size;
	}

}
