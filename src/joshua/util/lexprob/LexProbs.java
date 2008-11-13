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
package joshua.util.lexprob;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


import joshua.sarray.HierarchicalPhrase;
import joshua.util.Pair;
import joshua.util.sentence.Vocabulary;

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
	
	private final Vocabulary sourceVocab;
	private final Vocabulary targetVocab;
	
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
	}
	
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

	public Pair<Float, Float> calculateLexProbs(HierarchicalPhrase sourcePhrase) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
