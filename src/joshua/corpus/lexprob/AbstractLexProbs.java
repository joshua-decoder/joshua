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
package joshua.corpus.lexprob;

import joshua.corpus.alignment.AlignmentGrid;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;
import joshua.util.Lists;
import joshua.util.Lists.IndexedInt;

/**
 * Implements lexical probability methods 
 * which will be directly reusable by most implementations.
 * 
 * @author Lane Schwartz
 */
public abstract class AbstractLexProbs implements LexicalProbabilities {

	/* See Javadoc for LexicalProbabilities#getTargetGivenSourceAlignments(Pattern,Pattern). */
	public AlignmentGrid getTargetGivenSourceAlignments(Pattern targetPattern, Pattern sourcePattern) {
		
		SymbolTable sourceVocab = getSourceVocab();
		SymbolTable targetVocab = getTargetVocab();
		
		StringBuilder alignmentPoints = new StringBuilder();
		
		for (IndexedInt indexedTarget : Lists.eachWithIndex(targetPattern.getWordIDs())) {
			final int targetWord = indexedTarget.getValue();
			final int targetIndex = indexedTarget.getIndex();
			
			if (targetVocab.isNonterminal(targetWord)) {
				//TODO Do something special here
				
			} else {
				
				float max = targetGivenSource(targetWord, null);
				Integer bestSourceIndex = null;
				
				for (IndexedInt indexedSource : Lists.eachWithIndex(sourcePattern.getWordIDs())) {	
					int sourceWord = indexedSource.getValue();
					int sourceIndex = indexedSource.getIndex();
					
					if (! sourceVocab.isNonterminal(sourceWord)) {
						float score = this.targetGivenSource(targetWord, sourceWord);
						if (score > max) {
							max = score;
							bestSourceIndex = sourceIndex;
						}
						
					}
					
				}
				
				if (bestSourceIndex != null) {
					alignmentPoints.append(bestSourceIndex);
					alignmentPoints.append('-');
					alignmentPoints.append(targetIndex);
					alignmentPoints.append(' ');
				}
				
			}
			
		}
		
		return new AlignmentGrid(alignmentPoints.toString());
	}
	
	/* See Javadoc for LexicalProbabilities#getSourceGivenTargetAlignments(Pattern,Pattern). */
	public AlignmentGrid getSourceGivenTargetAlignments(Pattern sourcePattern, Pattern targetPattern) {
		
		SymbolTable sourceVocab = getSourceVocab();
		SymbolTable targetVocab = getTargetVocab();
		
		StringBuilder alignmentPoints = new StringBuilder();
		
		for (IndexedInt indexedSource : Lists.eachWithIndex(sourcePattern.getWordIDs())) {			
			int sourceWord = indexedSource.getValue();
			int sourceIndex = indexedSource.getIndex();
			
			if (sourceVocab.isNonterminal(sourceWord)) {
				//TODO Do something special here
				
			} else {
			
				float max = sourceGivenTarget(sourceWord, null);
				Integer bestTargetIndex = null;
				
				for (IndexedInt indexedTarget : Lists.eachWithIndex(targetPattern.getWordIDs())) {				
					int targetWord = indexedTarget.getValue();
					int targetIndex = indexedTarget.getIndex();
					
					if (! targetVocab.isNonterminal(targetWord)) {
						float score = this.sourceGivenTarget(sourceWord, targetWord);
						if (score > max) {
							max = score;
							bestTargetIndex = targetIndex;
						}
					}
					
				}
				
				
				if (bestTargetIndex != null) {
					alignmentPoints.append(sourceIndex);
					alignmentPoints.append('-');
					alignmentPoints.append(bestTargetIndex);
					alignmentPoints.append(' ');
				}
			}
		
			sourceIndex += 1;
		}
		
		return new AlignmentGrid(alignmentPoints.toString());
	}
	
	/* See Javadoc for LexicalProbabilities#lexProbSourceGivenTarget(Pattern,Pattern). */
	public float lexProbSourceGivenTarget(Pattern sourcePattern, Pattern targetPattern) {
		
		float sourceGivenTarget = 1.0f;
		
		for (Integer sourceWord : sourcePattern.getTerminals()) {
			float max = sourceGivenTarget(sourceWord, null);
			for (Integer targetWord : targetPattern.getTerminals()) {
				float score = this.sourceGivenTarget(sourceWord, targetWord);
				if (score > max) {
					max = score;
				}
			}
			sourceGivenTarget *= max;
		}
		
		if (sourceGivenTarget <= 0) {
			sourceGivenTarget = getFloorProbability();
		} 
		
		return sourceGivenTarget;
	}
	
	/* See Javadoc for LexicalProbabilities#lexProbTargetGivenSource(Pattern,Pattern). */
	public float lexProbTargetGivenSource(Pattern targetPattern, Pattern sourcePattern) {
		
		float targetGivenSource = 1.0f;
		
		for (Integer targetWord : targetPattern.getTerminals()) {
		
			float max = targetGivenSource(targetWord, null);
			for (Integer sourceWord : sourcePattern.getTerminals()) {
				float score = this.targetGivenSource(targetWord, sourceWord);
				if (score > max) {
					max = score;
				}
			}
			targetGivenSource *= max;
		}
		
		if (targetGivenSource <= 0) {
			targetGivenSource = getFloorProbability();
		} 
		
		return targetGivenSource;
	}
	
}
