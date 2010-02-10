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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.logging.Level;
import java.util.logging.Logger;

import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.ParallelCorpus;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.suffix_array.HierarchicalPhrase;
import joshua.corpus.vocab.SymbolTable;
import joshua.util.Counts;
import joshua.util.Pair;

/**
 * Represents lexical probability distributions in both directions.
 * <p>
 * This class calculates the probabilities from sorted word pair
 * counts.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class LexProbs extends AbstractLexProbs {

	/** Logger for this class. */
	private static final Logger logger = 
		Logger.getLogger(LexProbs.class.getName());
	
	/** Source language symbol table. */
	protected final SymbolTable sourceVocab;
	
	/** Target language symbol table. */
	protected final SymbolTable targetVocab;
	
	/** Aligned parallel corpus. */
	protected final ParallelCorpus parallelCorpus;
	
	/** 
	 * The probability returned when no calculated lexical
	 * translation probability is known.
	 */
	protected float floorProbability;
	
	/** Word co-occurrence counts from the parallel corpus. */
	protected Counts<Integer,Integer> counts;
	
	
	/**
	 * Constructs lexical translation probabilities from a
	 * parallel corpus.
	 * 
	 * @param parallelCorpus Aligned parallel corpus
	 * @param floorProbability
	 */
	public LexProbs(ParallelCorpus parallelCorpus, float floorProbability) {
		
		logger.info("Calculating lexical translation probability table");
		this.counts = initializeCooccurrenceCounts(parallelCorpus, floorProbability);
		
		this.sourceVocab = parallelCorpus.getSourceCorpus().getVocabulary();
		this.targetVocab = parallelCorpus.getTargetCorpus().getVocabulary();
		
		this.parallelCorpus = parallelCorpus;
		this.floorProbability = floorProbability; //Float.MIN_VALUE;
		logger.info("Calculating lexical translation probability table");
		
	}

	/**
	 * Constructs lexical translation probabilities from a
	 * parallel corpus.
	 * 
	 * @param parallelCorpus Aligned parallel corpus
	 * @param ObjectIn
	 */
	public LexProbs(ParallelCorpus parallelCorpus, String lexCountsFileName) {
		
//		logger.info("Calculating lexical translation probability table");
//		this.counts = initializeCooccurrenceCounts(parallelCorpus, floorProbability);
		
		this.sourceVocab = parallelCorpus.getSourceCorpus().getVocabulary();
		this.targetVocab = parallelCorpus.getTargetCorpus().getVocabulary();
		
		this.parallelCorpus = parallelCorpus;
		
//		File lexCounts = new File(lexCountsFileName);
		this.counts = new Counts<Integer, Integer>();
		this.floorProbability = Float.MIN_VALUE;
//		File lexCounts = new File(lexCountsFileName);
//		if (!lexCounts.exists()) {
//		} else {
			
		try {
			ObjectInput in = new ObjectInputStream(new FileInputStream(lexCountsFileName));
//			readExternal(in);
			logger.info("Reading lexical translation probability table");
			readExternal(in);
			in.close();
		} catch (Exception e) {
			logger.info("Calculating lexical translation probability table");
			this.counts = initializeCooccurrenceCounts(parallelCorpus, floorProbability);
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} 
			
//		}


		
//		logger.info("Calculating lexical translation probability table");
		
	}
	/**
	 * Gets co-occurrence counts from a parallel corpus.
	 * 
	 * @param parallelCorpus Aligned parallel corpus
	 * @param floorProbability
	 * @return Word co-occurrence counts from the parallel
	 *         corpus.
	 */
	private static Counts<Integer,Integer> initializeCooccurrenceCounts(ParallelCorpus parallelCorpus, float floorProbability) {
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Counting word co-occurrence from parallel corpus. Using floor probability " + floorProbability);
		}
		
		Alignments alignments = parallelCorpus.getAlignments();
		Corpus sourceCorpus = parallelCorpus.getSourceCorpus();
		Corpus targetCorpus = parallelCorpus.getTargetCorpus();
		int numSentences = parallelCorpus.getNumSentences();
		
		Counts<Integer,Integer> counts = new Counts<Integer,Integer>(floorProbability);
		
		// Iterate over each sentence
		for (int sentenceID=0; sentenceID<numSentences; sentenceID++) {

			int sourceStart = sourceCorpus.getSentencePosition(sentenceID);
			int sourceEnd = sourceCorpus.getSentenceEndPosition(sentenceID);

			int targetStart = targetCorpus.getSentencePosition(sentenceID);
			int targetEnd = targetCorpus.getSentenceEndPosition(sentenceID);

			// Iterate over each word in the source sentence
			for (int sourceIndex=sourceStart; sourceIndex<sourceEnd; sourceIndex++) {

				// Get the token for the current source word
				int sourceWord = sourceCorpus.getWordID(sourceIndex);
				
				// Get the target indices aligned to this source word
				int[] targetPoints = alignments.getAlignedTargetIndices(sourceIndex);
				
				// If the source word is unaligned,
				// then we treat it as being aligned to a special NULL token;
				// we use Java's null to represent the NULL token
				if (targetPoints==null) {
					
					counts.incrementCount(sourceWord, null);
					
				} else {
					
					// If the source word is aligned,
					// then we must iterate over each aligned target point
					for (int targetPoint : targetPoints) {

						int targetWord = targetCorpus.getWordID(targetPoint);

						counts.incrementCount(sourceWord, targetWord);
					}
				}
				
			}
			
			// Iterate over each word in the target sentence
			for (int targetIndex=targetStart; targetIndex<targetEnd; targetIndex++) {

				// Get the token for the current source word
				int targetWord = targetCorpus.getWordID(targetIndex);
				
				// Get the source indices aligned to this target word
				int[] sourcePoints = alignments.getAlignedSourceIndices(targetIndex);
				
				// If the source word is unaligned,
				// then we treat it as being aligned to a special NULL token;
				// we use Java's null to represent the NULL token
				if (sourcePoints==null) {
					
					counts.incrementCount(null, targetWord);
					
				}
			}
			
		}
		
		return counts;
	}
	
	
	/* See Javadoc for LexicalProbabilities#sourceGivenTarget(Integer,Integer). */
	public float sourceGivenTarget(Integer sourceWord, Integer targetWord) {
		return counts.getProbability(sourceWord, targetWord);
	}
	
	/* See Javadoc for LexicalProbabilities#targetGivenSource(Integer,Integer). */
	public float targetGivenSource(Integer targetWord, Integer sourceWord) {
		return counts.getReverseProbability(targetWord, sourceWord);
	}
	
	/* See Javadoc for LexicalProbabilities#sourceGivenTarget(String,String). */
	public float sourceGivenTarget(String sourceWord, String targetWord) {
		Integer targetID = (targetWord==null) ? null : targetVocab.getID(targetWord);
		Integer sourceID = (sourceWord==null) ? null : sourceVocab.getID(sourceWord);
		
		return sourceGivenTarget(sourceID, targetID);
	}
	
	/* See Javadoc for LexicalProbabilities#targetGivenSource(String,String). */
	public float targetGivenSource(String targetWord, String sourceWord) {
		int targetID = (targetWord==null) ? null : targetVocab.getID(targetWord);
		int sourceID = (sourceWord==null) ? null : sourceVocab.getID(sourceWord);
		
		return targetGivenSource(targetID, sourceID);
	}
	
	/* See Javadoc for LexicalProbabilities#lexProbSourceGivenTarget(MatchedHierarchicalPhrases,int,HierarchicalPhrase). */
	public float lexProbSourceGivenTarget(MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex, HierarchicalPhrase targetPhrase) {
		
		float sourceGivenTarget = 1.0f;
		
		Corpus sourceCorpus = parallelCorpus.getSourceCorpus();
		Corpus targetCorpus = parallelCorpus.getTargetCorpus();
		Alignments alignments = parallelCorpus.getAlignments();
		
		// Iterate over each terminal sequence in the source phrase
		for (int seq=0; seq<sourcePhrases.getNumberOfTerminalSequences(); seq++) {
			
			// Iterate over each source index in the current terminal sequence
			for (int sourceWordIndex=sourcePhrases.getTerminalSequenceStartIndex(sourcePhraseIndex, seq),
						end=sourcePhrases.getTerminalSequenceEndIndex(sourcePhraseIndex, seq);
					sourceWordIndex<end; 
					sourceWordIndex++) {
				
								
				int sourceWord = sourceCorpus.getWordID(sourceWordIndex);
				int[] targetIndices = alignments.getAlignedTargetIndices(sourceWordIndex);
				
				float sum = 0.0f;
				float average;
				
				if (targetIndices==null) {
					
					sum += this.sourceGivenTarget(sourceWord, null);
					average = sum;
					
				} else {
					for (int targetIndex : targetIndices) {

						int targetWord = targetCorpus.getWordID(targetIndex);
						sum += sourceGivenTarget(sourceWord, targetWord);
						
					}
					average = sum / targetIndices.length;
				}
				
				sourceGivenTarget *= average;
			}
			
		}
		
		return sourceGivenTarget;
	}

	/* See Javadoc for LexicalProbabilities#lexProbTargetGivenSource(MatchedHierarchicalPhrases,int,HierarchicalPhrase). */
	public float lexProbTargetGivenSource(MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex, HierarchicalPhrase targetPhrase) {
		
		final boolean LOGGING_FINEST = logger.isLoggable(Level.FINEST);
		
		Corpus sourceCorpus = parallelCorpus.getSourceCorpus();
		Corpus targetCorpus = parallelCorpus.getTargetCorpus();
		Alignments alignments = parallelCorpus.getAlignments();
		
		StringBuilder s;
		if (LOGGING_FINEST) {
			s = new StringBuilder();
			s.append("lexProb( ");
			s.append(sourcePhrases.getPattern().toString());
			s.append(" | ");
			s.append(targetPhrase.toString());
			s.append(" )  =  1.0");
		} else {
			s = null;
		}
		
		float targetGivenSource = 1.0f;

		// Iterate over each terminal sequence in the target phrase
		for (int seq=0; seq<targetPhrase.getNumberOfTerminalSequences(); seq++) {
			
			// Iterate over each source index in the current terminal sequence
			for (int targetWordIndex=targetPhrase.getTerminalSequenceStartIndex(seq),
						end=targetPhrase.getTerminalSequenceEndIndex(seq);
					targetWordIndex<end; 
					targetWordIndex++) {
				
				int targetWord = targetCorpus.getWordID(targetWordIndex);
				int[] sourceIndices = alignments.getAlignedSourceIndices(targetWordIndex);
				
				float sum = 0.0f;
				float average;
				
				if (LOGGING_FINEST) s.append(" * (");
								
				if (sourceIndices==null) {

					sum += targetGivenSource(targetWord, null);
					average = sum;
					if (LOGGING_FINEST) s.append(sum);
					
				} else {
					
					for (int sourceIndex : sourceIndices) {

						int sourceWord = sourceCorpus.getWordID(sourceIndex);
						float value = targetGivenSource(targetWord, sourceWord);
						sum += value;
						if (LOGGING_FINEST) {
							s.append('+');
							s.append(value);
						}
					}
					average = sum / sourceIndices.length;
				}

				if (LOGGING_FINEST) s.append(')');
				targetGivenSource *= average;
				
			}
			
		}
		
		if (LOGGING_FINEST) logger.finest(s.toString());
		
		return targetGivenSource;
	}

	/* See Javadoc for LexicalProbabilities#getFloorProbability. */
	public float getFloorProbability() {
		return floorProbability;
	}
	
	/**
	 * Gets a string representation of the lexical probabilities. 
	 * <p>
	 * The returned string will have one line per word pair.
	 * The pairs are not guaranteed to be returned in any particular order.
	 * 
	 * @return a string representation of the lexical probabilities
	 */
	@Override
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		for (Pair<Integer,Integer> pair : counts) {

			Integer sourceID = pair.first;
			Integer targetID = pair.second;
			
			if (sourceID==null) {
				s.append("NULL");
			} else {
				s.append(sourceVocab.getWord(sourceID));
			}
			
			s.append(' ');
			
			if (targetID==null) {
				s.append("NULL");
			} else {
				s.append(targetVocab.getWord(targetID));
			}
			
			s.append(' ');
			s.append(targetGivenSource(targetID,sourceID));
			
			s.append(' ');
			s.append(sourceGivenTarget(sourceID,targetID));
			
			s.append('\n');
		}
		
		return s.toString();
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		
		counts.writeExternal(out);
		
	}
	
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		/*
		Map<Integer, Map<Integer, Integer>> ctMap = 
			(HashMap<Integer,Map<Integer,Integer>>) in.readObject();
		counts.setCounts(ctMap);
		
		// Read bTotals

		Map<Integer, Integer> btMap = 
			(HashMap<Integer,Integer>) in.readObject();
		
		counts.setBTotals(btMap);
		
		// Read probabilities 
		Map<Integer, Map<Integer, Float>> pbMap = 
			(HashMap<Integer,Map<Integer,Float>>) in.readObject();
		counts.setProbabilities(pbMap);
		
		// Read reverse probabilities 
		Map<Integer, Map<Integer, Float>> rpMap = 
			(HashMap<Integer,Map<Integer,Float>>) in.readObject();
		counts.setProbabilities(rpMap);
		*/
		
		this.counts.readExternal(in);
		floorProbability = counts.getFloorProbability();
		

	}
	
	public SymbolTable getSourceVocab() {
		return sourceVocab;
	}
	
	public SymbolTable getTargetVocab() {
		return targetVocab;
	}
	
	/**
	 * Gets the word co-occurrence counts for this object.
	 * 
	 * @return the word co-occurrence counts for this object.
	 */
	protected Counts<Integer,Integer> getCounts() {
		return this.counts;
	}
}
