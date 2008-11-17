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

import joshua.util.sentence.Vocabulary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 *
 * 
 * @author Lane Schwartz
 */
public class MemoryMappedCorpusArray { // implements Corpus {

	private final Vocabulary vocabulary;
	
	private final MappedByteBuffer binaryCorpusBuffer;
	private final MappedByteBuffer binarySentenceBuffer;
	
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
		Vocabulary vocabulary,
		String     binaryCorpusFileName,
		int        binaryCorpusFileSize,
		String     binarySentenceFileName,
		int        binarySentenceFileSize
	) throws IOException {
		
		this.vocabulary = vocabulary;
		
		RandomAccessFile binarySentenceFile = new RandomAccessFile( binarySentenceFileName, "r" );
	    FileChannel binarySentenceFileChannel = binarySentenceFile.getChannel();
	    this.binarySentenceBuffer = binarySentenceFileChannel.map( FileChannel.MapMode.READ_ONLY, 0, binarySentenceFileSize );

		RandomAccessFile binaryCorpusFile = new RandomAccessFile( binaryCorpusFileName, "r" );
	    FileChannel binaryCorpusFileChannel = binaryCorpusFile.getChannel();
	    this.binaryCorpusBuffer = binaryCorpusFileChannel.map( FileChannel.MapMode.READ_ONLY, 0, binaryCorpusFileSize );

	    this.numberOfWords = binaryCorpusFileSize;
	    this.numberOfSentences = binarySentenceFileSize;
	}
	
	
	/**
	 * @return the number of words in the corpus. 
	 */
	public int size() {
		return numberOfWords;
	}
	
	
	/**
	 * @return the number of sentences in the corpus.
	 */
	public int getNumSentences() {
		return numberOfSentences;
	}
	
	
	public int getNumWords() {
		return numberOfWords;
	}
	
	
	public Vocabulary getVocabulary() {
		return vocabulary;
	}
	
	
	/**
	 * @return the integer representation of the Word at the
	 *         specified position in the corpus.
	 */
	public int getWordID(int position) {
		return binaryCorpusBuffer.getInt(position*4);	
	}
	
	
	/**
	 * @return the position in the corpus of the first word of
	 *         the specified sentence. If the sentenceID is
	 *         outside of the bounds of the sentences, then it
	 *         returns the last position in the corpus.
	 */
	public int getSentencePosition(int sentenceID) {
		if (sentenceID >= numberOfSentences) {
			return numberOfWords-1;
		}
		return binarySentenceBuffer.getInt(sentenceID*4);
	}
	
	
	/*
	public int[] findSentencesContaining(Phrase phrase) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public int[] findSentencesContaining(Phrase phrase, int maxSentences) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public int getNumOccurrences(Phrase phrase) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public Phrase getSentence(int sentenceIndex) {
		// TODO Auto-generated method stub
		return null;
	}
	*/
}
