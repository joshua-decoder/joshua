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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


import joshua.corpus.Corpus;
import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.ParallelCorpus;
import joshua.corpus.alignment.Alignments;
import joshua.corpus.vocab.SymbolTable;
import joshua.corpus.vocab.Vocabulary;
import joshua.util.Pair;

/**
 * Represents lexical probability distributions in both directions.
 * <p>
 * This class calculates the probabilities from sorted word pair counts.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class LexProbs implements LexicalProbabilities {
	
	//TODO Investigate doing all of this via sampling using the suffix array
	//     and/or allow reading it from the GIZA++ lexprob output
	
	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(LexProbs.class.getName());
	
	private final Map<Integer,Map<Integer,Float>> sourceGivenTarget;
	private final Map<Integer,Map<Integer,Float>> targetGivenSource;
	
	private final SymbolTable sourceVocab;
	private final SymbolTable targetVocab;
	
	private final ParallelCorpus parallelCorpus;
	
	/**
	 * 
	 * @param source_given_target_counts_scanner
	 * @param target_given_source_counts_scanner
	 * @param sourceVocab
	 * @param targetVocab
	 */
	public LexProbs(Scanner source_given_target_counts_scanner, Scanner target_given_source_counts_scanner, Vocabulary sourceVocab, Vocabulary targetVocab) {
		this.sourceGivenTarget = calculateLexProbs(source_given_target_counts_scanner, sourceVocab, targetVocab);
		this.targetGivenSource = calculateLexProbs(target_given_source_counts_scanner, targetVocab, sourceVocab);
		this.sourceVocab = sourceVocab;
		this.targetVocab = targetVocab;
		this.parallelCorpus = null;
	}
	
	public LexProbs(ParallelCorpus parallelCorpus) {
		
		Pair<Map<Integer,Map<Integer,Integer>>, Map<Integer,Map<Integer,Integer>>> pair = 
			initializeCooccurrenceCounts(parallelCorpus);
		
		this.sourceGivenTarget = calculateLexProbs(pair.getFirst());
		this.targetGivenSource = calculateLexProbs(pair.getSecond());
		
		this.sourceVocab = parallelCorpus.getSourceCorpus().getVocabulary();
		this.targetVocab = parallelCorpus.getTargetCorpus().getVocabulary();
		
		this.parallelCorpus = parallelCorpus;
		
	}
	
//	public LexProbs(Map<Integer,Map<Integer,Integer>> sourceToTargetCounts, Map<Integer,Map<Integer,Integer>> targetToSourceCounts, SymbolTable sourceVocab, SymbolTable targetVocab) {
//		this.sourceVocab = sourceVocab;
//		this.targetVocab = targetVocab;
//		this.sourceGivenTarget = calculateLexProbs(targetToSourceCounts);
//		this.targetGivenSource = calculateLexProbs(sourceToTargetCounts);
//	}
	
	private static Pair<Map<Integer,Map<Integer,Integer>>, Map<Integer,Map<Integer,Integer>>> initializeCooccurrenceCounts(ParallelCorpus parallelCorpus) {
		
		Map<Integer,Map<Integer,Integer>> sourceToTargetCounts = new HashMap<Integer,Map<Integer,Integer>>();
		Map<Integer,Map<Integer,Integer>> targetToSourceCounts = new HashMap<Integer,Map<Integer,Integer>>();
		
		Alignments alignments = parallelCorpus.getAlignments();
		Corpus sourceCorpus = parallelCorpus.getSourceCorpus();
		Corpus targetCorpus = parallelCorpus.getTargetCorpus();
		int numSentences = parallelCorpus.getNumSentences();
				
		// Iterate over each sentence
		for (int sentenceID=0; sentenceID<numSentences; sentenceID++) {

			int sourceStart = sourceCorpus.getSentencePosition(sentenceID);
			int sourceEnd = sourceCorpus.getSentenceEndPosition(sentenceID);

			int targetStart = targetCorpus.getSentencePosition(sentenceID);
			int targetEnd = targetCorpus.getSentenceEndPosition(sentenceID);

			for (int sourceIndex=sourceStart; sourceIndex<sourceEnd; sourceIndex++) {

				int[] targetPoints = alignments.getAlignedTargetIndices(sourceIndex);

				int sourceWord = sourceCorpus.getWordID(sourceIndex);

				// We may have never seen this source word before
				if (! sourceToTargetCounts.containsKey(sourceWord)) {
					// In that case, initialize a new map
					sourceToTargetCounts.put(sourceWord, new HashMap<Integer,Integer>());
				}

				// Get the map for the current source word
				Map<Integer,Integer> counts = sourceToTargetCounts.get(sourceWord);

				// If the source word is unaligned,
				// then we treat it as being aligned to a special NULL token;
				// we use Java's null to represent the NULL token
				if (targetPoints==null) {

					if (counts.containsKey(null)) {
						counts.put(null, counts.get(null) + 1);
					} else {
						counts.put(null, 1);
					}


				} else {

					// If the source word is aligned,
					// then we must iterate over each aligned target point
					
					for (int targetPoint : targetPoints) {

						int targetWord = targetCorpus.getWordID(targetPoint);

						if (counts.containsKey(targetWord)) {
							counts.put(targetWord, counts.get(targetWord) + 1);
						} else {
							counts.put(targetWord, 1);
						}
					}

				}

			}


			// TODO Repeat the above comments here, and split this method into two
			
			////
			for (int targetIndex=targetStart; targetIndex<targetEnd; targetIndex++) {

				int targetWord = targetCorpus.getWordID(targetIndex);

				if (! targetToSourceCounts.containsKey(targetWord)) {
					targetToSourceCounts.put(targetWord, new HashMap<Integer,Integer>());
				}

				Map<Integer,Integer> counts = targetToSourceCounts.get(targetWord);

				int[] sourcePoints = alignments.getAlignedSourceIndices(targetIndex);
				
				if (sourcePoints==null) {

					if (counts.containsKey(null)) {
						counts.put(null, counts.get(null) + 1);
					} else {
						counts.put(null, 1);
					}


				} else {

					for (int sourcePoint : sourcePoints) {

						int sourceWord = sourceCorpus.getWordID(sourcePoint);

						if (counts.containsKey(sourceWord)) {
							counts.put(sourceWord, counts.get(sourceWord) + 1);
						} else {
							counts.put(sourceWord, 1);
						}
					}

				}

			}
			////
		}
		
		return new Pair<Map<Integer,Map<Integer,Integer>>, Map<Integer,Map<Integer,Integer>>>(
				targetToSourceCounts, sourceToTargetCounts);
	}
	
