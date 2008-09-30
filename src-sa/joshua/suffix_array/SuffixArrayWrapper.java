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
package joshua.suffix_array;

import java.util.Collections;
import java.util.List;


import edu.jhu.sa.util.sentence.Phrase;
import edu.jhu.sa.util.sentence.Span;


//TODO Come up with a better name for this class

/**
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-09-18 12:47:23 -0500 (Thu, 18 Sep 2008) $
 */
public class SuffixArrayWrapper {

	final SuffixArray source;
	final CorpusArray target;
	final AlignmentArray alignments;
	
	public SuffixArrayWrapper(CorpusArray source, CorpusArray target, AlignmentArray alignments) {
		this(new SuffixArray(source), target, alignments);
	}
	
	public SuffixArrayWrapper(SuffixArray source, CorpusArray target, AlignmentArray alignments) {
		this.source = source;
		this.target = target;
		this.alignments = alignments;
	}
	
	/**
	 * Gets a list of translations for a source phrase.
	 * 
	 * @param source a phrase in the source language
	 * @param sampleSize a translation will be found for no more than sampleSize instances of sourcePattern
	 * 
	 * @return a list of translations for the given source phrase
	 */
	public List<HierarchicalPhrase> getTranslations(Pattern sourcePattern, int sampleSize) {
		
		// We start with a source pattern that is not tied to any position in the source corpus
		
		
		// Find all instances in source suffix array where this pattern is found
		//  (collection of HierarchicalPhrase objects)
		
		// Iterate over each HierarchicalPhrase that matches the pattern, selecting no more than sampleSize instances
		//  step = numberOfInstances / sampleSize
		{
			// Get the start and end positions in the source corpus of the current HierarchicalPhrase
		
			// Get the span in the target corpus that aligns to the current match
		
			// Verify that there is a consistent alignment for the current HierarchicalPhrase
			{
				// To construct a target Phrase:
				// For each NT is the source phrase
				{
					// Get the target span of the source NT
					
					// Construct a list of all terminal word IDs in the target span
					
					// If there is a consistent alignment for the current NT
					{
					
						// In the target ID list, replace the terminals in the target NT span with a nonterminal ID
						
					} // Else
					{
						// do not consider this phrase
					}
				}
				
				// Add the new target Phrase to the results 
				
			}
		}
		
		// Return the results
		
		//int startSourceIndex = source.getCorpusStartPosition();
		//int endSourceIndex = source.getCorpusEndPosition() + 1;
		
		/*
		int[] span = source.findPhrase(sourcePattern);
		
		for (int i=span[0]; i<span[1]; i++) {
			
		}
		
		if (alignments.hasConsistentAlignment(startSourceIndex, endSourceIndex)) {
			
			if (source.pattern.arity()==0) {
				
				Span targetSpan = alignments.getAlignedTargetSpan(startSourceIndex, endSourceIndex);
				
				//XXX Is this the best way to get an array of word IDs?
				int[] targetWords = target.getPhrase(targetSpan.start, targetSpan.end).getWordIDs();//new int[targetSpan.size()];
				
				Pattern targetPattern = new Pattern(target.vocab, targetWords);
				
				int[] terminalStartingIndices = new int[1];
				terminalStartingIndices[0] = targetWords[0];
				
				new HierarchicalPhrase(targetPattern, terminalStartingIndices, target, targetWords.length);
			}
			
			throw new RuntimeException("Not implemented yet!");
			
		} else {
			return Collections.emptyList();
		}
		*/
		
		throw new RuntimeException("Not implemented yet!");
	}
}
