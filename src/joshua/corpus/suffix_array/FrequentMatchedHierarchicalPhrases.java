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
package joshua.corpus.suffix_array;

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;

/**
 *
 *
 * @author Lane Schwartz
 */
public abstract class FrequentMatchedHierarchicalPhrases extends
		AbstractHierarchicalPhrases {

	protected FrequentMatchedHierarchicalPhrases(Pattern pattern, int numPhrases) {
		super(pattern, numPhrases);
		throw new RuntimeException("Not supported");
		// TODO Auto-generated constructor stub
	}

//	private final FrequentMatches frequentMatches;
//	private final Corpus corpus;
//	
//	public FrequentMatchedHierarchicalPhrases(Pattern pattern, FrequentMatches frequentMatches, Corpus corpus) {
//		super(pattern, frequentMatches.getMatchCount(pattern));
//		
//		this.frequentMatches = frequentMatches;
//		this.corpus = corpus;
//	}
//
//	
//	/* @see joshua.corpus.MatchedHierarchicalPhrases#getSentenceNumber(int) */
//	public int getSentenceNumber(int phraseIndex) {
//		
//		int position = frequentMatches.getStartPosition(pattern, phraseIndex, 0);
//		return corpus.getSentenceIndex(position);
//	}
//	
//	
//	/* @see joshua.corpus.MatchedHierarchicalPhrases#getStartPosition(int, int) */
//	public int getStartPosition(int phraseIndex, int positionNumber) {
//		
//		return frequentMatches.getStartPosition(pattern, phraseIndex, positionNumber);
//		
//	}
//	
//	/* @see joshua.corpus.MatchedHierarchicalPhrases#isEmpty() */
//	public boolean isEmpty() {
//		return ! frequentMatches.contains(pattern);
//	}
//	
//	/* @see joshua.corpus.MatchedHierarchicalPhrases#copyWithFinalX() */
//	public MatchedHierarchicalPhrases copyWithFinalX() {
//		
//		final Pattern patternX = getPatternWithFinalX();
//		final FrequentMatchedHierarchicalPhrases parent = this;
//		
//		return new FrequentMatchedHierarchicalPhrases(patternX, frequentMatches, corpus) {
//			public int getStartPosition(int phraseIndex, int positionNumber) {
//				return parent.getStartPosition(phraseIndex, positionNumber);
//			}
//			
//			public boolean isEmpty() {
//				return parent.isEmpty();
//			}
//		};
//		
//	}
//
//	/* @see joshua.corpus.MatchedHierarchicalPhrases#copyWithInitialX() */
//	public MatchedHierarchicalPhrases copyWithInitialX() {
//		
//		final Pattern xPattern = getPatternWithInitialX();
//		final FrequentMatchedHierarchicalPhrases parent = this;
//		
//		return new FrequentMatchedHierarchicalPhrases(xPattern, frequentMatches, corpus) {
//			public int getStartPosition(int phraseIndex, int positionNumber) {
//				return parent.getStartPosition(phraseIndex, positionNumber);
//			}
//			
//			public boolean isEmpty() {
//				return parent.isEmpty();
//			}
//		};
//	}

}
