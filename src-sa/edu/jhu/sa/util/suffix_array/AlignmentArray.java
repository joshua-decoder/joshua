/* This file is part of the Joshua Machine Translation System.
 * 
 * Joshua is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or 
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package edu.jhu.sa.util.suffix_array;

// Imports
import java.io.*;

import edu.jhu.sa.util.sentence.Span;

/**
 * AlignmentArray is an auxiliary class which stores alignment information for  
 * a parallel corpus.  For each source word it stores the minimum and maximum index
 * of aligned words in the target corpus, and for each target word it stores the min
 * and max indexed of aligned words in the source corpus.  The intent is to increase 
 * the speed of the phrase extraction. 
 *
 * This class was inspired by a conversation with Adam Lopez. 
 *
 * @author Chris Callison-Burch
 * @since  13 May 2008
 * @version $LastChangedDate:2008-07-30 17:15:52 -0400 (Wed, 30 Jul 2008) $
 */

public class AlignmentArray {

//===============================================================
// Constants
//===============================================================

	public static final int UNALIGNED = Integer.MAX_VALUE;
	
//===============================================================
// Member variables
//===============================================================

	/** Stores the lowest index of aligned target words for each word in the source corpus. */  
	protected int[] lowestAlignedTargetIndex;
	
	/** Stores the highest index of aligned target words for each word in the source corpus. */
	protected int[] highestAlignedTargetIndex;

	/** Stores the lowest index of aligned source words for each word in the target corpus. */  
	protected int[] lowestAlignedSourceIndex;
	
	/** Stores the highest index of aligned source words for each word in the target corpus. */
	protected int[] highestAlignedSourceIndex;


 
//===============================================================
// Constructor(s)
//===============================================================

	/**
	 * This protected constructor is used by the SuffixArrayFactory.loadAlignmentArray and SuffixArrayFactory.createAlignmentArray methods. 
	 */
	protected AlignmentArray(int[] lowestAlignedTargetIndex, int[] highestAlignedTargetIndex,
							 int[] lowestAlignedSourceIndex, int[] highestAlignedSourceIndex) {
		this.lowestAlignedTargetIndex = lowestAlignedTargetIndex;
		this.highestAlignedTargetIndex = highestAlignedTargetIndex;
		this.lowestAlignedSourceIndex = lowestAlignedSourceIndex;
		this.highestAlignedSourceIndex = highestAlignedSourceIndex;
	}

//===============================================================
// Public
//===============================================================
	
	//===========================================================
	// Accessor methods (set/get)
	//===========================================================
	
	/**
	 * This method looks up target span for the given source span
	 * 
	 * @param startSourceIndex the staring position in the source corpus (inclusive)
	 * @param endSourceIndex the end position in the source corpus (exclusive) 
	 * @return a tuple containing the min and max indices in the target corpus, if the span is unaligned the value will be <UNALIGNED, undefined>
	 */
	public Span getAlignedTargetSpan(int startSourceIndex, int endSourceIndex) {
		return getAlignedSpan(startSourceIndex, endSourceIndex, lowestAlignedTargetIndex, highestAlignedTargetIndex);
	}
	
	public Span getAlignedTargetSpan(Span sourceSpan) {
		return getAlignedSpan(sourceSpan.start, sourceSpan.end, lowestAlignedTargetIndex, highestAlignedTargetIndex);
	}
	
	
	/**
	 * This method looks up source span for the given target span
	 * 
	 * @param startTargetIndex the staring position in the target corpus (inclusive)
	 * @param endTargetIndex the end position in the target corpus (exclusive) 
	 * @return a tuple containing the min and max indices in the source corpus, if the span is unaligned the value will be <UNALIGNED, undefined>
	 */
	public Span getAlignedSourceSpan(int startTargetIndex, int endTargetIndex) {
		return getAlignedSpan(startTargetIndex, endTargetIndex, lowestAlignedSourceIndex, highestAlignedSourceIndex);
	}
	
