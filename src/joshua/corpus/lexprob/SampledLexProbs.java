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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.CorpusArray;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.suffix_array.BasicPhrase;
import joshua.corpus.suffix_array.HierarchicalPhrase;
import joshua.corpus.suffix_array.SuffixArray;
import joshua.corpus.suffix_array.SuffixArrayFactory;
import joshua.corpus.suffix_array.Suffixes;
import joshua.corpus.vocab.SymbolTable;
import joshua.util.Cache;
import joshua.util.Pair;


/**
 * Represents lexical probability distributions in both directions.
 * <p>
 * This class calculates the probabilities by sampling directly
 * from a parallel corpus.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate:2008-11-13 13:13:31 -0600 (Thu, 13 Nov 2008) $
 * @deprecated
 */
public class SampledLexProbs extends AbstractLexProbs {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(SampledLexProbs.class.getName());
	
	private final Cache<Integer,Map<Integer,Float>> sourceGivenTarget;
	private final Cache<Integer,Map<Integer,Float>> targetGivenSource;
	
	private final Suffixes sourceSuffixArray;
	private final Suffixes targetSuffixArray;
	
	/** Corpus array representing the target language corpus. */
	final Corpus targetCorpus; 
	
	/**
	 * Represents alignments between words in the source corpus
	 * and the target corpus.
	 */
	private final Alignments alignments;
	
	private final SymbolTable sourceVocab;
	private final SymbolTable targetVocab;
	
	private final float floorProbability;
	
	/** 
	 * When calculating probabilities, if a probability is less
	 * than this value, do not explicitly store it.
	 */
	private final float thresholdProbability;
	
	private final int sampleSize;
	

	public SampledLexProbs(int sampleSize, Suffixes sourceSuffixArray, Suffixes targetSuffixArray, Alignments alignments, int cacheCapacity, boolean precalculate) {
		
		this.sampleSize = sampleSize;
		this.sourceSuffixArray = sourceSuffixArray;
		this.targetSuffixArray = targetSuffixArray;
		this.targetCorpus = targetSuffixArray.getCorpus();
		this.alignments = alignments;
		this.sourceVocab = sourceSuffixArray.getVocabulary();
		this.targetVocab = targetSuffixArray.getVocabulary();
		this.thresholdProbability = 1.0f/(sampleSize*100); //TODO come up with a good value for this
		this.floorProbability = 1.0f/(sampleSize*100);
		this.sourceGivenTarget = new Cache<Integer,Map<Integer,Float>>(cacheCapacity);
		this.targetGivenSource = new Cache<Integer,Map<Integer,Float>>(cacheCapacity);
		
		if (precalculate) {
		
			for (int sourceWord : sourceVocab.getAllIDs()) {
				calculateTargetGivenSource(sourceWord);
			}
		
			for (int targetWord : targetVocab.getAllIDs()) {
				calculateSourceGivenTarget(targetWord);
			}
		}
		
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		s.append("SampledLexProbs size information:");
		s.append('\n');
		
		s.append(sourceGivenTarget.size() + " target sides in sourceGivenTarget");
		s.append('\n');
		
		int count = 0;
		for (Map<Integer, Float> entry : sourceGivenTarget.values()) {
			count += entry.size();
		}
		
		s.append(count + " source-target pairs in sourceGivenTarget");
		s.append('\n');
		
		
		s.append(targetGivenSource.size() + " source sides in targetGivenSource");
		s.append('\n');
		
		count = 0;
		for (Map<Integer, Float> entry : targetGivenSource.values()) {
			count += entry.size();
		}
		
		s.append(count + " target-source pairs in targetGivenSource");
		s.append('\n');		
		
		
		return s.toString();
	}
	
