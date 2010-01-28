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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import joshua.corpus.MatchedHierarchicalPhrases;
import joshua.corpus.alignment.AlignmentGrid;
import joshua.corpus.suffix_array.HierarchicalPhrase;
import joshua.corpus.suffix_array.Pattern;
import joshua.corpus.vocab.SymbolTable;

/**
 * Represents lexical probability distributions in both directions.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface LexicalProbabilities {
	
	/**
	 * Gets the lexical translation probability of the source
	 * word given the target word.
	 * 
	 * @param sourceWord Source language word symbol
	 * @param targetWord Target language word symbol
	 * @return the lexical translation probability of the source
	 *         word given the target word
	 */
	float sourceGivenTarget(Integer sourceWord, Integer targetWord);
	
	/**
	 * Gets the lexical translation probability of the target
	 * word given the source word.
	 * 
	 * @param targetWord Target language word symbol
	 * @param sourceWord Source language word symbol
	 * @return the lexical translation probability of the target
	 *         word given the source word
	 */
	float targetGivenSource(Integer targetWord, Integer sourceWord);
	
	/**
	 * Gets the lexical translation probability of the source
	 * word given the target word.
	 * 
	 * @param sourceWord Source language word
	 * @param targetWord Target language word
	 * @return the lexical translation probability of the source
	 *         word given the target word
	 */
	float sourceGivenTarget(String sourceWord, String targetWord);
	
	/**
	 * Gets the lexical translation probability of the target
	 * word given the source word.
	 *
	 * @param targetWord Target language word symbol
	 * @param sourceWord Source language word symbol
	 * @return the lexical translation probability of the target
	 *         word given the source word
	 */
	float targetGivenSource(String targetWord, String sourceWord);

	/**
	 * Gets the lexical translation probability of a source
	 * phrase given a target phrase.
	 * 
	 * @param sourcePhrases Collection of source phrases with
	 *                      a common pattern
	 * @param sourcePhraseIndex Index (into the collection) of
	 *                      a particular source phrase instance
	 * @param targetPhrase  Instance of a particular target phrase
	 * @return the lexical translation probability of a source
	 *         phrase given a target phrase.
	 */
	float lexProbSourceGivenTarget(MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex, HierarchicalPhrase targetPhrase);
	
	/**
	 * Gets the lexical translation probability of a target
	 * phrase given a source phrase.
	 * 
	 * @param sourcePhrases Collection of source phrases with
	 *                      a common pattern
	 * @param sourcePhraseIndex Index (into the collection) of
	 *                      a particular source phrase instance
	 * @param targetPhrase  Instance of a particular target phrase
	 * @return the lexical translation probability of a target
	 *         phrase given a source phrase.
	 */
	float lexProbTargetGivenSource(MatchedHierarchicalPhrases sourcePhrases, int sourcePhraseIndex, HierarchicalPhrase targetPhrase);
	
	/**
	 * Gets the lexical translation probability 
	 * of a source pattern given a target pattern,
	 * assuming the most likely (Viterbi) alignment 
	 * between the words in the patterns.
	 * 
	 * @param sourcePattern Source language pattern
	 * @param targetPattern Target language pattern
	 * @return The lexical translation probability 
	 *         of a source pattern given a target pattern
	 */
	public float lexProbSourceGivenTarget(Pattern sourcePattern, Pattern targetPattern);
	
	/**
	 * Gets the lexical translation probability 
	 * of a target pattern given a source pattern,
	 * assuming the most likely (Viterbi) alignment 
	 * between the words in the patterns.
	 * 
	 * @param targetPattern Target language pattern
	 * @param sourcePattern Source language pattern
	 * @return The lexical translation probability 
	 *         of a target pattern given a source pattern
	 */
	public float lexProbTargetGivenSource(Pattern targetPattern, Pattern sourcePattern);
	
	/**
	 * Gets the most likely (Viterbi) alignment between 
	 * the words in the source pattern and the words in the target pattern,
	 * as defined by the lexical translation probability distribution 
	 * of source words given target words. 
	 * 
	 * @param sourcePattern Source language pattern
	 * @param targetPattern Target language pattern
	 * @return The most likely (Viterbi) alignment between 
	 *         the words in the source pattern and 
	 *         the words in the target pattern,
	 *         as defined by the lexical translation probability distribution 
	 *         of source words given target words
	 */
	public AlignmentGrid getSourceGivenTargetAlignments(Pattern sourcePattern, Pattern targetPattern); 
	
	/**
	 * Gets the most likely (Viterbi) alignment between 
	 * the words in the target pattern and the words in the source pattern,
	 * as defined by the lexical translation probability distribution 
	 * of target words given source words. 
	 * 
	 * @param targetPattern Target language pattern
	 * @param sourcePattern Source language pattern
	 * @return The most likely (Viterbi) alignment between 
	 *         the words in the target pattern and 
	 *         the words in the source pattern,
	 *         as defined by the lexical translation probability distribution 
	 *         of target words given source words
	 */
	public AlignmentGrid getTargetGivenSourceAlignments(Pattern targetPattern, Pattern sourcePattern); 
	
	
	
	/**
	 * Gets the probability returned when no calculated lexical
	 * translation probability is known.
	 *
	 * @return the probability returned when no calculated
	 *         lexical translation probability is known
	 */
	float getFloorProbability();
	
	/**
	 * Gets the symbol table for the source language.
	 * This may well be the same table used for the target language.
	 * 
	 * @return The symbol table for the source language.
	 */
	SymbolTable getSourceVocab();
	
	/**
	 * Gets the symbol table for the target language.
	 * This may well be the same table used for the source language.
	 * 
	 * @return The symbol table for the target language.
	 */
	SymbolTable getTargetVocab();
	
	/**
	 * Read external binarized object
	 * @param in
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException;
	
	/**
	 * Write the object externally
	 * @param out
	 */
	public void writeExternal(ObjectOutput out) throws IOException;
	
}