//	public static LexicalProbabilities createLexicalProbabilities(ParallelCorpus parallelCorpus) {
//
//		Alignments alignments = parallelCorpus.getAlignments();
//		Corpus sourceCorpus = parallelCorpus.getSourceCorpus();
//		Corpus targetCorpus = parallelCorpus.getTargetCorpus();
//		int numSentences = parallelCorpus.getNumSentences();
//
//		Map<Integer,Map<Integer,Integer>> sourceToTargetCounts = new HashMap<Integer,Map<Integer,Integer>>();
//		Map<Integer,Map<Integer,Integer>> targetToSourceCounts = new HashMap<Integer,Map<Integer,Integer>>();
//
//
//		
//
//		return new LexProbs(sourceToTargetCounts, targetToSourceCounts, sourceCorpus.getVocabulary(), targetCorpus.getVocabulary());
//		
//	}
	
	/**
	 * 
	 * @param sourceWord
	 * @param targetWord
	 * @return
	 */
	public float sourceGivenTarget(Integer sourceWord, Integer targetWord) {
		return sourceGivenTarget.get(targetWord).get(sourceWord);
	}
	
	/**
	 * 
	 * @param targetWord
	 * @param sourceWord
	 * @return
	 */
	public float targetGivenSource(Integer targetWord, Integer sourceWord) {
		return targetGivenSource.get(sourceWord).get(targetWord);
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
		Map<Integer,Float> map = sourceGivenTarget.get(targetID);
		return map.get(sourceID);
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
		Map<Integer,Float> map = targetGivenSource.get(sourceID);
		return map.get(targetID);
	}
	

	/**
	 * Calculates a lexical translation probability distribution.
	 * 
	 * @param Map from givenWord to word to co-occurrence count
	 * @return A map from givenWord to word to probability
	 */
	private static Map<Integer,Map<Integer,Float>> calculateLexProbs(Map<Integer,Map<Integer,Integer>> counterMaps) {

		Map<Integer,Map<Integer,Float>> map = new HashMap<Integer,Map<Integer,Float>>();

		
		for (Map.Entry<Integer,Map<Integer,Integer>> entry : counterMaps.entrySet()) {
			
			Integer givenWord = entry.getKey();
			Map<Integer,Integer> wordCounts = entry.getValue();
			
			int denominatorCounter = 0;
			for (int count : wordCounts.values()) {
				denominatorCounter += count;
			}
			
			if (denominatorCounter > 0) {
				
				Map<Integer,Float> probsMap = new HashMap<Integer,Float>();
				
				float denominator = (float) denominatorCounter;
				for (Map.Entry<Integer, Integer> wordCountEntry : wordCounts.entrySet()) {
					
					Integer word = wordCountEntry.getKey();
					int count = wordCountEntry.getValue();
					
					// This is the maximum likelihood estimate of p(word | givenWord)
					probsMap.put(word, count / denominator);
					
				}
				
				map.put(givenWord, probsMap);
				
			}
						
		}
		
		return map;
	}
	
	
	/**
	 * 
	 * @param word_count_scanner
	 * @param wordsVocab
	 * @param givenWordsVocab
	 * @return A map from givenWord to word to probability
	 */
	private Map<Integer,Map<Integer,Float>> calculateLexProbs(Scanner word_count_scanner, Vocabulary wordsVocab, Vocabulary givenWordsVocab) {

		Map<Integer,Map<Integer,Float>> map = new HashMap<Integer,Map<Integer,Float>>();

		boolean finished = false;
		String alpha = null;
		int denominator = 0;
		Map<String,Integer> word_pairs = new HashMap<String,Integer>();
		int count = 0;
		while (! finished) {

			if (word_count_scanner.hasNextLine()) {
				count++;
				
				String[] data = word_count_scanner.nextLine().trim().split("\\s+");

				if (data.length == 3) {
					int pair_count = Integer.valueOf(data[0]);
					String word = data[2];
					String given_word = data[1];
					
					if (logger.isLoggable(Level.FINE)) logger.fine("count( " + word + " | " + given_word + " ) = " + pair_count);
					
					
					if (!given_word.equals(alpha) && alpha!=null ) {

						int givenWordID = givenWordsVocab.getID(alpha);

						Map<Integer,Float> probs;
						if (!map.containsKey(givenWordID)) {
							probs = new HashMap<Integer,Float>();
							map.put(givenWordID, probs);
							if (logger.isLoggable(Level.FINER)) logger.finer("Creating new map for " + given_word);
						} else {
							probs = map.get(givenWordID);
							if (logger.isLoggable(Level.FINER)) logger.finer("Have exisiting map for " + given_word);
						}


						for (Map.Entry<String, Integer> entry : word_pairs.entrySet()) {

							int wordID = wordsVocab.getID(entry.getKey());
							float prob = (float) entry.getValue() / (float) denominator;
							if (logger.isLoggable(Level.FINE)) logger.fine("1 Putting P( " + wordsVocab.getWord(wordID) + "{ "+wordID+ "} | " + givenWordsVocab.getWord(givenWordID) +"{ "+givenWordID+ "} " + ") = " + prob);
							probs.put(wordID, prob);
							if (logger.isLoggable(Level.FINEST)) logger.finest("Now have P( " + wordsVocab.getWord(wordID) + "{ "+wordID+ "} | " + givenWordsVocab.getWord(givenWordID) +"{ "+givenWordID+ "} " + ") = " + map.get(givenWordID).get(wordID));
							
						}

						word_pairs.clear();
						denominator = 0;
						alpha = given_word;
						if (logger.isLoggable(Level.FINER)) logger.finer("alpha is now " + alpha);

					}

					word_pairs.put(word, pair_count);
					denominator += pair_count;
					if (alpha==null) alpha = given_word;

				} else {
					finished = true;
				}
			} else {
				finished = true;
			}

		}

		if (alpha != null  &&  denominator > 0) {

			int givenWordID = givenWordsVocab.getID(alpha);

			Map<Integer,Float> probs;
			if (!map.containsKey(givenWordID)) {
				probs = new HashMap<Integer,Float>();
				map.put(givenWordID, probs);
			} else {
				probs = map.get(givenWordID);
			}

			for (Map.Entry<String, Integer> entry : word_pairs.entrySet()) {
				int wordID = wordsVocab.getID(entry.getKey());
				float prob = (float) entry.getValue() / (float) denominator;
				probs.put(wordID, prob);
				if (logger.isLoggable(Level.FINE)) logger.fine("2 Putting P( " + wordsVocab.getWord(wordID) + " | " + givenWordsVocab.getWord(givenWordID) + ") = " + prob);
			}
		}

		return map;
	}

	public Pair<Float, Float> calculateLexProbs(MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex) {

		float sourceGivenTarget = 1.0f;
		
		Map<Integer,List<Integer>> reverseAlignmentPoints = new HashMap<Integer,List<Integer>>(); 
	
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
				
				if (targetIndices==null) {
					
					sum += this.sourceGivenTarget(sourceWord, null);
					
				} else {
					for (int targetIndex : targetIndices) {

						int targetWord = targetCorpus.getWordID(targetIndex);
						sum += sourceGivenTarget(sourceWord, targetWord);
						
						// Keeping track of the reverse alignment points 
						//   (we need to do this convoluted step because we don't actually have a HierarchicalPhrase for the target side)
						if (!reverseAlignmentPoints.containsKey(targetIndex)) {
							reverseAlignmentPoints.put(targetIndex, new ArrayList<Integer>());
						}
						reverseAlignmentPoints.get(targetIndex).add(sourceWord);
					}
				}
				
				float average = sum / targetIndices.length;
				sourceGivenTarget *= average;
			}
			
		}

		
		float targetGivenSource = 1.0f;

		// Iterate over each terminal sequence in the source phrase
		for (int seq=0; seq<sourcePhrases.getNumberOfTerminalSequences(); seq++) {
			
			int sourceSequenceStart = sourcePhrases.getTerminalSequenceStartIndex(sourcePhraseIndex, seq);
			int sourceSequenceEnd = sourcePhrases.getTerminalSequenceEndIndex(sourcePhraseIndex, seq);
			
			
			
			// Iterate over each source index in the current terminal sequence
			for (int sourceWordIndex=sourcePhrases.getTerminalSequenceStartIndex(sourcePhraseIndex, seq),
						end=sourcePhrases.getTerminalSequenceEndIndex(sourcePhraseIndex, seq);
					sourceWordIndex<end; 
					sourceWordIndex++) {
				
			}
			
		}
		
		// Actually calculate the reverse lexical translation probabilities
		for (Map.Entry<Integer, List<Integer>> entry : reverseAlignmentPoints.entrySet()) {

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
}