	/**
	 * For unit testing.
	 * 
	 * @param sourceCorpusString
	 * @param targetCorpusString
	 * @param alignmentString
	 * @return
	 * @throws IOException
	 */
	public static SampledLexProbs getSampledLexProbs(String sourceCorpusString, String targetCorpusString, String alignmentString) throws IOException {

		String sourceFileName;
		{
			File sourceFile = File.createTempFile("source", new Date().toString());
			PrintStream sourcePrintStream = new PrintStream(sourceFile);
			sourcePrintStream.println(sourceCorpusString);
			sourcePrintStream.close();
			sourceFileName = sourceFile.getAbsolutePath();
		}
	
		String targetFileName;
		{
			File targetFile = File.createTempFile("target", new Date().toString());
			PrintStream targetPrintStream = new PrintStream(targetFile);
			targetPrintStream.println(targetCorpusString);
			targetPrintStream.close();
			targetFileName = targetFile.getAbsolutePath();
		}
		
		String alignmentFileName;
		{
			File alignmentFile = File.createTempFile("alignment", new Date().toString());
			PrintStream alignmentPrintStream = new PrintStream(alignmentFile);
			alignmentPrintStream.println(alignmentString);
			alignmentPrintStream.close();
			alignmentFileName = alignmentFile.getAbsolutePath();
		}
		
		CorpusArray sourceCorpusArray =
			SuffixArrayFactory.createCorpusArray(sourceFileName);
		SuffixArray sourceSuffixArray = 
			SuffixArrayFactory.createSuffixArray(sourceCorpusArray, SuffixArray.DEFAULT_CACHE_CAPACITY);
		
		CorpusArray targetCorpusArray =
			SuffixArrayFactory.createCorpusArray(targetFileName);
		SuffixArray targetSuffixArray = 
			SuffixArrayFactory.createSuffixArray(targetCorpusArray, SuffixArray.DEFAULT_CACHE_CAPACITY);

		Alignments alignmentArray = SuffixArrayFactory.createAlignments(alignmentFileName, sourceSuffixArray, targetSuffixArray);

		return new SampledLexProbs(Integer.MAX_VALUE, sourceSuffixArray, targetSuffixArray, alignmentArray, Cache.DEFAULT_CAPACITY, false);
		
	}
	