	/**
	 * This method determines whether there is a consistent word alignment for the
	 * specified source phrase.
	 * ccb - debugging
	 */
	public boolean hasConsistentAlignment(int startSourceIndex, int endSourceIndex) {
		Span targetSpan = getAlignedTargetSpan(startSourceIndex, endSourceIndex);
		//if(targetSpan[0] == UNALIGNED) return false;
		if(targetSpan.start == UNALIGNED) return false;
		// check back to see what sourceSpan the targetSpan aligns back to, so that we can check that it's within bounds
		Span sourceSpan = getAlignedSourceSpan(targetSpan.start, targetSpan.end);
		//int[] sourceSpan = getAlignedSourceSpan(targetSpan[0], targetSpan[1]);
		
		//if(sourceSpan[0] < startSourceIndex || sourceSpan[1] > endSourceIndex) {
		if(sourceSpan.start < startSourceIndex || sourceSpan.end > endSourceIndex) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Gets a target span that is consistent with the provided source span, if one exists.
	 * If no consistent span exists, returns null.
	 * 
	 * @param sourceSpan
	 * @return a target span that is consistent with the provided source span, if one exists, null otherwise
	 */
	public Span getConsistentTargetSpan(Span sourceSpan) {
		Span targetSpan = getAlignedTargetSpan(sourceSpan);
		
		if (targetSpan.start == UNALIGNED) return null;
		
		// check back to see what sourceSpan the targetSpan aligns back to, so that we can check that it's within bounds
		Span correspondingSourceSpan = getAlignedSourceSpan(targetSpan.start, targetSpan.end);
		
		if(correspondingSourceSpan.start < sourceSpan.start || correspondingSourceSpan.end > sourceSpan.end) {
			return null;
		} else {
			return targetSpan;
		}
	}
	
	
	//===========================================================
	// Methods
	//===========================================================


//===============================================================
// Protected 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================



//===============================================================
// Private 
//===============================================================
	
	//===============================================================
	// Methods
	//===============================================================

	
	
	/**
	 * This method looks up the minimum and maximum aligned indices for the span.
	 * 
	 * @param startIndex the staring word (inclusive)
	 * @param endIndex the end word (exclusive) 
	 * @return a tuple containing the min (inclusive) and max (exclusive) aligned indices, if the span is unaligned the value will be <UNALIGNED, ?>
	 */
	private Span getAlignedSpan(int startIndex, int endIndex, int[] lowestAlignedIndex, int[] highestAlignedIndex) {
		int lowestHighestMin = UNALIGNED;
		int lowestHighestMax = -1;
		
		//int[] lowestHighest = new int[2];
		//lowestHighest[0] = UNALIGNED;
		//lowestHighest[1] = -1;
		
		for(int i = startIndex; i < endIndex; i++) {
			if(lowestAlignedIndex[i] != UNALIGNED) {
				lowestHighestMin = ( lowestAlignedIndex[i] < lowestHighestMin) ?  lowestAlignedIndex[i] : lowestHighestMin; //Math.min(lowestAlignedIndex[i], lowestHighestMin);
				lowestHighestMax = (highestAlignedIndex[i] > lowestHighestMax) ? highestAlignedIndex[i] : lowestHighestMax; //Math.max(highestAlignedIndex[i], lowestHighestMax);
				//lowestHighest[0] = Math.min(lowestAlignedIndex[i], lowestHighest[0]);
				//lowestHighest[1] = Math.max(highestAlignedIndex[i], lowestHighest[1]);
			}
		}
		
		//lowestHighest[1]++;
		lowestHighestMax++;
		
		//return lowestHighest;
		return new Span(lowestHighestMin,lowestHighestMax);
	}
	

	
//===============================================================
// Static
//===============================================================


//===============================================================
// Main 
//===============================================================

	public static void main(String[] args) throws IOException {
		
		if(args.length != 5) {
			System.out.println("Usage: java AlignmentArray sourceLang targetLang corpusName dir alignmentsFilename");
			System.exit(0);
		}
		
		String sourceLang = args[0];
		String targetLang = args[1];
		String corpusName = args[2];
		String directory = args[3];
		//String alignmentsFilename = args[4];
		
		SuffixArray sourceCorpus = SuffixArrayFactory.loadSuffixArray(sourceLang, corpusName, directory);
		SuffixArray targetCorpus = SuffixArrayFactory.loadSuffixArray(targetLang, corpusName, directory);
		AlignmentArray alignmentArray = SuffixArrayFactory.loadAlignmentArray(sourceLang, targetLang, corpusName, directory);
		
		// ccb - debugging
		for(int i = 0; i < sourceCorpus.getNumWords(); i++) {
			for(int j = i+1; j <= sourceCorpus.getNumWords(); j++) {
				//int[] alignedTargetWords = alignmentArray.getAlignedTargetSpan(i, j);
				Span alignedTargetWords = alignmentArray.getAlignedTargetSpan(i, j);
				if(alignmentArray.hasConsistentAlignment(i, j)) {
					//System.out.println(sourceCorpus.getPhrase(i, j) + " [" + i + "," + j + "]\t" + targetCorpus.getPhrase(alignedTargetWords[0], alignedTargetWords[1])  + " [" + alignedTargetWords[0] + "," + (alignedTargetWords[1]) + "]");
					System.out.println(sourceCorpus.getPhrase(i, j) + " [" + i + "," + j + "]\t" + targetCorpus.getPhrase(alignedTargetWords.start, alignedTargetWords.end)  + " [" + alignedTargetWords.start + "," + (alignedTargetWords.end) + "]");
				} else {
//					System.out.println(sourceCorpus.getPhrase(i, j) + ": NO CONSISTENT ALIGNMENT");
				}
			}
		}


	}
}

