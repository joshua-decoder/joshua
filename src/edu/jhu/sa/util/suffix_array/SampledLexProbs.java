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
package edu.jhu.sa.util.suffix_array;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import joshua.util.lexprob.LexicalProbabilities;
import joshua.util.sentence.Vocabulary;


/**
 * Represents lexical probability distributions in both directions.
 * <p>
 * This class calculates the probabilities by sampling directly from a parallel corpus.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class SampledLexProbs implements LexicalProbabilities {

	/** Logger for this class. */
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(SampledLexProbs.class.getName());
	
	private final Map<Integer,Map<Integer,Float>> sourceGivenTarget;
	private final Map<Integer,Map<Integer,Float>> targetGivenSource;
	
	private final SuffixArray sourceSuffixArray;
	private final SuffixArray targetSuffixArray;
	
	private final AlignmentArray alignments;
	
	private final Vocabulary sourceVocab;
	private final Vocabulary targetVocab;
	
	private final float floorProbability;
	
	private final int sampleSize;
	
	public SampledLexProbs(int sampleSize, SuffixArray sourceSuffixArray, SuffixArray targetSuffixArray, AlignmentArray alignments, boolean precalculate) {
		
		this.sampleSize = sampleSize;
		this.sourceSuffixArray = sourceSuffixArray;
		this.targetSuffixArray = targetSuffixArray;
		this.alignments = alignments;
		this.sourceVocab = sourceSuffixArray.getVocabulary();
		this.targetVocab = targetSuffixArray.getVocabulary();
		this.floorProbability = 1.0f/(sampleSize*100);
		this.sourceGivenTarget = new HashMap<Integer,Map<Integer,Float>>();
		this.targetGivenSource = new HashMap<Integer,Map<Integer,Float>>();
		
		if (precalculate) {
		
			for (int sourceWord : sourceVocab.getAllIDs()) {
				calculateTargetGivenSource(sourceWord);
			}
		
			for (int targetWord : targetVocab.getAllIDs()) {
				calculateSourceGivenTarget(targetWord);
			}
		}
		
	}
	
	
	/**
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	public float sourceGivenTarget(Integer sourceWord, Integer targetWord) {
		
		if (!sourceGivenTarget.containsKey(targetWord)) {
			calculateSourceGivenTarget(targetWord);
		}
		
		Map<Integer,Float> map = sourceGivenTarget.get(targetWord);
		if (map.containsKey(sourceWord)) {
			return sourceGivenTarget.get(targetWord).get(sourceWord);
		} else {
			return floorProbability;
		}

	}
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
	 */
	public float targetGivenSource(Integer targetWord, Integer sourceWord) {
		
		if (!targetGivenSource.containsKey(sourceWord)) {
			calculateTargetGivenSource(sourceWord);
		}

		Map<Integer,Float> map = targetGivenSource.get(sourceWord);
		if (map.containsKey(targetWord)) {
			return targetGivenSource.get(sourceWord).get(targetWord);
		} else {
			return floorProbability;
		}
		
	}
	
	/**
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	public float sourceGivenTarget(String sourceWord, String targetWord) {
		int targetID = targetVocab.getID(targetWord);
		int sourceID = sourceVocab.getID(sourceWord);
		return sourceGivenTarget(sourceID, targetID);
	}
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
	 */
	public float targetGivenSource(String targetWord, String sourceWord) {
		int targetID = targetVocab.getID(targetWord);
		int sourceID = sourceVocab.getID(sourceWord);
		return targetGivenSource(targetID, sourceID);
	}

	
	private void calculateSourceGivenTarget(Integer targetWord) {

		Map<Integer,Integer> counts = new HashMap<Integer,Integer>();
		
		int[] targetSuffixArrayBounds = targetSuffixArray.findPhrase(new BasicPhrase(targetVocab, targetWord));
		int step = (targetSuffixArrayBounds[1]-targetSuffixArrayBounds[0]<sampleSize) ? 1 : targetSuffixArrayBounds[1]-targetSuffixArrayBounds[0] / sampleSize;
		
		float total = 0;
		
		for (int targetSuffixArrayIndex=targetSuffixArrayBounds[0],samples=0; targetSuffixArrayIndex<=targetSuffixArrayBounds[1] && samples<sampleSize; targetSuffixArrayIndex+=step, samples++) {
			int targetCorpusIndex = targetSuffixArray.suffixes[targetSuffixArrayIndex];
			int[] alignedSourceIndices = alignments.getAlignedSourceIndices(targetCorpusIndex);
			if (alignedSourceIndices==null) {
				if (!counts.containsKey(null)) {
					counts.put(null,1);
				} else {
					counts.put(null,
							counts.get(null) + 1);
				}
				total++;
			} else {
				for (int sourceIndex : alignedSourceIndices) {
					int sourceWord = sourceSuffixArray.corpus.getWordID(sourceIndex);
					if (!counts.containsKey(sourceWord)) {
						counts.put(sourceWord,1);
					} else {
						counts.put(sourceWord,
								counts.get(sourceWord) + 1);
					}
					total++;
				}
			}
		}
		
		Map<Integer,Float> sourceProbs = new HashMap<Integer,Float>();
		for (Map.Entry<Integer,Integer> entry : counts.entrySet()) {
			sourceProbs.put(entry.getKey(), entry.getValue()/total);
		}
		sourceGivenTarget.put(targetWord, sourceProbs);
	}
	
	private void calculateTargetGivenSource(int sourceWord) {

		Map<Integer,Integer> counts = new HashMap<Integer,Integer>();
		
		int[] sourceSuffixArrayBounds = sourceSuffixArray.findPhrase(new BasicPhrase(sourceVocab, sourceWord));
		int step = (sourceSuffixArrayBounds[1]-sourceSuffixArrayBounds[0]<sampleSize) ? 1 : sourceSuffixArrayBounds[1]-sourceSuffixArrayBounds[0] / sampleSize;
		
		float total = 0;
		
		for (int sourceSuffixArrayIndex=sourceSuffixArrayBounds[0],samples=0; sourceSuffixArrayIndex<=sourceSuffixArrayBounds[1] && samples<sampleSize; sourceSuffixArrayIndex+=step, samples++) {
			int sourceCorpusIndex = sourceSuffixArray.suffixes[sourceSuffixArrayIndex];
			int[] alignedTargetIndices = alignments.getAlignedTargetIndices(sourceCorpusIndex);
			if (alignedTargetIndices==null) {
				if (!counts.containsKey(null)) {
					counts.put(null,1);
				} else {
					counts.put(null,
							counts.get(null) + 1);
				}
				total++;
			} else {
				for (int targetIndex : alignedTargetIndices) {
					int targetWord = targetSuffixArray.corpus.getWordID(targetIndex);
					if (!counts.containsKey(targetWord)) {
						counts.put(targetWord,1);
					} else {
						counts.put(targetWord,
								counts.get(targetWord) + 1);
					}
					total++;
				}
			}
		}
		
		Map<Integer,Float> targetProbs = new HashMap<Integer,Float>();
		for (Map.Entry<Integer,Integer> entry : counts.entrySet()) {
			targetProbs.put(entry.getKey(), entry.getValue()/total);
		}
		targetGivenSource.put(sourceWord, targetProbs);
		
	}
}