	/**
	 * Calculates the lexical probability of a source word given
	 * a target word.
	 * <p>
	 * If this information has not previously been stored, this
	 * method calculates it.
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	public float sourceGivenTarget(Integer sourceWord, Integer targetWord) {
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Need to get source given target lexprob p(" + sourceVocab.getWord(sourceWord) + " | " +  targetVocab.getWord(targetWord) + "); sourceWord ID == " + sourceWord + "; targetWord ID == " + targetWord);
				
		if (!sourceGivenTarget.containsKey(targetWord)) {
			calculateSourceGivenTarget(targetWord);
		}
		
		Map<Integer,Float> map = sourceGivenTarget.get(targetWord);
		if (map.containsKey(sourceWord)) {
			return sourceGivenTarget.get(targetWord).get(sourceWord);
		} else {
			if (logger.isLoggable(Level.FINE)) logger.fine("No source given target lexprob found for p(" + sourceVocab.getWord(sourceWord) + " | " + targetVocab.getWord(targetWord) + "); returning FLOOR_PROBABILITY " + floorProbability);
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
		
		if (logger.isLoggable(Level.FINE)) logger.fine("Need to get target given source lexprob p(" + targetVocab.getWord(targetWord) + " | " + sourceVocab.getWord(sourceWord) + "); sourceWord ID == " + sourceWord + "; targetWord ID == " + targetWord);
		
		if (!targetGivenSource.containsKey(sourceWord)) {
			calculateTargetGivenSource(sourceWord);
		}

		Map<Integer,Float> map = targetGivenSource.get(sourceWord);
		if (map.containsKey(targetWord)) {
			return map.get(targetWord);
		} else {
			if (logger.isLoggable(Level.FINE)) logger.fine("No target given source lexprob found for p(" + targetVocab.getWord(targetWord) + " | " + sourceVocab.getWord(sourceWord) + "); returning FLOOR_PROBABILITY " + floorProbability + "; sourceWord ID == " + sourceWord + "; targetWord ID == " + targetWord);
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
		if (logger.isLoggable(Level.FINER)) logger.finer("Need to get target given source lexprob p(" + targetWord + " | " + sourceWord + "); sourceID==" +sourceVocab.getID(sourceWord) + "; targetID=="+targetVocab.getID(targetWord));
		int targetID = targetVocab.getID(targetWord);
		int sourceID = sourceVocab.getID(sourceWord);
		return targetGivenSource(targetID, sourceID);
	}

	
	/**
	 * Calculates the lexical translation probabilities (in
	 * both directions) for a specific instance of a source
	 * phrase in the corpus.
	 * <p>
	 * This method does NOT currently handle NULL aligned points
	 * according to Koehn et al (2003). This may change in
	 * future releases.
	 * <p>
	 * The problem arises when we need to calculate the
	 * word-to-word lexical weights using the sourceGivenTarget
	 * and targetGivenSource methods (actual calculations occur
	 * in calculateSourceGivenTarget and calculateTargetGivenSource).
	 * <p>
	 * Let's say we want to calculate P(s14 | t75). (s14 is a
	 * source word, t75 is a target word) We call sourceGivenTarget
	 * and see that we haven't calculated the map for P(? | t75),
	 * so we call calculateSourceGivenTarget(t75).
	 * <p>
	 * The calculateSourceGivenTarget method looks up all
	 * instances of t75 in the target suffix array. It then
	 * samples some of those instances and looks up the aligned
	 * source word(s) for each sampled target word. Based on
	 * that, probabilities are calculated and stored.
	 * <p>
	 * Now, what happens if instead of t75, we have NULL?
	 * <p>
	 * The calculateSourceGivenTarget cannot look up all instances
	 * of NULL in the target suffix array. This is a problem.
	 * <p>
	 * We have access to all the information we need to calculate
	 * null lexical translation probabilities. But, this would
	 * probably be best done as a pre-process.
	 * <p>
	 * One possible solution would be to have a pre-process
	 * that steps through each line in the alignment array to
	 * find null alignment points and calculates null probabilities
	 * at that point.
	 * 
	 * @param sourcePhrases
	 * @param sourcePhraseIndex
	 * @param targetPhrase
	 * @return the lexical probability and reverse lexical
	 *         probability
	 */
	public Pair<Float,Float> calculateLexProbs(MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex, HierarchicalPhrase targetPhrase) {
		
//		HierarchicalPhrase sourcePhrase = sourcePhrases.get(sourcePhraseIndex, sourceSuffixArray.getCorpus());
		
		float sourceGivenTarget = 1.0f;
		
		Map<Integer,List<Integer>> reverseAlignmentPoints = new HashMap<Integer,List<Integer>>(); 
		
		// Iterate over each terminal sequence in the source phrase
//		for (int seq=0; seq<sourcePhrases.size(); seq++) {
//		int a = sourcePhrase.terminalSequenceStartIndices.length;
//		int b = sourcePhrases.size();
//		if (a!=b) {
//			int x=0; x++;
//
//			int z = sourcePhrases.getNumberOfTerminalSequences();
//			Corpus corpus = sourceSuffixArray.getCorpus();
//			sourcePhrases.get(sourcePhraseIndex, corpus);
//		}
		for (int seq=0; seq<sourcePhrases.getNumberOfTerminalSequences(); seq++) {
//		for (int seq=0; seq<sourcePhrase.terminalSequenceStartIndices.length; seq++) {
			
			// Iterate over each source index in the current terminal sequence
			for (int sourceWordIndex=sourcePhrases.getTerminalSequenceStartIndex(sourcePhraseIndex, seq),
//					end=sourcePhrase.terminalSequenceEndIndices[seq]; 
					end=sourcePhrases.getTerminalSequenceEndIndex(sourcePhraseIndex, seq);
			sourceWordIndex<end; 
			sourceWordIndex++) {
//			for (int sourceWordIndex=sourcePhrase.terminalSequenceStartIndices[seq]; 
//					sourceWordIndex<sourcePhrase.terminalSequenceEndIndices[seq]; 
//					sourceWordIndex++) {
				
				float sum = 0.0f;
				
//				int sourceWord = sourceSuffixArray.corpus.corpus[sourceWordIndex];
				int sourceWord = sourceSuffixArray.getCorpus().getWordID(sourceWordIndex);
				int[] targetIndices = alignments.getAlignedTargetIndices(sourceWordIndex);
				
				if (targetIndices==null) {
					
					//XXX We are not handling NULL aligned points according to Koehn et al (2003)
				
					//float sourceGivenNullAlignment = sourceGivenTarget(sourceWord, null);
					//sourceGivenTarget *= sourceGivenNullAlignment;
					
					//throw new RuntimeException("No alignments for source word at index " + sourceWordIndex);
					
				} else {
					// Iterate over each target index aligned to the current source word
					for (int targetIndex : targetIndices) {

//						int targetWord = targetCorpus.corpus[targetIndex];
						int targetWord = targetCorpus.getWordID(targetIndex);
						sum += sourceGivenTarget(sourceWord, targetWord);

						// Keeping track of the reverse alignment points 
						//   (we need to do this convoluted step because we don't actually have a HierarchicalPhrase for the target side)
						if (!reverseAlignmentPoints.containsKey(targetIndex)) {
							reverseAlignmentPoints.put(targetIndex, new ArrayList<Integer>());
						}
						reverseAlignmentPoints.get(targetIndex).add(sourceWord);

					}

					float average = sum / targetIndices.length;
					sourceGivenTarget *= average;
				}
			}
			
		}
		
		float targetGivenSource = 1.0f;
		
		// Actually calculate the reverse lexical translation probabilities
		for (Map.Entry<Integer, List<Integer>> entry : reverseAlignmentPoints.entrySet()) {
			
//			int targetWord = targetCorpus.corpus[entry.getKey()];
			int targetWord = targetCorpus.getWordID(entry.getKey());
			float sum = 0.0f;
			
			List<Integer> alignedSourceWords = entry.getValue();
			
			for (int sourceWord : alignedSourceWords) {
				sum += targetGivenSource(targetWord, sourceWord);
			}
			float average = sum / ((float) alignedSourceWords.size());
			targetGivenSource *= average;
		}

		return new Pair<Float,Float>(sourceGivenTarget,targetGivenSource);
	}
	
	
	/**
	 * Calculates the lexical probabilities for a target word.
	 * 
	 * @param targetWord
	 */
	private void calculateSourceGivenTarget(Integer targetWord) {

		Map<Integer,Integer> counts = new HashMap<Integer,Integer>();
		
		int[] targetSuffixArrayBounds = targetSuffixArray.findPhrase(new BasicPhrase(targetVocab, targetWord));
		int step = (targetSuffixArrayBounds[1]-targetSuffixArrayBounds[0]<sampleSize) ? 1 : (targetSuffixArrayBounds[1]-targetSuffixArrayBounds[0]) / sampleSize;
		
		float total = 0;
		
		for (int targetSuffixArrayIndex=targetSuffixArrayBounds[0],samples=0; targetSuffixArrayIndex<=targetSuffixArrayBounds[1] && samples<sampleSize; targetSuffixArrayIndex+=step, samples++) {
			int targetCorpusIndex = targetSuffixArray.getCorpusIndex(targetSuffixArrayIndex);
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
					int sourceWord = sourceSuffixArray.getCorpus().getWordID(sourceIndex);
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
			// entry.getKey() corresponds to the source word
			// entry.getValue() corresponds to the number of times we have seen this source/target word pair
			// total is the number of times we saw this target with any source word
			float prob = entry.getValue()/total;
			if (prob > thresholdProbability) {
				sourceProbs.put(entry.getKey(), prob);
			} else {
				// Don't explicitly store a probability for this source-target pair
				// Instead, when querying for this pair return the floor value.
			}
		}
		sourceGivenTarget.put(targetWord, sourceProbs);
	}
	
	private void calculateTargetGivenSource(int sourceWord) {

		if (logger.isLoggable(Level.FINE)) logger.fine("Calculating lexprob distribution P( TARGET | " + sourceVocab.getWord(sourceWord) + "); sourceWord ID == " + sourceWord);
				
		Map<Integer,Integer> counts = new HashMap<Integer,Integer>();
		
		int[] sourceSuffixArrayBounds = sourceSuffixArray.findPhrase(new BasicPhrase(sourceVocab, sourceWord));
		int step = (sourceSuffixArrayBounds[1]-sourceSuffixArrayBounds[0]<sampleSize) ? 1 : (sourceSuffixArrayBounds[1]-sourceSuffixArrayBounds[0]) / sampleSize;
		
		float total = 0;
		
		for (int sourceSuffixArrayIndex=sourceSuffixArrayBounds[0],samples=0; sourceSuffixArrayIndex<=sourceSuffixArrayBounds[1] && samples<sampleSize; sourceSuffixArrayIndex+=step, samples++) {
			int sourceCorpusIndex = sourceSuffixArray.getCorpusIndex(sourceSuffixArrayIndex);
			int[] alignedTargetIndices = alignments.getAlignedTargetIndices(sourceCorpusIndex);
			if (alignedTargetIndices==null) {
				if (!counts.containsKey(null)) {
					if (logger.isLoggable(Level.FINEST)) logger.finest("Setting count(null | " + sourceVocab.getWord(sourceWord) + ") = 1");
					counts.put(null,1);
				} else {
					counts.put(null,
							counts.get(null) + 1);
				}
				total++;

			} else {
				for (int targetIndex : alignedTargetIndices) {
					int targetWord = targetSuffixArray.getCorpus().getWordID(targetIndex);
					if (!counts.containsKey(targetWord)) {
						if (logger.isLoggable(Level.FINEST)) logger.finest("Setting count(" +targetVocab.getWord(targetWord) + " | " + sourceVocab.getWord(sourceWord) + ") = 1" + "; sourceWord ID == " + sourceWord + "; targetWord ID == " + targetWord);
						counts.put(targetWord,1);
					} else {
						int incrementedCount = counts.get(targetWord) + 1;
						if (logger.isLoggable(Level.FINEST)) logger.finest("Setting count(" +targetVocab.getWord(targetWord) + " | " + sourceVocab.getWord(sourceWord) + ") = " + incrementedCount + "; sourceWord ID == " + sourceWord + "; targetWord ID == " + targetWord);
						counts.put(targetWord,incrementedCount);
					}
					total++;

				}
			}
		}
		
		Map<Integer,Float> targetProbs = new HashMap<Integer,Float>();
		for (Map.Entry<Integer,Integer> entry : counts.entrySet()) {
			// entry.getKey() corresponds to the target word
			// entry.getValue() corresponds to the number of times we have seen this target/source word pair
			// total is the number of times we saw this source with any target word
			Integer targetWord = entry.getKey();
			float prob = ((float) entry.getValue())/total;
			if (prob > thresholdProbability) {
				if (logger.isLoggable(Level.FINEST)) logger.finest("Setting p(" +targetVocab.getWord(entry.getKey()) + " | " + sourceVocab.getWord(sourceWord) + ") = " + prob + "; sourceWord ID == " + sourceWord + "; targetWord ID == " + targetWord);
				targetProbs.put(targetWord, prob);
			} else {
				// Don't explicitly store a probability for this source-target pair
				// Instead, when querying for this pair return the floor value.
			}
		}
		if (logger.isLoggable(Level.FINER)) logger.finer("Storing " + targetProbs.size() + " probabilities for lexprob distribution P( TARGET | " + sourceVocab.getWord(sourceWord) + ")");
		targetGivenSource.put(sourceWord, targetProbs);
		
	}

	public float lexProbSourceGivenTarget(
			MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex,
			HierarchicalPhrase targetPhrase) {
		// TODO Auto-generated method stub
		throw new RuntimeException();
	}

	public float lexProbTargetGivenSource(
			MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex,
			HierarchicalPhrase targetPhrase) {
		// TODO Auto-generated method stub
		throw new RuntimeException();
	}

	public float getFloorProbability() {
		return floorProbability;
	}
	
	public SymbolTable getSourceVocab() {
		return sourceVocab;
	}
	
	public SymbolTable getTargetVocab() {
		return targetVocab;
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}
}

